/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.ScannerBlockEntity;
import dev.jstech.computers.gui.layout.ScannerLayout;
import dev.jstech.computers.menu.ScannerMenu;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.scan.Scans;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;

/**
 * A scanner's window, in its era's skin and the look of the mod's devices: the name and the state in the header; on
 * the left the glass and what lies on it, the kind of file it makes and its pace, Scan and Stop, and that the item
 * stays; on the right the computer and folder the file goes to, the last file made, the page being read with the
 * light bar on it and what a scanner reads; under them the inventory and three readouts.
 */
@PaletteHolder
public class ScannerScreen extends AbstractComputerScreen<ScannerMenu> {

    /** The page being read when the window last looked, and when it saw that page begin; -1 when none is read. */
    private int seenPage = -1;
    private long pageSeenMs;

    /** The page in the preview: a book's paper and ink, a printed sheet's, a map's, an enchanted book's, the bar. */
    private static final Palette<Page> PAGE = Palettes.declare(JsComputers.MODID, "screen/scanner_page",
            new Page(0xffe9dfc4, 0xff3a3428, 0xfff4f4f0, 0xff3a3f48, 0xffd8c890, 0xff4a7ac8, 0xff6aa04a, 0xffd03a2a,
                    0xff6a3f86, 0xffc9a2ff, 0xffffffe6, 0xff96c8ff));
    /* The rows of ink on the page, every few pixels, each a little shorter or longer than the last. */
    private static final int INK_PITCH = 4;
    private static final int INK_SHORTEN = 11;
    private static final int MOST_ENCHANTMENTS = 3;
    /* The patches of land on a map's water, as x, y, width and height inside the page; and the marker's corner. */
    private static final int[][] LAND = {{4, 4, 20, 14}, {30, 10, 18, 24}, {8, 26, 14, 10}, {50, 4, 10, 8}};
    private static final int MARKER_X = 34;
    private static final int MARKER_Y = 18;
    private static final int MARKER = 3;
    private static final double MS_PER_SECOND = 1000.0;

    public ScannerScreen(final ScannerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = ScannerLayout.WIDTH;
        this.imageHeight = ScannerLayout.HEIGHT;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            final int mx = (int) mouseX;
            final int my = (int) mouseY;
            if (canScan() && hover(mx, my, ScannerLayout.SCAN_X, ScannerLayout.BUTTON_Y, ScannerLayout.BUTTON_W,
                    ScannerLayout.BUTTON_H)) {
                sendButton(ScannerMenu.BUTTON_SCAN);
                return true;
            }
            if (scanner().scanning() && hover(mx, my, ScannerLayout.STOP_X, ScannerLayout.BUTTON_Y,
                    ScannerLayout.BUTTON_W, ScannerLayout.BUTTON_H)) {
                sendButton(ScannerMenu.BUTTON_STOP);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The scanner's own era, whose skin its window wears. */
    @Override
    @Nullable
    protected Era screenEra() {
        return scanner().model().era();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        final ScannerBlockEntity scanner = scanner();
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + ScannerLayout.HEADER_X, y + ScannerLayout.HEADER_Y, ScannerLayout.HEADER_W);
        Draw.text(g, font, clip(title.getString().toUpperCase(Locale.ROOT), ScannerLayout.TITLE_CHARS * 6, 1.0F),
                x + ScannerLayout.TITLE_X, y + ScannerLayout.TITLE_Y, JsTechTheme.accent());
        drawState(g, scanner);
        JsTechTheme.panel(g, x + ScannerLayout.LEFT_X, y + ScannerLayout.LEFT_Y, ScannerLayout.LEFT_W,
                ScannerLayout.PANEL_H);
        JsTechTheme.panel(g, x + ScannerLayout.RIGHT_X, y + ScannerLayout.LEFT_Y, ScannerLayout.RIGHT_W,
                ScannerLayout.PANEL_H);
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }
        drawLeft(g, scanner, mouseX, mouseY);
        drawRight(g, scanner);
        drawReadouts(g, scanner);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Everything is drawn with the window in renderBg; the container's own two labels are not used.
    }

