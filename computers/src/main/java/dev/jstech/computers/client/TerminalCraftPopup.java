/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.CraftPlanRequestPayload;
import dev.jstech.computers.operation.payload.CraftSubmitPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Having a thing made: how many, what it would take, and whether the network can do it now.
 *
 * <p>The plan is the machine's answer, not a guess: every change to the quantity asks again, and what
 * comes back says which ingredients are there and which are not. That is why there are two ways to say
 * go. One commits the whole craft, and the other takes as much of it as the network can actually make,
 * which is the honest thing to offer when the answer is "not all of it".
 */
final class TerminalCraftPopup {

    private static final int POPUP_W = ComputerTerminalLayout.POPUP_W;
    private static final int POPUP_H = ComputerTerminalLayout.POPUP_H;
    private static final long[] STEPS = {-64, -1, 1, 64};
    private static final String[] STEP_LABELS = {"-64", "-1", "+1", "+64"};
    private static final int PLAN_ROWS = 5;
    private static final long MOST = 99_999L;
    /*
     * Ticks of quiet before the machine is asked to plan again. Planning walks the whole recipe against
     * what the network holds, so asking on every keystroke would send one of those per character typed
     * and per click held on the step buttons. Waiting out the typing costs a tenth of a second.
     */
    private static final int PLAN_DELAY = 3;

    private final ComputerTerminalScreen screen;
    private final ComputerTerminalMenu menu;
    private final TerminalQuantityBox qty;

    @Nullable
    private CraftCatalogPayload.Entry entry;
    private long amount = 1;
    private int planCountdown = -1;

    TerminalCraftPopup(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu,
                       final TerminalQuantityBox qty) {
        this.screen = screen;
        this.menu = menu;
        this.qty = qty;
    }

    boolean isOpen() {
        return this.entry != null;
    }

    void open(final CraftCatalogPayload.Entry pattern) {
        this.entry = pattern;
        this.amount = 1;
        menu.setCraftPlan(null);
        placeField();
        this.qty.set(1);
        screen.giveKeyboardTo(this.qty);
        askForPlan();
    }

    void close() {
        this.entry = null;
        this.planCountdown = -1;
        menu.setCraftPlan(null);
    }

    /** Sends the plan request once the number being typed or clicked has stopped changing. */
    void tick() {
        if (this.planCountdown < 0) {
            return;
        }
        if (--this.planCountdown < 0) {
            askForPlan();
        }
    }

    /** Enter commits the whole craft, and only when the network says the whole craft is possible. */
    void submitIfPossible() {
        final var plan = menu.craftPlan();
        if (plan != null && plan.feasible()) {
            submit(false);
        }
    }

    /** Where the quantity field belongs while this question is up. */
    void placeField() {
        this.qty.moveTo(x() + 10, y() + 32);
    }

    /** What the typed digits mean, which is at least one of the thing and never absurdly many. */
    void typed(final long value) {
        this.amount = Math.max(1, Math.min(MOST, value));
        this.planCountdown = PLAN_DELAY;
    }

    void render(final GuiGraphics g, final int mouseX, final int mouseY) {
        final CraftCatalogPayload.Entry pattern = this.entry;
        if (pattern == null) {
            return;
        }
        final var plan = menu.craftPlan();
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        final int left = screen.left();
        final int top = screen.top();
        g.fill(left, top, left + ComputerTerminalLayout.WIDTH, top + ComputerTerminalLayout.HEIGHT, 0xE0070A0F);
        final int px = x();
        final int py = y();
        g.fill(px - 2, py - 2, px + POPUP_W + 2, py + POPUP_H + 2, 0xFF0A1A1F);
        g.fill(px, py, px + POPUP_W, py + POPUP_H, JsTechTheme.panel());
        g.fill(px, py, px + POPUP_W, py + 1, JsTechTheme.accent());

        screen.drawDataIcon(g, StorageKey.of(pattern.result()), -1L, px + 6, py + 5);
        g.drawString(screen.tabFont(), GameText.resolve(TerminalTexts.CRAFT_TITLE.with(
                        trim(pattern.result().getHoverName().getString(), 18))),
                px + 28, py + 8, JsTechTheme.text(), false);

        /*
         * The quantity field is a widget the screen drew earlier, under this panel. It is drawn again here,
         * at a raised z, or the number being typed would sit behind the panel it belongs to.
         */
        g.fill(px + 6, py + 28, px + 130, py + 46, JsTechTheme.track());
        this.qty.render(g, mouseX, mouseY, 0.0f);
        for (int i = 0; i < STEP_LABELS.length; i++) {
            final int bx = px + 134 + i * 16;
            g.fill(bx, py + 30, bx + 15, py + 44,
                    hover(mouseX, mouseY, bx, py + 30, 15, 14) ? JsTechTheme.hover() : JsTechTheme.screen());
            g.drawCenteredString(screen.tabFont(), STEP_LABELS[i], bx + 8, py + 33, JsTechTheme.accent());
        }

        g.drawString(screen.tabFont(), GameText.resolve(TerminalTexts.PLAN_RAW), px + 6, py + 52, JsTechTheme.dim(),
                false);
        int rowY = py + 63;
        if (plan == null) {
            g.drawString(screen.tabFont(), GameText.resolve(TerminalTexts.PLANNING), px + 6, rowY, JsTechTheme.dim(),
                    false);
        } else {
            for (int i = 0; i < Math.min(PLAN_ROWS, plan.rows().size()); i++) {
                final var row = plan.rows().get(i);
                screen.drawDataIcon(g, StorageKey.of(row.item()), -1L, px + 6, rowY - 2);
                g.drawString(screen.tabFont(), trim(row.item().getHoverName().getString(), 14),
                        px + 26, rowY + 2, JsTechTheme.text(), false);
                final String counts = ComputerTerminalScreen.fmt(row.have()) + " / "
                        + ComputerTerminalScreen.fmt(row.need());
                g.drawString(screen.tabFont(), counts, px + POPUP_W - screen.tabFont().width(counts) - 8,
                        rowY + 2, row.satisfied() ? JsTechTheme.green() : JsTechTheme.red(), false);
                rowY += 14;
            }
            if (plan.rows().size() > PLAN_ROWS) {
                g.drawString(screen.tabFont(),
                        GameText.resolve(TerminalTexts.MORE.with(plan.rows().size() - PLAN_ROWS)),
                        px + 26, rowY, JsTechTheme.dim(), false);
            }
            final String est = GameText.resolve(plan.estimateTicks() > 0
                    ? TerminalTexts.ESTIMATE.with(Math.max(1, plan.estimateTicks() / 20))
                    : TerminalTexts.NO_ESTIMATE.text());
            g.drawString(screen.tabFont(), est, px + 6, py + 144, JsTechTheme.dim(), false);
            if (!plan.feasible()) {
                g.drawString(screen.tabFont(), GameText.resolve(TerminalTexts.MAX_NOW.with(
                                ComputerTerminalScreen.fmt(plan.maxFeasible()))),
                        px + 70, py + 144, JsTechTheme.amber(), false);
            }
        }

        final boolean feasible = plan != null && plan.feasible();
        final boolean partialUseful = plan != null && !plan.feasible() && plan.maxFeasible() > 0;
        button(g, px + 6, py + 156, 56, GameText.resolve(TerminalTexts.CRAFT),
                feasible ? JsTechTheme.green() : JsTechTheme.dim(),
                feasible && hover(mouseX, mouseY, px + 6, py + 156, 56, 14));
        button(g, px + 66, py + 156, 84, GameText.resolve(TerminalTexts.PARTIAL_BUTTON),
                partialUseful ? JsTechTheme.amber() : JsTechTheme.dim(),
                partialUseful && hover(mouseX, mouseY, px + 66, py + 156, 84, 14));
        button(g, px + 154, py + 156, 44, GameText.resolve(TerminalTexts.CLOSE), JsTechTheme.dim(),
                hover(mouseX, mouseY, px + 154, py + 156, 44, 14));
        g.pose().popPose();
    }

