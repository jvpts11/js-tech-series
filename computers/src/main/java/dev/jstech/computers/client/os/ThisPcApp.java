/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.ThisPcLayout;
import dev.jstech.computers.operation.payload.EjectMediaPayload;
import dev.jstech.computers.operation.payload.InstallFromMediaPayload;
import dev.jstech.computers.operation.payload.RenameVolumePayload;
import dev.jstech.computers.operation.payload.RequestThisPcPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.ThisPcPayload;
import dev.jstech.computers.operation.payload.ThisPcPayload.WireDisk;
import dev.jstech.computers.operation.payload.ThisPcPayload.WireMedia;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.MinSpecTooltip;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.CellGrid;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.ScrollPanel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The "This PC" desktop app, a system page: the machine itself in a card (name, kind, era, system,
 * network), then its devices and drives (each disk with a usage bar and its system, each linked drive
 * naming what is in it and, for an installer, what it would install, with Install, Open and Eject),
 * the hardware seated in it, and the programs installed on it as a grid. Everything is fetched from
 * the server and refreshes after an action. The geometry lives in {@link ThisPcLayout}.
 *
 * <p>The card and the page are components; the page is rebuilt from each listing the server sends,
 * a row per drive with its own buttons, and scrolls as one.
 */
@PaletteHolder
public final class ThisPcApp implements IDesktopApp {

    /** This PC's own colours, {@code jsc:app/this_pc}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/this_pc",
            new Colours(0xFF3F77C8, 0xFF5B9E5B, 0xFFD79A3A, 0xFF2E7D32, 0xFFB35C00, 0xFFD7DBE4,
                    0xFF2E3238, 0xFF1C1F24, 0xFF39D6C4, 0xFFEF6A5A, 0xFF3D434C, 0xFF1C1F24,
                    0xFF8B93A4, 0xFFC7CDDA, 0xFFEDF0F6, 0xFF49E07A, 0xFF5A6273,
                    0xFFEEF0F4, 0xFFC2C7D4,
                    0xFF2E3238, 0xFFB8BEC8, 0xFF1C1F24,
                    0xFF1C2438, 0xFFB8BEC8, 0xFF0B1220,
                    0xFFB9C0CE, 0xFFEDF0F6, 0xFF6E7686));
    private static final int RENAME_W = 34;
    private static final int NAME_MAX = 32;
    private static final long DOUBLE_CLICK_MS = 300L;

    private OsSkin skin = OsSkin.fallback();

    private final BlockPos host;
    /** The window's name, which differs by platform: "This PC" on Frames, "Disks" on Linux. */
    private final String title;
    private ThisPcPayload data = new ThisPcPayload(ThisPcPayload.WireMachine.EMPTY, List.of(), List.of(), List.of());
    private int lastX;
    private int lastY;
    private int lastMouseX;
    private int lastMouseY;

    private long lastClickAt;
    private String lastClickKey = "";
    private String selectedKey = "";
    /** The volume the inline field renames, or empty while it renames the machine itself. */
    private String volumeRenameKey = "";

    private static ThisPcApp active;

    // components
    private final Panel root = new Panel();
    private final Label nameLabel;
    private final Label kindLabel;
    private final Label systemLabel;
    private final Button renameButton;
    private final TextField nameField;
    private final ScrollPanel page = new ScrollPanel().setStep(ThisPcLayout.KV_ROW_H);
    private final SectionHeader drivesHeader;
    private final Label noDrives;
    private final List<DriveRow> rows = new ArrayList<>();
    private final SectionHeader hardwareHeader;
    private final List<Label> hwKeys = new ArrayList<>();
    private final List<Label> hwValues = new ArrayList<>();
    private final SectionHeader programsHeader;
    private final Label noPrograms;
    private final CellGrid programGrid;

    /** A section's title band across the page. */
    private static final class SectionHeader extends UiComponent {

        private final Supplier<String> text;

        SectionHeader(final Supplier<String> text) {
            this.text = text;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            g.fill(x() + 1, y(), right() - 1, bottom(), ctx.skin().listHover());
            g.fill(x() + 1, bottom() - 1, right() - 1, bottom(), ctx.skin().edge());
            g.drawString(ctx.font(), text.get(), x() + 4, y() + 2, ctx.skin().dim(), false);
        }
    }

