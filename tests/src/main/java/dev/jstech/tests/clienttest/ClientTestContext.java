/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.testkit.client.ClientTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The series' client tests' helper: the Core's test kit's, with the series' own steps on top, a scenario built with
 * the series' machines at the test's origin and the desktop's points of J's Computers. Each step the kit has hands
 * this helper back, so a test chains the series' steps after the kit's.
 */
public final class ClientTestContext extends ClientTestHelper {

    ClientTestContext(final Minecraft mc, final ClientTests.Planned test, final BlockPos origin) {
        super(mc, test, origin);
    }

    /** Builds a scenario on the server with the series' fixtures, at this test's origin. */
    public ClientTestContext thenBuild(final int delayTicks, final Consumer<TestWorldBuilder> build) {
        return thenServer(delayTicks, level -> build.accept(TestWorldBuilder.at(level, origin())));
    }

    /** Clicks a point of the desktop, by the geometry of its windows and programs as they report it. */
    public void clickDesktop(final int[] point) {
        final DesktopScreen desktop = screen(DesktopScreen.class);
        // A desktop drawn smaller puts its points closer together on the screen; the click goes where they are drawn.
        click(desktop.desktopX() + point[0] * desktop.desktopScale() + 0.5,
                desktop.desktopY() + point[1] * desktop.desktopScale() + 0.5);
    }

    /** Presses on one point of the desktop, drags to another and lets go, at the desktop's scale. */
    public void dragDesktop(final int[] from, final int[] to) {
        final DesktopScreen desktop = screen(DesktopScreen.class);
        final double s = desktop.desktopScale();
        final double sx = desktop.desktopX() + from[0] * s + 0.5;
        final double sy = desktop.desktopY() + from[1] * s + 0.5;
        final double ex = desktop.desktopX() + to[0] * s + 0.5;
        final double ey = desktop.desktopY() + to[1] * s + 0.5;
        desktop.mouseClicked(sx, sy, 0);
        desktop.mouseDragged((sx + ex) / 2, (sy + ey) / 2, 0, (ex - sx) / 2, (ey - sy) / 2);
        desktop.mouseDragged(ex, ey, 0, (ex - sx) / 2, (ey - sy) / 2);
        desktop.mouseReleased(ex, ey, 0);
    }

    /** The right button on a point of the desktop, the way {@link #clickDesktop} is the left one. */
    public void rightClickDesktop(final int[] point) {
        final DesktopScreen desktop = screen(DesktopScreen.class);
        rightClick(desktop.desktopX() + point[0] * desktop.desktopScale() + 0.5,
                desktop.desktopY() + point[1] * desktop.desktopScale() + 0.5);
    }

    /** Rests the pointer on a point of the desktop without pressing anything, the way {@link #pointAt} does. */
    public void pointAtDesktop(final int[] point) {
        final DesktopScreen desktop = screen(DesktopScreen.class);
        pointAt(desktop.desktopX() + point[0] * desktop.desktopScale() + 0.5,
                desktop.desktopY() + point[1] * desktop.desktopScale() + 0.5);
    }

    @Override
    public ClientTestContext then(final int delayTicks, final Runnable action) {
        super.then(delayTicks, action);
        return this;
    }

    @Override
    public ClientTestContext thenServer(final int delayTicks, final Consumer<ServerLevel> action) {
        super.thenServer(delayTicks, action);
        return this;
    }

    @Override
    public ClientTestContext thenWaitUntil(final BooleanSupplier condition, final int maxTicks, final String what) {
        super.thenWaitUntil(condition, maxTicks, what);
        return this;
    }

    @Override
    public ClientTestContext thenWaitUntil(final BooleanSupplier condition, final int maxTicks, final String what,
                                           final Supplier<String> diagnostics) {
        super.thenWaitUntil(condition, maxTicks, what, diagnostics);
        return this;
    }

    @Override
    public ClientTestContext thenWaitUntilServer(final Predicate<ServerLevel> condition, final int maxTicks,
                                                 final String what,
                                                 final Function<ServerLevel, String> diagnostics) {
        super.thenWaitUntilServer(condition, maxTicks, what, diagnostics);
        return this;
    }

    @Override
    public <T extends Screen> ClientTestContext thenAwaitScreen(final Class<T> type, final int maxTicks) {
        super.thenAwaitScreen(type, maxTicks);
        return this;
    }

    @Override
    public ClientTestContext thenAwaitLandDrawn(final int maxTicks) {
        super.thenAwaitLandDrawn(maxTicks);
        return this;
    }

    @Override
    public ClientTestContext thenAwaitNoScreen(final int maxTicks) {
        super.thenAwaitNoScreen(maxTicks);
        return this;
    }

    @Override
    public ClientTestContext thenSaveAndReload(final int delayTicks) {
        super.thenSaveAndReload(delayTicks);
        return this;
    }

    @Override
    public ClientTestContext thenTeleport(final int delayTicks, final BlockPos relative, final Direction facing) {
        super.thenTeleport(delayTicks, relative, facing);
        return this;
    }

    @Override
    public ClientTestContext thenRightClick(final int delayTicks, final BlockPos relative) {
        super.thenRightClick(delayTicks, relative);
        return this;
    }

    @Override
    public ClientTestContext thenRightClickFront(final int delayTicks, final BlockPos relative, final double x,
                                                 final double y) {
        super.thenRightClickFront(delayTicks, relative, x, y);
        return this;
    }

    @Override
    public ClientTestContext thenScreenshot(final int delayTicks, final String label) {
        super.thenScreenshot(delayTicks, label);
        return this;
    }

    @Override
    public ClientTestContext thenMatchesReference(final int delayTicks, final String label, final int x, final int y,
                                                  final int width, final int height) {
        super.thenMatchesReference(delayTicks, label, x, y, width, height);
        return this;
    }

    @Override
    public ClientTestContext thenAssert(final int delayTicks, final BooleanSupplier condition, final String message) {
        super.thenAssert(delayTicks, condition, message);
        return this;
    }

    @Override
    public ClientTestContext thenPlace(final int delayTicks, final BlockPos relative) {
        super.thenPlace(delayTicks, relative);
        return this;
    }

    @Override
    public ClientTestContext thenPlaceAgainst(final int delayTicks, final BlockPos relative, final Direction face) {
        super.thenPlaceAgainst(delayTicks, relative, face);
        return this;
    }

    @Override
    public ClientTestContext thenSneakClick(final int delayTicks, final BlockPos relative, final Direction face) {
        super.thenSneakClick(delayTicks, relative, face);
        return this;
    }

    @Override
    public ClientTestContext thenGive(final int delayTicks, final ItemStack... stacks) {
        super.thenGive(delayTicks, stacks);
        return this;
    }
}
