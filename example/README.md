# J's Example

The example mod of [J's Core](../core/README.md): a small technology mod built on the Core alone, and only on
what the Core promises. Read it as a tutorial, file by file; every part of it compiles and is tested on every
build.

Its unit test holds it to the promise. It reads the mod's compiled classes and checks everything they reach in
the Core against the Core's API photo (`core/src/test/resources/api/core.txt`), and it fails if the mod reaches
anything the photo does not promise, or anything of J's Computers or J's Industrial at all. A mod of your own can
run the same check: it is the test kit's `ApiUse` ([The API](../docs/API.md#holding-a-mod-to-the-promise)).

## What it adds, and where to look

| What | Where | The Core's kit |
| --- | --- | --- |
| Every block, item, cable, part, fluid and menu, declared once | `ExampleContent.java` | content ([Content](../core/docs/CONTENT.md)) |
| A generator that burns fuel into energy | `machine/GeneratorBlockEntity.java` | machines ([Machines](../core/docs/MACHINES.md)) |
| A battery | `machine/BatteryBlockEntity.java` | machines |
| A crusher, and its recipes as data | `machine/CrusherBlockEntity.java`, `src/main/resources/data/jsexample/recipe/crushing` | machines |
| A tank, filled by buckets and pipes | `machine/TankBlock.java`, `machine/TankBlockEntity.java` | block entity fields ([Content](../core/docs/CONTENT.md)) |
| Oil, with its block and its bucket | `ExampleContent.OIL` | fluids ([Content](../core/docs/CONTENT.md)) |
| An energy cable and a fluid pipe in the Core's cable block | `ExampleContent.ENERGY_CABLE`, `ExampleContent.FLUID_PIPE` | cables ([Cables](../core/docs/CABLES.md)) |
| A level sensor mounted on a cable | `part/LevelSensorPart.java` | parts on cables ([Cables](../core/docs/CABLES.md)) |
| A settings file | `ExampleSettings.java` | settings ([Settings](../core/docs/SETTINGS.md)) |
| A number kept for the whole server and one for each player | `ExampleState.java` | state ([State and saves](../core/docs/STATE_AND_SAVES.md)) |
| A simple screen declared in a file | `screen/LogbookMenu.java`, `client/JsExampleClient.java`, `src/main/resources/assets/jsexample/jstech/screens/logbook.json` | screens ([Screens](../core/docs/SCREENS.md)) |
| Block states, models, English names and loot, generated | `JsExample.java`, `src/generated/resources` | data generation ([Getting started](../core/docs/GETTING_STARTED.md)) |

The blocks wear the game's own textures, so the mod carries no pictures of its own; the two models it writes by
hand are the plugs its cable and pipe end in at a block.

## Playing it

In a development run of the series it is loaded with the other mods. Its creative tab, J's Example, holds all of
it: put coal in the generator, run the energy cable to the battery and the crusher, put cobblestone in the crusher,
mount the level sensor on the cable beside the battery and read it with a comparator or redstone, and open the
logbook to see the energy the server made and how many times you opened it.

## Tested

Its GameTests are the test mod's suite `example` (`./gradlew :tests:runGameTestServer -PgtMods=example`): the
generator burning fuel as its setting says and counting it for the server, the cable carrying its energy to the
battery, the crusher working its recipe file, the pipe and a bucket filling the tank with oil, the sensor reading
the battery, and the logbook counting a player's openings and starting over from its button.
