/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.datagen;

import dev.jstech.core.datagen.ContentData;
import dev.jstech.core.datagen.DeclaredFilesProvider;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.datagen.advancement.SpaceAdvancementTabs;
import dev.jstech.space.universe.StarSystem;
import dev.jstech.space.universe.StarSystems;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for the space mod's data generation, run via {@code ./gradlew :space:runData}: what its declared content
 * needs, its block states, models, English names and loot, its advancements, one generator per tab, the template of
 * a system's space and the files of its star systems. The run fails, saying what, when something the content declares
 * is missing.
 */
public final class JsSpaceDataGenerators {

    private JsSpaceDataGenerators() {
    }

    /** Hands the mod's bus the data generation, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(GatherDataEvent.class, JsSpaceDataGenerators::onGatherData);
    }

    public static void onGatherData(final GatherDataEvent event) {
        // The loose stones' block states name models written beside them, so they are known before any is asked for.
        LooseStoneModelProvider.announce(event.getExistingFileHelper());
        final ContentData data = ContentData.gather(event, SpaceContent.CONTENT)
                .alsoNaming(SpaceAdvancementTabs::translations);
        data.client(new LooseStoneModelProvider(data.output(), data.existingFiles()));
        data.server(new AdvancementProvider(data.output(), data.lookup(), data.existingFiles(),
                List.<AdvancementProvider.AdvancementGenerator>copyOf(SpaceAdvancementTabs.all())));
        data.server(new DeclaredFilesProvider<>(data.output(), JsSpace.MODID, "star_systems",
                "jsspace/star_systems", StarSystem.CODEC, StarSystems::declared));
        data.server(new DeclaredFilesProvider<>(data.output(), JsSpace.MODID, "worlds", "jsspace/worlds",
                SpaceWorld.CODEC, SpaceWorlds::declared));
        // The rocket parts' blocks as rooms and physics see them: the crews' pressure shell, and each block's mass.
        data.server(new RocketKitDataProvider(data.output(), data.lookup()));
        data.check();
    }
}
