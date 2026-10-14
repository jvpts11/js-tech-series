/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.JourneyCommands;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The shells of each era's other systems in a player's hands, the ones the era's own desktop journey does not sit at:
 * UNIX on the Vintage, FreeBSD and each Linux distribution on the Legacy, each on the era's Personal Computer on its
 * own network, and MC-NET at the network's Mainframe, on the console of the monitor in front of it. At each the player
 * types every command the system offers (the lines {@link JourneyCommands} gives, the same the headless check runs),
 * clears the glass between them, leaves any full-screen program a command opens, and reads no error in what each one
 * answers. Last, reboot starts the computer again.
 */
public final class EraOtherShellsClientTests {

    /** How long a command may take to answer before what it printed is read. */
    private static final int ANSWER_WAIT = 6;
    /** The keys that leave the full-screen programs a command can open: a TUI, a shell, a pager. */
    private static final int[] LEAVE_KEYS = {GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_Q,
            GLFW.GLFW_KEY_F10};
    private static final int LEAVE_WAIT = 60;
    private static final int TIMEOUT = 8_000;
    /* The Mainframe's monitor in front of it, and where the player stands to use it. */
    private static final BlockPos MAINFRAME_MONITOR = new BlockPos(3, 2, 1);
    private static final BlockPos AT_THE_MAINFRAME = new BlockPos(5, 2, 1);