    /* The state at the header's right end, after its lamp: the page it reads, ready, or what stopped it. */
    private void drawState(final GuiGraphics g, final ScannerBlockEntity scanner) {
        final String words;
        final int colour;
        if (scanner.scanning()) {
            final int pages = scanner.pages();
            words = GameText.resolve(pages > 1 ? ScannerTexts.STATE_PAGE.with(scanner.page() + 1, pages)
                    : ScannerTexts.STATE_SCANNING.text());
            colour = JsTechTheme.amber();
        } else {
            words = GameText.resolve(switch (scanner.problem()) {
                case NONE -> ScannerTexts.STATE_READY;
                case NOTHING_ON_GLASS -> ScannerTexts.STATE_NOTHING;
                case NOT_LINKED -> ScannerTexts.STATE_NOT_LINKED;
                case NO_SYSTEM -> ScannerTexts.STATE_NO_SYSTEM;
                case DISK_FULL -> ScannerTexts.STATE_DISK_FULL;
                case NOT_SAVED -> ScannerTexts.STATE_NOT_SAVED;
            });
            colour = scanner.problem() == ScannerBlockEntity.Problem.NONE ? JsTechTheme.green() : JsTechTheme.red();
        }
        final int right = leftPos + ScannerLayout.STATE_RIGHT;
        final int width = font.width(words);
        Draw.text(g, font, words, right - width, topPos + ScannerLayout.TITLE_Y, colour);
        final int lampX = right - width - ScannerLayout.LAMP_SIZE - 3;
        final int lampY = topPos + ScannerLayout.TITLE_Y + 2;
        g.fill(lampX, lampY, lampX + ScannerLayout.LAMP_SIZE, lampY + ScannerLayout.LAMP_SIZE, colour);
    }

    /* The glass and what lies on it, what it makes, its pace, the buttons and the note. */
    private void drawLeft(final GuiGraphics g, final ScannerBlockEntity scanner, final int mouseX, final int mouseY) {
        final int dim = JsTechTheme.dim();
        small(g, GameText.resolve(ScannerTexts.GLASS), ScannerLayout.GLASS_X, ScannerLayout.GLASS_LABEL_Y, dim);
        final ItemStack item = scanner.onGlass();
        final List<String> lines = describe(item);
        for (int i = 0; i < lines.size() && i < ScannerLayout.ITEM_LINES; i++) {
            small(g, clip(lines.get(i), ScannerLayout.LEFT_X + ScannerLayout.LEFT_W - 3 - ScannerLayout.ITEM_X,
                    ScannerLayout.SMALL), ScannerLayout.ITEM_X, ScannerLayout.ITEM_Y + i * ScannerLayout.ITEM_LINE,
                    i == 0 && !item.isEmpty() ? JsTechTheme.text() : dim);
        }
        final FileType makes = Scans.makes(item);
        readout(g, ScannerLayout.FACT_X, ScannerLayout.MAKES_Y, ScannerLayout.FACT_W, ScannerTexts.MAKES,
                makes == null ? GameText.resolve(ScannerTexts.NONE) : "." + makes.extension(),
                makes == null ? dim : JsTechTheme.text());
        readout(g, ScannerLayout.FACT_X, ScannerLayout.SPEED_Y, ScannerLayout.FACT_W, ScannerTexts.SPEED,
                GameText.resolve(ScannerTexts.PER_PAGE.with(seconds(scanner.model().pageSeconds()))),
                JsTechTheme.text());
        // Scan stays lit while it reads, as a pressed key does; Stop is the key that can be pressed then.
        drawButton(g, ScannerLayout.SCAN_X, ScannerTexts.SCAN, canScan(),
                canScan() || scanner.scanning() ? JsTechTheme.accent() : dim, mouseX, mouseY);
        drawButton(g, ScannerLayout.STOP_X, ScannerTexts.STOP, scanner.scanning(),
                scanner.scanning() ? JsTechTheme.text() : dim, mouseX, mouseY);
        small(g, GameText.resolve(ScannerTexts.NOTE_1), ScannerLayout.GLASS_X, ScannerLayout.NOTE_Y, dim);
        small(g, GameText.resolve(ScannerTexts.NOTE_2), ScannerLayout.GLASS_X,
                ScannerLayout.NOTE_Y + ScannerLayout.NOTE_LINE, dim);
    }

