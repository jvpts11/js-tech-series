/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.gui.layout.NetworkEventLogLayout;
import dev.jstech.computers.operation.payload.NetworkEventsPayload;
import dev.jstech.computers.operation.payload.RequestNetworkEventsPayload;
import dev.jstech.computers.printer.PrintLayout;
import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.ScrollBar;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Manager's network events: what happened to every machine on the network, newest first, through three
 * filters (the level, the machine, how far back), with Print and Export under the list. The Log tab shows it beside
 * the Operations, and every position on it is read from {@link NetworkEventLogLayout}, the same numbers its test
 * checks.
 *
 * <p>It asks the server again every couple of seconds while it shows, saying which revision of the log it holds; the
 * server answers with the rows only when the log has changed since, or the filters have.
 */
@PaletteHolder
final class NetworkEventLogView {

    private final BlockPos host;
    private final PrintDialog printDialog;
    private final FileDialog files;
    private final Supplier<String> programName;
    private final Panel body = new Panel();
    private final Dropdown levelFilter;
    private Dropdown machineFilter;
    private final Dropdown periodFilter;
    private final ColumnHeader columns;
    private final ListView<NetworkEventsPayload.Row> list;
    private final ScrollBar bar;
    private final Label none;
    private final Label count;
    private final Button print;
    private final Button export;
    private List<NetworkEventsPayload.Row> rows = List.of();
    private List<NetworkEventsPayload.Machine> machines = List.of();
    private long revision = RequestNetworkEventsPayload.NOTHING_KNOWN;
    private boolean answered;
    /** The level shown: 0 for every one, else the level's place in {@link EventLevel#ALL} plus one. */
    private int level;
    /** The machine shown, by its node's id, or one of the request's two marks. */
    private String machine = RequestNetworkEventsPayload.ANY_MACHINE;
    private byte period = RequestNetworkEventsPayload.PERIOD_DAY;
    /** What became of the last export, shown in place of the count until the filters change. */
    @Nullable
    private Text exported;
    /** Where the last export was sent to be written, or null before any. */
    @Nullable
    private String exportedPath;
    /** The whole of the line under the pointer when its column cuts it, drawn in the window's tooltip pass. */
    @Nullable
    private String hoveredMessage;
    private int frame;

    private static final int REFRESH_FRAMES = 40;
    private static final int TIP_H = 13;
    /*
     * Plain text, which the Editor opens: a .log is the system's own, written only by the system, so a program's save
     * of one is refused.
     */
    private static final String FILE_NAME = "network-log-day%d.txt";
    private static final String FILE_KIND = "txt";
    /** The levels' colours in the list: app/network_event_log. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/network_event_log",
            new Colours(0xFF7D8A9C, 0xFFE0A020, 0xFFD1495B));

    NetworkEventLogView(final BlockPos host, final IDesktopApp owner, final PrintDialog printDialog,
                        final Supplier<String> programName) {
        this.host = host;
        this.printDialog = printDialog;
        this.files = new FileDialog(host, owner);
        this.programName = programName;
        final List<Supplier<String>> levels = new ArrayList<>();
        levels.add(() -> GameText.resolve(NetworkEventLogTexts.ALL_LEVELS));
        for (final EventLevel one : EventLevel.ALL) {
            levels.add(() -> GameText.resolve(one.label()));
        }
        levelFilter = body.add(new Dropdown(levels, () -> level, this::chooseLevel, shown -> shown));
        machineFilter = body.add(machineDropdown());
        periodFilter = body.add(new Dropdown(List.of(word(NetworkEventLogTexts.LAST_HOUR),
                word(NetworkEventLogTexts.LAST_DAY), word(NetworkEventLogTexts.LAST_WEEK),
                word(NetworkEventLogTexts.ALL_KEPT)), () -> period, this::choosePeriod, shown -> shown));
        columns = body.add(new ColumnHeader(List.of(GameText.resolve(NetworkEventLogTexts.TIME),
                GameText.resolve(NetworkEventLogTexts.LEVEL), GameText.resolve(NetworkEventLogTexts.MACHINE),
                GameText.resolve(NetworkEventLogTexts.EVENT))).setSortable(false));
        list = body.add(new ListView<>(() -> rows, NetworkEventLogLayout.ROW_H, this::renderRow));
        bar = body.add(new ScrollBar(() -> Math.max(0, rows.size() - list.visibleRows()), list::scroll,
                list::setScroll));
        none = body.add(new Label(GameText.resolve(NetworkEventLogTexts.NONE), Label.Tone.DIM));
        count = body.add(new Label(this::countText, Label.Tone.DIM));
        print = body.add(new Button(GameText.resolve(PrintTexts.PRINT), this::print));
        export = body.add(new Button(GameText.resolve(NetworkEventLogTexts.EXPORT), this::chooseExport));
    }

    /** The events, which the window adds to its own components. */
    UiComponent component() {
        return body;
    }

