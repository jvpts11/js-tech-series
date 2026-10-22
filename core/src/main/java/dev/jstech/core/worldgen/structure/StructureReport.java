/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.google.gson.JsonObject;
import dev.jstech.core.worldgen.modify.StructureSetModifier;
import dev.jstech.core.worldgen.modify.WorldgenModifiers;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * {@code /jstech dump structures}: every structure set the server holds, as its worlds are made from it, with its
 * placement and its structures, each with its weight, its step and how many biomes it may stand in, those the
 * operator turned off marked; and then every worldgen modifier made as the server started, with what went wrong. The
 * names to write in {@code jstech-structures.toml} and in a datapack's modifiers are all here.
 */
public final class StructureReport {

    private static final String ROW_END = " |\n";
    private static final String CELL = " | ";
    private static final String SPACING = "spacing";
    private static final String SEPARATION = "separation";

    private StructureReport() {
    }

    /** The page, as plain Markdown. */
    public static String page(final MinecraftServer server) {
        final Registry<StructureSet> sets = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        final StringBuilder out = new StringBuilder("# Structures\n\nEach structure set, as the worlds of this server "
                + "are made from it.\n");
        final List<Holder.Reference<StructureSet>> sorted = sets.holders()
                .sorted(Comparator.comparing(set -> set.key().location().toString())).toList();
        for (final Holder.Reference<StructureSet> set : sorted) {
            out.append("\n## ").append(set.key().location()).append("\n\n").append(placement(set.value(),
                    server.registryAccess())).append("\n\n| Structure | Weight | Step | Biomes |\n")
                    .append("| --- | --- | --- | --- |\n");
            for (final StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                final Structure structure = entry.structure().value();
                final String id = entry.structure().unwrapKey().map(key -> key.location().toString())
                        .orElse("(unnamed)");
                final boolean off = entry.structure().unwrapKey()
                        .map(key -> OperatorStructures.isOff(key.location())).orElse(false);
                out.append("| ").append(id).append(off ? " (off)" : "").append(CELL).append(entry.weight())
                        .append(CELL).append(structure.step().getSerializedName()).append(CELL)
                        .append(structure.biomes().size()).append(ROW_END);
            }
        }
        out.append("\n## Worldgen modifiers made as the server started\n\n");
        final List<WorldgenModifiers.Made> made = WorldgenModifiers.made();
        if (made.isEmpty()) {
            out.append("None.\n");
        }
        for (final WorldgenModifiers.Made one : made) {
            out.append("- ").append(one.id()).append(": ").append(one.kind()).append(" on ").append(one.target())
                    .append(one.done() ? "" : ", left as it was: " + String.join("; ", one.problems())).append('\n');
        }
        return out.toString();
    }

    /*
     * How a set's placement spreads its structures, in words: its kind, and its spacing and separation for one that
     * has them (the game's spread at random, the Core's, another mod's), or its rings.
     */
    private static String placement(final StructureSet set, final RegistryAccess registries) {
        final StructurePlacement placement = set.placement();
        final ResourceLocation kind = BuiltInRegistries.STRUCTURE_PLACEMENT.getKey(placement.type());
        if (placement instanceof ConcentricRingsStructurePlacement rings) {
            return "Placed by " + kind + ", in rings round the world's middle: " + rings.count() + " structures, "
                    + rings.spread() + " in the first ring, the rings " + rings.distance() + " apart.";
        }
        final Optional<JsonObject> fields = StructureSetModifier.placementOf(set, registries);
        if (fields.isPresent() && fields.get().has(SPACING) && fields.get().has(SEPARATION)) {
            return "Placed by " + kind + ": spacing " + fields.get().get(SPACING).getAsInt() + " chunks, separation "
                    + fields.get().get(SEPARATION).getAsInt() + " chunks.";
        }
        return "Placed by " + kind + ".";
    }
}
