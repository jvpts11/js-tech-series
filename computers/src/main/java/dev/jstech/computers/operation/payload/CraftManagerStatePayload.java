/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the current state of the Crafting Manager for an open Crafting Computer.
 *
 * <p>The {@code .craft} files on the removable medium the computer reads; what the computer keeps, by place: the ROM
 * of each Crafting Card with its bench recipes, then each Crafting Interface it drives with its processing and
 * pipeline recipes, each entry marked when a file of its name is on the medium; and each interface as the Interfaces
 * tab lists it, with how many interfaces the cards drive and how many on the cable wait for another card.
 *
 * @param mediaVolumeKey the {@code media:<readerPos>} key of the medium, or {@code ""} when none
 * @param mediaLabel     the display label of the medium (e.g. "Floppy (A:)"), or empty when none
 * @param mediaFiles     file names of {@code .craft} files on the medium (up to 64)
 * @param places         the cards' ROMs, then the interfaces, with what each holds
 * @param hasCard        true when the computer has a Crafting Card installed (actions are enabled)
 * @param status         a short status line for the manager
 * @param statusWarns    whether that line reports something the player has to act on, such as a full ROM
 * @param interfaces     the interfaces the computer drives, as the Interfaces tab lists them
 * @param budget         how many interfaces the computer's cards drive together
 * @param waiting        how many interfaces on its crafting cable wait, driven by none, for another card
 */
