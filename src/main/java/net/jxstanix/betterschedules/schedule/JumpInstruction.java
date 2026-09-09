package net.jxstanix.betterschedules.schedule;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime.State;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import net.createmod.catnip.data.Pair;
import net.jxstanix.betterschedules.BetterSchedules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Sends the schedule back to an earlier entry, optionally only a limited number of times. Lets a
 * train run the same few stops repeatedly without the entries being pasted in over and over.
 */
public class JumpInstruction extends ScheduleInstruction {

	/** Counts jumps taken so far. Lives in the instruction data so it survives a save per train. */
	private static final String TAKEN = "Taken";

	public JumpInstruction() {
		data.putInt("Target", 1);
		data.putInt("Repeats", 0);
		data.putInt(TAKEN, 0);
	}

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("jump");
	}

	@Override
	public boolean supportsConditions() {
		return false;
	}

	/** 1-based entry number as shown in the editor. */
	public int getTarget() {
		return intData("Target");
	}

	/** 0 means jump every time. */
	public int getRepeats() {
		return intData("Repeats");
	}

	@Override
	@Nullable
	public DiscoveredPath start(ScheduleRuntime runtime, Level level) {
		int size = runtime.schedule.entries.size();
		int target = Mth.clamp(getTarget() - 1, 0, Math.max(0, size - 1));
		int limit = getRepeats();
		int taken = intData(TAKEN);

		boolean exhausted = limit > 0 && taken >= limit;
		boolean selfReferential = target == runtime.currentEntry;

		runtime.state = State.PRE_TRANSIT;

		if (exhausted || selfReferential) {
			data.putInt(TAKEN, 0);
			runtime.currentEntry++;
			return null;
		}

		if (limit > 0)
			data.putInt(TAKEN, taken + 1);
		runtime.currentEntry = target;
		return null;
	}

	@Override
	public Pair<ItemStack, Component> getSummary() {
		int limit = getRepeats();
		String label = "-> " + getTarget() + (limit > 0 ? " x" + limit : "");
		return Pair.of(new ItemStack(Items.COMPASS), Component.literal(label));
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return new ItemStack(Items.COMPASS);
	}

	@Override
	public List<Component> getTitleAs(String type) {
		int limit = getRepeats();
		Component detail = limit > 0
			? BetterSchedules.translate("schedule.instruction.jump.limited", getTarget(), limit)
			: BetterSchedules.translate("schedule.instruction.jump.always", getTarget());
		return ImmutableList.of(
			BetterSchedules.translate("schedule.instruction.jump.summary").withStyle(ChatFormatting.GOLD),
			detail.copy().withStyle(ChatFormatting.DARK_AQUA));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addScrollInput(0, 58, (i, l) -> {
			i.titled(BetterSchedules.translate("schedule.instruction.jump.target"))
				.withRange(1, 51);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, "Target");

		builder.addScrollInput(63, 58, (i, l) -> {
			i.titled(BetterSchedules.translate("schedule.instruction.jump.repeats"))
				.withRange(0, 65);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, "Repeats");
	}
}
