/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.air.AirMath;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineWords;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetector;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetectorBlockEntity;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetectorLayout;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereDetectorMenu;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereReadings;
import dev.jstech.core.jstechseries.atmosphere.AtmosphereTexts;
import dev.jstech.core.material.Materials;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.RoomView;
import dev.jstech.core.room.RoomViews;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The Atmosphere Detector's screen, as the approved design draws it: no inventory, since it holds nothing; on the
 * steel face its plate; the pressure gauge from 0 to 120 kPa with the band one can breathe in, the figure and how fast
 * it changes under it; the air on a display, each gas with its share and its own pressure and what the air does, and a
 * bar of the shares; the room as the detector draws it, its walls, its air in moving dashes and its leaks blinking, its
 * size and leaks under it, or the open air; the lamps BREATHABLE, HYPOXIA, LEAKING and VACUUM; and the key that shows
 * the room in the world.
 */
@PaletteHolder
public final class AtmosphereDetectorScreen extends CoreContainerScreen<AtmosphereDetectorMenu> {

    private int ticks;

    /** The gauge's, the display's and the drawing's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "atmosphere_detector",
            new Colours(0xFF << 24 | 0xEFE9D2, 0xFF << 24 | 0x787E86, 0xFF << 24 | 0x323232, 0xFF << 24 | 0x3CAA50,
                    0xFF << 24 | 0xC83C32, 0xFF << 24 | 0xC41E18, 0xFF << 24 | 0x282828, 0xFF << 24 | 0x6E96DC,
                    0xFF << 24 | 0x78DC8C, 0xFF << 24 | 0xC8A0F0, 0xFF << 24 | 0xA0A0A0, 0xFF << 24 | 0x1E2024,
                    0xFF << 24 | 0x0E1218, 0xFF << 24 | 0x6E7884, 0xFF << 24 | 0x55FFFF, 0xFF << 24 | 0xFF463C,
                    0xFF << 24 | 0x781E1A, 0xFF << 24 | 0x5A8CE6, 0xFF << 24 | 0x55E6FF));
    /* The gauge sweeps three quarters of a turn, from the bottom left round to the bottom right. */
    private static final double SWEEP_FROM = Math.PI * 0.75;
    private static final double SWEEP = Math.PI * 1.5;
    private static final int TICK_EVERY = 5;
    private static final int LONG_EVERY = 20;
    private static final int[] FIGURES = {0, 40, 80, 120};
    private static final int BREATHE_FROM = 80;
    private static final int BREATHE_TO = 110;
    private static final int THIN_TO = 25;
    private static final double STEADY = 0.05;
    private static final int BLINK = 4;
    private static final int ITEM_INSET = 6;
    private static final double SHADE_HI = 0.3;
    private static final double SHADE_LO = 0.45;
    private static final double VACUUM = 1.0;
    private static final int MOST_SCALE = 8;

