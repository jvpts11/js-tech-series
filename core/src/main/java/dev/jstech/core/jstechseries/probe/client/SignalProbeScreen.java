/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.jstechseries.probe.SignalProbeState;
import dev.jstech.core.jstechseries.probe.SignalProbeStatePayload;
import dev.jstech.core.jstechseries.probe.SignalReading;
import dev.jstech.core.jstechseries.probe.SignalView;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.signal.Band;
import dev.jstech.core.signal.RadioReach;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The signal probe held up before the player over the darkened world, as the approved design draws it: the network
 * probe's body, its display with two pages (here: the redstone on the wire it reads and the frequencies heard where the
 * player stands; the radio: the frequencies, one chosen and told in full), the soft keys that turn the pages, the pad
 * that chooses a frequency and points out where it comes from, and its HOLD, SHOW, LIGHT and POWER keys.
 */
@PaletteHolder
public final class SignalProbeScreen extends Screen {

    private final InteractionHand hand;
    private SignalProbeState state;
    private int left;
    private int top;

    /** The colours the signal probe adds to its family's. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "signal_probe", new Colours(
            0xFF << 24 | 0xE61E14, 0xFF << 24 | 0xFF8C78, 0xFF << 24 | 0x321614, 0xFF << 24 | 0xFF5A50,
            0xFF << 24 | 0x28303C, 0xFF << 24 | 0x1E3C46));
    /** The rows of frequencies each page shows, and how far apart. */
    public static final int HERE_ROWS = 5;
    public static final int RADIO_ROWS = 5;
    public static final int ROW_PITCH = 10;
    private static final int FRAME_MILLIS = 125;
    private static final float BIG = 2F;
    private static final int STRENGTH_BARS_X = 62;
    private static final int STRENGTH_BAR_PITCH = 6;
    private static final int STRENGTH_BAR_WIDTH = 4;
    private static final int WHOSE_X = 22;
    private static final int FROM_X = 58;
    private static final int BARS_FROM_RIGHT = 38;
    private static final int DETAIL_PITCH = 8;

    /** The probe in {@code hand}, held up. */
    public SignalProbeScreen(final InteractionHand hand) {
        super(GameText.component(SignalProbe.TITLE));
        this.hand = hand;
        this.state = SignalProbe.state(held());
    }

