/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import static dev.jstech.computers.client.FirmwareScreenTexts.HTTP_BACK;
import static dev.jstech.computers.client.FirmwareScreenTexts.HTTP_COMMUNITY;
import static dev.jstech.computers.client.FirmwareScreenTexts.HTTP_INSTALL;
import static dev.jstech.computers.client.FirmwareScreenTexts.HTTP_MORE;
import static dev.jstech.computers.client.FirmwareScreenTexts.HTTP_SCROLL;
import static dev.jstech.computers.client.FirmwareScreenTexts.HTTP_STARTED;
import static dev.jstech.computers.client.FirmwareScreenTexts.HTTP_TITLE;
import static dev.jstech.computers.client.FirmwareScreenTexts.HTTP_WHERE;
import static dev.jstech.computers.client.FirmwareScreenTexts.LOCAL_DISK;
import static dev.jstech.computers.client.FirmwareScreenTexts.NET_INSTALLER;
import static dev.jstech.computers.client.FirmwareScreenTexts.ONLY_RUNS;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_ADDRESSES;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_AGENT;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_BACK;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_CARD;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_CHOOSE;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_COMMUNITY;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_ENTER;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_MIRROR;
import static dev.jstech.computers.client.FirmwareScreenTexts.PXE_TITLE;
import static dev.jstech.computers.client.FirmwareScreenTexts.of;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.FirmwareLayout;
import dev.jstech.computers.gui.layout.NetBootLayout;
import dev.jstech.computers.operation.payload.firmware.NetBootPayloads;
import dev.jstech.computers.operation.payload.firmware.NetworkBoot;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.boot.NetworkAddresses;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Mirror's menu, once the firmware has asked the network (computers/docs/design/operating-systems.md): on the
 * Legacy's BIOS the card's PXE agent takes the screen, its addresses at the top and the menu in the BIOS's blue; on
 * the Standard's UEFI the list fills the Boot Manager's panel, with INSTALL and BACK.
 *
 * <p>The menu lists every system the Mirror serves this machine and, last, booting from the local disk. It draws and
 * picks; what a choice does is the setup's, which sends it.
 */
@PaletteHolder
final class NetBootMenu {

    private final boolean agent;
    private final List<int[]> rowHits = new ArrayList<>();
    private NetBootPayloads.Menu menu;
    private int selected;
    private int top;
    @Nullable
    private int[] installHit;
    @Nullable
    private int[] backHit;

    /** The PXE agent's own screen: black, with its lines in a light grey. */
    private static final Palette<Agent> AGENT = Palettes.declare(JsComputers.MODID, "firmware/pxe",
            new Agent(0xFF000000, 0xFFC8C8C8));

    /**
     * @param agent whether the PXE agent draws it, on the Legacy's BIOS, rather than the UEFI's HTTP Boot
     * @param menu  what the Mirror answered
     */
    NetBootMenu(final boolean agent, final NetBootPayloads.Menu menu) {
        this.agent = agent;
        this.menu = menu;
    }

    /** A fresh answer for the same menu, keeping the row the player is on. */
    void update(final NetBootPayloads.Menu answer) {
        this.menu = answer;
        // A shorter answer than the last would leave the list scrolled past its end, showing nothing.
        final int listed = this.agent ? offers().size() : rows();
        this.top = Math.max(0, Math.min(this.top, listed - 1));
        move(0);
    }

    /** Moves the choice up or down by {@code step}, within the list. */
    void move(final int step) {
        this.selected = Math.max(0, Math.min(rows() - 1, this.selected + step));
        final int visible = this.agent ? NetBootLayout.ROWS : NetBootLayout.HTTP_ROWS;
        final int listed = this.agent ? offers().size() : rows();
        if (this.selected < listed) {
            this.top = Math.max(0, Math.min(this.top, this.selected));
            this.top = Math.max(this.top, this.selected - visible + 1);
        }
    }

    /** Whether the choice is booting from the local disk rather than one of the Mirror's systems. */
    boolean onLocalDisk() {
        return this.selected >= offers().size();
    }

    /** The system chosen, as the Mirror names it. */
    String chosenSystem() {
        return onLocalDisk() ? "" : offers().get(this.selected).id();
    }

    /** The row under the pointer, or -1; the first click picks it, a second one on the same row chooses it. */
    int rowAt(final double mouseX, final double mouseY) {
        for (final int[] hit : this.rowHits) {
            if (in(hit, mouseX, mouseY)) {
                return hit[4];
            }
        }
        return -1;
    }

    /** Puts the choice on that row; answers whether it was already there. */
    boolean pick(final int row) {
        final boolean again = row == this.selected;
        this.selected = row;
        return again;
    }

    boolean overInstall(final double mouseX, final double mouseY) {
        return in(this.installHit, mouseX, mouseY);
    }

    boolean overBack(final double mouseX, final double mouseY) {
        return in(this.backHit, mouseX, mouseY);
    }

    /** The HTTP Boot panel's title, which names the Mainframe. */
    String httpTitle() {
        return of(HTTP_TITLE.with(this.menu.mirror()));
    }