    public AtmosphereDetectorScreen(final AtmosphereDetectorMenu menu, final Inventory inventory,
                                    final Component title) {
        super(menu, inventory, title);
        this.imageWidth = AtmosphereDetectorLayout.WIDTH;
        this.imageHeight = AtmosphereDetectorLayout.HEIGHT;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (AtmosphereDetectorLayout.onShow(mouseX - this.leftPos, mouseY - this.topPos)) {
            g.renderTooltip(this.font, GameText.component(showing() ? AtmosphereTexts.HIDE_TIP
                    : AtmosphereTexts.SHOW_TIP), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT
                && AtmosphereDetectorLayout.onShow(mouseX - this.leftPos, mouseY - this.topPos)) {
            AtmosphereDetector.SHOW_ROOM.send(!showing());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.ticks++;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final int x0 = this.leftPos;
        final int y0 = this.topPos;
        MachineArt.window(g, x0, y0 + AtmosphereDetectorLayout.WINDOW_Y, x0 + AtmosphereDetectorLayout.WIDTH,
                y0 + AtmosphereDetectorLayout.HEIGHT, x0 + AtmosphereDetectorLayout.TAB_X,
                x0 + AtmosphereDetectorLayout.TAB_X + AtmosphereDetectorLayout.TAB_SIZE);
        MachineArt.tab(g, x0 + AtmosphereDetectorLayout.TAB_X, y0, true);
        g.renderItem(new ItemStack(AtmosphereDetector.BLOCK.get()), x0 + AtmosphereDetectorLayout.TAB_X + ITEM_INSET,
                y0 + ITEM_INSET);
        MachineArt.shade(g, x0 + AtmosphereDetectorLayout.FACE_LEFT, y0 + AtmosphereDetectorLayout.FACE_TOP,
                x0 + AtmosphereDetectorLayout.FACE_RIGHT, y0 + AtmosphereDetectorLayout.FACE_BOTTOM, m.steel(),
                SHADE_HI, SHADE_LO);
        for (final int bx : new int[] {AtmosphereDetectorLayout.FACE_LEFT + 2, AtmosphereDetectorLayout.FACE_RIGHT
                - 5}) {
            for (final int by : new int[] {AtmosphereDetectorLayout.FACE_TOP + 2,
                    AtmosphereDetectorLayout.FACE_BOTTOM - 5}) {
                MachineArt.bolt(g, x0 + bx, y0 + by);
            }
        }
        MachineArt.plate(g, this.font, x0 + AtmosphereDetectorLayout.PLATE_X, y0 + AtmosphereDetectorLayout.PLATE_Y,
                MachineWords.upper(AtmosphereTexts.PLATE), m.aluminium(), m.plateInk());
        final AtmosphereReadings read = detector().readings();
        gauge(g, read);
        air(g, read);
        room(g, read);
        lamps(g, read);
        MachineArt.pushbutton(g, this.font, x0 + AtmosphereDetectorLayout.SHOW_X, y0 + AtmosphereDetectorLayout.SHOW_Y,
                showing(), PALETTE.get().show(), MachineWords.upper(AtmosphereTexts.SHOW));
        MachineArt.engrave(g, this.font, MachineWords.upper(AtmosphereTexts.IN_THE_WORLD),
                x0 + AtmosphereDetectorLayout.SHOW_X, y0 + AtmosphereDetectorLayout.WORLD_WORDS_Y, false);
    }

    /* Nothing over the face: its words are its own, and it has no inventory. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
    }

    private AtmosphereDetectorBlockEntity detector() {
        return this.menu.detector();
    }

    /* Whether this detector shows its room in the world to this player now. */
    private boolean showing() {
        final RoomView view = this.minecraft == null || this.minecraft.level == null ? RoomView.NONE
                : RoomViews.seen(this.minecraft.level.getGameTime());
        return view.shown() && view.source() == RoomView.Source.DETECTOR;
    }

    /* The gauge, from 0 to 120 kPa: the band one can breathe in green, too thin in red, the needle, the figures. */
    private void gauge(final GuiGraphics g, final AtmosphereReadings read) {
        final Colours c = PALETTE.get();
        final int cx = this.leftPos + AtmosphereDetectorLayout.DIAL_X;
        final int cy = this.topPos + AtmosphereDetectorLayout.DIAL_Y;
        final int r = AtmosphereDetectorLayout.DIAL_RADIUS;
        MachineArt.disc(g, cx, cy, r + 3, c.rim(), false);
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (Math.hypot(x, y) <= r) {
                    MachineArt.dot(g, cx + x, cy + y, MachineArt.mix(c.face(), c.hub(), 0.08 * (y + r) / (2.0 * r)));
                }
            }
        }
        for (int v = 0; v <= AtmosphereDetectorLayout.DIAL_MOST; v += TICK_EVERY) {
            final double a = angle(v);
            final int from = v % LONG_EVERY == 0 ? r - 5 : r - 3;
            for (int s = from; s < r - 1; s++) {
                MachineArt.dot(g, cx + (int) Math.round(s * Math.cos(a)), cy + (int) Math.round(s * Math.sin(a)),
                        c.tick());
            }
        }
        band(g, cx, cy, r, BREATHE_FROM, BREATHE_TO, c.breathe());
        band(g, cx, cy, r, 0, THIN_TO, c.thin());
        for (final int v : FIGURES) {
            final double a = angle(v);
            MachineArt.textCentred(g, this.font, Integer.toString(v), cx + (int) Math.round((r - 11) * Math.cos(a)),
                    cy + (int) Math.round((r - 11) * Math.sin(a)) - 3, c.tick());
        }
        MachineArt.textCentred(g, this.font, GameText.resolve(AtmosphereTexts.UNIT), cx, cy + 12, c.tick());
        final double needle = angle(Math.min(AtmosphereDetectorLayout.DIAL_MOST, Math.max(0, read.pressure())));
        for (int s = -3; s < r - 4; s++) {
            MachineArt.dot(g, cx + (int) Math.round(s * Math.cos(needle)), cy + (int) Math.round(s * Math.sin(
                    needle)), c.needle());
        }
        MachineArt.fill(g, cx - 2, cy - 2, cx + 3, cy + 3, c.hub());
        MachineArt.glassShine(g, cx - r, cy - r, cx + r, cy + r);
        final String figure = GameText.resolve(AtmosphereTexts.KILOPASCALS.with(String.format(Locale.ROOT, "%.0f",
                read.pressure())));
        Draw.textCentered(g, this.font, figure, cx, this.topPos + AtmosphereDetectorLayout.FIGURE_Y, c.hub());
        final TextKey rate = read.change() < -STEADY ? AtmosphereTexts.FALLING : read.change() > STEADY
                ? AtmosphereTexts.RISING : AtmosphereTexts.STEADY;
        MachineArt.engrave(g, this.font, GameText.resolve(rate.with(String.format(Locale.ROOT, "%.1f",
                Math.abs(read.change())))), cx, this.topPos + AtmosphereDetectorLayout.RATE_Y, true);
    }

