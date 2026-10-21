/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbquests;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The words of the Core's tasks in FTB Quests: the name of each kind of task and of its fields, under the keys FTB
 * Quests looks them up by, and the title a task takes when its quest gives it none. Nothing here names FTB Quests, so
 * the words are in the game whether or not it is installed.
 */
@TextHolder
public final class QuestTexts {

    public static final TextKey KNOWLEDGE_TASK = TextKey.of("ftbquests.task.jscore.knowledge", "Knowledge");
    public static final TextKey KNOWLEDGE_FIELD = TextKey.of("ftbquests.task.jscore.knowledge.knowledge",
            "Knowledge");
    public static final TextKey MILESTONE_TASK = TextKey.of("ftbquests.task.jscore.milestone", "Milestone");
    public static final TextKey MILESTONE_FIELD = TextKey.of("ftbquests.task.jscore.milestone.milestone",
            "Milestone");
    public static final TextKey STEP_TASK = TextKey.of("ftbquests.task.jscore.axis_step", "Step Along an Axis");
    public static final TextKey STEP_AXIS_FIELD = TextKey.of("ftbquests.task.jscore.axis_step.axis", "Axis");
    public static final TextKey STEP_FIELD = TextKey.of("ftbquests.task.jscore.axis_step.step", "Step");
    /** A knowledge task's title: "Know Fusion". */
    public static final TextKey KNOW = TextKey.of("jscore.quest.know", "Know %s");
    /** A milestone task's title: "Reach Orbit". */
    public static final TextKey PASS = TextKey.of("jscore.quest.pass", "Reach %s");
    /** An axis task's title: "Hardware era: Legacy". */
    public static final TextKey STEP = TextKey.of("jscore.quest.step", "%s: %s");

    private QuestTexts() {
    }
}
