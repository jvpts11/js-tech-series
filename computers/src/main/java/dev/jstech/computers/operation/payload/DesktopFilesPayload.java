/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.gui.CdeStyle;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.program.ComputerSettings;
import dev.jstech.computers.program.StartTiles;
import dev.jstech.core.text.TextBounds;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Server to client: the listing of the desktop folder for the Frames desktop background. Each entry
 * is a {@link DiskFilesPayload.WireFile} (path, extension, weight, read-only and directory flags),
 * rendered as an icon on the desktop. Reuses the Files app's wire type so there is one file model.
 *
 * <p>{@code iconCells} carries every free-positioned desktop icon's pinned grid cell, so the client
 * places those icons exactly where the player dropped them; an icon with no entry flows into the next
 * free auto-layout cell. {@code pinned} names the programs pinned to the panel, by program id path, in
 * the order they sit there, and {@code startTiles} the tiles of Frames 10's Start, each {@code <program>:<size>} in
 * the player's order. {@code defaultApps} holds the program chosen with Always for each extension.
 * {@code cdeStyle} is CDE's palette and backdrops as the machine keeps them, which only CDE reads.
 * {@code trashFull} says whether anything is in the desktop's trash, which is the picture its icon wears.
 * {@code sourceBuilt} names, by program id path, the installed programs the machine built from source, whose
 * windows hold a little less memory, so the desktop weighs a window the way the machine does.
 * {@code versions} gives, by program id path, the version each installed package is at, which is what an editor
 * reads the version of the language from: the machine's compiler knows the language up to its own number.
 * {@code loadMbPerSecond} is how fast the machine loads a program, its disk at its processor's pace, which is how long
 * the desktop shows a program's start under way.
 */
