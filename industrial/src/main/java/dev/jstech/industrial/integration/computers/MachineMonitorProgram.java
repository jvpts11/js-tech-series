/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.api.client.DesktopProgram;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.MenuBar;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.ScrollBar;
import dev.jstech.core.client.gui.component.SearchField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.integration.computers.MachineMonitorPayloads.Answer;
import dev.jstech.industrial.integration.computers.MachineMonitorPayloads.Detail;
import dev.jstech.industrial.integration.computers.MachineMonitorPayloads.Row;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Machine Monitor's window, on the desktop of the computer at its host: the alerts of every Industrial Controller
 * Computer on the computer's network, newest first, a lamp of each one's grade, the alerts shown chosen by their kind
 * (and, in the modern look, by a machine's name), the counts of each kind, the panel of the machine chosen with what it
 * reads now, and a status line. Acknowledge dims the alert chosen, Acknowledge all every alert; nothing is ever taken
 * away. It asks the server again every couple of seconds while it is open. Every position is read from
 * {@link MachineMonitorLayout}, the same numbers its test checks.
 */
final class MachineMonitorProgram extends DesktopProgram {

    private final BlockPos host;
    private final boolean classic;
    private final Panel body = new Panel();
    private final MenuBar menus = new MenuBar(MachineMonitorLayout.MENU_W, MachineMonitorLayout.MENU_H);
    private final Button acknowledge;
    private final Button acknowledgeAll;
    private final Label show;
    private final Dropdown filter;
    private final SearchField search = new SearchField(SEARCH_LETTERS);
    private final ColumnHeader columns;
    private final ListView<Row> list;
    private final ScrollBar bar;
    /** The kind shown: none for every alert. */
    private @Nullable AlertKind kind;
    /** The alert chosen, by its controller and its number, kept as the list is asked for again. */
    private long chosenController = Long.MIN_VALUE;
    private long chosenId = Long.MIN_VALUE;
    private int frame;

    /* How often it asks the server again while open, in frames: about every two seconds. */
    private static final int REFRESH_FRAMES = 40;
    private static final int SEARCH_LETTERS = 32;
    private static final int GAP = MachineMonitorLayout.GAP;
    /* The kinds in the order the choice of alerts and the counts show them. */
    private static final List<AlertKind> KINDS = List.of(AlertKind.values());

    MachineMonitorProgram(final BlockPos host, @Nullable final ResourceLocation desktop) {
        this.host = host;
        this.classic = classicOn(desktop);
        this.acknowledge = this.body.add(new Button(word(MachineMonitorTexts.ACKNOWLEDGE), this::acknowledgeChosen));
        this.acknowledgeAll = this.body.add(new Button(word(MachineMonitorTexts.ACKNOWLEDGE_ALL),
                this::acknowledgeEvery));
        this.show = this.body.add(new Label(word(MachineMonitorTexts.SHOW)));
        final List<Supplier<String>> kinds = new ArrayList<>();
        kinds.add(word(MachineMonitorTexts.ALL_ALERTS));
        for (final AlertKind one : KINDS) {
            kinds.add(word(one.text()));
        }
        this.filter = this.body.add(new Dropdown(kinds, () -> this.kind == null ? 0 : KINDS.indexOf(this.kind) + 1,
                this::choose, shown -> shown));
        this.search.setPlaceholder(GameText.resolve(MachineMonitorTexts.SEARCH));
        if (!this.classic) {
            this.body.add(this.search);
        }
        this.columns = this.body.add(new ColumnHeader(List.of("", GameText.resolve(MachineMonitorTexts.TIME),
                GameText.resolve(MachineMonitorTexts.MACHINE), GameText.resolve(MachineMonitorTexts.WHERE),
                GameText.resolve(MachineMonitorTexts.ALERT), GameText.resolve(MachineMonitorTexts.READING)))
                .setSortable(false));
        this.list = this.body.add(new ListView<>(this::shown, MachineMonitorLayout.ROW_H, this::renderRow)
                .setSelectable(true).setOnClick((index, button, mx, my) -> chooseRow(index)));
        this.bar = this.body.add(new ScrollBar(() -> Math.max(0, shown().size() - this.list.visibleRows()),
                this.list::scroll, this.list::setScroll));
        this.menus.add(GameText.resolve(MachineMonitorTexts.MENU_VIEW), this::viewMenu)
                .add(GameText.resolve(MachineMonitorTexts.MENU_ALERTS), this::alertsMenu);
    }

    @Override
    public String title() {
        return GameText.resolve(MachineMonitor.SPEC.name());
    }

    @Override
    public int width() {
        return this.classic ? MachineMonitorLayout.CLASSIC_W : MachineMonitorLayout.MODERN_W;
    }

    @Override
    public int height() {
        return this.classic ? MachineMonitorLayout.CLASSIC_H : MachineMonitorLayout.MODERN_H;
    }

    @Override
    public void draw(final GuiGraphics g, final Font font, final ISkin skin, final int x, final int y, final int w,
                     final int h, final int mouseX, final int mouseY) {
        if (this.frame++ % REFRESH_FRAMES == 0) {
            PacketDistributor.sendToServer(new MachineMonitorPayloads.Request(this.host));
        }
        final Answer answer = MachineMonitorPayloads.latest(this.host);
        layout(x, y, w, h);
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, 0f);
        final int tableW = MachineMonitorLayout.tableW(w, this.classic);
        final int listY = y + MachineMonitorLayout.listY(this.classic);
        skin.field(g, x, listY, tableW, MachineMonitorLayout.listH(h, this.classic), false);
        drawCounts(g, font, skin, x, y + MachineMonitorLayout.chipsY(this.classic), answer);
        this.body.render(g, ctx);
        if (answer == null || shown().isEmpty()) {
            small(g, font, GameText.resolve(answer == null ? MachineMonitorTexts.READING_NETWORK
                    : MachineMonitorTexts.NO_ALERTS), x + 3, listY + 3, skin.dim());
        }
        final int[] panel = MachineMonitorLayout.detail(w, h, this.classic);
        drawDetail(g, font, skin, x + panel[0], y + panel[1], panel[2], panel[3]);
        drawStatus(g, font, skin, x, y + MachineMonitorLayout.statusY(h), w, answer);
        if (this.classic) {
            this.menus.setWindow(x, y, w, h);
            this.menus.render(g, ctx);
        }
        this.body.renderOverlay(g, ctx);
    }

    @Override
    public void mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.classic && (this.menus.isOpen() || this.menus.titleAt(mouseX, mouseY) >= 0)) {
            this.menus.mouseClicked(mouseX, mouseY, button);
            return;
        }
        this.body.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final double mouseX, final double mouseY, final int button) {
        this.body.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.list.setScroll(this.list.scroll() - (int) Math.signum(delta));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.classic && this.menus.isOpen()) {
            return this.menus.keyPressed(key, scanCode, modifiers);
        }
        return this.body.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char typed) {
        return this.body.charTyped(typed);
    }

    /** The alerts the list shows now, by the kind chosen and the machine searched for; for tests too. */
    List<Row> shown() {
        final Answer answer = MachineMonitorPayloads.latest(this.host);
        if (answer == null) {
            return List.of();
        }
        final String wanted = this.classic ? "" : this.search.query().strip().toLowerCase(Locale.ROOT);
        final List<Row> rows = new ArrayList<>();
        for (final Row row : answer.rows()) {
            if ((this.kind == null || row.kind() == this.kind)
                    && (wanted.isEmpty() || row.machine().toLowerCase(Locale.ROOT).contains(wanted))) {
                rows.add(row);
            }
        }
        return rows;
    }

    /** Whether it is drawn in its classic look; for tests. */
    boolean classic() {
        return this.classic;
    }

    /* Puts each component where the layout says, in a window at x, y, w wide and h tall. */
    private void layout(final int x, final int y, final int w, final int h) {
        this.body.setBounds(x, y, w, h);
        this.menus.setBounds(x, y, w, MachineMonitorLayout.MENU_H);
        final int tools = y + MachineMonitorLayout.toolsY(this.classic);
        this.acknowledge.setBounds(x, tools, MachineMonitorLayout.ACK_W, MachineMonitorLayout.BUTTON_H);
        this.acknowledgeAll.setBounds(x + MachineMonitorLayout.ackAllX(), tools, MachineMonitorLayout.ACK_ALL_W,
                MachineMonitorLayout.BUTTON_H);
        this.show.setVisible(this.classic);
        this.show.setBounds(x + MachineMonitorLayout.showX(), tools + 2, MachineMonitorLayout.SHOW_W, 8);
        this.filter.setBounds(x + MachineMonitorLayout.filterX(this.classic), tools, MachineMonitorLayout.FILTER_W,
                MachineMonitorLayout.BUTTON_H);
        this.search.setBounds(x + MachineMonitorLayout.searchX(), tools, MachineMonitorLayout.SEARCH_W,
                MachineMonitorLayout.BUTTON_H);
        final int tableW = MachineMonitorLayout.tableW(w, this.classic);
        this.columns.setBounds(x, y + MachineMonitorLayout.headerY(this.classic),
                tableW - MachineMonitorLayout.BAR_W, MachineMonitorLayout.HEADER_H);
        this.columns.setColumnX(x, x + MachineMonitorLayout.timeX(), x + MachineMonitorLayout.machineX(),
                x + MachineMonitorLayout.whereX(), x + MachineMonitorLayout.alertX(),
                x + MachineMonitorLayout.readingX());
        final int listY = y + MachineMonitorLayout.listY(this.classic);
        final int listH = MachineMonitorLayout.listH(h, this.classic);
        this.list.setBounds(x, listY, tableW - MachineMonitorLayout.BAR_W, listH);
        this.list.setSelected(indexOfChosen());
        final int rows = shown().size();
        this.bar.setBounds(x + tableW - MachineMonitorLayout.BAR_W, listY, MachineMonitorLayout.BAR_W,
                this.list.visibleRows() * MachineMonitorLayout.ROW_H);
        this.bar.setVisible(rows > this.list.visibleRows());
        final Row chosen = chosen();
        this.acknowledge.setEnabled(chosen != null && !chosen.acknowledged());
        this.acknowledgeAll.setEnabled(shown().stream().anyMatch(row -> !row.acknowledged()));
    }

    /* The count of each kind of alert kept, each with its grade's lamp, one after the other. */
    private void drawCounts(final GuiGraphics g, final Font font, final ISkin skin, final int x, final int y,
                            @Nullable final Answer answer) {
        final Map<AlertKind, Integer> counts = new EnumMap<>(AlertKind.class);
        if (answer != null) {
            for (final Row row : answer.rows()) {
                counts.merge(row.kind(), 1, Integer::sum);
            }
        }
        int at = x;
        for (final AlertKind one : KINDS) {
            lamp(g, at, y + 2, one.severity(), false);
            final String label = GameText.resolve(MachineMonitorTexts.COUNTED.with(one.text(),
                    counts.getOrDefault(one, 0)));
            small(g, font, label, at + MachineMonitorLayout.LAMP + 2, y + 1, skin.text());
            at += MachineMonitorLayout.LAMP + 2 + Math.round(font.width(label) * MachineMonitorLayout.SMALL)
                    + MachineMonitorLayout.CHIP_GAP;
        }
    }

    private void renderRow(final GuiGraphics g, final UiContext ctx, final Row row, final int index, final int x,
                           final int y, final int w, final int h, final boolean hovered, final boolean selected) {
        final Font font = ctx.font();
        final ISkin skin = ctx.skin();
        skin.listRow(g, x, y, w, h, hovered, selected);
        /*
         * An alert acknowledged is dimmed, never taken away, and its lamp goes hollow: still read on the row chosen,
         * whose text keeps the selection's ink to stay legible on it.
         */
        final int ink = row.acknowledged() && !selected ? skin.dim() : skin.listRowText(selected);
        lamp(g, x + 1, y + 2, row.kind().severity(), row.acknowledged());
        small(g, font, IndustrialControllerBlockEntity.clock(row.dayTime()), x + MachineMonitorLayout.timeX(), y + 2,
                ink);
        cell(g, font, row.machine(), x + MachineMonitorLayout.machineX(), y, MachineMonitorLayout.MACHINE_W, ink);
        cell(g, font, row.where().toShortString(), x + MachineMonitorLayout.whereX(), y, MachineMonitorLayout.WHERE_W,
                ink);
        cell(g, font, GameText.resolve(row.kind().text()), x + MachineMonitorLayout.alertX(), y,
                MachineMonitorLayout.ALERT_W, ink);
        cell(g, font, GameText.resolve(row.reading()), x + MachineMonitorLayout.readingX(), y,
                MachineMonitorLayout.readingW(w + MachineMonitorLayout.BAR_W), ink);
    }

    /* The panel of the alert chosen: its machine, the alert and since when, and what the machine reads now. */
    private void drawDetail(final GuiGraphics g, final Font font, final ISkin skin, final int x, final int y,
                            final int w, final int h) {
        skin.field(g, x, y, w, h, false);
        final Row row = chosen();
        if (row == null) {
            small(g, font, Texts.clip(font, GameText.resolve(MachineMonitorTexts.CHOOSE),
                    (int) (w / MachineMonitorLayout.SMALL) - 8), x + 4, y + 4, skin.dim());
            return;
        }
        final List<String> lines = detailLines(row);
        final int perColumn = this.classic ? (lines.size() + 1) / 2 : lines.size();
        final int columnW = this.classic ? (w - 3 * GAP) / 2 : w - 2 * GAP;
        for (int i = 0; i < lines.size(); i++) {
            final int column = i / perColumn;
            final int line = i % perColumn;
            final String text = lines.get(i);
            final int lineY = y + GAP + line * MachineMonitorLayout.LINE_H;
            if (lineY + MachineMonitorLayout.LINE_H > y + h) {
                continue;
            }
            small(g, font, Texts.clip(font, text, (int) (columnW / MachineMonitorLayout.SMALL)),
                    x + GAP + column * (columnW + GAP), lineY, i == 0 ? skin.text() : skin.dim());
        }
    }

    /* The panel's lines: the machine, the alert and since when, where it stands, then what it reads now. */
    private static List<String> detailLines(final Row row) {
        final Detail now = row.detail();
        final List<String> lines = new ArrayList<>();
        lines.add(row.machine());
        lines.add(GameText.resolve((row.active() ? MachineMonitorTexts.SINCE : MachineMonitorTexts.OVER)
                .with(row.kind().text(), IndustrialControllerBlockEntity.clock(row.dayTime()))));
        lines.add(GameText.resolve(row.reading()));
        lines.add(GameText.resolve(MachineMonitorTexts.AT.with(row.where().toShortString())));
        if (now.temperature() != Detail.NONE) {
            lines.add(GameText.resolve(MachineMonitorTexts.TEMPERATURE.with(now.temperature())));
            lines.add(GameText.resolve(MachineMonitorTexts.LIMIT.with(now.limit())));
        }
        if (!now.state().isEmpty()) {
            lines.add(GameText.resolve(MachineMonitorTexts.STATE.with(now.state())));
        }
        if (now.power() != Detail.NONE) {
            final UnitFormatter.Scaled power = UnitFormatter.forCurrentLocale().si(now.power());
            lines.add(GameText.resolve(now.port().isEmpty()
                    ? MachineMonitorTexts.POWER.with(power.number(), power.prefix())
                    : MachineMonitorTexts.POWER_PORT.with(power.number(), power.prefix(), now.port())));
        }
        lines.add(GameText.resolve(MachineMonitorTexts.READ_BY.with(now.controller())));
        if (!now.card().isEmpty()) {
            lines.add(now.card());
        }
        return lines;
    }

    /* The status line: how many alerts and how many not acknowledged, the controllers and machines read, and when. */
    private static void drawStatus(final GuiGraphics g, final Font font, final ISkin skin, final int x, final int y,
                                   final int w, @Nullable final Answer answer) {
        skin.statusBar(g, x, y, w, MachineMonitorLayout.STATUS_H);
        if (answer == null) {
            return;
        }
        final long unseen = answer.rows().stream().filter(row -> !row.acknowledged()).count();
        final String line = GameText.resolve(MachineMonitorTexts.STATUS_LINE.with(
                MachineMonitorTexts.ALERTS_COUNT.with(answer.rows().size(), unseen),
                MachineMonitorTexts.READ_COUNT.with(answer.controllers(), answer.machines()),
                MachineMonitorTexts.READ_AT.with(IndustrialControllerBlockEntity.clock(answer.dayTime()))));
        small(g, font, Texts.clip(font, line, (int) (w / MachineMonitorLayout.SMALL) - 8), x + 3, y + 2,
                skin.text());
    }

    private static void cell(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                             final int w, final int ink) {
        small(g, font, Texts.clip(font, text, (int) (w / MachineMonitorLayout.SMALL)), x, y + 2, ink);
    }

    /* Text at the window's small scale, the one its layout measures with. */
    private static void small(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                              final int ink) {
        Texts.scaled(g, font, text, x, y, MachineMonitorLayout.SMALL, ink);
    }

    /* The lamp of a grade: lit, or only its rim once the alert is acknowledged. */
    private static void lamp(final GuiGraphics g, final int x, final int y, final AlertKind.Severity severity,
                             final boolean hollow) {
        final MachineMonitorColours.Colours colours = MachineMonitorColours.PALETTE.get();
        final int colour = switch (severity) {
            case CRITICAL -> colours.critical();
            case WARNING -> colours.warning();
            case NOTICE -> colours.notice();
        };
        final int size = MachineMonitorLayout.LAMP;
        if (!hollow) {
            g.fill(x, y, x + size, y + size, colour);
            return;
        }
        g.fill(x, y, x + size, y + 1, colour);
        g.fill(x, y + size - 1, x + size, y + size, colour);
        g.fill(x, y + 1, x + 1, y + size - 1, colour);
        g.fill(x + size - 1, y + 1, x + size, y + size - 1, colour);
    }

    private List<ContextMenu.Item> viewMenu() {
        final List<ContextMenu.Item> items = new ArrayList<>();
        items.add(new ContextMenu.Item(GameText.resolve(MachineMonitorTexts.ALL_ALERTS), true, () -> choose(0)));
        int index = 1;
        for (final AlertKind one : KINDS) {
            final int chosen = index++;
            items.add(new ContextMenu.Item(GameText.resolve(one.text()), true, () -> choose(chosen)));
        }
        return items;
    }

    private List<ContextMenu.Item> alertsMenu() {
        final Row chosen = chosen();
        return List.of(new ContextMenu.Item(GameText.resolve(MachineMonitorTexts.ACKNOWLEDGE),
                        chosen != null && !chosen.acknowledged(), this::acknowledgeChosen),
                new ContextMenu.Item(GameText.resolve(MachineMonitorTexts.ACKNOWLEDGE_ALL),
                        shown().stream().anyMatch(row -> !row.acknowledged()), this::acknowledgeEvery));
    }

    private void choose(final int index) {
        this.kind = index <= 0 || index > KINDS.size() ? null : KINDS.get(index - 1);
        this.list.setScroll(0);
    }

    private void chooseRow(final int index) {
        final List<Row> rows = shown();
        if (index >= 0 && index < rows.size()) {
            this.chosenController = rows.get(index).controller();
            this.chosenId = rows.get(index).id();
        }
    }

    private @Nullable Row chosen() {
        for (final Row row : shown()) {
            if (row.controller() == this.chosenController && row.id() == this.chosenId) {
                return row;
            }
        }
        return null;
    }

    private int indexOfChosen() {
        final List<Row> rows = shown();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).controller() == this.chosenController && rows.get(i).id() == this.chosenId) {
                return i;
            }
        }
        return -1;
    }

    private void acknowledgeChosen() {
        final Row chosen = chosen();
        if (chosen != null && !chosen.acknowledged()) {
            PacketDistributor.sendToServer(new MachineMonitorPayloads.Acknowledge(this.host, chosen.controller(),
                    chosen.id()));
        }
    }

    private void acknowledgeEvery() {
        PacketDistributor.sendToServer(new MachineMonitorPayloads.Acknowledge(this.host, 0L,
                MachineMonitorPayloads.EVERY_ALERT));
    }

    private static Supplier<String> word(final TextKey key) {
        return () -> GameText.resolve(key);
    }

    /* The classic look on the desktops of a menu bar's age: Frames 95, XP and 7. */
    private static boolean classicOn(@Nullable final ResourceLocation desktop) {
        final DesktopEnvironmentDef def = desktop == null ? null : OsRegistry.getDesktop(desktop);
        if (def == null) {
            return true;
        }
        return def.panelStyle() == PanelStyle.FRAMES_95 || def.panelStyle() == PanelStyle.FRAMES_XP
                || def.panelStyle() == PanelStyle.FRAMES_7;
    }
}
