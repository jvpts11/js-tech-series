# Content

How a mod declares what it adds to the game on J's Core: blocks, items, creative tabs, block entities, the
blocks that run them, their menus, items that hold things, and the ids saved in worlds. Read
[Getting started](GETTING_STARTED.md) first: it sets up the `ModContent` every declaration here hangs from.

The idea behind all of it is the same: **say a thing once.** A block is declared in one place, with its name,
look, drops, tab and tags, and everything the game needs about it (registration, the block state file, the
models, the English name, the loot table, the tags) is made from that declaration, either while the game loads
or when you run the data generation. Nothing is a second list kept in step by hand.

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
| `drops(Drops.SELF)` / `drops(Drops.NONE)` | What breaking it gives. The default is itself when it has an item, nothing otherwise. | no |
| `tab(section)` | The creative tab section it shows in. | no |
| `tag(tagKey)` | A block tag it belongs to; repeat for more. | no |
| `geo(look)` | Drawn by a GeckoLib model instead of a block model ([Screens](SCREENS.md#models)). | no |
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
| `DRAWN_BY_ENTITY` | Drawn by the renderer of the block's block entity, for a body too large or too alive for a model. |
| `HANDMADE` | A model you wrote by hand; nothing is generated. |
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

An item can hold modes, energy, a fluid or other items, each said in one line of its declaration. The Core
adds the data components, the capabilities (so chargers, pipes and machines reach what it holds) and the
lines on its tooltip.

```java
public static final ItemMode SCAN = ItemMode.of("scan", MyTexts.MODE_SCAN);
public static final ItemMode MARK = ItemMode.of("mark", MyTexts.MODE_MARK);

public static final ItemEntry<Item> SURVEYOR = CONTENT.item("surveyor", Item::new)
        .named("Surveyor")
        .look(IItemLook.FLAT)
        .modes(SCAN, MARK)                  // two or more; the first is where it starts
        .holdsEnergy(50_000, 500, 500)      // capacity, most in a tick, most out a tick (FE)
        .holdsFluid(4_000)                  // millibuckets; 1000 is one bucket
        .holdsItems(9)                      // slots, up to 256
        .register();
```

- **Modes** are the ways an item works, switched by a key the player binds in the game's controls ("Change
  Item Mode"; it has no key until bound, and the tooltip says so). Read the current one with
  `ItemStates.mode(stack)`, change it with `ItemStates.setMode(stack, mode)`. The mode is saved by its id, so an
  id never changes once released; an id a stack does not know falls back to the first mode.
- **FE** (Forge Energy) is the energy unit every NeoForge mod shares. An item that holds energy is reached by
  anything that charges or drains items.
- An item that holds energy, a fluid or items never stacks: two of them would have to share what they hold.
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
  code of yours at those moments.
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

Register the menu type with NeoForge as usual (`IMenuTypeExtension.create(KilnMenu::new)` in your mod's menu
register) and its screen on the client with `RegisterMenuScreensEvent`.

## Materials

*Added 2026-06-15.*

The Core keeps the catalogue of metals the mods process, so every mod trades the same items: a mod that makes
iron dust and a mod that uses it agree on what iron dust is. A material (`ModMaterial`: iron, copper, gold,
tin, bronze) comes in forms (`MaterialForm`: ingot, nugget, dust, plate, bolt, rod, gear). The game already has
some of them (the iron ingot); the Core registers the rest as they are needed, today iron dust, iron plate and
copper plate, under the `jscore` namespace. Reach one with `MaterialItems.get(ModMaterial.IRON,
MaterialForm.DUST)`.

Every form is also in the common tag other mods use, `c:<forms>/<material>` (`c:dusts/iron`, `c:plates/copper`,
`c:ingots/iron`), so a recipe that asks for the tag takes iron dust from any mod.

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
