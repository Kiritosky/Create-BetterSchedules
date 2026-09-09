package net.jxstanix.betterschedules.schedule;

import java.util.List;
import java.util.function.Supplier;

import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;

import net.createmod.catnip.data.Pair;
import net.jxstanix.betterschedules.BetterSchedules;
import net.minecraft.resources.ResourceLocation;

/**
 * Create resolves schedule entries by id out of {@link Schedule#INSTRUCTION_TYPES} and
 * {@link Schedule#CONDITION_TYPES}, so adding to those lists during mod construction is all an
 * add-on needs to do. Both sides register the same entries, which keeps the editor dropdown and the
 * server in sync.
 */
public class ScheduleTypes {

	private static boolean registered;

	public static void register() {
		if (registered)
			return;
		registered = true;

		registerInstruction("destination_if_carrying", ConditionalDestinationInstruction::new);
		registerInstruction("jump", JumpInstruction::new);

		registerCondition("serve_all_carriages", ServeAllCarriagesCondition::new);
		registerCondition("shunt", ShuntCondition::new);
		registerCondition("next_platform_free", PlatformFreeCondition::new);
		registerCondition("carriage_fluid_threshold", CarriageFluidThresholdCondition::new);
		registerCondition("carriage_item_threshold", CarriageItemThresholdCondition::new);
		registerCondition("carriage_fluid_percent", CarriageFluidPercentCondition::new);
		registerCondition("carriage_item_percent", CarriageItemPercentCondition::new);

		BetterSchedules.LOGGER.info("Registered {} schedule instructions and {} conditions", 2, 7);
	}

	private static void registerInstruction(String name, Supplier<? extends ScheduleInstruction> factory) {
		ResourceLocation id = BetterSchedules.asResource(name);
		if (!contains(Schedule.INSTRUCTION_TYPES, id))
			Schedule.INSTRUCTION_TYPES.add(Pair.of(id, factory));
	}

	private static void registerCondition(String name, Supplier<? extends ScheduleWaitCondition> factory) {
		ResourceLocation id = BetterSchedules.asResource(name);
		if (!contains(Schedule.CONDITION_TYPES, id))
			Schedule.CONDITION_TYPES.add(Pair.of(id, factory));
	}

	private static <T> boolean contains(List<Pair<ResourceLocation, T>> types, ResourceLocation id) {
		for (Pair<ResourceLocation, T> pair : types)
			if (pair.getFirst().equals(id))
				return true;
		return false;
	}
}
