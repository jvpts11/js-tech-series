/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.ScrollPanel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigPreset;
import dev.jstech.core.config.ConfigPresets;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.text.GameText;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The presets of one settings file: those the mod ships, and those saved on this game, each picked with a click, a
 * saved one thrown away with another; and the settings on the screen saved as a new one under a name.
 */
public final class ConfigPresetsPopup extends Popup {

    private final ConfigFile file;
    private final Supplier<Map<String, Object>> current;
    private final BiConsumer<String, Map<String, Object>> onPick;
    private final ScrollPanel list;
    private final List<Line> lines = new ArrayList<>();
    private final TextField name;
    private final Button save;
    private String message = "";
    private boolean failed;

    private static final int WIDTH = 236;
    private static final int HEIGHT = 186;
    private static final int PAD = 8;
    private static final int LINE = 22;
    private static final int LIST_Y = 18;
    private static final int SMALL_BUTTON = 44;

    /**
     * @param file    the file whose presets these are
     * @param current the screen's values of the file, changes and all, as a file holds them
     * @param onPick  what picking one does, given its title and its values
     */
    ConfigPresetsPopup(final ConfigFile file, final Supplier<Map<String, Object>> current,
                       final BiConsumer<String, Map<String, Object>> onPick) {
        super(GameText.resolve(ConfigScreenTexts.PRESETS_TITLE) + " - " + file.fileName(), WIDTH, HEIGHT);
        this.file = file;
        this.current = current;
        this.onPick = onPick;
        this.list = add(new ScrollPanel());
        this.list.setStep(LINE);
        this.name = add(new TextField(48));
        this.name.setPlaceholder(GameText.resolve(ConfigScreenTexts.PRESET_NAME));
        this.save = add(new Button(GameText.resolve(ConfigScreenTexts.SAVE_PRESET), this::saveCurrent)
                .setFocusable(true));
        setLayouter(popup -> place());
        fill();
    }

    /** The names the list shows, the mod's first; for a test to read. */
    public List<String> names() {
        return lines.stream().filter(line -> line.pick() != null).map(Line::title).toList();
    }

    /** Picks the preset shown as {@code title}, as its Pick does. */
    public boolean pick(final String title) {
        for (final Line line : lines) {
            if (line.title().equals(title) && line.pick() != null) {
                line.pick().press();
                return true;
            }
        }
        return false;
    }

    /** Saves the screen's values as {@code preset}, as typing its name and Save these does. */
    public void saveAs(final String preset) {
        name.set(preset);
        saveCurrent();
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        super.render(g, ctx);
        if (!message.isEmpty()) {
            ctx.skin().drawText(g, ctx.font(), message, x() + PAD, bottom() - PAD - 20 - 11,
                    failed ? ConfigWidgets.colour(ctx.skin(), StyleRole.WARNING) : ctx.skin().dim());
        }
    }

    private void place() {
        final int left = x() + PAD;
        final int inner = width() - 2 * PAD;
        final int listBottom = bottom() - PAD - 20 - 14;
        list.setBounds(left, y() + LIST_Y, inner, listBottom - y() - LIST_Y);
        list.setContentHeight(lines.size() * LINE);
        for (int i = 0; i < lines.size(); i++) {
            final Line line = lines.get(i);
            final int ly = list.contentY(i * LINE);
            final int buttons = (line.pick() == null ? 0 : SMALL_BUTTON) + (line.delete() == null ? 0
                    : SMALL_BUTTON + 4);
            line.label().setBounds(left + 2, ly, inner - buttons - 10, LINE - 2);
            if (line.pick() != null) {
                line.pick().setBounds(left + inner - 6 - buttons, ly + 1, SMALL_BUTTON, LINE - 4);
            }
            if (line.delete() != null) {
                line.delete().setBounds(left + inner - 6 - SMALL_BUTTON, ly + 1, SMALL_BUTTON, LINE - 4);
            }
        }
        final int buttonsY = bottom() - PAD - 20;
        final int saveWidth = 70;
        name.setBounds(left, buttonsY, inner - saveWidth - 4, 20);
        save.setBounds(left + inner - saveWidth, buttonsY, saveWidth, 20);
    }

    /* The list, made again: the mod's presets under their header, then the saved ones under theirs. */
    private void fill() {
        for (final Line line : lines) {
            list.remove(line.label());
            if (line.pick() != null) {
                list.remove(line.pick());
            }
            if (line.delete() != null) {
                list.remove(line.delete());
            }
        }
        lines.clear();
        if (!file.presets().isEmpty()) {
            header(GameText.resolve(ConfigScreenTexts.FROM_THE_MOD));
            for (final ConfigPreset preset : file.presets()) {
                final String title = GameText.resolve(preset.title());
                line(title, () -> onPick.accept(title, preset.plain()), null);
            }
        }
        final List<String> saved = ConfigPresets.saved(file);
        header(GameText.resolve(ConfigScreenTexts.SAVED_HERE));
        if (saved.isEmpty()) {
            final Label none = list.add(new Label(GameText.resolve(ConfigScreenTexts.NO_PRESETS), Label.Tone.DIM));
            lines.add(new Line("", none, null, null));
        }
        for (final String preset : saved) {
            line(preset, () -> ConfigPresets.load(file, preset).ifPresent(values -> onPick.accept(preset, values)),
                    () -> {
                        ConfigPresets.delete(file, preset);
                        fill();
                    });
        }
    }

    private void header(final String title) {
        lines.add(new Line(title, list.add(new Label(title, Label.Tone.ACCENT)), null, null));
    }

    private void line(final String title, final Runnable onPickIt, final Runnable onDelete) {
        final Label label = list.add(new Label(title));
        final Button pick = list.add(new Button(GameText.resolve(ConfigScreenTexts.PICK), () -> {
            onPickIt.run();
            close();
        }).setFocusable(true));
        final Button delete = onDelete == null ? null
                : list.add(new Button(GameText.resolve(ConfigScreenTexts.DELETE), onDelete).setFocusable(true));
        lines.add(new Line(title, label, pick, delete));
    }

    private void saveCurrent() {
        final String typed = (name.edit().isBlank() ? name.value() : name.edit()).trim();
        if (!ConfigPresets.validName(typed)) {
            message = GameText.resolve(ConfigScreenTexts.BAD_NAME);
            failed = true;
            return;
        }
        try {
            ConfigPresets.save(file, typed, current.get());
            message = GameText.resolve(ConfigScreenTexts.PRESET_SAVED.with(typed));
            failed = false;
            name.set("");
            fill();
        } catch (final IOException e) {
            message = Optional.ofNullable(e.getMessage()).orElse(typed);
            failed = true;
        }
    }

    /**
     * A line of the list: a header, or a preset with its buttons.
     *
     * @param title  what it says
     * @param label  the text
     * @param pick   its Pick, none for a header
     * @param delete its Delete, none for a header or a preset of the mod
     */
    private record Line(String title, UiComponent label, Button pick, Button delete) {
    }
}
