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

/** What the studio's tools say. */
@TextHolder
public final class ToolTexts {

    public static final TextKey ASKING = TextKey.of("gui.jscore.studio.tools.asking", "Asking the server...");
    public static final TextKey NO_BLOCK = TextKey.of("gui.jscore.studio.tools.no_block",
            "Look at a block, then open the studio");
    public static final TextKey LOOK_AGAIN = TextKey.of("gui.jscore.studio.tools.look_again", "Look again");
    public static final TextKey LOOK_AGAIN_SAID = TextKey.of("gui.jscore.studio.tools.look_again_said",
            "Look at a block and open the studio again: it opens on this tool");
    /* How a place is written. */
    public static final TextKey AT = TextKey.of("gui.jscore.studio.tools.at", "%s at %s");
    public static final TextKey PLACE = TextKey.of("gui.jscore.studio.tools.place", "%s, %s, %s");

    public static final TextKey BLOCK_HEAD = TextKey.of("gui.jscore.studio.block.head",
            "The block you look at, read every second");
    public static final TextKey WATCHING_ON = TextKey.of("gui.jscore.studio.block.watching_on", "Watching: on");
    public static final TextKey WATCHING_OFF = TextKey.of("gui.jscore.studio.block.watching_off", "Watching: off");
    public static final TextKey FIELDS_LIVE = TextKey.of("gui.jscore.studio.block.fields", "Its fields, live");
    public static final TextKey NO_FIELDS = TextKey.of("gui.jscore.studio.block.no_fields",
            "It has no block entity");
    public static final TextKey FACES = TextKey.of("gui.jscore.studio.block.faces", "What each face offers");
    public static final TextKey IN_AND_OUT = TextKey.of("gui.jscore.studio.block.in_and_out",
            "In and out, each second");
    public static final TextKey NORTH = TextKey.of("gui.jscore.studio.block.north", "North");
    public static final TextKey SOUTH = TextKey.of("gui.jscore.studio.block.south", "South");
    public static final TextKey EAST = TextKey.of("gui.jscore.studio.block.east", "East");
    public static final TextKey WEST = TextKey.of("gui.jscore.studio.block.west", "West");
    public static final TextKey UP = TextKey.of("gui.jscore.studio.block.up", "Up");
    public static final TextKey DOWN = TextKey.of("gui.jscore.studio.block.down", "Down");
    public static final TextKey NOTHING = TextKey.of("gui.jscore.studio.block.nothing", "nothing");
    /* Stored and capacity. */
    public static final TextKey FACE_ENERGY = TextKey.of("gui.jscore.studio.block.face_energy",
            "energy: %s of %s FE");
    /* Slots, and what the first hold. */
    public static final TextKey FACE_ITEMS = TextKey.of("gui.jscore.studio.block.face_items", "items: %s slots%s");
    /* Tanks, and what they hold. */
    public static final TextKey FACE_FLUID = TextKey.of("gui.jscore.studio.block.face_fluid", "fluid: %s tanks%s");
    /* An item or a fluid, and how much. */
    public static final TextKey HELD = TextKey.of("gui.jscore.studio.block.held", ", %s x%s");
    public static final TextKey ENERGY_IN = TextKey.of("gui.jscore.studio.block.energy_in", "Energy in");
    public static final TextKey ENERGY_OUT = TextKey.of("gui.jscore.studio.block.energy_out", "Energy out");
    public static final TextKey ITEMS_IN = TextKey.of("gui.jscore.studio.block.items_in", "Items in");
    public static final TextKey ITEMS_OUT = TextKey.of("gui.jscore.studio.block.items_out", "Items out");
    public static final TextKey FLUID_IN = TextKey.of("gui.jscore.studio.block.fluid_in", "Fluid in");
    public static final TextKey FLUID_OUT = TextKey.of("gui.jscore.studio.block.fluid_out", "Fluid out");
    public static final TextKey FE_PER_SECOND = TextKey.of("gui.jscore.studio.block.fe_per_second", "%s FE/s");
    public static final TextKey PER_SECOND = TextKey.of("gui.jscore.studio.block.per_second", "%s/s");
    public static final TextKey MB_PER_SECOND = TextKey.of("gui.jscore.studio.block.mb_per_second", "%s mB/s");
    public static final TextKey NOT_COUNTED = TextKey.of("gui.jscore.studio.block.not_counted",
            "Only a block entity of the Core counts what crosses its faces");