    /**
     * A disk or a linked drive on the page: two lines of text, the usage bar of a disk, and the buttons
     * that act on it. A click selects it and a double-click opens it in the explorer.
     */
    private final class DriveRow extends Panel {

        private final String key;
        @Nullable
        private final WireDisk disk;
        @Nullable
        private final WireMedia media;
        private final int letterIndex;
        private final List<Button> buttons = new ArrayList<>();
        @Nullable
        private Button install;
        @Nullable
        private final Runnable open;

        DriveRow(final WireDisk disk, final int letterIndex) {
            this.key = "disk:" + disk.slot();
            this.disk = disk;
            this.media = null;
            this.letterIndex = letterIndex;
            this.open = () -> DesktopScreen.requestOpenFiles("");
            buttons.add(add(new Button(GameText.resolve(ThisPcTexts.OPEN), open)));
        }

        DriveRow(final WireMedia media, final int letterIndex) {
            this.key = "media:" + media.readerPos();
            this.disk = null;
            this.media = media;
            this.letterIndex = letterIndex;
            this.open = media.loaded() ? () -> DesktopScreen.requestOpenFiles(key) : null;
            if (media.installable()) {
                install = add(new Button(GameText.resolve(ThisPcTexts.INSTALL), () -> install(media.readerPos()))
                        .setPrimary(true));
                buttons.add(install);
            }
            if (media.loaded()) {
                buttons.add(add(new Button(GameText.resolve(ThisPcTexts.OPEN), open)));
                buttons.add(add(new Button(GameText.resolve(ThisPcTexts.EJECT), () -> eject(media.readerPos()))));
            }
        }

        /** The row across the content at {@code y}; the buttons keep to the right edge. */
        void layout(final int contentX, final int y, final int width) {
            setBounds(contentX + 1, y, width - 2, ThisPcLayout.DRIVE_ROW_H);
            final int by = y + (ThisPcLayout.DRIVE_ROW_H - ThisPcLayout.BTN_H) / 2;
            for (int i = 0; i < buttons.size(); i++) {
                buttons.get(i).setBounds(contentX + ThisPcLayout.buttonX(width, i, buttons.size()), by,
                        ThisPcLayout.BTN_W, ThisPcLayout.BTN_H);
            }
        }