    /* Where the file goes, the page and its progress, and what a scanner reads. */
    private void drawRight(final GuiGraphics g, final ScannerBlockEntity scanner) {
        final int dim = JsTechTheme.dim();
        final int room = ScannerLayout.RIGHT_X + ScannerLayout.RIGHT_W - 4 - ScannerLayout.TEXT_X;
        small(g, GameText.resolve(ScannerTexts.SAVES_TO), ScannerLayout.TEXT_X, ScannerLayout.SAVES_LABEL_Y, dim);
        final boolean linked = !scanner.computer().isEmpty();
        Draw.text(g, font, clip(linked ? scanner.computer() : GameText.resolve(ScannerTexts.NO_COMPUTER), room, 1.0F),
                leftPos + ScannerLayout.TEXT_X, topPos + ScannerLayout.COMPUTER_Y,
                linked ? JsTechTheme.text() : dim);
        small(g, clip(scanner.folder(), room, ScannerLayout.SMALL), ScannerLayout.TEXT_X, ScannerLayout.FOLDER_Y, dim);
        final boolean made = !scanner.lastFile().isEmpty();
        small(g, clip(made ? scanner.lastFile() : GameText.resolve(ScannerTexts.NO_FILE_YET), room,
                ScannerLayout.SMALL), ScannerLayout.TEXT_X, ScannerLayout.FILE_Y, made ? JsTechTheme.accent() : dim);
        final int pages = scanner.pages();
        final int read = scanner.scanning() ? Math.min(pages, scanner.page() + 1) : 0;
        final float share = pageShare(scanner);
        drawPreview(g, Scans.makes(scanner.onGlass()), share);
        small(g, GameText.resolve(ScannerTexts.PAGE), ScannerLayout.SIDE_X, ScannerLayout.PAGE_LABEL_Y, dim);
        Draw.text(g, font, GameText.resolve(ScannerTexts.PAGE_OF.with(read, scanner.onGlass().isEmpty() ? 0 : pages)),
                leftPos + ScannerLayout.SIDE_X, topPos + ScannerLayout.PAGE_Y, JsTechTheme.text());
        final int barX = leftPos + ScannerLayout.SIDE_X;
        final int barY = topPos + ScannerLayout.BAR_Y;
        g.fill(barX, barY, barX + ScannerLayout.SIDE_W, barY + ScannerLayout.BAR_H, JsTechTheme.track());
        if (scanner.scanning()) {
            final int done = (int) (ScannerLayout.SIDE_W * (scanner.page() + share) / pages);
            g.fill(barX, barY, barX + done, barY + ScannerLayout.BAR_H, JsTechTheme.accent());
        }
        small(g, GameText.resolve(ScannerTexts.READS), ScannerLayout.SIDE_X, ScannerLayout.READS_LABEL_Y, dim);
        final TextKey[] reads = {ScannerTexts.READS_WRITTEN, ScannerTexts.READS_QUILLS, ScannerTexts.READS_ENCHANTED,
                ScannerTexts.READS_MAPS, ScannerTexts.READS_PAPER};
        for (int i = 0; i < reads.length; i++) {
            small(g, GameText.resolve(reads[i]), ScannerLayout.SIDE_X,
                    ScannerLayout.READS_Y + i * ScannerLayout.READS_LINE, JsTechTheme.text());
        }
    }

    /*
     * How far into the page being read the scanner is, from 0 to 1. The scanner tells its players the page, not the
     * ticks into it, so the light bar moves on the window's own clock from the moment it saw that page begin; a window
     * opened halfway through a page starts that page's bar at the top.
     */
    private float pageShare(final ScannerBlockEntity scanner) {
        if (!scanner.scanning()) {
            seenPage = -1;
            return 0F;
        }
        final long now = Util.getMillis();
        if (scanner.page() != seenPage) {
            seenPage = scanner.page();
            pageSeenMs = now;
        }
        return (float) Math.min(1.0, (now - pageSeenMs) / (scanner.model().pageSeconds() * MS_PER_SECOND));
    }

