/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.villager;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;
import static dev.jstech.computers.registry.ComputingContent.shelf;

import com.google.common.collect.ImmutableSet;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.FacingShapes;
import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The two professions of the computers (computers/docs/design/survival.md, "Villagers"): the Programmer at the
 * Programmer's Desk and the IT Technician at the Repair Bench, each a villager takes as its work as it takes a lectern;
 * the job sites made at the Crafting Table. Whether villagers take either is the server's to say.
 */
@TextHolder
public final class ComputerVillagers {

    /** The Programmer's name over its head and in its trading window. */
    public static final TextKey PROGRAMMER_NAME = TextKey.of("entity.minecraft.villager.jsc.programmer",
            "Programmer");
    /** The IT Technician's name over its head and in its trading window. */
    public static final TextKey IT_TECHNICIAN_NAME = TextKey.of("entity.minecraft.villager.jsc.it_technician",
            "IT Technician");
    /** The Programmer's Desk: a desk on its legs, an old terminal on it, its keyboard, floppies, a sheet of code. */
    public static final BlockEntry<JobSiteBlock> PROGRAMMERS_DESK = jobSite("programmers_desk",
            FacingShapes.of(new double[] {0, 7, 0, 16, 9, 16}, new double[] {0, 0, 0, 2, 7, 2},
                    new double[] {14, 0, 0, 16, 7, 2}, new double[] {0, 0, 14, 2, 7, 16},
                    new double[] {14, 0, 14, 16, 7, 16}, new double[] {2, 4, 0, 14, 7, 14},
                    new double[] {2, 9, 7, 11, 16, 14}, new double[] {2, 9, 1, 11, 10, 5}))
            .named("Programmer's Desk").register();
    /** The Repair Bench: a bench with a pegboard of tools at its back, a board on its mat, a toolbox under it. */
    public static final BlockEntry<JobSiteBlock> REPAIR_BENCH = jobSite("repair_bench",
            FacingShapes.of(new double[] {0, 8, 0, 16, 10, 14}, new double[] {0, 0, 0, 2, 8, 2},
                    new double[] {14, 0, 0, 16, 8, 2}, new double[] {0, 0, 12, 2, 8, 14},
                    new double[] {14, 0, 12, 16, 8, 14}, new double[] {0, 2, 2, 16, 3, 12},
                    new double[] {0, 8, 14, 16, 16, 16}))
            .named("Repair Bench").register();
    private static final DeferredRegister<PoiType> POIS = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE,
            JsComputers.MODID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(
            Registries.VILLAGER_PROFESSION, JsComputers.MODID);
    public static final DeferredHolder<PoiType, PoiType> PROGRAMMER_POI = POIS.register("programmer",
            () -> new PoiType(ImmutableSet.copyOf(PROGRAMMERS_DESK.get().getStateDefinition().getPossibleStates()), 1,
                    1));
    public static final DeferredHolder<PoiType, PoiType> IT_TECHNICIAN_POI = POIS.register("it_technician",
            () -> new PoiType(ImmutableSet.copyOf(REPAIR_BENCH.get().getStateDefinition().getPossibleStates()), 1,
                    1));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> PROGRAMMER = PROFESSIONS.register(
            "programmer", () -> profession("programmer", PROGRAMMER_POI.getKey(), VillagerSettings::programmer));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> IT_TECHNICIAN = PROFESSIONS.register(
            "it_technician", () -> profession("it_technician", IT_TECHNICIAN_POI.getKey(),
                    VillagerSettings::itTechnician));

    private ComputerVillagers() {
    }

    /** Hands the buses the professions, their job sites, their trades and the villages' shop, from the constructor. */
    public static void register(final IEventBus modBus) {
        POIS.register(modBus);
        PROFESSIONS.register(modBus);
        NeoForge.EVENT_BUS.addListener(ComputerTrades::onTrades);
        VillageShops.listen();
    }

    /*
     * A profession whose villagers take {@code site} as their work while the server lets them: off, no villager takes
     * it, and one that had it keeps it no longer.
     */
    private static VillagerProfession profession(final String name, final ResourceKey<PoiType> site,
                                                 final BooleanSupplier allowed) {
        return new VillagerProfession(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, name).toString(),
                (Holder<PoiType> poi) -> allowed.getAsBoolean() && poi.is(site),
                (Holder<PoiType> poi) -> allowed.getAsBoolean() && poi.is(site), ImmutableSet.of(), ImmutableSet.of(),
                null);
    }

    private static BlockBuilder<JobSiteBlock> jobSite(final String id, final Map<Direction, VoxelShape> shapes) {
        return CONTENT.block(id, JobSiteBlock.with(shapes))
                .properties(properties -> properties.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)
                        .noOcclusion())
                .look(IBlockLook.facingByState(state -> new IBlockModel.Handmade("job_site/" + id)))
                .tag(BlockTags.MINEABLE_WITH_AXE).item().itemLook(IItemLook.parent("block/job_site/" + id))
                .tab(shelf(Shelf.PERIPHERALS, null));
    }
}
