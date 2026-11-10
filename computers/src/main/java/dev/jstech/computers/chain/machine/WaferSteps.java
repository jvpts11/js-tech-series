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
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity.Processing;
import dev.jstech.core.machine.ProcessingMachineBlockEntity.Take;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.matter.MatterStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * The steps a wafer takes on its way to chips (computers/docs/design/survival.md, "How a chip is made"), each worked
 * out from what the machine holds, as no recipe can: the wafer carries its chip and its layer from machine to machine.
 * Each layer goes through the Lithography Station (photoresist, the era's light, the mask in its slot, never used up),
 * the Plasma Etcher, the Ion Implanter and the Deposition Chamber, by what the design's table asks of each; a chip of
 * a later era has more layers. The Packaging Line cuts a finished wafer into its chips, each in its era's package, or a
 * finished wafer of light into its laser diodes or LEDs. A machine works a step only from exactly what it takes.
 */
final class WaferSteps {

    /* The layers a chip of each era has, from the Vintage; a wafer of light takes one. */
    private static final int[] LAYERS = {2, 3, 4, 6, 8};
    private static final int LIGHT_LAYERS = 1;
    /* What each layer takes, in millibuckets: the resist, the immersion's water, a laser's gas, the etch's acid. */
    private static final int PHOTORESIST = 100;
    private static final int IMMERSION = 100;
    private static final int LASER_GAS = 10;
    private static final int ACID = 100;
    private static final int CHLORINE = 100;
    private static final int ETCH_ARGON = 10;
    /* One dopant dust dopes this many layers; one tin nugget gives the EUV plasma's droplets for this many. */
    static final int DOPED_LAYERS = 8;
    static final int DROPLET_LAYERS = 4;
    /* What one finished wafer is cut into: memory and controller dies are small, a processor's or graphics' big. */
    private static final int SMALL_DIES = 8;
    private static final int BIG_DIES = 4;
    private static final int LASER_DIODES = 16;
    private static final int LEDS = 32;
    /* The epoxy and the copper foil of a package go four chips to one. */
    private static final int CHIPS_A_SHARE = 4;
    private static final int TICKS = 20;

    private WaferSteps() {
    }

    /**
     * The step {@code machine} works on what {@code input} holds, if a wafer is there for it: its work, the era of the
     * wafer it works on, and what the machine's count of layers left on its dopant dust or its tin becomes once done.
     */
    static Optional<Step> step(final ChainMachine machine, final List<ChainMachines.Worked> worked,
                               final ProcessingInput input, final int layersLeft) {
        final String id = machine.id();
        if (id.startsWith("lithography_")) {
            return lithography(machine, worked, input, layersLeft);
        }
        return switch (id) {
            case "plasma_etcher" -> etch(machine, worked, input);
            case "ion_implanter" -> implant(machine, worked, input, layersLeft);
            case "deposition_chamber" -> deposit(machine, worked, input);
            case "packaging_line" -> packaging(machine, worked, input);
            default -> Optional.empty();
        };
    }

    /** How many layers a wafer of {@code era}'s chips has. */
    static int layers(final Era era) {
        return LAYERS[ChainParts.ERAS.indexOf(era)];
    }

    /* A new wafer and its mask make a wafer in process on its first layer; one back from its deposition, the next. */
    private static Optional<Step> lithography(final ChainMachine machine, final List<ChainMachines.Worked> worked,
                                              final ProcessingInput input, final int layersLeft) {
        final Needs needs = new Needs(input);
        final int maskSlot = needs.find(stack -> Mask.of(stack) != null);
        if (maskSlot < 0) {
            return Optional.empty();
        }
        final Mask mask = Mask.of(input.items().get(maskSlot));
        needs.catalyst(maskSlot);
        final Planned planned = mask.light() ? lightWafer(needs, mask) : siliconWafer(needs, mask);
        if (planned == null) {
            return Optional.empty();
        }
        final Era era = planned.era();
        final ChainMachines.Worked serving = served(worked, era);
        if (serving == null) {
            return Optional.empty();
        }
        needs.fluid(SeriesChemicals.PHOTORESIST, PHOTORESIST);
        int left = layersLeft;
        if (era == HardwareEra.LEGACY) {
            needs.gas(Elements.of(Element.KRYPTON), LASER_GAS);
        } else if (era == HardwareEra.TRANSITION || era == HardwareEra.STANDARD) {
            needs.gas(Elements.of(Element.ARGON), LASER_GAS);
            needs.fluid(SeriesChemicals.ULTRAPURE_WATER, IMMERSION);
        } else if (era == HardwareEra.ADVANCED) {
            // The EUV source's tin droplets: one nugget for every four layers it exposes.
            if (layersLeft <= 0) {
                needs.item(nugget(Element.TIN), 1);
                left = DROPLET_LAYERS;
            }
            left--;
        }
        final ItemStack out = new ItemStack(ChainChips.waferInProcess(era).get());
        out.set(ComputingComponents.WAFER_WORK.get(), planned.work().to(WaferWork.Step.ETCH));
        final int after = left;
        return needs.make(out, machine, serving).map(work -> new Step(work, era, after));
    }

    /* A silicon wafer starting the chip of {@code mask}, or one of its era back for its next layer. */
    private static @Nullable Planned siliconWafer(final Needs needs, final Mask mask) {
        final Item silicon = MaterialUses.item(Elements.of(Element.SILICON), Forms.WAFER).get();
        final int fresh = needs.find(stack -> stack.is(silicon));
        if (fresh >= 0) {
            needs.item(fresh, 1);
            return new Planned(mask.era(), new WaferWork(mask.chip(), 1, layers(mask.era()),
                    WaferWork.Step.LITHOGRAPHY));
        }
        final Item inProcess = ChainChips.waferInProcess(mask.era()).get();
        final int back = needs.find(stack -> stack.is(inProcess) && at(stack, WaferWork.Step.LITHOGRAPHY)
                && work(stack).chip().equals(mask.chip()));
        if (back < 0) {
            return null;
        }
        needs.item(back, 1);
        return new Planned(mask.era(), work(needs.stack(back)));
    }

    /* A wafer of light taking its one layer through the laser diodes' mask or the LEDs'. */
    private static @Nullable Planned lightWafer(final Needs needs, final Mask mask) {
        for (final Light light : Light.values()) {
            if (light.led() != mask.led()) {
                continue;
            }
            final Item wafer = MaterialUses.item(light.material(), Forms.WAFER).get();
            final int slot = needs.find(stack -> stack.is(wafer));
            if (slot >= 0) {
                needs.item(slot, 1);
                return new Planned(light.era(), new WaferWork(light.made(), 1, LIGHT_LAYERS,
                        WaferWork.Step.LITHOGRAPHY));
            }
        }
        return null;
    }

    /* The exposed layer etched away: by acid up to the Legacy, by a chlorine plasma with argon after. */
    private static Optional<Step> etch(final ChainMachine machine, final List<ChainMachines.Worked> worked,
                                       final ProcessingInput input) {
        final Needs needs = new Needs(input);
        final Held held = needs.wafer(WaferWork.Step.ETCH);
        if (held == null) {
            return Optional.empty();
        }
        if (held.era().isAtMost(HardwareEra.LEGACY)) {
            needs.fluid(SeriesChemicals.HYDROFLUORIC_ACID, ACID);
        } else {
            needs.gas(Elements.of(Element.CHLORINE), CHLORINE);
            needs.gas(Elements.of(Element.ARGON), ETCH_ARGON);
        }
        return needs.make(held.next(WaferWork.Step.IMPLANT), machine, worked.getFirst())
                .map(work -> new Step(work, held.era(), 0));
    }

    /* The layer doped: one phosphorus or boron dust for every eight layers. */
    private static Optional<Step> implant(final ChainMachine machine, final List<ChainMachines.Worked> worked,
                                          final ProcessingInput input, final int layersLeft) {
        final Needs needs = new Needs(input);
        final Held held = needs.wafer(WaferWork.Step.IMPLANT);
        if (held == null) {
            return Optional.empty();
        }
        int left = layersLeft;
        if (layersLeft <= 0) {
            final Item phosphorus = MaterialUses.item(Elements.of(Element.PHOSPHORUS), Forms.DUST).get();
            final Item boron = MaterialUses.item(Elements.of(Element.BORON), Forms.DUST).get();
            needs.item(stack -> stack.is(phosphorus) || stack.is(boron), 1);
            left = DOPED_LAYERS;
        }
        final int after = left - 1;
        return needs.make(held.next(WaferWork.Step.DEPOSITION), machine, worked.getFirst())
                .map(work -> new Step(work, held.era(), after));
    }

    /*
     * The layer's wiring laid on: aluminium in the Vintage and the Legacy, refined copper in the Transition, copper and
     * tungsten in turn in the Standard (its chips' first layer with hafnium, the high-k gate), cobalt and ruthenium in
     * turn in the Advanced. The last layer done, the wafer is finished.
     */
    private static Optional<Step> deposit(final ChainMachine machine, final List<ChainMachines.Worked> worked,
                                          final ProcessingInput input) {
        final Needs needs = new Needs(input);
        final Held held = needs.wafer(WaferWork.Step.DEPOSITION);
        if (held == null) {
            return Optional.empty();
        }
        final Era era = held.era();
        final boolean odd = held.work().layer() % 2 == 1;
        if (era.isAtMost(HardwareEra.LEGACY)) {
            needs.item(nugget(Element.ALUMINIUM), 1);
        } else if (era == HardwareEra.TRANSITION) {
            needs.item(nuggetOf(SeriesAlloys.REFINED_COPPER), 1);
        } else if (era == HardwareEra.STANDARD) {
            needs.item(nugget(odd ? Element.COPPER : Element.TUNGSTEN), 1);
            if (held.work().layer() == 1 && !held.light()) {
                needs.item(nugget(Element.HAFNIUM), 1);
            }
        } else {
            needs.item(nugget(odd ? Element.COBALT : Element.RUTHENIUM), 1);
        }
        final ItemStack out;
        if (held.work().lastLayer()) {
            out = new ItemStack(held.light() ? Light.of(held.work().chip()).finished()
                    : ChainParts.FINISHED_WAFERS.get(ChainParts.ERAS.indexOf(era)).get());
            out.set(ComputingComponents.WAFER_WORK.get(), held.work().to(WaferWork.Step.PACKAGING));
        } else {
            out = new ItemStack(ChainChips.waferInProcess(era).get());
            final WaferWork done = held.work();
            out.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(done.chip(), done.layer() + 1, done.layers(),
                    WaferWork.Step.LITHOGRAPHY));
        }
        return needs.make(out, machine, worked.getFirst()).map(work -> new Step(work, era, 0));
    }

    /*
     * A finished wafer cut into its dies, each packaged as its era packages chips: in ceramic with two aluminium
     * bonding wires in the Vintage; in epoxy (one resin for four) with two gold bonding wires and copper foil (one for
     * four) in the Legacy; in epoxy on a lead-free solder nugget's balls from the Transition. A wafer of light is cut
     * into its laser diodes or LEDs, which need no package.
     */
    private static Optional<Step> packaging(final ChainMachine machine, final List<ChainMachines.Worked> worked,
                                            final ProcessingInput input) {
        final Needs needs = new Needs(input);
        final Held held = needs.wafer(WaferWork.Step.PACKAGING);
        if (held == null) {
            return Optional.empty();
        }
        final Item chip = BuiltInRegistries.ITEM.get(held.work().chip());
        if (held.light()) {
            final int made = Light.of(held.work().chip()).led() ? LEDS : LASER_DIODES;
            return needs.make(new ItemStack(chip, made), machine, worked.getFirst())
                    .map(work -> new Step(work, held.era(), 0));
        }
        final int dies = small(chip) ? SMALL_DIES : BIG_DIES;
        final Era era = held.era();
        if (era == HardwareEra.VINTAGE) {
            needs.item(ChainParts.CERAMIC_PACKAGE.get(), dies);
            needs.item(ChainParts.ALUMINIUM_BONDING_WIRE.get(), 2 * dies);
        } else if (era == HardwareEra.LEGACY) {
            needs.item(plateOf(SeriesCompounds.EPOXY_RESIN), dies / CHIPS_A_SHARE);
            needs.item(ChainParts.GOLD_BONDING_WIRE.get(), 2 * dies);
            needs.item(MaterialUses.item(Elements.of(Element.COPPER), Forms.FOIL).get(), dies / CHIPS_A_SHARE);
        } else {
            needs.item(plateOf(SeriesCompounds.EPOXY_RESIN), dies / CHIPS_A_SHARE);
            needs.item(nuggetOf(SeriesAlloys.LEAD_FREE_SOLDER), dies);
        }
        return needs.make(new ItemStack(chip, dies), machine, worked.getFirst())
                .map(work -> new Step(work, era, 0));
    }

    /* The worked kind of a station that serves {@code era}, or none. */
    @Nullable
    private static ChainMachines.Worked served(final List<ChainMachines.Worked> worked, final Era era) {
        for (final ChainMachines.Worked kind : worked) {
            if (kind.era() == era) {
                return kind;
            }
        }
        return null;
    }

    /* Whether a memory or controller chip, the small dies a wafer gives eight of. */
    private static boolean small(final Item chip) {
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            if (ChainParts.MEMORY_CHIPS.get(i).get() == chip || ChainParts.CONTROLLER_CHIPS.get(i).get() == chip) {
                return true;
            }
        }
        return false;
    }

    private static boolean at(final ItemStack stack, final WaferWork.Step step) {
        final WaferWork work = stack.get(ComputingComponents.WAFER_WORK.get());
        return work != null && work.next() == step;
    }

    private static WaferWork work(final ItemStack stack) {
        return stack.get(ComputingComponents.WAFER_WORK.get());
    }

    private static Item nugget(final Element element) {
        return nuggetOf(Elements.of(element));
    }

    private static Item nuggetOf(final Material material) {
        return MaterialUses.item(material, Forms.NUGGET).get();
    }

    private static Item plateOf(final Material material) {
        return MaterialUses.item(material, Forms.PLATE).get();
    }

    private static ResourceLocation idOf(final ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem());
    }

    /**
     * A step worked out: the work, the era of the wafer it works on, and the layers the machine's dust or tin is
     * good for once it is done.
     *
     * @param work       the work
     * @param era        the wafer's era
     * @param layersLeft the machine's count of layers left once the work is done
     */
    record Step(Processing work, Era era, int layersLeft) {
    }

    /* A wafer about to start a layer: its era and where it stands. */
    private record Planned(Era era, WaferWork work) {
    }

    /* The wafer a machine holds for its step: its era, where it stands, and whether it is a wafer of light. */
    private record Held(Era era, WaferWork work, boolean light) {

        ItemStack next(final WaferWork.Step step) {
            final ItemStack out = new ItemStack(ChainChips.waferInProcess(era).get());
            out.set(ComputingComponents.WAFER_WORK.get(), work.to(step));
            return out;
        }
    }

    /* What a mask exposes: its era, the chip it makes, or which light. */
    private record Mask(Era era, @Nullable ResourceLocation chip, boolean light, boolean led) {

        static @Nullable Mask of(final ItemStack stack) {
            if (stack.isEmpty()) {
                return null;
            }
            if (stack.is(ChainChips.LASER_DIODE_MASK.get())) {
                return new Mask(HardwareEra.LEGACY, null, true, false);
            }
            if (stack.is(ChainChips.LED_MASK.get())) {
                return new Mask(HardwareEra.STANDARD, null, true, true);
            }
            for (int i = 0; i < ChainParts.ERAS.size(); i++) {
                final Era era = ChainParts.ERAS.get(i);
                if (stack.is(ChainParts.MEMORY_MASKS.get(i).get())) {
                    return new Mask(era, idOf(ChainParts.MEMORY_CHIPS.get(i).get()), false, false);
                }
                if (stack.is(ChainParts.CONTROLLER_MASKS.get(i).get())) {
                    return new Mask(era, idOf(ChainParts.CONTROLLER_CHIPS.get(i).get()), false, false);
                }
            }
            return ChainChips.dieOf(stack.getItem()).filter(die -> die.mask().get() == stack.getItem())
                    .map(die -> new Mask(die.era(), idOf(die.chip().get()), false, false)).orElse(null);
        }
    }

    /* The three wafers of light: the era each is first made in, what it is cut into, its finished wafer. */
    private enum Light {
        GALLIUM_ARSENIDE(HardwareEra.LEGACY, false),
        ALGAINP(HardwareEra.TRANSITION, false),
        GALLIUM_NITRIDE_LASER(HardwareEra.STANDARD, false),
        GALLIUM_NITRIDE_LED(HardwareEra.STANDARD, true);

        private final Era era;
        private final boolean led;

        Light(final Era era, final boolean led) {
            this.era = era;
            this.led = led;
        }

        Era era() {
            return era;
        }

        boolean led() {
            return led;
        }

        Material material() {
            return switch (this) {
                case GALLIUM_ARSENIDE -> SeriesCompounds.GALLIUM_ARSENIDE;
                case ALGAINP -> SeriesCompounds.ALGAINP;
                case GALLIUM_NITRIDE_LASER, GALLIUM_NITRIDE_LED -> SeriesCompounds.GALLIUM_NITRIDE;
            };
        }

        ResourceLocation made() {
            return idOf(switch (this) {
                case GALLIUM_ARSENIDE -> ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get();
                case ALGAINP -> ChainParts.ALGAINP_LASER_DIODE.get();
                case GALLIUM_NITRIDE_LASER -> ChainParts.GALLIUM_NITRIDE_LASER_DIODE.get();
                case GALLIUM_NITRIDE_LED -> ChainParts.LED.get();
            });
        }

        ItemLike finished() {
            return switch (this) {
                case GALLIUM_ARSENIDE -> ChainChips.FINISHED_GALLIUM_ARSENIDE_WAFER.get();
                case ALGAINP -> ChainChips.FINISHED_ALGAINP_WAFER.get();
                case GALLIUM_NITRIDE_LASER, GALLIUM_NITRIDE_LED -> ChainChips.FINISHED_GALLIUM_NITRIDE_WAFER.get();
            };
        }

        /* The light whose wafer is cut into {@code made}. */
        static Light of(final ResourceLocation made) {
            for (final Light light : values()) {
                if (light.made().equals(made)) {
                    return light;
                }
            }
            throw new IllegalArgumentException("no wafer of light makes " + made);
        }

        /* Whether {@code made} is what a wafer of light is cut into. */
        static boolean makes(final ResourceLocation made) {
            for (final Light light : values()) {
                if (light.made().equals(made)) {
                    return true;
                }
            }
            return false;
        }
    }

    /*
     * What a step takes from what the machine holds, slot by slot and tank by tank, each place at most once; the step
     * is made only when everything held is taken, so nothing is made from part of what a machine holds.
     */
    private static final class Needs {

        private final ProcessingInput input;
        private final boolean[] slots;
        private final boolean[] tanks;
        private final boolean[] matter;
        private final List<Take> takes = new ArrayList<>();
        private final List<Take> fluidTakes = new ArrayList<>();
        private final List<Take> matterTakes = new ArrayList<>();
        private final List<Integer> catalysts = new ArrayList<>();
        /* Whether something it needs is not held, or not enough of it. */
        private boolean missing;

        Needs(final ProcessingInput input) {
            this.input = input;
            this.slots = new boolean[input.items().size()];
            this.tanks = new boolean[input.fluids().size()];
            this.matter = new boolean[input.matter().size()];
        }

        /* The first slot not yet taken holding what {@code wanted} accepts, or -1. */
        int find(final Predicate<ItemStack> wanted) {
            for (int slot = 0; slot < slots.length; slot++) {
                final ItemStack held = input.items().get(slot);
                if (!slots[slot] && !held.isEmpty() && wanted.test(held)) {
                    return slot;
                }
            }
            return -1;
        }

        ItemStack stack(final int slot) {
            return input.items().get(slot);
        }

        void catalyst(final int slot) {
            slots[slot] = true;
            catalysts.add(slot);
        }

        void item(final int slot, final int count) {
            if (slot < 0 || input.items().get(slot).getCount() < count) {
                missing = true;
                return;
            }
            slots[slot] = true;
            takes.add(new Take(slot, count));
        }

        void item(final Item wanted, final int count) {
            item(stack -> stack.is(wanted), count);
        }

        void item(final Predicate<ItemStack> wanted, final int count) {
            item(find(wanted), count);
        }

        /* The wafer waiting for {@code step}, of any era: a wafer in process, or a finished one for its packaging. */
        @Nullable Held wafer(final WaferWork.Step step) {
            final int slot = find(stack -> at(stack, step));
            if (slot < 0) {
                return null;
            }
            item(slot, 1);
            final ItemStack held = input.items().get(slot);
            final WaferWork work = work(held);
            final boolean light = Light.makes(work.chip());
            return new Held(light ? Light.of(work.chip()).era() : eraOf(held.getItem()), work, light);
        }

        void fluid(final Material material, final int millibuckets) {
            for (int tank = 0; tank < tanks.length; tank++) {
                final FluidStack held = input.fluids().get(tank);
                if (!tanks[tank] && !held.isEmpty() && held.is(MaterialLiquids.fluid(material).get())
                        && held.getAmount() >= millibuckets) {
                    tanks[tank] = true;
                    fluidTakes.add(new Take(tank, millibuckets));
                    return;
                }
            }
            missing = true;
        }

        void gas(final Material material, final int millibuckets) {
            for (int tank = 0; tank < matter.length; tank++) {
                final Optional<MatterStack> held = input.matter().get(tank);
                if (!matter[tank] && held.isPresent() && held.get().material() == material
                        && held.get().amount() >= millibuckets) {
                    matter[tank] = true;
                    matterTakes.add(new Take(tank, millibuckets));
                    return;
                }
            }
            missing = true;
        }

        /* The work making {@code out} in the machine's step at {@code worked}'s draw, when it takes all held. */
        Optional<Processing> make(final ItemStack out, final ChainMachine machine, final ChainMachines.Worked worked) {
            if (missing || !allTaken()) {
                return Optional.empty();
            }
            return Optional.of(new Processing(takes, fluidTakes, List.of(out), List.of(), machine.seconds() * TICKS,
                    worked.joulesATick(), matterTakes, List.of(), List.of(), catalysts));
        }

        private boolean allTaken() {
            for (int slot = 0; slot < slots.length; slot++) {
                if (!slots[slot] && !input.items().get(slot).isEmpty()) {
                    return false;
                }
            }
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!tanks[tank] && !input.fluids().get(tank).isEmpty()) {
                    return false;
                }
            }
            for (int tank = 0; tank < matter.length; tank++) {
                if (!matter[tank] && input.matter().get(tank).map(held -> held.amount() > 0).orElse(false)) {
                    return false;
                }
            }
            return true;
        }

        /* The era of a wafer in process or a finished wafer, by its item. */
        private static Era eraOf(final Item item) {
            for (int i = 0; i < ChainParts.ERAS.size(); i++) {
                if (ChainChips.WAFERS_IN_PROCESS.get(i).get() == item
                        || ChainParts.FINISHED_WAFERS.get(i).get() == item) {
                    return ChainParts.ERAS.get(i);
                }
            }
            throw new IllegalArgumentException("not a wafer of the chain: " + item);
        }
    }
}