    /** The PXE agent's whole screen, on the monitor's glass at {@code (x, y)}. */
    void renderAgent(final GuiGraphics g, final Font font, final int x, final int y) {
        this.rowHits.clear();
        final FirmwareScreen.Bios bios = FirmwareScreen.BIOS.get();
        g.fill(x, y, x + FirmwareLayout.WIDTH, y + FirmwareLayout.HEIGHT, AGENT.get().ground());
        final String[] lines = {
                of(PXE_AGENT.with(Branding.HARDWARE_HOUSE)),
                of(PXE_CARD.with(this.menu.card())),
                of(PXE_ADDRESSES.with(this.menu.address(), NetworkAddresses.MASK, NetworkAddresses.MAINFRAME)),
                of(PXE_MIRROR.with(this.menu.mirror(), NetworkAddresses.MAINFRAME))};
        for (int i = 0; i < lines.length; i++) {
            small(g, font, lines[i], x + NetBootLayout.AGENT_X,
                    y + NetBootLayout.AGENT_Y + i * NetBootLayout.AGENT_STEP, AGENT.get().text());
        }
        final int bx = x + NetBootLayout.BOX_X;
        final int by = y + NetBootLayout.BOX_Y;
        g.fill(bx, by, bx + NetBootLayout.BOX_W, by + NetBootLayout.BOX_H, bios.ground());
        Draw.outline(g, bx, by, NetBootLayout.BOX_W, NetBootLayout.BOX_H, bios.border());
        Draw.outline(g, bx + 2, by + 2, NetBootLayout.BOX_W - 4, NetBootLayout.BOX_H - 4, bios.border());
        // The title sits across the top border, on the box's own blue.
        final String title = " " + of(PXE_TITLE.with(this.menu.mirror())) + " ";
        final int tx = bx + (NetBootLayout.BOX_W - font.width(title)) / 2;
        g.fill(tx, by - 1, tx + font.width(title), by + 3, bios.ground());
        Draw.text(g, font, title, tx, by - NetBootLayout.TITLE_RISE, bios.text());
        final int rowX = x + NetBootLayout.ROW_X;
        final List<NetworkBoot.Offer> offers = offers();
        for (int i = this.top; i < offers.size() && i < this.top + NetBootLayout.ROWS; i++) {
            final NetworkBoot.Offer offer = offers.get(i);
            final int ry = y + NetBootLayout.ROWS_TOP + (i - this.top) * NetBootLayout.ROW_PITCH;
            agentRow(g, font, i, rowX, ry, of(NET_INSTALLER.with(offer.name())),
                    offer.community() ? of(PXE_COMMUNITY.with(offer.house())) : offer.house());
        }
        g.fill(rowX, y + NetBootLayout.RULE_Y, rowX + NetBootLayout.ROW_W, y + NetBootLayout.RULE_Y + 1,
                bios.border());
        agentRow(g, font, offers.size(), rowX, y + NetBootLayout.LOCAL_Y, of(LOCAL_DISK), "");
        final int textX = rowX + NetBootLayout.TEXT_INSET;
        small(g, font, of(ONLY_RUNS), textX, y + NetBootLayout.NOTE_Y, bios.text());
        small(g, font, of(PXE_ENTER) + "   " + of(PXE_CHOOSE) + "   " + of(PXE_BACK), textX,
                y + NetBootLayout.KEYS_Y, bios.amber());
    }

