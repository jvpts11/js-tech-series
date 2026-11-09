/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.BorderLinkLayout;
import dev.jstech.computers.operation.payload.wan.BorderLinkRequestPayload;
import dev.jstech.computers.operation.payload.wan.BorderLinkStatePayload;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.wan.PortSettings;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Nouvell BorderLink's window on the Frames desktops of a WAN Gateway Computer (computers/docs/design/network.md):
 * the gateway's four ports with the line in each, the gateway at its far end, whether the line extends this network or
 * federates two, and whether it is up; under them the port picked, what the network at its far end may do here,
 * whether the port relays for other networks, and whether it is on.
 *
 * <p>The switches are changed here and reach the gateway on Apply or OK, as a properties window's do; Cancel leaves
 * them as the gateway has them. The window asks the gateway how its lines stand every second.
 */
@PaletteHolder
public final class BorderLinkApp implements IDesktopApp {

    private final BlockPos host;
    private final boolean statusBar;
    private final Panel root = new Panel();
    /* The four rights' switches, in the order the window lays them out: two rows of two. */
    private final List<Checkbox> rights = new ArrayList<>();
    private final Checkbox relay;
    private final Checkbox on;
    private final Button[] buttons = new Button[3];
    /* The switches changed here and not yet applied, by port; -1 for a port left as the gateway has it. */
    private final int[] pending = new int[PORTS];
    @Nullable
    private BorderLinkStatePayload state;
    private int selected = 1;
    private long askedAt;
    private boolean closing;
    /* Where the content stood when last drawn, which a click on the list of ports is read against. */
    private int left;
    private int top;
    private OsSkin skin = OsSkin.fallback();

