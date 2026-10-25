# Tools and items

*Added 2026-10-21.*

Items that do more than one thing: tools with modes, tools that mine an area or a vein, tools that lay blocks out or
copy and paste a region, tools on energy, fuel or compressed air, tools that heat, charge and wait, tools with
modules, and items that answer a key wherever they are carried. Everything here is declared with the item, on the
same builder as what it holds ([Content](CONTENT.md#items-that-hold-things)).

## Declaring a tool

```java
public static final ItemMode SINGLE = ItemMode.of("single", MyTexts.SINGLE);
public static final ItemMode AREA = ItemMode.of("area", MyTexts.AREA);
public static final ItemMode VEIN = ItemMode.of("vein", MyTexts.VEIN);

public static final ItemEntry<PickaxeItem> DRILL = CONTENT.item("atomic_drill",
                properties -> new PickaxeItem(Tiers.DIAMOND, properties))
        .named("Atomic Drill")
        .modes(SINGLE, AREA, VEIN)
        .holdsEnergy(1_000_000, 10_000, 0)
        .tool(ToolSpec.builder()
                .mines(AREA, MiningShape.area(3, 3, 1))     // 3 across, 3 up, 1 deep
                .mines(VEIN, MiningShape.vein(64))          // up to 64 blocks of the vein
                .runsOn(ToolPower.energy(400))              // 400 FE each block it breaks
                .heats(new ToolHeat(100, 2, 10))            // overheats at 100, +2 a block, cools 10 a second
                .build())
        .register();
```

`ToolSpec` says, for each mode or for every mode (`mines(shape)` without a mode), what the tool mines besides the
block it breaks and what it builds; what it works on; how it heats; how long it waits after a use (`cooldown`, in
ticks, the wait the game draws over the icon); and how long it takes to charge (`charges`, in ticks). A mode named
in the spec must be one of the item's modes, and what it works on must be something the item holds; the game stops
as it loads when either is not, saying which.

The tool's mode is changed with the Core's mode key, or by turning the mouse wheel while sneaking with the tool in
the main hand: down for the next mode, up for the one before.

## What the player sees

- **The ring of modes**: holding the mode key a moment opens a ring of the item's modes over the darkened world, the
  first at the top, each with its name and, for a tool that mines, a picture of what it mines (one block, a grid of
  the area, a tunnel, a vein); the mode it is in has a green dot. The mouse points at a mode and letting go of the key
  chooses it; letting go in the middle, or Escape, keeps the one it is in. A tap of the key still passes to the next.
- **The name with its mode**: the game's name of the item above the hotbar, shown as it comes into the hand, says the
  mode it is in (`Atomic Drill: Area 3x3`) and shows again when the mode changes; under it, small, what it works on,
  how much of how much (`energy 27 500 / 50 000 FE`).
- **Under the crosshair**: a white bar of how charged a tool held in use is, and an orange bar of how hot a tool is,
  marked where its heat ends.
- **On the icon**: over the game's own durability bar, a bar for each thing the item holds (energy in red, a fuel in
  orange, another fluid in its colour, a gas in light blue, heat in orange), and the first letter of its mode in the
  top left corner.
- **In the world**: a tool that mines an area or a vein outlines every block it will take with a box of its own; a
  building tool shows the blocks it will place, translucent with a white edge, as many as the inventory holds. Claims
  are the server's to weigh, so a block on claimed land is outlined and then left as the tool works.

Their colours are the `jscore:tools/hud` palette, which a resource pack can change.

## Mining an area or a vein

When a player breaks a block with a tool that works and mines an area or a vein, the rest of it breaks too, each
block as the player would break it: its drops, the tool's wear, the experience and every protection's say come as
for a block broken by hand. The blocks taken are:

- **An area** of the face the player broke into, centred on the block broken (an even size has its extra row above
  or its extra column to the right, as the player sees the face), `depth` blocks into it. On the top or the bottom
  of a block, "up" is the way the player faces.
- **A vein**: the blocks touching the one broken at a face, an edge or a corner, of the same block or of the same ore
  (two blocks in one `c:ores/...` tag, as iron ore and deepslate iron ore), and those touching them, nearest first.

Of those, a block is taken only if the tool is made for it (it mines it faster than a hand, or the block needs it for
its drops), it is no harder than the block broken by more than the server's margin, it is no liquid, and the player
may break it there. Breaking stops when the tool wears out, leaves the hand or runs out of what it works on.
`AreaMining.around(level, player, tool, pos, face, shape)` gives the blocks a tool would take, for a preview.

| Setting | Default | What it tunes |
| --- | --- | --- |
| `tools.most_broken` | 64 | The most blocks broken in one blow, the one broken among them. |
| `tools.hardness_margin` | 2.0 | How much harder than the block broken a block may be and still be taken (stone 1.5, an ore 3, obsidian 50). |

## What a tool works on

```java
ToolPower.energy(400)                                      // FE of the energy it holds
ToolPower.fuel(List.of(rl("diesel")), 200_000)             // joules a use, burnt from the fluid it holds
ToolPower.gas(List.of(rl("compressed_air")), 50)           // millibuckets a use from its gas tank
```

A tool spends what it works on each block it breaks and each use. Energy is drawn from the item's own store at any
rate, whatever rate it gives to others. A fuel is one of the fuels engines burn ([Entities](ENTITIES.md#fuels)), by
id: a use burns the millibuckets of it that give the joules asked, rounded up. A gas comes from the item's tank,
declared with `holdsGas(volume, pressure)`, which holds the volume times the pressure, counted at the pressure of the
open air, and is reached by the Core's gas capability, so a compressed air line fills it.

**Out of what it works on, a tool does no more than a bare hand**: it breaks blocks no faster than a hand would, gets
no drops a hand would not, and mines no area. A player in creative spends nothing.

## Heat, waits and charging

A tool that heats adds `perUse` heat each block and each use, and gives `coolPerSecond` back to the air; at `most` it
overheats, and does no more than a bare hand until it has cooled down to nothing. The heat is reckoned from the
world's time when it is asked for, so a tool in a chest cools there with nothing counted each tick.

A mod's own item that does something on use asks `Tools.ready(player, stack)` first (it works, and is not waiting) and
calls `Tools.used(player, stack)` after, which spends a use, adds the heat and starts the wait. `ChargingItem` does
this for an item held in use to charge and let go to act, as a bow is drawn:

```java
public final class Railgun extends ChargingItem {

    public Railgun(final Properties properties) {
        super(properties);
    }

    @Override
    protected boolean release(final Level level, final LivingEntity user, final ItemStack stack,
                              final float charge) {
        if (charge < 0.5F) {
            return false;                      // let go too soon: no shot, nothing spent
        }
        fire(level, user, charge);
        return true;
    }
}
```

declared with `ToolSpec.builder().charges(40).cooldown(20).runsOn(ToolPower.energy(25_000))`. `Tools.charged(stack,
ticks)` says how charged it is after `ticks` held, from 0 to 1.

## Building tools

```java
.modes(LINE, PLANE, COPY, PASTE)
.tool(ToolSpec.builder()
        .builds(LINE, BuildMode.line(16)).builds(PLANE, BuildMode.plane(64))
        .builds(COPY, BuildMode.copy()).builds(PASTE, BuildMode.paste())
        .build())
```

Used on the face of a block, a building tool:

- **Line**: places a copy of the block in front of the face, and of each block like it in a line through it (across
  the face for a side, along where the player looks for a top or a bottom).
- **Plane**: the same for each block like it touching it over the plane of that face, and those touching them,
  nearest first. This is how the Construction Wand works.
- **Copy**: the first use marks a corner, the second copies the region between the two into the tool (only blocks,
  never what a block keeps inside, and never more than the server allows). Sneaking, it clears the copy.
- **Paste**: places the copy with its lowest corner in front of the face, reaching away from the player, to the right
  and up, turned as the player has turned since copying it, the lowest layer first.

Each block placed comes from the player's supplies, goes only where nothing stands, where it can stay and where the
player may place it, is placed as the player would place it (so protections and other mods see it), and spends a use
of the tool. The player's inventory is asked first, only its plain stacks (a named or changed stack is kept); then
every supply a mod added, which is how J's Computers gives the blocks of its network:

```java
BuildSupplies.add(new IBuildSupply() {
    @Override
    public long available(final ServerPlayer player, final Item item) { /* what the network holds */ }

    @Override
    public long take(final ServerPlayer player, final Item item, final long count, final boolean simulate) { /* ... */ }
});
```

| Setting | Default | What it tunes |
| --- | --- | --- |
| `tools.most_placed` | 64 | The most blocks a line or a plane places in one use. |
| `tools.most_copied` | 4096 | The most places a copy of a region holds; a larger region is not copied. |

## Modules in tools

A tool takes modules through the same kit as worn things ([Entities](ENTITIES.md#modules-in-worn-things)), declared
as a host whose modules work while it is held in the main hand:

```java
Modules.declareHost(MULTITOOL.getId(), ModuleHostDef.tool(List.of(new ModuleHostDef.Socket("tool", 4))));
Modules.declare(VEIN_MODULE.getId(), new ModuleDef("tool", 20, List.of(       // 20 FE each use
        new CoreModuleEffects.Mining(MiningShape.vein(32)))));
Modules.declare(SILK_MODULE.getId(), new ModuleDef("tool", 20, List.of(
        new CoreModuleEffects.Enchanted(Enchantments.SILK_TOUCH, 1))));
```

```json
{"sockets": [{"kind": "tool", "count": 4}], "held": true}
{"kind": "tool", "energy": 0, "effects": [{"type": "jscore:attribute", "attribute": "minecraft:player.block_interaction_range", "amount": 2}]}
```

- `mining` makes the tool mine an area or a vein (the largest a module gives); the tool's own mode wins when it has
  one for the mode it is in.
- `enchantment` makes it work as though it had an enchantment (silk touch, fortune, efficiency), by its key; turning
  the module off takes it away.
- `attribute` changes an attribute of the hand that holds it: reach, mining speed, attack.
- A tool's modules spend their energy each use (each block broken, each shot), not each second, counted in socket
  order while the tool's energy lasts; a module whose energy is not there does nothing.

## Keys for items not held

```java
public static final KeyAction MAGNET_KEY = ItemActions.declare(
        KeyAction.builder(rl("magnet"), MyTexts.MAGNET_KEY),
        stack -> stack.is(MAGNET.get()),
        (player, stack, shift) -> ItemActions.toggle(player, stack));
```

The press reaches the server, which finds the first stack the action takes, in the hands, then in what is worn
(armour, and the slots of Curios or Accessories), then in the inventory, and hands it to the action. The stack is the
carried one itself, so what the action writes into it stays. An item that turns on and off keeps it in the Core's
`jscore:item_on` component: `ItemActions.isOn(stack)`, `setOn`, and `toggle`, which says the new state on the action
bar; its tooltip says On or Off.

## Durability from a material

```java
.durability(1_561)                                                    // as the game counts a tool's uses
.durabilityFrom(MyMaterials.STEEL, MyProperties.HARDNESS, h -> (int) (h * 120))
```

The material catalogue keeps only real data, never game rules, so how long a tool lasts by its material is the mod's
own rule over one of the material's properties. The material's declared value is read once, as the item is
declared, since the game fixes how long an item lasts as it starts; a data pack's value for that property does not
change it. An item that wears out stacks alone.

## A tool that strikes

*Added 2026-10-25.*

```java
.look(IItemLook.HANDHELD)                                             // slanted up in the hand, as an axe
.strikes(7, 0.9)                                                      // 7 harm a blow, 0.9 blows a second
```

`strikes(damage, speed)` makes a tool a weapon in the main hand, with the two numbers its tooltip shows: the harm a
blow does and the blows a second, a bare hand's own counted in, as the game shows an iron axe's 9 and 0.9. It is
counted under the game's own names for a tool's attack, so the tooltip writes it in green as it does a sword's.
`IItemLook.HANDHELD` holds the tool slanted up in the hand, as the game holds an axe, from the same flat texture.

## What can go wrong

- **The ring of modes never opens.** The mode key has no key bound (it has none until the player gives it one), or
  the item in the main hand has no modes; the mouse wheel while sneaking still changes the mode.

- **The game stops as it loads, naming a tool.** Its spec mines or builds in a mode the item does not have, or it works
  on something the item does not hold: declare `holdsEnergy`, `holdsFluid` or `holdsGas` before `tool(...)`.
- **A tool mines only the block broken.** It is out of what it works on or overheated, it is in a mode with no area,
  or the blocks around are ones it is not made for, far harder than the one broken, or claimed by someone else.
- **A vein stops short.** It reached the tool's most or the server's `tools.most_broken`, or the ore around is another
  block in no common ore tag with the one broken.
- **A chainsaw will not run though full.** The fluid in it is not one of the fuels it burns, or holds less than a use.
- **A building tool places nothing.** The player holds none of the block (plain stacks only), the places in front are
  taken or claimed, or the tool is waiting since its last use.
- **A copy is refused.** The region is larger than `tools.most_copied`, or the two corners are in different worlds.
- **A tool's module does nothing.** The tool is not declared a held host, the module is off or in a socket of another
  kind, or the tool's energy does not cover it.
