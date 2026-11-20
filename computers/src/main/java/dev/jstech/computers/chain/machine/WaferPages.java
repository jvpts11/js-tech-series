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
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
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
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * A wafer's steps as a recipe viewer shows them (computers/docs/design/survival.md, "How a chip is made"): no recipe
 * holds them, as the wafer carries its chip and its layer from machine to machine, so each page is worked out from
 * the steps' own rules ({@link WaferSteps}). A page is one step on its machine: what lies in which of its slots, what
 * its tanks hold, and three lines saying the layer and the next step, the time and what it uses, and what else the
 * machine needs. Lithography has a page a mask, etching and implanting one an era, deposition one a metal, and
 * packaging one a chip.
 */
@TextHolder
public final class WaferPages {

    /* The lines of the pages, each step's. */
    static final TextKey LITHOGRAPHY_LAYERS = TextKey.of("jsc.wafer_page.lithography_layers",
            "Each of the %s layers of %s; next: etching");
    static final TextKey LITHOGRAPHY_LIGHT = TextKey.of("jsc.wafer_page.lithography_light",
            "The one layer of %s; next: etching");
    static final TextKey LITHOGRAPHY_USES = TextKey.of("jsc.wafer_page.lithography_uses", "%s s. %s; the mask stays");
    static final TextKey LINKED = TextKey.of("jsc.wafer_page.linked",
            "Needs a computer, on, linked to its peripheral port");
    static final TextKey TIN = TextKey.of("jsc.wafer_page.tin", "a tin nugget every %s layers");
    static final TextKey ETCH_LAYERS = TextKey.of("jsc.wafer_page.etch_layers",
            "Any layer of a %s chip; next: implanting");
    static final TextKey USES = TextKey.of("jsc.wafer_page.uses", "%s s. %s");
    static final TextKey ETCH_ACID_BEFORE = TextKey.of("jsc.wafer_page.etch_acid_before",
            "Up to the Legacy: 100 mB of hydrofluoric acid");
    static final TextKey ETCH_PLASMA_AFTER = TextKey.of("jsc.wafer_page.etch_plasma_after",
            "From the Transition: chlorine and argon");
    static final TextKey IMPLANT_LAYERS = TextKey.of("jsc.wafer_page.implant_layers",
            "Any layer of a %s chip; next: deposition");
    static final TextKey IMPLANT_USES = TextKey.of("jsc.wafer_page.implant_uses",
            "%s s. One phosphorus or boron dust dopes %s layers");
    static final TextKey DEPOSIT_LAYERS = TextKey.of("jsc.wafer_page.deposit_layers",
            "%s of a %s chip: its %s wiring");
    static final TextKey DEPOSIT_ANY = TextKey.of("jsc.wafer_page.deposit_any", "Any layer");
    static final TextKey DEPOSIT_ODD = TextKey.of("jsc.wafer_page.deposit_odd", "An odd layer");
    static final TextKey DEPOSIT_EVEN = TextKey.of("jsc.wafer_page.deposit_even", "An even layer");
    static final TextKey DEPOSIT_FIRST = TextKey.of("jsc.wafer_page.deposit_first",
            "The first layer of a Standard chip: copper and its hafnium gate");
    static final TextKey DEPOSIT_NEXT = TextKey.of("jsc.wafer_page.deposit_next",
            "%s s. Next: the next layer, or packaging");
    static final TextKey DEPOSIT_METALS = TextKey.of("jsc.wafer_page.deposit_metals",
            "Its metal by era: Al, Cu, Cu and W, Co and Ru");
    static final TextKey PACKAGE_INTO = TextKey.of("jsc.wafer_page.package_into", "A finished %s wafer into %s %s");
    static final TextKey PACKAGE_NONE = TextKey.of("jsc.wafer_page.package_none", "%s s. Light needs no package");
    static final TextKey PACKAGE_SMALL = TextKey.of("jsc.wafer_page.package_small",
            "A memory or controller wafer gives 8 chips");
    static final TextKey PACKAGE_BIG = TextKey.of("jsc.wafer_page.package_big",
            "A processor or graphics wafer gives 4 chips");
    static final TextKey AMOUNT = TextKey.of("jsc.wafer_page.amount", "%s mB of %s");
    static final TextKey COUNT = TextKey.of("jsc.wafer_page.count", "%s %s");
    private static final String AND = ", ";

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
                out.add(new Page(Step.LITHOGRAPHY, station, slots, litho(station, light.era()),
                        List.of(LITHOGRAPHY_LIGHT.with(name(BuiltInRegistries.ITEM.get(light.made()))),
                                lithoUses(station, light.era()), linked(station))));
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
            out.add(new Page(Step.LITHOGRAPHY, station, slots, litho(station, era),
                    List.of(LITHOGRAPHY_LAYERS.with(layers, name(chip)), lithoUses(station, era), linked(station))));
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

