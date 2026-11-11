/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.firmware;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.client.FirmwareScreen;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextCodecs;
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

import java.util.List;

/**
 * The firmware's network boot (computers/docs/design/operating-systems.md): the setup asks the Mirror what it serves,
 * the Mirror's menu comes back, and the player's choice is booted.
 */
public final class NetBootPayloads {

    /** The most systems a menu carries, more than there are. */
    public static final int MOST = 64;
    private static final int MOST_TEXT = 64;
    /* One system on the wire. */
    private static final StreamCodec<RegistryFriendlyByteBuf, NetworkBoot.Offer> OFFER = StreamCodec.of(
            (buf, offer) -> {
                buf.writeUtf(TextBounds.clip(offer.id(), MOST_TEXT), MOST_TEXT);
                buf.writeUtf(TextBounds.clip(offer.name(), MOST_TEXT), MOST_TEXT);
                buf.writeUtf(TextBounds.clip(offer.house(), MOST_TEXT), MOST_TEXT);
                buf.writeBoolean(offer.community());
                TextCodecs.STREAM_CODEC.encode(buf, offer.image());
            },
            buf -> new NetworkBoot.Offer(buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT),
                    buf.readBoolean(), TextCodecs.STREAM_CODEC.decode(buf)));

    private NetBootPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        // Both are sent from the setup, which is one of the monitor's own sessions.
        ComputerAccess.accept(registrar, Ask.TYPE, Ask.STREAM_CODEC,
                ComputerAccess.screenAt(Ask::host, Ask::monitor, MonitorSessionMenu.Phase.FIRMWARE),
                NetBootPayloads::handleAsk);
        ComputerAccess.accept(registrar, Boot.TYPE, Boot.STREAM_CODEC,
                ComputerAccess.screenAt(Boot::host, Boot::monitor, MonitorSessionMenu.Phase.FIRMWARE),
                NetBootPayloads::handleBoot);
        registrar.playToClient(Menu.TYPE, Menu.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> FirmwareScreen.acceptNetwork(payload)));
    }

    private static void handleAsk(final Ask payload, final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IOsHost machine) {
            PacketDistributor.sendToPlayer(player, menuOf(payload.host(), level, machine, Text.EMPTY));
        }
    }

    private static void handleBoot(final Boot payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.host()) instanceof IOsHost machine)) {
            return;
        }
        final Text failure = NetworkBoot.boot(level, machine, payload.system());
        if (failure != null) {
            PacketDistributor.sendToPlayer(player, menuOf(payload.host(), level, machine, failure));
            return;
        }
        JscEvents.award(player, JscEvents.NETWORK_BOOT);
        // Whatever the machine restarts into, the installer or a live session, is what the monitor shows next.
        MonitorBlock.openSession(player, level, payload.monitor(), payload.host());
    }

    /* The Mirror's menu for that machine, or the word that none answered, with why a choice was refused. */
    private static Menu menuOf(final BlockPos host, final ServerLevel level, final IOsHost machine,
                               final Text refusal) {
        final NetworkBoot.Listing listing = NetworkBoot.listing(level, machine);
        return listing == null
                ? new Menu(host, false, "", "", "", List.of(), NetworkBoot.NO_MIRROR.text())
                : new Menu(host, true, listing.mirror(), listing.card(), listing.address(), listing.offers(), refusal);
    }

    /** Client to server: what does the Mirror serve this machine? */
    public record Ask(BlockPos host, BlockPos monitor) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Ask> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "net_boot_ask"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ask> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Ask::host, BlockPos.STREAM_CODEC, Ask::monitor, Ask::new);

        @Override
        public CustomPacketPayload.Type<Ask> type() {
            return TYPE;
        }
    }

    /** Client to server: boot the installer of that system from the Mirror. */
    public record Boot(BlockPos host, BlockPos monitor, String system) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Boot> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "net_boot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Boot> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Boot::host, BlockPos.STREAM_CODEC, Boot::monitor,
                ByteBufCodecs.stringUtf8(MOST_TEXT), Boot::system, Boot::new);

        @Override
        public CustomPacketPayload.Type<Boot> type() {
            return TYPE;
        }
    }

    /**
     * Server to client: the Mirror's menu.
     *
     * @param answered whether a Mirror answered at all
     * @param mirror   the Mainframe serving it, by the name it goes by
     * @param card     the machine's card's hardware address
     * @param address  the address the network handed the machine
     * @param offers   the systems it serves this machine
     * @param refusal  why the last choice was not booted, or nothing
     */
    public record Menu(BlockPos host, boolean answered, String mirror, String card, String address,
                       List<NetworkBoot.Offer> offers, Text refusal) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Menu> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "net_boot_menu"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Menu> STREAM_CODEC = StreamCodec.of(
                (buf, menu) -> {
                    buf.writeBlockPos(menu.host());
                    buf.writeBoolean(menu.answered());
                    buf.writeUtf(TextBounds.clip(menu.mirror(), MOST_TEXT), MOST_TEXT);
                    buf.writeUtf(menu.card(), MOST_TEXT);
                    buf.writeUtf(menu.address(), MOST_TEXT);
                    OFFER.apply(ByteBufCodecs.list(MOST)).encode(buf, menu.offers());
                    TextCodecs.STREAM_CODEC.encode(buf, menu.refusal());
                },
                buf -> new Menu(buf.readBlockPos(), buf.readBoolean(), buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT),
                        buf.readUtf(MOST_TEXT), OFFER.apply(ByteBufCodecs.list(MOST)).decode(buf),
                        TextCodecs.STREAM_CODEC.decode(buf)));

        public Menu {
            offers = List.copyOf(offers.size() > MOST ? offers.subList(0, MOST) : offers);
        }

        @Override
        public CustomPacketPayload.Type<Menu> type() {
            return TYPE;
        }
    }
}
