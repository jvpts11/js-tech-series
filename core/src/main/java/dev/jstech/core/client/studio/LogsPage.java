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
import dev.jstech.core.developer.studio.LogRing;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.log.LogLevel;
import dev.jstech.core.log.LogSystem;
import dev.jstech.core.log.LogSystems;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The logs of each system, each with how much it writes, changed while the game runs, and the last lines of the one
 * chosen; and the errors that teach, each with its mod, its declaration, what is wrong and how to put it right.
 */
final class LogsPage extends StudioPage {

    private final Map<LogLevel, Button> levels = new EnumMap<>(LogLevel.class);
    private int chosen;
    private int scroll;

    private static final int ROW = 13;
    private static final int LINE_ROW = 20;
    private static final int CARD = 48;
    private static final String TIME = "HH:mm:ss";

    LogsPage(final StudioScreen studio) {
        super(studio);
        for (final LogLevel level : LogLevel.values()) {
            this.levels.put(level, this.panel.add(new Button(level::serializedName, () -> this.system().ifPresent(
                    system -> LogSystems.setLevel(system, level))).setLabelScale(StudioDraw.SMALL)));
        }
    }

    @Override
    TextKey tab() {
        return StudioTool.LOGS.title();
    }

    @Override
    List<String> crumbs() {
        return this.system().map(system -> List.of(words(this.tab()), system.id())).orElse(List.of(words(
                this.tab())));
    }

    @Override
    List<String> lines() {
        return LogSystems.all().stream().map(LogSystem::id).toList();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect middle = this.columns().get(1);
        int x = middle.x() + StudioDraw.INSET;
        for (final Button button : this.levels.values()) {
            place(button, new StudioLayout.Rect(x, middle.y() + 16, 34, 12));
            x += 36;
        }
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final List<StudioLayout.Rect> columns = this.columns();
        final StudioLayout.Rect left = columns.get(0);
        StudioDraw.panel(g, font(), left, words(ToolTexts.EVERY_SYSTEM));
        final List<LogSystem> systems = LogSystems.all();
        for (int i = 0; i < systems.size(); i++) {
            final int y = left.y() + StudioDraw.HEAD + 4 + (i - this.scroll) * ROW;
            if (i < this.scroll || y + ROW > left.bottom()) {
                continue;
            }
            final LogSystem system = systems.get(i);
            final boolean on = i == this.chosen;
            if (on) {
                g.fill(left.x() + 3, y - 2, left.right() - 3, y + ROW - 3, colours.selected());
                Draw.outline(g, left.x() + 3, y - 2, left.width() - 6, ROW - 1, colours.outline());
            }
            final LogLevel level = LogSystems.level(system);
            final String written = level.serializedName();
            StudioDraw.text(g, font(), system.loggerName(), left.x() + 8, y, left.width() - 46, on ? colours.text()
                    : colours.label());
            StudioDraw.small(g, font(), written, left.right() - 6 - Math.round(font().width(written)
                    * StudioDraw.SMALL), y + 1, 40, level == LogLevel.DEBUG || level == LogLevel.TRACE
                    ? colours.warning() : colours.dim());
        }
        final StudioLayout.Rect middle = columns.get(1);
        StudioDraw.panel(g, font(), middle, this.system().map(LogSystem::loggerName).orElse(""));
        this.system().ifPresent(system -> {
            final LogLevel now = LogSystems.level(system);
            this.levels.forEach((level, button) -> button.setPrimary(level == now));
            // The level the system writes at, outlined, as the game's own skin draws a chosen button like the others.
            final Button chosen = this.levels.get(now);
            Draw.outline(g, chosen.x() - 1, chosen.y() - 1, chosen.width() + 2, chosen.height() + 2,
                    colours.outline());
            this.lastLines(g, middle, system, colours);
        });
        this.errors(g, columns.get(2), colours);
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final StudioLayout.Rect left = this.columns().get(0);
        if (!left.contains(mouseX, mouseY)) {
            return false;
        }
        final int row = this.scroll + (int) ((mouseY - left.y() - StudioDraw.HEAD - 2) / ROW);
        if (row >= 0 && row < LogSystems.all().size()) {
            this.chosen = row;
        }
        return true;
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        final StudioLayout.Rect left = this.columns().get(0);
        if (!left.contains(mouseX, mouseY)) {
            return false;
        }
        this.scroll = ThingsPage.scrolled(this.scroll, scrollY, LogSystems.all().size(),
                (left.height() - StudioDraw.HEAD - 4) / ROW);
        return true;
    }