    public static final TextKey COST_HEAD = TextKey.of("gui.jscore.studio.cost.head",
            "What each machine costs the server, in microseconds a tick");
    /* Seconds. */
    public static final TextKey MEASURE = TextKey.of("gui.jscore.studio.cost.measure", "Measure %s s");
    public static final TextKey MEASURING = TextKey.of("gui.jscore.studio.cost.measuring", "Measuring...");
    public static final TextKey HEAT_ON = TextKey.of("gui.jscore.studio.cost.heat_on", "Heat map: on");
    public static final TextKey HEAT_OFF = TextKey.of("gui.jscore.studio.cost.heat_off", "Heat map: off");
    public static final TextKey BLOCK = TextKey.of("gui.jscore.studio.cost.block", "Block");
    public static final TextKey WHERE = TextKey.of("gui.jscore.studio.cost.where", "Where");
    public static final TextKey COST = TextKey.of("gui.jscore.studio.cost.cost", "µs / tick");
    public static final TextKey OWNER = TextKey.of("gui.jscore.studio.cost.owner", "Owner");
    public static final TextKey IN_THE_WORLD = TextKey.of("gui.jscore.studio.cost.in_the_world", "In the world");
    public static final TextKey HEAT_LEGEND = TextKey.of("gui.jscore.studio.cost.legend",
            "Each block glows as dear as it is;");
    public static final TextKey HEAT_LEGEND_2 = TextKey.of("gui.jscore.studio.cost.legend_2",
            "the dearest red, the cheapest yellow.");
    /* How many, and the milliseconds. */
    public static final TextKey MEASURED = TextKey.of("gui.jscore.studio.cost.measured",
            "%s machines measured, %s ms a tick");
    public static final TextKey NOT_MEASURED = TextKey.of("gui.jscore.studio.cost.not_measured",
            "Press Measure to time every machine");
    /* Seconds. */
    public static final TextKey MEASURED_FOR = TextKey.of("gui.jscore.studio.cost.measured_for",
            "measured for %s s");

    public static final TextKey PAYLOADS_HEAD = TextKey.of("gui.jscore.studio.payloads.head",
            "What the server sends you, tick by tick");
    public static final TextKey PAYLOADS_EVERYONE_HEAD = TextKey.of("gui.jscore.studio.payloads.everyone_head",
            "What the server sends everyone, tick by tick");
    public static final TextKey EVERYONE = TextKey.of("gui.jscore.studio.payloads.everyone", "Everyone");
    public static final TextKey TO_YOU = TextKey.of("gui.jscore.studio.payloads.to_you", "To you");
    /* The tick, and its size. */
    public static final TextKey BYTES_CAPTION = TextKey.of("gui.jscore.studio.payloads.caption",
            "Bytes a tick: the white bar is tick %s, %s");
    /* The tick, how many packets and their size. */
    public static final TextKey TICK_HEAD = TextKey.of("gui.jscore.studio.payloads.tick", "Tick %s: %s packets, %s");
    public static final TextKey KIND = TextKey.of("gui.jscore.studio.payloads.kind", "Kind");
    public static final TextKey MOD = TextKey.of("gui.jscore.studio.payloads.mod", "Mod");
    public static final TextKey PACKETS = TextKey.of("gui.jscore.studio.payloads.packets", "Packets");
    public static final TextKey BYTES = TextKey.of("gui.jscore.studio.payloads.bytes", "Bytes");
    public static final TextKey BIGGEST = TextKey.of("gui.jscore.studio.payloads.biggest", "Biggest");
    public static final TextKey NOTHING_SENT = TextKey.of("gui.jscore.studio.payloads.nothing",
            "Nothing was sent in the last hundred ticks");
    public static final TextKey LAST_100_YOU = TextKey.of("gui.jscore.studio.payloads.crumb_you",
            "to you, the last 100 ticks");
    public static final TextKey LAST_100_EVERYONE = TextKey.of("gui.jscore.studio.payloads.crumb_everyone",
            "to everyone, the last 100 ticks");

