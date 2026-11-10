/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import com.google.gson.JsonObject;
import dev.jstech.computers.JsComputers;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.worldgen.modify.StructureSetModifier;
import dev.jstech.core.worldgen.modify.WorldgenModifiers;
import dev.jstech.core.worldgen.structure.OperatorStructures;
import dev.jstech.core.worldgen.structure.StructureFit;
import dev.jstech.core.worldgen.structure.Structures;
import dev.jstech.core.worldgen.structure.TemplateChoosers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The abandoned buildings as structures (computers/docs/design/survival.md, "Abandoned buildings"), on J's Core's kit:
 * each a structure of its own whose start is one of its templates, one for each era it may be of, working and in
 * ruin, picked as it is placed ({@link AbandonedChooser}); its machines and chests filled as it is placed
 * ({@link AbandonedMachines}). On land in the Overworld, on ground flat enough, away from other structures. As the
 * server starts, the buildings it turns off are laid no more, and their spacing is the server's.
 */
public final class AbandonedBuildings {

    private static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(
            Registries.STRUCTURE_PROCESSOR, JsComputers.MODID);
    /** {@code jsc:abandoned_machines}: see {@link AbandonedMachines}. */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<AbandonedMachines>>
            MACHINES = PROCESSORS.register("abandoned_machines", () -> () -> AbandonedMachines.CODEC);
    /** The chooser of the buildings' templates, as their pools name it. */
    public static final ResourceLocation CHOOSER = id("abandoned");
    /* The ground a building stands on may rise this much across it; it stands from the shore to the hills. */
    private static final int SLOPE = 4;
    private static final int LOWEST = 55;
    private static final int HIGHEST = 180;
    /* A building keeps this many chunks from every other structure, and two of a kind a quarter of their spacing. */
    private static final int CLEAR_OF = 2;
    private static final int SEPARATION = 4;
    private static final String SPACING = "spacing";
    private static final String APART = "separation";

    static {
        for (final AbandonedBuilding building : AbandonedBuilding.values()) {
            Structures.structure(structure(building)).chosen(CHOOSER, templates(building))
                    .biomes(BiomeTags.IS_OVERWORLD).spacing(building.spacing(), building.spacing() / SEPARATION)
                    .clearOf(CLEAR_OF).fit(new StructureFit(building.reach(), SLOPE, LOWEST, HIGHEST, true,
                            building.sunk()))
                    .foundation(() -> Blocks.DIRT).declare();
        }
    }

    private AbandonedBuildings() {
    }

    /** Registers the processor and the chooser, and tunes the buildings as each server starts. */
    public static void register(final IEventBus modBus) {
        AbandonedBlocks.init();
        PROCESSORS.register(modBus);
        TemplateChoosers.register(CHOOSER, new AbandonedChooser());
        // After the Core's own changes to structures, which run a step earlier.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ServerAboutToStartEvent.class,
                event -> tune(event.getServer().registryAccess()));
    }

    /** The structure {@code building} is. */
    public static ResourceLocation structure(final AbandonedBuilding building) {
        return id("abandoned/" + building.serializedName());
    }

    /** {@code building}'s template of {@code era}, working or in ruin. */
    public static ResourceLocation template(final AbandonedBuilding building, final Era era, final boolean working) {
        return id("abandoned/" + building.serializedName() + "/" + era.path() + (working ? "_working" : "_ruin"));
    }

    /** The building whose template {@code template} is. */
    static AbandonedBuilding ofTemplate(final ResourceLocation template) {
        final String path = template.getPath();
        for (final AbandonedBuilding building : AbandonedBuilding.values()) {
            if (path.startsWith("abandoned/" + building.serializedName() + "/")) {
                return building;
            }
        }
        throw new IllegalArgumentException("no abandoned building has the template " + template);
    }

    /* Every template of {@code building}: each of its eras, working and in ruin. */
    private static List<ResourceLocation> templates(final AbandonedBuilding building) {
        final List<ResourceLocation> out = new ArrayList<>();
        for (final Era era : building.eras()) {
            out.add(template(building, era, true));
            out.add(template(building, era, false));
        }
        return out;
    }

    /* Lays no more the buildings the server turned off, and sets the others' spacing to the server's. */
    private static void tune(final RegistryAccess registries) {
        for (final AbandonedBuilding building : AbandonedBuilding.values()) {
            final Optional<Holder.Reference<StructureSet>> set = registries.registryOrThrow(Registries.STRUCTURE_SET)
                    .getHolder(ResourceKey.create(Registries.STRUCTURE_SET, structure(building)));
            if (set.isEmpty()) {
                continue;
            }
            if (!AbandonedSettings.laid(building)) {
                WorldgenModifiers.rebind(set.get(), new StructureSet(List.of(), set.get().value().placement()));
                continue;
            }
            final int spacing = AbandonedSettings.spacing(building);
            if (spacing != building.spacing()) {
                final OperatorStructures.Spacing spaced = OperatorStructures.spaced(building.spacing(),
                        building.spacing() / SEPARATION, (double) spacing / building.spacing());
                final JsonObject fields = new JsonObject();
                fields.addProperty(SPACING, spaced.spacing());
                fields.addProperty(APART, spaced.separation());
                WorldgenModifiers.rebind(set.get(), StructureSetModifier.withPlacement(set.get().value(), fields,
                        registries, new ArrayList<>()));
            }
        }
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
