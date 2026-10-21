/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
import mods.jscore.Core;
import mods.jscore.Processing;

// A processing machine's recipe from a script, by the machine kind's id; core/docs/MODPACKS.md shows it.
Processing.addRecipe("jstests:test_pressing", "jstests_pressing", [<item:minecraft:emerald> * 3],
    [<item:minecraft:redstone_block>], 40, 5);

// The team a GameTest makes learns Test Fusion, and the script has it pass Test Suborbital.
events.register<mods.jscore.event.KnowledgeGained>(event => {
    if (event.team == "jstests_crafttweaker" && event.knowledge == "jstests:test_fusion" && event.learnt) {
        Core.reach(event.server, event.team, "jstests:test_suborbital");
    }
});

// An Operation of the GameTest's own kind completes, and the script has the same team pass Test Orbit.
events.register<mods.jscore.event.OperationCompleted>(event => {
    if (event.type == "jstests_script_check") {
        Core.reach(event.server, "jstests_crafttweaker", "jstests:test_orbit");
    }
});