public record DesktopFilesPayload(List<DiskFilesPayload.WireFile> files, String wallpaper, String cdeStyle,
                                  String computerName, List<String> programs, List<String> sourceBuilt,
                                  List<WireIconCell> iconCells, Prefs prefs,
                                  List<WireCommunity> community, List<String> pinned, List<String> startTiles,
                                  Map<String, String> defaultApps, boolean trashFull, Map<String, String> versions,
                                  float loadMbPerSecond)
        implements CustomPacketPayload {

    public static final int MAX_FILES = 256;
    /*
     * Room for every program there is. It was sixteen, and a list is refused whole when it is longer than its
     * cap, so the seventeenth installed program took the machine's desktop away from it.
     */
    public static final int MAX_PROGRAMS = 128;
    public static final int MAX_ICON_CELLS = 256;
    public static final int MAX_PINNED = ComputerSettings.MAX_PINNED;
    public static final int MAX_TILES = StartTiles.MAX;
    public static final int MAX_DEFAULT_APPS = ComputerSettings.MAX_DEFAULT_APPS;

    /** How many player-written programs one desktop shows; the same cap the Mirror's shelf has. */
    public static final int MAX_COMMUNITY = 64;

    /**
     * A program the player installed from the Mirror, as the desktop needs it.
     *
     * <p>Only what it takes to put an icon on the desktop and start the thing: what it is called, which
     * icon it asked for, and the listing to run. Everything else about it stays on the server.
     */
    public record WireCommunity(String name, String icon, String entry) {
    }

    /**
     * The desktop-relevant per-computer settings the chrome applies: accent override, brightness, clock,
     * whether the taskbar app strip is centered (a Frames 11 look) or left-aligned, dark mode, the scale, and the
     * system's visual effects.
     */
    public record Prefs(int accent, int brightness, boolean clock12h, boolean taskbarCentered, boolean darkMode,
                        int scale, DesktopEffects effects) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Prefs> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeInt(p.accent);
                    buf.writeVarInt(p.brightness);
                    buf.writeBoolean(p.clock12h);
                    buf.writeBoolean(p.taskbarCentered);
                    buf.writeBoolean(p.darkMode);
                    buf.writeVarInt(p.scale);
                    DesktopEffects.STREAM_CODEC.encode(buf, p.effects);
                },
                buf -> new Prefs(buf.readInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                        buf.readBoolean(), buf.readVarInt(), DesktopEffects.STREAM_CODEC.decode(buf)));
    }

    /** One pinned desktop icon: its stable id ({@code app:<label>} / {@code file:<name>}) and packed grid cell. */
    public record WireIconCell(String key, int cell) {

        public static final StreamCodec<RegistryFriendlyByteBuf, WireIconCell> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(80), WireIconCell::key,
                        ByteBufCodecs.INT, WireIconCell::cell,
                        WireIconCell::new);
    }

    public static final CustomPacketPayload.Type<DesktopFilesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "desktop_files"));

    /*
     * Written out by hand: composite takes six pairs and this carries fourteen things. The alternative was
     * to bundle two of them into a record nobody else wants, which would have cost a reader more than
     * these two short methods do.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, DesktopFilesPayload> STREAM_CODEC =
            StreamCodec.of(DesktopFilesPayload::encode, DesktopFilesPayload::decode);

    private static void encode(final RegistryFriendlyByteBuf buf, final DesktopFilesPayload payload) {
        DiskFilesPayload.WireFile.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FILES))
                .encode(buf, payload.files);
        buf.writeUtf(payload.wallpaper, 48);
        buf.writeUtf(TextBounds.clip(payload.cdeStyle, CdeStyle.MOST_LETTERS), CdeStyle.MOST_LETTERS);
        /*
         * A name as long as a name may be. Written at that length, and cut to it rather than refused: this
         * packet is what puts a desktop in front of somebody, and a name a letter too long used to throw here
         * and leave them with no desktop at all.
         */
        buf.writeUtf(TextBounds.clip(payload.computerName, InstallerFlow.MOST_NAME_LETTERS),
                InstallerFlow.MOST_NAME_LETTERS);
        ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(MAX_PROGRAMS)).encode(buf, payload.programs);
        ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(MAX_PROGRAMS)).encode(buf, payload.sourceBuilt);
        WireIconCell.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ICON_CELLS)).encode(buf, payload.iconCells);
        Prefs.STREAM_CODEC.encode(buf, payload.prefs);
        buf.writeVarInt(Math.min(payload.community.size(), MAX_COMMUNITY));
        for (int i = 0; i < payload.community.size() && i < MAX_COMMUNITY; i++) {
            final WireCommunity one = payload.community.get(i);
            // Cut, never refused: a cap on writeUtf drops the connection, and these names are the player's.
            buf.writeUtf(TextBounds.clip(one.name(), 32), 32);
            buf.writeUtf(TextBounds.clip(one.icon(), 16), 16);
            buf.writeUtf(TextBounds.clip(one.entry(), 128), 128);
        }
        ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(MAX_PINNED)).encode(buf, payload.pinned);
        ByteBufCodecs.stringUtf8(40).apply(ByteBufCodecs.list(MAX_TILES)).encode(buf, payload.startTiles);
        buf.writeVarInt(Math.min(payload.defaultApps.size(), MAX_DEFAULT_APPS));
        int written = 0;
        for (final Map.Entry<String, String> one : payload.defaultApps.entrySet()) {
            if (written++ == MAX_DEFAULT_APPS) {
                break;
            }
            buf.writeUtf(TextBounds.clip(one.getKey(), 32), 32);
            buf.writeUtf(TextBounds.clip(one.getValue(), 64), 64);
        }
        buf.writeBoolean(payload.trashFull);
        buf.writeVarInt(Math.min(payload.versions.size(), MAX_PROGRAMS));
        int versioned = 0;
        for (final Map.Entry<String, String> one : payload.versions.entrySet()) {
            if (versioned++ == MAX_PROGRAMS) {
                break;
            }
            buf.writeUtf(TextBounds.clip(one.getKey(), 32), 32);
            buf.writeUtf(TextBounds.clip(one.getValue(), 16), 16);
        }
        buf.writeFloat(payload.loadMbPerSecond);
    }

    private static DesktopFilesPayload decode(final RegistryFriendlyByteBuf buf) {
        final List<DiskFilesPayload.WireFile> files =
                DiskFilesPayload.WireFile.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FILES)).decode(buf);
        final String wallpaper = buf.readUtf(48);
        final String cdeStyle = buf.readUtf(CdeStyle.MOST_LETTERS);
        final String computerName = buf.readUtf(InstallerFlow.MOST_NAME_LETTERS);
        final List<String> programs =
                ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(MAX_PROGRAMS)).decode(buf);
        final List<String> sourceBuilt =
                ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(MAX_PROGRAMS)).decode(buf);
        final List<WireIconCell> cells =
                WireIconCell.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ICON_CELLS)).decode(buf);
        final Prefs prefs = Prefs.STREAM_CODEC.decode(buf);
        final int count = Math.min(buf.readVarInt(), MAX_COMMUNITY);
        final List<WireCommunity> community = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            community.add(new WireCommunity(buf.readUtf(32), buf.readUtf(16), buf.readUtf(128)));
        }
        final List<String> pinned = ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(MAX_PINNED)).decode(buf);
        final List<String> startTiles =
                ByteBufCodecs.stringUtf8(40).apply(ByteBufCodecs.list(MAX_TILES)).decode(buf);
        final int apps = Math.min(buf.readVarInt(), MAX_DEFAULT_APPS);
        final Map<String, String> defaultApps = new LinkedHashMap<>();
        for (int i = 0; i < apps; i++) {
            defaultApps.put(buf.readUtf(32), buf.readUtf(64));
        }
        final boolean trashFull = buf.readBoolean();
        final int versionCount = Math.min(buf.readVarInt(), MAX_PROGRAMS);
        final Map<String, String> versions = new LinkedHashMap<>();
        for (int i = 0; i < versionCount; i++) {
            versions.put(buf.readUtf(32), buf.readUtf(16));
        }
        return new DesktopFilesPayload(files, wallpaper, cdeStyle, computerName, programs, sourceBuilt, cells, prefs,
                community, pinned, startTiles, defaultApps, trashFull, versions, buf.readFloat());
    }

    /* Copied on the way in, so what the desktop is handed cannot change under it after it arrives. */
    public DesktopFilesPayload {
        files = List.copyOf(files);
        programs = List.copyOf(programs);
        sourceBuilt = List.copyOf(sourceBuilt);
        iconCells = List.copyOf(iconCells);
        community = List.copyOf(community);
        pinned = List.copyOf(pinned);
        startTiles = List.copyOf(startTiles);
        defaultApps = Map.copyOf(defaultApps);
        versions = Map.copyOf(versions);
    }

    @Override
    public CustomPacketPayload.Type<DesktopFilesPayload> type() {
        return TYPE;
    }
}
