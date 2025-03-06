package com.soul.boomheadshot;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * System responsible for detecting and processing headshots in the game.
 * Handles hit detection, damage calculation, and special effects for headshots.
 */
@EventBusSubscriber(modid = BoomHeadshot.MODID)
public class HeadshotSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadshotSystem.class);

    /**
     * Represents the context of a headshot hit.
     */
    private record HeadHitContext(Vec3 attackerPos, Vec3 attackDirection, Vec3 hitPosition) {
        public HeadHitContext {
            Objects.requireNonNull(attackerPos, "Attacker position cannot be null");
            Objects.requireNonNull(attackDirection, "Attack direction cannot be null");
            Objects.requireNonNull(hitPosition, "Hit position cannot be null");
        }
    }

    /**
     * Handles the living damage event to process potential headshots.
     * @param event The damage event to process
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event == null) return;

        LivingEntity target = event.getEntity();
        Entity attacker = event.getSource().getDirectEntity();

        if (attacker == null) return;

        float originalDamage = event.getNewDamage();

        calculateHeadshot(target, attacker)
                .ifPresent(hitContext ->
                        applyHeadshotEffects(event, target, hitContext, originalDamage));
    }

    /**
     * Calculates whether a hit qualifies as a headshot.
     * @param target The entity being hit
     * @param attacker The attacking entity
     * @return Optional containing headshot context if successful
     */
    private static Optional < HeadHitContext > calculateHeadshot(LivingEntity target, Entity attacker) {
        if (target == null || attacker == null) {
            LOGGER.debug("Null target or attacker in headshot calculation");
            return Optional.empty();
        }

        final Vec3 attackerPos;
        final Vec3 attackDirection;

        if (attacker instanceof AbstractArrow arrow) {
            Vec3 pos = arrow.position();
            Vec3 direction = arrow.getDeltaMovement().normalize();
            attackerPos = pos.subtract(direction.scale(Config.arrowBacktrack));
            attackDirection = direction;
        } else if (attacker instanceof LivingEntity livingAttacker) {
            attackerPos = livingAttacker.getEyePosition();
            attackDirection = livingAttacker.getLookAngle();
        } else {
            attackerPos = attacker.position();
            attackDirection = target.position().subtract(attackerPos).normalize();
        }

        AABB headBox = createHeadHitbox(target);
        Vec3 rayEnd = attackerPos.add(attackDirection.scale(Config.rayTraceDistance));
        Optional < Vec3 > hitResult = headBox.clip(attackerPos, rayEnd);

        if (Config.debug) {
            DebugLogger.printDetectionInfo(target, attacker, headBox, attackerPos, attackDirection, hitResult);
        }

        return hitResult.map(hitPos -> new HeadHitContext(attackerPos, attackDirection, hitPos));
    }

    /**
     * Creates a hitbox for the entity's head.
     * @param target The entity to create the head hitbox for
     * @return AABB representing the head hitbox
     */
    private static AABB createHeadHitbox(LivingEntity target) {
        Objects.requireNonNull(target, "Target cannot be null");

        Vec3 targetPos = target.position();
        double eyeHeight = target.getEyeHeight();
        double headWidth = Math.min(target.getBbWidth(), Config.maxHeadWidth);

        return new AABB(
                targetPos.x - headWidth / 2,
                targetPos.y + eyeHeight - headWidth * Config.headHeightBottomRatio,
                targetPos.z - headWidth / 2,
                targetPos.x + headWidth / 2,
                targetPos.y + eyeHeight + Config.headHeightRatio * Config.headHeightTopRatio,
                targetPos.z + headWidth / 2
        );
    }

    /**
     * Spawns particles for the headshot effect.
     * @param level The game level
     * @param hitContext Context containing hit information
     */
    private static void spawnHeadshotParticles(Level level, HeadHitContext hitContext) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 baseMotion = hitContext.attackDirection().scale(-Config.particleSpeed);
        Vec3 hitPos = hitContext.hitPosition();

        IntStream.range(0, Config.particleCount).forEach(i -> {
            Vec3 spread = new Vec3(
                    (level.random.nextDouble() - 0.5) * Config.particleSpread,
                    (level.random.nextDouble() - 0.5) * Config.particleSpread,
                    (level.random.nextDouble() - 0.5) * Config.particleSpread
            );

            Vec3 particlePos = hitPos.add(spread);
            Vec3 particleMotion = baseMotion.add(spread);

            serverLevel.sendParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    particlePos.x, particlePos.y, particlePos.z,
                    1,
                    particleMotion.x, particleMotion.y, particleMotion.z,
                    0.1
            );
        });
    }

    /**
     * Gets the protection value for a specific helmet item.
     * The protection value reduces the headshot multiplier, with 1.0 being complete protection
     * and 0.0 being no protection.
     *
     * @param itemId The registry name of the helmet item
     * @return The protection value between 0.0 and 1.0
     */
    public static double getHelmetProtection(String itemId) {
        return Config.helmetProtections.stream()
                .filter(entry -> entry.startsWith(itemId + ":"))
                .findFirst()
                .map(entry -> {
                    String[] parts = entry.split(":");
                    try {
                        return Double.parseDouble(parts[1]);
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        LOGGER.error("Invalid helmet protection value for {}", itemId);
                        return 0.0;
                    }
                })
                .orElse(0.0);
    }

    /**
     * Applies the effects of a headshot, including:
     * - Damage modification based on helmet protection
     * - Status effects if enabled
     * - Particle effects
     * - Debug logging if enabled
     *
     * @param event The damage event being processed
     * @param target The entity being hit
     * @param hitContext Context containing information about the headshot
     * @param originalDamage The original damage amount before modification
     */
    private static void applyHeadshotEffects(LivingDamageEvent.Pre event, LivingEntity target,
                                             HeadHitContext hitContext, float originalDamage) {
        // Calculate helmet protection
        double helmetProtection = !target.getItemBySlot(EquipmentSlot.HEAD).isEmpty() ?
                getHelmetProtection(BuiltInRegistries.ITEM.getKey(
                        target.getItemBySlot(EquipmentSlot.HEAD).getItem()).toString()) :
                0.0;

        // Apply damage multiplier
        float headshotMultiplier = (float)(Config.headshotMultiplier * (1.0 - helmetProtection));
        float newDamage = originalDamage * headshotMultiplier;
        event.setNewDamage(newDamage);

        // Apply additional effects
        if (Config.enableHeadshotEffects && target.level() instanceof ServerLevel) {
            applyHeadshotStatusEffects(target);
        }

        spawnHeadshotParticles(target.level(), hitContext);

        // Debug logging
        if (Config.debug) {
            DebugLogger.logHeadshotConfirmation(hitContext, originalDamage, newDamage);
            LOGGER.debug("Headshot applied - Original damage: {}, Helmet protection: {}, " +
                            "Final multiplier: {}, New damage: {}",
                    originalDamage, helmetProtection, headshotMultiplier, newDamage);
        }
    }

    /**
     * Applies configured status effects for headshots.
     */
    private static void applyHeadshotStatusEffects(LivingEntity target) {
        for (String effectEntry: Config.headshotEffects) {
            String[] parts = effectEntry.split(":");
            if (parts.length >= 2) {
                try {
                    ResourceLocation effectId = ResourceLocation.tryParse(parts[0] + ":" + parts[1]);
                    if (effectId == null) continue;

                    int duration = Integer.parseInt(parts[2]);

                    var effectHolder = BuiltInRegistries.MOB_EFFECT.get(effectId);
                    effectHolder.ifPresent(holder -> target.addEffect(new MobEffectInstance(holder, duration, 0)));
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    LOGGER.warn("Invalid effect entry in config: {}", effectEntry);
                }
            }
        }
    }

    private static class DebugLogger {
        static void logHeadshotConfirmation(HeadHitContext hitContext, float originalDamage, float newDamage) {
            if (!Config.debug) return;

            LOGGER.debug("HEADSHOT CONFIRMED!");
            LOGGER.debug("Hit Position: {}", hitContext.hitPosition());
            LOGGER.debug("Original Damage: {}", originalDamage);
            LOGGER.debug("New Damage: {}", newDamage);
        }

        static void printDetectionInfo(LivingEntity target, Entity attacker, AABB headBox,
                                       Vec3 attackerPos, Vec3 attackDirection, Optional < Vec3 > hitResult) {
            if (!Config.debug) return;

            LOGGER.debug("=== Headshot Detection Debug ===");
            LOGGER.debug("Target: {}", target.getName().getString());
            LOGGER.debug("Attacker: {}", attacker.getName().getString());
            LOGGER.debug("Head Hitbox - Min: [{}, {}, {}], Max: [{}, {}, {}]",
                    headBox.minX, headBox.minY, headBox.minZ,
                    headBox.maxX, headBox.maxY, headBox.maxZ);
            LOGGER.debug("Attack Vector - From: {}, Direction: {}", attackerPos, attackDirection);
            LOGGER.debug("Hit Result: {}", hitResult.map(Vec3::toString).orElse("No hit"));
        }
    }
}