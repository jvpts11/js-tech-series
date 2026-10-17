/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.series;

import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.gametest.GameTestPlayers;
import dev.jstech.core.jstechseries.persistor.PersistorArea;
import dev.jstech.core.jstechseries.persistor.PersistorReadings;
import dev.jstech.core.jstechseries.persistor.PersistorSettings;
import dev.jstech.core.jstechseries.persistor.PersistorShape;
import dev.jstech.core.jstechseries.persistor.PersistorShapes;
import dev.jstech.core.jstechseries.persistor.SpacePersistor;
import dev.jstech.core.jstechseries.persistor.SpacePersistorBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.region.LoadedChunkMarkers;
import dev.jstech.core.team.Ownership;
import dev.jstech.tests.JsTests;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Space Persistor: it keeps the square round it loaded for its owner, nearest first, and marks it on its owner's
 * map; switched off, short of power or broken, it lets every chunk go; with its energy turned off by the server it
 * keeps them unpowered and offers no port; its shape, radius, clicks and name change what it keeps, and only a player
 * who may set it up changes them from its screen. Its tests share a batch of their own, apart from the tests that
 * lower the number of chunks an owner may keep.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpacePersistorGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_persistor";
    private static final BlockPos PERSISTOR = new BlockPos(1, 2, 1);
    private static final int SQUARE = 9;
    private static final int SETTLE = 30;
    private static final int TIMEOUT = 300;

    private SpacePersistorGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = TIMEOUT)
    public static void persistor_keepsItsSquareAndLetsItGoWhenSwitchedOff(final GameTestHelper helper) {
        final SpacePersistorBlockEntity persistor = persistor(helper);
        final ServerLevel level = helper.getLevel();
        final BlockPos at = persistor.getBlockPos();
        final UUID owner = UUID.randomUUID();
        persistor.own(Ownership.of(owner));
        persistor.power().generate(SpacePersistorBlockEntity.CAPACITY);
        helper.runAfterDelay(TIMEOUT - 10, () -> ChunkLoaders.releaseAll(level, at));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).size() == SQUARE,
                        "it keeps the three by three round it; got " + ChunkLoaders.loadedFrom(level, at).size()))
                .thenWaitUntil(() -> helper.assertTrue(persistor.screenReadings().keeping(),
                        "its screen reads KEEPING"))
                .thenExecute(() -> {
                    final PersistorReadings read = persistor.screenReadings();
                    helper.assertTrue(read.kept() == SQUARE && read.owners() == SQUARE && !read.full(),
                            "it reads nine kept, nine its owner's, and room for more; got " + read);
                    helper.assertTrue(ChunkLoaders.loadedBy(level.getServer(), owner) == SQUARE,
                            "they are its owner's");
                    helper.assertTrue(MapMarkers.of(level.getServer(), owner).stream().anyMatch(marker -> marker.id()
                            .equals(LoadedChunkMarkers.ownId(level, at)) && marker.area().size() == SQUARE),
                            "they are an area on its owner's map");
                    persistor.control().setOn(false);
                })
                .thenWaitUntil(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).isEmpty(),
                        "switched off, it lets every chunk go"))
                .thenExecute(() -> helper.assertTrue(MapMarkers.of(level.getServer(), owner).stream()
                        .noneMatch(marker -> marker.id().equals(LoadedChunkMarkers.ownId(level, at))),
                        "and its area leaves the map"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = TIMEOUT)
    public static void persistor_shortOfPowerKeepsNothingUnlessTheServerMakesItFree(final GameTestHelper helper) {
        final SpacePersistorBlockEntity persistor = persistor(helper);
        final ServerLevel level = helper.getLevel();
        final BlockPos at = persistor.getBlockPos();
        final int watts = PersistorSettings.wattsPerChunk();
        persistor.own(Ownership.of(UUID.randomUUID()));
        final Runnable restore = () -> {
            PersistorSettings.setWattsPerChunk(watts);
            ChunkLoaders.releaseAll(level, at);
        };
        helper.runAfterDelay(TIMEOUT - 10, restore);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(ChunkLoaders.loadedFrom(level, at).isEmpty(), "with no power it keeps nothing");
                    helper.assertTrue(persistor.status() == MachineStatus.NO_ENERGY, "and says it has no energy; got "
                            + persistor.status());
                    helper.assertTrue(offersEnergy(helper, at), "it offers a port for the power its chunks cost");
                    PersistorSettings.setWattsPerChunk(0);
                })
                .thenWaitUntil(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).size() == SQUARE,
                        "with its energy turned off by the server, it keeps its square unpowered"))
                .thenExecute(() -> {
                    helper.assertTrue(!offersEnergy(helper, at), "and offers no port and no FE on any side");
                    helper.assertTrue(persistor.joulesPerTick() == 0, "its area costs nothing");
                    PersistorSettings.setWattsPerChunk(watts);
                })
                .thenWaitUntil(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).isEmpty(),
                        "when power costs again, short of it, it lets them go"))
                .thenExecute(restore)
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = TIMEOUT)
    public static void persistor_letsEveryChunkGoWhenBroken(final GameTestHelper helper) {
        final SpacePersistorBlockEntity persistor = persistor(helper);
        final ServerLevel level = helper.getLevel();
        final BlockPos at = persistor.getBlockPos();
        persistor.own(Ownership.of(UUID.randomUUID()));
        persistor.power().generate(SpacePersistorBlockEntity.CAPACITY);
        persistor.setShape(PersistorShape.FREE);
        helper.runAfterDelay(TIMEOUT - 10, () -> ChunkLoaders.releaseAll(level, at));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).size() == 1,
                        "free and unclicked, it keeps its own chunk alone"))
                .thenExecute(() -> helper.destroyBlock(PERSISTOR))
                .thenExecute(() -> helper.assertTrue(ChunkLoaders.loadedFrom(level, at).isEmpty(),
                        "broken, it lets it go"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void persistor_shapeRadiusClicksAndNameChangeWhatItKeeps(final GameTestHelper helper) {
        final SpacePersistorBlockEntity persistor = persistor(helper);
        final ChunkPos own = new ChunkPos(persistor.getBlockPos());
        helper.assertTrue(persistor.wantedChunks().size() == SQUARE && persistor.wantedChunks().get(0).equals(own),
                "it starts with the square of radius one, its own chunk first");
        persistor.setShape(PersistorShape.FREE);
        helper.assertTrue(persistor.wantedChunks().equals(List.of(own)), "free, it wants its own alone");
        persistor.click(3, -2);
        helper.assertTrue(persistor.wantedChunks().contains(new ChunkPos(own.x + 3, own.z - 2))
                && persistor.wantedChunks().size() == 2, "a click adds a chunk");
        persistor.setShape(PersistorShape.LINE);
        persistor.setRadius(2);
        helper.assertTrue(persistor.wantedChunks().size() == 6, "a line of five across its facing, and the clicked "
                + "chunk kept; got " + persistor.wantedChunks());
        persistor.setRadius(500);
        helper.assertTrue(persistor.area().radius() == PersistorArea.MOST_RADIUS, "the radius is held at its most");
        persistor.rename("  A name far too long for its little plate  ");
        helper.assertTrue(persistor.name().length() == SpacePersistorBlockEntity.LONGEST_NAME
                && persistor.name().startsWith("A name"), "a name is trimmed and cut; got " + persistor.name());
        persistor.rename("");
        helper.assertTrue(persistor.name().isEmpty(), "an empty name takes it away");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void persistor_screenCallsChangeItOnlyForWhoMaySetItUp(final GameTestHelper helper) {
        final SpacePersistorBlockEntity persistor = persistor(helper);
        final ServerPlayer owner = GameTestPlayers.join(helper, "keeper");
        final ServerPlayer stranger = GameTestPlayers.join(helper, "passerby");
        try {
            persistor.own(Ownership.of(owner.getUUID()));
            for (final ServerPlayer player : new ServerPlayer[] {owner, stranger}) {
                final BlockPos near = persistor.getBlockPos().above();
                player.setPos(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
                player.containerMenu = new MachineMenu(1, player.getInventory(), persistor);
            }
            helper.assertTrue(SpacePersistor.SET_SHAPE.receive(owner, PersistorShape.round().indexOf(
                    PersistorShape.CIRCLE)), "its owner's call is taken");
            SpacePersistor.SET_RADIUS.receive(owner, 4);
            SpacePersistor.CLICK.receive(owner, PersistorShapes.pack(9, 9));
            SpacePersistor.RENAME_TO.receive(owner, "North quarry");
            helper.assertTrue(persistor.area().shape() == PersistorShape.CIRCLE && persistor.area().radius() == 4
                    && persistor.area().added().contains(PersistorShapes.pack(9, 9))
                    && "North quarry".equals(persistor.name()), "its owner sets shape, radius, a click and its name;"
                    + " got " + persistor.area() + ", " + persistor.name());
            SpacePersistor.SET_RADIUS.receive(stranger, 1);
            SpacePersistor.RENAME_TO.receive(stranger, "Mine now");
            helper.assertTrue(persistor.area().radius() == 4 && "North quarry".equals(persistor.name()),
                    "a stranger's calls change nothing");
        } finally {
            owner.containerMenu = owner.inventoryMenu;
            stranger.containerMenu = stranger.inventoryMenu;
            GameTestPlayers.leave(owner);
            GameTestPlayers.leave(stranger);
        }
        helper.succeed();
    }

    /* Whether any side of the block at {@code at} takes energy, a J's Energy port or FE. */
    private static boolean offersEnergy(final GameTestHelper helper, final BlockPos at) {
        return Arrays.stream(Direction.values()).anyMatch(side -> helper.getLevel().getCapability(EnergyPorts.PORT,
                at, side) != null || helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, side)
                != null);
    }

    private static SpacePersistorBlockEntity persistor(final GameTestHelper helper) {
        helper.setBlock(PERSISTOR, SpacePersistor.BLOCK.get());
        if (helper.getBlockEntity(PERSISTOR) instanceof SpacePersistorBlockEntity persistor) {
            return persistor;
        }
        throw new IllegalStateException("no Space Persistor where the test put it");
    }
}
