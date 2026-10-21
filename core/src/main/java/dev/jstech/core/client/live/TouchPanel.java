/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.gui.style.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

/**
 * A screen in the world built of the Core's components, as a screen on the player's monitor is: drawn into the
 * block's picture on the window of its style, the component under the crosshair lit as the mouse lights it, and each one
 * added with a name a part a press reaches, which the action bar names by what a press on it does ("Right click:
 * Start"). One panel serves every block of its kind; what it shows of each comes from the suppliers its components
 * read.
 *
 * <pre>{@code
 * TouchPanel pump = new TouchPanel(MODID);
 * pump.add(new Label(() -> "PUMP 2")).setBounds(4, 3, 40, 8);
 * pump.add("start", new Button("Start", () -> { })).setBounds(6, 30, 38, 14);
 * ScreenTouchClient.targets(PUMP, pump);
 * // In the block's renderer:
 * LiveScreen picture = LiveScreens.ask(pos, pump.painter(pos), 96, 48, 1, Tube.COLOUR, distance);
 * }</pre>
 */
public final class TouchPanel implements ITouchTargets {

    private final Supplier<Style> style;
    private final Panel panel = new Panel();
    private final List<Named> named = new ArrayList<>();

    /* No pointer: a pixel no component holds. */
    private static final int NOWHERE = Integer.MIN_VALUE / 2;

    /** A panel drawn in the style {@code modid}'s screens wear, on its window. */
    public TouchPanel(final String modid) {
        Objects.requireNonNull(modid, "modid");
        this.style = () -> Styles.forMod(modid);
    }

    /** A panel drawn in the style {@code style} gives each time, on its window. */
    public TouchPanel(final Supplier<Style> style) {
        this.style = Objects.requireNonNull(style, "style");
    }

    /** Adds a component a press does not reach (a title, a reading); returns it, for its bounds to be set. */
    public <T extends UiComponent> T add(final T component) {
        return panel.add(component);
    }

    /**
     * Adds a component a press reaches, known to the server by {@code name}: a press on it carries the name, and the
     * action bar says what a press does, in the component's own words (a button's label).
     */
    public <T extends UiComponent> T add(final String name, final T component) {
        named.add(new Named(Objects.requireNonNull(name, "name"), component));
        return panel.add(component);
    }

    /** What draws the picture of the block at {@code pos}, the component under the crosshair lit. */
    public ILivePainter painter(final BlockPos pos) {
        return (g, width, height, partialTick) -> paint(g, pos, width, height, partialTick);
    }

    /**
     * Draws the panel for the block at {@code pos} on its style's window, {@code width} by {@code height}, the
     * crosshair's pixel on its screen taken as the mouse.
     */
    public void paint(final GuiGraphics g, final BlockPos pos, final int width, final int height,
                      final float partialTick) {
        final StyleSkin skin = Styles.skin(style.get());
        final int[] pointer = ScreenTouchClient.pointerOn(pos).orElse(new int[] {NOWHERE, NOWHERE});
        skin.window(g, 0, 0, width, height);
        panel.render(g, new UiContext(skin, Minecraft.getInstance().font, pointer[0], pointer[1], partialTick));
    }

    /** The named component at the pixel, the last added first, as the one drawn in front. */
    @Override
    public Optional<TouchTarget> at(final BlockPos pos, final int x, final int y) {
        for (int i = named.size() - 1; i >= 0; i--) {
            final Named one = named.get(i);
            final UiComponent component = one.component();
            if (component.visible() && component.enabled() && component.contains(x, y)) {
                return Optional.of(new TouchTarget(one.name(), component.pressLabel()));
            }
        }
        return Optional.empty();
    }

    private record Named(String name, UiComponent component) {
    }
}
