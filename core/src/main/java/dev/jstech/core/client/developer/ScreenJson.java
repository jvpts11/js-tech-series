/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.developer.DevSources;
import dev.jstech.core.gui.screen.ScreenFile;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * A screen's file as the editor of screens changes it: read from the resource packs, its components found by their
 * place in the tree, moved, copied and taken out, and written back to the mod's source as people write it, two spaces
 * a level.
 */
final class ScreenJson {

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String ROOT = "root";
    private static final String CHILDREN = "children";
    private static final String ASSETS = "assets";
    private static final String JSON = ".json";

    private ScreenJson() {
    }

    /**
     * The file of the screen {@code id} as the mod's source has it, or the resource packs when the source has none,
     * or none. The source comes first: what the editor wrote there is newer than the packs until they are read again.
     */
    static Optional<JsonObject> read(final ResourceLocation id) {
        final Optional<Path> source = sourceOf(id).filter(Files::isRegularFile);
        if (source.isPresent()) {
            try (Reader reader = Files.newBufferedReader(source.get(), StandardCharsets.UTF_8)) {
                return object(JsonParser.parseReader(reader));
            } catch (final IOException | RuntimeException unread) {
                return Optional.empty();
            }
        }
        return Minecraft.getInstance().getResourceManager().getResource(location(id)).flatMap(resource -> {
            try (Reader reader = resource.openAsReader()) {
                return object(JsonParser.parseReader(reader));
            } catch (final IOException | RuntimeException unread) {
                return Optional.empty();
            }
        });
    }

    /** The component at {@code path} of the file's tree, the root for the empty path. */
    static Optional<JsonObject> at(final JsonObject file, final List<Integer> path) {
        JsonElement at = file.get(ROOT);
        for (final int index : path) {
            if (at == null || !at.isJsonObject() || !at.getAsJsonObject().has(CHILDREN)) {
                return Optional.empty();
            }
            final JsonArray children = at.getAsJsonObject().getAsJsonArray(CHILDREN);
            if (index < 0 || index >= children.size()) {
                return Optional.empty();
            }
            at = children.get(index);
        }
        return at != null && at.isJsonObject() ? Optional.of(at.getAsJsonObject()) : Optional.empty();
    }

    /** The children of the component at {@code path}, made when it has none yet. */
    static Optional<JsonArray> childrenOf(final JsonObject file, final List<Integer> path) {
        return at(file, path).map(component -> {
            if (!component.has(CHILDREN) || !component.get(CHILDREN).isJsonArray()) {
                component.add(CHILDREN, new JsonArray());
            }
            return component.getAsJsonArray(CHILDREN);
        });
    }

    /** Takes the component at {@code path} out of its parent; the root stays. */
    static void remove(final JsonObject file, final List<Integer> path) {
        if (path.isEmpty()) {
            return;
        }
        childrenOf(file, path.subList(0, path.size() - 1)).ifPresent(siblings -> {
            final int index = path.get(path.size() - 1);
            if (index < siblings.size()) {
                siblings.remove(index);
            }
        });
    }

    /** Puts a copy of the component at {@code path} right after it; the place of the copy, or none for the root. */
    static Optional<List<Integer>> duplicate(final JsonObject file, final List<Integer> path) {
        if (path.isEmpty()) {
            return Optional.empty();
        }
        final List<Integer> parent = path.subList(0, path.size() - 1);
        return at(file, path).flatMap(component -> childrenOf(file, parent).map(siblings -> {
            final int index = path.get(path.size() - 1) + 1;
            insert(siblings, index, component.deepCopy());
            return placeOf(parent, index);
        }));
    }

    /** Moves the component at {@code path} among its siblings to {@code index}; its new place. */
    static List<Integer> move(final JsonObject file, final List<Integer> path, final int index) {
        if (path.isEmpty()) {
            return path;
        }
        final List<Integer> parent = path.subList(0, path.size() - 1);
        final Optional<JsonArray> siblings = childrenOf(file, parent);
        if (siblings.isEmpty()) {
            return path;
        }
        final int from = path.get(path.size() - 1);
        final JsonElement moved = siblings.get().remove(from);
        final int to = Math.max(0, Math.min(siblings.get().size(), index));
        insert(siblings.get(), to, moved);
        return placeOf(parent, to);
    }

    /** Adds {@code component} at the end of the children of the component at {@code container}; its place. */
    static Optional<List<Integer>> append(final JsonObject file, final List<Integer> container,
                                          final JsonObject component) {
        return childrenOf(file, container).map(children -> {
            children.add(component);
            return placeOf(container, children.size() - 1);
        });
    }

    /** The file as people write it. */
    static String pretty(final JsonObject file) {
        return PRETTY.toJson(file) + "\n";
    }

    /**
     * Writes the file of the screen {@code id} to its mod's source, where the build copies its resources from; the
     * file written, or none when this run has no source for the mod.
     */
    static Optional<Path> write(final ResourceLocation id, final JsonObject file) throws IOException {
        final Optional<Path> out = sourceOf(id);
        if (out.isEmpty()) {
            return Optional.empty();
        }
        Files.createDirectories(out.get().getParent());
        Files.writeString(out.get(), pretty(file), StandardCharsets.UTF_8);
        return out;
    }

    /** Where the screen {@code id}'s file is among the resource packs' files. */
    static ResourceLocation location(final ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), ScreenFile.FOLDER + "/" + id.getPath() + JSON);
    }

    /* Where the screen {@code id}'s file is in its mod's source, or none when this run has no source for the mod. */
    private static Optional<Path> sourceOf(final ResourceLocation id) {
        return DevSources.resourcesOf(id.getNamespace()).map(resources -> resources.resolve(ASSETS)
                .resolve(id.getNamespace()).resolve(ScreenFile.FOLDER).resolve(id.getPath() + JSON));
    }

    private static Optional<JsonObject> object(final JsonElement read) {
        return read.isJsonObject() ? Optional.of(read.getAsJsonObject()) : Optional.empty();
    }

    private static List<Integer> placeOf(final List<Integer> parent, final int index) {
        final List<Integer> place = new ArrayList<>(parent);
        place.add(index);
        return List.copyOf(place);
    }

    private static void insert(final JsonArray array, final int index, final JsonElement element) {
        array.add(element);
        for (int i = array.size() - 1; i > index; i--) {
            array.set(i, array.get(i - 1));
        }
        array.set(index, element);
    }
}
