/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.operation.payload.teracoin.WalletRequestPayload;
import dev.jstech.computers.operation.payload.teracoin.WalletStatePayload;
import dev.jstech.computers.teracoin.Printer3DModel;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.CellGrid;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.client.os.TabbedWindowLook.FIELD_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.PAD;

/**
 * The Crypto Wallet's Print tab (computers/docs/design/teracoin.md, "The 3D Printer"): which of the computer's 3D
 * printers prints, the server's price list as tiles, the order with its copies, its total and how long it takes, and
 * the Auto print dialog, where the wallet prints one item by itself as its balance grows past what it keeps. The
 * computer answers everything; this draws it and sends what the player chose.
 */
final class CryptoWalletPrint {

    private final BlockPos host;
    private final Supplier<WalletStatePayload> state;
    private final Supplier<String> wallet;
    private final Supplier<String> note;
    private final IntSupplier gold;
    private final Runnable sent;
    private final Panel root;
    private final Map<String, Shown> shown = new HashMap<>();
    private String item = "";
    private long printer;
    private int printerCount = -1;
    private int priceCount = -1;
    private boolean autoChosen;
    private String autoItem = "";
    private long autoPrinter;
    private boolean keepRefused;
    private int orderX;
    private int orderY;
    private int orderW;
    private int gridW;
    private int ruleY;
    private final CellGrid grid;
    private Dropdown printerChoice;
    private final TextField copies;
    private final Button print;
    private final Button autoButton;
    private final Popup autoPopup;
    private final Checkbox byItself;
    private final Label itemLabel;
    private Dropdown itemChoice;
    private final Label printerLabel;
    private Dropdown autoPrinterChoice;
    private final Label keepLabel;
    private final TextField keep;
    private final Label trcLabel;
    private final Explained explained;
    private final Label lastLine;
    private final Label noteLine;
    private final Button ok;
    private final Button cancel;

    /* The printer's drop-down, after its label. */
    private static final int LABEL_W = 43;
    private static final int CHOICE_W = 127;
    /* The price list's tiles, from the tab's top, three across when the order beside them has its room. */
    private static final int GRID_DY = 44;
    private static final int CELL_W = 104;
    private static final int CELL_H = 31;
    private static final int INSET = 2;
    private static final int MOST_COLUMNS = 3;
    private static final int NAME_LINES = 2;
    private static final int NAME_PITCH = 9;
    private static final int ORDER_MIN = 150;
    private static final int GAP = 8;
    private static final int ORDER_H = 127;
    private static final int ORDER_TEXT_X = 48;
    private static final int AUTO_H = 26;
    private static final int COPIES_W = 26;
    private static final int SPIN_W = 11;
    private static final int POPUP_W = 360;
    private static final int POPUP_H = 220;
    private static final int POPUP_LABEL_W = 78;
    private static final int EXPLAINED_PITCH = 11;
    private static final float BIG_ICON = 2f;
    private static final float TILE_ICON = 4f / 3f;
    private static final float CHOICE_ICON = 0.7f;
    /* The example's leftover, in thousandths of the price: 1.43 TRC over a price of 10, as the design shows it. */
    private static final long EXAMPLE_LEFT_PERMILLE = 143L;
    private static final int EXAMPLE_COUNT = 4;
    /* The room the item's icon takes at the start of the item list's caption. */
    private static final String ICON_ROOM = "   ";

