/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.client.os.QuinceStoreApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.TextBounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Quince Store's payloads (computers/docs/design/quince.md): what the network's Mirror serves for the machine, and
 * a program got from it, free, by the same way {@code keg} installs it at the Terminal.
 */
public final class StorePayloads {

    /** The most programs a listing carries, far more than the Mirror serves. */
    public static final int MOST = 128;
    private static final int MOST_TEXT = 96;

    private StorePayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, Ask.TYPE, Ask.STREAM_CODEC, ComputerAccess.machine(Ask::host),
                StorePayloads::handleAsk);
        ComputerAccess.accept(registrar, Get.TYPE, Get.STREAM_CODEC, ComputerAccess.machine(Get::host),
                StorePayloads::handleGet);
        registrar.playToClient(Listing.TYPE, Listing.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> QuinceStoreApp.accept(payload)));
    }

    private static void handleAsk(final Ask payload, final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IComputerTerminalHost machine) {
            PacketDistributor.sendToPlayer(player, listingOf(payload.host(), new ServerCliComputer(machine, level)));
        }
    }

    private static void handleGet(final Get payload, final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IComputerTerminalHost machine) {
            final ServerCliComputer computer = new ServerCliComputer(machine, level);
            computer.packageInstall(payload.program(), false);
            PacketDistributor.sendToPlayer(player, listingOf(payload.host(), computer));
        }
    }

    /** What the Mirror serves for that machine: the mod's own programs, each saying whether the machine has it. */
    private static Listing listingOf(final BlockPos host, final ServerCliComputer computer) {
        final List<Row> rows = new ArrayList<>();
        for (final ICliComputer.PackageInfo info : computer.packagesAvailable()) {
            if (!info.community()) {
                rows.add(new Row(info.name(), info.description(), info.installed()));
            }
        }
        return new Listing(host, computer.mirrorReachable(), rows);
    }

    /**
     * One program the Store shows.
     *
     * @param program   the name it is got by, as {@code keg} knows it
     * @param name      what it is called
     * @param installed whether the machine already has it
     */
    public record Row(String program, String name, boolean installed) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.of(
                (buf, row) -> {
                    buf.writeUtf(TextBounds.clip(row.program(), MOST_TEXT), MOST_TEXT);
                    buf.writeUtf(TextBounds.clip(row.name(), MOST_TEXT), MOST_TEXT);
                    buf.writeBoolean(row.installed());
                },
                buf -> new Row(buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT), buf.readBoolean()));
    }

    /** Client to server: what does the Mirror serve for this machine? */
    public record Ask(BlockPos host) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Ask> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "store_ask"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ask> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Ask::host, Ask::new);

        @Override
        public CustomPacketPayload.Type<Ask> type() {
            return TYPE;
        }
    }

    /** Client to server: get that program from the Mirror. */
    public record Get(BlockPos host, String program) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Get> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "store_get"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Get> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Get::host, ByteBufCodecs.stringUtf8(MOST_TEXT), Get::program, Get::new);

        @Override
        public CustomPacketPayload.Type<Get> type() {
            return TYPE;
        }
    }

    /**
     * Server to client: what the Mirror serves for the machine.
     *
     * @param host      the machine
     * @param reachable whether the machine reaches a Mirror at all
     * @param rows      the programs, in the Mirror's order
     */
    public record Listing(BlockPos host, boolean reachable, List<Row> rows) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Listing> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "store_listing"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Listing> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Listing::host, ByteBufCodecs.BOOL, Listing::reachable,
                Row.STREAM_CODEC.apply(ByteBufCodecs.list(MOST)), Listing::rows, Listing::new);

        public Listing {
            rows = List.copyOf(rows.size() > MOST ? rows.subList(0, MOST) : rows);
        }

        @Override
        public CustomPacketPayload.Type<Listing> type() {
            return TYPE;
        }
    }
}