    @Override
    protected void init() {
        this.left = (this.width - ProbeLayout.WIDTH) / 2;
        this.top = (this.height - ProbeLayout.HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!held().is(SignalProbe.ITEM.get())) {
            onClose();
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final NetworkProbeScreen.Colours c = NetworkProbeScreen.PALETTE.get();
        g.blit(NetworkProbeScreen.PICTURE, this.left, this.top, 0, 0, ProbeLayout.WIDTH, ProbeLayout.HEIGHT,
                ProbeLayout.WIDTH, ProbeLayout.HEIGHT);
        final SignalReading reading = SignalView.shown(this.state.hold());
        small(g, SignalProbe.TITLE.text(), ProbeLayout.TITLE_X, ProbeLayout.TITLE_Y, c.silk());
        final boolean blink = Util.getMillis() / FRAME_MILLIS % 3 != 0;
        g.fill(x(ProbeLayout.LAMP_X), y(ProbeLayout.LAMP_Y), x(ProbeLayout.LAMP_X + ProbeLayout.LAMP_SIZE),
                y(ProbeLayout.LAMP_Y + ProbeLayout.LAMP_SIZE), this.state.on() && reading.linked() && blink
                        ? c.green() : c.lampOff());
        smallRight(g, NetworkProbe.LINK_WORD.text(), ProbeLayout.LINK_RIGHT, ProbeLayout.TITLE_Y, c.silk());
        if (this.state.on()) {
            if (this.state.light()) {
                g.fill(x(ProbeLayout.DISPLAY_LEFT), y(ProbeLayout.DISPLAY_TOP), x(ProbeLayout.DISPLAY_RIGHT),
                        y(ProbeLayout.DISPLAY_BOTTOM), c.lit());
            }
            tabs(g, c);
            switch (this.state.page()) {
                case HERE -> here(g, c, reading);
                case RADIO -> radio(g, c, reading);
            }
        }
        keys(g, c);
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The world stays in view behind the instrument, darkened, as behind the game's book.
        renderTransparentBackground(g);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - this.left;
        final double y = mouseY - this.top;
        final int soft = ProbeLayout.softAt(x, y);
        if (soft >= 0) {
            final List<SignalProbeState.Page> pages = SignalProbeState.Page.order();
            return set(this.state.onPage(pages.get(soft * pages.size() / ProbeLayout.PAGES)));
        }
        if (ProbeLayout.onKey(x, y, ProbeLayout.HOLD_X, ProbeLayout.HOLD_Y)) {
            return set(this.state.toggleHold());
        }
        if (ProbeLayout.onKey(x, y, ProbeLayout.SHOW_X, ProbeLayout.SHOW_Y)) {
            return set(this.state.toggleShow());
        }
        if (ProbeLayout.onKey(x, y, ProbeLayout.LIGHT_X, ProbeLayout.LIGHT_Y)) {
            return set(this.state.toggleLight());
        }
        if (ProbeLayout.onKey(x, y, ProbeLayout.POWER_X, ProbeLayout.POWER_Y)) {
            return set(this.state.togglePower());
        }
        final ProbeLayout.Pad pad = ProbeLayout.padAt(x, y);
        return pad != ProbeLayout.Pad.NONE ? press(pad) : super.mouseClicked(mouseX, mouseY, button);
    }

    /* The arrows and Enter work the pad, as on a keyboard the instrument would have. */
    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        final ProbeLayout.Pad pad = switch (keyCode) {
            case InputConstants.KEY_UP -> ProbeLayout.Pad.UP;
            case InputConstants.KEY_DOWN -> ProbeLayout.Pad.DOWN;
            case InputConstants.KEY_LEFT -> ProbeLayout.Pad.LEFT;
            case InputConstants.KEY_RIGHT -> ProbeLayout.Pad.RIGHT;
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> ProbeLayout.Pad.OK;
            default -> ProbeLayout.Pad.NONE;
        };
        return pad != ProbeLayout.Pad.NONE ? press(pad) : super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** The probe as the player holds it now. */
    public SignalProbeState state() {
        return this.state;
    }

    /* A key of the pad: the arrows turn the pages and choose a frequency; OK points out where the chosen one is. */
    private boolean press(final ProbeLayout.Pad pad) {
        final List<SignalReading.Row> rows = SignalView.shown(this.state.hold()).rows();
        return switch (pad) {
            case LEFT -> set(this.state.paged(-1));
            case RIGHT -> set(this.state.paged(1));
            case UP -> set(this.state.rowMoved(-1, rows.size()));
            case DOWN -> set(this.state.rowMoved(1, rows.size()));
            case OK -> {
                if (this.state.page() == SignalProbeState.Page.RADIO && this.state.row() < rows.size()) {
                    final SignalReading.Row row = rows.get(this.state.row());
                    if (row.dimension().isEmpty() || row.relayed()) {
                        ProbeMarks.show(List.of(row.from()), row.clash() ? PALETTE.get().red()
                                : NetworkProbeScreen.PALETTE.get().cyan());
                        onClose();
                    }
                }
                yield true;
            }
            case NONE -> false;
        };
    }

    private boolean set(final SignalProbeState to) {
        if (!to.equals(this.state)) {
            this.state = to;
            held().set(SignalProbe.STATE.get(), to);
            PacketDistributor.sendToServer(new SignalProbeStatePayload(this.hand == InteractionHand.MAIN_HAND, to));
        }
        return true;
    }

    private ItemStack held() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(this.hand);
    }