    /* The thing. */
    public static final TextKey MAKE = TextKey.of("gui.jscore.studio.recipes.make", "Make %s");
    public static final TextKey NEW_RECIPE = TextKey.of("gui.jscore.studio.recipes.new", "+ A new recipe");
    public static final TextKey WRITTEN_HERE = TextKey.of("gui.jscore.studio.recipes.written_here", "written here");
    public static final TextKey TRY = TextKey.of("gui.jscore.studio.recipes.try", "Try");
    public static final TextKey SAVE = TextKey.of("gui.jscore.studio.recipes.save", "Save");
    /* The kind of recipe. */
    public static final TextKey RIGHT = TextKey.of("gui.jscore.studio.recipes.right", "Read as a recipe of %s: right.");
    /* What is wrong. */
    public static final TextKey WRONG = TextKey.of("gui.jscore.studio.recipes.wrong", "Not a recipe: %s");
    public static final TextKey VIEWER = TextKey.of("gui.jscore.studio.recipes.viewer",
            "As the recipe viewer shows it");
    public static final TextKey AGAINST = TextKey.of("gui.jscore.studio.recipes.against", "Against the others");
    public static final TextKey SAME_INPUTS = TextKey.of("gui.jscore.studio.recipes.same_inputs", "Same inputs");
    /* The other recipe. */
    public static final TextKey SAME_INPUTS_DETAIL = TextKey.of("gui.jscore.studio.recipes.same_inputs_detail",
            "%s takes the same inputs");
    public static final TextKey DID_NOT_LOAD = TextKey.of("gui.jscore.studio.recipes.did_not_load",
            "A recipe that did not load");
    public static final TextKey CHANGED_BY_PACK = TextKey.of("gui.jscore.studio.recipes.changed",
            "Changed by the pack");
    /* The recipe. */
    public static final TextKey PACK_REPLACED = TextKey.of("gui.jscore.studio.recipes.replaced",
            "the pack replaced %s");
    public static final TextKey PACK_REMOVED = TextKey.of("gui.jscore.studio.recipes.removed",
            "the pack took %s away");
    public static final TextKey PACK_SWAPPED = TextKey.of("gui.jscore.studio.recipes.swapped",
            "the pack gave %s other items");
    public static final TextKey ADDED_BY_PACK = TextKey.of("gui.jscore.studio.recipes.added", "Added by the pack");
    /* The recipe and its pack. */
    public static final TextKey FROM_PACK = TextKey.of("gui.jscore.studio.recipes.from_pack", "%s, from %s");
    public static final TextKey NOTHING_AGAINST = TextKey.of("gui.jscore.studio.recipes.nothing_against",
            "Nothing stands against it");
    /* The file. */
    public static final TextKey SAVED_TO = TextKey.of("gui.jscore.studio.recipes.saved", "Saved to %s");
    public static final TextKey NO_SOURCE = TextKey.of("gui.jscore.studio.recipes.no_source",
            "This run has no source of the mod's to save it to");
    public static final TextKey CHOOSE_FIRST = TextKey.of("gui.jscore.studio.tools.choose_first",
            "Choose a thing on the Things page first");

    /* The thing. */
    public static final TextKey IS_IN = TextKey.of("gui.jscore.studio.tags.is_in", "%s is in");
    public static final TextKey IN_NO_TAG = TextKey.of("gui.jscore.studio.tags.no_tag", "It is in no tag");
    public static final TextKey REGISTRIES = TextKey.of("gui.jscore.studio.tags.registries", "Registries");
    public static final TextKey REGISTRY_ID = TextKey.of("gui.jscore.studio.tags.id", "Id");
    public static final TextKey REGISTRY_FROM = TextKey.of("gui.jscore.studio.tags.from", "From");
    public static final TextKey REGISTRY_STACK = TextKey.of("gui.jscore.studio.tags.stack", "Stack");
    public static final TextKey REGISTRY_COMPONENTS = TextKey.of("gui.jscore.studio.tags.components", "Components");
    /* The tag, and how many. */
    public static final TextKey HOLDS = TextKey.of("gui.jscore.studio.tags.holds", "%s holds %s");
    public static final TextKey CAME_FROM = TextKey.of("gui.jscore.studio.tags.came_from", "Where each came from");
    /* The pack. */
    public static final TextKey TAG_FILE = TextKey.of("gui.jscore.studio.tags.tag_file", "%s, the tag file");
    public static final TextKey REPLACED_BEFORE = TextKey.of("gui.jscore.studio.tags.replaced",
            "%s, replacing what came before");

