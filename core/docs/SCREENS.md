# Screens

How J's Core helps a mod draw: screens declared in files and laid out by the Core, styles the player picks for every
screen, layouts whose overlaps a test can catch, themes and skins that give every screen of an era or a system the
same look, text drawn one way everywhere, models the Core puts together, screens drawn into the world, and keys.
Three parts have pages of their own: [UI components](UI_COMPONENTS.md) (buttons, lists, fields), [Fonts](FONTS.md)
and [Motion](MOTION.md).

## Screens declared in files

*Added 2026-10-15.*

A screen is a JSON file in a resource pack, `assets/<mod>/jstech/screens/<name>.json`, that says what the screen
holds and where. The Java keeps only what the screen does. There is no style sheet: how the screen looks is the
style the player picked (below).

```json
{
  "width": 176,
  "root": {"type": "column", "gap": 4, "children": [
    {"type": "title"},
    {"type": "row", "gap": 6, "align": "center", "children": [
      {"type": "slot", "slot": 0},
      {"type": "progress", "bind": "progress", "max": 200, "grow": 1, "height": 8},
      {"type": "slot", "slot": 1}
    ]},
    {"type": "label", "bind": "energy", "format": "screen.mymod.press.energy"},
    {"type": "button", "text": "screen.mymod.press.start", "action": "start"},
    {"type": "inventory"}
  ]}
}
```

The screen of a menu is handed to the game from client setup; the last argument says what the file's names do:

```java
modBus.addListener(RegisterMenuScreensEvent.class, event -> DataScreens.register(event, PRESS_MENU.get(),
        rl("press"), menu -> screen -> screen.onAction("start", () -> START.send(true))));
```

A screen with no menu (an instrument's, a block's settings) opens with `DataScreens.open(rl("probe"), title,
logic)`. The file's namespace is the mod whose style the screen follows.

What a file holds:

- **Boxes that lay others out**: `column`, `row`, `grid` (its `columns` written `"auto"`, `"2fr"` or a width),
  `stack` (one over another), and `panel` and `well`, which draw the style's raised or sunken ground under what they
  hold. Each takes `gap`, `padding` (one number, two or four), `align`, `justify` and, for a row, `wrap`.
- **Components**: `title` (the screen's), `label` (a translation key in `text`, or a value of the menu in `bind`,
  written through the key in `format`), `button` (its `action`, or a `menu_button` the menu answers), `slot` (by its
  place among the menu's slots), `slots` (a run of them, `per_row` to a row), `inventory` (the player's, as the game
  lays it), `progress` (a value of the menu against its `max`), `image` (a sprite), `spacer`, `separator`,
  `checkbox` and `text_field`.