        private String drive() {
            return linux() ? "" : (char) ('C' + letterIndex) + ":";
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final Font font = ctx.font();
            final boolean sel = key.equals(selectedKey);
            ctx.skin().listRow(g, x(), y(), width(), height(), hovered(ctx), sel);
            final int cx = x() - 1;
            final int cy = y();
            final int width = width() + 2;
            final int tx = cx + ThisPcLayout.TEXT_X;
            final int maxW = ThisPcLayout.driveTextMaxW(width, buttons.size());
            final int textColor = ctx.skin().listRowText(sel);
            if (disk != null) {
                drawDiskIcon(g, cx + 4, cy + 5);
                final String label = GameText.resolve(disk.label()) + (drive().isEmpty() ? "" : "  " + drive());
                g.drawString(font, Texts.trim(font, label, maxW), tx, cy + 2, textColor, false);
                if (disk.system()) {
                    final String badge = GameText.resolve(disk.osPath().isEmpty() ? ThisPcTexts.SYSTEM.text()
                            : ThisPcTexts.SYSTEM_IS.with(prettyOs(disk.osPath())));
                    final int bw = font.width(badge);
                    if (font.width(label) + 6 + bw <= maxW) {
                        g.drawString(font, badge, tx + maxW - bw, cy + 2, PALETTE.get().good(), false);
                    }
                }
                /*
                 * The bar is segmented by what actually takes the space, so "the disk is full" always
                 * comes with "of what": the system, the items stored on it, or its files.
                 */
                final int barW = maxW - 70;
                final Colours c = PALETTE.get();
                g.fill(tx, cy + 12, tx + barW, cy + 15, c.barTrack());
                final long cap = Math.max(1, disk.capItems());
                int segX = tx;
                for (final long[] part : new long[][] {{disk.osItems(), c.segmentSystem()},
                    {disk.storeItems(), c.segmentStored()}, {disk.fileItems(), c.segmentFiles()}}) {
                    final int w = (int) Math.min(tx + barW - segX, barW * part[0] / cap);
                    if (w > 0) {
                        g.fill(segX, cy + 12, segX + w, cy + 15, (int) part[1]);
                        segX += w;
                    }
                }
                final String usage = GameText.resolve(ThisPcTexts.FREE.with(disk.freeItems(), disk.capItems()));
                g.drawString(font, usage, tx + maxW - font.width(usage), cy + 11, ctx.skin().dim(), false);
            } else if (media != null) {
                drawMediaIcon(g, cx + 4, cy + 5, media);
                final String head = prettyDrive(media.drive()) + (drive().isEmpty() ? "" : "  " + drive()) + "   "
                        + GameText.resolve(media.loaded() ? media.mediaName() : ThisPcTexts.NO_DISC.text());
                g.drawString(font, Texts.trim(font, head, maxW), tx, cy + 2, media.loaded() ? textColor : ctx.skin().dim(), false);
                final String detail;
                int detailColor = ctx.skin().dim();
                if (!media.loaded()) {
                    detail = GameText.resolve(ThisPcTexts.DRIVE_AWAY.with(prettyDrive(media.drive()),
                            media.blocksAway()));
                } else if (media.kind().equals(MediaKind.OS_INSTALL.serializedName())) {
                    detail = installLine(media, ThisPcTexts.INSTALLS, true);
                    detailColor = PALETTE.get().warn();
                } else if (media.kind().equals(MediaKind.PROGRAM_INSTALL.serializedName())) {
                    detail = installLine(media, media.installable() ? ThisPcTexts.INSTALLS : ThisPcTexts.INSTALLED,
                            false);
                    detailColor = media.installable() ? PALETTE.get().good() : ctx.skin().dim();
                } else {
                    detail = GameText.resolve(ThisPcTexts.DATA_MEDIUM.with(media.stored()));
                }
                g.drawString(font, Texts.trim(font, detail, maxW), tx, cy + 11, detailColor, false);
            }
            super.render(g, ctx);
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (super.mouseClicked(mx, my, button)) {
                return true; // a button on the row
            }
            final long now = System.currentTimeMillis();
            if (key.equals(lastClickKey) && now - lastClickAt < DOUBLE_CLICK_MS && open != null) {
                open.run();
            }
            lastClickKey = key;
            lastClickAt = now;
            selectedKey = key;
            return true;
        }

        @Override
        public List<Component> tooltip(final double mx, final double my) {
            final List<Component> onButton = super.tooltip(mx, my);
            if (!onButton.isEmpty()) {
                return onButton;
            }
            if (media != null) {
                // The requirements ride on the tooltip of the detail line; the row has room for one line.
                if (!media.needs().isEmpty() && media.loaded() && !media.kind().equals(MediaKind.DATA.serializedName()) && my >= y() + 11 && my < y() + 20) {
                    final List<Component> lines = new ArrayList<>(media.needs().size() + 1);
                    lines.add(Component.literal(media.payloadName().isEmpty() ? GameText.resolve(media.mediaName())
                            : media.payloadName()));
                    for (final Text need : media.needs()) {
                        lines.add(line(need, ChatFormatting.DARK_GRAY));
                    }
                    return lines;
                }
                return List.of();
            }
            if (disk != null) {
                final List<Component> lines = new ArrayList<>(6);
                lines.add(Component.literal(GameText.resolve(disk.label())));
                lines.add(line(disk.osPath().isEmpty() ? ThisPcTexts.NO_SYSTEM.text()
                        : ThisPcTexts.SYSTEM_NAMED.with(prettyOs(disk.osPath())),
                        disk.osPath().isEmpty() ? ChatFormatting.GRAY : ChatFormatting.AQUA));
                lines.add(line(ThisPcTexts.SHARE_SYSTEM.with(disk.osItems()), ChatFormatting.BLUE));
                lines.add(line(ThisPcTexts.SHARE_ITEMS.with(disk.storeItems()), ChatFormatting.GREEN));
                lines.add(line(ThisPcTexts.SHARE_FILES.with(disk.fileItems()), ChatFormatting.GOLD));
                lines.add(line(ThisPcTexts.SHARE_FREE.with(disk.freeItems()), ChatFormatting.DARK_GRAY));
                return lines;
            }
            return List.of();
        }
    }

    public ThisPcApp(final BlockPos host) {
        this(host, GameText.resolve(ThisPcTexts.TITLE));
    }

