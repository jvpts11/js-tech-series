/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.tests.JsTests;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Each line of the Core's access transformer still reaches what it was written for: a test that fails when the game
 * or NeoForge moves it, as every reach of the Core has.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class AccessTransformerGameTests {

    private static final String ARENA = "empty";

    private AccessTransformerGameTests() {
    }

    /*
     * The test mod compiles against the game as it ships, where a slot's place is final; only the Core is compiled
     * with its access transformer. So the place is written here as the running game has it, through its field.
     */
    @GameTest(template = ARENA)
    public static void slotPlace_canBeMovedWhereALayoutPutsIt(final GameTestHelper helper) {
        final Slot slot = new Slot(new SimpleContainer(1), 0, 3, 4);
        for (final String name : new String[] {"x", "y"}) {
            final Field field = field(helper, name);
            helper.assertTrue(Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers()),
                    "Slot." + name + " is public and not final");
            try {
                field.setInt(slot, 41);
            } catch (final IllegalAccessException refused) {
                helper.fail("Slot." + name + " cannot be written: " + refused.getMessage());
            }
        }
        helper.assertTrue(slot.x == 41 && slot.y == 41, "a slot moves to where its screen's layout put it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void smallBlocks_reachTheGamesOwnRules(final GameTestHelper helper) {
        for (final Class<?> item : new Class<?>[] {BlockItem.class, StandingAndWallBlockItem.class,
                GameMasterBlockItem.class}) {
            open(helper, item, "getPlacementState", BlockPlaceContext.class);
        }
        for (final Class<?> plate : new Class<?>[] {BasePressurePlateBlock.class, PressurePlateBlock.class,
                WeightedPressurePlateBlock.class}) {
            open(helper, plate, "getSignalStrength", Level.class, BlockPos.class);
        }
        open(helper, BasePressurePlateBlock.class, "getPressedTime");
        open(helper, WeightedPressurePlateBlock.class, "getPressedTime");
        for (final String name : new String[] {"type", "ticksToStayPressed"}) {
            openField(helper, ButtonBlock.class, name);
        }
        openField(helper, BasePressurePlateBlock.class, "type");
        helper.succeed();
    }

    /* The method {@code name} of {@code owner}, declared there, is public. */
    private static void open(final GameTestHelper helper, final Class<?> owner, final String name,
                             final Class<?>... parameters) {
        try {
            final Method method = owner.getDeclaredMethod(name, parameters);
            helper.assertTrue(Modifier.isPublic(method.getModifiers()), owner.getSimpleName() + "." + name
                    + " is public");
        } catch (final NoSuchMethodException moved) {
            helper.fail("the game no longer has " + owner.getSimpleName() + "." + name
                    + ": the access transformer reaches nothing");
        }
    }

    /* The field {@code name} of {@code owner} is public. */
    private static void openField(final GameTestHelper helper, final Class<?> owner, final String name) {
        try {
            helper.assertTrue(Modifier.isPublic(owner.getDeclaredField(name).getModifiers()),
                    owner.getSimpleName() + "." + name + " is public");
        } catch (final NoSuchFieldException moved) {
            helper.fail("the game no longer has " + owner.getSimpleName() + "." + name
                    + ": the access transformer reaches nothing");
        }
    }

    private static Field field(final GameTestHelper helper, final String name) {
        try {
            return Slot.class.getField(name);
        } catch (final NoSuchFieldException moved) {
            helper.fail("the game no longer has Slot." + name + ": the access transformer reaches nothing");
            throw new IllegalStateException(moved);
        }
    }
}