    /**
     * The tab of the wallet of the computer at {@code host}.
     *
     * @param root   the wallet's panel, which the tab's controls join
     * @param state  the computer's last answer, or null before the first
     * @param wallet the path of the wallet shown
     * @param note   the note of the last request while it is fresh, or empty
     * @param gold   Blockwright's gold, the colour of money
     * @param sent   told when a request leaves, so the wallet waits for the answer before asking again
     */
    CryptoWalletPrint(final Panel root, final BlockPos host, final Supplier<WalletStatePayload> state,
                      final Supplier<String> wallet, final Supplier<String> note, final IntSupplier gold,
                      final Runnable sent) {
        this.root = root;
        this.host = host;
        this.state = state;
        this.wallet = wallet;
        this.note = note;
        this.gold = gold;
        this.sent = sent;
        grid = root.add(new CellGrid(3, 4, 4, CELL_W, CELL_H).setInset(INSET).setWells(false)
                .setCues(CellGrid.Cues.RIGHT)
                .setSelected(index -> index < prices().size() && prices().get(index).item().equals(item))
                .setRenderer(this::renderTile)
                .setOnClick((index, button, shift) -> {
                    if (index < prices().size()) {
                        item = prices().get(index).item();
                    }
                }));
        copies = root.add(new TextField(2));
        copies.set("1");
        print = root.add(new Button(GameText.resolve(CryptoWalletTexts.PRINT), this::sendPrint).setPrimary(true));
        autoButton = root.add(new Button(GameText.resolve(CryptoWalletTexts.AUTO_PRINT_BUTTON), this::openAuto));

        autoPopup = new Popup(GameText.resolve(CryptoWalletTexts.AUTO_PRINT), POPUP_W, POPUP_H)
                .setLayouter(this::layoutAuto);
        byItself = autoPopup.add(new Checkbox(() -> GameText.resolve(CryptoWalletTexts.BY_ITSELF), () -> autoChosen,
                () -> autoChosen = !autoChosen));
        itemLabel = autoPopup.add(new Label(GameText.resolve(CryptoWalletTexts.ITEM)));
        printerLabel = autoPopup.add(new Label(GameText.resolve(CryptoWalletTexts.PRINTER)));
        keepLabel = autoPopup.add(new Label(GameText.resolve(CryptoWalletTexts.KEEP_AT_LEAST)));
        keep = autoPopup.add(new TextField(20));
        trcLabel = autoPopup.add(new Label(GameText.resolve(CryptoWalletTexts.TRC)));
        explained = autoPopup.add(new Explained());
        lastLine = autoPopup.add(new Label(this::lastPrinted, Label.Tone.DIM));
        noteLine = autoPopup.add(new Label(() -> keepRefused ? GameText.resolve(CryptoWalletTexts.KEEP_INVALID)
                : note.get()).setColor(gold));
        ok = autoPopup.add(new Button(GameText.resolve(CryptoWalletTexts.OK), this::confirmAuto).setPrimary(true));
        cancel = autoPopup.add(new Button(GameText.resolve(CryptoWalletTexts.CANCEL), autoPopup::close));
        syncChoices();
    }

    /** The Auto print dialog, which the wallet shows over everything while it is open. */
    Popup autoPopup() {
        return autoPopup;
    }

    /** Takes the computer's answer: an Auto print set or turned off closes the dialog. */
    void take(final WalletStatePayload payload) {
        final int code = payload.current().note();
        if (code == WalletStatePayload.NOTE_AUTO_SET || code == WalletStatePayload.NOTE_AUTO_OFF) {
            autoPopup.close();
        }
    }

    /**
     * Places the tab's controls under the balance, from {@code top} to {@code bottom}, or hides them when the tab is
     * not {@code showing}.
     */
    void layout(final int x, final int top, final int width, final int bottom, final boolean showing) {
        syncChoices();
        final List<WalletStatePayload.Price> prices = prices();
        final List<WalletStatePayload.Printer> printers = printers();
        final int px = x + PAD;
        final int pw = width - 2 * PAD;
        final int columns = Math.max(2, Math.min(MOST_COLUMNS, (pw - ORDER_MIN - GAP) / CELL_W));
        final int totalRows = Math.max(1, (prices.size() + columns - 1) / columns);
        final int gridTop = top + GRID_DY;
        final int rows = Math.max(1, Math.min(totalRows, (bottom - AUTO_H - gridTop) / CELL_H));
        grid.setColumns(columns).setVisibleRows(rows).setTotalRows(totalRows).setCellCount(prices.size())
                .place(px - 1, gridTop);
        grid.setVisible(showing && !prices.isEmpty());
        if (!prices.isEmpty() && indexOfPrice(item) < 0) {
            item = prices.get(0).item();
        }
        if (!printers.isEmpty() && indexOfPrinter(printer) < 0) {
            printer = printers.get(0).pos();
        }
        printerChoice.setVisible(showing && !printers.isEmpty());
        printerChoice.setBounds(px + LABEL_W, top + 9, CHOICE_W, FIELD_H);
        gridW = columns * CELL_W - INSET;
        ruleY = gridTop + rows * CELL_H + 7;
        orderX = px - 1 + columns * CELL_W + GAP;
        orderY = gridTop;
        orderW = x + width - PAD - orderX;
        final boolean ordering = showing && indexOfPrice(item) >= 0;
        copies.setVisible(ordering);
        copies.setBounds(orderX + 47, orderY + 48, COPIES_W, FIELD_H);
        print.setVisible(showing);
        autoButton.setVisible(showing);
        print.setBounds(orderX, orderY + ORDER_H + 6, orderW / 2 - 3, FIELD_H);
        autoButton.setBounds(orderX + orderW / 2 + 3, orderY + ORDER_H + 6, orderW - orderW / 2 - 3, FIELD_H);
        print.setEnabled(ordering && !printers.isEmpty());
        autoButton.setEnabled(!prices.isEmpty() && (!printers.isEmpty() || auto().on()));
    }

