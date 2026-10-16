/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.effect.EffectCodecs;
import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.core.effect.EffectEditorLayout;
import dev.jstech.core.effect.EffectPhysics;
import dev.jstech.core.effect.EffectSettings;
import dev.jstech.core.effect.Emitter;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The effects editor, in the world, on the real block: the effect plays where it will play, with the world's wind and
 * air, and the card beside it turns each emitter on and off and changes the chosen one's numbers, the effect changing
 * as they move. How the particles move comes from the place's physics, which "Moved by" turns on and off, not from
 * curves drawn by hand. The game's debug lines in the corner say what the effect costs (its particles against the
 * budget, its time a frame) and what acts on it. Save writes the effect's file. The world goes on behind it.
 */
public final class EffectEditorScreen extends Screen {

    private final ResourceLocation id;
    private final EffectAnchors.Placed placed;
    private final Object preview = new Object();
    private final List<Checkbox> emitterBoxes = new ArrayList<>();
    private EffectDefinition definition;
    private EffectEditorLayout card;
    private int chosen;
    private int firstShown;
    private Text status = Text.EMPTY;

    /* The sliders' ranges: rate a second, life and lasting in seconds, speeds, sizes, widths and reaches in blocks. */
    private static final double MOST_RATE = 200;
    private static final double LEAST_LIFE = 0.2;
    private static final double MOST_LIFE = 20;
    private static final double MOST_SPEED = 20;
    private static final double LEAST_SIZE = 0.05;
    private static final double MOST_SIZE = 8;
    private static final double LEAST_WIDTH = 0.01;
    private static final double MOST_WIDTH = 2;
    private static final double LEAST_LASTS = 0.1;
    private static final double MOST_LASTS = 30;
    private static final double LEAST_REACH = 0.5;
    private static final double MOST_REACH = 64;
    private static final int FULL_ALPHA = 255;
    private static final int ALPHA_SHIFT = 24;
    private static final int CHECK_X = 8;
    private static final int LABEL_X = 28;
    private static final int LABEL_DROP = 5;
    private static final int MARK_LENGTH = 30;
    private static final double MARK_SPREAD = Math.toRadians(10);
    private static final float SMALL = 0.75F;
    private static final double WHOLE = 1e-3;
    private static final List<TextKey> MOVE_WORDS = List.of(EffectTexts.GRAVITY, EffectTexts.THE_AIR,
            EffectTexts.THE_WIND);

    public EffectEditorScreen(final ResourceLocation id, final EffectDefinition definition,
                              final EffectAnchors.Placed placed) {
        super(Component.literal(id.toString()));
        this.id = id;
        this.definition = definition;
        this.placed = placed;
        this.card = EffectEditorLayout.of(Math.max(1, definition.emitters().size()), 1, 1);
    }

    /** The effect as it stands in the editor now. */
    public EffectDefinition definition() {
        return this.definition;
    }

    /** The emitter chosen, by its place in the effect. */
    public int chosen() {
        return this.chosen;
    }

    /** Chooses the emitter at {@code index}, its numbers then shown on the card. */
    public void choose(final int index) {
        if (index >= 0 && index < this.definition.emitters().size()) {
            this.chosen = index;
            rebuildWidgets();
        }
    }

    /** Turns the emitter at {@code index} on or off, as its box does. */
    public void enable(final int index, final boolean on) {
        change(index, this.definition.emitters().get(index).withEnabled(on));
    }

