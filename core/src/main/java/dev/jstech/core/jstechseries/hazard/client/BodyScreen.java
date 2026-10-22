/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.gui.data.DataScreens;
import dev.jstech.core.client.gui.data.ScreenWiring;
import dev.jstech.core.client.hazard.BodyTemperatureClient;
import dev.jstech.core.client.hazard.BodyTemperatureHud;
import dev.jstech.core.client.hazard.HazardsClient;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.hazard.BodyTemperature;
import dev.jstech.core.hazard.BodyTemperaturePayload;
import dev.jstech.core.hazard.Clothing;
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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * The body's screen, declared in a file ({@code assets/jscore/jstech/screens/body.json}) a resource pack may replace,
 * drawn in the style the player picks: what is worn, how it keeps the weather out and what it stops; the core's warmth
 * among its bands, with what the place does to it; the dose built up, the rate here and what is on the body; the lungs;
 * dissonance; and the other hazards on the body. Live: the game goes on behind it. This class hands the file its
 * values by name; the colours of its scales are a palette, its words and grounds the style's.
 */
@PaletteHolder
public final class BodyScreen {

    /** The file the screen is declared in. */
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "body");
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "screen/body", new Colours(
            0xFF2B4B83, 0xFF5B7CA5, 0xFF4B5D73, 0xFF4B7442, 0xFF8A6235, 0xFFA03A2E, 0xFFFFFFFF, 0xFF6EC83C,
            0xFFDC281E, 0xFF787878, 0xFF3C3228, 0xFF966EE6, 0xFFF05ADC));
    /** How many of the kinds of harm what is worn stops are listed, the most first. */
    public static final int STOP_ROWS = 5;
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET};
    /* Below these the core's change, the acclimation, the wind, the sun and a share are not told. */
    private static final double TOLD_CHANGE = 0.05;
    private static final double TOLD_ACCLIMATION = 0.05;
    private static final double TOLD_WIND = 0.5;
    private static final double TOLD_SUN = 1;
    private static final double TOLD_SHARE = 0.005;
    /* The rate here is told past this, in its unit. */
    private static final double TOLD_RATE = 0.005;
    private static final double LUNGS_MOST = 100;

    private BodyScreen() {
    }

    /** Opens the body's screen over the game. */
    public static void open() {
        DataScreens.open(FILE, GameText.component(HazardScreenTexts.BODY_TITLE), BodyScreen::wire);
    }

    /** Hands the file's names their values. */
    public static void wire(final ScreenWiring<Void> screen) {
        for (final EquipmentSlot slot : WORN) {
            screen.value("worn_" + slot.getName(), () -> worn(slot));
        }
        screen.value("worn_name", BodyScreen::wornName)
                .value("worn_from", BodyScreen::wornFrom)
                .value("anything_worn", () -> !named().isEmpty())
                .value("lets_out", BodyScreen::letsOut)
                .value("wind", BodyScreen::wind)
                .value("wet", () -> say(warmth().wet() ? HazardScreenTexts.SOAKED : HazardScreenTexts.DRY))
                .value("no_stops", () -> stops().isEmpty());
        for (int row = 0; row < STOP_ROWS; row++) {
            final int k = row;
            screen.value("stop_" + (k + 1), () -> stop(k).map(entry -> GameText.resolve(entry.getKey().name()))
                    .orElse(null));
            screen.value("stop_" + (k + 1) + "_share", () -> stop(k).map(entry -> percent(entry.getValue() * 100))
                    .orElse(""));
        }
        wireTemperature(screen);
        wireRadiation(screen);
        wireLungs(screen);
        wireDissonance(screen);
        screen.value("others", BodyScreen::others)
                .onAction("done", () -> Optional.ofNullable(Minecraft.getInstance().screen).ifPresent(Screen::onClose));
    }

    private static void wireTemperature(final ScreenWiring<Void> screen) {
        screen.value("temperature_on", BodyTemperature.HYPOTHERMIA::enabled)
                .value("temperature_band", BodyScreen::temperatureBand)
                .value("core_line", BodyScreen::coreLine)
                .value("temperature", () -> BodyScales.temperature(warmth(), PALETTE.get()))
                .value("place_line", BodyScreen::placeLine)
                .value("used", () -> Math.abs(warmth().acclimation()) >= TOLD_ACCLIMATION)
                .value("used_line", () -> {
                    final double used = warmth().acclimation();
                    return say((used < 0 ? HazardScreenTexts.USED_TO_COLD : HazardScreenTexts.USED_TO_HEAT)
                            .with(degrees(Math.abs(used))));
                });
    }

    private static void wireRadiation(final ScreenWiring<Void> screen) {
        screen.value("radiation_on", () -> DeviceReadings.radiation().isPresent())
                .value("radiation_band", () -> DeviceReadings.radiation().map(BodyScreen::bandOf)
                        .orElseGet(() -> say(HazardScreenTexts.NONE)))
                .value("dose_line", () -> DeviceReadings.radiation().map(kind -> say(HazardScreenTexts.DOSE_LINE
                        .with(DeviceReadings.figure(DeviceReadings.level(kind)) + " " + GameText.resolve(kind.unit()))))
                        .orElse(""))
                .value("dose", () -> DeviceReadings.radiation().map(kind -> BodyScales.builtUp(kind,
                        DeviceReadings.level(kind), PALETTE.get().radiationLow(), PALETTE.get().radiationHigh()))
                        .orElse(null))
                .value("rate", () -> DeviceReadings.radiation().map(kind -> outside(kind) + onYou())
                        .filter(rate -> rate > TOLD_RATE).isPresent())
                .value("rate_line", () -> DeviceReadings.radiation().map(kind -> say(HazardScreenTexts.RATE_LINE
                        .with(DeviceReadings.figure(outside(kind) + onYou()) + " " + DeviceReadings.rateUnit(kind),
                                DeviceReadings.figure(outside(kind)), DeviceReadings.figure(onYou())))).orElse(""))
                .value("contaminated", () -> HazardsClient.level(HazardDevices.CONTAMINATION) > 0);
    }

    private static void wireLungs(final ScreenWiring<Void> screen) {
        screen.value("lungs_band", () -> Lungs.BLACK_LUNG.band(lungHarm()).map(band -> GameText.resolve(band.name()))
                        .orElseGet(() -> say(HazardScreenTexts.CLEAR)))
                .value("lung_line", BodyScreen::lungLine)
                .value("lungs", () -> BodyScales.builtUp(Lungs.BLACK_LUNG, lungHarm(), PALETTE.get().lungsLow(),
                        PALETTE.get().lungsHigh()));
    }

    private static void wireDissonance(final ScreenWiring<Void> screen) {
        screen.value("dissonance_on", () -> DeviceReadings.on(HazardDevices.DISSONANCE).isPresent())
                .value("dissonance_band", () -> DeviceReadings.on(HazardDevices.DISSONANCE).map(BodyScreen::bandOf)
                        .orElseGet(() -> say(HazardScreenTexts.NONE)))
                .value("dissonance_line", () -> DeviceReadings.on(HazardDevices.DISSONANCE)
                        .filter(kind -> DeviceReadings.level(kind) > 0)
                        .map(kind -> say(HazardScreenTexts.BUILT_UP.with(DeviceReadings.figure(DeviceReadings.level(
                                kind)) + " " + GameText.resolve(kind.unit()))))
                        .orElseGet(() -> say(HazardScreenTexts.NORMAL_LAWS)))
                .value("dissonance", () -> DeviceReadings.on(HazardDevices.DISSONANCE).map(kind -> BodyScales
                        .builtUp(kind, DeviceReadings.level(kind), PALETTE.get().dissonanceLow(),
                                PALETTE.get().dissonanceHigh())).orElse(null));
    }

    /* What is worn in {@code slot}. */
    private static ItemStack worn(final EquipmentSlot slot) {
        final LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.getItemBySlot(slot);
    }

    /* The worn thing named for all that is worn: what covers the chest, else the first worn. */
    private static ItemStack named() {
        final ItemStack chest = worn(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            return chest;
        }
        for (final EquipmentSlot slot : WORN) {
            if (!worn(slot).isEmpty()) {
                return worn(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    private static String wornName() {
        final ItemStack named = named();
        return named.isEmpty() ? say(HazardScreenTexts.NOTHING_WORN) : named.getHoverName().getString();
    }

    /* The mod the worn thing named comes from, by its name. */
    private static String wornFrom() {
        final ItemStack named = named();
        if (named.isEmpty()) {
            return "";
        }
        final String mod = BuiltInRegistries.ITEM.getKey(named.getItem()).getNamespace();
        return ModList.get().getModContainerById(mod).map(container -> container.getModInfo().getDisplayName())
                .orElse(mod);
    }

    private static String letsOut() {
        final float letsOut = warmth().letsOut();
        return Float.isFinite(letsOut) ? say(HazardScreenTexts.LETS_OUT_VALUE.with(GameLocale.numbers()
                .decimal(Math.round(letsOut * 100) / 100.0))) : say(HazardScreenTexts.BARE);
    }

    /* Whether the wind is kept off all that clothes can cover, off none of it, or off a share of the body. */
    private static String wind() {
        final float kept = warmth().windproof();
        if (kept >= Clothing.covered() - TOLD_SHARE) {
            return say(HazardScreenTexts.KEPT_OUT);
        }
        return kept < TOLD_SHARE ? say(HazardScreenTexts.GETS_IN)
                : say(HazardScreenTexts.PARTLY_KEPT_OUT.with(Math.round(kept * 100)));
    }

    /* What is worn stops of each kind of harm, the most first, leaving out what it hardly stops. */
    private static List<Map.Entry<DamageKind, Double>> stops() {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of();
        }
        final List<Map.Entry<DamageKind, Double>> out = new ArrayList<>();
        for (final Map.Entry<DamageKind, Double> stop : Resistances.worn(player).entrySet()) {
            if (stop.getValue() >= TOLD_SHARE) {
                out.add(stop);
            }
        }
        out.sort(Map.Entry.<DamageKind, Double>comparingByValue(Comparator.reverseOrder()));
        return out;
    }

    private static Optional<Map.Entry<DamageKind, Double>> stop(final int row) {
        final List<Map.Entry<DamageKind, Double>> stops = stops();
        return row < stops.size() ? Optional.of(stops.get(row)) : Optional.empty();
    }

    /* The band of the cold or the heat the core is in, or that it is normal. */
    private static String temperatureBand() {
        for (final HazardKind kind : List.of(BodyTemperature.HYPOTHERMIA, BodyTemperature.OVERHEATING)) {
            final Optional<HazardBand> band = kind.band(HazardsClient.level(kind.id()));
            if (band.isPresent()) {
                return GameText.resolve(band.get().name());
            }
        }
        return say(HazardScreenTexts.NORMAL);
    }

    private static String coreLine() {
        final BodyTemperaturePayload now = warmth();
        final String trend = Math.abs(now.perMinute()) < TOLD_CHANGE ? say(HazardScreenTexts.STEADY)
                : say((now.perMinute() < 0 ? HazardScreenTexts.FALLING : HazardScreenTexts.RISING)
                        .with(degrees(Math.abs(now.perMinute()))));
        return say(HazardScreenTexts.CORE_LINE.with(degrees(BodyScales.celsius(now.coreKelvin())), trend));
    }

    /* The place's air, wind and sun, what the body makes, and what it loses or the place gives it. */
    private static String placeLine() {
        final BodyTemperaturePayload now = warmth();
        final UnitFormatter numbers = GameLocale.numbers();
        final String air = GameText.resolve(BodyTemperatureHud.CORE.with(numbers.decimal(Math.round(
                BodyScales.celsius(now.airKelvin())))));
        final String wind = now.wind() < TOLD_WIND ? say(HazardScreenTexts.NO_WIND)
                : say(HazardScreenTexts.A_WIND.with(numbers.decimal(Math.round(now.wind()))));
        final String sun = now.sun() < TOLD_SUN ? say(HazardScreenTexts.NO_SUN)
                : say(HazardScreenTexts.SUN.with(numbers.decimal(Math.round(now.sun()))));
        final String made = say(HazardScreenTexts.WATTS.with(numbers.decimal(Math.round(now.made()))));
        final String lost = say(HazardScreenTexts.WATTS.with(numbers.decimal(Math.round(Math.abs(now.lost())))));
        return say((now.lost() < 0 ? HazardScreenTexts.PLACE_GIVES : HazardScreenTexts.PLACE_LOSES)
                .with(air, wind, sun, made, lost));
    }

    private static double lungHarm() {
        double harm = 0;
        for (final HazardKind kind : Lungs.kinds()) {
            harm += HazardsClient.level(kind.id());
        }
        return Math.min(LUNGS_MOST, harm);
    }

    /* The lungs' harm in all, and each dust's share of it. */
    private static String lungLine() {
        final List<String> shares = new ArrayList<>();
        for (final HazardKind kind : Lungs.kinds()) {
            final double level = HazardsClient.level(kind.id());
            if (Math.round(level) > 0) {
                shares.add(say(HazardScreenTexts.LUNG_SHARE.with(GameText.resolve(kind.name()), percent(level))));
            }
        }
        final String harm = percent(lungHarm());
        return shares.isEmpty() ? say(HazardScreenTexts.LUNG_LINE.with(harm))
                : say(HazardScreenTexts.LUNG_SHARES.with(harm, String.join(", ", shares)));
    }

    /* The other hazards built up in the body, each with its band or how much there is, or that there is none. */
    private static String others() {
        final Set<ResourceLocation> shownAbove = Set.of(HazardDevices.RADIATION, HazardDevices.CONTAMINATION,
                HazardDevices.DISSONANCE, BodyTemperature.HYPOTHERMIA.id(), BodyTemperature.OVERHEATING.id());
        final List<String> others = new ArrayList<>();
        for (final HazardKind kind : Hazards.all()) {
            final double level = HazardsClient.level(kind.id());
            if (shownAbove.contains(kind.id()) || Lungs.kinds().contains(kind) || level <= 0) {
                continue;
            }
            final String how = kind.band(level).map(band -> GameText.resolve(band.name()))
                    .orElseGet(() -> DeviceReadings.figure(level) + " " + GameText.resolve(kind.unit()));
            others.add(say(HazardScreenTexts.OTHER_ONE.with(GameText.resolve(kind.name()), how)));
        }
        return others.isEmpty() ? say(HazardScreenTexts.OTHER_NONE)
                : say(HazardScreenTexts.OTHER.with(String.join(", ", others)));
    }

    private static String bandOf(final HazardKind kind) {
        return kind.band(DeviceReadings.level(kind)).map(band -> GameText.resolve(band.name()))
                .orElseGet(() -> say(HazardScreenTexts.NONE));
    }

    /* How fast the series' radiation comes from outside where the player stands, and from what is on them. */
    private static double outside(final HazardKind kind) {
        return DeviceReadings.rateHere(kind);
    }

    private static double onYou() {
        return IonizingRadiation.contaminationRate(HazardsClient.level(HazardDevices.CONTAMINATION));
    }

    private static BodyTemperaturePayload warmth() {
        return BodyTemperatureClient.now();
    }

    private static String degrees(final double celsius) {
        return GameText.resolve(BodyTemperatureHud.CORE.with(GameLocale.numbers()
                .decimal(Math.round(celsius * 10) / 10.0)));
    }

    private static String percent(final double value) {
        return say(HazardScreenTexts.SHARE.with(Math.round(value)));
    }

    private static String say(final TextKey key) {
        return GameText.resolve(key);
    }

    private static String say(final Text said) {
        return GameText.resolve(said);
    }

    /**
     * The colours of the body's screen's scales.
     *
     * @param deepCold       the core's scale below severe hypothermia
     * @param cold           its band of moderate hypothermia
     * @param cool           its band of mild hypothermia
     * @param normal         its normal band
     * @param warm           its band of heat exhaustion
     * @param hot            its band of heatstroke
     * @param pointer        the pointer at the core
     * @param radiationLow   the dose's scale at nothing
     * @param radiationHigh  the dose's scale at its end
     * @param lungsLow       the lungs' scale at nothing
     * @param lungsHigh      the lungs' scale at its end
     * @param dissonanceLow  dissonance's scale at nothing
     * @param dissonanceHigh dissonance's scale at its end
     */
    public record Colours(int deepCold, int cold, int cool, int normal, int warm, int hot, int pointer,
                          int radiationLow, int radiationHigh, int lungsLow, int lungsHigh, int dissonanceLow,
                          int dissonanceHigh) {
    }
}
