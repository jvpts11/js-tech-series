/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One control of a program's content: a rectangle that draws itself through the skin, takes the input that
 * lands on it and keeps its own state (pressed, focused, scrolled) between frames. The owner lays it out by
 * {@link #setBounds} every frame, so a window that resizes or a tab that changes moves its controls without
 * the control knowing; input arrives in the same coordinates the bounds were given in.
 *
 * <p>Components live in a {@link Panel}, which routes clicks, keys and the keyboard focus among them.
 */
public abstract class UiComponent {

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused;
    @Nullable
    private Panel parent;
    @Nullable
    private Component spoken;

    // geometry

    /** Places the component; called by its owner before every render. Returns this, for chaining. */
    public UiComponent setBounds(final int x, final int y, final int width, final int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The first x past the right edge. */
    public int right() {
        return x + width;
    }

    /** The first y past the bottom edge. */
    public int bottom() {
        return y + height;
    }

    /** Whether the point lies inside the bounds. */
    public boolean contains(final double mx, final double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    /** The centre of the bounds, where a test clicks to hit the control. */
    public int[] center() {
        return new int[] {x + width / 2, y + height / 2};
    }

    // state

    public boolean visible() {
        return visible;
    }

    public UiComponent setVisible(final boolean value) {
        visible = value;
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public UiComponent setEnabled(final boolean value) {
        enabled = value;
        return this;
    }

    /** Whether the component takes the keyboard when clicked. */
    public boolean focusable() {
        return false;
    }

    public boolean isFocused() {
        return focused;
    }

    /**
     * Whether the component draws the same every frame whatever happens (a frame, a panel, fixed words), so its
     * screen may draw it once into a picture and lay that down again rather than draw it each frame.
     */
    public boolean still() {
        return false;
    }

    /**
     * Whether the component, while it has the keyboard, takes every key as typing (a field, a text area, a command
     * line), so that Tab, the arrows and the game's keys do not leave it; the others let Tab and the arrows move on.
     */
    public boolean takesTyping() {
        return false;
    }

    /** Hands the keyboard to the component or takes it away; a component losing it gets {@link #onBlur()}. */
    void setFocused(final boolean value) {
        if (focused == value) {
            return;
        }
        focused = value;
        if (!value) {
            onBlur();
        }
    }

    /**
     * Gives the component the name the game's narrator calls it by, for one that has no words of its own to read (a
     * switch, a slider, a field beside its label); null leaves its own.
     */
    public UiComponent setSpoken(@Nullable final Component name) {
        spoken = name;
        return this;
    }

    /** The name the narrator calls the component by, when one was given. */
    @Nullable
    public Component spoken() {
        return spoken;
    }

    /**
     * What the game's narrator says of the component when the keyboard reaches it or the pointer rests on it: its
     * kind, its name and its state, as the game says them of its own widgets. Empty says nothing.
     */
    public Component narration() {
        return spoken == null ? Component.empty() : spoken;
    }

    /** How the component is used, said after its narration while it has the keyboard; empty for nothing. */
    public Component narrationUsage() {
        return Component.empty();
    }

    /** Called when the component loses the keyboard: the place to commit what was typed. */
    protected void onBlur() {
    }

    /** Gives the keyboard up, so the owner moves the focus away from this component. */
    protected final void blur() {
        if (parent != null) {
            parent.blur(this);
        } else {
            setFocused(false);
        }
    }

    void attach(@Nullable final Panel owner) {
        parent = owner;
    }

    @Nullable
    protected Panel parent() {
        return parent;
    }

    /** Whether the cursor is over the component and the component can react to it. */
    protected boolean hovered(final UiContext ctx) {
        return enabled && visible && contains(ctx.mouseX(), ctx.mouseY());
    }

    // drawing and input

    /** Draws the component in its bounds. */
    public abstract void render(GuiGraphics g, UiContext ctx);

    /** A click inside the bounds; true when the component took it. */
    public boolean mouseClicked(final double mx, final double my, final int button) {
        return false;
    }

    /** The mouse moving with a button held after a click this component took. */
    public boolean mouseDragged(final double mx, final double my, final int button) {
        return false;
    }

    /** The button of a click this component took being released, wherever the cursor is now. */
    public boolean mouseReleased(final double mx, final double my, final int button) {
        return false;
    }

    /** The wheel turning over the component ({@code delta} &gt; 0 is up); true when the component took it. */
    public boolean mouseScrolled(final double mx, final double my, final double delta) {
        return false;
    }

    /** A character typed while the component has the keyboard; true when it took it. */
    public boolean charTyped(final char c) {
        return false;
    }

    /** A key pressed while the component has the keyboard; true when it took it. */
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return false;
    }

    /** A key let go while the component has the keyboard; true when it took it. */
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        return false;
    }

    /** The tooltip for the cursor at this point, or an empty list for none. */
    public List<Component> tooltip(final double mx, final double my) {
        return List.of();
    }

    /**
     * Whether the component has something open over the others (a drop-down's list, a colour picker): its panel then
     * draws it after every other child and hands it the pointer first, wherever the open part reaches.
     */
    public boolean overlayOpen() {
        return false;
    }

    /** Whether the open part over the others covers the point; only asked while {@link #overlayOpen()}. */
    public boolean overlayContains(final double mx, final double my) {
        return false;
    }

    /** Draws the open part over the others, after every other child of its panel; only while it is open. */
    public void renderOverlay(final GuiGraphics g, final UiContext ctx) {
    }

    /** Closes what is open over the others, as a click outside it or Escape does. */
    public void closeOverlay() {
    }

    /** Lets go of what the component holds outside itself (a texture it drew into), when its screen goes away. */
    public void dispose() {
    }
}
