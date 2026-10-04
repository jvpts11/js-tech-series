/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.printer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.PrintedPaperLayout;
import dev.jstech.computers.item.PrintedPaperItem;
import dev.jstech.computers.printer.PrintLayout;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrinterModel;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A printed sheet read page by page, as a written book is: the sheet looks like its printer's (the fanfold with its
 * green bars and tractor holes from the dot matrix, a plain sheet from the others), upright or on its side as its
 * pages were laid out, and a picture shows on it as that printer put it on paper. The arrows under it, or the arrow
 * and page keys, turn the pages.
 */
@PaletteHolder
public final class PrintedPaperScreen extends Screen {

    private final PrintedDocument document;
    private final boolean fanfold;
    private final boolean landscape;
    private int page;
    /* Where the sheet was last drawn and at what size, for the clicks on its arrows. */
    private float originX;
    private float originY;
    private float scale = 1F;

    /** The paper, its bands and holes, the print, and the pager under the sheet. */
    private static final Palette<SheetColours> COLOURS = Palettes.declare(JsComputers.MODID, "screen/printed_paper",
            new SheetColours(0xFFFBFBF8, 0xFFF4F4F0, 0xFFD7EBD7, 0xFFCFCFC8, 0xFF23262C, 0xFF6A707A, 0xFFDDDDDD,
                    0xFF6F6F6F));
    private static final String PREVIOUS = "<";
    private static final String NEXT = ">";
    /** The room kept round the sheet when the window is too small for it. */
    private static final int ROOM = 8;
    /** How many fanfold lines one green band covers. */
    private static final int BAND_LINES = 2;
    /** The rows a picture's caption takes above it. */
    private static final int CAPTION_LINES = 3;

    private PrintedPaperScreen(final PrintedDocument document) {
        super(Component.literal(document.title()));
        this.document = document;
        final PrinterModel model = document.printerModel();
        this.fanfold = model != null && model.sheet() == PrinterModel.Sheet.FANFOLD;
        this.landscape = widest(document) > PrintLayout.PORTRAIT_COLUMNS;
    }

    /** Opens {@code document} to read it. */
    public static void open(final PrintedDocument document) {
        Minecraft.getInstance().setScreen(new PrintedPaperScreen(document));
    }

    /** The colours of the paper, which a framed picture is drawn on too. */
    public static SheetColours colours() {
        return COLOURS.get();
    }

    /** The page being read, from 0. */
    public int page() {
        return page;
    }

    /** How many pages the sheet has. */
    public int pages() {
        return document.pages().size();
    }

    /** The document being read. */
    public PrintedDocument document() {
        return document;
    }

