/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.worn.WornItems;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The server's part of how sound travels: it tells each player, once a second when it changed, the air round their
 * head (a sealed room's, with its size, or the place's), which only the server knows; a blast close by leaves the
 * ears of those near it ringing, unless they wear something that protects them; and a blow striking a living thing
 * raises the impact cue, which each player's packs sound by the kind of thing struck.
 */
public final class Acoustics {

    /** What protects the ears when worn: ear defenders, a helmet that closes over them; a mod puts its own here. */
    public static final TagKey<Item> PROTECTS_EARS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "protects_ears"));
    private static final int TICKS_A_SECOND = 20;
    /* The damage of a blow heard at its loudest, and how soft the softest is heard. */
    private static final float FULL_BLOW = 10F;
    private static final float QUIETEST_BLOW = 0.3F;
    /* What each player was told last, so they are told again only when it changes. */
    private static final Map<ServerPlayer, ListenerAirPayload> TOLD = new WeakHashMap<>();

    private Acoustics() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, Acoustics::onServerTick);
        NeoForge.EVENT_BUS.addListener(ExplosionEvent.Detonate.class, Acoustics::onDetonate);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, Acoustics::onDamage);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> forget(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class,
                event -> forget(event.getEntity()));
    }

    /** The air round {@code player}'s head, as they are told it: their sealed room's and its size, or the place's. */
    public static ListenerAirPayload airOf(final ServerPlayer player) {
        final ServerLevel level = player.serverLevel();
        final Vec3 eyes = player.getEyePosition();
        final Optional<Room> room = SealedRooms.roomAt(level, BlockPos.containing(eyes));
        if (room.isPresent()) {
            final Climate inside = room.get().air().climate();
            return new ListenerAirPayload((float) inside.pressure(), (float) inside.temperature(),
                    (float) room.get().gasVolume(), room.get().faces().size());
        }
        final Climate place = SealedRooms.placeClimate(level, eyes);
        return new ListenerAirPayload((float) place.pressure(), (float) place.temperature(), 0F, 0F);
    }

    /** How a blast of {@code power} at {@code centre} leaves {@code player}'s ears. */
    public static EarRing ringOf(final ServerPlayer player, final Vec3 centre, final double power) {
        final boolean protectedEars = WornItems.wears(player, stack -> stack.is(PROTECTS_EARS));
        return EarRing.from(power, player.getEyePosition().distanceTo(centre), protectedEars);
    }

    /** What a blow striking {@code struck} sounds by: the kind of thing it is. */
    public static SoundContext impactOf(final Entity struck) {
        return SoundContext.EMPTY.with(SoundContext.ENTITY,
                BuiltInRegistries.ENTITY_TYPE.getKey(struck.getType()).toString());
    }

    private static void onServerTick(final ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % TICKS_A_SECOND != 0) {
            return;
        }
        for (final ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            // A player with no connection, as a test's or another mod's stand-in, has no game to tell.
            if (player.connection == null) {
                continue;
            }
            final ListenerAirPayload air = airOf(player);
            final ListenerAirPayload told = TOLD.get(player);
            if (told == null || !told.same(air)) {
                TOLD.put(player, air);
                PacketDistributor.sendToPlayer(player, air);
            }
        }
    }

    /* A blast of the Core's own rings each player's ears as its wave reaches them, not here. */
    private static void onDetonate(final ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || Explosions.isBlast(event.getExplosion())) {
            return;
        }
        final Vec3 centre = event.getExplosion().center();
        final double power = event.getExplosion().radius();
        for (final ServerPlayer player : level.players()) {
            final EarRing ring = ringOf(player, centre, power);
            if (ring.rings() && player.connection != null) {
                PacketDistributor.sendToPlayer(player, new EarRingPayload((float) ring.strength(), ring.ticks()));
            }
        }
    }

    /* A blow or a shot struck a living thing: it sounds as what it struck is made of, as loud as it hurt. */
    private static void onDamage(final LivingDamageEvent.Post event) {
        final LivingEntity struck = event.getEntity();
        if (!(struck.level() instanceof ServerLevel level) || event.getSource().getDirectEntity() == null
                || BuiltInRegistries.ENTITY_TYPE.getKey(struck.getType()).toString().length()
                > SoundContext.MAX_LENGTH) {
            return;
        }
        Audio.cue(level, struck.getX(), struck.getY(0.5), struck.getZ(), AcousticCues.IMPACT, impactOf(struck),
                Math.clamp(event.getNewDamage() / FULL_BLOW, QUIETEST_BLOW, 1F), 1F);
    }

    private static void forget(final Player player) {
        if (player instanceof ServerPlayer server) {
            TOLD.remove(server);
        }
    }
}
