/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.guide.ReaderMarks;
import dev.jstech.core.log.CoreLogs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * What a reader keeps in each manual, on this client's disk, the reader's own and carried from world to world as
 * where they left it is: the entries they have seen, so the contents say what is new since; their bookmarks, each a
 * ribbon of a colour hanging over its page; and the notes they wrote in pages' margins. A page is kept as where it
 * stands in its entry, section or chapter ({@link ManualPlaces.Place}), so a mark stays on its words when a resource
 * pack or another language lays the pages out differently.
 */
public final class ManualMarks {

    /** How many colours the ribbons come in. */
    public static final int COLOURS = 3;
    /** The most bookmarks a manual keeps. */
    public static final int MOST_BOOKMARKS = 8;
    /** The longest note, in characters. */
    public static final int MOST_NOTE = 160;

    private static final Logger LOGGER = CoreLogs.GUIDE.logger();
    private static final String FOLDER = "jstech";
    private static final String FILE = "manual_marks.json";
    private static final String SEEN = "seen";
    private static final String BOOKMARKS = "bookmarks";
    private static final String NOTES = "notes";
    private static final String AT = "at";
    private static final String PAGES = "pages";
    private static final String COLOUR = "colour";
    private static final String TEXT = "text";

    private static final Map<String, Kept> KEPT = new HashMap<>();
    private static boolean read;

    private ManualMarks() {
    }

    /** Whether the manual of that id was ever opened on this client, so what it held then is known. */
    public static synchronized boolean opened(final String manual) {
        readOnce();
        return KEPT.containsKey(manual);
    }

    /** The entries of the manual of that id the reader has seen. */
    public static synchronized Set<String> seen(final String manual) {
        readOnce();
        final Kept kept = KEPT.get(manual);
        return kept == null ? Set.of() : Set.copyOf(kept.seen);
    }

    /** Notes the entries as seen, and writes the disk when one was not. */
    public static synchronized void see(final String manual, final Collection<String> entries) {
        readOnce();
        final boolean known = KEPT.containsKey(manual);
        final Kept kept = KEPT.computeIfAbsent(manual, id -> new Kept());
        if (kept.seen.addAll(entries) || !known) {
            write();
        }
    }

    /** The manual's bookmarks, in the order they were made. */
    public static synchronized List<Bookmark> bookmarks(final String manual) {
        readOnce();
        final Kept kept = KEPT.get(manual);
        return kept == null ? List.of() : List.copyOf(kept.bookmarks);
    }

    /**
     * Marks the page at {@code place} with a ribbon of the colour the fewest wear; nothing when it is marked already or
     * the manual holds as many bookmarks as it keeps.
     *
     * @return the page's bookmark, made now or before; none when there is no room for one
     */
    public static synchronized Optional<Bookmark> mark(final String manual, final ManualPlaces.Place place) {
        readOnce();
        final Kept kept = KEPT.computeIfAbsent(manual, id -> new Kept());
        for (final Bookmark bookmark : kept.bookmarks) {
            if (bookmark.place().equals(place)) {
                return Optional.of(bookmark);
            }
        }
        if (kept.bookmarks.size() >= MOST_BOOKMARKS) {
            return Optional.empty();
        }
        final Bookmark made = new Bookmark(place, ReaderMarks.nextColour(kept.bookmarks.stream()
                .map(Bookmark::colour).toList(), COLOURS));
        kept.bookmarks.add(made);
        write();
        return Optional.of(made);
    }

    /** Takes the bookmark off the page at {@code place}, if it has one. */
    public static synchronized void unmark(final String manual, final ManualPlaces.Place place) {
        readOnce();
        final Kept kept = KEPT.get(manual);
        if (kept != null && kept.bookmarks.removeIf(bookmark -> bookmark.place().equals(place))) {
            write();
        }
    }

    /** The note written on the page at {@code place}, if any. */
    public static synchronized Optional<String> note(final String manual, final ManualPlaces.Place place) {
        readOnce();
        final Kept kept = KEPT.get(manual);
        return kept == null ? Optional.empty() : Optional.ofNullable(kept.notes.get(place));
    }

    /** Writes {@code text} on the page at {@code place}, cut to the longest a note is; nothing written takes it off. */
    public static synchronized void write(final String manual, final ManualPlaces.Place place, final String text) {
        readOnce();
        final Kept kept = KEPT.computeIfAbsent(manual, id -> new Kept());
        final String stripped = text.strip();
        final String written = stripped.length() > MOST_NOTE ? stripped.substring(0, MOST_NOTE) : stripped;
        final String before = written.isEmpty() ? kept.notes.remove(place) : kept.notes.put(place, written);
        if (!written.equals(before)) {
            write();
        }
    }

