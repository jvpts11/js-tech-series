/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import static dev.jstech.computers.client.os.UpdatePopupTexts.BACK;
import static dev.jstech.computers.client.os.UpdatePopupTexts.BACK_ANYWHERE;
import static dev.jstech.computers.client.os.UpdatePopupTexts.CANCEL;
import static dev.jstech.computers.client.os.UpdatePopupTexts.CARD_ENCHANT;
import static dev.jstech.computers.client.os.UpdatePopupTexts.CARD_REPAIR;
import static dev.jstech.computers.client.os.UpdatePopupTexts.CARD_SMELT;
import static dev.jstech.computers.client.os.UpdatePopupTexts.COST;
import static dev.jstech.computers.client.os.UpdatePopupTexts.FREE;
import static dev.jstech.computers.client.os.UpdatePopupTexts.HELD;
import static dev.jstech.computers.client.os.UpdatePopupTexts.HELD_NOWHERE;
import static dev.jstech.computers.client.os.UpdatePopupTexts.LOADING;
import static dev.jstech.computers.client.os.UpdatePopupTexts.MAX;
import static dev.jstech.computers.client.os.UpdatePopupTexts.NAME;
import static dev.jstech.computers.client.os.UpdatePopupTexts.NOT_TAKEN;
import static dev.jstech.computers.client.os.UpdatePopupTexts.NO_CHANGE;
import static dev.jstech.computers.client.os.UpdatePopupTexts.PAY;
import static dev.jstech.computers.client.os.UpdatePopupTexts.SMELT;
import static dev.jstech.computers.client.os.UpdatePopupTexts.TAB_ENCHANT;
import static dev.jstech.computers.client.os.UpdatePopupTexts.TAB_REPAIR;
import static dev.jstech.computers.client.os.UpdatePopupTexts.TAB_SMELT;
import static dev.jstech.computers.client.os.UpdatePopupTexts.TITLE;
import static dev.jstech.computers.client.os.UpdatePopupTexts.UPDATE;
import static dev.jstech.computers.client.os.UpdatePopupTexts.WAIT_NETWORK;
import static dev.jstech.computers.client.os.UpdatePopupTexts.WAIT_WORKSHOP;
import static dev.jstech.computers.client.os.UpdatePopupTexts.WITH;
import static dev.jstech.computers.client.os.UpdatePopupTexts.WITH_NOTHING;
import static dev.jstech.computers.client.os.UpdatePopupTexts.WORN;
import static dev.jstech.computers.client.os.UpdatePopupTexts.YOU_HAVE;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.ANVIL_ARROW_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.ANVIL_ARROW_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.BACK_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.BADGE_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.BAR_H;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.BAR_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.BODY_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.BUTTON_H;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.BUTTON_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.CANCEL_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.CARD_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.CELL;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.COST_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.FIELD_H;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.FIELD_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.FIELD_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.FOOT_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.ITEM_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.ITEM_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.LEFT_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.LINE;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.MAX_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.MAX_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.NAME_ROW_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.NAME_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.NAME_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.OFFER_H;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.OFFER_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.PAD;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.PANEL_H;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.PANEL_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.PANEL_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.PANEL_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.PAY_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.QTY_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.QTY_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.RESULT_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.RIGHT_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.SMELT_ARROW_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.SMELT_ARROW_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.SMELT_RESULT_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.STATUS_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.STATUS_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.TAB_H;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.TAB_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.TEXT_W;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.TITLE_H;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.UPDATE_X;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.WAIT_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.WHERE_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.WITH_Y;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.offerY;
import static dev.jstech.computers.gui.layout.UpdateWindowLayout.tabX;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.UpdateWindowLayout;
import dev.jstech.computers.operation.payload.update.UpdatePreviewPayload;
import dev.jstech.computers.operation.payload.update.UpdatePreviewRequestPayload;
import dev.jstech.computers.operation.payload.update.UpdateSubmitPayload;
import dev.jstech.computers.operation.payload.workshop.WorkshopStatePayload;
import dev.jstech.computers.program.iql.IqlUpdate;
import dev.jstech.computers.workshop.UpdateAction;
import dev.jstech.computers.workshop.UpdateDoor;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.core.client.gui.component.AmountStepper;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Interactor's Update window: one tab for each card's action on an item the network holds, Enchant,
 * Smelt and Repair, each greyed out when the computer has no card for it or the item does not take it. The panel
 * shows the item, how many the network holds and where, the card that does the work, and the tab's own part: the
 * three offers with their clue and price, the furnace's amount and the wait behind the Workshop's own smelting, or
 * the repair with the most worn of the item, its material from the network, the name and the price beside an
 * anvil's. Update sends the UPDATE; the window closes once the network has taken it.
 *
 * <p>The geometry is {@link UpdateWindowLayout}'s; everything shown comes from the server, asked again whenever the
 * name changes, since an anvil's result and price depend on it.
 */
