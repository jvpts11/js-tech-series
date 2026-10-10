# The interface

What players see and hear: screens and their components, themes, fonts, motion, what is drawn over the game and in the
world, keys, and sound. Every mod of the series draws through it, so they look and behave as one.

## What exists today

### Screens

([Screens](../SCREENS.md), [UI components](../UI_COMPONENTS.md).)

- **Layouts that can be tested**: a screen's positions live in a layout with no game code, which the screen and its
  menu both read (so a slot and its frame never part), and a test checks that nothing overlaps or runs out of bounds.
- **Themes and skins**: a block's screen paints through the **theme of its era** (colours from a palette a resource
  pack can change, and a style: scanlines on a Vintage screen, glass on an Advanced one); inside a computer the look is
  the system's **skin**, so the same button is a Frames 95 button on one machine and a flat one on another.
- **About twenty components**: panels, scroll panels, dialogs, labels, buttons, checkboxes, text fields and areas, a
  command line, tabs, lists, column headers, grids of cells, context menus, breadcrumbs, scroll bars, progress bars,
  steppers; and the tested logic behind the fields and editors.
- **Text and models**: drawing text, block and item models on a screen.
- **Screens in the world**: a monitor shows a picture painted with the same calls as a screen, drawn ten times a second
  up to 16 blocks away and twice a second up to 32; the server sends what it shows only to the players near it, and
  only when it changed.
- **Keys** a mod declares, sent to the server as actions.

### Fonts and motion

- **Fonts from their free sources**, declared once and turned into what the game reads, with a **grid painter** for
  terminal-like views; the Core carries **Misc Fixed**, the X Window System's font, in the public domain, in three
  sizes ([Fonts](../FONTS.md)).
- **Motion**: curves, profiles a resource pack can change, and one clock every motion reads ([Motion](../MOTION.md)).

### Over the game

- **HUD elements** stacked by corner, so two mods never draw over each other; **holograms** in the world; what **Jade**
  shows for every block that describes itself ([Overlays](../OVERLAYS.md)).

### Sound

([Sound](../SOUND.md).)

- A sound is **declared once** (where it is heard from, whether it loops, its channel, its files, how far it carries,
  its subtitle), and its registration and language entry follow from that line.
- **Channels** with a volume each, a **Sound Mixer** screen, a mute for any sound of the game, ducking under alerts,
  visual signs for alerts.
- **Running sounds** that follow what makes them, rooms of many machines heard as one, occlusion by walls, and a budget
  of voices.
- **Cues**: code says what happened, and a data file a resource pack can replace picks the sound by its context.
- **Sounds made as they play**: a synthesiser, decoders, sound devices with their own limits, a voice allocator.
- **Recordings** that are not the mods' own: stored on the server, fetched by the players who hear them, with shares
  and a command to clear them.

## To build

The toolkit gathers what owo-ui, LDLib and YACL do, in one place, for any mod.

### How a screen is made

- **Screens declared as data**: a layout file with its styles, as owo-ui's and LDLib's XML; the Java keeps only the
  logic. It stays explicit: no annotations doing things behind the scenes.
- **A flex and grid layout engine**: gaps, margins, smallest and largest sizes, growing; it fits the font, the GUI scale
  and the window. It stays testable: the overlap test runs on the layout worked out.
- **Binding to data**: a component is bound to a synced value of the menu and follows it, with no hand-written packets.
- **Style sheets** per mod and per theme, which a resource pack can replace, beside the palettes.

### For those who make screens

- **Live reload**: a screen's file is edited and the screen changes with no restart.
- **An inspector**, as a browser's "inspect element": the outline of every component, its name, the layout's boxes and
  the slots' numbers, on a developer's key.
- **A visual editor in the game**, to draw screens by dragging components, as LDLib's: for development only.

### More components

Sliders, drop-down lists, option buttons and switches; a colour picker; trees; tables that sort and filter; line, bar
and pie charts (for the machines' statistics); a node graph editor; a 3D viewer of a block, an item or an entity; a map
view; tank gauges showing the fluid's own texture, and energy bars; ghost slots; rich text with items and images in
tooltips.

### Accessibility

- **Every control reached by keyboard**, in a focus order, and by game controllers (Controlify).
- **The game's narrator reads the components.**
- **Text size apart from the GUI scale**, high-contrast palettes and palettes for colour blindness (contrast is already
  tested), and a setting for **less motion**.

### In the world and on the HUD

- **World screens that can be touched**: a click on a monitor in the world and it answers. J's Computers' mirrored
  monitor becomes one use of the kit.
- **A HUD the player arranges**: an editor to move and hide the HUD elements of every mod.

### Cost and tests

- **Drawing less**: still parts are kept in textures, and only what changed is drawn again.
- **Visual tests**: screens are drawn off-screen in the client tests and compared with reference pictures, so a change
  nobody meant is caught.