    /* The tool. */
    public static final TextKey TOOL = TextKey.of("gui.jscore.studio.loot.tool", "Tool: %s");
    public static final TextKey HAND = TextKey.of("gui.jscore.studio.loot.hand", "Hand");
    /* The level. */
    public static final TextKey FORTUNE = TextKey.of("gui.jscore.studio.loot.fortune", "Fortune: %s");
    public static final TextKey FORTUNE_NONE = TextKey.of("gui.jscore.studio.loot.fortune_none", "none");
    public static final TextKey SILK = TextKey.of("gui.jscore.studio.loot.silk", "Silk Touch");
    /* How many. */
    public static final TextKey ROLLS = TextKey.of("gui.jscore.studio.loot.rolls", "Rolls: %s");
    public static final TextKey ROLL = TextKey.of("gui.jscore.studio.loot.roll", "Roll");
    /* How many. */
    public static final TextKey WHAT_ROLLS_GAVE = TextKey.of("gui.jscore.studio.loot.gave", "What %s rolls gave");
    public static final TextKey ITEM = TextKey.of("gui.jscore.studio.loot.item", "Item");
    public static final TextKey IN_ROLLS = TextKey.of("gui.jscore.studio.loot.in_rolls", "In rolls");
    public static final TextKey EACH_ROLL = TextKey.of("gui.jscore.studio.loot.each_roll", "Each roll");
    public static final TextKey FEWEST_MOST = TextKey.of("gui.jscore.studio.loot.fewest_most", "Fewest, most");
    /* How many on average. */
    public static final TextKey LOOT_SUMMARY = TextKey.of("gui.jscore.studio.loot.summary",
            "Each roll gives %s items on average");
    /* The table. */
    public static final TextKey NO_TABLE = TextKey.of("gui.jscore.studio.loot.no_table", "There is no loot table %s");
    public static final TextKey PRESS_ROLL = TextKey.of("gui.jscore.studio.loot.press_roll",
            "Press Roll to roll the table");

    public static final TextKey EVERY_MOD = TextKey.of("gui.jscore.studio.translations.every_mod", "Every mod");
    /* How many. */
    public static final TextKey KEYS = TextKey.of("gui.jscore.studio.translations.keys", "%s keys");
    /* The language, and how many. */
    public static final TextKey LACKS = TextKey.of("gui.jscore.studio.translations.lacks", "What %s lacks: %s");
    /* The language. */
    public static final TextKey LANGUAGE = TextKey.of("gui.jscore.studio.translations.language", "Language: %s");
    /* The English. */
    public static final TextKey IN_ENGLISH = TextKey.of("gui.jscore.studio.translations.in_english", "en_us: %s");
    /* The file. */
    public static final TextKey OPEN_FILE = TextKey.of("gui.jscore.studio.translations.open", "Open %s");
    public static final TextKey TOO_LONG = TextKey.of("gui.jscore.studio.translations.too_long",
            "Texts too long for their place");
    /* The component and the language. */
    public static final TextKey TOO_LONG_WHAT = TextKey.of("gui.jscore.studio.translations.too_long_what",
            "%s, in %s");
    /* Pixels. */
    public static final TextKey PAST_ITS_BOX = TextKey.of("gui.jscore.studio.translations.past",
            "%s px past its box");
    public static final TextKey NOTHING_LACKS = TextKey.of("gui.jscore.studio.translations.nothing_lacks",
            "Nothing is missing");
    public static final TextKey EVERY_TEXT_FITS = TextKey.of("gui.jscore.studio.translations.fits",
            "Every text fits");

