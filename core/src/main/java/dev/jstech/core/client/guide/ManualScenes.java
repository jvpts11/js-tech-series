/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.core.JsCore;
import dev.jstech.core.api.client.GpuSurface;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.model.kit.SceneRenderer;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideLayout;
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
import java.util.OptionalInt;
import java.util.function.ToIntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    /**
     * A balloon's radius, eleven pixels across so its small number clears the ring at every scale of the screen, and
     * how far apart two balloons on one side stand at least.
     */
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
    /**
     * A scene shown step by step: the band of its step over the picture's top, how dark, the timeline's buttons and
     * their marks, how far the track stands in from what is beside it, and what a scene shown in the world is known as.
     */
    private static final int CAPTION_BAND = 10;
    private static final int RGB = 0xFFFFFF;
    private static final int BAND_ALPHA = 0xC0 << 24;
    private static final int STEP_BUTTON = 11;
    private static final int TRACK_INSET = 2;
    private static final String BACK = "|<";
    private static final String PLAY = ">";
    private static final String PAUSE = "||";
    private static final String FORWARD = ">|";
    private static final String WORLD_PATH = "manual_scene/";
    private static final long MILLIS_PER_TICK = 50L;

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
        final int height = pictureHeight(piece) - 1;
        state.play(Util.getMillis());
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
        if (scene.steps().isEmpty()) {
            this.strip(g, state, piece, x, y + pictureHeight(piece), mx, my);
        } else {
            this.stepCaption(g, state, x + 1, y + 1, width);
            this.timeline(g, state, piece, x, y + pictureHeight(piece), mx, my);
        }
        final Optional<Component> tip = this.buttonTip(piece, x, y, mx, my);
        if (tip.isPresent()) {
            return List.of(tip.get());
        }
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
        final int stripY = y + pictureHeight(piece);
        final Optional<ManualScreenLayout.Rect> world = this.inWorld(piece, x, stripY);
        if (world.isPresent() && world.get().contains(mx, my)) {
            this.pressed = world.get();
            MultiblockShows.toggle(worldId(piece), scene.inWorld(state.step));
            AudioEngine.playOnScreen(GuideSounds.CLICK);
            return true;
        }
        if (!scene.steps().isEmpty()) {
            return this.pressTimeline(piece, state, x, stripY, mx, my);
        }
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
            final ManualScreenLayout.Rect more = this.more(world.orElse(reset));
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
        if (turned == 0.0 || !new ManualScreenLayout.Rect(x, y, piece.width(), pictureHeight(piece))
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
        final int stripY = y + pictureHeight(piece);
        if (this.inWorld(piece, x, stripY).filter(rect -> rect.contains(mx, my)).isPresent()) {
            return true;
        }
        if (!scene.steps().isEmpty()) {
            final Timeline line = this.timelineOf(piece, x, stripY);
            return line.back().contains(mx, my) || line.play().contains(mx, my) || line.forward().contains(mx, my)
                    || line.track().contains(mx, my);
        }
        final ManualScreenLayout.Rect reset = this.reset(piece, x, y + pictureHeight(piece));
        if (reset.contains(mx, my)) {
            return true;
        }
        if (!scene.layers()) {
            return false;
        }
        final ManualScreenLayout.Rect more = this.more(this.inWorld(piece, x, stripY).orElse(reset));
        return more.contains(mx, my) || this.fewer(scene, more).contains(mx, my);
    }

    /** The Reset button of a scene whose frame stands at that place. */
    ManualScreenLayout.Rect resetButton(final GuidePiece.Scene piece, final int x, final int y) {
        return this.reset(piece, x, y + pictureHeight(piece));
    }

    /** The button that shows a layer more of a scene drawn at that place, or the one that hides its top layer. */
    ManualScreenLayout.Rect layerButton(final GuidePiece.Scene piece, final int x, final int y, final boolean more) {
        final int stripY = y + pictureHeight(piece);
        final ManualScreenLayout.Rect shown = this.more(this.inWorld(piece, x, stripY).orElse(this.reset(piece, x,
                stripY)));
        return more ? shown : this.fewer(piece.scene(), shown);
    }

    /** How the reader has turned a scene, once it has been drawn. */
    Optional<GuideBlock.SceneView> viewOf(final String id) {
        final State state = this.states.get(id);
        return state == null ? Optional.empty() : Optional.of(state.view);
    }

    /** How many layers of a scene are shown, counted up from the bottom, once it has been drawn. */
    OptionalInt layersOf(final String id) {
        final State state = this.states.get(id);
        return state == null ? OptionalInt.empty() : OptionalInt.of(state.layers);
    }

    /** The step a scene shown step by step is at, from 0, once it has been drawn. */
    OptionalInt stepOf(final String id) {
        final State state = this.states.get(id);
        return state == null ? OptionalInt.empty() : OptionalInt.of(state.step);
    }

    /** Whether a scene shown step by step is playing, once it has been drawn. */
    boolean playing(final String id) {
        final State state = this.states.get(id);
        return state != null && state.playing;
    }

    /** The timeline's buttons of a scene drawn at that place: the step before, play or pause, the next step. */
    List<ManualScreenLayout.Rect> timelineButtons(final GuidePiece.Scene piece, final int x, final int y) {
        final Timeline line = this.timelineOf(piece, x, y + pictureHeight(piece));
        return List.of(line.back(), line.play(), line.forward());
    }

    /** The button that shows a scene drawn at that place in the world, where anything shows multiblocks there. */
    Optional<ManualScreenLayout.Rect> inWorldButton(final GuidePiece.Scene piece, final int x, final int y) {
        return this.inWorld(piece, x, y + pictureHeight(piece));
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
        // A scene shown step by step stands as its step says; another always stands whole.
        if (state.level == null || state.levelStep != state.step) {
            state.level = new SceneLevel(state.scene, state.scene.blocksAt(state.step));
            state.levelStep = state.step;
            state.stale = true;
        }
        if (state.stale) {
            // What the screen has drawn so far goes onto it before the scene's own target is drawn into.
            g.flush();
            state.level.showLayers(state.layers);
            final SceneProjection projection = new SceneProjection(state.scene, state.view, pixelsWide, pixelsTall);
            SceneRenderer.draw(state.surface, SceneRenderer.View.of(projection.middle(), state.view.yaw(),
                    state.view.pitch(), (float) projection.scale()), new SceneLevelPainter(state.level));
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
        final Optional<ManualScreenLayout.Rect> world = this.inWorld(piece, x, y);
        world.ifPresent(rect -> this.worldButton(g, piece, rect, mx, my));
        final ManualScreenLayout.Rect beside = world.orElse(reset);
        int room = beside.x() - x - BUTTON_ROOM;
        if (scene.layers()) {
            final ManualScreenLayout.Rect more = this.more(beside);
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

    /** Over the top of a scene shown step by step, the step it is at: "2 of 3: Put the chest beside the furnace". */
    private void stepCaption(final GuiGraphics g, final State state, final int x, final int y, final int width) {
        final List<GuideBlock.SceneStep> steps = state.scene.steps();
        final String said = this.text.text(GuideTexts.SCENE_STEP.key(), state.step + 1, steps.size(),
                this.text.text(steps.get(state.step).captionKey()));
        g.fill(x, y, x + width, y + CAPTION_BAND, this.colour.applyAsInt(GuideStyle.INK) & RGB | BAND_ALPHA);
        GuideFonts.draw(g, this.font, this.fit(said, width - 2 * GAP), TextSize.SMALL, this.style, x + GAP, y + 2,
                this.colour.applyAsInt(GuideStyle.PAPER));
    }

    /**
     * The strip of a scene shown step by step: the step before, play or pause, the next step, the timeline with a
     * mark at each step and the playhead past those done, which step of how many, and In world.
     */
    private void timeline(final GuiGraphics g, final State state, final GuidePiece.Scene piece, final int x,
                          final int y, final int mx, final int my) {
        final List<GuideBlock.SceneStep> steps = state.scene.steps();
        g.fill(x + 1, y, x + piece.width() - 1, y + 1, this.colour.applyAsInt(GuideStyle.RULE));
        final Timeline line = this.timelineOf(piece, x, y);
        this.button(g, line.back(), BACK, state.step > 0, mx, my);
        this.button(g, line.play(), state.playing ? PAUSE : PLAY, true, mx, my);
        this.button(g, line.forward(), FORWARD, state.step < steps.size() - 1, mx, my);
        final ManualScreenLayout.Rect track = line.track();
        final int middle = track.y() + track.height() / 2;
        final int rule = this.colour.applyAsInt(GuideStyle.RULE);
        final int ink = this.colour.applyAsInt(GuideStyle.INK);
        final int link = this.colour.applyAsInt(GuideStyle.LINK);
        g.fill(track.x(), middle, track.x() + track.width(), middle + 1, rule);
        final int head = track.x() + (int) Math.round(track.width() * state.played(Util.getMillis()));
        g.fill(track.x(), middle - 1, head, middle + 1, link);
        for (int k = 0; k < steps.size(); k++) {
            final int mark = markX(track, k, steps.size());
            g.fill(mark, middle - 2, mark + 1, middle + 3, k <= state.step ? ink : rule);
        }
        g.fill(head - 1, track.y() + 1, head + 2, track.y() + track.height() - 1, link);
        final String count = this.text.text(GuideTexts.SCENE_STEP_SHORT.key(), state.step + 1, steps.size());
        GuideFonts.draw(g, this.font, count, TextSize.SMALL, this.style, line.countX(), y + 3,
                this.colour.applyAsInt(GuideStyle.FAINT));
        this.inWorld(piece, x, y).ifPresent(rect -> this.worldButton(g, piece, rect, mx, my));
    }

    /** What a click on the timeline does: a step back or on, play or pause, or the step marked nearest the click. */
    private boolean pressTimeline(final GuidePiece.Scene piece, final State state, final int x, final int stripY,
                                  final double mx, final double my) {
        final Timeline line = this.timelineOf(piece, x, stripY);
        final int last = state.scene.steps().size() - 1;
        final long now = Util.getMillis();
        if (line.back().contains(mx, my) && state.step > 0) {
            this.pressed = line.back();
            state.goTo(state.step - 1, now);
        } else if (line.forward().contains(mx, my) && state.step < last) {
            this.pressed = line.forward();
            state.goTo(state.step + 1, now);
        } else if (line.play().contains(mx, my)) {
            this.pressed = line.play();
            state.toggle(now);
        } else if (line.track().contains(mx, my)) {
            int nearest = 0;
            for (int k = 1; k <= last; k++) {
                if (Math.abs(markX(line.track(), k, last + 1) - mx) < Math.abs(markX(line.track(), nearest,
                        last + 1) - mx)) {
                    nearest = k;
                }
            }
            state.goTo(nearest, now);
        } else {
            if (my < stripY) {
                this.dragging = piece.id();
                this.dragX = mx;
                this.dragY = my;
            }
            return true;
        }
        AudioEngine.playOnScreen(GuideSounds.CLICK);
        return true;
    }

    /** Where the parts of a scene's timeline stand, its strip at {@code stripY}. */
    private Timeline timelineOf(final GuidePiece.Scene piece, final int x, final int stripY) {
        final int top = stripY + 1;
        final ManualScreenLayout.Rect back = new ManualScreenLayout.Rect(x + GAP, top, STEP_BUTTON, BUTTON_HEIGHT);
        final ManualScreenLayout.Rect play = new ManualScreenLayout.Rect(back.x() + STEP_BUTTON + 1, top,
                STEP_BUTTON, BUTTON_HEIGHT);
        final ManualScreenLayout.Rect forward = new ManualScreenLayout.Rect(play.x() + STEP_BUTTON + 1, top,
                STEP_BUTTON, BUTTON_HEIGHT);
        final int end = this.inWorld(piece, x, stripY).map(ManualScreenLayout.Rect::x)
                .orElse(x + piece.width() - GAP);
        final int steps = piece.scene().steps().size();
        // Room for the widest the count can be, so the track stands still as the steps go by.
        final int countWidth = GuideFonts.width(this.font, this.text.text(GuideTexts.SCENE_STEP_SHORT.key(), steps,
                steps), TextSize.SMALL, this.style);
        final int countX = end - GAP - countWidth;
        final int trackX = forward.x() + STEP_BUTTON + GAP + TRACK_INSET;
        return new Timeline(back, play, forward, new ManualScreenLayout.Rect(trackX, top,
                Math.max(1, countX - GAP - TRACK_INSET - trackX), BUTTON_HEIGHT), countX);
    }

    /**
     * Where the mark of step {@code step} of {@code steps} stands: the first at the track's start, the last at its
     * end.
     */
    private static int markX(final ManualScreenLayout.Rect track, final int step, final int steps) {
        return steps < 2 ? track.x() : track.x() + (track.width() - 1) * step / (steps - 1);
    }

    /**
     * The button that shows a scene's blocks in the world, at its strip's right end, before Reset when the scene has
     * one; none where nothing shows multiblocks in the world.
     */
    private Optional<ManualScreenLayout.Rect> inWorld(final GuidePiece.Scene piece, final int x, final int stripY) {
        if (!MultiblockShows.canShowInWorld()) {
            return Optional.empty();
        }
        final int width = GuideFonts.width(this.font, this.text.text(GuideTexts.SCENE_IN_WORLD.key()),
                TextSize.SMALL, this.style) + BUTTON_ROOM;
        final int right = piece.scene().steps().isEmpty() ? this.reset(piece, x, stripY).x() - GAP
                : x + piece.width() - GAP;
        return Optional.of(new ManualScreenLayout.Rect(right - width, stripY + 1, width, BUTTON_HEIGHT));
    }

    /** In world, drawn sunk while the scene is shown there. */
    private void worldButton(final GuiGraphics g, final GuidePiece.Scene piece, final ManualScreenLayout.Rect rect,
                             final int mx, final int my) {
        final boolean shown = MultiblockShows.shownInWorld().filter(worldId(piece)::equals).isPresent();
        this.button(g, rect, this.text.text(GuideTexts.SCENE_IN_WORLD.key()), shown ? ManualButtonLook.State.PRESSED
                : this.stateOf(rect, true, mx, my));
    }

    /** What the button under the pointer does, for its tooltip: In world, or a button of the timeline. */
    private Optional<Component> buttonTip(final GuidePiece.Scene piece, final int x, final int y, final int mx,
                                          final int my) {
        final int stripY = y + pictureHeight(piece);
        if (this.inWorld(piece, x, stripY).filter(rect -> rect.contains(mx, my)).isPresent()) {
            return Optional.of(Component.literal(this.text.text(GuideTexts.SCENE_IN_WORLD_TIP.key())));
        }
        if (piece.scene().steps().isEmpty()) {
            return Optional.empty();
        }
        final Timeline line = this.timelineOf(piece, x, stripY);
        final String key = line.back().contains(mx, my) ? GuideTexts.SCENE_BACK.key()
                : line.play().contains(mx, my) ? (this.playing(piece.id()) ? GuideTexts.SCENE_PAUSE.key()
                : GuideTexts.SCENE_PLAY.key()) : line.forward().contains(mx, my) ? GuideTexts.SCENE_FORWARD.key() : "";
        return key.isEmpty() ? Optional.empty() : Optional.of(Component.literal(this.text.text(key)));
    }

    /** What a scene is known as while it is shown in the world. */
    private static ResourceLocation worldId(final GuidePiece.Scene piece) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, WORLD_PATH + Integer.toHexString(
                piece.id().hashCode()));
    }

    /** "Layer 2 of 5", or "All layers" when none is peeled away. */
    private String layers(final State state, final GuideBlock.Scene scene) {
        return state.layers >= scene.tall() ? this.text.text(GuideTexts.SCENE_ALL_LAYERS.key())
                : this.text.text(GuideTexts.SCENE_LAYER.key(), state.layers, scene.tall());
    }

    /** A word on a button of the strip, sunk while held, lit under the pointer, faded when it leads nowhere. */
    private void button(final GuiGraphics g, final ManualScreenLayout.Rect rect, final String word,
                        final boolean leads, final int mx, final int my) {
        this.button(g, rect, word, this.stateOf(rect, leads, mx, my));
    }

    /** How a button of the strip is drawn: faded, sunk while held, lit under the pointer, or as it is. */
    private ManualButtonLook.State stateOf(final ManualScreenLayout.Rect rect, final boolean leads, final int mx,
                                          final int my) {
        return !leads ? ManualButtonLook.State.DISABLED
                : rect.equals(this.pressed) && rect.contains(mx, my) ? ManualButtonLook.State.PRESSED
                : rect.contains(mx, my) ? ManualButtonLook.State.HOVER : ManualButtonLook.State.NORMAL;
    }

    /** A word on a button of the strip, drawn as {@code state} says. */
    private void button(final GuiGraphics g, final ManualScreenLayout.Rect rect, final String word,
                        final ManualButtonLook.State state) {
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

    /**
     * How tall a scene's picture is: the frame its page gave it, which is shorter than the scene asked when a page
     * holds no more, less the strip of its controls under it.
     */
    private static int pictureHeight(final GuidePiece.Scene piece) {
        return piece.height() - GuideLayout.SCENE_STRIP;
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
        /** A scene shown step by step: the step it is at, the step its level stands as, and whether it plays. */
        private int step;
        private int levelStep = -1;
        private boolean playing;
        private long stepStarted;

        private State(final GuideBlock.Scene scene) {
            this.scene = scene;
            this.view = scene.view();
            this.layers = scene.tall();
            // A scene shown step by step plays at once, unless the reader asked for less movement.
            this.playing = !scene.steps().isEmpty() && !MotionClock.reduced();
            this.stepStarted = Util.getMillis();
        }

        /** Goes on to the next step once this one has been shown its time; stops at the last. */
        private void play(final long now) {
            if (!this.playing) {
                return;
            }
            final List<GuideBlock.SceneStep> steps = this.scene.steps();
            if (now - this.stepStarted < steps.get(this.step).ticks() * MILLIS_PER_TICK) {
                return;
            }
            if (this.step < steps.size() - 1) {
                this.step++;
                this.stepStarted = now;
            } else {
                this.playing = false;
            }
        }

        private void goTo(final int step, final long now) {
            this.step = step;
            this.stepStarted = now;
        }

        /** Pauses, or plays on; at the last step, from the first again. */
        private void toggle(final long now) {
            if (this.playing) {
                this.playing = false;
                return;
            }
            if (this.step >= this.scene.steps().size() - 1) {
                this.step = 0;
            }
            this.stepStarted = now;
            this.playing = true;
        }

        /** How far along the timeline it is, from 0 at the first step to 1 at the last, part of a step counted in. */
        private double played(final long now) {
            final List<GuideBlock.SceneStep> steps = this.scene.steps();
            if (steps.size() < 2) {
                return this.playing ? Math.min(1.0, (now - this.stepStarted)
                        / (double) (steps.getFirst().ticks() * MILLIS_PER_TICK)) : 1.0;
            }
            final double into = !this.playing || this.step >= steps.size() - 1 ? 0.0
                    : Math.min(1.0, (now - this.stepStarted) / (double) (steps.get(this.step).ticks()
                    * MILLIS_PER_TICK));
            return Math.min(1.0, (this.step + into) / (steps.size() - 1));
        }
    }

    /**
     * Where the parts of a scene's timeline stand.
     *
     * @param back    the step before
     * @param play    play or pause
     * @param forward the next step
     * @param track   the track the steps are marked along
     * @param countX  where "2/3" is written
     */
    private record Timeline(ManualScreenLayout.Rect back, ManualScreenLayout.Rect play,
                            ManualScreenLayout.Rect forward, ManualScreenLayout.Rect track, int countX) {
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
