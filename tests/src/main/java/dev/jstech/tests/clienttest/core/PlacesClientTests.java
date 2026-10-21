/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.map.ClientMarkers;
import dev.jstech.core.client.gui.component.MapView;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.map.Marker;
import dev.jstech.core.text.GameText;
import dev.jstech.core.worldgen.place.Landmarks;
import dev.jstech.core.worldgen.structure.SettlementPlan;
import dev.jstech.core.worldgen.structure.Settlements;
import dev.jstech.core.worldgen.structure.SpreadPlacement;
import dev.jstech.core.worldgen.structure.StructurePlacer;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPlaces;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

/**
 * The Core's places as a player sees them, to set beside what they should look like: a hut of the test land worn by
 * rule, standing on its foundations; a town at its first stage and again grown two days on; and markers reaching the
 * player's game, the landmark they discovered among them, as the map mods and the series' maps are handed them.
 */
public final class PlacesClientTests {

    private static final ResourceKey<Level> LAND = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "places_land"));
    private static final int ARRIVE = 600;
    /* Ticks for block changes made on the server to reach the player's game. */
    private static final int CHANGES = 20;
    /* Ticks after the land is drawn before the picture, for a frame of it to be shown. */
    private static final int SHOOT = 5;
    private static final int WAIT = 200;
    private static final long NOON = 6_000L;
    private static final int CHUNKS_SEARCHED = 32;
    /* How far off and above a structure the player looks at it from. */
    private static final int OFF = 6;
    private static final int UP = 3;
    private static final int TOWN_UP = 36;

    private PlacesClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void testLand_showsAWornHutOnItsFoundations(final ClientTestContext ctx) {
        final StructureStart[] hut = new StructureStart[1];
        final Vec3[] stand = new Vec3[1];
        ctx.afterTest(() -> leave(ctx));
        ctx.thenServer(0, level -> {
            final ServerLevel land = RuntimeDimensions.getOrCreate(level.getServer(), LAND,
                    TestWorldGen.TEST_LAND.id());
            level.getServer().overworld().setDayTime(NOON);
            hut[0] = placeWhereSpread(land, TestWorldGen.HUT.id());
            if (hut[0] != null) {
                // The hut's own piece: the structure's box takes in the ground shaped round it besides.
                final BoundingBox box = hut[0].getPieces().getFirst().getBoundingBox();
                stand[0] = new Vec3(box.maxX() + OFF + 0.5, box.minY() + UP, box.getCenter().getZ() + 0.5);
                go(ctx, land, stand[0]);
            }
        })
                .thenAssert(1, () -> hut[0] != null, "a hut stands in the test land")
                .thenWaitUntil(() -> arrived(ctx, stand[0], hut[0].getBoundingBox().getCenter()), ARRIVE,
                        "the player's game to reach the hut")
                .then(1, () -> ctx.lookAt(Vec3.atCenterOf(hut[0].getPieces().getFirst().getBoundingBox()
                        .getCenter())))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "worn_hut_on_its_foundations")
                .then(1, () -> {
                    final BoundingBox box = hut[0].getPieces().getFirst().getBoundingBox();
                    int birch = 0;
                    for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(),
                            box.maxY(), box.maxZ())) {
                        birch += ctx.mc().level.getBlockState(at).is(Blocks.BIRCH_PLANKS) ? 1 : 0;
                    }
                    if (birch == 0) {
                        throw new ClientTestFailure("the hut shows no worn planks on the player's game");
                    }
                });
    }

    @ClientTest(timeoutTicks = 3200)
    public static void testLand_showsATownAtItsFirstStageAndGrown(final ClientTestContext ctx) {
        final StructureStart[] town = new StructureStart[1];
        final BlockPos[] middle = new BlockPos[1];
        final Vec3[] stand = new Vec3[1];
        final String[] key = new String[1];
        final boolean[] grown = new boolean[1];
        ctx.afterTest(() -> leave(ctx));
        ctx.thenServer(0, level -> {
            final ServerLevel land = RuntimeDimensions.getOrCreate(level.getServer(), LAND,
                    TestWorldGen.TEST_LAND.id());
            level.getServer().overworld().setDayTime(NOON);
            town[0] = placeWhereSpread(land, TestWorldGen.TOWN.id());
            if (town[0] == null) {
                return;
            }
            BoundingBox whole = town[0].getBoundingBox();
            for (final StructurePiece piece : town[0].getPieces()) {
                if (piece instanceof SettlementPlan plan) {
                    for (final SettlementPlan.Planned later : plan.planned()) {
                        whole = BoundingBox.encapsulatingBoxes(List.of(whole, later.piece().getBoundingBox()))
                                .orElse(whole);
                    }
                }
            }
            for (int cx = whole.minX() >> 4; cx <= whole.maxX() >> 4; cx++) {
                for (int cz = whole.minZ() >> 4; cz <= whole.maxZ() >> 4; cz++) {
                    land.getChunk(cx, cz);
                }
            }
            key[0] = TestWorldGen.TOWN.id() + "@" + town[0].getChunkPos().x + "," + town[0].getChunkPos().z;
            middle[0] = new BlockPos(whole.getCenter().getX(), town[0].getBoundingBox().maxY(),
                    whole.getCenter().getZ());
            stand[0] = new Vec3(middle[0].getX() + 0.5, middle[0].getY() + TOWN_UP, middle[0].getZ() + TOWN_UP);
            go(ctx, land, stand[0]);
        })
                .thenAssert(1, () -> town[0] != null, "a town stands in the test land")
                .thenWaitUntil(() -> arrived(ctx, stand[0], middle[0]), ARRIVE, "the player's game to reach the town")
                .then(1, () -> ctx.lookAt(Vec3.atCenterOf(middle[0])))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "town_at_its_first_stage")
                .thenServer(1, level -> {
                    final ServerLevel land = level.getServer().getLevel(LAND);
                    grown[0] = land != null && Settlements.advance(land, key[0], 2)
                            && Settlements.of(land).get(key[0]).standing() == TestWorldGen.TOWN.growth().get()
                            .stages();
                })
                .thenAssert(1, () -> grown[0], "two days on, the town stands at its last stage")
                // The buildings put up reach the player's game as block changes first, then are drawn.
                .then(CHANGES, () -> ctx.lookAt(Vec3.atCenterOf(middle[0])))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "town_grown_two_days_on");
    }

    @ClientTest(timeoutTicks = 1200)
    public static void markers_reachThePlayersGameWithTheLandmarksTheyFound(final ClientTestContext ctx) {
        final String id = "jstests_marker_test";
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            MapMarkers.hide(ctx.server(), player.getUUID(), id);
            Landmarks.forget(ctx.server(), player.getUUID(), TestPlaces.SPIRE);
        }));
        ctx.thenServer(0, level -> {
            final ServerPlayer player = ctx.serverPlayer();
            MapMarkers.show(player, Marker.point(id, TestPlaces.RUINS.id(), player.level().dimension(),
                    player.blockPosition().east(8), GameText.component(TestPlaces.RUINS_NAME.text())));
            Landmarks.discover(player, TestPlaces.SPIRE);
        })
                .thenWaitUntil(() -> shown(id).isPresent() && shown(TestPlaces.SPIRE.toString()).isPresent(), WAIT,
                        "the marker and the landmark to reach the player's game")
                .then(1, () -> {
                    final Marker ruin = shown(id).get();
                    final Marker spire = shown(TestPlaces.SPIRE.toString()).get();
                    ctx.assertTrue(spire.layer().equals(MapMarkers.LANDMARKS.id()) && spire.pos().equals(new BlockPos(
                            TestPlaces.SPIRE_X, TestPlaces.SPIRE_Y, TestPlaces.SPIRE_Z)),
                            "the landmark found is a marker of the landmarks' layer, at its place");
                    final List<MapView.Marker> drawn = ClientMarkers.forMapView();
                    ctx.assertTrue(drawn.stream().anyMatch(mark -> mark.pos().equals(ruin.pos())
                            && mark.colour() == (0xFF << 24 | TestPlaces.RUINS.colour())),
                            "the series' maps draw the marker in its layer's colour");
                })
                .thenServer(1, level -> MapMarkers.hide(level.getServer(), ctx.serverPlayer().getUUID(), id))
                .thenWaitUntil(() -> shown(id).isEmpty(), WAIT, "the hidden marker to leave the player's game");
    }

    private static Optional<Marker> shown(final String id) {
        return ClientMarkers.shown().stream().filter(marker -> marker.id().equals(id)).findFirst();
    }

    /* Places the structure at the first chunk round the origin its spread picks where it stands; null if none. */
    private static StructureStart placeWhereSpread(final ServerLevel land, final ResourceLocation id) {
        final ChunkGeneratorStructureState state = land.getChunkSource().getGeneratorState();
        final SpreadPlacement spread = (SpreadPlacement) land.registryAccess().registryOrThrow(
                Registries.STRUCTURE_SET).get(id).placement();
        for (int x = -CHUNKS_SEARCHED; x <= CHUNKS_SEARCHED; x++) {
            for (int z = -CHUNKS_SEARCHED; z <= CHUNKS_SEARCHED; z++) {
                if (spread.isStructureChunk(state, x, z)) {
                    final Optional<StructureStart> placed = StructurePlacer.place(land, id, new ChunkPos(x, z));
                    if (placed.isPresent()) {
                        return placed.get();
                    }
                }
            }
        }
        return null;
    }

    private static void leave(final ClientTestContext ctx) {
        ctx.server().execute(() -> {
            final BlockPos home = ctx.origin().above(2);
            ctx.serverPlayer().teleportTo(ctx.server().overworld(), home.getX() + 0.5, home.getY(),
                    home.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), LAND);
        });
    }

    private static void go(final ClientTestContext ctx, final ServerLevel land, final Vec3 at) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(land, at.x, at.y, at.z, 90F, 35F);
    }

    private static boolean arrived(final ClientTestContext ctx, final Vec3 at, final BlockPos look) {
        return ctx.mc().level != null && ctx.mc().level.dimension().equals(LAND) && ctx.player() != null
                && ctx.player().position().distanceTo(at) < 2.0
                && !ctx.mc().level.getChunk(look.getX() >> 4, look.getZ() >> 4).isEmpty();
    }
}
