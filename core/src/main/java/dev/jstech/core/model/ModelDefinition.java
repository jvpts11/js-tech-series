/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * How a model is drawn, as its definition file says beside it ({@code assets/<mod>/jsmodels/<name>.model.json}),
 * written by the data run from what a mod declares: the model file, the textures it is painted with, the animation
 * files it plays from besides its own, whether it is see-through, which bones glow besides those named {@code _glow},
 * whether a block drawn with it shows the cracks of being broken, its variants, each chosen by the block's state:
 * another model or texture, and bones hidden or shown, and the detail it is drawn in: its size beside its file's, how
 * many triangles at most, and how many far away. Every file is named as {@code <mod>:<path>} from the mod's
 * {@code assets} folder. Pure.
 *
 * @param model       the model file
 * @param textures    the textures, the first for every face that names none; empty for those the model carries
 * @param animations  the animation files
 * @param translucent whether it is drawn see-through, its parts sorted back to front
 * @param glow        the bones that glow besides those whose names end in {@code _glow}
 * @param cracks      whether a block drawn with it shows the cracks of being broken
 * @param variants    its variants, the first whose state matches chosen
 * @param glowLight   the light its glowing parts cast on the world around, from 0 (none) to 15
 * @param glowColour  the colour they glow in, {@code 0xRRGGBB}, for their halo and their light
 * @param detail      the detail it is drawn in
 */
