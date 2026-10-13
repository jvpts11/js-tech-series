/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.hardware.ExperienceIndex;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.core.audio.StereoSide;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the full state the Settings app draws: the editable per-computer knobs, the
 * read-only hardware/OS specs (System &amp; Display pages), the installed programs (Programs page),
 * the per-disk usage (Storage page), the memory ledger (what holds RAM, for the System Monitor), the system's
 * sound (the Sound page, and the volume control on the panel) and how Frames 7 rates the machine's parts.
 * Sent in reply to {@link RequestSettingsPayload} and after every {@link SetSettingPayload}.
 *
 * <p>The stream codec is written by hand because the payload has more fields than
 * {@code StreamCodec.composite} accepts.
 *
 * <p>The processor, its architecture and the disks travel as text, read in the player's language; the names the
 * player chose travel as they are.
 */
@TextHolder
public record SettingsSnapshotPayload(
        BlockPos hostPos,
        String wallpaper,
        String computerName,
        int accent,
        boolean clock12h,
        int guiScale,
        int brightness,
        String saveDrive,
        boolean removableAutoOpen,
        String themePreset,
        boolean taskbarCentered,
        boolean darkMode,
        int netshare,
        Text cpuLabel,
        int cpuMhz,
        Text cpuArch,
        int ramMb,
        int vramMb,
        String osLabel,
        String platform,
        List<String> installed,
        List<DiskUse> disks,
        int ramUsedMb,
        List<RamUse> ramUses,
        List<ShareRow> shares,
        boolean remoteAllowed,
        Sound sound,
        Gpu gpu,
        DesktopEffects effects,
        ExperienceIndex experience
) implements CustomPacketPayload {

    /**
     * The machine's graphics, for the Task Manager and the System Monitor: the card (or the graphics on the processor's
     * die), its cores and clock, the slot it sits in, its video memory and what holds it, whether that memory is the
     * system's own lent to it, how busy the card is, and each monitor and graphics window holding memory.
     */
    public record Gpu(Text card, int cores, int mhz, String slot, long totalKb, long monitorsKb, long windowsKb,
                      boolean shared, int load, List<VramRow> usedBy) {

        /** A machine with no graphics to speak of. */
        public static final Gpu NONE = new Gpu(Text.EMPTY, 0, 0, "", 0L, 0L, 0L, false, 0, List.of());

        public Gpu {
            usedBy = List.copyOf(usedBy);
        }

        /** Whether the machine has graphics at all. */
        public boolean present() {
            return totalKb > 0L || cores > 0;
        }

        /** What is held of its video memory. */
        public long usedKb() {
            return monitorsKb + windowsKb;
        }
    }

    /** One thing holding video memory: what it is called, how much it holds, and whether it is a monitor. */
    public record VramRow(Text name, long kb, boolean monitor) {
    }

    /**
     * The system's sound, for the Sound page and the panel's volume control: how loud it plays, whether it is muted,
     * where it goes (a {@code SoundOutput} id), what plays it, whether that plays recordings at all rather than only
     * beeps, and the speakers linked to the machine.
     */
    public record Sound(int volume, boolean muted, String output, Text hardware, boolean plays,
                        List<SpeakerRow> speakers) {

        /** A machine with no sound hardware of its own to set. */
        public static final Sound NONE = new Sound(100, false, "both", Text.EMPTY, false, List.of());

        public Sound {
            speakers = List.copyOf(speakers);
        }
    }

    /** One speaker linked to the machine: the name a program finds it by, empty while it has none, and its side. */
    public record SpeakerRow(String name, StereoSide side) {}

    /** One disk's usage for the Storage page. */
    public record DiskUse(Text label, long capMb, long usedMb, boolean system) {}

    /** One folder this computer shares with the network: its share name, its path and whether others may write. */
    public record ShareRow(String name, String path, boolean writable) {}

    /**
     * One thing holding memory: what to call it, how many megabytes it was given, how many bytes of them it is
     * holding this moment, the serialized name of its ledger kind, and the number it answers to if it is
     * something that can be ended.
     *
     * <p>Both sizes travel because both are shown: the megabytes are what the machine promised and what a new
     * program is measured against, and the bytes are what is really in there, which is what moves while a
     * program runs.
     *
     * <p>A window is ended by its name, but two scripts can be started from the same file and only the
     * number tells them apart, so the number travels for those and is 0 for everything else.
     */
    public record RamUse(String label, int mb, long heldBytes, String kind, int id) {}

    /** How many processors a machine has seated, as its specifications say it. */
    public static final TextKey ONE_CPU = TextKey.of("jsc.settings_snapshot.one_cpu", "%s CPU");
    public static final TextKey CPUS = TextKey.of("jsc.settings_snapshot.cpus", "%s CPUs");

    public static final int MAX = 64;

    public static final CustomPacketPayload.Type<SettingsSnapshotPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "settings_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsSnapshotPayload> STREAM_CODEC =
            StreamCodec.of(SettingsSnapshotPayload::encode, SettingsSnapshotPayload::decode);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public SettingsSnapshotPayload {
        disks = List.copyOf(disks);
        installed = List.copyOf(installed);
        ramUses = List.copyOf(ramUses);
        shares = List.copyOf(shares);
        sound = sound == null ? Sound.NONE : sound;
        gpu = gpu == null ? Gpu.NONE : gpu;
        experience = experience == null ? ExperienceIndex.NONE : experience;
    }

    @Override
    public CustomPacketPayload.Type<SettingsSnapshotPayload> type() {
        return TYPE;
    }

    /** The most characters a label travels with; a longer one is cut, never refused. */
    public static final int LABEL_MAX = 48;
    /** The longest name a speaker takes. */
    private static final int SPEAKER_NAME_MAX = 32;

    /*
     * Every string is cut to its cap before it is written. A cap on writeUtf is a hard failure that
     * drops the connection, and a running program's name is a path the player chose, which nothing
     * here can promise to be short: a project under progs/<solution>/<project>/build already was not.
     */
    private static void encode(final RegistryFriendlyByteBuf buf, final SettingsSnapshotPayload p) {
        buf.writeBlockPos(p.hostPos);
        buf.writeUtf(TextBounds.clip(p.wallpaper, LABEL_MAX), LABEL_MAX);
        buf.writeUtf(TextBounds.clip(p.computerName, InstallerFlow.MOST_NAME_LETTERS), InstallerFlow.MOST_NAME_LETTERS);
        buf.writeInt(p.accent);
        buf.writeBoolean(p.clock12h);
        buf.writeVarInt(p.guiScale);
        buf.writeVarInt(p.brightness);
        buf.writeUtf(TextBounds.clip(p.saveDrive, 4), 4);
        buf.writeBoolean(p.removableAutoOpen);
        buf.writeUtf(TextBounds.clip(p.themePreset, 32), 32);
        buf.writeBoolean(p.taskbarCentered);
        buf.writeBoolean(p.darkMode);
        buf.writeVarInt(p.netshare);
        TextCodecs.STREAM_CODEC.encode(buf, p.cpuLabel);
        buf.writeVarInt(p.cpuMhz);
        TextCodecs.STREAM_CODEC.encode(buf, p.cpuArch);
        buf.writeVarInt(p.ramMb);
        buf.writeVarInt(p.vramMb);
        buf.writeUtf(TextBounds.clip(p.osLabel, LABEL_MAX), LABEL_MAX);
        buf.writeUtf(TextBounds.clip(p.platform, 24), 24);
        buf.writeVarInt(Math.min(p.installed.size(), MAX));
        for (int i = 0; i < p.installed.size() && i < MAX; i++) {
            buf.writeUtf(TextBounds.clip(p.installed.get(i), 96), 96);
        }
        buf.writeVarInt(Math.min(p.disks.size(), MAX));
        for (int i = 0; i < p.disks.size() && i < MAX; i++) {
            final DiskUse d = p.disks.get(i);
            TextCodecs.STREAM_CODEC.encode(buf, d.label());
            buf.writeVarLong(d.capMb());
            buf.writeVarLong(d.usedMb());
            buf.writeBoolean(d.system());
        }
        buf.writeVarInt(p.ramUsedMb);
        buf.writeVarInt(Math.min(p.ramUses.size(), MAX));
        for (int i = 0; i < p.ramUses.size() && i < MAX; i++) {
            final RamUse r = p.ramUses.get(i);
            buf.writeUtf(TextBounds.clip(r.label(), LABEL_MAX), LABEL_MAX);
            buf.writeVarInt(r.mb());
            buf.writeVarLong(r.heldBytes());
            buf.writeUtf(TextBounds.clip(r.kind(), 16), 16);
            buf.writeVarInt(r.id());
        }
        buf.writeVarInt(Math.min(p.shares.size(), MAX));
        for (int i = 0; i < p.shares.size() && i < MAX; i++) {
            final ShareRow s = p.shares.get(i);
            buf.writeUtf(TextBounds.clip(s.name(), LABEL_MAX), LABEL_MAX);
            buf.writeUtf(TextBounds.clip(s.path(), 128), 128);
            buf.writeBoolean(s.writable());
        }
        buf.writeBoolean(p.remoteAllowed);
        final Sound sound = p.sound;
        buf.writeVarInt(sound.volume());
        buf.writeBoolean(sound.muted());
        buf.writeUtf(TextBounds.clip(sound.output(), 16), 16);
        TextCodecs.STREAM_CODEC.encode(buf, sound.hardware());
        buf.writeBoolean(sound.plays());
        buf.writeVarInt(Math.min(sound.speakers().size(), MAX));
        for (int i = 0; i < sound.speakers().size() && i < MAX; i++) {
            final SpeakerRow speaker = sound.speakers().get(i);
            buf.writeUtf(TextBounds.clip(speaker.name(), SPEAKER_NAME_MAX), SPEAKER_NAME_MAX);
            buf.writeVarInt(speaker.side().id());
        }
        writeGpu(buf, p.gpu);
        DesktopEffects.STREAM_CODEC.encode(buf, p.effects);
        final ExperienceIndex index = p.experience;
        buf.writeVarInt(index.processor());
        buf.writeVarInt(index.memory());
        buf.writeVarInt(index.graphics());
        buf.writeVarInt(index.gaming());
        buf.writeVarInt(index.disk());
    }

    private static SettingsSnapshotPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos pos = buf.readBlockPos();
        final String wallpaper = buf.readUtf(48);
        final String computerName = buf.readUtf(InstallerFlow.MOST_NAME_LETTERS);
        final int accent = buf.readInt();
        final boolean clock12h = buf.readBoolean();
        final int guiScale = buf.readVarInt();
        final int brightness = buf.readVarInt();
        final String saveDrive = buf.readUtf(4);
        final boolean removableAutoOpen = buf.readBoolean();
        final String themePreset = buf.readUtf(32);
        final boolean taskbarCentered = buf.readBoolean();
        final boolean darkMode = buf.readBoolean();
        final int netshare = buf.readVarInt();
        final Text cpuLabel = TextCodecs.STREAM_CODEC.decode(buf);
        final int cpuMhz = buf.readVarInt();
        final Text cpuArch = TextCodecs.STREAM_CODEC.decode(buf);
        final int ramMb = buf.readVarInt();
        final int vramMb = buf.readVarInt();
        final String osLabel = buf.readUtf(48);
        final String platform = buf.readUtf(24);
        final int programCount = Math.min(buf.readVarInt(), MAX);
        final List<String> installed = new ArrayList<>(programCount);
        for (int i = 0; i < programCount; i++) {
            installed.add(buf.readUtf(96));
        }
        final int diskCount = Math.min(buf.readVarInt(), MAX);
        final List<DiskUse> disks = new ArrayList<>(diskCount);
        for (int i = 0; i < diskCount; i++) {
            disks.add(new DiskUse(TextCodecs.STREAM_CODEC.decode(buf), buf.readVarLong(), buf.readVarLong(),
                    buf.readBoolean()));
        }
        final int ramUsedMb = buf.readVarInt();
        final int ramUseCount = Math.min(buf.readVarInt(), MAX);
        final List<RamUse> ramUses = new ArrayList<>(ramUseCount);
        for (int i = 0; i < ramUseCount; i++) {
            ramUses.add(new RamUse(buf.readUtf(48), buf.readVarInt(), buf.readVarLong(), buf.readUtf(16),
                    buf.readVarInt()));
        }
        final int shareCount = Math.min(buf.readVarInt(), MAX);
        final List<ShareRow> shares = new ArrayList<>(shareCount);
        for (int i = 0; i < shareCount; i++) {
            shares.add(new ShareRow(buf.readUtf(48), buf.readUtf(128), buf.readBoolean()));
        }
        final boolean remoteAllowed = buf.readBoolean();
        final int volume = buf.readVarInt();
        final boolean muted = buf.readBoolean();
        final String output = buf.readUtf(16);
        final Text hardware = TextCodecs.STREAM_CODEC.decode(buf);
        final boolean plays = buf.readBoolean();
        final int speakerCount = Math.min(buf.readVarInt(), MAX);
        final List<SpeakerRow> speakers = new ArrayList<>(speakerCount);
        for (int i = 0; i < speakerCount; i++) {
            speakers.add(new SpeakerRow(buf.readUtf(SPEAKER_NAME_MAX), StereoSide.byId(buf.readVarInt())));
        }
        return new SettingsSnapshotPayload(pos, wallpaper, computerName, accent, clock12h, guiScale, brightness,
                saveDrive, removableAutoOpen, themePreset, taskbarCentered, darkMode, netshare, cpuLabel, cpuMhz,
                cpuArch, ramMb, vramMb, osLabel, platform, installed, disks, ramUsedMb, ramUses, shares,
                remoteAllowed, new Sound(volume, muted, output, hardware, plays, speakers), readGpu(buf),
                DesktopEffects.STREAM_CODEC.decode(buf), new ExperienceIndex(buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
    }

    private static void writeGpu(final RegistryFriendlyByteBuf buf, final Gpu gpu) {
        TextCodecs.STREAM_CODEC.encode(buf, gpu.card());
        buf.writeVarInt(gpu.cores());
        buf.writeVarInt(gpu.mhz());
        buf.writeUtf(TextBounds.clip(gpu.slot(), LABEL_MAX), LABEL_MAX);
        buf.writeVarLong(gpu.totalKb());
        buf.writeVarLong(gpu.monitorsKb());
        buf.writeVarLong(gpu.windowsKb());
        buf.writeBoolean(gpu.shared());
        buf.writeVarInt(gpu.load());
        buf.writeVarInt(Math.min(gpu.usedBy().size(), MAX));
        for (int i = 0; i < gpu.usedBy().size() && i < MAX; i++) {
            final VramRow row = gpu.usedBy().get(i);
            TextCodecs.STREAM_CODEC.encode(buf, row.name());
            buf.writeVarLong(row.kb());
            buf.writeBoolean(row.monitor());
        }
    }

    private static Gpu readGpu(final RegistryFriendlyByteBuf buf) {
        final Text card = TextCodecs.STREAM_CODEC.decode(buf);
        final int cores = buf.readVarInt();
        final int mhz = buf.readVarInt();
        final String slot = buf.readUtf(LABEL_MAX);
        final long totalKb = buf.readVarLong();
        final long monitorsKb = buf.readVarLong();
        final long windowsKb = buf.readVarLong();
        final boolean shared = buf.readBoolean();
        final int load = buf.readVarInt();
        final int rows = Math.min(buf.readVarInt(), MAX);
        final List<VramRow> usedBy = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            usedBy.add(new VramRow(TextCodecs.STREAM_CODEC.decode(buf), buf.readVarLong(), buf.readBoolean()));
        }
        return new Gpu(card, cores, mhz, slot, totalKb, monitorsKb, windowsKb, shared, load, usedBy);
    }
}