@PaletteHolder
public final class UpdatePopup extends Popup {

    private final BlockPos host;
    private final BlockPos monitorPos;
    private final TabStrip tabs;
    private final UiComponent panel;
    private final TextField nameField;
    private final AmountStepper amount;
    private final Button max;
    private final Button update;
    private final Button cancel;
    private ItemStack item = ItemStack.EMPTY;
    @Nullable
    private UpdatePreviewPayload preview;
    private int offer = -1;
    private boolean tabChosen;
    private String status = "";

    public static final int TAB_ENCHANT_INDEX = 0;
    public static final int TAB_SMELT_INDEX = 1;
    public static final int TAB_REPAIR_INDEX = 2;
    /** This window's own colours: what is at work, what the player cannot afford, and the offer picked. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/update",
            new Colours(0xFF2E8B3E, 0xFFB03A2E, 0xFF316AC5));
    private static final int OFFERS = 3;

    /* The window open now, which the server's answers go to; null while none is. */
    @Nullable
    private static UpdatePopup active;

    public UpdatePopup(final BlockPos host, final BlockPos monitorPos) {
        super(UpdatePopup::titleText, UpdateWindowLayout.W, UpdateWindowLayout.H);
        this.host = host;
        this.monitorPos = monitorPos;
        tabs = add(new TabStrip(List.of(GameText.resolve(TAB_ENCHANT), GameText.resolve(TAB_SMELT),
                GameText.resolve(TAB_REPAIR))));
        // Too narrow for a word after the label: a tab that cannot be used only dims; the panel says why.
        tabs.setDisabled(i -> !tabOpen(i), () -> "");
        tabs.setOnSelect(i -> tabChosen = true);
        panel = add(new UiComponent() {
            @Override
            public void render(final GuiGraphics g, final UiContext ctx) {
                renderPanel(g, ctx);
            }

            @Override
            public boolean mouseClicked(final double mx, final double my, final int button) {
                return clickOffer(mx, my);
            }
        });
        nameField = add(new TextField(IqlUpdate.MAX_NAME));
        nameField.setOnEdit(this::requestPreview);
        amount = add(new AmountStepper());
        max = add(new Button(GameText.resolve(MAX), this::maxAmount).setLabelScale(Texts.SMALL));
        update = add(new Button(GameText.resolve(UPDATE), this::submit).setPrimary(true).setLabelScale(Texts.SMALL));
        cancel = add(new Button(GameText.resolve(CANCEL), this::close).setLabelScale(Texts.SMALL));
        setLayouter(popup -> layout());
        setOnClose(() -> {
            if (active == this) {
                active = null;
            }
            preview = null;
            item = ItemStack.EMPTY;
        });
    }

    /** Delivers what the server says about the item to the window open now. */
    public static void accept(final UpdatePreviewPayload payload) {
        final UpdatePopup window = active;
        if (window == null || !window.isOpen()) {
            return;
        }
        window.preview = payload;
        window.status = GameText.resolve(payload.status());
        if (payload.sent()) {
            window.close();
            return;
        }
        if (window.nameField.value().isEmpty() && !payload.name().isEmpty()) {
            window.nameField.sync(payload.name());
        }
        window.amount.setRange(1, Math.max(1L, payload.held()));
        if (!window.tabChosen) {
            for (int i = 0; i < OFFERS; i++) {
                if (window.tabOpen(i)) {
                    window.tabs.setSelected(i);
                    break;
                }
            }
        }
        if (window.offer < 0 || !window.offerThere(window.offer)) {
            window.offer = window.firstOffer();
        }
    }

    /** Opens the window over {@code stack}, an item as the network holds it, and asks the server what to show. */
    public void openFor(final ItemStack stack) {
        item = stack.copyWithCount(1);
        preview = null;
        offer = -1;
        tabChosen = false;
        status = "";
        nameField.sync("");
        amount.setRange(1, Long.MAX_VALUE / 4).setAmount(1);
        active = this;
        open();
        requestPreview();
    }

