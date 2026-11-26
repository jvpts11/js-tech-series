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
import dev.jstech.core.text.TextBounds;
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
 * <p>The disks the computer reads recipe files from, and one folder of one of them: its folders and its {@code .craft}
 * files; what the computer keeps, by place: the ROM of each Crafting Card with its bench recipes, then each Crafting
 * Interface it drives with its processing and pipeline recipes; and each interface as the Interfaces tab lists it,
 * with how many interfaces the cards drive and how many on the cable wait for another card.
 *
 * <p>Only the answer to a {@link RequestCraftManagerPayload} lists a folder: the answer to an action lists none
 * ({@link #listed()} is false), and a view keeps the folder it shows, which it asks for again after the action.
 *
 * @param view           the view the listing answers, as the request counted it; {@link #NO_LISTING} when it lists
 *                       nothing
 * @param sources        every disk the computer reads: its own, then the medium in each linked drive
 * @param mediaVolumeKey the disk listed: {@link #SYSTEM_DISK}, a medium's {@code media:<readerPos>} key, or
 *                       {@code ""} when none
 * @param mediaLabel     the disk's display label (e.g. "Floppy (A:)"), or empty when none
 * @param folder         the folder listed, {@code ""} for the disk's top
 * @param folders        the names of the folders in it (up to 64)
 * @param mediaFiles     the names of the {@code .craft} files in it (up to 64)
 * @param places         the cards' ROMs, then the interfaces, with what each holds
 * @param hasCard        true when the computer has a Crafting Card installed (actions are enabled)
 * @param status         a short status line for the manager
 * @param statusWarns    whether that line reports something the player has to act on, such as a full ROM
 * @param interfaces     the interfaces the computer drives, as the Interfaces tab lists them
 * @param budget         how many interfaces the computer's cards drive together
 * @param waiting        how many interfaces on its crafting cable wait, driven by none, for another card
 */
public record CraftManagerStatePayload(int view, List<WireSource> sources, String mediaVolumeKey, Text mediaLabel,
                                       String folder, List<String> folders, List<String> mediaFiles,
                                       List<WirePlace> places, boolean hasCard, Text status, boolean statusWarns,
                                       List<WireInterface> interfaces, int budget, int waiting)
        implements CustomPacketPayload {

    /** The key of the computer's own disk among the sources. */
    public static final String SYSTEM_DISK = "system";
    /** The view of an answer that lists no folder. */
    public static final int NO_LISTING = -1;
    public static final int MAX_SOURCES = 12;
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
        sources = List.copyOf(sources);
        folders = List.copyOf(folders);
        mediaFiles = List.copyOf(mediaFiles);
        places = List.copyOf(places);
        interfaces = List.copyOf(interfaces);
    }

    @Override
    public CustomPacketPayload.Type<CraftManagerStatePayload> type() {
        return TYPE;
    }

    /** Whether this lists a folder, as the answer to a request does. */
    public boolean listed() {
        return view != NO_LISTING;
    }

    /** This state with the folder {@code shown} lists, for a view that keeps what it shows over an action. */
    public CraftManagerStatePayload withListingOf(final CraftManagerStatePayload shown) {
        return new CraftManagerStatePayload(shown.view, shown.sources, shown.mediaVolumeKey, shown.mediaLabel,
                shown.folder, shown.folders, shown.mediaFiles, places, hasCard, status, statusWarns, interfaces,
                budget, waiting);
    }

    /** Every entry of every place, in listing order. */
    public List<WireRomEntry> entries() {
        final List<WireRomEntry> all = new ArrayList<>();
        places.forEach(place -> all.addAll(place.entries()));
        return all;
    }

    /**
     * One disk the computer reads.
     *
     * @param key    {@link #SYSTEM_DISK}, or the medium's {@code media:} key
     * @param label  what it is called
     * @param usable whether recipe files can be read from it and written to it: a disk with a filesystem, a
     *               writable medium; an installer disc in a drive is listed only so drive letters count as the
     *               file explorer counts them
     */
    public record WireSource(String key, Text label, boolean usable) {
    }

    /**
     * One recipe a place holds.
     *
     * @param ref      the place's number and its own, packed, which an action names it by
     * @param name     what it is called
     * @param fileName the name its {@code .craft} file takes, so a view can mark the recipes the folder it shows holds
     * @param kind     {@link #BENCH}, {@link #PROCESSING} or {@link #PIPELINE}
     */
    public record WireRomEntry(int ref, Text name, String fileName, byte kind) {
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
     * @param name      what it is called: the machine it feeds, unless a player named it
     * @param patterns  how many patterns it holds
     * @param capacity  how many it holds at most
     * @param exclusive whether it runs one recipe at a time
     * @param state     {@link #IDLE}, {@link #RUNNING}, {@link #PAUSED}, {@link #NO_MACHINE} or {@link #DRAINING}
     * @param detail    what it runs now, for a running interface
     * @param maxJobs   the most jobs at once, 0 for as many as come
     */
    public record WireInterface(int place, Text name, int patterns, int capacity, boolean exclusive, byte state,
                                Text detail, int maxJobs) {
    }

    private static void write(final RegistryFriendlyByteBuf buf, final CraftManagerStatePayload p) {
        buf.writeVarInt(p.view);
        final List<WireSource> sources = p.sources.subList(0, Math.min(MAX_SOURCES, p.sources.size()));
        buf.writeVarInt(sources.size());
        for (final WireSource source : sources) {
            buf.writeUtf(cut(source.key()), MAX_TEXT);
            TextCodecs.STREAM_CODEC.encode(buf, source.label());
            buf.writeBoolean(source.usable());
        }
        buf.writeUtf(cut(p.mediaVolumeKey), MAX_TEXT);
        TextCodecs.STREAM_CODEC.encode(buf, p.mediaLabel);
        buf.writeUtf(TextBounds.clip(p.folder, FsPaths.MAX_PATH_LENGTH), FsPaths.MAX_PATH_LENGTH);
        writeNames(buf, p.folders);
        writeNames(buf, p.mediaFiles);
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
                buf.writeUtf(TextBounds.clip(entry.fileName(), FsPaths.MAX_NAME_LENGTH), FsPaths.MAX_NAME_LENGTH);
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
        final int view = buf.readVarInt();
        final int sourceCount = Math.min(MAX_SOURCES, buf.readVarInt());
        final List<WireSource> sources = new ArrayList<>();
        for (int i = 0; i < sourceCount; i++) {
            sources.add(new WireSource(buf.readUtf(MAX_TEXT), TextCodecs.STREAM_CODEC.decode(buf),
                    buf.readBoolean()));
        }
        final String key = buf.readUtf(MAX_TEXT);
        final Text label = TextCodecs.STREAM_CODEC.decode(buf);
        final String folder = buf.readUtf(FsPaths.MAX_PATH_LENGTH);
        final List<String> folders = readNames(buf);
        final List<String> files = readNames(buf);
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
                entries.add(new WireRomEntry(buf.readVarInt(), TextCodecs.STREAM_CODEC.decode(buf),
                        buf.readUtf(FsPaths.MAX_NAME_LENGTH), buf.readByte()));
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
                    buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readByte(),
                    TextCodecs.STREAM_CODEC.decode(buf), buf.readVarInt()));
        }
        final int budget = buf.readVarInt();
        final int waiting = buf.readVarInt();
        return new CraftManagerStatePayload(view, sources, key, label, folder, folders, files, places, hasCard,
                status, warns, interfaces, budget, waiting);
    }

    /* A list of names, as many as one field carries, each as long as a filesystem name may be. */
    private static void writeNames(final RegistryFriendlyByteBuf buf, final List<String> names) {
        final List<String> sent = names.subList(0, Math.min(MAX_MEDIA_FILES, names.size()));
        buf.writeVarInt(sent.size());
        sent.forEach(name -> buf.writeUtf(name, FsPaths.MAX_NAME_LENGTH));
    }

    private static List<String> readNames(final RegistryFriendlyByteBuf buf) {
        final int count = Math.min(MAX_MEDIA_FILES, buf.readVarInt());
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            names.add(buf.readUtf(FsPaths.MAX_NAME_LENGTH));
        }
        return names;
    }

    private static String cut(final String value) {
        return TextBounds.clip(value, MAX_TEXT);
    }
}
