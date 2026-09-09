package net.jxstanix.betterschedules.schedule;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.condition.LazyTickedScheduleCondition;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import net.createmod.catnip.data.Pair;
import net.jxstanix.betterschedules.BetterSchedules;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Holds the train at its current stop until the station it is about to travel to is actually free.
 * Create's pathfinding only penalises occupied stations, so without this a train still departs and
 * then sits on the approach, blocking the signal behind it.
 */
public class PlatformFreeCondition extends LazyTickedScheduleCondition {

	public enum Occupancy {
		DOCKED_OR_INBOUND, DOCKED;

		public static List<Component> translatedOptions() {
			return Arrays.stream(values())
				.map(o -> (Component) BetterSchedules.translate("occupancy." + o.name().toLowerCase()))
				.toList();
		}
	}

	private static final String TARGET = "Target";
	private static final String WAITED = "Waited";

	public PlatformFreeCondition() {
		super(10);
		data.putInt("Occupancy", Occupancy.DOCKED_OR_INBOUND.ordinal());
	}

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("next_platform_free");
	}

	public Occupancy getOccupancy() {
		return enumData("Occupancy", Occupancy.class);
	}

	@Override
	protected boolean lazyTickCompletion(Level level, Train train, CompoundTag context) {
		if (train.graph == null)
			return true;

		DestinationInstruction next = nextDestination(train.runtime);
		if (next == null)
			return true;

		String regex = next.getFilterForRegex();
		boolean anyMatch = false;

		for (GlobalStation station : train.graph.getPoints(EdgePointType.STATION)) {
			if (!station.name.matches(regex))
				continue;
			anyMatch = true;
			Train occupant = getOccupancy() == Occupancy.DOCKED ? station.getPresentTrain() : station.getImminentTrain();
			if (occupant == null || occupant == train)
				return true;
		}

		// Nothing matches the filter at all - that is the destination's problem to report, not ours
		if (!anyMatch)
			return true;

		context.putString(TARGET, next.getFilter());
		int waited = context.getInt(WAITED) + 10;
		context.putInt(WAITED, waited);
		if (waited % 100 == 0)
			requestStatusToUpdate(context);
		return false;
	}

	/**
	 * The destination this stop will travel to next. Conditional destinations may end up skipped, so
	 * this is the first candidate rather than a guarantee.
	 */
	@Nullable
	private static DestinationInstruction nextDestination(ScheduleRuntime runtime) {
		if (runtime == null || runtime.schedule == null)
			return null;
		List<ScheduleEntry> entries = runtime.schedule.entries;
		int size = entries.size();
		if (size == 0)
			return null;

		for (int i = 1; i <= size; i++) {
			int index = runtime.currentEntry + i;
			if (index >= size) {
				if (!runtime.schedule.cyclic)
					return null;
				index %= size;
			}
			if (entries.get(index).instruction instanceof DestinationInstruction destination)
				return destination;
		}
		return null;
	}

	@Override
	public Pair<ItemStack, Component> getSummary() {
		return Pair.of(AllBlocks.TRACK_STATION.asStack(),
			BetterSchedules.translate("schedule.condition.next_platform_free.short"));
	}

	@Override
	public List<Component> getTitleAs(String type) {
		return ImmutableList.of(
			Component.translatable(getId().getNamespace() + ".schedule." + type + ".next_platform_free"),
			BetterSchedules.translate("occupancy." + getOccupancy().name().toLowerCase())
				.withStyle(ChatFormatting.DARK_AQUA));
	}

	@Override
	public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag context) {
		String target = context.getString(TARGET);
		if (target.isBlank())
			return Component.empty();
		return BetterSchedules.translate("schedule.condition.next_platform_free.status", target);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addSelectionScrollInput(0, 121, (i, l) -> {
			i.forOptions(Occupancy.translatedOptions())
				.titled(BetterSchedules.translate("schedule.condition.next_platform_free.occupancy"));
		}, "Occupancy");
	}
}
