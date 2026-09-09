package net.jxstanix.betterschedules.schedule;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.condition.CargoThresholdCondition;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import net.createmod.catnip.data.Pair;
import net.jxstanix.betterschedules.BetterSchedules;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A cargo threshold that can be narrowed to a single carriage instead of summing the whole train.
 * Needed to describe "this wagon is full" while the rest of the train is still empty.
 */
public abstract class CarriageThresholdCondition extends CargoThresholdCondition {

	/** How many wagons the dropdown offers. Trains longer than this can still use "any wagon". */
	public static final int MAX_LISTED_CARRIAGES = 12;

	protected FilterItemStack filter = FilterItemStack.empty();

	public CarriageThresholdCondition() {
		data.putInt("Carriage", 0);
	}

	/** 0 means "anywhere on the train", otherwise a 1-based carriage index. */
	public int getCarriage() {
		return intData("Carriage");
	}

	protected List<Carriage> selectedCarriages(Train train) {
		int index = getCarriage();
		if (index <= 0)
			return train.carriages;
		if (index > train.carriages.size())
			return List.of();
		return List.of(train.carriages.get(index - 1));
	}

	protected Component carriageLabel() {
		int index = getCarriage();
		return index <= 0 ? BetterSchedules.translate("carriage.any")
			: BetterSchedules.translate("carriage.numbered", index);
	}

	protected static List<Component> carriageOptions() {
		List<Component> options = new ArrayList<>();
		options.add(BetterSchedules.translate("carriage.any"));
		for (int i = 1; i <= MAX_LISTED_CARRIAGES; i++)
			options.add(BetterSchedules.translate("carriage.numbered", i));
		return options;
	}

	@Override
	protected ItemStack getIcon() {
		return filter.item();
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
		Pair<ItemStack, Component> summary = super.getSummary();
		int index = getCarriage();
		if (index <= 0)
			return summary;
		return Pair.of(summary.getFirst(), summary.getSecond().copy().append(Component.literal(" #" + index)));
	}

	@Override
	public List<Component> getTitleAs(String type) {
		return ImmutableList.of(
			Component.translatable(getId().getNamespace() + ".schedule." + type + "." + getId().getPath()),
			BetterSchedules.translate("schedule.condition.carriage_threshold.detail", carriageLabel(),
				Component.literal(getOperator().formatted + " " + getThreshold()).append(unitLabel())));
	}

	protected abstract Component unitLabel();

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		super.initConfigurationWidgets(builder);
		builder.addSelectionScrollInput(71, 50, (i, l) -> {
			i.forOptions(carriageOptions())
				.titled(BetterSchedules.translate("schedule.condition.carriage_threshold.carriage"));
		}, "Carriage");
	}
}
