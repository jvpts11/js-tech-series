/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.serialization.Lifecycle;
import dev.jstech.core.registry.StableNamespaces;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The game's warning about experimental settings, kept off the data of namespaces declared stable and only theirs: an
 * entry of one registers as stable, and of another as the game says; a world whose extra dimensions are all of
 * declared namespaces is stable, and one with another mod's dimension is still experimental.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StableNamespaceGameTests {

    private static final String ARENA = "empty";
    private static final String OTHER = "anothermod";
    private static final ResourceKey<Registry<String>> PROBES = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "stability_probes"));

    private StableNamespaceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void registries_takeEntriesOfStableNamespacesAsStable(final GameTestHelper helper) {
        helper.assertTrue(StableNamespaces.isStable(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "a")),
                "the test mod declared its namespace stable");
        final MappedRegistry<String> ours = new MappedRegistry<>(PROBES, Lifecycle.stable());
        ours.register(ResourceKey.create(PROBES, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "probe")),
                "probe", new RegistrationInfo(Optional.empty(), Lifecycle.experimental()));
        helper.assertTrue(ours.registryLifecycle() == Lifecycle.stable(),
                "an entry of a stable namespace registers as stable; the registry is " + ours.registryLifecycle());
        final MappedRegistry<String> theirs = new MappedRegistry<>(PROBES, Lifecycle.stable());
        theirs.register(ResourceKey.create(PROBES, ResourceLocation.fromNamespaceAndPath(OTHER, "probe")),
                "probe", new RegistrationInfo(Optional.empty(), Lifecycle.experimental()));
        helper.assertTrue(theirs.registryLifecycle() != Lifecycle.stable(),
                "another mod's entry registers as the game says");
        final MinecraftServer server = helper.getLevel().getServer();
        final Lifecycle moon = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE)
                .registrationInfo(ResourceKey.create(Registries.DIMENSION_TYPE, TestWorldGen.MOON.id()))
                .map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental());
        helper.assertTrue(moon == Lifecycle.stable(), "the test moon's dimension type, from data, is stable");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dimensions_ofStableNamespacesKeepAWorldStable(final GameTestHelper helper) {
        final LevelStem overworld = helper.getLevel().getServer().registryAccess()
                .registryOrThrow(Registries.LEVEL_STEM).getOrThrow(LevelStem.OVERWORLD);
        final Lifecycle ours = bake(LevelStem.OVERWORLD, overworld, JsTests.MODID);
        helper.assertTrue(ours == Lifecycle.stable(), "a world with a stable namespace's dimension is stable; it is "
                + ours);
        final Lifecycle theirs = bake(LevelStem.OVERWORLD, overworld, OTHER);
        helper.assertTrue(theirs != Lifecycle.stable(), "with another mod's dimension it is still experimental");
        helper.succeed();
    }

    /* A world of the overworld and one dimension more, of {@code namespace}, as the game bakes it when it opens. */
    private static Lifecycle bake(final ResourceKey<LevelStem> key, final LevelStem stem, final String namespace) {
        final ResourceKey<LevelStem> extra = ResourceKey.create(Registries.LEVEL_STEM,
                ResourceLocation.fromNamespaceAndPath(namespace, "extra"));
        // A copy, since a registry takes each value once.
        final WorldDimensions dimensions = new WorldDimensions(Map.of(key, stem, extra,
                new LevelStem(stem.type(), stem.generator())));
        return dimensions.bake(new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable())).lifecycle();
    }
}
