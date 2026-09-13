/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The codecs that carry an enum constant by what it declares: a name in a data file, an id on the wire. Every constant
 * goes out and comes back, and what no constant declares is refused by name and read as the fallback by id.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StableCodecGameTests {

    private StableCodecGameTests() {
    }

    private static final String ARENA = "empty";

    @GameTest(template = ARENA)
    public static void byName_writesEveryConstantAsItsNameAndReadsItBack(final GameTestHelper helper) {
        final Codec<HardwareEra> codec = StableCodecs.byName(HardwareEra.class);
        for (final HardwareEra era : HardwareEra.values()) {
            final JsonElement written = codec.encodeStart(JsonOps.INSTANCE, era).getOrThrow();
            helper.assertTrue(era.serializedName().equals(written.getAsString()),
                    era + " is written as " + written + " instead of its name");
            helper.assertTrue(codec.parse(JsonOps.INSTANCE, written).getOrThrow() == era,
                    era + " does not read back from its name");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void byName_refusesANameNoConstantDeclaresAndSaysWhichExist(final GameTestHelper helper) {
        final DataResult<HardwareEra> read = StableCodecs.byName(HardwareEra.class)
                .parse(JsonOps.INSTANCE, new JsonPrimitive("STANDARD"));
        helper.assertTrue(read.error().isPresent(), "a constant's Java name is not the name it declares");
        helper.assertTrue(read.error().get().message().contains("standard"), "the error lists the names there are");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void byId_sendsEveryConstantAsOneByteAndReadsAStaleByteAsTheFallback(final GameTestHelper helper) {
        final StreamCodec<ByteBuf, OperationPriority> codec =
                StableCodecs.byId(OperationPriority.class, OperationPriority.DEFAULT);
        for (final OperationPriority level : OperationPriority.values()) {
            final ByteBuf buf = Unpooled.buffer();
            codec.encode(buf, level);
            helper.assertTrue(buf.readableBytes() == 1, level + " takes " + buf.readableBytes() + " bytes, not one");
            helper.assertTrue(codec.decode(buf) == level, level + " does not read back from its id");
        }
        final ByteBuf stale = Unpooled.buffer().writeByte(99);
        helper.assertTrue(codec.decode(stale) == OperationPriority.DEFAULT, "an id no level declares reads as the default");
        helper.succeed();
    }
}
