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
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
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
 * GNOME's Disks (computers/docs/design/storage-devices.md): the disks down its side, the chosen one's model, size and
 * partitioning over its volumes, and the chosen volume's details under them. + makes a partition of the free space
 * chosen, - deletes the partition chosen, the gear formats or resizes it, and the menu at the top formats the whole
 * disk with a new table, GPT or MBR. An MBR disk past 2 TB says so in red, and shows the part its table cannot reach.
 */
@PaletteHolder
public final class GnomeDisksApp implements IDesktopApp {

    private final DiskToolClient disks;
    private final Panel root = new Panel();
    private final ContextMenu menu;
    private final List<Hit> hits = new ArrayList<>();
    @Nullable
    private DiskForm form;
    private OsSkin skin = OsSkin.fallback();
    private int device;
    private long chosenStart = -1L;
    private int[] content = {0, 0, DiskToolLayout.W, DiskToolLayout.H};

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/gnome_disks",
            new Colours(0xFFFFFFFF, 0xFF2E3436, 0xFF888A85, 0xFFEBEBEB, 0xFFDEDDDA, 0xFF3584E4, 0xFFC01C28,
                    0xFFE5A50A));
    private static final int MENU_W = 110;
    private static final int MENU_ROW = 10;
    private static final int BUTTON_ADD = 0;
    private static final int BUTTON_REMOVE = 1;
    private static final int BUTTON_GEAR = 2;
    private static final long BYTES_PER_MB = 1_048_576L;
    private static final double MB_PER_TB = 1_048_576.0;
    private static final double MB_PER_GB = 1_024.0;

    public GnomeDisksApp(final BlockPos host) {
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
        final DiskReport report = this.disks.report();
        this.sidebar(g, font, x, y, height, report, c);
        final DiskReport.Disk disk = report.disk(this.device);
        final int rx = x + DiskToolLayout.GD_SIDE_W + 6;
        final int rw = width - DiskToolLayout.GD_SIDE_W - 12;
        final int mx = x + width - DiskToolLayout.GD_MENU_W - 4;
        Draw.outline(g, mx, y + 2, DiskToolLayout.GD_MENU_W, DiskToolLayout.GD_MENU_W, c.dim());
        for (int dot = 0; dot < 3; dot++) {
            g.fill(mx + 7, y + 5 + dot * 4, mx + 9, y + 7 + dot * 4, c.ink());
        }
        this.hits.add(new Hit(Hit.MENU, mx, y + 2, DiskToolLayout.GD_MENU_W, DiskToolLayout.GD_MENU_W, 0L));
        if (disk == null) {
            Draw.text(g, font, GameText.resolve(DiskToolTexts.READING), rx, y + 6, c.dim());
        } else {
            this.details(g, font, rx, y, rw, disk, c);
            this.volumes(g, font, rx, y + DiskToolLayout.GD_BAR_Y, rw, disk, c);
            this.buttons(g, rx, y + DiskToolLayout.GD_BUTTONS_Y, c);
            this.chosen(g, font, rx, y + DiskToolLayout.GD_VOLUME_Y, disk, c);
        }
        final DiskReport.Refusal refusal = this.disks.refusal();
        if (refusal != null) {
            Draw.text(g, font, clip(font, GameText.resolve(DiskTexts.reason(refusal)), rw), rx,
                    y + height - 11, c.red());
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
                this.press(hit, (int) mouseX, (int) mouseY);
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

    private void press(final Hit hit, final int mouseX, final int mouseY) {
        final DiskReport.Disk disk = this.disks.report().disk(this.device);
        final DiskReport.Part part = disk == null ? null : partAt(disk, this.chosenStart);
        switch (hit.kind()) {
            case Hit.DISK -> {
                this.device = (int) hit.value();
                this.chosenStart = -1L;
            }
            case Hit.SPOT -> this.chosenStart = hit.value();
            case Hit.MENU -> {
                if (disk != null) {
                    this.open(List.of(item(DiskToolTexts.GD_FORMAT_DISK, true, () -> this.formatDisk(disk))),
                            mouseX, mouseY);
                }
            }
            case Hit.BUTTON -> {
                if (disk == null) {
                    return;
                }
                if (hit.value() == BUTTON_ADD && part == null && this.chosenStart >= 0L) {
                    this.create(disk, this.chosenStart);
                } else if (hit.value() == BUTTON_REMOVE && part != null && !part.mounted()) {
                    this.deleteAsked(disk, part);
                } else if (hit.value() == BUTTON_GEAR && part != null) {
                    this.open(List.of(item(DiskToolTexts.GD_FORMAT_PARTITION, !part.mounted(),
                                    () -> this.format(disk, part)),
                            item(DiskToolTexts.GD_RESIZE, part.fileSystem() != null, () -> this.resize(disk, part))),
                            mouseX, mouseY);
                }
            }
            default -> {
            }
        }
    }

    private void sidebar(final GuiGraphics g, final Font font, final int x, final int y, final int height,
                         final DiskReport report, final Colours c) {
        g.fill(x, y, x + DiskToolLayout.GD_SIDE_W, y + height, c.side());
        int dy = y + 4;
        for (final DiskReport.Disk disk : report.disks()) {
            if (disk.index() == this.device) {
                g.fill(x, dy - 2, x + DiskToolLayout.GD_SIDE_W, dy + DiskToolLayout.GD_ENTRY_H - 4, c.chosen());
            }
            g.fill(x + 4, dy + 3, x + 16, dy + 11, c.dim());
            g.fill(x + 13, dy + 8, x + 15, dy + 10, c.blue());
            Draw.text(g, font, clip(font, GameText.resolve(DiskToolTexts.GD_DISK.with(tb(disk.sizeMb()))),
                    DiskToolLayout.GD_SIDE_W - 22), x + 20, dy, c.ink());
            Draw.text(g, font, clip(font, disk.model(), DiskToolLayout.GD_SIDE_W - 22), x + 20, dy + 9, c.dim());
            this.hits.add(new Hit(Hit.DISK, x, dy - 2, DiskToolLayout.GD_SIDE_W, DiskToolLayout.GD_ENTRY_H - 2,
                    disk.index()));
            dy += DiskToolLayout.GD_ENTRY_H;
        }
    }

    /* The disk's size and kind, its device, and its model, serial, size and partitioning. */
    private void details(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                         final DiskReport.Disk disk, final Colours c) {
        final String head = GameText.resolve(DiskToolTexts.GD_HARD_DISK.with(tb(disk.sizeMb())));
        Draw.text(g, font, head, x + (width - font.width(head)) / 2, y + 4, c.ink());
        final String dev = device(disk);
        Draw.text(g, font, dev, x + (width - font.width(dev)) / 2, y + 13, c.dim());
        // The rule that parts the disk's name from its details.
        g.fill(x, y + 23, x + width, y + 24, c.chosen());
        final int lx = x + width / 3;
        final String[][] rows = {
                {GameText.resolve(DiskToolTexts.GD_MODEL), disk.model()},
                {GameText.resolve(DiskToolTexts.GD_SERIAL), serial(disk)},
                {GameText.resolve(DiskToolTexts.GD_SIZE), tb(disk.sizeMb()) + " ("
                        + String.format(Locale.ROOT, "%,d", disk.sizeMb() * BYTES_PER_MB) + " bytes)"},
                {GameText.resolve(DiskToolTexts.GD_PARTITIONING), GameText.resolve(disk.scheme().displayName())},
        };
        int ry = y + DiskToolLayout.GD_DETAILS_Y;
        for (final String[] row : rows) {
            Draw.text(g, font, row[0], lx - 4 - font.width(row[0]), ry, c.dim());
            Draw.text(g, font, clip(font, row[1], x + width - lx), lx, ry, c.ink());
            ry += DiskToolLayout.ROW;
        }
        if (disk.scheme() == PartitionScheme.MBR && disk.sizeMb() > PartitionScheme.MBR.reachOf(disk.sizeMb())) {
            Draw.text(g, font, clip(font, GameText.resolve(DiskToolTexts.GD_MBR_REACH.with(
                    tb(PartitionScheme.MBR.reachOf(disk.sizeMb())))), x + width - lx), lx, ry, c.red());
        }
        Draw.text(g, font, GameText.resolve(DiskToolTexts.GD_VOLUMES), x, y + DiskToolLayout.GD_VOLUMES_Y, c.ink());
    }

    /* The volumes: each partition and free space across the width, and the part an MBR table cannot reach. */
    private void volumes(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                         final DiskReport.Disk disk, final Colours c) {
        final List<long[]> spans = new ArrayList<>();
        for (final DiskReport.Part part : disk.parts()) {
            spans.add(new long[]{part.startMb(), part.sizeMb(), part.number()});
        }
        for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
            spans.add(new long[]{gap.startMb(), gap.sizeMb(), -1L});
        }
        final long reach = disk.layout().reachMb();
        if (reach < disk.sizeMb()) {
            spans.add(new long[]{reach, disk.sizeMb() - reach, -2L});
        }
        spans.sort((a, b) -> Long.compare(a[0], b[0]));
        final int h = DiskToolLayout.GD_BAR_H;
        int bx = x;
        for (int i = 0; i < spans.size(); i++) {
            final long[] span = spans.get(i);
            final int w = i == spans.size() - 1 ? x + width - bx
                    : Math.max(24, (int) (width * span[1] / Math.max(1L, disk.sizeMb())));
            final boolean here = span[0] == this.chosenStart;
            final DiskReport.Part part = span[2] < 0 ? null : disk.part((int) span[2]);
            g.fill(bx, y, bx + w, y + h, span[2] == -2L ? c.side() : here ? c.chosen() : c.paper());
            Draw.outline(g, bx, y, w, h, here ? c.blue() : c.dim());
            if (span[2] == -2L) {
                // The hatch of what the table cannot reach, one short stroke at a time.
                for (int line = 0; line < w; line += 6) {
                    g.fill(bx + line, y + h - 3 - line % h, bx + Math.min(w, line + 3), y + h - 2 - line % h,
                            c.hatch());
                }
            }
            final String name = span[2] == -2L ? GameText.resolve(DiskToolTexts.GD_BEYOND)
                    : part == null ? GameText.resolve(DiskToolTexts.GD_FREE_SPACE)
                    : !part.label().isEmpty() ? part.label()
                    : GameText.resolve(DiskToolTexts.GD_PARTITION.with(part.number()));
            final String sized = tb(span[1]) + (part == null || part.fileSystem() == null ? ""
                    : " " + part.fileSystem().displayName());
            Draw.text(g, font, clip(font, name, w - 4), bx + Math.max(2, (w - font.width(name)) / 2), y + 9,
                    span[2] == -2L ? c.dim() : c.ink());
            Draw.text(g, font, clip(font, sized, w - 4), bx + Math.max(2, (w - font.width(sized)) / 2), y + 19,
                    c.dim());
            if (span[2] != -2L) {
                this.hits.add(new Hit(Hit.SPOT, bx, y, w, h, span[0]));
            }
            bx += w;
        }
    }

    private void buttons(final GuiGraphics g, final int x, final int y, final Colours c) {
        final int size = DiskToolLayout.GD_BUTTON;
        for (int button = 0; button < 3; button++) {
            final int bx = x + button * (size + 2);
            Draw.outline(g, bx, y, size, size, c.dim());
            this.hits.add(new Hit(Hit.BUTTON, bx, y, size, size, button));
        }
        g.fill(x + 3, y + 6, x + 11, y + 8, c.ink());
        g.fill(x + 6, y + 3, x + 8, y + 11, c.ink());
        g.fill(x + size + 5, y + 6, x + size + 13, y + 8, c.ink());
        final int gx = x + 2 * (size + 2);
        Draw.outline(g, gx + 4, y + 4, 6, 6, c.ink());
        g.fill(gx + 6, y + 6, gx + 8, y + 8, c.ink());
    }

    /* The chosen volume's size, contents and device. */
    private void chosen(final GuiGraphics g, final Font font, final int x, final int y, final DiskReport.Disk disk,
                        final Colours c) {
        final DiskReport.Part part = partAt(disk, this.chosenStart);
        if (part == null) {
            return;
        }
        final String fs = part.fileSystem() == null ? GameText.resolve(DiskToolTexts.GD_UNKNOWN)
                : part.fileSystem().displayName();
        final String[][] rows = {
                {GameText.resolve(DiskToolTexts.GD_SIZE), tb(part.sizeMb())},
                {GameText.resolve(DiskToolTexts.GD_CONTENTS), GameText.resolve((part.mounted()
                        ? DiskToolTexts.GD_MOUNTED : DiskToolTexts.GD_NOT_MOUNTED).with(fs))},
                {GameText.resolve(DiskToolTexts.GD_DEVICE), device(disk) + part.number()},
        };
        int ry = y;
        for (final String[] row : rows) {
            Draw.text(g, font, row[0], x, ry, c.dim());
            Draw.text(g, font, row[1], x + 60, ry, c.ink());
            ry += DiskToolLayout.ROW;
        }
    }

    /* Format Disk: what it is erased with, and the table it gets, after the warning that it erases everything. */
    private void formatDisk(final DiskReport.Disk disk) {
        final int[] chosen = {0};
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.GD_FORMAT_DISK), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H)
                .info(() -> GameText.resolve(DiskToolTexts.GD_ERASE), () -> GameText.resolve(DiskToolTexts.GD_QUICK))
                .control(() -> GameText.resolve(DiskToolTexts.GD_PARTITIONING), new Dropdown(List.of(
                        () -> GameText.resolve(DiskToolTexts.GD_GPT), () -> GameText.resolve(DiskToolTexts.GD_MBR),
                        () -> GameText.resolve(DiskToolTexts.GD_NONE)),
                        () -> chosen[0], index -> chosen[0] = index, word -> word))
                .note(() -> GameText.resolve(DiskToolTexts.GD_ERASES), PALETTE.get().ink())
                .buttons(() -> GameText.resolve(DiskToolTexts.GD_FORMAT_BUTTON),
                        () -> this.disks.change(chosen[0] == 2 ? DiskCommand.erase(disk.index())
                                : DiskCommand.table(disk.index(), chosen[0] == 0 ? PartitionScheme.GPT
                                : PartitionScheme.MBR)),
                        () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* Create Partition: its size, its name and its type, in the free space chosen. */
    private void create(final DiskReport.Disk disk, final long start) {
        long room = 0L;
        for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
            if (gap.startMb() == start) {
                room = gap.sizeMb();
            }
        }
        final long max = room;
        final TextField amount = new TextField(12).set(Long.toString(max));
        final TextField name = new TextField(32);
        final List<FileSystemType> types = types();
        final int[] chosen = {0};
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.GD_CREATE), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H)
                .control(() -> GameText.resolve(DiskToolTexts.GD_PARTITION_SIZE), amount)
                .control(() -> GameText.resolve(DiskToolTexts.GD_VOLUME_NAME), name)
                .control(() -> GameText.resolve(DiskToolTexts.GD_TYPE), choice(types, chosen))
                .buttons(() -> GameText.resolve(DiskToolTexts.GD_CREATE), () -> {
                    this.disks.change(DiskCommand.create(disk.index(), start, Math.min(max, number(amount.edit())),
                            types.get(chosen[0]).serializedName()));
                    this.chosenStart = start;
                }, () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* Format Partition: its name and its type; everything on it is lost. */
    private void format(final DiskReport.Disk disk, final DiskReport.Part part) {
        final TextField name = new TextField(32).set(part.label());
        final List<FileSystemType> types = types();
        final int[] chosen = {Math.max(0, types.indexOf(part.fileSystem()))};
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.GD_FORMAT_PARTITION), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H)
                .control(() -> GameText.resolve(DiskToolTexts.GD_VOLUME_NAME), name)
                .control(() -> GameText.resolve(DiskToolTexts.GD_TYPE), choice(types, chosen))
                .note(() -> GameText.resolve(DiskToolTexts.GD_DELETE_ASK), PALETTE.get().red())
                .buttons(() -> GameText.resolve(DiskToolTexts.GD_FORMAT_BUTTON), () -> {
                    this.disks.change(DiskCommand.format(disk.index(), part.number(),
                            types.get(chosen[0]).serializedName()));
                    this.disks.change(DiskCommand.label(disk.index(), part.number(), name.edit()));
                }, () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    /* Resize: its current, smallest and largest size, and the size asked. */
    private void resize(final DiskReport.Disk disk, final DiskReport.Part part) {
        final long largest = part.sizeMb() + disk.layout().roomAfter(part.entry());
        final TextField amount = new TextField(12).set(Long.toString(part.sizeMb()));
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.GD_RESIZE), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H)
                .info(() -> GameText.resolve(DiskToolTexts.GD_CURRENT_SIZE), () -> tb(part.sizeMb()))
                .info(() -> GameText.resolve(DiskToolTexts.GD_MINIMAL_SIZE), () -> tb(part.usedMb()))
                .info(() -> GameText.resolve(DiskToolTexts.GD_MAXIMAL_SIZE), () -> tb(largest))
                .control(() -> GameText.resolve(DiskToolTexts.GD_NEW_SIZE), amount)
                .buttons(() -> GameText.resolve(DiskToolTexts.GD_RESIZE), () -> this.disks.change(
                        DiskCommand.resize(disk.index(), part.number(), Math.min(largest, number(amount.edit())))),
                        () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    private void deleteAsked(final DiskReport.Disk disk, final DiskReport.Part part) {
        this.form = new DiskForm(() -> GameText.resolve(DiskToolTexts.GD_DELETE), DiskToolLayout.FORM_W,
                DiskToolLayout.FORM_H / 2 + 20)
                .note(() -> GameText.resolve(DiskToolTexts.GD_DELETE_ASK), PALETTE.get().ink())
                .buttons(() -> GameText.resolve(DiskToolTexts.GD_DELETE), () -> {
                    this.disks.change(DiskCommand.delete(disk.index(), part.number()));
                    this.chosenStart = -1L;
                }, () -> GameText.resolve(DiskToolTexts.CANCEL));
        this.form.open();
    }

    private void open(final List<ContextMenu.Item> items, final int mouseX, final int mouseY) {
        this.menu.open(items, mouseX, mouseY, this.content[0], this.content[1], this.content[2], this.content[3]);
    }

    private static ContextMenu.Item item(final TextKey label, final boolean enabled, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), enabled, action);
    }

    private static List<FileSystemType> types() {
        return List.of(FileSystemType.EXT4, FileSystemType.NTFS, FileSystemType.FAT32, FileSystemType.EXFAT,
                FileSystemType.BTRFS, FileSystemType.XFS, FileSystemType.SWAP);
    }

    private static Dropdown choice(final List<FileSystemType> types, final int[] chosen) {
        final List<Supplier<String>> names = new ArrayList<>();
        for (final FileSystemType type : types) {
            names.add(type::displayName);
        }
        return new Dropdown(names, () -> chosen[0], index -> chosen[0] = index, word -> word);
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

    private static String device(final DiskReport.Disk disk) {
        return "/dev/sd" + (char) ('a' + disk.index());
    }

    /* A serial number made from the model, the same every time the disk is shown. */
    private static String serial(final DiskReport.Disk disk) {
        final int hash = Math.abs((disk.model() + disk.index()).hashCode());
        return String.format(Locale.ROOT, "VK%d-%04d-%04X", disk.sizeMb() / 1_048_576L + 1, hash % 10_000,
                hash / 10_000 % 65_536);
    }

    /* A size as Disks writes it: TB, GB or MB to a tenth. */
    private static String tb(final long mb) {
        if (mb >= 1_048_576L) {
            return String.format(Locale.ROOT, "%.1f TB", mb / MB_PER_TB);
        }
        if (mb >= 1_024L) {
            return String.format(Locale.ROOT, "%.1f GB", mb / MB_PER_GB);
        }
        return mb + " MB";
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
     * Something a click lands on: a disk down the side, a volume, a button or the menu.
     *
     * @param value the disk's index, where the volume starts, or the button
     */
    private record Hit(int kind, int x, int y, int w, int h, long value) {

        static final int DISK = 0;
        static final int SPOT = 1;
        static final int BUTTON = 2;
        static final int MENU = 3;
    }

    /**
     * The colours of Disks: its paper and ink, its dim words, its side panel, the volume chosen, the blue of a
     * selection's edge, the red of a warning, and the hatch of what the table cannot reach.
     */
    private record Colours(int paper, int ink, int dim, int side, int chosen, int blue, int red, int hatch) {
    }
}
