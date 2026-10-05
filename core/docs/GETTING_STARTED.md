# Getting started

This page takes you from an empty NeoForge mod to one that declares its first block and item on J's Core and
has the game's files written for it. It assumes you can program in Java and have a NeoForge 1.21.1 mod that
builds; it assumes nothing about the Core.

## What the Core is

*Added 2026-09-05.*

J's Core is a library: code other mods are built on. On its own it adds almost nothing to play. What it gives a
mod is the plumbing a technology mod needs and would otherwise write for itself: declaring blocks and items
once and having their models, names and loot written from that one declaration; block entities whose state is
saved and sent to players by saying so on each field; menus; energy, fluids and items moving between blocks;
cables; networks of devices; processing machines and their recipes; multiblocks; sounds; screens; settings;
dimensions; entities; and more, each with a page of its own here.

Every mod of the J's Tech Series is built on it, and any other mod may be too, whatever its licence (the
[Core's README](../README.md#using-the-core-in-your-project) says what the licence asks in return).

### What is promised, and what is not yet

Two kinds of code live in the Core:

- **The API**: the packages named `api` (`dev.jstech.core.api` and `dev.jstech.core.api.client`) and a few types
  [docs/API.md](../../docs/API.md) lists. Everything there is written down line by line, carries a version
  number, and is not taken away without a cycle of warning.
- **Everything else**: the library parts these pages describe. They work, the series' own mods use them every
  day, and they are documented so you can use them too. What they are not yet is promised: until they move into
  the API, a release may change their shape, and the changelog says so when it does. If your mod depends on one,
  read the changelog's "API" section before you update.

## Adding the Core to your project

*Added 2026-09-05.*

The Core is not on a public Maven repository yet. Until it is, build against the jar of a release:

1. Download `jscore-1.21.1-<version>.jar` from the
   [releases of the series](https://github.com/jvpts11/js-tech-series/releases).
2. Put it in a folder of your project, for example `libs/`.
3. Depend on it in your `build.gradle`:

   ```groovy
   dependencies {
       implementation files('libs/jscore-1.21.1-<version>.jar')
   }
   ```

4. Tell the game your mod needs it, in `src/main/resources/META-INF/neoforge.mods.toml` (replace `yourmod` with
   your mod's id):

   ```toml
   [[dependencies.yourmod]]
   modId = "jscore"
   type = "required"
   versionRange = "[<version>,)"
   ordering = "AFTER"
   side = "BOTH"
   ```

   `ordering = "AFTER"` makes the game load the Core before your mod, so everything the Core sets up is there when
   your mod's code runs.

The mods of the series itself depend on the Core as a Gradle project (`implementation project(':core')`) and ask
for exactly their own version, because the whole series is released together.

## Your mod's class

*Added 2026-09-23.*

Everything a mod adds to the game (blocks, items, block entities, creative tabs, sounds, fluids, cables,
entities) is declared on one object, a `ModContent`, made once for your mod:

```java
@Mod(MyMod.MODID)
public final class MyMod {

    public static final String MODID = "mymod";

    public MyMod(final IEventBus modEventBus) {
        MyContent.CONTENT.register(modEventBus);
    }
}
```

```java
public final class MyContent {

    public static final ModContent CONTENT = new ModContent(MyMod.MODID);

    private MyContent() {
    }
}
```

`register` hands everything declared on it to the game. The game's registration happens through what NeoForge
calls the **mod bus**, the `IEventBus` your mod's constructor is given: every mod has its own, and the game asks
each mod for its blocks, items and the rest through it while it loads.

One `ModContent` per mod: making a second one for the same mod id throws, so two classes cannot each believe
they own your mod's content.

## Your first block and item

*Added 2026-09-23.*

A declaration names the thing once, in English, says how it looks, and registers it. Everything else (the
block state file, the models, the English name, the loot table that makes it drop itself) is written from that
declaration when you run the data generation, below.

```java
public final class MyContent {

    public static final ModContent CONTENT = new ModContent(MyMod.MODID);

    public static final ContentTab TAB = CONTENT.tab("main", "My Mod", () -> MyContent.COPPER_BOX);
    public static final ContentTab.Section MAIN = TAB.section();

    public static final BlockEntry<Block> COPPER_BOX = CONTENT.block("copper_box", Block::new)
            .properties(p -> p.strength(2.0F).requiresCorrectToolForDrops())
            .named("Copper Box")
            .look(IBlockLook.cubeAll("copper_box"))
            .item()
            .tab(MAIN)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .register();

    public static final ItemEntry<Item> COPPER_WIRE = CONTENT.item("copper_wire", Item::new)
            .named("Copper Wire")
            .look(IItemLook.FLAT)
            .tab(MAIN)
            .register();

    private MyContent() {
    }
}
```

- `block(id, factory)` and `item(id, factory)` take the id (lower case, the part after `mymod:`) and how to
  make the block or item from its properties.
- `named` gives the English name. It is required: a block without one fails at `register()`.
- `look` says how it is drawn. `IBlockLook.cubeAll("copper_box")` is a cube with the same texture on all six
  faces, read from `assets/mymod/textures/block/copper_box.png`; `IItemLook.FLAT` is an item drawn from its
  texture in `textures/item/`. [Content](CONTENT.md) lists every look.
- `item()` gives the block an item, so a player can hold it and place it. Without it the block can only be
  placed by code.
- `tab` puts it in a section of a creative tab. A tab is made of sections shown one after another, each made once
  with `section()`; whatever is declared into a section shows in it, in the order it was declared, so the tab is
  never a second list to keep in step with your declarations.
- `tag` adds it to a block tag, here the one that lets a pickaxe mine it.
- `register()` checks the declaration and hands it over. What it gives back (`BlockEntry`, `ItemEntry`) is
  NeoForge's `DeferredBlock` and `DeferredItem`: call `get()` on it once the game has loaded to reach the block
  or item itself.

The fields are made when Java first loads the class that holds them. The class must therefore be loaded before
`register` runs: here it is, because `register` is called through `MyContent.CONTENT`. If you spread your
declarations over several classes, touch each of them before calling `register` (an empty `declare()` method
called from your constructor is enough).

## Writing the files: data generation

*Added 2026-09-23.*

The game reads a block's look and name from files (block states, models, language files, loot tables). The
Core writes those files from your declarations in a step called **data generation**, which runs the game with
no world and saves the files into your project. Hook it up once:

```java
@EventBusSubscriber(modid = MyMod.MODID)
public final class MyDataGenerators {

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent event) {
        ContentData.gather(event, MyContent.CONTENT);
    }
}
```

Then run your project's data run (in a project built like the series, `./gradlew runData`). The files land in
`src/generated/resources`, which your build must include as a resource folder; commit them with your code.

Run it again every time you add or change a declaration, a name, a look, a palette or a recipe. Nothing else
writes these files, so a block declared but not generated is a magenta and black cube with an untranslated
name. And read the end of the run: when it stops with an error (a texture it was told to use is missing, for
example), the providers after the error never ran, and some files are missing even though others were written.

`ContentData` also takes providers of your own, for files the Core does not write (recipes, for example):

```java
final ContentData data = ContentData.gather(event, MyContent.CONTENT);
data.server(new MyRecipeProvider(data.output(), data.lookup()));
```

## What can go wrong

- **"ModContent for mymod already exists".** Two `new ModContent("mymod")`. Keep one, in one class, and use it
  everywhere.
- **A block or item is missing from the game, with no error.** The class that declares it was never loaded
  before `register` ran. Touch the class from your constructor first.
- **`register()` throws "no name" or "no look".** Every block needs `named` and `look`; every item needs `named`.
  A block that sets `drops`, `tab` or `itemLook` also needs `item()`, since those are about its item.
- **The block shows as a magenta and black cube, or its name shows as `block.mymod.copper_box`.** The data
  generation has not run since it was declared, or it stopped on an error before reaching the models and the
  language file. Run it and read its last lines.
- **The texture is missing in game but the model is there.** The look names a texture file that is not in
  `assets/mymod/textures/`. The name in the look is the file's name without `.png`.

## Where to go next

| Page | What it covers |
| --- | --- |
| [Content](CONTENT.md) | Every builder: blocks, items, looks, tabs, tags, block entities, menus, materials, items with state. |
| [Text and colour](TEXT_AND_COLOUR.md) | Sentences a player reads, in every language; the colours of screens, which resource packs can change. |
| [Machines](MACHINES.md) | Processing machines, recipes with counts and fluids, upgrades, and JEI and EMI. |
| [Energy and resources](ENERGY_AND_RESOURCES.md) | Energy, fluids, chemicals and items moving between blocks. |
| [Networks](NETWORKS.md) and [Operations](OPERATIONS.md) | Data networks of devices, and the requests they carry out. |
| [Cables](CABLES.md) | The one cable block every mod lays its cables in. |
| [Multiblocks](MULTIBLOCKS.md) | Structures made of many blocks, with ports. |
| [World](WORLD.md) | Data per chunk and region, chunk loading, ores, structures and dimensions. |
| [State and saves](STATE_AND_SAVES.md) | Saving, versions of what is saved, data packs, schedules, time and diagnostics. |
| [Ownership](OWNERSHIP.md) | Owners, access and teams. |
| [Progression](PROGRESSION.md) | Eras, tiers, progression steps and advancements. |
| [Commands](COMMANDS.md) | Commands under `/jstech`. |
| [Entities](ENTITIES.md) | Entities, vehicles, robots, projectiles and what a player wears. |
| [Overlays](OVERLAYS.md) | The HUD, holograms and what Jade shows about a block. |
| [Manuals](MANUALS.md) | Your mod's chapter in the manuals, and manuals of its own. |
| [Settings](SETTINGS.md) | Settings files, their ranges, and the settings screen. |
| [Sound](SOUND.md) | Sounds declared once, channels, cues, synthesis and recordings. |
| [Screens](SCREENS.md) | Skins, themes, layouts you can test, and drawing text. [UI components](UI_COMPONENTS.md), [fonts](FONTS.md) and [motion](MOTION.md) have pages of their own. |
| [Testing](TESTING.md) | Testing a mod on the Core: what JUnit can test, and the GameTest kit. |