    public static final TextKey EVERY_FILE = TextKey.of("gui.jscore.studio.settings.every_file",
            "Every mod and file");
    public static final TextKey SETTING = TextKey.of("gui.jscore.studio.settings.setting", "Setting");
    public static final TextKey VALUE = TextKey.of("gui.jscore.studio.settings.value", "Value");
    public static final TextKey DEFAULT = TextKey.of("gui.jscore.studio.settings.default", "Default");
    public static final TextKey RANGE = TextKey.of("gui.jscore.studio.settings.range", "Range");
    /* The least and the most. */
    public static final TextKey RANGE_VALUE = TextKey.of("gui.jscore.studio.settings.range_value", "%s to %s");
    public static final TextKey CHANGED_NOTE = TextKey.of("gui.jscore.studio.settings.changed_note",
            "Yellow: the pack changed it.");
    public static final TextKey EXPORT = TextKey.of("gui.jscore.studio.settings.export", "Export the pack's preset");
    /* The file, and how many. */
    public static final TextKey EXPORTED = TextKey.of("gui.jscore.studio.settings.exported", "%s: %s settings");
    /* What went wrong. */
    public static final TextKey NOT_EXPORTED = TextKey.of("gui.jscore.studio.settings.not_exported",
            "The preset was not written: %s");

    public static final TextKey CATCH = TextKey.of("gui.jscore.studio.structures.catch", "Catch a structure");
    public static final TextKey CORNER_1 = TextKey.of("gui.jscore.studio.structures.corner_1", "Corner 1");
    public static final TextKey CORNER_2 = TextKey.of("gui.jscore.studio.structures.corner_2", "Corner 2");
    public static final TextKey CORNER_HINT = TextKey.of("gui.jscore.studio.structures.hint",
            "Look at a block, open the studio and press a corner.");
    public static final TextKey NOT_SET = TextKey.of("gui.jscore.studio.structures.not_set", "not set");
    public static final TextKey SIZE = TextKey.of("gui.jscore.studio.structures.size", "Size");
    /* The three sides. */
    public static final TextKey SIZE_VALUE = TextKey.of("gui.jscore.studio.structures.size_value", "%s x %s x %s");
    public static final TextKey BLOCKS = TextKey.of("gui.jscore.studio.structures.blocks", "Blocks");
    /* How many. */
    public static final TextKey BLOCKS_VALUE = TextKey.of("gui.jscore.studio.structures.blocks_value",
            "%s, air left out");
    public static final TextKey BLOCK_ENTITIES = TextKey.of("gui.jscore.studio.structures.entities",
            "Block entities");
    /* How many. */
    public static final TextKey BLOCK_ENTITIES_VALUE = TextKey.of("gui.jscore.studio.structures.entities_value",
            "%s, kept with what they hold");
    public static final TextKey NAME = TextKey.of("gui.jscore.studio.structures.name", "Name");
    public static final TextKey SAVE_TO_MOD = TextKey.of("gui.jscore.studio.structures.save", "Save to the mod");
    public static final TextKey SHOW_PROJECTOR = TextKey.of("gui.jscore.studio.structures.show",
            "Show with the projector");
    public static final TextKey HIDE_PROJECTOR = TextKey.of("gui.jscore.studio.structures.hide",
            "Hide the projector's view");
    public static final TextKey AS_PROJECTOR = TextKey.of("gui.jscore.studio.structures.as_projector",
            "As the projector shows it");
    public static final TextKey A_NEW_ONE = TextKey.of("gui.jscore.studio.structures.new", "a new one");
    public static final TextKey NOT_WRITTEN = TextKey.of("gui.jscore.studio.structures.not_written",
            "The structure was not written; the log says why");

