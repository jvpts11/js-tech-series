/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.client.AbstractMachineAssemblyScreen;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.FirmwareScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import org.lwjgl.glfw.GLFW;

/**
 * The steps every era's journey shares: the era's network laid as {@link EraNetwork} lays it, and the player sitting
 * down at its Personal Computer, which comes up in the era's own system: a command prompt for MC-DOS, a desktop for
 * every graphical system.
 */
final class EraJourney {

    static final int SETTLE = 4;
    static final int SCREEN_WAIT = 80;
    /** Long enough for a cold start's self-test and the system's start on the era's own disk. */
    static final int BOOT_WAIT = 2_400;

    private EraJourney() {
    }

    /** Lays the era's network in the test's area. */
    static ClientTestContext lay(final ClientTestContext ctx, final EraKit kit) {
        return ctx.thenGive(0).thenBuild(0, world -> EraNetwork.lay(world, kit));
    }

    /** Lays the era's network and sits the player at its computer until the era's system is up on the monitor. */
    static ClientTestContext atTheDesk(final ClientTestContext ctx, final EraKit kit) {
        return sit(lay(ctx, kit), kit);
    }

    /** Sits the player at the network's computer, already laid, until the era's system is up on the monitor. */
    static ClientTestContext sit(final ClientTestContext ctx, final EraKit kit) {
        return ctx.thenWaitUntil(away(ctx), SCREEN_WAIT, "the screen to be left")
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(systemScreen(kit), BOOT_WAIT);
    }

    /** The screen the era's desktop system shows a seated player. */
    static Class<? extends Screen> systemScreen(final EraKit kit) {
        return kit == EraKit.VINTAGE ? CommandPromptScreen.class : DesktopScreen.class;
    }

    /** What the era's desktop calls its terminal, on the Start menu and on the window: Frames 11 has Megashell. */
    static String terminal(final EraKit kit) {
        return kit == EraKit.ADVANCED ? "Megashell" : "Command Prompt";
    }

    /**
     * Opens {@code label} from the Start menu as a player of the era does: the menu first, then, once it has finished
     * opening (Frames 11 slides it up), the name typed into its search box and Enter on a Start that has one, which
     * reaches an entry a long list has scrolled out of sight, or the entry clicked on one that has none.
     */
    static ClientTestContext launch(final ClientTestContext ctx, final EraKit kit, final String label) {
        return ctx.thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(label),
                        SCREEN_WAIT, label + " to be on the Start menu")
                .then(0, () -> {
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    ctx.click(desktop.startButtonX(), desktop.startButtonY());
                })
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).isStartOpen(), SCREEN_WAIT,
                        "the Start menu to open")
                .then(SETTLE * 3, () -> {
                    if (searchableStart(kit)) {
                        ctx.type(label);
                        ctx.key(GLFW.GLFW_KEY_ENTER);
                        return;
                    }
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    final int item = desktop.launcherLabels().indexOf(label);
                    ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
                });
    }

    /** Whether the era's desktop has a search box on its Start: Frames 7 has one, and every Frames after it. */
    static boolean searchableStart(final EraKit kit) {
        return kit.era().isAtLeast(EraKit.TRANSITION.era());
    }

    /** Picks up hotbar slot {@code slot} and puts it in seat {@code index} of {@code kind} on the open screen. */
    static void seatFromHotbar(final ClientTestContext ctx,
                               final Class<? extends AbstractMachineAssemblyScreen<?>> screen, final int slot,
                               final SeatKind kind, final int index) {
        final Seat seat = seat(ctx, screen, kind, index);
        ctx.clickGui(hotbarX(slot), hotbarY(ctx, screen));
        ctx.clickGui(seat.x() + 8, seat.y() + 8);
    }

    /** Seat {@code index} of {@code kind} on the open assembly screen. */
    static Seat seat(final ClientTestContext ctx, final Class<? extends AbstractMachineAssemblyScreen<?>> screen,
                     final SeatKind kind, final int index) {
        final List<Seat> seats = ctx.screen(screen).geometry().seats(kind);
        if (index >= seats.size()) {
            throw new ClientTestFailure("the screen has " + seats.size() + " " + kind + " seats, not " + (index + 1));
        }
        return seats.get(index);
    }

    /** The middle of hotbar slot {@code slot} on an assembly screen, from the window's left. */
    static int hotbarX(final int slot) {
        return 16 + slot * 18;
    }

    /** The middle of the hotbar's row on the open assembly screen, from the window's top. */
    static int hotbarY(final ClientTestContext ctx, final Class<? extends AbstractMachineAssemblyScreen<?>> screen) {
        final AssemblyGeometry geometry = ctx.screen(screen).geometry();
        return geometry.inventoryY() + 58 + 8;
    }

    /** Done once no screen is up; until then presses Escape every so often, as a player leaving a screen does. */
    static BooleanSupplier away(final ClientTestContext ctx) {
        return new Pressing(ctx, GLFW.GLFW_KEY_ESCAPE, () -> ctx.mc().screen == null);
    }

    /** Done once the firmware's Setup is up; until then presses DEL every so often, through the self-test. */
    static BooleanSupplier intoSetup(final ClientTestContext ctx) {
        return new Pressing(ctx, GLFW.GLFW_KEY_DELETE, () -> ctx.openScreen(FirmwareScreen.class) != null);
    }

    /* Presses a key every half second while a screen is up, until what it waits for is so. */
    private static final class Pressing implements BooleanSupplier {

        private final ClientTestContext ctx;
        private final int key;
        private final BooleanSupplier done;
        private int ticks;

        Pressing(final ClientTestContext ctx, final int key, final BooleanSupplier done) {
            this.ctx = ctx;
            this.key = key;
            this.done = done;
        }

        @Override
        public boolean getAsBoolean() {
            if (done.getAsBoolean()) {
                return true;
            }
            if (ticks++ % 10 == 0 && ctx.mc().screen != null) {
                ctx.key(key);
            }
            return false;
        }
    }
}
