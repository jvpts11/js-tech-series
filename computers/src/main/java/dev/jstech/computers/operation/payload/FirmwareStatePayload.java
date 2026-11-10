/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuConditions;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.os.media.MediaVolume;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: everything the firmware boot manager shows for one computer: the boot entries (each
 * disk, with or without a system; each linked drive holding a bootable medium), the preferred boot disk,
 * the hardware summary, and the power supply, whose wear the self-test warns of. Sent in reply to
 * {@link RequestFirmwareStatePayload} and after every {@link FirmwareActionPayload} that changes the state.
 *
 * <p>The network boot's entry comes apart from the others, in {@code network}: only the setup's boot page offers it,
 * so the self-test never lists it among the drives nor starts it on its own.
 */
public record FirmwareStatePayload(
        BlockPos hostPos,
        int eraPlace,
        Machine machine,
        int bootSlot,
        int installTargetSlot,
        List<Entry> entries,
        RaidInfo raid,
        @Nullable Psu psu,
        @Nullable Thermal thermal,
        @Nullable Entry network
) implements CustomPacketPayload {

    /**
     * The power supply as the self-test reads it: its name, what it is, and how worn it is.
     *
     * @param name      the supply by model
     * @param spec      what the supply is: its rating, its life, its units
     * @param condition how worn each of its units is
     */
    public record Psu(Text name, PsuSpec spec, PsuCondition condition) {
    }

    /**
     * The processor's temperature as the firmware reads it (computers/docs/design/temperature.md), the case fans that
     * turn, and the thermal trip that last shut the machine down, which its self-test warns of.
     *
     * @param celsius  how hot the processor is, in whole °C
     * @param fans     the case fans turning
     * @param fanSeats the seats the case has for them
     * @param lastTrip how hot it was at the trip that last shut it down, in whole °C; 0 when none did
     */
    public record Thermal(int celsius, int fans, int fanSeats, int lastTrip) {
    }

    /**
     * The same state with the processor's temperature left out, at 0: what a screen that does not show it carries, so
     * a temperature that moves as the machine warms does not make its picture new every degree. The trip it warns of
     * stays.
     */
    public FirmwareStatePayload withoutTemperature() {
        if (thermal == null) {
            return this;
        }
        return new FirmwareStatePayload(hostPos, eraPlace, machine, bootSlot, installTargetSlot, entries, raid, psu,
                new Thermal(0, thermal.fans(), thermal.fanSeats(), thermal.lastTrip()), network);
    }

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
     * @param cpuArch    the instruction set by name, "x86-64"; the self-test reads it out on its own and the
     *                   hardware page puts the word size beside it
     * @param cpuBits    the instruction set's word size
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
    /** Booting from the network: the Mirror's systems, through the card's PXE agent or the UEFI's HTTP boot. */
    public static final int KIND_NETWORK = 2;
    public static final int MAX_ENTRIES = 32;

    /** The most capacities a RAID summary carries, one per mode, so the encoder and the decoder agree on the cap. */
    public static final int MAX_RAID_CAPACITIES = RaidMode.values().length;

    /**
     * One boot entry, in the parts a firmware reads out rather than as a sentence.
     *
     * <p>A self-test prints these in columns, a boot menu names the device beside what is on it, and a setup
     * page lists them a third way, so they arrive apart and each screen puts them together as its own age did.
     *
     * @param kind        {@link #KIND_DISK} (an installed disk), {@link #KIND_MEDIA} (a drive's medium) or
     *                    {@link #KIND_NETWORK} (the network)
     * @param ref         the disk slot index, or the media reader's packed block position, the computer's own for a
     *                    drive on its front
     * @param osId        the OS on it ({@code ""} when the disk has no system / the medium is not an installer)
     * @param label       what the device holds: the system's name, an installer's name, or "no system"/"empty"
     * @param device      the hardware itself by model, which is data, or the kind of drive for a reader, which is
     *                    read in the player's language
     * @param size        how much the device holds, in words, and nothing for a drive whose size means nothing
     * @param note        why this cannot be booted or installed, and nothing when it can
     * @param bootable    whether this entry can be booted (a disk with an OS; an OS-installer medium whose OS
     *                    passes the hardware era gate)
     * @param installMode the id of the OS's install mode for a medium (guided / live manual / source), else -1
     * @param bay         for a medium, which of the reader's volumes it is on, as {@link MediaVolume#bay()} counts
     *                    them; {@link MediaVolume#MEDIUM} for a disk
     */
    public record Entry(int kind, long ref, String osId, Text label, Text device, String size, Text note,
                        boolean bootable, int installMode, int bay) {

        /** An entry for a disk, or for the medium of a reader on a cable. */
        public Entry(final int kind, final long ref, final String osId, final Text label, final Text device,
                     final String size, final Text note, final boolean bootable, final int installMode) {
            this(kind, ref, osId, label, device, size, note, bootable, installMode, MediaVolume.MEDIUM);
        }
    }

    public static final CustomPacketPayload.Type<FirmwareStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "firmware_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FirmwareStatePayload> STREAM_CODEC =
            StreamCodec.of(FirmwareStatePayload::encode, FirmwareStatePayload::decode);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public FirmwareStatePayload {
        entries = List.copyOf(entries);
    }

    /** The state of a machine whose power supply is not read: the guide's pictures, a host with none. */
    public FirmwareStatePayload(final BlockPos hostPos, final int eraPlace, final Machine machine, final int bootSlot,
                                final int installTargetSlot, final List<Entry> entries, final RaidInfo raid) {
        this(hostPos, eraPlace, machine, bootSlot, installTargetSlot, entries, raid, null, null, null);
    }

    /** The state of a machine whose temperature is not read: one with none of its own. */
    public FirmwareStatePayload(final BlockPos hostPos, final int eraPlace, final Machine machine, final int bootSlot,
                                final int installTargetSlot, final List<Entry> entries, final RaidInfo raid,
                                @Nullable final Psu psu) {
        this(hostPos, eraPlace, machine, bootSlot, installTargetSlot, entries, raid, psu, null, null);
    }

    /** The state of a machine that cannot boot from the network: the Vintage's, or a Quince machine. */
    public FirmwareStatePayload(final BlockPos hostPos, final int eraPlace, final Machine machine, final int bootSlot,
                                final int installTargetSlot, final List<Entry> entries, final RaidInfo raid,
                                @Nullable final Psu psu, @Nullable final Thermal thermal) {
        this(hostPos, eraPlace, machine, bootSlot, installTargetSlot, entries, raid, psu, thermal, null);
    }

    /** The supply {@code stack} as the self-test reads it, or null for anything that is no supply. */
    @Nullable
    public static Psu psuOf(final ItemStack stack) {
        final PsuSpec spec = PsuConditions.spec(stack);
        return spec == null ? null : new Psu(GameText.of(stack.getHoverName()), spec, PsuConditions.of(stack));
    }

    @Override
    public CustomPacketPayload.Type<FirmwareStatePayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final FirmwareStatePayload p) {
        buf.writeBlockPos(p.hostPos);
        buf.writeVarInt(p.eraPlace);
        writeMachine(buf, p.machine);
        buf.writeVarInt(p.bootSlot);
        buf.writeVarInt(p.installTargetSlot);
        buf.writeVarInt(Math.min(p.entries.size(), MAX_ENTRIES));
        for (int i = 0; i < p.entries.size() && i < MAX_ENTRIES; i++) {
            writeEntry(buf, p.entries.get(i));
        }
        buf.writeBoolean(p.network != null);
        if (p.network != null) {
            writeEntry(buf, p.network);
        }
        final RaidInfo raid = p.raid == null ? RaidInfo.ABSENT : p.raid;
        buf.writeBoolean(raid.present());
        if (raid.present()) {
            buf.writeVarInt(raid.mode());
            buf.writeVarInt(raid.members());
            buf.writeVarInt(raid.drives());
            final int capacityCount = Math.min(raid.capacities().size(), MAX_RAID_CAPACITIES);
            buf.writeVarInt(capacityCount);
            for (int i = 0; i < capacityCount; i++) {
                buf.writeVarLong(raid.capacities().get(i));
            }
        }
        buf.writeBoolean(p.psu != null);
        if (p.psu != null) {
            TextCodecs.STREAM_CODEC.encode(buf, p.psu.name());
            final PsuSpec spec = p.psu.spec();
            buf.writeVarInt(spec.wattage());
            buf.writeVarInt(spec.efficiencyPercent());
            buf.writeBoolean(spec.autoScaling());
            buf.writeVarInt(spec.lifeDays());
            buf.writeVarInt(spec.units());
            PsuConditions.STREAM_CODEC.encode(buf, p.psu.condition());
        }
        buf.writeBoolean(p.thermal != null);
        if (p.thermal != null) {
            buf.writeVarInt(p.thermal.celsius());
            buf.writeVarInt(p.thermal.fans());
            buf.writeVarInt(p.thermal.fanSeats());
            buf.writeVarInt(p.thermal.lastTrip());
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
            entries.add(readEntry(buf));
        }
        final Entry network = buf.readBoolean() ? readEntry(buf) : null;
        RaidInfo raid = RaidInfo.ABSENT;
        if (buf.readBoolean()) {
            final int mode = buf.readVarInt();
            final int members = buf.readVarInt();
            final int drives = buf.readVarInt();
            final int capacityCount = Math.min(buf.readVarInt(), MAX_RAID_CAPACITIES);
            final List<Long> capacities = new ArrayList<>(capacityCount);
            for (int i = 0; i < capacityCount; i++) {
                capacities.add(buf.readVarLong());
            }
            raid = new RaidInfo(true, mode, members, drives, capacities);
        }
        Psu psu = null;
        if (buf.readBoolean()) {
            final Text name = TextCodecs.STREAM_CODEC.decode(buf);
            final PsuSpec spec = new PsuSpec(Math.max(1, buf.readVarInt()), Math.max(1, Math.min(100,
                    buf.readVarInt())), buf.readBoolean(), Math.max(1, buf.readVarInt()), Math.max(1, buf.readVarInt()));
            psu = new Psu(name, spec, PsuConditions.STREAM_CODEC.decode(buf));
        }
        final Thermal thermal = buf.readBoolean()
                ? new Thermal(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()) : null;
        return new FirmwareStatePayload(pos, era, machine, bootSlot, target, entries, raid, psu, thermal, network);
    }

    private static void writeEntry(final RegistryFriendlyByteBuf buf, final Entry e) {
        buf.writeVarInt(e.kind());
        buf.writeVarLong(e.ref());
        buf.writeUtf(e.osId(), 64);
        TextCodecs.STREAM_CODEC.encode(buf, e.label());
        // A model's name is cut to the column it is drawn in; a drive's kind is put together at the other end.
        TextCodecs.STREAM_CODEC.encode(buf, e.device() instanceof Text.Literal model && model.value().length() > 48
                ? Text.literal(TextBounds.clip(model.value(), 48)) : e.device());
        buf.writeUtf(e.size(), 16);
        TextCodecs.STREAM_CODEC.encode(buf, e.note());
        buf.writeBoolean(e.bootable());
        buf.writeVarInt(e.installMode());
        buf.writeInt(e.bay());
    }

    private static Entry readEntry(final RegistryFriendlyByteBuf buf) {
        return new Entry(buf.readVarInt(), buf.readVarLong(), buf.readUtf(64),
                TextCodecs.STREAM_CODEC.decode(buf), TextCodecs.STREAM_CODEC.decode(buf), buf.readUtf(16),
                TextCodecs.STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readVarInt(), buf.readInt());
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
