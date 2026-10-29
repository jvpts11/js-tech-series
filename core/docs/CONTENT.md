# Content

How a mod declares what it adds to the game on J's Core: blocks, items, creative tabs, block entities, the
blocks that run them, their menus, items that hold things, and the ids saved in worlds. Read
[Getting started](GETTING_STARTED.md) first: it sets up the `ModContent` every declaration here hangs from.

The idea behind all of it is the same: **say a thing once.** A block is declared in one place, with its name,
look, drops, tab and tags, and everything the game needs about it (registration, the block state file, the
models, the English name, the loot table, the tags) is made from that declaration, either while the game loads
or when you run the data generation. Nothing is a second list kept in step by hand.

[J's Example](../../example/README.md)'s `ExampleContent` declares a whole mod this way, and compiles and is tested
on every build: machines, a tank, a fluid, a cable, a pipe, a part, an item and a menu.

## Blocks

*Added 2026-09-23.*

```java
public static final BlockEntry<Block> COPPER_BOX = CONTENT.block("copper_box", Block::new)
        .properties(p -> p.strength(2.0F).requiresCorrectToolForDrops())
        .named("Copper Box")
        .look(IBlockLook.cubeAll("copper_box"))
        .item()
        .tab(MAIN)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
```

| Step | What it does | Needed? |
| --- | --- | --- |
| `block(id, factory)` | Starts a block with that id (lower case) made by that factory from its properties. | yes |
| `properties(p -> ...)` | The game's block properties: strength, sound, light, whether a tool is needed. | no |
| `named("...")` | The English name, written to the language file. | yes |
| `look(...)` | How it is drawn (below). | yes |
| `item()` or `item((block, props) -> ...)` | Gives it an item, the plain kind or one you make. | no |
| `itemLook(...)` | How its item is drawn, when not like the block. | no |
| `drops(Drops.SELF)` / `drops(Drops.NONE)` | What breaking it gives. The default is itself when it has an item, nothing otherwise. `Drops.SELF_WITH_CONTENTS` keeps what its block entity holds in the item; `Drops.HANDMADE` uses a loot table written by hand in the mod's data (grass giving dirt unless broken with silk touch) and generates none. | no |
| `tab(section)` | The creative tab section it shows in. | no |
| `tag(tagKey)` | A block tag it belongs to; repeat for more. | no |
| `geo(look)` | Drawn from a `.geo.json` model, not a block model ([Models](MODELS.md#drawing-a-block)). | no |
| `register()` | Checks the declaration and registers it. | yes |

`register()` refuses a declaration that does not hold together, with an exception naming what is wrong: a
block with no name or no look, or one that sets `drops`, `tab` or `itemLook` without having an item. It gives
back a `BlockEntry`, which is NeoForge's `DeferredBlock`: `get()` reaches the block once the game has loaded.

### Looks

A look says which model each state of the block shows, and the data generation writes the block state file and
the models from it. The texture names below are files in `assets/<your mod>/textures/block/`, without `.png`.

| Look | What it draws |
| --- | --- |
| `IBlockLook.cubeAll(id)` | The texture `id` on all six faces. |
| `IBlockLook.column(id)` | A pillar: `id_side` around, `id_top` on both ends. |
| `IBlockLook.orientable(id)` | A machine facing the way it was placed: `id_front`, `id_side`, `id_top`. Pass `IBlockLook::orientable` to `look` and it is given the block's id. |
| `IBlockLook.fixed(model)` | One model whatever the state. |
| `IBlockLook.axis(model)` | One model laid along the block's axis, as the game's logs are; the block is a `RotatedPillarBlock`. |
| `IBlockLook.byState(state -> model)` | A model chosen from the whole state and never turned: a sprinkler open or shut, a detector's lamp lit or dark. |
| `IBlockLook.facing(model)` | One model turned to the block's horizontal facing; `.frontAgainstFacing()` turns it round, `.whileOn(property, otherModel)` swaps the model while a property is on (a screen lit, a drive loaded). |
| `IBlockLook.facingByState(state -> model)` | A model chosen from the whole state, turned to the facing: for a block with more than one property that changes how it looks. |
| `IBlockLook.pipe(texture, core, arm)` | A cable: a core in the middle and an arm toward each side it connects to. |

The models a look takes are made by `IBlockModel`: `cubeAll(name)`, `column(name)`, `orientable(name)` and
`orientable(name, side, front, top)`, plus the records `BottomTop`, `SixFaces`, `ParticleOnly` (for a block
drawn by its block entity) and `Handmade` (a model you wrote by hand in your resources, used as it is).

An item's look is an `IItemLook`:

| Look | What it draws |
| --- | --- |
| `OF_BLOCK` | A block's item showing its block's model (what a block's item gets by default). |
| `FLAT` | A flat picture, the texture `textures/item/<id>.png`. |
| `HANDHELD` | The same picture, held as a tool is, slanted up in the hand as an axe or a sword. |
| `DRAWN_BY_ENTITY` | Drawn by the renderer of the block's block entity, for a body too large or too alive for a model. |
| `HANDMADE` | A model you wrote by hand; nothing is generated. |
| `HELD` | Drawn in three dimensions, as the game's spyglass and trident are: the flat picture `textures/item/<id>.png` in a slot and an item frame, and the model `models/held/<id>.json`, which you write beside the textures with its boxes and its display transforms, in the hand, on the ground and on the head (*added 2026-10-25*). |
| `CABLE` | A length of cable, for a cable's item. |
| `BUCKET` | The game's bucket with the fluid it holds drawn in it. |
| `IItemLook.parent("block/...")` | Another model, unchanged. |

## Creative tabs

*Added 2026-09-23.*

```java
public static final ContentTab TAB = CONTENT.tab("main", "My Mod", () -> MyContent.COPPER_BOX);
public static final ContentTab.Section MACHINES = TAB.section();
public static final ContentTab.Section PARTS = TAB.section();
```

A tab is made of sections shown one after another, in the order they were made. A block or item joins a
section where it is declared, so what is in the tab, and in what order, follows from the declarations. A
section can also show stacks that are declared elsewhere (`alsoShowing(output -> output.accept(stack))`), and
can sort its entries another way than the order they were declared in (`sortedBy(comparator)`). The tab's name
is written to the language file from the English you give it.

A section can also take an item declared elsewhere as one of its entries (`with(() -> OTHER.get())`), for a tab
that gathers what several places declare, and can show only the entries a test lets through
(`shownIf(item -> ...)`), asked each time the tab is filled.

### A picture on the tab's button

*Added 2026-10-28.*

A tab's button shows its icon item. A mod whose mark is no item of its own can show a 16-pixel picture there
instead:

```java
public static final ContentTab TAB = CONTENT.tab("main", "My Mod", "mymod:gui/tab_icon");
```

The picture is named as an item's texture is, `mymod:gui/tab_icon` being `assets/mymod/textures/gui/tab_icon.png`,
and drawn where the item would stand. A tab declared with an item can take one too, with `.picture(texture)`. As
every creative tab, it shows once it holds a thing.

### The J's Tech tab

*Added 2026-10-15.*

The series has one creative tab of what its mods share, **J's Tech**, in three sections: the materials, the fluids
and components of the shared catalogue, and the shared blocks and tools (the series' manual first). It shows only
what the installed mods use, and a mod declares what it uses once, from its constructor: a material in a form with
`MaterialUses` (see [Materials](#materials)), anything else of the shared content with `SharedUses`:

```java
public static final Supplier<Item> TIN_PLATE = MaterialUses.use(MODID, SeriesMaterials.TIN, Forms.PLATE);
SharedUses.declare(MODID, () -> CoreGuide.MANUAL);
```

What the J's Tech tab shows is left out of the other tabs of every mod that declared its uses, so nothing shows twice;
those tabs keep the mod's own content. Until a mod declares its uses, its tabs stay as they are.

### Item tags from the tabs

*Added 2026-10-12.*

```java
public static final ContentTab TAB = CONTENT.tab("main", "My Mod", () -> MyContent.COPPER_BOX).tagging("all");
public static final ContentTab.Section MACHINES = TAB.section().tagging("machines");
```

A tab or a section can name item tags of the mod, by their path in its namespace. When the mod's data is
generated, every item declared into the section is written into the section's tags and into its tab's, here
`#mymod:machines` and `#mymod:all`. A tag that groups things the way the tabs already group them is then never a
list kept by hand, and a datapack, a modpack's script or another mod can ask for "any of these" through it. The
stacks a section only shows besides (`alsoShowing`) are not declared items and stay out of its tags.

## Script bindings

*Added 2026-10-12.*

```java
public static final ScriptBinding SCRIPTS = CONTENT.scriptBinding("MyMod", MyModScripts.class);
```

A mod can hand the scripts of a modpack a class of its own under a name: with KubeJS installed, a script calls
the class's public static methods as `MyMod.method(...)`. It is for what a script cannot name by an item id, such
as an item made with data written on it. The class must name nothing of KubeJS, so it loads without it; J's Core's
plugin for KubeJS, which only KubeJS ever loads, hands KubeJS every binding the mods declared. The name is a plain
identifier, and two mods cannot declare the same one. J's Computers' `JsComputers` is the example
([its page](../../computers/docs/MODPACKS.md)).

## Items

*Added 2026-09-23.*

```java
public static final ItemEntry<Item> COPPER_WIRE = CONTENT.item("copper_wire", Item::new)
        .named("Copper Wire")
        .look(IItemLook.FLAT)
        .tab(PARTS)
        .register();
```

`named` is required. Besides `look` and `tab`, an item can carry a default value of a data component (a piece
of typed data an item stack holds, NeoForge's replacement for the old free-form item tags):
`.component(MyComponents.CHARGE, 0)`. A mod declares its own components on the same `ModContent`:

```java
public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
        CONTENT.component("charge", Codec.INT, ByteBufCodecs.VAR_INT);
```

The codec writes the value into saved worlds, the stream codec sends it to players.

### Items that hold things

*Added 2026-10-01.*

An item can hold modes, energy, a fluid, a gas or other items, each said in one line of its declaration. The
Core adds the data components, the capabilities (so chargers, pipes and machines reach what it holds) and the
lines on its tooltip. What an item does as a tool, how long it lasts, and keys that work it when it is not held
are on their own page ([Tools and items](TOOLS.md)).

```java
public static final ItemMode SCAN = ItemMode.of("scan", MyTexts.MODE_SCAN);
public static final ItemMode MARK = ItemMode.of("mark", MyTexts.MODE_MARK);

public static final ItemEntry<Item> SURVEYOR = CONTENT.item("surveyor", Item::new)
        .named("Surveyor")
        .look(IItemLook.FLAT)
        .modes(SCAN, MARK)                  // two or more; the first is where it starts
        .holdsEnergy(50_000, 500, 500)      // capacity, most in a tick, most out a tick (FE)
        .holdsFluid(4_000)                  // millibuckets; 1000 is one bucket
        .holdsGas(1_000, 200)               // a tank of 1000 mB at 200 times the open air's pressure
        .holdsItems(9)                      // slots, up to 256
        .register();
```

- **Modes** are the ways an item works, switched by a key the player binds in the game's controls ("Change
  Item Mode"; it has no key until bound, and the tooltip says so), or by the mouse wheel turned while sneaking
  with the item in the main hand; the key held a moment opens a ring of the modes to choose from
  ([Tools and items](TOOLS.md#what-the-player-sees)). Read the current one with
  `ItemStates.mode(stack)`, change it with `ItemStates.setMode(stack, mode)`. The mode is saved by its id, so an
  id never changes once released; an id a stack does not know falls back to the first mode.
- **FE** (Forge Energy) is the energy unit every NeoForge mod shares. An item that holds energy is reached by
  anything that charges or drains items.
- A **gas** is held in a tank of a volume at a pressure, holding their product of one gas at a time, counted at
  the pressure of the open air, and is reached by the Core's gas capability (`MatterCapabilities.GAS_ITEM`).
- An item that holds energy, a fluid, a gas or items never stacks: two of them would have to share what they hold.
- An item that holds items refuses other items that hold items, so nothing can be nested without end.

## Block entities

*Added 2026-09-27.*

A **block entity** is the part of a block that keeps state and runs code: a machine's inventory, its energy,
its progress. The game gives every block entity a way to save itself, a way to send itself to the players who
see it, and a way to share numbers with an open menu, and each of the three is normally written by hand. A
`SyncedBlockEntity` writes all three from fields you declare once, saying on each one where it goes:

```java
public class KilnBlockEntity extends SyncedBlockEntity {

    private final FieldItemHandler items;
    private final FieldEnergyStorage energy;
    private final IntField progress;
    private final BoolField lit;

    public KilnBlockEntity(final BlockPos pos, final BlockState state) {
        super(MyContent.KILN_BE.get(), pos, state);
        this.items = fields().items("Items", 2).save().exposed().dropsWhenBroken();
        this.energy = fields().energy("Energy", 10_000, 200, 0).save().toMenu().exposed();
        this.progress = fields().integer("Progress", 0).save().toMenu();
        this.lit = fields().flag("Lit", false).save().toClient();
        fields().mirror(KilnBlock.LIT, this.lit::get);
    }
}
```

Each field has a key (the name it is saved under) and is sent where its marks say:

| Mark | Where the field goes |
| --- | --- |
| `save()` | Into the world's save, and back when the world loads. |
| `toClient()` | To the players who can see the block, whenever it changes; only these fields are written on their side. |
| `toMenu()` | To an open menu, as numbers the screen draws (a progress arrow, an energy bar). |
| `exposed()` | Offered to pipes, cables and machines through NeoForge's capabilities, on every side. |

The kinds of field: `integer`, `flag`, `longInteger`, `derived` (a number worked out from other state, never
saved), `items(key, slots)`, `energy(key, capacity, maxIn, maxOut)`, `fluid(key, capacity)`, `value(key, codec,
initial)` and `nullable(key, codec)` for anything a codec can write, and `part(name, part)` for a piece that
writes itself. An inventory can also `slotLimit`, `accepts` only some items in some slots, lock itself while
something is true, and tell you when a slot changes; a tank can be `outputOnly`.

What follows from the declarations:

- Saving and loading are written for you. A key missing from an old save keeps its starting value.
- What changed in a tick is sent once, at the end of the tick, however many fields changed.
- The capabilities of every `exposed()` field are registered for every block entity type declared through
  `ModContent.blockEntity`, so no mod registers them one by one.
- `dropsWhenBroken()` spills an inventory when the block is broken; `whenBroken` and `whenNeighbourChanges` run
  code of yours at those moments, and `whenMoved` when the block moves with a moving structure, which is never a
  break ([Moving structures](MOVING_STRUCTURES.md#blocks-that-remember-other-blocks)).
- `mirror(property, value)` keeps a property of the block state equal to a value, so the look follows the state
  (a furnace's front lit while it burns) with no code of yours.
- `layout(saveLayout)` versions what the block entity saves, so a later version of your mod can read an older
  save ([State and saves](STATE_AND_SAVES.md#versions-of-what-is-saved)).

Declare every field in the constructor or a field initialiser. The first save, load, update or menu closes the
declarations, and declaring after that throws: a field that appeared later would be missing from what was
already saved and sent. Two fields with one key throw too, and the key `version` is kept for the Core.

Register the type on your `ModContent`, naming every block that makes it:

```java
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KilnBlockEntity>> KILN_BE =
        CONTENT.blockEntity("kiln", KilnBlockEntity::new, KILN);
```

## Devices: a block that runs its block entity and opens a menu

*Added 2026-09-27.*

Most machine blocks are the same block with a different block entity: it faces the way it was placed, ticks
its block entity on the server, and opens a menu when used. A `Device` says the three things and makes the
block:

```java
public static final BlockEntry<DeviceBlock> KILN = CONTENT.block("kiln",
                Device.of(() -> MyContent.KILN_BE.get())
                        .ticks(KilnBlockEntity::serverTick)
                        .opensMenu(KilnMenu::new)::block)
        .properties(p -> p.strength(3.5F).requiresCorrectToolForDrops())
        .named("Kiln")
        .look(IBlockLook::orientable)
        .item()
        .register();
```

`ticks` takes the game's ticker, a static method `(level, pos, state, blockEntity)` run every server tick (20 a
second). `opensMenu` takes how to make the menu from `(containerId, playerInventory, blockEntity)`; the device
also writes the block's position for the player's side, which reads it back with `MenuOpening.blockEntity`.

A comparator reads a device's block entity when it speaks redstone (`ISignalOutput`, [Redstone and
signals](SIGNALS.md)); a machine belongs to whoever placed it, and a working machine shows it
([Machines](MACHINES.md#what-every-machine-shares)).

## Menus

*Added 2026-09-27.*

A **menu** is the server's half of a screen: which slots it has, where items go when shift-clicked, which
buttons it answers and when it closes. A `CoreMenu` says each once:

```java
public class KilnMenu extends CoreMenu {

    public KilnMenu(final int containerId, final Inventory inventory, final KilnBlockEntity kiln) {
        super(MyMenus.KILN.get(), containerId, inventory, MenuValidity.blockEntity(kiln));
        final GuiLayout layout = KilnLayout.layout();
        final SlotGroup input = slots(slot(kiln.items(), 0, layout.slotAt("input")));
        final SlotGroup output = slots(outputSlot(kiln.items(), 1, layout.slotAt("output")));
        final PlayerSlots player = playerInventory(inventory, layout.playerInventoryAt());
        shiftClick(input, player.all());
        shiftClick(output, player.all());
        shiftClick(player.all(), input);
        data(kiln.fields().menuData());
        button(0, p -> kiln.reset());
    }

    public KilnMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, KilnBlockEntity.class));
    }
}
```

- The slot positions come from the screen's layout class (`KilnLayout`), the same one the screen draws its
  slot frames from and a test checks for overlaps, so the slots and their frames cannot drift apart
  ([Screens](SCREENS.md#layouts-you-can-test)).
- `outputSlot` takes nothing from the player: it only gives.
- A group with no `shiftClick` route moves nothing when shift-clicked.
- `data(...)` shares the `toMenu()` fields with the screen, in the order they were declared.
- `button(id, ...)` runs on the server when the screen sends that button.
- `value(() -> ...)` and `flag(() -> ...)` share a number or a yes/no the server works out.
- `MenuValidity` closes the menu when it should: `blockEntity(be)` when that very block entity is gone or the
  player walked away; `block(level, pos, BlockClass.class)` for any block of a family (a menu shared by several
  variants of a block must check the family, or it opens and closes in the same tick); `near(level, pos, blocks)`.

Declare the menu type with the rest of your content, and its screen on the client with `RegisterMenuScreensEvent`:

```java
public static final DeferredHolder<MenuType<?>, MenuType<KilnMenu>> KILN =
        CONTENT.menu("kiln", KilnMenu::new);
```

`CONTENT.menu` registers it under your mod's id when `CONTENT.register(modEventBus)` runs; the factory is the
constructor that reads the block's position from what the server sent.

## Materials

*Added 2026-06-15; rewritten 2026-10-15.*

The Core keeps one catalogue of what the world is made of, so every mod trades the same things: a mod that makes tin
dust and a mod that asks for it agree on what tin dust is, and neither depends on the other. The catalogue is a set of
registries, every entry known by an id (`namespace:path`) and open to any mod:

| Registry | What it holds | The Core's own |
| --- | --- | --- |
| `Materials` | Elements, alloys, minerals, compounds, liquids, gases, exotic matter | none; the series' are in `SeriesMaterials` |
| `Forms` | What a material is made into: an ingot, a dust, a plate, a block, an ore | 21 forms |
| `MaterialProperties` | The real data a material carries: never a game rule | symbol, atomic number, formula, colour, state at room temperature |
| `MineralFamilies` | The chemistry that decides how a mineral is processed | sulfide, oxide, carbonate, silicate, arsenide, native, halide, sulfate, phosphate, arsenate, vanadate |
| `HostRocks` | The rocks ores sit in | the game's twelve: stone, deepslate, granite, diorite, andesite, tuff, calcite, sandstone, netherrack, basalt, blackstone, end stone |

### A material

A material has a **kind** (`MaterialKind`: metal, crystal, gem, non-metal solid, mineral, liquid, gas, exotic), set by
its state at room temperature, and the kind says which forms it can take: a metal an ingot, a wire, a gear; a crystal a
boule and a wafer; a gem a gem. It can take other states besides (a molten metal is a liquid). It has a colour, which
its things are drawn in, and the values of its properties. It has **no tier**: when it becomes available is for each
mod's recipes to say. A material declared once, as a static final, from the mod's constructor:

```java
public static final TextKey MYTHRIL_NAME = TextKey.of("mymod.material.mythril", "Mythril");
public static final Material MYTHRIL = Materials.material(rl("mythril"), MaterialKind.METAL, MYTHRIL_NAME)
        .colour(0x8FB8D8).with(MaterialProperties.SYMBOL, "My").also(MatterState.LIQUID).declare();
```

For a form the game already has a thing for, a material says so (`provided(Forms.INGOT, () -> Items.IRON_INGOT)`,
`providedOre(HostRocks.DEEPSLATE, () -> Blocks.DEEPSLATE_IRON_ORE)`), and that thing stays the game's: mods that ask
for the game's iron ore by its id keep working.

### Using a material: only what some mod uses exists

A form of a material has a thing in the game only if an installed mod says it uses it. Mods declare their uses from
their constructors; the Core registers the union as the game registers its blocks and items, each thing once, under
the material's namespace (`jscore:tin_plate`):

```java
public static final Supplier<Item> TIN_PLATE = MaterialUses.use(MODID, SeriesMaterials.TIN, Forms.PLATE);
MaterialUses.ore(MODID, SeriesMaterials.TIN, HostRocks.STONE, HostRocks.DEEPSLATE);
```

`use` gives the thing's item, the game's own where the material has one. An ore is declared with the rocks its veins
cross: a block in each (`jscore:tin_ore` in stone, `jscore:deepslate_tin_ore`, as the game names its own), each
dropping the same thing, the material's raw form for a metal or a mineral, its gem, its crystal or its dust, and the
ore itself with Silk Touch. `MaterialUses.item`, `block` and `oreBlock` reach a thing anywhere, and `Materials.of(item)`
says what material and form an item is, the game's own included. A use declared after the game registered its things,
a form the material's kind cannot take, or an ore with no rock stops the game loading with a paragraph saying so.

A mod declares rocks of its own the same way, and lays any material's ores in them, its own or another mod's:

```java
public static final TextKey MARE_BASALT_ORE = TextKey.of("jsspace.host_rock.mare_basalt", "Mare Basalt %s Ore");
public static final HostRock MARE_BASALT = HostRocks.rock(rl("mare_basalt"), SpaceRocks.MARE_BASALT,
        MARE_BASALT_ORE).declare();
MaterialUses.ore(MODID, SeriesMinerals.ILMENITE, MARE_BASALT);   // jscore:mare_basalt_ilmenite_ore
```

An ore takes its hardness, a little over its rock's, and its sound from its rock, so an ore in a rock a mod declared is
made once every mod has registered its blocks, when the game registers its items. *Added 2026-10-29.*

Every thing is in its material's common tag and its form's (`c:plates/tin` in `c:plates`, `c:ores/tin` in `c:ores`,
an ore in `c:ores_in_ground/<rock>`), every block is mined with a pickaxe, and every block has its drops: all this
made as the server loads its data, in a data pack of the Core's own, which any data pack can override.

### Names

A thing is named from its form's name with its material's in it ("%s Plate" gives "Tin Plate", "Block of %s" "Block
of Tin"), and an ore from its rock's ("Deepslate %s Ore"), so a material and its things are translated once, and a
material renamed names all its things at once.

### Art made as the game loads

No texture of a material's thing ships in the Core: each is drawn as the player's game loads its resources, in the
material's colour. The forms the game has a picture for come from the game's own texture, recoloured texel by texel,
its light and shade kept (every ingot is the iron ingot in the material's colour, every dust the glowstone dust, every
rod the stick); the rest come from the Core's own templates. A material's colour gives a ramp: its shadows cooler and
deeper, its lights warmer and paler. An ore is its rock's texture with the specks of one of the game's ores laid on it
(`OreLook`: which ore's specks, a colour of the ore's own when it is not the metal's, and a second colour for the hues
of a mineral of two). A metallic or glassy mineral has a sheen (`withSheen`): its specks' darkest texels a little under
half its colour, their middle at the colour and their brightest rising to the sheen, which is what shows a black
mineral, as ilmenite, on a dark rock, where the ramp of its colour alone would keep it dark. A mineral that fills cracks
in its rock, as gypsum does, takes the veins of the Nether's quartz instead of specks (`OreLook.Pattern.QUARTZ`), taken
off the netherrack and laid on any rock. A form a mod adds draws from a grey template of the mod's own:
`Forms.form(...).art(rl("item/pellet"))`. The models and block states are made the same way, and a resource pack can
replace any of them.

### Values from a data pack, and tooltips

A data pack gives properties' values over a material's own, one file per material,
`data/<namespace>/material_values/<material>.json`, and can say when a line shows; the values reach every player.

```json
{ "values": { "mymod:molar_mass": 55.845 }, "shown": { "jscore:atomic_number": "always" } }
```

A material's things show its properties in their tooltips, the game's iron ingot included: each property's line
always, only with Shift held (with a line saying so), or never, as the property says unless the material or a data
pack says otherwise. A mod registers properties of its own with `MaterialProperties.property(id, codec, name)`. A
property declared `headline()` shows its value alone, set apart at the head of the tooltip, as an element's symbol
does: "Fe", in gold, rather than "Symbol: Fe" (*added 2026-10-24*).

### Packs made as the game loads

The art and the data above are packs the Core makes as the game loads, and any mod can make its own the same way: a
`RuntimePacks.declare(id, PackType, title, files)` whose files are made from what the installed mods declared, each
only when first read. Such a pack is always on, below every pack a player or a server adds.

A texture such a pack draws on the player's side, named by a hand-written model, is promised from the mod's common
code with `RuntimePacks.promise(textureId)`, so the check of the mod's generated data finds it there. `GameTexture`
reads one of the game's own textures to draw from and writes the PNG back, so nothing of the game's art ships: the
series' charred wood and ash are drawn this way from the game's oak and grey powder.

*Added 2026-10-17.*

### The series' catalogue

The series' own materials are in `SeriesMaterials`, which any mod uses as it uses its own:

- **The 118 known elements** (`Element`, `Elements.of(Element.URANIUM)`), as data: atomic number, symbol, standard
  atomic weight (`MaterialProperties.ATOMIC_WEIGHT`, `Elements.atomicWeight("Fe")`), the kind each is at room
  temperature (bromine and mercury liquids, silicon and germanium crystals, the rest metals, non-metal solids or
  gases), a colour near the pure element's, and its hazard classes. A metal can be molten, a gas liquefied, hydrogen
  and helium a plasma. Iron, copper and gold keep the game's ingots, nuggets, blocks, raw ores and ores.
- **The minerals** (`SeriesMinerals`): hematite, magnetite, siderite, pyrite, chalcopyrite, cuprite, malachite,
  electrum and cassiterite, each declared from its formula (below).
- **Isotopes** (`Isotopes`), materials of their own tied to their element by their mass number
  (`material.isotope()`), named after it ("Uranium-235") and written as chemists write them (²³⁵U): deuterium,
  tritium, helium-3, the reactor's fuels and targets, the sources of radiation, and the stable, neutron-rich isotope
  of each superheavy element from rutherfordium to oganesson, near the island of stability's 184 neutrons. A mod adds
  its own with `Materials.isotope(id, element, massNumber)`.
- **The elements beyond the known table** (`Superheavies`, 119 to 250), each known by its systematic name
  (`jscore:ununennium`, symbol Uue), which it shows until it is discovered.
- **The exotic forms of matter** (`ExoticMatter`), a kind of their own, each in the state it calls for: metallic
  hydrogen, neutronium, quark-gluon plasma, strange matter, dark matter, Bose-Einstein condensate, negative-mass
  matter, degenerate matter, time crystals, photonic matter and computronium.

### Minerals, their ores and their marks

A mineral is what comes out of the ground (chalcopyrite, not copper), a material of the kind `MINERAL` that knows what
it contains, its family and the rocks its veins cross:

```java
Materials.material(rl("chalcopyrite"), MaterialKind.MINERAL, CHALCOPYRITE_NAME).colour(0xC9A43A)
        .mineral(MineralFamilies.SULFIDE, HostRocks.DEEPSLATE, HostRocks.GRANITE)
        .contains(Elements.of(Element.COPPER), 0.6).contains(Elements.of(Element.SULFUR), 0.3).declare();
```

Its main element comes first, the one it is mined for, then its byproducts, each a share of its mass, the shares adding
up to the whole at most; the main element need not be most of it. Its ores are declared with `MaterialUses.ore(...)` as
any material's, named for the rock and the mineral ("Deepslate Chalcopyrite Ore", `c:ores/chalcopyrite`), and drop its
raw form ("Raw Chalcopyrite", `c:raw_materials/chalcopyrite`). An element mined as such has a generic "X ore" too, in
`c:ores/<element>` (`Elements.hasGenericOre(Element.TIN)`); the elements that come only as byproducts, the rare earths
and those whose ores are their salts have none.

The series declares its minerals from their formulas, and a mod can too, with `MineralBuilder`: its own elements come at
their shares by the atomic weights, a natural alloy says its parts, and a trace element is given at its real grade,
counted at a thousand times it and never past a tenth of the mineral (`TraceGrade`), the traces taking their room from
the rest:

```java
MineralBuilder.of(rl("chalcopyrite"), CHALCOPYRITE_NAME, MineralFamilies.SULFIDE, Element.COPPER).formula("CuFeS2")
        .byproduct(Element.SULFUR).trace(Element.GOLD, 0.000_01).rocks(HostRocks.DEEPSLATE, HostRocks.GRANITE)
        .declare();
```

A formula is read by `ChemicalFormula` as chemists write it (`Cu2CO3(OH)2`, `CuSO4·5H2O`), with its atoms, its molar mass
and each element's share of the mass, and the formula's tooltip line shows its counts small, CuFeS₂.

A material's hazards are data too: its hazard classes (`MaterialHazards.HAZARDS`, `HazardClass`), the nine GHS
pictograms they call for (`MaterialHazards.pictograms(material)`, or with the pressure it is held at, which adds the gas
cylinder for a gas), and what a cloud of a gas does (`MaterialHazards.gasHazards(material)`). See
[Hazards](HAZARDS.md#the-hazards-of-materials).

A material's things can bear marks in their tooltips (`MaterialMarks.declare(id, text, which)`): every mineral with
uranium or thorium in it is marked naturally radioactive, unless the world's setting `hazards.natural_radioactivity`
turns that off; the dose it gives comes with the Core's radiation.

### Liquids and the shared catalogue

A material's liquid exists only when a mod uses it, at the temperature the mod says, since the pipes that carry it
have to stand that (where mods differ, the hottest is kept):

```java
public static final Supplier<Fluid> MOLTEN_TIN = MaterialLiquids.use(MODID, SeriesMaterials.TIN, 505);
```

A liquid is itself (gasoline), molten for a solid (`jscore:molten_tin`, "Molten Tin") or liquefied for a gas
(`jscore:liquid_oxygen`). It pours into the world, fills a bucket, is in the common tag of its name (`c:molten_tin`)
and is drawn as the game's water in its material's colour.

The shared catalogue (`SharedCatalogue`) holds what the series' mods share that is no element: the common fuels
(gasoline, kerosene, diesel), carbon dioxide, ammonia and air, and the generic components (servomotor, electric motor, LED, sensor, oscillator,
antenna, magnetic head). A component exists only when a mod uses it, through the catalogue any mod adds to:

```java
public static final Supplier<Item> MY_LED = CatalogueItems.use(MODID, SharedCatalogue.LED);
```

Each mod that makes one gives its own recipe. A mod used without J's Industrial gives a simpler path; where J's
Industrial is installed, the mod's own server rule, off by default, can make the industrial chain the only path. The
simpler recipe carries the condition `{"type": "jscore:simple_path", "mod": "mymod"}`, and the mod declares its rule
with `SimplePath.rule(MODID, () -> MySettings.INDUSTRIAL_CHAIN_ONLY.get())`.

### Naming what is discovered

On a server, the player who first discovers an element beyond the known table gives it its name and its symbol, and
the element and all its things take them, for everyone and in every language. A mod says when an element is
discovered, at its first synthesis, with the player who made it, or the owner of the machine that did, who may be away:

```java
ElementNaming.discover(player, Superheavies.of(119));
ElementNaming.discover(server, machineOwner, Superheavies.of(119));
```

The discoverer is told how to name it, `/jstech name_element 119 <name> <symbol>`, at once or the next time they join;
the right to name it is theirs from the first. A name holds letters only, no more
than the world allows, repeats no other element's name and, where the world asks, ends in "-ium"; a symbol has one or
two letters and repeats none. Once given a name is not changed, but an operator can take one back,
`/jstech unname_element 119`, and its discoverer names it again. The names are kept with the world and sent to every
player (`MaterialNames`, which any mod can use for materials of its own that a world names). In a world whose
settings turn naming off, an element takes the series' own name when discovered (Genevium, Teslium... Aeternium, for
seventeen of them), or keeps its systematic one. The settings are in `jstech-balance.toml`, section `elements`.

## Ids that are saved

*Added 2026-09-13.*

Anything written into a world or sent to a player must mean the same thing next year. Java's own name and
position of an enum constant do not promise that: rename a constant or put a new one before it and every save
that stored it now means something else. So an enum that is saved or sent says its number or its name itself:

```java
public enum Kind implements IStableId {
    SMALL(0),
    LARGE(1);

    private static final StableIds<Kind> IDS = StableIds.of(Kind.class);

    private final int id;

    Kind(final int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return this.id;
    }

    public static Kind byId(final int id) {
        return IDS.byId(id, SMALL);
    }
}
```

`IStableId` is for numbers (0 to 127, for packets, menus and saves); `IStableName` is for names (lower case,
for data files and anything a player types). `StableIds.of` and `StableNames.of` refuse a number out of range or
a name twice as the class loads. `StableCodecs.byName` and `byId` make the codecs that write them.

The series' own tests refuse code that saves an enum by its position or Java name, and keep a list of every
number given out so that none can move unnoticed. A mod on the Core gets none of those checks for its own code,
but the rule is the same.

## What can go wrong

- **The block entity is never created.** The block is not among those named in `blockEntity(..., blocks)`. Every
  block, every variant included, that makes the block entity is named there.
- **"declared after first use".** A field was declared outside the constructor, after the block entity had
  already saved, loaded or been sent. Move it into the constructor.
- **A value resets when the world reloads.** Its field has no `save()`.
- **A value is right on the server but wrong in the screen or on the block.** Its field has no `toMenu()` (for
  the screen) or `toClient()` (for the block as players see it).
- **Pipes do not take from or give to the machine.** The field has no `exposed()`, or the block entity type was
  not registered through `ModContent.blockEntity`.
- **The menu opens and closes at once.** Its validity check failed on the first tick: usually a menu shared by
  several blocks that checks for one of them only.
- **An item that should stack does not.** It holds energy, a fluid or items, and such items never stack.