    private EraOtherShellsClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_unixAnswersEveryCommand(final ClientTestContext ctx) {
        tty(ctx, EraKit.VINTAGE, "unix");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_freeBsdAnswersEveryCommand(final ClientTestContext ctx) {
        tty(ctx, EraKit.LEGACY, "freebsd");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_ubuntuAnswersEveryCommand(final ClientTestContext ctx) {
        tty(ctx, EraKit.LEGACY, "ubuntu");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_debianAnswersEveryCommand(final ClientTestContext ctx) {
        tty(ctx, EraKit.LEGACY, "debian");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_fedoraAnswersEveryCommand(final ClientTestContext ctx) {
        tty(ctx, EraKit.LEGACY, "fedora");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_archAnswersEveryCommand(final ClientTestContext ctx) {
        tty(ctx, EraKit.LEGACY, "arch");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_gentooAnswersEveryCommand(final ClientTestContext ctx) {
        tty(ctx, EraKit.LEGACY, "gentoo");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_mcNetAnswersEveryCommandAtTheMainframe(final ClientTestContext ctx) {
        final List<String> lines = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        final AtomicReference<String> clear = new AtomicReference<>();
        EraJourney.lay(ctx, EraKit.VINTAGE)
                .thenBuild(0, world -> world.placeMonitor(MAINFRAME_MONITOR, Direction.EAST,
                        EraKit.VINTAGE.monitor()))
                .thenTeleport(EraJourney.SETTLE, AT_THE_MAINFRAME, Direction.WEST)
                .thenRightClick(EraJourney.SETTLE, MAINFRAME_MONITOR)
                .thenAwaitScreen(ComputerTerminalScreen.class, EraJourney.BOOT_WAIT)
                // The Mainframe's monitor opens its panel; its prompt is the Console heading.
                .then(EraJourney.SETTLE, () -> {
                    final int[] console = ctx.screen(ComputerTerminalScreen.class).headingPoint("Console");
                    ctx.clickGui(console[0], console[1]);
                })
                .thenServer(EraJourney.SETTLE, level -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe(ctx, level), level);
                    lines.addAll(JourneyCommands.lines(cli));
                    clear.set(clearCommand(cli));
                })
                .then(0, () -> {
                    for (final String line : lines) {
                        typed(ctx, line, clear.get(), errors, level -> mainframe(ctx, level));
                    }
                    ctx.then(0, () -> ctx.assertTrue(errors.isEmpty(), "MC-NET: " + errors.size() + " of "
                                    + lines.size() + " lines answered with an error: " + String.join(" | ", errors)))
                            .thenScreenshot(1, "vintage-mc-net-shell");
                });
    }

    /*
     * The system put on the era's computer, which is started again into it; at its prompt every line is typed, and
     * the reboot at the end starts the machine once more.
     */
    private static void tty(final ClientTestContext ctx, final EraKit kit, final String system) {
        final ResourceLocation os = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system);
        final List<String> lines = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        final AtomicReference<String> clear = new AtomicReference<>();
        final long[] session = new long[1];
        EraJourney.lay(ctx, kit)
                .thenServer(EraJourney.SETTLE, level -> {
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    ctx.assertTrue(computer.installOs(os), system + " goes on the " + kit + " computer");
                    computer.togglePower();
                    computer.togglePower();
                })
                .thenTeleport(EraJourney.SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(EraJourney.SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, EraJourney.BOOT_WAIT)
                .thenServer(EraJourney.SETTLE, level -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer(ctx, level), level);
                    lines.addAll(JourneyCommands.lines(cli));
                    clear.set(clearCommand(cli));
                })
                .then(0, () -> {
                    for (final String line : lines) {
                        typed(ctx, line, clear.get(), errors, level -> computer(ctx, level));
                    }
                    ctx.then(0, () -> ctx.assertTrue(errors.isEmpty(), system + ": " + errors.size() + " of "
                                    + lines.size() + " lines answered with an error: " + String.join(" | ", errors)))
                            .thenScreenshot(1, kit.name().toLowerCase(Locale.ROOT) + "-" + system + "-shell")
                            .thenServer(0, level -> session[0] = computer(ctx, level).console().session())
                            .then(1, () -> ctx.type("reboot"))
                            .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                            .thenWaitUntilServer(level -> {
                                final PersonalComputerBlockEntity computer = computer(ctx, level);
                                return computer.console().session() != session[0] && computer.isRunning()
                                        && computer.hasOs();
                            }, EraJourney.BOOT_WAIT, "reboot to start " + system + " again",
                                    level -> "session=" + computer(ctx, level).console().session()
                                            + " running=" + computer(ctx, level).isRunning());
                });
    }

    /*
     * Clears the glass, types the line, leaves whatever it opened and notes an error it answered with. The exit that
     * ends a session on another machine is typed there, where clearing is not this system's to do.
     */
    private static void typed(final ClientTestContext ctx, final String line, @Nullable final String clear,
                              final List<String> errors, final Machine machine) {
        if (clear != null && !"exit".equals(line)) {
            ctx.then(1, () -> ctx.type(clear)).then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER));
        }
        ctx.then(2, () -> ctx.type(line))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenServer(ANSWER_WAIT, level -> {
                    final String error = firstError(machine.at(level).console().glass());
                    if (error != null) {
                        errors.add(line + ": " + error);
                    }
                })
                .thenWaitUntil(new Leave(ctx), LEAVE_WAIT, "the prompt to be back after " + line);
    }

    /* The command that clears this system's glass: cls on the DOS family, clear on every other. */
    @Nullable
    private static String clearCommand(final ServerCliComputer cli) {
        for (final String name : List.of("clear", "cls")) {
            for (final ICliCommand command : CliCommands.all()) {
                if (command.name().equals(name) && command.available(cli)) {
                    return name;
                }
            }
        }
        return null;
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

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    /* The machine whose console answers, read on the server. */
    @FunctionalInterface
    private interface Machine {
        AbstractComputerBlockEntity at(ServerLevel level);
    }

    /*
     * Done once the prompt is the system's own again; while a full-screen program holds it, presses the keys that
     * leave one in turn, a few ticks apart, so a program that ignores one key gets the next. The Mainframe's console
     * has no full-screen program of its own, so it is back at once.
     */
    private static final class Leave implements BooleanSupplier {

        private final ClientTestContext ctx;
        private int ticks;
        private int key;

        Leave(final ClientTestContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public boolean getAsBoolean() {
            final CommandPromptScreen<?> prompt = ctx.openScreen(CommandPromptScreen.class);
            if (prompt == null || !prompt.editing()) {
                return true;
            }
            if (ticks++ % 4 == 0) {
                /*
                 * Besides the keys, the two programs no key alone leaves: ee's menu first, whose first entry leaves
                 * the editor and whose first answer keeps what was typed, and last the line the old vi wants.
                 */
                final int next = key++ % (LEAVE_KEYS.length + 2);
                if (next == 0) {
                    ctx.key(GLFW.GLFW_KEY_ESCAPE);
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                } else if (next == LEAVE_KEYS.length + 1) {
                    ctx.type(":q!");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                } else {
                    ctx.key(LEAVE_KEYS[next - 1]);
                }
            }
            return false;
        }
    }
}
