/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.JsTests;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Space's rules for sealed rooms on the worlds it makes: the game's glass and doors, which hold a room's air on the
 * Earth, burst on the Moon when the room is filled, where stone holds; and the game's open flames go out where the air
 * holds too little oxygen to burn, a torch falling in the Moon's vacuum and in a room whose air runs out, a candle and
 * a campfire left unlit and a lantern fallen in a room of nitrogen, and all of them burning anywhere with the setting
 * off. Each test on the Moon has a copy of it of its own, its huts high over the ground in the open vacuum.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PressurizedRoomGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_rooms";
    private static final String SETTINGS = "jsspace_rooms_settings";
    /* How high over the Moon's ground its huts stand, and the overworld hut's low corner in the arena. */
    private static final int HIGH_UP = 200;
    private static final BlockPos ARENA_HUT = new BlockPos(2, 1, 2);
    /* A hut's shell runs SIZE blocks each way from its low corner; its air starts a block in. */
    private static final int SIZE = 3;
    private static final double ATMOSPHERE = 101.3;
    private static final Map<ResourceLocation, Double> AIR = Map.of(AirMix.NITROGEN, 0.79, AirMix.OXYGEN, 0.21);
    private static final Map<ResourceLocation, Double> NITROGEN = Map.of(AirMix.NITROGEN, 1.0);
    /* How far round a torch put out its item is looked for. */
    private static final double FALL_REACH = 12;

    private PressurizedRoomGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void glassAndDoors_ofTheGameBurstOnTheMoonWhereStoneHoldsAndOnTheEarthHold(
            final GameTestHelper helper) {
        final ResourceKey<Level> key = key("rooms_seals");
        final ServerLevel moon = moon(helper, key);
        final BlockPos glassHut = new BlockPos(1, HIGH_UP, 1);
        final BlockPos doorHut = new BlockPos(6, HIGH_UP, 1);
        final BlockPos stoneHut = new BlockPos(11, HIGH_UP, 1);
        hut(moon, glassHut);
        hut(moon, doorHut);
        hut(moon, stoneHut);
        moon.setBlockAndUpdate(window(glassHut), Blocks.GLASS.defaultBlockState());
        ironDoor(moon, window(doorHut));
        final ServerLevel earth = helper.getLevel();
        final BlockPos earthHut = helper.absolutePos(ARENA_HUT);
        hut(earth, earthHut);
        earth.setBlockAndUpdate(window(earthHut), Blocks.GLASS.defaultBlockState());
        ironDoor(earth, window(earthHut).south());
        for (final BlockPos low : new BlockPos[] {glassHut, doorHut, stoneHut}) {
            SealedRooms.track(moon, inside(low));
        }
        SealedRooms.track(earth, inside(earthHut));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(moon, glassHut).isPresent()
                        && room(moon, doorHut).isPresent() && room(moon, stoneHut).isPresent()
                        && room(earth, earthHut).isPresent(), "the four huts are rooms"))
                .thenExecute(() -> {
                    fill(room(moon, glassHut).orElseThrow(), ATMOSPHERE, AIR);
                    fill(room(moon, doorHut).orElseThrow(), ATMOSPHERE, AIR);
                    fill(room(moon, stoneHut).orElseThrow(), ATMOSPHERE, AIR);
                    // The same difference as on the Moon: the overworld's air outside, twice as much inside.
                    fill(room(earth, earthHut).orElseThrow(), 2 * ATMOSPHERE, AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(moon.getBlockState(window(glassHut)).isAir()
                                && moon.getBlockState(window(doorHut)).isAir(),
                        "on the Moon the game's glass and its iron door burst under the room's air"))
                .thenExecuteAfter(40, () -> {
                    final double stone = room(moon, stoneHut).map(Room::pressure).orElse(0.0);
                    final double held = room(earth, earthHut).map(Room::pressure).orElse(0.0);
                    final boolean glass = earth.getBlockState(window(earthHut)).is(Blocks.GLASS);
                    final boolean door = earth.getBlockState(window(earthHut).south()).is(Blocks.IRON_DOOR);
                    for (final BlockPos low : new BlockPos[] {glassHut, doorHut, stoneHut}) {
                        SealedRooms.untrack(moon, inside(low));
                    }
                    SealedRooms.untrack(earth, inside(earthHut));
                    RuntimeDimensions.remove(helper.getLevel().getServer(), key);
                    helper.assertTrue(stone > 0.9 * ATMOSPHERE, "the hut of stone holds its air; " + stone);
                    helper.assertTrue(glass && door && held > 1.9 * ATMOSPHERE,
                            "on the Earth the same glass and door hold the same difference; " + held);
                })
                .thenSucceed();
    }

    /*
     * The test server runs its ticks as fast as it can, so the twenty seconds the Moon's entities may take to load
     * while many tests read and write the disk can pass in far more ticks than a test usually waits.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 12_000)
    public static void torch_fallsInTheMoonsVacuumAndBurnsInARoomOfAirUntilItRunsOut(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("rooms_torch");
        final ServerLevel moon = moon(helper, key);
        final BlockPos open = new BlockPos(8, HIGH_UP + 1, 8);
        moon.setBlockAndUpdate(open.below(), Blocks.STONE.defaultBlockState());
        final BlockPos low = new BlockPos(1, HIGH_UP, 1);
        final BlockPos torch = inside(low);
        hut(moon, low);
        SealedRooms.track(moon, inside(low).above());
        helper.startSequence()
                // The new world's entities load a while after its chunk is forced, as much as twenty seconds while
                // many tests read and write the disk; an item dropped before they have would be hidden from the search.
                .thenWaitUntil(() -> helper.assertTrue(moon.areEntitiesLoaded(ChunkPos.asLong(open)),
                        "the Moon's chunk holds its entities"))
                .thenExecute(() -> moon.setBlockAndUpdate(open, Blocks.TORCH.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertTrue(moon.getBlockState(open).isAir() && fallen(moon, open),
                        "a torch set in the Moon's vacuum goes out and falls; " + moon.getBlockState(open)))
                .thenWaitUntil(() -> helper.assertTrue(room(moon, low).isPresent(), "the hut is a room"))
                .thenExecute(() -> {
                    fill(room(moon, low).orElseThrow(), ATMOSPHERE, AIR);
                    moon.setBlockAndUpdate(torch, Blocks.TORCH.defaultBlockState());
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(moon.getBlockState(torch).is(Blocks.TORCH), "in the room's air it burns");
                    room(moon, low).orElseThrow().empty();
                })
                .thenWaitUntil(() -> helper.assertTrue(moon.getBlockState(torch).isAir(),
                        "the room's air gone, it goes out and falls"))
                .thenExecute(() -> {
                    SealedRooms.untrack(moon, inside(low).above());
                    RuntimeDimensions.remove(helper.getLevel().getServer(), key);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void candleCampfireAndLantern_goOutInARoomOfNitrogen(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos low = helper.absolutePos(ARENA_HUT);
        final BlockPos candle = inside(low);
        final BlockPos campfire = inside(low).east().south();
        final BlockPos lantern = inside(low).south();
        hut(level, low);
        SealedRooms.track(level, inside(low).above());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(level, low).isPresent(), "the hut is a room"))
                .thenExecute(() -> {
                    fill(room(level, low).orElseThrow(), ATMOSPHERE, NITROGEN);
                    level.setBlockAndUpdate(candle, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT,
                            true));
                    level.setBlockAndUpdate(campfire, Blocks.CAMPFIRE.defaultBlockState()
                            .setValue(CampfireBlock.LIT, true));
                    level.setBlockAndUpdate(lantern, Blocks.LANTERN.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(!level.getBlockState(candle).getValue(CandleBlock.LIT)
                                && !level.getBlockState(campfire).getValue(CampfireBlock.LIT)
                                && level.getBlockState(lantern).isAir(),
                        "with no oxygen the candle and the campfire go out and the lantern falls"))
                .thenExecute(() -> SealedRooms.untrack(level, inside(low).above()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = SETTINGS, timeoutTicks = 300)
    public static void flames_burnAnywhereWithTheSettingOff(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("rooms_flames_off");
        final ServerLevel moon = moon(helper, key);
        final BlockPos open = new BlockPos(8, HIGH_UP + 1, 8);
        SpaceServerConfig.FILE.set(SpaceServerConfig.FLAMES_NEED_AIR, false);
        moon.setBlockAndUpdate(open.below(), Blocks.STONE.defaultBlockState());
        moon.setBlockAndUpdate(open, Blocks.TORCH.defaultBlockState());
        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final BlockState left = moon.getBlockState(open);
                    SpaceServerConfig.FILE.set(SpaceServerConfig.FLAMES_NEED_AIR, true);
                    RuntimeDimensions.remove(helper.getLevel().getServer(), key);
                    helper.assertTrue(left.is(Blocks.TORCH), "with flames needing no air the torch burns in the "
                            + "Moon's vacuum; " + left);
                })
                .thenSucceed();
    }

    /* A copy of the Moon of its own for this test, its first chunk kept loaded. */
    private static ServerLevel moon(final GameTestHelper helper, final ResourceKey<Level> key) {
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key,
                MoonWorld.world().dimension().location());
        moon.setChunkForced(0, 0, true);
        return moon;
    }

    /* A closed box of stone from {@code low}, SIZE blocks each way, its inside air. */
    private static void hut(final ServerLevel level, final BlockPos low) {
        for (int x = 0; x <= SIZE; x++) {
            for (int y = 0; y <= SIZE; y++) {
                for (int z = 0; z <= SIZE; z++) {
                    final boolean shell = x == 0 || x == SIZE || y == 0 || y == SIZE || z == 0 || z == SIZE;
                    level.setBlockAndUpdate(low.offset(x, y, z), (shell ? Blocks.STONE : Blocks.AIR)
                            .defaultBlockState());
                }
            }
        }
    }

    /* The first block of a hut's air, on its floor. */
    private static BlockPos inside(final BlockPos low) {
        return low.offset(1, 1, 1);
    }

    /* A place of a hut's east wall, beside its first block of air. */
    private static BlockPos window(final BlockPos low) {
        return low.offset(SIZE, 1, 1);
    }

    /* An iron door, shut, its two halves in a hut's east wall from {@code lower} up. */
    private static void ironDoor(final ServerLevel level, final BlockPos lower) {
        final BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST);
        place(level, lower, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        place(level, lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /* Sets a block telling only the room kit, as a door's halves must be set one by one. */
    private static void place(final ServerLevel level, final BlockPos pos, final BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /* Empties {@code room} and fills it with air of {@code shares} at {@code kilopascals}. */
    private static void fill(final Room room, final double kilopascals, final Map<ResourceLocation, Double> shares) {
        room.empty();
        for (final Map.Entry<ResourceLocation, Double> gas : shares.entrySet()) {
            room.add(gas.getKey(), GasMath.moles(kilopascals * gas.getValue(), room.gasVolume(), GasMath.TEMPERED));
        }
    }

    /*
     * Whether a torch is out as an item near {@code pos} or anywhere under it: popped out in the Moon's pull, it flies
     * some blocks up and away, then falls past the block it stood on to the ground far below.
     */
    private static boolean fallen(final ServerLevel level, final BlockPos pos) {
        return !level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(FALL_REACH)
                .expandTowards(0, level.getMinBuildHeight() - pos.getY(), 0),
                item -> item.getItem().is(Items.TORCH)).isEmpty();
    }

    private static Optional<Room> room(final ServerLevel level, final BlockPos low) {
        return SealedRooms.roomAt(level, inside(low).above());
    }

    /* A dimension of its own for this run: a room kept with an earlier run's would come back with it. */
    private static ResourceKey<Level> key(final String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                path + "_" + UUID.randomUUID().toString().substring(0, 8)));
    }
}
