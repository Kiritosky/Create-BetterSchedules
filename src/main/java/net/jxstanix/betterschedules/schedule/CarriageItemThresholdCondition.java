package net.jxstanix.betterschedules.schedule;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

import net.jxstanix.betterschedules.BetterSchedules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class CarriageItemThresholdCondition extends CarriageThresholdCondition {

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("carriage_item_threshold");
	}

	@Override
	protected Component getUnit() {
		return Component.empty();
	}

	@Override
	protected Component unitLabel() {
		return BetterSchedules.translate("unit.items");
	}

	@Override
	protected boolean test(Level level, Train train, CompoundTag context) {
		int found = 0;
		for (Carriage carriage : selectedCarriages(train)) {
			IItemHandlerModifiable items = carriage.storage.getAllItems();
			for (int i = 0; i < items.getSlots(); i++) {
				ItemStack inSlot = items.getStackInSlot(i);
				if (!filter.test(level, inSlot))
					continue;
				found += inSlot.getCount();
			}
		}

		requestStatusToUpdate(found, context);
		return getOperator().test(found, getThreshold());
	}

	@Override
	public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag context) {
		int snapshot = getLastDisplaySnapshot(context);
		if (snapshot == -1)
			return Component.empty();
		return BetterSchedules.translate("schedule.condition.carriage_threshold.status", carriageLabel(), snapshot,
			unitLabel());
	}
}
