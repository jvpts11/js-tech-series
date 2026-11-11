/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import static dev.jstech.computers.datagen.recipe.BenchItems.board;
import static dev.jstech.computers.datagen.recipe.BenchItems.caseFan;
import static dev.jstech.computers.datagen.recipe.BenchItems.chip;
import static dev.jstech.computers.datagen.recipe.BenchItems.controller;
import static dev.jstech.computers.datagen.recipe.BenchItems.index;
import static dev.jstech.computers.datagen.recipe.BenchItems.memory;
import static dev.jstech.computers.datagen.recipe.BenchItems.part;
import static dev.jstech.computers.datagen.recipe.BenchItems.plating;
import static dev.jstech.computers.datagen.recipe.BenchItems.populated;
import static dev.jstech.computers.datagen.recipe.BenchRecipes.bench;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.of;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.recipe;

import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.chain.BoardParts;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.hardware.CpuSocketId;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.registry.OwnHardware;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Every hardware part of the catalogue at the bench of its era (computers/docs/design/recipes.md, "The hardware
 * parts"), each by the rule of its kind with its counts taken from its own figures, so that of two alike the bigger
 * takes more and no two recipes repeat: the motherboards from their populated boards and components, the processors
 * and graphics cards from their microarchitecture's chips, the memory modules, the supplies, the disks, the sound
 * cards and the bay drives. A part's contacts are plated with a tin nugget in the Vintage and a gold one after.
 */
final class HardwareRecipes {

    /* The form factors a board takes two populated boards for. */
    private static final List<FormFactor> LARGE = List.of(FormFactor.EATX, FormFactor.EEB, FormFactor.MTX);
    /* Video memory each memory chip holds, by era from the Vintage, in megabytes. */
    private static final int[] VRAM_PER_CHIP = {2, 32, 128, 1024, 4096};
    /* A hard disk's platters by era from the Vintage; a supply's watts a transformer. */
    private static final int[] PLATTERS = {1, 2, 3, 4, 4};
    private static final int WATTS_A_TRANSFORMER = 300;
    private static final int CORES_A_CHIP = 8;
    /* The memory of Quince's Q chips, in their package beside the die: 8, 16 and 24 GB, a memory chip for 4 GB. */
    private static final Map<String, Integer> PACKAGE_MEMORY_CHIPS = Map.of("q1", 2, "q2", 4, "q3", 6);
    /* The dies of Quince's processors, by their microarchitecture. */
    private static final Map<String, String> QUINCE_DIES = Map.of("m68000", "m68k", "ppc_750", "g3",
            "ppc_7450", "g4", "firestorm", "q1", "avalanche", "q2", "everest", "q3");
    /* Quince's sockets, whose components are named for Quince: the G3 and the G4 share the processor card's. */
    private static final Map<String, String> QUINCE_SOCKETS = Map.of("dip_64", "socket_quince_dip64",
            "powerq_g3", "socket_quince_card", "powerq_g4", "socket_quince_card", "quince_q", "socket_quince_q");
    private static final int OLDER_SLOTS = 2;
    private static final String QUINCE_BOARD = "quince_board_";

    private HardwareRecipes() {
    }

    static void write(final RecipeOutput output) {
        boards(output);
        processors(output);
        graphics(output);
        memoryModules(output);
        supplies(output);
        disks(output);
        soundCards(output);
        bayDrives(output);
    }

    /*
     * A board's second step: its populated board (two for the big form factors), a socket for each processor, its
     * memory slots shared among the generations it takes, its expansion slots (two of the older bus beside the
     * newer's), its storage connectors (two disks to an IDE header, one to a SATA port or an M.2 slot) and its power
     * connectors.
     */
    private static void boards(final RecipeOutput output) {
        for (final Part<MotherboardSpec> part : parts(MotherboardSpec.class)) {
            if (part.id().startsWith(QUINCE_BOARD)) {
                // Quince's logic boards are the design's own table, the real machines' numbers (ComputerRecipes).
                continue;
            }
            final MotherboardSpec board = part.spec();
            final Era era = board.era();
            final Map<ItemLike, Integer> items = new LinkedHashMap<>();
            items.put(populated(era), LARGE.contains(board.formFactor()) ? 2 : 1);
            items.put(socket(board.socket()), board.cpuSlots());
            final List<RamGeneration> gens = new ArrayList<>(board.acceptedRam());
            // By the slot's pins, and of two alike the older first, as the table of slots lists them.
            gens.sort(Comparator.comparingInt((RamGeneration gen) -> slot(gen).pins())
                    .thenComparingInt(gen -> BoardParts.all().indexOf(slot(gen))));
            final int share = board.ramSlots() / gens.size();
            for (int i = 0; i < gens.size(); i++) {
                final int count = i == gens.size() - 1 ? board.ramSlots() - share * (gens.size() - 1) : share;
                add(items, slot(gens.get(i)).item().get(), count);
            }
            final int older = board.olderBus() == null ? 0 : OLDER_SLOTS;
            // A mining board's slots are x1, each a riser's cable to a card standing on the frame.
            add(items, board.formFactor() == FormFactor.MNG ? part("expansion_slot_pcie_x1")
                    : expansion(board.pcieGeneration()), board.pcieSlots() - older);
            if (board.olderBus() != null) {
                add(items, expansion(board.olderBus()), older);
            }
            if (era.isAtMost(HardwareEra.LEGACY)) {
                add(items, part("storage_ide"), Math.ceilDiv(board.diskSlots(), 2));
            } else {
                add(items, part(era == HardwareEra.ADVANCED ? "storage_m2" : "storage_sata"), board.diskSlots());
            }
            if (era == HardwareEra.VINTAGE) {
                add(items, part("power_at"), 1);
            } else {
                add(items, part(era == HardwareEra.LEGACY ? "power_atx20" : "power_atx24"), 1);
                if (board.formFactor() == FormFactor.EEB || board.formFactor() == FormFactor.MTX
                        || board.cpuSlots() > 1) {
                    add(items, part("power_eps"), board.cpuSlots());
                }
            }
            save(output, era, part, items);
        }
    }

    /*
     * A processor: its microarchitecture's chips, one for every eight cores, a faster model of the same era, socket
     * and microarchitecture one chip more than the one below it; a copper plate, its heat spreader, and a nickel foil
     * from the Transition; its contacts' nugget; and a socket of its family. Quince's own take one chip each, and the
     * Q chips the memory in their package.
     */
    private static void processors(final RecipeOutput output) {
        final Map<List<Object>, Integer> chipsSoFar = new LinkedHashMap<>();
        final List<Part<CpuSpec>> cpus = parts(CpuSpec.class);
        cpus.sort(Comparator.comparingInt((Part<CpuSpec> part) -> part.spec().cores())
                .thenComparingInt(part -> part.spec().freqMhz()).thenComparingInt(part -> part.spec().tdpWatts()));
        for (final Part<CpuSpec> part : cpus) {
            final CpuSpec cpu = part.spec();
            final Era era = cpu.era();
            final String arch = cpu.design().arch().id();
            final boolean quince = QUINCE_DIES.containsKey(arch);
            final String die = quince ? QUINCE_DIES.get(arch) : arch;
            // Its performance cores, which the dies are counted by; the efficiency ones share them.
            int chips = Math.max(1, Math.ceilDiv(cpu.cores(), CORES_A_CHIP));
            if (!quince) {
                final List<Object> family = List.of(era, cpu.socket(), arch);
                chips = Math.max(chips, chipsSoFar.getOrDefault(family, 0) + 1);
                chipsSoFar.put(family, chips);
            }
            final Map<ItemLike, Integer> items = new LinkedHashMap<>();
            items.put(chip(ChainChips.PROCESSORS, era, die), chips);
            items.put(of(Element.COPPER, Forms.PLATE), 1);
            if (PACKAGE_MEMORY_CHIPS.containsKey(die)) {
                items.put(memory(era), PACKAGE_MEMORY_CHIPS.get(die));
            }
            if (!era.isAtMost(HardwareEra.LEGACY)) {
                items.put(of(Element.NICKEL, Forms.FOIL), 1);
            }
            items.put(plating(era), 1);
            items.put(socket(cpu.socket()), 1);
            save(output, era, part, items);
        }
    }

    /*
     * A graphics card: its microarchitecture's chips, a bigger model of the same era and microarchitecture one more
     * than the one below it; memory chips by its video memory; two tantalum and four ceramic capacitors, two inductors,
     * an etched board, an aluminium plate as its heatsink, its era's case fan from the Legacy, and its contacts'
     * nugget.
     */
    private static void graphics(final RecipeOutput output) {
        final Map<List<Object>, Integer> chipsSoFar = new LinkedHashMap<>();
        final List<Part<GpuSpec>> gpus = parts(GpuSpec.class);
        gpus.sort(Comparator.comparingInt((Part<GpuSpec> part) -> part.spec().cores())
                .thenComparingInt(part -> part.spec().vramMb()).thenComparingInt(part -> part.spec().tdpWatts()));
        for (final Part<GpuSpec> part : gpus) {
            final GpuSpec gpu = part.spec();
            final Era era = gpu.era();
            final List<Object> family = List.of(era, gpu.arch().id());
            final int chips = chipsSoFar.getOrDefault(family, 0) + 1;
            chipsSoFar.put(family, chips);
            final Map<ItemLike, Integer> items = new LinkedHashMap<>();
            items.put(chip(ChainChips.GRAPHICS, era, gpu.arch().id()), chips);
            items.put(memory(era), Math.max(1, Math.ceilDiv(gpu.vramMb(),
                    VRAM_PER_CHIP[index(era)])));
            items.put(ChainParts.TANTALUM_CAPACITOR.get(), 2);
            items.put(ChainParts.CERAMIC_CAPACITOR.get(), 4);
            items.put(ChainParts.INDUCTOR.get(), 2);
            items.put(board(era), 1);
            items.put(of(Element.ALUMINIUM, Forms.PLATE), 1);
            if (era != HardwareEra.VINTAGE) {
                items.put(caseFan(era), 1);
            }
            items.put(plating(era), 1);
            save(output, era, part, items);
        }
    }

    /*
     * A memory module: eight memory chips of its era, and four more for each bigger module of its era and generation;
     * the etched board; the solder; its edge, as many copper foils as its slot has, and their plating; a registered
     * module also its register, a controller chip.
     */
    private static void memoryModules(final RecipeOutput output) {
        final Map<List<Object>, Integer> rank = new LinkedHashMap<>();
        final List<Part<RamSpec>> modules = parts(RamSpec.class);
        modules.sort(Comparator.comparingLong(part -> part.spec().bufferItems()));
        for (final Part<RamSpec> part : modules) {
            final RamSpec module = part.spec();
            final Era era = module.era();
            final boolean registered = part.id().endsWith("_rdimm");
            final List<Object> family = List.of(era, module.generation(), registered);
            final int bigger = rank.getOrDefault(family, -1) + 1;
            rank.put(family, bigger);
            final Map<ItemLike, Integer> items = new LinkedHashMap<>();
            items.put(memory(era), 8 + 4 * bigger);
            items.put(board(era), 1);
            items.put(of(era.isAtMost(HardwareEra.LEGACY) ? SeriesAlloys.SOLDER : SeriesAlloys.LEAD_FREE_SOLDER,
                    Forms.WIRE), 1);
            items.put(of(Element.COPPER, Forms.FOIL), BenchRecipes.contacts(slot(module.generation())));
            items.put(plating(era), 1);
            if (registered) {
                items.put(controller(era), 1);
            }
            save(output, era, part, items);
        }
    }

    /*
     * A power supply: a transformer for every 300 W, at least one more than the smaller supply of its era, two
     * inductors, four tantalum capacitors, its era's controller chip, four copper wires, an aluminium plate, two steel
     * plates, its era's case fan, and the plug of its era.
     */
    private static void supplies(final RecipeOutput output) {
        final Map<Era, Integer> transformersSoFar = new LinkedHashMap<>();
        final List<Part<PsuSpec>> supplies = parts(PsuSpec.class);
        supplies.sort(Comparator.comparingInt(part -> part.spec().wattage()));
        for (final Part<PsuSpec> part : supplies) {
            final Era era = part.era();
            final int transformers = Math.max(Math.ceilDiv(part.spec().wattage(), WATTS_A_TRANSFORMER),
                    transformersSoFar.getOrDefault(era, 0) + 1);
            transformersSoFar.put(era, transformers);
            final Map<ItemLike, Integer> items = new LinkedHashMap<>();
            items.put(ChainParts.TRANSFORMER.get(), transformers);
            items.put(ChainParts.INDUCTOR.get(), 2);
            items.put(ChainParts.TANTALUM_CAPACITOR.get(), 4);
            items.put(controller(era), 1);
            items.put(of(Element.COPPER, Forms.WIRE), 4);
            items.put(of(Element.ALUMINIUM, Forms.PLATE), 1);
            items.put(of(SeriesAlloys.STEEL, Forms.PLATE), 2);
            items.put(caseFan(era), 1);
            items.put(part(era == HardwareEra.VINTAGE ? "power_at" : era == HardwareEra.LEGACY ? "power_atx20"
                    : "power_atx24"), 1);
            save(output, era, part, items);
        }
    }

    /*
     * A hard disk: its platters by era and one more for each bigger disk of the era, a magnetic head per platter, the
     * motor, the controller chip, an aluminium plate and its connector (IDE to the Legacy, SATA after). A solid-state
     * one: four memory chips (eight in the Advanced) and one more for each bigger of its era and kind, the controller
     * chip, the etched board; the SATA one in its aluminium case with its port, the NVMe one a bare stick with its gold
     * edge.
     */
    private static void disks(final RecipeOutput output) {
        final Map<List<Object>, Integer> rank = new LinkedHashMap<>();
        final List<Part<DiskSpec>> disks = parts(DiskSpec.class);
        disks.sort(Comparator.comparingLong(part -> part.spec().capacityItems()));
        for (final Part<DiskSpec> part : disks) {
            final DiskSpec disk = part.spec();
            final Era era = disk.era();
            final List<Object> family = List.of(era, disk.tier());
            final int bigger = rank.getOrDefault(family, -1) + 1;
            rank.put(family, bigger);
            final Map<ItemLike, Integer> items = new LinkedHashMap<>();
            if (disk.tier() == StorageTier.HDD) {
                final int platters = PLATTERS[index(era)] + bigger;
                items.put(era == HardwareEra.VINTAGE ? ChainParts.PLATTER_VINTAGE.get() : ChainParts.PLATTER.get(),
                        platters);
                items.put(ChainParts.MAGNETIC_HEAD.get(), platters);
                items.put(ChainParts.ELECTRIC_MOTOR.get(), 1);
                items.put(controller(era), 1);
                items.put(of(Element.ALUMINIUM, Forms.PLATE), 1);
                items.put(part(era.isAtMost(HardwareEra.LEGACY) ? "storage_ide" : "storage_sata"), 1);
            } else {
                items.put(memory(era), (era == HardwareEra.ADVANCED ? 8 : 4)
                        + bigger);
                items.put(controller(era), 1);
                items.put(board(era), 1);
                if (disk.tier() == StorageTier.SSD) {
                    items.put(of(Element.ALUMINIUM, Forms.PLATE), 1);
                    items.put(part("storage_sata"), 1);
                } else {
                    items.put(Items.GOLD_NUGGET, 1);
                }
            }
            save(output, era, part, items);
        }
    }

    /*
     * A sound card: its sound processor (its era's controller chip), four capacitors, the etched board, its edge (as
     * many copper foils as its slot) and their plating; a wavetable card also its sample memory, a memory chip.
     */
    private static void soundCards(final RecipeOutput output) {
        for (final Part<SoundCardSpec> part : parts(SoundCardSpec.class)) {
            final SoundCardSpec card = part.spec();
            final Era era = card.era();
            final Map<ItemLike, Integer> items = new LinkedHashMap<>();
            items.put(controller(era), 1);
            if (card.synthesis() == SoundCardSpec.Synthesis.WAVETABLE) {
                items.put(memory(era), 1);
            }
            items.put(ChainParts.CERAMIC_CAPACITOR.get(), 4);
            items.put(board(era), 1);
            items.put(of(Element.COPPER, Forms.FOIL), BenchRecipes.contacts(BoardParts.byPath(
                    expansionPath(card.bus()))));
            items.put(plating(era), 1);
            save(output, era, part, items);
        }
    }

    /*
     * The optical drives (the motor, a lens, the laser diode of their era, the etched board and their connector) and
     * the hot-swap bay (a steel tray and frame with its backplane and the disk's connector).
     */
    private static void bayDrives(final RecipeOutput output) {
        final Item lens = of(SeriesCompounds.GLASS, Forms.LENS);
        final Map<ItemLike, Integer> bay = new LinkedHashMap<>();
        bay.put(of(SeriesAlloys.STEEL, Forms.PLATE), 2);
        bay.put(ChainParts.ETCHED_BOARDS.getFirst().get(), 1);
        bay.put(part("storage_ide"), 1);
        bay.put(of(SeriesCompounds.PVC, Forms.SHEET), 1);
        save(output, HardwareEra.VINTAGE, HardwareItems.HOT_SWAP_DRIVE_BAY.get(), bay);
        optical(output, HardwareEra.LEGACY, HardwareItems.INTERNAL_CD_DRIVE.get(), lens,
                ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(), "storage_ide");
        optical(output, HardwareEra.TRANSITION, HardwareItems.INTERNAL_DVD_DRIVE.get(), lens,
                ChainParts.ALGAINP_LASER_DIODE.get(), "storage_sata");
        optical(output, HardwareEra.ADVANCED, HardwareItems.INTERNAL_BLU_RAY_DRIVE.get(), lens,
                ChainParts.GALLIUM_NITRIDE_LASER_DIODE.get(), "storage_sata");
    }

    private static void optical(final RecipeOutput output, final Era era, final Item drive, final Item lens,
                                final Item laser, final String connector) {
        final Map<ItemLike, Integer> items = new LinkedHashMap<>();
        items.put(ChainParts.ELECTRIC_MOTOR.get(), 1);
        items.put(lens, 1);
        items.put(laser, 1);
        items.put(board(era), 1);
        items.put(part(connector), 1);
        save(output, era, drive, items);
    }

    /*
     * Every part of J's Computers' own catalogue whose sheet is a {@code type}, in the order the catalogue declares
     * them, which settles the order of two parts whose own figures are alike.
     */
    private static <S> List<Part<S>> parts(final Class<S> type) {
        final List<Part<S>> out = new ArrayList<>();
        for (final ResourceLocation id : OwnHardware.ids()) {
            final Item item = BuiltInRegistries.ITEM.get(id);
            final HardwareRegistry.Part part = HardwareRegistry.part(item);
            if (part != null && type.isInstance(part.spec())) {
                out.add(new Part<>(item, id.getPath(), type.cast(part.spec()), part.era()));
            }
        }
        return out;
    }

    private static void save(final RecipeOutput output, final Era era, final Part<?> part,
                             final Map<ItemLike, Integer> items) {
        save(output, era, part.item(), items);
    }

    private static void save(final RecipeOutput output, final Era era, final Item result,
                             final Map<ItemLike, Integer> items) {
        final ChainRecipeItems.Saved made = recipe(bench(era), BuiltInRegistries.ITEM.getKey(result).getPath());
        items.forEach((item, count) -> made.builder().input(item, count));
        made.builder().output(result, 1);
        made.save(output);
    }

    private static void add(final Map<ItemLike, Integer> items, final ItemLike item, final int count) {
        if (count > 0) {
            items.merge(item, count, Integer::sum);
        }
    }

    /* The socket component of {@code socket}'s family. */
    private static Item socket(final CpuSocketId socket) {
        final String id = path(socket.id());
        return part(QUINCE_SOCKETS.getOrDefault(id, "socket_" + id));
    }

    private static BoardParts.Component slot(final RamGeneration generation) {
        return BoardParts.byPath("memory_slot_" + path(generation.id()));
    }

    private static Item expansion(final PcieGeneration bus) {
        return part(expansionPath(bus));
    }

    /* The expansion slot of a bus's family: ISA, PCI, AGP for every AGP speed, PCIe for every PCIe generation. */
    private static String expansionPath(final PcieGeneration bus) {
        return "expansion_slot_" + lower(bus.family().name());
    }

    private static String lower(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /* An id's path, without the mod it is of: "lga_1150" of "jsc:lga_1150". */
    private static String path(final String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    /* A part of the catalogue: its item, its id's path, its sheet and its era. */
    private record Part<S>(Item item, String id, S spec, Era era) {
    }
}
