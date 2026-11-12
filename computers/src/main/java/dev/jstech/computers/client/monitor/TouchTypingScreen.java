/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.MachineKeyboard;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * The typing line of a touch monitor (computers/docs/design/peripherals.md): where the chat's line is, it takes what the
 * player types and hands it, key by key, to the screen held on the glass, showing what has been typed since it came up.
 * It comes up when a touch lands on something that takes typing, and Escape puts it away. While it is up the mouse
 * still turns the player and the use button still touches the glass, so a system's touch keyboard can be used with it.
 */
@PaletteHolder
public final class TouchTypingScreen extends Screen {

    private final StringBuilder typed = new StringBuilder();
    /** Whether the use button is down, which is the finger on the glass. */
    private boolean fingerDown;
    /* Where the mouse last was, in the screen's units, to turn the player by how far it moved. */
    private double mouseX = Double.NaN;
    private double mouseY = Double.NaN;

    private static final Palette<Inks> INKS = Palettes.declare(JsComputers.MODID, "monitor/touch_line",
            new Inks(0xffffffff, 0xffd0d0d0, 0x60000000));
    /* The longest run of what was typed the line keeps; it shows as much of its end as fits. */
    private static final int KEPT = 256;
    /* How the game turns the player for the mouse: its sensitivity, curved, times this, as the game's own turning. */
    private static final double TURN = 8.0;

    private TouchTypingScreen() {
        super(GameText.component(TouchTexts.TITLE));
    }

    /** Whether the use button is down while the line is up, the finger on the glass. */
    public boolean fingerDown() {
        return fingerDown;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* The world stays as it is behind the line, as it does behind the chat's. */
    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        final Inks inks = INKS.get();
        final String note = GameText.resolve(TouchScreens.heldOnWall() ? TouchTexts.NOTE_WALL : TouchTexts.NOTE);
        g.fill(2, height - 30, 6 + font.width(note) + 4, height - 18, inks.noteGround());
        Draw.text(g, font, note, 6, height - 28, inks.note());
        g.fill(2, height - 14, width - 2, height - 2, minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
        String shown = typed + "_";
        while (font.width(GameText.resolve(TouchTexts.LINE.with(shown))) > width - 10 && shown.length() > 1) {
            shown = shown.substring(1);
        }
        Draw.text(g, font, GameText.resolve(TouchTexts.LINE.with(shown)), 4, height - 12, inks.line());
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        final Screen held = TouchScreens.held();
        if (held == null) {
            onClose();
            return true;
        }
        held.charTyped(c, modifiers);
        typed.append(c);
        if (typed.length() > KEPT) {
            typed.delete(0, typed.length() - KEPT);
        }
        return true;
    }

    /** Every key but Escape goes to the held screen, as the machine's keyboard would send it. */
    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final Screen held = TouchScreens.held();
        if (key == GLFW.GLFW_KEY_ESCAPE || held == null) {
            onClose();
            return true;
        }
        if (!(held instanceof MachineKeyboard.ITakesKeysFirst glass && glass.keyFirst(key, scanCode, modifiers))) {
            held.keyPressed(key, scanCode, modifiers);
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !typed.isEmpty()) {
            typed.setLength(typed.length() - 1);
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            typed.setLength(0);
        }
        return true;
    }

    @Override
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        final Screen held = TouchScreens.held();
        return held != null && held.keyReleased(key, scanCode, modifiers);
    }

    /* The mouse turns the player as it does in the world, so the crosshair can be aimed at the glass. */
    @Override
    public void mouseMoved(final double x, final double y) {
        if (!Double.isNaN(mouseX) && minecraft.player != null) {
            final Window window = minecraft.getWindow();
            final double dx = (x - mouseX) * window.getScreenWidth() / window.getGuiScaledWidth();
            final double dy = (y - mouseY) * window.getScreenHeight() / window.getGuiScaledHeight();
            final double speed = Math.pow(minecraft.options.sensitivity().get() * 0.6 + 0.2, 3) * TURN;
            minecraft.player.turn(dx * speed, dy * speed * (minecraft.options.invertYMouse().get() ? -1 : 1));
        }
        mouseX = x;
        mouseY = y;
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final int button) {
        if (minecraft.options.keyUse.matchesMouse(button)) {
            fingerDown = TouchFinger.press();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final int button) {
        if (minecraft.options.keyUse.matchesMouse(button)) {
            fingerDown = false;
        }
        return true;
    }

    /* The pointer is let go for whatever screen comes next; the game catches it again going back to the world. */
    @Override
    public void removed() {
        final Window window = minecraft.getWindow();
        InputConstants.grabOrReleaseMouse(window.getWindow(), GLFW.GLFW_CURSOR_NORMAL, window.getScreenWidth() / 2.0,
                window.getScreenHeight() / 2.0);
    }

    /* The pointer stays caught, as it is in the world, for the mouse to keep turning the player. */
    @Override
    protected void init() {
        InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR_DISABLED,
                minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
    }

    /** Brings the typing line up over the world, unless a screen is up already. */
    static void open() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            minecraft.setScreen(new TouchTypingScreen());
        }
    }

    /**
     * What the typing line is written in.
     *
     * @param line       the typed line
     * @param note       the note over it
     * @param noteGround what the note stands on
     */
    private record Inks(int line, int note, int noteGround) {
    }
}
