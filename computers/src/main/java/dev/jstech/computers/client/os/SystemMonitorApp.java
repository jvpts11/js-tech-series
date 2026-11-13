/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.ThermalModel;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.VramLedger;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.computers.operation.payload.MachineReadingsPayload;
import dev.jstech.computers.operation.payload.RequestSettingsPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload.DiskUse;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload.RamUse;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The System Monitor: a pre-installed "this machine at a glance" dashboard. It reports the local computer's
 * processor, memory, and graphics specs, and draws a live usage bar for every disk. It reuses the settings
 * snapshot the Settings app already builds, so it needs no server code of its own, and re-requests it on a
 * slow cadence so the storage bars track items being stored.
 */
@PaletteHolder
public final class SystemMonitorApp implements IDesktopApp {

    private static final int REFRESH_FRAMES = 40;
    /** The window's own colours, {@code jsc:app/system_monitor}: a disk usage bar at each threshold. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/system_monitor",
            new Colours(0xFF2EA043, 0xFFE0A020, 0xFFD1495B));
    private static final int BAR_H = 8;
    private static final int DISK_ROW_H = BAR_H + 12;
    private static final int MEM_ROW_H = 10;
    /** Memory holders shown before the list scrolls, so the disks below keep their room. */
    private static final int MEM_ROWS_SHOWN = 4;
    /* The power and temperature sections: each graph's height, the period menu's width, and the room both take. */
    private static final int GRAPH_H = 14;
    private static final int PERIOD_MENU_W = 72;
    private static final int POWER_ROOM = 2 * (12 + 10 + GRAPH_H) + 6;

    private final BlockPos host;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private SettingsSnapshotPayload data;
    private int frame;
    private int lastMouseX;
    private int lastMouseY;

    /* The monitors open, each told what its machine holds: two on one machine both stay up to date. */
    private static final List<SystemMonitorApp> OPEN = new ArrayList<>();

    private final Panel root = new Panel();
    private final Label loadingLabel;
    private final Label nameLabel;
    private final Label osLabel;
    private final Label[] specGroups = new Label[5];
    private final Label[] specLabels = new Label[5];
    private final Label[] specValues = new Label[5];
    private final Label memoryHeader;
    private final Label memoryFree;
    private final ListView<RamUse> memList;
    private final Label storageHeader;
    private final Label programsLabel;
    private final ListView<DiskUse> diskList;
    /* Power and temperature over the period its menu picks, each a line of figures over a small graph. */
    private final MachineReadingsView readings;
    private final Label powerHeader;
    private final Dropdown periodMenu;
    private final Label drawLabel;
    private final Label drawValue;
    private final Label temperatureHeader;
    private final Label cpuLabel;
    private final Label temperatureValue;
    /* Where the two graphs were laid out this frame: x, y, width, height; a zero width hides one. */
    private final int[] drawGraph = new int[4];
    private final int[] temperatureGraph = new int[4];

