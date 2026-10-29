/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.serialization.JsonOps;
import dev.jstech.space.rocket.CabinLamp;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.tests.JsTests;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;

/**
 * What a rocket's cabin reads of its flight: the clock in minutes and seconds, then hours and minutes, signed; the
 * climb's strip still at its middle; every share kept within its ends; the lamps lit; the attitude read from a turn,
 * heading first; and the readings written and sent whole.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CabinReadingsGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_cabin_readings";
    private static final float CLOSE = 1e-3F;

    private CabinReadingsGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void timer_showsMinutesAndSecondsThenHoursAndMinutesSigned(final GameTestHelper helper) {
        check(helper, "-00:10", clock(-10).timer());
        check(helper, "+00:00", clock(0).timer());
        check(helper, "+12:34", clock(12 * 60 + 34).timer());
        check(helper, "+99:59", clock(99 * 60 + 59).timer());
        check(helper, "+01:40", clock(100 * 60).timer());
        check(helper, "+31:24", clock(31 * 3600 + 24 * 60 + 59).timer());
        check(helper, "-02:05", clock(-(2 * 3600 + 5 * 60)).timer());
        check(helper, "+99:59", clock(500 * 3600).timer());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void climbShare_isTheMiddleStillAndItsEndsAtTheScale(final GameTestHelper helper) {
        near(helper, 0.5F, climbing(0).climbShare(), "still");
        near(helper, 1.0F, climbing(CabinReadings.CLIMB_SCALE).climbShare(), "climbing at the scale");
        near(helper, 0.0F, climbing(-CabinReadings.CLIMB_SCALE).climbShare(), "falling at the scale");
        near(helper, 0.75F, climbing(CabinReadings.CLIMB_SCALE / 2).climbShare(), "climbing at half the scale");
        near(helper, 1.0F, climbing(10 * CabinReadings.CLIMB_SCALE).climbShare(), "climbing past the scale");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void readings_keepEveryShareWithinItsEndsAndTellTheLampsLit(final GameTestHelper helper) {
        final CabinReadings read = new CabinReadings(0, 1.5F, -0.2F, 0, 2, 0, 0, 0, 5, Float.NaN,
                Set.of(CabinLamp.ENGINE, CabinLamp.MASTER_ALARM));
        near(helper, 1, read.fuel(), "fuel");
        near(helper, 0, read.oxidiser(), "oxidiser");
        near(helper, 1, read.throttle(), "throttle");
        near(helper, 2, read.air(), "air");
        near(helper, 0, read.propellant(), "propellant");
        helper.assertTrue(read.lit(CabinLamp.ENGINE) && read.lit(CabinLamp.MASTER_ALARM)
                && !read.lit(CabinLamp.STAGE), "the lamps lit are the ones it was given: " + read.lit());
        helper.assertTrue(CabinReadings.AT_REST.lit().isEmpty(), "at rest, no lamp is lit");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void attitude_isReadHeadingFirstThenPitchThenRoll(final GameTestHelper helper) {
        final CabinReadings.Attitude still = CabinReadings.attitude(new Quaternionf());
        near(helper, 0, still.pitch() + still.roll() + still.heading(), "unturned");
        final CabinReadings.Attitude turned = CabinReadings.attitude(new Quaternionf()
                .rotateY((float) Math.toRadians(30)).rotateX((float) Math.toRadians(20))
                .rotateZ((float) Math.toRadians(-10)));
        near(helper, 30, turned.heading(), "heading");
        near(helper, 20, turned.pitch(), "pitch");
        near(helper, -10, turned.roll(), "roll");
        final CabinReadings moved = clock(42).turned(turned);
        helper.assertTrue(moved.clock() == 42 && Math.abs(moved.pitch() - 20) < CLOSE,
                "turned, the rest of its readings stay: " + moved);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void readings_areWrittenAndSentWhole(final GameTestHelper helper) {
        final CabinReadings read = new CabinReadings(-30, 0.25F, 0.5F, 12, 0.85F, 4, -5, 90, 0.9F, 0.6F,
                Set.of(CabinLamp.GUIDANCE, CabinLamp.LEGS, CabinLamp.DOCKED));
        final CabinReadings written = CabinReadings.CODEC.parse(JsonOps.INSTANCE, CabinReadings.CODEC
                .encodeStart(JsonOps.INSTANCE, read).getOrThrow()).getOrThrow();
        helper.assertTrue(written.equals(read), "written and read again, the same: " + written);
        final ByteBuf buf = Unpooled.buffer();
        CabinReadings.STREAM_CODEC.encode(buf, read);
        final CabinReadings sent = CabinReadings.STREAM_CODEC.decode(buf);
        helper.assertTrue(sent.equals(read), "sent and received, the same: " + sent);
        helper.succeed();
    }

    private static void check(final GameTestHelper helper, final String wanted, final String shown) {
        helper.assertTrue(wanted.equals(shown), "the clock shows " + wanted + ", not " + shown);
    }

    private static void near(final GameTestHelper helper, final float wanted, final float got, final String what) {
        helper.assertTrue(Math.abs(wanted - got) < CLOSE, what + ": " + wanted + " wanted, " + got);
    }

    private static CabinReadings clock(final int seconds) {
        return new CabinReadings(seconds, 1, 1, 0, 0, 0, 0, 0, 1, 1, Set.of());
    }

    private static CabinReadings climbing(final float blocksASecond) {
        return new CabinReadings(0, 1, 1, blocksASecond, 0, 0, 0, 0, 1, 1, Set.of());
    }
}
