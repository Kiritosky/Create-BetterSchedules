package net.jxstanix.betterschedules.train;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Cargo lookups that span both the item and the fluid side of a train's mounted storage. */
public final class TrainCargo {

	private TrainCargo() {}

	/**
	 * Whether the train carries anything matching the filter. An empty filter matches any non-empty
	 * slot or tank, which is what makes "is this train loaded at all" expressible.
	 */
	public static boolean isCarrying(Level level, Train train, FilterItemStack filter) {
		for (Carriage carriage : train.carriages) {
			if (carriage.storage == null)
				continue;

			IItemHandlerModifiable items = carriage.storage.getAllItems();
			for (int i = 0; i < items.getSlots(); i++) {
				ItemStack inSlot = items.getStackInSlot(i);
				if (!inSlot.isEmpty() && filter.test(level, inSlot))
					return true;
			}

			IFluidHandler fluids = carriage.storage.getFluids();
			for (int i = 0; i < fluids.getTanks(); i++) {
				FluidStack inTank = fluids.getFluidInTank(i);
				if (!inTank.isEmpty() && filter.test(level, inTank))
					return true;
			}
		}
		return false;
	}
}