    /** Takes what the server says the log shows, if it is about this window's Mainframe. */
    void accept(final NetworkEventsPayload payload) {
        if (!payload.hostPos().equals(host)) {
            return;
        }
        answered = true;
        revision = payload.revision();
        if (payload.unchanged()) {
            return;
        }
        rows = payload.rows();
        if (!sameMachines(payload.machines())) {
            machines = payload.machines();
            body.remove(machineFilter);
            machineFilter = body.add(machineDropdown());
        }
    }

    /** Asks the server what the log shows through the filters, saying which revision this window holds. */
    void request() {
        final byte asked = level == 0 ? RequestNetworkEventsPayload.ANY_LEVEL
                : (byte) EventLevel.ALL.get(level - 1).id();
        PacketDistributor.sendToServer(new RequestNetworkEventsPayload(host, asked, machine, period, revision));
    }

    /** The rows shown now, newest first; for tests. */
    List<NetworkEventsPayload.Row> rows() {
        return rows;
    }

    /** Whether the server has answered yet; for tests. */
    boolean answered() {
        return answered;
    }

    /** Shows only {@code what}, or every level for null; for tests and the filter. */
    void showLevel(@Nullable final EventLevel what) {
        chooseLevel(what == null ? 0 : EventLevel.ALL.indexOf(what) + 1);
    }

    /** The centre of Export, where a test clicks it. */
    int[] exportPoint() {
        return export.center();
    }

    /** The system's file dialog Export opens; for tests. */
    FileDialog exportDialog() {
        return files;
    }

    /** Where the last export was sent to be written, or null before any; for tests. */
    @Nullable
    String exportedPath() {
        return exportedPath;
    }

    /** The events as the file Export writes: one line each, as the filters show them. */
    String exportText() {
        final StringBuilder out = new StringBuilder();
        for (final NetworkEventsPayload.Row row : rows) {
            out.append(timeOf(row)).append(' ').append(GameText.resolve(EventLevel.byId(row.level()).label()))
                    .append(' ').append(machineOf(row)).append(' ').append(GameText.resolve(row.message()))
                    .append('\n');
        }
        return out.toString();
    }

    /** Lays the events out in the tab's rectangle under the switch, and keeps them fresh while they show. */
    void layout(final int x, final int y, final int w, final int h, final boolean shown) {
        body.setVisible(shown);
        hoveredMessage = null;
        if (!shown) {
            return;
        }
        if (frame++ % REFRESH_FRAMES == 0) {
            request();
        }
        // The panel passes a click on only to what lies inside its own bounds.
        body.setBounds(x, y, w, h);
        levelFilter.setBounds(x, y + NetworkEventLogLayout.FILTER_Y, NetworkEventLogLayout.LEVEL_FILTER_W,
                NetworkEventLogLayout.FILTER_H);
        machineFilter.setBounds(x + NetworkEventLogLayout.machineFilterX(), y + NetworkEventLogLayout.FILTER_Y,
                NetworkEventLogLayout.MACHINE_FILTER_W, NetworkEventLogLayout.FILTER_H);
        periodFilter.setBounds(x + NetworkEventLogLayout.periodFilterX(), y + NetworkEventLogLayout.FILTER_Y,
                NetworkEventLogLayout.PERIOD_FILTER_W, NetworkEventLogLayout.FILTER_H);
        columns.setBounds(x, y + NetworkEventLogLayout.HEADER_Y, w - NetworkEventLogLayout.BAR_W,
                NetworkEventLogLayout.HEADER_H);
        columns.setColumnX(x + 2, x + NetworkEventLogLayout.levelX(), x + NetworkEventLogLayout.machineX(),
                x + NetworkEventLogLayout.eventX());
        final int listH = NetworkEventLogLayout.listH(h);
        list.setBounds(x, y + NetworkEventLogLayout.LIST_Y, w - NetworkEventLogLayout.BAR_W, listH);
        list.setVisible(!rows.isEmpty());
        bar.setBounds(x + w - NetworkEventLogLayout.BAR_W, y + NetworkEventLogLayout.LIST_Y,
                NetworkEventLogLayout.BAR_W, list.visibleRows() * NetworkEventLogLayout.ROW_H);
        bar.setVisible(rows.size() > list.visibleRows());
        none.setVisible(answered && rows.isEmpty());
        none.setBounds(x + 2, y + NetworkEventLogLayout.LIST_Y + 2, w, 8);
        final int footY = y + NetworkEventLogLayout.footY(h);
        count.setBounds(x, footY + 2, NetworkEventLogLayout.printX(w) - NetworkEventLogLayout.GAP, 8);
        print.setBounds(x + NetworkEventLogLayout.printX(w), footY, NetworkEventLogLayout.PRINT_W,
                NetworkEventLogLayout.BUTTON_H);
        export.setBounds(x + NetworkEventLogLayout.exportX(w), footY, NetworkEventLogLayout.EXPORT_W,
                NetworkEventLogLayout.BUTTON_H);
        print.setEnabled(!rows.isEmpty());
        export.setEnabled(!rows.isEmpty());
    }

