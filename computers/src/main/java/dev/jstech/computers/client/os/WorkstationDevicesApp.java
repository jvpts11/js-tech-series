/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.WorkstationDevicesLayout;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.os.devices.DeviceRows;
import dev.jstech.computers.os.devices.WorkstationDeviceRows;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

/**
 * CDE's Devices dialog, opened from Workstation Info: a fact per port of the workstation (Video, Audio, then the
 * ports by their names) with what is plugged into it or "free", and Disable and Enable for the device selected.
 *
 * <p>Everything shown is the machine's, asked for when the dialog opens and after every change; a device the computer
 * disabled is written in the quieter ink and crossed out.
 */
@PaletteHolder
public final class WorkstationDevicesApp implements IDesktopApp {

    private final BlockPos host;
    private final Consumer<DeviceMap> listener = this::accept;
    private List<WorkstationDeviceRows.Row> rows = List.of();
    @Nullable
    private DeviceMap map;
    /** The device selected, by its position, so the selection survives the machine's next answer. */
    private long selected = DeviceRows.NO_DEVICE;
    private int scroll;
    private OsSkin skin;
    private int left;
    private int top;

    /** The key the dialog's window is opened and remembered under, which its window factory is registered at. */
    public static final String KEY = JsComputers.MODID + ":workstation_info/devices";

    /** This dialog's own colours, {@code jsc:app/workstation_devices}: the labels' and headings' quieter ink. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/workstation_devices",
            new Colours(0xFF3A3D4A));
    private static final String ELLIPSIS = "...";

    public WorkstationDevicesApp(final BlockPos host) {
        this.host = host;
        ClientDeviceMaps.listen(host, this.listener);
    }

    /** The facts as {@code label=value}, or nothing until the machine has answered. */
    public List<String> shownDevices() {
        final List<String> out = new ArrayList<>();
        for (final WorkstationDeviceRows.Row row : this.rows) {
            out.add(GameText.resolve(row.label()) + "=" + GameText.resolve(row.value()));
        }
        return out;
    }

    /** Whether the fact reading {@code fact} ({@code label=value}) stands for a disabled device. */
    public boolean deviceDisabled(final String fact) {
        final int index = shownDevices().indexOf(fact);
        return index >= 0 && this.rows.get(index).disabled();
    }

    /** The middle of the fact reading {@code fact}, in desktop pixels, or null while it is not shown. */
    @Nullable
    public int[] deviceCentre(final String fact) {
        final int shown = shownDevices().indexOf(fact) - this.scroll;
        if (shown < 0 || shown >= WorkstationDevicesLayout.ROWS) {
            return null;
        }
        return new int[] {this.left + WorkstationDevicesLayout.W / 2,
            this.top + WorkstationDevicesLayout.rowY(shown) + WorkstationDevicesLayout.ROW_H / 2};
    }

    /** The middle of Disable, in desktop pixels. */
    public int[] disableCentre() {
        return CdeStylePages.centre(WorkstationDevicesLayout.disable(), this.left, this.top);
    }

    /** The middle of Enable, in desktop pixels. */
    public int[] enableCentre() {
        return CdeStylePages.centre(WorkstationDevicesLayout.enable(), this.left, this.top);
    }

    @Override
    public String title() {
        return GameText.resolve(WorkstationInfoTexts.DEVICES_TITLE);
    }

    @Override
    public int defaultWidth() {
        return WorkstationDevicesLayout.W + WorkstationDevicesLayout.FRAME_W;
    }