    public SystemMonitorApp(final BlockPos host) {
        this.host = host;
        this.readings = new MachineReadingsView(host);
        loadingLabel = root.add(new Label(GameText.resolve(SystemMonitorTexts.READING), Label.Tone.DIM));
        nameLabel = root.add(new Label(() -> data == null || data.computerName().isEmpty()
                ? GameText.resolve(SystemMonitorTexts.COMPUTER) : data.computerName()));
        osLabel = root.add(new Label(() -> data == null ? "" : data.osLabel() + "  (" + data.platform() + ")", Label.Tone.DIM)
                .setAlign(Label.Align.RIGHT));
        spec(0, SystemMonitorTexts.PROCESSOR, () -> data == null || data.cpuLabel().isEmpty() ? "-"
                : GameText.resolve(data.cpuLabel()), () -> cpuClock(data == null ? 0 : data.cpuMhz()));
        // A second processor line with no group of its own, the way a continuation row reads under the one above it.
        spec(1, null, () -> data == null || data.cpuArch().isEmpty() ? "-" : GameText.resolve(data.cpuArch()),
                () -> "");
        spec(2, SystemMonitorTexts.MEMORY, () -> GameText.resolve(SystemMonitorTexts.RAM), () -> data == null ? "-"
                : GameText.resolve(SystemMonitorTexts.HELD_OF.with(RamLedger.heldLabel(heldBytes()),
                        JsTechTheme.fmt(data.ramMb()))));
        // Under the memory, the swap behind it and how much is in it (computers/docs/design/hardware.md).
        spec(3, null, () -> data == null || data.swap().sizeMb() <= 0 ? GameText.resolve(SystemMonitorTexts.SWAP)
                        : GameText.resolve(SystemMonitorTexts.SWAP_ON.with(shortName(data.swap().where()))),
                () -> data == null || data.swap().sizeMb() <= 0 ? GameText.resolve(SystemMonitorTexts.NO_SWAP)
                        : GameText.resolve(SystemMonitorTexts.SWAP_HELD_OF.with(JsTechTheme.fmt(data.swap().usedMb()),
                                JsTechTheme.fmt(data.swap().sizeMb()))));
        // The card by its name, and its video memory held of what it has with how busy it is; a bar under them.
        spec(4, SystemMonitorTexts.GRAPHICS, () -> data == null || !data.gpu().present()
                        ? GameText.resolve(SystemMonitorTexts.NO_GPU) : GameText.resolve(data.gpu().card()),
                () -> data == null || !data.gpu().present() ? "-"
                        : GameText.resolve(SystemMonitorTexts.VIDEO_HELD.with(VramLedger.label(data.gpu().usedKb()),
                                VramLedger.label(data.gpu().totalKb()), data.gpu().load())));
        memoryHeader = root.add(new Label(GameText.resolve(SystemMonitorTexts.MEMORY_HEADER), Label.Tone.DIM));
        memoryFree = root.add(new Label(this::memoryFreeText, Label.Tone.DIM).setAlign(Label.Align.RIGHT));
        memList = root.add(new ListView<RamUse>(() -> data == null ? List.of() : data.ramUses(), MEM_ROW_H, this::renderMemoryRow));
        storageHeader = root.add(new Label(GameText.resolve(SystemMonitorTexts.STORAGE_HEADER), Label.Tone.DIM));
        programsLabel = root.add(new Label(() -> data == null ? ""
                : GameText.resolve(SystemMonitorTexts.PROGRAMS_INSTALLED.with(data.installed().size())), Label.Tone.DIM)
                .setAlign(Label.Align.RIGHT));
        diskList = root.add(new ListView<DiskUse>(this::storageRows, DISK_ROW_H, this::renderDiskRow));
        powerHeader = root.add(new Label(GameText.resolve(TaskManagerTexts.POWER_SECTION), Label.Tone.DIM));
        periodMenu = root.add(new Dropdown(List.of(word(NetworkEventLogTexts.LAST_HOUR),
                word(NetworkEventLogTexts.LAST_DAY), word(NetworkEventLogTexts.LAST_WEEK)), readings::period,
                readings::choose, shown -> shown));
        drawLabel = root.add(new Label(GameText.resolve(TaskManagerTexts.DRAW)));
        drawValue = root.add(new Label(this::drawText).setAlign(Label.Align.RIGHT));
        temperatureHeader = root.add(new Label(GameText.resolve(TaskManagerTexts.TEMPERATURE_SECTION), Label.Tone.DIM));
        cpuLabel = root.add(new Label(GameText.resolve(TaskManagerTexts.CPU)));
        temperatureValue = root.add(new Label(this::temperatureText).setAlign(Label.Align.RIGHT));
        OPEN.add(this);
        PacketDistributor.sendToServer(new RequestSettingsPayload(host));
    }

    /** What the machine is really holding: the sum of what each thing on the list holds this moment. */
    private long heldBytes() {
        long sum = 0L;
        for (final RamUse use : data.ramUses()) {
            sum += use.heldBytes();
        }
        return sum;
    }

