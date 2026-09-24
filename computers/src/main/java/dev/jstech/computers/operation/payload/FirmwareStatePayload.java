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
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: everything the firmware boot manager shows for one computer: the boot entries (each
 * disk, with or without a system; each linked drive holding a bootable medium), the preferred boot disk,
 * and the hardware summary. Sent in reply to {@link RequestFirmwareStatePayload} and after every
 * {@link FirmwareActionPayload} that changes the state.
 */
public record FirmwareStatePayload(
        BlockPos hostPos,
        int eraId,
        Machine machine,
        int bootSlot,
        int installTargetSlot,
        List<Entry> entries,
        RaidInfo raid
) implements CustomPacketPayload {

    /**
     * What the firmware found when the machine powered on: the same facts its self-test reads out and its
     * hardware page lists.
     *
     * <p>Read off the parts themselves rather than off the block, so a computer answers with what is seated in
     * it and not with what kind of computer it is.
     *
     * @param name       what the player called the machine, or what kind of machine it is when they called it
     *                   nothing
     * @param cpuName    the processor by model
     * @param cores      how many cores it has
     * @param cpuMhz     its clock, zero when no processor is seated
     * @param cpuArch    the architecture by name, "x86-64"; the self-test reads it out on its own and the
     *                   hardware page puts the word size beside it
     * @param cpuBits    the architecture's word size
     * @param boardName  the motherboard by model
     * @param ramMb      the memory counted over the modules seated
     * @param ramModules how many modules are in
     * @param ramSlots   how many the board has
     * @param ramName    the memory modules by model, so a self-test names what it counted
     * @param gpuName    the video card by model, or empty when the machine draws nothing
     * @param monitors   how many monitors are really linked, which is not the same as "connected"
     * @param ports      how many peripheral ports the board offers
     * @param eraLabel   the hardware generation in words
     */
    @TextHolder
    public record Machine(Text name, Text cpuName, int cores, int cpuMhz, String cpuArch, int cpuBits,
                          Text boardName, int ramMb, int ramModules, int ramSlots, Text ramName,
                          Text gpuName, int monitors, int ports, Text eraLabel) {

        /** A machine nothing could be read from: no parts, or a host that is not a computer. */
        public static final Machine NONE = new Machine(Text.EMPTY, Text.EMPTY, 0, 0, "", 0, Text.EMPTY, 0, 0, 0,
                Text.EMPTY, Text.EMPTY, 0, 0, Text.EMPTY);

        /** How many modules of which kind: "2x DDR4 Module". */
        private static final TextKey MODULES = TextKey.of("jsc.payload.firmware_state.modules", "%sx %s");

        /** Whether a processor was found at all, which is what decides that there is a self-test to show. */
        public boolean hasCpu() {
            return this.cpuMhz > 0;
        }

        /** The memory as a self-test names it: how many modules of which kind, or nothing when none are in. */
        public Text memoryModules() {
            if (this.ramModules <= 0 || this.ramName.isEmpty()) {
                return Text.EMPTY;
            }
            return MODULES.with(this.ramModules, this.ramName);
        }
    }

    /**
     * The storage controller the firmware found, if any. A real RAID controller is configured from
     * its own setup at power-on rather than from inside the running system, so the firmware is where
     * this lives.
     *
     * @param present    whether a controller is fitted at all (false hides the storage page)
     * @param mode       the id of the array mode currently configured
     * @param members    how many drives the array was formed with (0 when unconfigured)
     * @param drives     how many member drives are present right now
     * @param capacities usable capacity in items for each mode, one per mode in the order the modes are declared
     */
    public record RaidInfo(boolean present, int mode, int members, int drives, List<Long> capacities) {

        public static final RaidInfo ABSENT = new RaidInfo(false, 0, 0, 0, List.of());

        /** Whether the array is missing members but still serving. */
        public boolean degraded() {
            return members > drives && drives > 0;
        }
    }

    public static final int KIND_DISK = 0;
    public static final int KIND_MEDIA = 1;
    public static final int MAX_ENTRIES = 32;

    /**
     * One boot entry, in the parts a firmware reads out rather than as a sentence.
     *
     * <p>A self-test prints these in columns, a boot menu names the device beside what is on it, and a setup
     * page lists them a third way, so they arrive apart and each screen puts them together as its own age did.
     *
     * @param kind        {@link #KIND_DISK} (an installed disk) or {@link #KIND_MEDIA} (a linked drive's medium)
     * @param ref         the disk slot index, or the media reader's packed block position
     * @param osId        the OS on it ({@code ""} when the disk has no system / the medium is not an installer)
     * @param label       what the device holds: the system's name, an installer's name, or "no system"/"empty"
     * @param device      the hardware itself by model, which is data, or the kind of drive for a reader, which is
     *                    read in the player's language
     * @param size        how much the device holds, in words, and nothing for a drive whose size means nothing
     * @param note        why this cannot be booted or installed, and nothing when it can
     * @param bootable    whether this entry can be booted (a disk with an OS; an OS-installer medium whose OS
     *                    passes the hardware era gate)
     * @param installMode the id of the OS's install mode for a medium (guided / live manual / source), else -1
     */
    public record Entry(int kind, long ref, String osId, Text label, Text device, String size, Text note,
                        boolean bootable, int installMode) {
    }

    public static final CustomPacketPayload.Type<FirmwareStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "firmware_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FirmwareStatePayload> STREAM_CODEC =
            StreamCodec.of(FirmwareStatePayload::encode, FirmwareStatePayload::decode);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public FirmwareStatePayload {
        entries = List.copyOf(entries);
    }

    @Override
    public CustomPacketPayload.Type<FirmwareStatePayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final FirmwareStatePayload p) {
        buf.writeBlockPos(p.hostPos);
        buf.writeVarInt(p.eraId);
        writeMachine(buf, p.machine);
        buf.writeVarInt(p.bootSlot);
        buf.writeVarInt(p.installTargetSlot);
        buf.writeVarInt(Math.min(p.entries.size(), MAX_ENTRIES));
        for (int i = 0; i < p.entries.size() && i < MAX_ENTRIES; i++) {
            final Entry e = p.entries.get(i);
            buf.writeVarInt(e.kind());
            buf.writeVarLong(e.ref());
            buf.writeUtf(e.osId(), 64);
            TextCodecs.STREAM_CODEC.encode(buf, e.label());
            // A model's name is cut to the column it is drawn in; a drive's kind is put together at the other end.
            TextCodecs.STREAM_CODEC.encode(buf, e.device() instanceof Text.Literal model && model.value().length() > 48
                    ? Text.literal(model.value().substring(0, 48)) : e.device());
            buf.writeUtf(e.size(), 16);
            TextCodecs.STREAM_CODEC.encode(buf, e.note());
            buf.writeBoolean(e.bootable());
            buf.writeVarInt(e.installMode());
        }
        final RaidInfo raid = p.raid == null ? RaidInfo.ABSENT : p.raid;
        buf.writeBoolean(raid.present());
        if (raid.present()) {
            buf.writeVarInt(raid.mode());
            buf.writeVarInt(raid.members());
            buf.writeVarInt(raid.drives());
            buf.writeVarInt(raid.capacities().size());
            for (final long capacity : raid.capacities()) {
                buf.writeVarLong(capacity);
            }
        }
    }

    private static FirmwareStatePayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos pos = buf.readBlockPos();
        final int era = buf.readVarInt();
        final Machine machine = readMachine(buf);
        final int bootSlot = buf.readVarInt();
        final int target = buf.readVarInt();
        final int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
        final List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readVarInt(), buf.readVarLong(), buf.readUtf(64),
                    TextCodecs.STREAM_CODEC.decode(buf), TextCodecs.STREAM_CODEC.decode(buf), buf.readUtf(16),
                    TextCodecs.STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readVarInt()));
        }
        RaidInfo raid = RaidInfo.ABSENT;
        if (buf.readBoolean()) {
            final int mode = buf.readVarInt();
            final int members = buf.readVarInt();
            final int drives = buf.readVarInt();
            final int capacityCount = Math.min(buf.readVarInt(), 8);
            final List<Long> capacities = new ArrayList<>(capacityCount);
            for (int i = 0; i < capacityCount; i++) {
                capacities.add(buf.readVarLong());
            }
            raid = new RaidInfo(true, mode, members, drives, capacities);
        }
        return new FirmwareStatePayload(pos, era, machine, bootSlot, target, entries, raid);
    }

    private static void writeMachine(final RegistryFriendlyByteBuf buf, final Machine m) {
        TextCodecs.STREAM_CODEC.encode(buf, m.name());
        TextCodecs.STREAM_CODEC.encode(buf, m.cpuName());
        buf.writeVarInt(m.cores());
        buf.writeVarInt(m.cpuMhz());
        buf.writeUtf(m.cpuArch(), 32);
        buf.writeVarInt(m.cpuBits());
        TextCodecs.STREAM_CODEC.encode(buf, m.boardName());
        buf.writeVarInt(m.ramMb());
        buf.writeVarInt(m.ramModules());
        buf.writeVarInt(m.ramSlots());
        TextCodecs.STREAM_CODEC.encode(buf, m.ramName());
        TextCodecs.STREAM_CODEC.encode(buf, m.gpuName());
        buf.writeVarInt(m.monitors());
        buf.writeVarInt(m.ports());
        TextCodecs.STREAM_CODEC.encode(buf, m.eraLabel());
    }

    private static Machine readMachine(final RegistryFriendlyByteBuf buf) {
        final Text name = TextCodecs.STREAM_CODEC.decode(buf);
        final Text cpuName = TextCodecs.STREAM_CODEC.decode(buf);
        final int cores = buf.readVarInt();
        final int mhz = buf.readVarInt();
        final String arch = buf.readUtf(32);
        final int bits = buf.readVarInt();
        final Text board = TextCodecs.STREAM_CODEC.decode(buf);
        final int ramMb = buf.readVarInt();
        final int modules = buf.readVarInt();
        final int slots = buf.readVarInt();
        final Text ramName = TextCodecs.STREAM_CODEC.decode(buf);
        final Text gpuName = TextCodecs.STREAM_CODEC.decode(buf);
        final int monitors = buf.readVarInt();
        final int ports = buf.readVarInt();
        return new Machine(name, cpuName, cores, mhz, arch, bits, board, ramMb, modules, slots, ramName, gpuName,
                monitors, ports, TextCodecs.STREAM_CODEC.decode(buf));
    }
}