    private static Text lithoUses(final ChainMachine station, final Era era) {
        final List<Text> uses = new ArrayList<>(amounts(litho(station, era)));
        if (era == HardwareEra.ADVANCED) {
            uses.add(TIN.with(WaferSteps.DROPLET_LAYERS));
        }
        return LITHOGRAPHY_USES.with(station.seconds(), TextLists.join(AND, uses));
    }

    private static Text linked(final ChainMachine station) {
        return station.linked() ? LINKED.text() : Text.EMPTY;
    }

    /* An era's etching: by acid up to the Legacy, by a chlorine plasma with argon after. */
    private static void etch(final List<Page> out, final ChainMachine etcher, final Era era) {
        final Map<Integer, List<ItemStack>> slots = new HashMap<>();
        slots.put(0, List.of(any(era, WaferWork.Step.ETCH)));
        slots.put(etcher.layout().firstOutput(), List.of(any(era, WaferWork.Step.IMPLANT)));
        final Map<Integer, Fill> tanks = new LinkedHashMap<>();
        final boolean acid = era.isAtMost(HardwareEra.LEGACY);
        if (acid) {
            tanks.put(0, new Fill(SeriesChemicals.HYDROFLUORIC_ACID, WaferSteps.ACID));
        } else {
            tanks.put(gas(etcher, 0), new Fill(Elements.of(Element.CHLORINE), WaferSteps.CHLORINE));
            tanks.put(gas(etcher, 1), new Fill(Elements.of(Element.ARGON), WaferSteps.ETCH_ARGON));
        }
        out.add(new Page(Step.ETCH, etcher, slots, tanks, List.of(ETCH_LAYERS.with(era.name()),
                USES.with(etcher.seconds(), TextLists.join(AND, amounts(tanks))),
                (acid ? ETCH_PLASMA_AFTER : ETCH_ACID_BEFORE).text())));
    }

    /* An era's implanting: a phosphorus or boron dust for every eight layers. */
    private static void implant(final List<Page> out, final ChainMachine implanter, final Era era) {
        final Map<Integer, List<ItemStack>> slots = new HashMap<>();
        slots.put(0, List.of(dust(Element.PHOSPHORUS), dust(Element.BORON)));
        slots.put(1, List.of(any(era, WaferWork.Step.IMPLANT)));
        slots.put(implanter.layout().firstOutput(), List.of(any(era, WaferWork.Step.DEPOSITION)));
        out.add(new Page(Step.IMPLANT, implanter, slots, Map.of(), List.of(IMPLANT_LAYERS.with(era.name()),
                IMPLANT_USES.with(implanter.seconds(), WaferSteps.DOPED_LAYERS), Text.EMPTY)));
    }

    /* Each era's wiring: its metal a page, and the Standard's first layer with its hafnium. */
    private static void deposition(final List<Page> out, final ChainMachine chamber) {
        deposit(out, chamber, HardwareEra.VINTAGE, DEPOSIT_ANY, Elements.of(Element.ALUMINIUM), null);
        deposit(out, chamber, HardwareEra.LEGACY, DEPOSIT_ANY, Elements.of(Element.ALUMINIUM), null);
        deposit(out, chamber, HardwareEra.TRANSITION, DEPOSIT_ANY, SeriesAlloys.REFINED_COPPER, null);
        deposit(out, chamber, HardwareEra.STANDARD, null, Elements.of(Element.COPPER),
                WaferSteps.nugget(Element.HAFNIUM));
        deposit(out, chamber, HardwareEra.STANDARD, DEPOSIT_ODD, Elements.of(Element.COPPER), null);
        deposit(out, chamber, HardwareEra.STANDARD, DEPOSIT_EVEN, Elements.of(Element.TUNGSTEN), null);
        deposit(out, chamber, HardwareEra.ADVANCED, DEPOSIT_ODD, Elements.of(Element.COBALT), null);
        deposit(out, chamber, HardwareEra.ADVANCED, DEPOSIT_EVEN, Elements.of(Element.RUTHENIUM), null);
    }