    /* The page under the glass as the scanner reads it: the part already read, the light bar, the rest still dark. */
    private void drawPreview(final GuiGraphics g, @Nullable final FileType kind, final float share) {
        final int x = leftPos + ScannerLayout.PREVIEW_X;
        final int y = topPos + ScannerLayout.PREVIEW_Y;
        final int w = ScannerLayout.PREVIEW_W;
        final int h = ScannerLayout.PREVIEW_H;
        g.fill(x, y, x + w, y + h, JsTechTheme.track());
        if (kind != null && share > 0F) {
            final Page page = PAGE.get();
            final int read = (int) (h * share);
            final boolean sheet = kind == FileType.TXT || kind == FileType.PIX;
            g.fill(x, y, x + w, y + read, kind == FileType.MAP ? page.mapPaper() : sheet ? page.sheet() : page.paper());
            if (kind == FileType.MAP) {
                drawMap(g, page, x, y, w, read);
            } else {
                final int ink = kind == FileType.ENCH ? page.enchantedInk() : sheet ? page.sheetInk() : page.ink();
                for (int ly = y + INK_PITCH; ly < y + read - 2; ly += INK_PITCH) {
                    final int length = w - 8 - (ly * 7) % INK_SHORTEN;
                    g.fill(x + 4, ly, x + 4 + length, ly + 1, ink);
                }
                if (kind == FileType.ENCH && read > INK_PITCH + 2) {
                    g.fill(x + 4, y + INK_PITCH, x + w - 4, y + INK_PITCH + 1, page.shimmer());
                }
            }
            g.fill(x, y + read, x + w, y + read + 1, page.bar());
            g.fill(x, y + read + 1, x + w, y + read + 2, page.glow());
        }
        JsTechTheme.hLine(g, x, y, w);
        JsTechTheme.hLine(g, x, y + h - 1, w);
        JsTechTheme.vLine(g, x, y, h);
        JsTechTheme.vLine(g, x + w - 1, y, h);
    }

    /* The part of a map already read: its water, the patches of land the bar has passed, and the player's marker. */
    private static void drawMap(final GuiGraphics g, final Page page, final int x, final int y, final int w,
                                final int read) {
        if (read > 4) {
            g.fill(x + 3, y + 3, x + w - 3, y + read - 1, page.water());
        }
        for (final int[] land : LAND) {
            if (land[1] + land[3] < read) {
                g.fill(x + land[0], y + land[1], x + land[0] + land[2], y + land[1] + land[3], page.land());
            }
        }
        if (MARKER_Y + MARKER < read) {
            g.fill(x + MARKER_X, y + MARKER_Y, x + MARKER_X + MARKER, y + MARKER_Y + MARKER, page.marker());
        }
    }

    /* The three readouts beside the inventory: the computer, the files made, and what a scan uses up. */
    private void drawReadouts(final GuiGraphics g, final ScannerBlockEntity scanner) {
        final boolean linked = !scanner.computer().isEmpty();
        final String files = GameText.resolve(scanner.scanned() == 1 ? ScannerTexts.ONE_FILE.text()
                : ScannerTexts.FILES.with(scanner.scanned()));
        final int x = ScannerLayout.READOUT_X;
        final int w = ScannerLayout.READOUT_W;
        readout(g, x, ScannerLayout.READOUT_Y, w, ScannerTexts.LINKED,
                linked ? scanner.computer() : GameText.resolve(ScannerTexts.NONE),
                linked ? JsTechTheme.text() : JsTechTheme.dim());
        readout(g, x, ScannerLayout.READOUT_Y + ScannerLayout.READOUT_STEP, w, ScannerTexts.SCANNED, files,
                JsTechTheme.text());
        readout(g, x, ScannerLayout.READOUT_Y + 2 * ScannerLayout.READOUT_STEP, w, ScannerTexts.USES_UP,
                GameText.resolve(ScannerTexts.NOTHING), JsTechTheme.dim());
    }

    /* One instrument of the window, at a point of it: its label small over its value, which sits right. */
    private void readout(final GuiGraphics g, final int rx, final int ry, final int w, final TextKey label,
                         final String value, final int colour) {
        final int x = leftPos + rx;
        final int y = topPos + ry;
        JsTechTheme.panel(g, x, y, w, ScannerLayout.READOUT_H);
        JsTechTheme.textS(g, font, GameText.resolve(label), x + 3, y + 2, JsTechTheme.dim());
        final String shown = clip(value, w - 6, 1.0F);
        Draw.text(g, font, shown, x + w - 3 - font.width(shown), y + ScannerLayout.READOUT_VALUE_Y, colour);
    }

