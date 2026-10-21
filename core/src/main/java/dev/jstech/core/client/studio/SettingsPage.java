/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigPresets;
import dev.jstech.core.config.ConfigTree;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

/**
 * The settings of every mod on the Core: each mod's files, and each setting of the file chosen with its value, its
 * default and its range, a value the pack changed in yellow; and the pack's preset exported, what differs from the
 * defaults written as a preset of the file.
 */
final class SettingsPage extends StudioPage {

    private final Button export;
    private final List<Row> rows = new ArrayList<>();
    private int chosen;
    private int scroll;
    private String exported = "";

    private static final int ROW = 12;
    private static final int KEY_ROW = 16;
    /** What the preset is called, the pack's own. */
    private static final String PRESET = "pack";

    SettingsPage(final StudioScreen studio) {
        super(studio);
        this.export = this.panel.add(new Button(() -> words(ToolTexts.EXPORT), this::export));
    }

    @Override
    TextKey tab() {
        return StudioTool.SETTINGS.title();
    }

    @Override
    List<String> crumbs() {
        final ConfigFile file = this.file();
        return file == null ? List.of(words(this.tab())) : List.of(words(this.tab()), this.modOf(file),
                file.name());
    }

    @Override
    List<String> lines() {
        final ConfigFile file = this.file();
        return file == null ? List.of() : file.keys().stream().map(ConfigKey::dottedPath).toList();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect right = this.columns().get(1);
        place(this.export, new StudioLayout.Rect(right.x(), right.bottom() - 20, 160, 18));
    }

    @Override
    void shown() {
        this.rows.clear();
        for (final IModInfo mod : ModList.get().getMods()) {
            final List<ConfigFile> files = ConfigFiles.of(mod.getModId());
            if (files.isEmpty()) {
                continue;
            }
            this.rows.add(new Row(mod.getDisplayName(), null));
            files.forEach(file -> this.rows.add(new Row(file.name(), file)));
        }
        if (this.file() == null) {
            this.chosen = this.rows.indexOf(this.rows.stream().filter(row -> row.file() != null).findFirst()
                    .orElse(null));
        }
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final List<StudioLayout.Rect> columns = this.columns();
        final StudioLayout.Rect left = columns.get(0);
        StudioDraw.panel(g, font(), left, words(ToolTexts.EVERY_FILE));
        for (int i = 0; i < this.rows.size(); i++) {
            final int y = left.y() + StudioDraw.HEAD + 4 + i * ROW;
            if (y + ROW > left.bottom()) {
                break;
            }
            final Row row = this.rows.get(i);
            if (row.file() == null) {
                StudioMarks.arrow(g, left.x() + 6, y + 3, true, colours.text());
                StudioDraw.text(g, font(), row.name(), left.x() + 15, y + 1, left.width() - 20, colours.text());
                continue;
            }
            final boolean on = i == this.chosen;
            if (on) {
                g.fill(left.x() + 3, y - 1, left.right() - 3, y + ROW - 1, colours.selected());
                Draw.outline(g, left.x() + 3, y - 1, left.width() - 6, ROW, colours.outline());
            }
            StudioDraw.text(g, font(), row.name(), left.x() + 20, y + 1, left.width() - 26, on ? colours.text()
                    : colours.label());
        }
        final StudioLayout.Rect right = columns.get(1);
        final StudioLayout.Rect table = new StudioLayout.Rect(right.x(), right.y(), right.width(),
                right.height() - 24);
        StudioDraw.panel(g, font(), table, "");
        final ConfigFile file = this.file();
        if (file != null) {
            this.keys(g, table, file, colours);
        }
        StudioDraw.small(g, font(), this.exported, this.export.x() + this.export.width() + 6, this.export.y() + 6,
                right.right() - this.export.x() - this.export.width() - 8, colours.label());
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final StudioLayout.Rect left = this.columns().get(0);
        if (!left.contains(mouseX, mouseY)) {
            return false;
        }
        final int row = (int) ((mouseY - left.y() - StudioDraw.HEAD - 3) / ROW);
        if (row >= 0 && row < this.rows.size() && this.rows.get(row).file() != null) {
            this.chosen = row;
            this.scroll = 0;
            this.exported = "";
        }
        return true;
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        final ConfigFile file = this.file();
        if (file != null && this.columns().get(1).contains(mouseX, mouseY)) {
            this.scroll = ThingsPage.scrolled(this.scroll, scrollY, file.keys().size(), this.keysShown());
            return true;
        }
        return false;
    }

