/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.SoundfoundryCovers;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The server's music catalogue: the albums its owner put in {@code config/jstech/soundfoundry/catalog/}, a folder
 * each, and those the data packs carry in {@code soundfoundry/catalog/}.
 *
 * <p>An album is a folder of Ogg Vorbis and Wave files, with an {@code album.json} beside them when its owner wants
 * to name it; see {@link CatalogAssembly} for what is read from where. Every song is kept in the server's store of
 * recordings, once, and is sent from there to whoever plays it. Reading a catalogue of many albums means reading
 * every byte of it, so it is done off the game's thread, when the server has started, when its data packs are
 * reloaded, and when an operator asks; the new catalogue takes the old one's place in one step.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class SoundfoundryCatalog {

    /** Where the albums are, under the server's config folder and under a data pack's namespace. */
    public static final String FOLDER = "soundfoundry/catalog";
    /** What an album's own notes are called, beside its songs. */
    public static final String NOTES = "album.json";
    /** What an album's cover is called, beside its songs: a PNG, or a JPEG under the same name. */
    public static final String COVER = "cover";

    private static final Logger LOGGER = LogUtils.getLogger();
    /* The largest song read into the catalogue: far past any real one, well short of what memory holds. */
    private static final long MOST_BYTES = 1L << 30;