    @Override
    public int defaultHeight() {
        return WorkstationDevicesLayout.H + WorkstationDevicesLayout.FRAME_H;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return defaultHeight();
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onRestored() {
        ClientDeviceMaps.forget(this.listener);
        ClientDeviceMaps.listen(this.host, this.listener);
    }

    @Override
    public void onClosed() {
        ClientDeviceMaps.forget(this.listener);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        final DesktopState desktop = DesktopScreen.current();
        if (this.skin == null || desktop == null) {
            return;
        }
        final CdePalette p = desktop.prefs().cdePalette();
        drawWell(g, font, p);
        final DeviceMap.Device device = ClientDeviceMaps.device(this.map, this.selected);
        drawButton(g, font, WorkstationDevicesLayout.disable(), WorkstationInfoTexts.DISABLE,
                device != null && !device.disabled(), mouseX, mouseY, false);
        drawButton(g, font, WorkstationDevicesLayout.enable(), WorkstationInfoTexts.ENABLE,
                device != null && device.disabled(), mouseX, mouseY, false);
        drawButton(g, font, WorkstationDevicesLayout.close(), WorkstationInfoTexts.CLOSE, true, mouseX, mouseY, true);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button != 0) {
            return;
        }
        final double mx = mouseX - this.left;
        final double my = mouseY - this.top;
        if (WorkstationDevicesLayout.close().holds(mx, my)) {
            ActiveDesktop.closeWindowFor(this);
            return;
        }
        final DeviceMap.Device device = ClientDeviceMaps.device(this.map, this.selected);
        if (device != null && WorkstationDevicesLayout.disable().holds(mx, my) && !device.disabled()) {
            ClientDeviceMaps.setDisabled(this.host, device.pos(), true);
            return;
        }
        if (device != null && WorkstationDevicesLayout.enable().holds(mx, my) && device.disabled()) {
            ClientDeviceMaps.setDisabled(this.host, device.pos(), false);
            return;
        }
        final int row = (int) Math.floor((my - WorkstationDevicesLayout.rowY(0)) / WorkstationDevicesLayout.ROW_H);
        if (WorkstationDevicesLayout.well().holds(mx, my) && row >= 0 && row < WorkstationDevicesLayout.ROWS
                && this.scroll + row < this.rows.size() && this.rows.get(this.scroll + row).isDevice()) {
            this.selected = this.rows.get(this.scroll + row).pos();
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final int most = Math.max(0, this.rows.size() - WorkstationDevicesLayout.ROWS);
        this.scroll = Math.max(0, Math.min(most, this.scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            ActiveDesktop.closeWindowFor(this);
            return true;
        }
        return false;
    }

    private void accept(final DeviceMap answer) {
        this.map = answer;
        this.rows = WorkstationDeviceRows.rows(answer);
        this.scroll = Math.min(this.scroll, Math.max(0, this.rows.size() - WorkstationDevicesLayout.ROWS));
    }

    /** The well: the heading, then a fact per port, the one selected marked, a disabled one crossed out. */
    private void drawWell(final GuiGraphics g, final Font font, final CdePalette p) {
        final Rect well = WorkstationDevicesLayout.well();
        this.skin.panel(g, this.left + well.x(), this.top + well.y(), well.w(), well.h());
        Texts.small(g, font, GameText.resolve(WorkstationInfoTexts.DEVICES).toUpperCase(Locale.ROOT),
                this.left + WorkstationDevicesLayout.innerX(), this.top + WorkstationDevicesLayout.headY(),
                PALETTE.get().labelInk());
        for (int r = 0; r < WorkstationDevicesLayout.ROWS && this.scroll + r < this.rows.size(); r++) {
            final WorkstationDeviceRows.Row row = this.rows.get(this.scroll + r);
            final int rowY = this.top + WorkstationDevicesLayout.rowY(r);
            if (row.isDevice() && row.pos() == this.selected) {
                g.fill(this.left + well.x() + 2, rowY - 1, this.left + well.x() + well.w() - 2,
                        rowY + WorkstationDevicesLayout.ROW_H - 1, p.active());
            }
            final String label = GameText.resolve(row.label());
            Texts.small(g, font, label,
                    this.left + WorkstationDevicesLayout.LABEL_RIGHT - Texts.smallWidth(font, label), rowY,
                    PALETTE.get().labelInk());
            final String value = fit(font, GameText.resolve(row.value()), WorkstationDevicesLayout.valueWidth());
            final int ink = row.free() || row.disabled() ? PALETTE.get().labelInk() : p.ink();
            Texts.small(g, font, value, this.left + WorkstationDevicesLayout.VALUE_X, rowY, ink);
            if (row.disabled()) {
                final int lineY = rowY + 3;
                g.fill(this.left + WorkstationDevicesLayout.VALUE_X, lineY,
                        this.left + WorkstationDevicesLayout.VALUE_X + Texts.smallWidth(font, value), lineY + 1,
                        ink);
            }
        }
    }

    /* A button along the foot; one that would do nothing now is drawn with its word in the quieter ink. */
    private void drawButton(final GuiGraphics g, final Font font, final Rect r, final TextKey key,
                            final boolean live, final int mouseX, final int mouseY, final boolean isDefault) {
        final String words = GameText.resolve(key);
        this.skin.button(g, font, this.left + r.x(), this.top + r.y(), r.w(), r.h(), live ? words : "",
                live && r.holds(mouseX - this.left, mouseY - this.top), false, isDefault);
        if (!live) {
            Texts.small(g, font, words, this.left + r.x() + (r.w() - Texts.smallWidth(font, words)) / 2,
                    this.top + r.y() + (r.h() - 7) / 2, PALETTE.get().labelInk());
        }
    }

    /** A fact in the small text, cut with an ellipsis when it is wider than its column. */
    private static String fit(final Font font, final String text, final int room) {
        if (Texts.smallWidth(font, text) <= room) {
            return text;
        }
        final int units = Math.max(1, Texts.smallFits(room) - font.width(ELLIPSIS));
        return font.plainSubstrByWidth(text, units) + ELLIPSIS;
    }

    /** This dialog's colours: the labels' and headings' quieter ink. */
    private record Colours(int labelInk) {
    }
}
