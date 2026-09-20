/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.DownloadToMediaPayload;
import dev.jstech.computers.operation.payload.LoadFromMediaPayload;
import dev.jstech.computers.operation.payload.PatternStudioEditPayload;
import dev.jstech.computers.operation.payload.PatternStudioStatePayload;
import dev.jstech.computers.operation.payload.RemoveRomCraftPayload;
import dev.jstech.computers.operation.payload.RequestCraftManagerPayload;
import dev.jstech.computers.operation.payload.RequestPatternStudioPayload;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Patterns heading: teaching this machine a recipe, in the network system's own shape.
 *
 * <p>A pattern is born at a Pattern Encoder and nowhere else. The encoder writes the {@code .craft} onto
 * the medium in its bay, the medium is carried to a drive linked to a Crafting Computer, and it is loaded
 * from there into the machine's Recipe ROM, which is what the network then offers as a craft. None of that
 * is changed here, and nothing here encodes a pattern by itself.
 *
 * <p>What was missing was the loading. The two programs that do it are declared for desktops only, so a
 * Crafting Computer running a network system could not put a single pattern into its own ROM: the one
 * player this system exists for could not autocraft with recipes of their own. This heading is those two
 * programs' work with their requirements intact, done by the space rather than by a window, since a
 * machine with no windows runs no graphical programs.
 */
final class PatternsTerminalTab extends AbstractTerminalTab {

    private static final int GRID_X = ComputerTerminalLayout.GRID_X;
    private static final int PANE_X = ComputerTerminalLayout.PANE_X;
    private static final int PANE_W = ComputerTerminalLayout.PANE_W;

    /** The draft: three by three, the shape every bench recipe is written in, and what it makes. */
    private static final int DRAFT_Y = 34;
    private static final int DRAFT_COLS = 3;
    private static final int RESULT_X = GRID_X + 96;
    private static final int RESULT_Y = DRAFT_Y + 18;

    /** The right-hand column: where the encoder is said to be and where a draft is sent. */
    private static final int RIGHT_X = PANE_X - 4;
    private static final int RIGHT_W = ComputerTerminalLayout.WIDTH - RIGHT_X - 6;

    /** The three places a draft can be sent, which are the Pattern Studio's own three. */
    private static final int SEND_Y = 80;
    private static final int SEND_W = 44;
    private static final int SEND_H = 12;
    private static final int SEND_GAP = 5;
    private static final int SEND_ENCODER_X = RIGHT_X;
    private static final int SEND_DISK_X = RIGHT_X + SEND_W + SEND_GAP;
    private static final int SEND_ROM_X = RIGHT_X + 2 * (SEND_W + SEND_GAP);

    /** The rule under the draft, and the two lists beneath it. */
    private static final int SPLIT_Y = 98;
    private static final int LIST_Y = 114;
    private static final int LIST_ROWS = 3;
    private static final int ROW_H = 11;
    private static final int ACTION_Y = LIST_Y + LIST_ROWS * ROW_H + 3;
    private static final int ACTION_W = 74;
    private static final int ACTION_H = 12;

    /** How many patterns a Recipe ROM holds, which is what the count beside it is measured against. */
    private static final int ROM_LIMIT = CraftManagerStatePayload.MAX_ROM_ENTRIES;

    /** The one heading open at a time, so a reply from the machine knows which view to reach. */
    @Nullable
    private static PatternsTerminalTab open;

    @Nullable
    private PatternStudioStatePayload studio;
    @Nullable
    private CraftManagerStatePayload manager;

    /** Which file on the medium and which pattern in the ROM are picked out, or -1 for none. */
    private int pickedFile = -1;
    private int pickedRom = -1;

    /** How far down each list is scrolled. */
    private int fileScroll;
    private int romScroll;

    /** How many ticks until the machine is asked again, so a medium put in shows up without a click. */
    private int refreshIn;

    PatternsTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    /** Takes the Crafting Manager's answer, when this heading is the one that asked for it. */
    static boolean accept(final CraftManagerStatePayload payload) {
        final PatternsTerminalTab showing = showing();
        if (showing == null) {
            return false;
        }
        showing.manager = payload;
        return true;
    }

    /** Takes the Pattern Studio's answer, when this heading is the one that asked for it. */
    static boolean accept(final PatternStudioStatePayload payload) {
        final PatternsTerminalTab showing = showing();
        if (showing == null) {
            return false;
        }
        showing.studio = payload;
        return true;
    }

