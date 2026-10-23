/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.hazard.BodyTemperatureClient;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.worldgen.biome.BiomeClimates;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A player's warmth in the world, in kelvin: each second their core warms or cools ({@link BodyHeat}) by the place
 * (the air's temperature and pressure, the wind under the open sky, the sun, the blocks hot and cold near, the water
 * they are in, the rain on them), through what they wear ({@link Clothing}), and by what they do: standing, walking,
 * sprinting, swimming, swinging an arm. Past the edges of the normal warmth come the two kinds of the hazard kit,
 * measured from the core: hypothermia below 35 °C, mild, moderate below 32 °C and severe below 28 °C; overheating above
 * 37.5 °C, heat exhaustion, and heatstroke above 40 °C. A body that stays in a cold or a hot climate gets used to it
 * over hours of play, its bands moving up to three quarters of a degree toward it, and loses that away from it. A
 * player is born
 * with a normal core and keeps one in creative; the server can turn the whole of it off.
 */
@TextHolder
public final class BodyTemperature {

    public static final TextKey COLD_NAME = TextKey.of("jscore.hazard.hypothermia", "Hypothermia");
    public static final TextKey HOT_NAME = TextKey.of("jscore.hazard.overheating", "Overheating");
    public static final TextKey DEGREES = TextKey.of("jscore.hazard.degrees", "°C");
    public static final TextKey MILD_HYPOTHERMIA = TextKey.of("jscore.hazard.hypothermia.mild", "Mild hypothermia");
    public static final TextKey MODERATE_HYPOTHERMIA = TextKey.of("jscore.hazard.hypothermia.moderate",
            "Moderate hypothermia");
    public static final TextKey SEVERE_HYPOTHERMIA = TextKey.of("jscore.hazard.hypothermia.severe",
            "Severe hypothermia");
    public static final TextKey HEAT_EXHAUSTION = TextKey.of("jscore.hazard.overheating.exhaustion",
            "Heat exhaustion");
    public static final TextKey HEATSTROKE = TextKey.of("jscore.hazard.overheating.heatstroke", "Heatstroke");
    public static final TextKey COLD_TREATMENT = TextKey.of("jscore.hazard.hypothermia.treatment",
            "Warm the body slowly: out of the wind and the water, in dry clothes, near a fire");
    public static final TextKey HOT_TREATMENT = TextKey.of("jscore.hazard.overheating.treatment",
            "Cool the body: shade, water and rest, and less worn");
    public static final TextKey DIED_COLD = TextKey.of("death.attack.jscore.hypothermia", "%1$s froze to death");
    public static final TextKey DIED_COLD_FIGHTING = TextKey.of("death.attack.jscore.hypothermia.player",
            "%1$s froze to death while fighting %2$s");
    public static final TextKey DIED_HOT = TextKey.of("death.attack.jscore.heatstroke", "%1$s died of heatstroke");
    public static final TextKey DIED_HOT_FIGHTING = TextKey.of("death.attack.jscore.heatstroke.player",
            "%1$s died of heatstroke while fighting %2$s");
    /** What the cold kills as, and the heat, each its own death message. */
    public static final ResourceKey<DamageType> FROZE = damage("hypothermia");
    public static final ResourceKey<DamageType> OVERHEATED = damage("heatstroke");
    /** Where the bands start, in kelvin from the normal 37 °C: below it at 35, 32 and 28 °C, above at 37.5 and 40. */
    public static final double MILD = 2;
    public static final double MODERATE = 5;
    public static final double SEVERE = 9;
    public static final double EXHAUSTION = 0.5;
    public static final double STROKE = 3;
    /** What a body makes by what it does, in watts. */
    public static final double RESTING = 100;
    public static final double WALKING = 200;
    public static final double SPRINTING = 600;
    public static final double SWIMMING = 400;
    public static final double WORKING = 100;
    /** How far a body gets used to a climate at most, in kelvin, and how fast, in kelvin a second: in an hour. */
    public static final double MOST_USED_TO = 0.75;
    public static final double GETTING_USED = MOST_USED_TO / 3_600;
    /* Below this air a body gets used to the cold, above this to the heat, in kelvin: 5 and 30 °C. */
    private static final double COLD_CLIMATE = 278.15;
    private static final double HOT_CLIMATE = 303.15;
    /* The sunlight at noon under a clear sky, in watts a square metre, and the share of it rain takes. */
    private static final double SUNLIGHT = 1_000;
    private static final double RAIN_SHADE = 0.75;
    /* How far a body goes in a second to be walking, in blocks. */
    private static final double WALKED = 1.0;
    /* The core is held between these, in kelvin: no body lives on past them. */
    private static final double COLDEST = 283.15;
    private static final double HOTTEST = 318.15;
    private static final double MOST = 30;
    private static final String VERSION = "1";

