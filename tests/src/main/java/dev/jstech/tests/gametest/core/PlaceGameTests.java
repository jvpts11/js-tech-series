/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.map.Marker;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.GameText;
import dev.jstech.core.worldgen.place.Finding;
import dev.jstech.core.worldgen.place.Landmark;
import dev.jstech.core.worldgen.place.Landmarks;
import dev.jstech.core.worldgen.structure.KnowledgeSealBlock;
import dev.jstech.core.worldgen.structure.OptionalItemEntry;
import dev.jstech.core.worldgen.structure.StructurePlacer;
import dev.jstech.core.worldgen.structure.StructureStates;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPlaces;
import dev.jstech.tests.TestWorldGen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's places: a landmark discovered by coming near, once, for the player and their team; finding a thing of a
 * kind at a precision, roughly at first and exactly later, never further off than it says and the same each time;
 * markers shown to a player or shared with their team; a seal that opens only to a team that knows what it asks; loot
 * of another mod's item only where that mod is installed; and what a structure remembers of itself.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PlaceGameTests {

    private static final String ARENA = "empty";
    private static final int PLACES_TRIED = 200;
    /* How many places along the test land's diagonal a hut is tried at, until it stands. */
    private static final int SPOTS = 40;

    private PlaceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void landmark_isDiscoveredOnceByComingNear(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MinecraftServer server = level.getServer();
        final Landmark spire = Landmarks.get(level, TestPlaces.SPIRE).orElse(null);
        helper.assertTrue(spire != null && spire.pos().equals(new BlockPos(TestPlaces.SPIRE_X, TestPlaces.SPIRE_Y,
                TestPlaces.SPIRE_Z)), "the spire is declared where it stands");
        helper.assertTrue(spire.near(Level.OVERWORLD, spire.pos().east(TestPlaces.SPIRE_RADIUS - 2))
                        && !spire.near(Level.OVERWORLD, spire.pos().east(TestPlaces.SPIRE_RADIUS + 2))
                        && !spire.near(Level.NETHER, spire.pos()),
                "it is near within its radius, in its own dimension");
        final ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "Surveyor"));
        final AtomicInteger heard = new AtomicInteger();
        final Consumer<Landmarks.Discovered> listener = event -> {
            if (event.player() == player && event.id().equals(TestPlaces.SPIRE)) {
                heard.incrementAndGet();
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, Landmarks.Discovered.class, listener);
        try {
            helper.assertTrue(!Landmarks.discovered(server, player.getUUID()).contains(TestPlaces.SPIRE),
                    "a new player has discovered nothing");
            helper.assertTrue(Landmarks.discover(player, TestPlaces.SPIRE), "coming near discovers it");
            helper.assertTrue(!Landmarks.discover(player, TestPlaces.SPIRE), "once");
            helper.assertTrue(Landmarks.discovered(server, player.getUUID()).contains(TestPlaces.SPIRE),
                    "and it is noted");
            helper.assertTrue(heard.get() == 1, "and told on the game's bus, once");
            helper.assertTrue(Landmarks.discovered(server, UUID.randomUUID()).isEmpty(),
                    "another player has not discovered it");
            helper.assertTrue(!Landmarks.discover(player, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                    "nowhere")), "a landmark no mod declared is not discovered");
            Landmarks.forget(server, player.getUUID(), TestPlaces.SPIRE);
            helper.assertTrue(!Landmarks.discovered(server, player.getUUID()).contains(TestPlaces.SPIRE)
                    && Landmarks.discover(player, TestPlaces.SPIRE), "a landmark forgotten is discovered again");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void finding_isRoughThenExactNeverFurtherOffThanItSays(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos spire = new BlockPos(TestPlaces.SPIRE_X, TestPlaces.SPIRE_Y, TestPlaces.SPIRE_Z);
        final Optional<Finding.Found> exact = Finding.find(level, BlockPos.ZERO,
                new Finding.LandmarkTarget(TestPlaces.SPIRE), Finding.EXACT);
        helper.assertTrue(exact.isPresent() && exact.get().near().equals(spire) && exact.get().within() == 0,
                "at its finest it finds the spire's own place");
        final Optional<Finding.Found> rough = Finding.find(level, BlockPos.ZERO,
                new Finding.LandmarkTarget(TestPlaces.SPIRE), 0);
        helper.assertTrue(rough.isPresent() && rough.get().within() == Finding.within(0)
                && flat(rough.get().near(), spire) <= Finding.within(0), "roughly, within what it says");
        helper.assertTrue(Finding.within(1) == Finding.within(0) / 2, "each level of precision halves the distance");
        boolean alike = true;
        boolean within = true;
        for (int i = 0; i < PLACES_TRIED; i++) {
            final BlockPos place = new BlockPos(i * 37 - 3000, 64, i * -53 + 1000);
            final int precision = i % Finding.EXACT;
            final Finding.Found once = Finding.blur(place, precision);
            alike &= once.equals(Finding.blur(place, precision));
            within &= flat(once.near(), place) <= Finding.within(precision);
        }
        helper.assertTrue(alike, "a rough answer is the same each time");
        helper.assertTrue(within, "and never further off than it says");
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION, ResourceLocation
                .fromNamespaceAndPath(JsTests.MODID, "finding_" + UUID.randomUUID().toString().substring(0, 8)));
        final ServerLevel land = RuntimeDimensions.getOrCreate(level.getServer(), copy, TestWorldGen.TEST_LAND.id());
        try {
            helper.assertTrue(Finding.find(land, BlockPos.ZERO, new Finding.LandmarkTarget(TestPlaces.SPIRE),
                    Finding.EXACT).isEmpty(), "a landmark of another dimension is not found");
            helper.assertTrue(Finding.find(land, BlockPos.ZERO, new Finding.VeinTarget(TestWorldGen.IRON_VEINS),
                    Finding.EXACT).isPresent(), "a vein of the test land's iron is found");
            helper.assertTrue(Finding.find(land, BlockPos.ZERO, new Finding.BiomeTarget(TestWorldGen.LAND), 4)
                    .isPresent(), "and its biomes, though the search starts below the world");
        } finally {
            RuntimeDimensions.remove(level.getServer(), copy);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void markers_areShownToAPlayerOrSharedWithTheirTeam(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MinecraftServer server = level.getServer();
        final ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "Cartographer"));
        final Component label = GameText.component(TestPlaces.RUINS_NAME.text());
        final Marker ruin = Marker.point("test_ruin", TestPlaces.RUINS.id(), Level.OVERWORLD, new BlockPos(10, 64, 10),
                label);
        MapMarkers.show(player, ruin);
        MapMarkers.show(player, Marker.point("test_ruin", TestPlaces.RUINS.id(), Level.OVERWORLD,
                new BlockPos(20, 64, 20), label));
        helper.assertTrue(MapMarkers.of(server, player.getUUID()).size() == 1 && MapMarkers.of(server,
                player.getUUID()).getFirst().pos().equals(new BlockPos(20, 64, 20)),
                "a marker of the same id takes the place of the old");
        final Marker area = Marker.area("test_claim", TestPlaces.RUINS.id(), Level.OVERWORLD,
                List.of(new ChunkPos(0, 0), new ChunkPos(1, 0)), label);
        MapMarkers.share(server, player.getUUID(), area);
        helper.assertTrue(MapMarkers.of(server, player.getUUID()).size() == 2, "a shared marker joins their own");
        helper.assertTrue(area.isArea() && area.pos().getX() == 16 && area.colourIn(TestPlaces.RUINS) == 0xC08040
                && area.coloured(0x102030).colourIn(TestPlaces.RUINS) == 0x102030,
                "an area's marker lies at its middle, in its layer's colour or its own");
        MapMarkers.hide(server, player.getUUID(), "test_ruin");
        MapMarkers.hide(server, player.getUUID(), "test_claim");
        helper.assertTrue(MapMarkers.of(server, player.getUUID()).isEmpty(), "and hiding takes them off");
        helper.assertTrue(MapMarkers.layerOf(TestPlaces.RUINS.id()) == TestPlaces.RUINS
                && MapMarkers.layerOf(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "nowhere"))
                == MapMarkers.FOUND, "a layer is known by its id, an unknown one shown as found places");
        helper.assertTrue(MapMarkers.layers().indexOf(MapMarkers.LANDMARKS) < MapMarkers.layers()
                .indexOf(TestPlaces.RUINS), "the layers keep the order they were declared in");
        final JsonElement written = Marker.CODEC.encodeStart(JsonOps.INSTANCE, area).getOrThrow();
        helper.assertTrue(Marker.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow().equals(area),
                "a marker reads back as it was kept");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void seal_opensOnlyToATeamThatKnowsWhatItAsks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MinecraftServer server = level.getServer();
        final BlockPos at = new BlockPos(1, 2, 1);
        final BlockPos open = new BlockPos(3, 2, 1);
        helper.setBlock(at, TestPlaces.SEAL.get());
        helper.setBlock(open, TestPlaces.SEAL.get());
        final KnowledgeSealBlock.Entity seal = helper.getBlockEntity(at);
        seal.ask(TestPlaces.LORE.id());
        final CompoundTag kept = seal.saveWithoutMetadata(level.registryAccess());
        helper.assertTrue(kept.getString("knowledge").equals(TestPlaces.LORE.id().toString()),
                "what a seal asks is kept as a template sets it");
        final ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "Scholar"));
        final String team = CoreTeams.teamOf(server, player.getUUID());
        try {
            helper.useBlock(at, player);
            helper.assertBlockPresent(TestPlaces.SEAL.get(), at);
            helper.useBlock(open, player);
            helper.assertBlockPresent(Blocks.AIR, open);
            TeamProgress.grant(server, team, TestPlaces.LORE, 1.0);
            helper.useBlock(at, player);
            helper.assertBlockPresent(Blocks.AIR, at);
        } finally {
            TeamProgress.forget(server, team, TestPlaces.LORE.id());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void optionalItem_isLootOnlyWhereItsModIsInstalled(final GameTestHelper helper) {
        final LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
        final List<ItemStack> present = LootTable.lootTable().withPool(LootPool.lootPool()
                .add(OptionalItemEntry.of(ResourceLocation.withDefaultNamespace("diamond")))).build()
                .getRandomItems(params);
        helper.assertTrue(present.size() == 1 && present.getFirst().is(Items.DIAMOND),
                "an item that exists is given");
        final List<ItemStack> absent = LootTable.lootTable().withPool(LootPool.lootPool()
                .add(OptionalItemEntry.of(ResourceLocation.fromNamespaceAndPath("absentmod", "titanium_ingot"))))
                .build().getRandomItems(params);
        helper.assertTrue(absent.isEmpty(), "one of a mod not installed gives nothing, and breaks nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void structureStates_rememberWhatHappenedInAStructure(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION, ResourceLocation
                .fromNamespaceAndPath(JsTests.MODID, "states_" + UUID.randomUUID().toString().substring(0, 8)));
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        final AtomicInteger heard = new AtomicInteger();
        final Consumer<StructureStates.Changed> listener = event -> {
            if (event.level() == land) {
                heard.incrementAndGet();
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, StructureStates.Changed.class, listener);
        try {
            StructureStart start = null;
            for (int x = 4; x < SPOTS && start == null; x++) {
                start = StructurePlacer.place(land, TestWorldGen.HUT.id(), new ChunkPos(x, x)).orElse(null);
            }
            helper.assertTrue(start != null, "a hut stands in the test land");
            final BlockPos middle = start.getBoundingBox().getCenter();
            helper.assertTrue(StructureStates.at(land, middle.above(start.getBoundingBox().getYSpan())).isEmpty(),
                    "a place above the hut is in no structure");
            final Optional<StructureStates.Placed> hut = StructureStates.at(land, middle);
            helper.assertTrue(hut.isPresent() && hut.get().structure().equals(TestWorldGen.HUT.id()),
                    "a place inside the hut is known to be in it");
            helper.assertTrue(StructureStates.get(land, hut.get()).isEmpty(), "which remembers nothing yet");
            StructureStates.update(land, hut.get(), state -> {
                state.putBoolean("awake", true);
                return state;
            });
            helper.assertTrue(StructureStates.get(land, hut.get()).getBoolean("awake"), "and then what it was told");
            StructureStates.update(land, hut.get(), state -> state);
            helper.assertTrue(heard.get() == 1, "each change told on the game's bus, and only a change");
            helper.assertTrue(StructureStates.STATES.containing(land, middle).stream()
                    .anyMatch(entry -> entry.key().equals(hut.get().key())),
                    "kept over the hut's box, for every mod to read by place");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    private static long flat(final BlockPos a, final BlockPos b) {
        final long x = a.getX() - b.getX();
        final long z = a.getZ() - b.getZ();
        return (long) Math.ceil(Math.sqrt((double) (x * x + z * z)));
    }
}
