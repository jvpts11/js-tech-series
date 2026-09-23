/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.datagen.advancement.ConditionalAdvancementProvider;
import dev.jstech.computers.datagen.advancement.JscAdvancementTabs;
import dev.jstech.computers.os.OsBootstrap;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.datagen.ContentData;
import java.util.function.BiConsumer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for all data generation, run via {@code ./gradlew runData}: what the declared content needs, the names
 * the mod keeps in registries of its own, and its advancements and recipe machines.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class JscDataGenerators {

    private JscDataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent event) {
        final ContentData data = ContentData.gather(event, ComputingContent.CONTENT)
                .alsoNaming(JscDataGenerators::systemsAndPrograms)
                .alsoNaming(JscAdvancementTabs::translations);
        data.server(new JscRecipeMachinesProvider(data.output()));
        data.server(new JscAdvancementProvider(data.output(), data.lookup(), data.existingFiles()));
        data.server(new ConditionalAdvancementProvider(data.output(), data.lookup()));
    }

    /**
     * Every system's and every program's name, and what each program does in one line, from the one registry they
     * are declared in: shown on an install disc, in the package manager and in the installed-programs list.
     */
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
