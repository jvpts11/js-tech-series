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

The lines are worked out on the server and sent to the player, so they can say what only the server knows.
`details` is true while the player holds the key for more. A block entity that has an owner (`IOwned`) shows
"Owner: ..." too. Jade's settings have a switch for these lines. Without Jade, nothing of this loads.

## What can go wrong

- **Two elements overlap.** One of them is drawn outside `HudElements` (straight on the game's HUD layer); declare
  it here instead.
- **A hologram does not appear.** The player is more than 48 blocks away, or does not have the chunk loaded, or the
  hologram's ticks ran out.
- **Jade shows nothing.** The block entity does not implement `IDescribed`, or Jade's switch for it is off.
