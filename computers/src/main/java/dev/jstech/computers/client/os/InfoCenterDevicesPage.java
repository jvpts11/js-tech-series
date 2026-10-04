/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.InfoCenterLayout;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.os.devices.DeviceRows;
import dev.jstech.computers.os.devices.InfoCenterRows;
import dev.jstech.core.client.gui.component.ScrollBar;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The "Devices by port" page of KDE's Info Center: every port the machine has, by kind, beside what is plugged into
 * it, and the button that disables or enables the device selected.
 *
 * <p>Everything shown is the machine's, asked for when the page is opened and after every change; a device the
 * computer disabled is crossed out.
 */
final class InfoCenterDevicesPage {

    private final BlockPos host;
    private final Consumer<DeviceMap> listener = this::accept;
    private final ScrollBar bar = new ScrollBar(this::mostScroll, () -> this.scroll, v -> this.scroll = v);
    @Nullable
    private DeviceMap map;
    private List<InfoCenterRows.Row> rows = List.of();
    /** The device selected, by its position, so the selection survives the machine's next answer. */
    private long selected = DeviceRows.NO_DEVICE;
    private int scroll;
    private boolean listening;
    /* Where the page was last drawn on the screen, so a click read in window terms reaches the scrollbar. */
    private int originX;
    private int originY;

    private static final int SCROLL_W = 5;

    InfoCenterDevicesPage(final BlockPos host) {
        this.host = host;
    }

    /** The page is up: listen for the machine's answers and ask for the first. */
    void open() {
        if (!this.listening) {
            ClientDeviceMaps.listen(this.host, this.listener);
            this.listening = true;
        }
    }

    /** The page or its window went away. */
    void close() {
        ClientDeviceMaps.forget(this.listener);
        this.listening = false;
    }

    /** The rows as the page reads them, port and device, for a test. */
    List<String> shownRows() {
        final List<String> out = new ArrayList<>();
        for (final InfoCenterRows.Row row : this.rows) {
            final String device = GameText.resolve(row.device());
            out.add(device.isEmpty() ? GameText.resolve(row.port()) : GameText.resolve(row.port()) + ": " + device);
        }
        return out;
    }

    /** Whether the row reading {@code label} stands for a disabled device. */
    boolean rowDisabled(final String label) {
        final int index = shownRows().indexOf(label);
        return index >= 0 && this.rows.get(index).disabled();
    }

    /** The middle of the row reading {@code label}, from the window's top left, or null while it is not shown. */
    @Nullable
    int[] rowCentre(final String label) {
        final int index = shownRows().indexOf(label) - this.scroll;
        if (index < 0 || index >= InfoCenterLayout.rowsShown()) {
            return null;
        }
        return new int[] {InfoCenterLayout.W / 2,
                InfoCenterLayout.LIST_Y + index * InfoCenterLayout.ROW_H + InfoCenterLayout.ROW_H / 2};
    }

    void render(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y, final int mx,
                final int my) {
        if (this.map == null) {
            Texts.small(g, font, GameText.resolve(DeviceManagerTexts.READING), x + InfoCenterLayout.PAGE_PAD,
                    y + InfoCenterLayout.LIST_Y, skin.dim());
            return;
        }
        this.originX = x;
        this.originY = y;
        final int listW = rowWidth();
        final int shown = InfoCenterLayout.rowsShown();
        for (int i = 0; i < shown && this.scroll + i < this.rows.size(); i++) {
            final InfoCenterRows.Row row = this.rows.get(this.scroll + i);
            final int ry = y + InfoCenterLayout.LIST_Y + i * InfoCenterLayout.ROW_H;
            final int rx = x + InfoCenterLayout.PAGE_PAD;
            final boolean hovered = row.isDevice() && mx >= rx && mx < rx + listW && my >= ry
                    && my < ry + InfoCenterLayout.ROW_H;
            skin.listRow(g, rx, ry, listW, InfoCenterLayout.ROW_H, hovered,
                    row.isDevice() && row.pos() == this.selected);
            renderRow(g, font, skin, row, x, ry);
        }
        this.bar.setBounds(x + InfoCenterLayout.W - InfoCenterLayout.PAGE_PAD - SCROLL_W, y + InfoCenterLayout.LIST_Y,
                SCROLL_W, shown * InfoCenterLayout.ROW_H);
        this.bar.render(g, new UiContext(skin, font, mx, my, 0f));
        final DeviceMap.Device device = ClientDeviceMaps.device(this.map, this.selected);
        final TextKey words = device != null && device.disabled() ? InfoCenterTexts.ENABLE_SELECTED
                : InfoCenterTexts.DISABLE_SELECTED;
        final int bx = x + InfoCenterLayout.buttonX();
        final int by = y + InfoCenterLayout.BUTTON_Y;
        final boolean hot = device != null && mx >= bx && mx < bx + InfoCenterLayout.BUTTON_W && my >= by
                && my < by + InfoCenterLayout.BUTTON_H;
        skin.button(g, font, bx, by, InfoCenterLayout.BUTTON_W, InfoCenterLayout.BUTTON_H, "", hot, false, false);
        final String label = GameText.resolve(words);
        Texts.small(g, font, label, bx + (InfoCenterLayout.BUTTON_W - Texts.smallWidth(font, label)) / 2,
                by + (InfoCenterLayout.BUTTON_H - InfoCenterLayout.TEXT_H) / 2,
                device != null ? skin.text() : skin.dim());
    }