- **On every component**: `id` (what the code and the tests find it by), `width`, `height`, the smallest and largest
  sizes, `grow`, `shrink`, `margin`, `align_self`, `span` in a grid, a `tooltip` key, a `narration` key (the name the
  game's narrator calls a component by that has no words of its own, a switch or a slider), and `visible_if` and
  `enabled_if`, each a value's name.

A kind is named by its short name when it is the Core's (`"button"`), by its id when it is another mod's. A mod
declares its own with `ElementTypes.declare(id, propsCodec, holdsOthers)` and registers what draws it with
`ElementBuilders.register` from client setup. The menu's slots are moved under the frames the file placed, so a slot
and its frame never part; the screen is as tall as what it holds needs.

When the file is read again (a resource pack changed, or in a development run the file was saved), the open screen
is built again from it at once, with no restart. A file that cannot be read says what is wrong in the place of the
screen, and in the log.

## The layout engine

*Added 2026-10-15.*

`FlexLayout` lays a tree of `FlexBox`es out: rows and columns that share their room by `grow`, give it up by `shrink`
down to their smallest sizes, wrap when asked, and put what is left where `justify` says; grids whose columns are
fixed, sized by what they hold, or a share of the rest; stacks laid over each other. Every size is a whole pixel, and
a share is cut by largest remainder, so the parts always add up. It has no game types: the screens declared in files
are laid out with it, and a unit test lays out the same tree.

```java
final FlexBox root = FlexBox.column("root").padding(Insets.of(6)).gap(4)
        .add(FlexBox.leaf("title", text.of("Kiln")).role(Role.TEXT))
        .add(FlexBox.row("slots").gap(2)
                .add(FlexBox.leaf("in", IMeasure.fixed(18, 18)).role(Role.SOLID))
                .add(FlexBox.leaf("bar", IMeasure.fixed(24, 8)).grow(1).role(Role.SOLID))
                .add(FlexBox.leaf("out", IMeasure.fixed(18, 18)).role(Role.SOLID)));
final Placed placed = FlexLayout.layout(root, 176, FlexLayout.preferred(root, 176).height());
assertTrue(placed.guiLayout(176, placed.height()).isClean());
```

`placed.overflows()` names the boxes whose contents need more room than they got, even at their smallest.

## Values the menu shows

*Added 2026-10-15.*

A component of a file binds to a value of its menu by name, and no packet is written for it:

```java
progress = value("progress", () -> press.progress());                       // an int
ready = flag("ready", () -> press.isReady());                               // yes or no
energy = synced("energy", ByteBufCodecs.VAR_LONG, 0L, () -> press.energy()); // anything with a codec
```

`synced` carries a value of any kind its codec writes, a number past what the game's own menu data carries (a short)
included; all the synced values of every menu travel in one payload of the Core's, sent as the menu opens and then
only for the values that changed. `menu.bound("energy")` reads one by name, which is what the file's `bind` does.

## Styles

*Added 2026-10-15.*

Everything a screen declared in a file draws comes from a **style**: the sprites its pieces are cut from, its
colours by role, the font, the spacing, the finish (scanlines, bevels, glass) and the sound of a press. The player
picks it under **This player** in the Core's settings: one for every screen and, if they want, another for a given
mod's, changed at once with a preview. The server imposes nothing.

The styles that come with the Core are **Game** (the default: drawn from the game's own widgets, so a resource pack
that repaints them repaints the Core's too), **J's Tech**, **high contrast**, three for **colour blindness**, and the
**era styles**. A mod sets only the default of its own screens, `Styles.setModDefault(modid, style)`; J's Computers'
computing blocks wear their era's.

A style is a file of a resource pack, `assets/<namespace>/jstech/styles/<name>.json`; a pack replaces one by its name
and adds new ones:

```json
{
  "name": "mypack.style.copper",
  "parent": "jscore:game",
  "colours": {"accent": "#FFB87333", "title": "#FF3B2414"},
  "pieces": {"button": "mypack:widget/copper_button", "slot": {"kind": "flat", "fill": "slot_bg", "edge": "line"}},
  "metrics": {"gap": 5},
  "finish": {"double_bevel": true}
}
```

A piece is a sprite of the GUI atlas, a `slice` of a texture cut in nine, a `tile` laid side by side, `flat` colours
(each a role's key or `#AARRGGBB`), `menu` (the game's own ground of a menu: its panorama or the world, blurred), or
`none`. What a style does not set comes from its `parent`, or from a `palette` it names; the rest is drawn flat in its
colours. The overlap and contrast checks run in every style the game has.

**A menu**, a screen over the whole game rather than a window (the Core's settings screen is one), is drawn with the
style's `menu_ground`, `list_ground` and `separator_footer` pieces, and its text takes the menu's roles (*added
2026-10-15*): `menu_text`, `menu_dim`, `heading` (a group's title), `notice` (what waits for a restart),
`info` (what takes hold on a reload, a link) and `warning` (a rule broken). Each falls back to the window's role of the
same kind (`text`, `dim`, `accent`, `amber`, `accent2`, `red`), so a style made before them still reads.
`Styles.menuSkin(style)` is the skin a component draws through on a menu; `Styles.skin(style)` the one in a window.
Text typed in a field takes `field_text` in every style, through `ISkin.fieldText()`.

What a style does not change: the drawing of a machine's face, which is its mod's art, and the inside of a computer,
whose look is the installed system's skin.

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
- `CoreModels.multipart(block)`: a block that holds several parts its block entity places (the Core wires its own cable block itself; register yours, not that one).
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

## The keyboard, a controller and the narrator

*Added 2026-10-21.*

Everything on a screen declared in a file can be reached without the mouse, as the game's own screens are: Tab
gives the keyboard to the first component and moves it on in the order the file reads, the arrows move it to the
nearest one that way, and Enter or Space presses a button, ticks a box or turns a switch. A field keeps every key but
Tab and Escape while it has the keyboard, so typing an E does not close the screen; Escape first closes a list that
is open. A component the keyboard reaches is drawn as the pointer resting on it draws it.

The components are handed to the game's screen as its own widgets are (`KeyboardFocus.of(panel)`, each entry added
with `addWidget`), so the game moves the keyboard among them, a controller moves among them the way it moves among
the game's buttons (Controlify does), and the game's narrator reads each in the game's words, in the player's
language: "Press button", "Checkbox: Ready: OFF", "Speed: 40 slider", with how it is used after it. A component's
`narration()` says what the narrator reads; one with no words of its own takes a name from its file's `narration`
key or from `setSpoken`. A screen of your own built from the Core's components hands them over the same way.

## What can go wrong

- **A screen shows "No resource pack has the screen" or a line of what is wrong.** Its file is not at
  `assets/<mod>/jstech/screens/<name>.json`, or cannot be read; the line and the log say where.
- **A component shows its kind in brackets.** Nothing on this game draws that kind: the mod that declares it is
  missing on the client, or registers its builder late.
- **A button does nothing.** Its `action` was not handed over with `onAction`; the developer's inspector lists the
  names a file used that nobody handed over.
- **A bound value shows `null`.** The menu binds no value of that name, or binds it after the screen was built.
- **A style shows magenta.** It sets a colour role to nothing, and nothing it builds on sets it; the log names the
  roles.
- **A test passes but the screen overlaps in the game.** The screen draws from numbers of its own instead of its
  layout class; draw from the layout.
- **Text runs past a panel.** The font is wider than the layout thought: give the text fewer characters, or clip it
  with `Texts.clip`.
- **A slot and its frame are a pixel apart.** The menu and the screen read different positions; both read the
  layout.
- **A resource pack's colours do not reach the screen.** The screen reads a palette once into a field instead of
  each time it paints ([Text and colour](TEXT_AND_COLOUR.md)).