    /* The two pages' tabs along the display's foot, the one shown lit. */
    private void tabs(final GuiGraphics g, final NetworkProbeScreen.Colours c) {
        final List<SignalProbeState.Page> pages = SignalProbeState.Page.order();
        final int width = (ProbeLayout.DISPLAY_RIGHT - ProbeLayout.DISPLAY_LEFT) / pages.size();
        final int tabTop = ProbeLayout.DISPLAY_BOTTOM - ProbeLayout.TABS_HEIGHT;
        for (int k = 0; k < pages.size(); k++) {
            final boolean on = pages.get(k) == this.state.page();
            final int tx = ProbeLayout.DISPLAY_LEFT + k * width;
            g.fill(x(tx), y(tabTop), x(tx + width), y(ProbeLayout.DISPLAY_BOTTOM), on ? c.cyan() : c.tabOff());
            if (k > 0) {
                g.fill(x(tx), y(tabTop), x(tx + 1), y(ProbeLayout.DISPLAY_BOTTOM), c.tft());
            }
            final Text word = SignalProbe.page(pages.get(k)).text();
            small(g, word, tx + (width - width(word)) / 2, tabTop + 2, on ? c.tabInk() : c.dim());
        }
    }

    /* The header: a word in the game's font on the left, a small figure on the right. */
    private void header(final GuiGraphics g, final NetworkProbeScreen.Colours c, final Text title, final Text right) {
        g.fill(x(ProbeLayout.DISPLAY_LEFT), y(ProbeLayout.DISPLAY_TOP), x(ProbeLayout.DISPLAY_RIGHT),
                y(ProbeLayout.DISPLAY_TOP + ProbeLayout.HEADER_HEIGHT), c.head());
        Draw.text(g, this.font, GameText.resolve(title), x(ProbeLayout.DISPLAY_LEFT + 3),
                y(ProbeLayout.DISPLAY_TOP + 2), c.white());
        smallRight(g, right, ProbeLayout.DISPLAY_RIGHT - 3, ProbeLayout.DISPLAY_TOP + 3, c.headText());
    }

    /* Here, as the approved design lays it out: the redstone on the wire, its peak, and the frequencies heard here. */
    private void here(final GuiGraphics g, final NetworkProbeScreen.Colours c, final SignalReading reading) {
        final Colours s = PALETTE.get();
        final BlockPos wire = BlockPos.of(reading.wire());
        header(g, c, SignalProbe.REDSTONE.text(), reading.linked() ? place(wire) : Text.literal(""));
        final int x = ProbeLayout.TEXT_LEFT;
        final int y0 = ProbeLayout.DISPLAY_TOP;
        small(g, SignalProbe.ON_THE_WIRE.text(), x, y0 + 15, c.cyan());
        if (reading.linked()) {
            final String strength = String.valueOf(reading.strength());
            Draw.textScaled(g, this.font, strength, x(x), y(y0 + 23), c.white(), BIG);
            final int bigWidth = (int) Math.ceil(this.font.width(strength) * BIG);
            small(g, SignalProbe.OF_FULL.with(Text.literal(String.valueOf(Reading.FULL))), x + bigWidth + 3,
                    y0 + 31, c.dim());
            for (int k = 0; k < Reading.FULL; k++) {
                final int sx = x + STRENGTH_BARS_X + k * STRENGTH_BAR_PITCH;
                final boolean lit = k < reading.strength();
                g.fill(x(sx), y(y0 + 25), x(sx + STRENGTH_BAR_WIDTH), y(y0 + 37), lit ? s.redstone()
                        : s.redstoneOff());
                if (lit) {
                    g.fill(x(sx), y(y0 + 25), x(sx + STRENGTH_BAR_WIDTH), y(y0 + 26), s.redstoneTop());
                }
            }
            small(g, SignalProbe.PEAK.with(Text.literal(String.valueOf(reading.peak()))), x, y0 + 42, c.dim());
        } else {
            final boolean linked = held().get(SignalProbe.LINK.get()) != null;
            small(g, (linked ? SignalProbe.LOST_WIRE : SignalProbe.NOT_LINKED).text(), x, y0 + 27, c.dim());
        }
        small(g, SignalProbe.HEARD_HERE.text(), x, y0 + 53, c.cyan());
        if (reading.rows().isEmpty()) {
            small(g, SignalProbe.NOTHING_HEARD.text(), x, y0 + 63, c.dim());
            return;
        }
        columns(g, c, y0 + 61);
        rows(g, c, reading.rows(), firstShown(reading.rows().size(), HERE_ROWS), HERE_ROWS, y0 + 70);
    }