/* The largest notes file and cover read: real ones are a few kilobytes, so a bigger one is not what it claims. */
    private static final long NOTES_MOST_BYTES = 1L << 20;
    private static final long COVER_MOST_BYTES = 16L << 20;
    private static final String FROM_CONFIG = "config";
    /* The keys an album's notes are written under. */
    private static final String NOTES_TITLE = "title";
    private static final String NOTES_ARTIST = "artist";
    private static final String NOTES_YEAR = "year";
    /* Which reading is the latest, so one that finishes after a later one began is not taken. */
    private static final AtomicInteger READING = new AtomicInteger();

    private static volatile Snapshot current = Snapshot.EMPTY;

    private SoundfoundryCatalog() {
    }

    /** The catalogue as it was last read. */
    public static Snapshot current() {
        return current;
    }

    /** Every recording the catalogue offers, which the server keeps however long nobody plays it. */
    public static List<MediaId> media() {
        final List<MediaId> media = new ArrayList<>();
        for (final CatalogAlbum album : current.albums()) {
            for (final CatalogTrack track : album.tracks()) {
                media.add(track.media());
            }
        }
        return media;
    }

    /** The folder the server's own albums are in. */
    public static Path configFolder() {
        return FMLPaths.CONFIGDIR.get().resolve("jstech").resolve(FOLDER);
    }

    /**
     * Reads the catalogue again, off the game's thread, and puts it in place of the last one.
     *
     * @param resources the data packs to read, which a reload hands over before the server holds them
     * @param done      told what was read, on the server's thread; may be null
     */
    public static void reload(final MinecraftServer server, final ResourceManager resources,
                              @Nullable final Consumer<Snapshot> done) {
        final int reading = READING.incrementAndGet();
        final MediaStore store = MediaStore.current().orElse(null);
        if (store == null || !ComputersServerConfig.soundfoundryCatalog()) {
            current = Snapshot.EMPTY;
            if (done != null) {
                done.accept(Snapshot.EMPTY);
            }
            return;
        }
        final Path folder = configFolder();
        CompletableFuture.supplyAsync(() -> load(folder, resources, store), Util.ioPool())
                .exceptionally(failed -> {
                    LOGGER.error("The Soundfoundry catalogue could not be read", failed);
                    // A failed reading leaves the catalogue that was there rather than emptying it.
                    return null;
                })
                .thenAcceptAsync(read -> {
                    if (reading != READING.get()) {
                        // A later reading took over, so these counts are of a catalogue nobody will see.
                        return;
                    }
                    if (read != null) {
                        current = read;
                        SoundfoundryCovers.forget();
                        LOGGER.info("Soundfoundry catalogue: {} albums, {} songs, {} files passed over",
                                read.albums().size(), read.songs(), read.skipped());
                    }
                    if (done != null) {
                        done.accept(current);
                    }
                }, server);
    }

    /**
     * Reads every album in {@code folder} and in the data packs, keeping their songs in {@code store}, on whatever
     * thread calls it. A file that cannot be read is passed over, counted and written in the log.
     */
    public static Snapshot load(final Path folder, final ResourceManager resources, final MediaStore store) {
        final Reading reading = new Reading(store);
        readFolder(folder, reading);
        readDataPacks(resources, reading);
        final List<CatalogAlbum> albums = new ArrayList<>(reading.albums);
        albums.sort(Comparator.comparing((CatalogAlbum album) -> album.title().toLowerCase(Locale.ROOT))
                .thenComparing(CatalogAlbum::id));
        int songs = 0;
        for (final CatalogAlbum album : albums) {
            songs += album.tracks().size();
        }
        return new Snapshot(albums, songs, reading.skipped, reading.covers);
    }

    @SubscribeEvent
    public static void onServerStarted(final ServerStartedEvent event) {
        final MinecraftServer server = event.getServer();
        reload(server, server.getResourceManager(), null);
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        READING.incrementAndGet();
        current = Snapshot.EMPTY;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(final AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    private static void readFolder(final Path folder, final Reading reading) {
        try {
            Files.createDirectories(folder);
        } catch (final IOException cannotMake) {
            LOGGER.warn("The Soundfoundry catalogue folder {} could not be made: {}", folder, cannotMake.getMessage());
            return;
        }
        try (Stream<Path> listed = Files.list(folder)) {
            for (final Path album : listed.filter(Files::isDirectory).sorted().toList()) {
                readAlbumFolder(album, reading);
            }
        } catch (final IOException unreadable) {
            LOGGER.warn("The Soundfoundry catalogue folder {} could not be read: {}", folder, unreadable.getMessage());
        }
    }

    private static void readAlbumFolder(final Path album, final Reading reading) {
        final String name = album.getFileName().toString();
        final List<CatalogAssembly.Found> found = new ArrayList<>();
        CatalogAssembly.Notes notes = CatalogAssembly.Notes.NONE;
        try (Stream<Path> listed = Files.list(album)) {
            for (final Path file : listed.filter(Files::isRegularFile).sorted().toList()) {
                // A file that cannot be read is passed over on its own, so the files after it still count.
                notes = dispatch(file.getFileName().toString(), file.toString(), FROM_CONFIG + "/" + name,
                        most -> Files.size(file) > most ? null : Files.readAllBytes(file), reading, found, notes);
            }
        } catch (final IOException unreadable) {
            reading.passOver(album.toString(), unreadable.getMessage());
        }
        reading.add(FROM_CONFIG + "/" + name, name, notes, found);
    }

    private static void readDataPacks(final ResourceManager resources, final Reading reading) {
        final String prefix = FOLDER + "/";
        final Map<String, List<Map.Entry<ResourceLocation, Resource>>> byAlbum = new TreeMap<>();
        for (final Map.Entry<ResourceLocation, Resource> entry : resources.listResources(FOLDER,
                location -> wanted(location.getPath())).entrySet()) {
            final String rest = entry.getKey().getPath().substring(prefix.length());
            final int slash = rest.indexOf('/');
            if (slash > 0 && rest.indexOf('/', slash + 1) < 0) {
                byAlbum.computeIfAbsent(entry.getKey().getNamespace() + "/" + rest.substring(0, slash),
                        album -> new ArrayList<>()).add(entry);
            }
        }
        for (final Map.Entry<String, List<Map.Entry<ResourceLocation, Resource>>> album : byAlbum.entrySet()) {
            final String id = album.getKey();
            final List<CatalogAssembly.Found> found = new ArrayList<>();
            CatalogAssembly.Notes notes = CatalogAssembly.Notes.NONE;
            final List<Map.Entry<ResourceLocation, Resource>> files = new ArrayList<>(album.getValue());
            files.sort(Map.Entry.comparingByKey());
            for (final Map.Entry<ResourceLocation, Resource> file : files) {
                final String path = file.getKey().getPath();
                notes = dispatch(path.substring(path.lastIndexOf('/') + 1), file.getKey().toString(), id, most -> {
                    try (InputStream in = file.getValue().open()) {
                        // Reads one byte past the cap and no further, so a huge file never fills memory.
                        final byte[] bytes = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8L, most + 1L));
                        return bytes.length > most ? null : bytes;
                    }
                }, reading, found, notes);
            }
            reading.add(id, id.substring(id.indexOf('/') + 1), notes, found);
        }
    }

    /*
     * What to do with one file of an album, whichever place it came from: its notes are read, its cover is made
     * small, a song is kept, and anything else is ignored. The place only supplies the bytes. Returns the notes the
     * album has afterwards.
     */
    private static CatalogAssembly.Notes dispatch(final String fileName, final String where, final String albumId,
                                                  final Bytes source, final Reading reading,
                                                  final List<CatalogAssembly.Found> found,
                                                  final CatalogAssembly.Notes notes) {
        final boolean isNotes = fileName.equalsIgnoreCase(NOTES);
        final boolean isCover = !isNotes && isCover(fileName);
        if (!isNotes && !isCover && songKind(fileName) == null) {
            return notes;
        }
        final long most = isNotes ? NOTES_MOST_BYTES : isCover ? COVER_MOST_BYTES : MOST_BYTES;
        try {
            final byte[] bytes = source.read(most);
            if (bytes == null) {
                reading.tooLarge(where);
            } else if (isNotes) {
                return notesOf(new String(bytes, StandardCharsets.UTF_8), where);
            } else if (isCover) {
                reading.cover(albumId, bytes, where);
            } else {
                reading.keep(found, fileName, bytes, where);
            }
        } catch (final IOException unreadable) {
            reading.passOver(where, unreadable.getMessage());
        }
        return notes;
    }

    private static boolean wanted(final String path) {
        final String name = path.substring(path.lastIndexOf('/') + 1);
        return name.equalsIgnoreCase(NOTES) || isCover(name) || songKind(name) != null;
    }

    /* Whether a file beside the songs is the album's cover. */
    private static boolean isCover(final String fileName) {
        final String name = fileName.toLowerCase(Locale.ROOT);
        return name.equals(COVER + ".png") || name.equals(COVER + ".jpg") || name.equals(COVER + ".jpeg");
    }

    /* The kind of song a file is by its name, or null for a file that is not one. */
    @Nullable
    private static FileType songKind(final String fileName) {
        final int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? null
                : FileType.forRecording(fileName.substring(dot + 1).toLowerCase(Locale.ROOT)).orElse(null);
    }

    private static CatalogAssembly.Notes notesOf(final String json, final String where) {
        try {
            final JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                LOGGER.warn("{} is not an object of notes, and is left out", where);
                return CatalogAssembly.Notes.NONE;
            }
            final JsonObject object = parsed.getAsJsonObject();
            return new CatalogAssembly.Notes(text(object, NOTES_TITLE), text(object, NOTES_ARTIST),
                    text(object, NOTES_YEAR));
        } catch (final JsonParseException malformed) {
            LOGGER.warn("{} could not be read, and is left out: {}", where, malformed.getMessage());
            return CatalogAssembly.Notes.NONE;
        }
    }

    private static String text(final JsonObject object, final String key) {
        final JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    /**
     * The catalogue as one reading found it.
     *
     * @param albums  its albums, by title
     * @param songs   how many songs they hold together
     * @param skipped how many files could not be read and were passed over
     * @param covers  the covers of the albums that have one beside their songs, by album, each a small PNG
     */
    public record Snapshot(List<CatalogAlbum> albums, int songs, int skipped, Map<String, byte[]> covers) {

        /** A catalogue with nothing in it. */
        public static final Snapshot EMPTY = new Snapshot(List.of(), 0, 0, Map.of());

        public Snapshot {
            albums = List.copyOf(albums);
            covers = Map.copyOf(covers);
        }

        /** The cover of that album, or null when it has none of its own. */
        @Nullable
        public byte[] cover(final String album) {
            return covers.get(album);
        }
    }

    /* Where the bytes of one file come from: null when the file is larger than {@code most}, without holding it. */
    @FunctionalInterface
    private interface Bytes {

        @Nullable
        byte[] read(long most) throws IOException;
    }

    /** What one reading has found so far. */
    private static final class Reading {

        private final MediaStore store;
        private final List<CatalogAlbum> albums = new ArrayList<>();
        private final Map<String, byte[]> covers = new HashMap<>();
        private int skipped;

        Reading(final MediaStore store) {
            this.store = store;
        }

        void keep(final List<CatalogAssembly.Found> found, final String fileName, final byte[] bytes,
                  final String where) {
            final FileType kind = songKind(fileName);
            try {
                final MediaId media = store.put(bytes, kind.extension());
                found.add(new CatalogAssembly.Found(fileName, media, store.info(media)));
            } catch (final IOException notASong) {
                passOver(where, notASong.getMessage());
            }
        }

        /* An album's cover, made small; one that is no picture is passed over like a song that is no song. */
        void cover(final String album, final byte[] bytes, final String where) {
            final byte[] small = SoundfoundryCovers.scaled(bytes);
            if (small.length == 0) {
                skipped++;
                LOGGER.warn("Soundfoundry catalogue: {} was passed over: it is not a picture that can be read",
                        where);
            } else {
                covers.put(album, small);
            }
        }

        void passOver(final String where, final String why) {
            skipped++;
            LOGGER.warn("Soundfoundry catalogue: {} was passed over: {}", where, why);
        }

        void tooLarge(final String where) {
            skipped++;
            LOGGER.warn("Soundfoundry catalogue: {} was passed over: it is larger than the most a file of its kind"
                    + " may be", where);
        }

        void add(final String id, final String folder, final CatalogAssembly.Notes notes,
                 final List<CatalogAssembly.Found> found) {
            if (!found.isEmpty()) {
                albums.add(CatalogAssembly.album(id, folder, notes, found));
            }
        }
    }

    /**
     * Reads the catalogue again when the server's data packs are reloaded. The first loading comes before the server
     * exists and keeps nothing: the catalogue is read once it has started.
     */
    private static final class Listener extends SimplePreparableReloadListener<Void> {

        @Override
        protected Void prepare(final ResourceManager manager, final ProfilerFiller profiler) {
            return null;
        }

        @Override
        protected void apply(final Void nothing, final ResourceManager manager, final ProfilerFiller profiler) {
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && server.isRunning()) {
                // Named in full: a reload listener has a reload of its own.
                SoundfoundryCatalog.reload(server, manager, null);
            }
        }
    }
}
