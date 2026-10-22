/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.model.ExpressionContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The queries an entity's model reads ready-made, under the names Bedrock's Molang gives them, handed over each frame
 * before the model is animated: how fast it goes and how its limbs swing, where it is (on the ground, in water, in
 * lava, on fire, riding), how it turns its head and body, how hurt it is, and the time of day. A model's keyframes read
 * them as {@code query.ground_speed}; a mod's driver adds its own beside them.
 *
 * <table>
 *   <caption>The queries</caption>
 *   <tr><th>Query</th><th>What it reads</th></tr>
 *   <tr><td>{@code ground_speed}, {@code vertical_speed}</td><td>blocks a second, across and up</td></tr>
 *   <tr><td>{@code limb_swing} ({@code modified_distance_moved})</td><td>how far its limbs have swung</td></tr>
 *   <tr><td>{@code limb_swing_amount} ({@code modified_move_speed})</td><td>how hard they swing now, 0 to 1</td></tr>
 *   <tr><td>{@code is_on_ground}, {@code is_in_water}, {@code is_in_water_or_rain}, {@code is_in_lava},
 *       {@code is_on_fire}, {@code is_sneaking}, {@code is_sprinting}, {@code is_swimming}, {@code is_riding},
 *       {@code is_alive}, {@code is_baby}</td><td>1 or 0</td></tr>
 *   <tr><td>{@code head_x_rotation}, {@code head_y_rotation}</td><td>degrees the head looks up and turns from the
 *       body</td></tr>
 *   <tr><td>{@code body_y_rotation}, {@code yaw_speed}</td><td>degrees the body faces, and how fast it turns a
 *       second</td></tr>
 *   <tr><td>{@code hurt_time}, {@code health}, {@code max_health}</td><td>ticks of the last hurt left, and its
 *       health</td></tr>
 *   <tr><td>{@code distance_from_camera}, {@code time_of_day}, {@code moon_phase}</td><td>blocks to the viewer, the
 *       day from 0 to 1, the moon from 0 to 7</td></tr>
 * </table>
 */
public final class EntityQueries {

    /** Every query it hands over, for a page that lists them and a test that checks them. */
    public static final List<String> NAMES = List.of("ground_speed", "vertical_speed", "limb_swing",
            "modified_distance_moved", "limb_swing_amount", "modified_move_speed", "is_on_ground", "is_in_water",
            "is_in_water_or_rain", "is_in_lava", "is_on_fire", "is_sneaking", "is_sprinting", "is_swimming",
            "is_riding", "is_alive", "is_baby", "head_x_rotation", "head_y_rotation", "body_y_rotation", "yaw_speed",
            "hurt_time", "health", "max_health", "distance_from_camera", "time_of_day", "moon_phase");
    private static final double TICKS_A_SECOND = 20.0;
    private static final double DAY = 24_000.0;

    private EntityQueries() {
    }

    /** Hands {@code context} the queries of {@code entity} as it stands {@code partialTick} into the tick. */
    public static void hand(final ExpressionContext context, final Entity entity, final float partialTick) {
        final Vec3 motion = entity.getDeltaMovement();
        context.value("ground_speed", Math.sqrt(motion.x * motion.x + motion.z * motion.z) * TICKS_A_SECOND);
        context.value("vertical_speed", motion.y * TICKS_A_SECOND);
        context.value("is_on_ground", flag(entity.onGround()));
        context.value("is_in_water", flag(entity.isInWater()));
        context.value("is_in_water_or_rain", flag(entity.isInWaterOrRain()));
        context.value("is_in_lava", flag(entity.isInLava()));
        context.value("is_on_fire", flag(entity.isOnFire()));
        context.value("is_sneaking", flag(entity.isCrouching()));
        context.value("is_sprinting", flag(entity.isSprinting()));
        context.value("is_swimming", flag(entity.isSwimming()));
        context.value("is_riding", flag(entity.isPassenger()));
        context.value("is_alive", flag(entity.isAlive()));
        context.value("head_x_rotation", entity.getViewXRot(partialTick));
        context.value("yaw_speed", Mth.wrapDegrees(entity.getYRot() - entity.yRotO) * TICKS_A_SECOND);
        if (entity instanceof LivingEntity living) {
            final float body = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
            final float head = Mth.rotLerp(partialTick, living.yHeadRotO, living.yHeadRot);
            final double swing = living.walkAnimation.position(partialTick);
            final double amount = Math.min(1.0, living.walkAnimation.speed(partialTick));
            context.value("limb_swing", swing);
            context.value("modified_distance_moved", swing);
            context.value("limb_swing_amount", amount);
            context.value("modified_move_speed", amount);
            context.value("body_y_rotation", body);
            context.value("head_y_rotation", Mth.wrapDegrees(head - body));
            context.value("hurt_time", Math.max(0.0, living.hurtTime - partialTick));
            context.value("health", living.getHealth());
            context.value("max_health", living.getMaxHealth());
            context.value("is_baby", flag(living.isBaby()));
        } else {
            context.value("body_y_rotation", entity.getViewYRot(partialTick));
        }
        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        context.value("distance_from_camera", Math.sqrt(entity.distanceToSqr(camera)));
        context.value("time_of_day", Math.floorMod(entity.level().getDayTime(), (long) DAY) / DAY);
        context.value("moon_phase", entity.level().getMoonPhase());
    }

    private static double flag(final boolean value) {
        return value ? 1.0 : 0.0;
    }
}