    /* The radio: the chosen frequency told in full, and the list to choose from. */
    private void radio(final GuiGraphics g, final NetworkProbeScreen.Colours c, final SignalReading reading) {
        final Colours s = PALETTE.get();
        header(g, c, SignalProbe.RADIO.text(), SignalProbe.HEARD_COUNT.with(Text.literal(String.valueOf(
                reading.heard().size()))));
        final int x = ProbeLayout.TEXT_LEFT;
        int y = ProbeLayout.PAGE_TOP;
        if (reading.rows().isEmpty()) {
            small(g, SignalProbe.NOTHING_HEARD.text(), x, y, c.dim());
            return;
        }
        final int chosen = Math.min(this.state.row(), reading.rows().size() - 1);
        final SignalReading.Row row = reading.rows().get(chosen);
        fitted(g, SignalProbe.FREQUENCY.with(Text.literal(number(row) + "  " + bandWord(row.band()))), y,
                c.cyan());
        y += DETAIL_PITCH;
        fitted(g, SignalProbe.OWNER.with(row.owner().isEmpty() ? SignalProbe.NOBODY.text()
                : Text.literal(row.owner())), y, c.text());
        y += DETAIL_PITCH;
        fitted(g, SignalProbe.SENT_FROM.with(fromText(row), place(BlockPos.of(row.from()))), y, c.text());
        y += DETAIL_PITCH;
        if (row.clash()) {
            fitted(g, SignalProbe.CLASHES.text(), y, s.red());
        } else if (!row.heard()) {
            fitted(g, SignalProbe.NOT_HEARD.text(), y, c.amber());
        } else {
            final Text arrives = SignalProbe.RECEPTION.with(Text.literal(String.valueOf(row.bars())),
                    Text.literal(String.valueOf(RadioReach.MOST_BARS)));
            small(g, arrives, x, y, c.text());
            small(g, SignalProbe.CARRIES.with(Text.literal(String.valueOf(row.value()))), x + width(arrives) + 6, y,
                    c.white());
        }
        y += DETAIL_PITCH;
        fitted(g, NetworkProbe.OK_SHOWS.text(), y, c.dim());
        columns(g, c, y + 10);
        rows(g, c, reading.rows(), firstShown(reading.rows().size(), RADIO_ROWS), RADIO_ROWS, y + 19);
    }

    /* The heads of the columns of frequencies. */
    private void columns(final GuiGraphics g, final NetworkProbeScreen.Colours c, final int y) {
        final int x = ProbeLayout.TEXT_LEFT;
        small(g, SignalProbe.FREQ.text(), x, y, c.dim());
        small(g, SignalProbe.WHOSE.text(), x + WHOSE_X, y, c.dim());
        small(g, SignalProbe.FROM.text(), x + FROM_X, y, c.dim());
        smallRight(g, SignalProbe.SENDS.text(), ProbeLayout.TEXT_RIGHT, y, c.dim());
    }

