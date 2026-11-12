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
import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The KDE Partition Manager (computers/docs/design/storage-devices.md): the devices down its side, the chosen one's
 * partitions as a bar and as a table, and its pending operations. New, Resize/Move, Delete and Properties (where a
 * partition's file system is recreated) queue an operation; Apply writes them all, Undo takes the last back, Clear
 * forgets them. A mounted ext4 only grows, as its Resize/Move dialog says; a new partition table erases the device.
 */
@PaletteHolder
public final class PartitionManagerApp implements IDesktopApp {

    private final DiskToolClient disks;
    private final Panel root = new Panel();
    private final ContextMenu menu;
    private final List<Pending> pending = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    @Nullable
    private DiskForm form;
    private OsSkin skin = OsSkin.fallback();
    private int device;
    private long chosenStart = -1L;
    private int[] content = {0, 0, DiskToolLayout.W, DiskToolLayout.H};

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/partition_manager",
            new Colours(0xFFEFF0F1, 0xFF31363B, 0xFF7F8C8D, 0xFF93CEE9, 0xFF3DAEE9, 0xFF27AE60, 0xFF2980B9,
                    0xFFDA4453, 0xFFBDC3C7, 0xFF8E44AD, 0xFFFFFFFF));
    /* The toolbar's actions, in their order; the greyed ones only stand there, as on the real one. */
    private static final int APPLY = 0;
    private static final int UNDO = 1;
    private static final int CLEAR = 2;
    private static final int NEW = 3;
    private static final int RESIZE = 4;
    private static final int DELETE = 5;
    private static final int PROPERTIES = 6;
    private static final TextKey[] TOOLBAR = {DiskToolTexts.KPM_APPLY, DiskToolTexts.KPM_UNDO,
            DiskToolTexts.KPM_CLEAR, DiskToolTexts.KPM_NEW, DiskToolTexts.KPM_RESIZE, DiskToolTexts.KPM_DELETE,
            DiskToolTexts.KPM_PROPERTIES};
    private static final int TOOL_GAP = 7;
    /* The narrowest a partition's box is drawn, so a small one still shows the start of its name. */
    private static final int MIN_BOX = 20;
    /* How strongly the used part of a box is filled with its file system's colour, as an alpha. */
    private static final int USED_ALPHA = 89;
    private static final int MENU_W = 110;
    private static final int MENU_ROW = 10;
    private static final double MB_PER_GIB = 1_024.0;

    public PartitionManagerApp(final BlockPos host) {
        this.disks = new DiskToolClient(host);
        this.menu = this.root.add(new ContextMenu(MENU_W, MENU_ROW));
    }

    @Override
    public String title() {
        return ProgramClient.nameOf("disks");
    }

    @Override
    public int defaultWidth() {
        return DesktopWindow.windowWidthFor(DiskToolLayout.W);
    }

    @Override
    public int defaultHeight() {
        return DesktopWindow.windowHeightFor(DiskToolLayout.H);
    }

    @Override
    public int minWidth() {
        return DesktopWindow.windowWidthFor(DiskToolLayout.W);
    }

    @Override
    public int minHeight() {
        return DesktopWindow.windowHeightFor(DiskToolLayout.H);
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
        g.fill(x, y, x + width, y + height, c.paper());
        Draw.text(g, font, GameText.resolve(DiskToolTexts.KPM_MENU), x + 4, y + 2, c.ink());
        this.toolbar(g, font, x + 4, y + DiskToolLayout.KPM_TOOLBAR_Y + 1, c);
        final DiskReport report = this.disks.report();
        // The status bar runs along the foot of the window, whatever height the window is.
        final int sy = y + height - DiskToolLayout.BAR_H;
        this.devices(g, font, x, y + DiskToolLayout.KPM_TOP, sy - 2, report, c);
        final DiskReport.Disk disk = report.disk(this.device);
        final int rx = x + DiskToolLayout.KPM_SIDE_W + 2;
        final int rw = width - DiskToolLayout.KPM_SIDE_W - 4;
        if (disk != null) {
            this.bar(g, font, rx, y + DiskToolLayout.KPM_TOP, rw, disk, c);
            this.table(g, font, rx, y + DiskToolLayout.KPM_TABLE_Y, rw, disk, c);
        }
        this.pendingList(g, font, rx, y + DiskToolLayout.KPM_PENDING_Y, c);
        g.fill(x, sy, x + width, sy + DiskToolLayout.BAR_H, c.side());
        final DiskReport.Refusal refusal = this.disks.refusal();
        Draw.text(g, font, refusal != null ? clip(font, GameText.resolve(DiskTexts.reason(refusal)), width - 8)
                : GameText.resolve(DiskToolTexts.KPM_COUNT.with(this.pending.size())), x + 3, sy + 2,
                refusal != null ? c.swap() : c.ink());
        if (disk != null && refusal == null) {
            final String scheme = GameText.resolve(disk.scheme().displayName());
            Draw.text(g, font, scheme, x + width - 3 - font.width(scheme), sy + 2, c.dim());
        }
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
                this.press(hit, button, (int) mouseX, (int) mouseY);
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

    private void press(final Hit hit, final int button, final int mouseX, final int mouseY) {
        switch (hit.kind()) {
            case Hit.TOOL -> this.tool((int) hit.value());
            case Hit.DEVICE -> {
                this.device = (int) hit.value();
                this.chosenStart = -1L;
                if (button == 1) {
                    this.menu.open(List.of(new ContextMenu.Item(GameText.resolve(DiskToolTexts.KPM_NEW_TABLE), true,
                                    this::newTable)), mouseX, mouseY, this.content[0], this.content[1],
                            this.content[2], this.content[3]);
                }
            }
            default -> this.chosenStart = hit.value();
        }
    }

    /* What a toolbar button does. */
    private void tool(final int which) {
        final DiskReport.Disk disk = this.disks.report().disk(this.device);
        final DiskReport.Part part = disk == null ? null : partAt(disk, this.chosenStart);
        switch (which) {
            case APPLY -> {
                if (!this.pending.isEmpty()) {
                    this.applyAsked();
                }
            }
            case UNDO -> {
                if (!this.pending.isEmpty()) {
                    this.pending.removeLast();
                }
            }
            case CLEAR -> this.pending.clear();
            case NEW -> {
                if (disk != null && part == null && this.chosenStart >= 0L) {
                    this.newPartition(disk, this.chosenStart);
                }
            }
            case RESIZE -> {
                if (disk != null && part != null && part.fileSystem() != null) {
                    this.resize(disk, part);
                }
            }
            case DELETE -> {
                if (disk != null && part != null) {
                    this.queue(DiskToolTexts.KPM_OP_DELETE.with(device(disk, part)),
                            DiskCommand.delete(disk.index(), part.number()));
                }
            }
            case PROPERTIES -> {
                if (disk != null && part != null) {
                    this.properties(disk, part);
                }
            }
            default -> {
            }
        }
    }

    private void toolbar(final GuiGraphics g, final Font font, final int x, final int y, final Colours c) {
        int tx = x;
        final DiskReport.Disk disk = this.disks.report().disk(this.device);
        final DiskReport.Part part = disk == null ? null : partAt(disk, this.chosenStart);
        for (int i = 0; i < TOOLBAR.length; i++) {
            final String word = GameText.resolve(TOOLBAR[i]);
            final boolean on = switch (i) {
                case APPLY, UNDO, CLEAR -> !this.pending.isEmpty();
                case NEW -> part == null && this.chosenStart >= 0L;
                default -> part != null;
            };
            Draw.text(g, font, word, tx, y, on ? c.ink() : c.dim());
            this.hits.add(new Hit(Hit.TOOL, tx, y - 1, font.width(word), 10, i));
            tx += font.width(word) + TOOL_GAP;
        }
    }

    /* The devices down the side, from {@code y} to {@code bottom}, the chosen one lit. */
    private void devices(final GuiGraphics g, final Font font, final int x, final int y, final int bottom,
                         final DiskReport report, final Colours c) {
        g.fill(x, y, x + DiskToolLayout.KPM_SIDE_W, bottom, c.side());
        Draw.text(g, font, GameText.resolve(DiskToolTexts.KPM_DEVICES), x + 3, y + 2, c.dim());
        int dy = y + 13;
        for (final DiskReport.Disk disk : report.disks()) {
            if (disk.index() == this.device) {
                g.fill(x, dy - 1, x + DiskToolLayout.KPM_SIDE_W, dy + 18, c.chosen());
            }
            // The drive's small picture: its case, and a red and a blue lamp on its front.
            g.fill(x + 3, dy + 2, x + 13, dy + 9, c.dim());
            g.fill(x + 4, dy + 3, x + 12, dy + 8, c.unallocated());
            g.fill(x + 5, dy + 5, x + 7, dy + 7, c.swap());
            g.fill(x + 9, dy + 5, x + 11, dy + 7, c.ntfs());
            Draw.text(g, font, clip(font, disk.model(), DiskToolLayout.KPM_SIDE_W - 18), x + 16, dy, c.ink());
            // The device a disk is under, a path the same in every language.
            final String device = "/dev/sd" + (char) ('a' + disk.index());
            Draw.text(g, font, clip(font, size(disk.sizeMb()) + " - " + device, DiskToolLayout.KPM_SIDE_W - 18),
                    x + 16, dy + 9, c.dim());
            this.hits.add(new Hit(Hit.DEVICE, x, dy - 1, DiskToolLayout.KPM_SIDE_W, 19, disk.index()));
            dy += 21;
        }
    }

    /*
     * The chosen device's partitions as boxes across its width, each as wide as its share of the disk, edged in its
     * file system's colour and filled from the left as far as it is used; the chosen one edged dark.
     */
    private void bar(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                     final DiskReport.Disk disk, final Colours c) {
        final List<long[]> spans = DiskMap.spans(disk);
        final int[] edges = DiskMap.edges(spans, disk.sizeMb(), width, MIN_BOX);
        final int h = DiskToolLayout.KPM_BAR_H;
        for (int i = 0; i < spans.size(); i++) {
            final long[] span = spans.get(i);
            final DiskReport.Part part = span[2] < 0 ? null : disk.part((int) span[2]);
            final int bx = x + edges[i];
            final int w = edges[i + 1] - edges[i] - 2;
            final int edge = part == null ? c.unallocated() : this.colourOf(part, c);
            g.fill(bx, y, bx + w, y + h, c.paper());
            if (part != null && part.fileSystem() != null && !part.swap() && part.sizeMb() > 0L) {
                final int used = (int) ((w - 4) * Math.min(part.usedMb(), part.sizeMb()) / part.sizeMb());
                g.fill(bx + 2, y + 2, bx + 2 + used, y + h - 2, MachineArt.alpha(edge, USED_ALPHA));
            }
            final int frame = span[0] == this.chosenStart ? c.ink() : edge;
            Draw.outline(g, bx, y, w, h, frame);
            Draw.outline(g, bx + 1, y + 1, w - 2, h - 2, frame);
            final String name = part == null ? GameText.resolve(DiskToolTexts.KPM_UNALLOCATED) : device(disk, part);
            Draw.text(g, font, clip(font, name, w - 4), bx + 3, y + 3, c.ink());
            Draw.text(g, font, clip(font, size(span[1]), w - 4), bx + 3, y + 11, c.ink());
            this.hits.add(new Hit(Hit.SPOT, bx, y, w, h, span[0]));
        }
    }

    private void table(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                       final DiskReport.Disk disk, final Colours c) {
        final int[] cols = DiskToolLayout.spread(DiskToolLayout.KPM_COLUMNS, width,
                DiskToolLayout.W - DiskToolLayout.KPM_SIDE_W - 4);
        final TextKey[] heads = {DiskToolTexts.KPM_PARTITION, DiskToolTexts.KPM_TYPE, DiskToolTexts.KPM_MOUNT_POINT,
                DiskToolTexts.KPM_LABEL, DiskToolTexts.KPM_SIZE, DiskToolTexts.KPM_USED, DiskToolTexts.KPM_AVAILABLE};
        for (int i = 0; i < heads.length; i++) {
            Draw.text(g, font, clip(font, GameText.resolve(heads[i]), room(cols, i, width)), x + cols[i], y, c.dim());
        }
        int ry = y + DiskToolLayout.ROW;
        int rows = 0;
        for (final long[] span : DiskMap.spans(disk)) {
            if (rows++ >= DiskToolLayout.KPM_TABLE_ROWS) {
                break;
            }
            final DiskReport.Part part = span[2] < 0 ? null : disk.part((int) span[2]);
            if (span[0] == this.chosenStart) {
                g.fill(x, ry - 1, x + width, ry + DiskToolLayout.ROW - 1, c.chosen());
            }
            final String[] cells = part == null
                    ? new String[]{GameText.resolve(DiskToolTexts.KPM_UNALLOCATED),
                    GameText.resolve(DiskToolTexts.KPM_UNALLOCATED), "", "", size(span[1]), "", ""}
                    : new String[]{device(disk, part), part.fileSystem() == null ? "" : typeName(part),
                    mountPoint(part), part.label(), size(part.sizeMb()), part.fileSystem() == null || part.swap()
                    ? "" : size(part.usedMb()), part.fileSystem() == null || part.swap() ? "" : size(part.freeMb())};
            if (part != null && !mountPoint(part).isEmpty()) {
                // The padlock of a mounted partition, before its name: its shackle over its body.
                Draw.outline(g, x + 1, ry, 4, 4, c.ink());
                g.fill(x, ry + 3, x + 6, ry + 7, c.ink());
            }
            for (int i = 0; i < cells.length; i++) {
                // The name stands after the padlock's place, the type after its colour's square.
                final int lead = i <= 1 ? 7 : 0;
                if (i == 1) {
                    g.fill(x + cols[i], ry + 1, x + cols[i] + 5, ry + 6, part == null ? c.unallocated()
                            : this.colourOf(part, c));
                }
                Draw.text(g, font, clip(font, cells[i], room(cols, i, width) - lead), x + cols[i] + lead, ry,
                        c.ink());
            }
            this.hits.add(new Hit(Hit.SPOT, x, ry - 1, width, DiskToolLayout.ROW, span[0]));
            ry += DiskToolLayout.ROW;
        }
    }

    private void pendingList(final GuiGraphics g, final Font font, final int x, final int y, final Colours c) {
        Draw.text(g, font, GameText.resolve(DiskToolTexts.KPM_PENDING), x, y, c.dim());
        if (this.pending.isEmpty()) {
            Draw.text(g, font, GameText.resolve(DiskToolTexts.KPM_NONE), x, y + DiskToolLayout.ROW, c.dim());
            return;
        }
        int py = y + DiskToolLayout.ROW;
        for (final Pending op : this.pending) {
            Draw.text(g, font, clip(font, GameText.resolve(op.said()), DiskToolLayout.W - DiskToolLayout.KPM_SIDE_W
                    - 8), x, py, c.ink());
            py += DiskToolLayout.ROW;
        }
    }

    /* Create a new partition: its size, its file system and its label, in the free space chosen. */
    private void newPartition(final DiskReport.Disk disk, final long start) {
        long room = 0L;
        for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
            if (gap.startMb() == start) {
                room = gap.sizeMb();
            }
        }
        final long max = room;
        final TextField amount = new TextField(12).set(Long.toString(max));
        final TextField label = new TextField(32);
        final List<FileSystemType> types = List.of(FileSystemType.EXT4, FileSystemType.BTRFS, FileSystemType.XFS,
                FileSystemType.FAT32, FileSystemType.NTFS, FileSystemType.SWAP);
        final int[] chosen = {0};
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.KPM_NEW_TITLE), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H)
                .control(() -> GameText.resolve(DiskToolTexts.KPM_FILE_SYSTEM), choice(types, chosen))
                .control(() -> GameText.resolve(DiskToolTexts.KPM_LABEL_FIELD), label)
                .control(() -> GameText.resolve(DiskToolTexts.KPM_SIZE_FIELD), amount)
                .buttons(() -> GameText.resolve(DiskToolTexts.OK), () -> {
                    final long size = Math.min(max, number(amount.edit()));
                    final FileSystemType fs = types.get(chosen[0]);
                    this.queue(DiskToolTexts.KPM_OP_CREATE.with(fs.displayName(), size(size), "/dev/sd"
                            + (char) ('a' + disk.index())), DiskCommand.create(disk.index(), start, size,
                            fs.serializedName()));
                }, () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* Resize/Move: the size asked; a mounted file system that shrinks only unmounted says so. */
    private void resize(final DiskReport.Disk disk, final DiskReport.Part part) {
        final long room = disk.layout().roomAfter(part.entry());
        final FileSystemType fs = part.fileSystem();
        final boolean growsOnly = part.mounted() && fs != null && !fs.resizes(false, true);
        final TextField amount = new TextField(12).set(Long.toString(part.sizeMb() + room));
        final DiskForm resize = new DiskForm(() -> GameText.resolve(DiskToolTexts.KPM_RESIZE_TITLE
                .with(device(disk, part))), DiskToolLayout.FORM_W, DiskToolLayout.FORM_H)
                .info(() -> GameText.resolve(DiskToolTexts.KPM_FREE_BEFORE), () -> size(0L))
                .control(() -> GameText.resolve(DiskToolTexts.KPM_SIZE_FIELD), amount)
                .info(() -> GameText.resolve(DiskToolTexts.KPM_FREE_AFTER),
                        () -> size(Math.max(0L, part.sizeMb() + room - number(amount.edit()))));
        if (growsOnly) {
            resize.note(() -> GameText.resolve(DiskToolTexts.KPM_SMALLEST.with(size(part.sizeMb()))) + " "
                    + device(disk, part) + ": " + GameText.resolve(DiskTexts.reason(
                    DiskReport.Refusal.SHRINKS_ONLY_UNMOUNTED)), PALETTE.get().swap());
        }
        this.form = resize.buttons(() -> GameText.resolve(DiskToolTexts.OK), () -> {
            final long to = Math.max(1L, Math.min(part.sizeMb() + room, number(amount.edit())));
            this.queue(DiskToolTexts.KPM_OP_RESIZE.with(device(disk, part), size(to)),
                    DiskCommand.resize(disk.index(), part.number(), to));
        }, () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* Properties: its label, and its file system recreated when asked. */
    private void properties(final DiskReport.Disk disk, final DiskReport.Part part) {
        final TextField label = new TextField(32).set(part.label());
        final List<FileSystemType> types = List.of(FileSystemType.EXT4, FileSystemType.EXT3, FileSystemType.BTRFS,
                FileSystemType.XFS, FileSystemType.FAT32, FileSystemType.NTFS, FileSystemType.EXFAT,
                FileSystemType.SWAP);
        final int[] chosen = {Math.max(0, types.indexOf(part.fileSystem()))};
        final boolean[] recreate = {false};
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.KPM_PROPERTIES_TITLE.with(device(disk, part))),
                DiskToolLayout.FORM_W, DiskToolLayout.FORM_H)
                .control(() -> GameText.resolve(DiskToolTexts.KPM_LABEL_FIELD), label)
                .control(() -> GameText.resolve(DiskToolTexts.KPM_FILE_SYSTEM), choice(types, chosen))
                .wide(new Checkbox(() -> GameText.resolve(DiskToolTexts.KPM_RECREATE), () -> recreate[0],
                        () -> recreate[0] = !recreate[0]))
                .buttons(() -> GameText.resolve(DiskToolTexts.OK), () -> {
                    final FileSystemType fs = types.get(chosen[0]);
                    if (recreate[0] || fs != part.fileSystem()) {
                        this.queue(DiskToolTexts.KPM_OP_FORMAT.with(fs.displayName(), device(disk, part)),
                                DiskCommand.format(disk.index(), part.number(), fs.serializedName()));
                    }
                    if (!label.edit().equals(part.label())) {
                        this.queue(DiskToolTexts.KPM_OP_LABEL.with(device(disk, part), label.edit()),
                                DiskCommand.label(disk.index(), part.number(), label.edit()));
                    }
                }, () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* New Partition Table: MS-DOS or GPT, and the warning that it destroys everything on the device. */
    private void newTable() {
        final DiskReport.Disk disk = this.disks.report().disk(this.device);
        if (disk == null) {
            return;
        }
        final List<PartitionScheme> schemes = List.of(PartitionScheme.GPT, PartitionScheme.MBR);
        final int[] chosen = {0};
        final String name = "/dev/sd" + (char) ('a' + disk.index());
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.KPM_TABLE_TITLE.with(name)),
                DiskToolLayout.FORM_W, DiskToolLayout.FORM_H)
                .control(() -> "", new Dropdown(List.of(() -> "GPT", () -> "MS-Dos"), () -> chosen[0],
                        index -> chosen[0] = index, word -> word))
                .note(() -> GameText.resolve(DiskToolTexts.KPM_TABLE_WARNING), PALETTE.get().swap())
                .buttons(() -> GameText.resolve(DiskToolTexts.KPM_TABLE_CREATE), () -> this.queue(
                        DiskToolTexts.KPM_OP_TABLE.with(schemes.get(chosen[0]).name(), name),
                        DiskCommand.table(disk.index(), schemes.get(chosen[0]))),
                        () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* Apply: asked first, then every pending operation written in its order. */
    private void applyAsked() {
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.KPM_APPLY_TITLE), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H / 2 + 20)
                .note(() -> GameText.resolve(DiskToolTexts.KPM_APPLY_ASK), PALETTE.get().ink())
                .buttons(() -> GameText.resolve(DiskToolTexts.KPM_APPLY), () -> {
                    for (final Pending op : this.pending) {
                        this.disks.change(op.path());
                    }
                    this.pending.clear();
                }, () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    private void queue(final Text said, final String path) {
        this.pending.add(new Pending(said, path));
    }

    private int colourOf(final DiskReport.Part part, final Colours c) {
        final FileSystemType fs = part.fileSystem();
        if (fs == null) {
            return c.unallocated();
        }
        return switch (fs) {
            case FAT12, FAT16, FAT32, EXFAT -> c.fat();
            case NTFS -> c.ntfs();
            case EXT2, EXT3, EXT4, XFS, BTRFS -> c.ext();
            case SWAP -> c.swap();
            default -> c.other();
        };
    }

    @Nullable
    private static DiskReport.Part partAt(final DiskReport.Disk disk, final long start) {
        for (final DiskReport.Part part : disk.parts()) {
            if (part.startMb() == start) {
                return part;
            }
        }
        return null;
    }

    private static String device(final DiskReport.Disk disk, final DiskReport.Part part) {
        return "/dev/sd" + (char) ('a' + disk.index()) + part.number();
    }

    /* A partition's file system as the Partition Manager names it: fat32, ntfs, ext4, linuxswap. */
    private static String typeName(final DiskReport.Part part) {
        final FileSystemType fs = part.fileSystem();
        return fs == FileSystemType.SWAP ? "linuxswap" : fs == null ? "" : fs.displayName().toLowerCase(Locale.ROOT);
    }

    /* Where a partition is mounted: the root, the EFI one, the swap, and the others under /media. */
    private static String mountPoint(final DiskReport.Part part) {
        if (part.role() == PartitionRole.EFI) {
            return "/boot/efi";
        }
        if (part.mounted()) {
            return part.swap() ? "" : "/";
        }
        return part.label().isEmpty() || part.swap() ? "" : "/media/" + part.label().toLowerCase(Locale.ROOT);
    }

    private static Dropdown choice(final List<FileSystemType> types, final int[] chosen) {
        final List<Supplier<String>> names = new ArrayList<>();
        for (final FileSystemType type : types) {
            names.add(() -> type == FileSystemType.SWAP ? "linuxswap" : type.displayName().toLowerCase(Locale.ROOT));
        }
        return new Dropdown(names, () -> chosen[0], index -> chosen[0] = index, word -> word);
    }

    /* A size as the Partition Manager writes it: MiB, GiB or TiB to two places. */
    private static String size(final long mb) {
        if (mb >= 1_048_576L) {
            return String.format(Locale.ROOT, "%.2f TiB", mb / (MB_PER_GIB * MB_PER_GIB));
        }
        if (mb >= 1_024L) {
            return String.format(Locale.ROOT, "%.2f GiB", mb / MB_PER_GIB);
        }
        return String.format(Locale.ROOT, "%.2f MiB", (double) mb);
    }

    private static int room(final int[] cols, final int i, final int width) {
        return (i + 1 < cols.length ? cols[i + 1] : width) - cols[i] - 3;
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

    /** An operation waiting for Apply: what the list says of it, and the change it asks the machine for. */
    private record Pending(Text said, String path) {
    }

    /**
     * Something a click lands on: a toolbar button, a device, a partition or a stretch of free space.
     *
     * @param value the button, the device's index, or where the partition or free space starts
     */
    private record Hit(int kind, int x, int y, int w, int h, long value) {

        static final int TOOL = 0;
        static final int DEVICE = 1;
        static final int SPOT = 2;
    }

    /**
     * The Partition Manager's colours: its paper and ink, its dim words, the row chosen, the side panel, and the edge
     * of each kind of file system: FAT, NTFS, ext, swap, the unallocated and any other.
     */
    private record Colours(int paper, int ink, int dim, int chosen, int ntfs, int fat, int ext, int swap,
                           int unallocated, int other, int side) {
    }
}