    /** Scrolls the list by {@code step} rows. */
    void scroll(final int step) {
        list.setScroll(list.scroll() + step);
    }

    /**
     * Draws the whole of a line too long for its column over everything, beside the pointer and inside the window's
     * content ({@code cx}, {@code cy}, {@code cw}, {@code ch}).
     */
    void renderTooltip(final GuiGraphics g, final Font font, final ISkin skin, final int mouseX, final int mouseY,
                       final int cx, final int cy, final int cw, final int ch) {
        if (hoveredMessage == null || !body.visible()) {
            return;
        }
        final int boxW = Math.min(cw, font.width(hoveredMessage) + 8);
        final String shown = Texts.clip(font, hoveredMessage, boxW - 8);
        final int bx = Math.max(cx, Math.min(mouseX + 10, cx + cw - boxW - 1));
        final int by = Math.max(cy, Math.min(mouseY + 6, cy + ch - TIP_H - 1));
        // The tooltip pass runs at the base pose, so it lifts to the tooltip depth to draw over the window.
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.TOOLTIP);
        skin.tooltip(g, bx, by, boxW, TIP_H);
        Draw.text(g, font, shown, bx + 4, by + 3, skin.text());
        g.pose().popPose();
    }

    private void chooseLevel(final int index) {
        level = index;
        refilter();
    }

    private void chooseMachine(final int index) {
        machine = index == 0 ? RequestNetworkEventsPayload.ANY_MACHINE
                : index == 1 ? RequestNetworkEventsPayload.WHOLE_NETWORK
                : index - 2 < machines.size() ? machines.get(index - 2).id() : RequestNetworkEventsPayload.ANY_MACHINE;
        refilter();
    }

    private void choosePeriod(final int index) {
        period = (byte) index;
        refilter();
    }

    /* The filters changed: what the window holds no longer answers them, so it asks afresh. */
    private void refilter() {
        revision = RequestNetworkEventsPayload.NOTHING_KNOWN;
        exported = null;
        list.setScroll(0);
        request();
    }

    /* The filter by machine: every machine, the whole network, then each machine the log names. */
    private Dropdown machineDropdown() {
        final List<Supplier<String>> options = new ArrayList<>();
        options.add(word(NetworkEventLogTexts.ALL_MACHINES));
        options.add(word(NetworkEvents.WHOLE_NETWORK));
        for (final NetworkEventsPayload.Machine one : machines) {
            options.add(one::name);
        }
        return new Dropdown(options, this::machineIndex, this::chooseMachine, shown -> shown);
    }

    private int machineIndex() {
        if (machine.equals(RequestNetworkEventsPayload.WHOLE_NETWORK)) {
            return 1;
        }
        for (int i = 0; i < machines.size(); i++) {
            if (machines.get(i).id().equals(machine)) {
                return i + 2;
            }
        }
        return 0;
    }

    private boolean sameMachines(final List<NetworkEventsPayload.Machine> other) {
        if (other.size() != machines.size()) {
            return false;
        }
        for (int i = 0; i < other.size(); i++) {
            if (!other.get(i).equals(machines.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String countText() {
        if (exported != null) {
            return GameText.resolve(exported);
        }
        return GameText.resolve(rows.size() == 1 ? NetworkEventLogTexts.ONE.text()
                : NetworkEventLogTexts.COUNT.with(rows.size()));
    }

    private void renderRow(final GuiGraphics g, final UiContext ctx, final NetworkEventsPayload.Row row,
                           final int index, final int x, final int y, final int w, final int h,
                           final boolean hovered, final boolean selected) {
        final Font font = ctx.font();
        ctx.skin().listRow(g, x, y, w, h, hovered, false);
        Draw.text(g, font, timeOf(row), x + 2, y + 2, ctx.skin().dim());
        final EventLevel what = EventLevel.byId(row.level());
        Draw.text(g, font, GameText.resolve(what.label()), x + NetworkEventLogLayout.levelX(), y + 2,
                colourOf(what));
        Draw.text(g, font, Texts.clip(font, machineOf(row), NetworkEventLogLayout.MACHINE_W),
                x + NetworkEventLogLayout.machineX(), y + 2, ctx.skin().text());
        final String message = GameText.resolve(row.message());
        final int eventW = w - NetworkEventLogLayout.eventX();
        Draw.text(g, font, Texts.clip(font, message, eventW), x + NetworkEventLogLayout.eventX(), y + 2,
                ctx.skin().text());
        if (hovered && font.width(message) > eventW) {
            hoveredMessage = message;
        }
    }

    /* The world's clock for an event of today, else its day. */
    private static String timeOf(final NetworkEventsPayload.Row row) {
        final Minecraft mc = Minecraft.getInstance();
        final long today = mc.level == null ? -1L : GameCalendar.day(mc.level.getDayTime());
        final long day = GameCalendar.day(row.dayTime());
        if (day == today) {
            return String.format(Locale.ROOT, "%02d:%02d", GameCalendar.hourOf(row.dayTime()),
                    GameCalendar.minuteOf(row.dayTime()));
        }
        return GameText.resolve(NetworkEventLogTexts.DAY.with(day));
    }

    private static String machineOf(final NetworkEventsPayload.Row row) {
        return row.machineName().isEmpty() ? GameText.resolve(NetworkEvents.WHOLE_NETWORK) : row.machineName();
    }

    private static int colourOf(final EventLevel what) {
        final Colours colours = PALETTE.get();
        return switch (what) {
            case INFO -> colours.info();
            case WARN -> colours.warn();
            case ERROR -> colours.error();
        };
    }

    /* The events as they are shown, to the machine's printer. */
    private void print() {
        final List<List<String>> table = new ArrayList<>();
        table.add(List.of(GameText.resolve(NetworkEventLogTexts.TIME), GameText.resolve(NetworkEventLogTexts.LEVEL),
                GameText.resolve(NetworkEventLogTexts.MACHINE), GameText.resolve(NetworkEventLogTexts.EVENT)));
        for (final NetworkEventsPayload.Row row : rows) {
            table.add(List.of(timeOf(row), GameText.resolve(EventLevel.byId(row.level()).label()), machineOf(row),
                    GameText.resolve(row.message())));
        }
        printDialog.show(PrintDialog.Document.text(GameText.resolve(NetworkEventLogTexts.PRINT_TITLE),
                programName.get(), PrintLayout.table(table, true)));
    }

    private void chooseExport() {
        final Minecraft mc = Minecraft.getInstance();
        final long day = mc.level == null ? 0L : GameCalendar.day(mc.level.getDayTime());
        files.saveAs(NetworkEventLogTexts.EXPORT_TITLE.text(), "", String.format(Locale.ROOT, FILE_NAME, day),
                List.of(FileDialog.Filter.of(NetworkEventLogTexts.TEXT_FILES, FILE_KIND)), this::exportTo);
    }

    private void exportTo(final String path) {
        final String content = exportText();
        if (!FileSaves.send(host, path, content)) {
            exported = FileSaves.tooLong(path, content);
            return;
        }
        FilesApps.diskChanged();
        exported = NetworkEventLogTexts.EXPORTED.with(path);
        exportedPath = path;
    }

    private static Supplier<String> word(final TextKey key) {
        return () -> GameText.resolve(key);
    }

    /* The three levels' colours in the list. */
    private record Colours(int info, int warn, int error) {
    }
}