    /* A key of the window; {@code pressable} keys light up under the mouse. */
    private void drawButton(final GuiGraphics g, final int bx, final TextKey label, final boolean pressable,
                            final int colour, final int mouseX, final int mouseY) {
        final int x = leftPos + bx;
        final int y = topPos + ScannerLayout.BUTTON_Y;
        final boolean lit = pressable && mouseX >= x && mouseX < x + ScannerLayout.BUTTON_W && mouseY >= y
                && mouseY < y + ScannerLayout.BUTTON_H;
        JsTechTheme.button(g, x, y, ScannerLayout.BUTTON_W, ScannerLayout.BUTTON_H, lit);
        final String words = GameText.resolve(label);
        Draw.text(g, font, words, x + (ScannerLayout.BUTTON_W - font.width(words)) / 2, y + 3,
                lit ? JsTechTheme.text() : colour);
    }

    /* What the window says of the item on the glass, a line at a time: its name first, then what it holds. */
    private static List<String> describe(final ItemStack item) {
        final List<String> out = new ArrayList<>();
        if (item.isEmpty()) {
            out.add(GameText.resolve(ScannerTexts.EMPTY_GLASS));
            out.add(GameText.resolve(ScannerTexts.EMPTY_GLASS_2));
            out.add(GameText.resolve(ScannerTexts.EMPTY_GLASS_3));
            return out;
        }
        final WrittenBookContent written = item.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (item.is(Items.WRITTEN_BOOK) && written != null) {
            out.add(written.title().raw());
            out.add(GameText.resolve(ScannerTexts.BY.with(written.author())));
            out.add(pages(Scans.pages(item)));
            return out;
        }
        if (item.is(Items.ENCHANTED_BOOK)) {
            out.add(item.getHoverName().getString());
            final ItemEnchantments stored = item.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
                    ItemEnchantments.EMPTY);
            for (final Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
                if (out.size() >= MOST_ENCHANTMENTS) {
                    break;
                }
                out.add(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
            }
            return out;
        }
        if (item.is(Items.FILLED_MAP)) {
            // A map goes by its number, as its tooltip gives it, unless the player named it.
            final MapId id = item.get(DataComponents.MAP_ID);
            final String number = id == null ? "" : GameText.resolve(ScannerTexts.MAP_NUMBER.with(id.id()));
            final boolean named = item.has(DataComponents.CUSTOM_NAME) || number.isEmpty();
            out.add(named ? item.getHoverName().getString() : number);
            if (named && !number.isEmpty()) {
                out.add(number);
            }
            return out;
        }
        final PrintedDocument document = item.get(ComputingComponents.PRINTED_DOCUMENT.get());
        out.add(item.is(ComputingModule.PRINTED_PAPER.get()) && document != null ? document.title()
                : item.getHoverName().getString());
        out.add(pages(Scans.pages(item)));
        return out;
    }

    private static String pages(final int count) {
        return GameText.resolve(count == 1 ? ScannerTexts.ONE_PAGE.text() : ScannerTexts.PAGES.with(count));
    }

    /* A page's seconds as the window says them: whole seconds without a point, half a second with one. */
    private static String seconds(final double seconds) {
        return seconds == Math.rint(seconds) ? String.valueOf((long) seconds) : String.valueOf(seconds);
    }

    private boolean canScan() {
        return !scanner().scanning() && !scanner().onGlass().isEmpty();
    }

    private ScannerBlockEntity scanner() {
        return menu.scanner();
    }

    /* A line at the small letters at a point of the window. */
    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour) {
        JsTechTheme.textS(g, font, text, leftPos + x, topPos + y, colour);
    }

    /* A line cut to the room it has, with an ellipsis, at the given scale. */
    private String clip(final String text, final int room, final float scale) {
        final int limit = (int) (room / scale);
        if (font.width(text) <= limit) {
            return text;
        }
        return font.plainSubstrByWidth(text, limit - font.width("...")) + "...";
    }

    /**
     * The colours of the page in the preview.
     *
     * @param paper        a book's paper
     * @param ink          a book's lines
     * @param sheet        a printed sheet's paper
     * @param sheetInk     a printed sheet's lines
     * @param mapPaper     the paper round a map
     * @param water        a map's water
     * @param land         a map's land
     * @param marker       the player's marker on a map
     * @param enchantedInk an enchanted book's lines
     * @param shimmer      the shimmer across an enchanted book's first line
     * @param bar          the light bar
     * @param glow         its glow under it
     */
    private record Page(int paper, int ink, int sheet, int sheetInk, int mapPaper, int water, int land, int marker,
                        int enchantedInk, int shimmer, int bar, int glow) {
    }
}
