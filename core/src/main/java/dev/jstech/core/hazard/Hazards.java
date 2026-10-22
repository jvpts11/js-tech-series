/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.client.hazard.HazardsClient;
import dev.jstech.core.registry.CoreAttachments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The hazards at work: every kind any mod declares, and what has built up of each in every living thing. Once a second
 * each body takes from every source of each hazard, what it wears taking its share off by the kind the hazard is
 * protected by, and recovers from each away from its sources; then each band it stands in does what it does. Players
 * always; creatures only for the kinds they suffer, near a player, and when the server lets them. A player is told
 * their own hazards each second something changed, for the instruments in their hands and the body's screen.
 */
public final class Hazards {

    private static final Map<ResourceLocation, HazardKind> DECLARED = new LinkedHashMap<>();
    private static final String VERSION = "1";
    private static final int TICKS_A_SECOND = 20;
    /* How near a player a creature must be for its hazards to be weighed, in blocks. */
    private static final double NEAR_A_PLAYER = 64;
    /* What each player was told last. */
    private static final Map<ServerPlayer, HazardsPayload> TOLD = new WeakHashMap<>();
    /* What mods work out of each body every second, before its hazards are weighed. */
    private static final List<IBodySecond> SECONDS = new CopyOnWriteArrayList<>();
    /* Every kind, as a list read every tick without a lock; set again as each is declared. */
    private static volatile List<HazardKind> declared = List.of();

    private Hazards() {
    }

    /** Declares {@code kind}, from a mod's constructor; its texts are said beside it, in a class of texts. */
    public static HazardKind declare(final HazardKind kind) {
        synchronized (DECLARED) {
            if (DECLARED.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException("the hazard " + kind.id() + " is declared twice");
            }
            declared = List.copyOf(DECLARED.values());
        }
        return kind;
    }

    /** The kind {@code id}, if one is declared. */
    public static Optional<HazardKind> find(final ResourceLocation id) {
        synchronized (DECLARED) {
            return Optional.ofNullable(DECLARED.get(id));
        }
    }

    /** Every kind declared, in the order they were. */
    public static Collection<HazardKind> all() {
        return declared;
    }

    /**
     * Has {@code step} work out what it keeps of each body every second, before the body's hazards are weighed, so a
     * kind measured from it ({@link HazardKind.Builder#gauged}) reads it fresh; from a mod's constructor.
     */
    public static void everySecond(final IBodySecond step) {
        SECONDS.add(Objects.requireNonNull(step, "step"));
    }

    /** Hands the buses the payload and what weighs the hazards, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(HazardsPayload.TYPE, HazardsPayload.STREAM_CODEC, Hazards::onHazards));
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, Hazards::onEntityTick);
    }

    /** What has built up of {@code kind} in {@code body}. */
    public static double level(final LivingEntity body, final HazardKind kind) {
        return body.hasData(CoreAttachments.HAZARDS) ? body.getData(CoreAttachments.HAZARDS).level(kind.id()) : 0;
    }

    /** How fast {@code kind} built up in {@code body} in the last second, what it wears having taken its share. */
    public static double rate(final LivingEntity body, final HazardKind kind) {
        return body.hasData(CoreAttachments.HAZARDS) ? body.getData(CoreAttachments.HAZARDS).rate(kind.id()) : 0;
    }

    /** How fast {@code kind} came at {@code body} in the last second, before what it wears took its share. */
    public static double exposure(final LivingEntity body, final HazardKind kind) {
        return body.hasData(CoreAttachments.HAZARDS) ? body.getData(CoreAttachments.HAZARDS).exposure(kind.id())
                : 0;
    }

    /** The band of {@code kind} {@code body} stands in, if any. */
    public static Optional<HazardBand> band(final LivingEntity body, final HazardKind kind) {
        return kind.band(level(body, kind));
    }

    /** Adds {@code amount} of {@code kind} to {@code body} at once: a sudden dose, a splash; never past its most. */
    public static void add(final LivingEntity body, final HazardKind kind, final double amount) {
        set(body, kind, level(body, kind) + amount);
    }

    /** Takes {@code amount} of {@code kind} from {@code body}: a medicine, a shower; never below nothing. */
    public static void treat(final LivingEntity body, final HazardKind kind, final double amount) {
        set(body, kind, level(body, kind) - amount);
    }

    /** Sets what has built up of {@code kind} in {@code body}, held between nothing and its most. */
    public static void set(final LivingEntity body, final HazardKind kind, final double level) {
        final double held = Math.clamp(level, 0, kind.most());
        if (held <= 0 && !body.hasData(CoreAttachments.HAZARDS)) {
            return;
        }
        body.getData(CoreAttachments.HAZARDS).set(kind.id(), held);
    }

