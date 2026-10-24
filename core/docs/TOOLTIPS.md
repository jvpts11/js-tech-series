# Tooltips

An item's tooltip is where a player first reads what it is. The Core keeps the lines of each mod together, opens the
long parts only when a key asks for them, and draws what text says badly: how full something is, a picture, a block
seen in 3D, what an item holds.

## Sections, by mod

A mod hands the Core what it says in tooltips once, from its client, as an `ITooltipSection`:

```java
ItemTooltips.add(MODID, (stack, tooltip) -> {
    if (stack.is(DRILL.get())) {
        tooltip.line(DrillTexts.AREA.text());
        tooltip.shift().line(DrillTexts.WHAT_IT_DOES.text());
        tooltip.ctrl().line(DrillTexts.USES.with(200));
    }
});
```

The section is asked for every item whose tooltip is shown and adds nothing to the items it has nothing to say of. A
line given as a `Text` is drawn in the game's grey; a `Component` keeps its own style.

Every tooltip then shows, after the game's own lines:

1. the lines of the item's own mod, then each other mod's, by the names the mods are shown by, a two-pixel room
   between one mod and the next;
2. in each mod's lines, what is shown always, then what Shift shows (what the item does) while Shift is held, and
   what Ctrl shows (its numbers) while Ctrl is held;
3. while neither key is held and something waits for one, a dark grey hint at the foot: "Hold Shift for more",
   "Hold Ctrl for the numbers", or both in one line;
4. last of all, the name of the mod the item comes from, in the game's blue and in italics, once. When another mod
   (a recipe viewer, a mod-name mod) already put the name there, the Core does not repeat it.

The lines a mod adds through the game's own `ItemTooltipEvent` still come, after the Core's. The Core's own lines (the
materials, what an item holds, hazards, physics, the manual key, a machine taken away whole) are a section like any
other.

`ItemTooltips.lines(stack, context, player, flags, shift, ctrl)` gives what the sections add with the keys held or
not, for a test or for a page that shows an item's tooltip.

## Pieces that are not text

Each of these is a `TooltipComponent`, added to a section with `piece`, or returned from an item's
`getTooltipImage` as the game takes one:

| Piece | What it draws |
| --- | --- |
| `TooltipBar.of(held, most, paint, label)` | A bar as wide as a short word with its words beside it, "Energy: 27.5 kFE of 50 kFE". The paint is `Paint.ENERGY` (red), `Paint.GAS` (grey), `new Paint.Fluid(stack)` (the fluid's own texture and tint, as the world shows it) or `new Paint.Colour(rgb)`. |
| `new TooltipPicture(texture, width, height)` | A whole texture in the middle of the tooltip, such as a photo a printer printed; a texture drawn while the game runs can be shown once it is registered under its name. |
| `new TooltipPreview(blocks, size)`, `TooltipPreview.block(state, size)` | Blocks seen from above at a slant, each block's edges drawn faint, turning slowly about their middle and sized to fit the square: a multiblock's terminal shows the structure it builds. It stands still for a player who asked for less movement, seen from the corner the game shows a block in a slot from: the east face on the left, the north face on the right. |
| `TooltipContents.of(stacks)` | What an item holds, in the slots of the game's bundle tooltip: each kind once with all it holds of it, the most first, six to a row and three rows at most; the last slot says how many kinds more. |

A piece added to a section stands, wherever only a tooltip's text is read (a search reads tooltips so), as a line of
its words: a bar's label, or nothing. On the screen it is drawn as itself.

## What every item gets

- An item declared to hold energy, a fluid, a gas or stacks (see [Content](CONTENT.md)) shows a bar for each, its
  slots with what they hold, the whole list with Shift, and with Ctrl how fast it takes and gives energy.
- A worn item shows a bar of how much it has left, in the colour of its bar on the icon, unless the game's advanced
  tooltips already say it.

## Settings

The player's file `jstech-tooltips.toml`:

| Key | Default | What it does |
| --- | --- | --- |
| `tooltips.mod_name` | `true` | Ends every item's tooltip with the name of its mod. |
| `tooltips.durability` | `true` | Shows the bar of how worn a worn item is. |

## Colours

The bars' frame, track, energy and gas, the shine on a bar's top row and the "+n" of the last slot are the palette
`jscore:gui/tooltip_pieces`, which a resource pack can change.