    /** Draws what the tab's controls do not: the words, the order and the Auto print line. */
    void render(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int top, final int width) {
        final int px = x + PAD;
        final int right = x + width - PAD;
        Draw.text(g, font, GameText.resolve(CryptoWalletTexts.PRINTER), px + 1, top + 13, skin.text());
        if (printers().isEmpty()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(CryptoWalletTexts.NONE_LINKED),
                    right - (px + LABEL_W)), px + LABEL_W, top + 13, skin.dim());
        } else {
            final int captionX = px + LABEL_W + CHOICE_W + 7;
            Draw.text(g, font, Texts.clip(font, GameText.resolve(CryptoWalletTexts.ONE_OF_LINKED),
                    right - captionX), captionX, top + 13, skin.dim());
        }
        Draw.text(g, font, GameText.resolve(CryptoWalletTexts.WHAT_PRINTS), px + 1, top + 32, skin.dim());
        if (prices().isEmpty()) {
            Draw.text(g, font, GameText.resolve(CryptoWalletTexts.PRINTS_NOTHING), px + 3, top + GRID_DY + 4,
                    skin.dim());
        }
        renderOrder(g, font, skin);
        g.fill(px - 1, ruleY, px - 1 + gridW, ruleY + 1, skin.edge());
        Draw.text(g, font, GameText.resolve(CryptoWalletTexts.AUTO_PRINT), px + 1, ruleY + 7, skin.text());
        final WalletStatePayload.Auto auto = auto();
        final String line = auto.on() ? GameText.resolve(CryptoWalletTexts.AUTO_ON.with(nameOf(auto.item()),
                printerName(auto.printer()), Trc.plain(auto.keep())))
                : GameText.resolve(CryptoWalletTexts.AUTO_OFF);
        final int lineX = x + 69;
        Draw.text(g, font, Texts.clip(font, line, px - 1 + gridW - lineX), lineX, ruleY + 7,
                auto.on() ? skin.text() : skin.dim());
    }

    /** Draws the item over the Auto print dialog's item list, once the dialog is drawn. */
    void renderAutoOver(final GuiGraphics g) {
        final int index = indexOfPrice(autoItem);
        if (autoPopup.isOpen() && itemChoice.visible() && index >= 0) {
            g.pose().pushPose();
            g.pose().translate(itemChoice.x() + 3, itemChoice.y() + 2, 0f);
            g.pose().scale(CHOICE_ICON, CHOICE_ICON, CHOICE_ICON);
            DesktopItems.item(g, shownOf(prices().get(index).item()).stack(), 0, 0);
            g.pose().popPose();
        }
    }

    /** A click on the copies' arrows; whether it was one. */
    boolean mouseClicked(final double mouseX, final double mouseY) {
        if (!copies.visible()) {
            return false;
        }
        final int sx = copies.right() + 1;
        final int sy = copies.y();
        if (mouseX < sx || mouseX >= sx + SPIN_W || mouseY < sy || mouseY >= sy + FIELD_H) {
            return false;
        }
        final int now = copiesChosen();
        final int next = mouseY < sy + FIELD_H / 2 ? now + 1 : now - 1;
        copies.set(String.valueOf(Math.max(1, Math.min(Printer3DBlockEntity.MOST_COPIES, next))));
        return true;
    }

    /** Chooses {@code itemId} and {@code count} copies and presses Print, as a player would. */
    void printNow(final String itemId, final int count) {
        item = itemId;
        copies.set(String.valueOf(count));
        sendPrint();
    }

    /** Sets the wallet's Auto print to {@code itemId}, keeping {@code kept} TRC, as the dialog does. */
    void autoPrint(final String itemId, final String kept) {
        openAuto();
        autoChosen = true;
        autoItem = itemId;
        keep.set(kept);
        confirmAuto();
    }

    /** Turns the wallet's Auto print off, as the dialog does with its box unticked. */
    void autoPrintOff() {
        openAuto();
        autoChosen = false;
        confirmAuto();
    }

    private void sendPrint() {
        if (indexOfPrice(item) < 0 || indexOfPrinter(printer) < 0) {
            return;
        }
        PacketDistributor.sendToServer(new WalletRequestPayload(host, WalletRequestPayload.PRINT, wallet.get(), item,
                "", 0L, printer, copiesChosen()));
        sent.run();
    }

    private void openAuto() {
        final WalletStatePayload.Auto auto = auto();
        autoChosen = true;
        autoItem = auto.on() ? auto.item() : item;
        autoPrinter = auto.on() ? auto.printer() : printer;
        final WalletStatePayload payload = state.get();
        final long balance = payload == null ? 0L : payload.current().balance();
        keep.set(Trc.plain(auto.on() ? auto.keep() : balance - balance % Trc.UNITS));
        keepRefused = false;
        autoPopup.open();
    }

    private void confirmAuto() {
        final long kept = Trc.parse(keep.edit());
        if (autoChosen && kept < 0L) {
            keepRefused = true;
            return;
        }
        keepRefused = false;
        final boolean on = autoChosen && indexOfPrice(autoItem) >= 0 && indexOfPrinter(autoPrinter) >= 0;
        PacketDistributor.sendToServer(new WalletRequestPayload(host, WalletRequestPayload.AUTO_PRINT, wallet.get(),
                on ? autoItem : "", "", on ? kept : 0L, on ? autoPrinter : 0L, 0));
        sent.run();
    }

    private void layoutAuto(final Popup p) {
        syncChoices();
        final Font font = Minecraft.getInstance().font;
        final int left = p.x() + 9;
        final int top = p.contentTop() + 9;
        final int inner = p.width() - 18;
        final int choiceX = left + POPUP_LABEL_W;
        byItself.setBounds(left, top, inner, 11);
        itemLabel.setBounds(left, top + 22, POPUP_LABEL_W - 4, 9);
        itemChoice.setBounds(choiceX, top + 19, inner - POPUP_LABEL_W, FIELD_H);
        printerLabel.setBounds(left, top + 43, POPUP_LABEL_W - 4, 9);
        autoPrinterChoice.setBounds(choiceX, top + 40, inner - POPUP_LABEL_W, FIELD_H);
        keepLabel.setBounds(left, top + 64, POPUP_LABEL_W - 4, 9);
        keep.setBounds(choiceX, top + 61, 53, FIELD_H);
        trcLabel.setBounds(choiceX + 58, top + 64, 24, 9);
        final int boxTop = top + 84;
        explained.setBounds(left, boxTop, inner, explained.heightFor(font, inner));
        lastLine.setBounds(left, explained.bottom() + 8, inner, 9);
        cancel.setBounds(p.right() - 68, p.bottom() - FIELD_H - 8, 60, FIELD_H);
        ok.setBounds(p.right() - 133, p.bottom() - FIELD_H - 8, 60, FIELD_H);
        noteLine.setBounds(left, ok.y() + 4, ok.x() - 8 - left, 9);
        final boolean settable = !prices().isEmpty() && !printers().isEmpty();
        ok.setEnabled(!autoChosen || settable);
    }

    private void renderOrder(final GuiGraphics g, final Font font, final OsSkin skin) {
        final int index = indexOfPrice(item);
        if (index < 0) {
            return;
        }
        final WalletStatePayload.Price price = prices().get(index);
        final Shown one = shownOf(price.item());
        g.fill(orderX, orderY, orderX + orderW, orderY + ORDER_H, skin.panelBg());
        Draw.outline(g, orderX, orderY, orderW, ORDER_H, skin.edge());
        g.pose().pushPose();
        g.pose().translate(orderX + 8, orderY + 8, 0f);
        g.pose().scale(BIG_ICON, BIG_ICON, BIG_ICON);
        DesktopItems.item(g, one.stack(), 0, 0);
        g.pose().popPose();
        final int textX = orderX + ORDER_TEXT_X;
        final int textW = orderX + orderW - 6 - textX;
        TabbedWindowLook.heading(g, font, Texts.clip(font, one.name(), (int) (textW / TabbedWindowLook.HEADING)), textX,
                orderY + 11, skin.text());
        Draw.text(g, font, Texts.clip(font, GameText.resolve(CryptoWalletTexts.EACH.with(Trc.plain(price.units()))),
                textW), textX, orderY + 27, gold.getAsInt());
        Draw.text(g, font, GameText.resolve(CryptoWalletTexts.COPIES), orderX + 8, orderY + 52, skin.text());
        final int sx = copies.right() + 1;
        skin.field(g, sx, copies.y(), SPIN_W, FIELD_H, false);
        Draw.text(g, font, "+", sx + 3, copies.y(), skin.dim());
        Draw.text(g, font, "-", sx + 3, copies.y() + 6, skin.dim());
        final int count = copiesChosen();
        Draw.text(g, font, GameText.resolve(CryptoWalletTexts.TOTAL), orderX + 8, orderY + 71, skin.text());
        final String total = GameText.resolve(CryptoWalletTexts.AMOUNT_TRC.with(Trc.plain(price.units() * count)));
        TabbedWindowLook.heading(g, font, total, orderX + orderW - 8 - Math.round(font.width(total)
                * TabbedWindowLook.HEADING), orderY + 69, gold.getAsInt());
        final int printerIndex = indexOfPrinter(printer);
        int noteY = orderY + 85;
        if (printerIndex >= 0) {
            final int seconds = Printer3DModel.byName(printers().get(printerIndex).model()).itemSeconds();
            Draw.text(g, font, Texts.clip(font, GameText.resolve(CryptoWalletTexts.TAKES.with(
                    duration(seconds * count), seconds)), orderW - 16), orderX + 8, noteY, skin.dim());
            noteY += 11;
        }
        TabbedWindowLook.paragraph(g, font, GameText.resolve(CryptoWalletTexts.PAID_AS_STARTS), orderX + 8, noteY,
                orderW - 16, NAME_PITCH, skin.dim());
    }

    private void renderTile(final GuiGraphics g, final UiContext ctx, final int index, final int x, final int y,
                            final int w, final int h, final boolean hovered) {
        if (index >= prices().size()) {
            return;
        }
        final WalletStatePayload.Price price = prices().get(index);
        final boolean chosen = price.item().equals(item);
        ctx.skin().listRow(g, x, y, w, h, hovered, chosen);
        Draw.outline(g, x, y, w, h, chosen ? ctx.skin().accent() : ctx.skin().edge());
        final Shown one = shownOf(price.item());
        g.pose().pushPose();
        g.pose().translate(x + 4, y + (h - 21) / 2, 0f);
        g.pose().scale(TILE_ICON, TILE_ICON, TILE_ICON);
        DesktopItems.item(g, one.stack(), 0, 0);
        g.pose().popPose();
        final List<FormattedCharSequence> lines = TabbedWindowLook.lines(ctx.font(), one.name(), w - 35);
        final int nameLines = Math.min(NAME_LINES, lines.size());
        for (int i = 0; i < nameLines; i++) {
            Draw.text(g, ctx.font(), lines.get(i), x + 29, y + 3 + i * NAME_PITCH, ctx.skin().text());
        }
        Draw.text(g, ctx.font(), GameText.resolve(CryptoWalletTexts.AMOUNT_TRC.with(Trc.plain(price.units()))),
                x + 29, y + 4 + Math.max(1, nameLines) * NAME_PITCH, gold.getAsInt());
    }

    /* Builds the drop-downs again when the printers or the prices come and go, so each lists what there is. */
    private void syncChoices() {
        if (printers().size() != printerCount) {
            printerCount = printers().size();
            printerChoice = rebuilt(root, printerChoice, printerCount, this::printerOption,
                    () -> indexOfPrinter(printer), index -> printer = printerAt(index), words -> words);
            autoPrinterChoice = rebuilt(autoPopup, autoPrinterChoice, printerCount, this::printerOption,
                    () -> indexOfPrinter(autoPrinter), index -> autoPrinter = printerAt(index),
                    words -> words.isEmpty() ? GameText.resolve(CryptoWalletTexts.NONE_LINKED)
                            : GameText.resolve(CryptoWalletTexts.LINKED_OPTION.with(words)));
        }
        if (prices().size() != priceCount) {
            priceCount = prices().size();
            itemChoice = rebuilt(autoPopup, itemChoice, priceCount, this::priceOption,
                    () -> indexOfPrice(autoItem), index -> autoItem = index < prices().size()
                            ? prices().get(index).item() : "", words -> words.isEmpty() ? "" : ICON_ROOM + words);
        }
    }

    private String printerOption(final int index) {
        final List<WalletStatePayload.Printer> printers = printers();
        if (index >= printers.size()) {
            return "";
        }
        final String model = printers.get(index).model();
        int same = 0;
        int nth = 0;
        for (int i = 0; i < printers.size(); i++) {
            if (printers.get(i).model().equals(model)) {
                same++;
                if (i <= index) {
                    nth++;
                }
            }
        }
        return same > 1 ? GameText.resolve(CryptoWalletTexts.PRINTER_NTH.with(model, nth)) : model;
    }

    private String priceOption(final int index) {
        if (index >= prices().size()) {
            return "";
        }
        final WalletStatePayload.Price price = prices().get(index);
        return GameText.resolve(CryptoWalletTexts.PRICE_OPTION.with(shownOf(price.item()).name(),
                Trc.plain(price.units())));
    }

    private String printerName(final long pos) {
        final int index = indexOfPrinter(pos);
        return index < 0 ? GameText.resolve(CryptoWalletTexts.NOT_LINKED_NOW) : printerOption(index);
    }

    private String lastPrinted() {
        final WalletStatePayload.Auto auto = auto();
        if (!auto.on() || auto.lastCount() <= 0) {
            return GameText.resolve(CryptoWalletTexts.AUTO_NEVER);
        }
        final long dayTime = auto.lastDayTime();
        return GameText.resolve(CryptoWalletTexts.AUTO_LAST.with(auto.lastCount(), GameCalendar.day(dayTime) + 1,
                String.format(Locale.ROOT, "%02d:%02d", GameCalendar.hourOf(dayTime), GameCalendar.minuteOf(dayTime)),
                String.format(Locale.ROOT, "%,d", auto.lastBlock())));
    }

    /* The worked example of the dialog, from the item chosen and what the wallet keeps. */
    private String example() {
        final int index = Math.max(0, indexOfPrice(autoItem));
        final long price = prices().isEmpty() ? Trc.of(10) : prices().get(index).units();
        final long typed = Trc.parse(keep.edit());
        final long kept = Math.max(0L, typed);
        final long left = price * EXAMPLE_LEFT_PERMILLE / 1000L;
        return GameText.resolve(CryptoWalletTexts.AUTO_EXPLAINED.with(Trc.plain(kept + EXAMPLE_COUNT * price + left),
                EXAMPLE_COUNT, Trc.plain(kept + left)));
    }

    private int copiesChosen() {
        final String typed = copies.edit().trim();
        if (typed.isEmpty() || !typed.chars().allMatch(Character::isDigit)) {
            return 1;
        }
        return Math.max(1, Math.min(Printer3DBlockEntity.MOST_COPIES, Integer.parseInt(typed)));
    }

    private String duration(final int seconds) {
        if (seconds < 60) {
            return GameText.resolve(CryptoWalletTexts.SECONDS.with(seconds));
        }
        return seconds % 60 == 0 ? GameText.resolve(CryptoWalletTexts.MINUTES.with(seconds / 60))
                : GameText.resolve(CryptoWalletTexts.MINUTES_SECONDS.with(seconds / 60, seconds % 60));
    }

    private int indexOfPrice(final String id) {
        final List<WalletStatePayload.Price> prices = prices();
        for (int i = 0; i < prices.size(); i++) {
            if (prices.get(i).item().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfPrinter(final long pos) {
        final List<WalletStatePayload.Printer> printers = printers();
        for (int i = 0; i < printers.size(); i++) {
            if (printers.get(i).pos() == pos) {
                return i;
            }
        }
        return -1;
    }

    private long printerAt(final int index) {
        final List<WalletStatePayload.Printer> printers = printers();
        return index >= 0 && index < printers.size() ? printers.get(index).pos() : 0L;
    }

    private List<WalletStatePayload.Price> prices() {
        final WalletStatePayload payload = state.get();
        return payload == null ? List.of() : payload.print().prices();
    }

    private List<WalletStatePayload.Printer> printers() {
        final WalletStatePayload payload = state.get();
        return payload == null ? List.of() : payload.print().printers();
    }

    private WalletStatePayload.Auto auto() {
        final WalletStatePayload payload = state.get();
        return payload == null ? WalletStatePayload.Auto.OFF : payload.print().auto();
    }

    private String nameOf(final String id) {
        return shownOf(id).name();
    }

    /* The item of {@code id} as the tab shows it, its stack and its name, made once. */
    private Shown shownOf(final String id) {
        return shown.computeIfAbsent(id, key -> {
            final ResourceLocation location = ResourceLocation.tryParse(key);
            if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
                return new Shown(ItemStack.EMPTY, key);
            }
            final ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(location));
            return new Shown(stack, stack.getHoverName().getString());
        });
    }

    /* A drop-down of {@code count} options read from {@code option}, put in {@code parent} in place of {@code old}. */
    private static Dropdown rebuilt(final Panel parent, @Nullable final Dropdown old, final int count,
                                    final IntFunction<String> option, final IntSupplier chosen,
                                    final IntConsumer onChoose, final Function<String, String> caption) {
        if (old != null) {
            parent.remove(old);
        }
        final List<Supplier<String>> options = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int index = i;
            options.add(() -> option.apply(index));
        }
        return parent.add(new Dropdown(options, chosen, onChoose, caption));
    }

    /* An item as the tab shows it: the stack its icon is drawn from and its name. */
    private record Shown(ItemStack stack, String name) {
    }

    /* The box that says how Auto print works, its example worked out from the item and what the wallet keeps. */
    private final class Explained extends UiComponent {

        /* How tall the box is at {@code width}: its two paragraphs and a margin. */
        int heightFor(final Font font, final int width) {
            final int lines = TabbedWindowLook.lines(font, example(), width - 10).size()
                    + TabbedWindowLook.lines(font, GameText.resolve(CryptoWalletTexts.AUTO_RUNS), width - 10).size();
            return lines * EXPLAINED_PITCH + 7;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            g.fill(x(), y(), right(), bottom(), ctx.skin().panelBg());
            Draw.outline(g, x(), y(), width(), height(), ctx.skin().edge());
            final int used = TabbedWindowLook.paragraph(g, ctx.font(), example(), x() + 5, y() + 4, width() - 10,
                    EXPLAINED_PITCH, ctx.skin().text());
            TabbedWindowLook.paragraph(g, ctx.font(), GameText.resolve(CryptoWalletTexts.AUTO_RUNS), x() + 5,
                    y() + 4 + used, width() - 10, EXPLAINED_PITCH, ctx.skin().dim());
        }
    }
}