    /* What the memory section says of itself: what is free, and what is in swap once something is. */
    private String memoryFreeText() {
        if (data == null) {
            return "";
        }
        final String free = JsTechTheme.fmt(Math.max(0, data.ramMb() - data.ramUsedMb()));
        return data.swap().usedMb() > 0
                ? GameText.resolve(SystemMonitorTexts.FREE_IN_SWAP.with(free, JsTechTheme.fmt(data.swap().usedMb())))
                : GameText.resolve(SystemMonitorTexts.FREE.with(free));
    }

    /* The storage rows: each disk, then the swap partition with how full it is, on a system that keeps one. */
    private List<DiskUse> storageRows() {
        if (data == null) {
            return List.of();
        }
        if (data.swap().sizeMb() <= 0) {
            return data.disks();
        }
        final List<DiskUse> rows = new ArrayList<>(data.disks());
        rows.add(new DiskUse(SystemMonitorTexts.SWAP_PARTITION.with(shortName(data.swap().where())),
                data.swap().sizeMb(), data.swap().usedMb(), false));
        return rows;
    }

    /* A device by its short name, sda5 for /dev/sda5. */
    private static String shortName(final String where) {
        return where.startsWith("/dev/") ? where.substring("/dev/".length()) : where;
    }

    /* A line of specifications: its group, or none for a line that continues the one above, then its two values. */
    private void spec(final int index, @Nullable final TextKey group, final Supplier<String> label,
                      final Supplier<String> value) {
        specGroups[index] = root.add(new Label(group == null ? "" : GameText.resolve(group), Label.Tone.DIM));
        specLabels[index] = root.add(new Label(label));
        specValues[index] = root.add(new Label(value).setAlign(Label.Align.RIGHT));
    }

    /** Routes a settings snapshot to the open System Monitor window (shared with the Settings app). */
    public static void accept(final SettingsSnapshotPayload payload) {
        for (final SystemMonitorApp monitor : OPEN) {
            if (monitor.host.equals(payload.hostPos())) {
                monitor.data = payload;
            }
        }
    }

