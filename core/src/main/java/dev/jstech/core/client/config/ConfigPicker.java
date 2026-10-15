/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.SearchField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The picker of one of the game's things for a setting: a window with a search, the things that match in slots, the
 * chosen one's name, how many match, and Cancel and Choose. A double click, or Enter, chooses too.
 */
public final class ConfigPicker extends Popup {

    private final List<ConfigThings.Thing> all;
    private final List<ConfigThings.Thing> shown = new ArrayList<>();
    private final Consumer<String> onChoose;
    private final SearchField search;
    private final Grid grid;
    private final Button cancel;
    private final Button choose;
    private int chosen;

    /** The grid: eight slots across, three rows shown. */
    private static final int ACROSS = 8;
    private static final int ROWS = 3;
    private static final int CELL = 20;
    private static final int PAD = 8;
    private static final int WIDTH = PAD * 2 + ACROSS * CELL;
    private static final int HEIGHT = 150;
    private static final int SEARCH_Y = 17;
    private static final int GRID_Y = 38;
    private static final int NAME_Y = GRID_Y + ROWS * CELL + 4;
    private static final int COUNT_Y = NAME_Y + 10;
    private static final int BUTTON_WIDTH = 76;

    ConfigPicker(final ConfigThings.Kind kind, final Consumer<String> onChoose) {
        super(GameText.resolve(titleOf(kind)), WIDTH, HEIGHT);
        this.all = ConfigThings.all(kind);
        this.onChoose = onChoose;
        this.search = add(new SearchField(64));
        this.search.setPlaceholder(GameText.resolve(ConfigScreenTexts.SEARCH));
        this.search.setRevertOnEscape(false);
        this.search.setOnEdit(this::filter);
        this.grid = add(new Grid());
        this.cancel = add(new Button(GameText.resolve(ConfigScreenTexts.CANCEL), this::close).setFocusable(true));
        this.choose = add(new Button(GameText.resolve(ConfigScreenTexts.CHOOSE), this::take).setFocusable(true));
        setLayouter(popup -> place());
        filter();
        // The search has the keyboard from the start, so typing finds at once.
        focus(search);
    }

    /** The ids of the things the search shows, in order; for a test to read. */
    public List<String> shownIds() {
        return shown.stream().map(ConfigThings.Thing::id).toList();
    }

    /** Searches, as typing in the search does. */
    public void searchFor(final String words) {
        search.set(words);
        filter();
    }