    /**
     * The HTTP Boot list inside the UEFI's Boot Manager panel at {@code (x, y)}, {@code w} by {@code h}: the rows, the
     * line that says where the list came from, and INSTALL and BACK.
     */
    void renderHttp(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                    final double mouseX, final double mouseY) {
        this.rowHits.clear();
        final FirmwareScreen.Uefi uefi = FirmwareScreen.UEFI.get();
        final int count = rows();
        final int rowH = FirmwareLayout.ROW_H + FirmwareLayout.UEFI_ROW_H_BONUS;
        final int left = x + FirmwareLayout.UEFI_ROW_INSET;
        final int right = x + w - FirmwareLayout.UEFI_ROW_INSET;
        for (int slot = 0; slot < NetBootLayout.HTTP_ROWS && this.top + slot < count; slot++) {
            final int row = this.top + slot;
            final int ry = y + NetBootLayout.HTTP_ROWS_TOP + slot * NetBootLayout.HTTP_ROW_PITCH;
            final boolean sel = row == this.selected;
            this.rowHits.add(new int[]{left, ry, right - left, rowH, row});
            g.fill(left, ry, right, ry + rowH, sel ? uefi.selection() : uefi.placeholder());
            if (sel) {
                g.fill(left, ry, left + FirmwareLayout.UEFI_ROW_ACCENT_W, ry + rowH, uefi.accent());
            }
            dot(g, x, ry, uefi.amber());
            final boolean local = row >= offers().size();
            final NetworkBoot.Offer offer = local ? null : offers().get(row);
            Draw.text(g, font, local ? of(LOCAL_DISK) : offer.name(), x + FirmwareLayout.UEFI_LABEL_X,
                    ry + FirmwareLayout.UEFI_LABEL_DY, uefi.text());
            final String where = local ? "" : of(offer.community() ? HTTP_COMMUNITY.with(offer.house())
                    : HTTP_WHERE.with(offer.house(), offer.image()));
            small(g, font, where, x + w - smallWidth(font, where) - FirmwareLayout.UEFI_DETAIL_RIGHT_MARGIN,
                    ry + FirmwareLayout.UEFI_LABEL_DY + 1, uefi.dim());
        }
        final int hidden = count - this.top - NetBootLayout.HTTP_ROWS;
        if (hidden > 0) {
            final int ry = y + NetBootLayout.HTTP_ROWS_TOP + NetBootLayout.HTTP_ROWS * NetBootLayout.HTTP_ROW_PITCH;
            dot(g, x, ry, uefi.dim());
            Draw.text(g, font, of(HTTP_MORE.with(hidden)), x + FirmwareLayout.UEFI_LABEL_X,
                    ry + FirmwareLayout.UEFI_LABEL_DY, uefi.dim());
            final String scroll = of(HTTP_SCROLL);
            small(g, font, scroll, x + w - smallWidth(font, scroll) - FirmwareLayout.UEFI_DETAIL_RIGHT_MARGIN,
                    ry + FirmwareLayout.UEFI_LABEL_DY + 1, uefi.dim());
        }
        final int by = y + h - FirmwareLayout.UEFI_ACTION_BOTTOM_MARGIN;
        small(g, font, of(HTTP_STARTED.with(this.menu.mirror())), x + FirmwareLayout.UEFI_CONTENT_INSET_X,
                by - NetBootLayout.HTTP_NOTE_ABOVE, uefi.dim());
        this.installHit = new int[]{x + FirmwareLayout.UEFI_BOOT_BTN_X, by, FirmwareLayout.UEFI_BOOT_BTN_W,
                FirmwareLayout.UEFI_ACTION_BTN_H};
        button(g, font, this.installHit, of(HTTP_INSTALL), in(this.installHit, mouseX, mouseY) ? uefi.text()
                : uefi.accent(), uefi.onAccent());
        this.backHit = new int[]{x + NetBootLayout.HTTP_BACK_X, by, NetBootLayout.HTTP_BACK_W,
                FirmwareLayout.UEFI_ACTION_BTN_H};
        button(g, font, this.backHit, of(HTTP_BACK), in(this.backHit, mouseX, mouseY) ? uefi.text()
                : uefi.placeholder(), uefi.dim());
    }

    /* One row of the agent's menu: the system, and who made it at the right; the chosen one on the bar. */
    private void agentRow(final GuiGraphics g, final Font font, final int row, final int x, final int y,
                          final String name, final String house) {
        final FirmwareScreen.Bios bios = FirmwareScreen.BIOS.get();
        final boolean sel = row == this.selected;
        this.rowHits.add(new int[]{x, y - 2, NetBootLayout.ROW_W, NetBootLayout.ROW_PITCH, row});
        if (sel) {
            g.fill(x, y - 2, x + NetBootLayout.ROW_W, y - 2 + NetBootLayout.ROW_PITCH, bios.selection());
        }
        small(g, font, name, x + NetBootLayout.TEXT_INSET, y, sel ? bios.ground() : bios.text());
        small(g, font, house, x + NetBootLayout.ROW_W - NetBootLayout.TEXT_INSET - smallWidth(font, house), y,
                sel ? bios.ground() : bios.value());
    }

    /* The square at the start of a UEFI row. */
    private static void dot(final GuiGraphics g, final int x, final int ry, final int color) {
        g.fill(x + FirmwareLayout.UEFI_DOT_X, ry + FirmwareLayout.UEFI_DOT_DY,
                x + FirmwareLayout.UEFI_DOT_X + FirmwareLayout.UEFI_DOT_W,
                ry + FirmwareLayout.UEFI_DOT_DY + FirmwareLayout.UEFI_DOT_H, color);
    }

    private static void button(final GuiGraphics g, final Font font, final int[] hit, final String label,
                               final int fill, final int text) {
        g.fill(hit[0], hit[1], hit[0] + hit[2], hit[1] + hit[3], fill);
        Draw.textCentered(g, font, label, hit[0] + hit[2] / 2, hit[1] + FirmwareLayout.UEFI_ACTION_LABEL_DY, text);
    }

    private static void small(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                              final int color) {
        Draw.textScaled(g, font, text, x, y, color, NetBootLayout.SMALL);
    }

    private static int smallWidth(final Font font, final String text) {
        return Math.round(font.width(text) * NetBootLayout.SMALL);
    }

    private static boolean in(@Nullable final int[] r, final double mx, final double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    /* The Mirror's systems, then booting from the local disk. */
    private int rows() {
        return offers().size() + 1;
    }

    private List<NetworkBoot.Offer> offers() {
        return this.menu.offers();
    }

    /** The agent's screen: its ground, and its lines of addresses. */
    private record Agent(int ground, int text) {
    }
}
