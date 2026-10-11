/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.core.api.client.GpuSurface;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.core.guide.GuideSounds;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.SceneProjection;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.gui.ManualButtonLook;
import dev.jstech.core.gui.layout.ManualScreenLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The scenes of an open manual: how the reader has turned, brought near and peeled each, the picture of each kept on
 * the graphics card, their balloons and the strip of their controls, and what the pointer and the wheel do to them.
 *
 * <p>A scene is drawn again only when it is turned, brought nearer, peeled or shown at another size; frame to frame
 * its kept picture is put on the page. A balloon stands at the frame's nearer side, level with the part it points at,
 * and the pointer on its number, or on its line of the legend, outlines the part and lights both.
 */
final class ManualScenes {

    private final Map<String, State> states = new HashMap<>();
    private final Font font;
    private final GuideStyle style;
    private final ToIntFunction<String> colour;
    private final IGuideText text;
    /** The scene being turned by the pointer, and where the pointer was a moment ago. */
    private String dragging = "";
    private double dragX;
    private double dragY;
    /** A button held down, drawn sunk while the pointer stays on it. */
    @Nullable
    private ManualScreenLayout.Rect pressed;
    /** The balloon lit in this frame, by its scene and its number, and the one the pointer lights for the next. */
    private String litScene = "";
    private int litNumber;
    private String nextScene = "";
    private int nextNumber;

    /** A balloon's radius, and how far apart two balloons on one side stand at least. */
    private static final int BALLOON = 5;
    private static final int BALLOON_PITCH = 12;
    /** How far a balloon stands in from the frame's side. */
    private static final int EDGE = 2;
    /** The strip's buttons: how tall they are, and the room round a word on one. */
    private static final int BUTTON_HEIGHT = 9;
    private static final int BUTTON_ROOM = 6;
    private static final int GAP = 2;
    /** How many degrees a scene turns for each pixel the pointer drags it, and how much nearer a turn of the wheel. */
    private static final float TURN = 0.8F;
    private static final float ZOOM_STEP = 1.1F;
    /** How far a lit balloon's colour and a lit line of the legend go from the link's colour towards white. */
    private static final double BALLOON_LIGHT = 0.5;
    private static final double KEY_LIGHT = 0.8;
    /** What a word cut short ends with. */
    private static final String ELLIPSIS = "...";

    ManualScenes(final Font font, final GuideStyle style, final ToIntFunction<String> colour,
                 final IGuideText text) {
        this.font = font;
        this.style = style;
        this.colour = colour;
        this.text = text;
    }

    /** Begins a frame: what the pointer lit in the last one is lit in this one. */
    void frame() {
        this.litScene = this.nextScene;
        this.litNumber = this.nextNumber;
        this.nextScene = "";
        this.nextNumber = 0;
    }

    /**
     * Draws a scene in its frame at that place on the screen, with its balloons and the strip of its controls, and
     * gives the words of the balloon under the pointer for its tooltip, or none.
     */
    List<Component> draw(final GuiGraphics g, final int x, final int y, final GuidePiece.Scene piece, final int mx,
                         final int my) {
        final State state = this.state(piece);
        final GuideBlock.Scene scene = piece.scene();
        final int width = piece.width() - 2;
        final int height = scene.height() - 1;
        g.fill(x, y, x + piece.width(), y + piece.height(), this.colour.applyAsInt(GuideStyle.SHADE));
        Draw.outline(g, x, y, piece.width(), piece.height(), this.colour.applyAsInt(GuideStyle.RULE));
        this.picture(g, state, x + 1, y + 1, width, height);
        final SceneProjection projection = new SceneProjection(scene, state.view, width, height);
        final List<Placed> balloons = this.balloons(projection, state, width, height);
        int lit = this.litScene.equals(piece.id()) ? this.litNumber : 0;
        GuideBlock.SceneBalloon pointed = null;
        for (final Placed placed : balloons) {
            if (Math.hypot(mx - (x + 1 + placed.x()), my - (y + 1 + placed.y())) <= BALLOON + 1) {
                pointed = placed.balloon();
                lit = pointed.number();
                this.nextScene = piece.id();
                this.nextNumber = lit;
            }
        }
        for (final Placed placed : balloons) {
            if (placed.balloon().number() == lit) {
                this.outline(g, projection.bounds(placed.balloon().part()), x + 1, y + 1, width, height);
            }
        }
        for (final Placed placed : balloons) {
            this.line(g, x + 1 + placed.x(), y + 1 + placed.y(), x + 1 + (int) Math.round(placed.target().x()),
                    y + 1 + (int) Math.round(placed.target().y()));
        }
        for (final Placed placed : balloons) {
            this.balloon(g, x + 1 + placed.x(), y + 1 + placed.y(), placed.balloon().number(),
                    placed.balloon().number() == lit);
        }
        this.strip(g, state, piece, x, y + scene.height(), mx, my);
        if (pointed == null) {
            return List.of();
        }
        final List<Component> words = new ArrayList<>();
        words.add(Component.literal(this.text.text(pointed.key())));
        if (!pointed.detailKey().isEmpty()) {
            words.add(Component.literal(this.text.text(pointed.detailKey())).withStyle(ChatFormatting.GRAY));
        }
        return words;
    }