    /**
     * The heading that is on the glass right now, or null when none is.
     *
     * <p>The screen it belongs to has to still be the one being shown: a heading whose screen has been
     * closed would otherwise go on swallowing answers that the desktop's own programs were waiting for.
     */
    @Nullable
    private static PatternsTerminalTab showing() {
        final PatternsTerminalTab tab = open;
        return tab != null && Minecraft.getInstance().screen == tab.screen ? tab : null;
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y,
                            final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        /*
         * Asking is the tick's job and not the frame's. It was asked here whenever nothing had come back
         * yet, which is several messages a tick while the answer is in flight, and a flood of them for as
         * long as the heading is open on a machine that does not answer: standing more than eight blocks
         * from the monitor is enough, because that is where the machine stops taking these.
         */
        open = this;
        drawDraft(g, x, y);
        g.fill(x + GRID_X, y + SPLIT_Y, x + ComputerTerminalLayout.WIDTH - 6, y + SPLIT_Y + 1, LINE());
        drawList(g, x + GRID_X, y, PANE_X - GRID_X - 10, mediaFiles().size(), fileScroll, pickedFile);
        drawList(g, x + PANE_X, y, PANE_W, romEntries().size(), romScroll, pickedRom);
        button(g, x + GRID_X, y + ACTION_Y, ACTION_W, !mediaFiles().isEmpty());
        button(g, x + GRID_X + ACTION_W + 6, y + ACTION_Y, ACTION_W, pickedFile >= 0);
        button(g, x + PANE_X, y + ACTION_Y, ACTION_W - 8, pickedRom >= 0);
        button(g, x + PANE_X + ACTION_W + 2, y + ACTION_Y, ACTION_W - 8, pickedRom >= 0 && hasMedium());
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        labelDraft(g);
        labelEncoder(g);
        labelLists(g);
    }

    @Override
    public void onContainerTick() {
        if (--refreshIn <= 0) {
            refreshIn = 20;
            ask();
        }
        /*
         * A list can shrink under what was picked out: load every file on a medium and the file that was
         * chosen is no longer there. What is picked out has to be something that exists, or a button
         * stands lit over a row nobody can see.
         */
        if (pickedFile >= mediaFiles().size()) {
            pickedFile = -1;
        }
        if (pickedRom >= romEntries().size()) {
            pickedRom = -1;
        }
        fileScroll = clamp(fileScroll, mediaFiles().size());
        romScroll = clamp(romScroll, romEntries().size());
    }

    @Override
    public void onRemoved() {
        if (open == this) {
            open = null;
        }
    }

    /** The wheel walks whichever list it is over, and does nothing anywhere else on the heading. */
    @Override
    public boolean onMouseScrolled(final double mouseX, final double mouseY, final double dy) {
        final double localY = mouseY - screen.top();
        if (dy == 0 || localY < LIST_Y || localY >= LIST_Y + LIST_ROWS * ROW_H) {
            return false;
        }
        final int step = (int) -Math.signum(dy);
        if (mouseX < screen.left() + PANE_X) {
            fileScroll = clamp(fileScroll + step, mediaFiles().size());
        } else {
            romScroll = clamp(romScroll + step, romEntries().size());
        }
        return true;
    }

