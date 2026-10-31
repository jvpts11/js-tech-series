/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.life.SuitAir;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The small ship's pressurised cabin: a hull of its kit is a sealed room, and stays one when its blocks merge into
 * formed parts; with no life support section, its flight console's small air reserve tops the cabin back up to the
 * sea's air as the crew breathe and the hull leaks, paying for it out of the reserve; and a suited crew member inside
 * breathes the cabin's air, the helmet off, the suit's tanks spared.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipCabinAirGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos LOW = new BlockPos(2, 1, 2);
    private static final BlockPos HIGH_CORNER = new BlockPos(6, 4, 6);
    private static final BlockPos INSIDE = new BlockPos(4, 2, 4);
    /* The flight console, furniture standing in the cabin against its west wall, the cabin's air round it. */
    private static final BlockPos CONSOLE = new BlockPos(3, 2, 4);
    private static final double SEA_OXYGEN = 21.2;
    /* What the crew breathe and the hull leaks before the reserve makes it good, in moles. */
    private static final double BREATHED = 20;
    private static final double LEAKED = 10;
    private static final int SUIT_AIR = 600;

    private ShipCabinAirGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void cabin_isSealedToppedUpFromItsReserveAndBreathedWithTheHelmetOff(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        hull(helper);
        helper.setBlock(CONSOLE, ShipKitBlocks.FLIGHT_CONSOLE.get());
        SealedRooms.track(level, helper.absolutePos(INSIDE));
        final double[] before = new double[2];
        final ServerPlayer[] crew = new ServerPlayer[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper) != null, "the ship's hull is a sealed cabin"))
                .thenExecute(() -> {
                    final AirReserveEntity reserve = (AirReserveEntity) helper.getBlockEntity(CONSOLE);
                    before[0] = reserve.oxygen();
                    before[1] = reserve.nitrogen();
                    // As the crew breathe and the hull leaks: some oxygen and some nitrogen gone.
                    room(helper).take(AirMix.OXYGEN, BREATHED);
                    room(helper).take(AirMix.NITROGEN, LEAKED);
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    final Room cabin = room(helper);
                    final AirReserveEntity reserve = (AirReserveEntity) helper.getBlockEntity(CONSOLE);
                    final double sea = GasMath.moles(SEA_OXYGEN, cabin.gasVolume(), GasMath.TEMPERED);
                    helper.assertTrue(Math.abs(cabin.moles(AirMix.OXYGEN) - sea) < 0.5,
                            "the cabin is topped back up to the sea's oxygen: " + cabin.moles(AirMix.OXYGEN)
                                    + " of " + sea);
                    helper.assertTrue(before[0] - reserve.oxygen() > BREATHED - 1
                                    && before[1] - reserve.nitrogen() > LEAKED - 1,
                            "out of the reserve: " + reserve.oxygen() + " mol of oxygen and " + reserve.nitrogen()
                                    + " of nitrogen left");
                    // The hull's blocks merge into formed parts, and still hold the cabin's air.
                    for (final BlockPos wall : walls()) {
                        FormedParts.lay(level, helper.absolutePos(CONSOLE), helper.absolutePos(wall), null,
                                List.of());
                    }
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(helper.absolutePos(HIGH_CORNER))
                            .getBlock() instanceof FormedPartBlock, "the hull's blocks are merged");
                    helper.assertTrue(room(helper) != null, "the formed hull is still a sealed cabin");
                    crew[0] = GameTestPlayers.join(level, UUID.randomUUID(), "crew");
                    crew[0].setPos(Vec3.atBottomCenterOf(helper.absolutePos(INSIDE)));
                    suit(crew[0]);
                    SuitAir.second(crew[0]);
                    final SpaceSuit suit = SpaceSuits.declared().get(LunarSuit.ID);
                    final int air = LifeSupport.air(level, crew[0].getItemBySlot(EquipmentSlot.CHEST), suit);
                    GameTestPlayers.leave(crew[0]);
                    SealedRooms.untrack(level, helper.absolutePos(INSIDE));
                    helper.assertTrue(air == SUIT_AIR, "inside, the crew breathe the cabin's air with the helmet "
                            + "off, the suit's tanks spared: " + air);
                })
                .thenSucceed();
    }

    /* A closed box of the ship's hull plating from LOW to HIGH_CORNER, its inside air. */
    private static void hull(final GameTestHelper helper) {
        for (int x = LOW.getX(); x <= HIGH_CORNER.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH_CORNER.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH_CORNER.getZ(); z++) {
                    final BlockPos at = new BlockPos(x, y, z);
                    helper.setBlock(at, shell(at) ? ShipKitBlocks.HULL_PLATING.get() : Blocks.AIR);
                }
            }
        }
    }

    private static boolean shell(final BlockPos at) {
        return at.getX() == LOW.getX() || at.getX() == HIGH_CORNER.getX() || at.getY() == LOW.getY()
                || at.getY() == HIGH_CORNER.getY() || at.getZ() == LOW.getZ() || at.getZ() == HIGH_CORNER.getZ();
    }

    private static List<BlockPos> walls() {
        return BlockPos.betweenClosedStream(LOW, HIGH_CORNER).map(BlockPos::immutable)
                .filter(at -> shell(at) && !at.equals(CONSOLE)).toList();
    }

    private static Room room(final GameTestHelper helper) {
        return SealedRooms.roomAt(helper.getLevel(), helper.absolutePos(INSIDE)).orElse(null);
    }

    /* Puts the lunar suit on {@code player}, its tanks holding SUIT_AIR seconds. */
    private static void suit(final ServerPlayer player) {
        final SpaceSuit suit = SpaceSuits.declared().get(LunarSuit.ID);
        final ItemStack chest = new ItemStack(LunarSuit.SUIT.get());
        LifeSupport.setAir(player.level(), chest, suit, SUIT_AIR);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(LunarSuit.HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(LunarSuit.TROUSERS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(LunarSuit.BOOTS.get()));
    }
}
