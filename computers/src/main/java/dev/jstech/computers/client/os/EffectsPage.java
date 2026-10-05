/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.EffectsPages;
import dev.jstech.computers.gui.layout.EffectsPageLayout;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.ScrollPanel;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * A system's own page for its visual effects, inside the Settings window: built from the page that system's
 * definition gives, drawn in the window's skin, and sending each change to the machine as it is made, the way the
 * rest of the window does.
 *
 * <p>The systems whose page was a dialog keep its buttons. Cancel puts back what the page showed when it was opened
 * or last applied, Apply makes what it shows now the thing Cancel goes back to, and OK and Close only leave it.
 */
final class EffectsPage {

    private final BiConsumer<String, String> set;
    private final Runnable back;
    /** The rows' own content, which scrolls when it outgrows the window. */
    private final ScrollPanel scroll = new ScrollPanel();
    /** What the page showed when it was opened or last applied, which Cancel goes back to. */
    private DesktopEffects kept = DesktopEffects.ALL_ON;
    /** Frames XP's preset as the player last picked it while the page is open, or -1 to read it off the boxes. */
    private int preset = -1;
    /** The control of each row, in row order, for a test to aim at; null for a row with none. */
    private final List<UiComponent> controls = new ArrayList<>();
    /** The dialog's buttons along the foot, left to right, and the way back at the title's right. */
    private final List<Button> footer = new ArrayList<>();
    @Nullable
    private Button backButton;

    /** The small text every row is written in. */
    private static final float SMALL = Texts.SMALL;

    EffectsPage(final BiConsumer<String, String> set, final Runnable back) {
        this.set = set;
        this.back = back;
    }

    /** Opens the page on what the machine has now, which is what Cancel goes back to until Apply. */
    void open(final DesktopEffects now) {
        kept = now;
        preset = -1;
        scroll.setScroll(0);
    }

    /** How far the rows are scrolled, part of what the window rebuilds the page for. */
    int scrolled() {
        return scroll.scroll();
    }

    /** The control of row {@code index}, or null. */
    @Nullable
    UiComponent control(final int index) {
        return index >= 0 && index < controls.size() ? controls.get(index) : null;
    }

    /** The dialog's button {@code index} along the foot, or null. */
    @Nullable
    Button footerButton(final int index) {
        return index >= 0 && index < footer.size() ? footer.get(index) : null;
    }

    @Nullable
    Button backButton() {
        return backButton;
    }

    /** Builds the page into {@code target} over the area {@code x}, {@code y}, {@code w} by {@code h}. */
    void build(final Panel target, final EffectsPages.Page page, final int x, final int y, final int w,
               final int h, final Font font, final DesktopEffects now) {
        controls.clear();
        footer.clear();
        backButton = null;
        final int textW = (int) ((w - EffectsPageLayout.BOX - 4) / SMALL);
        final EffectsPageLayout.Placed placed = EffectsPageLayout.place(page, w, h, row -> lines(font, row, textW));
        final int room = EffectsPageLayout.titleRoom(page, w);
        final String title = Texts.clip(font, GameText.resolve(page.title()),
                (int) (room / EffectsPageLayout.TITLE_SCALE));
        target.add(new Label(title).setScale(EffectsPageLayout.TITLE_SCALE)).setBounds(x, y + 3, room, 8);
        if (page.footer() == EffectsPages.Footer.NONE) {
            backButton = target.add(new Button(GameText.resolve(EffectsPageTexts.BACK), back).setLabelScale(SMALL));
            backButton.setBounds(x + w - EffectsPageLayout.BACK_W, y, EffectsPageLayout.BACK_W,
                    EffectsPageLayout.CONTROL_H);
        }
        scroll.clear();
        target.add(scroll);
        scroll.setStep(EffectsPageLayout.LINE_H * 2).setContentHeight(placed.contentHeight());
        scroll.setBounds(x, y + placed.scrollTop(), w, placed.scrollHeight());
        for (int i = 0; i < page.rows().size(); i++) {
            final int top = scroll.contentY(placed.rowY().get(i));
            controls.add(row(page, page.rows().get(i), x, top, w, placed.rowH().get(i), font, now));
        }
        if (placed.footerY() >= 0) {
            final List<TextKey> words = page.footer() == EffectsPages.Footer.OK_CANCEL_APPLY
                    ? List.of(EffectsPageTexts.OK, EffectsPageTexts.CANCEL, EffectsPageTexts.APPLY)
                    : List.of(EffectsPageTexts.OK, EffectsPageTexts.APPLY, EffectsPageTexts.CLOSE);
            for (int i = 0; i < words.size(); i++) {
                final TextKey word = words.get(i);
                final Button button = target.add(new Button(GameText.resolve(word), () -> press(page, word, now))
                        .setLabelScale(SMALL));
                button.setBounds(x + EffectsPageLayout.footerX(w, i, words.size()), y + placed.footerY(),
                        EffectsPageLayout.FOOTER_BUTTON_W, EffectsPageLayout.CONTROL_H);
                if (word == EffectsPageTexts.APPLY) {
                    button.setEnabled(!now.equals(kept));
                }
                footer.add(button);
            }
        }
    }

