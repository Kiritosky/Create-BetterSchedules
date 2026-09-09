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

public class CarriageFluidThresholdCondition extends CarriageThresholdCondition {

	@Override
	public ResourceLocation getId() {
		return BetterSchedules.asResource("carriage_fluid_threshold");
	}

	@Override
	protected Component getUnit() {
		return Component.literal("b");
	}

	@Override
	protected Component unitLabel() {
		return BetterSchedules.translate("unit.buckets_short");
	}

	@Override
	protected boolean test(Level level, Train train, CompoundTag context) {
		int found = 0;
		for (Carriage carriage : selectedCarriages(train)) {
			IFluidHandler fluids = carriage.storage.getFluids();
			for (int i = 0; i < fluids.getTanks(); i++) {
				FluidStack inTank = fluids.getFluidInTank(i);
				if (!filter.test(level, inTank))
					continue;
				found += inTank.getAmount();
			}
		}

		requestStatusToUpdate(found / 1000, context);
		return getOperator().test(found, getThreshold() * 1000);
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