    /* Where each body was at its last second, so a second's walk is known; touched on the server's thread only. */
    private static final Map<LivingEntity, Vec3> WHERE = new WeakHashMap<>();
    /* What each player was told last. */
    private static final Map<ServerPlayer, BodyTemperaturePayload> TOLD = new WeakHashMap<>();

    public static final HazardKind HYPOTHERMIA = Hazards.declare(HazardKind.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hypothermia"), COLD_NAME, DEGREES)
            .band(MILD, MILD_HYPOTHERMIA, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 0))
            .band(MODERATE, MODERATE_HYPOTHERMIA, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 1),
                    HazardEffects.effect(MobEffects.DIG_SLOWDOWN, 0), HazardEffects.effect(MobEffects.WEAKNESS, 0))
            .band(SEVERE, SEVERE_HYPOTHERMIA, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 2),
                    HazardEffects.effect(MobEffects.WEAKNESS, 1), HazardEffects.damage(0.5F))
            .gauged(BodyTemperature::coldLevel).treatedBy(COLD_TREATMENT).deadlyAs(FROZE)
            .enabledWhen(HazardBalance::bodyTemperature).upTo(MOST).build());
    public static final HazardKind OVERHEATING = Hazards.declare(HazardKind.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "overheating"), HOT_NAME, DEGREES)
            .band(EXHAUSTION, HEAT_EXHAUSTION, HazardEffects.effect(MobEffects.WEAKNESS, 0),
                    HazardEffects.effect(MobEffects.HUNGER, 0))
            .band(STROKE, HEATSTROKE, HazardEffects.effect(MobEffects.CONFUSION, 0),
                    HazardEffects.effect(MobEffects.WEAKNESS, 1), HazardEffects.damage(0.5F))
            .gauged(BodyTemperature::heatLevel).treatedBy(HOT_TREATMENT).deadlyAs(OVERHEATED)
            .enabledWhen(HazardBalance::bodyTemperature).upTo(MOST).build());

    private BodyTemperature() {
    }

    /** Hands the mod's bus the payload, and the hazards the second of a body's warmth, once, from the Core. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(BodyTemperaturePayload.TYPE, BodyTemperaturePayload.STREAM_CODEC,
                        BodyTemperature::onTold));
        Hazards.everySecond(BodyTemperature::second);
    }

    /** How warm {@code body} is: its core, and how far it is used to a climate. */
    public static BodyWarmth warmth(final LivingEntity body) {
        return body.hasData(CoreAttachments.BODY_WARMTH) ? body.getData(CoreAttachments.BODY_WARMTH)
                : BodyWarmth.NORMAL;
    }

    /** Sets {@code body}'s core to {@code kelvin}, as a fall into ice water or a command does. */
    public static void setCore(final LivingEntity body, final double kelvin) {
        body.setData(CoreAttachments.BODY_WARMTH, new BodyWarmth(Math.clamp(kelvin, COLDEST, HOTTEST),
                warmth(body).acclimation()));
    }

    /** Sets how far {@code body} is used to a climate, in kelvin: below none to the cold, above to the heat. */
    public static void setAcclimation(final LivingEntity body, final double kelvin) {
        body.setData(CoreAttachments.BODY_WARMTH, new BodyWarmth(warmth(body).coreKelvin(),
                Math.clamp(kelvin, -MOST_USED_TO, MOST_USED_TO)));
    }

    /** How far {@code body}'s core is below the normal 37 °C, less for a body used to the cold. */
    public static double coldLevel(final LivingEntity body, final ServerLevel level) {
        final BodyWarmth warmth = warmth(body);
        return Math.max(0, BodyHeat.NORMAL + Math.min(0, warmth.acclimation()) - warmth.coreKelvin());
    }

    /** How far {@code body}'s core is above the normal 37 °C, less for a body used to the heat. */
    public static double heatLevel(final LivingEntity body, final ServerLevel level) {
        final BodyWarmth warmth = warmth(body);
        return Math.max(0, warmth.coreKelvin() - BodyHeat.NORMAL - Math.max(0, warmth.acclimation()));
    }

    /** Where {@code body} is, as heat meets it. */
    public static BodyHeat.Place placeOf(final ServerLevel level, final LivingEntity body) {
        final BlockPos eyes = BlockPos.containing(body.getEyePosition());
        final double air = airKelvin(level, eyes);
        final boolean open = level.canSeeSky(eyes);
        final double wind = open ? Winds.at(level, body.getEyePosition()).speed() : 0;
        return new BodyHeat.Place(air, HeatClimate.pressure(level, eyes), wind, sun(level, open), BlockTemperatures
                .radiantKelvin(level, body, air), body.isInWater(), Math.max(BodyHeat.FREEZING, air),
                level.isRainingAt(eyes));
    }

    /**
     * How warm the air at {@code pos} is to a body, in kelvin: a sealed room's or a biome's own, where they have one;
     * in a dimension of the game's own rules, its biome's climate from the game's values, so a snowy plain is cold and
     * a desert warm; elsewhere its dimension's air.
     */
    public static double airKelvin(final Level level, final BlockPos pos) {
        final Optional<HeatClimate.Air> own = HeatClimate.own(level, pos);
        if (own.isPresent()) {
            return own.get().kelvin();
        }
        if (DimensionRulesData.of(level) == DimensionRules.OVERWORLD) {
            return HeatMath.kelvin(BiomeClimates.celsius(level, pos));
        }
        return HeatClimate.ambient(level, pos);
    }

    /* A second of {@code body}'s warmth: the place, what it wears and what it does, and the player told. */
    private static void second(final LivingEntity body, final ServerLevel level) {
        if (!(body instanceof Player player)) {
            return;
        }
        final Vec3 now = body.position();
        final Vec3 before = WHERE.put(body, now);
        if (!HazardBalance.bodyTemperature() || player.isCreative() || player.isSpectator()) {
            if (warmth(body).coreKelvin() != BodyHeat.NORMAL) {
                setCore(body, BodyHeat.NORMAL);
            }
            return;
        }
        final BodyWarmth was = warmth(body);
        final BodyHeat.Place place = placeOf(level, body);
        final BodyHeat.Clothes clothes = Clothing.worn(body);
        final boolean walked = before != null && Math.hypot(now.x - before.x, now.z - before.z) >= WALKED;
        final BodyHeat.Balance balance = BodyHeat.step(was.coreKelvin(), place, clothes, work(body, walked), 1);
        final BodyWarmth warmth = new BodyWarmth(Math.clamp(balance.coreKelvin(), COLDEST, HOTTEST),
                usedTo(was.acclimation(), place));
        body.setData(CoreAttachments.BODY_WARMTH, warmth);
        if (body instanceof ServerPlayer server && server.connection != null) {
            tell(server, new BodyTemperaturePayload((float) warmth.coreKelvin(), (float) balance.perMinute(),
                    (float) place.airKelvin(), (float) place.wind(), (float) place.sun(), (float) balance.made(),
                    (float) balance.lost(), balance.wet(), (float) balance.letsOut(), (float) clothes.windproof(),
                    (float) clothes.waterproof(), (float) warmth.acclimation()));
        }
    }

    /* What {@code body} makes by what it does, in watts: walking a second's {@code walked} or not. */
    private static double work(final LivingEntity body, final boolean walked) {
        final double doing = body.isSwimming() ? SWIMMING : body.isSprinting() ? SPRINTING
                : walked ? WALKING : RESTING;
        return body.swinging ? doing + WORKING : doing;
    }

    /* How far a body used {@code now} to a climate is after a second at {@code place}. */
    private static double usedTo(final double now, final BodyHeat.Place place) {
        final double air = place.inWater() ? place.waterKelvin() : place.airKelvin();
        final double toward = air < COLD_CLIMATE ? -MOST_USED_TO : air > HOT_CLIMATE ? MOST_USED_TO : 0;
        return now < toward ? Math.min(toward, now + GETTING_USED) : Math.max(toward, now - GETTING_USED);
    }

    /* The sunlight on a body under the open sky, by the sun's height and the rain. */
    private static double sun(final ServerLevel level, final boolean open) {
        if (!open || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
            return 0;
        }
        final double height = Math.cos(level.getSunAngle(1.0F));
        return SUNLIGHT * Math.max(0, height) * (1 - RAIN_SHADE * level.getRainLevel(1.0F));
    }

    private static void tell(final ServerPlayer player, final BodyTemperaturePayload now) {
        if (!now.equals(TOLD.get(player))) {
            TOLD.put(player, now);
            PacketDistributor.sendToPlayer(player, now);
        }
    }

    private static ResourceKey<DamageType> damage(final String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path));
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onTold(final BodyTemperaturePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> BodyTemperatureClient.told(payload));
    }
}
