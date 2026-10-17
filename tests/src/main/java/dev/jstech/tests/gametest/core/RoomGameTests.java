/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.gametest.GameTestPlayers;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetector;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetectorBlockEntity;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereReadings;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.RoomBalance;
import dev.jstech.core.room.RoomEvent;
import dev.jstech.core.room.RoomVent;
import dev.jstech.core.room.Seal;
import dev.jstech.core.room.Seals;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The sealed rooms kit at work in the world: a closed hut found as a room holding the place's air, worked out again
 * when a block of it changes by any means; air rushing out of a hole and pulling things after it, a crack leaking
 * slowly and glass holding; doors keeping rooms apart and their airs mixing over time once open; a room's air breathed
 * on the Moon, carbon dioxide and too much oxygen harming whoever breathes them; fire out without oxygen and an inferno
 * in pure oxygen; a room under water kept dry and flooding through a broken window; a weak block bursting; mobs
 * breathing by their kind and plants growing only with air, breathing carbon dioxide out as oxygen; vents filling and
 * emptying a room; the events of rooms; how blocks seal; the Atmosphere Detector; and the settings. Rooms share the
 * world's state, so they have batches of their own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RoomGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_rooms";
    private static final String SETTINGS = "jscore_rooms_settings";
    private static final String SIZE = "jscore_rooms_size";
    /* The hut most tests stand: its shell from LOW to HIGH, its eight blocks of air from INSIDE. */
    private static final BlockPos LOW = new BlockPos(2, 1, 2);
    private static final BlockPos HIGH = new BlockPos(5, 4, 5);
    private static final BlockPos INSIDE = new BlockPos(3, 2, 3);
    private static final BlockPos EAST_WALL = new BlockPos(5, 2, 4);
    private static final double ATMOSPHERE = 101.3;
    private static final Map<ResourceLocation, Double> NITROGEN = Map.of(AirMix.NITROGEN, 1.0);
    private static final Map<ResourceLocation, Double> OXYGEN = Map.of(AirMix.OXYGEN, 1.0);

    private RoomGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void room_isFoundAndHoldsThePlacesAir(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        track(helper, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the closed hut is a room"))
                .thenExecute(() -> {
                    untrack(helper, INSIDE);
                    final Room room = room(helper, INSIDE).orElseThrow();
                    helper.assertTrue(room.volume() == 8, "it holds its eight blocks of air; " + room.volume());
                    helper.assertTrue(Math.abs(room.pressure() - ATMOSPHERE) < 0.5 && room.air().breathable()
                            && Math.abs(room.air().share(AirMix.OXYGEN) - 0.21) < 1e-6,
                            "full of the overworld's air; " + room.air());
                    final Climate inside = SealedRooms.climateAt(helper.getLevel(), helper.absolutePos(INSIDE));
                    helper.assertTrue(inside.temperature() == 20, "kept at 20 °C; " + inside);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void room_isWorkedOutAgainWhenABlockChangesEvenWithoutNotice(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        track(helper, INSIDE);
        final ServerLevel level = helper.getLevel();
        final BlockPos filled = helper.absolutePos(new BlockPos(4, 3, 4));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).map(Room::volume).orElse(0) == 8,
                        "the room is found"))
                // Changed telling nothing and no one, as another mod's machine might.
                .thenExecute(() -> level.setBlock(filled, Blocks.STONE.defaultBlockState(), 0))
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).map(Room::volume).orElse(0) == 7,
                        "a block filled in without notice is noted: seven blocks of air"))
                .thenExecute(() -> level.getServer().getCommands().performPrefixedCommand(level.getServer()
                        .createCommandSourceStack().withLevel(level).withSuppressedOutput(), "setblock "
                        + filled.getX() + " " + filled.getY() + " " + filled.getZ() + " minecraft:air"))
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).map(Room::volume).orElse(0) == 8,
                        "and one taken away by a command"))
                .thenExecute(() -> untrack(helper, INSIDE))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void forget_letsGoOfTheRoomsOfAnAreaAndTheirAir(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        track(helper, INSIDE);
        final ServerLevel level = helper.getLevel();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the closed hut is a room"))
                .thenExecute(() -> {
                    fill(room(helper, INSIDE).orElseThrow(), 0, NITROGEN);
                    final AABB area = new AABB(helper.absolutePos(LOW).getCenter(),
                            helper.absolutePos(HIGH).getCenter());
                    helper.assertTrue(SealedRooms.forget(level, area) == 1, "the hut's room is forgotten");
                    helper.assertTrue(room(helper, INSIDE).isEmpty(), "and no room holds the hut");
                    helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(room(helper, INSIDE).isEmpty(),
                            "nothing asks for a room there any more, so a block changing finds none");
                    track(helper, INSIDE);
                })
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE)
                                .map(room -> Math.abs(room.pressure() - ATMOSPHERE) < 0.5).orElse(false),
                        "asked for again, it is found again holding the place's air, not the vacuum it was left"))
                .thenExecute(() -> untrack(helper, INSIDE))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void hole_letsTheAirRushOutIntoAVacuumPullingThingsAfterIt(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("rooms_hole_moon");
        final ServerLevel moon = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.MOON.id());
        moon.setChunkForced(0, 0, true);
        moonHut(moon, Blocks.STONE);
        final BlockPos inside = new BlockPos(3, 9, 3);
        SealedRooms.track(moon, inside);
        final ItemEntity[] stone = new ItemEntity[1];
        final double[] from = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(moon, inside).isPresent(),
                        "the hut on the Moon is a room"))
                .thenExecute(() -> {
                    fill(SealedRooms.roomAt(moon, inside).orElseThrow(), ATMOSPHERE,
                            Map.of(AirMix.NITROGEN, 0.79, AirMix.OXYGEN, 0.21));
                    stone[0] = new ItemEntity(moon, 3.5, 9.05, 4.5, Items.STONE.getDefaultInstance());
                    stone[0].setDeltaMovement(0, 0, 0);
                    moon.addFreshEntity(stone[0]);
                    from[0] = stone[0].getX();
                    moon.setBlockAndUpdate(new BlockPos(5, 9, 4), Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(moon, inside).map(Room::breached)
                        .orElse(false), "the room is opened to the vacuum"))
                .thenExecuteAfter(40, () -> {
                    final double pressure = SealedRooms.roomAt(moon, inside).map(Room::pressure).orElse(0.0);
                    final boolean pulled = stone[0].isRemoved() || stone[0].getX() > from[0] + 0.2;
                    final String at = stone[0].position().toString();
                    SealedRooms.untrack(moon, inside);
                    RuntimeDimensions.remove(server, key);
                    helper.assertTrue(pressure < 60, "the air rushed out; " + pressure);
                    helper.assertTrue(pulled, "and pulled the stone towards the hole; " + at);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void leaks_aCrackSlowlyAHoleFastAndGlassHardly(final GameTestHelper helper) {
        final BlockPos[] insides = {new BlockPos(1, 2, 2), new BlockPos(4, 2, 2), new BlockPos(7, 2, 2)};
        final Block[] walls = {Blocks.COPPER_GRATE, Blocks.AIR, Blocks.GLASS};
        for (int i = 0; i < insides.length; i++) {
            hut(helper, insides[i].offset(-1, -1, -1), insides[i].offset(1, 2, 1), Blocks.STONE);
            track(helper, insides[i]);
        }
        final double[] after = new double[insides.length];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (final BlockPos inside : insides) {
                        helper.assertTrue(room(helper, inside).isPresent(), "each hut is a room");
                    }
                })
                .thenExecute(() -> {
                    // Under what the copper grate stands, so nothing bursts and only the leaks tell.
                    for (int i = 0; i < insides.length; i++) {
                        final Room room = room(helper, insides[i]).orElseThrow();
                        room.add(AirMix.NITROGEN, GasMath.moles(40, room.gasVolume(), GasMath.TEMPERED));
                        helper.setBlock(insides[i].offset(0, 0, -1), walls[i]);
                    }
                })
                .thenExecuteAfter(30, () -> {
                    for (int i = 0; i < insides.length; i++) {
                        after[i] = room(helper, insides[i]).map(Room::pressure).orElse(0.0);
                        untrack(helper, insides[i]);
                    }
                    helper.assertTrue(after[1] < after[0] && after[0] < after[2],
                            "the hole lost most, the copper grate less, the glass least: " + after[1] + " < "
                                    + after[0] + " < " + after[2]);
                    helper.assertTrue(after[2] > 140, "glass hardly leaks; " + after[2]);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void doors_keepRoomsApartAndMixTheirAirsOnceOpen(final GameTestHelper helper) {
        hut(helper, new BlockPos(1, 1, 2), new BlockPos(7, 4, 5), Blocks.STONE);
        for (int y = 2; y <= 3; y++) {
            for (int z = 3; z <= 4; z++) {
                helper.setBlock(new BlockPos(4, y, z), Blocks.STONE);
            }
        }
        final BlockPos lower = new BlockPos(4, 2, 3);
        door(helper, lower, false);
        final BlockPos west = new BlockPos(2, 2, 3);
        final BlockPos east = new BlockPos(6, 2, 3);
        track(helper, west);
        track(helper, east);
        final double[] shut = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, west).isPresent() && room(helper, east).isPresent()
                        && room(helper, west).get() != room(helper, east).get(), "the door keeps two rooms apart"))
                .thenExecute(() -> {
                    fill(room(helper, west).orElseThrow(), ATMOSPHERE, OXYGEN);
                    fill(room(helper, east).orElseThrow(), ATMOSPHERE, NITROGEN);
                })
                .thenExecuteAfter(40, () -> {
                    shut[0] = room(helper, east).orElseThrow().air().share(AirMix.OXYGEN);
                    door(helper, lower, true);
                })
                .thenExecuteAfter(80, () -> {
                    untrack(helper, west);
                    untrack(helper, east);
                    final double open = room(helper, east).orElseThrow().air().share(AirMix.OXYGEN);
                    helper.assertTrue(shut[0] < 0.01, "shut, hardly any oxygen crosses; " + shut[0]);
                    helper.assertTrue(open > 0.02 && open < 0.45, "open, the airs mix, over time; " + open);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void air_ofARoomIsBreathedOnTheMoonAndKeepsWarm(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("rooms_moon");
        final ServerLevel moon = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.MOON.id());
        moon.setChunkForced(0, 0, true);
        moonHut(moon, Blocks.STONE);
        final BlockPos inside = new BlockPos(3, 9, 3);
        SealedRooms.track(moon, inside);
        final ServerPlayer player = GameTestPlayers.join(moon, UUID.randomUUID(), "moonwalker");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(moon, inside).isPresent(),
                        "the hut on the Moon is a room"))
                .thenExecute(() -> {
                    final Room room = SealedRooms.roomAt(moon, inside).orElseThrow();
                    helper.assertTrue(room.pressure() < 0.01, "built in a vacuum, it holds none; " + room.pressure());
                    fill(room, ATMOSPHERE, Map.of(AirMix.NITROGEN, 0.79, AirMix.OXYGEN, 0.21));
                    player.setPos(3.5, 9.0, 3.5);
                    final Set<DimensionHazardEvent.Hazard> in = DimensionEffects.weigh(player);
                    player.setPos(3.5, 20.0, 3.5);
                    final Set<DimensionHazardEvent.Hazard> out = DimensionEffects.weigh(player);
                    SealedRooms.untrack(moon, inside);
                    GameTestPlayers.leave(player);
                    RuntimeDimensions.remove(server, key);
                    helper.assertTrue(in.isEmpty(), "filled, its air is breathed and its warmth kept; " + in);
                    helper.assertTrue(out.contains(DimensionHazardEvent.Hazard.AIR)
                            && out.contains(DimensionHazardEvent.Hazard.COLD), "out on the Moon, neither; " + out);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void air_withCarbonDioxideOrTooMuchOxygenHarms(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        track(helper, INSIDE);
        final ServerPlayer player = GameTestPlayers.join(helper, "breather");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the room is found"))
                .thenExecute(() -> {
                    final Room room = room(helper, INSIDE).orElseThrow();
                    final BlockPos at = helper.absolutePos(INSIDE);
                    player.setPos(at.getX() + 0.5, at.getY() - 0.6, at.getZ() + 0.5);
                    fill(room, ATMOSPHERE, Map.of(AirMix.NITROGEN, 0.77, AirMix.OXYGEN, 0.21,
                            AirMix.CARBON_DIOXIDE, 0.02));
                    final Set<DimensionHazardEvent.Hazard> stale = DimensionEffects.weigh(player);
                    fill(room, ATMOSPHERE, Map.of(AirMix.NITROGEN, 0.73, AirMix.OXYGEN, 0.21,
                            AirMix.CARBON_DIOXIDE, 0.06));
                    final Set<DimensionHazardEvent.Hazard> stifling = DimensionEffects.weigh(player);
                    fill(room, ATMOSPHERE, OXYGEN);
                    final Set<DimensionHazardEvent.Hazard> pure = DimensionEffects.weigh(player);
                    untrack(helper, INSIDE);
                    GameTestPlayers.leave(player);
                    helper.assertTrue(stale.contains(DimensionHazardEvent.Hazard.CARBON_DIOXIDE)
                            && !stale.contains(DimensionHazardEvent.Hazard.AIR), "2% carbon dioxide: a headache; "
                            + stale);
                    helper.assertTrue(stifling.contains(DimensionHazardEvent.Hazard.AIR), "6%: it stifles; "
                            + stifling);
                    helper.assertTrue(pure.contains(DimensionHazardEvent.Hazard.OXYGEN)
                            && !pure.contains(DimensionHazardEvent.Hazard.AIR), "pure oxygen at full pressure "
                            + "poisons, slowly; " + pure);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void fire_goesOutWithoutOxygen(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        helper.setBlock(INSIDE.below(), Blocks.NETHERRACK);
        track(helper, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the room is found"))
                .thenExecute(() -> {
                    fill(room(helper, INSIDE).orElseThrow(), ATMOSPHERE, NITROGEN);
                    helper.setBlock(INSIDE, Blocks.FIRE);
                })
                .thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.FIRE, INSIDE))
                .thenExecute(() -> untrack(helper, INSIDE))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void fire_turnsIntoAnInfernoInPureOxygen(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.OAK_PLANKS);
        helper.setBlock(INSIDE.below(), Blocks.NETHERRACK);
        track(helper, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the room is found"))
                .thenExecute(() -> {
                    fill(room(helper, INSIDE).orElseThrow(), 34, OXYGEN);
                    helper.setBlock(INSIDE, Blocks.FIRE);
                })
                .thenWaitUntil(() -> helper.assertTrue(fires(helper) >= 4, "the whole room catches at once; "
                        + fires(helper) + " fires"))
                .thenExecute(() -> untrack(helper, INSIDE))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void fire_goesOutInAVacuum(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("rooms_fire_moon");
        final ServerLevel moon = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.MOON.id());
        moon.setChunkForced(0, 0, true);
        final BlockPos ground = new BlockPos(5, 8, 5);
        moon.setBlockAndUpdate(ground, Blocks.NETHERRACK.defaultBlockState());
        moon.setBlockAndUpdate(ground.above(), Blocks.FIRE.defaultBlockState());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!(moon.getBlockState(ground.above()).getBlock()
                        instanceof BaseFireBlock), "no oxygen, no fire"))
                .thenExecute(() -> RuntimeDimensions.remove(server, key))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void underWater_aRoomStaysDryAndFloodsThroughAHole(final GameTestHelper helper) {
        hut(helper, new BlockPos(0, 1, 0), new BlockPos(8, 6, 8), Blocks.STONE);
        hut(helper, LOW, HIGH, Blocks.STONE);
        for (int x = 1; x <= 7; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 1; z <= 7; z++) {
                    final BlockPos at = new BlockPos(x, y, z);
                    if (shell(at, LOW, HIGH)) {
                        continue;
                    }
                    helper.setBlock(at, Blocks.WATER);
                }
            }
        }
        helper.setBlock(new BlockPos(4, 6, 4), Blocks.AIR);
        track(helper, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(),
                        "the hut under water is a room"))
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(INSIDE))
                        .isAir() && room(helper, INSIDE).orElseThrow().water() == 0, "kept dry: its water pumped out"))
                .thenExecute(() -> helper.setBlock(new BlockPos(5, 2, 3), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).map(Room::water).orElse(0) > 0,
                        "the water rushes in through the hole"))
                .thenExecute(() -> untrack(helper, INSIDE))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void block_burstsUnderMorePressureThanItStands(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        helper.setBlock(EAST_WALL, Blocks.GLASS);
        track(helper, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the room is found"))
                .thenExecute(() -> {
                    final Room room = room(helper, INSIDE).orElseThrow();
                    room.add(AirMix.NITROGEN, GasMath.moles(250, room.gasVolume(), GasMath.TEMPERED));
                })
                .thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.GLASS, EAST_WALL))
                .thenExecute(() -> untrack(helper, INSIDE))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void mobs_breatheByTheirKind(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        track(helper, INSIDE);
        final Cow[] cow = new Cow[1];
        final Zombie[] zombie = new Zombie[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the room is found"))
                .thenExecute(() -> {
                    fill(room(helper, INSIDE).orElseThrow(), ATMOSPHERE, NITROGEN);
                    cow[0] = helper.spawn(EntityType.COW, INSIDE);
                    zombie[0] = helper.spawn(EntityType.ZOMBIE, INSIDE.offset(1, 0, 1));
                })
                .thenExecuteAfter(60, () -> {
                    untrack(helper, INSIDE);
                    helper.assertTrue(cow[0].getAirSupply() < cow[0].getMaxAirSupply() - 30,
                            "a cow chokes in nitrogen; air " + cow[0].getAirSupply());
                    helper.assertTrue(zombie[0].getAirSupply() == zombie[0].getMaxAirSupply(),
                            "a zombie does not breathe; air " + zombie[0].getAirSupply());
                    cow[0].discard();
                    zombie[0].discard();
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void plants_growOnlyWithAirAndBreatheOutOxygen(final GameTestHelper helper) {
        hut(helper, new BlockPos(2, 1, 2), new BlockPos(5, 5, 5), Blocks.STONE);
        for (int x = 3; x <= 4; x++) {
            for (int z = 3; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 5, z), Blocks.GLASS);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.FARMLAND);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.WHEAT);
            }
        }
        final BlockPos air = new BlockPos(3, 4, 3);
        final BlockPos wheat = new BlockPos(3, 3, 3);
        track(helper, air);
        final double[] oxygen = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, air).isPresent(), "the greenhouse is a room"))
                .thenExecute(() -> {
                    final Room room = room(helper, air).orElseThrow();
                    helper.assertTrue(room.plants() == 4, "with four crops; " + room.plants());
                    room.empty();
                    growTicks(helper, wheat);
                    helper.assertTrue(age(helper, wheat) == 0, "in a vacuum nothing grows");
                    fill(room, ATMOSPHERE, Map.of(AirMix.NITROGEN, 0.9, AirMix.CARBON_DIOXIDE, 0.1));
                    oxygen[0] = room.moles(AirMix.OXYGEN);
                })
                .thenExecuteAfter(45, () -> {
                    final Room room = room(helper, air).orElseThrow();
                    helper.assertTrue(room.moles(AirMix.OXYGEN) > oxygen[0] + 0.01,
                            "the crops breathe carbon dioxide out as oxygen; " + room.moles(AirMix.OXYGEN));
                    growTicks(helper, wheat);
                    untrack(helper, air);
                    helper.assertTrue(age(helper, wheat) > 0, "with air, they grow");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void vent_fillsARoomFromATankAndEmptiesItIntoOne(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        track(helper, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the room is found"))
                .thenExecute(() -> {
                    final Room room = room(helper, INSIDE).orElseThrow();
                    final ServerLevel level = helper.getLevel();
                    final BlockPos vent = helper.absolutePos(EAST_WALL);
                    final MatterTank full = new MatterTank(MatterState.GAS, 100_000, 1, () -> { });
                    full.insert(new MatterStack(Elements.of(Element.OXYGEN), 22_400), false);
                    final double before = room.moles(AirMix.OXYGEN);
                    final long filled = RoomVent.fill(level, vent, full, 2_240);
                    helper.assertTrue(filled == 2_240 && Math.abs(room.moles(AirMix.OXYGEN) - before - 100) < 1e-6,
                            "2,240 mB of oxygen went in as a hundred moles");
                    fill(room, ATMOSPHERE, OXYGEN);
                    final MatterTank empty = new MatterTank(MatterState.GAS, 100_000, 1, () -> { });
                    final double held = room.moles(AirMix.OXYGEN);
                    final long emptied = RoomVent.empty(level, vent, empty, 2_240);
                    untrack(helper, INSIDE);
                    helper.assertTrue(emptied == 2_240 && Math.abs(held - room.moles(AirMix.OXYGEN) - 100) < 1e-6
                            && empty.held().map(MatterStack::amount).orElse(0L) == 2_240,
                            "and a hundred moles came back out into the tank");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void events_tellWhenARoomFormsFillsAndLeaks(final GameTestHelper helper) {
        final List<String> heard = new ArrayList<>();
        final BlockPos cell = helper.absolutePos(INSIDE);
        final Consumer<RoomEvent.Formed> formed = event -> hear(event, cell, "formed", heard);
        final Consumer<RoomEvent.Filled> filled = event -> hear(event, cell, "filled", heard);
        final Consumer<RoomEvent.Leaking> leaking = event -> hear(event, cell, "leaking", heard);
        NeoForge.EVENT_BUS.addListener(RoomEvent.Formed.class, formed);
        NeoForge.EVENT_BUS.addListener(RoomEvent.Filled.class, filled);
        NeoForge.EVENT_BUS.addListener(RoomEvent.Leaking.class, leaking);
        hut(helper, LOW, HIGH, Blocks.STONE);
        track(helper, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(heard.contains("formed"), "a room formed"))
                .thenExecute(() -> {
                    final Room room = room(helper, INSIDE).orElseThrow();
                    room.empty();
                })
                .thenExecuteAfter(25, () -> fill(room(helper, INSIDE).orElseThrow(), ATMOSPHERE,
                        Map.of(AirMix.NITROGEN, 0.79, AirMix.OXYGEN, 0.21)))
                .thenWaitUntil(() -> helper.assertTrue(heard.contains("filled"), "it filled with air to breathe"))
                .thenExecute(() -> helper.setBlock(EAST_WALL, Blocks.COPPER_GRATE))
                .thenWaitUntil(() -> helper.assertTrue(heard.contains("leaking"), "it began to leak"))
                .thenExecute(() -> {
                    NeoForge.EVENT_BUS.unregister(formed);
                    NeoForge.EVENT_BUS.unregister(filled);
                    NeoForge.EVENT_BUS.unregister(leaking);
                    untrack(helper, INSIDE);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void seals_areDataAndShapes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(Seals.of(level, at, Blocks.STONE.defaultBlockState()).wall()
                && Seals.of(level, at, Blocks.STONE.defaultBlockState()).leak() == 0, "stone seals");
        final Seal glass = Seals.of(level, at, Blocks.GLASS.defaultBlockState());
        helper.assertTrue(glass.wall() && glass.leak() > 0 && Seals.rating(glass, Blocks.GLASS.defaultBlockState())
                < Seals.rating(Seal.SOLID, Blocks.STONE.defaultBlockState()), "glass leaks a little, stands less");
        final BlockState door = Blocks.OAK_DOOR.defaultBlockState();
        final Seal doorSeal = Seals.of(level, at, door);
        helper.assertTrue(doorSeal.door() && Seals.leak(doorSeal, door) < 0.01
                && Seals.leak(doorSeal, door.setValue(DoorBlock.OPEN, true)) == 1.0,
                "a door leaks a little shut and everything open");
        helper.assertTrue(!Seals.of(level, at, Blocks.STONE_SLAB.defaultBlockState()).wall()
                && !Seals.of(level, at, Blocks.AIR.defaultBlockState()).wall(), "a slab and air let air through");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void detector_readsItsRoomAndTheOpenAir(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        helper.setBlock(INSIDE, AtmosphereDetector.BLOCK.get());
        // High over the arena, whose barriers close its own air in like a room.
        final BlockPos outside = new BlockPos(4, 30, 4);
        helper.setBlock(outside, AtmosphereDetector.BLOCK.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(readings(helper, INSIDE).room(), "it reads its room"))
                .thenWaitUntil(() -> helper.assertTrue(readings(helper, outside).pressure() > 0,
                        "the other reads the open air"))
                .thenExecute(() -> {
                    final AtmosphereReadings room = readings(helper, INSIDE);
                    final AtmosphereReadings open = readings(helper, outside);
                    helper.assertTrue(room.volume() == 7 && Math.abs(room.pressure() - ATMOSPHERE) < 0.5
                            && room.gases().getFirst().id().equals(AirMix.NITROGEN) && room.leaks() == 0,
                            "seven blocks of the overworld's air, nitrogen first, sealed; " + room);
                    helper.setBlock(outside, Blocks.AIR);
                    helper.assertTrue(!open.room() && Math.abs(open.pressure() - ATMOSPHERE) < 0.01,
                            "the open air, the overworld's; " + open);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = SETTINGS, timeoutTicks = 300)
    public static void settings_turnTheConsequencesOff(final GameTestHelper helper) {
        hut(helper, LOW, HIGH, Blocks.STONE);
        helper.setBlock(EAST_WALL, Blocks.GLASS);
        helper.setBlock(INSIDE.below(), Blocks.NETHERRACK);
        track(helper, INSIDE);
        final Cow[] cow = new Cow[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, INSIDE).isPresent(), "the room is found"))
                .thenExecute(() -> {
                    RoomBalance.setBlocksBurst(false);
                    RoomBalance.setFireNeedsOxygen(false);
                    RoomBalance.setMobsBreathe(false);
                    final Room room = room(helper, INSIDE).orElseThrow();
                    fill(room, 300, NITROGEN);
                    helper.setBlock(INSIDE, Blocks.FIRE);
                    cow[0] = helper.spawn(EntityType.COW, INSIDE.offset(1, 0, 1));
                })
                .thenExecuteAfter(60, () -> {
                    final boolean glass = helper.getLevel().getBlockState(helper.absolutePos(EAST_WALL))
                            .is(Blocks.GLASS);
                    final boolean fire = helper.getLevel().getBlockState(helper.absolutePos(INSIDE)).is(Blocks.FIRE);
                    final boolean breathing = cow[0].getAirSupply() == cow[0].getMaxAirSupply();
                    RoomBalance.setBlocksBurst(true);
                    RoomBalance.setFireNeedsOxygen(true);
                    RoomBalance.setMobsBreathe(true);
                    cow[0].discard();
                    untrack(helper, INSIDE);
                    helper.assertTrue(glass, "with bursting off, the glass holds");
                    helper.assertTrue(fire, "with fire needing no oxygen, it burns in nitrogen");
                    helper.assertTrue(breathing, "with mobs not breathing, the cow is fine");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = SIZE, timeoutTicks = 200)
    public static void settings_setTheLargestRoom(final GameTestHelper helper) {
        RoomBalance.setLargestRoom(4);
        hut(helper, LOW, HIGH, Blocks.STONE);
        track(helper, INSIDE);
        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final boolean found = room(helper, INSIDE).isPresent();
                    RoomBalance.setLargestRoom(RoomBalance.LARGEST_DEFAULT);
                    untrack(helper, INSIDE);
                    helper.assertTrue(!found, "eight blocks is more than the largest room of four");
                })
                .thenSucceed();
    }

    /* A closed box of {@code wall} from {@code low} to {@code high}, its inside air. */
    private static void hut(final GameTestHelper helper, final BlockPos low, final BlockPos high, final Block wall) {
        for (int x = low.getX(); x <= high.getX(); x++) {
            for (int y = low.getY(); y <= high.getY(); y++) {
                for (int z = low.getZ(); z <= high.getZ(); z++) {
                    final BlockPos at = new BlockPos(x, y, z);
                    helper.setBlock(at, shell(at, low, high) ? wall : Blocks.AIR);
                }
            }
        }
    }

    /* The hut on the Moon: a stone box from 2, 8, 2 to 5, 11, 5, standing on the ground. */
    private static void moonHut(final ServerLevel moon, final Block wall) {
        final BlockPos low = new BlockPos(2, 8, 2);
        final BlockPos high = new BlockPos(5, 11, 5);
        for (int x = low.getX(); x <= high.getX(); x++) {
            for (int y = low.getY(); y <= high.getY(); y++) {
                for (int z = low.getZ(); z <= high.getZ(); z++) {
                    final BlockPos at = new BlockPos(x, y, z);
                    moon.setBlockAndUpdate(at, shell(at, low, high) ? wall.defaultBlockState()
                            : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static boolean shell(final BlockPos at, final BlockPos low, final BlockPos high) {
        return at.getX() == low.getX() || at.getX() == high.getX() || at.getY() == low.getY()
                || at.getY() == high.getY() || at.getZ() == low.getZ() || at.getZ() == high.getZ();
    }

    /* A wooden door, its two halves, facing east, open or shut. */
    private static void door(final GameTestHelper helper, final BlockPos lower, final boolean open) {
        final BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST)
                .setValue(DoorBlock.OPEN, open);
        helper.setBlock(lower, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /* Empties {@code room} and fills it with air of {@code shares} at {@code kilopascals}. */
    private static void fill(final Room room, final double kilopascals, final Map<ResourceLocation, Double> shares) {
        room.empty();
        for (final Map.Entry<ResourceLocation, Double> gas : shares.entrySet()) {
            room.add(gas.getKey(), GasMath.moles(kilopascals * gas.getValue(), room.gasVolume(), GasMath.TEMPERED));
        }
    }

    private static Optional<Room> room(final GameTestHelper helper, final BlockPos at) {
        return SealedRooms.roomAt(helper.getLevel(), helper.absolutePos(at));
    }

    private static void track(final GameTestHelper helper, final BlockPos at) {
        SealedRooms.track(helper.getLevel(), helper.absolutePos(at));
    }

    private static void untrack(final GameTestHelper helper, final BlockPos at) {
        SealedRooms.untrack(helper.getLevel(), helper.absolutePos(at));
    }

    /* How many fires burn in the hut and on its walls. */
    private static int fires(final GameTestHelper helper) {
        int count = 0;
        for (int x = LOW.getX(); x <= HIGH.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH.getZ(); z++) {
                    if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z))).getBlock()
                            instanceof BaseFireBlock) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /* Ticks the crop at {@code at} as the game's random ticks would, many times. */
    private static void growTicks(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(at);
        for (int i = 0; i < 200; i++) {
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock) {
                state.randomTick(level, pos, level.getRandom());
            }
        }
    }

    private static int age(final GameTestHelper helper, final BlockPos at) {
        final BlockState state = helper.getLevel().getBlockState(helper.absolutePos(at));
        return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
    }

    private static AtmosphereReadings readings(final GameTestHelper helper, final BlockPos at) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(at)) instanceof AtmosphereDetectorBlockEntity
                detector ? detector.readings() : AtmosphereReadings.NONE;
    }

    /* Notes {@code what} happened, when it happened to the room holding {@code cell}. */
    private static void hear(final RoomEvent event, final BlockPos cell, final String what, final List<String> heard) {
        if (event.room().cells().contains(cell.asLong())) {
            heard.add(what);
        }
    }

    /* A dimension of its own for this run: a room kept with an earlier run's would come back with it. */
    private static ResourceKey<Level> key(final String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                path + "_" + UUID.randomUUID().toString().substring(0, 8)));
    }
}