    /** Turns to the next page, or the previous one with a negative step, staying within the document. */
    public void turn(final int step) {
        page = Math.max(0, Math.min(pages() - 1, page + step));
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        final SheetColours colours = COLOURS.get();
        // The world blurred and dimmed behind the sheet, as behind a book being read.
        renderBackground(g, mouseX, mouseY, partialTick);
        final PrintedPaperLayout.Sheet sheet = landscape ? PrintedPaperLayout.landscape(fanfold)
                : PrintedPaperLayout.portrait(fanfold);
        scale = Math.min(1F, Math.min((float) (width - ROOM * 2) / sheet.totalWidth(),
                (float) (height - ROOM * 2) / sheet.totalHeight()));
        originX = (width - sheet.totalWidth() * scale) / 2F;
        originY = (height - sheet.totalHeight() * scale) / 2F;
        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        g.pose().scale(scale, scale, 1F);
        drawSheet(g, sheet, colours);
        if (document.isPicture()) {
            drawPicture(g, sheet, colours);
        } else {
            drawText(g, sheet, colours);
        }
        drawPager(g, sheet, colours);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            final PrintedPaperLayout.Sheet sheet = landscape ? PrintedPaperLayout.landscape(fanfold)
                    : PrintedPaperLayout.portrait(fanfold);
            final double x = (mouseX - originX) / scale;
            final double y = (mouseY - originY) / scale;
            final int pagerY = sheet.height() + PrintedPaperLayout.PAGER_GAP;
            if (y >= pagerY && y < pagerY + PrintedPaperLayout.PAGER_H) {
                if (x >= 0 && x < PrintedPaperLayout.ARROW_W) {
                    turn(-1);
                    return true;
                }
                if (x >= sheet.width() - PrintedPaperLayout.ARROW_W && x < sheet.width()) {
                    turn(1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* The paper, with the fanfold's bands down its print and its strips of holes down both sides. */
    private void drawSheet(final GuiGraphics g, final PrintedPaperLayout.Sheet sheet, final SheetColours colours) {
        g.fill(0, 0, sheet.width(), sheet.height(), fanfold ? colours.fanfold() : colours.paper());
        if (!fanfold) {
            return;
        }
        final int band = PrintedPaperLayout.LINE_H * BAND_LINES;
        for (int y = PrintedPaperLayout.MARGIN + band; y < sheet.height(); y += band * 2) {
            g.fill(PrintedPaperLayout.TRACTOR, y, sheet.width() - PrintedPaperLayout.TRACTOR,
                    Math.min(sheet.height(), y + band), colours.bar());
        }
        for (int y = 5; y < sheet.height() - 3; y += 8) {
            g.fill(4, y, 8, y + 4, colours.hole());
            g.fill(sheet.width() - 8, y, sheet.width() - 4, y + 4, colours.hole());
        }
    }

    private void drawText(final GuiGraphics g, final PrintedPaperLayout.Sheet sheet, final SheetColours colours) {
        final String[] lines = document.pages().get(page).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Draw.textScaled(g, font, lines[i], sheet.textX(), sheet.textY() + i * PrintedPaperLayout.LINE_H,
                    colours.ink(), PrintedPaperLayout.TEXT_SCALE);
        }
    }

    /* The picture's caption, its file and where it was printed from, and the picture fitted to the sheet under it. */
    private void drawPicture(final GuiGraphics g, final PrintedPaperLayout.Sheet sheet, final SheetColours colours) {
        final String name = document.pictureName().isEmpty() ? document.title() : document.pictureName();
        Draw.textScaled(g, font, name, sheet.textX(), sheet.textY(), colours.ink(), PrintedPaperLayout.TEXT_SCALE);
        if (!document.program().isEmpty()) {
            final String from = GameText.resolve(PrintedPaperItem.FROM.with(document.program(), document.from()));
            Draw.textScaled(g, font, from, sheet.textX(), sheet.textY() + PrintedPaperLayout.LINE_H, colours.faint(),
                    PrintedPaperLayout.TEXT_SCALE);
        }
        final PrintedPictureTextures.Entry ink = PrintedPictureTextures.ink(document);
        if (ink == null) {
            return;
        }
        final int top = sheet.textY() + CAPTION_LINES * PrintedPaperLayout.LINE_H;
        final int roomW = sheet.width() - sheet.textX() * 2;
        final int roomH = sheet.height() - top - PrintedPaperLayout.MARGIN;
        final float fit = Math.min((float) roomW / ink.width(), (float) roomH / ink.height());
        final int w = Math.max(1, (int) (ink.width() * fit));
        final int h = Math.max(1, (int) (ink.height() * fit));
        RenderSystem.enableBlend();
        g.blit(ink.texture(), sheet.textX() + (roomW - w) / 2, top, w, h, 0, 0, ink.width(), ink.height(),
                ink.width(), ink.height());
        RenderSystem.disableBlend();
    }

    private void drawPager(final GuiGraphics g, final PrintedPaperLayout.Sheet sheet, final SheetColours colours) {
        final int y = sheet.height() + PrintedPaperLayout.PAGER_GAP + 2;
        Draw.text(g, font, PREVIOUS, 3, y, page > 0 ? colours.pager() : colours.pagerDim());
        Draw.text(g, font, NEXT, sheet.width() - PrintedPaperLayout.ARROW_W + 3, y,
                page < pages() - 1 ? colours.pager() : colours.pagerDim());
        final String count = GameText.resolve(PrintedPaperItem.PAGE_OF.with(page + 1, pages()));
        Draw.textCentered(g, font, count, sheet.width() / 2, y, colours.pager());
    }

    /* The longest line of print in the document, which tells a page laid on its side from one standing. */
    private static int widest(final PrintedDocument document) {
        int widest = 0;
        for (final String page : document.pages()) {
            for (final String line : page.split("\n", -1)) {
                widest = Math.max(widest, line.length());
            }
        }
        return widest;
    }

    /**
     * The colours of a sheet being read.
     *
     * @param paper    a plain sheet
     * @param fanfold  the fanfold's paper between its bands
     * @param bar      the fanfold's green bands
     * @param hole     the fanfold's tractor holes
     * @param ink      the print
     * @param faint    the caption under a picture's name
     * @param pager    the page count and an arrow that turns
     * @param pagerDim an arrow at the end of the document
     */
    public record SheetColours(int paper, int fanfold, int bar, int hole, int ink, int faint, int pager, int pagerDim) {
    }
}
