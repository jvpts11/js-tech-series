/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Seeing what a block is of in the physics of a moving structure: a player who holds an item of {@link #SEES_PHYSICS}
 * (the physics tool), or wears one on the head, or holds the key of the diagram of forces, reads in a block's tooltip
 * its mass and the forces it gives. A mod puts its own tool or goggles in the tag.
 */
@TextHolder
public final class PhysicsSight {

    /** The items that let a player see a block's physics: held in a hand, or worn on the head. */
    public static final TagKey<Item> SEES_PHYSICS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "sees_physics"));

    public static final TextKey HEAD = TextKey.of("jscore.moving.physics.head", "In a moving structure:");
    public static final TextKey MASS = TextKey.of("jscore.moving.physics.mass", "Mass: %s");
    public static final TextKey THRUST = TextKey.of("jscore.moving.physics.thrust", "Thrust: %s");
    public static final TextKey SHAFT = TextKey.of("jscore.moving.physics.shaft", "Turned by a shaft, full at %s rpm");
    public static final TextKey WING = TextKey.of("jscore.moving.physics.wing", "Wing: %s m², stalls at %s°");
    public static final TextKey DISPLACES = TextKey.of("jscore.moving.physics.displaces", "Displaces %s m³");
    public static final TextKey LEVITATION = TextKey.of("jscore.moving.physics.levitation", "Holds up %s");
    public static final TextKey MAGNET = TextKey.of("jscore.moving.physics.magnet", "Magnet: %s within %s blocks");
    public static final TextKey WHEEL = TextKey.of("jscore.moving.physics.wheel",
            "Wheel: %s m across, drives with %s, brakes with %s");
    public static final TextKey BURNER = TextKey.of("jscore.moving.physics.burner", "Burner: %s");
    public static final TextKey DRAG = TextKey.of("jscore.moving.physics.drag", "Drags %s of a flat face");
    public static final TextKey WATTS = TextKey.of("jscore.moving.physics.watts", "%s %sW");
    public static final TextKey HINT = TextKey.of("jscore.moving.physics.hint", "Hold %s to see its physics");

    private PhysicsSight() {
    }

    /** Whether {@code player} sees a block's physics by what they hold or wear: an item of the tag. */
    public static boolean sees(final Player player) {
        return player.getMainHandItem().is(SEES_PHYSICS) || player.getOffhandItem().is(SEES_PHYSICS)
                || player.getItemBySlot(EquipmentSlot.HEAD).is(SEES_PHYSICS);
    }
}
