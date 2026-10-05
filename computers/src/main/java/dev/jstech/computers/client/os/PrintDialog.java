/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.computers.client.printer.PrintedPictureTextures;
import dev.jstech.computers.operation.payload.printer.PrintPayload;
import dev.jstech.computers.operation.payload.printer.PrintersPayload;
import dev.jstech.computers.operation.payload.printer.RequestPrintersPayload;
import dev.jstech.computers.printer.PrintLayout;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrinterModel;
import dev.jstech.computers.printer.Printers;
import dev.jstech.core.client.gui.component.AmountStepper;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.component.WaitBar;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Print window a program opens over itself, in the system's own way: the classic dialog of Frames 95 and XP and
 * the older desktops (the printers to choose from, the chosen one's status, where it is and its paper, the pages, the
 * copies and the way the page lies), and on the flat desktops of today the same controls beside a preview of the page,
 * as Frames 11 shows it. It counts the sheets the document will take from the tray before anything is sent.
 */
public final class PrintDialog implements IDesktopApp {

    private final BlockPos host;
    /** The program that opened this window, whose window it sits over and holds. */
    private final IDesktopApp owner;
    private final Panel root = new Panel();
    private final Label printerLabel;
    private final ListView<PrintersPayload.Row> printerList;
    private final Label status;
    private final Label location;
    private final WaitBar looking;
    private final Label paper;
    private final Label rangeLabel;
    private final Checkbox all;
    private final Checkbox range;
    private final TextField from;
    private final TextField to;
    private final Label copiesLabel;
    private final AmountStepper copies;
    private final Label orientationLabel;
    private final Button portrait;
    private final Button landscape;
    private final Label note;
    private final Button previous;
    private final Button next;
    private final Button print;
    private final Button cancel;
    private OsSkin skin = OsSkin.fallback();
    private boolean open;
    private boolean listed;
    private Document document = Document.text("", "", "");
    private List<PrintersPayload.Row> printers = List.of();
    private String machine = "";
    private int chosen;
    private boolean pagesOnly;
    private boolean lying;
    private int previewPage;
    /* Where the preview was last laid out, which the frame draws into. */
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewH;

    /** How wide the bar for the wait for printers runs at most. */
    private static final int LOOKING_W = 100;
    private static final int CLASSIC_W = 272;
    private static final int CLASSIC_H = 166;
    private static final int MODERN_W = 330;
    private static final int MODERN_H = 140;
    /** The labels in front of each setting's row in the dialog with a preview, and the size of its note's letters. */
    private static final int ROW_LABEL_W = 58;
    private static final float NOTE_SCALE = 0.75F;
    private static final int ROW_H = 11;
    private static final int LIST_H = 34;
    /** The width of the controls' column in the dialog with a preview. */
    private static final int CONTROLS_W = 170;
    /** The copies' stepper: its four buttons and the number between them. */
    private static final int COPIES_W = 100;
    /** How tall a printed line is in the preview, and the paper round it. */
    private static final int PREVIEW_LINE = 4;
    private static final int PREVIEW_MARGIN = 6;

