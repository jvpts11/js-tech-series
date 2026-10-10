/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload.WireInterface;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload.WirePlace;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload.WireRomEntry;
import dev.jstech.computers.operation.payload.DownloadToMediaPayload;
import dev.jstech.computers.operation.payload.LoadFromMediaPayload;
import dev.jstech.computers.operation.payload.MoveCraftPayload;
import dev.jstech.computers.operation.payload.RemoveRomCraftPayload;
import dev.jstech.computers.operation.payload.RequestCraftManagerPayload;
import dev.jstech.computers.operation.payload.SetCraftInterfacePayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static dev.jstech.computers.client.os.CraftingManagerTexts.BENCH_RECIPES;
import static dev.jstech.computers.client.os.CraftingManagerTexts.CARDS;
import static dev.jstech.computers.client.os.CraftingManagerTexts.CARD_LINE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.CARD_REQUIRED;
import static dev.jstech.computers.client.os.CraftingManagerTexts.CARD_REQUIRED_TO_MANAGE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.DOWNLOAD;
import static dev.jstech.computers.client.os.CraftingManagerTexts.EXCLUSIVE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.FIRST_WITH_ROOM;
import static dev.jstech.computers.client.os.CraftingManagerTexts.HINT;
import static dev.jstech.computers.client.os.CraftingManagerTexts.IDLE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.INSERT_A_DISC;
import static dev.jstech.computers.client.os.CraftingManagerTexts.INTERFACES_TAB;
import static dev.jstech.computers.client.os.CraftingManagerTexts.INTERFACE_COLUMN;
import static dev.jstech.computers.client.os.CraftingManagerTexts.INTO;
import static dev.jstech.computers.client.os.CraftingManagerTexts.JOBS_AUTO;
import static dev.jstech.computers.client.os.CraftingManagerTexts.JOBS_COLUMN;
import static dev.jstech.computers.client.os.CraftingManagerTexts.KIND_NOTE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.LOAD;
import static dev.jstech.computers.client.os.CraftingManagerTexts.LOAD_ALL;
import static dev.jstech.computers.client.os.CraftingManagerTexts.MACHINE_COLUMN;
import static dev.jstech.computers.client.os.CraftingManagerTexts.MEDIA;
import static dev.jstech.computers.client.os.CraftingManagerTexts.MODE_COLUMN;
import static dev.jstech.computers.client.os.CraftingManagerTexts.MOVE_TO;
import static dev.jstech.computers.client.os.CraftingManagerTexts.NOT_EXCLUSIVE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.NO_CRAFT_FILES;
import static dev.jstech.computers.client.os.CraftingManagerTexts.NO_INTERFACES;
import static dev.jstech.computers.client.os.CraftingManagerTexts.NO_MACHINE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.NO_RECIPES;
import static dev.jstech.computers.client.os.CraftingManagerTexts.NO_REMOVABLE_MEDIA;
import static dev.jstech.computers.client.os.CraftingManagerTexts.PATTERNS;
import static dev.jstech.computers.client.os.CraftingManagerTexts.PATTERNS_COLUMN;
import static dev.jstech.computers.client.os.CraftingManagerTexts.PAUSE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.PAUSED;
import static dev.jstech.computers.client.os.CraftingManagerTexts.PICK_MOVE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.PICK_PLACE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.PLACE_COUNT;
import static dev.jstech.computers.client.os.CraftingManagerTexts.RECIPES_TAB;
import static dev.jstech.computers.client.os.CraftingManagerTexts.REMOVE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.RESUME;
import static dev.jstech.computers.client.os.CraftingManagerTexts.STATE_COLUMN;
import static dev.jstech.computers.client.os.CraftingManagerTexts.SUMMARY;
import static dev.jstech.computers.client.os.CraftingManagerTexts.SUMMARY_ONE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.THIS_COMPUTER;
import static dev.jstech.computers.client.os.CraftingManagerTexts.WAITING;

/**
 * The Crafting Manager desktop app: moves {@code .craft} recipe files between a removable medium in a linked drive and
 * the places a Crafting Computer keeps recipes in, and sets the interfaces it drives.
 *
 * <p>Recipes: the left pane lists the {@code .craft} files on the medium; the right pane lists what the computer
 * keeps, by place: the ROM of each Crafting Card with its bench recipes, then each Crafting Interface it drives with
 * its processing and pipeline recipes. Load puts a selection into the place chosen in "into" (or the first with room
 * of the right kind), Move to takes a recipe to another place, Download writes recipes back onto the medium and
 * Remove takes them out. Interfaces: each interface with its machine, how full it is, its mode, what it is doing and
 * its most jobs, which can be set there, and the cards with how many interfaces each drives and its ROM. Every action
 * needs a Crafting Card; without one the app shows a banner and disables the buttons.
 */
