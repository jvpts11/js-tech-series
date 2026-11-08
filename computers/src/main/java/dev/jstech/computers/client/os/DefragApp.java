/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.DefragLayout;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.os.defrag.DefragLook;
import dev.jstech.computers.os.defrag.DefragMap;
import dev.jstech.computers.os.defrag.DefragState;
import dev.jstech.computers.program.cli.Stamps;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Defrag of the Frames desktops (computers/docs/design/storage.md), in each edition's own form: Frames 95's Disk
 * Defragmenter with its map of coloured blocks, XP's with its volumes and its two bars, 7's with its schedule and the
 * state of each disk, and Optimize Drives on 10 and 11. Corrupted items are drawn in yellow and bad sectors in black
 * everywhere, and each says what the last pass gave back and lost.
 *
 * <p>Nothing of the machine is kept here but what it last said: the window asks it for its Defrag's state as a file
 * is asked for, and asks again a few times a second while a pass runs.
 */
@PaletteHolder
public final class DefragApp implements IDesktopApp, CodeFileReplies.IReader {

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/defrag",
            new Colours(0xFF000080, 0xFF00FFFF, 0xFFFFFFFF, 0xFFFF0000, 0xFFFF00FF, 0xFFF0C000, 0xFF000000,
                    0xFFE9372E, 0xFF2B54E4, 0xFF29A62F, 0xFF7F9DB9, 0xFFECE9D8, 0xFF1E3287, 0xFFC42B1C, 0xFFCCE8FF,
                    0xFF808080, 0xFF000000, 0xFFC0C0C0));

    /* How often the window asks again, in milliseconds: often while a pass runs, now and then otherwise. */
    private static final long ASK_RUNNING_MS = 400L;
    private static final long ASK_IDLE_MS = 2_000L;

    private final BlockPos host;
    private final DefragLook formOf;
    private final Panel root = new Panel();
    private final Button[] buttons = new Button[5];
    private final Button close;
    private DefragState state;
    private int selected;
    private long askedAt;
    private boolean details = true;
    private boolean legend;
    private boolean report;
    private boolean closing;
    /* Where the content stood when last drawn, which a click on the list of disks is read against. */
    private int top;
    @Nullable
    private String analysis;
    private OsSkin skin = OsSkin.fallback();

    public DefragApp(final BlockPos host, final ResourceLocation desktopId) {
        this.host = host;
        this.formOf = lookOf(desktopId);
        this.state = new DefragState(this.formOf, List.of(), null, null, true);
        for (int i = 0; i < this.buttons.length; i++) {
            final int which = i;
            this.buttons[i] = this.root.add(new Button("", () -> press(which)));
        }
        this.close = this.root.add(new Button(GameText.resolve(DefragTexts.CLOSE), () -> this.closing = true));
        ask(DefragState.LOOK);
    }

    @Override
    public String title() {
        if (this.formOf == DefragLook.FRAMES_95) {
            return GameText.resolve((running() ? DefragTexts.DEFRAGMENTING_DRIVE : DefragTexts.READY_DRIVE)
                    .with(letter(this.selected)));
        }
        return ProgramClient.nameOf("defrag");
    }

    @Override
    public int defaultWidth() {
        return DefragLayout.W;
    }

    @Override
    public int defaultHeight() {
        return DefragLayout.height(this.formOf);
    }

    @Override
    public int minWidth() {
        return DefragLayout.W;
    }

    @Override
    public int minHeight() {
        return DefragLayout.height(this.formOf);
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public boolean wantsToClose() {
        return this.closing;
    }

    @Override
    public void onClosed() {
        CodeFileReplies.forget(this);
    }

    @Override
    public void onContent(final String target, final String content, final boolean exists) {
        if (!DefragState.names(target)) {
            return;
        }
        this.state = DefragState.read(content);
        if (this.selected >= this.state.disks().size()) {
            this.selected = 0;
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.top = y;
        final long now = Util.getMillis();
        if (now - this.askedAt >= (running() ? ASK_RUNNING_MS : ASK_IDLE_MS)) {
            ask(DefragState.LOOK);
        }
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        for (final Button button : this.buttons) {
            button.setVisible(false);
        }
        this.close.setVisible(false);
        switch (this.formOf) {
            case FRAMES_95 -> render95(g, font, x, y);
            case FRAMES_XP -> renderXp(g, font, x, y, width);
            case FRAMES_7 -> render7(g, font, x, y, height);
            default -> render10(g, font, x, y, height);
        }
        this.root.render(g, new UiContext(this.skin, font, mouseX, mouseY, partialTick));
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (this.root.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        // A click on a row of the list of disks picks that disk.
        final int listY = switch (this.formOf) {
            case FRAMES_XP -> DefragLayout.XP_LIST_Y;
            case FRAMES_7 -> DefragLayout.F7_LIST_Y;
            case FRAMES_95 -> -1;
            default -> DefragLayout.F10_LIST_Y;
        };
        final int row = listY < 0 ? -1
                : (int) Math.floor((mouseY - this.top - listY - DefragLayout.LIST_ROW - 1) / DefragLayout.LIST_ROW);
        if (row >= 0 && row < Math.min(DefragLayout.LIST_ROWS, this.state.disks().size())) {
            this.selected = row;
            this.analysis = null;
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        this.root.mouseReleased(mouseX, mouseY, button);
    }

    /* The buttons, which each edition puts to its own words; what each does is the same on all of them. */
    private void press(final int which) {
        final String action = switch (this.formOf) {
            case FRAMES_95 -> new String[] {running() ? DefragState.STOP : DefragState.START,
                    running() ? DefragState.PAUSE : "drive", "legend", "details"}[Math.min(3, which)];
            case FRAMES_XP -> new String[] {"analyze", DefragState.START, DefragState.PAUSE, DefragState.STOP,
                    "report"}[which];
            case FRAMES_7 -> which == 0 ? "analyze" : which == 1 ? (running() ? DefragState.STOP : DefragState.START)
                    : DefragState.SCHEDULE;
            default -> which == 0 ? "analyze" : which == 1 ? (running() ? DefragState.STOP : DefragState.START)
                    : DefragState.SCHEDULE;
        };
        switch (action) {
            case "drive" -> this.selected = this.state.disks().isEmpty() ? 0
                    : (this.selected + 1) % this.state.disks().size();
            case "legend" -> this.legend = !this.legend;
            case "details" -> this.details = !this.details;
            case "report" -> this.report = !this.report;
            case "analyze" -> {
                final DefragState.Disk disk = this.state.disk(this.selected);
                this.analysis = disk == null ? null : GameText.resolve(DefragTexts.ANALYSIS.with(label(disk),
                        disk.corrupted(), disk.bad()));
                ask(DefragState.LOOK);
            }
            default -> ask(action);
        }
    }

    private void render95(final GuiGraphics g, final Font font, final int x, final int y) {
        final Colours c = PALETTE.get();
        final DefragState.Disk disk = this.state.disk(this.selected);
        final int mapW = DefragLayout.MAP_COLS * DefragLayout.MAP_CELL;
        final int mapH = DefragLayout.MAP_ROWS * DefragLayout.MAP_CELL;
        final int mx = x + DefragLayout.MAP_X;
        final int my = y + DefragLayout.MAP_Y;
        Draw.outline(g, mx - 1, my - 1, mapW + 2, mapH + 2, c.frame());
        g.fill(mx, my, mx + mapW, my + mapH, c.free());
        if (disk != null && this.details) {
            final DefragMap.Cell[] cells = DefragMap.cells(disk, this.state.running(),
                    DefragLayout.MAP_COLS * DefragLayout.MAP_ROWS);
            for (int i = 0; i < cells.length; i++) {
                final int cx = mx + i % DefragLayout.MAP_COLS * DefragLayout.MAP_CELL;
                final int cy = my + i / DefragLayout.MAP_COLS * DefragLayout.MAP_CELL;
                g.fill(cx, cy, cx + DefragLayout.MAP_CELL - 1, cy + DefragLayout.MAP_CELL - 1, colour95(cells[i], c));
            }
        }
        if (this.legend) {
            legendBox(g, font, mx + 6, my + 6, List.of(c.optimized(), c.unoptimized(), c.free(), c.read(),
                    c.corrupt(), c.bad()), List.of(DefragTexts.OPTIMIZED, DefragTexts.UNOPTIMIZED,
                    DefragTexts.FREE_SPACE, DefragTexts.READING, DefragTexts.CORRUPTED, DefragTexts.BAD_SECTORS));
        }
        final DefragState.Run run = runningHere();
        text(g, font, (run != null ? DefragTexts.DEFRAGMENTING_DRIVE : DefragTexts.READY_DRIVE)
                .with(letter(this.selected)), x + DefragLayout.MAP_X, y + DefragLayout.F95_LINE_Y);
        final int bx = x + DefragLayout.MAP_X;
        final int by = y + DefragLayout.F95_BAR_Y;
        g.fill(bx, by, bx + DefragLayout.F95_BAR_W, by + 10, c.free());
        Draw.outline(g, bx, by, DefragLayout.F95_BAR_W, 10, c.frame());
        final int percent = run == null ? 0 : run.percent();
        for (int k = 0; k < (DefragLayout.F95_BAR_W - 4) / 6 * percent / 100; k++) {
            g.fill(bx + 2 + k * 6, by + 2, bx + 6 + k * 6, by + 8, c.optimized());
        }
        text(g, font, DefragTexts.COMPLETE.with(percent), bx, y + DefragLayout.F95_PERCENT_Y);
        counts(g, font, DefragTexts.COUNTS, bx, y + DefragLayout.F95_COUNTS_Y);
        final List<TextKey> labels = List.of(run != null ? DefragTexts.STOP : DefragTexts.START,
                run != null ? (run.paused() ? DefragTexts.RESUME : DefragTexts.PAUSE) : DefragTexts.DRIVE,
                DefragTexts.LEGEND, this.details ? DefragTexts.HIDE_DETAILS : DefragTexts.SHOW_DETAILS);
        for (int i = 0; i < labels.size(); i++) {
            final String label = labels.get(i) == DefragTexts.DRIVE ? GameText.resolve(DefragTexts.DRIVE
                    .with(letter((this.selected + 1) % Math.max(1, this.state.disks().size()))))
                    : GameText.resolve(labels.get(i));
            show(this.buttons[i], label, x + DefragLayout.F95_BUTTON_X[i], y + DefragLayout.F95_BUTTON_Y[i],
                    DefragLayout.F95_BUTTON_W);
        }
    }

    private void renderXp(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        final Colours c = PALETTE.get();
        text(g, font, DefragTexts.MENU, x + 6, y + DefragLayout.XP_MENU_Y);
        list(g, font, x + 2, y + DefragLayout.XP_LIST_Y, width - 4, DefragLayout.XP_LIST_H, DefragLayout.XP_COLUMNS,
                List.of(DefragTexts.VOLUME, DefragTexts.SESSION, DefragTexts.FILE_SYSTEM, DefragTexts.CAPACITY,
                        DefragTexts.FREE), disk -> {
                    final DefragState.Run run = this.state.running();
                    final boolean here = run != null && run.disk() == disk.index();
                    return new String[] {"(" + disk.drive() + ")",
                            here ? GameText.resolve(run.paused() ? DefragTexts.PAUSED : DefragTexts.DEFRAGMENTING)
                                    : disk.dead() ? GameText.resolve(DefragTexts.FAILED) : "",
                            disk.fileSystem(), size(disk.capacityMb()), size(disk.freeMb())};
                });
        final DefragState.Disk disk = this.state.disk(this.selected);
        text(g, font, DefragTexts.BEFORE, x + 2, y + DefragLayout.XP_BEFORE_Y);
        usageBar(g, x + 2, y + DefragLayout.XP_BEFORE_BAR_Y, width - 4, disk, false, c);
        text(g, font, DefragTexts.AFTER, x + 2, y + DefragLayout.XP_AFTER_Y);
        usageBar(g, x + 2, y + DefragLayout.XP_AFTER_BAR_Y, width - 4, disk, true, c);
        final DefragState.Run run = runningHere();
        final List<TextKey> labels = List.of(DefragTexts.ANALYZE, DefragTexts.DEFRAGMENT,
                run != null && run.paused() ? DefragTexts.RESUME : DefragTexts.PAUSE, DefragTexts.STOP,
                DefragTexts.VIEW_REPORT);
        for (int i = 0; i < labels.size(); i++) {
            show(this.buttons[i], GameText.resolve(labels.get(i)), x + DefragLayout.XP_BUTTON_X[i],
                    y + DefragLayout.XP_BUTTONS_Y, DefragLayout.XP_BUTTON_W[i]);
        }
        this.buttons[1].setEnabled(this.state.running() == null);
        this.buttons[2].setEnabled(run != null);
        this.buttons[3].setEnabled(run != null);
        legendBox(g, font, x + 2, y + DefragLayout.XP_LEGEND_Y, List.of(c.fragmented(), c.contiguous(),
                c.unmovable(), c.free(), c.corrupt(), c.bad()), List.of(DefragTexts.FRAGMENTED,
                DefragTexts.CONTIGUOUS, DefragTexts.UNMOVABLE, DefragTexts.FREE_SPACE, DefragTexts.CORRUPTED_SHORT,
                DefragTexts.BAD_SECTORS));
        reportLines(g, font, x + 2, y + DefragLayout.XP_REPORT_Y, this.skin.text(), true);
        final int sy = y + DefragLayout.XP_STATUS_Y;
        g.fill(x, sy, x + width, sy + DefragLayout.BUTTON_H, c.statusBar());
        Draw.outline(g, x, sy, width, DefragLayout.BUTTON_H, c.listFrame());
        text(g, font, run != null ? DefragTexts.STATUS_RUNNING.with("(" + letter(run.disk()) + ")", run.percent())
                : DefragTexts.STATUS_READY.with("(" + letter(this.selected) + ")"), x + 4, sy + 3);
    }

    private void render7(final GuiGraphics g, final Font font, final int x, final int y, final int height) {
        final Colours c = PALETTE.get();
        text(g, font, DefragTexts.INTRO_7_A, x + 6, y + 4);
        text(g, font, DefragTexts.INTRO_7_B, x + 6, y + 4 + DefragLayout.ROW);
        Draw.text(g, font, GameText.resolve(DefragTexts.SCHEDULE), x + 6, y + DefragLayout.F7_SCHEDULE_Y, c.heading());
        text(g, font, this.state.schedule() ? DefragTexts.SCHEDULE_ON : DefragTexts.SCHEDULE_OFF, x + 14,
                y + DefragLayout.F7_SCHEDULE_Y + 12);
        if (this.state.schedule()) {
            text(g, font, DefragTexts.RUN_AT, x + 14, y + DefragLayout.F7_SCHEDULE_Y + 22);
        }
        show(this.buttons[2], GameText.resolve(DefragTexts.CONFIGURE), x + DefragLayout.F7_SCHEDULE_BUTTON_X,
                y + DefragLayout.F7_SCHEDULE_BUTTON_Y, DefragLayout.F7_SCHEDULE_BUTTON_W);
        Draw.text(g, font, GameText.resolve(DefragTexts.CURRENT), x + 6, y + DefragLayout.F7_STATUS_Y, c.heading());
        list(g, font, x + DefragLayout.LIST_X, y + DefragLayout.F7_LIST_Y, DefragLayout.LIST_W, DefragLayout.LIST_H,
                DefragLayout.F7_COLUMNS, List.of(DefragTexts.DISK, DefragTexts.LAST_RUN, DefragTexts.PROGRESS),
                disk -> {
                    final DefragState.Run run = this.state.running();
                    final boolean here = run != null && run.disk() == disk.index();
                    return new String[] {label(disk), here ? GameText.resolve(DefragTexts.RUNNING) : lastRun(disk),
                            here ? GameText.resolve(DefragTexts.PASS.with(run.percent())) : condition(disk)};
                });
        reportLines(g, font, x + DefragLayout.LIST_X, y + DefragLayout.F7_RESULT_Y, c.error(), false);
        show(this.buttons[0], GameText.resolve(DefragTexts.ANALYZE_DISK), x + DefragLayout.F7_BUTTON_X[0],
                y + DefragLayout.F7_BUTTONS_Y, DefragLayout.F7_BUTTON_W[0]);
        show(this.buttons[1], GameText.resolve(running() ? DefragTexts.STOP_OPERATION : DefragTexts.DEFRAGMENT_DISK),
                x + DefragLayout.F7_BUTTON_X[1], y + DefragLayout.F7_BUTTONS_Y, DefragLayout.F7_BUTTON_W[1]);
        showClose(x, y, height);
    }

    private void render10(final GuiGraphics g, final Font font, final int x, final int y, final int height) {
        final Colours c = PALETTE.get();
        text(g, font, DefragTexts.INTRO_10_A, x + 6, y + 4);
        text(g, font, DefragTexts.INTRO_10_B, x + 6, y + 4 + DefragLayout.ROW);
        text(g, font, DefragTexts.INTRO_10_C, x + 6, y + 4 + 2 * DefragLayout.ROW);
        text(g, font, DefragTexts.STATUS, x + 6, y + DefragLayout.F10_STATUS_Y);
        list(g, font, x + DefragLayout.LIST_X, y + DefragLayout.F10_LIST_Y, DefragLayout.LIST_W, DefragLayout.LIST_H,
                DefragLayout.F10_COLUMNS, List.of(DefragTexts.DRIVE_COLUMN, DefragTexts.MEDIA_TYPE,
                        DefragTexts.LAST_ANALYZED, DefragTexts.CURRENT_STATUS), disk -> {
                    final DefragState.Run run = this.state.running();
                    final boolean here = run != null && run.disk() == disk.index();
                    return new String[] {label(disk), GameText.resolve(disk.tier() == 0 ? DefragTexts.HARD_DISK
                            : DefragTexts.SOLID_STATE), here ? GameText.resolve(DefragTexts.RUNNING) : lastRun(disk),
                            here ? GameText.resolve(DefragTexts.REPAIRING_PERCENT.with(run.percent()))
                                    : GameText.resolve(disk.dead() ? DefragTexts.FAILED
                                    : disk.corrupted() > 0L || disk.bad() > 0 ? DefragTexts.NEEDS_REPAIR
                                    : DefragTexts.OK)};
                });
        reportLines(g, font, x + DefragLayout.LIST_X, y + DefragLayout.F10_RESULT_Y, c.error(), false);
        show(this.buttons[0], GameText.resolve(DefragTexts.ANALYZE), x + DefragLayout.F10_BUTTON_X[0],
                y + DefragLayout.F10_BUTTONS_Y, DefragLayout.F10_BUTTON_W[0]);
        show(this.buttons[1], GameText.resolve(running() ? DefragTexts.STOP : DefragTexts.OPTIMIZE),
                x + DefragLayout.F10_BUTTON_X[1], y + DefragLayout.F10_BUTTONS_Y, DefragLayout.F10_BUTTON_W[1]);
        text(g, font, DefragTexts.SCHEDULED, x + 6, y + DefragLayout.F10_SCHEDULE_Y);
        show(this.buttons[2], GameText.resolve(DefragTexts.CHANGE_SETTINGS), x + DefragLayout.F10_SCHEDULE_BUTTON_X,
                y + DefragLayout.F10_SCHEDULE_Y - 3, DefragLayout.F10_SCHEDULE_BUTTON_W);
        text(g, font, this.state.schedule() ? DefragTexts.ON : DefragTexts.OFF, x + 6,
                y + DefragLayout.F10_SCHEDULE_Y + 12);
        if (this.state.schedule()) {
            text(g, font, DefragTexts.AUTOMATIC, x + 6, y + DefragLayout.F10_SCHEDULE_Y + 22);
            text(g, font, DefragTexts.FREQUENCY, x + 6, y + DefragLayout.F10_SCHEDULE_Y + 32);
        }
        showClose(x, y, height);
    }

    /* A list of the disks with a header row, the picked disk's row lit; {@code cells} says what each column shows. */
    private void list(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                      final int[] columns, final List<TextKey> headers,
                      final Function<DefragState.Disk, String[]> cells) {
        final Colours c = PALETTE.get();
        g.fill(x, y, x + w, y + h, c.free());
        Draw.outline(g, x, y, w, h, c.listFrame());
        g.fill(x + 1, y + 1, x + w - 1, y + DefragLayout.LIST_ROW + 1, c.statusBar());
        for (int i = 0; i < headers.size(); i++) {
            Draw.text(g, font, GameText.resolve(headers.get(i)), x + columns[i] - 4 + 4, y + 2, c.ink());
        }
        final List<DefragState.Disk> disks = this.state.disks();
        for (int r = 0; r < Math.min(DefragLayout.LIST_ROWS, disks.size()); r++) {
            final int ry = y + 2 + DefragLayout.LIST_ROW * (r + 1);
            if (r == this.selected) {
                g.fill(x + 1, ry - 1, x + w - 1, ry + DefragLayout.LIST_ROW - 1, c.selection());
            }
            final String[] row = cells.apply(disks.get(r));
            for (int i = 0; i < row.length && i < columns.length; i++) {
                final int limit = (i + 1 < columns.length ? columns[i + 1] : w) - columns[i] - 3;
                Draw.text(g, font, clip(font, row[i], limit), x + columns[i], ry, c.ink());
            }
        }
    }

    /*
     * The two bars of XP: before, the disk as it stands, its data in stripes of what is still to put back together and
     * what is already together, the corrupted items in yellow and the bad sectors in black; after, the data together at
     * the front, the system's unmovable part, and what bad sectors are left after a pass.
     */
    private void usageBar(final GuiGraphics g, final int x, final int y, final int w,
                          @Nullable final DefragState.Disk disk, final boolean after, final Colours c) {
        g.fill(x, y, x + w, y + DefragLayout.XP_BAR_H, c.free());
        if (disk != null) {
            final double used = disk.used() / 1000.0;
            final double corrupted = used * disk.damage() / 1000.0;
            final double bad = disk.bad() / 1000.0;
            if (after) {
                final int dataW = (int) Math.round(w * (used - bad));
                g.fill(x, y, x + Math.max(0, dataW), y + DefragLayout.XP_BAR_H, c.contiguous());
                g.fill(x + Math.max(0, dataW), y, x + Math.max(0, dataW) + 3, y + DefragLayout.XP_BAR_H,
                        c.unmovable());
                final int badW = (int) Math.round(w * (bad - bad / 4.0));
                g.fill(x + w - badW, y, x + w, y + DefragLayout.XP_BAR_H, c.bad());
            } else {
                final DefragMap.Cell[] cells = DefragMap.cells(disk, null, w / 2);
                for (int i = 0; i < cells.length; i++) {
                    final int colour = switch (cells[i]) {
                        case USED -> i % 3 == 0 ? c.contiguous() : c.fragmented();
                        case CORRUPTED -> c.corrupt();
                        case BAD -> c.bad();
                        case UNMOVABLE -> c.unmovable();
                        default -> c.free();
                    };
                    g.fill(x + i * 2, y, x + i * 2 + 2, y + DefragLayout.XP_BAR_H, colour);
                }
                if (corrupted <= 0.0 && bad <= 0.0 && used <= 0.0) {
                    g.fill(x, y, x + w, y + DefragLayout.XP_BAR_H, c.free());
                }
            }
        }
        Draw.outline(g, x, y, w, DefragLayout.XP_BAR_H, c.listFrame());
    }

    /* Swatches with their names, in three columns of two. */
    private void legendBox(final GuiGraphics g, final Font font, final int x, final int y, final List<Integer> colours,
                           final List<TextKey> names) {
        final Colours c = PALETTE.get();
        final boolean overMap = this.formOf == DefragLook.FRAMES_95;
        if (overMap) {
            g.fill(x - 3, y - 3, x + 150, y + names.size() * DefragLayout.ROW + 2, c.grey());
            Draw.outline(g, x - 3, y - 3, 153, names.size() * DefragLayout.ROW + 5, c.ink());
        }
        for (int i = 0; i < names.size(); i++) {
            final int lx = overMap ? x : x + DefragLayout.XP_LEGEND_X[i % 3];
            final int ly = overMap ? y + i * DefragLayout.ROW : y + i / 3 * 11;
            g.fill(lx, ly, lx + 7, ly + 7, colours.get(i));
            Draw.outline(g, lx, ly, 7, 7, c.ink());
            Draw.text(g, font, GameText.resolve(names.get(i)), lx + 10, ly, c.ink());
        }
    }

    /*
     * What the last pass did, or what an analysis found, in two lines: in the window's ink on XP, in red on 7, 10 and
     * 11 as their result lines are.
     */
    private void reportLines(final GuiGraphics g, final Font font, final int x, final int y, final int colour,
                             final boolean xp) {
        if (this.analysis != null) {
            Draw.text(g, font, this.analysis, x, y, colour);
            return;
        }
        final DefragState.Run run = runningHere();
        final DefragState.Result last = this.state.last();
        if (xp && this.report) {
            final DefragState.Disk disk = this.state.disk(this.selected);
            if (disk != null) {
                Draw.text(g, font, GameText.resolve(DefragTexts.REPORT.with(label(disk), disk.damage() / 10,
                        disk.bad() / 10)), x, y, colour);
            }
            return;
        }
        if (run != null) {
            Draw.text(g, font, GameText.resolve((xp ? DefragTexts.REPAIRING : DefragTexts.RESULT).with(
                    "(" + letter(run.disk()) + ")", run.found(), run.recovered(), run.lost())), x, y, colour);
            if (xp) {
                Draw.text(g, font, GameText.resolve(DefragTexts.REPAIRED.with(run.recovered(), run.lost(),
                        run.badBefore(), run.badBefore())), x, y + DefragLayout.ROW, colour);
            }
            return;
        }
        if (last == null) {
            return;
        }
        final String drive = "(" + letter(last.disk()) + ")";
        if (xp) {
            Draw.text(g, font, GameText.resolve(DefragTexts.REPAIRING.with(drive, last.found())), x, y, colour);
            Draw.text(g, font, GameText.resolve(DefragTexts.REPAIRED.with(last.recovered(), last.lost(),
                    last.badBefore(), last.badAfter())), x, y + DefragLayout.ROW, colour);
            return;
        }
        Draw.text(g, font, GameText.resolve(DefragTexts.RESULT.with(drive, last.found(), last.recovered(),
                last.lost())), x, y, colour);
        if (last.badBefore() > 0) {
            Draw.text(g, font, GameText.resolve(DefragTexts.RESULT_BAD.with(last.badBefore(), last.badAfter())), x,
                    y + DefragLayout.ROW, colour);
        }
    }

    /* The corrupted items found, recovered and lost, of the pass under way or else of the last one. */
    private void counts(final GuiGraphics g, final Font font, final TextKey words, final int x, final int y) {
        final DefragState.Run run = runningHere();
        final DefragState.Result last = this.state.last();
        if (run != null) {
            text(g, font, words.with(run.found(), run.recovered(), run.lost()), x, y);
        } else if (last != null && last.disk() == this.selected) {
            text(g, font, words.with(last.found(), last.recovered(), last.lost()), x, y);
        }
    }

    private void showClose(final int x, final int y, final int height) {
        show(this.close, GameText.resolve(DefragTexts.CLOSE), x + DefragLayout.CLOSE_X,
                y + height - DefragLayout.BUTTON_H - 4, DefragLayout.CLOSE_W);
    }

    private static void show(final Button button, final String label, final int x, final int y, final int w) {
        button.setLabel(label);
        button.setBounds(x, y, w, DefragLayout.BUTTON_H);
        button.setVisible(true);
    }

    private void text(final GuiGraphics g, final Font font, final Text words, final int x, final int y) {
        Draw.text(g, font, GameText.resolve(words), x, y, this.skin.text());
    }

    private void text(final GuiGraphics g, final Font font, final TextKey words, final int x, final int y) {
        text(g, font, words.text(), x, y);
    }

    private void ask(final String action) {
        this.askedAt = Util.getMillis();
        final String path = DefragState.path(action, this.selected);
        CodeFileReplies.expectContent(this, path);
        PacketDistributor.sendToServer(new RequestFileContentPayload(this.host, path));
    }

    private boolean running() {
        return this.state.running() != null;
    }

    /* The pass under way, when it is on the disk picked. */
    @Nullable
    private DefragState.Run runningHere() {
        final DefragState.Run run = this.state.running();
        return run != null && run.disk() == this.selected ? run : null;
    }

    /* A disk's letter, without its colon, as the windows name a drive. */
    private String letter(final int index) {
        return this.formOf.drive(index).replace(":", "");
    }

    /* A disk as 7 and 10 list it: the system disk by its letter, the others as storage. */
    private String label(final DefragState.Disk disk) {
        final String letter = "(" + disk.drive() + ")";
        return disk.index() == 0 ? letter : GameText.resolve(DefragTexts.STORAGE_DRIVE.with(letter));
    }

    private static String lastRun(final DefragState.Disk disk) {
        return disk.lastRun() < 0L ? GameText.resolve(DefragTexts.NEVER) : Stamps.of(disk.lastRun());
    }

    /* How a disk not being repaired stands, in 7's progress column. */
    private static String condition(final DefragState.Disk disk) {
        if (disk.dead()) {
            return GameText.resolve(DefragTexts.FAILED);
        }
        return disk.corrupted() > 0L ? GameText.resolve(DefragTexts.HAS_CORRUPTED.with(disk.corrupted()))
                : GameText.resolve(DefragTexts.NOT_FRAGMENTED);
    }

    private static int colour95(final DefragMap.Cell cell, final Colours c) {
        return switch (cell) {
            case USED -> c.unoptimized();
            case FREE -> c.free();
            case CORRUPTED -> c.corrupt();
            case BAD -> c.bad();
            case DONE -> c.optimized();
            case READING -> c.read();
            case UNMOVABLE -> c.fixed();
        };
    }

    private static String size(final long megabytes) {
        return megabytes >= 1024L ? String.format(Locale.ROOT, "%.1f GB", megabytes / 1024.0)
                : megabytes + " MB";
    }

    private static String clip(final Font font, final String text, final int width) {
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(0, width - 4)) + "..";
    }

    /** The edition a desktop's id names; the newest one for a desktop that is no Frames. */
    private static DefragLook lookOf(final ResourceLocation desktopId) {
        final String path = desktopId == null ? "" : desktopId.getPath();
        return switch (path) {
            case "frames_95" -> DefragLook.FRAMES_95;
            case "frames_xp" -> DefragLook.FRAMES_XP;
            case "frames_7" -> DefragLook.FRAMES_7;
            case "frames_10" -> DefragLook.FRAMES_10;
            default -> DefragLook.FRAMES_11;
        };
    }

    /**
     * The Defrag's colours, {@code jsc:app/defrag}: Frames 95's map (optimized navy, still to optimize cyan, free
     * white, read red, the system's own magenta), the corrupted yellow and the bad black every edition shares, XP's
     * bars (fragmented red, contiguous blue, unmovable green), the list frame and header, 7's headings, the red of the
     * result lines, the picked row, the map's frame, the ink, and 95's grey.
     */
    private record Colours(int optimized, int unoptimized, int free, int read, int fixed, int corrupt, int bad,
                           int fragmented, int contiguous, int unmovable, int listFrame, int statusBar, int heading,
                           int error, int selection, int frame, int ink, int grey) {
    }
}
