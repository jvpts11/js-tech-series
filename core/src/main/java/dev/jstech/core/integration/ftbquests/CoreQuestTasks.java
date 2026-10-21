/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbquests;

import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.world.item.Items;

/**
 * The Core's kinds of task in FTB Quests: {@code jscore:knowledge}, met as the team learns a piece of knowledge,
 * {@code jscore:milestone}, met once it passes a milestone, and {@code jscore:axis_step}, met once it reaches a step
 * along an axis. Each is checked for every player of a team with the quest open once a second, and as they log in.
 */
final class CoreQuestTasks {

    /** How often a task is checked for each player, in ticks. */
    static final int CHECK_TICKS = 20;

    private CoreQuestTasks() {
    }

    /** Hands FTB Quests the Core's kinds of task, once, before any quest file is read. */
    static void register() {
        KnowledgeTask.type = TaskTypes.register(FtbQuestsIntegration.KNOWLEDGE, KnowledgeTask::new,
                () -> ItemIcon.getItemIcon(Items.BOOK));
        MilestoneTask.type = TaskTypes.register(FtbQuestsIntegration.MILESTONE, MilestoneTask::new,
                () -> ItemIcon.getItemIcon(Items.NETHER_STAR));
        AxisStepTask.type = TaskTypes.register(FtbQuestsIntegration.AXIS_STEP, AxisStepTask::new,
                () -> ItemIcon.getItemIcon(Items.EXPERIENCE_BOTTLE));
    }
}