    public PrintDialog(final BlockPos host, final IDesktopApp owner) {
        this.host = host;
        this.owner = owner;
        this.printerLabel = root.add(new Label(() -> GameText.resolve(modern() ? PrintTexts.PRINTER
                : PrintTexts.SELECT_PRINTER), Label.Tone.DIM));
        this.printerList = root.add(new ListView<>(() -> printers, ROW_H, this::drawPrinter)
                .setOnClick((index, button, mx, my) -> choose(index)));
        this.status = root.add(new Label(this::statusLine));
        this.location = root.add(new Label(this::locationLine, Label.Tone.DIM));
        this.looking = root.add(new WaitBar());
        this.paper = root.add(new Label(this::paperLine, Label.Tone.DIM));
        this.rangeLabel = root.add(new Label(GameText.resolve(PrintTexts.PAGE_RANGE), Label.Tone.DIM));
        this.all = root.add(new Checkbox(() -> GameText.resolve(PrintTexts.ALL), () -> !pagesOnly,
                () -> pagesOnly = false));
        this.range = root.add(new Checkbox(() -> GameText.resolve(PrintTexts.PAGES), () -> pagesOnly,
                () -> pagesOnly = true));
        this.from = root.add(new TextField(3).setOnEdit(() -> pagesOnly = true));
        this.to = root.add(new TextField(3).setOnEdit(() -> pagesOnly = true));
        this.copiesLabel = root.add(new Label(GameText.resolve(PrintTexts.COPIES), Label.Tone.DIM));
        this.copies = root.add(new AmountStepper().setRange(1, 99).setAmount(1));
        this.orientationLabel = root.add(new Label(GameText.resolve(PrintTexts.ORIENTATION), Label.Tone.DIM));
        this.portrait = root.add(new Button(GameText.resolve(PrintTexts.PORTRAIT), () -> lie(false)));
        this.landscape = root.add(new Button(GameText.resolve(PrintTexts.LANDSCAPE), () -> lie(true)));
        this.note = root.add(new Label(this::noteLine, Label.Tone.DIM));
        this.previous = root.add(new Button("<", () -> turnPreview(-1)));
        this.next = root.add(new Button(">", () -> turnPreview(1)));
        this.print = root.add(new Button(GameText.resolve(PrintTexts.PRINT), this::confirm).setPrimary(true));
        this.cancel = root.add(new Button(GameText.resolve(PrintTexts.CANCEL), this::close));
    }

    /** Opens over the owner's window to print {@code what}, asking the machine for its printers. */
    public void show(final Document what) {
        this.document = what;
        this.listed = false;
        this.printers = List.of();
        this.chosen = 0;
        this.pagesOnly = false;
        this.lying = false;
        this.previewPage = 0;
        this.copies.setAmount(1);
        this.from.set("1");
        this.to.set(String.valueOf(pageCount()));
        if (!open) {
            open = true;
            ActiveDesktop.openDialogFor(owner, this);
        }
        PrintReplies.expectPrinters(this);
        PacketDistributor.sendToServer(new RequestPrintersPayload(host));
    }

    public BlockPos host() {
        return host;
    }

    public boolean isOpen() {
        return open;
    }

    /** The printers the machine answered with, and its name. */
    public void onPrinters(final String machineName, final List<PrintersPayload.Row> rows) {
        this.machine = machineName;
        this.printers = List.copyOf(rows);
        this.listed = true;
        this.chosen = 0;
        printerList.setSelected(rows.isEmpty() ? -1 : 0);
    }

    /** Puts the window away without printing. */
    public void close() {
        if (!open) {
            return;
        }
        open = false;
        PrintReplies.forget(this);
        ActiveDesktop.closeDialog(this);
    }

    /** The names of the printers listed, for a test to find one by. */
    public List<String> printerNames() {
        return printers.stream().map(row -> GameText.resolve(name(row))).toList();
    }

    /** How many sheets the print will take from the tray, its copies counted. */
    public int sheets() {
        if (document.isPicture()) {
            return (int) copies.amount();
        }
        final int total = pageCount();
        final int first = pagesOnly ? Math.max(1, parse(from.value(), 1)) : 1;
        final int last = pagesOnly ? Math.min(total, parse(to.value(), total)) : total;
        return Math.max(1, last - first + 1) * (int) copies.amount();
    }

    /** Sends the document to the chosen printer and puts the window away; with no printer, it stays and says so. */
    public void confirm() {
        if (printers.isEmpty()) {
            return;
        }
        final PrintersPayload.Row printer = printers.get(Math.min(chosen, printers.size() - 1));
        final int total = pageCount();
        final int first = pagesOnly ? Math.max(1, parse(from.value(), 1)) : 0;
        final int last = pagesOnly ? Math.min(total, parse(to.value(), total)) : 0;
        PacketDistributor.sendToServer(new PrintPayload(host, printer.pos(), document.title(), document.program(),
                document.text(), document.picture(), document.pictureName(), (int) copies.amount(), lying, first,
                last));
        close();
    }

    @Override
    public String title() {
        return GameText.resolve(PrintTexts.PRINT);
    }

    @Override
    public int defaultWidth() {
        return (modern() ? MODERN_W : CLASSIC_W) + 8;
    }

    @Override
    public int defaultHeight() {
        return (modern() ? MODERN_H : CLASSIC_H) + DesktopWindow.TITLE_H + 8;
    }