    /** Modal: a click outside closes it, a right click closes it, everything else is answered here. */
    boolean clicked(final double mouseX, final double mouseY, final int button) {
        if (this.entry == null) {
            return false;
        }
        if (button == 1) {
            close();
            return true;
        }
        if (button != 0) {
            return true;
        }
        final int px = x();
        final int py = y();
        final int mx = (int) mouseX;
        final int my = (int) mouseY;
        if (mx < px || mx >= px + POPUP_W || my < py || my >= py + POPUP_H) {
            close();
            return true;
        }
        if (this.qty.isMouseOver(mouseX, mouseY)) {
            screen.giveKeyboardTo(this.qty);
            return this.qty.mouseClicked(mouseX, mouseY, button);
        }
        for (int i = 0; i < STEPS.length; i++) {
            final int bx = px + 134 + i * 16;
            if (hover(mx, my, bx, py + 30, 15, 14)) {
                setAmount(this.amount + STEPS[i]);
                return true;
            }
        }
        final var plan = menu.craftPlan();
        final boolean feasible = plan != null && plan.feasible();
        final boolean partialUseful = plan != null && !plan.feasible() && plan.maxFeasible() > 0;
        if (feasible && hover(mx, my, px + 6, py + 156, 56, 14)) {
            submit(false);
            return true;
        }
        if (partialUseful && hover(mx, my, px + 66, py + 156, 84, 14)) {
            submit(true);
            return true;
        }
        if (hover(mx, my, px + 154, py + 156, 44, 14)) {
            close();
            return true;
        }
        return true;
    }

    private void setAmount(final long value) {
        this.amount = Math.max(1, Math.min(MOST, value));
        this.qty.set(this.amount);
        this.planCountdown = PLAN_DELAY;
    }

    private void askForPlan() {
        this.planCountdown = -1;
        final CraftCatalogPayload.Entry pattern = this.entry;
        if (pattern != null) {
            PacketDistributor.sendToServer(new CraftPlanRequestPayload(
                    menu.monitorPos(), menu.hostPos(), pattern.result(), this.amount));
        }
    }

    private void submit(final boolean partial) {
        final CraftCatalogPayload.Entry pattern = this.entry;
        if (pattern == null) {
            return;
        }
        PacketDistributor.sendToServer(new CraftSubmitPayload(
                menu.monitorPos(), menu.hostPos(), pattern.result(), this.amount, partial, true,
                OperationPriority.DEFAULT));
        close();
    }

    private int x() {
        return screen.left() + (ComputerTerminalLayout.WIDTH - POPUP_W) / 2;
    }

    private int y() {
        return screen.top() + (ComputerTerminalLayout.HEIGHT - POPUP_H) / 2;
    }

    private void button(final GuiGraphics g, final int x, final int y, final int w,
                        final String label, final int color, final boolean hovered) {
        g.fill(x, y, x + w, y + 14, hovered ? JsTechTheme.hover() : JsTechTheme.screen());
        g.fill(x, y, x + w, y + 1, JsTechTheme.line());
        g.drawCenteredString(screen.tabFont(), label, x + w / 2, y + 3, color);
    }

    private static boolean hover(final double mouseX, final double mouseY, final int x, final int y,
                                 final int w, final int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static String trim(final String s, final int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