    /** Draws a line of a scene's legend: its balloon, lit with its part while the pointer is on either. */
    void drawKey(final GuiGraphics g, final int x, final int y, final GuidePiece.SceneKey key, final int mx,
                 final int my) {
        final boolean pointed = mx >= x && mx < x + key.width() && my >= y && my < y + key.height();
        if (pointed) {
            this.nextScene = key.scene();
            this.nextNumber = key.number();
        }
        final boolean lit = pointed || this.litScene.equals(key.scene()) && this.litNumber == key.number();
        if (lit) {
            g.fill(x, y, x + key.width(), y + key.height(), ManualButtonLook.lighten(
                    this.colour.applyAsInt(GuideStyle.LINK), KEY_LIGHT));
        }
        this.balloon(g, x + BALLOON + 1, y + 1 + BALLOON, key.number(), lit);
    }

    /**
     * What a click on a scene does at that place on the screen: Reset, a layer more or less, or the start of a drag
     * that turns it. Whether the click was on the scene's frame, which takes it whatever it was on.
     */
    boolean press(final GuidePiece.Scene piece, final int x, final int y, final double mx, final double my) {
        if (!new ManualScreenLayout.Rect(x, y, piece.width(), piece.height()).contains(mx, my)) {
            return false;
        }
        final State state = this.state(piece);
        final GuideBlock.Scene scene = piece.scene();
        final int stripY = y + scene.height();
        final ManualScreenLayout.Rect reset = this.reset(piece, x, stripY);
        if (reset.contains(mx, my)) {
            this.pressed = reset;
            state.view = scene.view();
            state.layers = scene.tall();
            state.stale = true;
            AudioEngine.playOnScreen(GuideSounds.CLICK);
            return true;
        }
        if (scene.layers()) {
            final ManualScreenLayout.Rect more = this.more(reset);
            final ManualScreenLayout.Rect fewer = this.fewer(scene, more);
            if (more.contains(mx, my) || fewer.contains(mx, my)) {
                this.pressed = more.contains(mx, my) ? more : fewer;
                state.layers = Math.max(1, Math.min(scene.tall(), state.layers + (more.contains(mx, my) ? 1 : -1)));
                state.stale = true;
                AudioEngine.playOnScreen(GuideSounds.CLICK);
                return true;
            }
        }
        if (my < stripY) {
            this.dragging = piece.id();
            this.dragX = mx;
            this.dragY = my;
        }
        return true;
    }

    /** Turns the scene being dragged as the pointer moves: across turns it round, up and down tilts it. */
    boolean drag(final double mx, final double my) {
        final State state = this.states.get(this.dragging);
        if (state == null) {
            return false;
        }
        state.view = state.view.turned((float) (mx - this.dragX) * TURN, (float) (my - this.dragY) * TURN);
        state.stale = true;
        this.dragX = mx;
        this.dragY = my;
        return true;
    }

    /** Lets go of a scene being dragged and of a button held down. */
    void release() {
        this.dragging = "";
        this.pressed = null;
    }

    /** Brings the scene under the pointer nearer or sends it farther, a turn of the wheel at a time. */
    boolean scroll(final GuidePiece.Scene piece, final int x, final int y, final double mx, final double my,
                   final double turned) {
        if (turned == 0.0 || !new ManualScreenLayout.Rect(x, y, piece.width(), piece.scene().height())
                .contains(mx, my)) {
            return false;
        }
        final State state = this.state(piece);
        state.view = state.view.zoomed(turned > 0.0 ? ZOOM_STEP : 1.0F / ZOOM_STEP);
        state.stale = true;
        return true;
    }