    /** The tab up now, one of the {@code TAB_} indices. */
    public int tab() {
        return tabs.selected();
    }

    /** Whether tab {@code tab} can be used: the card is in the computer and the item takes its action. */
    public boolean tabOpen(final int tab) {
        if (preview == null) {
            return false;
        }
        return switch (tab) {
            case TAB_ENCHANT_INDEX -> WorkshopCard.ENCHANTING.in(preview.cards())
                    && preview.takes(UpdateAction.ENCHANT);
            case TAB_SMELT_INDEX -> WorkshopCard.FURNACE.in(preview.cards()) && preview.takes(UpdateAction.SMELT);
            case TAB_REPAIR_INDEX -> WorkshopCard.ANVIL.in(preview.cards());
            default -> false;
        };
    }

    /** What the server said last, or null before it has. */
    @Nullable
    public UpdatePreviewPayload preview() {
        return preview;
    }

    /** The status line, as the footer shows it. */
    public String status() {
        return status;
    }

    /** Window-local centre of tab {@code tab}, for a test's click. */
    public int[] tabCenter(final int tab) {
        return new int[] {x() + tabX(tab) + TAB_W / 2, y() + TITLE_H + TAB_H / 2};
    }

    /** Centre of offer {@code index}, for a test's click. */
    public int[] offerCenter(final int index) {
        return new int[] {panelX() + OFFER_X + TEXT_W / 2, panelY() + offerY(index) + OFFER_H / 2};
    }

    /** Centre of the Update button, for a test's click. */
    public int[] updateCenter() {
        return new int[] {x() + UPDATE_X + BUTTON_W / 2, y() + FOOT_Y + BUTTON_H / 2};
    }

    /** The offer picked, from zero, or -1. */
    public int offer() {
        return offer;
    }

    private static String titleText() {
        final UpdatePopup window = active;
        return GameText.resolve(TITLE.with(window == null ? "" : window.item.getHoverName().getString()));
    }

    private void layout() {
        tabs.setBounds(x() + PAD, y() + TITLE_H, UpdateWindowLayout.TABS * (TAB_W + 2) - 2, TAB_H);
        panel.setBounds(x() + PANEL_X, y() + PANEL_Y, PANEL_W, PANEL_H);
        final boolean ready = preview != null && tabOpen(tab());
        nameField.setBounds(panelX() + FIELD_X, panelY() + NAME_ROW_Y, FIELD_W, FIELD_H);
        nameField.setVisible(ready && tab() == TAB_REPAIR_INDEX);
        amount.setBounds(panelX() + QTY_X, panelY() + BODY_Y + 3, QTY_W, FIELD_H);
        amount.setVisible(ready && tab() == TAB_SMELT_INDEX);
        max.setBounds(panelX() + MAX_X, panelY() + BODY_Y + 3, MAX_W, FIELD_H);
        max.setVisible(ready && tab() == TAB_SMELT_INDEX);
        update.setBounds(x() + UPDATE_X, y() + FOOT_Y, BUTTON_W, BUTTON_H);
        update.setEnabled(canUpdate());
        cancel.setBounds(x() + CANCEL_X, y() + FOOT_Y, BUTTON_W, BUTTON_H);
    }

    private int panelX() {
        return x() + PANEL_X;
    }

    private int panelY() {
        return y() + PANEL_Y;
    }

    private void requestPreview() {
        if (!item.isEmpty()) {
            PacketDistributor.sendToServer(new UpdatePreviewRequestPayload(monitorPos, host, item, nameField.value()));
        }
    }

    private void maxAmount() {
        if (preview != null) {
            amount.setAmount(preview.held());
        }
    }

    /* Whether Update can be pressed now: the tab is open and what it would do is there and paid for. */
    private boolean canUpdate() {
        if (preview == null || !tabOpen(tab())) {
            return false;
        }
        return switch (tab()) {
            case TAB_ENCHANT_INDEX -> offerThere(offer) && affordable(Math.max(preview.offers().get(offer).required(),
                    preview.offers().get(offer).levels()));
            case TAB_SMELT_INDEX -> preview.held() > 0L && !preview.smeltsInto().isEmpty();
            default -> !preview.repaired().isEmpty() && affordable(preview.cardLevels());
        };
    }

