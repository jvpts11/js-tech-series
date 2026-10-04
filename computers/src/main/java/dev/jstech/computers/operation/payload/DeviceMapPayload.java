/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server to client: a machine's hardware and ports, for its system's Device Manager, read off it as it stands. */
public record DeviceMapPayload(BlockPos hostPos, DeviceMap map) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DeviceMapPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "device_map"));

    /* Written out by hand: the map is a tree, the hubs' ports nested in the ports that carry them. */
    public static final StreamCodec<RegistryFriendlyByteBuf, DeviceMapPayload> STREAM_CODEC =
            StreamCodec.of(DeviceMapPayload::encode, DeviceMapPayload::decode);

    /** The longest a name or an icon kind may be; a host name is the player's, so it is cut rather than refused. */
    private static final int MAX_TEXT = 64;
    /** The most items any one list of the map carries; a machine has far fewer parts or ports than this. */
    private static final int MAX_ITEMS = 256;
    /** How deep hubs nest, as deep as their links can go. */
    private static final int MAX_DEPTH = 16;
    /** The board's kind of port, by its stable id; one this side does not know reads as USB. */
    private static final StreamCodec<ByteBuf, DeviceMap.PortFamily> FAMILY =
            StableCodecs.byId(DeviceMap.PortFamily.class, DeviceMap.PortFamily.USB);

    @Override
    public CustomPacketPayload.Type<DeviceMapPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final DeviceMapPayload p) {
        final DeviceMap m = p.map();
        buf.writeBlockPos(p.hostPos());
        writeString(buf, m.host());
        TextCodecs.STREAM_CODEC.encode(buf, m.board());
        writeTexts(buf, m.processors());
        writeTexts(buf, m.memory());
        writeTexts(buf, m.disks());
        buf.writeVarInt(Math.min(m.video().size(), MAX_ITEMS));
        for (final DeviceMap.VideoCard card : m.video().subList(0, Math.min(m.video().size(), MAX_ITEMS))) {
            TextCodecs.STREAM_CODEC.encode(buf, card.name());
            writePorts(buf, card.outputs(), 0);
        }
        buf.writeVarInt(Math.min(m.audio().size(), MAX_ITEMS));
        for (final DeviceMap.AudioSource source : m.audio().subList(0, Math.min(m.audio().size(), MAX_ITEMS))) {
            TextCodecs.STREAM_CODEC.encode(buf, source.name());
            writePorts(buf, source.outputs(), 0);
        }
        FAMILY.encode(buf, m.family());
        writePorts(buf, m.devicePorts(), 0);
        writeTexts(buf, m.network());
        writeTexts(buf, m.cards());
        final int icons = Math.min(m.cardIcons().size(), MAX_ITEMS);
        buf.writeVarInt(icons);
        for (int i = 0; i < icons; i++) {
            buf.writeUtf(m.cardIcons().get(i), MAX_TEXT);
        }
    }

    private static DeviceMapPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = buf.readBlockPos();
        final String name = buf.readUtf(MAX_TEXT);
        final Text board = TextCodecs.STREAM_CODEC.decode(buf);
        final List<Text> processors = readTexts(buf);
        final List<Text> memory = readTexts(buf);
        final List<Text> disks = readTexts(buf);
        final int cards = Math.min(buf.readVarInt(), MAX_ITEMS);
        final List<DeviceMap.VideoCard> video = new ArrayList<>(cards);
        for (int i = 0; i < cards; i++) {
            video.add(new DeviceMap.VideoCard(TextCodecs.STREAM_CODEC.decode(buf), readPorts(buf, 0)));
        }
        final int sources = Math.min(buf.readVarInt(), MAX_ITEMS);
        final List<DeviceMap.AudioSource> audio = new ArrayList<>(sources);
        for (int i = 0; i < sources; i++) {
            audio.add(new DeviceMap.AudioSource(TextCodecs.STREAM_CODEC.decode(buf), readPorts(buf, 0)));
        }
        final DeviceMap.PortFamily portFamily = FAMILY.decode(buf);
        final List<DeviceMap.Port> ports = readPorts(buf, 0);
        final List<Text> network = readTexts(buf);
        final List<Text> cardNames = readTexts(buf);
        final int icons = Math.min(buf.readVarInt(), MAX_ITEMS);
        final List<String> cardIcons = new ArrayList<>(icons);
        for (int i = 0; i < icons; i++) {
            cardIcons.add(buf.readUtf(MAX_TEXT));
        }
        return new DeviceMapPayload(host, new DeviceMap(name, board, processors, memory, disks, video, audio,
                portFamily, ports, network, cardNames, cardIcons));
    }

    private static void writePorts(final RegistryFriendlyByteBuf buf, final List<DeviceMap.Port> ports,
                                   final int depth) {
        final int count = depth >= MAX_DEPTH ? 0 : Math.min(ports.size(), MAX_ITEMS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            final DeviceMap.Port port = ports.get(i);
            TextCodecs.STREAM_CODEC.encode(buf, port.name());
            final int plugged = Math.min(port.plugged().size(), MAX_ITEMS);
            buf.writeVarInt(plugged);
            for (int d = 0; d < plugged; d++) {
                final DeviceMap.Device device = port.plugged().get(d);
                writeString(buf, device.icon());
                TextCodecs.STREAM_CODEC.encode(buf, device.name());
                buf.writeLong(device.pos());
                buf.writeBoolean(device.disabled());
                writePorts(buf, device.ports(), depth + 1);
            }
        }
    }

    private static List<DeviceMap.Port> readPorts(final RegistryFriendlyByteBuf buf, final int depth) {
        final int count = Math.min(buf.readVarInt(), MAX_ITEMS);
        final List<DeviceMap.Port> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final Text name = TextCodecs.STREAM_CODEC.decode(buf);
            final int plugged = Math.min(buf.readVarInt(), MAX_ITEMS);
            final List<DeviceMap.Device> devices = new ArrayList<>(plugged);
            for (int d = 0; d < plugged; d++) {
                final String icon = buf.readUtf(MAX_TEXT);
                final Text deviceName = TextCodecs.STREAM_CODEC.decode(buf);
                final long pos = buf.readLong();
                final boolean disabled = buf.readBoolean();
                final List<DeviceMap.Port> hubPorts = depth + 1 >= MAX_DEPTH ? skipPorts(buf)
                        : readPorts(buf, depth + 1);
                devices.add(new DeviceMap.Device(icon, deviceName, pos, disabled, hubPorts));
            }
            out.add(new DeviceMap.Port(name, devices));
        }
        return out;
    }

    /* The deepest level is written with no ports, so reading it takes only its count. */
    private static List<DeviceMap.Port> skipPorts(final RegistryFriendlyByteBuf buf) {
        buf.readVarInt();
        return List.of();
    }

    private static void writeTexts(final RegistryFriendlyByteBuf buf, final List<Text> texts) {
        final int count = Math.min(texts.size(), MAX_ITEMS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            TextCodecs.STREAM_CODEC.encode(buf, texts.get(i));
        }
    }

    private static List<Text> readTexts(final RegistryFriendlyByteBuf buf) {
        final int count = Math.min(buf.readVarInt(), MAX_ITEMS);
        final List<Text> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(TextCodecs.STREAM_CODEC.decode(buf));
        }
        return out;
    }

    private static void writeString(final RegistryFriendlyByteBuf buf, final String text) {
        buf.writeUtf(text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT), MAX_TEXT);
    }
}
