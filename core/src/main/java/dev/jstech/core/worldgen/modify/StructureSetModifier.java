/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.modify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * {@code jscore:structure_set}: changes one of the game's or any mod's structure sets, by its id: what of its
 * placement {@code "placement"} says, field by field, over the placement as it was (its spacing, separation,
 * frequency, salt), the structures {@code "remove"} names taken out of it, and those {@code "add"} gives put in:
 *
 * <pre>{@code
 * {"type": "jscore:structure_set", "target": "minecraft:villages", "placement": {"spacing": 48, "separation": 16},
 *  "remove": ["minecraft:village_snowy"], "add": [{"structure": "mymod:hamlet", "weight": 2}]}
 * }</pre>
 *
 * <p>A set left with no structure places nothing. A structure's biomes, its spawns and its step are NeoForge's own
 * structure modifiers' to change.
 *
 * @param target    the set changed
 * @param placement the fields of its placement to set, over those it has
 * @param remove    the structures taken out of it
 * @param add       the structures put in it, each with its weight
 */
public record StructureSetModifier(ResourceKey<StructureSet> target, Optional<JsonElement> placement,
                                   List<ResourceKey<Structure>> remove,
                                   List<StructureSet.StructureSelectionEntry> add) implements IWorldgenModifier {

    /** The kind's id. */
    public static final ResourceLocation KIND = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_set");
    public static final MapCodec<StructureSetModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(Registries.STRUCTURE_SET).fieldOf("target").forGetter(StructureSetModifier::target),
            ExtraCodecs.JSON.optionalFieldOf("placement").forGetter(StructureSetModifier::placement),
            ResourceKey.codec(Registries.STRUCTURE).listOf().optionalFieldOf("remove", List.of())
                    .forGetter(StructureSetModifier::remove),
            StructureSet.StructureSelectionEntry.CODEC.listOf().optionalFieldOf("add", List.of())
                    .forGetter(StructureSetModifier::add)
    ).apply(i, StructureSetModifier::new));

    public StructureSetModifier {
        remove = List.copyOf(remove);
        add = List.copyOf(add);
    }

    /** {@code set}'s placement as its codec writes it, its fields by name; none when it cannot be written. */
    public static Optional<JsonObject> placementOf(final StructureSet set, final RegistryAccess registries) {
        return StructurePlacement.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), set.placement())
                .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject);
    }

    /**
     * {@code set} with the fields {@code fields} names set over its placement's, read and written with the placement's
     * own codec; what is wrong with them added to {@code problems}, the set then as it was.
     */
    public static StructureSet withPlacement(final StructureSet set, final JsonObject fields,
                                             final RegistryAccess registries, final List<String> problems) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        final DataResult<StructurePlacement> changed = StructurePlacement.CODEC.encodeStart(ops, set.placement())
                .flatMap(written -> {
                    final JsonObject merged = written.getAsJsonObject().deepCopy();
                    for (final Map.Entry<String, JsonElement> field : fields.entrySet()) {
                        merged.add(field.getKey(), field.getValue());
                    }
                    return StructurePlacement.CODEC.parse(ops, merged);
                });
        if (changed.error().isPresent()) {
            problems.add("its placement cannot take " + fields + ": " + changed.error().get().message());
            return set;
        }
        return new StructureSet(set.structures(), changed.getOrThrow());
    }

    @Override
    public ResourceLocation kind() {
        return KIND;
    }

    @Override
    public ResourceLocation changes() {
        return target.location();
    }

    @Override
    public boolean apply(final RegistryAccess registries, final List<String> problems) {
        final Optional<Holder.Reference<StructureSet>> holder = registries.registryOrThrow(Registries.STRUCTURE_SET)
                .getHolder(target);
        if (holder.isEmpty()) {
            problems.add("no structure set is called " + target.location());
            return false;
        }
        final List<StructureSet.StructureSelectionEntry> structures = new ArrayList<>();
        for (final StructureSet.StructureSelectionEntry entry : holder.get().value().structures()) {
            if (entry.structure().unwrapKey().filter(remove::contains).isEmpty()) {
                structures.add(entry);
            }
        }
        structures.addAll(add);
        StructureSet changed = new StructureSet(structures, holder.get().value().placement());
        if (placement.isPresent()) {
            if (!placement.get().isJsonObject()) {
                problems.add("its placement is written as an object of the fields to set, not " + placement.get());
                return false;
            }
            final int known = problems.size();
            changed = withPlacement(changed, placement.get().getAsJsonObject(), registries, problems);
            if (problems.size() > known) {
                return false;
            }
        }
        WorldgenModifiers.rebind(holder.get(), changed);
        return true;
    }
}