    @Override
    public String title() {
        return GameText.resolve(SystemMonitorTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return 260;
    }

    @Override
    public int defaultHeight() {
        return 320;
    }

    @Override
    public int minWidth() {
        return 220;
    }

    @Override
    public int minHeight() {
        return 176;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
        // A window brought back with the machine's layout is told again what its machine holds.
        if (!OPEN.contains(this)) {
            OPEN.add(this);
        }
    }

    @Override
    public void onClosed() {
        OPEN.remove(this);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        frame++;
        if (frame % REFRESH_FRAMES == 0) {
            PacketDistributor.sendToServer(new RequestSettingsPayload(host));
        }
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        final int px = x + 6;
        final int pw = width - 12;
        final boolean ready = data != null;
        loadingLabel.setVisible(!ready);
        loadingLabel.setBounds(px, y + 8, pw, 8);
        for (final var c : List.of(nameLabel, osLabel, memoryHeader, memoryFree, memList, storageHeader,
                programsLabel, diskList)) {
            c.setVisible(ready);
        }
        int row = y + 6;
        nameLabel.setBounds(px, row, pw / 2, 8);
        osLabel.setBounds(px + pw / 2, row, pw / 2, 8);
        row += 12;
        if (ready) {
            g.fill(px, row, px + pw, row + 1, skin.edge());
        }
        row += 5;
        for (int i = 0; i < specGroups.length; i++) {
            specGroups[i].setVisible(ready);
            specLabels[i].setVisible(ready);
            specValues[i].setVisible(ready);
            specGroups[i].setBounds(px, row, 60, 8);
            specLabels[i].setBounds(px + 62, row, pw / 2 - 62, 8);
            specValues[i].setBounds(px + pw / 2, row, pw / 2, 8);
            row += 12;
        }
        if (ready && data.gpu().present()) {
            // Under the graphics line, how full the card's memory is.
            final double full = (double) data.gpu().usedKb() / Math.max(1L, data.gpu().totalKb());
            g.fill(px + 62, row - 3, px + pw, row - 1, skin.fieldBg());
            g.fill(px + 62, row - 3, px + 62 + (int) ((pw - 62) * Math.min(1.0, full)), row - 1, skin.accent());
        }
        row += 4;
        memoryHeader.setBounds(px, row, pw / 2, 8);
        memoryFree.setBounds(px + pw / 2, row, pw / 2, 8);
        row += 12;
        // The memory list shows a few holders and scrolls past that, so the disks below keep their room.
        final int holders = data == null ? 1 : Math.max(1, data.ramUses().size());
        final int memH = MEM_ROW_H * Math.min(MEM_ROWS_SHOWN, holders);
        memList.setBounds(px, row, pw, memH);
        row += memH + 4;
        storageHeader.setBounds(px, row, pw / 2, 8);
        programsLabel.setBounds(px + pw / 2, row, pw / 2, 8);
        row += 12;
        // The disks take their rows, and power and temperature the rest under them.
        final int disks = data == null ? 1 : Math.max(1, data.disks().size());
        final int diskH = Math.max(DISK_ROW_H, Math.min(DISK_ROW_H * disks, y + height - row - POWER_ROOM));
        diskList.setBounds(px, row, pw, diskH);
        row += diskH + 4;
        layoutPower(px, row, pw, ready);
        root.render(g, ctx);
        if (ready) {
            renderGraph(g, drawGraph, readings.latest() == null ? new int[0] : readings.latest().draw(),
                    readings.latest() == null ? 1 : readings.latest().rating());
            renderGraph(g, temperatureGraph, readings.latest() == null ? new int[0]
                    : readings.latest().temperature(), (int) ThermalModel.TRIP_AT);
        }
    }

    /* The power and temperature sections from {@code row} down: each a header, a line of figures and a graph. */
    private void layoutPower(final int px, final int top, final int pw, final boolean ready) {
        readings.tick();
        final MachineReadingsPayload now = readings.latest();
        final boolean hot = ready && now != null && now.hasTemperature();
        for (final var c : List.of(powerHeader, periodMenu, drawLabel, drawValue)) {
            c.setVisible(ready);
        }
        for (final var c : List.of(temperatureHeader, cpuLabel, temperatureValue)) {
            c.setVisible(hot);
        }
        int row = top;
        powerHeader.setBounds(px, row, pw / 2, 8);
        periodMenu.setBounds(px + pw - PERIOD_MENU_W, row - 2, PERIOD_MENU_W, 11);
        row += 12;
        drawLabel.setBounds(px, row, pw / 3, 8);
        drawValue.setBounds(px + pw / 3, row, pw - pw / 3, 8);
        row += 10;
        setGraph(drawGraph, ready, px, row, pw);
        row += GRAPH_H + 6;
        temperatureHeader.setBounds(px, row, pw / 2, 8);
        row += 12;
        cpuLabel.setBounds(px, row, pw / 3, 8);
        temperatureValue.setBounds(px + pw / 3, row, pw - pw / 3, 8);
        row += 10;
        setGraph(temperatureGraph, hot, px, row, pw);
    }

    private static void setGraph(final int[] rect, final boolean shown, final int x, final int y, final int w) {
        rect[0] = x;
        rect[1] = y;
        rect[2] = shown ? w : 0;
        rect[3] = GRAPH_H;
    }

    /* A small graph in the window's own colours: the line of the period's means over the field's ground. */
    private void renderGraph(final GuiGraphics g, final int[] rect, final int[] series, final int max) {
        if (rect[2] <= 0) {
            return;
        }
        final int x = rect[0];
        final int y = rect[1];
        final int w = rect[2];
        final int h = rect[3];
        g.fill(x, y, x + w, y + h, skin.fieldBg());
        Draw.outline(g, x, y, w, h, skin.edge());
        final int[] values = MachineReadingsView.filled(series);
        if (values.length < 2) {
            return;
        }
        final int span = Math.max(1, max);
        for (int px = 1; px < w - 1; px++) {
            final int value = values[Math.min(values.length - 1, (px - 1) * values.length / (w - 2))];
            final int py = y + h - 2 - Math.max(0, Math.min(h - 3, value * (h - 3) / span));
            g.fill(x + px, py, x + px + 1, py + 1, skin.accent());
        }
    }

    /* What the parts draw of the supply's rating, and what the line gives a tick: "395 W of 650 W · 22 FE/t". */
    private String drawText() {
        final MachineReadingsPayload now = readings.latest();
        return now == null ? "" : GameText.resolve(TaskManagerTexts.DRAW_OF.with(now.drawNow(), now.rating(),
                Math.round(now.lineNow() / 20.0)));
    }

    /* The processor's temperature against where it starts to slow: "68 °C of 90 °C". */
    private String temperatureText() {
        final MachineReadingsPayload now = readings.latest();
        return now == null ? "" : GameText.resolve(TaskManagerTexts.CELSIUS_OF.with(now.celsius(),
                (int) ThermalModel.THROTTLE_AT));
    }

    private static Supplier<String> word(final TextKey key) {
        return () -> GameText.resolve(key);
    }

    private void renderMemoryRow(final GuiGraphics g, final UiContext ctx, final RamUse use, final int index, final int x,
                                 final int y, final int w, final int h, final boolean hovered, final boolean selected) {
        final Font font = ctx.font();
        // What it holds now, which is what a person watching this list is watching for.
        final String amount = RamLedger.heldLabel(use.heldBytes());
        final int amountW = font.width(amount);
        // A window is listed by the name the desktop gives its program, not by the key the machine keeps it under.
        final RamLedger.Kind kind = RamLedger.Kind.find(use.kind());
        final String label = kind == RamLedger.Kind.WINDOW ? ActiveDesktop.windowName(use.label())
                : kind == RamLedger.Kind.GRAPHICS ? GameText.resolve(SystemMonitorTexts.SHARED_GRAPHICS) : use.label();
        // A program with part of itself in swap says how much, and how fast that leaves it running.
        final String swapped = use.inSwapMb() <= 0 ? "" : GameText.resolve(SystemMonitorTexts.IN_SWAP.with(
                use.inSwapMb(), use.speedPercent()));
        final int swappedW = font.width(swapped);
        final String name = font.plainSubstrByWidth(label, w - amountW - swappedW - 12);
        Draw.text(g, font, name, x, y + 1, ctx.skin().text());
        if (!swapped.isEmpty()) {
            Draw.text(g, font, swapped, x + w - amountW - swappedW - 8, y + 1, ctx.skin().accent());
        }
        Draw.text(g, font, amount, x + w - amountW, y + 1, ctx.skin().dim());
    }

    private void renderDiskRow(final GuiGraphics g, final UiContext ctx, final DiskUse disk, final int index, final int x,
                               final int y, final int w, final int h, final boolean hovered, final boolean selected) {
        final Font font = ctx.font();
        final String tag = GameText.resolve(disk.system() ? SystemMonitorTexts.SYSTEM_DISK.with(disk.label())
                : disk.label());
        Draw.text(g, font, tag, x, y, ctx.skin().text());
        final long cap = Math.max(1L, disk.capMb());
        final double frac = Math.min(1.0, (double) disk.usedMb() / cap);
        final String usage = DiskSpec.sizeLabel(disk.usedMb())
                + " / " + DiskSpec.sizeLabel(disk.capMb());
        Draw.text(g, font, usage, x + w - font.width(usage), y, ctx.skin().dim());
        final int barY = y + 10;
        g.fill(x, barY, x + w, barY + BAR_H, ctx.skin().fieldBg());
        g.fill(x, barY, x + (int) (w * frac), barY + BAR_H, usageColor(frac));
        Draw.outline(g, x, barY, w, BAR_H, ctx.skin().edge());
    }

    private static int usageColor(final double frac) {
        final Colours c = PALETTE.get();
        if (frac >= 0.9) {
            return c.critical();
        }
        if (frac >= 0.7) {
            return c.warning();
        }
        return c.normal();
    }

    private static String cpuClock(final int mhz) {
        if (mhz <= 0) {
            return "-";
        }
        return mhz >= 1000 ? String.format(Locale.ROOT, "%.2f GHz", mhz / 1000.0) : mhz + " MHz";
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        return root.mouseScrolled(lastMouseX, lastMouseY, delta);
    }

    /** A disk usage bar's colour under, at, and over the threshold that flags it. */
    private record Colours(int normal, int warning, int critical) {
    }
}
