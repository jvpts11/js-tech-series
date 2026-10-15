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
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.grid.LineSurvey;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.probe.ProbeReading;
import dev.jstech.core.jstechseries.probe.ProbeState;
import dev.jstech.core.jstechseries.probe.ProbeStatePayload;
import dev.jstech.core.jstechseries.probe.ProbeView;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The network probe held up before the player over the darkened world, as the approved design draws it: its orange
 * holster, its display with the line's name and its four pages (the summary, the members, the flow of the last minute
 * and the losses), the soft keys that turn the pages, the pad that chooses a row and shows it in the world, and its
 * HOLD, SHOW, LIGHT and POWER keys. No inventory.
 */
@PaletteHolder
public final class NetworkProbeScreen extends Screen {

    private final InteractionHand hand;
    private ProbeState state;
    private int left;
    private int top;

    /** The instrument's picture, drawn as the approved design draws it, with no words. */
    public static final ResourceLocation PICTURE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "textures/gui/network_probe.png");
    /** The instrument's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "network_probe", new Colours(
            0xFF << 24 | 0xD6DAE0, 0xFF << 24 | 0xE0E4E8, 0xFF << 24 | 0x828A96, 0xFF << 24 | 0xFFBE3C,
            0xFF << 24 | 0x5FE07A, 0xFF << 24 | 0x007680, 0xFF << 24 | 0xC8F0F0, 0xFF << 24 | 0xFFFFFF,
            0xFF << 24 | 0x321E00, 0xFF << 24 | 0x001E22, 0xFF << 24 | 0x1E4628, 0xFF << 24 | 0x16202C,
            0x60 << 24, 0xFF << 24 | 0x55FFFF, 0xFF << 24 | 0x1E242E, 0xFF << 24 | 0x00282C,
            0x78 << 24 | 0xFF5A50, 0xFF << 24 | 0xFFDCD6, 0xFF << 24 | 0x1E3C46, 0xFF << 24 | 0x0C1018));
    private static final int FRAME_MILLIS = 125;
    private static final int SPARK_WIDTH = 92;
    private static final int SPARK_HEIGHT = 22;
    private static final int GRAPH_HEIGHT = 56;
    private static final int GLYPH = 7;
    private static final int COUNT_PITCH = 54;
    private static final float DIM_MIX = 0.45F;
    /* Below a quarter of its ceiling, a line's flow is drawn against its own peak. */
    private static final int PEAK_SHARE = 4;
    private static final String[] SOURCE = {" XXXXX ", "X     X", "X XX  X", "X X X X", "X  XX X", "X     X", " XXXXX "};
    private static final String[] LOAD = {" XXXXX ", "X     X", "X X X X", "X XXX X", "X X X X", "X     X", " XXXXX "};
    private static final String[] STORE = {"  XXX  ", "XXXXXXX", "X     X", "X XXX X", "X     X", "X XXX X", "XXXXXXX"};
    private static final String[] IDLE = {"       ", " XXXXX ", " X   X ", " X   X ", " X   X ", " XXXXX ", "       "};

    /** The probe in {@code hand}, held up. */
    public NetworkProbeScreen(final InteractionHand hand) {
        super(GameText.component(NetworkProbe.TITLE));
        this.hand = hand;
        this.state = NetworkProbe.state(held());
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
        if (!held().is(NetworkProbe.ITEM.get())) {
            onClose();
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final Colours c = PALETTE.get();
        g.blit(PICTURE, this.left, this.top, 0, 0, ProbeLayout.WIDTH, ProbeLayout.HEIGHT, ProbeLayout.WIDTH,
                ProbeLayout.HEIGHT);
        final ProbeReading reading = ProbeView.shown(this.state.hold());
        final boolean linked = held().get(NetworkProbe.LINK.get()) != null;
        small(g, NetworkProbe.TITLE.text(), ProbeLayout.TITLE_X, ProbeLayout.TITLE_Y, c.silk());
        final boolean blink = Util.getMillis() / FRAME_MILLIS % 3 != 0;
        g.fill(x(ProbeLayout.LAMP_X), y(ProbeLayout.LAMP_Y), x(ProbeLayout.LAMP_X + ProbeLayout.LAMP_SIZE),
                y(ProbeLayout.LAMP_Y + ProbeLayout.LAMP_SIZE), this.state.on() && linked && reading.present()
                        && blink ? c.green() : c.lampOff());
        smallRight(g, NetworkProbe.LINK_WORD.text(), ProbeLayout.LINK_RIGHT, ProbeLayout.TITLE_Y, c.silk());
        display(g, c, reading, linked);
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
            return set(this.state.onPage(ProbeState.Page.order().get(soft)));
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
    public ProbeState state() {
        return this.state;
    }

    /* A key of the pad: the arrows turn the pages and choose a row; OK shows the chosen row's blocks in the world. */
    private boolean press(final ProbeLayout.Pad pad) {
        final List<Line> rows = rows(ProbeView.shown(this.state.hold())).stream().filter(Line::row).toList();
        return switch (pad) {
            case LEFT -> set(this.state.paged(-1));
            case RIGHT -> set(this.state.paged(1));
            case UP -> set(this.state.rowMoved(-1, rows.size()));
            case DOWN -> set(this.state.rowMoved(1, rows.size()));
            case OK -> {
                if (this.state.page() == ProbeState.Page.MEMBERS && this.state.row() < rows.size()) {
                    final ProbeReading.Group group = rows.get(this.state.row()).group();
                    ProbeMarks.show(group.places(), roleColour(PALETTE.get(), group.role()));
                    onClose();
                }
                yield true;
            }
            case NONE -> false;
        };
    }

    private boolean set(final ProbeState to) {
        if (!to.equals(this.state)) {
            this.state = to;
            held().set(NetworkProbe.STATE.get(), to);
            PacketDistributor.sendToServer(new ProbeStatePayload(this.hand == InteractionHand.MAIN_HAND, to));
        }
        return true;
    }

    private ItemStack held() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(this.hand);
    }

    /* The display: dark when off; a word when there is nothing to read; the header, the page and the tabs otherwise. */
    private void display(final GuiGraphics g, final Colours c, final ProbeReading reading, final boolean linked) {
        if (!this.state.on()) {
            return;
        }
        if (this.state.light()) {
            g.fill(x(ProbeLayout.DISPLAY_LEFT), y(ProbeLayout.DISPLAY_TOP), x(ProbeLayout.DISPLAY_RIGHT),
                    y(ProbeLayout.DISPLAY_BOTTOM), c.lit());
        }
        tabs(g, c);
        if (!linked || !reading.present() || reading.unit() == ProbeReading.Unit.NONE) {
            final TextKey why = !linked ? NetworkProbe.NOT_LINKED : !reading.present() ? NetworkProbe.LOST
                    : NetworkProbe.CARRIES_NOTHING;
            wrapped(g, why.text(), ProbeLayout.PAGE_TOP, c.dim());
            return;
        }
        header(g, c, reading);
        switch (this.state.page()) {
            case SUMMARY -> summary(g, c, reading);
            case MEMBERS -> members(g, c, reading);
            case FLOW -> flow(g, c, reading);
            case LOSSES -> losses(g, c, reading);
        }
    }

    /* The header: the line's cable, and on the right its class and how many cables the part has. */
    private void header(final GuiGraphics g, final Colours c, final ProbeReading reading) {
        g.fill(x(ProbeLayout.DISPLAY_LEFT), y(ProbeLayout.DISPLAY_TOP), x(ProbeLayout.DISPLAY_RIGHT),
                y(ProbeLayout.DISPLAY_TOP + ProbeLayout.HEADER_HEIGHT), c.head());
        final String rated = rated(reading);
        final TextKey counted = reading.unit() == ProbeReading.Unit.MECHANICAL ? NetworkProbe.PARTS
                : NetworkProbe.CABLES;
        final Text right = Text.literal((rated.isEmpty() ? "" : rated + "  ") + GameText.resolve(
                counted.with(Text.literal(String.valueOf(reading.cables())))));
        final int rightWidth = width(right);
        smallRight(g, right, ProbeLayout.DISPLAY_RIGHT - 3, ProbeLayout.DISPLAY_TOP + 3, c.headText());
        // A long name is drawn smaller, down to the small text, and only then cut where the right side begins.
        final int room = ProbeLayout.DISPLAY_RIGHT - 3 - rightWidth - 4 - (ProbeLayout.DISPLAY_LEFT + 3);
        final String full = cableName(reading.cable().toString()).toUpperCase(Locale.ROOT);
        final float scale = Math.max(ProbeLayout.SMALL, Math.min(1F, room / (float) Math.max(1, this.font.width(
                full))));
        final String name = this.font.plainSubstrByWidth(full, (int) (room / scale));
        final int nameY = ProbeLayout.DISPLAY_TOP + (scale >= 1F ? 2 : 3);
        Draw.textScaled(g, this.font, name, x(ProbeLayout.DISPLAY_LEFT + 3), y(nameY), c.white(), scale);
    }

    /* The tabs of the four pages along the display's foot, the one shown lit. */
    private void tabs(final GuiGraphics g, final Colours c) {
        final List<ProbeState.Page> pages = ProbeState.Page.order();
        final int width = (ProbeLayout.DISPLAY_RIGHT - ProbeLayout.DISPLAY_LEFT) / pages.size();
        final int top = ProbeLayout.DISPLAY_BOTTOM - ProbeLayout.TABS_HEIGHT;
        for (int k = 0; k < pages.size(); k++) {
            final boolean on = pages.get(k) == this.state.page();
            final int tx = ProbeLayout.DISPLAY_LEFT + k * width;
            g.fill(x(tx), y(top), x(tx + width), y(ProbeLayout.DISPLAY_BOTTOM), on ? c.cyan() : c.tabOff());
            if (k > 0) {
                g.fill(x(tx), y(top), x(tx + 1), y(ProbeLayout.DISPLAY_BOTTOM), c.tft());
            }
            final Text word = NetworkProbe.page(pages.get(k)).text();
            small(g, word, tx + (width - width(word)) / 2, top + 2, on ? c.tabInk() : c.dim());
        }
    }

    /* The summary, as the approved design lays it out: the flow, the narrowest cable, the losses and the members. */
    private void summary(final GuiGraphics g, final Colours c, final ProbeReading reading) {
        final int x = ProbeLayout.TEXT_LEFT;
        final int y0 = ProbeLayout.DISPLAY_TOP;
        small(g, NetworkProbe.FLOW.text(), x, y0 + 15, c.cyan());
        sparkline(g, c, reading, x, y0 + 23, SPARK_WIDTH, SPARK_HEIGHT);
        Draw.text(g, this.font, GameText.resolve(rate(reading, reading.moved())), x(ProbeLayout.DISPLAY_LEFT + 104),
                y(y0 + 22), c.white());
        small(g, NetworkProbe.LAST_MINUTE.text(), ProbeLayout.DISPLAY_LEFT + 104, y0 + 33, c.dim());
        small(g, NetworkProbe.NARROWEST.text(), x, y0 + 51, c.cyan());
        if (reading.narrowest().isPresent()) {
            final LineSurvey.Narrowest narrowest = reading.narrowest().get();
            small(g, NetworkProbe.NARROWEST_AT.with(Text.literal(cableName(narrowest.cable())), place(
                    narrowest.pos())), x, y0 + 58, c.text());
            final Text of = NetworkProbe.OF.with(rate(reading, reading.moved()), rate(reading, narrowest.ceiling()));
            small(g, of, x, y0 + 65, c.text());
            if (reading.full()) {
                final boolean blink = Util.getMillis() / FRAME_MILLIS % 2 == 0;
                small(g, NetworkProbe.FULL.text(), x + width(of) + 4, y0 + 65, blink ? c.amber()
                        : mix(c.amber(), c.tft(), 0.4F));
            }
        } else {
            small(g, NetworkProbe.NO_LIMIT.text(), x, y0 + 58, c.text());
        }
        if (reading.wanted() > reading.moved() && reading.wanted() > 0) {
            final long percent = Math.round(100.0 * (reading.moved() - reading.lost()) / reading.wanted());
            small(g, NetworkProbe.WANT.with(rate(reading, reading.wanted()), Text.literal(percent + "%")), x,
                    y0 + 72, c.dim());
        }
        small(g, NetworkProbe.LOSSES.text(), x, y0 + 81, c.cyan());
        small(g, reading.unit().loses() ? NetworkProbe.LOSS_LINE.with(rate(reading,
                reading.lost()), share(reading.lost(), reading.moved())) : NetworkProbe.NO_LOSS.text(), x, y0 + 88,
                c.text());
        small(g, NetworkProbe.MEMBERS.text(), x, y0 + 97, c.cyan());
        final LineSurvey.Role[] roles = {LineSurvey.Role.SOURCE, LineSurvey.Role.CONSUMER, LineSurvey.Role.STORE};
        final TextKey[] words = {NetworkProbe.COUNT_SOURCE, NetworkProbe.COUNT_CONSUMER, NetworkProbe.COUNT_STORE};
        for (int k = 0; k < roles.length; k++) {
            final int gx = x + k * COUNT_PITCH;
            glyph(g, gx, y0 + 105, glyphOf(roles[k]), roleColour(c, roles[k]));
            Draw.text(g, this.font, String.valueOf(reading.count(roles[k])), x(gx + 10), y(y0 + 105), c.white());
            small(g, words[k].text(), gx, y0 + 114, c.dim());
        }
    }

    /* Every block the line plugs into, by what it is to the line and by kind, the chosen row lit. */
    private void members(final GuiGraphics g, final Colours c, final ProbeReading reading) {
        final List<Line> lines = rows(reading);
        if (lines.isEmpty()) {
            small(g, NetworkProbe.NO_MEMBERS.text(), ProbeLayout.TEXT_LEFT, ProbeLayout.PAGE_TOP, c.dim());
            return;
        }
        final int chosen = Math.min(this.state.row(), (int) lines.stream().filter(Line::row).count() - 1);
        int rowIndex = -1;
        int chosenLine = 0;
        for (int k = 0; k < lines.size(); k++) {
            if (lines.get(k).row() && ++rowIndex == chosen) {
                chosenLine = k;
            }
        }
        final int room = ProbeLayout.PAGE_BOTTOM - ProbeLayout.GROUP_PITCH - ProbeLayout.PAGE_TOP;
        int first = 0;
        while (first < chosenLine && height(lines, first, chosenLine + 1) > room) {
            first++;
        }
        int y = ProbeLayout.PAGE_TOP;
        rowIndex = -1;
        for (int k = 0; k < first; k++) {
            if (lines.get(k).row()) {
                rowIndex++;
            }
        }
        for (int k = first; k < lines.size(); k++) {
            final Line line = lines.get(k);
            final int pitch = line.row() ? ProbeLayout.ROW_PITCH : ProbeLayout.GROUP_PITCH;
            if (y + pitch > ProbeLayout.PAGE_TOP + room) {
                break;
            }
            if (!line.row()) {
                small(g, NetworkProbe.group(line.role()).text(), ProbeLayout.TEXT_LEFT, y, c.cyan());
                smallRight(g, groupSummary(reading, line.role()), ProbeLayout.TEXT_RIGHT, y, c.dim());
            } else {
                rowIndex++;
                if (rowIndex == chosen) {
                    g.fill(x(ProbeLayout.DISPLAY_LEFT + 1), y(y - 2), x(ProbeLayout.DISPLAY_RIGHT - 1), y(y + 9),
                            c.select());
                }
                final ProbeReading.Group group = line.group();
                glyph(g, ProbeLayout.TEXT_LEFT, y, glyphOf(group.role()), roleColour(c, group.role()));
                final String name = this.font.plainSubstrByWidth(blockName(group.block()),
                        (int) (80 / ProbeLayout.SMALL));
                small(g, Text.literal(name), ProbeLayout.TEXT_LEFT + 11, y + 1, c.text());
                small(g, NetworkProbe.TIMES.with(Text.literal(String.valueOf(group.count()))),
                        ProbeLayout.TEXT_LEFT + 92, y + 1, c.dim());
                smallRight(g, rate(reading, Math.abs(group.perTick())), ProbeLayout.TEXT_RIGHT, y + 1, c.white());
            }
            y += pitch;
        }
        small(g, NetworkProbe.OK_SHOWS.text(), ProbeLayout.TEXT_LEFT, ProbeLayout.PAGE_BOTTOM - 6, c.dim());
    }

    /* The flow of each second of the last minute, against the narrowest cable's ceiling, and its figures. */
    private void flow(final GuiGraphics g, final Colours c, final ProbeReading reading) {
        final int x = ProbeLayout.TEXT_LEFT;
        final int width = ProbeLayout.TEXT_RIGHT - x;
        small(g, NetworkProbe.FLOW_MINUTE.text(), x, ProbeLayout.PAGE_TOP, c.cyan());
        final int graphTop = ProbeLayout.PAGE_TOP + 10;
        sparkline(g, c, reading, x, graphTop, width, GRAPH_HEIGHT);
        small(g, NetworkProbe.MINUTE_AGO.text(), x, graphTop + GRAPH_HEIGHT + 2, c.dim());
        smallRight(g, NetworkProbe.NOW_WORD.text(), ProbeLayout.TEXT_RIGHT, graphTop + GRAPH_HEIGHT + 2, c.dim());
        long most = 0L;
        long least = Long.MAX_VALUE;
        long sum = 0L;
        for (final long second : reading.history()) {
            most = Math.max(most, second);
            least = Math.min(least, second);
            sum += second;
        }
        final long mean = reading.history().isEmpty() ? 0L : sum / reading.history().size();
        final int figures = graphTop + GRAPH_HEIGHT + 12;
        small(g, NetworkProbe.NOW.with(rate(reading, reading.moved())), x, figures, c.white());
        small(g, NetworkProbe.MEAN.with(rate(reading, mean)), x + width / 2, figures, c.text());
        small(g, NetworkProbe.MOST.with(rate(reading, most)), x, figures + 9, c.dim());
        small(g, NetworkProbe.LEAST.with(rate(reading, least == Long.MAX_VALUE ? 0L : least)), x + width / 2,
                figures + 9, c.dim());
    }

    /*
     * What the cables lost, or a network of shafts to friction, against what the sources sent; a line of pipes or
     * tubes loses nothing.
     */
    private void losses(final GuiGraphics g, final Colours c, final ProbeReading reading) {
        final int x = ProbeLayout.TEXT_LEFT;
        small(g, NetworkProbe.LOSSES.text(), x, ProbeLayout.PAGE_TOP, c.cyan());
        if (!reading.unit().loses()) {
            wrapped(g, NetworkProbe.LOSES_NOTHING.text(), ProbeLayout.PAGE_TOP + 10, c.text());
            return;
        }
        int y = ProbeLayout.PAGE_TOP + 10;
        small(g, NetworkProbe.LOST_ON_THE_WAY.with(rate(reading, reading.lost())), x, y, c.white());
        y += 10;
        small(g, NetworkProbe.SHARE.with(share(reading.lost(), reading.moved())), x, y, c.text());
        y += 10;
        small(g, NetworkProbe.DELIVERED.with(rate(reading, reading.moved()), rate(reading, Math.max(0L,
                reading.moved() - reading.lost()))), x, y, c.text());
        y += 10;
        if (reading.unit() == ProbeReading.Unit.MECHANICAL) {
            wrapped(g, NetworkProbe.FRICTION.text(), y, c.dim());
        } else {
            small(g, NetworkProbe.ALONG.with(Text.literal(String.valueOf(reading.cables()))), x, y, c.dim());
        }
    }

    /*
     * Each second's flow as a bar, against the narrowest cable's ceiling drawn across the top, amber where the line ran
     * full; against the most it carried, a quarter over, when no cable limits it or its ceiling is far above the flow,
     * so a line that carries little still shows how it goes.
     */
    private void sparkline(final GuiGraphics g, final Colours c, final ProbeReading reading, final int sx,
                           final int sy, final int width, final int height) {
        final List<Long> history = reading.history();
        final long ceilingAmount = reading.narrowest().map(LineSurvey.Narrowest::ceiling).orElse(0L);
        long peak = 0L;
        for (final long second : history) {
            peak = Math.max(peak, second);
        }
        final boolean ceiling = ceilingAmount > 0 && peak >= ceilingAmount / PEAK_SHARE;
        final long scale = ceiling ? ceilingAmount : peak + peak / PEAK_SHARE;
        g.fill(x(sx), y(sy), x(sx + width), y(sy + 1), ceiling ? c.ceiling() : c.tabOff());
        if (history.isEmpty() || scale <= 0) {
            return;
        }
        for (int i = 0; i < width; i++) {
            final long second = history.get(Math.min(history.size() - 1, i * history.size() / width));
            final double share = Math.min(1.0, second / (double) scale);
            final int barTop = sy + (int) Math.round((1 - share) * height);
            final int colour = ceiling && second >= scale ? c.amber() : c.green();
            g.fill(x(sx + i), y(barTop), x(sx + i + 1), y(sy + height), mix(colour, c.tft(), DIM_MIX));
            g.fill(x(sx + i), y(barTop), x(sx + i + 1), y(barTop + 1), colour);
        }
    }

    /* The four keys' words, each key sunk a pixel while it is on, and OK on the pad. */
    private void keys(final GuiGraphics g, final Colours c) {
        key(g, c, NetworkProbe.HOLD, ProbeLayout.HOLD_X, ProbeLayout.HOLD_Y, this.state.hold(), c.amberInk());
        key(g, c, NetworkProbe.SHOW, ProbeLayout.SHOW_X, ProbeLayout.SHOW_Y, this.state.show(), c.tealInk());
        key(g, c, NetworkProbe.LIGHT, ProbeLayout.LIGHT_X, ProbeLayout.LIGHT_Y, this.state.light(), c.silk());
        key(g, c, NetworkProbe.POWER, ProbeLayout.POWER_X, ProbeLayout.POWER_Y, this.state.on(), c.powerInk());
        final Text ok = NetworkProbe.OK.text();
        small(g, ok, ProbeLayout.PAD_X - width(ok) / 2, ProbeLayout.PAD_Y - 3, c.silk());
    }

    private void key(final GuiGraphics g, final Colours c, final TextKey label, final int keyX, final int keyY,
                     final boolean on, final int ink) {
        if (on) {
            g.fill(x(keyX), y(keyY), x(keyX + ProbeLayout.KEY_WIDTH), y(keyY + ProbeLayout.KEY_HEIGHT), c.pressed());
        }
        final Text word = label.text();
        small(g, word, keyX + (ProbeLayout.KEY_WIDTH - width(word)) / 2, keyY + (on ? 4 : 3), ink);
    }

    /* The members' page as lines: a heading for each role that has blocks, and a row for each kind of them. */
    private static List<Line> rows(final ProbeReading reading) {
        final List<Line> lines = new ArrayList<>();
        for (final LineSurvey.Role role : List.of(LineSurvey.Role.SOURCE, LineSurvey.Role.CONSUMER,
                LineSurvey.Role.STORE, LineSurvey.Role.IDLE)) {
            boolean headed = false;
            for (final ProbeReading.Group group : reading.groups()) {
                if (group.role() != role) {
                    continue;
                }
                if (!headed) {
                    lines.add(new Line(role, null));
                    headed = true;
                }
                lines.add(new Line(role, group));
            }
        }
        return lines;
    }

    private static int height(final List<Line> lines, final int from, final int to) {
        int height = 0;
        for (int k = from; k < to; k++) {
            height += lines.get(k).row() ? ProbeLayout.ROW_PITCH : ProbeLayout.GROUP_PITCH;
        }
        return height;
    }

    /* What a heading says on the right: what the sources give, what the consumers want and get, the rest's total. */
    private Text groupSummary(final ProbeReading reading, final LineSurvey.Role role) {
        final long total = reading.total(role);
        return switch (role) {
            case SOURCE -> NetworkProbe.GIVES.with(rate(reading, total));
            case CONSUMER -> reading.wanted() >= 0 ? NetworkProbe.WANT_GET.with(rate(reading, reading.wanted()),
                    rate(reading, total)) : NetworkProbe.TAKE.with(rate(reading, total));
            case STORE -> NetworkProbe.NET.with(Text.literal((total < 0 ? "-" : "") + GameText.resolve(rate(reading,
                    Math.abs(total)))));
            case IDLE -> Text.literal("");
        };
    }

    private static Text rate(final ProbeReading reading, final long perTick) {
        return reading.rate(perTick);
    }

    /* {@code part} as a share of {@code whole}, to a tenth of a per cent. */
    private static Text share(final long part, final long whole) {
        final double percent = whole <= 0 ? 0.0 : 100.0 * part / whole;
        return Text.literal(String.format(Locale.ROOT, "%.1f%%", percent));
    }

    /* The rating of the line's class, for an energy with classes: "400 V". */
    private static String rated(final ProbeReading reading) {
        if (reading.lineClass().isEmpty()) {
            return "";
        }
        final Optional<Energy> energy = reading.energy().flatMap(Energies::byId);
        final Optional<EnergyClass> lineClass = energy.flatMap(found -> found.classNamed(reading.lineClass()));
        return lineClass.flatMap(EnergyClass::ratingKey).map(rating -> GameText.resolve(rating.text())).orElse("");
    }

    private static Text place(final long pos) {
        final BlockPos at = BlockPos.of(pos);
        return Text.literal(at.getX() + ", " + at.getY() + ", " + at.getZ());
    }

    private static String cableName(final String cable) {
        final ResourceLocation id = ResourceLocation.tryParse(cable);
        if (NetworkProbe.MECHANICAL.equals(id)) {
            return GameText.resolve(NetworkProbe.SHAFTS.text());
        }
        return id == null ? cable : CoreCables.REGISTRY.getOptional(id).map(type -> type.stack().getHoverName()
                .getString()).orElse(cable);
    }

    private static String blockName(final ResourceLocation block) {
        return BuiltInRegistries.BLOCK.getOptional(block).map(found -> found.getName().getString())
                .orElse(block.toString());
    }

    private static String[] glyphOf(final LineSurvey.Role role) {
        return switch (role) {
            case SOURCE -> SOURCE;
            case CONSUMER -> LOAD;
            case STORE -> STORE;
            case IDLE -> IDLE;
        };
    }

    /** The colour of what a block is to the line: green for a source, amber for a consumer, cyan for a store. */
    public static int roleColour(final Colours c, final LineSurvey.Role role) {
        return switch (role) {
            case SOURCE -> c.green();
            case CONSUMER -> c.amber();
            case STORE -> c.cyan();
            case IDLE -> c.dim();
        };
    }

    private void glyph(final GuiGraphics g, final int gx, final int gy, final String[] rows, final int colour) {
        for (int j = 0; j < GLYPH; j++) {
            for (int i = 0; i < GLYPH; i++) {
                if (rows[j].charAt(i) != ' ') {
                    g.fill(x(gx + i), y(gy + j), x(gx + i + 1), y(gy + j + 1), colour);
                }
            }
        }
    }

    /* Words broken over the display's lines, in small text. */
    private void wrapped(final GuiGraphics g, final Text text, final int from, final int colour) {
        final int room = (int) ((ProbeLayout.TEXT_RIGHT - ProbeLayout.TEXT_LEFT) / ProbeLayout.SMALL);
        int y = from;
        for (final FormattedCharSequence line : this.font.split(GameText.component(text), room)) {
            g.pose().pushPose();
            g.pose().translate(x(ProbeLayout.TEXT_LEFT), y(y), 0);
            g.pose().scale(ProbeLayout.SMALL, ProbeLayout.SMALL, 1F);
            Draw.text(g, this.font, line, 0, 0, colour);
            g.pose().popPose();
            y += 8;
        }
    }

    private void small(final GuiGraphics g, final Text text, final int sx, final int sy, final int colour) {
        Draw.textScaled(g, this.font, GameText.resolve(text), x(sx), y(sy), colour, ProbeLayout.SMALL);
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

    /* {@code from} moved toward {@code toward} by {@code amount}, its alpha kept. */
    private static int mix(final int from, final int toward, final float amount) {
        final int a = from >>> 24;
        final int r = Math.round((from >> 16 & 0xFF) + ((toward >> 16 & 0xFF) - (from >> 16 & 0xFF)) * amount);
        final int gr = Math.round((from >> 8 & 0xFF) + ((toward >> 8 & 0xFF) - (from >> 8 & 0xFF)) * amount);
        final int b = Math.round((from & 0xFF) + ((toward & 0xFF) - (from & 0xFF)) * amount);
        return a << 24 | r << 16 | gr << 8 | b;
    }

    /**
     * The instrument's colours.
     *
     * @param silk      the printed words on its face
     * @param text      the display's words
     * @param dim       the display's quieter words
     * @param amber     a full line, and a consumer
     * @param green     a source, and the lamp lit
     * @param head      the display's header
     * @param headText  the class and the cables beside the header's name
     * @param white     the header's name and the big figures
     * @param amberInk  the word on the amber key
     * @param tealInk   the word on the teal key
     * @param lampOff   the lamp unlit
     * @param lit       the display lit
     * @param pressed   the shade over a key that is on
     * @param cyan      the page's headings, the tab shown, and a store
     * @param tabOff    the other tabs
     * @param tabInk    the word on the tab shown
     * @param ceiling   the narrowest cable's ceiling across the flow
     * @param powerInk  the word on the red key
     * @param select    the chosen row
     * @param tft       the display's own dark, which the bars fade into
     */
    public record Colours(int silk, int text, int dim, int amber, int green, int head, int headText, int white,
                          int amberInk, int tealInk, int lampOff, int lit, int pressed, int cyan, int tabOff,
                          int tabInk, int ceiling, int powerInk, int select, int tft) {
    }

    /** A line of the members' page: a role's heading, or a row of blocks of one kind. */
    private record Line(LineSurvey.Role role, @Nullable ProbeReading.Group group) {

        boolean row() {
            return this.group != null;
        }
    }
}
