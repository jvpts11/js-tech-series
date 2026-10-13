/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.data.DataRegistries;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestData;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Content from datapacks: notes read from the test mod's JSON files when the server loaded its data and again on a
 * reload, a file that does not read left out, and a registry of shades a datapack filled.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DataRegistryGameTests {

    private static final String ARENA = "empty";
    private static final ResourceLocation FIRST = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "first");
    private static final ResourceLocation SECOND = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "second");
    private static final ResourceLocation BROKEN = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "broken");

    private DataRegistryGameTests() {
    }

    @GameTest(template = ARENA)
    public static void dataRegistry_isReadWhenTheServerLoadsItsData(final GameTestHelper helper) {
        helper.assertTrue(TestData.NOTES.get(FIRST).equals(Optional.of(new TestData.Shade("first", 1))),
                "the first note was read: " + TestData.NOTES.entries());
        helper.assertTrue(TestData.NOTES.get(SECOND).isPresent(), "and the second");
        helper.assertTrue(TestData.NOTES.get(BROKEN).isEmpty(), "a file that does not read is left out");
        helper.assertTrue(DataRegistries.all().contains(TestData.NOTES), "the notes are among the declared registries");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dataRegistry_readsItsFilesAgainOnAReload(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final long version = TestData.NOTES.version();
        final int heard = TestData.NOTE_READS.get();

        TestData.NOTES.reload(server.getResourceManager(), server.registryAccess());

        helper.assertTrue(TestData.NOTES.version() > version, "the reload read the files again");
        helper.assertTrue(TestData.NOTE_READS.get() > heard, "what listens heard the new values");
        helper.assertTrue(TestData.NOTES.entries().size() == 2, "the same two notes: " + TestData.NOTES.entries());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void datapackRegistry_holdsWhatTheDatapackPutInIt(final GameTestHelper helper) {
        final TestData.Shade teal = helper.getLevel().registryAccess().registryOrThrow(TestData.SHADES)
                .get(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "teal"));

        helper.assertTrue(teal != null && teal.equals(new TestData.Shade("teal", 3381657)),
                "the datapack's teal is in the registry: " + teal);
        helper.succeed();
    }
}