    /* A coloured band along the gauge's edge, from {@code from} to {@code to} kilopascals. */
    private static void band(final GuiGraphics g, final int cx, final int cy, final int r, final int from,
                             final int to, final int colour) {
        for (int v = from; v <= to; v++) {
            final double a = angle(v);
            for (int s = r - 2; s < r; s++) {
                MachineArt.dot(g, cx + (int) Math.round(s * Math.cos(a)), cy + (int) Math.round(s * Math.sin(a)),
                        colour);
            }
        }
    }

    private static double angle(final double kilopascals) {
        return SWEEP_FROM + SWEEP * kilopascals / AtmosphereDetectorLayout.DIAL_MOST;
    }

    /* The air: each gas, its share and its own pressure, what the air does, and a bar of the shares. */
    private void air(final GuiGraphics g, final AtmosphereReadings read) {
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final Colours c = PALETTE.get();
        final int x = this.leftPos + AtmosphereDetectorLayout.AIR_X;
        final int y = this.topPos + AtmosphereDetectorLayout.AIR_Y;
        final int w = AtmosphereDetectorLayout.AIR_WIDTH;
        MachineArt.engrave(g, this.font, MachineWords.upper(AtmosphereTexts.THE_AIR),
                this.leftPos + AtmosphereDetectorLayout.AIR_LABEL_X, this.topPos + AtmosphereDetectorLayout.LABEL_Y,
                false);
        MachineArt.lcd(g, this.font, x, y, w, AtmosphereDetectorLayout.AIR_HEIGHT, List.of(), true, false);
        final List<AtmosphereReadings.Gas> gases = read.gases();
        for (int i = 0; i < gases.size() && i < AtmosphereDetectorLayout.AIR_LINES - 1; i++) {
            final AtmosphereReadings.Gas gas = gases.get(i);
            final int ly = y + 4 + i * AtmosphereDetectorLayout.AIR_PITCH;
            final String share = String.format(Locale.ROOT, "%.1f%%", gas.share() * 100);
            final String partial = GameText.resolve(AtmosphereTexts.KILOPASCALS.with(String.format(Locale.ROOT,
                    "%.1f", gas.share() * read.pressure())));
            MachineArt.text(g, this.font, AtmosphereDetector.gasLabel(gas.id()), x + 4, ly, m.lcdGlow());
            MachineArt.text(g, this.font, share, x + 52 - MachineArt.width(this.font, share), ly, m.lcdGlow());
            MachineArt.text(g, this.font, partial, x + w - 4 - MachineArt.width(this.font, partial), ly,
                    m.lcdGlow());
        }
        MachineArt.text(g, this.font, MachineArt.fit(this.font, GameText.resolve(status(read)), w - 8), x + 4,
                y + 4 + (AtmosphereDetectorLayout.AIR_LINES - 1) * AtmosphereDetectorLayout.AIR_PITCH, m.lcdGlow());
        MachineArt.glassShine(g, x, y, x + w, y + AtmosphereDetectorLayout.AIR_HEIGHT);
        final int bx = this.leftPos + AtmosphereDetectorLayout.BAR_X;
        final int by = this.topPos + AtmosphereDetectorLayout.BAR_Y;
        final int bw = AtmosphereDetectorLayout.BAR_WIDTH;
        MachineArt.fill(g, bx - 1, by - 1, bx + bw + 1, by + AtmosphereDetectorLayout.BAR_HEIGHT + 1, c.barGround());
        int at = 0;
        final List<int[]> words = new ArrayList<>();
        for (final AtmosphereReadings.Gas gas : gases) {
            final int width = Math.max(1, (int) Math.round(bw * gas.share()));
            if (at + width > bw) {
                break;
            }
            MachineArt.fill(g, bx + at, by, bx + at + width, by + AtmosphereDetectorLayout.BAR_HEIGHT,
                    colour(gas.id()));
            if (width >= MachineArt.width(this.font, AtmosphereDetector.gasLabel(gas.id())) + 2) {
                words.add(new int[] {at, gases.indexOf(gas)});
            }
            at += width;
        }
        for (final int[] word : words) {
            MachineArt.engrave(g, this.font, AtmosphereDetector.gasLabel(gases.get(word[1]).id()), bx + word[0],
                    this.topPos + AtmosphereDetectorLayout.BAR_WORDS_Y, false);
        }
    }

