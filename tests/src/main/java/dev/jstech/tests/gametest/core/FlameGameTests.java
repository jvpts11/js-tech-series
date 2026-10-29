/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.tests.JsTests;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The flames that burn the air's oxygen besides fire, as J's Core keeps them: the game's furnace, standing in a sealed
 * room, lights its fuel only while the room's air holds oxygen enough for a fire, and goes out, spending no more fuel,
 * the second the room runs short. Rooms share the world's state, so they have a batch of their own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FlameGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_flames";
    /* The hut: its shell from LOW to HIGH, a furnace on its floor, and a block of the air round it. */
    private static final BlockPos LOW = new BlockPos(2, 1, 2);
    private static final BlockPos HIGH = new BlockPos(5, 4, 5);
    private static final BlockPos FURNACE = new BlockPos(3, 2, 3);
    private static final BlockPos INSIDE = new BlockPos(4, 3, 4);
    private static final double ATMOSPHERE = 101.3;
    private static final Map<ResourceLocation, Double> AIR = Map.of(AirMix.NITROGEN, 0.79, AirMix.OXYGEN, 0.21);
    private static final Map<ResourceLocation, Double> NITROGEN = Map.of(AirMix.NITROGEN, 1.0);
    /* The coal and the ore the furnace is given. */
    private static final int LOAD = 8;
    private static final int FUEL_SLOT = 1;
    private static final int INPUT_SLOT = 0;

    private FlameGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void furnace_doesNotLightWithoutOxygen(final GameTestHelper helper) {
        hut(helper);
        SealedRooms.track(helper.getLevel(), helper.absolutePos(INSIDE));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper).isPresent(), "the hut is a room"))
                .thenExecute(() -> {
                    fill(room(helper).orElseThrow(), NITROGEN);
                    load(helper);
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertFalse(lit(helper), "in air of nitrogen alone the furnace does not light");
                    helper.assertTrue(coal(helper) == LOAD, "and spends no fuel; " + coal(helper));
                    fill(room(helper).orElseThrow(), AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(lit(helper), "given air, it lights"))
                .thenExecute(() -> SealedRooms.untrack(helper.getLevel(), helper.absolutePos(INSIDE)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void furnace_goesOutWhenItsRoomRunsShort(final GameTestHelper helper) {
        hut(helper);
        SealedRooms.track(helper.getLevel(), helper.absolutePos(INSIDE));
        final int[] left = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper).isPresent(), "the hut is a room"))
                .thenExecute(() -> load(helper))
                .thenWaitUntil(() -> helper.assertTrue(lit(helper), "in the overworld's air the furnace lights"))
                .thenExecute(() -> fill(room(helper).orElseThrow(), NITROGEN))
                .thenWaitUntil(() -> helper.assertFalse(lit(helper), "its room's oxygen gone, its flame goes out"))
                .thenExecute(() -> left[0] = coal(helper))
                .thenExecuteAfter(60, () -> {
                    SealedRooms.untrack(helper.getLevel(), helper.absolutePos(INSIDE));
                    helper.assertFalse(lit(helper), "and it does not light again");
                    helper.assertTrue(coal(helper) == left[0], "nor spends more fuel; " + coal(helper));
                })
                .thenSucceed();
    }

    /* A closed box of stone from LOW to HIGH, its inside air, and a furnace on its floor. */
    private static void hut(final GameTestHelper helper) {
        for (int x = LOW.getX(); x <= HIGH.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH.getZ(); z++) {
                    final boolean shell = x == LOW.getX() || x == HIGH.getX() || y == LOW.getY() || y == HIGH.getY()
                            || z == LOW.getZ() || z == HIGH.getZ();
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        helper.setBlock(FURNACE, Blocks.FURNACE);
    }

    /* Empties {@code room} and fills it with air of {@code shares} at the overworld's pressure. */
    private static void fill(final Room room, final Map<ResourceLocation, Double> shares) {
        room.empty();
        for (final Map.Entry<ResourceLocation, Double> gas : shares.entrySet()) {
            room.add(gas.getKey(), GasMath.moles(ATMOSPHERE * gas.getValue(), room.gasVolume(), GasMath.TEMPERED));
        }
    }

    /* Gives the furnace coal to burn and raw iron to smelt. */
    private static void load(final GameTestHelper helper) {
        final AbstractFurnaceBlockEntity furnace = furnace(helper);
        furnace.setItem(INPUT_SLOT, new ItemStack(Items.RAW_IRON, LOAD));
        furnace.setItem(FUEL_SLOT, new ItemStack(Items.COAL, LOAD));
    }

    private static boolean lit(final GameTestHelper helper) {
        return helper.getBlockState(FURNACE).getValue(AbstractFurnaceBlock.LIT);
    }

    private static int coal(final GameTestHelper helper) {
        return furnace(helper).getItem(FUEL_SLOT).getCount();
    }

    private static AbstractFurnaceBlockEntity furnace(final GameTestHelper helper) {
        return (AbstractFurnaceBlockEntity) helper.getBlockEntity(FURNACE);
    }

    private static Optional<Room> room(final GameTestHelper helper) {
        return SealedRooms.roomAt(helper.getLevel(), helper.absolutePos(INSIDE));
    }
}
