/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */

// A processing machine's recipe written with the function the Core gives every kind; core/docs/MODPACKS.md shows it.
ServerEvents.recipes(event => {
  event.recipes.jstests.test_pressing(['2x minecraft:diamond'], ['minecraft:coal_block']).ticks(40).energyPerTick(5)
    .id('jstests:kubejs_pressing')
})

// The team a GameTest makes learns Test Comprehension whole, and the script has it pass Test Orbit.
JsCoreEvents.knowledgeGained(event => {
  if (event.team == 'jstests_kubejs' && event.knowledge == 'jstests:test_comprehension' && event.learnt) {
    JsCore.reach(event.server, event.team, 'jstests:test_orbit')
  }
})

// An Operation of the GameTest's own kind completes, and the script has the same team pass Test Suborbital.
JsCoreEvents.operationCompleted(event => {
  if (event.type == 'jstests_script_check') {
    JsCore.reach(event.server, 'jstests_kubejs', 'jstests:test_suborbital')
  }
})
