/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbquests;

import dev.jstech.core.JsCore;
import dev.jstech.core.optional.OptionalMod;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Soft integration with FTB Quests: when the mod is present, its quests have tasks of the Core's knowledge,
 * milestones and steps along an axis ({@link CoreQuestTasks}). Nothing here touches an FTB Quests class unless the mod
 * is loaded.
 */
public final class FtbQuestsIntegration {

    public static final String MOD_ID = "ftbquests";
    /** FTB Quests, behind its guard. */
    public static final OptionalMod MOD = OptionalMod.of(MOD_ID);
    /** The task met as the team learns a piece of knowledge. */
    public static final ResourceLocation KNOWLEDGE = id("knowledge");
    /** The task met once the team passes a milestone. */
    public static final ResourceLocation MILESTONE = id("milestone");
    /** The task met once the team reaches a step along an axis. */
    public static final ResourceLocation AXIS_STEP = id("axis_step");

    private FtbQuestsIntegration() {
    }

    public static boolean isLoaded() {
        return MOD.isLoaded();
    }

    /** Hands FTB Quests the Core's kinds of task as the game sets up; a no-op when FTB Quests is absent. */
    public static void bootstrap(final IEventBus modBus) {
        MOD.ifLoaded(() -> () -> modBus.addListener(FMLCommonSetupEvent.class,
                event -> event.enqueueWork(FtbQuestsIntegration::registerTasks)));
    }

    /** The ids of the Core's kinds of task, as a quest's file names them. */
    public static List<ResourceLocation> taskKinds() {
        return List.of(KNOWLEDGE, MILESTONE, AXIS_STEP);
    }

    // Kept in its own method so FTB Quests' classes are only loaded here.
    private static void registerTasks() {
        CoreQuestTasks.register();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
