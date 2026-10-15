/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.codec.NetworkCodecs;
import dev.jstech.core.codec.RecordCodecs;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A record's codec written one line a component: it writes and reads the record back, reads a left-out optional field
 * as its fallback, says which field it could not read, refuses fields that are not the record's, takes more than the
 * game's own builder does; and the network codec taken from it carries the same value.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CodecKitGameTests {

    private static final String ARENA = "empty";
    private static final Codec<Setting> CODEC = RecordCodecs.of(Setting.class)
            .field("name", Codec.STRING)
            .field("level", Codec.INT)
            .optional("shown", Codec.BOOL, true)
            .build();

    private CodecKitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void build_writesAndReadsTheRecordBack(final GameTestHelper helper) {
        final Setting setting = new Setting("speed", 3, false);
        final JsonElement written = CODEC.encodeStart(JsonOps.INSTANCE, setting).getOrThrow();

        helper.assertTrue(setting.equals(CODEC.parse(JsonOps.INSTANCE, written).getOrThrow()),
                "the setting comes back as it was written");
        helper.assertTrue(written.getAsJsonObject().get("level").getAsInt() == 3, "under the component's own name");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void optional_readsTheFallbackWhereTheFieldIsLeftOut(final GameTestHelper helper) {
        final JsonObject written = new JsonObject();
        written.addProperty("name", "speed");
        written.addProperty("level", 1);

        helper.assertTrue(new Setting("speed", 1, true).equals(CODEC.parse(JsonOps.INSTANCE, written).getOrThrow()),
                "a setting written without shown is shown");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void decode_namesTheFieldItCouldNotRead(final GameTestHelper helper) {
        final JsonObject written = new JsonObject();
        written.addProperty("name", "speed");
        final DataResult<Setting> read = CODEC.parse(JsonOps.INSTANCE, written);

        helper.assertTrue(read.error().isPresent() && read.error().get().message().contains("Setting.level"),
                "the error names the field that was missing; got " + read);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void build_refusesFieldsThatAreNotTheRecordsInOrder(final GameTestHelper helper) {
        helper.assertTrue(refused(() -> RecordCodecs.of(Setting.class).field("level", Codec.INT)
                .field("name", Codec.STRING).field("shown", Codec.BOOL).build()), "fields out of order are refused");
        helper.assertTrue(refused(() -> RecordCodecs.of(Setting.class).field("name", Codec.STRING).build()),
                "and so are too few");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void build_takesMoreComponentsThanTheGamesOwnBuilder(final GameTestHelper helper) {
        final Codec<Wide> codec = RecordCodecs.of(Wide.class)
                .field("a", Codec.INT).field("b", Codec.INT).field("c", Codec.INT).field("d", Codec.INT)
                .field("e", Codec.INT).field("f", Codec.INT).field("g", Codec.INT).field("h", Codec.INT)
                .field("i", Codec.INT).field("j", Codec.INT).field("k", Codec.INT).field("l", Codec.INT)
                .field("m", Codec.INT).field("n", Codec.INT).field("o", Codec.INT).field("p", Codec.INT)
                .field("q", Codec.INT)
                .build();
        final Wide wide = new Wide(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17);

        helper.assertTrue(wide.equals(codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, wide)
                .getOrThrow()).getOrThrow()), "a record of seventeen components comes back whole");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fromSave_carriesTheValueOverTheNetwork(final GameTestHelper helper) {
        final StreamCodec<RegistryFriendlyByteBuf, Setting> network = NetworkCodecs.fromSave(CODEC);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        final Setting setting = new Setting("speed", 4, false);

        network.encode(buffer, setting);
        helper.assertTrue(setting.equals(network.decode(buffer)), "the setting arrives as it was saved");
        helper.succeed();
    }

    private static boolean refused(final Runnable declaration) {
        try {
            declaration.run();
            return false;
        } catch (final DeclarationError expected) {
            return true;
        }
    }

    private record Setting(String name, int level, boolean shown) {
    }

    private record Wide(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m,
                        int n, int o, int p, int q) {
    }
}
