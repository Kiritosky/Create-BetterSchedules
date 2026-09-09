package net.jxstanix.betterschedules;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.jxstanix.betterschedules.schedule.ScheduleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(BetterSchedules.ID)
public class BetterSchedules {

	public static final String ID = "betterschedules";
	public static final Logger LOGGER = LogUtils.getLogger();

	public BetterSchedules(IEventBus modEventBus, ModContainer container) {
		ScheduleTypes.register();
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	public static MutableComponent translate(String key, Object... args) {
		return Component.translatable(ID + "." + key, args);
	}
}