    /** The lines a row's text takes at that width, for the rows that wrap. */
    static int lines(final Font font, final EffectsPages.IRow row, final int textW) {
        final TextKey text = switch (row) {
            case EffectsPages.Check check -> check.label();
            case EffectsPages.Note note -> note.text();
            case EffectsPages.Toggle toggle -> toggle.description();
            default -> null;
        };
        if (text == null) {
            return 1;
        }
        final int room = row instanceof EffectsPages.Toggle
                ? textW - (int) ((EffectsPageLayout.SWITCH_W + 4) / SMALL) : textW;
        return Math.max(1, font.split(Component.literal(GameText.resolve(text)), Math.max(1, room)).size());
    }

    /** Builds one row in the part that scrolls and gives back the thing in it a player clicks, or null. */
    @Nullable
    private UiComponent row(final EffectsPages.Page page, final EffectsPages.IRow row, final int x, final int top,
                            final int w, final int rowH, final Font font, final DesktopEffects now) {
        return switch (row) {
            case EffectsPages.Heading heading -> {
                scroll.add(new Label(GameText.resolve(heading.text()))).setBounds(x, top + 2, w, 9);
                yield null;
            }
            case EffectsPages.Note note -> {
                scroll.add(new Wrapped(GameText.resolve(note.text()), true)).setBounds(x, top, w, rowH);
                yield null;
            }
            case EffectsPages.Check check -> scroll.add(new CheckRow(GameText.resolve(check.label()),
                    () -> check.inverted() == now.isOff(check.effect()), () -> flip(check.effect(), now)))
                    .setBounds(x, top, w, rowH);
            case EffectsPages.Toggle toggle -> scroll.add(new ToggleRow(GameText.resolve(toggle.label()),
                    toggle.description() == null ? "" : GameText.resolve(toggle.description()),
                    () -> toggle.inverted() == now.isOff(toggle.effect()), () -> flip(toggle.effect(), now)))
                    .setBounds(x, top, w, rowH);
            case EffectsPages.Slider slider -> scroll.add(new SliderRow(GameText.resolve(slider.label()),
                    GameText.resolve(slider.low()), GameText.resolve(slider.high()), slider.speeds().size(),
                    () -> nearest(slider.speeds(), now.speed()),
                    step -> set.accept("effectspeed", Integer.toString(slider.speeds().get(step)))))
                    .setBounds(x, top, w, rowH);
            case EffectsPages.Choice choice -> {
                scroll.add(new Label(GameText.resolve(choice.label())).setScale(SMALL))
                        .setBounds(x, top + 3, w - EffectsPageLayout.CHOICE_W - 4, 8);
                final boolean on = !now.isOff(choice.effect());
                yield scroll.add(new Button(GameText.resolve(on ? choice.on() : choice.off()),
                        () -> flip(choice.effect(), now)).setLabelScale(SMALL))
                        .setBounds(x + w - EffectsPageLayout.CHOICE_W, top, EffectsPageLayout.CHOICE_W,
                                EffectsPageLayout.CONTROL_H);
            }
            case EffectsPages.SpeedChoice speed -> {
                scroll.add(new Label(GameText.resolve(speed.label())).setScale(SMALL))
                        .setBounds(x, top + 3, w - EffectsPageLayout.CHOICE_W - 4, 8);
                final int at = nearest(speed.speeds(), now.speed());
                yield scroll.add(new Button(GameText.resolve(speed.names().get(at)), () -> set.accept("effectspeed",
                        Integer.toString(speed.speeds().get((at + 1) % speed.speeds().size()))))
                        .setLabelScale(SMALL))
                        .setBounds(x + w - EffectsPageLayout.CHOICE_W, top, EffectsPageLayout.CHOICE_W,
                                EffectsPageLayout.CONTROL_H);
            }
            case EffectsPages.Presets presets -> presets(presets, x, top, w, now);
        };
    }

