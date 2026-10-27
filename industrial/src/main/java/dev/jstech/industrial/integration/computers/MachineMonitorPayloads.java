/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.industrial.JsIndustrial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * What the Machine Monitor asks of the server, from a computer's desktop: the alerts of every Industrial Controller
 * Computer on the computer's network, with what each alerted machine reads now, which it asks again every couple of
 * seconds while it shows; and an operator's acknowledging one alert, or all. Only a player at that computer's screen
 * asks. The answer waits in {@link #latest} for the window to read it.
 */
public final class MachineMonitorPayloads {

    /** The most alerts an answer carries, newest first. */
    public static final int MOST_ROWS = 256;
    /** The id of no alert: an acknowledgement of every alert of the network. */
    public static final long EVERY_ALERT = -1L;
    /* The longest name and card a row carries; longer ones are cut, as the wire refuses them. */
    private static final int MOST_LETTERS = 64;
    private static final StableNames<AlertKind> KINDS = StableNames.of(AlertKind.class);
    /* The latest answer for each computer, by where it stands; the window of that computer reads it. */
    private static final Map<BlockPos, Answer> LATEST = new ConcurrentHashMap<>();

    private MachineMonitorPayloads() {
    }

    /** Registers the payloads; the mod's listener on the event, through the guard. */
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        ComputerAccess.accept(registrar, Request.TYPE, Request.STREAM_CODEC, ComputerAccess.machine(Request::host),
                MachineMonitorPayloads::answer);
        ComputerAccess.accept(registrar, Acknowledge.TYPE, Acknowledge.STREAM_CODEC,
                ComputerAccess.machine(Acknowledge::host), MachineMonitorPayloads::acknowledge);
        registrar.playToClient(Answer.TYPE, Answer.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(MachineMonitorPayloads::received));
    }

    /** The latest answer for the computer at {@code host}, or null before any. */
    public static @Nullable Answer latest(final BlockPos host) {
        return LATEST.get(host);
    }

    /**
     * What the Machine Monitor of the computer at {@code host} shows: the alerts of every switched-on controller on
     * its network, newest first, with how many controllers there are and how many machines they read.
     */
    public static Answer collect(final ServerLevel level, final BlockPos host) {
        final List<Row> rows = new ArrayList<>();
        int controllers = 0;
        int machines = 0;
        for (final IndustrialControllerBlockEntity controller : controllersOf(level, host)) {
            if (!controller.isRunning()) {
                continue;
            }
            controllers++;
            machines += controller.machines().size();
            rows.addAll(controller.monitorRows(level));
        }
        rows.sort(Comparator.comparingLong(Row::dayTime).reversed());
        return new Answer(host, List.copyOf(rows.subList(0, Math.min(MOST_ROWS, rows.size()))), controllers,
                machines, level.getDayTime());
    }

    /* The controllers on the network of the computer at {@code host}. */
    private static List<IndustrialControllerBlockEntity> controllersOf(final ServerLevel level, final BlockPos host) {
        final NetworkUuid network = level.getBlockEntity(host) instanceof AbstractComputerBlockEntity computer
                ? computer.networkUuid() : null;
        final List<IndustrialControllerBlockEntity> found = new ArrayList<>();
        if (network == null) {
            return found;
        }
        for (final NetworkSystem.MemberNode member : NetworkSystem.get(level).membersCarryingOut(network,
                ControllerOperations.MACHINE_SET.id())) {
            if (level.getBlockEntity(BlockPos.of(member.pos())) instanceof IndustrialControllerBlockEntity controller) {
                found.add(controller);
            }
        }
        return found;
    }

    private static void answer(final Request request, final ServerPlayer player, final ServerLevel level) {
        PacketDistributor.sendToPlayer(player, collect(level, request.host()));
    }

    private static void acknowledge(final Acknowledge asked, final ServerPlayer player, final ServerLevel level) {
        for (final IndustrialControllerBlockEntity controller : controllersOf(level, asked.host())) {
            if (asked.id() == EVERY_ALERT) {
                controller.acknowledgeAll();
            } else if (controller.getBlockPos().asLong() == asked.controller()) {
                controller.acknowledge(asked.id());
            }
        }
        PacketDistributor.sendToPlayer(player, collect(level, asked.host()));
    }

    private static void received(final Answer answer, final Player player) {
        LATEST.put(answer.host(), answer);
    }

    private static ResourceLocation payloadId(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }

    private static String clip(final String text) {
        return text.length() > MOST_LETTERS ? text.substring(0, MOST_LETTERS) : text;
    }

    /** The window of the computer at {@code host} asking for its network's alerts. */
    public record Request(BlockPos host) implements CustomPacketPayload {

        public static final Type<Request> TYPE = new Type<>(payloadId("machine_monitor_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Request::host, Request::new);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    /**
     * An operator at the computer at {@code host} acknowledging the alert numbered {@code id} of the controller at
     * {@code controller}, or, for {@link #EVERY_ALERT}, every alert of the network.
     */
    public record Acknowledge(BlockPos host, long controller, long id) implements CustomPacketPayload {

        public static final Type<Acknowledge> TYPE = new Type<>(payloadId("machine_monitor_acknowledge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Acknowledge> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Acknowledge::host, ByteBufCodecs.VAR_LONG, Acknowledge::controller,
                ByteBufCodecs.VAR_LONG, Acknowledge::id, Acknowledge::new);

        @Override
        public Type<Acknowledge> type() {
            return TYPE;
        }
    }

    /**
     * What the Machine Monitor of the computer at {@code host} shows: the alerts, how many controllers are on, how many
     * machines they read, and the world's clock as it was read.
     */
    public record Answer(BlockPos host, List<Row> rows, int controllers, int machines, long dayTime)
            implements CustomPacketPayload {

        public static final Type<Answer> TYPE = new Type<>(payloadId("machine_monitor_answer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Answer> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Answer::host, Row.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_ROWS)),
                Answer::rows, ByteBufCodecs.VAR_INT, Answer::controllers, ByteBufCodecs.VAR_INT, Answer::machines,
                ByteBufCodecs.VAR_LONG, Answer::dayTime, Answer::new);

        public Answer {
            rows = List.copyOf(rows);
        }

        @Override
        public Type<Answer> type() {
            return TYPE;
        }
    }

    /**
     * One alert as the window shows it, and what its machine reads now for the machine's panel.
     *
     * @param controller  where the controller that raised it stands
     * @param id          its number among that controller's alerts
     * @param dayTime     the world's clock when it was raised
     * @param machine     the machine's name
     * @param where       where the machine stands
     * @param kind        what it is about
     * @param reading     what the machine read as it was raised
     * @param acknowledged whether an operator has seen it
     * @param active      whether what raised it still holds
     * @param detail      what the machine reads now
     */
    public record Row(long controller, long id, long dayTime, String machine, BlockPos where, AlertKind kind,
                      Text reading, boolean acknowledged, boolean active, Detail detail) {

        public static final StreamCodec<FriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.of(Row::write, Row::read);

        private static void write(final FriendlyByteBuf buf, final Row row) {
            buf.writeVarLong(row.controller);
            buf.writeVarLong(row.id);
            buf.writeVarLong(row.dayTime);
            buf.writeUtf(clip(row.machine), MOST_LETTERS);
            buf.writeBlockPos(row.where);
            buf.writeUtf(row.kind.serializedName(), MOST_LETTERS);
            TextCodecs.STREAM_CODEC.encode(buf, row.reading);
            buf.writeBoolean(row.acknowledged);
            buf.writeBoolean(row.active);
            Detail.write(buf, row.detail);
        }

        private static Row read(final FriendlyByteBuf buf) {
            return new Row(buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readUtf(MOST_LETTERS),
                    buf.readBlockPos(), KINDS.byName(buf.readUtf(MOST_LETTERS), AlertKind.DAMAGED),
                    TextCodecs.STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readBoolean(), Detail.read(buf));
        }
    }

    /**
     * What an alerted machine reads now, for the window's panel: its state, its temperature and the most it stands in
     * degrees Celsius ({@link #NONE} when it has none), what it draws in watts and the class of its port ({@link #NONE}
     * and an empty text when it draws none), the controller that reads it and the card's node.
     */
    public record Detail(Text state, long temperature, long limit, long power, Text port, String controller,
                         String card) {

        /** A value the machine does not have. */
        public static final long NONE = Long.MIN_VALUE;

        private static void write(final FriendlyByteBuf buf, final Detail detail) {
            TextCodecs.STREAM_CODEC.encode(buf, detail.state);
            buf.writeLong(detail.temperature);
            buf.writeLong(detail.limit);
            buf.writeLong(detail.power);
            TextCodecs.STREAM_CODEC.encode(buf, detail.port);
            buf.writeUtf(clip(detail.controller), MOST_LETTERS);
            buf.writeUtf(clip(detail.card), MOST_LETTERS);
        }

        private static Detail read(final FriendlyByteBuf buf) {
            return new Detail(TextCodecs.STREAM_CODEC.decode(buf), buf.readLong(), buf.readLong(), buf.readLong(),
                    TextCodecs.STREAM_CODEC.decode(buf), buf.readUtf(MOST_LETTERS), buf.readUtf(MOST_LETTERS));
        }
    }
}
