/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.hazard.HazardsClient;
import dev.jstech.core.hazard.DamageKind;
import dev.jstech.core.hazard.HazardBand;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.Lungs;
import dev.jstech.core.hazard.Resistances;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.hazard.HazardScreenTexts;
import dev.jstech.core.jstechseries.hazard.IonizingRadiation;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The body's screen, a worn panel of old green enamel, chipped and rusted: what is worn and how much of each kind of
 * harm it stops; the dose taken on a big meter with the radiation's bands painted on its scale; the rate here on a
 * meter of two needles, from outside and from what is on the body, with a lamp asking for a shower; the lungs on an
 * edgewise meter; dissonance in five lamps; and the other hazards on the body. Live: the game goes on behind it.
 */
@PaletteHolder
public final class BodyScreen extends Screen {

    private int left;
    private int top;

    /** The panel, and how big it is; its lit lamps sit to its right in the same picture. */
    public static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "textures/gui/hazard/body_panel.png");
    public static final int WIDTH = 340;
    public static final int HEIGHT = 246;
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "body_panel", new Colours(
            0xFF << 24 | 0x1A1A1A, 0xFF << 24 | 0x464642, 0xFF << 24 | 0x787872, 0xFF << 24 | 0xE2DCC8,
            0xFF << 24 | 0x28241E, 0xFF << 24 | 0xFFD678, 0xFF << 24 | 0xB41812, 0xFF << 24 | 0x1E1E1E,
            0xFF << 24 | 0xBE1E18, 0x28 << 24, 0xFF << 24 | 0x1E1E1C, 0x32 << 24 | 0xFFFFFF));
    /* The picture is laid out in a sheet this big. */
    private static final int SHEET_WIDTH = 512;
    private static final int SHEET_HEIGHT = 256;
    /* The tray of what is worn: its slots, the name of what is worn, and the rows of what it stops. */
    private static final int SLOT_X = 32;
    private static final int SLOT_Y = 53;
    private static final int SLOT_PITCH = 19;
    private static final int WORN_NAME_X = 53;
    private static final int WORN_NAME_Y = 56;
    private static final int WORN_NAME_WIDTH = 36;
    private static final int STOPS_X = 30;
    private static final int STOPS_RIGHT = 86;
    private static final int STOPS_Y = 132;
    private static final int STOPS_PITCH = 10;
    private static final int STOPS_ROWS = 5;
    /* The dose meter: its centre and radius, how far its scale runs, and the dose its scale ends at. */
    private static final int DOSE_X = 143;
    private static final int DOSE_Y = 83;
    private static final int DOSE_RADIUS = 40;
    private static final double DOSE_MOST = 10;
    /* The rate meter, its scale of tens from a hundredth to a hundred. */
    private static final int RATE_X = 251;
    private static final int RATE_Y = 79;
    private static final int RATE_RADIUS = 32;
    private static final int LEGEND_X = 228;
    private static final int OUTSIDE_Y = 118;
    private static final int ON_YOU_Y = 126;
    /* The shower lamp: where its lit picture goes, and its word. */
    private static final int WASH_X = 292;
    private static final int WASH_Y = 34;
    private static final int WASH_WORD_X = 289;
    private static final int WASH_WORD_Y = 46;
    private static final int LIT_WASH_U = 344;
    private static final int LIT_WASH_SIZE = 12;
    /* The lungs' edgewise meter: its scale's ends, the needle's run, and the lines under it. */
    private static final int LUNGS_X = 99;
    private static final int LUNGS_Y = 155;
    private static final int LUNG_SCALE_X = 101;
    private static final int LUNG_SCALE_WIDTH = 112;
    private static final int LUNG_NEEDLE_TOP = 168;
    private static final int LUNG_NEEDLE_BOTTOM = 181;
    private static final int LUNG_TEXT_Y = 184;
    private static final int LUNG_BAND_Y = 192;
    /* The dissonance lamps: the first lamp's place, their pitch, their lit picture, their numerals. */
    private static final int DISSONANCE_X = 227;
    private static final int LAMP_X = 229;
    private static final int LAMP_Y = 169;
    private static final int LAMP_PITCH = 18;
    private static final int LIT_LAMP_U = 360;
    private static final int LIT_LAMP_SIZE = 14;
    private static final int NUMERAL_Y = 181;
    private static final int LAMPS = 5;
    private static final List<String> NUMERALS = List.of("I", "II", "III", "IV", "V");
    /* The plate of the other hazards, and how far it may run. */
    private static final int OTHER_Y = 213;
    private static final int OTHER_MOST_WIDTH = 216;
    /* The meters' scales sweep 240 degrees, from the lower left round to the lower right. */
    private static final double SCALE_START = Math.toRadians(150);
    private static final double SCALE_SWEEP = Math.toRadians(240);
    private static final float SMALL = 0.5F;
    private static final int PLATE_HEIGHT = 9;
    private static final List<Mark> DOSE_MARKS = List.of(new Mark(0, "0"), new Mark(1, "I"), new Mark(2, "II"),
            new Mark(4, "III"), new Mark(6, "IV"), new Mark(8, "V"), new Mark(10, "10"));
    private static final List<Mark> RATE_MARKS = List.of(new Mark(0.01, ".01"), new Mark(0.1, ".1"),
            new Mark(1, "1"), new Mark(10, "10"), new Mark(100, "100"));
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET};

    public BodyScreen() {
        super(GameText.component(HazardScreenTexts.BODY_TITLE));
    }

    /** Where the panel starts on the screen, its left then its top. */
    public int left() {
        return this.left;
    }

    public int top() {
        return this.top;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            return;
        }
        g.blit(PANEL, this.left, this.top, 0, 0, WIDTH, HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        plate(g, 25, 20, GameText.resolve(HazardScreenTexts.BODY_DOSIMETRY), 0);
        drawWorn(g, player);
        drawDose(g);
        drawRate(g, player);
        drawLungs(g);
        drawDissonance(g);
        drawOthers(g);
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
    }

    /* What is worn, its name, and what it stops of each kind of harm, the most first. */
    private void drawWorn(final GuiGraphics g, final Player player) {
        final Colours c = PALETTE.get();
        plate(g, 29, 39, GameText.resolve(HazardScreenTexts.PROTECTION), 58);
        ItemStack named = ItemStack.EMPTY;
        for (int k = 0; k < WORN.length; k++) {
            final ItemStack worn = player.getItemBySlot(WORN[k]);
            g.renderItem(worn, this.left + SLOT_X, this.top + SLOT_Y + k * SLOT_PITCH);
            if (named.isEmpty() || WORN[k] == EquipmentSlot.CHEST && !worn.isEmpty()) {
                named = worn;
            }
        }
        final Component name = named.isEmpty() ? GameText.component(HazardScreenTexts.NOTHING_WORN)
                : named.getHoverName();
        final List<FormattedCharSequence> lines = this.font.split(name, (int) (WORN_NAME_WIDTH / SMALL));
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.pose().pushPose();
            g.pose().translate(this.left + WORN_NAME_X, this.top + WORN_NAME_Y + i * 7, 0);
            g.pose().scale(SMALL, SMALL, 1);
            Draw.text(g, this.font, lines.get(i), 0, 0, c.plateInk());
            g.pose().popPose();
        }
        final List<Map.Entry<DamageKind, Double>> stops = new ArrayList<>(Resistances.worn(player).entrySet());
        stops.sort(Map.Entry.<DamageKind, Double>comparingByValue(Comparator.reverseOrder()));
        for (int k = 0; k < Math.min(STOPS_ROWS, stops.size()); k++) {
            final int y = this.top + STOPS_Y + k * STOPS_PITCH;
            small(g, GameText.resolve(stops.get(k).getKey().name()).toUpperCase(Locale.ROOT), this.left + STOPS_X, y,
                    c.plateInk());
            final String share = Math.round(stops.get(k).getValue() * 100) + "%";
            small(g, share, this.left + STOPS_RIGHT - width(share), y, c.amberInk());
        }
    }

    /* The dose taken on its meter, with the band it reached on the plate under it. */
    private void drawDose(final GuiGraphics g) {
        final Colours c = PALETTE.get();
        plate(g, 111, 33, GameText.resolve(HazardScreenTexts.DOSE_TAKEN), 64);
        final int cx = this.left + DOSE_X;
        final int cy = this.top + DOSE_Y;
        for (final Mark mark : DOSE_MARKS) {
            markLabel(g, cx, cy, DOSE_RADIUS, angle(mark.value() / DOSE_MOST), mark.text());
        }
        final Optional<HazardKind> radiation = DeviceReadings.radiation();
        centred(g, radiation.map(kind -> GameText.resolve(kind.unit())).orElse(""), cx,
                cy + DOSE_RADIUS / 2 + 3, c.ink());
        final double dose = radiation.map(DeviceReadings::level).orElse(0.0);
        needle(g, cx, cy, DOSE_RADIUS, angle(dose / DOSE_MOST), c.doseNeedle());
        pivot(g, cx, cy, DOSE_RADIUS);
        final String band = radiation.flatMap(kind -> kind.band(dose)).map(found -> GameText.resolve(found.name()))
                .orElse("");
        final String under = radiation.map(kind -> GameText.resolve(HazardScreenTexts.DOSE_AND_BAND.with(
                DeviceReadings.figure(dose) + " " + GameText.resolve(kind.unit()), band))).orElse(
                GameText.resolve(HazardDevices.OFF)).toUpperCase(Locale.ROOT).trim();
        final int w = width(under) + 12;
        plate(g, DOSE_X - w / 2, DOSE_Y + DOSE_RADIUS + 8, under, w);
    }

    /* The rate here on its meter: one needle for what comes from outside, one for what is on the body. */
    private void drawRate(final GuiGraphics g, final Player player) {
        final Colours c = PALETTE.get();
        final Optional<HazardKind> radiation = DeviceReadings.radiation();
        final String unit = radiation.map(DeviceReadings::rateUnit).orElse("");
        plate(g, 217, 33, GameText.resolve(HazardScreenTexts.RATE_HERE.with(unit)), 68);
        final int cx = this.left + RATE_X;
        final int cy = this.top + RATE_Y;
        for (final Mark mark : RATE_MARKS) {
            markLabel(g, cx, cy, RATE_RADIUS, angle(logShare(mark.value())), mark.text());
        }
        centred(g, unit, cx, cy + RATE_RADIUS / 2 + 3, c.ink());
        final double outside = radiation.map(DeviceReadings::rateHere).orElse(0.0);
        // The radioactive dust on the body doses it as its own rate, by how much of it there is.
        final double onYou = radiation.isPresent()
                ? IonizingRadiation.contaminationRate(HazardsClient.level(HazardDevices.CONTAMINATION)) : 0;
        needle(g, cx, cy, RATE_RADIUS, angle(logShare(outside)), c.outsideNeedle());
        needle(g, cx, cy, RATE_RADIUS, angle(logShare(onYou)), c.onYouNeedle());
        pivot(g, cx, cy, RATE_RADIUS);
        small(g, GameText.resolve(HazardScreenTexts.OUTSIDE.with(DeviceReadings.figure(outside))),
                this.left + LEGEND_X, this.top + OUTSIDE_Y, c.plateInk());
        small(g, GameText.resolve(HazardScreenTexts.ON_YOU.with(DeviceReadings.figure(onYou))),
                this.left + LEGEND_X, this.top + ON_YOU_Y, c.plateInk());
        final boolean contaminated = HazardsClient.level(HazardDevices.CONTAMINATION) > 0;
        if (contaminated && player.tickCount / 5 % 2 == 0) {
            g.blit(PANEL, this.left + WASH_X, this.top + WASH_Y, LIT_WASH_U, 0, LIT_WASH_SIZE, LIT_WASH_SIZE,
                    SHEET_WIDTH, SHEET_HEIGHT);
        }
        small(g, GameText.resolve(HazardScreenTexts.WASH), this.left + WASH_WORD_X, this.top + WASH_WORD_Y,
                c.plateInk());
    }

    /* The lungs: the harm in all on the edgewise meter, each dust's share, and the band it reached. */
    private void drawLungs(final GuiGraphics g) {
        final Colours c = PALETTE.get();
        plate(g, LUNGS_X, LUNGS_Y, GameText.resolve(HazardScreenTexts.LUNGS), 34);
        double harm = 0;
        final List<String> shares = new ArrayList<>();
        for (final HazardKind kind : Lungs.kinds()) {
            final double level = HazardsClient.level(kind.id());
            harm += level;
            if (Math.round(level) > 0) {
                shares.add(GameText.resolve(HazardScreenTexts.LUNG_SHARE.with(GameText.resolve(kind.name()),
                        Math.round(level))));
            }
        }
        harm = Math.min(100, harm);
        final int nx = this.left + LUNG_SCALE_X + (int) Math.round(LUNG_SCALE_WIDTH * harm / 100);
        g.fill(nx, this.top + LUNG_NEEDLE_TOP, nx + 1, this.top + LUNG_NEEDLE_BOTTOM, c.doseNeedle());
        final String said = shares.isEmpty() ? GameText.resolve(HazardScreenTexts.CLEAR_LUNGS)
                : String.join(", ", shares);
        small(g, GameText.resolve(HazardScreenTexts.LUNG_HARM.with(Math.round(harm), said)).toUpperCase(Locale.ROOT),
                this.left + LUNGS_X, this.top + LUNG_TEXT_Y, c.plateInk());
        Lungs.BLACK_LUNG.band(harm).ifPresent(band -> small(g, GameText.resolve(band.name())
                .toUpperCase(Locale.ROOT), this.left + LUNGS_X, this.top + LUNG_BAND_Y, c.amberInk()));
    }

    /* Dissonance: a lamp lit for each band reached, and the band's name, or the normal laws. */
    private void drawDissonance(final GuiGraphics g) {
        final Colours c = PALETTE.get();
        plate(g, DISSONANCE_X, LUNGS_Y, GameText.resolve(HazardScreenTexts.DISSONANCE), 58);
        final Optional<HazardKind> dissonance = DeviceReadings.on(HazardDevices.DISSONANCE);
        final Optional<HazardBand> band = dissonance.flatMap(kind -> kind.band(DeviceReadings.level(kind)));
        final int lit = dissonance.map(kind -> band.map(found -> kind.bands().indexOf(found) + 1).orElse(0))
                .orElse(0);
        for (int k = 0; k < LAMPS; k++) {
            if (k < lit) {
                g.blit(PANEL, this.left + LAMP_X + k * LAMP_PITCH - 3, this.top + LAMP_Y - 3, LIT_LAMP_U, 0,
                        LIT_LAMP_SIZE, LIT_LAMP_SIZE, SHEET_WIDTH, SHEET_HEIGHT);
            }
            centred(g, NUMERALS.get(k), this.left + LAMP_X + k * LAMP_PITCH + 4, this.top + NUMERAL_Y, c.plateInk());
        }
        small(g, band.map(found -> GameText.resolve(found.name())).orElseGet(() -> GameText.resolve(
                HazardScreenTexts.NORMAL_LAWS)).toUpperCase(Locale.ROOT), this.left + DISSONANCE_X,
                this.top + LUNG_BAND_Y, c.plateInk());
    }

    /* The other hazards on the body, each with its band or how much has built up. */
    private void drawOthers(final GuiGraphics g) {
        final Set<ResourceLocation> shownAbove = Set.of(HazardDevices.RADIATION, HazardDevices.CONTAMINATION,
                HazardDevices.DISSONANCE, Lungs.BLACK_LUNG.id(), Lungs.SILICOSIS.id(), Lungs.ASBESTOSIS.id());
        final List<String> others = new ArrayList<>();
        for (final HazardKind kind : Hazards.all()) {
            final double level = HazardsClient.level(kind.id());
            if (shownAbove.contains(kind.id()) || level <= 0) {
                continue;
            }
            final String how = kind.band(level).map(band -> GameText.resolve(band.name()))
                    .orElseGet(() -> DeviceReadings.figure(level) + " " + GameText.resolve(kind.unit()));
            others.add(GameText.resolve(HazardScreenTexts.OTHER_ONE.with(GameText.resolve(kind.name()), how)));
        }
        String said = others.isEmpty() ? GameText.resolve(HazardScreenTexts.OTHER_NONE)
                : GameText.resolve(HazardScreenTexts.OTHER.with(String.join(", ", others)));
        said = said.toUpperCase(Locale.ROOT);
        final int room = (int) ((OTHER_MOST_WIDTH - 12) / SMALL);
        if (this.font.width(said) > room) {
            said = this.font.plainSubstrByWidth(said, room - this.font.width("...")) + "...";
        }
        plate(g, LUNGS_X, OTHER_Y, said, 0);
    }

    /* A black label plate, its letters in cream, two screws: as wide as {@code width}, or as its words when 0. */
    private void plate(final GuiGraphics g, final int x, final int y, final String text, final int width) {
        final Colours c = PALETTE.get();
        final int w = width > 0 ? Math.max(width, width(text) + 12) : width(text) + 12;
        final int px = this.left + x;
        final int py = this.top + y;
        g.fill(px, py, px + w, py + PLATE_HEIGHT, c.plate());
        g.fill(px, py, px + w, py + 1, c.plateTop());
        g.fill(px + 2, py + 4, px + 3, py + 5, c.screw());
        g.fill(px + w - 3, py + 4, px + w - 2, py + 5, c.screw());
        small(g, text, px + 6, py + 2, c.plateInk());
    }

    /* A figure printed on a meter's scale, inside the tick at {@code angle}. */
    private void markLabel(final GuiGraphics g, final int cx, final int cy, final int r, final double angle,
                           final String text) {
        centred(g, text, cx + (int) Math.round((r - 12) * Math.cos(angle)),
                cy + (int) Math.round((r - 12) * Math.sin(angle)) - 3, PALETTE.get().ink());
    }

    /* A needle from just behind the pivot out to the scale, with its shadow. */
    private void needle(final GuiGraphics g, final int cx, final int cy, final int r, final double angle,
                        final int colour) {
        final Colours c = PALETTE.get();
        for (int s = -4; s < r - 3; s++) {
            final int px = cx + (int) Math.round(s * Math.cos(angle));
            final int py = cy + (int) Math.round(s * Math.sin(angle));
            g.fill(px + 1, py + 1, px + 2, py + 2, c.shadow());
            g.fill(px, py, px + 1, py + 1, colour);
        }
    }

    /* The needles' pivot over them, and the glint on the meter's glass. */
    private void pivot(final GuiGraphics g, final int cx, final int cy, final int r) {
        final Colours c = PALETTE.get();
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, c.pivot());
        for (int k = 0; k < Math.min(r, 10); k++) {
            final int gx = cx - r / 2 + k;
            final int gy = cy - r + 4 + k / 3;
            g.fill(gx, gy, gx + 1, gy + 1, c.glint());
        }
    }

    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour) {
        Draw.textScaled(g, this.font, text, x, y, colour, SMALL);
    }

    private void centred(final GuiGraphics g, final String text, final int cx, final int y, final int colour) {
        small(g, text, cx - width(text) / 2, y, colour);
    }

    private int width(final String text) {
        return (int) Math.ceil(this.font.width(text) * SMALL);
    }

    /* Where a share of a meter's scale points, as an angle on the screen. */
    private static double angle(final double share) {
        return SCALE_START + SCALE_SWEEP * Math.clamp(share, 0, 1);
    }

    /* Where a rate sits on a scale of tens from a hundredth to a hundred, as a share of the scale. */
    private static double logShare(final double rate) {
        return (Math.log10(Math.max(rate, 0.01)) + 2) / 4;
    }

    /* A figure printed on a meter's scale, at the value it marks. */
    private record Mark(double value, String text) {
    }

    /**
     * The colours of the body's screen.
     *
     * @param plate         a label plate
     * @param plateTop      a plate's top edge
     * @param screw         a plate's screws
     * @param plateInk      the cream letters on the plates and the panel
     * @param ink           the figures printed on the meters' faces
     * @param amberInk      the shares worn things stop, and the lungs' band
     * @param doseNeedle    the dose meter's needle and the lungs' needle
     * @param outsideNeedle the needle of what comes from outside
     * @param onYouNeedle   the needle of what is on the body
     * @param shadow        a needle's shadow
     * @param pivot         the needles' pivot
     * @param glint         the glint on a meter's glass
     */
    public record Colours(int plate, int plateTop, int screw, int plateInk, int ink, int amberInk, int doseNeedle,
                          int outsideNeedle, int onYouNeedle, int shadow, int pivot, int glint) {
    }
}
