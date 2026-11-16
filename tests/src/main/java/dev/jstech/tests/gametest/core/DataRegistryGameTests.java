/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.data.DataRegistries;
import dev.jstech.core.data.DataRegistryPayload;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestData;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
    private static final ResourceLocation REFUSED = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "refused");
    /* A registry of forty values of sixty-four thousand bytes each: more than one message's tag may hold. */
    private static final int BIG_VALUES = 40;
    private static final int BIG_VALUE_INTS = 16_384;

    private DataRegistryGameTests() {
    }

    /*
     * A synced registry bigger than one message's tag may be, as the formed shapes of many machines are, still reaches
     * a joining player's game whole: sent as it is, the message's tag was refused as it was read, and the player was
     * dropped from the server.
     */
    @GameTest(template = ARENA)
    public static void dataRegistry_tooBigForOneMessageStillReachesThePlayerWhole(final GameTestHelper helper) {
        final ResourceLocation registry = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "big");
        final CompoundTag entries = new CompoundTag();
        for (int value = 0; value < BIG_VALUES; value++) {
            final CompoundTag each = new CompoundTag();
            each.putIntArray("boxes", new int[BIG_VALUE_INTS]);
            entries.put(JsTests.MODID + ":value_" + value, each);
        }
        final CompoundTag read = new CompoundTag();
        final List<DataRegistryPayload> sent = DataRegistryPayload.parts(registry, entries);
        for (final DataRegistryPayload part : sent) {
            final RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                    helper.getLevel().registryAccess());
            DataRegistryPayload.STREAM_CODEC.encode(wire, part);
            read.merge(DataRegistryPayload.STREAM_CODEC.decode(wire).entries());
        }
        helper.assertTrue(sent.size() > 1 && sent.getLast().last(), "it goes in parts, the last saying so: "
                + sent.size());
        helper.assertTrue(read.getAllKeys().equals(entries.getAllKeys()), "every value reaches the player's game: "
                + read.size() + " of " + entries.size());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dataRegistry_isReadWhenTheServerLoadsItsData(final GameTestHelper helper) {
        helper.assertTrue(TestData.NOTES.get(FIRST).equals(Optional.of(new TestData.Shade("first", 1))),
                "the first note was read: " + TestData.NOTES.entries());
        helper.assertTrue(TestData.NOTES.get(SECOND).isPresent(), "and the second");
        helper.assertTrue(TestData.NOTES.get(BROKEN).isEmpty(), "a file that does not read is left out");
        helper.assertTrue(TestData.NOTES.get(REFUSED).isEmpty(), "and one whose value refuses itself as it is made");
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