    /**
     * A second of every hazard on {@code body}: what mods work out of it first, then what it takes, what it recovers,
     * or what a measured kind reads, and what its bands do.
     */
    public static void second(final LivingEntity body, final ServerLevel level) {
        for (final IBodySecond step : SECONDS) {
            step.second(body, level);
        }
        final boolean player = body instanceof Player;
        for (final HazardKind kind : all()) {
            if (!kind.enabled() || !player && !kind.affectsMobs()) {
                continue;
            }
            if (kind.gauge().isPresent()) {
                measure(body, level, kind, kind.gauge().get());
                continue;
            }
            double taken = 0;
            for (final IHazardSource source : kind.sources()) {
                taken += Math.max(0, source.rate(body, level));
            }
            final double before = level(body, kind);
            if (taken <= 0 && before <= 0) {
                if (body.hasData(CoreAttachments.HAZARDS)) {
                    body.getData(CoreAttachments.HAZARDS).rate(kind.id(), 0, 0);
                }
                continue;
            }
            final double stopped = kind.protection().map(protection -> Resistances.total(body, protection)).orElse(0.0);
            // What comes no faster than the background every body lives in builds up nothing.
            final double rate = taken > kind.background() ? taken * (1 - stopped) : 0;
            final double now = rate > 0 ? before + rate
                    : before - Math.min(before, kind.recovered(before) * (1 + kind.faster(body, level)));
            set(body, kind, now);
            body.getData(CoreAttachments.HAZARDS).rate(kind.id(), rate, taken);
            kind.band(level(body, kind)).ifPresent(band -> band.effects().forEach(effect ->
                    effect.second(body, kind, level(body, kind))));
        }
    }

    /** What {@code player} would be told of their hazards now. */
    public static HazardsPayload payloadOf(final ServerPlayer player) {
        final List<HazardsPayload.Entry> entries = new ArrayList<>();
        if (player.hasData(CoreAttachments.HAZARDS)) {
            final HazardLevels levels = player.getData(CoreAttachments.HAZARDS);
            for (final HazardKind kind : all()) {
                final double level = levels.level(kind.id());
                final double rate = levels.rate(kind.id());
                final double exposure = levels.exposure(kind.id());
                if (level > 0 || rate > 0 || exposure > 0) {
                    entries.add(new HazardsPayload.Entry(kind.id(), (float) level, (float) rate, (float) exposure));
                }
            }
        }
        return new HazardsPayload(entries);
    }

    /*
     * A measured kind: it stands where {@code gauge} reads, how fast it got there is its rate, and its band does what
     * it does.
     */
    private static void measure(final LivingEntity body, final ServerLevel level, final HazardKind kind,
                                final IHazardGauge gauge) {
        final double before = level(body, kind);
        final double now = Math.max(0, gauge.level(body, level));
        if (now <= 0 && before <= 0) {
            if (body.hasData(CoreAttachments.HAZARDS)) {
                body.getData(CoreAttachments.HAZARDS).rate(kind.id(), 0, 0);
            }
            return;
        }
        set(body, kind, now);
        body.getData(CoreAttachments.HAZARDS).rate(kind.id(), now - before, now - before);
        kind.band(level(body, kind)).ifPresent(band -> band.effects().forEach(effect ->
                effect.second(body, kind, level(body, kind))));
    }

    private static void onEntityTick(final EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity body) || !(body.level() instanceof ServerLevel level)
                || !body.isAlive()) {
            return;
        }
        final boolean player = body instanceof Player;
        // Each body has its own second, so a crowd's are spread over the ticks; a creature is weighed only near a
        // player, and that is asked only on its second, so the many ticks between cost a creature nothing.
        if ((body.tickCount + body.getId()) % TICKS_A_SECOND == 0 && (player || HazardBalance.mobsSuffer()
                && level.hasNearbyAlivePlayer(body.getX(), body.getY(), body.getZ(), NEAR_A_PLAYER))) {
            second(body, level);
            if (body instanceof ServerPlayer server) {
                HazardMaps.second(server);
                if (server.connection != null) {
                    tell(server);
                }
            }
        }
        if (!body.hasData(CoreAttachments.HAZARDS) || body.getData(CoreAttachments.HAZARDS).clear()) {
            return;
        }
        for (final HazardKind kind : declared) {
            final double built = level(body, kind);
            if (built > 0 && kind.enabled() && (player || kind.affectsMobs())) {
                kind.band(built).ifPresent(band -> band.effects().forEach(effect -> effect.tick(body, kind,
                        built)));
            }
        }
    }

    private static void tell(final ServerPlayer player) {
        final HazardsPayload now = payloadOf(player);
        final HazardsPayload told = TOLD.get(player);
        if (told == null ? !now.hazards().isEmpty() : !told.equals(now)) {
            TOLD.put(player, now);
            PacketDistributor.sendToPlayer(player, now);
        }
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onHazards(final HazardsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> HazardsClient.told(payload));
    }
}
