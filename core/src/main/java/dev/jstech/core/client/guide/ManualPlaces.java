/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * Where the player left each manual, kept on this client's disk so a book opens again where it was closed, the game
 * closed and opened in between. A place is the entry, section or chapter whose pages were open and how many pages into
 * it, rather than a page's number, so the book still opens at the right words when a resource pack or another language
 * lays its pages out differently; a book closed at its cover opens at its cover.
 */
public final class ManualPlaces {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER = "jstech";
    private static final String FILE = "manuals.json";
    private static final String AT = "at";
    private static final String PAGES = "pages";

    private static final Map<String, Place> PLACES = new HashMap<>();
    private static boolean read;

    private ManualPlaces() {
    }

    /** Where the manual of that id was left, if it was ever closed on this client. */
    public static synchronized Optional<Place> of(final String manual) {
        readOnce();
        return Optional.ofNullable(PLACES.get(manual));
    }

    /** Keeps where the manual of that id was left, and writes it to the disk at once. */
    public static synchronized void leave(final String manual, final Place place) {
        readOnce();
        if (place.equals(PLACES.put(manual, place))) {
            return;
        }
        write();
    }

    /** Forgets what is held and reads the disk again, as a game opened anew would (client tests do). */
    public static synchronized void readAgain() {
        PLACES.clear();
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
            final JsonElement kept = JsonParser.parseString(Files.readString(file));
            if (!kept.isJsonObject()) {
                return;
            }
            for (final Map.Entry<String, JsonElement> manual : kept.getAsJsonObject().entrySet()) {
                if (manual.getValue().isJsonObject()) {
                    final JsonObject place = manual.getValue().getAsJsonObject();
                    PLACES.put(manual.getKey(), new Place(place.has(AT) ? place.get(AT).getAsString() : "",
                            place.has(PAGES) ? place.get(PAGES).getAsInt() : 0));
                }
            }
        } catch (final IOException | RuntimeException e) {
            // A file spoiled by hand only loses the places; every book opens at its cover again.
            LOGGER.warn("Where the manuals were left could not be read from {}", file, e);
        }
    }

    private static void write() {
        final JsonObject all = new JsonObject();
        PLACES.forEach((manual, place) -> {
            final JsonObject written = new JsonObject();
            written.addProperty(AT, place.target());
            written.addProperty(PAGES, place.pagesIn());
            all.add(manual, written);
        });
        final Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, all.toString());
        } catch (final IOException e) {
            LOGGER.warn("Where the manuals were left could not be written to {}", file, e);
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER).resolve(FILE);
    }

    /**
     * Where a manual was left.
     *
     * @param target  the entry, section or chapter whose pages were open, or empty for the cover
     * @param pagesIn how many pages past where it starts
     */
    public record Place(String target, int pagesIn) {

        /** A book closed at its cover. */
        public static final Place COVER = new Place("", 0);

        public Place {
            target = target == null ? "" : target;
            pagesIn = Math.max(0, pagesIn);
        }

        /** Whether the book was closed at its cover. */
        public boolean cover() {
            return this.target.isEmpty();
        }
    }
}
