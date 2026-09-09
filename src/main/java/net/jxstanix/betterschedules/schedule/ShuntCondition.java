package net.jxstanix.betterschedules.schedule;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
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
 * Rolls the train a fixed distance without leaving the station, then reports itself as met. Placed
 * between two cargo conditions it turns a single stop into a multi-wagon shunting sequence.
 */
public class ShuntCondition extends ScheduleWaitCondition {

	public enum Unit {
		METERS, CARRIAGES;

		public static List<Component> translatedOptions() {
			return Arrays.stream(values())
				.map(u -> (Component) BetterSchedules.translate("unit." + u.name().toLowerCase()))
				.toList();
		}
	}

	public ShuntCondition() {
		data.putInt("Distance", 5);
		data.putInt("Unit", Unit.METERS.ordinal());
	}

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("shunt");
	}

	public int getDistance() {
		return intData("Distance");
	}

	public Unit getUnit() {
		return enumData("Unit", Unit.class);
	}

	private double resolveDistance(Train train) {
		int amount = Math.abs(getDistance());
		return getUnit() == Unit.CARRIAGES ? amount * TrainShunter.averageCarriageLength(train) : amount;
	}

	@Override
	public boolean tickCompletion(Level level, Train train, CompoundTag context) {
		int configured = getDistance();
		if (configured == 0)
			return true;

		double distance = resolveDistance(train);
		TrainShunter.Result result = TrainShunter.tick(level, train, context, distance, configured > 0);
		if (result != TrainShunter.Result.MOVING)
			return true;

		if (level.getGameTime() % 10 == 0)
			requestStatusToUpdate(context);
		return false;
	}

	@Override
	public Pair<ItemStack, Component> getSummary() {
		int distance = getDistance();
		String arrow = distance < 0 ? "<< " : ">> ";
		return Pair.of(new ItemStack(Items.RAIL),
			Component.literal(arrow + Math.abs(distance) + (getUnit() == Unit.CARRIAGES ? "x" : "m")));
	}

	@Override
	public List<Component> getTitleAs(String type) {
		int distance = getDistance();
		MutableComponent detail = BetterSchedules
			.translate(distance < 0 ? "schedule.condition.shunt.backward" : "schedule.condition.shunt.forward",
				Math.abs(distance), BetterSchedules.translate("unit." + getUnit().name().toLowerCase()))
			.withStyle(ChatFormatting.DARK_AQUA);
		return ImmutableList.of(Component.translatable(getId().getNamespace() + ".schedule." + type + ".shunt"), detail);
	}

	@Override
	public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag context) {
		double left = TrainShunter.remaining(context, resolveDistance(train));
		return BetterSchedules.translate("schedule.condition.shunt.status", String.format("%.1f", left));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addScrollInput(0, 31, (i, l) -> {
			i.titled(BetterSchedules.translate("schedule.condition.shunt.distance"))
				.withShiftStep(5)
				.withRange(-64, 65);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, "Distance");

		builder.addSelectionScrollInput(36, 85, (i, l) -> {
			i.forOptions(Unit.translatedOptions())
				.titled(BetterSchedules.translate("schedule.condition.shunt.unit"));
		}, "Unit");
	}
}
