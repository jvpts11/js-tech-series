/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.IsmsLayout;
import dev.jstech.computers.program.iql.IqlScript;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The dialogs of the IQL Server Management Studio, one at a time over its window: the question before a statement
 * that cannot be undone (with "Don't ask again"), a plain question, a note with only OK, the values of a query's
 * template parameters, the Options and the Query Options, and Index Maintenance. Each is drawn in the studio's own
 * dress, through its skin.
 */
@PaletteHolder
final class IsmsDialogs {

    private final IsmsApp app;
    @Nullable
    private Popup open;
    /** The dialog's main button, which Enter presses and a test clicks. */
    @Nullable
    private Button primary;
    private boolean warning;

    /** The sign a warning wears, {@code jsc:app/isms/dialog}: its yellow, its edge and its mark. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/isms/dialog",
            new Colours(0xFFF2C200, 0xFF8A6A00, 0xFF000000));
    private static final int ICON = 16;
    private static final int LINE_H = 10;

    IsmsDialogs(final IsmsApp app) {
        this.app = app;
    }

    boolean isOpen() {
        return open != null && open.isOpen();
    }

    /** Asks before {@code statement}, which cannot be undone; Yes runs {@code yes}, and may stop the asking. */
    void confirm(final String statement, final Runnable yes) {
        final boolean[] stop = {false};
        final Popup popup = popup(IsmsTexts.CONFIRM, IsmsLayout.CONFIRM_W, IsmsLayout.CONFIRM_H, true);
        final List<Label> lines = lines(popup, GameText.resolve(IsmsTexts.CONFIRM_TEXT.with(statement)),
                IsmsLayout.CONFIRM_W - IsmsLayout.DIALOG_PAD * 2 - ICON - 6, 3);
        final Checkbox dontAsk = popup.add(new Checkbox(() -> GameText.resolve(IsmsTexts.DONT_ASK), () -> stop[0],
                () -> stop[0] = !stop[0]));
        final Button no = popup.add(new Button(GameText.resolve(IsmsTexts.NO), this::close));
        primary = popup.add(new Button(GameText.resolve(IsmsTexts.YES), () -> {
            close();
            if (stop[0]) {
                app.settings().askFirst = false;
                app.saveSettings();
            }
            yes.run();
        }).setPrimary(true));
        popup.setLayouter(p -> {
            placeLines(p, lines, true);
            final int buttonsY = p.bottom() - IsmsLayout.DIALOG_PAD - IsmsLayout.FIELD_H;
            dontAsk.setBounds(p.x() + IsmsLayout.DIALOG_PAD, buttonsY - IsmsLayout.CHECK_ROW_H - 2,
                    p.width() - IsmsLayout.DIALOG_PAD * 2, IsmsLayout.CHECK_ROW_H - 2);
            buttons(p, buttonsY, primary, no);
        });
        show(popup);
    }

    /** Asks {@code message}; Yes runs {@code yes}. */
    void ask(final Text message, final Runnable yes) {
        final Popup popup = popup(IsmsTexts.CONFIRM, IsmsLayout.NOTE_W, IsmsLayout.NOTE_H, true);
        final List<Label> lines = lines(popup, GameText.resolve(message),
                IsmsLayout.NOTE_W - IsmsLayout.DIALOG_PAD * 2 - ICON - 6, 5);
        final Button no = popup.add(new Button(GameText.resolve(IsmsTexts.NO), this::close));
        primary = popup.add(new Button(GameText.resolve(IsmsTexts.YES), () -> {
            close();
            yes.run();
        }).setPrimary(true));
        popup.setLayouter(p -> {
            placeLines(p, lines, true);
            buttons(p, p.bottom() - IsmsLayout.DIALOG_PAD - IsmsLayout.FIELD_H, primary, no);
        });
        show(popup);
    }

    /** Tells {@code message} under {@code title}, with only OK; a warning wears the warning sign. */
    void note(final Text title, final Text message, final boolean warn) {
        final Popup popup = popup(title, IsmsLayout.NOTE_W, IsmsLayout.NOTE_H, warn);
        final List<Label> lines = lines(popup, GameText.resolve(message),
                IsmsLayout.NOTE_W - IsmsLayout.DIALOG_PAD * 2 - (warn ? ICON + 6 : 0), 5);
        primary = popup.add(new Button(GameText.resolve(IsmsTexts.OK), this::close).setPrimary(true));
        popup.setLayouter(p -> {
            placeLines(p, lines, warn);
            buttons(p, p.bottom() - IsmsLayout.DIALOG_PAD - IsmsLayout.FIELD_H, primary);
        });
        show(popup);
    }