    /** Writes the effect's file, as Save does; where it was written, or none if it could not be. */
    public Path save() {
        try {
            final Path written = EffectFileWriter.write(this.id, this.definition);
            this.status = EffectTexts.SAVED.with(Text.literal(EffectFiles.path(this.id)));
            return written;
        } catch (final IOException failed) {
            this.status = EffectTexts.NOT_SAVED.with(EffectTexts.NO_FOLDER.with(Text.literal(
                    this.id.getNamespace())));
            return null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The world shows behind the card, as it plays the effect.
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        final EffectCards.Colours c = EffectCards.PALETTE.get();
        EffectCards.debugLines(g, this.font, debugLines());
        markers(g);
        EffectCards.window(g, this.card.x(), this.card.y(), this.card.x() + EffectEditorLayout.WIDTH,
                this.card.y() + this.card.height());
        final int x = this.card.x() + EffectEditorLayout.INSET;
        Draw.text(g, this.font, Component.literal(this.id.getPath()), x, this.card.y() + EffectEditorLayout.TITLE_Y,
                c.ink(), c.ground());
        small(g, EffectFiles.path(this.id), x, this.card.y() + EffectEditorLayout.FILE_Y, c.soft());
        Draw.text(g, this.font, GameText.component(EffectTexts.EMITTERS), x,
                this.card.y() + EffectEditorLayout.EMITTERS_Y, c.ink(), c.ground());
        for (int row = 0; row < this.card.shown(); row++) {
            final int index = this.firstShown + row;
            if (index >= this.definition.emitters().size()) {
                break;
            }
            final int y = this.card.row(row);
            final boolean isChosen = index == this.chosen;
            if (isChosen) {
                g.fill(this.card.x() + 4, y - 1, this.card.x() + EffectEditorLayout.WIDTH - 4,
                        y + this.card.pitch() - 1, c.chosen());
            }
            Draw.text(g, this.font, Component.literal(this.definition.emitters().get(index).name()),
                    this.card.x() + LABEL_X, y + LABEL_DROP, isChosen ? c.white() : c.ink(),
                    isChosen ? c.chosen() : c.ground());
        }
        if (!this.definition.emitters().isEmpty()) {
            Draw.text(g, this.font, Component.literal(heading(this.definition.emitters().get(this.chosen).name())),
                    x, this.card.heading(), c.ink(), c.ground());
            Draw.text(g, this.font, GameText.component(EffectTexts.MOVED_BY), x, this.card.movedBy(), c.ink(),
                    c.ground());
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (!this.definition.emitters().isEmpty()) {
            for (int i = 0; i < EffectEditorLayout.MOVES; i++) {
                Draw.text(g, this.font, GameText.component(MOVE_WORDS.get(i)), this.card.x() + LABEL_X,
                        this.card.move(i) + LABEL_DROP, c.ink(), c.ground());
            }
        }
        if (!this.status.isEmpty()) {
            small(g, GameText.resolve(this.status), x, this.card.buttons() + EffectEditorLayout.BUTTON_HEIGHT + 3,
                    c.soft());
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        for (int row = 0; row < this.card.shown(); row++) {
            final int y = this.card.row(row);
            if (mouseX >= this.card.x() + LABEL_X && mouseX < this.card.x() + EffectEditorLayout.WIDTH - 4
                    && mouseY >= y - 1 && mouseY < y + this.card.pitch() - 1) {
                choose(this.firstShown + row);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double across, final double down) {
        final int most = Math.max(0, this.definition.emitters().size() - this.card.shown());
        final int next = Math.clamp(this.firstShown - (int) Math.signum(down), 0, most);
        if (next != this.firstShown) {
            this.firstShown = next;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, across, down);
    }

    @Override
    public void removed() {
        ClientEffects.release(this.preview);
        super.removed();
    }

    @Override
    protected void init() {
        this.card = EffectEditorLayout.of(Math.max(1, this.definition.emitters().size()), this.width, this.height);
        this.emitterBoxes.clear();
        if (EffectEngine.held(this.preview).isEmpty()) {
            ClientEffects.preview(this.preview, this.definition, this.placed::at, this.placed::way);
        }
        for (int row = 0; row < this.card.shown(); row++) {
            final int index = this.firstShown + row;
            if (index >= this.definition.emitters().size()) {
                break;
            }
            final Checkbox box = Checkbox.builder(Component.empty(), this.font)
                    .pos(this.card.x() + CHECK_X, this.card.row(row) + 1)
                    .selected(this.definition.emitters().get(index).enabled())
                    .onValueChange((checkbox, on) -> enable(index, on)).build();
            this.emitterBoxes.add(box);
            addRenderableWidget(box);
        }
        if (!this.definition.emitters().isEmpty()) {
            sliders(this.definition.emitters().get(this.chosen));
            moves(this.definition.emitters().get(this.chosen));
        }
        final int x = this.card.x() + EffectEditorLayout.INSET;
        addRenderableWidget(Button.builder(GameText.component(EffectTexts.SAVE), button -> save())
                .bounds(x, this.card.buttons(), EffectEditorLayout.BUTTON_WIDTH, EffectEditorLayout.BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(GameText.component(EffectTexts.DONE), button -> onClose())
                .bounds(x + EffectEditorLayout.BUTTON_WIDTH + EffectEditorLayout.BUTTON_GAP, this.card.buttons(),
                        EffectEditorLayout.BUTTON_WIDTH, EffectEditorLayout.BUTTON_HEIGHT).build());
    }

    /* The chosen emitter's four sliders: a particle emitter's rate, life, speed and size, another's own four. */
    private void sliders(final Emitter emitter) {
        final int index = this.chosen;
        if (emitter.kind() == Emitter.Kind.PARTICLES) {
            final Emitter.Emission e = emitter.emission();
            slider(0, e.rate() / MOST_RATE, v -> EffectTexts.RATE.with(number(v * MOST_RATE)),
                    v -> change(index, current(index).withEmission(current(index).emission().withRate(
                            Math.round(v * MOST_RATE)))));
            slider(1, unspan(e.life().max(), LEAST_LIFE, MOST_LIFE),
                    v -> EffectTexts.LIFE.with(number(span(v, LEAST_LIFE, MOST_LIFE))),
                    v -> change(index, current(index).withEmission(current(index).emission().withLife(
                            keepRatio(current(index).emission().life(), span(v, LEAST_LIFE, MOST_LIFE))))));
            slider(2, e.speed().max() / MOST_SPEED, v -> EffectTexts.LEAVES_AT.with(number(v * MOST_SPEED)),
                    v -> change(index, current(index).withEmission(current(index).emission().withSpeed(
                            keepRatio(current(index).emission().speed(), v * MOST_SPEED)))));
            final double largest = largest(emitter.look().size());
            slider(3, unspan(largest, LEAST_SIZE, MOST_SIZE), v -> {
                final double now = span(v, LEAST_SIZE, MOST_SIZE);
                final double factor = now / Math.max(LEAST_SIZE, largest(current(index).look().size()));
                return EffectTexts.SIZE.with(number(smallest(current(index).look().size()) * factor), number(now));
            }, v -> {
                final double factor = span(v, LEAST_SIZE, MOST_SIZE)
                        / Math.max(LEAST_SIZE, largest(current(index).look().size()));
                change(index, current(index).withLook(current(index).look().withSizeTimes(factor)));
            });
            return;
        }
        final Emitter.Extra extra = emitter.extra();
        final boolean aimed = emitter.kind() == Emitter.Kind.BEAM || emitter.kind() == Emitter.Kind.ARC;
        slider(0, unspan(emitter.look().width(), LEAST_WIDTH, MOST_WIDTH),
                v -> EffectTexts.WIDTH.with(number(span(v, LEAST_WIDTH, MOST_WIDTH))),
                v -> change(index, current(index).withLook(current(index).look().withWidth(span(v, LEAST_WIDTH,
                        MOST_WIDTH)))));
        slider(1, unspan(extra.fade(), LEAST_LASTS, MOST_LASTS),
                v -> EffectTexts.LASTS.with(number(span(v, LEAST_LASTS, MOST_LASTS))),
                v -> change(index, current(index).withExtra(current(index).extra().withFade(span(v, LEAST_LASTS,
                        MOST_LASTS)))));
        slider(2, unspan(aimed ? extra.length() : extra.radius(), LEAST_REACH, MOST_REACH),
                v -> EffectTexts.REACH.with(number(span(v, LEAST_REACH, MOST_REACH))),
                v -> change(index, current(index).withExtra(aimed
                        ? current(index).extra().withLength(span(v, LEAST_REACH, MOST_REACH))
                        : current(index).extra().withRadius(span(v, LEAST_REACH, MOST_REACH)))));
        final boolean screen = emitter.kind() == Emitter.Kind.SCREEN;
        final double strength = screen ? extra.shake()
                : (double) (emitter.look().colour().getFirst() >>> ALPHA_SHIFT) / FULL_ALPHA;
        slider(3, strength, v -> EffectTexts.STRENGTH.with(number(v)),
                v -> change(index, screen ? current(index).withExtra(current(index).extra().withShake(v))
                        : current(index).withLook(current(index).look().withAlpha((int) Math.round(v
                                * FULL_ALPHA)))));
    }

    /* What moves the chosen emitter's particles: the place's pull, its air, its wind. */
    private void moves(final Emitter emitter) {
        final int index = this.chosen;
        final EffectPhysics.Moves moves = emitter.motion().moves();
        final boolean[] on = {moves.gravity(), moves.air(), moves.wind()};
        for (int i = 0; i < EffectEditorLayout.MOVES; i++) {
            final int which = i;
            final Checkbox box = Checkbox.builder(Component.empty(), this.font)
                    .pos(this.card.x() + CHECK_X, this.card.move(i) + 1).selected(on[i])
                    .onValueChange((checkbox, value) -> {
                        final EffectPhysics.Moves now = current(index).motion().moves();
                        final EffectPhysics.Moves changed = new EffectPhysics.Moves(
                                which == 0 ? value : now.gravity(), which == 1 ? value : now.air(),
                                which == 2 ? value : now.wind());
                        change(index, current(index).withMotion(current(index).motion().withMoves(changed)));
                    }).build();
            box.active = emitter.kind() == Emitter.Kind.PARTICLES;
            addRenderableWidget(box);
        }
    }

    private void slider(final int index, final double value, final DoubleFunction<Text> label,
                        final DoubleConsumer apply) {
        final int x = this.card.x() + EffectEditorLayout.INSET;
        addRenderableWidget(new Knob(x, this.card.slider(index), EffectEditorLayout.WIDTH - 2
                * EffectEditorLayout.INSET, this.card.widgetHeight(), Math.clamp(value, 0.0, 1.0), label, apply));
    }

    private Emitter current(final int index) {
        return this.definition.emitters().get(index);
    }

    /* Has the emitter at {@code index} be {@code changed}, and the effect playing take it on at once. */
    private void change(final int index, final Emitter changed) {
        this.definition = this.definition.withEmitter(index, changed);
        EffectEngine.held(this.preview).ifPresent(effect -> effect.replace(this.definition));
        this.status = Text.EMPTY;
    }

    /* What the effect costs and what acts on it, as the game's debug lines say. */
    private List<Component> debugLines() {
        final List<Component> lines = new ArrayList<>();
        lines.add(GameText.component(EffectTexts.EFFECT.with(Text.literal(this.id.toString()))));
        lines.add(GameText.component(EffectTexts.PARTICLES.with(Text.literal(String.valueOf(
                EffectEngine.particles())), Text.literal(String.valueOf(EffectSettings.particles())))));
        lines.add(GameText.component(EffectTexts.COST.with(number(EffectEngine.costMillis()))));
        final EffectPlace place = EffectEngine.held(this.preview).map(effect -> effect.place)
                .orElse(EffectPlace.STILL);
        final Wind wind = place.wind();
        lines.add(GameText.component(wind.speed() <= 0 ? EffectTexts.NO_WIND.text()
                : EffectTexts.WIND.with(number(wind.speed()), EffectTexts.COMPASS.get(wind.fromPoint()))));
        lines.add(GameText.component(EffectTexts.AIR.with(number(place.physics().density()),
                number(place.physics().gravity()))));
        return lines;
    }

    /* Where the effect starts, in pink on the world: its way out, as a narrow cone, and the chosen emitter's name. */
    private void markers(final GuiGraphics g) {
        final float[] from = EffectCards.onScreen(this.placed.at(), this.width, this.height);
        final float[] to = EffectCards.onScreen(this.placed.at().add(this.placed.way().scale(1.5)), this.width,
                this.height);
        if (from == null || to == null) {
            return;
        }
        final double angle = Math.atan2(to[1] - from[1], to[0] - from[0]);
        for (final double side : new double[] {-MARK_SPREAD, MARK_SPREAD}) {
            EffectCards.dotted(g, from[0], from[1], (float) (from[0] + MARK_LENGTH * Math.cos(angle + side)),
                    (float) (from[1] + MARK_LENGTH * Math.sin(angle + side)));
        }
        if (!this.definition.emitters().isEmpty()) {
            final String name = this.definition.emitters().get(this.chosen).name();
            small(g, name, (int) from[0] - this.font.width(name), (int) from[1] - MARK_LENGTH + 8,
                    EffectCards.PALETTE.get().marker());
        }
    }

    private void small(final GuiGraphics g, final String words, final int x, final int y, final int colour) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(SMALL, SMALL, 1);
        Draw.text(g, this.font, words, 0, 0, colour);
        g.pose().popPose();
    }

    /* A number as the card says it: whole where it is whole, else to one place, or two below one. */
    private static Text number(final double value) {
        if (Math.abs(value - Math.rint(value)) < WHOLE) {
            return Text.literal(String.valueOf(Math.round(value)));
        }
        return Text.literal(String.format(Locale.ROOT, Math.abs(value) < 1 ? "%.2f" : "%.1f", value));
    }

    private static String heading(final String name) {
        return name.isEmpty() ? name : Character.toString(Character.toUpperCase(name.codePointAt(0)))
                + name.substring(Character.charCount(name.codePointAt(0)));
    }

    private static double span(final double share, final double least, final double most) {
        return least + (most - least) * share;
    }

    private static double unspan(final double value, final double least, final double most) {
        return (value - least) / (most - least);
    }

    private static EffectCodecs.Range keepRatio(final EffectCodecs.Range range, final double most) {
        final double ratio = range.max() <= 0 ? 1 : range.min() / range.max();
        return new EffectCodecs.Range(most * ratio, most);
    }

    private static double largest(final List<Double> sizes) {
        return sizes.stream().mapToDouble(Double::doubleValue).max().orElse(LEAST_SIZE);
    }

    private static double smallest(final List<Double> sizes) {
        return sizes.stream().mapToDouble(Double::doubleValue).min().orElse(LEAST_SIZE);
    }

    /* A slider of the game's own look, its value from 0 to 1, saying its number and changing the effect as it moves. */
    private static final class Knob extends AbstractSliderButton {

        private final DoubleFunction<Text> label;
        private final DoubleConsumer apply;

        Knob(final int x, final int y, final int width, final int height, final double value,
             final DoubleFunction<Text> label, final DoubleConsumer apply) {
            super(x, y, width, height, Component.empty(), value);
            this.label = label;
            this.apply = apply;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(GameText.component(this.label.apply(this.value)));
        }

        @Override
        protected void applyValue() {
            this.apply.accept(this.value);
        }
    }
}
