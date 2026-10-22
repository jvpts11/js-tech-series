/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Reads a model file of any format the Core knows, by its name's ending: a Blockbench project ({@code .bbmodel}), a
 * Bedrock or GeckoLib geometry ({@code .geo.json}), glTF ({@code .gltf}, {@code .glb}), FBX ({@code .fbx}) or OBJ
 * ({@code .obj}); and the animations of a Bedrock or GeckoLib animation file ({@code .animation.json}). Every reader
 * is the Core's own, written from the formats' descriptions; none of them needs the program that wrote the file.
 * Pure.
 */
public final class ModelReaders {

    /** The endings of the model files read, the longest first, as a name is matched against them in turn. */
    public static final List<String> MODEL_ENDINGS = List.of(".geo.json", ".bbmodel", ".gltf", ".glb", ".fbx",
            ".obj", ".stl", ".3mf", ".step", ".stp", ".usda", ".usdc", ".usdz", ".usd");
    /** The ending of the animation files read. */
    public static final String ANIMATION_ENDING = ".animation.json";
    /* The endings of the files that hold parts as a CAD program or a scene draws them, each part made a bone. */
    private static final List<String> PART_ENDINGS = List.of(".stl", ".3mf", ".step", ".stp", ".usda", ".usdc",
            ".usdz", ".usd");

    private ModelReaders() {
    }

    /** Whether {@code file}'s name ends as a model file the Core reads. */
    public static boolean readsModel(final String file) {
        final String lower = file.toLowerCase(Locale.ROOT);
        for (final String ending : MODEL_ENDINGS) {
            if (lower.endsWith(ending)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code file} is of a format that holds the parts a CAD program or a scene drew, rather than bones made
     * for the game: STL, 3MF, STEP and USD. Each of its parts is read as a bone.
     */
    public static boolean fromParts(final String file) {
        final String lower = file.toLowerCase(Locale.ROOT);
        return PART_ENDINGS.stream().anyMatch(lower::endsWith);
    }

    /** What a model file is called without its ending, as {@code fan} for {@code fan.geo.json}. */
    public static String stem(final String file) {
        final String lower = file.toLowerCase(Locale.ROOT);
        final int slash = Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\'));
        for (final String ending : MODEL_ENDINGS) {
            if (lower.endsWith(ending)) {
                return file.substring(slash + 1, file.length() - ending.length());
            }
        }
        if (lower.endsWith(ANIMATION_ENDING)) {
            return file.substring(slash + 1, file.length() - ANIMATION_ENDING.length());
        }
        return file.substring(slash + 1);
    }

    /**
     * The models a file holds, by its name's ending: one for every format but a Bedrock geometry file, which can hold
     * several, the first being the one it is drawn with.
     *
     * @param file   the file's name or path, ending as one of {@link #MODEL_ENDINGS}
     * @param bytes  the file
     * @param beside the bytes of a file the model names beside it, by the name it writes; null for one not there
     * @throws ModelFormatException when the file is not of a format read, or not one of its format
     */
    public static List<ModelData> read(final String file, final byte[] bytes, final Function<String, byte[]> beside) {
        final String lower = file.toLowerCase(Locale.ROOT);
        final String name = stem(file);
        try {
            if (lower.endsWith(".geo.json")) {
                return BedrockReader.geometry(name, json(bytes));
            }
            if (lower.endsWith(".bbmodel")) {
                return List.of(BlockbenchReader.read(name, json(bytes)));
            }
            if (lower.endsWith(".gltf") || lower.endsWith(".glb")) {
                return List.of(GltfReader.read(name, bytes, beside));
            }
            if (lower.endsWith(".fbx")) {
                return List.of(FbxReader.read(name, bytes));
            }
            if (lower.endsWith(".obj")) {
                return List.of(ObjReader.read(name, bytes, beside));
            }
            if (lower.endsWith(".stl")) {
                return List.of(StlReader.read(name, bytes));
            }
            if (lower.endsWith(".3mf")) {
                return List.of(ThreeMfReader.read(name, bytes));
            }
            if (lower.endsWith(".step") || lower.endsWith(".stp")) {
                return List.of(StepReader.read(name, bytes));
            }
            if (lower.endsWith(".usda") || lower.endsWith(".usdc") || lower.endsWith(".usdz")
                    || lower.endsWith(".usd")) {
                return List.of(UsdReader.read(name, bytes, beside));
            }
        } catch (final JsonParseException | IllegalStateException | IllegalArgumentException
                       | IndexOutOfBoundsException | UnsupportedOperationException | ClassCastException broken) {
            // A file of the right ending but broken inside says what broke, not how deep in the reader it was found.
            throw new ModelFormatException("the model file " + file + " does not read: " + broken.getMessage(),
                    broken);
        }
        throw new ModelFormatException("the file " + file + " is of no model format the Core reads");
    }

    /**
     * The animations of a Bedrock or GeckoLib animation file.
     *
     * @throws ModelFormatException when it is not one
     */
    public static List<ModelAnimation> animations(final String file, final byte[] bytes) {
        try {
            return BedrockReader.animations(stem(file), json(bytes));
        } catch (final JsonParseException | IllegalStateException | IllegalArgumentException
                       | UnsupportedOperationException | ClassCastException broken) {
            throw new ModelFormatException("the animation file " + file + " does not read: " + broken.getMessage(),
                    broken);
        }
    }

    private static JsonObject json(final byte[] bytes) {
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
