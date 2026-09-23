/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.advancement.HardwareMilestones;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The machines themselves: putting one together, the eras it can be built in, and the racks, datacenters, clusters
 * and supercomputers they grow into. The Advanced, Exa and Singularity builds join once those eras have hardware.
 */
public final class HardwareAdvancements extends AdvancementTab {

    public HardwareAdvancements() {
        super("hardware", ResourceLocation.withDefaultNamespace("textures/block/iron_block.png"));
        this.root(ComputingModule.PERSONAL_COMPUTER_ITEM.get(), "Hardware", "Get your hands on a computer",
                () -> InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
                        ComputingModule.PERSONAL_COMPUTER_ITEM.get(),
                        ComputingModule.VINTAGE_PERSONAL_COMPUTER_ITEM.get(),
                        ComputingModule.LEGACY_PERSONAL_COMPUTER_ITEM.get(),
                        ComputingModule.MAINFRAME_ITEM.get(),
                        ComputingModule.VINTAGE_MAINFRAME_ITEM.get(),
                        ComputingModule.LEGACY_MAINFRAME_ITEM.get(),
                        ComputingModule.CRAFTING_COMPUTER_ITEM.get(),
                        ComputingModule.VINTAGE_CRAFTING_COMPUTER_ITEM.get(),
                        ComputingModule.LEGACY_CRAFTING_COMPUTER_ITEM.get(),
                        ComputingModule.CLUSTER_MANAGEMENT_COMPUTER_ITEM.get(),
                        ComputingModule.VINTAGE_CLUSTER_MANAGEMENT_COMPUTER_ITEM.get(),
                        ComputingModule.LEGACY_CLUSTER_MANAGEMENT_COMPUTER_ITEM.get())));

        this.task("pc_master_race", "root", ComputingModule.MONITOR_ITEM.get(), "PC Master Race",
                "Watch a computer pass its power-on self-test", on(JscEvents.POST_PASSED));
        this.task("off_and_on_again", "pc_master_race", Items.LEVER, "Have You Tried Turning It Off and On Again?",
                "Restart a running computer from its power menu", on(JscEvents.POWER_CYCLED));
        this.task("download_more_ram", "pc_master_race", ComputingModule.RAM_DDR3_8192.get(), "Download More RAM",
                "Fill every RAM slot on a motherboard", on(JscEvents.RAM_FILLED));
        this.task("save_icon", "pc_master_race", ComputingModule.FLOPPY_DISK.get(), "The Save Icon",
                "Save a file to a floppy disk", on(JscEvents.FLOPPY_SAVED));
        this.goal("battlestation", "pc_master_race", ComputingModule.MONITOR_ITEM.get(), "Battlestation",
                "Link four monitors to one computer", on(JscEvents.BATTLESTATION));

        this.task("vintage_build", "pc_master_race", ComputingModule.VINTAGE_PERSONAL_COMPUTER_ITEM.get(),
                "640K Ought to Be Enough", "Build a working Vintage computer", built(HardwareEra.VINTAGE));
        this.task("legacy_build", "vintage_build", ComputingModule.LEGACY_PERSONAL_COMPUTER_ITEM.get(),
                "Party Like It's 1999", "Build a working Legacy computer", built(HardwareEra.LEGACY));
        this.task("standard_build", "legacy_build", ComputingModule.PERSONAL_COMPUTER_ITEM.get(),
                "Moore's Law Isn't Dead", "Build a working Standard computer", built(HardwareEra.STANDARD));
        final Map<String, Supplier<Criterion<?>>> everyEra = new LinkedHashMap<>();
        for (final HardwareEra era : new HardwareEra[] {HardwareEra.VINTAGE, HardwareEra.LEGACY,
                HardwareEra.STANDARD}) {
            everyEra.put(HardwareMilestones.eraDetail(era), built(era));
        }
        this.challengeOfAll("time_traveller", "standard_build", Items.CLOCK, "Time Traveller",
                "Build a working computer of every era there is", everyEra);

        this.goal("rack_em_up", "root", ComputingModule.SERVER_RACK_ITEM.get(), "Rack 'Em Up",
                "Fill every unit of a Server Rack", on(JscEvents.RACK_FILLED));
        this.goal("someone_elses_computer", "rack_em_up", ComputingModule.SERVER_ROUTER_ITEM.get(),
                "It's Just Someone Else's Computer", "Form a datacenter section behind a Server Router",
                on(JscEvents.DATACENTER_FORMED));
        this.goal("herding_cats", "someone_elses_computer",
                ComputingModule.CLUSTER_MANAGEMENT_COMPUTER_ITEM.get(), "Herding Cats",
                "Run a whole cluster from a Cluster Management Computer", on(JscEvents.CLUSTER_RUN));
        this.challenge("top500", "herding_cats", ComputingModule.SUPERCOMPUTER_RACK_ITEM.get(), "TOP500",
                "Bring a supercomputer online: Supercomputer Racks on High Compute Cable",
                on(JscEvents.SUPERCOMPUTER_ONLINE));
    }

    private static Supplier<Criterion<?>> built(final HardwareEra era) {
        return on(JscEvents.ERA_BUILT, HardwareMilestones.eraDetail(era));
    }
}
