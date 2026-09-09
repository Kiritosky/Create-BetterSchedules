package net.jxstanix.betterschedules.schedule;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime.State;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import net.createmod.catnip.data.Pair;
import net.jxstanix.betterschedules.BetterSchedules;
import net.jxstanix.betterschedules.train.TrainCargo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A destination the train only travels to when its cargo matches. When the test fails the entry is
 * skipped and the schedule moves straight on to the next one, so a cyclic schedule made of these
 * reads as a list of routing rules evaluated in order.
 *
 * <p>Extends Create's own destination so the station matching, conductor check and pathfinding stay
 * identical - only the decision to go at all is new.
 */
public class ConditionalDestinationInstruction extends DestinationInstruction {

	public enum Mode {
		CARRYING, NOT_CARRYING;

		public static List<Component> translatedOptions() {
			return Arrays.stream(values())
				.map(m -> (Component) BetterSchedules.translate("mode." + m.name().toLowerCase()))
				.toList();
		}
	}

	private FilterItemStack filter = FilterItemStack.empty();

	public ConditionalDestinationInstruction() {
		data.putInt("Mode", Mode.CARRYING.ordinal());
	}

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("destination_if_carrying");
	}

	public Mode getMode() {
		return enumData("Mode", Mode.class);
	}

	/** True when the train's cargo satisfies this entry's rule. */
	public boolean matches(Level level, com.simibubi.create.content.trains.entity.Train train) {
		boolean carrying = TrainCargo.isCarrying(level, train, filter);
		return getMode() == Mode.CARRYING ? carrying : !carrying;
	}

	@Override
	@Nullable
	public DiscoveredPath start(ScheduleRuntime runtime, Level level) {
		if (matches(level, runtime.train))
			return super.start(runtime, level);

		// Create also calls this while in transit to refresh the path; only skip when we are the
		// entry actually being started, otherwise we would advance the schedule mid-journey.
		if (runtime.state == State.PRE_TRANSIT)
			runtime.currentEntry++;
		return null;
	}

	@Override
	public int slotsTargeted() {
		return 1;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		filter = FilterItemStack.of(stack);
	}

	@Override
	public ItemStack getItem(int slot) {
		return filter.item();
	}

	@Override
	protected void writeAdditional(HolderLookup.Provider registries, CompoundTag tag) {
		super.writeAdditional(registries, tag);
		tag.put("Filter", filter.serializeNBT(registries));
	}

	@Override
	protected void readAdditional(HolderLookup.Provider registries, CompoundTag tag) {
		super.readAdditional(registries, tag);
		if (tag.contains("Filter"))
			filter = FilterItemStack.of(registries, tag.getCompound("Filter"));
	}

	@Override
	public Pair<ItemStack, Component> getSummary() {
		ItemStack icon = filter.isEmpty() ? AllBlocks.TRACK_STATION.asStack() : filter.item();
		return Pair.of(icon, Component.literal((getMode() == Mode.CARRYING ? "" : "! ") + getFilter()));
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return AllBlocks.TRACK_STATION.asStack();
	}

	@Override
	public List<Component> getTitleAs(String type) {
		return ImmutableList.of(
			BetterSchedules.translate("schedule.instruction.destination_if_carrying.summary")
				.withStyle(ChatFormatting.GOLD),
			BetterSchedules
				.translate("schedule.instruction.destination_if_carrying.detail",
					BetterSchedules.translate("mode." + getMode().name().toLowerCase()), Component.literal(getFilter()))
				.withStyle(ChatFormatting.DARK_AQUA));
	}

	@Override
	public List<Component> getSecondLineTooltip(int slot) {
		return ImmutableList.of(BetterSchedules.translate("schedule.instruction.destination_if_carrying.slot"),
			BetterSchedules.translate("schedule.instruction.destination_if_carrying.slot_1")
				.withStyle(ChatFormatting.GRAY));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addTextInput(0, 85, (e, t) -> modifyEditBox(e), "Text");
		builder.addSelectionScrollInput(90, 31, (i, l) -> {
			i.forOptions(Mode.translatedOptions())
				.titled(BetterSchedules.translate("schedule.instruction.destination_if_carrying.mode"));
		}, "Mode");
	}
}