    @Override
    public boolean onMouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button != 0) {
            return false;
        }
        final int x = screen.left();
        final int y = screen.top();
        // The draft: a click with something carried puts it in a cell, a click with nothing empties one.
        final int cell = draftCellAt(mouseX - x, mouseY - y);
        if (cell >= 0) {
            final boolean filled = studio != null && cell < studio.bench().size()
                    && !studio.bench().get(cell).stack().isEmpty();
            edit(menu.getCarried().isEmpty() && filled
                    ? PatternStudioEditPayload.BENCH_CLEAR_CELL : PatternStudioEditPayload.BENCH_SET_CELL, cell);
            return true;
        }
        if (inRect(mouseX, mouseY, x + SEND_ENCODER_X, y + SEND_Y, SEND_W, SEND_H)) {
            bar(PatternStudioEditPayload.BURN);
            return true;
        }
        if (inRect(mouseX, mouseY, x + SEND_DISK_X, y + SEND_Y, SEND_W, SEND_H)) {
            bar(PatternStudioEditPayload.SAVE_TO_DISK);
            return true;
        }
        if (inRect(mouseX, mouseY, x + SEND_ROM_X, y + SEND_Y, SEND_W, SEND_H)) {
            bar(PatternStudioEditPayload.LOAD_INTO_ROM);
            return true;
        }
        final int file = rowAt(mouseX - x, mouseY - y, GRID_X, PANE_X - GRID_X - 10, mediaFiles().size(), fileScroll);
        if (file >= 0) {
            pickedFile = file;
            return true;
        }
        final int rom = rowAt(mouseX - x, mouseY - y, PANE_X, PANE_W, romEntries().size(), romScroll);
        if (rom >= 0) {
            pickedRom = rom;
            return true;
        }
        return clickedActions(mouseX, mouseY, x, y);
    }

    // Asking the machine

    /**
     * Asks the machine for both halves of what this heading shows.
     *
     * <p>Two messages because they are two things the machine keeps: the draft and the encoder belong to
     * the workbench, and the medium and the ROM belong to the Crafting Computer. Both already answer a
     * screen that is open on that machine, so nothing new had to be built to ask them from here.
     */
    private void ask() {
        PacketDistributor.sendToServer(new RequestPatternStudioPayload(menu.hostPos(), menu.monitorPos()));
        PacketDistributor.sendToServer(new RequestCraftManagerPayload(menu.hostPos()));
    }

    private void edit(final int action, final int index) {
        PacketDistributor.sendToServer(
                PatternStudioEditPayload.at(menu.hostPos(), menu.monitorPos(), action, index));
        refreshIn = 1;
    }

    private void bar(final int action) {
        PacketDistributor.sendToServer(PatternStudioEditPayload.at(menu.hostPos(), menu.monitorPos(),
                action, 0));
        refreshIn = 1;
    }

    private boolean clickedActions(final double mouseX, final double mouseY, final int x, final int y) {
        final String volume = manager == null ? "" : manager.mediaVolumeKey();
        if (inRect(mouseX, mouseY, x + GRID_X, y + ACTION_Y, ACTION_W, ACTION_H) && !volume.isEmpty()) {
            PacketDistributor.sendToServer(new LoadFromMediaPayload(menu.hostPos(), volume, List.of(), true));
            refreshIn = 1;
            return true;
        }
        if (inRect(mouseX, mouseY, x + GRID_X + ACTION_W + 6, y + ACTION_Y, ACTION_W, ACTION_H)
                && pickedFile >= 0 && pickedFile < mediaFiles().size() && !volume.isEmpty()) {
            PacketDistributor.sendToServer(new LoadFromMediaPayload(menu.hostPos(), volume,
                    List.of(mediaFiles().get(pickedFile)), false));
            refreshIn = 1;
            return true;
        }
        if (inRect(mouseX, mouseY, x + PANE_X, y + ACTION_Y, ACTION_W - 8, ACTION_H) && pickedRomEntry() != null) {
            PacketDistributor.sendToServer(new RemoveRomCraftPayload(menu.hostPos(),
                    List.of(pickedRomEntry().index())));
            pickedRom = -1;
            refreshIn = 1;
            return true;
        }
        if (inRect(mouseX, mouseY, x + PANE_X + ACTION_W + 2, y + ACTION_Y, ACTION_W - 8, ACTION_H)
                && pickedRomEntry() != null && !volume.isEmpty()) {
            PacketDistributor.sendToServer(new DownloadToMediaPayload(menu.hostPos(), volume,
                    List.of(pickedRomEntry().index())));
            refreshIn = 1;
            return true;
        }
        /*
         * A press on a button that is not lit does nothing, but it is still a press on a button: letting it
         * through would hand it to the screen behind, which is no answer to somebody who aimed at one.
         */
        return inRect(mouseX, mouseY, x + GRID_X, y + ACTION_Y,
                ComputerTerminalLayout.WIDTH - GRID_X - 6, ACTION_H);
    }

    // Drawing

    private void drawDraft(final GuiGraphics g, final int x, final int y) {
        for (int i = 0; i < 9; i++) {
            final int sx = x + GRID_X + (i % DRAFT_COLS) * 18;
            final int sy = y + DRAFT_Y + (i / DRAFT_COLS) * 18;
            slotBg(g, sx, sy);
            if (studio != null && i < studio.bench().size()) {
                final var cellItem = studio.bench().get(i).stack();
                if (!cellItem.isEmpty()) {
                    drawDataIcon(g, StorageKey.of(cellItem), -1L, sx, sy);
                }
            }
        }
        slotBg(g, x + RESULT_X, y + RESULT_Y);
        if (studio != null && !studio.preview().isEmpty()) {
            drawDataIcon(g, StorageKey.of(studio.preview()), studio.preview().getCount(),
                    x + RESULT_X, y + RESULT_Y);
        }
        final boolean draft = studio != null && !studio.preview().isEmpty();
        button(g, x + SEND_ENCODER_X, y + SEND_Y, SEND_W, draft && encoderLinked());
        button(g, x + SEND_DISK_X, y + SEND_Y, SEND_W, draft);
        button(g, x + SEND_ROM_X, y + SEND_Y, SEND_W, draft);
    }

    private void labelDraft(final GuiGraphics g) {
        g.drawString(font(), "Draft", GRID_X, 22, DIM(), false);
        g.drawString(font(), "makes", GRID_X + 60, RESULT_Y + 5, DIM(), false);
        if (studio == null) {
            g.drawString(font(), "asking the machine ...", GRID_X, DRAFT_Y + 60, DIM(), false);
        }
    }

    private void labelEncoder(final GuiGraphics g) {
        final int px = RIGHT_X;
        g.drawString(font(), "Pattern Encoder", px, 22, DIM(), false);
        final PatternStudioStatePayload.Encoder enc = studio == null ? null : studio.encoder();
        if (enc == null || !enc.linked()) {
            g.drawString(font(), "none linked to this machine", px, 36, DIM(), false);
            g.drawString(font(), "A pattern is born at an encoder;", px, 48, DIM(), false);
            g.drawString(font(), "the ROM is where it is taught.", px, 58, DIM(), false);
        } else {
            g.drawString(font(), enc.era().isEmpty() ? "linked" : "linked  " + enc.era(), px, 36,
                    enc.error() ? RED() : GREEN(), false);
            final String media = enc.media().isEmpty() ? "no medium in its bay" : enc.media();
            g.drawString(font(), font().plainSubstrByWidth(media, RIGHT_W), px, 47, DIM(), false);
            if (!enc.status().isEmpty()) {
                g.drawString(font(), font().plainSubstrByWidth(enc.status(), RIGHT_W), px, 58,
                        enc.error() ? RED() : DIM(), false);
            }
        }
        g.drawString(font(), "Send this draft to", px, SEND_Y - 10, DIM(), false);
        final boolean draft = studio != null && !studio.preview().isEmpty();
        g.drawCenteredString(font(), "Encoder", SEND_ENCODER_X + SEND_W / 2, SEND_Y + 2,
                draft && encoderLinked() ? ACCENT() : DIM());
        g.drawCenteredString(font(), "Disk", SEND_DISK_X + SEND_W / 2, SEND_Y + 2, draft ? ACCENT() : DIM());
        g.drawCenteredString(font(), "ROM", SEND_ROM_X + SEND_W / 2, SEND_Y + 2, draft ? ACCENT() : DIM());
    }

    private void labelLists(final GuiGraphics g) {
        final List<String> files = mediaFiles();
        final String label = manager == null || manager.mediaLabel().isEmpty()
                ? "No medium in a linked drive" : "Medium: " + manager.mediaLabel();
        g.drawString(font(), font().plainSubstrByWidth(label, PANE_X - GRID_X - 12), GRID_X, SPLIT_Y + 6,
                files.isEmpty() ? DIM() : TEXT(), false);
        for (int i = 0; i < LIST_ROWS && i + fileScroll < files.size(); i++) {
            final String name = files.get(i + fileScroll);
            g.drawString(font(), font().plainSubstrByWidth(name, PANE_X - GRID_X - 16),
                    GRID_X + 4, LIST_Y + i * ROW_H + 2, i + fileScroll == pickedFile ? ACCENT() : TEXT(), false);
        }
        if (files.isEmpty()) {
            g.drawString(font(), "nothing on it to load", GRID_X + 4, LIST_Y + 2, DIM(), false);
        }
        g.drawCenteredString(font(), "Load all", GRID_X + ACTION_W / 2, ACTION_Y + 2,
                files.isEmpty() ? DIM() : ACCENT());
        g.drawCenteredString(font(), "Load one", GRID_X + ACTION_W + 6 + ACTION_W / 2, ACTION_Y + 2,
                pickedFile >= 0 ? ACCENT() : DIM());

        final List<CraftManagerStatePayload.WireRomEntry> rom = romEntries();
        g.drawString(font(), "Recipe ROM", PANE_X, SPLIT_Y + 6, TEXT(), false);
        final String count = rom.size() + " of " + ROM_LIMIT;
        g.drawString(font(), count, PANE_X + PANE_W - font().width(count), SPLIT_Y + 6, ACCENT(), false);
        for (int i = 0; i < LIST_ROWS && i + romScroll < rom.size(); i++) {
            final CraftManagerStatePayload.WireRomEntry entry = rom.get(i + romScroll);
            g.drawString(font(), font().plainSubstrByWidth(entry.name(), PANE_W - 10),
                    PANE_X + 4, LIST_Y + i * ROW_H + 2, i + romScroll == pickedRom ? ACCENT() : TEXT(), false);
        }
        if (rom.isEmpty()) {
            g.drawString(font(), "nothing taught yet", PANE_X + 4, LIST_Y + 2, DIM(), false);
        }
        g.drawCenteredString(font(), "Unload", PANE_X + (ACTION_W - 8) / 2, ACTION_Y + 2,
                pickedRom >= 0 ? ACCENT() : DIM());
        g.drawCenteredString(font(), "Download", PANE_X + ACTION_W + 2 + (ACTION_W - 8) / 2, ACTION_Y + 2,
                pickedRom >= 0 && hasMedium() ? ACCENT() : DIM());
    }

    private void drawList(final GuiGraphics g, final int x, final int y, final int w,
                          final int size, final int scroll, final int picked) {
        for (int i = 0; i < LIST_ROWS; i++) {
            final int ry = y + LIST_Y + i * ROW_H;
            final boolean on = i + scroll == picked && i + scroll < size;
            g.fill(x, ry, x + w, ry + ROW_H - 1, on ? TAB_ON() : PANEL());
            if (on) {
                g.fill(x, ry, x + 2, ry + ROW_H - 1, ACCENT());
            }
        }
    }

    private void button(final GuiGraphics g, final int bx, final int by, final int w, final boolean enabled) {
        g.fill(bx, by, bx + w, by + ACTION_H, enabled ? TAB_ON() : TRACK());
        g.fill(bx, by, bx + w, by + 1, enabled ? ACCENT() : LINE());
    }

    // Readings

    private List<String> mediaFiles() {
        return manager == null ? List.of() : manager.mediaFiles();
    }

    private List<CraftManagerStatePayload.WireRomEntry> romEntries() {
        return manager == null ? List.of() : manager.romEntries();
    }

    @Nullable
    private CraftManagerStatePayload.WireRomEntry pickedRomEntry() {
        final List<CraftManagerStatePayload.WireRomEntry> rom = romEntries();
        return pickedRom >= 0 && pickedRom < rom.size() ? rom.get(pickedRom) : null;
    }

    private boolean hasMedium() {
        return manager != null && !manager.mediaVolumeKey().isEmpty();
    }

    private boolean encoderLinked() {
        return studio != null && studio.encoder().linked();
    }

    private int clamp(final int scroll, final int size) {
        return Math.max(0, Math.min(scroll, Math.max(0, size - LIST_ROWS)));
    }

    /** Which draft cell a screen-local point is over, or -1 for none. */
    private int draftCellAt(final double localX, final double localY) {
        if (localX < GRID_X || localX >= GRID_X + DRAFT_COLS * 18
                || localY < DRAFT_Y || localY >= DRAFT_Y + DRAFT_COLS * 18) {
            return -1;
        }
        return (int) ((localY - DRAFT_Y) / 18) * DRAFT_COLS + (int) ((localX - GRID_X) / 18);
    }

    /** Which row of a list a screen-local point is over, or -1 for none. */
    private int rowAt(final double localX, final double localY, final int listX, final int listW,
                      final int size, final int scroll) {
        if (localX < listX || localX >= listX + listW || localY < LIST_Y
                || localY >= LIST_Y + LIST_ROWS * ROW_H) {
            return -1;
        }
        final int row = (int) ((localY - LIST_Y) / ROW_H) + scroll;
        return row < size ? row : -1;
    }
}