    /*
     * Up to {@code shown} frequencies from the {@code first}, the chosen one lit: its number, its band, where it is
     * from, its bars and what it sends; a clash in red, its transmitters joined by a red line down the left; one of the
     * team not heard here, dim, "lost".
     */
    private void rows(final GuiGraphics g, final NetworkProbeScreen.Colours c, final List<SignalReading.Row> rows,
                      final int first, final int shown, final int top) {
        final Colours s = PALETTE.get();
        final int x = ProbeLayout.TEXT_LEFT;
        final int chosen = Math.min(this.state.row(), rows.size() - 1);
        for (int k = first; k < Math.min(rows.size(), first + shown); k++) {
            final SignalReading.Row row = rows.get(k);
            final int y = top + (k - first) * ROW_PITCH;
            if (k == chosen) {
                g.fill(x(ProbeLayout.DISPLAY_LEFT + 1), y(y - 2), x(ProbeLayout.DISPLAY_RIGHT - 1), y(y + 8),
                        s.chosen());
            }
            final boolean lost = !row.heard();
            small(g, Text.literal(number(row)), x, y, lost ? c.dim() : c.white());
            small(g, Text.literal(bandWord(row.band())), x + WHOSE_X, y, lost ? c.dim() : c.headText());
            final int fromRoom = (int) ((ProbeLayout.DISPLAY_RIGHT - BARS_FROM_RIGHT - (x + FROM_X) - 4)
                    / ProbeLayout.SMALL);
            final String from = this.font.plainSubstrByWidth(GameText.resolve(fromText(row)), fromRoom);
            small(g, Text.literal(from), x + FROM_X, y, lost ? c.dim() : c.text());
            bars(g, c, s, ProbeLayout.DISPLAY_RIGHT - BARS_FROM_RIGHT, y - 1, row.bars(), row.heard());
            final Text sends = row.clash() ? SignalProbe.CLASH.text() : lost ? SignalProbe.LOST.text()
                    : Text.literal(String.valueOf(row.value()));
            smallRight(g, sends, ProbeLayout.TEXT_RIGHT, y, row.clash() ? s.red() : lost ? c.amber() : c.white());
            if (row.clash() && (k == 0 || !sameFrequency(rows.get(k - 1), row))) {
                int end = k;
                while (end + 1 < rows.size() && sameFrequency(rows.get(end + 1), row)) {
                    end++;
                }
                final int last = Math.min(end, first + shown - 1);
                g.fill(x(ProbeLayout.DISPLAY_LEFT + 1), y(y - 1), x(ProbeLayout.DISPLAY_LEFT + 2),
                        y(top + (last - first) * ROW_PITCH + 8), s.red());
            }
        }
    }

    /* Reception in four bars, as a radio shows it. */
    private void bars(final GuiGraphics g, final NetworkProbeScreen.Colours c, final Colours s, final int bx,
                      final int by, final int lit, final boolean heard) {
        for (int k = 0; k < RadioReach.MOST_BARS; k++) {
            final int h = 2 + k * 2;
            g.fill(x(bx + k * 3), y(by + 8 - h), x(bx + k * 3 + 2), y(by + 8), heard && k < lit ? c.green()
                    : s.barOff());
        }
    }

    /* The first row shown of {@code count} when {@code shown} fit, the chosen one in view. */
    private int firstShown(final int count, final int shown) {
        final int chosen = Math.min(this.state.row(), count - 1);
        return Math.max(0, Math.min(count - shown, chosen - shown + 1));
    }

    /*
     * Where a frequency is heard from: how far, or from a relay how far; from another dimension, its name, through a
     * relay when it is heard.
     */
    private static Text fromText(final SignalReading.Row row) {
        final Text where = row.dimension().isEmpty()
                ? SignalProbe.METRES.with(Text.literal(String.valueOf(Math.round(row.distance()))))
                : Text.literal(dimensionName(row.dimension()));
        return row.relayed() ? SignalProbe.RELAYED.with(where) : where;
    }