public record ModelDefinition(String model, List<String> textures, List<String> animations, boolean translucent,
                              Set<String> glow, boolean cracks, List<Variant> variants, int glowLight,
                              int glowColour, Detail detail) {

    /** White, the colour a glow is unless its definition says another. */
    public static final int WHITE = 0xFFFFFF;
    private static final int BRIGHTEST = 15;
    private static final int HEX = 16;

    public ModelDefinition {
        Objects.requireNonNull(model, "model");
        textures = List.copyOf(textures);
        animations = List.copyOf(animations);
        glow = Set.copyOf(glow);
        variants = List.copyOf(variants);
        glowLight = Math.clamp(glowLight, 0, BRIGHTEST);
        glowColour = glowColour & WHITE;
        Objects.requireNonNull(detail, "detail");
    }

    /** The definition of a model drawn as its file is, with nothing else. */
    public static ModelDefinition of(final String model, final List<String> textures) {
        return new ModelDefinition(model, textures, List.of(), false, Set.of(), true, List.of(), 0, WHITE,
                Detail.AS_MADE);
    }

    /**
     * The variant a block of state {@code state} is drawn as, its properties by name: the first whose every property
     * matches; none for a state no variant names.
     */
    public Variant variantFor(final Map<String, String> state) {
        for (final Variant variant : this.variants) {
            if (variant.matches(state)) {
                return variant;
            }
        }
        return Variant.NONE;
    }

    /** The definition as its file writes it. */
    public JsonObject toJson() {
        final JsonObject out = new JsonObject();
        out.addProperty("model", this.model);
        if (!this.textures.isEmpty()) {
            out.add("textures", strings(this.textures));
        }
        if (!this.animations.isEmpty()) {
            out.add("animations", strings(this.animations));
        }
        if (this.translucent) {
            out.addProperty("translucent", true);
        }
        if (!this.glow.isEmpty()) {
            out.add("glow", strings(new TreeSet<>(this.glow)));
        }
        if (!this.cracks) {
            out.addProperty("cracks", false);
        }
        if (!this.variants.isEmpty()) {
            final JsonArray variantArray = new JsonArray();
            for (final Variant variant : this.variants) {
                variantArray.add(variant.toJson());
            }
            out.add("variants", variantArray);
        }
        if (this.glowLight > 0) {
            out.addProperty("glow_light", this.glowLight);
        }
        if (this.glowColour != WHITE) {
            out.addProperty("glow_colour", String.format(Locale.ROOT, "#%06X", this.glowColour));
        }
        this.detail.into(out);
        return out;
    }

    /**
     * The definition a file writes.
     *
     * @throws ModelFormatException when it names no model
     */
    public static ModelDefinition fromJson(final String name, final JsonObject json) {
        if (!json.has("model")) {
            throw new ModelFormatException("the model definition " + name + " names no model file");
        }
        final List<Variant> variants = new ArrayList<>();
        if (json.has("variants")) {
            for (final JsonElement each : json.getAsJsonArray("variants")) {
                variants.add(Variant.fromJson(each.getAsJsonObject()));
            }
        }
        return new ModelDefinition(json.get("model").getAsString(), strings(json, "textures"),
                strings(json, "animations"), ModelJson.flag(json, "translucent", false),
                new TreeSet<>(strings(json, "glow")), ModelJson.flag(json, "cracks", true), variants,
                json.has("glow_light") ? json.get("glow_light").getAsInt() : 0, colour(name, json),
                Detail.fromJson(name, json));
    }

    /* The colour its glow is in, written #RRGGBB; white when it says none. */
    private static int colour(final String name, final JsonObject json) {
        if (!json.has("glow_colour")) {
            return WHITE;
        }
        final String text = json.get("glow_colour").getAsString();
        try {
            return Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, HEX);
        } catch (final NumberFormatException e) {
            throw new ModelFormatException("the model definition " + name + " glows in a colour that does not read: "
                    + text);
        }
    }

    private static JsonArray strings(final Iterable<String> values) {
        final JsonArray out = new JsonArray();
        values.forEach(out::add);
        return out;
    }

    private static List<String> strings(final JsonObject json, final String key) {
        final List<String> out = new ArrayList<>();
        if (json.has(key)) {
            for (final JsonElement each : json.getAsJsonArray(key)) {
                out.add(each.getAsString());
            }
        }
        return out;
    }

    /**
     * A way of drawing the model chosen by a block's state: when every property it names has the value it gives, the
     * model is drawn from another file or painted with another texture, and the bones it hides or shows are.
     *
     * @param when    the properties, by name, and the values they must have
     * @param model   the model file drawn instead, or empty for the definition's
     * @param texture the texture painted with instead, or empty for the definition's
     * @param hidden  the bones hidden
     * @param shown   the bones shown that the model hides
     */
    public record Variant(Map<String, String> when, String model, String texture, Set<String> hidden,
                          Set<String> shown) {

        /** The variant of a state no variant names: the definition as it is. */
        public static final Variant NONE = new Variant(Map.of(), "", "", Set.of(), Set.of());

        public Variant {
            when = Map.copyOf(when);
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(texture, "texture");
            hidden = Set.copyOf(hidden);
            shown = Set.copyOf(shown);
        }

        /** Whether {@code state} has every value it names. */
        public boolean matches(final Map<String, String> state) {
            for (final Map.Entry<String, String> wanted : this.when.entrySet()) {
                if (!wanted.getValue().equals(state.get(wanted.getKey()))) {
                    return false;
                }
            }
            return true;
        }

        JsonObject toJson() {
            final JsonObject out = new JsonObject();
            final JsonObject whenObject = new JsonObject();
            new TreeMap<>(this.when).forEach(whenObject::addProperty);
            out.add("when", whenObject);
            if (!this.model.isEmpty()) {
                out.addProperty("model", this.model);
            }
            if (!this.texture.isEmpty()) {
                out.addProperty("texture", this.texture);
            }
            if (!this.hidden.isEmpty()) {
                out.add("hide", strings(new TreeSet<>(this.hidden)));
            }
            if (!this.shown.isEmpty()) {
                out.add("show", strings(new TreeSet<>(this.shown)));
            }
            return out;
        }

        static Variant fromJson(final JsonObject json) {
            final Map<String, String> when = new LinkedHashMap<>();
            if (json.has("when")) {
                json.getAsJsonObject("when").entrySet().forEach(entry -> when.put(entry.getKey(),
                        entry.getValue().getAsString()));
            }
            return new Variant(when, ModelJson.string(json, "model", ""), ModelJson.string(json, "texture", ""),
                    new TreeSet<>(strings(json, "hide")), new TreeSet<>(strings(json, "show")));
        }
    }

    /**
     * The detail a model is drawn in: how many times bigger than its file says, its file's units taken as they are
     * (a metre a block, so a pump of 600 mm is 0.6 blocks); the most triangles it is drawn with, its meshes brought
     * down to them as they are read; and from how far away it is drawn with fewer.
     *
     * @param scale        how many times bigger than its file says
     * @param triangles    the most triangles it is drawn with near; 0 for as many as its file has
     * @param farFrom      from how many blocks away it is drawn with {@code farTriangles}; 0 for never
     * @param farTriangles the most triangles it is drawn with from there
     */
    public record Detail(double scale, int triangles, int farFrom, int farTriangles) {

        /** As its file has it, at every distance. */
        public static final Detail AS_MADE = new Detail(1.0, 0, 0, 0);
        /**
         * The most triangles a model may have without saying how many it is drawn with: a file past them, as a CAD
         * program's drawing of every screw, stops the generation of data until its declaration brings it down.
         */
        public static final int MOST_UNSAID = 20_000;

        public Detail {
            if (!(scale > 0) || triangles < 0 || farFrom < 0 || farTriangles < 0 || farFrom > 0 && farTriangles == 0) {
                throw new IllegalArgumentException("a model's detail is a scale above 0, triangles of 0 or more, "
                        + "and triangles far away when it says from how far: " + scale + ", " + triangles + ", "
                        + farFrom + ", " + farTriangles);
            }
        }

        /** Whether it is drawn with fewer triangles far away. */
        public boolean hasFar() {
            return this.farFrom > 0;
        }

        /** The most triangles it may be drawn with near: what it says, or {@link #MOST_UNSAID}. */
        public int most() {
            return this.triangles > 0 ? this.triangles : MOST_UNSAID;
        }

        void into(final JsonObject out) {
            if (this.scale != 1.0) {
                out.addProperty("scale", this.scale);
            }
            if (this.triangles > 0) {
                out.addProperty("triangles", this.triangles);
            }
            if (this.farFrom > 0) {
                final JsonObject far = new JsonObject();
                far.addProperty("from", this.farFrom);
                far.addProperty("triangles", this.farTriangles);
                out.add("far", far);
            }
        }

        static Detail fromJson(final String name, final JsonObject json) {
            final JsonObject far = json.has("far") ? json.getAsJsonObject("far") : new JsonObject();
            try {
                return new Detail(json.has("scale") ? json.get("scale").getAsDouble() : 1.0,
                        json.has("triangles") ? json.get("triangles").getAsInt() : 0,
                        far.has("from") ? far.get("from").getAsInt() : 0,
                        far.has("triangles") ? far.get("triangles").getAsInt() : 0);
            } catch (final IllegalArgumentException wrong) {
                throw new ModelFormatException("the model definition " + name + " has a detail that cannot be: "
                        + wrong.getMessage());
            }
        }
    }
}