    private void keys(final GuiGraphics g, final StudioLayout.Rect table, final ConfigFile file,
                      final StudioPalette.Colours colours) {
        final int[] at = {table.x() + 12, table.x() + table.width() * 54 / 100, table.x() + table.width() * 68 / 100,
            table.x() + table.width() * 82 / 100};
        final List<TextKey> heads = List.of(ToolTexts.SETTING, ToolTexts.VALUE, ToolTexts.DEFAULT, ToolTexts.RANGE);
        for (int i = 0; i < heads.size(); i++) {
            StudioDraw.small(g, font(), words(heads.get(i)), at[i], table.y() + 6, table.width() / 4, colours.text());
        }
        final ConfigDraft draft = new ConfigDraft(file);
        final List<ConfigKey<?>> keys = List.copyOf(file.keys());
        final int last = Math.min(keys.size(), this.scroll + this.keysShown());
        for (int i = this.scroll; i < last; i++) {
            final ConfigKey<?> key = keys.get(i);
            final int y = table.y() + 20 + (i - this.scroll) * KEY_ROW;
            final Object value = draft.plainValue(key);
            final Object fallback = plainDefault(key);
            final boolean changed = !Objects.equals(value, fallback);
            StudioDraw.text(g, font(), key.dottedPath(), at[0], y, at[1] - at[0] - 6, colours.label());
            StudioDraw.text(g, font(), String.valueOf(value), at[1], y, at[2] - at[1] - 6, changed
                    ? colours.warning() : colours.label());
            StudioDraw.text(g, font(), String.valueOf(fallback), at[2], y, at[3] - at[2] - 6, colours.dim());
            StudioDraw.small(g, font(), key.range().map(range -> GameText.resolve(ToolTexts.RANGE_VALUE.with(
                    range.min(), range.max()))).orElse(""), at[3], y + 1, table.right() - at[3] - 6, colours.dim());
        }
        StudioDraw.small(g, font(), words(ToolTexts.CHANGED_NOTE), at[0], Math.min(table.bottom() - 10,
                table.y() + 24 + (last - this.scroll) * KEY_ROW), table.width() - 24, colours.dim());
    }

    /* What differs from the defaults in the file chosen, written as the pack's preset of it. */
    private void export() {
        final ConfigFile file = this.file();
        if (file == null) {
            return;
        }
        final ConfigDraft draft = new ConfigDraft(file);
        final Map<String, Object> plain = new LinkedHashMap<>();
        int count = 0;
        for (final ConfigKey<?> key : file.keys()) {
            final Object value = draft.plainValue(key);
            if (!Objects.equals(value, plainDefault(key))) {
                ConfigTree.put(plain, ConfigTree.path(key.dottedPath()), value);
                count++;
            }
        }
        try {
            ConfigPresets.save(file, PRESET, plain);
            this.exported = GameText.resolve(ToolTexts.EXPORTED.with(ConfigPresets.folder(file).resolve(PRESET)
                    .toString(), count));
        } catch (final IOException unwritten) {
            this.exported = GameText.resolve(ToolTexts.NOT_EXPORTED.with(String.valueOf(unwritten.getMessage())));
        }
    }

    private List<StudioLayout.Rect> columns() {
        return StudioLayout.columns(this.regions.body(), 0.30);
    }

    private int keysShown() {
        return Math.max(1, (this.columns().get(1).height() - 24 - 34) / KEY_ROW);
    }

    private @Nullable ConfigFile file() {
        return this.chosen >= 0 && this.chosen < this.rows.size() ? this.rows.get(this.chosen).file() : null;
    }

    private String modOf(final ConfigFile file) {
        String mod = "";
        for (final Row row : this.rows) {
            if (row.file() == null) {
                mod = row.name();
            } else if (row.file() == file) {
                return mod;
            }
        }
        return mod;
    }

    private static <T> Object plainDefault(final ConfigKey<T> key) {
        return key.plain(key.defaultValue());
    }

    /** A row of the list: a mod, or one of its files. */
    private record Row(String name, @Nullable ConfigFile file) {
    }
}
