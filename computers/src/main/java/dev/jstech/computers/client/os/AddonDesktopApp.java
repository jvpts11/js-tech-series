/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.api.client.DesktopProgram;
import dev.jstech.core.api.client.SurfaceRenderer;
import dev.jstech.core.client.gui.surface.SurfaceHost;
import dev.jstech.core.gui.SurfaceTexts;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A window of a program a mod wrote in Java, as the desktop holds it: the frame and the room are the desktop's, what is
 * inside is the program's.
 *
 * <p>The program runs for the player who opened its window with their own hands on this game. The same window shown
 * any other way, restored from what the machine remembers for another player or drawn on the monitor's face in the
 * world, shows the placeholder instead, unless the program says what it draws is safe to show anywhere.
 */
public final class AddonDesktopApp implements IDesktopApp {

    private final ResourceLocation program;
    private final DesktopProgram app;
    @Nullable
    private final SurfaceHost surface;
    /** Whether this game's player opened it, which is whom it runs for. */
    private final boolean opener;
    private OsSkin skin = OsSkin.fallback();

    /** Set while a window is being opened by the player's own hand, so the program it makes knows it runs for them. */
    private static boolean openingByHand;

    public AddonDesktopApp(final ResourceLocation program, final DesktopProgram app) {
        this.program = program;
        this.app = app;
        final SurfaceRenderer renderer = app.renderer();
        this.surface = renderer == null ? null : new SurfaceHost(renderer, program.toString());
        this.opener = openingByHand;
    }

    /** Opens a window by the player's own hand: what {@code make} makes runs for this game's player. */
    static IDesktopApp byHand(final Supplier<IDesktopApp> make) {
        openingByHand = true;
        try {
            return make.get();
        } finally {
            openingByHand = false;
        }
    }

    /** Whether this game draws what the program shows, rather than the placeholder: for its opener, off the world. */
    public boolean showsHere() {
        return this.opener && !OffscreenDesktop.drawingFace();
    }

    @Override
    public String title() {
        final String said = this.app.title();
        return said == null || said.isEmpty() ? ProgramClient.nameOf(this.program) : said;
    }

    @Override
    public int defaultWidth() {
        return this.app.width();
    }

    @Override
    public int defaultHeight() {
        return this.app.height() + DesktopWindow.TITLE_H;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin == null ? OsSkin.fallback() : osSkin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        if (this.surface != null) {
            this.surface.draw(g, font, this.skin, x, y, width, height, this.showsHere());
        } else if (this.showsHere() || this.app.worldSafe()) {
            this.app.draw(g, font, this.skin, x, y, width, height, mouseX, mouseY);
        } else {
            SurfaceHost.placeholder(g, font, this.skin, x, y, width, height, SurfaceTexts.ONLY_OPENER);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (!this.showsHere()) {
            return;
        }
        if (this.surface == null || !this.surface.mouseClicked(mouseX, mouseY, button)) {
            this.app.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        if (!this.showsHere()) {
            return;
        }
        if (this.surface == null || !this.surface.mouseReleased(mouseX, mouseY, button)) {
            this.app.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        return this.showsHere() && (this.surface != null ? this.surface.mouseScrolled(delta)
                : this.app.mouseScrolled(delta));
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return this.showsHere() && (this.surface != null ? this.surface.keyPressed(key, scanCode, modifiers)
                : this.app.keyPressed(key, scanCode, modifiers));
    }

    @Override
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        return this.showsHere() && (this.surface != null ? this.surface.keyReleased(key, scanCode, modifiers)
                : this.app.keyReleased(key, scanCode, modifiers));
    }

    @Override
    public boolean charTyped(final char c) {
        return this.showsHere() && (this.surface != null ? this.surface.charTyped(c) : this.app.charTyped(c));
    }

    @Override
    public void onClosed() {
        if (this.surface != null) {
            this.surface.close();
        }
        this.app.closed();
    }
}
