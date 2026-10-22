# Overlays

What a mod shows on top of the game: elements of the HUD (the picture over the world: health, hotbar), holograms
floating in the world, and what Jade says about the block a player looks at.

## HUD elements

*Added 2026-10-05.*

Every mod that draws on the HUD wants a corner, and two mods in one corner draw over each other. The Core stacks
them: each element says its corner and its size, and is placed after the ones declared before it in that corner.

```java
// client setup
HudElements.declare(rl("oxygen"), HudStack.Corner.TOP_RIGHT, new IHudElement() {
    @Override
    public boolean shown(final Minecraft minecraft) {
        return minecraft.player != null && inThinAir(minecraft.player);   // asked every frame: keep it quick
    }

    @Override
    public HudStack.Size size(final Minecraft minecraft) {
        return new HudStack.Size(60, 10);
    }

    @Override
    public void render(final GuiGraphics graphics, final DeltaTracker delta, final int x, final int y) {
        Grounds.fill(graphics, x, y, x + 60, y + 10, MyPalettes.HUD.get().bar());
    }
});
```

They are drawn under the chat, 4 pixels from the screen's edge and 2 apart, and hide when the player hides the
HUD (F1). An element that is not shown takes no room.

An element whose corner the player chooses is declared with a supplier of its corner, asked every frame:
`HudElements.declare(rl("notifications"), Settings::corner, element)`. The Core's notifications are one
([Notifications](NOTIFICATIONS.md)).

### The HUD editor

*Added 2026-10-21.*

The player moves, resizes and hides every mod's HUD elements in the HUD editor, opened from the settings screen
("Where the HUD's elements are", under This player) or by its key, "Edit the HUD", which has none until given one.
Over the game, dimmed under a grid, each element has a dashed box with its mod's and its own name on a label: a box
dragged moves the element and settles against the others or on the grid, the chosen box (yellow) is resized by the
squares at its corners in quarter steps, from half its size to three times, and its eye hides it (grey). Reset all
puts every element back, Cancel leaves them as they were, and Done keeps what was done. The HUD follows the editor
while it is open.

What the player did is kept in their `jstech-hud.toml`, each element by its id: the corner it is held from, how far
from it, its size and whether it is hidden. An element held from a corner stays by that corner whatever the window's
size, and an element placed leaves its corner's stack; one never touched stays where its mod declared it.

Name your element for the editor under the translation key `hud.<namespace>.<path>` of its id. An element that is
empty while nothing happens (as notifications are) says how big its box is in the editor with `editSize`. A placed
element is scaled about its top left and drawn there, so one that keeps where it drew, for a click, keeps the right
place at its own size.

## Holograms

*Added 2026-10-05.*

Lines of text floating at a place in the world, for the players near it:

```java
Holograms.show(level, rl("kiln_status"), Vec3.atCenterOf(pos.above()),
        List.of(MyTexts.READY.text(), LookTexts.WORKING.with(42)), 0);
Holograms.hide(level, rl("kiln_status"), Vec3.atCenterOf(pos.above()));
```

- The last number is how many ticks it stays; 0 keeps it up as long as players are near.
- Showing the same id again replaces it. Up to 16 lines.
- It is sent to the players who have that place's chunk loaded, and drawn for those within 48 blocks.
- Holograms are not saved: a machine shows its hologram again when it loads.

## What Jade shows

*Added 2026-10-05.*

**Jade** is a mod that shows what a player is looking at. When it is installed, a block entity that implements
`IDescribed` adds lines to it:

```java
@Override
public void describe(final List<Text> lines, final boolean details) {
    lines.add(this.working ? LookTexts.WORKING.with(this.percent()) : LookTexts.IDLE.text());
}
```

A block of several pieces, such as a cable with parts on it, implements `IDescribedAt` instead (or as well): it is
asked with the player looking, and describes the piece their look picks
([Block spaces and microblocks](MULTIPART.md#the-wrench-and-the-look-at-tooltip)).

The lines are worked out on the server and sent to the player, so they can say what only the server knows.
`details` is true while the player holds the key for more. A block entity that has an owner (`IOwned`) shows
"Owner: ..." too. Jade's settings have a switch for these lines. Without Jade, nothing of this loads.

*Added 2026-10-22.* Jade's box is drawn in the player's style too: when a style is chosen in the Core's settings
([Settings](SETTINGS.md#the-settings-screen)), the box takes the style's ground, its edge and its text colours,
with square corners, and changes as soon as the player picks another style. It does so through a theme of Jade's
own, `jscore:player_style`, which is hidden from Jade's list of themes; with no style chosen, the theme the player
had in Jade comes back.

## What can go wrong

- **Two elements overlap.** One of them is drawn outside `HudElements` (straight on the game's HUD layer); declare
  it here instead.
- **A hologram does not appear.** The player is more than 48 blocks away, or does not have the chunk loaded, or the
  hologram's ticks ran out.
- **Jade shows nothing.** The block entity does not implement `IDescribed`, or Jade's switch for it is off.
