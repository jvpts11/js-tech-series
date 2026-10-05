# Screens

How J's Core helps a mod draw: layouts whose overlaps a test can catch, themes and skins that give every screen of
an era or a system the same look, text drawn one way everywhere, models the Core puts together, screens drawn
into the world, and keys. Three parts have pages of their own: [UI components](UI_COMPONENTS.md) (buttons, lists,
fields), [Fonts](FONTS.md) and [Motion](MOTION.md).

## Layouts you can test

*Added 2026-08-26.*

The commonest faults of a screen are a button over a label, text running off the edge, and a slot frame drawn a
pixel away from the slot. They are invisible to the compiler, and a test cannot see a screen. So the positions of
a screen live in a **layout class**: plain Java, with no game types, that a test can run.

```java
public final class KilnLayout {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;

    private KilnLayout() {
    }

    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.slot("input", 56, 35);
        l.slot("output", 116, 35);
        l.box("flame", 81, 38, 14, 14);
        l.text("title", 8, 6, 20, 1.0F);          // up to 20 characters at full size
        l.playerInventory(8, 84);
        return l;
    }
}
```

```java
@Test
void layout_isClean() {
    final GuiLayout layout = KilnLayout.layout();
    assertTrue(layout.isClean(), () -> layout.overlaps() + " " + layout.outOfBounds());
}
```

- `box` and `slot` are solid: two solids may touch but not overlap. `text` is only checked against the edges,
  since a caption over a panel is on purpose. A slot is 18 by 18; `playerInventory` lays the 3 by 9 grid and the
  hotbar.
- The **menu** places its slots from `layout.slotAt("input")` and the **screen** draws the frames from the same
  call ([Content](CONTENT.md#menus)), so a slot and its frame cannot drift apart.
- Test at the smallest size the screen can have, and with the longest text it can show.

## Themes and skins

*Added 2026-06-13.*

A screen of a block (a machine's window) paints through a **theme** of its era. `EraThemes.of(era)` gives it:
colours (panel, slots, lines, accent, text) from a palette a resource pack can change, and a style (scanlines on a
Vintage screen, glass bands on an Advanced one). Bind it while the screen paints, and draw through `JsTechTheme`:

```java
JsTechTheme.bind(EraThemes.of(HardwareEra.LEGACY));
try {
    JsTechTheme.window(g, leftPos, topPos, imageWidth, imageHeight);
    JsTechTheme.slot(g, leftPos + 55, topPos + 34);
    JsTechTheme.text(g, font, title.getString(), leftPos + 8, topPos + 6, JsTechTheme.text());
} finally {
    JsTechTheme.unbind();
}
```

Inside a computer's screen the look is the system's, not the era's: a **skin** (`ISkin`, in
`dev.jstech.core.api.client`) that J's Computers gives each desktop, so the same button looks like a Frames 95
button on one machine and a flat one on another. Components draw through it and never paint their own chrome.

## Drawing text

*Added 2026-09-05.*

```java
Grounds.fill(g, x, y, x + w, y + h, palette.panel());   // paints the ground, and says what colour it is
Draw.text(g, font, "Ready", x + 4, y + 4, palette.text());
```

All text goes through `Draw.text` (and `Texts.small`, `Texts.clip` for text that must fit a width). It draws
without a shadow: a shadow made small text harder to read. Painting the ground with `Grounds` first tells the Core
what the text sits on, which is how a shadow could come back suiting each ground. The series' own code is tested
never to call the game's `drawString` directly.

`ColorContrast.ratio(text, ground)` gives how readable a colour is on another (1 none, 21 black on white); keep
text at 4.5 or more.

## Models

*Added 2026-10-01.*

Models the Core puts together while the game loads, registered from your client setup:

- `CoreModels.connected(block, IJoinRule.sameBlock(), tiles)`: a block whose faces join their neighbours' into one
  picture (glass, panels), from five tiles: alone, across, upright, inner corner, whole.
- `CoreModels.multipart(block)`: a block that holds several parts its block entity places (the cable block).
- `GeoLook` with `.geo(look)` on a block: drawn by a GeckoLib model, `assets/<mod>/geo/<model>.geo.json` with its
  texture and animations; `LookGeoModel` is the model class to hand its renderer.

## Screens in the world

*Added 2026-10-04.*

A monitor in the world can show a picture drawn with the same calls as a screen: `LiveScreens.ask(key, painter,
width, height, scale, tube, distanceSquared)` gives a texture painted by `painter.paint(graphics, width, height,
partialTick)`, and `LiveScreens.draw(...)` puts it on a face. It is drawn again ten times a second up to 16 blocks
away, twice a second up to 32, and not at all beyond. On the server, `LiveFeed` sends the picture's description
only to the players near it and only when it changed.

## Keys

*Added 2026-10-01.*

A key is declared once for both sides: the player's game makes the key binding, and the server hears the press.

```java
public static final KeyAction SCAN = KeyActions.declare(KeyAction.builder(rl("scan"), MyTexts.SCAN_KEY)
        .key(GLFW.GLFW_KEY_G)                          // unbound unless given
        .onServer((player, shift) -> Scanner.scan(player)));
```

On the client, `KeyActionsClient.sendsWhen(SCAN, () -> holdsScanner())` sends the press only when it means
something, and `onPress(SCAN, ...)` runs client code instead.

## What can go wrong

- **A test passes but the screen overlaps in the game.** The screen draws from numbers of its own instead of its
  layout class; draw from the layout.
- **Text runs past a panel.** The font is wider than the layout thought: give the text fewer characters, or clip it
  with `Texts.clip`.
- **A slot and its frame are a pixel apart.** The menu and the screen read different positions; both read the
  layout.
- **A resource pack's colours do not reach the screen.** The screen reads a palette once into a field instead of
  each time it paints ([Text and colour](TEXT_AND_COLOUR.md)).