    /*
     * A page of the deposition of a nugget of {@code metal} on the layers {@code which} says of an {@code era} chip,
     * {@code gate} beside it on the Standard's first layer (which none, then); the wafer out is the next layer's, or
     * finished.
     */
    private static void deposit(final List<Page> out, final ChainMachine chamber, final Era era,
                                final @Nullable TextKey which, final Material metal, final @Nullable Item gate) {
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
        final Text first = which == null ? DEPOSIT_FIRST.text()
                : DEPOSIT_LAYERS.with(which.text(), era.name(), metal.name());
        out.add(new Page(Step.DEPOSITION, chamber, slots, Map.of(), List.of(first,
                DEPOSIT_NEXT.with(chamber.seconds()), DEPOSIT_METALS.text())));
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
            out.add(new Page(Step.PACKAGING, line, slots, Map.of(), List.of(PACKAGE_INTO.with(light.era().name(),
                    count, name(made)), PACKAGE_NONE.with(line.seconds()), Text.EMPTY)));
        }
    }

    private static void packaged(final List<Page> out, final ChainMachine line, final Era era, final Item chip) {
        final int index = ChainParts.ERAS.indexOf(era);
        final boolean small = WaferSteps.small(chip);
        final int dies = small ? WaferSteps.SMALL_DIES : WaferSteps.BIG_DIES;
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
        final List<Text> uses = new ArrayList<>();
        for (int slot = 1; slot < line.layout().firstOutput(); slot++) {
            for (final ItemStack stack : slots.getOrDefault(slot, List.of())) {
                uses.add(COUNT.with(stack.getCount(), name(stack.getItem())));
            }
        }
        out.add(new Page(Step.PACKAGING, line, slots, Map.of(), List.of(PACKAGE_INTO.with(era.name(), dies,
                name(chip)), USES.with(line.seconds(), TextLists.join(AND, uses)),
                (small ? PACKAGE_SMALL : PACKAGE_BIG).text())));
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

    /* A wafer in process of {@code era}, a memory chip's, as a page about any of its era's shows one. */
    private static ItemStack any(final Era era, final WaferWork.Step step) {
        final int index = ChainParts.ERAS.indexOf(era);
        return wafer(era, WaferSteps.idOf(ChainParts.MEMORY_CHIPS.get(index).get()), 1, WaferSteps.layers(era), step);
    }

    private static ItemStack wafer(final Era era, final ResourceLocation chip, final int layer, final int layers,
                                   final WaferWork.Step step) {
        final ItemStack out = new ItemStack(ChainChips.waferInProcess(era).get());
        out.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(chip, layer, layers, step));
        return out;
    }

    private static ItemStack dust(final Element element) {
        return new ItemStack(MaterialUses.item(Elements.of(element), Forms.DUST).get());
    }

    private static List<Text> amounts(final Map<Integer, Fill> tanks) {
        final List<Text> out = new ArrayList<>();
        for (final Fill fill : tanks.values()) {
            out.add(AMOUNT.with(fill.amount(), fill.material().name()));
        }
        return out;
    }

    private static Text name(final ItemLike item) {
        return GameText.of(new ItemStack(item).getHoverName());
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
     * @param lines   its three lines
     */
    public record Page(Step step, ChainMachine machine, Map<Integer, List<ItemStack>> slots, Map<Integer, Fill> tanks,
                       List<Text> lines) {

        public Page {
            slots = Map.copyOf(slots);
            tanks = Map.copyOf(tanks);
            lines = List.copyOf(lines);
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
