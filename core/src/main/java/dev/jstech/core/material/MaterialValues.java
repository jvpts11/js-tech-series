/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The values a data pack gives materials' properties, over those their declarations give: one file per material,
 * {@code data/<namespace>/material_values/<material>.json} for the material {@code <namespace>:<material>}, read again
 * whenever the server reloads its data and sent to every player. A file can also say when a property's line shows in
 * that material's tooltip.
 *
 * <pre>{@code
 * {
 *   "values": { "mymod:molar_mass": 55.845, "jscore:symbol": "Fe" },
 *   "shown": { "jscore:atomic_number": "always" }
 * }
 * }</pre>
 */
public final class MaterialValues {

    /** The files, by material id. */
    public static final DataRegistry<File> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "material_values"), "material_values", File.CODEC)
            .synced().register();

    private MaterialValues() {
    }

    /** Declares the files, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The value of {@code property} for {@code material} on the side {@code level} is on: a file's, or its own. */
    public static <T> Optional<T> value(final Material material, final MaterialProperty<T> property,
                                        final Level level) {
        return value(material, property, level.isClientSide());
    }

    /** The value of {@code property} for {@code material}, on the player's side or the server's. */
    public static <T> Optional<T> value(final Material material, final MaterialProperty<T> property,
                                        final boolean playerSide) {
        final File file = file(material, playerSide);
        if (file != null) {
            final Dynamic<?> given = file.values().get(property.id());
            if (given != null) {
                final Optional<T> read = property.codec().parse(given).result();
                if (read.isPresent()) {
                    return read;
                }
            }
        }
        return material.value(property);
    }

    /** When {@code property}'s line shows in {@code material}'s tooltip: as a file says, or as the material says. */
    public static MaterialProperty.Shown shown(final Material material, final MaterialProperty<?> property,
                                               final boolean playerSide) {
        final File file = file(material, playerSide);
        if (file != null) {
            final MaterialProperty.Shown given = file.shown().get(property.id());
            if (given != null) {
                return given;
            }
        }
        return material.shown(property);
    }

    /** The lines of {@code material}'s tooltip, property by property in the order they were declared. */
    public static List<Line> lines(final Material material, final boolean playerSide) {
        final List<Line> out = new ArrayList<>();
        for (final MaterialProperty<?> property : MaterialProperties.all()) {
            final MaterialProperty.Shown shown = shown(material, property, playerSide);
            if (shown == MaterialProperty.Shown.NEVER) {
                continue;
            }
            value(material, property, playerSide).ifPresent(value -> out.add(new Line(property, property.read(value),
                    shown)));
        }
        return out;
    }

    private static @Nullable File file(final Material material, final boolean playerSide) {
        final Map<ResourceLocation, File> files = playerSide ? FILES.clientEntries() : FILES.entries();
        return files.get(material.id());
    }

    /**
     * One material's file.
     *
     * @param values the values it gives, by property id, each read with its property's codec when asked for
     * @param shown  when the lines it names show, by property id
     */
    public record File(Map<ResourceLocation, Dynamic<?>> values, Map<ResourceLocation, MaterialProperty.Shown> shown) {

        /** How a file is read. */
        public static final Codec<File> CODEC = RecordCodecBuilder.create(file -> file.group(
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.PASSTHROUGH).optionalFieldOf("values", Map.of())
                        .forGetter(File::values),
                Codec.unboundedMap(ResourceLocation.CODEC, MaterialProperty.Shown.CODEC)
                        .optionalFieldOf("shown", Map.of()).forGetter(File::shown)
        ).apply(file, File::new));

        public File {
            values = Map.copyOf(values);
            shown = Map.copyOf(shown);
        }
    }

    /**
     * One line of a material's tooltip.
     *
     * @param property the property
     * @param value    its value, as it reads
     * @param shown    when it shows: always or with Shift
     */
    public record Line(MaterialProperty<?> property, Text value, MaterialProperty.Shown shown) {
    }
}
