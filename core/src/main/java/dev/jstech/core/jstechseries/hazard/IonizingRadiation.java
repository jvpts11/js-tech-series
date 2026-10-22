/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.DamageKind;
import dev.jstech.core.hazard.DamageKinds;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.hazard.HazardEffects;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.IHazardEffect;
import dev.jstech.core.hazard.ItemHazards;
import dev.jstech.core.hazard.Radiation;
import dev.jstech.core.hazard.RadiationKind;
import dev.jstech.core.hazard.Withering;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The series' ionizing radiation, in real units. Each place has a dose rate, in millisieverts an hour, and a body
 * builds up a dose, in sieverts, through two channels: the external dose, from every source round it through the blocks
 * between and from what it carries, which only distance, time and shielding reduce, and of which worn shielding stops
 * its share; and contamination, radioactive dust on the skin, in kilobecquerels, which keeps dosing until water washes
 * it off and which a suit stops from settling. The dose brings the bands of the acute radiation syndrome, Rad I at 1
 * sievert to Rad V at 8, and the body recovers slowly away from the sources. The sky, the minerals with uranium or
 * thorium in them and carried radioactive things give it off; any mod's blocks give it off through
 * {@link Radiation#emit}. Where the dose rate passes a hundred millisieverts an hour, the land withers. The whole of it
 * is turned off with the world's radiation setting.
 */
@TextHolder
public final class IonizingRadiation {

    public static final TextKey NAME = TextKey.of("jscore.hazard.ionizing_radiation", "Ionizing radiation");
    public static final TextKey SHORT = TextKey.of("jscore.hazard.ionizing_radiation.short", "RAD");
    public static final TextKey SIEVERTS = TextKey.of("jscore.hazard.ionizing_radiation.unit", "Sv");
    public static final TextKey PER_HOUR = TextKey.of("jscore.hazard.ionizing_radiation.rate_unit", "mSv/h");
    public static final TextKey RAD_I = TextKey.of("jscore.hazard.ionizing_radiation.rad1", "Rad I");
    public static final TextKey RAD_II = TextKey.of("jscore.hazard.ionizing_radiation.rad2", "Rad II");
    public static final TextKey RAD_III = TextKey.of("jscore.hazard.ionizing_radiation.rad3", "Rad III");
    public static final TextKey RAD_IV = TextKey.of("jscore.hazard.ionizing_radiation.rad4", "Rad IV");
    public static final TextKey RAD_V = TextKey.of("jscore.hazard.ionizing_radiation.rad5", "Rad V");
    public static final TextKey TREATMENT = TextKey.of("jscore.hazard.ionizing_radiation.treatment",
            "Rest away from the sources; take potassium iodide before radioactive iodine, Prussian blue for caesium");
    public static final TextKey GAMMA = TextKey.of("jscore.radiation.ionizing", "Gamma rays");
    public static final TextKey CONTAMINATION_NAME = TextKey.of("jscore.hazard.radioactive_contamination",
            "Radioactive contamination");
    public static final TextKey CONTAMINATION_SHORT = TextKey.of("jscore.hazard.radioactive_contamination.short",
            "CONTAM");
    public static final TextKey KILOBECQUERELS = TextKey.of("jscore.hazard.radioactive_contamination.unit", "kBq");
    public static final TextKey CONTAMINATED = TextKey.of("jscore.hazard.radioactive_contamination.band",
            "Contaminated");
    public static final TextKey WASH = TextKey.of("jscore.hazard.radioactive_contamination.treatment",
            "Wash it off: stand in water or the rain, or under a shower");
    public static final TextKey CONTAMINATION_GUARD = TextKey.of("jscore.damage_kind.contamination", "Contamination");
    public static final TextKey DIED = TextKey.of("death.attack.jscore.radiation_sickness",
            "%1$s died of radiation sickness");
    public static final TextKey DIED_FIGHTING = TextKey.of("death.attack.jscore.radiation_sickness.player",
            "%1$s died of radiation sickness while fighting %2$s");

    /** A dose rate in sieverts a second, as millisieverts an hour. */
    public static final double RATE_FACTOR = 3_600_000;
    /** The dose rate a kilobecquerel of contamination on the body gives it, in millisieverts an hour. */
    public static final double PER_KILOBECQUEREL = 0.01;
    /**
     * The background every body lives in, in millisieverts an hour: the sky's and the rock's, which give no dose worth
     * counting and through which the body goes on recovering.
     */
    public static final double BACKGROUND = 0.1;
    /** Where the dose rate withers the land, in millisieverts an hour. */
    public static final double WITHERS_AT = 100;
    /** What radiation sickness kills as, its own death message. */
    public static final ResourceKey<DamageType> SICKNESS = ResourceKey.create(Registries.DAMAGE_TYPE,
            id("radiation_sickness"));
    /** What a suit and a respirator stop: the dust that settles on the body and is breathed in. */
    public static final DamageKind CONTAMINATION_PROTECTION = DamageKinds.declare(new DamageKind(id("contamination"),
            CONTAMINATION_GUARD));
    /* How fast the body recovers from its dose away from the sources, in sieverts a second. */
    private static final double RECOVERY = 0.0005;
    /* What share of its contamination a body sheds by itself in a second, and how much faster water washes it. */
    private static final double SHED = 0.0005;
    private static final double WASHED = 400;
    /* How much contamination a carried radioactive thing leaves on its carrier a second, for each millisievert an
     * hour it gives off. */
    private static final double DUST = 0.05;
    /* How far a source is counted from, in blocks, and how much each kilogram a cubic metre of a block weakens it:
     * about five centimetres of the block's material for each block, so lead stops nearly all, water some. */
    private static final int REACH = 32;
    private static final double ATTENUATION = 0.0003;
    private static final double BLOCK_METRES = 0.05;

    /** The dose taken, in sieverts; its rate shown in millisieverts an hour. */
    public static final HazardKind DOSE = Hazards.declare(HazardKind.builder(HazardDevices.RADIATION, NAME, SIEVERTS)
            .band(1, RAD_I, HazardEffects.effect(MobEffects.HUNGER, 0))
            .band(2, RAD_II, HazardEffects.effect(MobEffects.HUNGER, 0),
                    HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 0))
            .band(4, RAD_III, HazardEffects.effect(MobEffects.HUNGER, 1), HazardEffects.effect(MobEffects.WEAKNESS, 0),
                    HazardEffects.damage(0.25F))
            .band(6, RAD_IV, HazardEffects.effect(MobEffects.HUNGER, 1), HazardEffects.effect(MobEffects.WEAKNESS, 1),
                    HazardEffects.effect(MobEffects.CONFUSION, 0), HazardEffects.damage(1))
            .band(8, RAD_V, HazardEffects.effect(MobEffects.WEAKNESS, 1), HazardEffects.effect(MobEffects.WITHER, 0),
                    HazardEffects.damage(2))
            .recovers(RECOVERY).protectedBy(DamageKinds.RADIATION).treatedBy(TREATMENT).deadlyAs(SICKNESS)
            .enabledWhen(HazardBalance::radiation).affectsMobs().upTo(20).shortName(SHORT)
            .rateShownIn(PER_HOUR, RATE_FACTOR).background(BACKGROUND / RATE_FACTOR).mapped(0.1, 1, 10, 100)
            .build());
    /** The radioactive dust on the body, in kilobecquerels, which keeps dosing until it is washed off. */
    public static final HazardKind CONTAMINATION = Hazards.declare(HazardKind.builder(HazardDevices.CONTAMINATION,
                    CONTAMINATION_NAME, KILOBECQUERELS)
            .band(0, CONTAMINATED, doses())
            .recoversShare(SHED).protectedBy(CONTAMINATION_PROTECTION).treatedBy(WASH)
            .enabledWhen(HazardBalance::radiation).affectsMobs().upTo(1_000_000).shortName(CONTAMINATION_SHORT)
            .build());
    /** The rays that carry the external dose, from every source round a body. */
    public static final RadiationKind RAYS = Radiation.declare(new RadiationKind(id("ionizing"), GAMMA, DOSE,
            ATTENUATION, REACH, BLOCK_METRES));

    static {
        RAYS.addAmbient(CosmicRadiation::at);
        RAYS.addAmbient((level, at) -> NaturalRadiation.at(level, at, RAYS));
        RAYS.addCarried(IonizingRadiation::shining);
        CONTAMINATION.addSource((body, level) -> carried(body) * DUST);
        CONTAMINATION.addRecovery((body, level) -> body.isInWaterOrRain() ? WASHED : 0);
        Withering.declare((level, pos) -> HazardBalance.radiation()
                ? Radiation.rateAt(level, Vec3.atCenterOf(pos), RAYS) / WITHERS_AT : 0);
    }

    private IonizingRadiation() {
    }

    /** Declares the radiation, from the Core's constructor. */
    public static void declare() {
        NaturalRadiation.listen();
    }

    /** The dose rate {@code kilobecquerels} of contamination on a body gives it, in millisieverts an hour. */
    public static double contaminationRate(final double kilobecquerels) {
        return kilobecquerels * PER_KILOBECQUEREL;
    }

    /*
     * What the radioactive things {@code body} carries loose give off, in millisieverts an hour: their hazard's
     * strength; what settles on it as dust.
     */
    private static double carried(final LivingEntity body) {
        return body instanceof Player player ? ItemHazards.carried(player, ItemHazards.RADIOACTIVE) : 0;
    }

    /*
     * What reaches {@code body} of the rays of what it carries, in millisieverts an hour: what it carries loose, and
     * what its containers hold through their walls, a lead one stopping nearly all.
     */
    private static double shining(final LivingEntity body) {
        return body instanceof Player player ? ItemHazards.carriedWithin(player, ItemHazards.RADIOACTIVE,
                wall -> RAYS.through(wall.density(), wall.thickness())) : 0;
    }

    /* What contamination does each second: it doses the body as its own rate, which no worn shielding stops. */
    private static IHazardEffect doses() {
        return new IHazardEffect() {
            @Override
            public void second(final LivingEntity body, final HazardKind kind, final double level) {
                if (DOSE.enabled()) {
                    Hazards.add(body, DOSE, DOSE.perSecond(contaminationRate(level)));
                }
            }
        };
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
