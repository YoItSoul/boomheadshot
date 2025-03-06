package com.soul.boomheadshot;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(BoomHeadshot.MODID)
public class BoomHeadshot {
    public static final String MODID = "boomheadshot";

    public BoomHeadshot(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.register(Config.class);
    }
}