/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.api.SuitModule;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The lunar suit: its four pieces worn whole are a pressure suit with half an hour of air in its tanks, standing the
 * Moon's cold, and armour as leather is; modules fit it as any suit, the gold visor on the helmet alone, where it cuts
 * all the Sun's glare.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LunarSuitGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_lunar_suit";
    /* Where the player stands on the test Moon, high in its vacuum. */
    private static final Vec3 HIGH = new Vec3(0.5, 100, 0.5);
    /* The armour its four pieces give, as leather's do. */
    private static final int ARMOUR = 7;
    private static final double CLOSE = 1e-6;

    private LunarSuitGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void lunarSuit_wornWholeBreathesHalfAnHourAndStandsTheMoon(final GameTestHelper helper) {
        final ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(
                JsTests.MODID, "lunar_suit_" + UUID.randomUUID().toString().substring(0, 8)));
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key,
                TestWorldGen.MOON.id());
        final ServerPlayer player = GameTestPlayers.join(moon, UUID.randomUUID(), "lunar_walker");
        player.setPos(HIGH);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(LunarSuit.HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(LunarSuit.SUIT.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(LunarSuit.TROUSERS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(LunarSuit.BOOTS.get()));
        final SpaceSuit suit = SpaceSuits.worn(player).orElse(null);
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        final int capacity = suit == null ? -1 : LifeSupport.capacity(moon, chest, suit);
        final Set<DimensionHazardEvent.Hazard> hazards = DimensionEffects.weigh(player);
        int armour = 0;
        for (final EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET)) {
            armour += player.getItemBySlot(slot).getItem() instanceof ArmorItem piece ? piece.getDefense() : 0;
        }
        GameTestPlayers.leave(player);
        RuntimeDimensions.remove(helper.getLevel().getServer(), key);
        helper.assertTrue(suit != null && suit.name().equals(LunarSuit.NAME.key()),
                "the four pieces worn are the lunar suit; " + suit);
        helper.assertTrue(capacity == LunarSuit.AIR, "its tanks hold half an hour of air: " + capacity);
        helper.assertTrue(!hazards.contains(DimensionHazardEvent.Hazard.AIR)
                && !hazards.contains(DimensionHazardEvent.Hazard.COLD), "it breathes and stands the Moon's cold; "
                + hazards);
        helper.assertTrue(armour == ARMOUR, "its pieces are armour as leather's are: " + armour);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void goldVisor_fitsTheHelmetAloneAndCutsAllTheGlare(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final SpaceSuit suit = SpaceSuits.declared().get(LunarSuit.ID);
        final ItemStack helmet = new ItemStack(LunarSuit.HELMET.get());
        final ItemStack chest = new ItemStack(LunarSuit.SUIT.get());
        helper.assertFalse(LifeSupport.fit(level, chest, module("gold_visor")), "the gold visor does not fit the suit");
        helper.assertTrue(LifeSupport.fit(level, helmet, module("gold_visor")), "it fits the helmet");
        final List<SuitModule> fitted = LifeSupport.modules(level, helmet);
        helper.assertTrue(fitted.size() == 1 && fitted.getFirst().kind() == SuitModule.Kind.GLARE
                && Math.abs(fitted.getFirst().amount() - 1) < CLOSE, "and cuts all the glare; " + fitted);
        helper.assertTrue(LifeSupport.fit(level, chest, module("extra_oxygen")), "the extra oxygen fits the suit");
        helper.assertTrue(LifeSupport.capacity(level, chest, suit) == LunarSuit.AIR * 3 / 2,
                "and holds half as much air again: " + LifeSupport.capacity(level, chest, suit));
        helper.succeed();
    }

    private static ResourceLocation module(final String id) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, id);
    }
}