    public ThisPcApp(final BlockPos host, final String title) {
        this.host = host;
        this.title = title;

        nameLabel = root.add(new Label(this::machineName));
        kindLabel = root.add(new Label(this::kindLine, Label.Tone.DIM));
        systemLabel = root.add(new Label(this::systemLine)
                .setColor(() -> data.machine().osLabel().isEmpty() ? PALETTE.get().warn() : 0)
                .setTone(Label.Tone.DIM));
        renameButton = root.add(new Button(GameText.resolve(ThisPcTexts.RENAME), this::startRename));
        root.add(page);
        drivesHeader = page.add(new SectionHeader(() -> GameText.resolve(ThisPcTexts.DEVICES_AND_DRIVES)));
        noDrives = page.add(new Label(GameText.resolve(ThisPcTexts.NO_DRIVES), Label.Tone.DIM));
        hardwareHeader = page.add(new SectionHeader(() -> GameText.resolve(ThisPcTexts.HARDWARE)));
        final TextKey[] keys = {ThisPcTexts.BOARD, ThisPcTexts.PROCESSOR, ThisPcTexts.ARCHITECTURE,
            ThisPcTexts.MEMORY, ThisPcTexts.GRAPHICS, ThisPcTexts.POWER, ThisPcTexts.PERIPHERALS, ThisPcTexts.BUILD};
        for (int i = 0; i < keys.length; i++) {
            final int line = i;
            hwKeys.add(page.add(new Label(GameText.resolve(keys[i]), Label.Tone.DIM)));
            hwValues.add(page.add(new Label(() -> GameText.resolve(hardwareValue(line)))
                    .setColor(() -> line == keys.length - 1
                            ? (data.machine().buildValid() ? PALETTE.get().good() : PALETTE.get().warn()) : 0)));
        }
        programsHeader = page.add(new SectionHeader(() -> GameText.resolve(ThisPcTexts.INSTALLED_PROGRAMS.with(
                data.installedPrograms().size()))));
        noPrograms = page.add(new Label(GameText.resolve(ThisPcTexts.NO_PROGRAMS), Label.Tone.DIM));
        programGrid = page.add(new CellGrid(1, 1, 1, ThisPcLayout.PROG_CELL_W, ThisPcLayout.PROG_CELL_H)
                .setWells(false)
                .setInset(2)
                .setSelected(i -> i < data.installedPrograms().size() && ("program:" + data.installedPrograms().get(i)).equals(selectedKey))
                .setRenderer(this::renderProgramCell)
                .setTooltip(this::programTooltip)
                .setOnClick((index, button, shift) -> selectedKey = "program:" + data.installedPrograms().get(index)));
        // The inline field sits over the card's name; it is added last so it stays in front.
        nameField = root.add(new TextField(NAME_MAX).setOnCommit(this::commitRename));
        nameField.setVisible(false);

        active = this;
        request();
    }

