/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.JsCore;
import dev.jstech.core.dimension.Climate;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Rooms shown to the players who see them, and the air each player breathes, sent to their game every second. A player
 * holding or wearing something of {@link #SEES_ROOMS} (goggles) sees the room they are in, or the one they look at;
 * an Atmosphere Detector shows its room to whoever asks it to. What a player's game holds of both is read on their
 * side through {@link #seen} and {@link #airHere}.
 */
public final class RoomViews {

    /** The items that let a player see rooms: held in a hand, or worn on the head. */
    public static final TagKey<Item> SEES_ROOMS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "sees_rooms"));
    /** How far a player's look finds a room, in blocks. */
    public static final double LOOK_REACH = 24.0;
    /** How far from a detector its room stays shown, in blocks. */
    public static final double SHOW_REACH = 64.0;
    private static final String VERSION = "1";
    private static final double LOOK_STEP = 0.5;
    /* The ticks a view stays on a player's game after it was last sent: two of the seconds it is sent in. */
    private static final long FRESH = 50;
    /* The detectors showing their rooms, by the player each shows its room to. */
    private static final Map<UUID, Shown> SHOWN = new HashMap<>();
    /* The players who were last sent a room, to be sent none once they no longer see one. */
    private static final Set<UUID> SEEING = new HashSet<>();
    private static volatile RoomView seenOnClient = RoomView.NONE;
    private static volatile long seenAt;
    private static volatile Here hereOnClient = Here.OPEN_AIR;

    private RoomViews() {
    }

    /** Hands the mod's bus the payloads, once, from the Core's constructor. */
    static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(ViewPayload.TYPE, ViewPayload.STREAM_CODEC, RoomViews::onView)
                .playToClient(HerePayload.TYPE, HerePayload.STREAM_CODEC, RoomViews::onHere));
    }

    /** Whether {@code player} sees rooms: an item of the tag in a hand or on the head. */
    public static boolean sees(final Player player) {
        return player.getMainHandItem().is(SEES_ROOMS) || player.getOffhandItem().is(SEES_ROOMS)
                || player.getItemBySlot(EquipmentSlot.HEAD).is(SEES_ROOMS);
    }

    /** Has the detector at {@code detector} show its room to {@code player}, or stop showing it. */
    public static void show(final ServerPlayer player, final BlockPos detector, final boolean on) {
        if (on) {
            SHOWN.put(player.getUUID(), new Shown(player.level().dimension(), detector.immutable()));
        } else {
            SHOWN.remove(player.getUUID());
        }
    }

    /** Whether the detector at {@code detector} shows its room to {@code player}. */
    public static boolean showing(final ServerPlayer player, final BlockPos detector) {
        final Shown shown = SHOWN.get(player.getUUID());
        return shown != null && shown.detector().equals(detector) && shown.dimension() == player.level().dimension();
    }

    /** The room a player's game shows now, as last sent; none once it is two seconds old. */
    public static RoomView seen(final long gameTime) {
        return gameTime - seenAt > FRESH ? RoomView.NONE : seenOnClient;
    }

    /** The air the player breathes now, on their game, as last sent. */
    public static Here airHere() {
        return hereOnClient;
    }

    /** What a room of {@code level} shows of itself, as seen by {@code source}. */
    public static RoomView viewOf(final ServerLevel level, final RoomStore store, final Room room,
                                  final RoomView.Source source) {
        final List<RoomView.Leak> leaks = new ArrayList<>();
        final Set<BlockPos> named = new HashSet<>();
        for (final RoomFace face : room.faces()) {
            if (leaks.size() >= RoomView.MOST_LEAKS) {
                break;
            }
            if (face.area() < GasMath.NOTICEABLE || store.roomAt(face.beyond().asLong()) == room
                    || !named.add(face.block())) {
                continue;
            }
            leaks.add(new RoomView.Leak(face.block(), face.kind() == RoomFace.Kind.HOLE,
                    face.kind() == RoomFace.Kind.DOOR, face.side(),
                    BuiltInRegistries.BLOCK.getKey(level.getBlockState(face.block()).getBlock())));
        }
        final RoomAir air = room.air();
        final Map<ResourceLocation, Double> shares = new HashMap<>();
        for (final ResourceLocation gas : air.moles().keySet()) {
            shares.put(gas, air.share(gas));
        }
        return new RoomView(room.id(), room.outline(), leaks, room.volume(), air.pressure(), room.change(), shares,
                source);
    }

    /** A second of sending every player of a dimension the room they see and the air they breathe. */
    static void second(final LevelRooms rooms) {
        final ServerLevel level = rooms.level();
        for (final ServerPlayer player : level.players()) {
            final Climate here = SealedRooms.climateAt(level, BlockPos.containing(player.getEyePosition()));
            PacketDistributor.sendToPlayer(player, new HerePayload(new Here(here.pressure(), here.breathable(),
                    SealedRooms.roomAt(level, BlockPos.containing(player.getEyePosition())).isPresent())));
            final RoomStore store = rooms.store();
            final Room room = store == null ? null : shownTo(level, store, player);
            if (room != null) {
                final RoomView.Source source = SHOWN.containsKey(player.getUUID()) ? RoomView.Source.DETECTOR
                        : RoomView.Source.GOGGLES;
                PacketDistributor.sendToPlayer(player, new ViewPayload(viewOf(level, store, room, source)));
                SEEING.add(player.getUUID());
            } else if (SEEING.remove(player.getUUID())) {
                PacketDistributor.sendToPlayer(player, new ViewPayload(RoomView.NONE));
            }
        }
    }

    /* The room a player sees: a detector's they asked to see, else, through goggles, theirs or the one looked at. */
    private static @Nullable Room shownTo(final ServerLevel level, final RoomStore store, final ServerPlayer player) {
        final Shown shown = SHOWN.get(player.getUUID());
        if (shown != null) {
            if (shown.dimension() != level.dimension()
                    || shown.detector().distToCenterSqr(player.position()) > SHOW_REACH * SHOW_REACH) {
                SHOWN.remove(player.getUUID());
            } else {
                return SealedRooms.roomBeside(level, shown.detector()).orElse(null);
            }
        }
        if (!sees(player)) {
            return null;
        }
        final Vec3 eye = player.getEyePosition();
        final Vec3 look = player.getLookAngle();
        for (double away = 0; away <= LOOK_REACH; away += LOOK_STEP) {
            final Room room = store.roomAt(BlockPos.containing(eye.add(look.scale(away))).asLong());
            if (room != null) {
                return room;
            }
        }
        return null;
    }

    private static void onView(final ViewPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            seenOnClient = payload.view();
            final Level level = context.player().level();
            seenAt = level.getGameTime();
        });
    }

    private static void onHere(final HerePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> hereOnClient = payload.here());
    }

    /*
     * A detector showing its room to a player.
     */
    private record Shown(ResourceKey<Level> dimension, BlockPos detector) {
    }

    /**
     * The air where a player breathes, as their game knows it.
     *
     * @param pressure   its pressure, in kilopascals
     * @param breathable whether it can be breathed
     * @param room       whether a sealed room holds it
     */
    public record Here(double pressure, boolean breathable, boolean room) {

        /** The overworld's open air, until the server says otherwise. */
        public static final Here OPEN_AIR = new Here(101.3, true, false);
        static final StreamCodec<ByteBuf, Here> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Here::pressure, ByteBufCodecs.BOOL, Here::breathable, ByteBufCodecs.BOOL,
                Here::room, Here::new);
    }

    /** The room a player sees, sent to their game. */
    record ViewPayload(RoomView view) implements CustomPacketPayload {

        static final Type<ViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                "room_view"));
        static final StreamCodec<ByteBuf, ViewPayload> STREAM_CODEC = RoomView.STREAM_CODEC.map(ViewPayload::new,
                ViewPayload::view);

        @Override
        public Type<ViewPayload> type() {
            return TYPE;
        }
    }

    /** The air a player breathes, sent to their game. */
    record HerePayload(Here here) implements CustomPacketPayload {

        static final Type<HerePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                "air_here"));
        static final StreamCodec<ByteBuf, HerePayload> STREAM_CODEC = Here.STREAM_CODEC.map(HerePayload::new,
                HerePayload::here);

        @Override
        public Type<HerePayload> type() {
            return TYPE;
        }
    }
}