    private List<StudioLayout.Rect> columns() {
        return StudioLayout.columns(this.regions.body(), 0.25, 0.46);
    }

    private Optional<LogSystem> system() {
        final List<LogSystem> systems = LogSystems.all();
        return this.chosen < systems.size() ? Optional.of(systems.get(this.chosen)) : Optional.empty();
    }

    /* The last lines the system wrote, the newest at the foot. */
    private void lastLines(final GuiGraphics g, final StudioLayout.Rect area, final LogSystem system,
                           final StudioPalette.Colours colours) {
        final List<LogRing.Line> lines = StudioLogs.lines(system.loggerName());
        final int top = area.y() + 34;
        if (lines.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_LINES), area.x() + StudioDraw.INSET, top,
                    area.width() - 2 * StudioDraw.INSET, colours.dim());
            return;
        }
        final int fits = Math.max(1, (area.bottom() - top) / LINE_ROW);
        final SimpleDateFormat clock = new SimpleDateFormat(TIME);
        final List<LogRing.Line> shown = lines.subList(Math.max(0, lines.size() - fits), lines.size());
        for (int i = 0; i < shown.size(); i++) {
            final LogRing.Line line = shown.get(i);
            final int y = top + i * LINE_ROW;
            final String when = clock.format(new Date(line.millis()));
            StudioDraw.small(g, font(), when, area.x() + StudioDraw.INSET, y, 40, colours.dim());
            StudioDraw.small(g, font(), line.level(), area.x() + StudioDraw.INSET + 34, y, 40,
                    "warn".equals(line.level()) || "error".equals(line.level()) ? colours.warning() : colours.dim());
            StudioDraw.small(g, font(), line.message(), area.x() + StudioDraw.INSET, y + 8,
                    area.width() - 2 * StudioDraw.INSET, colours.label());
        }
    }

    private void errors(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        StudioDraw.panel(g, font(), area, words(ToolTexts.ERRORS_THAT_TEACH));
        final List<DeclarationError> errors = DeclarationError.recent().reversed();
        if (errors.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_ERRORS), area.x() + StudioDraw.INSET,
                    area.y() + StudioDraw.HEAD + 4, area.width() - 2 * StudioDraw.INSET, colours.dim());
            return;
        }
        for (int i = 0; i < errors.size(); i++) {
            final int y = area.y() + StudioDraw.HEAD + 4 + i * CARD;
            if (y + CARD > area.bottom()) {
                break;
            }
            final DeclarationError error = errors.get(i);
            StudioMarks.draw(g, font(), area.x() + StudioDraw.INSET, y + 1, StudioSheet.Mark.ERROR);
            final int x = area.x() + StudioDraw.INSET + StudioMarks.WIDTH + 5;
            final int room = area.right() - x - StudioDraw.INSET;
            final String mod = error.mod() == null ? words(ToolTexts.UNKNOWN_MOD) : modName(error.mod());
            StudioDraw.text(g, font(), mod, x, y, room, colours.text());
            StudioDraw.small(g, font(), error.declaration(), x, y + 10, room, colours.label());
            StudioDraw.small(g, font(), error.problem(), x, y + 18, room, colours.dim());
            StudioDraw.small(g, font(), error.fix(), x, y + 27, room, colours.fine());
        }
    }

    private static String modName(final String modid) {
        final String name = StudioThings.modName(modid);
        return name.isEmpty() ? modid : name;
    }
}