public record CraftManagerStatePayload(String mediaVolumeKey, Text mediaLabel, List<String> mediaFiles,
                                       List<WirePlace> places, boolean hasCard, Text status, boolean statusWarns,
                                       List<WireInterface> interfaces, int budget, int waiting)
        implements CustomPacketPayload {

    public static final int MAX_MEDIA_FILES = 64;
    public static final int MAX_PLACES = 40;
    public static final int MAX_ENTRIES = 16;
    public static final int MAX_INTERFACES = 36;
    /** What an entry is: a bench recipe, a processing recipe or a pipeline. */
    public static final byte BENCH = 0;
    public static final byte PROCESSING = 1;
    public static final byte PIPELINE = 2;
    /** What an interface is doing. */
    public static final byte IDLE = 0;
    public static final byte RUNNING = 1;
    public static final byte PAUSED = 2;
    public static final byte NO_MACHINE = 3;
    public static final byte DRAINING = 4;
    private static final int MAX_TEXT = 64;

    public static final CustomPacketPayload.Type<CraftManagerStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "craft_manager_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftManagerStatePayload> STREAM_CODEC =
            StreamCodec.of(CraftManagerStatePayload::write, CraftManagerStatePayload::read);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public CraftManagerStatePayload {
        mediaFiles = List.copyOf(mediaFiles);
        places = List.copyOf(places);
        interfaces = List.copyOf(interfaces);
    }

    @Override
    public CustomPacketPayload.Type<CraftManagerStatePayload> type() {
        return TYPE;
    }

    /** Every entry of every place, in listing order. */
    public List<WireRomEntry> entries() {
        final List<WireRomEntry> all = new ArrayList<>();
        places.forEach(place -> all.addAll(place.entries()));
        return all;
    }

    /**
     * One recipe a place holds.
     *
     * @param ref     the place's number and its own, packed, which an action names it by
     * @param name    what it is called
     * @param inMedia true when a {@code .craft} file of its name is on the medium
     * @param kind    {@link #BENCH}, {@link #PROCESSING} or {@link #PIPELINE}
     */
    public record WireRomEntry(int ref, Text name, boolean inMedia, byte kind) {
    }

    /**
     * One place.
     *
     * @param card     true for a card's ROM, false for an interface
     * @param title    the card and its slot, or the interface's name
     * @param used     how many recipes it holds
     * @param capacity how many it holds at most
     * @param drives   for a card, how many interfaces it drives; 0 for an interface
     * @param entries  what it holds
     */
    public record WirePlace(boolean card, Text title, int used, int capacity, int drives,
                            List<WireRomEntry> entries) {

        public WirePlace {
            entries = List.copyOf(entries);
        }
    }

    /**
     * One interface on the Interfaces tab.
     *
     * @param place     its place's number, which an action names it by
     * @param name      what it is called
     * @param machine   the machine it feeds, or empty
     * @param patterns  how many patterns it holds
     * @param capacity  how many it holds at most
     * @param exclusive whether it runs one recipe at a time
     * @param state     {@link #IDLE}, {@link #RUNNING}, {@link #PAUSED}, {@link #NO_MACHINE} or {@link #DRAINING}
     * @param detail    what it runs now, for a running interface
     * @param maxJobs   the most jobs at once, 0 for as many as come
     */
    public record WireInterface(int place, Text name, Text machine, int patterns, int capacity, boolean exclusive,
                                byte state, Text detail, int maxJobs) {
    }

    private static void write(final RegistryFriendlyByteBuf buf, final CraftManagerStatePayload p) {
        buf.writeUtf(cut(p.mediaVolumeKey), MAX_TEXT);
        TextCodecs.STREAM_CODEC.encode(buf, p.mediaLabel);
        final List<String> files = p.mediaFiles.subList(0, Math.min(MAX_MEDIA_FILES, p.mediaFiles.size()));
        buf.writeVarInt(files.size());
        files.forEach(name -> buf.writeUtf(name, FsPaths.MAX_NAME_LENGTH));
        final List<WirePlace> places = p.places.subList(0, Math.min(MAX_PLACES, p.places.size()));
        buf.writeVarInt(places.size());
        for (final WirePlace place : places) {
            buf.writeBoolean(place.card());
            TextCodecs.STREAM_CODEC.encode(buf, place.title());
            buf.writeVarInt(place.used());
            buf.writeVarInt(place.capacity());
            buf.writeVarInt(place.drives());
            final List<WireRomEntry> entries = place.entries().subList(0,
                    Math.min(MAX_ENTRIES, place.entries().size()));
            buf.writeVarInt(entries.size());
            for (final WireRomEntry entry : entries) {
                buf.writeVarInt(entry.ref());
                TextCodecs.STREAM_CODEC.encode(buf, entry.name());
                buf.writeBoolean(entry.inMedia());
                buf.writeByte(entry.kind());
            }
        }
        buf.writeBoolean(p.hasCard);
        TextCodecs.STREAM_CODEC.encode(buf, p.status);
        buf.writeBoolean(p.statusWarns);
        final List<WireInterface> interfaces = p.interfaces.subList(0, Math.min(MAX_INTERFACES,
                p.interfaces.size()));
        buf.writeVarInt(interfaces.size());
        for (final WireInterface face : interfaces) {
            buf.writeVarInt(face.place());
            TextCodecs.STREAM_CODEC.encode(buf, face.name());
            TextCodecs.STREAM_CODEC.encode(buf, face.machine());
            buf.writeVarInt(face.patterns());
            buf.writeVarInt(face.capacity());
            buf.writeBoolean(face.exclusive());
            buf.writeByte(face.state());
            TextCodecs.STREAM_CODEC.encode(buf, face.detail());
            buf.writeVarInt(face.maxJobs());
        }
        buf.writeVarInt(p.budget);
        buf.writeVarInt(p.waiting);
    }

    private static CraftManagerStatePayload read(final RegistryFriendlyByteBuf buf) {
        final String key = buf.readUtf(MAX_TEXT);
        final Text label = TextCodecs.STREAM_CODEC.decode(buf);
        final int fileCount = Math.min(MAX_MEDIA_FILES, buf.readVarInt());
        final List<String> files = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            files.add(buf.readUtf(FsPaths.MAX_NAME_LENGTH));
        }
        final int placeCount = Math.min(MAX_PLACES, buf.readVarInt());
        final List<WirePlace> places = new ArrayList<>();
        for (int i = 0; i < placeCount; i++) {
            final boolean card = buf.readBoolean();
            final Text title = TextCodecs.STREAM_CODEC.decode(buf);
            final int used = buf.readVarInt();
            final int capacity = buf.readVarInt();
            final int drives = buf.readVarInt();
            final int entryCount = Math.min(MAX_ENTRIES, buf.readVarInt());
            final List<WireRomEntry> entries = new ArrayList<>();
            for (int e = 0; e < entryCount; e++) {
                entries.add(new WireRomEntry(buf.readVarInt(), TextCodecs.STREAM_CODEC.decode(buf), buf.readBoolean(),
                        buf.readByte()));
            }
            places.add(new WirePlace(card, title, used, capacity, drives, entries));
        }
        final boolean hasCard = buf.readBoolean();
        final Text status = TextCodecs.STREAM_CODEC.decode(buf);
        final boolean warns = buf.readBoolean();
        final int interfaceCount = Math.min(MAX_INTERFACES, buf.readVarInt());
        final List<WireInterface> interfaces = new ArrayList<>();
        for (int i = 0; i < interfaceCount; i++) {
            interfaces.add(new WireInterface(buf.readVarInt(), TextCodecs.STREAM_CODEC.decode(buf),
                    TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
                    buf.readByte(), TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt()));
        }
        final int budget = buf.readVarInt();
        final int waiting = buf.readVarInt();
        return new CraftManagerStatePayload(key, label, files, places, hasCard, status, warns, interfaces, budget,
                waiting);
    }

    private static String cut(final String value) {
        return value.length() > MAX_TEXT ? value.substring(0, MAX_TEXT) : value;
    }
}