    /** Frames XP's four radio buttons; the first is given back as the row's control. */
    private UiComponent presets(final EffectsPages.Presets presets, final int x, final int top, final int w,
                                final DesktopEffects now) {
        final int shown = preset >= 0 ? preset : presetOf(presets, now);
        final List<TextKey> names = presets.names();
        UiComponent first = null;
        for (int i = 0; i < names.size(); i++) {
            final int choice = i;
            final UiComponent radio = scroll.add(new RadioRow(GameText.resolve(names.get(i)), () -> shown == choice,
                    () -> pick(presets, choice, now)));
            radio.setBounds(x, top + EffectsPageLayout.presetY(i), w, EffectsPageLayout.presetY(1));
            if (first == null) {
                first = radio;
            }
        }
        return first;
    }

    /** Picks a preset: every effect on for the first two, every one off for the third, the boxes left for Custom. */
    private void pick(final EffectsPages.Presets presets, final int choice, final DesktopEffects now) {
        preset = choice;
        if (choice == EffectsPages.PRESET_CUSTOM) {
            return;
        }
        final boolean on = choice != EffectsPages.PRESET_PERFORMANCE;
        for (final String effect : presets.effects()) {
            if (now.isOff(effect) == on) {
                set.accept("effect", effect + (on ? " on" : " off"));
            }
        }
    }

    /** The preset the boxes amount to: every one on is the system's own choice, every one off the fastest. */
    private static int presetOf(final EffectsPages.Presets presets, final DesktopEffects now) {
        int off = 0;
        for (final String effect : presets.effects()) {
            if (now.isOff(effect)) {
                off++;
            }
        }
        return off == 0 ? EffectsPages.PRESET_CHOOSE : off == presets.effects().size()
                ? EffectsPages.PRESET_PERFORMANCE : EffectsPages.PRESET_CUSTOM;
    }

    /** Switches one effect the other way; a box changed by hand is the custom choice on Frames XP. */
    private void flip(final String effect, final DesktopEffects now) {
        if (preset >= 0) {
            preset = EffectsPages.PRESET_CUSTOM;
        }
        set.accept("effect", effect + (now.isOff(effect) ? " on" : " off"));
    }

    /** A button of the dialog's foot. */
    private void press(final EffectsPages.Page page, final TextKey word, final DesktopEffects now) {
        if (word == EffectsPageTexts.APPLY) {
            kept = now;
            return;
        }
        if (word == EffectsPageTexts.CANCEL) {
            for (final String effect : effectsOf(page)) {
                if (now.isOff(effect) != kept.isOff(effect)) {
                    set.accept("effect", effect + (kept.isOff(effect) ? " off" : " on"));
                }
            }
            if (now.speed() != kept.speed()) {
                set.accept("effectspeed", Integer.toString(kept.speed()));
            }
        }
        back.run();
    }

    /** Every effect a page switches. */
    private static Set<String> effectsOf(final EffectsPages.Page page) {
        final Set<String> out = new LinkedHashSet<>();
        for (final EffectsPages.IRow row : page.rows()) {
            switch (row) {
                case EffectsPages.Check check -> out.add(check.effect());
                case EffectsPages.Toggle toggle -> out.add(toggle.effect());
                case EffectsPages.Choice choice -> out.add(choice.effect());
                case EffectsPages.Presets presets -> out.addAll(presets.effects());
                default -> { }
            }
        }
        return out;
    }

