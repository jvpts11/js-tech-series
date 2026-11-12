/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.teracoin.WalletRequestPayload;
import dev.jstech.computers.operation.payload.teracoin.WalletStatePayload;
import dev.jstech.computers.teracoin.BlockSquare;
import dev.jstech.computers.teracoin.LedgerEntry;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.RadioGroup;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.client.os.TabbedWindowLook.FIELD_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.HEADER_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.LINE_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.PAD;
import static dev.jstech.computers.client.os.TabbedWindowLook.ROW_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.STATUS_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.TAB_H;

/**
 * The Crypto Wallet, Blockwright's program for Teracoin (computers/docs/design/teracoin.md), on any desktop, in its
 * look, with Blockwright's gold. Its tabs: the wallet at a glance, sending, the address to be paid at (also drawn as a
 * square of blocks to show across a room), the history, printing on the computer's 3D printers (its own class,
 * {@link CryptoWalletPrint}), and the computer's wallets. At its foot, the chain's line. A
 * wallet is a file the computer keeps; the window asks the computer for everything and never sees the key.
 */
@PaletteHolder
public final class CryptoWalletApp implements IDesktopApp {

    private final BlockPos host;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private WalletStatePayload state;
    private String wallet = "";
    private int tab;
    private int filter;
    private int note;
    private long noteUntil;
    private long copiedUntil;
    private long lastRequestMillis;
    private boolean offeredNew;
    private boolean copyChosen = true;
    private int lastMouseX;
    private int lastMouseY;

    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final ColumnHeader latestColumns;
    private final ListView<WalletStatePayload.Move> latestList;
    private final TextField payTo;
    private final TextField sendLabel;
    private final TextField sendAmount;
    private final Button send;
    private final Button clear;
    private final ColumnHeader waitingColumns;
    private final ListView<WalletStatePayload.Move> waitingList;
    private final Button copy;
    private final ColumnHeader walletColumns;
    private final ListView<WalletStatePayload.Wallet> walletList;
    private final List<RadioGroup> filters = new ArrayList<>();
    private final ColumnHeader moveColumns;
    private final ListView<WalletStatePayload.Move> moveList;
    private final Button newWallet;
    private final Button open;
    private final Button firstWallet;
    private final Popup newPopup;
    private final TextField nameField;
    private final TextField folderField;
    private final Checkbox copyBox;
    private final Button create;
    private final Button cancel;
    private final CryptoWalletPrint printTab;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/crypto_wallet",
            new Colours(0xFFB07A14, 0xFFFFF3D4, 0xFFE8C77A, 0xFF5A3D08, 0xFF1C2028, 0xFFFFFFFF));
    private static final StableIds<LedgerEntry.Kind> KINDS = StableIds.of(LedgerEntry.Kind.class);
    private static final ResourceLocation COIN = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "textures/gui/teracoin/coin.png");
    private static final ResourceLocation MARK = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "textures/gui/teracoin/blockwright.png");
    private static final TextKey[] TABS = {CryptoWalletTexts.OVERVIEW, CryptoWalletTexts.SEND,
            CryptoWalletTexts.RECEIVE, CryptoWalletTexts.TRANSACTIONS, CryptoWalletTexts.PRINT,
            CryptoWalletTexts.WALLETS};
    private static final TextKey[] FILTERS = {CryptoWalletTexts.ALL, CryptoWalletTexts.RECEIVED,
            CryptoWalletTexts.SENT, CryptoWalletTexts.PAYOUTS, CryptoWalletTexts.PRINTED_FILTER};
    private static final int OVERVIEW = 0;
    private static final int SEND = 1;
    private static final int RECEIVE = 2;
    private static final int TRANSACTIONS = 3;
    private static final int PRINT = 4;
    private static final int WALLETS = 5;
    /* The band with the balance over the Overview, Send and Print tabs, and its coin. */
    private static final int BAND_H = 43;
    private static final int COIN_SIZE = 32;
    private static final int BAND_TEXT_X = 47;
    /* Where the Send tab's fields start, after their labels. */
    private static final int SEND_LABEL_W = 65;
    private static final int BUTTON_W = 64;
    private static final int LATEST = 5;
    private static final int MODULE = 5;
    private static final int QUIET = 4;
    private static final int SQUARE = BlockSquare.SIDE * MODULE + 2 * QUIET;
    private static final int ADDRESS_H = 19;
    private static final int RECEIVE_PITCH = 12;
    private static final int POPUP_W = 360;
    private static final int POPUP_H = 160;
    private static final int POPUP_LABEL_W = 52;
    private static final int NOTE_MILLIS = 6000;
    private static final int COPIED_MILLIS = 2000;
    private static final long REFRESH_MILLIS = 2000L;
    private static final float BIG = 1.75f;
    private static final int PLACES = 8;

    private static CryptoWalletApp active;

    /** The wallet of the computer at {@code host}; its monitor shows it as any window. */
    public CryptoWalletApp(final BlockPos host) {
        this.host = host;
        final List<String> tabLabels = new ArrayList<>();
        for (final TextKey key : TABS) {
            tabLabels.add(GameText.resolve(key));
        }
        tabs = root.add(new TabStrip(tabLabels).fitToLabels(16).setOnSelect(index -> tab = index));
        latestColumns = root.add(TabbedWindowLook.header(words(CryptoWalletTexts.WHEN, CryptoWalletTexts.WHAT,
                CryptoWalletTexts.AMOUNT, CryptoWalletTexts.STATUS)));
        latestList = root.add(new ListView<>(this::latest, ROW_H, this::renderLatest));
        payTo = root.add(new TextField(TeracoinAddress.LENGTH));
        sendLabel = root.add(new TextField(WalletRequestPayload.MOST_TEXT / 2));
        sendAmount = root.add(new TextField(24));
        send = root.add(new Button(GameText.resolve(CryptoWalletTexts.SEND), this::send).setPrimary(true));
        clear = root.add(new Button(GameText.resolve(CryptoWalletTexts.CLEAR), this::clearSend));
        waitingColumns = root.add(TabbedWindowLook.header(words(CryptoWalletTexts.SENT_COLUMN,
                CryptoWalletTexts.TO_COLUMN, CryptoWalletTexts.AMOUNT, CryptoWalletTexts.CONFIRMS)));
        waitingList = root.add(new ListView<>(this::waitingSends, ROW_H, this::renderWaiting));
        copy = root.add(new Button(() -> GameText.resolve(Util.getMillis() < copiedUntil ? CryptoWalletTexts.COPIED
                : CryptoWalletTexts.COPY), this::copyAddress).setPrimary(true));
        walletColumns = root.add(TabbedWindowLook.header(words(CryptoWalletTexts.FILE, CryptoWalletTexts.ADDRESS,
                CryptoWalletTexts.BALANCE)));
        walletList = root.add(new ListView<>(this::wallets, ROW_H, this::renderWallet)
                .setOnClick((index, button, mx, my) -> pick(index)));
        for (int i = 0; i < FILTERS.length; i++) {
            final int option = i;
            filters.add(root.add(new RadioGroup(List.of(() -> GameText.resolve(FILTERS[option])),
                    () -> filter == option ? 0 : -1, chosen -> filter = option)));
        }
        moveColumns = root.add(TabbedWindowLook.header(words(CryptoWalletTexts.WHEN, CryptoWalletTexts.WHAT,
                CryptoWalletTexts.AMOUNT, CryptoWalletTexts.BLOCKS)));
        moveList = root.add(new ListView<>(this::filtered, ROW_H, this::renderMove));
        newWallet = root.add(new Button(GameText.resolve(CryptoWalletTexts.NEW_WALLET), this::openNew));
        open = root.add(new Button(GameText.resolve(CryptoWalletTexts.OPEN), this::openChosen).setPrimary(true));
        firstWallet = root.add(new Button(GameText.resolve(CryptoWalletTexts.NEW_WALLET), this::openNew)
                .setPrimary(true));

        newPopup = new Popup(GameText.resolve(CryptoWalletTexts.NEW_WALLET), POPUP_W, POPUP_H)
                .setLayouter(this::layoutPopup);
        nameField = newPopup.add(new TextField(24));
        folderField = newPopup.add(new TextField(WalletRequestPayload.MOST_PATH));
        copyBox = newPopup.add(new Checkbox(() -> GameText.resolve(CryptoWalletTexts.COPY_TO.with(
                String.valueOf(state == null ? 'E' : state.current().copyDrive()))), () -> copyChosen,
                () -> copyChosen = !copyChosen));
        create = newPopup.add(new Button(GameText.resolve(CryptoWalletTexts.CREATE), this::createWallet)
                .setPrimary(true));
        cancel = newPopup.add(new Button(GameText.resolve(CryptoWalletTexts.CANCEL), newPopup::close));
        printTab = new CryptoWalletPrint(root, host, () -> state, () -> wallet, this::freshNote,
                () -> colours().gold(), () -> lastRequestMillis = Util.getMillis());

        active = this;
        request();
    }

    /** Routes the computer's answer to the open wallet of that computer. */
    public static void accept(final WalletStatePayload payload) {
        if (active == null || !payload.hostPos().equals(active.host)) {
            return;
        }
        active.take(payload);
    }

    @Override
    public void onRestored() {
        active = this;
        request();
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public String title() {
        if (state == null || !state.current().any()) {
            return GameText.resolve(CryptoWalletTexts.TITLE);
        }
        return GameText.resolve(CryptoWalletTexts.TITLE_OF.with(baseName(state.current().file())));
    }

    @Override
    public int defaultWidth() {
        return 493;
    }

    @Override
    public int defaultHeight() {
        return 313;
    }

    @Override
    public int minWidth() {
        return 400;
    }

    @Override
    public int minHeight() {
        return 280;
    }

    @Override
    public boolean waiting() {
        return state == null;
    }

    @Override
    public String saveState() {
        return wallet;
    }

    @Override
    public void restoreState(final String saved) {
        wallet = saved;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (Util.getMillis() - lastRequestMillis >= REFRESH_MILLIS) {
            request();
        }
        g.fill(x, y, x + width, y + height, skin.windowBg());
        final boolean any = state != null && state.current().any();
        layout(font, x, y, width, height, any);
        if (!any) {
            renderEmpty(g, font, x, y, width);
        } else {
            TabbedWindowLook.tabRow(g, skin, x, y, width);
            renderTab(g, font, x, y, width, height);
        }
        root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        if (state != null) {
            TeracoinLook.statusBar(g, font, skin, x, y + height - STATUS_H, width, state.chain(), freshNote());
        } else {
            skin.statusBar(g, x, y + height - STATUS_H, width, STATUS_H);
        }
    }

    @Override
    public boolean modalActive() {
        return openPopup() != null;
    }

    @Override
    public void renderModal(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height, final int mouseX, final int mouseY) {
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, 0f);
        if (newPopup.isOpen()) {
            newPopup.renderIn(g, ctx, x, y, width, height);
            renderPopupWords(g, font);
        } else if (printTab.autoPopup().isOpen()) {
            printTab.autoPopup().renderIn(g, ctx, x, y, width, height);
            printTab.renderAutoOver(g);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup popup = openPopup();
        if (popup != null) {
            popup.mouseClicked(mouseX, mouseY, button);
        } else if (button == 0 && (tab != PRINT || root.overlayOpen() || !printTab.mouseClicked(mouseX, mouseY))) {
            root.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup popup = openPopup();
        if (popup != null) {
            popup.mouseDragged(mouseX, mouseY, button);
        } else {
            root.mouseDragged(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup popup = openPopup();
        if (popup != null) {
            popup.mouseReleased(mouseX, mouseY, button);
        } else {
            root.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final Popup popup = openPopup();
        if (popup != null) {
            popup.mouseScrolled(lastMouseX, lastMouseY, delta);
            return true;
        }
        return root.mouseScrolled(lastMouseX, lastMouseY, delta);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final Popup popup = openPopup();
        return popup != null ? popup.keyPressed(key, scanCode, modifiers) : root.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean takesTyping() {
        final Popup popup = openPopup();
        return IDesktopApp.typingIn(popup != null ? popup : root);
    }

    @Override
    public boolean charTyped(final char c) {
        final Popup popup = openPopup();
        return popup != null ? popup.charTyped(c) : root.charTyped(c);
    }

    // what the client tests read and click

    /** Whether the computer has answered. */
    public boolean hasState() {
        return state != null;
    }

    /** The address of the wallet shown, or empty. */
    public String address() {
        return state == null ? "" : state.current().address();
    }

    /** The balance of the wallet shown, in hundred-millionths of a TRC. */
    public long balance() {
        return state == null ? 0L : state.current().balance();
    }

    /** The tab shown, from 0 for Overview. */
    public int tab() {
        return tab;
    }

    /** Shows the tab {@code index}. */
    public void showTab(final int index) {
        tab = index;
        tabs.setSelected(index);
    }

    /** Fills the send form as a player would. */
    public void fillSend(final String to, final String label, final String amount) {
        payTo.set(to);
        sendLabel.set(label);
        sendAmount.set(amount);
    }

    /** Presses Send. */
    public void pressSend() {
        send();
    }

    /** Makes a wallet named {@code name} in the computer's wallets folder, as the dialog does. */
    public void makeWallet(final String name) {
        openNew();
        nameField.set(name);
        createWallet();
    }

    /** Prints {@code copies} of the item {@code itemId} on the printer chosen, as the Print tab does. */
    public void printItem(final String itemId, final int copies) {
        printTab.printNow(itemId, copies);
    }

    /** Sets the wallet's Auto print to {@code itemId}, keeping {@code kept} TRC, as its dialog does. */
    public void autoPrint(final String itemId, final String kept) {
        printTab.autoPrint(itemId, kept);
    }

    /** Turns the wallet's Auto print off, as its dialog does. */
    public void autoPrintOff() {
        printTab.autoPrintOff();
    }

    /** The open dialog, which takes every click and key, or none. */
    @Nullable
    private Popup openPopup() {
        if (newPopup.isOpen()) {
            return newPopup;
        }
        return printTab.autoPopup().isOpen() ? printTab.autoPopup() : null;
    }

    /* The note of the last request while it is fresh, or empty. */
    private String freshNote() {
        return Util.getMillis() < noteUntil && note != WalletStatePayload.NOTE_NONE ? GameText.resolve(noteText(note))
                : "";
    }

    private void take(final WalletStatePayload payload) {
        state = payload;
        wallet = payload.current().path();
        printTab.take(payload);
        if (payload.current().note() != WalletStatePayload.NOTE_NONE) {
            note = payload.current().note();
            noteUntil = Util.getMillis() + NOTE_MILLIS;
            if (note == WalletStatePayload.NOTE_SENT) {
                clearSend();
            }
            if (note == WalletStatePayload.NOTE_CREATED) {
                newPopup.close();
            }
        }
        if (!payload.current().any() && payload.wallets().isEmpty() && !offeredNew) {
            offeredNew = true;
            openNew();
        }
    }

    private void request() {
        lastRequestMillis = Util.getMillis();
        PacketDistributor.sendToServer(WalletRequestPayload.look(host, wallet));
    }

    private void send() {
        final long amount = Trc.parse(sendAmount.edit());
        PacketDistributor.sendToServer(new WalletRequestPayload(host, WalletRequestPayload.SEND, wallet,
                payTo.edit().trim(), sendLabel.edit().trim(), Math.max(0L, amount), 0L, 0));
        lastRequestMillis = Util.getMillis();
    }

    private void clearSend() {
        payTo.set("");
        sendLabel.set("");
        sendAmount.set("");
    }

    private void copyAddress() {
        if (state != null && state.current().any()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(state.current().address());
            copiedUntil = Util.getMillis() + COPIED_MILLIS;
        }
    }

    private void pick(final int index) {
        final List<WalletStatePayload.Wallet> all = wallets();
        if (index >= 0 && index < all.size()) {
            walletList.setSelected(index);
            if (tab == RECEIVE) {
                wallet = all.get(index).path();
                request();
            }
        }
    }

    private void openChosen() {
        final List<WalletStatePayload.Wallet> all = wallets();
        final int chosen = walletList.selected();
        if (chosen >= 0 && chosen < all.size()) {
            wallet = all.get(chosen).path();
            showTab(OVERVIEW);
            request();
        }
    }

    private void openNew() {
        nameField.set(GameText.resolve(CryptoWalletTexts.DEFAULT_NAME));
        folderField.set(state == null ? "" : state.current().newFolder());
        copyChosen = true;
        newPopup.open();
    }

    private void createWallet() {
        final boolean copyIt = copyChosen && state != null && state.current().copyDrive() != 0;
        PacketDistributor.sendToServer(new WalletRequestPayload(host, WalletRequestPayload.CREATE, "",
                nameField.edit().trim(), folderField.edit().trim(), copyIt ? 1L : 0L, 0L, 0));
        lastRequestMillis = Util.getMillis();
    }

    private List<WalletStatePayload.Wallet> wallets() {
        return state == null ? List.of() : state.wallets();
    }

    private List<WalletStatePayload.Move> moves() {
        return state == null ? List.of() : state.moves();
    }

    private List<WalletStatePayload.Move> latest() {
        final List<WalletStatePayload.Move> all = moves();
        return all.subList(0, Math.min(LATEST, all.size()));
    }

    private List<WalletStatePayload.Move> waitingSends() {
        final List<WalletStatePayload.Move> out = new ArrayList<>();
        for (final WalletStatePayload.Move move : moves()) {
            if (move.waiting() && move.amount() < 0L) {
                out.add(move);
            }
        }
        return out;
    }

    private List<WalletStatePayload.Move> filtered() {
        final List<WalletStatePayload.Move> out = new ArrayList<>();
        for (final WalletStatePayload.Move move : moves()) {
            final LedgerEntry.Kind kind = KINDS.byId(move.kind(), LedgerEntry.Kind.SEND);
            final boolean shown = switch (filter) {
                case 1 -> move.amount() > 0L && (kind == LedgerEntry.Kind.SEND || kind == LedgerEntry.Kind.REWARD);
                case 2 -> kind == LedgerEntry.Kind.SEND && move.amount() < 0L;
                case 3 -> kind == LedgerEntry.Kind.PAYOUT || kind == LedgerEntry.Kind.FEE;
                case 4 -> kind == LedgerEntry.Kind.PRINT;
                default -> true;
            };
            if (shown) {
                out.add(move);
            }
        }
        return out;
    }

    private void layout(final Font font, final int x, final int y, final int width, final int height,
                        final boolean any) {
        final int px = x + PAD;
        final int pw = width - 2 * PAD;
        final int bottom = y + height - STATUS_H;
        TabbedWindowLook.placeTabs(tabs, x, y, width);
        tabs.setVisible(any);
        tabs.setSelected(tab);
        firstWallet.setVisible(!any);
        final int buttonW = Math.max(BUTTON_W, font.width(GameText.resolve(CryptoWalletTexts.NEW_WALLET)) + 16);
        firstWallet.setBounds(x + (width - buttonW) / 2, y + 116, buttonW, FIELD_H);
        final boolean overview = any && tab == OVERVIEW;
        final boolean sending = any && tab == SEND;
        final boolean receiving = any && tab == RECEIVE;
        final boolean history = any && tab == TRANSACTIONS;
        final boolean printing = any && tab == PRINT;
        final boolean walletsTab = any && tab == WALLETS;
        final int bodyTop = y + TAB_H + (overview || sending || printing ? BAND_H : 0);
        printTab.layout(x, bodyTop, width, bottom, printing);

        latestColumns.setVisible(overview);
        latestList.setVisible(overview);
        if (overview) {
            final int tableTop = bodyTop + 52;
            latestColumns.setBounds(px, tableTop, pw, HEADER_H);
            latestColumns.setColumnX(TabbedWindowLook.columns(px, pw, 15, 57, 74));
            latestList.setBounds(px, tableTop + HEADER_H, pw,
                    Math.min(LATEST * ROW_H, bottom - 4 - (tableTop + HEADER_H)));
        }

        payTo.setVisible(sending);
        sendLabel.setVisible(sending);
        sendAmount.setVisible(sending);
        send.setVisible(sending);
        clear.setVisible(sending);
        waitingColumns.setVisible(sending);
        waitingList.setVisible(sending);
        if (sending) {
            final int fieldX = x + PAD + SEND_LABEL_W;
            final int fieldW = width - 80 - (fieldX - x);
            payTo.setBounds(fieldX, bodyTop + 13, fieldW, FIELD_H);
            sendLabel.setBounds(fieldX, bodyTop + 34, fieldW, FIELD_H);
            sendAmount.setBounds(fieldX, bodyTop + 55, fieldW, FIELD_H);
            // The buttons come under the note on fees, however many lines it wraps to.
            final int noteLines = TabbedWindowLook.lines(font, GameText.resolve(CryptoWalletTexts.NO_FEE), pw).size();
            final int buttonsY = bodyTop + 97 + Math.max(1, noteLines) * LINE_H + 8;
            clear.setBounds(x + width - 11 - BUTTON_W, buttonsY, BUTTON_W, FIELD_H);
            send.setBounds(clear.x() - 8 - BUTTON_W, buttonsY, BUTTON_W, FIELD_H);
            final int tableTop = buttonsY + 40;
            waitingColumns.setBounds(px, tableTop, pw, HEADER_H);
            waitingColumns.setColumnX(TabbedWindowLook.columns(px, pw, 16, 60, 77));
            waitingList.setBounds(px, tableTop + HEADER_H, pw,
                    Math.max(ROW_H, bottom - 4 - (tableTop + HEADER_H)));
        }

        copy.setVisible(receiving);
        final boolean listsWallets = receiving || walletsTab;
        walletColumns.setVisible(listsWallets);
        walletList.setVisible(listsWallets);
        if (receiving) {
            copy.setBounds(x + width - 133, bodyTop + 30, 53, FIELD_H);
            final int tableTop = bodyTop + 55 + SQUARE + 22;
            walletColumns.setBounds(px, tableTop, pw, HEADER_H);
            walletList.setBounds(px, tableTop + HEADER_H, pw, Math.max(ROW_H, bottom - 4 - (tableTop + HEADER_H)));
        } else if (walletsTab) {
            walletColumns.setBounds(px, bodyTop + 26, pw, HEADER_H);
            walletList.setBounds(px, bodyTop + 26 + HEADER_H, pw,
                    Math.max(ROW_H, bottom - FIELD_H - 10 - (bodyTop + 26 + HEADER_H)));
        }
        walletColumns.setColumnX(TabbedWindowLook.columns(px, pw, 21, 65));
        newWallet.setVisible(walletsTab);
        open.setVisible(walletsTab);
        if (walletsTab) {
            final int newW = font.width(GameText.resolve(CryptoWalletTexts.NEW_WALLET)) + 16;
            open.setBounds(x + width - 11 - BUTTON_W, bottom - FIELD_H - 5, BUTTON_W, FIELD_H);
            newWallet.setBounds(open.x() - 8 - newW, bottom - FIELD_H - 5, newW, FIELD_H);
        }

        int filterX = px + font.width(GameText.resolve(CryptoWalletTexts.SHOW)) + 12;
        for (int i = 0; i < filters.size(); i++) {
            final RadioGroup option = filters.get(i);
            option.setVisible(history);
            final int w = RadioGroup.BUTTON + RadioGroup.LABEL_GAP + font.width(GameText.resolve(FILTERS[i])) + 12;
            option.setBounds(filterX, bodyTop + 10, w, RadioGroup.ROW);
            filterX += w;
        }
        moveColumns.setVisible(history);
        moveList.setVisible(history);
        if (history) {
            moveColumns.setBounds(px, bodyTop + 29, pw, HEADER_H);
            moveColumns.setColumnX(TabbedWindowLook.columns(px, pw, 15, 57, 75));
            moveList.setBounds(px, bodyTop + 29 + HEADER_H, pw,
                    Math.max(ROW_H, bottom - 18 - (bodyTop + 29 + HEADER_H)));
        }
    }

    private void layoutPopup(final Popup p) {
        final int left = p.x() + 8;
        final int top = p.contentTop() + 7;
        final int fieldX = left + POPUP_LABEL_W;
        final int fieldW = p.width() - 17 - POPUP_LABEL_W;
        nameField.setBounds(fieldX, top, fieldW, FIELD_H);
        folderField.setBounds(fieldX, top + 20, fieldW, FIELD_H);
        final boolean usb = state != null && state.current().copyDrive() != 0;
        copyBox.setVisible(usb);
        copyBox.setBounds(left + 1, top + 42, p.width() - 18, 11);
        cancel.setBounds(p.right() - 68, p.bottom() - FIELD_H - 8, 60, FIELD_H);
        create.setBounds(p.right() - 133, p.bottom() - FIELD_H - 8, 60, FIELD_H);
    }

    private void renderEmpty(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.blit(MARK, x + width / 2 - 16, y + 34, 0, 0, 32, 32, 32, 32);
        final String heading = GameText.resolve(CryptoWalletTexts.NO_WALLET);
        final int headingW = Math.round(font.width(heading) * TabbedWindowLook.HEADING);
        TabbedWindowLook.heading(g, font, heading, x + (width - headingW) / 2, y + 77, skin.text());
        final String line = GameText.resolve(CryptoWalletTexts.WALLET_IS);
        Draw.text(g, font, line, x + (width - font.width(line)) / 2, y + 95, skin.dim());
    }

    private void renderTab(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                           final int height) {
        final WalletStatePayload.Current current = state.current();
        final int px = x + PAD;
        final int pw = width - 2 * PAD;
        int top = y + TAB_H;
        if (tab == OVERVIEW || tab == SEND || tab == PRINT) {
            renderBand(g, font, x, top, width, current);
            top += BAND_H;
        }
        switch (tab) {
            case OVERVIEW -> {
                final String walletLine = GameText.resolve(CryptoWalletTexts.WALLET) + "  " + GameText.resolve(
                        CryptoWalletTexts.WALLET_IN.with(current.file(), current.folder()));
                Draw.text(g, font, Texts.clip(font, walletLine, pw), px, top + 8, skin.dim());
                Draw.text(g, font, Texts.clip(font, GameText.resolve(CryptoWalletTexts.ADDRESS) + "  "
                        + current.address(), pw), px, top + 20, skin.dim());
                TabbedWindowLook.heading(g, font, GameText.resolve(CryptoWalletTexts.LATEST), px, top + 36,
                        skin.text());
                if (moves().isEmpty()) {
                    Draw.text(g, font, GameText.resolve(CryptoWalletTexts.NO_MOVES), latestList.x() + 5,
                            latestList.y() + 3, skin.dim());
                }
            }
            case SEND -> {
                final int fieldX = payTo.x();
                Draw.text(g, font, GameText.resolve(CryptoWalletTexts.PAY_TO), px + 1, payTo.y() + 4, skin.text());
                Draw.text(g, font, GameText.resolve(CryptoWalletTexts.LABEL), px + 1, sendLabel.y() + 4, skin.text());
                Draw.text(g, font, GameText.resolve(CryptoWalletTexts.AMOUNT), px + 1, sendAmount.y() + 4,
                        skin.text());
                Draw.text(g, font, GameText.resolve(CryptoWalletTexts.TRC), sendAmount.right() + 5,
                        sendAmount.y() + 4, skin.text());
                Draw.text(g, font, GameText.resolve(CryptoWalletTexts.AVAILABLE.with(Trc.format(current.balance()))),
                        fieldX, top + 79, skin.dim());
                TabbedWindowLook.paragraph(g, font, GameText.resolve(CryptoWalletTexts.NO_FEE), px + 1, top + 97, pw,
                        LINE_H, skin.dim());
                TabbedWindowLook.heading(g, font, GameText.resolve(CryptoWalletTexts.WAITING_HEADING), px + 1,
                        waitingColumns.y() - 14, skin.text());
            }
            case RECEIVE -> renderReceive(g, font, x, px, top, width, current);
            case TRANSACTIONS -> {
                Draw.text(g, font, GameText.resolve(CryptoWalletTexts.SHOW), px + 1, top + 11, skin.text());
                Draw.text(g, font, GameText.resolve(CryptoWalletTexts.LINES_NOTE), px + 1,
                        y + height - STATUS_H - 13, skin.dim());
            }
            case PRINT -> printTab.render(g, font, skin, x, top, width);
            case WALLETS -> TabbedWindowLook.heading(g, font, GameText.resolve(CryptoWalletTexts.ON_THIS_COMPUTER), px,
                    top + 9, skin.text());
            default -> {
            }
        }
    }

    private void renderBand(final GuiGraphics g, final Font font, final int x, final int top, final int width,
                            final WalletStatePayload.Current current) {
        g.fill(x, top, x + width, top + BAND_H, skin.panelBg());
        g.fill(x, top + BAND_H - 1, x + width, top + BAND_H, skin.edge());
        g.blit(COIN, x + PAD, top + 5, COIN_SIZE, COIN_SIZE, 0, 0, 16, 16, 16, 16);
        Draw.text(g, font, GameText.resolve(CryptoWalletTexts.BALANCE), x + BAND_TEXT_X, top + 5, skin.dim());
        Texts.scaled(g, font, GameText.resolve(CryptoWalletTexts.AMOUNT_TRC.with(Trc.format(current.balance()))),
                x + BAND_TEXT_X, top + 17, BIG, colours().gold());
        if (current.pending() > 0L) {
            final String pending = GameText.resolve(CryptoWalletTexts.PENDING) + "  "
                    + GameText.resolve(CryptoWalletTexts.AMOUNT_TRC.with(Trc.signed(current.pending(), PLACES)));
            final String confirms = GameText.resolve(CryptoWalletTexts.CONFIRMS_NEXT);
            Draw.text(g, font, pending, x + width - PAD - font.width(pending), top + 7, skin.text());
            Draw.text(g, font, confirms, x + width - PAD - font.width(confirms), top + 19, skin.dim());
        }
    }

    private void renderReceive(final GuiGraphics g, final Font font, final int x, final int px, final int top,
                               final int width, final WalletStatePayload.Current current) {
        TabbedWindowLook.heading(g, font, GameText.resolve(CryptoWalletTexts.YOUR_ADDRESS), px + 3, top + 9,
                skin.text());
        final int fieldW = x + width - 140 - (px + 3);
        skin.field(g, px + 3, top + 28, fieldW, ADDRESS_H, false);
        Draw.text(g, font, Texts.clip(font, current.address(), fieldW - 10), px + 8, top + 34, skin.text());
        final int squareX = px + 3;
        final int squareY = top + 55;
        g.fill(squareX, squareY, squareX + SQUARE, squareY + SQUARE, colours().paper());
        Draw.outline(g, squareX, squareY, SQUARE, SQUARE, skin.edge());
        final boolean[][] square = BlockSquare.of(current.address());
        for (int row = 0; row < BlockSquare.SIDE; row++) {
            for (int column = 0; column < BlockSquare.SIDE; column++) {
                if (square[row][column]) {
                    final int bx = squareX + QUIET + column * MODULE;
                    final int by = squareY + QUIET + row * MODULE;
                    g.fill(bx, by, bx + MODULE, by + MODULE, colours().ink());
                }
            }
        }
        final int textX = squareX + SQUARE + 12;
        final int textW = x + width - PAD - textX;
        final int used = TabbedWindowLook.paragraph(g, font, GameText.resolve(CryptoWalletTexts.BELONGS), textX,
                squareY + 7, textW, RECEIVE_PITCH, skin.dim());
        TabbedWindowLook.paragraph(g, font, GameText.resolve(CryptoWalletTexts.TO_BE_PAID), textX,
                squareY + 7 + used + RECEIVE_PITCH, textW, RECEIVE_PITCH, skin.text());
        TabbedWindowLook.heading(g, font, GameText.resolve(CryptoWalletTexts.ON_THIS_COMPUTER), squareX,
                walletColumns.y() - 16, skin.text());
    }

    private void renderPopupWords(final GuiGraphics g, final Font font) {
        final int left = newPopup.x() + 8;
        Draw.text(g, font, GameText.resolve(CryptoWalletTexts.NAME), left + 1, nameField.y() + 4, skin.text());
        Draw.text(g, font, GameText.resolve(CryptoWalletTexts.SAVE_IN), left + 1, folderField.y() + 4,
                skin.text());
        final int boxTop = nameField.y() + 60;
        final int boxW = newPopup.width() - 16;
        final String warning = GameText.resolve(CryptoWalletTexts.KEY_WARNING);
        final int boxH = Math.max(1, TabbedWindowLook.lines(font, warning, boxW - 10).size()) * LINE_H + 7;
        g.fill(left, boxTop, left + boxW, boxTop + boxH, colours().warnFill());
        Draw.outline(g, left, boxTop, boxW, boxH, colours().warnEdge());
        TabbedWindowLook.paragraph(g, font, warning, left + 5, boxTop + 4, boxW - 10, LINE_H, colours().warnInk());
        if (Util.getMillis() < noteUntil && note != WalletStatePayload.NOTE_NONE
                && note != WalletStatePayload.NOTE_CREATED) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(noteText(note)), create.x() - 8 - left), left,
                    create.y() + 4, colours().gold());
        }
    }

    private void renderLatest(final GuiGraphics g, final UiContext ctx, final WalletStatePayload.Move move,
                              final int index, final int x, final int y, final int w, final int h,
                              final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, w, h, hovered, selected);
        TabbedWindowLook.cells(g, ctx, x, y, w, TabbedWindowLook.columns(x, w, 15, 57, 74), when(move.dayTime()),
                what(move), Trc.signed(move.amount(), PLACES), GameText.resolve(move.waiting()
                        ? CryptoWalletTexts.PENDING_STATUS : CryptoWalletTexts.CONFIRMED));
    }

    private void renderWaiting(final GuiGraphics g, final UiContext ctx, final WalletStatePayload.Move move,
                               final int index, final int x, final int y, final int w, final int h,
                               final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, w, h, hovered, selected);
        final String to = move.label().isEmpty() ? TeracoinAddress.shortForm(move.other())
                : GameText.resolve(CryptoWalletTexts.TO_LABEL.with(TeracoinAddress.shortForm(move.other()),
                        move.label()));
        TabbedWindowLook.cells(g, ctx, x, y, w, TabbedWindowLook.columns(x, w, 16, 60, 77), when(move.dayTime()), to,
                Trc.signed(move.amount(), PLACES), GameText.resolve(CryptoWalletTexts.NEXT_BLOCK));
    }

    private void renderMove(final GuiGraphics g, final UiContext ctx, final WalletStatePayload.Move move,
                            final int index, final int x, final int y, final int w, final int h,
                            final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, w, h, hovered, selected);
        TabbedWindowLook.cells(g, ctx, x, y, w, TabbedWindowLook.columns(x, w, 15, 57, 75), when(move.dayTime()),
                what(move), Trc.signed(move.amount(), PLACES), move.waiting()
                        ? GameText.resolve(CryptoWalletTexts.WAITING) : TabbedWindowLook.grouped(move.blocks()));
    }

    private void renderWallet(final GuiGraphics g, final UiContext ctx, final WalletStatePayload.Wallet one,
                              final int index, final int x, final int y, final int w, final int h,
                              final boolean hovered, final boolean selected) {
        final boolean shown = state != null && one.path().equals(state.current().path());
        ctx.skin().listRow(g, x, y, w, h, hovered, selected || shown);
        TabbedWindowLook.cells(g, ctx, x, y, w, TabbedWindowLook.columns(x, w, 21, 65), one.file(),
                TeracoinAddress.shortForm(one.address()), Trc.format(one.balance()));
    }

    private String what(final WalletStatePayload.Move move) {
        final LedgerEntry.Kind kind = KINDS.byId(move.kind(), LedgerEntry.Kind.SEND);
        return GameText.resolve(switch (kind) {
            case SEND -> move.amount() >= 0L ? CryptoWalletTexts.RECEIVED_FROM.with(
                    TeracoinAddress.shortForm(move.other()))
                    : move.label().isEmpty() ? CryptoWalletTexts.SENT_TO.with(TeracoinAddress.shortForm(move.other()))
                    : CryptoWalletTexts.SENT_TO_LABEL.with(TeracoinAddress.shortForm(move.other()), move.label());
            case PAYOUT -> CryptoWalletTexts.POOL_PAYOUT.with(move.other(), TabbedWindowLook.grouped(move.block()));
            case FEE -> CryptoWalletTexts.POOL_FEE.with(move.other(), TabbedWindowLook.grouped(move.block()));
            case REWARD -> CryptoWalletTexts.BLOCK_REWARD.with(TabbedWindowLook.grouped(move.block()));
            case PRINT -> CryptoWalletTexts.PRINTED.with(itemName(move.label()));
        });
    }

    private static String itemName(final String id) {
        final ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return id;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(key)).getHoverName().getString();
    }

    private static String when(final long dayTime) {
        return GameText.resolve(CryptoWalletTexts.WHEN_AT.with(GameCalendar.day(dayTime) + 1,
                String.format(Locale.ROOT, "%02d:%02d", GameCalendar.hourOf(dayTime),
                        GameCalendar.minuteOf(dayTime))));
    }

    private static TextKey noteText(final int code) {
        return switch (code) {
            case WalletStatePayload.NOTE_SENT -> CryptoWalletTexts.NOTE_SENT;
            case WalletStatePayload.NOTE_NOT_YOURS -> CryptoWalletTexts.NOTE_NOT_YOURS;
            case WalletStatePayload.NOTE_BAD_ADDRESS -> CryptoWalletTexts.NOTE_BAD_ADDRESS;
            case WalletStatePayload.NOTE_NOTHING -> CryptoWalletTexts.NOTE_NOTHING;
            case WalletStatePayload.NOTE_TOO_MUCH -> CryptoWalletTexts.NOTE_TOO_MUCH;
            case WalletStatePayload.NOTE_CREATED -> CryptoWalletTexts.NOTE_CREATED;
            case WalletStatePayload.NOTE_NAME_TAKEN -> CryptoWalletTexts.NOTE_NAME_TAKEN;
            case WalletStatePayload.NOTE_NOT_WRITTEN -> CryptoWalletTexts.NOTE_NOT_WRITTEN;
            case WalletStatePayload.NOTE_NO_NAME -> CryptoWalletTexts.NOTE_NO_NAME;
            case WalletStatePayload.NOTE_PRINTING -> CryptoWalletTexts.NOTE_PRINTING;
            case WalletStatePayload.NOTE_NOT_PRINTABLE -> CryptoWalletTexts.NOTE_NOT_PRINTABLE;
            case WalletStatePayload.NOTE_NO_PRINTER -> CryptoWalletTexts.NOTE_NO_PRINTER;
            case WalletStatePayload.NOTE_AUTO_SET -> CryptoWalletTexts.NOTE_AUTO_SET;
            case WalletStatePayload.NOTE_AUTO_OFF -> CryptoWalletTexts.NOTE_AUTO_OFF;
            default -> CryptoWalletTexts.NOTE_QUEUE_FULL;
        };
    }

    private static String baseName(final String file) {
        return file.toLowerCase(Locale.ROOT).endsWith(".wallet") ? file.substring(0, file.length() - 7) : file;
    }

    private static List<String> words(final TextKey... keys) {
        final List<String> out = new ArrayList<>();
        for (final TextKey key : keys) {
            out.add(GameText.resolve(key));
        }
        return out;
    }

    private static Colours colours() {
        return PALETTE.get();
    }

    /**
     * The wallet's own colours, beside its desktop's: Blockwright's gold for the balance, the warning box of the new
     * wallet's dialog, and the ink and paper of the square of blocks.
     */
    private record Colours(int gold, int warnFill, int warnEdge, int warnInk, int ink, int paper) {
    }
}
