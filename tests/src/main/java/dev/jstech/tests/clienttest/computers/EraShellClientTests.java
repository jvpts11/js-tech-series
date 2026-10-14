/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.JourneyCommands;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's shell in a player's hands: at the era's Personal Computer, on its own network, the player types every
 * command the system offers (the lines {@link JourneyCommands} gives, the same the headless check runs), leaves any
 * full-screen program a command opens, and reads no error in what each one answers. Last, reboot starts the machine
 * again.
 */
public final class EraShellClientTests {

    /** How long a command may take to answer before what it printed is read. */
    private static final int ANSWER_WAIT = 6;
    /** The keys that leave the full-screen programs a command can open: a TUI, a shell, a pager. */
    private static final int[] LEAVE_KEYS = {GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_Q,
            GLFW.GLFW_KEY_F10};
    private static final int LEAVE_WAIT = 60;

    private EraShellClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void vintage_everyCommandAnswersAtThePrompt(final ClientTestContext ctx) {
        shell(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void legacy_everyCommandAnswersAtThePrompt(final ClientTestContext ctx) {
        shell(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void transition_everyCommandAnswersAtThePrompt(final ClientTestContext ctx) {
        shell(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void standard_everyCommandAnswersAtThePrompt(final ClientTestContext ctx) {
        shell(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void advanced_everyCommandAnswersAtThePrompt(final ClientTestContext ctx) {
        shell(ctx, EraKit.ADVANCED);
    }

    private static void shell(final ClientTestContext ctx, final EraKit kit) {
        final List<String> lines = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        final long[] session = new long[1];
        EraJourney.atTheDesk(ctx, kit);
        if (kit != EraKit.VINTAGE) {
            final String terminal = EraJourney.terminal(kit);
            EraJourney.launch(ctx, kit, terminal)
                    .thenWaitUntil(() -> DesktopSteps.app(ctx, terminal, ShellApp.class) != null,
                            EraJourney.SCREEN_WAIT, "the " + terminal + " window to open");
        }
        ctx.thenServer(EraJourney.SETTLE, level -> lines.addAll(JourneyCommands.lines(
                        new ServerCliComputer(computer(ctx, level), level))))
                .then(0, () -> {
                    for (final String line : lines) {
                        typed(ctx, kit, line, errors);
                    }
                    ctx.then(0, () -> ctx.assertTrue(errors.isEmpty(), kit + ": " + errors.size()
                                    + " of " + lines.size() + " lines answered with an error: "
                                    + String.join(" | ", errors)))
                            .thenScreenshot(1, kit.name().toLowerCase() + "-shell")
                            .thenServer(0, level -> session[0] = computer(ctx, level).console().session())
                            .then(1, () -> ctx.type("reboot"))
                            .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                            .thenWaitUntilServer(level -> {
                                final PersonalComputerBlockEntity computer = computer(ctx, level);
                                return computer.console().session() != session[0] && computer.isRunning()
                                        && computer.hasOs();
                            }, EraJourney.BOOT_WAIT, "reboot to start the machine again",
                                    level -> "session=" + computer(ctx, level).console().session()
                                            + " running=" + computer(ctx, level).isRunning());
                });
    }

    /*
     * Clears the glass, types the line, leaves whatever it opened and notes an error it answered with. The exit that
     * ends a session on another machine is typed there, where this system's cls is not a command.
     */
    private static void typed(final ClientTestContext ctx, final EraKit kit, final String line,
                              final List<String> errors) {
        if (!"exit".equals(line)) {
            ctx.then(1, () -> ctx.type("cls")).then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER));
        }
        ctx.then(2, () -> ctx.type(line))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenServer(ANSWER_WAIT, level -> {
                    final String error = firstError(computer(ctx, level).console().glass());
                    if (error != null) {
                        errors.add(line + ": " + error);
                    }
                })
                .thenWaitUntil(new Leave(ctx, kit), LEAVE_WAIT, "the prompt to be back after " + line);
    }

    @Nullable
    private static String firstError(final List<WireLine> glass) {
        for (final WireLine line : glass) {
            for (final WireLine.Span span : line.spans()) {
                if (span.style() == CliStyle.ERROR.id()) {
                    final StringBuilder text = new StringBuilder();
                    line.spans().forEach(each -> text.append(each.text().english()));
                    return text.toString();
                }
            }
        }
        return null;
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }

    /*
     * Done once the prompt is the terminal's again; while a full-screen program holds it, presses the keys that leave
     * one in turn, a few ticks apart, so a program that ignores one key gets the next.
     */
    private static final class Leave implements BooleanSupplier {

        private final ClientTestContext ctx;
        private final EraKit kit;
        private int ticks;
        private int key;

        Leave(final ClientTestContext ctx, final EraKit kit) {
            this.ctx = ctx;
            this.kit = kit;
        }

        @Override
        public boolean getAsBoolean() {
            if (!editing()) {
                return true;
            }
            if (ticks++ % 4 == 0) {
                ctx.key(LEAVE_KEYS[key++ % LEAVE_KEYS.length]);
            }
            return false;
        }

        private boolean editing() {
            if (kit == EraKit.VINTAGE) {
                final CommandPromptScreen<?> prompt = ctx.openScreen(CommandPromptScreen.class);
                return prompt != null && prompt.editing();
            }
            final ShellApp shell = DesktopSteps.app(ctx, EraJourney.terminal(kit), ShellApp.class);
            return shell != null && shell.editing();
        }
    }
}