    @Override
    public int minWidth() {
        return CLASSIC_W;
    }

    @Override
    public int minHeight() {
        return CLASSIC_H;
    }

    @Override
    public void applySkin(final OsSkin value) {
        this.skin = value;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        layout(x, y, width);
        // The column beside the preview is narrow: its row labels and its note take smaller letters there.
        final float labels = modern() ? Texts.SMALL : 1F;
        rangeLabel.setScale(labels);
        copiesLabel.setScale(labels);
        orientationLabel.setScale(labels);
        note.setScale(modern() ? NOTE_SCALE : 1F);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        portrait.setPrimary(!lying);
        landscape.setPrimary(lying);
        print.setEnabled(!printers.isEmpty());
        root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        if (modern()) {
            drawPreview(g, font);
        }
    }

    @Override
    public boolean wantsEscape() {
        return true;
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mx, final double my, final int button) {
        root.mouseClicked(mx, my, button);
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mx, final double my, final int button) {
        root.mouseDragged(mx, my, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mx, final double my, final int button) {
        root.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return root.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    @Override
    public void onClosed() {
        open = false;
        PrintReplies.forget(this);
    }

    /* The dialog with a preview is the one the flat desktops of today draw: Frames 11 and its neighbours. */
    private boolean modern() {
        return skin.form() == OsSkin.Form.FLAT;
    }

    private void layout(final int cx, final int cy, final int cw) {
        if (modern()) {
            layoutBesidePreview(cx, cy, cw);
        } else {
            layoutClassic(cx, cy, cw);
        }
        previous.setVisible(modern());
        next.setVisible(modern());
    }

    /* The classic dialog: the printers over their facts, the page range and the copies with the way it lies. */
    private void layoutClassic(final int cx, final int cy, final int cw) {
        final int x = cx + 2;
        final int w = cw - 4;
        int y = cy + 2;
        printerLabel.setBounds(x, y, w, 9);
        y += 10;
        printerList.setBounds(x, y, w, LIST_H);
        y += LIST_H + 3;
        status.setBounds(x, y, w, 9);
        location.setBounds(x, y + 10, w, 9);
        placeLooking(x, y + 11, w);
        paper.setBounds(x, y + 20, w, 9);
        y += 33;
        final int half = w / 2 + 4;
        rangeLabel.setBounds(x, y, 100, 9);
        all.setBounds(x, y + 11, 60, 10);
        range.setBounds(x, y + 24, 44, 10);
        from.setBounds(x + 46, y + 23, 20, 11);
        to.setBounds(x + 70, y + 23, 20, 11);
        copiesLabel.setBounds(x + half, y, 60, 9);
        copies.setBounds(x + half, y + 11, COPIES_W, 11);
        orientationLabel.setBounds(x + half, y + 26, 80, 9);
        portrait.setBounds(x + half, y + 36, 52, 11);
        landscape.setBounds(x + half + 55, y + 36, 60, 11);
        note.setBounds(x, cy + CLASSIC_H - 32, w, 9);
        buttons(cy + CLASSIC_H - 14, cx + cw - 2);
    }

    /*
     * The flat desktops' dialog, short enough for a window over a program: the controls in a column, each setting
     * on a row of its own with its label in front, beside the preview of the page.
     */
    private void layoutBesidePreview(final int cx, final int cy, final int cw) {
        final int x = cx + 2;
        final int w = CONTROLS_W;
        final int field = x + ROW_LABEL_W;
        int y = cy + 2;
        printerLabel.setBounds(x, y, w, 9);
        y += 10;
        printerList.setBounds(x, y, w, ROW_H * 2);
        y += ROW_H * 2 + 3;
        status.setBounds(x, y, w, 9);
        location.setBounds(x, y + 10, w, 9);
        placeLooking(x, y + 11, w);
        paper.setBounds(x, y + 20, w, 9);
        y += 32;
        rangeLabel.setBounds(x, y + 1, ROW_LABEL_W, 9);
        all.setBounds(field, y, 30, 10);
        range.setBounds(field + 32, y, 46, 10);
        from.setBounds(field + 78, y - 1, 16, 11);
        to.setBounds(field + 96, y - 1, 16, 11);
        y += 14;
        copiesLabel.setBounds(x, y + 1, ROW_LABEL_W, 9);
        copies.setBounds(field, y, Math.min(COPIES_W, w - ROW_LABEL_W), 11);
        y += 14;
        orientationLabel.setBounds(x, y + 1, ROW_LABEL_W, 9);
        portrait.setBounds(field, y, 48, 11);
        landscape.setBounds(field + 50, y, 56, 11);
        note.setBounds(x, cy + MODERN_H - 28, w, 9);
        buttons(cy + MODERN_H - 14, cx + CONTROLS_W);
        previewX = cx + CONTROLS_W + 10;
        previewY = cy + 4;
        previewW = cw - CONTROLS_W - 14;
        previewH = MODERN_H - 22;
        previous.setBounds(previewX, previewY + previewH + 2, 11, 11);
        next.setBounds(previewX + previewW - 11, previewY + previewH + 2, 11, 11);
    }

    /* Print and Cancel at the right end of the last row. */
    private void buttons(final int y, final int right) {
        cancel.setBounds(right - 44, y, 44, 11);
        print.setBounds(right - 44 - 4 - 44, y, 44, 11);
    }

    /* One printer of the list: its name and the port it is on. */
    private void drawPrinter(final GuiGraphics g, final UiContext ctx, final PrintersPayload.Row row, final int index,
                             final int x, final int y, final int w, final int h, final boolean hovered,
                             final boolean selected) {
        if (selected || index == chosen) {
            g.fill(x, y, x + w, y + h, skin.listSelect());
        } else if (hovered) {
            g.fill(x, y, x + w, y + h, skin.listHover());
        }
        Texts.small(g, ctx.font(), GameText.resolve(name(row)) + "  (" + row.port() + ")", x + 3, y + 3,
                skin.text());
    }

    /* The page being printed, small, as the flat desktops preview it: the lines of print as grey bars. */
    private void drawPreview(final GuiGraphics g, final Font font) {
        final boolean wide = lying;
        final int pageH = Math.min(previewH - 4, wide ? previewW * 3 / 4 : previewH - 4);
        final int pageW = Math.min(previewW - 4, wide ? previewW - 4 : pageH * 3 / 4);
        final int px = previewX + (previewW - pageW) / 2;
        final int py = previewY + (previewH - pageH) / 2;
        g.fill(previewX, previewY, previewX + previewW, previewY + previewH, skin.panelBg());
        g.fill(px, py, px + pageW, py + pageH, skin.fieldBg());
        if (document.isPicture()) {
            drawPicturePreview(g, px, py, pageW, pageH);
        } else {
            final List<String> pages = PrintLayout.pages(document.title(), document.text(), lying);
            final String[] lines = pages.get(Math.min(previewPage, pages.size() - 1)).split("\n", -1);
            final int columns = lying ? PrintLayout.LANDSCAPE_COLUMNS : PrintLayout.PORTRAIT_COLUMNS;
            final int room = pageW - PREVIEW_MARGIN * 2;
            final int step = Math.max(2, Math.min(PREVIEW_LINE, (pageH - PREVIEW_MARGIN * 2) / Math.max(1,
                    lying ? PrintLayout.LANDSCAPE_LINES : PrintLayout.PORTRAIT_LINES)));
            for (int i = 0; i < lines.length; i++) {
                final int len = lines[i].stripTrailing().length();
                if (len == 0) {
                    continue;
                }
                final int bar = Math.max(1, room * Math.min(columns, len) / columns);
                final int ly = py + PREVIEW_MARGIN + i * step;
                g.fill(px + PREVIEW_MARGIN, ly, px + PREVIEW_MARGIN + bar, ly + Math.max(1, step - 2),
                        i == 0 ? skin.text() : skin.dim());
            }
        }
        final String count = GameText.resolve(PrintTexts.PREVIEW_PAGE.with(previewPage + 1, pageCount()));
        Texts.small(g, font, count, previewX + previewW / 2 - Texts.smallWidth(font, count) / 2,
                previewY + previewH + 4, skin.dim());
    }

    /* A picture's preview: as the chosen printer would put it on paper, fitted to the page. */
    private void drawPicturePreview(final GuiGraphics g, final int px, final int py, final int pageW,
                                    final int pageH) {
        if (printers.isEmpty()) {
            return;
        }
        final PrinterModel model = PrinterModel.find(printers.get(Math.min(chosen, printers.size() - 1)).model());
        if (model == null) {
            return;
        }
        final PrintedPictureTextures.Entry ink = PrintedPictureTextures.ink(PrintedDocument.picture(document.title(),
                "", document.program(), document.picture(), document.pictureName()).printedBy(model));
        if (ink == null) {
            return;
        }
        final int room = pageW - PREVIEW_MARGIN * 2;
        final float fit = Math.min((float) room / ink.width(), (float) (pageH - PREVIEW_MARGIN * 2) / ink.height());
        final int w = Math.max(1, (int) (ink.width() * fit));
        final int h = Math.max(1, (int) (ink.height() * fit));
        RenderSystem.enableBlend();
        g.blit(ink.texture(), px + (pageW - w) / 2, py + PREVIEW_MARGIN, w, h, 0, 0, ink.width(), ink.height(),
                ink.width(), ink.height());
        RenderSystem.disableBlend();
    }

    private void choose(final int index) {
        if (index >= 0 && index < printers.size()) {
            chosen = index;
            printerList.setSelected(index);
        }
    }

    private void lie(final boolean wide) {
        lying = wide;
        previewPage = Math.min(previewPage, pageCount() - 1);
        to.set(String.valueOf(pageCount()));
    }

    private void turnPreview(final int step) {
        previewPage = Math.max(0, Math.min(pageCount() - 1, previewPage + step));
    }

    private int pageCount() {
        return document.isPicture() ? 1 : PrintLayout.pages(document.title(), document.text(), lying).size();
    }

    @Nullable
    private PrintersPayload.Row current() {
        return printers.isEmpty() ? null : printers.get(Math.min(chosen, printers.size() - 1));
    }

    /*
     * The bar for the wait while the machine looks for printers, on the line the printer's place takes once one is
     * found: the search has no known end, so the bar runs rather than fills.
     */
    private void placeLooking(final int x, final int y, final int w) {
        looking.setBounds(x, y, Math.min(w, LOOKING_W), 7);
        looking.setVisible(current() == null && !listed);
    }

    private String statusLine() {
        final PrintersPayload.Row row = current();
        if (row == null) {
            return GameText.resolve(listed ? Printers.NO_PRINTER : PrintTexts.LOOKING);
        }
        return GameText.resolve(PrintTexts.STATUS.with(row.status()));
    }

    private String locationLine() {
        final PrintersPayload.Row row = current();
        return row == null ? "" : GameText.resolve(PrintTexts.LOCATION.with(row.port(), machine));
    }

    private String paperLine() {
        final PrintersPayload.Row row = current();
        return row == null ? "" : GameText.resolve(PrintTexts.PAPER.with(row.paper()));
    }

    private String noteLine() {
        final PrintersPayload.Row row = current();
        if (row == null) {
            return "";
        }
        final int sheets = sheets();
        return GameText.resolve(sheets == 1 ? PrintTexts.USES_ONE.with(row.paper())
                : PrintTexts.USES.with(sheets, row.paper()));
    }

    private static Text name(final PrintersPayload.Row row) {
        final PrinterModel model = PrinterModel.find(row.model());
        return model == null ? Text.literal(row.model()) : model.displayName().text();
    }

    private static int parse(final String value, final int fallback) {
        try {
            return Integer.parseInt(value.strip());
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * What a program prints: a document of text, or a picture.
     *
     * @param title       the document's title
     * @param program     the program printing it
     * @param text        its text, empty for a picture
     * @param picture     a picture as Paint saves it, or empty
     * @param pictureName the picture's file name, or empty
     */
    public record Document(String title, String program, String text, String picture, String pictureName) {

        /** A document of text. */
        public static Document text(final String title, final String program, final String text) {
            return new Document(title, program, text, "", "");
        }

        /** A picture. */
        public static Document picture(final String title, final String program, final String picture) {
            return new Document(title, program, "", picture, title);
        }

        public boolean isPicture() {
            return !picture.isEmpty();
        }
    }
}
