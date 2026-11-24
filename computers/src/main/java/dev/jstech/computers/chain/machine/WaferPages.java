/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

/**
 * A wafer's steps as a recipe viewer shows them (computers/docs/design/survival.md, "How a chip is made"): no recipe
 * holds them, as the wafer carries its chip and its layer from machine to machine, so each page is worked out from
 * the steps' own rules ({@link WaferSteps}). A page is one step on its machine: what lies in which of its slots and
 * what its tanks hold. Lithography has a page a mask, etching and implanting one an era, deposition one a metal, and
 * packaging one a chip.
 */
public final class WaferPages {

    private WaferPages() {
    }

    /** Every page of every step, the steps in their order: lithography, etching, implanting, deposition, packaging. */
    public static List<Page> all() {
        final List<Page> out = new ArrayList<>();
        lithography(out);
        final ChainMachine etcher = machine("plasma_etcher");
        final ChainMachine implanter = machine("ion_implanter");
        for (final Era era : ChainParts.ERAS) {
            etch(out, etcher, era);
        }
        for (final Era era : ChainParts.ERAS) {
            implant(out, implanter, era);
        }
        deposition(out, machine("deposition_chamber"));
        packaging(out, machine("packaging_line"));
        return List.copyOf(out);
    }

    /* A page a mask: a silicon chip's, each of its layers on its era's station; a light's, its one layer. */
    private static void lithography(final List<Page> out) {
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            final Era era = ChainParts.ERAS.get(i);
            silicon(out, era, ChainParts.MEMORY_MASKS.get(i).get(), ChainParts.MEMORY_CHIPS.get(i).get());
            silicon(out, era, ChainParts.CONTROLLER_MASKS.get(i).get(), ChainParts.CONTROLLER_CHIPS.get(i).get());
        }
        for (final ChainChips.Die die : ChainChips.all()) {
            silicon(out, die.era(), die.mask().get(), die.chip().get());
        }
        for (final WaferSteps.Light light : WaferSteps.Light.values()) {
            final Item mask = light.led() ? ChainChips.LED_MASK.get() : ChainChips.LASER_DIODE_MASK.get();
            station(light.era()).ifPresent(station -> {
                final Map<Integer, List<ItemStack>> slots = new HashMap<>();
                slots.put(0, List.of(new ItemStack(mask)));
                slots.put(1, List.of(new ItemStack(MaterialUses.item(light.material(), Forms.WAFER).get())));
                slots.put(station.layout().firstOutput(), List.of(wafer(light.era(), light.made(), 1, 1,
                        WaferWork.Step.ETCH)));
                out.add(new Page(Step.LITHOGRAPHY, station, slots, litho(station, light.era())));
            });
        }
    }

    /* A silicon chip's mask: a fresh wafer for its first layer, the wafer back from deposition for the rest. */
    private static void silicon(final List<Page> out, final Era era, final Item mask, final Item chip) {
        station(era).ifPresent(station -> {
            final int layers = WaferSteps.layers(era);
            final ResourceLocation id = WaferSteps.idOf(chip);
            final Map<Integer, List<ItemStack>> slots = new HashMap<>();
            slots.put(0, List.of(new ItemStack(mask)));
            slots.put(1, List.of(new ItemStack(MaterialUses.item(Elements.of(Element.SILICON), Forms.WAFER).get()),
                    wafer(era, id, 2, layers, WaferWork.Step.LITHOGRAPHY)));
            if (era == HardwareEra.ADVANCED) {
                slots.put(2, List.of(new ItemStack(WaferSteps.nugget(Element.TIN))));
            }
            slots.put(station.layout().firstOutput(), List.of(wafer(era, id, 1, layers, WaferWork.Step.ETCH)));
            out.add(new Page(Step.LITHOGRAPHY, station, slots, litho(station, era)));
        });
    }

    /* What a station's tanks hold for a layer of {@code era}: the resist, and the immersion and the laser's gas. */
    private static Map<Integer, Fill> litho(final ChainMachine station, final Era era) {
        final Map<Integer, Fill> tanks = new LinkedHashMap<>();
        tanks.put(0, new Fill(SeriesChemicals.PHOTORESIST, WaferSteps.PHOTORESIST));
        if (era == HardwareEra.LEGACY) {
            tanks.put(gas(station, 0), new Fill(Elements.of(Element.KRYPTON), WaferSteps.LASER_GAS));
        } else if (era == HardwareEra.TRANSITION || era == HardwareEra.STANDARD) {
            tanks.put(1, new Fill(SeriesChemicals.ULTRAPURE_WATER, WaferSteps.IMMERSION));
            tanks.put(gas(station, 0), new Fill(Elements.of(Element.ARGON), WaferSteps.LASER_GAS));
        }
        return tanks;
    }

    /* An era's etching: by acid up to the Legacy, by a chlorine plasma with argon after. */
    private static void etch(final List<Page> out, final ChainMachine etcher, final Era era) {
        final Map<Integer, List<ItemStack>> slots = new HashMap<>();
        slots.put(0, List.of(any(era, WaferWork.Step.ETCH)));
        slots.put(etcher.layout().firstOutput(), List.of(any(era, WaferWork.Step.IMPLANT)));
        final Map<Integer, Fill> tanks = new LinkedHashMap<>();
        if (era.isAtMost(HardwareEra.LEGACY)) {
            tanks.put(0, new Fill(SeriesChemicals.HYDROFLUORIC_ACID, WaferSteps.ACID));
        } else {
            tanks.put(gas(etcher, 0), new Fill(Elements.of(Element.CHLORINE), WaferSteps.CHLORINE));
            tanks.put(gas(etcher, 1), new Fill(Elements.of(Element.ARGON), WaferSteps.ETCH_ARGON));
        }
        out.add(new Page(Step.ETCH, etcher, slots, tanks));
    }

    /* An era's implanting: a phosphorus or boron dust for every eight layers. */
    private static void implant(final List<Page> out, final ChainMachine implanter, final Era era) {
        final Map<Integer, List<ItemStack>> slots = new HashMap<>();
        slots.put(0, List.of(dust(Element.PHOSPHORUS), dust(Element.BORON)));
        slots.put(1, List.of(any(era, WaferWork.Step.IMPLANT)));
        slots.put(implanter.layout().firstOutput(), List.of(any(era, WaferWork.Step.DEPOSITION)));
        out.add(new Page(Step.IMPLANT, implanter, slots, Map.of()));
    }

    /* Each era's wiring: its metal a page, and the Standard's first layer with its hafnium. */
    private static void deposition(final List<Page> out, final ChainMachine chamber) {
        deposit(out, chamber, HardwareEra.VINTAGE, Elements.of(Element.ALUMINIUM), null);
        deposit(out, chamber, HardwareEra.LEGACY, Elements.of(Element.ALUMINIUM), null);
        deposit(out, chamber, HardwareEra.TRANSITION, SeriesAlloys.REFINED_COPPER, null);
        deposit(out, chamber, HardwareEra.STANDARD, Elements.of(Element.COPPER), WaferSteps.nugget(Element.HAFNIUM));
        deposit(out, chamber, HardwareEra.STANDARD, Elements.of(Element.COPPER), null);
        deposit(out, chamber, HardwareEra.STANDARD, Elements.of(Element.TUNGSTEN), null);
        deposit(out, chamber, HardwareEra.ADVANCED, Elements.of(Element.COBALT), null);
        deposit(out, chamber, HardwareEra.ADVANCED, Elements.of(Element.RUTHENIUM), null);
    }

    /*
     * A page of the deposition of a nugget of {@code metal} on a layer of an {@code era} chip, {@code gate} beside it
     * on the Standard's first layer; the wafer out is the next layer's, or finished.
     */
    private static void deposit(final List<Page> out, final ChainMachine chamber, final Era era, final Material metal,
                                final @Nullable Item gate) {
        final int index = ChainParts.ERAS.indexOf(era);
        final ResourceLocation chip = WaferSteps.idOf(ChainParts.MEMORY_CHIPS.get(index).get());
        final int layers = WaferSteps.layers(era);
        final Map<Integer, List<ItemStack>> slots = new HashMap<>();
        slots.put(0, List.of(new ItemStack(WaferSteps.nuggetOf(metal))));
        slots.put(1, List.of(wafer(era, chip, 1, layers, WaferWork.Step.DEPOSITION)));
        if (gate != null) {
            slots.put(2, List.of(new ItemStack(gate)));
        }
        final ItemStack finished = new ItemStack(ChainParts.FINISHED_WAFERS.get(index).get());
        finished.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(chip, layers, layers,
                WaferWork.Step.PACKAGING));
        slots.put(chamber.layout().firstOutput(), List.of(wafer(era, chip, 2, layers, WaferWork.Step.LITHOGRAPHY),
                finished));
        out.add(new Page(Step.DEPOSITION, chamber, slots, Map.of()));
    }

    /* A page a chip: its finished wafer cut into its dies, each packaged as its era packages chips; light's, bare. */
    private static void packaging(final List<Page> out, final ChainMachine line) {
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            final Era era = ChainParts.ERAS.get(i);
            packaged(out, line, era, ChainParts.MEMORY_CHIPS.get(i).get());
            packaged(out, line, era, ChainParts.CONTROLLER_CHIPS.get(i).get());
        }
        for (final ChainChips.Die die : ChainChips.all()) {
            packaged(out, line, die.era(), die.chip().get());
        }
        for (final WaferSteps.Light light : WaferSteps.Light.values()) {
            final Item made = BuiltInRegistries.ITEM.get(light.made());
            final ItemStack finished = new ItemStack(light.finished());
            finished.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(light.made(), 1, 1,
                    WaferWork.Step.PACKAGING));
            final int count = light.led() ? WaferSteps.LEDS : WaferSteps.LASER_DIODES;
            final Map<Integer, List<ItemStack>> slots = new HashMap<>();
            slots.put(0, List.of(finished));
            slots.put(line.layout().firstOutput(), List.of(new ItemStack(made, count)));
            out.add(new Page(Step.PACKAGING, line, slots, Map.of()));
        }
    }

    private static void packaged(final List<Page> out, final ChainMachine line, final Era era, final Item chip) {
        final int index = ChainParts.ERAS.indexOf(era);
        final int dies = WaferSteps.small(chip) ? WaferSteps.SMALL_DIES : WaferSteps.BIG_DIES;
        final int shares = dies / WaferSteps.CHIPS_A_SHARE;
        final ItemStack finished = new ItemStack(ChainParts.FINISHED_WAFERS.get(index).get());
        finished.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(WaferSteps.idOf(chip),
                WaferSteps.layers(era), WaferSteps.layers(era), WaferWork.Step.PACKAGING));
        final Map<Integer, List<ItemStack>> slots = new HashMap<>();
        slots.put(0, List.of(finished));
        if (era == HardwareEra.VINTAGE) {
            slots.put(1, List.of(new ItemStack(ChainParts.CERAMIC_PACKAGE.get(), dies)));
            slots.put(2, List.of(new ItemStack(ChainParts.ALUMINIUM_BONDING_WIRE.get(), 2 * dies)));
        } else if (era == HardwareEra.LEGACY) {
            slots.put(1, List.of(new ItemStack(MaterialUses.item(Elements.of(Element.COPPER), Forms.FOIL).get(),
                    shares)));
            slots.put(2, List.of(new ItemStack(ChainParts.GOLD_BONDING_WIRE.get(), 2 * dies)));
            slots.put(3, List.of(new ItemStack(WaferSteps.plateOf(SeriesCompounds.EPOXY_RESIN), shares)));
        } else {
            slots.put(1, List.of(new ItemStack(WaferSteps.nuggetOf(SeriesAlloys.LEAD_FREE_SOLDER), dies)));
            slots.put(3, List.of(new ItemStack(WaferSteps.plateOf(SeriesCompounds.EPOXY_RESIN), shares)));
        }
        slots.put(line.layout().firstOutput(), List.of(new ItemStack(chip, dies)));
        out.add(new Page(Step.PACKAGING, line, slots, Map.of()));
    }

    /**
     * What a page's machine puts in its row of bins, bin by bin: on a packaging page, the plain chips it shows, then
     * as many of each class {@code recipes} ask of the chip; on any other, what its first output holds.
     */
    public static List<ItemStack> bins(final Page page, final RecipeManager recipes) {
        final List<ItemStack> first = page.slots().getOrDefault(page.machine().layout().firstOutput(), List.of());
        if (first.isEmpty()) {
            return List.of();
        }
        final ItemStack made = first.getFirst();
        return page.step() == Step.PACKAGING ? WaferSteps.sorted(made.getItem(), made.getCount(), recipes)
                : List.of(made);
    }

    /* The station of {@code era}'s layers, which its masks expose on. */
    private static Optional<ChainMachine> station(final Era era) {
        for (final ChainMachine machine : ChainMachines.all()) {
            if (machine.id().startsWith("lithography_")) {
                for (final ChainMachines.Worked worked : ChainMachines.workedBy(machine)) {
                    if (worked.era() == era) {
                        return Optional.of(machine);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static ChainMachine machine(final String id) {
        return ChainMachines.byId(id).orElseThrow(() -> new IllegalStateException("no chain machine " + id));
    }

    /* The index of {@code machine}'s gas inlet {@code k}: its gas tanks come after its liquids' in and out. */
    private static int gas(final ChainMachine machine, final int k) {
        return machine.layout().inputTanks() + machine.layout().outputTanks() + k;
    }

    /* A wafer of {@code era} going to {@code step}, a memory chip's, as a page about any of its era's shows one. */
    private static ItemStack any(final Era era, final WaferWork.Step step) {
        final int index = ChainParts.ERAS.indexOf(era);
        return wafer(era, WaferSteps.idOf(ChainParts.MEMORY_CHIPS.get(index).get()), 1, WaferSteps.layers(era), step);
    }

    private static ItemStack wafer(final Era era, final ResourceLocation chip, final int layer, final int layers,
                                   final WaferWork.Step step) {
        final ItemStack out = new ItemStack(WaferWork.item(era, step));
        out.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(chip, layer, layers, step));
        return out;
    }

    private static ItemStack dust(final Element element) {
        return new ItemStack(MaterialUses.item(Elements.of(element), Forms.DUST).get());
    }

    /** A wafer's step. */
    public enum Step {
        LITHOGRAPHY,
        ETCH,
        IMPLANT,
        DEPOSITION,
        PACKAGING
    }

    /**
     * One page.
     *
     * @param step    its step
     * @param machine the machine it is worked on
     * @param slots   what lies in which of the machine's slots, each slot's choices, its inputs and its outputs
     * @param tanks   what lies in which of its tanks, by their order (its liquids in, then out, then its gases)
     */
    public record Page(Step step, ChainMachine machine, Map<Integer, List<ItemStack>> slots, Map<Integer, Fill> tanks) {

        public Page {
            slots = Map.copyOf(slots);
            tanks = Map.copyOf(tanks);
        }
    }

    /**
     * What a tank holds on a page.
     *
     * @param material what it holds
     * @param amount   how much a step takes, in millibuckets
     */
    public record Fill(Material material, int amount) {
    }
}