    /** Asks the values of {@code doc}'s template parameters, and fills them in on OK. */
    void templateValues(final IsmsDocument doc) {
        final List<IqlScript.Parameter> parameters = IqlScript.parameters(doc.code.text());
        if (parameters.isEmpty()) {
            note(IsmsTexts.TEMPLATE_TITLE.text(), IsmsTexts.NO_PARAMETERS.text(), false);
            return;
        }
        final List<IqlScript.Parameter> shown = parameters.subList(0, Math.min(parameters.size(),
                IsmsLayout.MOST_TEMPLATE_ROWS));
        final Popup popup = popup(IsmsTexts.TEMPLATE_TITLE, IsmsLayout.TEMPLATE_W,
                IsmsLayout.templateH(shown.size()), false);
        final List<Label> names = new ArrayList<>();
        final List<TextField> values = new ArrayList<>();
        for (final IqlScript.Parameter parameter : shown) {
            names.add(popup.add(new Label(GameText.resolve(IsmsTexts.PARAMETER.with(parameter.name(),
                    parameter.type())))));
            values.add(popup.add(new TextField(64).set(parameter.fallback())));
        }
        final Button cancel = popup.add(new Button(GameText.resolve(IsmsTexts.CANCEL), this::close));
        primary = popup.add(new Button(GameText.resolve(IsmsTexts.OK), () -> {
            final Map<String, String> filled = new HashMap<>();
            for (int i = 0; i < shown.size(); i++) {
                filled.put(shown.get(i).name(), values.get(i).edit());
            }
            close();
            doc.code.setText(IqlScript.fill(doc.code.text(), filled));
            doc.dirty = true;
        }).setPrimary(true));
        popup.setLayouter(p -> {
            for (int i = 0; i < names.size(); i++) {
                final int y = p.y() + IsmsLayout.DIALOG_TITLE_H + IsmsLayout.DIALOG_PAD + i * IsmsLayout.TEMPLATE_ROW_H;
                names.get(i).setBounds(p.x() + IsmsLayout.DIALOG_PAD, y + 2, p.width() / 2 - IsmsLayout.DIALOG_PAD,
                        8);
                values.get(i).setBounds(p.x() + p.width() / 2, y, p.width() / 2 - IsmsLayout.DIALOG_PAD,
                        IsmsLayout.FIELD_H);
            }
            buttons(p, p.bottom() - IsmsLayout.DIALOG_PAD - IsmsLayout.FIELD_H, primary, cancel);
        });
        show(popup);
        popup.focus(values.get(0));
    }

    /** The Query Options: whether a script stops at its first error, and where its results go. */
    void queryOptions(final IsmsDocument doc) {
        final IsmsSettings settings = app.settings();
        final Popup popup = popup(IsmsTexts.QUERY_OPTIONS_TITLE, IsmsLayout.OPTIONS_W, IsmsLayout.OPTIONS_H, false);
        final Checkbox stop = popup.add(new Checkbox(() -> GameText.resolve(IsmsTexts.STOP_ON_ERROR),
                () -> settings.stopOnError, () -> settings.stopOnError = !settings.stopOnError));
        final Button results = popup.add(new Button(() -> GameText.resolve(IsmsTexts.RESULTS_TO.with(
                IsmsApp.modeWord(doc.mode))), () -> {
                    doc.mode = doc.mode.next();
                    settings.results = doc.mode;
                }));
        primary = popup.add(new Button(GameText.resolve(IsmsTexts.OK), () -> {
            close();
            app.saveSettings();
        }).setPrimary(true));
        popup.setLayouter(p -> {
            final int x = p.x() + IsmsLayout.DIALOG_PAD;
            final int y = p.y() + IsmsLayout.DIALOG_TITLE_H + IsmsLayout.DIALOG_PAD;
            stop.setBounds(x, y, p.width() - IsmsLayout.DIALOG_PAD * 2, IsmsLayout.CHECK_ROW_H - 2);
            results.setBounds(x, y + IsmsLayout.CHECK_ROW_H, p.width() / 2, IsmsLayout.FIELD_H);
            buttons(p, p.bottom() - IsmsLayout.DIALOG_PAD - IsmsLayout.FIELD_H, primary);
        });
        show(popup);
    }

