/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.core.client.material.ThingColours;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * How a chain machine shows what it is working on (computers/docs/design/survival.md), as its approved mock drew it
 * with its real inputs: which of its model's modes is shown (glass or ingots in the Grinder, a tube or a
 * flat screen on the Display Line), and the colour of each channel of its model drawn in the material at work (the
 * ore in the Crusher, the wire on the Coil Winder), read from what goes in and what comes out.
 */
public final class ChainReflect {

    private ChainReflect() {
    }

    /** The mode the machine shows for what it works on now; the model's own first one when it has no such modes. */
    public static String mode(final ChainMachineBlockEntity machine) {
        final String id = machine.machine().id();
        final List<ResourceLocation> in = machine.takes();
        final List<ResourceLocation> out = machine.makes();
        if (id.startsWith("lithography_")) {
            return any(in, "memory") || any(out, "memory") ? "memory" : "processor";
        }
        if (id.startsWith("assembly_")) {
            return any(out, "ram") || any(out, "memory") ? "memory" : "card";
        }
        return switch (id) {
            case "grinder" -> any(in, "glass") ? "glass" : "ingot";
            case "electrolytic_cell" -> in.isEmpty() ? "water" : "brine";
            case "plasma_etcher" -> any(machine.flows(), "chlorine") ? "chlorine" : "argon";
            case "ion_implanter" -> any(in, "phosphorus") ? "phosphorus" : "boron";
            case "deposition_chamber" -> any(in, "copper") ? "copper" : any(in, "tungsten") ? "tungsten"
                    : "aluminium";
            case "ceramic_kiln" -> any(out, "ferrite") ? "ferrites" : "packages";
            case "optical_polisher" -> any(out, "disc") ? "disc" : "lens";
            case "display_line" -> any(out, "tube") ? "crt" : "lcd";
            default -> "";
        };
    }

    /** The colour, as {@code 0xRRGGBB}, of the material the channel {@code channel} of the machine's model shows. */
    public static int colour(final ChainMachineBlockEntity machine, final String channel) {
        final List<ResourceLocation> in = machine.takes();
        final List<ResourceLocation> out = machine.makes();
        return switch (channel) {
            case "flux", "resin" -> item(in, 1).or(() -> item(in, 0)).orElse(ThingColours.GREY);
            case "ingot", "boule" -> item(out, 0).or(() -> item(in, 0)).orElse(ThingColours.GREY);
            case "batch", "package" -> item(out, 0).orElseGet(() -> fluid(machine));
            case "wire" -> named(in, "wire").or(() -> item(in, 0)).orElse(ThingColours.GREY);
            default -> item(in, 0).orElse(ThingColours.GREY);
        };
    }

    /* The colour of the {@code index}th item, if there is one. */
    private static Optional<Integer> item(final List<ResourceLocation> items, final int index) {
        return index < items.size() ? Optional.of(ThingColours.of(new ItemStack(BuiltInRegistries.ITEM
                .get(items.get(index))))) : Optional.empty();
    }

    /* The colour of the first item whose id names {@code part}, if there is one. */
    private static Optional<Integer> named(final List<ResourceLocation> items, final String part) {
        return items.stream().filter(id -> id.getPath().contains(part)).findFirst()
                .map(id -> ThingColours.of(new ItemStack(BuiltInRegistries.ITEM.get(id))));
    }

    /* The colour of the first liquid the work takes, or grey. */
    private static int fluid(final ChainMachineBlockEntity machine) {
        for (final ResourceLocation id : machine.flows()) {
            if (BuiltInRegistries.FLUID.containsKey(id) && BuiltInRegistries.FLUID.get(id) != Fluids.EMPTY) {
                return ThingColours.of(BuiltInRegistries.FLUID.get(id));
            }
        }
        return ThingColours.GREY;
    }

    private static boolean any(final List<ResourceLocation> ids, final String part) {
        return ids.stream().anyMatch(id -> id.getPath().contains(part));
    }
}
