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

/**
 * How full the vaults are as a share of the slots actually installed. Measured against each slot's
 * own limit, so a chest of unstackables still reads as full when every slot holds one.
 */
public class CarriageItemPercentCondition extends CarriageThresholdCondition {

	public CarriageItemPercentCondition() {
		data.putString("Threshold", "80");
	}

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("carriage_item_percent");
	}

	@Override
	protected Component getUnit() {
		return Component.literal("%");
	}

	@Override
	protected Component unitLabel() {
		return Component.literal("%");
	}

	@Override
	protected boolean test(Level level, Train train, CompoundTag context) {
		long contents = 0;
		long capacity = 0;

		for (Carriage carriage : selectedCarriages(train)) {
			if (carriage.storage == null)
				continue;
			IItemHandlerModifiable items = carriage.storage.getAllItems();
			for (int i = 0; i < items.getSlots(); i++) {
				ItemStack inSlot = items.getStackInSlot(i);
				capacity += items.getSlotLimit(i);
				if (!inSlot.isEmpty() && filter.test(level, inSlot))
					contents += inSlot.getCount();
			}
		}

		if (capacity == 0)
			return true;

		int percent = (int) (contents * 100 / capacity);
		requestStatusToUpdate(percent, context);
		return getOperator().test(percent, getThreshold());
	}

	@Override
	public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag context) {
		int snapshot = getLastDisplaySnapshot(context);
		if (snapshot == -1)
			return Component.empty();
		return BetterSchedules.translate("schedule.condition.carriage_percent.status", carriageLabel(), snapshot);
	}
}
