/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */

// The recipes the GameTests look for, written the way a modpack writes its own; computers/docs/MODPACKS.md shows them.
ServerEvents.recipes(event => {
  // A plain item, by its id.
  event.shaped('jsc:vintage_personal_computer', ['III', 'IRI', 'III'], {
    I: 'minecraft:iron_ingot',
    R: 'minecraft:redstone'
  }).id('jstests:vintage_personal_computer')
  // An installer exists only made, with its system written on it, so the script asks J's Computers for one.
  event.shapeless(JsComputers.installer('frames_95'), ['minecraft:paper', 'jsc:cd_rom'])
    .id('jstests:frames_95_installer')
  // Anything of one era, by the era's tag.
  event.shapeless('minecraft:diamond', ['#jsc:era/vintage'])
    .id('jstests:any_vintage_thing')
})

ServerEvents.recipes(event => {
  JsComputers.systems('vintage').forEach(system => {
    event.shapeless(JsComputers.installer(system), ['minecraft:paper', 'jsc:floppy_disk'])
  })
})