    private void submit() {
        if (!canUpdate() || preview == null) {
            return;
        }
        final int tab = tab();
        final UpdateAction action = tab == TAB_ENCHANT_INDEX ? UpdateAction.ENCHANT : tab == TAB_SMELT_INDEX
                ? UpdateAction.SMELT : repairs() ? UpdateAction.REPAIR : UpdateAction.NAME;
        final ItemStack target = action == UpdateAction.REPAIR || action == UpdateAction.NAME ? preview.worn() : item;
        final String name = action == UpdateAction.REPAIR || action == UpdateAction.NAME ? nameField.value() : "";
        PacketDistributor.sendToServer(new UpdateSubmitPayload(monitorPos, host, target, action.id(),
                action == UpdateAction.SMELT ? amount.amount() : 1L, Math.max(0, offer), name));
    }

    /* Whether the Repair tab mends the item, rather than only naming it: it is worn and something mends it. */
    private boolean repairs() {
        return preview != null && preview.worn().isDamaged() && !preview.material().isEmpty();
    }

    private boolean offerThere(final int index) {
        return preview != null && index >= 0 && index < preview.offers().size()
                && preview.offers().get(index).required() > 0;
    }

    private int firstOffer() {
        for (int i = 0; i < OFFERS; i++) {
            if (offerThere(i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean affordable(final int levels) {
        final var player = Minecraft.getInstance().player;
        return player != null && (player.getAbilities().instabuild || preview != null && preview.level() >= levels);
    }

    private boolean clickOffer(final double mx, final double my) {
        if (tab() != TAB_ENCHANT_INDEX || preview == null) {
            return false;
        }
        for (int i = 0; i < OFFERS; i++) {
            final int oy = panelY() + offerY(i);
            if (mx >= panelX() + OFFER_X && mx < panelX() + OFFER_X + TEXT_W && my >= oy && my < oy + OFFER_H
                    && offerThere(i)) {
                offer = i;
                return true;
            }
        }
        return false;
    }

    // drawing

    private void renderPanel(final GuiGraphics g, final UiContext ctx) {
        final Font font = ctx.font();
        final int px = panelX();
        final int py = panelY();
        ctx.skin().panel(g, px, py, PANEL_W, PANEL_H);
        cell(g, ctx, px + ITEM_X, py + ITEM_Y, item);
        Draw.text(g, font, Texts.clip(font, item.getHoverName().getString(), PANEL_W - NAME_X - 4), px + NAME_X,
                py + NAME_Y, ctx.skin().text());
        Texts.small(g, font, Texts.clip(font, heldLine(), Texts.smallFits(PANEL_W - NAME_X - 4)), px + NAME_X,
                py + WHERE_Y, ctx.skin().dim());
        if (preview == null) {
            Texts.small(g, font, GameText.resolve(LOADING), px + 4, py + CARD_Y, ctx.skin().dim());
            Texts.small(g, font, Texts.clip(font, status, Texts.smallFits(STATUS_W)), x() + PAD, y() + STATUS_Y,
                    ctx.skin().dim());
            return;
        }
        if (tabOpen(tab())) {
            Texts.small(g, font, Texts.clip(font, cardLine(), Texts.smallFits(TEXT_W)), px + 4, py + CARD_Y,
                    ctx.skin().dim());
            switch (tab()) {
                case TAB_ENCHANT_INDEX -> renderEnchant(g, ctx, px, py);
                case TAB_SMELT_INDEX -> renderSmelt(g, ctx, px, py);
                default -> renderRepair(g, ctx, px, py);
            }
        } else {
            Texts.small(g, font, Texts.clip(font, closedLine(), Texts.smallFits(TEXT_W)), px + 4, py + CARD_Y,
                    PALETTE.get().lacking());
        }
        Texts.small(g, font, Texts.clip(font, status, Texts.smallFits(STATUS_W)), x() + PAD, y() + STATUS_Y,
                ctx.skin().text());
    }

    private void renderEnchant(final GuiGraphics g, final UiContext ctx, final int px, final int py) {
        final Font font = ctx.font();
        for (int i = 0; i < OFFERS; i++) {
            final int ox = px + OFFER_X;
            final int oy = py + offerY(i);
            final boolean there = offerThere(i);
            ctx.skin().field(g, ox, oy, TEXT_W, OFFER_H, i == offer);
            if (i == offer) {
                Draw.outline(g, ox, oy, TEXT_W, OFFER_H, PALETTE.get().picked());
            }
            if (!there) {
                continue;
            }
            final WorkshopStatePayload.Offer shown = preview.offers().get(i);
            final boolean can = affordable(Math.max(shown.required(), shown.levels()));
            g.fill(ox + 1, oy + 1, ox + 1 + BADGE_W, oy + OFFER_H - 1, can ? PALETTE.get().working()
                    : ctx.skin().edge());
            final String needed = Integer.toString(shown.required());
            Texts.small(g, font, needed, ox + 1 + (BADGE_W - Texts.smallWidth(font, needed)) / 2, oy + 3,
                    ctx.skin().panelBg());
            final String cost = GameText.resolve(UpdateDoor.levels(shown.levels()));
            final int costW = Texts.smallWidth(font, cost);
            final int ink = can ? ctx.skin().text() : ctx.skin().dim();
            Texts.small(g, font, cost, ox + TEXT_W - costW - 4, oy + 3, ink);
            final String clue = GameText.resolve(WorkshopAppTexts.CLUE.with(GameText.of(shown.clue())));
            final int clueX = ox + BADGE_W + 5;
            Texts.small(g, font, Texts.clip(font, clue, Texts.smallFits(ox + TEXT_W - costW - 8 - clueX)), clueX,
                    oy + 3, ink);
        }
        if (offerThere(offer)) {
            final int levels = preview.offers().get(offer).levels();
            Texts.small(g, font, Texts.clip(font, GameText.resolve(PAY.with(levels, preview.level())),
                    Texts.smallFits(TEXT_W)), px + OFFER_X, py + PAY_Y, ctx.skin().dim());
        }
    }

    private void renderSmelt(final GuiGraphics g, final UiContext ctx, final int px, final int py) {
        final Font font = ctx.font();
        Texts.small(g, font, GameText.resolve(SMELT), px + 4, py + BODY_Y + 5, ctx.skin().text());
        arrow(g, ctx, px + SMELT_ARROW_X, py + BODY_Y + 5, SMELT_ARROW_W);
        cell(g, ctx, px + SMELT_RESULT_X, py + BODY_Y, preview.smeltsInto());
        final String into = preview.smeltsInto().getHoverName().getString();
        final long count = amount.amount() * Math.max(1, preview.smeltsInto().getCount());
        final String back = preview.where().isEmpty() ? GameText.resolve(BACK_ANYWHERE.with(count, into))
                : GameText.resolve(BACK.with(count, into, preview.where().get(0)));
        Texts.small(g, font, Texts.clip(font, back, Texts.smallFits(TEXT_W)), px + 4, py + BACK_Y,
                ctx.skin().text());
        ctx.skin().field(g, px + 4, py + BAR_Y, TEXT_W, BAR_H, false);
        if (preview.workshopTotal() > 0) {
            final int filled = (int) ((TEXT_W - 2) * (long) preview.workshopDone() / preview.workshopTotal());
            g.fill(px + 5, py + BAR_Y + 1, px + 5 + filled, py + BAR_Y + BAR_H - 1, PALETTE.get().working());
        }
        final String wait = preview.workshopTotal() > 0
                ? GameText.resolve(WAIT_WORKSHOP.with(preview.workshopDone(), preview.workshopTotal()))
                : preview.networkBusy() ? GameText.resolve(WAIT_NETWORK) : GameText.resolve(FREE);
        final List<String> lines = wrap(font, wait, TEXT_W);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            Texts.small(g, font, lines.get(i), px + 4, py + WAIT_Y + i * LINE, ctx.skin().dim());
        }
    }

    private void renderRepair(final GuiGraphics g, final UiContext ctx, final int px, final int py) {
        final Font font = ctx.font();
        final ItemStack worn = preview.worn();
        cell(g, ctx, px + LEFT_X, py + BODY_Y, worn);
        Draw.text(g, font, "+", px + LEFT_X + CELL + 4, py + BODY_Y + 5, ctx.skin().dim());
        cell(g, ctx, px + RIGHT_X, py + BODY_Y, preview.material().isEmpty() ? ItemStack.EMPTY
                : preview.material().copyWithCount(preview.materialUsed()));
        arrow(g, ctx, px + ANVIL_ARROW_X, py + BODY_Y + 5, ANVIL_ARROW_W);
        cell(g, ctx, px + RESULT_X, py + BODY_Y, preview.repaired());
        if (worn.isDamageableItem() && worn.isDamaged()) {
            final String used = GameText.resolve(WORN.with(worn.getDamageValue(), worn.getMaxDamage()));
            Texts.small(g, font, Texts.clip(font, used, Texts.smallFits(PANEL_W - RESULT_X - CELL - 8)),
                    px + RESULT_X + CELL + 4, py + BODY_Y + 5, ctx.skin().dim());
        }
        final String with = preview.material().isEmpty() ? GameText.resolve(WITH_NOTHING)
                : GameText.resolve(WITH.with(preview.material().getHoverName().getString(), preview.materialUsed(),
                preview.materialHeld()));
        Texts.small(g, font, Texts.clip(font, with, Texts.smallFits(TEXT_W)), px + LEFT_X, py + WITH_Y,
                ctx.skin().text());
        Texts.small(g, font, GameText.resolve(NAME), px + LEFT_X, py + NAME_ROW_Y + 2, ctx.skin().text());
        final String cost = preview.repaired().isEmpty() ? GameText.resolve(NO_CHANGE)
                : GameText.resolve(COST.with(UpdateDoor.levels(preview.cardLevels()), preview.anvilLevels()));
        final String have = GameText.resolve(YOU_HAVE.with(preview.level()));
        final int haveW = Texts.smallWidth(font, have);
        Texts.small(g, font, Texts.clip(font, cost, Texts.smallFits(TEXT_W - haveW - 6)), px + LEFT_X, py + COST_Y,
                preview.repaired().isEmpty() || !affordable(preview.cardLevels()) ? PALETTE.get().lacking()
                        : ctx.skin().text());
        Texts.small(g, font, have, px + LEFT_X + TEXT_W - haveW, py + COST_Y, ctx.skin().dim());
    }

    /* The line under the item's name: how many the network holds and on which servers. */
    private String heldLine() {
        if (preview == null) {
            return "";
        }
        if (preview.held() <= 0L) {
            return GameText.resolve(HELD_NOWHERE);
        }
        return GameText.resolve(HELD.with(preview.held(), String.join(", ", preview.where())));
    }

    /* The card that does the tab's work, on which computer, and how it works. */
    private String cardLine() {
        final String computer = preview.computer();
        return switch (tab()) {
            case TAB_ENCHANT_INDEX -> GameText.resolve(CARD_ENCHANT.with(cardName(WorkshopCard.ENCHANTING), computer));
            case TAB_SMELT_INDEX -> GameText.resolve(CARD_SMELT.with(cardName(WorkshopCard.FURNACE), computer,
                    preview.furnaceSpeed()));
            default -> GameText.resolve(CARD_REPAIR.with(cardName(WorkshopCard.ANVIL), computer));
        };
    }

    /* Why the tab up cannot be used: the computer has no card for it, or the item does not take its action. */
    private String closedLine() {
        final WorkshopCard card = switch (tab()) {
            case TAB_ENCHANT_INDEX -> WorkshopCard.ENCHANTING;
            case TAB_SMELT_INDEX -> WorkshopCard.FURNACE;
            default -> WorkshopCard.ANVIL;
        };
        return card.in(preview.cards()) ? GameText.resolve(NOT_TAKEN)
                : GameText.resolve(UpdateDoor.NO_CARD.with(cardName(card)));
    }

    private static Text cardName(final WorkshopCard card) {
        return GameText.of(UpdateDoor.cardItem(card).getHoverName());
    }

    private void cell(final GuiGraphics g, final UiContext ctx, final int x, final int y, final ItemStack stack) {
        ctx.skin().field(g, x, y, CELL, CELL, false);
        if (!stack.isEmpty()) {
            DesktopItems.itemWithCount(g, ctx.font(), stack, x + 1, y + 1, null);
        }
    }

    /* An arrow {@code w} wide, drawn in the skin's edge colour. */
    private static void arrow(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w) {
        final int head = 5;
        final int shaft = w - head;
        final int colour = ctx.skin().edge();
        for (int i = 0; i < w; i++) {
            if (i < shaft) {
                g.fill(x + i, y + 2, x + i + 1, y + 6, colour);
            } else {
                final int rise = 4 - (i - shaft);
                g.fill(x + i, y + 4 - rise, x + i + 1, y + 4 + rise, colour);
            }
        }
    }

    /* {@code text} broken into lines of the small text no wider than {@code width}, at the spaces. */
    private static List<String> wrap(final Font font, final String text, final int width) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            final String tried = line.isEmpty() ? word : line + " " + word;
            if (Texts.smallWidth(font, tried) <= width || line.isEmpty()) {
                line.setLength(0);
                line.append(tried);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** This window's colours: what is at work, what the player cannot afford, and the offer picked. */
    private record Colours(int working, int lacking, int picked) {
    }
}