    /** The step of a slider or a choice nearest the speed the machine has. */
    private static int nearest(final List<Integer> speeds, final int speed) {
        int best = 0;
        for (int i = 1; i < speeds.size(); i++) {
            if (Math.abs(speeds.get(i) - speed) < Math.abs(speeds.get(best) - speed)) {
                best = i;
            }
        }
        return best;
    }

    /** Draws text wrapped to a width at the small scale, a line at a time, cut at the most lines a row takes. */
    private static void wrapped(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                                final int w, final int colour) {
        final List<FormattedCharSequence> lines = font.split(Component.literal(text), Math.max(1, (int) (w / SMALL)));
        for (int i = 0; i < lines.size() && i < EffectsPageLayout.MOST_LINES; i++) {
            g.pose().pushPose();
            g.pose().translate(x, y + i * EffectsPageLayout.LINE_H, 0);
            g.pose().scale(SMALL, SMALL, 1);
            Draw.text(g, font, lines.get(i), 0, 0, colour);
            g.pose().popPose();
        }
    }

    /** A sentence wrapped to the page. */
    private static final class Wrapped extends UiComponent {

        private final String text;
        private final boolean dim;

        Wrapped(final String text, final boolean dim) {
            this.text = text;
            this.dim = dim;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            wrapped(g, ctx.font(), text, x(), y() + 1, width(), dim ? ctx.skin().dim() : ctx.skin().text());
        }
    }

    /** A box and the effect it switches, its words wrapped beside it. */
    private static final class CheckRow extends UiComponent {

        private final String label;
        private final BooleanSupplier on;
        private final Runnable toggle;

        CheckRow(final String label, final BooleanSupplier on, final Runnable toggle) {
            this.label = label;
            this.on = on;
            this.toggle = toggle;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final int box = EffectsPageLayout.BOX;
            final int by = y() + 1;
            g.fill(x(), by, x() + box, by + box, ctx.skin().fieldBg());
            Draw.outline(g, x(), by, box, box, ctx.skin().edge());
            if (on.getAsBoolean()) {
                // The tick: a short stroke down and a long one up, the mark every one of these systems drew.
                final int ink = ctx.skin().text();
                g.fill(x() + 2, by + 3, x() + 3, by + 5, ink);
                g.fill(x() + 3, by + 4, x() + 4, by + 6, ink);
                g.fill(x() + 4, by + 2, x() + 5, by + 5, ink);
                g.fill(x() + 5, by + 1, x() + 6, by + 3, ink);
            }
            wrapped(g, ctx.font(), label, x() + box + 4, y() + 1, width() - box - 4, ctx.skin().text());
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            toggle.run();
            return true;
        }
    }

    /** One of Frames XP's presets: a radio button and its words. */
    private static final class RadioRow extends UiComponent {

        private final String label;
        private final BooleanSupplier on;
        private final Runnable choose;

        RadioRow(final String label, final BooleanSupplier on, final Runnable choose) {
            this.label = label;
            this.on = on;
            this.choose = choose;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final int box = EffectsPageLayout.BOX;
            final int by = y() + 1;
            // Square-cornered like every control here; the dot in the middle is what says it is chosen.
            g.fill(x() + 1, by, x() + box - 1, by + box, ctx.skin().fieldBg());
            g.fill(x(), by + 1, x() + box, by + box - 1, ctx.skin().fieldBg());
            Draw.outline(g, x(), by, box, box, ctx.skin().edge());
            if (on.getAsBoolean()) {
                g.fill(x() + 2, by + 2, x() + box - 2, by + box - 2, ctx.skin().text());
            }
            final String fits = Texts.clip(ctx.font(), label, (int) ((width() - box - 4) / SMALL));
            Texts.scaled(g, ctx.font(), fits, x() + box + 4, y() + 1, SMALL, ctx.skin().text());
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            choose.run();
            return true;
        }
    }

    /** A switch at the right of its name, and the line under the name, as the newer settings pages drew one. */
    private static final class ToggleRow extends UiComponent {