@PaletteHolder
public final class CraftingManagerApp implements IDesktopApp {

    private final BlockPos host;
    private OsSkin skin = OsSkin.fallback();
    private String mediaVolumeKey = "";
    private Text mediaLabel = Text.EMPTY;
    private List<String> mediaFiles = List.of();
    private List<WirePlace> places = List.of();
    private List<WireInterface> interfaces = List.of();
    private int budget;
    private int waiting;
    private boolean hasCard;
    private boolean loaded;
    private String status = "";
    /** Whether the status line reports something the player has to act on, which is drawn as a warning. */
    private boolean statusWarns;
    private int tab;
    /* Where Load puts what it loads: a place's number, or -1 for the first with room. */
    private int into = -1;
    /* Whether the place picker that is open chooses where Load goes or where the selection moves. */
    private boolean pickingMove;
    private final Set<Integer> selectedMedia = new LinkedHashSet<>();
    /* The refs of the selected recipes. */
    private final Set<Integer> selectedRefs = new LinkedHashSet<>();
    private int lastMouseX;
    private int lastMouseY;
    private int lastW;
    /*
     * Frames since the state was last asked for. The window outlives the screen it was opened on (a machine's open
     * windows come back with their program instances when the monitor is entered again), so the state is re-asked for
     * while the window is shown, not only when it is created: a disc put in the drive after the window opened must
     * show up on its own.
     */
    private int refreshFrames;
    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final Label warnLabel;
    private final Label mediaHeader;
    private final Label romHeader;
    private final ListView<String> mediaList;
    private final Label mediaEmpty;
    private final ListView<Row> placeList;
    private final Label placesEmpty;
    private final Label statusLabel;
    private final Button loadButton;
    private final Button intoButton;
    private final Button loadAllButton;
    private final Button downloadButton;
    private final Button removeButton;
    private final Button moveButton;
    private final Popup placePicker;
    private final ListView<Integer> pickerList;
    private final Label noCardLabel;
    private final Label noInterfacesLabel;
    private final Label summaryLabel;
    private final ColumnHeader interfaceColumns;
    private final ListView<WireInterface> interfaceList;
    private final Label cardsHeader;
    private final ListView<WirePlace> cardList;
    private final Label waitingLabel;
    private final Label hintLabel;

    /** The card-required warning's band and ink, and the states' colours, {@code jsc:app/crafting_manager}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/crafting_manager",
            new Colours(0xFFFCE3A1, 0xFF6B4E00, 0xFF2EA043, 0xFFC98A10, 0xFFD1495B));
    private static final int PAD = 5;
    private static final int HEADER_H = 12;
    private static final int ROW_H = 11;
    private static final int BTN_H = 13;
    private static final int BAR_H = BTN_H + PAD * 2;
    private static final int TAB_H = 13;
    private static final int I_ROW_H = 14;
    private static final int MOST_JOBS = 16;
    private static final int REFRESH_EVERY_FRAMES = 40;
    /* Where each column of the Interfaces tab starts, as a share of the window's width. */
    private static final double[] COLUMNS = {0.0, 0.16, 0.32, 0.41, 0.60, 0.81, 0.88};

    private static CraftingManagerApp active;

