/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: what the "This PC" window shows. The machine itself (name, kind, era, system,
 * network, hardware), the disks installed in it, the removable media in its linked drives, and the
 * ids of the programs installed on it. A client copy of a computer knows none of this on its own.
 *
 * <p>What the machine says in words travels as text, so each player reads the card in their own language: the kind
 * of machine, the era, the parts and the drives by their names, and what an installer needs. The player's own
 * name for the machine, the system's and the network's names stay as they are.
 */
@TextHolder
public record ThisPcPayload(WireMachine machine, List<WireDisk> disks, List<WireMedia> media,
                            List<String> installedPrograms)
        implements CustomPacketPayload {

    // What a machine is.
    public static final TextKey MAINFRAME = TextKey.of("jsc.this_pc.mainframe", "Mainframe");
    public static final TextKey CRAFTING_COMPUTER = TextKey.of("jsc.this_pc.crafting_computer", "Crafting Computer");
    public static final TextKey CLUSTER_MANAGEMENT_COMPUTER =
            TextKey.of("jsc.this_pc.cluster_management_computer", "Cluster Management Computer");
    public static final TextKey SERVER = TextKey.of("jsc.this_pc.server", "Server");
    public static final TextKey PERSONAL_COMPUTER = TextKey.of("jsc.this_pc.personal_computer", "Personal Computer");

    // What is linked to it.
    public static final TextKey FLOPPY_DRIVE = TextKey.of("jsc.this_pc.floppy_drive", "Floppy Drive");
    public static final TextKey CD_DRIVE = TextKey.of("jsc.this_pc.cd_drive", "CD Drive");
    public static final TextKey DVD_DRIVE = TextKey.of("jsc.this_pc.dvd_drive", "DVD Drive");
    public static final TextKey DOCK_STATION = TextKey.of("jsc.this_pc.dock_station", "Dock Station");
    public static final TextKey MONITOR = TextKey.of("jsc.this_pc.monitor", "Monitor");
    public static final TextKey COUNTED = TextKey.of("jsc.this_pc.counted", "%s × %s");
    public static final TextKey WITH_CLOCK = TextKey.of("jsc.this_pc.with_clock", "%s · %s");

    public static final int MAX = 64;
    /** The most requirements a medium's row carries, which is more than any installer has. */
    public static final int MAX_NEEDS = 12;

    public static final CustomPacketPayload.Type<ThisPcPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "this_pc"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThisPcPayload> STREAM_CODEC =
            StreamCodec.composite(
                    WireMachine.STREAM_CODEC, ThisPcPayload::machine,
                    WireDisk.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), ThisPcPayload::disks,
                    WireMedia.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), ThisPcPayload::media,
                    ByteBufCodecs.stringUtf8(96).apply(ByteBufCodecs.list(MAX)), ThisPcPayload::installedPrograms,
                    ThisPcPayload::new);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public ThisPcPayload {
        disks = List.copyOf(disks);
        media = List.copyOf(media);
        installedPrograms = List.copyOf(installedPrograms);
    }

    @Override
    public CustomPacketPayload.Type<ThisPcPayload> type() {
        return TYPE;
    }

    /**
     * The machine card: what this computer is before any list.
     *
     * @param name         the player's name for it, or empty
     * @param kind         what it is (Personal Computer, Mainframe, ...)
     * @param era          the hardware era's name
     * @param osLabel      the installed system's name, or empty
     * @param osYear       the year that system shipped, 0 without one
     * @param networkLabel the network it sits on, or empty
     * @param boardLabel   the motherboard's name, or empty
     * @param cpuLabel     the processor's name with its clock, or empty
     * @param cpuCount     how many processors are seated
     * @param cpuArch      the architecture the processors are built on, or empty
     * @param ramMb        installed memory
     * @param vramMb       installed video memory
     * @param gpuCount     how many graphics cards are seated
     * @param psuLabel     the power supply's name, or empty
     * @param buildValid   whether the parts make a machine that comes up
     * @param peripherals  the linked peripherals, joined for display, or empty
     */
    public record WireMachine(String name, Text kind, Text era, String osLabel, int osYear,
                              String networkLabel, Text boardLabel, Text cpuLabel, int cpuCount,
                              Text cpuArch, int ramMb, int vramMb, int gpuCount, Text psuLabel,
                              boolean buildValid, Text peripherals) {

        public static final WireMachine EMPTY = new WireMachine("", Text.EMPTY, Text.EMPTY, "", 0, "", Text.EMPTY,
                Text.EMPTY, 0, Text.EMPTY, 0, 0, 0, Text.EMPTY, false, Text.EMPTY);

        // Written by hand: composite() tops out at six pairs, and the card carries sixteen fields.
        public static final StreamCodec<RegistryFriendlyByteBuf, WireMachine> STREAM_CODEC =
                StreamCodec.of((buf, m) -> {
                    buf.writeUtf(m.name(), 96);
                    TextCodecs.STREAM_CODEC.encode(buf, m.kind());
                    TextCodecs.STREAM_CODEC.encode(buf, m.era());
                    buf.writeUtf(m.osLabel(), 64);
                    buf.writeVarInt(m.osYear());
                    buf.writeUtf(m.networkLabel(), 64);
                    TextCodecs.STREAM_CODEC.encode(buf, m.boardLabel());
                    TextCodecs.STREAM_CODEC.encode(buf, m.cpuLabel());
                    buf.writeVarInt(m.cpuCount());
                    TextCodecs.STREAM_CODEC.encode(buf, m.cpuArch());
                    buf.writeVarInt(m.ramMb());
                    buf.writeVarInt(m.vramMb());
                    buf.writeVarInt(m.gpuCount());
                    TextCodecs.STREAM_CODEC.encode(buf, m.psuLabel());
                    buf.writeBoolean(m.buildValid());
                    TextCodecs.STREAM_CODEC.encode(buf, m.peripherals());
                }, buf -> new WireMachine(buf.readUtf(96), TextCodecs.STREAM_CODEC.decode(buf),
                        TextCodecs.STREAM_CODEC.decode(buf), buf.readUtf(64), buf.readVarInt(), buf.readUtf(64),
                        TextCodecs.STREAM_CODEC.decode(buf), TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(),
                        TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        TextCodecs.STREAM_CODEC.decode(buf), buf.readBoolean(), TextCodecs.STREAM_CODEC.decode(buf)));
    }

    /**
     * One disk installed in the computer.
     *
     * @param slot       the disk slot index (0-based within the disk range)
     * @param label      the disk's name
     * @param capItems   total capacity in item-equivalents (1 item = 4 MB)
     * @param usedItems  used space in item-equivalents (storage + files + OS footprint)
     * @param system     true if this is the bootable system disk
     * @param osPath     the installed OS path (e.g. {@code frames_xp}), or empty for a data disk
     * @param osItems    the share the installed system occupies
     * @param storeItems the share stored items occupy
     * @param fileItems  the share files and installed programs occupy
     */
    public record WireDisk(int slot, Text label, long capItems, long usedItems, boolean system,
                           String osPath, long osItems, long storeItems, long fileItems) {

        // Written by hand: composite() tops out at six pairs, and a disk now carries nine fields.
        public static final StreamCodec<RegistryFriendlyByteBuf, WireDisk> STREAM_CODEC =
                StreamCodec.of((buf, d) -> {
                    buf.writeVarInt(d.slot());
                    TextCodecs.STREAM_CODEC.encode(buf, d.label());
                    buf.writeVarLong(d.capItems());
                    buf.writeVarLong(d.usedItems());
                    buf.writeBoolean(d.system());
                    buf.writeUtf(d.osPath(), 64);
                    buf.writeVarLong(d.osItems());
                    buf.writeVarLong(d.storeItems());
                    buf.writeVarLong(d.fileItems());
                }, buf -> new WireDisk(buf.readVarInt(), TextCodecs.STREAM_CODEC.decode(buf), buf.readVarLong(),
                        buf.readVarLong(), buf.readBoolean(), buf.readUtf(64),
                        buf.readVarLong(), buf.readVarLong(), buf.readVarLong()));

        /** Free space in item-equivalents. */
        public long freeItems() {
            return Math.max(0L, capItems - usedItems);
        }
    }

    /**
     * One drive linked to the computer, with whatever medium is in it.
     *
     * @param readerPos   the drive block's packed position
     * @param drive       the drive type's serialized name (e.g. cd_drive)
     * @param mediaName   the medium's name, or empty for an empty drive
     * @param kind        the media kind's serialized name (os_install / program_install / data, empty when none)
     * @param payloadPath the OS/program id carried, or empty
     * @param installable true if this is an installer whose software is not yet installed here
     * @param payloadName what the medium installs, by name, or empty
     * @param payloadYear the year that software shipped, 0 for none
     * @param packageId   the id a package manager installs it by, or empty
     * @param needs       its requirements, one line each
     * @param stored      what a data medium holds, in item-equivalents
     * @param blocksAway  how far the drive is from the computer, in blocks
     */
    public record WireMedia(long readerPos, String drive, Text mediaName, String kind,
                            String payloadPath, boolean installable, String payloadName, int payloadYear,
                            String packageId, List<Text> needs, long stored, int blocksAway) {

        // Written by hand: composite() tops out at six pairs, and a drive now carries twelve fields.
        public static final StreamCodec<RegistryFriendlyByteBuf, WireMedia> STREAM_CODEC =
                StreamCodec.of((buf, m) -> {
                    buf.writeVarLong(m.readerPos());
                    buf.writeUtf(m.drive(), 48);
                    TextCodecs.STREAM_CODEC.encode(buf, m.mediaName());
                    buf.writeUtf(m.kind(), 24);
                    buf.writeUtf(m.payloadPath(), 96);
                    buf.writeBoolean(m.installable());
                    buf.writeUtf(m.payloadName(), 96);
                    buf.writeVarInt(m.payloadYear());
                    buf.writeUtf(m.packageId(), 64);
                    TextCodecs.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NEEDS)).encode(buf, m.needs());
                    buf.writeVarLong(m.stored());
                    buf.writeVarInt(m.blocksAway());
                }, buf -> new WireMedia(buf.readVarLong(), buf.readUtf(48), TextCodecs.STREAM_CODEC.decode(buf),
                        buf.readUtf(24), buf.readUtf(96), buf.readBoolean(), buf.readUtf(96), buf.readVarInt(),
                        buf.readUtf(64), TextCodecs.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NEEDS)).decode(buf),
                        buf.readVarLong(), buf.readVarInt()));

        public WireMedia {
            needs = List.copyOf(needs);
        }

        /** Whether the drive holds anything at all. */
        public boolean loaded() {
            return !mediaName.isEmpty();
        }
    }
}