    /* What the air does, on the display's last line. */
    private static TextKey status(final AtmosphereReadings read) {
        final double oxygen = AirMath.partial(read.share(AirMix.OXYGEN), read.pressure());
        if (read.pressure() < VACUUM) {
            return AtmosphereTexts.VACUUM_LINE;
        }
        if (GasMath.poisons(oxygen)) {
            return AtmosphereTexts.TOXIC_LINE;
        }
        if (GasMath.headache(read.share(AirMix.CARBON_DIOXIDE))) {
            return AtmosphereTexts.STALE_LINE;
        }
        return oxygen < AirMath.LEAST_OXYGEN ? AtmosphereTexts.HYPOXIA_LINE : AtmosphereTexts.FINE_LINE;
    }

    /* A gas's colour on the bar: the air's own gases each theirs, any other its material's. */
    private static int colour(final ResourceLocation gas) {
        final Colours c = PALETTE.get();
        if (gas.equals(AirMix.NITROGEN)) {
            return c.nitrogen();
        }
        if (gas.equals(AirMix.OXYGEN)) {
            return c.oxygen();
        }
        if (gas.equals(AirMix.ARGON)) {
            return c.argon();
        }
        if (gas.equals(AirMix.CARBON_DIOXIDE)) {
            return c.carbonDioxide();
        }
        return Materials.byId(gas).map(material -> 0xFF << 24 | material.colour()).orElse(c.carbonDioxide());
    }