    public CraftingManagerApp(final BlockPos host) {
        this.host = host;
        tabs = root.add(new TabStrip(List.of(GameText.resolve(RECIPES_TAB), GameText.resolve(INTERFACES_TAB)))
                .setOnSelect(i -> tab = i));
        warnLabel = root.add(new Label(GameText.resolve(CARD_REQUIRED_TO_MANAGE)).setColor(PALETTE.get().warnText()));
        mediaHeader = root.add(new Label(() -> GameText.resolve(mediaVolumeKey.isEmpty() ? NO_REMOVABLE_MEDIA.text()
                : MEDIA.with(mediaLabel)), Label.Tone.DIM));
        romHeader = root.add(new Label(GameText.resolve(THIS_COMPUTER), Label.Tone.DIM));
        mediaList = root.add(new ListView<String>(() -> mediaFiles, ROW_H, this::renderMediaRow)
                .setPadding(1)
                .setOnClick((index, button, mx, my) -> toggle(selectedMedia, index)));
        mediaEmpty = root.add(new Label(
                () -> GameText.resolve(mediaVolumeKey.isEmpty() ? INSERT_A_DISC : NO_CRAFT_FILES), Label.Tone.DIM));
        placeList = root.add(new ListView<Row>(this::rows, ROW_H, this::renderPlaceRow)
                .setPadding(1)
                .setOnClick(this::placeRowClicked));
        placesEmpty = root.add(new Label(GameText.resolve(NO_RECIPES), Label.Tone.DIM));
        statusLabel = root.add(new Label(() -> status.isEmpty() ? GameText.resolve(KIND_NOTE) : status)
                .setColor(() -> statusWarns && !status.isEmpty() ? PALETTE.get().warnText() : skin.dim()));
        loadButton = root.add(new Button(GameText.resolve(LOAD), this::loadSelected));
        intoButton = root.add(new Button(this::intoLabel, () -> openPicker(false)));
        loadAllButton = root.add(new Button(GameText.resolve(LOAD_ALL), this::loadAll));
        downloadButton = root.add(new Button(GameText.resolve(DOWNLOAD), this::downloadSelected));
        removeButton = root.add(new Button(GameText.resolve(REMOVE), this::removeSelected));
        moveButton = root.add(new Button(GameText.resolve(MOVE_TO), () -> openPicker(true)));
        pickerList = new ListView<Integer>(this::pickerChoices, ROW_H, this::renderPickerRow)
                .setOnClick(this::pickerClicked);
        placePicker = new Popup(() -> GameText.resolve(pickingMove ? PICK_MOVE : PICK_PLACE), 180, 120)
                .setLayouter(this::layoutPicker)
                .setCloseOnOutsideClick(true);
        placePicker.add(pickerList);

        noCardLabel = root.add(new Label(GameText.resolve(CARD_REQUIRED), Label.Tone.DIM));
        noInterfacesLabel = root.add(new Label(GameText.resolve(NO_INTERFACES), Label.Tone.DIM));
        summaryLabel = root.add(new Label(this::summaryText, Label.Tone.DIM));
        interfaceColumns = root.add(new ColumnHeader(List.of(GameText.resolve(INTERFACE_COLUMN),
                GameText.resolve(MACHINE_COLUMN), GameText.resolve(PATTERNS_COLUMN), GameText.resolve(MODE_COLUMN),
                GameText.resolve(STATE_COLUMN), GameText.resolve(JOBS_COLUMN))).setSortable(false));
        interfaceList = root.add(new ListView<WireInterface>(() -> interfaces, I_ROW_H, this::renderInterfaceRow)
                .setOnClick(this::interfaceRowClicked));
        cardsHeader = root.add(new Label(GameText.resolve(CARDS), Label.Tone.DIM));
        cardList = root.add(new ListView<WirePlace>(this::cards, ROW_H, this::renderCardRow));
        waitingLabel = root.add(new Label(() -> GameText.resolve(WAITING.with(waiting)), Label.Tone.DIM));
        hintLabel = root.add(new Label(GameText.resolve(HINT), Label.Tone.DIM));

        active = this;
        request();
    }

    /** Delivers a state refresh from the server to the open window. */
    public static void accept(final CraftManagerStatePayload payload) {
        if (active == null) {
            return;
        }
        active.mediaVolumeKey = payload.mediaVolumeKey();
        active.mediaLabel = payload.mediaLabel();
        active.mediaFiles = payload.mediaFiles();
        active.places = payload.places();
        active.interfaces = payload.interfaces();
        active.budget = payload.budget();
        active.waiting = payload.waiting();
        active.hasCard = payload.hasCard();
        final String status = GameText.resolve(payload.status());
        if (!status.isEmpty()) {
            active.status = status;
            active.statusWarns = payload.statusWarns();
        }
        active.loaded = true;
        active.selectedMedia.removeIf(i -> i >= active.mediaFiles.size());
        final Set<Integer> refs = new LinkedHashSet<>();
        payload.entries().forEach(entry -> refs.add(entry.ref()));
        active.selectedRefs.retainAll(refs);
        if (active.into >= active.places.size()) {
            active.into = -1;
        }
    }

    @Override
    public void onRestored() {
        active = this;
        request();
    }

    @Override
    public void onClosed() {
        // A closed window must not stay reachable or keep absorbing late replies.
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        /*
         * Runs each frame for the window being drawn: with two Crafting Computers open in turn, the replies must
         * reach the window on screen, not the instance created last.
         */
        active = this;
        this.skin = osSkin;
    }