    /** The studio's Options: asking before what cannot be undone, line numbers, and a query to start with. */
    void options() {
        final IsmsSettings settings = app.settings();
        final Popup popup = popup(IsmsTexts.OPTIONS_TITLE, IsmsLayout.OPTIONS_W, IsmsLayout.OPTIONS_H, false);
        final List<Checkbox> boxes = List.of(
                popup.add(new Checkbox(() -> GameText.resolve(IsmsTexts.ASK_FIRST), () -> settings.askFirst,
                        () -> settings.askFirst = !settings.askFirst)),
                popup.add(new Checkbox(() -> GameText.resolve(IsmsTexts.SHOW_LINE_NUMBERS),
                        () -> settings.lineNumbers, () -> settings.lineNumbers = !settings.lineNumbers)),
                popup.add(new Checkbox(() -> GameText.resolve(IsmsTexts.QUERY_AT_START), () -> settings.queryAtStart,
                        () -> settings.queryAtStart = !settings.queryAtStart)));
        primary = popup.add(new Button(GameText.resolve(IsmsTexts.OK), () -> {
            close();
            app.saveSettings();
        }).setPrimary(true));
        popup.setLayouter(p -> {
            for (int i = 0; i < boxes.size(); i++) {
                boxes.get(i).setBounds(p.x() + IsmsLayout.DIALOG_PAD, p.y() + IsmsLayout.DIALOG_TITLE_H
                        + IsmsLayout.DIALOG_PAD + i * IsmsLayout.CHECK_ROW_H, p.width() - IsmsLayout.DIALOG_PAD * 2,
                        IsmsLayout.CHECK_ROW_H - 2);
            }
            buttons(p, p.bottom() - IsmsLayout.DIALOG_PAD - IsmsLayout.FIELD_H, primary);
        });
        show(popup);
    }

    /** Index Maintenance: how the index stands, and a button for each of its jobs, run in a new query. */
    void indexMaintenance(final Text standing, final Consumer<String> run) {
        final Popup popup = popup(IsmsTexts.INDEX_TITLE, IsmsLayout.INDEX_W, IsmsLayout.INDEX_H, false);
        final List<Label> lines = lines(popup, GameText.resolve(standing),
                IsmsLayout.INDEX_W - IsmsLayout.DIALOG_PAD * 2, 2);
        final List<Button> jobs = new ArrayList<>();
        for (final IndexJob job : IndexJob.ALL) {
            jobs.add(popup.add(new Button(GameText.resolve(job.word()), () -> {
                close();
                run.accept(job.verb());
            })));
        }
        primary = popup.add(new Button(GameText.resolve(IsmsTexts.CLOSE), this::close));
        popup.setLayouter(p -> {
            placeLines(p, lines, false);
            final int w = (p.width() - IsmsLayout.DIALOG_PAD * 5) / 4;
            final int y = p.bottom() - IsmsLayout.DIALOG_PAD - IsmsLayout.FIELD_H;
            for (int i = 0; i < jobs.size(); i++) {
                jobs.get(i).setBounds(p.x() + IsmsLayout.DIALOG_PAD + i * (w + IsmsLayout.DIALOG_PAD), y, w,
                        IsmsLayout.FIELD_H);
            }
            primary.setBounds(p.x() + IsmsLayout.DIALOG_PAD + 3 * (w + IsmsLayout.DIALOG_PAD), y, w,
                    IsmsLayout.FIELD_H);
        });
        show(popup);
    }

    /** Draws the dialog open, over the window's content rectangle. */
    void render(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w, final int h) {
        final Popup popup = open;
        if (popup == null || !popup.isOpen()) {
            return;
        }
        popup.renderIn(g, ctx, x, y, w, h);
        if (warning) {
            sign(g, popup.x() + IsmsLayout.DIALOG_PAD, popup.y() + IsmsLayout.DIALOG_TITLE_H + 2);
        }
    }

    boolean mouseClicked(final double mx, final double my, final int button) {
        return isOpen() && open.mouseClicked(mx, my, button);
    }

    boolean mouseReleased(final double mx, final double my, final int button) {
        return isOpen() && open.mouseReleased(mx, my, button);
    }

    boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (!isOpen()) {
            return false;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && primary != null) {
            pressPrimary();
            return true;
        }
        return open.keyPressed(key, scanCode, modifiers);
    }

    boolean charTyped(final char c) {
        return isOpen() && open.charTyped(c);
    }

    /** Presses the dialog's main button, as Enter does: Yes, OK, or Close. */
    void pressPrimary() {
        final Button button = primary;
        if (button != null && isOpen()) {
            button.mouseClicked(button.center()[0], button.center()[1], 0);
            button.mouseReleased(button.center()[0], button.center()[1], 0);
        }
    }

    /** The dialog's main button, for a test to find; null with none open. */
    @Nullable
    Button primaryButton() {
        return isOpen() ? primary : null;
    }

    /** What the open dialog's lines say, for a test to read. */
    List<String> text() {
        final List<String> out = new ArrayList<>();
        if (isOpen()) {
            open.children().forEach(child -> {
                if (child instanceof Label label) {
                    out.add(label.text());
                }
            });
        }
        return out;
    }

    void close() {
        if (open != null) {
            open.close();
        }
        open = null;
        primary = null;
    }

    private Popup popup(final TextKey title, final int w, final int h, final boolean warn) {
        return popup(title.text(), w, h, warn);
    }

    private Popup popup(final Text title, final int w, final int h, final boolean warn) {
        close();
        this.warning = warn;
        final Popup popup = new Popup(GameText.resolve(title), w, h);
        popup.setCloseOnOutsideClick(false);
        return popup;
    }

    private void show(final Popup popup) {
        open = popup;
        popup.open();
    }

    /* The message's lines, broken where it says so and wrapped on its spaces, at most {@code most} of them. */
    private List<Label> lines(final Popup popup, final String message, final int width, final int most) {
        final Font font = app.font();
        final List<Label> out = new ArrayList<>();
        for (final String part : message.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (final String word : part.split(" ")) {
                final String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.width(candidate) > width && !line.isEmpty()) {
                    out.add(new Label(line.toString()));
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            out.add(new Label(line.toString()));
        }
        final List<Label> kept = out.subList(0, Math.min(most, out.size()));
        kept.forEach(popup::add);
        return List.copyOf(kept);
    }

    private static void placeLines(final Popup p, final List<Label> lines, final boolean signed) {
        final int x = p.x() + IsmsLayout.DIALOG_PAD + (signed ? ICON + 6 : 0);
        int y = p.y() + IsmsLayout.DIALOG_TITLE_H + 4;
        for (final Label line : lines) {
            line.setBounds(x, y, p.right() - IsmsLayout.DIALOG_PAD - x, 8);
            y += LINE_H;
        }
    }

    /* The buttons along the bottom right, the first given rightmost but one and the last at the edge. */
    private static void buttons(final Popup p, final int y, final Button... buttons) {
        int x = p.right() - IsmsLayout.DIALOG_PAD;
        for (int i = buttons.length - 1; i >= 0; i--) {
            x -= IsmsLayout.DIALOG_BUTTON_W;
            buttons[i].setBounds(x, y, IsmsLayout.DIALOG_BUTTON_W, IsmsLayout.FIELD_H);
            x -= IsmsLayout.DIALOG_PAD;
        }
    }

    /** The warning sign, a yellow triangle with a mark in it. */
    private static void sign(final GuiGraphics g, final int x, final int y) {
        final Colours c = PALETTE.get();
        for (int row = 0; row < 14; row++) {
            final int half = row / 2 + 1;
            final int cx = x + ICON / 2;
            g.fill(cx - half, y + row, cx + half, y + row + 1, row == 13 ? c.edge() : c.sign());
        }
        g.fill(x + ICON / 2 - 1, y + 5, x + ICON / 2 + 1, y + 10, c.mark());
        g.fill(x + ICON / 2 - 1, y + 11, x + ICON / 2 + 1, y + 13, c.mark());
    }

    /** One of the index's jobs, by the word on its button and the statement it runs. */
    private record IndexJob(TextKey word, String verb) {

        private static final List<IndexJob> ALL = List.of(new IndexJob(IsmsTexts.ANALYZE, "ANALYZE"),
                new IndexJob(IsmsTexts.REBUILD, "REINDEX"), new IndexJob(IsmsTexts.VACUUM, "VACUUM"));
    }

    /** The warning sign's colours. */
    private record Colours(int sign, int edge, int mark) {
    }
}