    /** Routes a "This PC" listing reply to the open window. */
    public static void accept(final ThisPcPayload payload) {
        if (active != null) {
            active.data = payload;
            active.rebuildRows();
        }
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    private void request() {
        PacketDistributor.sendToServer(new RequestThisPcPayload(host));
    }

    @Override
    public void onRestored() {
        active = this;
        request();
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public int defaultWidth() {
        return ThisPcLayout.DEFAULT_W;
    }

    @Override
    public int defaultHeight() {
        return ThisPcLayout.DEFAULT_H;
    }

    @Override
    public int minWidth() {
        return ThisPcLayout.MIN_W;
    }

    @Override
    public int minHeight() {
        return ThisPcLayout.MIN_H;
    }

    private boolean linux() {
        return skin.unixLike();
    }

    // state readers

    private String machineName() {
        final ThisPcPayload.WireMachine m = data.machine();
        return m.name().isEmpty() ? GameText.resolve(m.kind()) : m.name();
    }

    private String kindLine() {
        final ThisPcPayload.WireMachine m = data.machine();
        return GameText.resolve(m.name().isEmpty() ? ThisPcTexts.ERA.with(m.era())
                : ThisPcTexts.KIND_AND_ERA.with(m.kind(), m.era()));
    }

    private String systemLine() {
        final ThisPcPayload.WireMachine m = data.machine();
        final String system = m.osLabel().isEmpty() ? GameText.resolve(ThisPcTexts.NO_SYSTEM)
                : m.osLabel() + " · " + Branding.houseOf(m.osLabel()).name() + " " + m.osYear();
        final String net = GameText.resolve(m.networkLabel().isEmpty() ? ThisPcTexts.NOT_ON_NETWORK.text()
                : ThisPcTexts.ON_NETWORK.with(m.networkLabel()));
        return system + " · " + net;
    }

    private Text hardwareValue(final int line) {
        final ThisPcPayload.WireMachine m = data.machine();
        final Text none = ThisPcTexts.NONE.text();
        return switch (line) {
            case 0 -> m.boardLabel().isEmpty() ? none : m.boardLabel();
            case 1 -> m.cpuLabel().isEmpty() ? none
                    : m.cpuCount() > 1 ? ThisPcPayload.COUNTED.with(m.cpuCount(), m.cpuLabel()) : m.cpuLabel();
            case 2 -> m.cpuArch().isEmpty() ? none : m.cpuArch();
            case 3 -> m.ramMb() > 0 ? ThisPcTexts.MEMORY_VALUE.with(m.ramMb()) : none;
            case 4 -> m.gpuCount() > 0 ? ThisPcTexts.GRAPHICS_VALUE.with(m.gpuCount(), m.vramMb()) : none;
            case 5 -> m.psuLabel().isEmpty() ? none : m.psuLabel();
            case 6 -> m.peripherals().isEmpty() ? ThisPcTexts.NONE_LINKED.text() : m.peripherals();
            default -> (m.buildValid() ? ThisPcTexts.COMES_UP : ThisPcTexts.NOT_VALID).text();
        };
    }

    /* One line of a tooltip, in the player's language and the colour of what it counts. */
    private static Component line(final Text text, final ChatFormatting colour) {
        return Component.literal(GameText.resolve(text)).withStyle(colour);
    }

    /*
     * What an installer's row says under its name: what it installs, the year, whether a machine boots from it, and
     * the id a package manager knows it by, each said only when there is something to say.
     */
    private static String installLine(final WireMedia media, final TextKey verb, final boolean bootable) {
        final List<String> parts = new ArrayList<>();
        parts.add(GameText.resolve(verb.with(media.payloadName().isEmpty() ? media.payloadPath()
                : media.payloadName())));
        if (media.payloadYear() > 0) {
            parts.add(String.valueOf(media.payloadYear()));
        }
        if (bootable) {
            parts.add(GameText.resolve(ThisPcTexts.BOOTABLE));
        }
        if (!media.packageId().isEmpty()) {
            parts.add(GameText.resolve(ThisPcTexts.PACKAGE.with(media.packageId())));
        }
        return String.join(" · ", parts);
    }

    /** Rebuilds the drive rows from the listing: one row per disk, then one per linked drive. */
    private void rebuildRows() {
        for (final DriveRow row : rows) {
            page.remove(row);
        }
        rows.clear();
        int letter = 0;
        for (final WireDisk d : data.disks()) {
            rows.add(page.add(new DriveRow(d, letter++)));
        }
        for (final WireMedia m : data.media()) {
            rows.add(page.add(new DriveRow(m, letter++)));
        }
    }

    // rendering

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        active = this;
        lastX = x;
        lastY = y;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layout(x, y, width, height);

        // The machine's icon: a tower with a lit power dot.
        final ThisPcPayload.WireMachine m = data.machine();
        final int ix = x + 4;
        final int iy = y + 4;
        final Colours c = PALETTE.get();
        g.fill(ix, iy, ix + ThisPcLayout.CARD_ICON_W, iy + ThisPcLayout.CARD_H - 8, c.towerBody());
        g.fill(ix + 2, iy + 2, ix + ThisPcLayout.CARD_ICON_W - 2, iy + 7, c.towerBay());
        g.fill(ix + ThisPcLayout.CARD_ICON_W - 6, iy + 3, ix + ThisPcLayout.CARD_ICON_W - 4, iy + 5,
                m.buildValid() ? c.powerOn() : c.powerOff());
        g.fill(ix + 3, iy + 10, ix + ThisPcLayout.CARD_ICON_W - 3, iy + ThisPcLayout.CARD_H - 11, c.towerFace());
        Draw.outline(g, ix, iy, ThisPcLayout.CARD_ICON_W, ThisPcLayout.CARD_H - 8, c.towerEdge());
        g.fill(x, page.y() - 1, x + width, page.y(), skin.edge());

        root.render(g, ctx);
    }

    /** Places the card and lays the page out top to bottom, telling it how tall the whole content is. */
    private void layout(final int x, final int y, final int width, final int height) {
        final int tx = x + 4 + ThisPcLayout.CARD_ICON_W + 4;
        final int textMax = width - (tx - x) - RENAME_W - 8;
        final boolean renaming = nameField.isFocused();
        nameLabel.setVisible(!renaming);
        nameLabel.setBounds(tx, y + 3, textMax, 8);
        nameField.setVisible(renaming);
        nameField.setBounds(tx - 3, y + 1, textMax + 6, 11);
        kindLabel.setBounds(tx, y + 12, textMax, 8);
        systemLabel.setBounds(tx, y + 21, textMax, 8);
        renameButton.setBounds(x + width - RENAME_W - 3, y + 4, RENAME_W, ThisPcLayout.BTN_H);

        page.setBounds(x, y + ThisPcLayout.pageY(), width, ThisPcLayout.pageH(height));
        int cy = 2;
        drivesHeader.setBounds(x, page.contentY(cy), width, ThisPcLayout.HEADER_H);
        cy += ThisPcLayout.HEADER_H + 1;
        noDrives.setVisible(rows.isEmpty());
        if (rows.isEmpty()) {
            noDrives.setBounds(x + 8, page.contentY(cy + 3), width - 16, 8);
            cy += ThisPcLayout.KV_ROW_H + 4;
        } else {
            for (final DriveRow row : rows) {
                row.layout(x, page.contentY(cy), width);
                cy += ThisPcLayout.DRIVE_ROW_H;
            }
            cy += 2;
        }
        hardwareHeader.setBounds(x, page.contentY(cy), width, ThisPcLayout.HEADER_H);
        cy += ThisPcLayout.HEADER_H + 1;
        final int keyW = 52;
        for (int i = 0; i < hwKeys.size(); i++) {
            hwKeys.get(i).setBounds(x + 8, page.contentY(cy + 1), keyW, 8);
            hwValues.get(i).setBounds(x + 8 + keyW, page.contentY(cy + 1), width - 8 - keyW - 8, 8);
            cy += ThisPcLayout.KV_ROW_H;
        }
        cy += 2;
        programsHeader.setBounds(x, page.contentY(cy), width, ThisPcLayout.HEADER_H);
        cy += ThisPcLayout.HEADER_H + 1;
        final List<String> programs = data.installedPrograms();
        noPrograms.setVisible(programs.isEmpty());
        programGrid.setVisible(!programs.isEmpty());
        if (programs.isEmpty()) {
            noPrograms.setBounds(x + 8, page.contentY(cy + 3), width - 16, 8);
            cy += ThisPcLayout.KV_ROW_H + 4;
        } else {
            final int columns = ThisPcLayout.programColumns(width);
            final int gridRows = (programs.size() + columns - 1) / columns;
            programGrid.setColumns(columns).setVisibleRows(gridRows).setTotalRows(gridRows).setCellCount(programs.size())
                    .place(x + ThisPcLayout.programCellX(width, 0), page.contentY(cy));
            cy += gridRows * ThisPcLayout.PROG_CELL_H + 4;
        }
        page.setContentHeight(cy);
    }

    private void renderProgramCell(final GuiGraphics g, final UiContext ctx, final int index, final int cx, final int cy,
                                   final int w, final int h, final boolean hovered) {
        final List<String> programs = data.installedPrograms();
        if (index >= programs.size()) {
            return;
        }
        final String id = programs.get(index);
        final Font font = ctx.font();
        final boolean sel = ("program:" + id).equals(selectedKey);
        ProgramIcons.draw(g, cx + w / 2 - 6, cy + 2, 12, 12, programIdOf(id), skin.osPath());
        final String name = Texts.trim(font, prettyProgram(id), w - 4);
        g.drawString(font, name, cx + (w - font.width(name)) / 2, cy + 16, ctx.skin().listRowText(sel), false);
        final String pkg = Texts.trim(font, packageIdOf(id), w - 4);
        g.drawString(font, pkg, cx + (w - font.width(pkg)) / 2, cy + 24, ctx.skin().dim(), false);
    }

    private List<Component> programTooltip(final int index) {
        final List<String> programs = data.installedPrograms();
        if (index >= programs.size()) {
            return List.of();
        }
        final String id = programs.get(index);
        final List<Component> spec = MinSpecTooltip.programMinSpec(rl(id));
        final List<Component> lines = new ArrayList<>(spec.size() + 2);
        lines.add(Component.literal(prettyProgram(id)));
        final ProgramSpec program = OsRegistry.getProgram(rl(id));
        if (program != null) {
            lines.add(Component.translatable(program.descriptionKey()).withStyle(ChatFormatting.GRAY));
        }
        lines.addAll(spec);
        return lines;
    }

    @Override
    public void renderTooltip(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY) {
        final List<Component> lines = root.tooltip(mouseX, mouseY);
        if (!lines.isEmpty()) {
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private static void drawDiskIcon(final GuiGraphics g, final int x, final int y) {
        final Colours c = PALETTE.get();
        g.fill(x, y, x + 14, y + 10, c.diskFrame());
        g.fill(x + 1, y + 1, x + 13, y + 9, c.diskFace());
        g.fill(x + 2, y + 2, x + 12, y + 4, c.diskLight());
        g.fill(x + 9, y + 6, x + 11, y + 8, c.diskLed());
        Draw.outline(g, x, y, 14, 10, c.diskEdge());
    }

    private static void drawMediaIcon(final GuiGraphics g, final int x, final int y, final WireMedia m) {
        final Colours c = PALETTE.get();
        if (!m.loaded()) {
            g.fill(x + 2, y, x + 12, y + 10, c.emptyFace());
            Draw.outline(g, x + 2, y, 10, 10, c.emptyEdge());
            return;
        }
        final MediaDriveType drive = MediaDriveType.find(m.drive());
        if (drive == MediaDriveType.DOCK_STATION) {
            g.fill(x + 1, y + 1, x + 13, y + 9, c.dockBody());
            g.fill(x + 9, y + 3, x + 12, y + 7, c.dockLight());
            Draw.outline(g, x + 1, y + 1, 12, 8, c.dockEdge());
        } else if (drive == MediaDriveType.FLOPPY_DRIVE) {
            g.fill(x + 1, y, x + 13, y + 10, c.floppyBody());
            g.fill(x + 4, y + 1, x + 10, y + 4, c.floppyShutter());
            Draw.outline(g, x + 1, y, 12, 10, c.floppyEdge());
        } else {
            g.fill(x + 2, y, x + 12, y + 10, c.discBody());
            g.fill(x + 5, y + 3, x + 9, y + 7, c.discHub());
            Draw.outline(g, x + 2, y, 10, 10, c.discEdge());
        }
    }

    /**
     * This PC's colours: the three kinds of use on a disk's bar, what reads as fine and what as a warning, the bar's
     * empty track, the machine's tower on the card, and the pictures of a disk, an empty drive, a dock, a floppy and
     * a disc.
     */
    private record Colours(int segmentSystem, int segmentStored, int segmentFiles, int good, int warn, int barTrack,
                           int towerBody, int towerBay, int powerOn, int powerOff, int towerFace, int towerEdge,
                           int diskFrame, int diskFace, int diskLight, int diskLed, int diskEdge,
                           int emptyFace, int emptyEdge,
                           int dockBody, int dockLight, int dockEdge,
                           int floppyBody, int floppyShutter, int floppyEdge,
                           int discBody, int discHub, int discEdge) {
    }

    // inspection (client tests; points are content-local, i.e. relative to window.x()+4 / window.y()+18)

    /** The index of the first drive whose medium name contains {@code nameContains}, or -1. */
    public int mediaRowIndex(final String nameContains) {
        for (int i = 0; i < data.media().size(); i++) {
            if (GameText.resolve(data.media().get(i).mediaName()).contains(nameContains)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isInstallable(final int mediaIndex) {
        return mediaIndex >= 0 && mediaIndex < data.media().size() && data.media().get(mediaIndex).installable();
    }

    /** Content-local centre of the "Install" button of drive {@code mediaIndex}, from the last render. */
    public int[] installButtonCenter(final int mediaIndex) {
        if (mediaIndex < 0 || mediaIndex >= data.media().size()) {
            return new int[] {0, 0};
        }
        final long pos = data.media().get(mediaIndex).readerPos();
        for (final DriveRow row : rows) {
            if (row.media != null && row.media.readerPos() == pos && row.install != null) {
                final int[] c = row.install.center();
                return new int[] {c[0] - lastX, c[1] - lastY};
            }
        }
        return new int[] {0, 0};
    }

    // input

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (!root.mouseClicked(mouseX, mouseY, button)) {
            selectedKey = "";
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        // The wheel anywhere in the window moves the page.
        return page.mouseScrolled(lastMouseX, lastMouseY, delta);
    }

    private void install(final long readerPos) {
        PacketDistributor.sendToServer(new InstallFromMediaPayload(host, readerPos));
        request();
        /*
         * The install lands server-side before this refresh is processed, so the desktop's
         * launchers pick the new program up immediately.
         */
        DesktopScreen.refreshActive();
    }

    private void eject(final long readerPos) {
        PacketDistributor.sendToServer(new EjectMediaPayload(host, readerPos));
        request();
    }

    private void startRename() {
        volumeRenameKey = "";
        nameField.set(data.machine().name());
        root.focus(nameField);
    }

    /** A disk or medium is relabelled through the same volume rename the explorer uses, typed in the card's field. */
    private void startVolumeRename() {
        String label = "";
        if (selectedKey.startsWith("disk:")) {
            final int slot = Integer.parseInt(selectedKey.substring(5));
            for (final WireDisk d : data.disks()) {
                if (d.slot() == slot) {
                    label = GameText.resolve(d.label());
                }
            }
        } else {
            final long pos = Long.parseLong(selectedKey.substring(6));
            for (final WireMedia m : data.media()) {
                if (m.readerPos() == pos) {
                    label = GameText.resolve(m.mediaName());
                }
            }
        }
        volumeRenameKey = selectedKey;
        nameField.set(label);
        root.focus(nameField);
    }

    private void commitRename(final String typed) {
        final String name = typed.trim();
        if (!volumeRenameKey.isEmpty()) {
            if (!name.isEmpty()) {
                PacketDistributor.sendToServer(new RenameVolumePayload(host, volumeRenameKey, name));
            }
            volumeRenameKey = "";
            request();
            return;
        }
        if (!name.equals(data.machine().name())) {
            // The machine's own name, the same setting the Settings app writes.
            PacketDistributor.sendToServer(new SetSettingPayload(host, "name", name));
            request();
        }
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (root.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_F2) {
            // F2 renames the machine, or the selected disk or medium.
            if (selectedKey.startsWith("disk:") || selectedKey.startsWith("media:")) {
                startVolumeRename();
            } else {
                startRename();
            }
            return true;
        }
        return false;
    }

    // helpers

    private static String prettyDrive(final String drive) {
        final MediaDriveType type = MediaDriveType.find(drive);
        return type == null ? drive : GameText.resolve(switch (type) {
            case FLOPPY_DRIVE -> ThisPcTexts.FLOPPY;
            case CD_DRIVE -> ThisPcTexts.CD;
            case DVD_DRIVE -> ThisPcTexts.DVD;
            case DOCK_STATION -> ThisPcTexts.USB;
        });
    }

    private static ResourceLocation rl(final String id) {
        final ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed != null) {
            return parsed;
        }
        final String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return ResourceLocation.fromNamespaceAndPath("jsc", path);
    }

    private static ResourceLocation programIdOf(final String id) {
        return ResourceLocation.tryParse(id.contains(":") ? id : "jsc:" + id);
    }

    /** A program's human name, read from the single registry rather than a duplicated switch. */
    private static String prettyProgram(final String id) {
        final ProgramSpec spec = OsRegistry.getProgram(rl(id));
        return spec != null ? spec.displayName() : rl(id).getPath();
    }

    /** The id a package manager installs a program by. */
    private static String packageIdOf(final String id) {
        final ProgramSpec spec = OsRegistry.getProgram(rl(id));
        return spec != null ? spec.commandName() : rl(id).getPath();
    }

    private static String prettyOs(final String osPath) {
        final OsDef os = OsRegistry.getOs(rl(osPath));
        return os != null ? os.displayName() : osPath;
    }
}