    /* The room as the detector draws it, its size and leaks under it; or the open air. */
    private void room(final GuiGraphics g, final AtmosphereReadings read) {
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final Colours c = PALETTE.get();
        MachineArt.engrave(g, this.font, MachineWords.upper(AtmosphereTexts.THE_ROOM),
                this.leftPos + AtmosphereDetectorLayout.ROOM_LABEL_X, this.topPos + AtmosphereDetectorLayout.LABEL_Y,
                false);
        final int vx = this.leftPos + AtmosphereDetectorLayout.VIEW_X;
        final int vy = this.topPos + AtmosphereDetectorLayout.VIEW_Y;
        final int vw = AtmosphereDetectorLayout.VIEW_WIDTH;
        final int vh = AtmosphereDetectorLayout.VIEW_HEIGHT;
        MachineArt.bezel(g, vx - 3, vy - 3, vx + vw + 3, vy + vh + 3, false);
        MachineArt.fill(g, vx, vy, vx + vw, vy + vh, c.viewGround());
        final String line;
        if (read.room()) {
            drawing(g, read, vx, vy, vw, vh, c);
            line = read.leaks() > 0 ? GameText.resolve(AtmosphereTexts.ROOM_LINE.with(read.volume(), read.leaks()))
                    : GameText.resolve(AtmosphereTexts.SEALED_LINE.with(read.volume()));
        } else {
            line = MachineWords.upper(AtmosphereTexts.OPEN_AIR);
            MachineArt.textCentred(g, this.font, line, vx + vw / 2, vy + vh / 2 - 3, c.walls());
        }
        MachineArt.glassShine(g, vx, vy, vx + vw, vy + vh);
        final int lx = this.leftPos + AtmosphereDetectorLayout.ROOM_X;
        final int ly = this.topPos + AtmosphereDetectorLayout.ROOM_Y;
        MachineArt.lcd(g, this.font, lx, ly, AtmosphereDetectorLayout.ROOM_WIDTH, AtmosphereDetectorLayout.ROOM_HEIGHT,
                List.of(), true, false);
        MachineArt.text(g, this.font, line, lx + 3, ly + 2, m.lcdGlow());
    }

    /* The room drawn in the view as from above a corner: its walls in grey, its air in moving dashes, its leaks. */
    private void drawing(final GuiGraphics g, final AtmosphereReadings read, final int vx, final int vy,
                         final int vw, final int vh, final Colours c) {
        final BlockPos size = read.size();
        final int across = size.getX() + 2;
        final int along = size.getZ() + 2;
        final int up = size.getY() + 2;
        final int scale = Math.max(1, Math.min(MOST_SCALE, Math.min((vw - 4) / (across + along),
                (vh - 4) / ((across + along) / 2 + up))));
        // The drawing spans across + along blocks wide and half that plus its height high; its middle is the view's.
        final Iso iso = new Iso(vx + vw / 2 - (across - along) * scale / 2,
                vy + vh / 2 - ((across + along) * scale / 2 - up * scale) / 2, scale);
        box(g, iso, 0, 0, 0, across, up, along, c.walls(), false);
        box(g, iso, 1, 1, 1, across - 1, up - 1, along - 1, c.air(), true);
        final boolean on = this.ticks / BLINK % 2 == 0;
        for (final BlockPos spot : read.spots()) {
            final int[] at = iso.at(spot.getX() + 1.5, spot.getY() + 1.5, spot.getZ() + 1.5);
            final int half = scale >= 4 ? 2 : 1;
            MachineArt.fill(g, at[0] - half, at[1] - half, at[0] + half + 1, at[1] + half + 1, on ? c.leak()
                    : c.leakDark());
        }
    }