    public static final TextKey RECORDING_A_TEST = TextKey.of("gui.jscore.studio.tests.recording",
            "Recording a test");
    /* The block. */
    public static final TextKey STEP_USE = TextKey.of("gui.jscore.studio.tests.step_use", "Use %s");
    /* The component. */
    public static final TextKey STEP_CLICK = TextKey.of("gui.jscore.studio.tests.step_click", "Click '%s'");
    /* Across and down from the middle. */
    public static final TextKey STEP_CLICK_AT = TextKey.of("gui.jscore.studio.tests.step_click_at",
            "Click %s, %s from the middle");
    /* What is typed. */
    public static final TextKey STEP_TYPE = TextKey.of("gui.jscore.studio.tests.step_type", "Type '%s'");
    /* The key. */
    public static final TextKey STEP_KEY = TextKey.of("gui.jscore.studio.tests.step_key", "Press %s");
    /* How many ticks. */
    public static final TextKey STEP_WAIT = TextKey.of("gui.jscore.studio.tests.step_wait", "Wait %s ticks");
    /* The picture. */
    public static final TextKey STEP_PICTURE = TextKey.of("gui.jscore.studio.tests.step_picture",
            "Take the picture '%s'");
    public static final TextKey STEP_CLOSE = TextKey.of("gui.jscore.studio.tests.step_close", "Close the screen");
    public static final TextKey RECORDING_NOW = TextKey.of("gui.jscore.studio.tests.recording_now", "recording...");
    public static final TextKey STOP = TextKey.of("gui.jscore.studio.tests.stop", "Stop");
    public static final TextKey KEEP = TextKey.of("gui.jscore.studio.tests.keep", "Keep it as a client test");
    /* The test and the mod. */
    public static final TextKey KEPT = TextKey.of("gui.jscore.studio.tests.kept", "%s, in the client tests of %s");
    public static final TextKey NOTHING_RECORDED = TextKey.of("gui.jscore.studio.tests.nothing",
            "Press Record a test, then click through a screen");
    public static final TextKey REFERENCES = TextKey.of("gui.jscore.studio.tests.references", "Reference pictures");
    /* How many. */
    public static final TextKey DIFFERS = TextKey.of("gui.jscore.studio.tests.differs", "differs in %s pixels");
    public static final TextKey SAME = TextKey.of("gui.jscore.studio.tests.same", "the same");
    public static final TextKey NO_REFERENCE = TextKey.of("gui.jscore.studio.tests.no_reference",
            "no reference yet");
    public static final TextKey EXPECTED = TextKey.of("gui.jscore.studio.tests.expected", "Expected");
    public static final TextKey NOW = TextKey.of("gui.jscore.studio.tests.now", "Now");
    public static final TextKey DIFFERENCE = TextKey.of("gui.jscore.studio.tests.difference", "The difference");
    public static final TextKey TAKE_NEW = TextKey.of("gui.jscore.studio.tests.take_new", "Take the new one");
    /* The picture. */
    public static final TextKey TAKEN = TextKey.of("gui.jscore.studio.tests.taken", "%s is the reference now");
    public static final TextKey NO_PICTURES = TextKey.of("gui.jscore.studio.tests.no_pictures",
            "No mod here has reference pictures");
    public static final TextKey PICTURE = TextKey.of("gui.jscore.studio.tests.picture", "Picture");
    public static final TextKey STOP_RECORDING = TextKey.of("gui.jscore.studio.tests.stop_recording",
            "Stop recording");

    public static final TextKey SAVE_HEAD = TextKey.of("gui.jscore.studio.save.head",
            "The block saved and read back, as a world closed and opened again");
    public static final TextKey SAVE_AND_READ = TextKey.of("gui.jscore.studio.save.save", "Save and read it back");
    public static final TextKey FIELD = TextKey.of("gui.jscore.studio.save.field", "Field");
    public static final TextKey BEFORE = TextKey.of("gui.jscore.studio.save.before", "Before");
    public static final TextKey READ_BACK = TextKey.of("gui.jscore.studio.save.read_back", "Read back");
    /* How many. */
    public static final TextKey CHANGED_SUMMARY = TextKey.of("gui.jscore.studio.save.changed",
            "%s fields come back changed: what it saves and what it reads differ.");
    public static final TextKey SAME_SUMMARY = TextKey.of("gui.jscore.studio.save.same",
            "Every field comes back as it went.");
    public static final TextKey PRESS_SAVE = TextKey.of("gui.jscore.studio.save.press",
            "Press Save and read it back");

    public static final TextKey EVERY_SYSTEM = TextKey.of("gui.jscore.studio.logs.every_system", "Every system");
    public static final TextKey ERRORS_THAT_TEACH = TextKey.of("gui.jscore.studio.logs.errors", "Errors that teach");
    public static final TextKey NO_LINES = TextKey.of("gui.jscore.studio.logs.no_lines",
            "It has written nothing since the studio first opened");
    public static final TextKey NO_ERRORS = TextKey.of("gui.jscore.studio.logs.no_errors",
            "No declaration has gone wrong");
    /* The mod. */
    public static final TextKey UNKNOWN_MOD = TextKey.of("gui.jscore.studio.logs.unknown_mod", "A mod");

    private ToolTexts() {
    }
}