    /** Chooses the thing shown at {@code index}, as a click on it and Choose do. */
    public void choose(final int index) {
        chosen = index;
        take();
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        super.render(g, ctx);
        final ISkin skin = ctx.skin();
        if (chosen >= 0 && chosen < shown.size()) {
            skin.drawText(g, ctx.font(), Texts.clip(ctx.font(), shown.get(chosen).name().getString(), WIDTH - 2 * PAD),
                    x() + PAD, y() + NAME_Y, skin.text());
        }
        final String query = search.edit().isBlank() ? search.value() : search.edit();
        final String count = GameText.resolve(query.isBlank()
                ? ConfigScreenTexts.TO_CHOOSE_FROM.with(shown.size())
                : ConfigScreenTexts.MATCHES.with(shown.size(), query.trim()));
        skin.drawText(g, ctx.font(), Texts.clip(ctx.font(), count, WIDTH - 2 * PAD), x() + PAD, y() + COUNT_Y,
                skin.dim());
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && focusedChild() != cancel) {
            take();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void place() {
        final int left = x() + PAD;
        search.setBounds(left, y() + SEARCH_Y, WIDTH - 2 * PAD, 16);
        grid.setBounds(left, y() + GRID_Y, ACROSS * CELL, ROWS * CELL);
        final int buttonsY = bottom() - PAD - 20;
        cancel.setBounds(left, buttonsY, BUTTON_WIDTH, 20);
        choose.setBounds(right() - PAD - BUTTON_WIDTH, buttonsY, BUTTON_WIDTH, 20);
        choose.setEnabled(chosen >= 0 && chosen < shown.size());
    }

    private void filter() {
        final String query = search.edit().isBlank() ? search.value() : search.edit();
        shown.clear();
        for (final ConfigThings.Thing thing : all) {
            if (ConfigThings.matches(thing, query)) {
                shown.add(thing);
            }
        }
        chosen = shown.isEmpty() ? -1 : 0;
        grid.scroll = 0;
    }

    private void take() {
        if (chosen >= 0 && chosen < shown.size()) {
            final String id = shown.get(chosen).id();
            close();
            onChoose.accept(id);
        }
    }

    private static TextKey titleOf(final ConfigThings.Kind kind) {
        return switch (kind) {
            case ITEM -> ConfigScreenTexts.CHOOSE_ITEM;
            case BLOCK -> ConfigScreenTexts.CHOOSE_BLOCK;
            case FLUID -> ConfigScreenTexts.CHOOSE_FLUID;
        };
    }

    /** The slots of the things that match, scrolled a row at a time. */
    private final class Grid extends UiComponent {

        private int scroll;
        private long lastClick;
        private int lastIndex = -1;

        /** Two clicks on one slot this close together are a double click, which chooses. */
        private static final long DOUBLE_CLICK_MILLIS = 300L;

        @Override
        public boolean focusable() {
            return true;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final int lit = ConfigWidgets.colour(ctx.skin(), StyleRole.MENU_TEXT);
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < ACROSS; column++) {
                    final int index = (scroll + row) * ACROSS + column;
                    final int sx = x() + column * CELL + 1;
                    final int sy = y() + row * CELL + 1;
                    ctx.skin().slot(g, sx, sy);
                    if (index >= shown.size()) {
                        continue;
                    }
                    if (!shown.get(index).icon().isEmpty()) {
                        g.renderItem(shown.get(index).icon(), sx, sy);
                    }
                    if (index == chosen) {
                        Draw.outline(g, sx - 1, sy - 1, ISkin.SLOT_SIZE, ISkin.SLOT_SIZE, lit);
                    } else if (ctx.over(sx, sy, 16, 16)) {
                        g.fill(sx, sy, sx + 16, sy + 16, ctx.skin().listHover() & ~(0xFF << 24) | 0x80 << 24);
                    }
                }
            }
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            final int index = indexAt(mx, my);
            if (button != 0 || index < 0) {
                return index >= 0;
            }
            final long now = System.currentTimeMillis();
            final boolean twice = index == lastIndex && now - lastClick < DOUBLE_CLICK_MILLIS;
            chosen = index;
            lastIndex = index;
            lastClick = now;
            if (twice) {
                take();
            }
            return true;
        }

        @Override
        public boolean mouseScrolled(final double mx, final double my, final double delta) {
            final int rows = (shown.size() + ACROSS - 1) / ACROSS;
            scroll = Math.max(0, Math.min(Math.max(0, rows - ROWS), scroll - (int) Math.signum(delta)));
            return true;
        }

        @Override
        public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
            final int moved = switch (key) {
                case GLFW.GLFW_KEY_LEFT -> chosen - 1;
                case GLFW.GLFW_KEY_RIGHT -> chosen + 1;
                case GLFW.GLFW_KEY_UP -> chosen - ACROSS;
                case GLFW.GLFW_KEY_DOWN -> chosen + ACROSS;
                default -> Integer.MIN_VALUE;
            };
            if (moved == Integer.MIN_VALUE || shown.isEmpty()) {
                return false;
            }
            chosen = Math.max(0, Math.min(shown.size() - 1, moved));
            final int row = chosen / ACROSS;
            scroll = Math.max(row - ROWS + 1, Math.min(scroll, row));
            return true;
        }

        @Override
        public List<Component> tooltip(final double mx, final double my) {
            final int index = indexAt(mx, my);
            return index < 0 ? List.of() : List.of(shown.get(index).name(),
                    Component.literal(shown.get(index).id()));
        }

        private int indexAt(final double mx, final double my) {
            if (!contains(mx, my)) {
                return -1;
            }
            final int column = (int) ((mx - x()) / CELL);
            final int row = (int) ((my - y()) / CELL);
            final int index = (scroll + row) * ACROSS + column;
            return index < shown.size() ? index : -1;
        }
    }
}