    /** Forgets that the reader saw an entry, as when it came into the book after they read it (client tests do). */
    public static synchronized void unsee(final String manual, final String entry) {
        readOnce();
        final Kept kept = KEPT.get(manual);
        if (kept != null && kept.seen.remove(entry)) {
            write();
        }
    }

    /** Forgets the manual of that id, as a reader who never opened it (client tests do). */
    public static synchronized void forget(final String manual) {
        readOnce();
        if (KEPT.remove(manual) != null) {
            write();
        }
    }

    /** Forgets what is held and reads the disk again, as a game opened anew would (client tests do). */
    public static synchronized void readAgain() {
        KEPT.clear();
        read = false;
        readOnce();
    }

    private static void readOnce() {
        if (read) {
            return;
        }
        read = true;
        final Path file = path();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            final JsonElement all = JsonParser.parseString(Files.readString(file));
            if (!all.isJsonObject()) {
                return;
            }
            for (final Map.Entry<String, JsonElement> manual : all.getAsJsonObject().entrySet()) {
                if (manual.getValue().isJsonObject()) {
                    KEPT.put(manual.getKey(), Kept.read(manual.getValue().getAsJsonObject()));
                }
            }
        } catch (final IOException | RuntimeException e) {
            // A file spoiled by hand only loses the marks; every page is as a new reader finds it.
            LOGGER.warn("The manuals' bookmarks and notes could not be read from {}", file, e);
        }
    }

    private static void write() {
        final JsonObject all = new JsonObject();
        KEPT.forEach((manual, kept) -> all.add(manual, kept.json()));
        final Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, all.toString());
        } catch (final IOException e) {
            LOGGER.warn("The manuals' bookmarks and notes could not be written to {}", file, e);
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER).resolve(FILE);
    }

    /**
     * A page marked with a ribbon.
     *
     * @param place  where the page stands
     * @param colour the ribbon's colour, from 0
     */
    public record Bookmark(ManualPlaces.Place place, int colour) {
    }

    /* What one manual keeps. */
    private static final class Kept {

        private final Set<String> seen = new LinkedHashSet<>();
        private final List<Bookmark> bookmarks = new ArrayList<>();
        private final Map<ManualPlaces.Place, String> notes = new LinkedHashMap<>();

        private static Kept read(final JsonObject json) {
            final Kept kept = new Kept();
            if (json.has(SEEN) && json.get(SEEN).isJsonArray()) {
                json.getAsJsonArray(SEEN).forEach(entry -> kept.seen.add(entry.getAsString()));
            }
            if (json.has(BOOKMARKS) && json.get(BOOKMARKS).isJsonArray()) {
                for (final JsonElement one : json.getAsJsonArray(BOOKMARKS)) {
                    final JsonObject bookmark = one.getAsJsonObject();
                    kept.bookmarks.add(new Bookmark(place(bookmark), Math.floorMod(bookmark.get(COLOUR).getAsInt(),
                            COLOURS)));
                }
            }
            if (json.has(NOTES) && json.get(NOTES).isJsonArray()) {
                for (final JsonElement one : json.getAsJsonArray(NOTES)) {
                    final JsonObject note = one.getAsJsonObject();
                    kept.notes.put(place(note), note.get(TEXT).getAsString());
                }
            }
            return kept;
        }

        private static ManualPlaces.Place place(final JsonObject json) {
            return new ManualPlaces.Place(json.has(AT) ? json.get(AT).getAsString() : "",
                    json.has(PAGES) ? json.get(PAGES).getAsInt() : 0);
        }

        private JsonObject json() {
            final JsonObject json = new JsonObject();
            final JsonArray seenJson = new JsonArray();
            this.seen.forEach(seenJson::add);
            json.add(SEEN, seenJson);
            final JsonArray bookmarksJson = new JsonArray();
            for (final Bookmark bookmark : this.bookmarks) {
                final JsonObject one = at(bookmark.place());
                one.addProperty(COLOUR, bookmark.colour());
                bookmarksJson.add(one);
            }
            json.add(BOOKMARKS, bookmarksJson);
            final JsonArray notesJson = new JsonArray();
            this.notes.forEach((place, text) -> {
                final JsonObject one = at(place);
                one.addProperty(TEXT, text);
                notesJson.add(one);
            });
            json.add(NOTES, notesJson);
            return json;
        }

        private static JsonObject at(final ManualPlaces.Place place) {
            final JsonObject json = new JsonObject();
            json.addProperty(AT, place.target());
            json.addProperty(PAGES, place.pagesIn());
            return json;
        }
    }
}