    private static final int PORTS = 4;
    private static final long ASK_MS = 1_000L;
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/borderlink",
            new Colours(0xFF10A010, 0xFFC42B1C));

    /* The window open now, which the gateway's answers are handed to. */
    @Nullable
    private static BorderLinkApp active;

    public BorderLinkApp(final BlockPos host, final ResourceLocation desktopId) {
        this.host = host;
        this.statusBar = desktopId != null && "frames_7".equals(desktopId.getPath());
        Arrays.fill(this.pending, -1);
        for (final WanRight right : WanRight.values()) {
            this.rights.add(this.root.add(new Checkbox(
                    () -> GameText.resolve(BorderLinkTexts.rightLabel(right)), () -> settings().allows(right),
                    () -> edit(s -> s.with(right, !s.allows(right))))).setBoxSize(BorderLinkLayout.CHECK_BOX));
        }
        this.relay = this.root.add(new Checkbox(() -> GameText.resolve(BorderLinkTexts.RELAY),
                () -> settings().relay(), () -> edit(s -> s.withRelay(!s.relay()))))
                .setBoxSize(BorderLinkLayout.CHECK_BOX);
        this.on = this.root.add(new Checkbox(() -> GameText.resolve(BorderLinkTexts.PORT_ON),
                () -> settings().on(), () -> edit(s -> s.withOn(!s.on())))).setBoxSize(BorderLinkLayout.CHECK_BOX);
        this.buttons[0] = this.root.add(new Button(GameText.resolve(BorderLinkTexts.OK), () -> {
            apply();
            this.closing = true;
        }));
        this.buttons[1] = this.root.add(new Button(GameText.resolve(BorderLinkTexts.CANCEL),
                () -> this.closing = true));
        this.buttons[2] = this.root.add(new Button(GameText.resolve(BorderLinkTexts.APPLY), this::apply));
        active = this;
        ask();
    }

    /** Hands the gateway's answer to the window open on it. */
    public static void accept(final BorderLinkStatePayload payload) {
        if (active != null && active.host.equals(payload.hostPos())) {
            active.state = payload;
        }
    }

    @Override
    public String title() {
        final String name = ProgramClient.nameOf(Programs.BORDERLINK);
        return this.state == null ? name : name + " " + this.state.version();
    }

    @Override
    public int defaultWidth() {
        return BorderLinkLayout.W;
    }

    @Override
    public int defaultHeight() {
        return BorderLinkLayout.height(this.statusBar);
    }

    @Override
    public int minWidth() {
        return BorderLinkLayout.W;
    }

    @Override
    public int minHeight() {
        return BorderLinkLayout.height(this.statusBar);
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public boolean wantsToClose() {
        return this.closing;
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void onRestored() {
        active = this;
        ask();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        if (Util.getMillis() - this.askedAt >= ASK_MS) {
            ask();
        }
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        final BorderLinkStatePayload shown = this.state;
        final boolean known = shown != null && shown.ports().size() == PORTS;
        for (final Checkbox box : this.rights) {
            box.setVisible(false);
        }
        this.relay.setVisible(false);
        this.on.setVisible(false);
        if (!known) {
            text(g, font, BorderLinkTexts.ASKING.text(), x + BorderLinkLayout.HEADER_X, y + BorderLinkLayout.HEADER_Y,
                    this.skin.dim());
        } else {
            text(g, font, BorderLinkTexts.HEADER.with(shown.name(), PORTS, ProgramClient.nameOf(Programs.BORDERLINK),
                    shown.version()), x + BorderLinkLayout.HEADER_X, y + BorderLinkLayout.HEADER_Y, this.skin.dim());
            ports(g, font, x + BorderLinkLayout.LIST_X, y + BorderLinkLayout.LIST_Y, shown, mouseX, mouseY);
            detail(g, font, x, y, shown.ports().get(this.selected - 1));
            footer(g, font, x, y, width, shown);
        }
        for (int i = 0; i < this.buttons.length; i++) {
            this.buttons[i].setBounds(x + BorderLinkLayout.BUTTON_X[i], y + BorderLinkLayout.BUTTONS_Y,
                    BorderLinkLayout.BUTTON_W, BorderLinkLayout.BUTTON_H);
        }
        this.buttons[2].setEnabled(dirty());
        this.root.render(g, new UiContext(this.skin, font, mouseX, mouseY, partialTick));
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (this.root.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        // A click on a row of the list picks that port.
        final double rowsTop = this.top + BorderLinkLayout.LIST_Y + BorderLinkLayout.LIST_HEAD + 1;
        final int row = (int) Math.floor((mouseY - rowsTop) / BorderLinkLayout.ROW);
        final boolean across = mouseX >= this.left + BorderLinkLayout.LIST_X
                && mouseX < this.left + BorderLinkLayout.LIST_X + BorderLinkLayout.LIST_W;
        if (across && mouseY >= rowsTop && row >= 0 && row < PORTS) {
            this.selected = row + 1;
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        this.root.mouseReleased(mouseX, mouseY, button);
    }

    /* The list of ports: a heading row, then each port with its line, far end, mode and state. */
    private void ports(final GuiGraphics g, final Font font, final int x, final int y,
                       final BorderLinkStatePayload shown, final int mouseX, final int mouseY) {
        final int w = BorderLinkLayout.LIST_W;
        this.skin.field(g, x, y, w, BorderLinkLayout.LIST_H, false);
        g.fill(x + 1, y + 1, x + w - 1, y + BorderLinkLayout.LIST_HEAD, this.skin.panelBg());
        g.fill(x + 1, y + BorderLinkLayout.LIST_HEAD, x + w - 1, y + BorderLinkLayout.LIST_HEAD + 1,
                this.skin.edge());
        final TextKey[] headings = {BorderLinkTexts.COLUMN_PORT, BorderLinkTexts.COLUMN_LINE,
                BorderLinkTexts.COLUMN_FAR, BorderLinkTexts.COLUMN_MODE, BorderLinkTexts.COLUMN_STATE};
        for (int c = 0; c < headings.length; c++) {
            text(g, font, headings[c].text(), x + BorderLinkLayout.COLUMNS[c], y + 2, this.skin.dim());
        }
        final Colours colours = PALETTE.get();
        for (int i = 0; i < PORTS; i++) {
            final BorderLinkStatePayload.Port port = shown.ports().get(i);
            final int ry = y + BorderLinkLayout.LIST_HEAD + 1 + i * BorderLinkLayout.ROW;
            final boolean picked = i + 1 == this.selected;
            final boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= ry
                    && mouseY < ry + BorderLinkLayout.ROW;
            this.skin.listRow(g, x + 1, ry, w - 2, BorderLinkLayout.ROW, hovered, picked);
            final int ink = this.skin.listRowText(picked);
            final int ty = ry + 2;
            text(g, font, Text.literal(String.valueOf(i + 1)), x + BorderLinkLayout.COLUMNS[0], ty, ink);
            if (!port.hasCard()) {
                text(g, font, BorderLinkTexts.NO_CARD.text(), x + BorderLinkLayout.COLUMNS[1], ty,
                        picked ? ink : this.skin.dim());
                continue;
            }
            cell(g, font, BorderLinkTexts.lineName(port), x, 1, ty, ink);
            cell(g, font, port.far(), x, 2, ty, ink);
            cell(g, font, BorderLinkTexts.modeName(port), x, 3, ty, ink);
            // On Frames 95 the picked row is navy, which neither green nor red reads on: its state takes the row's ink.
            final int stateInk = picked && this.skin.form() == OsSkin.Form.BEVEL ? ink
                    : port.up() ? colours.up() : colours.down();
            text(g, font, (port.up() ? BorderLinkTexts.UP : BorderLinkTexts.DOWN).text(),
                    x + BorderLinkLayout.COLUMNS[4], ty, stateInk);
        }
    }

    /* The port picked: its far end and line, then its switches, which vanish for a port with no card. */
    private void detail(final GuiGraphics g, final Font font, final int x, final int y,
                        final BorderLinkStatePayload.Port port) {
        final int tx = x + BorderLinkLayout.LIST_X;
        final int ink = this.skin.text();
        if (!port.hasCard()) {
            text(g, font, BorderLinkTexts.PORT_NO_CARD.with(this.selected), tx, y + BorderLinkLayout.DETAIL_Y, ink);
            text(g, font, BorderLinkTexts.PUT_A_CARD.text(), tx, y + BorderLinkLayout.INFO_Y, this.skin.dim());
            return;
        }
        final boolean far = !port.far().isEmpty();
        text(g, font, far ? BorderLinkTexts.PORT_FAR.with(this.selected, port.far())
                : BorderLinkTexts.PORT_NOTHING.with(this.selected), tx, y + BorderLinkLayout.DETAIL_Y, ink);
        text(g, font, BorderLinkTexts.INFO.with(BorderLinkTexts.lineName(port), port.speed(),
                BorderLinkTexts.where(port)), tx, y + BorderLinkLayout.INFO_Y, this.skin.dim());
        text(g, font, BorderLinkTexts.WHAT_MAY.with(far ? Text.literal(port.far())
                : BorderLinkTexts.THE_FAR_SIDE.text()), tx, y + BorderLinkLayout.RIGHTS_Y, ink);
        for (int i = 0; i < this.rights.size(); i++) {
            this.rights.get(i).setBounds(x + BorderLinkLayout.CHECK_X[i % 2], y + BorderLinkLayout.CHECK_Y[i / 2],
                    BorderLinkLayout.CHECK_W, BorderLinkLayout.CHECK_H);
            this.rights.get(i).setVisible(true);
        }
        this.relay.setBounds(x + BorderLinkLayout.CHECK_X[0], y + BorderLinkLayout.SWITCHES_Y,
                BorderLinkLayout.RELAY_W, BorderLinkLayout.CHECK_H);
        this.relay.setVisible(true);
        this.on.setBounds(x + BorderLinkLayout.CHECK_X[1], y + BorderLinkLayout.SWITCHES_Y, BorderLinkLayout.ON_W,
                BorderLinkLayout.CHECK_H);
        this.on.setVisible(true);
    }

    /* How many of the ports with a card have their line up: beside the buttons, or in Frames 7's status bar. */
    private void footer(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                        final BorderLinkStatePayload shown) {
        final Text count = BorderLinkTexts.LINES_UP.with(shown.linesUp(), shown.cards());
        if (this.statusBar) {
            this.skin.statusBar(g, x, y + BorderLinkLayout.H, width, BorderLinkLayout.STATUS_H);
            text(g, font, count, x + 4, y + BorderLinkLayout.H + 2, this.skin.text());
        } else {
            text(g, font, count, x + BorderLinkLayout.FOOTER_X, y + BorderLinkLayout.FOOTER_Y, this.skin.dim());
        }
    }

    /* A cell of the list, cut to its column. */
    private void cell(final GuiGraphics g, final Font font, final String words, final int x, final int column,
                      final int y, final int ink) {
        final int width = BorderLinkLayout.COLUMNS[column + 1] - BorderLinkLayout.COLUMNS[column] - 5;
        final String fits = font.width(words) <= width ? words
                : font.plainSubstrByWidth(words, Math.max(0, width - 6)) + "..";
        Draw.text(g, font, fits, x + BorderLinkLayout.COLUMNS[column], y, ink);
    }

    /* The switches of the port picked: as changed here, or else as the gateway has them. */
    private PortSettings settings() {
        final int changed = this.pending[this.selected - 1];
        if (changed >= 0) {
            return PortSettings.of(changed);
        }
        final BorderLinkStatePayload shown = this.state;
        return shown == null || shown.ports().size() != PORTS ? PortSettings.NEW
                : shown.ports().get(this.selected - 1).settings();
    }

    /* Changes a switch of the port picked; a port changed back to the gateway's own is no longer pending. */
    private void edit(final UnaryOperator<PortSettings> change) {
        final int bits = change.apply(settings()).bits();
        final BorderLinkStatePayload shown = this.state;
        final boolean asGateway = shown != null && shown.ports().size() == PORTS
                && shown.ports().get(this.selected - 1).bits() == bits;
        this.pending[this.selected - 1] = asGateway ? -1 : bits;
    }

    /* Sends every port's changed switches to the gateway, which answers with how its lines stand. */
    private void apply() {
        for (int port = 1; port <= PORTS; port++) {
            if (this.pending[port - 1] >= 0) {
                PacketDistributor.sendToServer(new BorderLinkRequestPayload(this.host, port, this.pending[port - 1]));
                this.pending[port - 1] = -1;
            }
        }
        this.askedAt = Util.getMillis();
    }

    private boolean dirty() {
        for (final int bits : this.pending) {
            if (bits >= 0) {
                return true;
            }
        }
        return false;
    }

    private void ask() {
        this.askedAt = Util.getMillis();
        PacketDistributor.sendToServer(BorderLinkRequestPayload.ask(this.host));
    }

    private void text(final GuiGraphics g, final Font font, final Text words, final int x, final int y,
                      final int colour) {
        Draw.text(g, font, GameText.resolve(words), x, y, colour);
    }

    /** BorderLink's colours, {@code jsc:app/borderlink}: a line that is up, and one that is down. */
    private record Colours(int up, int down) {
    }
}
