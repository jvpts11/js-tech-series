/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.client.os.ActiveDesktop;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliPackages;
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
 * What the Mirror has newer for a machine, and installing the updates a player chose: Linux Mint's Update Manager
 * ticks them, Settings > Programs has an Update button on each (computers/docs/design/programs.md).
 *
 * <p>The packages are the ones {@code apt upgrade} brings up, and installing one installs it as the upgrade does. A
 * program is installed at once; the desktop's window says it is installed the next time the desktop starts, since
 * the desktop already running is the one that keeps running.
 */
public final class UpdatePayloads {

    /** The most updates a listing carries, more than there are packages. */
    public static final int MOST = 128;
    private static final int MOST_TEXT = 64;

    private UpdatePayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, Ask.TYPE, Ask.STREAM_CODEC, ComputerAccess.machine(Ask::host),
                UpdatePayloads::handleAsk);
        ComputerAccess.accept(registrar, Apply.TYPE, Apply.STREAM_CODEC, ComputerAccess.machine(Apply::host),
                UpdatePayloads::handleApply);
        registrar.playToClient(Listing.TYPE, Listing.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> ActiveDesktop.acceptUpdates(payload)));
    }

    /**
     * Installs those updates on that machine, the ones of them that are updates at all, as every package manager here
     * installs an upgrade. Nothing without a Mirror to fetch from.
     *
     * @return how many were installed
     */
    public static int apply(final IComputerTerminalHost machine, final ServerLevel level, final List<String> ids) {
        return new ServerCliComputer(machine, level).packageUpgrade(ids);
    }

    private static void handleAsk(final Ask payload, final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IComputerTerminalHost machine) {
            PacketDistributor.sendToPlayer(player, listingOf(payload.host(), machine, level));
        }
    }

    private static void handleApply(final Apply payload, final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IComputerTerminalHost machine) {
            apply(machine, level, payload.programs());
            PacketDistributor.sendToPlayer(player, listingOf(payload.host(), machine, level));
        }
    }

    /** What the Mirror has newer for that machine, each against the version it has installed. */
    private static Listing listingOf(final BlockPos host, final IComputerTerminalHost machine,
                                     final ServerLevel level) {
        final ServerCliComputer computer = new ServerCliComputer(machine, level);
        final List<Update> rows = new ArrayList<>();
        for (final ICliPackages.PackageUpdate update : computer.packageUpdates()) {
            rows.add(new Update(update.id(), update.installed(), update.available()));
        }
        return new Listing(host, computer.mirrorReachable(), rows);
    }

    /**
     * One update: the package, by its id (a program's, or the name a player published theirs under), the version it
     * is installed at, empty when none was ever recorded, and the version the Mirror serves.
     */
    public record Update(String program, String installed, String available) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Update> STREAM_CODEC = StreamCodec.of(
                (buf, row) -> {
                    buf.writeUtf(TextBounds.clip(row.program(), MOST_TEXT), MOST_TEXT);
                    buf.writeUtf(TextBounds.clip(row.installed(), MOST_TEXT), MOST_TEXT);
                    buf.writeUtf(TextBounds.clip(row.available(), MOST_TEXT), MOST_TEXT);
                },
                buf -> new Update(buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT)));
    }

    /** Client to server: which updates does the Mirror have for this machine? */
    public record Ask(BlockPos host) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Ask> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "updates_ask"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ask> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Ask::host, Ask::new);

        @Override
        public CustomPacketPayload.Type<Ask> type() {
            return TYPE;
        }
    }

    /** Client to server: install these updates, by the ids of their packages. */
    public record Apply(BlockPos host, List<String> programs) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Apply> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "updates_apply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Apply> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Apply::host,
                ByteBufCodecs.stringUtf8(MOST_TEXT).apply(ByteBufCodecs.list(MOST)), Apply::programs,
                Apply::new);

        public Apply {
            programs = List.copyOf(programs.size() > MOST ? programs.subList(0, MOST) : programs);
        }

        @Override
        public CustomPacketPayload.Type<Apply> type() {
            return TYPE;
        }
    }

    /**
     * Server to client: the updates the Mirror has for the machine.
     *
     * @param reachable whether the machine reaches a Mirror at all
     */
    public record Listing(BlockPos host, boolean reachable, List<Update> rows) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Listing> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "updates_listing"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Listing> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Listing::host, ByteBufCodecs.BOOL, Listing::reachable,
                Update.STREAM_CODEC.apply(ByteBufCodecs.list(MOST)), Listing::rows, Listing::new);

        public Listing {
            rows = List.copyOf(rows.size() > MOST ? rows.subList(0, MOST) : rows);
        }

        @Override
        public CustomPacketPayload.Type<Listing> type() {
            return TYPE;
        }
    }
}
