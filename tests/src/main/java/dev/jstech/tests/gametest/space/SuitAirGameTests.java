/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.air.Lungs;
import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.life.OxygenBottleItem;
import dev.jstech.space.life.SuitAir;
import dev.jstech.space.life.SuitSupplies;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestVehicles;
import dev.jstech.tests.TestWorldGen;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A suit's air through J's Core's lungs: worn whole where the air cannot be breathed, its wearer breathes the tanks, a
 * second spent each second; where the air can be breathed, a room's, the wearer breathes that and the tanks are
 * spared, so the helmet comes off in a room with air and not in a vacuum. A hole leaks and thins the suit, two leave
 * too little oxygen to breathe, and a patch mends one. An oxygen bottle fills from a room's air, taking its oxygen out,
 * and from a gas tank, and pours into the suit's tanks.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SuitAirGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_suit_air";
    /* Where the player stands on the test Moon, high in its vacuum. */
    private static final Vec3 HIGH = new Vec3(0.5, 100, 0.5);
    /* The hut in the arena: its shell from LOW to HIGH_CORNER, a block of its air, and where a player stands in it. */
    private static final BlockPos LOW = new BlockPos(2, 1, 2);
    private static final BlockPos HIGH_CORNER = new BlockPos(5, 4, 5);
    private static final BlockPos INSIDE = new BlockPos(3, 3, 3);
    private static final Vec3 STANDING = new Vec3(3.5, 2, 3.5);
    private static final double CLOSE = 1e-6;
    /* The oxygen in the pump, in millibuckets: more than a bottle takes. */
    private static final long PUMP_OXYGEN = 1_000;

    private SuitAirGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void tanks_breatheWhereTheAirCannotAndTheHelmetComesOffInARoomWithAir(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("suit_air_moon");
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key,
                TestWorldGen.MOON.id());
        final ServerPlayer outside = GameTestPlayers.join(moon, UUID.randomUUID(), "outside");
        outside.setPos(HIGH);
        suit(outside);
        final boolean supplied = Lungs.supplied(outside).isPresent();
        final Set<DimensionHazardEvent.Hazard> suited = DimensionEffects.weigh(outside);
        SuitAir.second(outside);
        final int spent = LunarSuit.AIR - air(outside);
        outside.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        final Set<DimensionHazardEvent.Hazard> bareHeaded = DimensionEffects.weigh(outside);
        GameTestPlayers.leave(outside);
        RuntimeDimensions.remove(helper.getLevel().getServer(), key);
        hut(helper);
        SealedRooms.track(helper.getLevel(), helper.absolutePos(INSIDE));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper) != null, "the hut is a room"))
                .thenExecute(() -> {
                    final ServerPlayer inside = GameTestPlayers.join(helper.getLevel(), UUID.randomUUID(), "inside");
                    inside.setPos(helper.absoluteVec(STANDING));
                    suit(inside);
                    final boolean suppliedInside = Lungs.supplied(inside).isPresent();
                    SuitAir.second(inside);
                    final int spentInside = LunarSuit.AIR - air(inside);
                    inside.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    final Set<DimensionHazardEvent.Hazard> helmetOff = DimensionEffects.weigh(inside);
                    GameTestPlayers.leave(inside);
                    SealedRooms.untrack(helper.getLevel(), helper.absolutePos(INSIDE));
                    helper.assertTrue(supplied && !suited.contains(DimensionHazardEvent.Hazard.AIR),
                            "in the Moon's vacuum the suit gives its tanks' air; " + suited);
                    helper.assertTrue(spent == 1, "a second of air spent each second: " + spent);
                    helper.assertTrue(bareHeaded.contains(DimensionHazardEvent.Hazard.AIR),
                            "the helmet off in a vacuum, no air; " + bareHeaded);
                    helper.assertTrue(!suppliedInside && spentInside == 0,
                            "in a room of air the room's air is breathed and the tanks spared: " + spentInside);
                    helper.assertTrue(!helmetOff.contains(DimensionHazardEvent.Hazard.AIR),
                            "and the helmet comes off; " + helmetOff);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hole_leaksAndThinsTheSuitUntilPatched(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("suit_air_hole");
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key,
                TestWorldGen.MOON.id());
        final ServerPlayer player = GameTestPlayers.join(moon, UUID.randomUUID(), "holed");
        player.setPos(HIGH);
        suit(player);
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        SuitAir.hole(chest);
        final double holed = SuitAir.pressure(chest);
        final Set<DimensionHazardEvent.Hazard> oneHole = DimensionEffects.weigh(player);
        SuitAir.second(player);
        final int spent = LunarSuit.AIR - air(player);
        SuitAir.hole(chest);
        final Set<DimensionHazardEvent.Hazard> twoHoles = DimensionEffects.weigh(player);
        final boolean patched = SuitAir.patch(chest) && SuitAir.patch(chest);
        final boolean nothingLeft = SuitAir.patch(chest);
        final double mended = SuitAir.pressure(chest);
        GameTestPlayers.leave(player);
        RuntimeDimensions.remove(helper.getLevel().getServer(), key);
        helper.assertTrue(Math.abs(holed - SuitAir.PRESSURE * (1 - SuitAir.HOLE_DROP)) < CLOSE,
                "a hole thins the suit: " + holed + " kPa");
        helper.assertTrue(!oneHole.contains(DimensionHazardEvent.Hazard.AIR), "but leaves it oxygen to breathe; "
                + oneHole);
        helper.assertTrue(spent == 1 + SuitAir.LEAK, "and leaks while it is breathed: " + spent);
        helper.assertTrue(twoHoles.contains(DimensionHazardEvent.Hazard.AIR), "two leave too little; " + twoHoles);
        helper.assertTrue(patched && !nothingLeft && Math.abs(mended - SuitAir.PRESSURE) < CLOSE,
                "a patch mends each hole, and the suit holds its pressure again: " + mended);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void bottle_fillsFromARoomAndAGasTankAndPoursIntoTheSuit(final GameTestHelper helper) {
        hut(helper);
        final BlockPos pumpAt = new BlockPos(7, 2, 3);
        helper.setBlock(pumpAt, TestVehicles.PUMP.get());
        SealedRooms.track(helper.getLevel(), helper.absolutePos(INSIDE));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper) != null, "the hut is a room"))
                .thenExecute(() -> {
                    final ServerLevel level = helper.getLevel();
                    final double before = room(helper).moles(AirMix.OXYGEN);
                    final ItemStack fromRoom = new ItemStack(SuitSupplies.OXYGEN_BOTTLE.get());
                    final int roomSeconds = OxygenBottleItem.fillFromRoom(level, fromRoom,
                            helper.absolutePos(INSIDE));
                    final double taken = before - room(helper).moles(AirMix.OXYGEN);
                    final IMatterHandler pump = level.getCapability(MatterCapabilities.GAS,
                            helper.absolutePos(pumpAt), null);
                    pump.insert(new MatterStack(Elements.of(Element.OXYGEN), PUMP_OXYGEN), false);
                    final ItemStack fromTank = new ItemStack(SuitSupplies.OXYGEN_BOTTLE.get());
                    final int tankSeconds = OxygenBottleItem.fillFromGas(fromTank, pump);
                    final long leftInPump = pump.stack(0).map(MatterStack::amount).orElse(0L);
                    final SpaceSuit suit = SpaceSuits.declared().get(LunarSuit.ID);
                    final ItemStack chest = new ItemStack(LunarSuit.SUIT.get());
                    LifeSupport.setAir(level, chest, suit, 0);
                    final int poured = OxygenBottleItem.pour(level, fromRoom, chest);
                    SealedRooms.untrack(level, helper.absolutePos(INSIDE));
                    helper.assertTrue(roomSeconds == OxygenBottleItem.FULL, "a bottle fills from a room's air: "
                            + roomSeconds);
                    helper.assertTrue(Math.abs(taken - OxygenBottleItem.FULL * 0.01) < 1e-3,
                            "taking its oxygen out of the room: " + taken + " mol");
                    helper.assertTrue(tankSeconds == OxygenBottleItem.FULL && leftInPump < PUMP_OXYGEN
                            && leftInPump > 0, "and from a gas tank's oxygen: " + tankSeconds + ", " + leftInPump
                            + " mB left");
                    helper.assertTrue(poured == OxygenBottleItem.FULL
                                    && LifeSupport.air(level, chest, suit) == LunarSuit.AIR
                                    && OxygenBottleItem.air(fromRoom) == 0,
                            "and pours into a suit's empty tanks, filling them: " + poured);
                })
                .thenSucceed();
    }

    /* Puts the lunar suit on {@code player}, its four pieces. */
    private static void suit(final ServerPlayer player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(LunarSuit.HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(LunarSuit.SUIT.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(LunarSuit.TROUSERS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(LunarSuit.BOOTS.get()));
    }

    private static int air(final ServerPlayer player) {
        final SpaceSuit suit = SpaceSuits.declared().get(LunarSuit.ID);
        return LifeSupport.air(player.level(), player.getItemBySlot(EquipmentSlot.CHEST), suit);
    }

    /* A closed box of stone from LOW to HIGH_CORNER, its inside air. */
    private static void hut(final GameTestHelper helper) {
        for (int x = LOW.getX(); x <= HIGH_CORNER.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH_CORNER.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH_CORNER.getZ(); z++) {
                    final boolean shell = x == LOW.getX() || x == HIGH_CORNER.getX() || y == LOW.getY()
                            || y == HIGH_CORNER.getY() || z == LOW.getZ() || z == HIGH_CORNER.getZ();
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
    }

    private static Room room(final GameTestHelper helper) {
        return SealedRooms.roomAt(helper.getLevel(), helper.absolutePos(INSIDE)).orElse(null);
    }

    private static ResourceKey<Level> key(final String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                path + "_" + UUID.randomUUID().toString().substring(0, 8)));
    }
}
