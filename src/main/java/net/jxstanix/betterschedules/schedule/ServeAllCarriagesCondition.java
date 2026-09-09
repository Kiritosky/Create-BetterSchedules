package net.jxstanix.betterschedules.schedule;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import net.createmod.catnip.data.Pair;
import net.jxstanix.betterschedules.BetterSchedules;
import net.jxstanix.betterschedules.train.TrainShunter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Parks each carriage at the station's loading spot in turn: wait until cargo stops moving, roll
 * forward by one carriage, repeat. Works the same for filling and for draining, because it only
 * watches whether any cargo was exchanged recently.
 */
public class ServeAllCarriagesCondition extends ScheduleWaitCondition {

	private static final int PHASE_WAIT = 0;
	private static final int PHASE_ADVANCE = 1;
	private static final int PHASE_RETURN = 2;

	private static final String PHASE = "BsPhase";
	private static final String STEP = "BsStep";
	private static final String PITCH = "BsPitch";
	private static final String TRAVELLED = "BsTravelled";
	private static final String INITIALISED = "BsInit";

	public enum Finish {
		RETURN, STAY;

		public static List<Component> translatedOptions() {
			return Arrays.stream(values())
				.map(f -> (Component) BetterSchedules.translate("finish." + f.name().toLowerCase()))
				.toList();
		}
	}

	public ServeAllCarriagesCondition() {
		data.putInt("IdleTime", 3);
		data.putInt("Finish", Finish.RETURN.ordinal());
	}

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("serve_all_carriages");
	}

	public int getIdleSeconds() {
		return Math.max(1, intData("IdleTime"));
	}

	public boolean returnsToStart() {
		return enumData("Finish", Finish.class) == Finish.RETURN;
	}

	@Override
	public boolean tickCompletion(Level level, Train train, CompoundTag context) {
		if (train.carriages.isEmpty())
			return true;

		if (!context.getBoolean(INITIALISED)) {
			context.putBoolean(INITIALISED, true);
			resetIdleTrackers(train);
		}

		return switch (context.getInt(PHASE)) {
			case PHASE_ADVANCE -> tickAdvance(level, train, context);
			case PHASE_RETURN -> tickReturn(level, train, context);
			default -> tickWait(level, train, context);
		};
	}

	private boolean tickWait(Level level, Train train, CompoundTag context) {
		int idleTicks = getIdleSeconds() * 20;
		int idle = Integer.MAX_VALUE;
		for (Carriage carriage : train.carriages)
			if (carriage.storage != null)
				idle = Math.min(idle, carriage.storage.getTicksSinceLastExchange());

		// storage is momentarily null while a train is being assembled
		if (idle == Integer.MAX_VALUE)
			return false;

		if (idle <= idleTicks) {
			if (idle % 100 == 0)
				requestStatusToUpdate(context);
			return false;
		}

		int step = context.getInt(STEP);
		if (step + 1 < train.carriages.size()) {
			context.putInt(PHASE, PHASE_ADVANCE);
			context.putDouble(PITCH, TrainShunter.carriagePitch(train, step));
			requestStatusToUpdate(context);
			return false;
		}

		return finish(train, context);
	}

	private boolean tickAdvance(Level level, Train train, CompoundTag context) {
		double pitch = context.getDouble(PITCH);
		TrainShunter.Result result = TrainShunter.tick(level, train, context, pitch, true);

		if (result == TrainShunter.Result.MOVING)
			return false;
		if (result == TrainShunter.Result.BLOCKED)
			return finish(train, context);

		context.putDouble(TRAVELLED, context.getDouble(TRAVELLED) + pitch);
		context.putInt(STEP, context.getInt(STEP) + 1);
		context.putInt(PHASE, PHASE_WAIT);
		resetIdleTrackers(train);
		requestStatusToUpdate(context);
		return false;
	}

	private boolean tickReturn(Level level, Train train, CompoundTag context) {
		double travelled = context.getDouble(TRAVELLED);
		return TrainShunter.tick(level, train, context, travelled, false) != TrainShunter.Result.MOVING;
	}

	/** Either roll back to where the train docked, or stop right here. */
	private boolean finish(Train train, CompoundTag context) {
		if (!returnsToStart() || context.getDouble(TRAVELLED) <= 0) {
			TrainShunter.stop(train, context);
			return true;
		}
		context.putInt(PHASE, PHASE_RETURN);
		requestStatusToUpdate(context);
		return false;
	}

	private static void resetIdleTrackers(Train train) {
		for (Carriage carriage : train.carriages)
			carriage.storage.resetIdleCargoTracker();
	}

	@Override
	public Pair<ItemStack, Component> getSummary() {
		return Pair.of(new ItemStack(Items.CHEST_MINECART),
			BetterSchedules.translate("schedule.condition.serve_all_carriages.summary", getIdleSeconds()));
	}

	@Override
	public List<Component> getTitleAs(String type) {
		return ImmutableList.of(
			Component.translatable(getId().getNamespace() + ".schedule." + type + ".serve_all_carriages"),
			BetterSchedules
				.translate("schedule.condition.serve_all_carriages.detail", getIdleSeconds(),
					BetterSchedules.translate("finish." + (returnsToStart() ? "return" : "stay")))
				.withStyle(ChatFormatting.DARK_AQUA));
	}

	@Override
	public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag context) {
		int total = Math.max(1, train.carriages.size());
		int at = Math.min(total, context.getInt(STEP) + 1);
		return switch (context.getInt(PHASE)) {
			case PHASE_ADVANCE -> BetterSchedules.translate("schedule.condition.serve_all_carriages.shunting",
				Math.min(total, at + 1), total);
			case PHASE_RETURN -> BetterSchedules.translate("schedule.condition.serve_all_carriages.returning");
			default -> BetterSchedules.translate("schedule.condition.serve_all_carriages.status", at, total);
		};
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addScrollInput(0, 31, (i, l) -> {
			i.titled(BetterSchedules.translate("schedule.condition.serve_all_carriages.idle_time"))
				.withShiftStep(5)
				.withRange(1, 61);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, "IdleTime");

		builder.addSelectionScrollInput(36, 85, (i, l) -> {
			i.forOptions(Finish.translatedOptions())
				.titled(BetterSchedules.translate("schedule.condition.serve_all_carriages.finish"));
		}, "Finish");
	}
}
