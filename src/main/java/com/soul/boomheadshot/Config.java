package com.soul.boomheadshot;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Configuration handler for the BoomHeadshot mod.
 * Manages all configurable parameters for the headshot system.
 */
@EventBusSubscriber(modid = BoomHeadshot.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Combat settings
    private static final ModConfigSpec.DoubleValue HEADSHOT_MULTIPLIER = BUILDER
            .comment("Multiplier for damage when hitting a mob in the head")
            .defineInRange("headshotMultiplier", 2.0, 1.1, 10.0);

    private static final ModConfigSpec.BooleanValue ENABLE_HEADSHOT_EFFECTS = BUILDER
            .comment("Enable potion effects on headshot")
            .define("enableHeadshotEffects", false);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HEADSHOT_EFFECTS = BUILDER
            .comment("List of effects to apply on headshot in format 'effect_id:duration_in_ticks'",
                    "Example: minecraft:blindness:60")
            .define("headshotEffects", List.of("minecraft:blindness:60"));

    // Detection settings
    private static final ModConfigSpec.DoubleValue RAY_TRACE_DISTANCE_CONFIG = BUILDER
            .comment("Maximum distance for ray tracing headshot detection")
            .defineInRange("rayTraceDistance", 32.0, 1.0, 64.0);

    private static final ModConfigSpec.DoubleValue ARROW_BACKTRACK_CONFIG = BUILDER
            .comment("Distance to backtrack arrow for hit detection")
            .defineInRange("arrowBacktrack", 0.1, 0.0, 1.0);

    // Hitbox settings
    private static final ModConfigSpec.DoubleValue MAX_HEAD_WIDTH_CONFIG = BUILDER
            .comment("Maximum width of entity head hitbox")
            .defineInRange("maxHeadWidth", 0.5, 0.1, 2.0);

    private static final ModConfigSpec.DoubleValue HEAD_HEIGHT_RATIO_CONFIG = BUILDER
            .comment("Ratio of entity height considered as head")
            .defineInRange("headHeightRatio", 0.5, 0.1, 1.0);

    private static final ModConfigSpec.DoubleValue HEAD_HEIGHT_BOTTOM_RATIO_CONFIG = BUILDER
            .comment("Bottom ratio of head height from total entity height")
            .defineInRange("headHeightBottomRatio", 1.0/3.0, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue HEAD_HEIGHT_TOP_RATIO_CONFIG = BUILDER
            .comment("Top ratio of head height from total entity height")
            .defineInRange("headHeightTopRatio", 2.0/3.0, 0.0, 1.0);

    // Particle settings
    private static final ModConfigSpec.IntValue PARTICLE_COUNT_CONFIG = BUILDER
            .comment("Number of particles spawned on headshot")
            .defineInRange("particleCount", 20, 0, 100);

    private static final ModConfigSpec.DoubleValue PARTICLE_SPREAD_CONFIG = BUILDER
            .comment("Spread radius of headshot particles")
            .defineInRange("particleSpread", 0.5, 0.0, 2.0);

    private static final ModConfigSpec.DoubleValue PARTICLE_SPEED_CONFIG = BUILDER
            .comment("Speed of headshot particles")
            .defineInRange("particleSpeed", 0.1, 0.0, 1.0);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HELMET_PROTECTIONS = BUILDER
            .comment("List of helmets and their headshot multiplier reduction in format 'item_id:reduction_value'",
                    "Examples:",
                    "minecraft:leather_helmet:0.2",
                    "minecraft:chainmail_helmet:0.4",
                    "minecraft:iron_helmet:0.6",
                    "minecraft:golden_helmet:0.3",
                    "minecraft:diamond_helmet:0.8",
                    "minecraft:netherite_helmet:1.0",
                    "minecraft:turtle_helmet:0.5")
            .define("helmetProtections", List.of(
                    "minecraft:leather_helmet:0.2",
                    "minecraft:chainmail_helmet:0.4",
                    "minecraft:iron_helmet:0.6",
                    "minecraft:golden_helmet:0.3",
                    "minecraft:diamond_helmet:0.8",
                    "minecraft:netherite_helmet:1.0",
                    "minecraft:turtle_helmet:0.5"
            ));


    // Debug settings
    private static final ModConfigSpec.BooleanValue DEBUG_MODE = BUILDER
            .comment("Enable debug logging for headshot detection")
            .define("debug", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Public accessible fields
    public static double headshotMultiplier;
    public static boolean debug;
    public static boolean enableHeadshotEffects;
    public static List<? extends String> headshotEffects;
    public static double rayTraceDistance;
    public static double arrowBacktrack;
    public static double maxHeadWidth;
    public static double headHeightRatio;
    public static int particleCount;
    public static double particleSpread;
    public static double particleSpeed;
    public static double headHeightBottomRatio;
    public static double headHeightTopRatio;
    public static List<? extends String> helmetProtections;


    private Config() {
        // Private constructor to prevent instantiation
    }

    /**
     * Handles the configuration loading event.
     * Updates all configuration values when the config is loaded or reloaded.
     *
     * @param event The mod config event
     */
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        try {
            loadConfig();
            LOGGER.info("Successfully loaded BoomHeadshot configuration");
        } catch (Exception e) {
            LOGGER.error("Failed to load BoomHeadshot configuration", e);
            loadDefaultValues();
        }
    }

    private static void loadConfig() {
        headshotMultiplier = HEADSHOT_MULTIPLIER.get();
        debug = DEBUG_MODE.get();
        enableHeadshotEffects = ENABLE_HEADSHOT_EFFECTS.get();
        headshotEffects = HEADSHOT_EFFECTS.get();
        rayTraceDistance = RAY_TRACE_DISTANCE_CONFIG.get();
        arrowBacktrack = ARROW_BACKTRACK_CONFIG.get();
        maxHeadWidth = MAX_HEAD_WIDTH_CONFIG.get();
        headHeightRatio = HEAD_HEIGHT_RATIO_CONFIG.get();
        particleCount = PARTICLE_COUNT_CONFIG.get();
        particleSpread = PARTICLE_SPREAD_CONFIG.get();
        particleSpeed = PARTICLE_SPEED_CONFIG.get();
        headHeightBottomRatio = HEAD_HEIGHT_BOTTOM_RATIO_CONFIG.get();
        headHeightTopRatio = HEAD_HEIGHT_TOP_RATIO_CONFIG.get();
        helmetProtections = HELMET_PROTECTIONS.get();

    }

    private static void loadDefaultValues() {
        headshotMultiplier = 2.0;
        debug = false;
        enableHeadshotEffects = false;
        headshotEffects = List.of("minecraft:blindness:60");
        rayTraceDistance = 32.0;
        arrowBacktrack = 0.1;
        maxHeadWidth = 0.5;
        headHeightRatio = 0.5;
        particleCount = 20;
        particleSpread = 0.5;
        particleSpeed = 0.1;
        headHeightBottomRatio = 1.0/3.0;
        headHeightTopRatio = 2.0/3.0;
        helmetProtections = List.of(
                "minecraft:leather_helmet:0.2",
                "minecraft:chainmail_helmet:0.4",
                "minecraft:iron_helmet:0.6",
                "minecraft:golden_helmet:0.3",
                "minecraft:diamond_helmet:0.8",
                "minecraft:netherite_helmet:1.0",
                "minecraft:turtle_helmet:0.5"
        );


        LOGGER.warn("Loaded default configuration values due to loading error");
    }
}