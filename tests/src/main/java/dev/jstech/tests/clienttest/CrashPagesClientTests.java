/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CrashScreen;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.lwjgl.glfw.GLFW;

/**
 * Every crash page on the glass, as each system drew its own: a row of computers, one per system, each brought down and
 * then looked at, which is also a monitor opened on a page already standing. FreeBSD's is held with a key and let go
 * with the next, and the machine starts over from its self-test.
 */
public final class CrashPagesClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    /** Every machine of the row coming up together, on its solid-state disk. */
    private static final int BOOT_WAIT = 2_400;
    /** How far the counting pages are let run before they are pictured, so the count shows. */
    private static final int COUNTED = 40;
    private static final List<Case> CASES = List.of(
            new Case("frames_95", CrashCause.MEMORY, CrashStyle.FRAMES_95),
            new Case("frames_xp", CrashCause.MEMORY, CrashStyle.FRAMES_XP),
            new Case("frames_7", CrashCause.HARDWARE, CrashStyle.FRAMES_7),
            new Case("frames_10", CrashCause.MEMORY, CrashStyle.FRAMES_10),
            new Case("frames_11", CrashCause.HARDWARE, CrashStyle.FRAMES_11),
            new Case("debian", CrashCause.MEMORY, CrashStyle.CONSOLE),
            new Case("unix", CrashCause.INIT_KILLED, CrashStyle.CONSOLE),
            new Case("freebsd", CrashCause.INIT_KILLED, CrashStyle.CONSOLE));

    private CrashPagesClientTests() {
    }

    /** One computer of the row: its system, why it goes down, and the page that has to come up. */
    private record Case(String system, CrashCause cause, CrashStyle style) {
    }

    @ClientTest(timeoutTicks = 9000)
    public static void everySystem_crashesOntoItsOwnPage(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (int i = 0; i < CASES.size(); i++) {
                world.placeRunningPersonalComputer(computer(i),
                        ResourceLocation.fromNamespaceAndPath("jsc", CASES.get(i).system()));
                world.placeMonitor(monitor(i), Direction.EAST);
            }
        }).thenWaitUntilServer(level -> {
            for (int i = 0; i < CASES.size(); i++) {
                if (!up(machine(ctx, level, i))) {
                    return false;
                }
            }
            return true;
        }, BOOT_WAIT, "every machine of the row to come up", level -> "still coming up");
        for (int i = 0; i < CASES.size(); i++) {
            pictured(ctx, i);
        }
    }

    /* Brings the machine down, sits at it, and pictures its page; FreeBSD's is held and let go on top. */
    private static void pictured(final ClientTestContext ctx, final int i) {
        final Case one = CASES.get(i);
        final String name = one.system() + "-" + one.cause().name().toLowerCase(Locale.ROOT);
        ctx.thenTeleport(SETTLE, desk(i), Direction.WEST)
                .thenServer(SETTLE, level -> ctx.assertTrue(machine(ctx, level, i).crash(one.cause(), moment(one)),
                        one.system() + " should crash"))
                .thenRightClick(SETTLE, monitor(i))
                .thenAwaitScreen(CrashScreen.class, SCREEN_WAIT)
                .thenAssert(0, () -> page(ctx).page().style() == one.style()
                                && page(ctx).page().red() == (one.cause() == CrashCause.HARDWARE
                                && one.style().frames()),
                        one.system() + " should show its own page")
                .thenWaitUntil(() -> page(ctx).percent() >= COUNTED || !counts(one.style()), SCREEN_WAIT,
                        "the page to count far enough to show it counting")
                .thenScreenshot(0, name);
        if (one.system().equals("freebsd")) {
            ctx.then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_SPACE))
                    .thenWaitUntil(() -> page(ctx).held(), SCREEN_WAIT, "a key to stop FreeBSD's reboot")
                    .thenScreenshot(SETTLE, "freebsd-held")
                    .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_SPACE))
                    .thenWaitUntil(() -> !(ctx.mc().screen instanceof CrashScreen), SCREEN_WAIT,
                            "the next key to reboot it")
                    .thenServer(0, level -> ctx.assertTrue(machine(ctx, level, i).needsPost()
                                    || machine(ctx, level, i).booting(),
                            "FreeBSD should start over after the second key"));
        }
        ctx.then(SETTLE, () -> {
            if (ctx.mc().screen != null) {
                ctx.key(GLFW.GLFW_KEY_ESCAPE);
            }
        }).thenAwaitNoScreen(SCREEN_WAIT);
    }

    /* What each machine was doing as it went down. */
    private static CrashPages.Moment moment(final Case one) {
        return switch (one.cause()) {
            case MEMORY -> CrashPages.Moment.memory("soundfoundryd", 412,
                    List.of(new CrashPages.Victim(4211, "paint", 512)));
            case HARDWARE -> CrashPages.Moment.hardware("Stratix Layer DDR3-8192");
            case INIT_KILLED -> CrashPages.Moment.initKilled("kill 1", 15);
        };
    }

    /* Whether the page shows how far along it is: Frames 7's dump and the newer editions' percentage. */
    private static boolean counts(final CrashStyle style) {
        return style == CrashStyle.FRAMES_7 || style == CrashStyle.FRAMES_10 || style == CrashStyle.FRAMES_11;
    }

    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }

    private static CrashScreen page(final ClientTestContext ctx) {
        return ctx.screen(CrashScreen.class);
    }

    private static PersonalComputerBlockEntity machine(final ClientTestContext ctx, final ServerLevel level,
                                                       final int i) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(computer(i), PersonalComputerBlockEntity.class);
    }

    private static BlockPos computer(final int i) {
        return new BlockPos(5, 2, 2 + 3 * i);
    }

    private static BlockPos monitor(final int i) {
        return new BlockPos(6, 2, 2 + 3 * i);
    }

    private static BlockPos desk(final int i) {
        return new BlockPos(8, 2, 2 + 3 * i);
    }
}