    /* The edges of a box in the drawing, dashed and moving for the air. */
    private void box(final GuiGraphics g, final Iso iso, final int x0, final int y0, final int z0, final int x1,
                     final int y1, final int z1, final int colour, final boolean dashed) {
        final int[][] corners = {{x0, y0, z0}, {x1, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, {x0, y0, z1}, {x1, y0, z1},
            {x0, y1, z1}, {x1, y1, z1}};
        for (int i = 0; i < corners.length; i++) {
            for (int j = i + 1; j < corners.length; j++) {
                int differ = 0;
                for (int k = 0; k < 3; k++) {
                    differ += corners[i][k] != corners[j][k] ? 1 : 0;
                }
                if (differ == 1) {
                    line(g, iso.at(corners[i][0], corners[i][1], corners[i][2]), iso.at(corners[j][0],
                            corners[j][1], corners[j][2]), colour, dashed);
                }
            }
        }
    }

    private void line(final GuiGraphics g, final int[] from, final int[] to, final int colour, final boolean dashed) {
        final int steps = Math.max(Math.abs(to[0] - from[0]), Math.abs(to[1] - from[1]));
        for (int k = 0; k <= steps; k++) {
            if (dashed && (k + this.ticks) % 4 >= 2) {
                continue;
            }
            final double t = steps == 0 ? 0 : (double) k / steps;
            MachineArt.dot(g, (int) Math.round(from[0] + (to[0] - from[0]) * t),
                    (int) Math.round(from[1] + (to[1] - from[1]) * t), colour);
        }
    }

    /* The lamps: the air breathable, too thin, the room leaking, a vacuum. */
    private void lamps(final GuiGraphics g, final AtmosphereReadings read) {
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final double oxygen = AirMath.partial(read.share(AirMix.OXYGEN), read.pressure());
        final boolean breathable = oxygen >= AirMath.LEAST_OXYGEN;
        final boolean vacuum = read.pressure() < VACUUM;
        final boolean blink = this.ticks / BLINK % 2 == 0;
        lamp(g, AtmosphereDetectorLayout.LAMP_LEFT, AtmosphereDetectorLayout.LAMP_TOP, m.lampRun(), breathable,
                AtmosphereTexts.BREATHABLE);
        lamp(g, AtmosphereDetectorLayout.LAMP_LEFT, AtmosphereDetectorLayout.LAMP_BOTTOM, m.lampWait(),
                !breathable && !vacuum && blink, AtmosphereTexts.HYPOXIA);
        lamp(g, AtmosphereDetectorLayout.LAMP_RIGHT, AtmosphereDetectorLayout.LAMP_TOP, m.lampFault(),
                read.room() && read.leaks() > 0, AtmosphereTexts.LEAKING);
        lamp(g, AtmosphereDetectorLayout.LAMP_RIGHT, AtmosphereDetectorLayout.LAMP_BOTTOM, PALETTE.get().vacuum(),
                vacuum, AtmosphereTexts.VACUUM);
    }

    private void lamp(final GuiGraphics g, final int x, final int y, final int colour, final boolean lit,
                      final TextKey word) {
        MachineArt.lamp(g, this.leftPos + x, this.topPos + y, colour, lit);
        MachineArt.engrave(g, this.font, MachineWords.upper(word), this.leftPos + x
                + AtmosphereDetectorLayout.LAMP_WORD, this.topPos + y + 1, false);
    }

    /*
     * The drawing's projection, as from above a corner: a block across goes down to the right, a block along down to
     * the left, a block up straight up.
     */
    private record Iso(int x, int y, int scale) {

        int[] at(final double across, final double up, final double along) {
            return new int[] {(int) Math.round(this.x + (across - along) * this.scale),
                (int) Math.round(this.y + (across + along) * this.scale / 2.0 - up * this.scale)};
        }
    }

    /**
     * The screen's colours.
     *
     * @param face          the gauge's face
     * @param rim           its rim
     * @param tick          its ticks and figures
     * @param breathe       the band one can breathe in
     * @param thin          the band too thin
     * @param needle        the needle
     * @param hub           its hub, and the figure under the gauge
     * @param nitrogen      nitrogen on the bar
     * @param oxygen        oxygen on the bar
     * @param argon         argon on the bar
     * @param carbonDioxide carbon dioxide on the bar, and any gas with no colour of its own
     * @param barGround     the bar's ground
     * @param viewGround    the ground of the room's drawing
     * @param walls         the room's walls in it
     * @param air           the room's air in it
     * @param leak          a leak, lit
     * @param leakDark      and dark
     * @param vacuum        the lamp VACUUM
     * @param show          the key that shows the room
     */
    public record Colours(int face, int rim, int tick, int breathe, int thin, int needle, int hub, int nitrogen,
                          int oxygen, int argon, int carbonDioxide, int barGround, int viewGround, int walls, int air,
                          int leak, int leakDark, int vacuum, int show) {
    }
}
