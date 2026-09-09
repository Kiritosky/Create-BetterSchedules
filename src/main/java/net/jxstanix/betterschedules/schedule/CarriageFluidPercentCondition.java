package net.jxstanix.betterschedules.schedule;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

import net.jxstanix.betterschedules.BetterSchedules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Fill level as a share of the tanks actually installed, so the same schedule works whether the
 * train has two tanks or eight.
 */
public class CarriageFluidPercentCondition extends CarriageThresholdCondition {

	public CarriageFluidPercentCondition() {
		data.putString("Threshold", "80");
	}

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("carriage_fluid_percent");
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
			IFluidHandler fluids = carriage.storage.getFluids();
			for (int i = 0; i < fluids.getTanks(); i++) {
				capacity += fluids.getTankCapacity(i);
				FluidStack inTank = fluids.getFluidInTank(i);
				if (!inTank.isEmpty() && filter.test(level, inTank))
					contents += inTank.getAmount();
			}
		}

		// A train with no tanks can never reach a fill percentage; treat it as satisfied rather than
		// letting a misconfigured wagon number strand the train forever.
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
