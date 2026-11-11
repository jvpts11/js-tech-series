/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import dev.jstech.computers.advancement.HardwareMilestones;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;

/**
 * The Fab tab (computers/docs/design/advancements.md): the chain from the ore to the chip, rooted in the Vintage
 * Electronics Workbench, on a white concrete ground.
 */
public final class FabAdvancements extends JscAdvancementTab {

    public FabAdvancements() {
        super("fab", ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"));
        this.root(item("jsc:vintage_electronics_workbench"), "Some Assembly Required",
                "Make a Vintage Electronics Workbench", () -> InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(item("jsc:vintage_electronics_workbench"))));
        this.task("bare_metal", "root", item("jsc:machine_casing"), "Bare Metal",
                "Form your first machine of the chain", on(JscEvents.CHAIN_FORMED));
        this.task("nine_nines", "bare_metal", item("jscore:polysilicon_rod"), "Nine Nines",
                "Grow polysilicon in the Siemens Reactor", made("siemens_reactor"));
        this.task("pull_request", "nine_nines", item("jscore:silicon_boule"), "Pull Request",
                "Pull your first silicon boule", made("crystal_puller"));
        this.task("wafer_thin", "pull_request", item("jscore:silicon_wafer"), "Wafer-Thin",
                "Cut a boule into wafers", made("wafer_saw"));
        this.task("tape_out", "wafer_thin", item("jsc:mask_processor_vintage_i486"), "Tape-Out",
                "Write your first mask", on(JscEvents.MASK_WRITTEN));
        this.goal("first_silicon", "tape_out", item("jsc:processor_chip_vintage_i486"), "First Silicon",
                "Package your first chip", made("packaging_line"));
        this.task("bunny_suit", "bare_metal", item("jsc:clean_room_casing"), "Bunny Suit",
                "Build a Lithography Station", on(JscEvents.CHAIN_FORMED, ChainMachines.LITHOGRAPHY));
        final Map<String, Supplier<Criterion<?>>> everyEra = new LinkedHashMap<>();
        for (final Era era : new Era[] {HardwareEra.VINTAGE, HardwareEra.LEGACY, HardwareEra.TRANSITION,
                HardwareEra.STANDARD, HardwareEra.ADVANCED}) {
            everyEra.put(HardwareMilestones.eraDetail(era), exposed(era));
        }
        this.challengeOfAll("die_shrink", "bunny_suit", item("jsc:finished_wafer_advanced"), "Die Shrink",
                "Expose a layer in the Lithography Station of every era", everyEra);
        this.goal("thirteen_point_five_nanometres", "bunny_suit", item("jsc:mask_processor_advanced_alder_lake"),
                "13.5 Nanometres", "Expose a layer with extreme ultraviolet light", exposed(HardwareEra.ADVANCED));
        this.task("burn_in", "bare_metal", item("jsc:tube_screen"), "Burn-In",
                "Make a screen on the Display Line", made("display_line"));
        this.goal("pick_and_place", "first_silicon", item("jsc:populated_board_vintage"), "Pick and Place",
                "Let an Electronics Assembly Line make a part by itself", made(ChainMachines.ASSEMBLY));
        this.task("plug_and_pray", "pick_and_place", item("jsc:vintage_personal_computer"), "Plug and Pray",
                "Build a computer in the Prebuilder", made("prebuilder"));
        this.task("do_not_turn_off_your_computer", "plug_and_pray", item("jsc:motherboard_babyat_vintage"),
                "Do Not Turn Off Your Computer", "Flash a motherboard's firmware in the Preinstaller",
                on(JscEvents.FIRMWARE_FLASHED));
    }

    /* Earned by the first thing a machine of that family makes. */
    private Supplier<Criterion<?>> made(final String family) {
        return on(JscEvents.CHAIN_MADE, family);
    }

    /* Earned by a layer a Lithography Station exposes for a wafer of {@code era}. */
    private Supplier<Criterion<?>> exposed(final Era era) {
        return on(JscEvents.LAYER_EXPOSED, HardwareMilestones.eraDetail(era));
    }
}