    /** Whether the pointer is on one of a scene's buttons, which take a click. */
    boolean clickable(final GuidePiece.Scene piece, final int x, final int y, final double mx, final double my) {
        final GuideBlock.Scene scene = piece.scene();
        final ManualScreenLayout.Rect reset = this.reset(piece, x, y + scene.height());
        if (reset.contains(mx, my)) {
            return true;
        }
        if (!scene.layers()) {
            return false;
        }
        final ManualScreenLayout.Rect more = this.more(reset);
        return more.contains(mx, my) || this.fewer(scene, more).contains(mx, my);
    }

    /** The Reset button of a scene whose frame stands at that place. */
    ManualScreenLayout.Rect resetButton(final GuidePiece.Scene piece, final int x, final int y) {
        return this.reset(piece, x, y + piece.scene().height());
    }

    /** How the reader has turned a scene, once it has been drawn. */
    Optional<GuideBlock.SceneView> viewOf(final String id) {
        final State state = this.states.get(id);
        return state == null ? Optional.empty() : Optional.of(state.view);
    }

    /** Lets every scene's picture go, for a manual being closed. */
    void close() {
        for (final State state : this.states.values()) {
            if (state.surface != null) {
                state.surface.close();
            }
        }
        this.states.clear();
    }

    private State state(final GuidePiece.Scene piece) {
        return this.states.computeIfAbsent(piece.id(), id -> new State(piece.scene()));
    }

