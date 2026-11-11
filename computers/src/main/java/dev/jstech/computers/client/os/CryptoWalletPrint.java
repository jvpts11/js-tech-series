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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

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

    private static final int PAD = 6;
    private static final int LABEL_W = 34;
    private static final int CHOICE_W = 96;
    private static final int FIELD_H = 12;
    private static final int BUTTON_H = 12;
    private static final int GRID_DY = 30;
    private static final int CELL_W = 72;
    private static final int CELL_H = 24;
    private static final int INSET = 2;
    private static final int MOST_COLUMNS = 4;
    private static final int ORDER_MIN = 110;
    private static final int GAP = 9;
    private static final int ORDER_H = 94;
    private static final int AUTO_H = 18;
    private static final int COPIES_W = 24;
    private static final int SPIN_W = 9;
    private static final int POPUP_W = 270;
    private static final int POPUP_H = 166;
    private static final int POPUP_LABEL_W = 56;
    private static final float SMALL = Texts.SMALL;
    private static final float BIG_ICON = 1.5f;
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
        itemLabel = autoPopup.add(new Label(GameText.resolve(CryptoWalletTexts.ITEM)).setScale(SMALL));
        printerLabel = autoPopup.add(new Label(GameText.resolve(CryptoWalletTexts.PRINTER)).setScale(SMALL));
        keepLabel = autoPopup.add(new Label(GameText.resolve(CryptoWalletTexts.KEEP_AT_LEAST)).setScale(SMALL));
        keep = autoPopup.add(new TextField(20));
        trcLabel = autoPopup.add(new Label(GameText.resolve(CryptoWalletTexts.TRC)).setScale(SMALL));
        explained = autoPopup.add(new Explained());
        lastLine = autoPopup.add(new Label(this::lastPrinted, Label.Tone.DIM).setScale(SMALL));
        noteLine = autoPopup.add(new Label(() -> keepRefused ? GameText.resolve(CryptoWalletTexts.KEEP_INVALID)
                : note.get()).setColor(gold).setScale(SMALL));
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
                .place(px, gridTop);
        grid.setVisible(showing && !prices.isEmpty());
        if (!prices.isEmpty() && indexOfPrice(item) < 0) {
            item = prices.get(0).item();
        }
        if (!printers.isEmpty() && indexOfPrinter(printer) < 0) {
            printer = printers.get(0).pos();
        }
        printerChoice.setVisible(showing && !printers.isEmpty());
        printerChoice.setBounds(px + LABEL_W, top + 5, CHOICE_W, FIELD_H);
        gridW = columns * CELL_W - INSET;
        ruleY = gridTop + rows * CELL_H + 1;
        orderX = px + gridW + GAP;
        orderY = gridTop;
        orderW = px + pw - orderX;
        final boolean ordering = showing && indexOfPrice(item) >= 0;
        copies.setVisible(ordering);
        copies.setBounds(orderX + 34, orderY + 36, COPIES_W, FIELD_H);
        print.setVisible(showing);
        autoButton.setVisible(showing);
        print.setBounds(orderX, orderY + ORDER_H + 3, orderW / 2 - 2, BUTTON_H);
        autoButton.setBounds(orderX + orderW / 2 + 2, orderY + ORDER_H + 3, orderW - orderW / 2 - 2, BUTTON_H);
        print.setEnabled(ordering && !printers.isEmpty());
        autoButton.setEnabled(!prices.isEmpty() && (!printers.isEmpty() || auto().on()));
    }

    /** Draws what the tab's controls do not: the words, the order and the Auto print line. */
    void render(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int top, final int width) {
        final int px = x + PAD;
        final int pw = width - 2 * PAD;
        Texts.small(g, font, GameText.resolve(CryptoWalletTexts.PRINTER), px, top + 8, skin.text());
        if (printers().isEmpty()) {
            Texts.small(g, font, GameText.resolve(CryptoWalletTexts.NONE_LINKED), px + LABEL_W, top + 8, skin.dim());
        } else {
            final int captionX = px + LABEL_W + CHOICE_W + 6;
            Texts.small(g, font, Texts.clip(font, GameText.resolve(CryptoWalletTexts.ONE_OF_LINKED),
                    (int) ((px + pw - captionX) / SMALL)), captionX, top + 8, skin.dim());
        }
        Texts.small(g, font, GameText.resolve(CryptoWalletTexts.WHAT_PRINTS), px, top + 21, skin.dim());
        if (prices().isEmpty()) {
            Texts.small(g, font, GameText.resolve(CryptoWalletTexts.PRINTS_NOTHING), px + 3, top + GRID_DY + 4,
                    skin.dim());
        }
        renderOrder(g, font, skin);
        g.fill(px, ruleY, px + gridW, ruleY + 1, skin.edge());
        Texts.small(g, font, GameText.resolve(CryptoWalletTexts.AUTO_PRINT), px, ruleY + 5, skin.text());
        final WalletStatePayload.Auto auto = auto();
        final String line = auto.on() ? GameText.resolve(CryptoWalletTexts.AUTO_ON.with(nameOf(auto.item()),
                printerName(auto.printer()), Trc.plain(auto.keep())))
                : GameText.resolve(CryptoWalletTexts.AUTO_OFF);
        final int lineX = px + 46;
        Texts.small(g, font, Texts.clip(font, line, (int) ((px + gridW - lineX) / SMALL)), lineX, ruleY + 5,
                auto.on() ? skin.text() : skin.dim());
    }

    /** Draws the item over the Auto print dialog's item list, once the dialog is drawn. */
    void renderAutoOver(final GuiGraphics g) {
        final int index = indexOfPrice(autoItem);
        if (autoPopup.isOpen() && itemChoice.visible() && index >= 0) {
            g.pose().pushPose();
            g.pose().translate(itemChoice.x() + 5, itemChoice.y() + 2, 0f);
            g.pose().scale(0.5f, 0.5f, 0.5f);
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
        final int left = p.x() + 8;
        final int top = p.contentTop() + 4;
        final int inner = p.width() - 16;
        final int choiceX = left + POPUP_LABEL_W;
        byItself.setBounds(left, top, inner, 11);
        itemLabel.setBounds(left, top + 18, POPUP_LABEL_W - 4, 7);
        itemChoice.setBounds(choiceX, top + 15, inner - POPUP_LABEL_W, FIELD_H);
        printerLabel.setBounds(left, top + 34, POPUP_LABEL_W - 4, 7);
        autoPrinterChoice.setBounds(choiceX, top + 31, inner - POPUP_LABEL_W, FIELD_H);
        keepLabel.setBounds(left, top + 50, POPUP_LABEL_W - 4, 7);
        keep.setBounds(choiceX, top + 47, 44, FIELD_H);
        trcLabel.setBounds(choiceX + 48, top + 50, 20, 7);
        final int boxTop = top + 65;
        explained.setBounds(left, boxTop, inner, explained.heightFor(font, inner));
        lastLine.setBounds(left, explained.bottom() + 5, inner, 7);
        noteLine.setBounds(left, p.bottom() - BUTTON_H - 4, inner - 116, 7);
        cancel.setBounds(p.right() - 58, p.bottom() - BUTTON_H - 6, 50, BUTTON_H);
        ok.setBounds(p.right() - 114, p.bottom() - BUTTON_H - 6, 50, BUTTON_H);
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
        g.pose().translate(orderX + 6, orderY + 6, 0f);
        g.pose().scale(BIG_ICON, BIG_ICON, BIG_ICON);
        DesktopItems.item(g, one.stack(), 0, 0);
        g.pose().popPose();
        final int textX = orderX + 36;
        final int textW = orderX + orderW - 4 - textX;
        Draw.text(g, font, Texts.clip(font, one.name(), textW), textX, orderY + 8, skin.text());
        Texts.small(g, font, GameText.resolve(CryptoWalletTexts.EACH.with(Trc.plain(price.units()))), textX,
                orderY + 20, gold.getAsInt());
        Texts.small(g, font, GameText.resolve(CryptoWalletTexts.COPIES), orderX + 6, orderY + 39, skin.text());
        final int sx = copies.right() + 1;
        skin.field(g, sx, copies.y(), SPIN_W, FIELD_H, false);
        Texts.small(g, font, "+", sx + 2, copies.y() + 1, skin.dim());
        Texts.small(g, font, "-", sx + 3, copies.y() + 6, skin.dim());
        final int count = copiesChosen();
        Texts.small(g, font, GameText.resolve(CryptoWalletTexts.TOTAL), orderX + 6, orderY + 54, skin.text());
        final String total = GameText.resolve(CryptoWalletTexts.AMOUNT_TRC.with(Trc.plain(price.units() * count)));
        Draw.text(g, font, total, orderX + orderW - 6 - font.width(total), orderY + 52, gold.getAsInt());
        final int printerIndex = indexOfPrinter(printer);
        int noteY = orderY + 65;
        if (printerIndex >= 0) {
            final int seconds = Printer3DModel.byName(printers().get(printerIndex).model()).itemSeconds();
            Texts.small(g, font, Texts.clip(font, GameText.resolve(CryptoWalletTexts.TAKES.with(
                    duration(seconds * count), seconds)), (int) ((orderW - 12) / SMALL)), orderX + 6, noteY,
                    skin.dim());
            noteY += 9;
        }
        CryptoWalletApp.paragraph(g, font, GameText.resolve(CryptoWalletTexts.PAID_AS_STARTS), orderX + 6, noteY,
                orderW - 12, skin.dim());
    }

    private void renderTile(final GuiGraphics g, final UiContext ctx, final int index, final int x, final int y,
                            final int w, final int h, final boolean hovered) {
        if (index >= prices().size()) {
            return;
        }
        final WalletStatePayload.Price price = prices().get(index);
        Draw.outline(g, x, y, w, h, price.item().equals(item) ? ctx.skin().accent() : ctx.skin().edge());
        final Shown one = shownOf(price.item());
        DesktopItems.item(g, one.stack(), x + 3, y + (h - 16) / 2);
        final List<FormattedCharSequence> lines = ctx.font().split(Component.literal(one.name()),
                (int) ((w - 24) / SMALL));
        final int nameLines = Math.max(1, Math.min(2, lines.size()));
        final int textY = nameLines == 1 ? y + 4 : y + 1;
        g.pose().pushPose();
        g.pose().translate(x + 22, textY, 0f);
        g.pose().scale(SMALL, SMALL, 1f);
        for (int i = 0; i < nameLines && i < lines.size(); i++) {
            Draw.text(g, ctx.font(), lines.get(i), 0, i * 9, ctx.skin().text());
        }
        g.pose().popPose();
        Texts.small(g, ctx.font(), GameText.resolve(CryptoWalletTexts.AMOUNT_TRC.with(Trc.plain(price.units()))),
                x + 22, textY + nameLines * 7, gold.getAsInt());
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
            final int wrap = (int) ((width - 8) / SMALL);
            final int lines = font.split(Component.literal(example()), wrap).size()
                    + font.split(Component.literal(GameText.resolve(CryptoWalletTexts.AUTO_RUNS)), wrap).size();
            return Math.round(lines * 10 * SMALL) + 6;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            g.fill(x(), y(), right(), bottom(), ctx.skin().panelBg());
            Draw.outline(g, x(), y(), width(), height(), ctx.skin().edge());
            final int used = CryptoWalletApp.paragraph(g, ctx.font(), example(), x() + 4, y() + 3, width() - 8,
                    ctx.skin().text());
            CryptoWalletApp.paragraph(g, ctx.font(), GameText.resolve(CryptoWalletTexts.AUTO_RUNS), x() + 4,
                    y() + 3 + used, width() - 8, ctx.skin().dim());
        }
    }
}