        private final String label;
        private final String note;
        private final BooleanSupplier on;
        private final Runnable toggle;

        ToggleRow(final String label, final String note, final BooleanSupplier on, final Runnable toggle) {
            this.label = label;
            this.note = note;
            this.on = on;
            this.toggle = toggle;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final boolean lit = on.getAsBoolean();
            final int sw = EffectsPageLayout.SWITCH_W;
            final int sh = EffectsPageLayout.CONTROL_H - 4;
            final int sx = right() - sw;
            final int sy = y() + 2;
            final int textW = width() - sw - 4;
            Texts.scaled(g, ctx.font(), Texts.clip(ctx.font(), label, (int) (textW / SMALL)), x(), y() + 2, SMALL,
                    ctx.skin().text());
            if (!note.isEmpty()) {
                wrapped(g, ctx.font(), note, x(), y() + 2 + EffectsPageLayout.LINE_H + 1, textW, ctx.skin().dim());
            }
            // A track, filled with the accent while on, and the knob at the end it is switched to.
            g.fill(sx, sy, sx + sw, sy + sh, lit ? ctx.skin().accent() : ctx.skin().fieldBg());
            Draw.outline(g, sx, sy, sw, sh, ctx.skin().edge());
            final int knob = sh - 4;
            final int kx = lit ? sx + sw - 2 - knob : sx + 2;
            g.fill(kx, sy + 2, kx + knob, sy + 2 + knob, lit ? ctx.skin().fieldBg() : ctx.skin().dim());
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            toggle.run();
            return true;
        }
    }

    /** A slider over the effects' speed: its name, a track with a step for each speed, and its two ends named. */
    private static final class SliderRow extends UiComponent {

        private final String label;
        private final String low;
        private final String high;
        private final int steps;
        private final IntSupplier at;
        private final IntConsumer choose;

        SliderRow(final String label, final String low, final String high, final int steps, final IntSupplier at,
                  final IntConsumer choose) {
            this.label = label;
            this.low = low;
            this.high = high;
            this.steps = steps;
            this.at = at;
            this.choose = choose;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            Texts.scaled(g, ctx.font(), label, x(), y() + 1, SMALL, ctx.skin().text());
            final int ty = trackY();
            g.fill(left(), ty, right() - 3, ty + 3, ctx.skin().fieldBg());
            Draw.outline(g, left(), ty, right() - 3 - left(), 3, ctx.skin().edge());
            for (int i = 0; i < steps; i++) {
                final int tx = stepX(i);
                g.fill(tx, ty + 4, tx + 1, ty + 6, ctx.skin().dim());
            }
            final int thumb = stepX(at.getAsInt());
            g.fill(thumb - 2, ty - 3, thumb + 3, ty + 6, ctx.skin().accent());
            Draw.outline(g, thumb - 2, ty - 3, 5, 9, ctx.skin().edge());
            Texts.scaled(g, ctx.font(), low, left(), ty + 8, SMALL, ctx.skin().dim());
            Texts.scaled(g, ctx.font(), high, right() - 3 - Texts.smallWidth(ctx.font(), high), ty + 8, SMALL,
                    ctx.skin().dim());
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0) {
                return false;
            }
            choose.accept(stepAt(mx));
            return true;
        }

        @Override
        public boolean mouseDragged(final double mx, final double my, final int button) {
            final int step = stepAt(mx);
            if (step != at.getAsInt()) {
                choose.accept(step);
            }
            return true;
        }

        /** The step a click at {@code mx} lands nearest. */
        int stepAt(final double mx) {
            final double along = (mx - left()) / Math.max(1.0, right() - 3 - left());
            return Math.max(0, Math.min(steps - 1, (int) Math.round(along * (steps - 1))));
        }

        /** Where step {@code i} stands across the track. */
        int stepX(final int i) {
            return left() + (right() - 3 - left()) * i / Math.max(1, steps - 1);
        }

        private int left() {
            return x() + 3;
        }

        private int trackY() {
            return y() + EffectsPageLayout.LINE_H + 5;
        }
    }
}
