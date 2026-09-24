/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.install.InstallerFlow;
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
 * Server to client: the full state the Settings app draws: the editable per-computer knobs, the
 * read-only hardware/OS specs (System &amp; Display pages), the installed programs (Programs page),
 * the per-disk usage (Storage page) and the memory ledger (what holds RAM, for the System Monitor).
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
        boolean remoteAllowed
) implements CustomPacketPayload {

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
    }

    @Override
    public CustomPacketPayload.Type<SettingsSnapshotPayload> type() {
        return TYPE;
    }

    /** The most characters a label travels with; a longer one is cut, never refused. */
    public static final int LABEL_MAX = 48;

    /*
     * Every string is cut to its cap before it is written. A cap on writeUtf is a hard failure that
     * drops the connection, and a running program's name is a path the player chose, which nothing
     * here can promise to be short: a project under progs/<solution>/<project>/build already was not.
     */
    private static void encode(final RegistryFriendlyByteBuf buf, final SettingsSnapshotPayload p) {
        buf.writeBlockPos(p.hostPos);
        buf.writeUtf(clip(p.wallpaper, LABEL_MAX), LABEL_MAX);
        buf.writeUtf(clip(p.computerName, InstallerFlow.MOST_NAME_LETTERS), InstallerFlow.MOST_NAME_LETTERS);
        buf.writeInt(p.accent);
        buf.writeBoolean(p.clock12h);
        buf.writeVarInt(p.guiScale);
        buf.writeVarInt(p.brightness);
        buf.writeUtf(clip(p.saveDrive, 4), 4);
        buf.writeBoolean(p.removableAutoOpen);
        buf.writeUtf(clip(p.themePreset, 32), 32);
        buf.writeBoolean(p.taskbarCentered);
        buf.writeBoolean(p.darkMode);
        buf.writeVarInt(p.netshare);
        TextCodecs.STREAM_CODEC.encode(buf, p.cpuLabel);
        buf.writeVarInt(p.cpuMhz);
        TextCodecs.STREAM_CODEC.encode(buf, p.cpuArch);
        buf.writeVarInt(p.ramMb);
        buf.writeVarInt(p.vramMb);
        buf.writeUtf(clip(p.osLabel, LABEL_MAX), LABEL_MAX);
        buf.writeUtf(clip(p.platform, 24), 24);
        buf.writeVarInt(Math.min(p.installed.size(), MAX));
        for (int i = 0; i < p.installed.size() && i < MAX; i++) {
            buf.writeUtf(clip(p.installed.get(i), 96), 96);
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
            buf.writeUtf(clip(r.label(), LABEL_MAX), LABEL_MAX);
            buf.writeVarInt(r.mb());
            buf.writeVarLong(r.heldBytes());
            buf.writeUtf(clip(r.kind(), 16), 16);
            buf.writeVarInt(r.id());
        }
        buf.writeVarInt(Math.min(p.shares.size(), MAX));
        for (int i = 0; i < p.shares.size() && i < MAX; i++) {
            final ShareRow s = p.shares.get(i);
            buf.writeUtf(clip(s.name(), LABEL_MAX), LABEL_MAX);
            buf.writeUtf(clip(s.path(), 128), 128);
            buf.writeBoolean(s.writable());
        }
        buf.writeBoolean(p.remoteAllowed);
    }

    private static String clip(final String text, final int max) {
        final String s = text == null ? "" : text;
        return s.length() <= max ? s : s.substring(0, max);
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
        return new SettingsSnapshotPayload(pos, wallpaper, computerName, accent, clock12h, guiScale, brightness,
                saveDrive, removableAutoOpen, themePreset, taskbarCentered, darkMode, netshare, cpuLabel, cpuMhz,
                cpuArch, ramMb, vramMb, osLabel, platform, installed, disks, ramUsedMb, ramUses, shares,
                remoteAllowed);
    }
}
