/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * Reads USD, the scenes Pixar's format carries between Blender, Maya, Houdini and the rest: a layer as text
 * ({@code .usda}), in binary ({@code .usdc}), or packed with its pictures ({@code .usdz}, its first layer the stage's);
 * the prims it references, in itself or in other files beside it or in its package, taken in where they are
 * referenced; and the stage made a model ({@link UsdScene}). Sublayers, variants other than the default, payloads
 * loaded later and the other arcs of composition are let go. Pure.
 */
public final class UsdReader {

    private final Function<String, byte[]> files;
    private final Map<String, UsdPrim> layers = new HashMap<>();

    private static final int MOST_DEPTH = 8;
    private static final List<String> LAYER_ENDINGS = List.of(".usda", ".usdc", ".usd");

    private UsdReader(final Function<String, byte[]> files) {
        this.files = files;
    }

    /**
     * The model a USD file holds.
     *
     * @param beside the bytes of a file the stage names beside it, by the path it writes; null for one not there
     * @throws ModelFormatException when it is not one
     */
    public static ModelData read(final String name, final byte[] bytes, final Function<String, byte[]> beside) {
        byte[] layer = bytes;
        Function<String, byte[]> files = beside;
        if (bytes.length > 2 && bytes[0] == 'P' && bytes[1] == 'K') {
            final PackageFiles packed = PackageFiles.of(name, bytes);
            String first = null;
            for (final String path : packed.paths()) {
                if (LAYER_ENDINGS.stream().anyMatch(path::endsWith)) {
                    first = path;
                    break;
                }
            }
            if (first == null) {
                throw new ModelFormatException("the USD package " + name + " holds no layer");
            }
            final String root = first;
            layer = packed.file(root).orElseThrow();
            files = path -> packed.file(PackageFiles.resolve(root, strip(path))).orElseGet(() -> beside.apply(path));
        }
        final Function<String, byte[]> found = files;
        final UsdReader reader = new UsdReader(found);
        final UsdPrim stage = reader.layer(name, layer);
        reader.compose(stage, stage, 0);
        return new UsdScene(name, stage, path -> found.apply(strip(path))).model();
    }

    /** Whether {@code bytes} begin as a USD layer or package does. */
    public static boolean readsUsd(final byte[] bytes) {
        return UsdCrate.isCrate(bytes) || UsdaParser.isText(bytes)
                || bytes.length > 2 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private UsdPrim layer(final String name, final byte[] bytes) {
        if (UsdCrate.isCrate(bytes)) {
            return UsdCrate.read(name, bytes);
        }
        if (UsdaParser.isText(bytes)) {
            return UsdaParser.parse(name, new String(bytes, StandardCharsets.UTF_8));
        }
        throw new ModelFormatException("the file " + name + " is not a USD layer");
    }

    /* Takes the prims every prim references into it, in the same layer or another, deepest first. */
    private void compose(final UsdPrim stage, final UsdPrim prim, final int depth) {
        if (depth > MOST_DEPTH) {
            return;
        }
        for (final Reference reference : references(prim.metadata.get("references"))) {
            final UsdPrim source = source(stage, reference);
            if (source != null && source != prim) {
                merge(prim, source, source.path, prim.path);
            }
        }
        for (final UsdPrim child : new ArrayList<>(prim.children)) {
            compose(stage, child, depth + 1);
        }
    }

    /* The prim a reference names: in this stage, or the default prim of another layer, or one at its path there. */
    @Nullable
    private UsdPrim source(final UsdPrim stage, final Reference reference) {
        UsdPrim layer = stage;
        if (!reference.asset().isEmpty()) {
            layer = this.layers.computeIfAbsent(reference.asset().toLowerCase(Locale.ROOT), key -> {
                final byte[] bytes = this.files.apply(strip(reference.asset()));
                if (bytes == null) {
                    return null;
                }
                final UsdPrim read = layer(reference.asset(), bytes);
                compose(read, read, 1);
                return read;
            });
            if (layer == null) {
                return null;
            }
        }
        if (!reference.path().isEmpty()) {
            return layer.find(reference.path());
        }
        final Object named = layer.metadata.get("defaultPrim");
        if (named != null) {
            return layer.find("/" + named);
        }
        return layer.children.isEmpty() ? null : layer.children.getFirst();
    }

    /*
     * Takes {@code source}'s properties and children into {@code into} where it has none of its own, every path
     * within the source made the same path within the prim.
     */
    private static void merge(final UsdPrim into, final UsdPrim source, final String from, final String to) {
        if (into.type.isEmpty()) {
            into.type = source.type;
        }
        source.metadata.forEach(into.metadata::putIfAbsent);
        source.properties.forEach((propertyName, property) -> {
            if (into.properties.containsKey(propertyName)) {
                return;
            }
            final UsdPrim.Property copy = into.property(propertyName);
            copy.typeName = property.typeName;
            copy.value = property.value;
            copy.relationship = property.relationship;
            copy.samples.putAll(property.samples);
            copy.metadata.putAll(property.metadata);
            for (final String target : property.targets) {
                copy.targets.add(target.startsWith(from) ? to + target.substring(from.length()) : target);
            }
        });
        for (final UsdPrim child : source.children) {
            final UsdPrim made = into.child(child.name);
            if (made.specifier.equals("def") && !child.specifier.equals("def")) {
                made.specifier = child.specifier;
            }
            merge(made, child, from, to);
        }
    }

    /* The references a prim's metadata writes, in either layer's way: paths, assets, or "asset@path". */
    private static List<Reference> references(@Nullable final Object written) {
        final List<Reference> out = new ArrayList<>();
        if (written instanceof List<?> list) {
            for (final Object each : list) {
                out.addAll(references(each));
            }
        } else if (written instanceof UsdPrim.Asset asset) {
            out.add(new Reference(asset.path(), ""));
        } else if (written instanceof String text) {
            final int at = text.indexOf('@');
            if (at >= 0) {
                out.add(new Reference(text.substring(0, at), text.substring(at + 1)));
            } else if (text.startsWith("/")) {
                out.add(new Reference("", text));
            }
        }
        return out;
    }

    /* A path as the file writes it, without the "./" before it. */
    private static String strip(final String path) {
        return path.startsWith("./") ? path.substring(2) : path;
    }

    /* A reference: the asset it is in, empty for this stage, and the path of the prim, empty for its default. */
    private record Reference(String asset, String path) {
    }
}
