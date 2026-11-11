/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.DiskToolLayout;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.DiskTexts;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Frames' Disk Management (computers/docs/design/storage-devices.md), XP's and that of 7 to 11: the volumes over the
 * map of the disks, each block a partition or the unallocated space, and the menu a right click opens on one. Format
 * asks first and warns; Shrink, Extend and New Simple Volume ask how much; Delete warns; a disk with nothing on it is
 * converted to GPT or MBR. XP has no Shrink nor Extend, as its real one had none, and a full MBR disk greys New
 * Partition, saying why in the status bar.
 */
@PaletteHolder
public final class DiskManagementApp implements IDesktopApp {

    private final DiskToolClient disks;
    private final boolean xp;
    private final Panel root = new Panel();
    private final ContextMenu menu;
    private final List<Hit> hits = new ArrayList<>();
    @Nullable
    private DiskForm form;
    private OsSkin skin = OsSkin.fallback();
    private int chosenDisk = -1;
    private long chosenStart = -1L;
    private String status = "";
    /* Where the window's content was last drawn, which a menu stays inside. */
    private int[] content = {0, 0, DiskToolLayout.W, DiskToolLayout.H};

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/disk_management",
            new Colours(0xFFD4D0C8, 0xFF0A246A, 0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFF808080, 0xFFCCE8FF,
                    0xFFECE9D8, 0xFF7F9DB9));
    /* The narrowest a block of the map is drawn, so a small partition still shows its name. */
    private static final int MIN_BLOCK = 30;
    private static final int MENU_W = 140;
    private static final int MENU_ROW = 10;
    /* The one edition whose Disk Management has its own look and no Shrink nor Extend. */
    private static final String XP_DESKTOP = "frames_xp";

    public DiskManagementApp(final BlockPos host, final ResourceLocation desktopId) {
        this.disks = new DiskToolClient(host);
        this.xp = XP_DESKTOP.equals(desktopId.getPath());
        this.menu = this.root.add(new ContextMenu(MENU_W, MENU_ROW));
    }

    @Override
    public String title() {
        return ProgramClient.nameOf("disk_management");
    }

    @Override
    public int defaultWidth() {
        return DiskToolLayout.W;
    }

    @Override
    public int defaultHeight() {
        return DiskToolLayout.H;
    }

    @Override
    public int minWidth() {
        return DiskToolLayout.W;
    }

    @Override
    public int minHeight() {
        return DiskToolLayout.H;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onClosed() {
        this.disks.close();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.disks.keepUp();
        this.hits.clear();
        this.content = new int[]{x, y, width, height};
        final Colours c = PALETTE.get();
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        Draw.text(g, font, GameText.resolve(this.xp ? DiskToolTexts.DM_MENU_XP : DiskToolTexts.DM_MENU), x + 4,
                y + 2, this.skin.text());
        for (int i = 0; i < 6; i++) {
            final int bx = x + 4 + i * 12 + (i >= 2 ? 6 : 0);
            g.fill(bx, y + DiskToolLayout.DM_TOOLBAR_Y + 1, bx + 8, y + DiskToolLayout.DM_TOOLBAR_Y + 9, c.tool());
        }
        final DiskReport report = this.disks.report();
        final Map<String, String> letters = letters(report);
        this.volumes(g, font, x, y + DiskToolLayout.DM_LIST_Y, width, report, letters, c);
        int dy = y + DiskToolLayout.DM_MAP_Y;
        for (final DiskReport.Disk disk : report.disks()) {
            if (dy + DiskToolLayout.DM_DISK_H > y + DiskToolLayout.DM_STATUS_Y) {
                break;
            }
            this.disk(g, font, x, dy, width, disk, letters, c);
            dy += DiskToolLayout.DM_DISK_H + DiskToolLayout.DM_DISK_GAP;
        }
        this.statusBar(g, font, x, y + DiskToolLayout.DM_STATUS_Y, width, c);
        this.root.render(g, new UiContext(this.skin, font, mouseX, mouseY, partialTick));
    }

    @Override
    public boolean modalActive() {
        return this.form != null && this.form.isOpen();
    }

    @Override
    public void renderModal(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height, final int mouseX, final int mouseY) {
        if (this.form != null && this.form.isOpen()) {
            this.form.renderIn(g, new UiContext(this.skin, font, mouseX, mouseY, 0f), x, y, width, height);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (this.modalActive()) {
            this.form.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (this.menu.isOpen() && this.root.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        this.menu.close();
        for (final Hit hit : this.hits) {
            if (mouseX >= hit.x() && mouseX < hit.x() + hit.w() && mouseY >= hit.y() && mouseY < hit.y() + hit.h()) {
                this.chosenDisk = hit.disk();
                this.chosenStart = hit.start();
                this.status = "";
                if (button == 1) {
                    this.openMenu(hit, (int) mouseX, (int) mouseY);
                }
                return;
            }
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (this.modalActive()) {
            this.form.mouseReleased(mouseX, mouseY, button);
            return;
        }
        this.root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(final char c) {
        return this.modalActive() && this.form.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.modalActive()) {
            return this.form.keyPressed(key, scanCode, modifiers);
        }
        return this.menu.isOpen() && this.menu.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean takesTyping() {
        return this.modalActive();
    }

    /* The volumes: every partition with a file system Frames reads, the one chosen lit. */
    private void volumes(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                         final DiskReport report, final Map<String, String> letters, final Colours c) {
        final int[] cols = DiskToolLayout.DM_COLUMNS;
        final TextKey[] heads = {DiskToolTexts.DM_VOLUME, DiskToolTexts.DM_LAYOUT, DiskToolTexts.DM_TYPE,
                DiskToolTexts.DM_FILE_SYSTEM, DiskToolTexts.DM_STATUS, DiskToolTexts.DM_CAPACITY,
                DiskToolTexts.DM_FREE_SPACE, DiskToolTexts.DM_PERCENT_FREE};
        for (int i = 0; i < heads.length; i++) {
            final int room = (i + 1 < cols.length ? cols[i + 1] : width) - cols[i] - 3;
            Draw.text(g, font, clip(font, GameText.resolve(heads[i]), room), x + cols[i] + 1, y + 1, this.skin.text());
        }
        g.fill(x, y + DiskToolLayout.ROW, x + width, y + DiskToolLayout.ROW + 1, c.rule());
        int ry = y + DiskToolLayout.ROW + 2;
        int shown = 0;
        for (final DiskReport.Disk disk : report.disks()) {
            for (final DiskReport.Part part : disk.parts()) {
                if (shown >= DiskToolLayout.DM_LIST_ROWS || !shownAsVolume(part)) {
                    continue;
                }
                final boolean here = disk.index() == this.chosenDisk && part.startMb() == this.chosenStart;
                if (here) {
                    g.fill(x, ry - 1, x + width, ry + DiskToolLayout.ROW - 1, c.chosen());
                }
                final String[] cells = {volumeName(disk, part, letters), GameText.resolve(this.xp
                        ? DiskToolTexts.DM_PARTITION : DiskToolTexts.DM_SIMPLE),
                        GameText.resolve(DiskToolTexts.DM_BASIC), fileSystem(part), this.statusOf(part),
                        size(part.sizeMb()), part.fileSystem() == null || part.swap() ? "" : size(part.freeMb()),
                        part.sizeMb() == 0L ? "" : part.freeMb() * 100L / part.sizeMb() + " %"};
                for (int i = 0; i < cells.length; i++) {
                    final int room = (i + 1 < cols.length ? cols[i + 1] : width) - cols[i] - 3;
                    Draw.text(g, font, clip(font, cells[i], room), x + cols[i] + 1, ry, this.skin.text());
                }
                this.hits.add(new Hit(x, ry - 1, width, DiskToolLayout.ROW, disk.index(), part.startMb(), part));
                ry += DiskToolLayout.ROW;
                shown++;
            }
        }
    }

    /* One disk of the map: its cell, then a block for each partition and each stretch of unallocated space. */
    private void disk(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                      final DiskReport.Disk disk, final Map<String, String> letters, final Colours c) {
        final int h = DiskToolLayout.DM_DISK_H;
        final int lw = DiskToolLayout.DM_LABEL_W;
        g.fill(x + 2, y, x + 2 + lw, y + h, c.cell());
        Draw.text(g, font, GameText.resolve(DiskToolTexts.DM_DISK.with(disk.index())), x + 5, y + 1, c.ink());
        Draw.text(g, font, GameText.resolve(DiskToolTexts.DM_BASIC), x + 5, y + 10, c.ink());
        Draw.text(g, font, clip(font, size(disk.sizeMb()), lw - 4), x + 5, y + 19, c.ink());
        this.hits.add(new Hit(x + 2, y, lw, h, disk.index(), -2L, null));
        final List<long[]> spans = spans(disk);
        final int bx = x + lw + 4;
        final int bw = width - lw - 6;
        final int[] edges = edges(spans, disk.sizeMb(), bw);
        for (int i = 0; i < spans.size(); i++) {
            final long[] span = spans.get(i);
            final DiskReport.Part part = span[2] < 0 ? null : disk.part((int) span[2]);
            final int left = bx + edges[i];
            final int right = bx + edges[i + 1] - 1;
            final boolean here = disk.index() == this.chosenDisk && span[0] == this.chosenStart;
            g.fill(left, y, right, y + h, here ? c.chosen() : c.paper());
            g.fill(left, y, right, y + 3, part == null ? c.unallocated() : c.primary());
            Draw.outline(g, left, y, right - left, h, c.edge());
            final int room = right - left - 3;
            final String[] lines = part == null
                    ? new String[]{size(span[1]), GameText.resolve(DiskToolTexts.DM_UNALLOCATED)}
                    : new String[]{volumeName(disk, part, letters), size(part.sizeMb()) + " " + fileSystem(part),
                    this.statusOf(part)};
            for (int line = 0; line < lines.length; line++) {
                Draw.text(g, font, clip(font, lines[line], room), left + 2, y + 4 + line * 8, c.ink());
            }
            this.hits.add(new Hit(left, y, right - left, h, disk.index(), span[0], part));
        }
    }

    private void statusBar(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                           final Colours c) {
        g.fill(x, y, x + width, y + DiskToolLayout.BAR_H, c.cell());
        final DiskReport.Refusal refusal = this.disks.refusal();
        final String said = refusal != null ? GameText.resolve(DiskTexts.reason(refusal))
                : !this.status.isEmpty() ? this.status : !this.disks.answered()
                ? GameText.resolve(DiskToolTexts.READING) : "";
        if (!said.isEmpty()) {
            Draw.text(g, font, clip(font, said, width - 6), x + 3, y + 2, c.ink());
            return;
        }
        g.fill(x + 3, y + 3, x + 9, y + 9, c.unallocated());
        Draw.text(g, font, GameText.resolve(DiskToolTexts.DM_UNALLOCATED), x + 12, y + 2, c.ink());
        final int px = x + 18 + font.width(GameText.resolve(DiskToolTexts.DM_UNALLOCATED));
        g.fill(px, y + 3, px + 6, y + 9, c.primary());
        Draw.text(g, font, GameText.resolve(DiskToolTexts.DM_PRIMARY), px + 9, y + 2, c.ink());
    }

    /* The menu a right click opens: a partition's, the unallocated space's, or a disk's. */
    private void openMenu(final Hit hit, final int mouseX, final int mouseY) {
        final DiskReport.Disk disk = this.disks.report().disk(hit.disk());
        if (disk == null) {
            return;
        }
        final List<ContextMenu.Item> items = new ArrayList<>();
        if (hit.start() == -2L) {
            items.add(item(DiskToolTexts.DM_TO_GPT, disk.empty() && disk.scheme() != PartitionScheme.GPT,
                    () -> this.disks.change(DiskCommand.convert(disk.index(), PartitionScheme.GPT))));
            items.add(item(DiskToolTexts.DM_TO_MBR, disk.empty() && disk.scheme() != PartitionScheme.MBR,
                    () -> this.disks.change(DiskCommand.convert(disk.index(), PartitionScheme.MBR))));
        } else if (hit.part() == null) {
            final boolean full = disk.layout().full();
            if (full) {
                this.status = GameText.resolve(DiskTexts.reason(DiskReport.Refusal.TABLE_FULL));
            }
            items.add(item(this.xp ? DiskToolTexts.DM_NEW_PARTITION : DiskToolTexts.DM_NEW_VOLUME, !full,
                    () -> this.newVolume(disk, hit.start())));
        } else {
            final DiskReport.Part part = hit.part();
            items.add(item(DiskToolTexts.DM_OPEN, false, () -> { }));
            items.add(item(DiskToolTexts.DM_EXPLORE, false, () -> { }));
            items.add(ContextMenu.Item.separator());
            items.add(item(DiskToolTexts.DM_MARK_ACTIVE, disk.scheme() == PartitionScheme.MBR && !part.active(),
                    () -> this.disks.change(DiskCommand.active(disk.index(), part.number()))));
            items.add(item(DiskToolTexts.DM_CHANGE_LETTER, false, () -> { }));
            items.add(item(DiskToolTexts.DM_FORMAT, !part.mounted(), () -> this.format(disk, part)));
            items.add(ContextMenu.Item.separator());
            if (!this.xp) {
                final long room = disk.layout().roomAfter(part.entry());
                items.add(item(DiskToolTexts.DM_EXTEND, room > 0L, () -> this.extend(disk, part, room)));
                items.add(item(DiskToolTexts.DM_SHRINK, part.fileSystem() != null && part.freeMb() > 0L,
                        () -> this.shrink(disk, part)));
                items.add(item(DiskToolTexts.DM_ADD_MIRROR, false, () -> { }));
            }
            items.add(item(this.xp ? DiskToolTexts.DM_DELETE_PARTITION : DiskToolTexts.DM_DELETE_VOLUME,
                    !part.mounted(), () -> this.delete(disk, part)));
        }
        items.add(ContextMenu.Item.separator());
        items.add(item(DiskToolTexts.DM_PROPERTIES, false, () -> { }));
        items.add(item(DiskToolTexts.DM_HELP, false, () -> { }));
        this.menu.open(items, mouseX, mouseY, this.content[0], this.content[1], this.content[2], this.content[3]);
    }

    /* Format: the label, the file system and the quick format asked, then the warning before it is done. */
    private void format(final DiskReport.Disk disk, final DiskReport.Part part) {
        final TextField label = new TextField(32).set(part.label().isEmpty()
                ? GameText.resolve(DiskToolTexts.DM_NEW_VOLUME_LABEL) : part.label());
        final List<FileSystemType> types = this.xp ? List.of(FileSystemType.NTFS, FileSystemType.FAT32)
                : List.of(FileSystemType.NTFS, FileSystemType.FAT32, FileSystemType.EXFAT);
        final int[] chosen = {0};
        final boolean[] quick = {true};
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.DM_FORMAT_TITLE), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H)
                .control(() -> GameText.resolve(DiskToolTexts.DM_VOLUME_LABEL), label)
                .control(() -> GameText.resolve(DiskToolTexts.DM_FILE_SYSTEM_FIELD), choice(types, chosen))
                .wide(new Checkbox(() -> GameText.resolve(DiskToolTexts.DM_QUICK), () -> quick[0],
                        () -> quick[0] = !quick[0]))
                .buttons(() -> GameText.resolve(DiskToolTexts.OK), () -> this.confirm(
                        DiskToolTexts.DM_FORMAT_WARNING_TITLE, DiskToolTexts.DM_FORMAT_WARNING, DiskToolTexts.OK,
                        () -> {
                            this.disks.change(DiskCommand.format(disk.index(), part.number(),
                                    types.get(chosen[0]).serializedName()));
                            this.disks.change(DiskCommand.label(disk.index(), part.number(), label.edit()));
                        }), () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    private void shrink(final DiskReport.Disk disk, final DiskReport.Part part) {
        final TextField amount = new TextField(12).set(Long.toString(part.freeMb() / 2L));
        final String name = volumeName(disk, part, letters(this.disks.report()));
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.DM_SHRINK_TITLE.with(name)),
                DiskToolLayout.FORM_W, DiskToolLayout.FORM_H)
                .info(() -> GameText.resolve(DiskToolTexts.DM_SHRINK_BEFORE), () -> Long.toString(part.sizeMb()))
                .info(() -> GameText.resolve(DiskToolTexts.DM_SHRINK_AVAILABLE), () -> Long.toString(part.freeMb()))
                .control(() -> GameText.resolve(DiskToolTexts.DM_SHRINK_AMOUNT), amount)
                .info(() -> "", () -> Long.toString(part.sizeMb() - number(amount.edit())))
                .note(() -> GameText.resolve(DiskToolTexts.DM_SHRINK_NOTE.with(name, part.usedMb())),
                        this.skin.dim())
                .buttons(() -> GameText.resolve(DiskToolTexts.DM_SHRINK_BUTTON),
                        () -> this.disks.change(DiskCommand.resize(disk.index(), part.number(),
                                part.sizeMb() - number(amount.edit()))),
                        () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    private void extend(final DiskReport.Disk disk, final DiskReport.Part part, final long room) {
        final TextField amount = new TextField(12).set(Long.toString(room));
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.DM_EXTEND_TITLE), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H)
                .info(() -> GameText.resolve(DiskToolTexts.DM_EXTEND_TOTAL), () -> Long.toString(part.sizeMb()))
                .info(() -> GameText.resolve(DiskToolTexts.DM_EXTEND_MAX), () -> Long.toString(room))
                .control(() -> GameText.resolve(DiskToolTexts.DM_EXTEND_AMOUNT), amount)
                .buttons(() -> GameText.resolve(DiskToolTexts.DM_EXTEND_BUTTON),
                        () -> this.disks.change(DiskCommand.resize(disk.index(), part.number(),
                                part.sizeMb() + Math.min(room, number(amount.edit())))),
                        () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* New Simple Volume (New Partition on XP): its size, its file system and its label, made in the free space. */
    private void newVolume(final DiskReport.Disk disk, final long start) {
        long room = 0L;
        for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
            if (gap.startMb() == start) {
                room = gap.sizeMb();
            }
        }
        final long max = room;
        final TextField amount = new TextField(12).set(Long.toString(max));
        final TextField label = new TextField(32).set(GameText.resolve(DiskToolTexts.DM_NEW_VOLUME_LABEL));
        final List<FileSystemType> types = this.xp ? List.of(FileSystemType.NTFS, FileSystemType.FAT32)
                : List.of(FileSystemType.NTFS, FileSystemType.FAT32, FileSystemType.EXFAT);
        final int[] chosen = {0};
        this.form = new DiskForm(() -> GameText.resolve(this.xp ? DiskToolTexts.DM_NEW_TITLE_XP
                : DiskToolTexts.DM_NEW_TITLE), DiskToolLayout.FORM_W, DiskToolLayout.FORM_H)
                .info(() -> GameText.resolve(DiskToolTexts.DM_NEW_MAX), () -> Long.toString(max))
                .control(() -> GameText.resolve(DiskToolTexts.DM_NEW_SIZE), amount)
                .control(() -> GameText.resolve(DiskToolTexts.DM_FILE_SYSTEM_FIELD), choice(types, chosen))
                .control(() -> GameText.resolve(DiskToolTexts.DM_VOLUME_LABEL), label)
                .buttons(() -> GameText.resolve(DiskToolTexts.DM_FINISH), () -> {
                    this.disks.change(DiskCommand.create(disk.index(), start, Math.min(max,
                            number(amount.edit())), types.get(chosen[0]).serializedName()));
                    this.status = "";
                }, () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    private void delete(final DiskReport.Disk disk, final DiskReport.Part part) {
        this.confirm(this.xp ? DiskToolTexts.DM_DELETE_TITLE_XP : DiskToolTexts.DM_DELETE_TITLE,
                DiskToolTexts.DM_DELETE_WARNING, DiskToolTexts.DM_YES,
                () -> this.disks.change(DiskCommand.delete(disk.index(), part.number())));
    }

    /* A warning that asks before it is done: OK or Yes does it, the other leaves it. */
    private void confirm(final TextKey title, final TextKey warning, final TextKey doIt, final Runnable action) {
        this.form = new DiskForm(() -> GameText.resolve(title), DiskToolLayout.FORM_W, DiskToolLayout.FORM_H / 2 + 20)
                .note(() -> GameText.resolve(warning), this.skin.text())
                .buttons(() -> GameText.resolve(doIt), action, () -> GameText.resolve(
                        doIt == DiskToolTexts.DM_YES ? DiskToolTexts.DM_NO : DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* A partition's status as Disk Management words it: the system's own, the EFI one, a primary one. */
    private String statusOf(final DiskReport.Part part) {
        if (part.role() == PartitionRole.EFI) {
            return GameText.resolve(DiskToolTexts.DM_HEALTHY_EFI);
        }
        if (part.mounted() && !part.swap()) {
            return GameText.resolve(this.xp ? DiskToolTexts.DM_HEALTHY_SYSTEM : DiskToolTexts.DM_HEALTHY_BOOT);
        }
        if (!shownAsVolume(part)) {
            return GameText.resolve(this.xp ? DiskToolTexts.DM_HEALTHY_UNKNOWN : DiskToolTexts.DM_HEALTHY_PRIMARY);
        }
        return GameText.resolve(this.xp ? DiskToolTexts.DM_HEALTHY : DiskToolTexts.DM_HEALTHY_PRIMARY);
    }

    private static ContextMenu.Item item(final TextKey label, final boolean enabled, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), enabled, action);
    }

    private static Dropdown choice(final List<FileSystemType> types, final int[] chosen) {
        final List<Supplier<String>> names = new ArrayList<>();
        for (final FileSystemType type : types) {
            names.add(type::displayName);
        }
        return new Dropdown(names, () -> chosen[0], index -> chosen[0] = index, word -> word);
    }

    /*
     * The letter Frames gives each volume, as its drives are lettered: C: the one it runs from, then every other it
     * reads, disk by disk; keyed "disk:start".
     */
    private static Map<String, String> letters(final DiskReport report) {
        final Map<String, String> out = new HashMap<>();
        char next = 'D';
        for (final DiskReport.Disk disk : report.disks()) {
            for (final DiskReport.Part part : disk.parts()) {
                if (part.mounted() && !part.swap()) {
                    out.put(disk.index() + ":" + part.startMb(), "C:");
                }
            }
        }
        for (final DiskReport.Disk disk : report.disks()) {
            for (final DiskReport.Part part : disk.parts()) {
                final String key = disk.index() + ":" + part.startMb();
                if (!out.containsKey(key) && shownAsVolume(part) && next <= 'Z') {
                    out.put(key, next++ + ":");
                }
            }
        }
        return out;
    }

    /* Whether Frames shows a partition as a volume: one of a file system it reads. */
    private static boolean shownAsVolume(final DiskReport.Part part) {
        final FileSystemType fs = part.fileSystem();
        return part.role() == PartitionRole.DATA && fs != null && fs.holdsData()
                && fs.readBy(Platform.FRAMES, HardwareEra.ADVANCED);
    }

    private static String volumeName(final DiskReport.Disk disk, final DiskReport.Part part,
                                     final Map<String, String> letters) {
        final String letter = letters.get(disk.index() + ":" + part.startMb());
        if (letter == null) {
            return GameText.resolve(DiskToolTexts.DM_NO_LETTER.with(disk.index(), part.number()));
        }
        return part.label().isEmpty() ? "(" + letter + ")" : part.label() + " (" + letter + ")";
    }

    private static String fileSystem(final DiskReport.Part part) {
        final FileSystemType fs = part.fileSystem();
        return fs == null || !shownAsVolume(part) && part.role() != PartitionRole.EFI ? "" : fs.displayName();
    }

    /* The partitions and the unallocated stretches of a disk in the order they lie, as {start, size, number}. */
    private static List<long[]> spans(final DiskReport.Disk disk) {
        final List<long[]> spans = new ArrayList<>();
        for (final DiskReport.Part part : disk.parts()) {
            spans.add(new long[]{part.startMb(), part.sizeMb(), part.number()});
        }
        for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
            spans.add(new long[]{gap.startMb(), gap.sizeMb(), -1L});
        }
        spans.sort((a, b) -> Long.compare(a[0], b[0]));
        return spans;
    }

    /* Where each block starts across the map's width, by its size, none narrower than a name needs. */
    private static int[] edges(final List<long[]> spans, final long diskMb, final int width) {
        final int[] edges = new int[spans.size() + 1];
        final int room = Math.max(0, width - MIN_BLOCK * spans.size());
        int at = 0;
        for (int i = 0; i < spans.size(); i++) {
            edges[i] = at;
            at += MIN_BLOCK + (int) (room * spans.get(i)[1] / Math.max(1L, diskMb));
        }
        edges[spans.size()] = Math.min(width, Math.max(at, width));
        return edges;
    }

    private static String size(final long mb) {
        return mb >= 1_024L ? String.format(Locale.ROOT, "%.2f GB", mb / 1_024.0) : mb + " MB";
    }

    private static String clip(final Font font, final String text, final int room) {
        if (font.width(text) <= room) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font.width(cut + "..") > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    private static long number(final String text) {
        try {
            return Math.max(0L, Long.parseLong(text.strip()));
        } catch (final NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Something on the window a click lands on: a volume, a block or a disk's cell.
     *
     * @param disk  the disk it is about
     * @param start where its partition or free space starts, or -2 for the disk's own cell
     * @param part  its partition, or null for free space and a disk
     */
    private record Hit(int x, int y, int w, int h, int disk, long start, @Nullable DiskReport.Part part) {
    }

    /**
     * Disk Management's colours: its cells, the dark band of a primary partition and the black of the unallocated, the
     * ink, a block's paper and edge, the row chosen, the toolbar's buttons and the rule under the list's heads.
     */
    private record Colours(int cell, int primary, int unallocated, int ink, int paper, int edge, int chosen,
                           int tool, int rule) {
    }
}