    @Override
    public String title() {
        return GameText.resolve(CraftingManagerAppTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return 380;
    }

    @Override
    public int defaultHeight() {
        return 210;
    }

    @Override
    public int minWidth() {
        return 280;
    }

    @Override
    public int minHeight() {
        return 160;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastW = width;
        if (++refreshFrames >= REFRESH_EVERY_FRAMES) {
            refreshFrames = 0;
            request();
        }
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        Draw.outline(g, x, y, width, height, skin.edge());
        layout(x, y, width, height);
        // The bands the components sit on: the card warning, the pane headers, the status line, the action bar.
        if (warnLabel.visible()) {
            g.fill(x + 1, y + TAB_H + 1, x + width - 1, y + TAB_H + 1 + HEADER_H, PALETTE.get().warnBand());
        }
        if (tab == 0) {
            g.fill(mediaHeader.x() - 3, mediaHeader.y() - 2, mediaHeader.right() + 3, mediaHeader.y() - 2 + HEADER_H,
                    skin.panelBg());
            g.fill(romHeader.x() - 3, romHeader.y() - 2, romHeader.right() + 3, romHeader.y() - 2 + HEADER_H,
                    skin.panelBg());
            for (final ListView<?> list : List.of(mediaList, placeList)) {
                g.fill(list.x(), list.y(), list.right(), list.bottom(), skin.fieldBg());
                Draw.outline(g, list.x(), list.y(), list.width(), list.height(), skin.edge());
            }
            final int barY = loadButton.y() - PAD;
            g.fill(x + 1, barY - 10, x + width - 1, barY, skin.panelBg());
            g.fill(x + 1, barY, x + width - 1, y + height - 1, skin.panelBg());
            g.fill(x + 1, barY, x + width - 1, barY + 1, skin.edge());
        } else if (interfaceList.visible()) {
            g.fill(interfaceList.x(), interfaceList.y(), interfaceList.right(), interfaceList.bottom(), skin.fieldBg());
            Draw.outline(g, interfaceList.x(), interfaceList.y(), interfaceList.width(), interfaceList.height(),
                    skin.edge());
            g.fill(cardList.x(), cardList.y(), cardList.right(), cardList.bottom(), skin.fieldBg());
            Draw.outline(g, cardList.x(), cardList.y(), cardList.width(), cardList.height(), skin.edge());
        }
        root.render(g, ctx);
    }

    @Override
    public boolean modalActive() {
        return placePicker.isOpen();
    }

    @Override
    public void renderModal(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height, final int mouseX, final int mouseY) {
        if (placePicker.isOpen()) {
            placePicker.renderIn(g, new UiContext(skin, font, mouseX, mouseY, 0f), x, y, width, height);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (!loaded) {
            return;
        }
        if (placePicker.isOpen()) {
            placePicker.mouseClicked(mouseX, mouseY, button);
            return;
        }
        root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        if (placePicker.isOpen()) {
            return placePicker.mouseScrolled(lastMouseX, lastMouseY, delta);
        }
        if (root.mouseScrolled(lastMouseX, lastMouseY, delta)) {
            return true;
        }
        final int step = delta > 0 ? -1 : 1;
        if (tab == 1) {
            interfaceList.setScroll(interfaceList.scroll() + step);
        } else if (rows().size() > mediaFiles.size()) {
            placeList.setScroll(placeList.scroll() + step);
        } else {
            mediaList.setScroll(mediaList.scroll() + step);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (placePicker.isOpen()) {
            return placePicker.keyPressed(key, scanCode, modifiers);
        }
        return root.keyPressed(key, scanCode, modifiers);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean hasCard() {
        return hasCard;
    }

    public int activeTab() {
        return tab;
    }

    public String status() {
        return status;
    }

    public List<String> mediaFiles() {
        return mediaFiles;
    }

    /** What the computer keeps, by place, as received. */
    public List<WirePlace> places() {
        return places;
    }

    /** The interfaces as the Interfaces tab lists them. */
    public List<WireInterface> interfaces() {
        return interfaces;
    }

    /** Every recipe's display name, place by place, as listed. */
    public List<String> romNames() {
        final List<String> out = new ArrayList<>();
        for (final WirePlace place : places) {
            place.entries().forEach(entry -> out.add(GameText.resolve(entry.name())));
        }
        return out;
    }

    /** Where Load puts what it loads: a place's number, or -1 for the first with room. */
    public int into() {
        return into;
    }

    /** Chooses where Load puts what it loads, as the picker does. */
    public void chooseInto(final int place) {
        into = place >= 0 && place < places.size() ? place : -1;
    }

    /** Centre of the Recipes/Interfaces tab {@code index} (0 or 1), in the coordinates the content is drawn in. */
    public int[] tabCenter(final int index) {
        return tabs.tabCenter(index);
    }

    /** Centre of action button {@code index}: 0 Load, 1 into, 2 Load all, 3 Download, 4 Remove, 5 Move to. */
    public int[] actionButtonCenter(final int index) {
        final Button[] buttons = {loadButton, intoButton, loadAllButton, downloadButton, removeButton, moveButton};
        return buttons[Math.max(0, Math.min(buttons.length - 1, index))].center();
    }

    /** Centre of the {@code index}-th visible row of the media (left) list. */
    public int[] mediaRowCenter(final int index) {
        return mediaList.rowCenter(index);
    }

    /** Centre of the {@code index}-th visible row of the places (right) list, headers counted. */
    public int[] placeRowCenter(final int index) {
        return placeList.rowCenter(index);
    }

    /** The row of the places list that shows the recipe called {@code name}, or -1. */
    public int placeRowOf(final String name) {
        final List<Row> all = rows();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).entry() != null && GameText.resolve(all.get(i).entry().name()).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /** Centre of the {@code index}-th visible row of the Interfaces tab's list. */
    public int[] interfaceRowCenter(final int index) {
        return interfaceList.rowCenter(index);
    }

    /** Centre of the Interfaces tab's pause button on row {@code index}. */
    public int[] pauseButtonCenter(final int index) {
        final int[] rect = interfaceList.rowRect(index);
        final int bx = columnX(rect[0], lastW, 6);
        return new int[] {bx + (rect[0] + lastW - PAD - bx) / 2, rect[1] + I_ROW_H / 2};
    }

    private void request() {
        PacketDistributor.sendToServer(new RequestCraftManagerPayload(host));
    }

    /** Places the tab's components from the content rectangle; the other tab's are hidden. */
    private void layout(final int x, final int y, final int width, final int height) {
        tabs.setBounds(x, y, width, TAB_H);
        tabs.setSelected(tab);
        final int cy = y + TAB_H;
        final int ch = height - TAB_H;
        final boolean recipes = tab == 0;
        warnLabel.setVisible(recipes && loaded && !hasCard);
        warnLabel.setBounds(x + 4, cy + 3, width - 8, 8);
        final int top = loaded && !hasCard ? cy + 1 + HEADER_H : cy;
        final int barY = cy + ch - BAR_H;
        final int listTop = top + HEADER_H;
        final int listH = barY - 11 - listTop;
        final int leftW = (width - PAD * 3) * 2 / 5;
        final int rightW = width - PAD * 3 - leftW;
        final int leftX = x + PAD;
        final int rightX = leftX + leftW + PAD;
        mediaHeader.setVisible(recipes);
        mediaHeader.setBounds(leftX + 3, top + 2, leftW - 6, 8);
        romHeader.setVisible(recipes);
        romHeader.setBounds(rightX + 3, top + 2, rightW - 6, 8);
        mediaList.setVisible(recipes);
        mediaList.setBounds(leftX, listTop, leftW, listH);
        mediaEmpty.setVisible(recipes && mediaFiles.isEmpty());
        mediaEmpty.setBounds(leftX + 4, listTop + 4, leftW - 8, 8);
        placeList.setVisible(recipes);
        placeList.setBounds(rightX, listTop, rightW, listH);
        placesEmpty.setVisible(recipes && places.isEmpty());
        placesEmpty.setBounds(rightX + 4, listTop + 4, rightW - 8, 8);
        statusLabel.setVisible(recipes);
        statusLabel.setBounds(x + 4, barY - 9, width - 8, 8);
        final boolean anySelected = !selectedRefs.isEmpty();
        final Button[] bar = {loadButton, intoButton, loadAllButton, downloadButton, removeButton, moveButton};
        final boolean[] enabled = {
                hasCard && !selectedMedia.isEmpty(), hasCard && !places.isEmpty(),
                hasCard && !mediaVolumeKey.isEmpty(), hasCard && anySelected && !mediaVolumeKey.isEmpty(),
                hasCard && anySelected, hasCard && anySelected,
        };
        // The "into" button takes twice a plain one: it names the place.
        final int unit = (width - PAD * (bar.length + 1)) / (bar.length + 1);
        int bx = x + PAD;
        for (int i = 0; i < bar.length; i++) {
            final int w = i == 1 ? unit * 2 : unit;
            bar[i].setVisible(recipes);
            bar[i].setEnabled(loaded && enabled[i]);
            bar[i].setBounds(bx, barY + PAD, w, BTN_H);
            bx += w + PAD;
        }
        final boolean interfacesTab = !recipes;
        noCardLabel.setVisible(interfacesTab && !hasCard);
        noCardLabel.setBounds(x + PAD, cy + PAD, width - PAD * 2, 8);
        noInterfacesLabel.setVisible(interfacesTab && hasCard && interfaces.isEmpty());
        noInterfacesLabel.setBounds(x + PAD, cy + PAD + 12, width - PAD * 2, 8);
        final boolean show = interfacesTab && hasCard;
        summaryLabel.setVisible(show);
        summaryLabel.setBounds(x + PAD, cy + 3, width - PAD * 2, 8);
        final int cardRows = Math.max(1, cards().size());
        final int cardsH = cardRows * ROW_H + 2;
        final int bottomNotes = (waiting > 0 ? 10 : 0) + 10;
        final int cardsY = cy + ch - PAD - bottomNotes - cardsH;
        final int listY = cy + 14 + 12;
        interfaceColumns.setVisible(show && !interfaces.isEmpty());
        interfaceColumns.setBounds(x + 1, cy + 14, width - 2, 12);
        interfaceColumns.setColumnX(columnX(x, width, 0), columnX(x, width, 1), columnX(x, width, 2),
                columnX(x, width, 3), columnX(x, width, 4), columnX(x, width, 5));
        interfaceList.setVisible(show && !interfaces.isEmpty());
        interfaceList.setBounds(x + PAD, listY, width - PAD * 2, Math.max(I_ROW_H, cardsY - 12 - listY));
        cardsHeader.setVisible(show);
        cardsHeader.setBounds(x + PAD + 2, cardsY - 10, width - PAD * 2, 8);
        cardList.setVisible(show);
        cardList.setBounds(x + PAD, cardsY, width - PAD * 2, cardsH);
        waitingLabel.setVisible(show && waiting > 0);
        waitingLabel.setBounds(x + PAD, cardsY + cardsH + 2, width - PAD * 2, 8);
        hintLabel.setVisible(show && !interfaces.isEmpty());
        hintLabel.setBounds(x + PAD, cy + ch - PAD - 8, width - PAD * 2, 8);
    }

    /* The places list as rows: each place's header, then what it holds. */
    private List<Row> rows() {
        final List<Row> out = new ArrayList<>();
        for (int p = 0; p < places.size(); p++) {
            out.add(new Row(p, null));
            for (final WireRomEntry entry : places.get(p).entries()) {
                out.add(new Row(p, entry));
            }
        }
        return out;
    }

    private List<WirePlace> cards() {
        final List<WirePlace> out = new ArrayList<>();
        for (final WirePlace place : places) {
            if (place.card()) {
                out.add(place);
            }
        }
        return out;
    }

    private String summaryText() {
        final int cards = cards().size();
        return GameText.resolve((cards == 1 ? SUMMARY_ONE : SUMMARY).with(interfaces.size(), budget, cards));
    }

    private String intoLabel() {
        final Text where = into < 0 || into >= places.size() ? FIRST_WITH_ROOM.text() : places.get(into).title();
        return GameText.resolve(INTO.with(where));
    }

    private void renderMediaRow(final GuiGraphics g, final UiContext ctx, final String file, final int index,
                                final int x, final int y, final int w, final int h, final boolean hovered,
                                final boolean selected) {
        renderSelectableRow(g, ctx, file, selectedMedia.contains(index), x, y, w, h, hovered, 3);
    }

    private void renderPlaceRow(final GuiGraphics g, final UiContext ctx, final Row row, final int index, final int x,
                                final int y, final int w, final int h, final boolean hovered,
                                final boolean selected) {
        final WirePlace place = places.get(row.place());
        if (row.entry() == null) {
            g.fill(x, y, x + w, y + h, ctx.skin().panelBg());
            final String title = GameText.resolve(place.card() ? BENCH_RECIPES.with(place.title()) : place.title());
            final String count = GameText.resolve(PLACE_COUNT.with(place.used(), place.capacity()));
            final int countW = ctx.font().width(count);
            Draw.text(g, ctx.font(), Texts.clip(ctx.font(), title, w - countW - 10), x + 3, y + 2,
                    ctx.skin().text());
            Draw.text(g, ctx.font(), count, x + w - countW - 3, y + 2, ctx.skin().dim());
            return;
        }
        final String name = (row.entry().inMedia() ? "= " : "") + GameText.resolve(row.entry().name());
        renderSelectableRow(g, ctx, name, selectedRefs.contains(row.entry().ref()), x, y, w, h, hovered, 10);
    }

    private static void renderSelectableRow(final GuiGraphics g, final UiContext ctx, final String text,
                                            final boolean sel, final int x, final int y, final int w, final int h,
                                            final boolean hovered, final int indent) {
        ctx.skin().listRow(g, x, y, w, h, hovered, sel);
        Draw.text(g, ctx.font(), Texts.clip(ctx.font(), text, w - indent - 3), x + indent, y + 2,
                ctx.skin().listRowText(sel));
    }

    private void placeRowClicked(final int index, final int button, final double mx, final double my) {
        final List<Row> all = rows();
        if (index < 0 || index >= all.size()) {
            return;
        }
        final Row row = all.get(index);
        if (row.entry() == null) {
            into = row.place(); // a click on a place's header makes it where Load goes
        } else {
            toggle(selectedRefs, row.entry().ref());
        }
    }

    private List<Integer> pickerChoices() {
        final List<Integer> out = new ArrayList<>();
        if (!pickingMove) {
            out.add(-1);
        }
        for (int p = 0; p < places.size(); p++) {
            out.add(p);
        }
        return out;
    }

    private void renderPickerRow(final GuiGraphics g, final UiContext ctx, final Integer place, final int index,
                                 final int x, final int y, final int w, final int h, final boolean hovered,
                                 final boolean selected) {
        final boolean current = !pickingMove && place == into;
        ctx.skin().listRow(g, x, y, w, h, hovered, current);
        final String label = place < 0 ? GameText.resolve(FIRST_WITH_ROOM)
                : GameText.resolve(places.get(place).title()) + "  " + GameText.resolve(PLACE_COUNT.with(
                        places.get(place).used(), places.get(place).capacity()));
        Draw.text(g, ctx.font(), Texts.clip(ctx.font(), label, w - 6), x + 3, y + 2, ctx.skin().listRowText(current));
    }

    private void pickerClicked(final int index, final int button, final double mx, final double my) {
        final List<Integer> choices = pickerChoices();
        if (button != 0 || index < 0 || index >= choices.size()) {
            return;
        }
        final int place = choices.get(index);
        if (pickingMove) {
            for (final int ref : selectedRefs) {
                PacketDistributor.sendToServer(new MoveCraftPayload(host, ref, place));
            }
            selectedRefs.clear();
        } else {
            into = place;
        }
        placePicker.close();
    }

    private void openPicker(final boolean move) {
        pickingMove = move;
        pickerList.setScroll(0);
        placePicker.open();
        placePicker.placeIn(intoButton.x() - 20, intoButton.y() - 130, lastW, 130);
    }

    private void layoutPicker(final Popup p) {
        pickerList.setBounds(p.x() + 5, p.contentTop(), p.width() - 10, p.bottom() - p.contentTop() - 5);
    }

    private void renderInterfaceRow(final GuiGraphics g, final UiContext ctx, final WireInterface row,
                                    final int index, final int x, final int y, final int w, final int h,
                                    final boolean hovered, final boolean selected) {
        final Font font = ctx.font();
        ctx.skin().listRow(g, x, y, w, h, hovered, false);
        final int ty = y + 3;
        final String[] cells = {
                GameText.resolve(row.name()), GameText.resolve(row.machine()),
                GameText.resolve(PATTERNS.with(row.patterns(), row.capacity())),
                GameText.resolve(row.exclusive() ? EXCLUSIVE : NOT_EXCLUSIVE), stateText(row),
                row.maxJobs() == 0 ? GameText.resolve(JOBS_AUTO) : Integer.toString(row.maxJobs()),
        };
        for (int c = 0; c < cells.length; c++) {
            final int cx = columnX(x - PAD, lastW, c);
            final int room = columnX(x - PAD, lastW, c + 1) - cx - 3;
            final int colour = c == 4 ? stateColour(row) : c == 0 ? ctx.skin().text() : ctx.skin().dim();
            Texts.small(g, font, Texts.clip(font, cells[c], Texts.smallFits(room)), cx, ty, colour);
        }
        final int bx = columnX(x - PAD, lastW, 6);
        final int bw = x + w - bx - 2;
        final boolean paused = row.state() == CraftManagerStatePayload.PAUSED;
        ctx.skin().button(g, font, bx, y + 1, bw, h - 2, Texts.clip(font, GameText.resolve(paused ? RESUME : PAUSE),
                bw - 4), ctx.over(bx, y + 1, bw, h - 2), false, false);
    }

    private String stateText(final WireInterface row) {
        return switch (row.state()) {
            case CraftManagerStatePayload.PAUSED -> GameText.resolve(PAUSED);
            case CraftManagerStatePayload.NO_MACHINE -> GameText.resolve(NO_MACHINE);
            case CraftManagerStatePayload.RUNNING, CraftManagerStatePayload.DRAINING -> GameText.resolve(row.detail());
            default -> GameText.resolve(IDLE);
        };
    }

    private int stateColour(final WireInterface row) {
        return switch (row.state()) {
            case CraftManagerStatePayload.RUNNING -> PALETTE.get().good();
            case CraftManagerStatePayload.PAUSED, CraftManagerStatePayload.DRAINING -> PALETTE.get().warn();
            case CraftManagerStatePayload.NO_MACHINE -> PALETTE.get().bad();
            default -> skin.dim();
        };
    }

    private void interfaceRowClicked(final int index, final int button, final double mx, final double my) {
        if (index < 0 || index >= interfaces.size()) {
            return;
        }
        final WireInterface row = interfaces.get(index);
        final int[] rect = interfaceList.rowRect(index);
        final int left = rect[0] - PAD;
        if (mx >= columnX(left, lastW, 6)) {
            send(row, SetCraftInterfacePayload.PAUSED, row.state() == CraftManagerStatePayload.PAUSED ? 0 : 1);
        } else if (mx >= columnX(left, lastW, 5)) {
            final int jobs = button == 1 ? Math.max(0, row.maxJobs() - 1)
                    : row.maxJobs() >= MOST_JOBS ? 0 : row.maxJobs() + 1;
            send(row, SetCraftInterfacePayload.MAX_JOBS, jobs);
        } else if (mx >= columnX(left, lastW, 3) && mx < columnX(left, lastW, 4)) {
            send(row, SetCraftInterfacePayload.EXCLUSIVE, row.exclusive() ? 0 : 1);
        }
    }

    private void send(final WireInterface row, final int setting, final int value) {
        PacketDistributor.sendToServer(new SetCraftInterfacePayload(host, row.place(), setting, value));
    }

    private void renderCardRow(final GuiGraphics g, final UiContext ctx, final WirePlace card, final int index,
                               final int x, final int y, final int w, final int h, final boolean hovered,
                               final boolean selected) {
        final String line = GameText.resolve(CARD_LINE.with(card.title(), card.drives(), card.used(),
                card.capacity()));
        Draw.text(g, ctx.font(), Texts.clip(ctx.font(), line, w - 6), x + 3, y + 2, ctx.skin().text());
    }

    private void loadSelected() {
        final List<String> files = new ArrayList<>();
        for (final int i : selectedMedia) {
            if (i < mediaFiles.size()) {
                files.add(mediaFiles.get(i));
            }
        }
        if (!files.isEmpty()) {
            PacketDistributor.sendToServer(new LoadFromMediaPayload(host, mediaVolumeKey, files, false, into));
        }
    }

    private void loadAll() {
        PacketDistributor.sendToServer(new LoadFromMediaPayload(host, mediaVolumeKey, List.of(), true, into));
    }

    private void downloadSelected() {
        if (!selectedRefs.isEmpty()) {
            PacketDistributor.sendToServer(new DownloadToMediaPayload(host, mediaVolumeKey,
                    List.copyOf(selectedRefs)));
        }
    }

    private void removeSelected() {
        if (!selectedRefs.isEmpty()) {
            PacketDistributor.sendToServer(new RemoveRomCraftPayload(host, List.copyOf(selectedRefs)));
            selectedRefs.clear();
        }
    }

    private static void toggle(final Set<Integer> set, final int value) {
        if (!set.remove(value)) {
            set.add(value);
        }
    }

    private static int columnX(final int x, final int width, final int column) {
        return x + PAD + (int) Math.round((width - PAD * 2) * (column < COLUMNS.length ? COLUMNS[column] : 1.0));
    }

    /* A row of the places list: a place's header (no entry), or one recipe it holds. */
    private record Row(int place, @Nullable WireRomEntry entry) {
    }

    /** The card-required warning's band and ink, and the colours of a running, held and stuck interface. */
    private record Colours(int warnBand, int warnText, int good, int warn, int bad) {
    }
}