    /* A dimension as a player reads it: its name's words, each with a capital. */
    private static String dimensionName(final String dimension) {
        final ResourceLocation id = ResourceLocation.tryParse(dimension);
        final String path = id == null ? dimension : id.getPath();
        final StringBuilder name = new StringBuilder();
        for (final String word : path.split("[_/]")) {
            if (!word.isEmpty()) {
                if (!name.isEmpty()) {
                    name.append(' ');
                }
                name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
        }
        return name.toString();
    }

    private static boolean sameFrequency(final SignalReading.Row a, final SignalReading.Row b) {
        return a.band() == b.band() && a.number() == b.number() && a.clash() && b.clash();
    }

    private static String number(final SignalReading.Row row) {
        return row.band() == Band.PAIR ? "-" : String.valueOf(row.number());
    }

    private static String bandWord(final Band band) {
        return GameText.resolve(band.text()).toUpperCase(Locale.ROOT);
    }

    private static Text place(final BlockPos at) {
        return Text.literal(at.getX() + ", " + at.getY() + ", " + at.getZ());
    }

    /* The four keys' words, each key sunk a pixel while it is on, and OK on the pad. */
    private void keys(final GuiGraphics g, final NetworkProbeScreen.Colours c) {
        key(g, c, NetworkProbe.HOLD, ProbeLayout.HOLD_X, ProbeLayout.HOLD_Y, this.state.hold(), c.amberInk());
        key(g, c, NetworkProbe.SHOW, ProbeLayout.SHOW_X, ProbeLayout.SHOW_Y, this.state.show(), c.tealInk());
        key(g, c, NetworkProbe.LIGHT, ProbeLayout.LIGHT_X, ProbeLayout.LIGHT_Y, this.state.light(), c.silk());
        key(g, c, NetworkProbe.POWER, ProbeLayout.POWER_X, ProbeLayout.POWER_Y, this.state.on(), c.powerInk());
        final Text ok = NetworkProbe.OK.text();
        small(g, ok, ProbeLayout.PAD_X - width(ok) / 2, ProbeLayout.PAD_Y - 3, c.silk());
    }

    private void key(final GuiGraphics g, final NetworkProbeScreen.Colours c, final TextKey label, final int keyX,
                     final int keyY, final boolean on, final int ink) {
        if (on) {
            g.fill(x(keyX), y(keyY), x(keyX + ProbeLayout.KEY_WIDTH), y(keyY + ProbeLayout.KEY_HEIGHT), c.pressed());
        }
        final Text word = label.text();
        small(g, word, keyX + (ProbeLayout.KEY_WIDTH - width(word)) / 2, keyY + (on ? 4 : 3), ink);
    }

    private void small(final GuiGraphics g, final Text text, final int sx, final int sy, final int colour) {
        Draw.textScaled(g, this.font, GameText.resolve(text), x(sx), y(sy), colour, ProbeLayout.SMALL);
    }

    /* A line of the display from its left, in small text, cut where the display ends. */
    private void fitted(final GuiGraphics g, final Text text, final int sy, final int colour) {
        final int room = (int) ((ProbeLayout.TEXT_RIGHT - ProbeLayout.TEXT_LEFT) / ProbeLayout.SMALL);
        small(g, Text.literal(this.font.plainSubstrByWidth(GameText.resolve(text), room)), ProbeLayout.TEXT_LEFT, sy,
                colour);
    }

    private void smallRight(final GuiGraphics g, final Text text, final int right, final int sy, final int colour) {
        small(g, text, right - width(text), sy, colour);
    }

    private int width(final Text text) {
        return (int) Math.ceil(this.font.width(GameText.resolve(text)) * ProbeLayout.SMALL);
    }

    private int x(final int x) {
        return this.left + x;
    }

    private int y(final int y) {
        return this.top + y;
    }

    /**
     * The colours the signal probe adds to the network probe's.
     *
     * @param redstone    a lit bar of the wire's strength
     * @param redstoneTop the top edge of a lit bar
     * @param redstoneOff an unlit bar
     * @param red         a clash
     * @param barOff      an unlit bar of reception
     * @param chosen      the chosen frequency's row
     */
    public record Colours(int redstone, int redstoneTop, int redstoneOff, int red, int barOff, int chosen) {
    }
}