    /** Puts the scene's kept picture on the page, drawing it again first when what it shows has changed. */
    private void picture(final GuiGraphics g, final State state, final int x, final int y, final int width,
                         final int height) {
        final double scale = Minecraft.getInstance().getWindow().getGuiScale();
        final int pixelsWide = Math.max(1, (int) Math.round(width * scale));
        final int pixelsTall = Math.max(1, (int) Math.round(height * scale));
        if (state.surface == null || state.surface.width() != pixelsWide || state.surface.height() != pixelsTall) {
            if (state.surface != null) {
                state.surface.close();
            }
            state.surface = new GpuSurface(pixelsWide, pixelsTall);
            state.stale = true;
        }
        if (state.level == null) {
            state.level = new SceneLevel(state.scene);
        }
        if (state.stale) {
            // What the screen has drawn so far goes onto it before the scene's own target is drawn into.
            g.flush();
            state.level.showLayers(state.layers);
            SceneRenderer.draw(state.level, state.scene, state.view, state.surface);
            state.stale = false;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        state.surface.blit(g, x, y, width, height);
        RenderSystem.disableBlend();
    }

    /**
     * Where each balloon of the layers shown stands on the frame: at the side nearer the part it points at, level with
     * it, and pushed apart from the balloons beside it. A balloon whose part is off the frame, brought too near, is
     * left out until it is back.
     */
    private List<Placed> balloons(final SceneProjection projection, final State state, final int width,
                                  final int height) {
        final List<Placed> left = new ArrayList<>();
        final List<Placed> right = new ArrayList<>();
        for (final GuideBlock.SceneBalloon balloon : state.scene.balloons()) {
            if (balloon.part().y() >= state.layers) {
                continue;
            }
            final SceneProjection.Point target = projection.middleOf(balloon.part());
            if (target.x() < 0 || target.y() < 0 || target.x() > width || target.y() > height) {
                continue;
            }
            final boolean onLeft = target.x() < width / 2.0;
            final int side = onLeft ? EDGE + BALLOON : width - EDGE - BALLOON - 1;
            (onLeft ? left : right).add(new Placed(balloon, target, side, (int) Math.round(target.y())));
        }
        final List<Placed> placed = new ArrayList<>();
        placed.addAll(spread(left, height));
        placed.addAll(spread(right, height));
        return placed;
    }

    /** The balloons of one side, in the order of the parts they point at, pushed apart and kept inside the frame. */
    private static List<Placed> spread(final List<Placed> side, final int height) {
        final List<Placed> sorted = new ArrayList<>(side);
        sorted.sort(Comparator.comparingDouble(placed -> placed.target().y()));
        final int top = BALLOON + 1;
        final int bottom = height - BALLOON - 2;
        final int[] at = new int[sorted.size()];
        for (int i = 0; i < at.length; i++) {
            final int wanted = Math.max(top, Math.min(bottom, sorted.get(i).y()));
            at[i] = i == 0 ? wanted : Math.max(wanted, at[i - 1] + BALLOON_PITCH);
        }
        // Pushed down past the frame's foot, they are pushed back up from it, each above the one under it.
        for (int i = at.length - 1; i >= 0; i--) {
            at[i] = Math.min(at[i], i == at.length - 1 ? bottom : at[i + 1] - BALLOON_PITCH);
        }
        final List<Placed> out = new ArrayList<>();
        for (int i = 0; i < at.length; i++) {
            final Placed placed = sorted.get(i);
            out.add(new Placed(placed.balloon(), placed.target(), placed.x(), at[i]));
        }
        return out;
    }

    /** The box round a lit part, kept inside the frame: a pale line inside a dark one, seen on any colour. */
    private void outline(final GuiGraphics g, final SceneProjection.Bounds bounds, final int x, final int y,
                         final int width, final int height) {
        final int left = x + Math.max(0, (int) Math.floor(bounds.left()) - 1);
        final int top = y + Math.max(0, (int) Math.floor(bounds.top()) - 1);
        final int right = x + Math.min(width, (int) Math.ceil(bounds.right()) + 1);
        final int bottom = y + Math.min(height, (int) Math.ceil(bounds.bottom()) + 1);
        if (right - left < 3 || bottom - top < 3) {
            return;
        }
        final int ink = this.colour.applyAsInt(GuideStyle.INK);
        Draw.outline(g, left, top, right - left, bottom - top, ink);
        Draw.outline(g, left + 1, top + 1, right - left - 2, bottom - top - 2, ManualButtonLook.lighten(ink, 1.0));
    }

    /** The line from a balloon's edge to the point it is pinned to. */
    private void line(final GuiGraphics g, final int fromX, final int fromY, final int toX, final int toY) {
        final int ink = this.colour.applyAsInt(GuideStyle.INK);
        final int steps = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
        for (int i = BALLOON + 1; i <= steps; i++) {
            final int px = fromX + (toX - fromX) * i / steps;
            final int py = fromY + (toY - fromY) * i / steps;
            g.fill(px, py, px + 1, py + 1, ink);
        }
    }

    /** A round balloon with its number, filled with paper, or with the link's colour while it is lit. */
    private void balloon(final GuiGraphics g, final int x, final int y, final int number, final boolean lit) {
        final int ink = this.colour.applyAsInt(GuideStyle.INK);
        final int fill = lit ? ManualButtonLook.lighten(this.colour.applyAsInt(GuideStyle.LINK), BALLOON_LIGHT)
                : this.colour.applyAsInt(GuideStyle.PAPER);
        for (int dy = -BALLOON; dy <= BALLOON; dy++) {
            for (int dx = -BALLOON; dx <= BALLOON; dx++) {
                final double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance <= BALLOON + 0.5) {
                    g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, distance > BALLOON - 0.5 ? ink : fill);
                }
            }
        }
        final String written = String.valueOf(number);
        final int width = GuideFonts.width(this.font, written, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, written, TextSize.SMALL, this.style, x - width / 2 + 1, y - 2,
                ManualButtonLook.markFor(fill, ink, this.colour.applyAsInt(GuideStyle.PAPER)));
    }

    /** The strip along the frame's foot: what the pointer and the wheel do, the layers shown, and Reset. */
    private void strip(final GuiGraphics g, final State state, final GuidePiece.Scene piece, final int x,
                       final int y, final int mx, final int my) {
        final GuideBlock.Scene scene = piece.scene();
        g.fill(x + 1, y, x + piece.width() - 1, y + 1, this.colour.applyAsInt(GuideStyle.RULE));
        final ManualScreenLayout.Rect reset = this.reset(piece, x, y);
        this.button(g, reset, this.text.text(GuideTexts.SCENE_RESET.key()), true, mx, my);
        int room = reset.x() - x - BUTTON_ROOM;
        if (scene.layers()) {
            final ManualScreenLayout.Rect more = this.more(reset);
            final ManualScreenLayout.Rect fewer = this.fewer(scene, more);
            this.button(g, more, "+", state.layers < scene.tall(), mx, my);
            this.button(g, fewer, "-", state.layers > 1, mx, my);
            final String shown = this.layers(state, scene);
            GuideFonts.draw(g, this.font, shown, TextSize.SMALL, this.style, fewer.x() + fewer.width() + GAP, y + 3,
                    this.colour.applyAsInt(GuideStyle.INK));
            room = fewer.x() - x - BUTTON_ROOM;
        }
        GuideFonts.draw(g, this.font, this.fit(this.text.text(GuideTexts.SCENE_HINT.key()), room), TextSize.SMALL,
                this.style, x + 3, y + 3, this.colour.applyAsInt(GuideStyle.FAINT));
    }

    /** "Layer 2 of 5", or "All layers" when none is peeled away. */
    private String layers(final State state, final GuideBlock.Scene scene) {
        return state.layers >= scene.tall() ? this.text.text(GuideTexts.SCENE_ALL_LAYERS.key())
                : this.text.text(GuideTexts.SCENE_LAYER.key(), state.layers, scene.tall());
    }

    /** A word on a button of the strip, sunk while held, lit under the pointer, faded when it leads nowhere. */
    private void button(final GuiGraphics g, final ManualScreenLayout.Rect rect, final String word,
                        final boolean leads, final int mx, final int my) {
        final ManualButtonLook.State state = !leads ? ManualButtonLook.State.DISABLED
                : rect.equals(this.pressed) && rect.contains(mx, my) ? ManualButtonLook.State.PRESSED
                : rect.contains(mx, my) ? ManualButtonLook.State.HOVER : ManualButtonLook.State.NORMAL;
        final ManualButtonLook look = ManualButtonLook.of(this.colour.applyAsInt(GuideStyle.PAPER),
                this.colour.applyAsInt(GuideStyle.INK), this.colour.applyAsInt(GuideStyle.LABEL), state);
        ManualControls.bevel(g, rect.x(), rect.y(), rect.width(), rect.height(), look.fill(), look.light(),
                look.dark());
        final int width = GuideFonts.width(this.font, word, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, word, TextSize.SMALL, this.style, rect.x() + (rect.width() - width) / 2,
                rect.y() + 2, look.mark());
    }

    /** Reset, at the strip's right end. */
    private ManualScreenLayout.Rect reset(final GuidePiece.Scene piece, final int x, final int stripY) {
        final int width = GuideFonts.width(this.font, this.text.text(GuideTexts.SCENE_RESET.key()), TextSize.SMALL,
                this.style) + BUTTON_ROOM;
        return new ManualScreenLayout.Rect(x + piece.width() - width - GAP, stripY + 1, width, BUTTON_HEIGHT);
    }

    /** The button that shows a layer more, just before Reset. */
    private ManualScreenLayout.Rect more(final ManualScreenLayout.Rect reset) {
        return new ManualScreenLayout.Rect(reset.x() - GAP - BUTTON_HEIGHT, reset.y(), BUTTON_HEIGHT, BUTTON_HEIGHT);
    }

    /** The button that hides the top layer, before the words saying how many are shown. */
    private ManualScreenLayout.Rect fewer(final GuideBlock.Scene scene, final ManualScreenLayout.Rect more) {
        // Room for the widest the words can be, so the buttons stand still as the layers change.
        final int words = Math.max(GuideFonts.width(this.font, this.text.text(GuideTexts.SCENE_ALL_LAYERS.key()),
                TextSize.SMALL, this.style), GuideFonts.width(this.font, this.text.text(GuideTexts.SCENE_LAYER.key(),
                scene.tall(), scene.tall()), TextSize.SMALL, this.style));
        return new ManualScreenLayout.Rect(more.x() - 2 * GAP - words - BUTTON_HEIGHT, more.y(), BUTTON_HEIGHT,
                BUTTON_HEIGHT);
    }

    /** The words as they fit the room, cut short when they do not. */
    private String fit(final String words, final int room) {
        if (GuideFonts.width(this.font, words, TextSize.SMALL, this.style) <= room) {
            return words;
        }
        String cut = words;
        while (!cut.isEmpty() && GuideFonts.width(this.font, cut + ELLIPSIS, TextSize.SMALL, this.style) > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.isEmpty() ? "" : cut.stripTrailing() + ELLIPSIS;
    }

    /** How a reader has left one scene, and its picture. */
    private static final class State {

        private final GuideBlock.Scene scene;
        private GuideBlock.SceneView view;
        /** How many layers are shown, counted up from the bottom. */
        private int layers;
        @Nullable
        private GpuSurface surface;
        @Nullable
        private SceneLevel level;
        /** Whether the picture no longer shows what the scene shows now. */
        private boolean stale = true;

        private State(final GuideBlock.Scene scene) {
            this.scene = scene;
            this.view = scene.view();
            this.layers = scene.tall();
        }
    }

    /**
     * A balloon where it stands on the frame, and the point it is pinned to.
     *
     * @param x across the frame, its middle
     * @param y down the frame
     */
    private record Placed(GuideBlock.SceneBalloon balloon, SceneProjection.Point target, int x, int y) {
    }
}
