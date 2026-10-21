/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/** The studio's pages, each a tab under its bar, in their order: the things of the game, and the tools. */
@TextHolder
public enum StudioTool {

    THINGS(TextKey.of("gui.jscore.studio.tool.things", "Things")),
    BLOCK(TextKey.of("gui.jscore.studio.tool.block", "Block")),
    COST(TextKey.of("gui.jscore.studio.tool.cost", "Cost")),
    PAYLOADS(TextKey.of("gui.jscore.studio.tool.payloads", "Payloads")),
    RECIPES(TextKey.of("gui.jscore.studio.tool.recipes", "Recipes")),
    TAGS(TextKey.of("gui.jscore.studio.tool.tags", "Tags")),
    LOOT(TextKey.of("gui.jscore.studio.tool.loot", "Loot")),
    TRANSLATIONS(TextKey.of("gui.jscore.studio.tool.translations", "Translations")),
    SETTINGS(TextKey.of("gui.jscore.studio.tool.settings", "Settings")),
    STRUCTURES(TextKey.of("gui.jscore.studio.tool.structures", "Structures")),
    TESTS(TextKey.of("gui.jscore.studio.tool.tests", "Tests")),
    SAVE(TextKey.of("gui.jscore.studio.tool.save", "Save")),
    LOGS(TextKey.of("gui.jscore.studio.tool.logs", "Logs")),
    AGENTS(TextKey.of("gui.jscore.studio.tool.agents", "Agents"));

    private final TextKey name;

    StudioTool(final TextKey name) {
        this.name = name;
    }

    /** Every page, in the order of the tabs. */
    public static List<StudioTool> inOrder() {
        return List.of(values());
    }

    /** What its tab says. */
    public TextKey title() {
        return this.name;
    }
}
