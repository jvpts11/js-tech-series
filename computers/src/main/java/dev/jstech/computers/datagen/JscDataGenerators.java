/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen;

import dev.jstech.computers.chain.ChipItem;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.datagen.advancement.JscAdvancementTabs;
import dev.jstech.computers.datagen.recipe.ComputersRecipes;
import dev.jstech.computers.os.OsBootstrap;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.datagen.ContentData;
import dev.jstech.core.datagen.advancement.ConditionalAdvancementProvider;
import java.util.function.BiConsumer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for all data generation, run via {@code ./gradlew runData}: what the declared content needs, the names
 * the mod keeps in registries of its own, and its advancements. The run fails, saying what, when something the
 * content declares is missing.
 */
public final class JscDataGenerators {

    private JscDataGenerators() {
    }

    /** Hands the mod's bus the data generation, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(GatherDataEvent.class, JscDataGenerators::onGatherData);
    }

    public static void onGatherData(final GatherDataEvent event) {
        final ContentData data = ContentData.gather(event, ComputingContent.CONTENT)
                .alsoNaming(JscDataGenerators::systemsAndPrograms)
                .alsoNaming(JscAdvancementTabs::translations)
                .alsoNaming(JscDataGenerators::chainMachines)
                .alsoNaming(JscDataGenerators::classedChips);
        data.server(new JscAdvancementProvider(data.output(), data.lookup(), data.existingFiles()));
        data.server(new ConditionalAdvancementProvider(data.output(), data.lookup(), JscAdvancementTabs::all));
        data.server(new ComputersRecipes(data.output(), data.lookup()));
        data.alsoLoot(AbandonedLootTables::new, LootContextParamSets.CHEST);
        data.check();
    }

    /**
     * Every system's and every program's name, and what each program does in one line, from the one registry they
     * are declared in: shown on an install disc, in the package manager and in the installed-programs list.
     */
    /* Each chain machine's own name, which a window calls the machine by, apart from its terminal block's. */
    private static void chainMachines(final BiConsumer<String, String> add) {
        for (final ChainMachine machine : ChainMachines.all()) {
            add.accept(machine.nameKey().key(), machine.nameKey().english());
        }
    }

    /* Each chip's name with its class, "Memory Chip (Advanced, Class %s)", beside its plain one. */
    private static void classedChips(final BiConsumer<String, String> add) {
        for (final ItemEntry<?> entry : ComputingContent.CONTENT.declaredItems()) {
            if (entry.get() instanceof ChipItem chip) {
                add.accept(chip.getDescriptionId() + ChipItem.CLASSED, ChipItem.classedEnglish(entry.english()));
            }
        }
    }

    private static void systemsAndPrograms(final BiConsumer<String, String> add) {
        for (final OsDef os : OsBootstrap.builtinOses()) {
            add.accept(os.titleKey(), os.displayName());
        }
        for (final ProgramSpec program : OsBootstrap.builtinPrograms()) {
            add.accept(program.titleKey(), program.displayName());
            add.accept(program.descriptionKey(), program.description());
        }
    }
}