    /** A click from the window's top left: a row selects its device, the button disables or enables it. */
    boolean mouseClicked(final double mx, final double my) {
        if (this.bar.contains(this.originX + mx, this.originY + my)
                && this.bar.mouseClicked(this.originX + mx, this.originY + my, 0)) {
            return true;
        }
        if (mx >= InfoCenterLayout.buttonX() && mx < InfoCenterLayout.buttonX() + InfoCenterLayout.BUTTON_W
                && my >= InfoCenterLayout.BUTTON_Y && my < InfoCenterLayout.BUTTON_Y + InfoCenterLayout.BUTTON_H) {
            final DeviceMap.Device device = ClientDeviceMaps.device(this.map, this.selected);
            if (device != null) {
                ClientDeviceMaps.setDisabled(this.host, device.pos(), !device.disabled());
            }
            return true;
        }
        final int index = this.scroll + (int) Math.floor((my - InfoCenterLayout.LIST_Y) / InfoCenterLayout.ROW_H);
        if (my >= InfoCenterLayout.LIST_Y && my < InfoCenterLayout.LIST_Y + InfoCenterLayout.LIST_H
                && index >= 0 && index < this.rows.size() && this.rows.get(index).isDevice()) {
            this.selected = this.rows.get(index).pos();
            return true;
        }
        return false;
    }

    boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, Math.min(mostScroll(), this.scroll - (int) Math.signum(delta)));
        return true;
    }

    private void accept(final DeviceMap answer) {
        this.map = answer;
        this.rows = InfoCenterRows.rows(answer);
        this.scroll = Math.min(this.scroll, mostScroll());
    }

    /** How far the list scrolls: the rows past the ones the page shows at once. */
    private int mostScroll() {
        return Math.max(0, this.rows.size() - InfoCenterLayout.rowsShown());
    }

    /** How wide a row is: the page's inside, less the scrollbar while there is more than it shows. */
    private int rowWidth() {
        return InfoCenterLayout.W - 2 * InfoCenterLayout.PAGE_PAD - (mostScroll() > 0 ? SCROLL_W + 1 : 0);
    }

    private void renderRow(final GuiGraphics g, final Font font, final OsSkin skin, final InfoCenterRows.Row row,
                           final int x, final int y) {
        final int textY = y + (InfoCenterLayout.ROW_H - InfoCenterLayout.TEXT_H) / 2 + 1;
        final boolean chosen = row.isDevice() && row.pos() == this.selected;
        final int ink = chosen ? skin.listRowText(true) : skin.text();
        if (row.kind() == InfoCenterRows.Kind.HEADING) {
            icon(g, skin, row.icon(), x + InfoCenterLayout.PAGE_PAD, y + 1);
            Texts.small(g, font, GameText.resolve(row.port()), x + InfoCenterLayout.PAGE_PAD
                    + InfoCenterLayout.ICON + 3, textY, skin.text());
            return;
        }
        final int portTextX = x + InfoCenterLayout.PORT_X + InfoCenterLayout.ICON + 2;
        final int portRoom = InfoCenterLayout.DEVICE_X - InfoCenterLayout.PORT_X - InfoCenterLayout.ICON - 6;
        if (row.kind() == InfoCenterRows.Kind.PORT) {
            icon(g, skin, row.icon(), x + InfoCenterLayout.PORT_X, y + 1);
        }
        Texts.small(g, font, Texts.clip(font, GameText.resolve(row.port()), Texts.smallFits(portRoom)), portTextX,
                textY, row.kind() == InfoCenterRows.Kind.HUB_COUNT ? skin.dim() : ink);
        final int deviceX = x + InfoCenterLayout.DEVICE_X;
        if (!row.deviceIcon().isEmpty()) {
            icon(g, skin, row.deviceIcon(), deviceX, y + 1);
        }
        final int deviceTextX = deviceX + InfoCenterLayout.ICON + 2;
        final int deviceRoom = InfoCenterLayout.W - InfoCenterLayout.PAGE_PAD - InfoCenterLayout.DEVICE_X
                - InfoCenterLayout.ICON - 4;
        final String device = Texts.clip(font, GameText.resolve(row.device()), Texts.smallFits(deviceRoom));
        final int deviceInk = row.free() || row.disabled() || row.kind() == InfoCenterRows.Kind.HUB_COUNT
                ? (chosen ? ink : skin.dim()) : ink;
        Texts.small(g, font, device, deviceTextX, textY, deviceInk);
        if (row.disabled()) {
            final int lineY = y + InfoCenterLayout.ROW_H / 2;
            g.fill(deviceTextX, lineY, deviceTextX + Texts.smallWidth(font, device), lineY + 1, deviceInk);
        }
    }

    /* A device or port icon in the skin's treatment, when one has been drawn for it. */
    private static void icon(final GuiGraphics g, final OsSkin skin, final String kind, final int x, final int y) {
        if (kind.isEmpty()) {
            return;
        }
        final ResourceLocation sprite = SkinSprites.find("device", kind, kind, skin.iconSet());
        if (SkinSprites.exists(sprite)) {
            SkinSprites.draw(g, sprite, x, y, InfoCenterLayout.ICON, InfoCenterLayout.ICON, 16);
        }
    }
}
