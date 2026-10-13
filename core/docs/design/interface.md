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

### The camera

In the spirit of SecurityCraft's cameras, the zoom mods and cinematic modes:

- **Zoom with real magnification**: binoculars and J's Space's telescopes with true magnification and field of view,
  the telescope showing the planets in the sky ([The world](world.md)).
- **A chase camera** for vehicles and moving structures, which **follows the ship's turn** and rolls with it.
- **Free look in a cockpit**: the head turns while the vehicle keeps its course.
- **Cinematic paths**: a camera on keyframes for scenes (a rocket's launch seen from outside), which a mod starts and
  the player can skip.
- **Cameras in the world seen on a screen**: a camera block whose picture goes to a monitor, through the screens in the
  world; security cameras, and what a probe sees.
- **Seeing from somewhere else**: a periscope, a probe's camera, a robot's or a drone's.
- **Within the game's means**: views drawn into a texture have their own resolution and rate, and a limit per player.

### Notifications

- **Notifications on the Core's HUD**, instead of the game's toasts, whose way of being drawn changes with every
  version of the game.
- **A level** (information, warning, alert), with an icon, a colour from the palette, a sound from the sound kit and a
  duration.
- **Grouping**: ten alike warnings become one, with a count.
- **A history**, opened by a key, of recent notifications.
- **Actions**: a click opens the right place (the manual's page, the machine on the map).
- **The player chooses** where they appear, what to silence by kind and by mod, and a "do not disturb" mode.

### Tooltips

In the spirit of Iceberg:

- **Tooltips with pictures, previews of items and of blocks in 3D**, and bars of energy, fluid and durability.
- **Sections opened with Shift or Ctrl**, so a tooltip is never huge.
- **A preview of what is inside** items that hold things (a disk, a backpack, a cell).
- **Lines from different mods kept in a clear order**, with the mod's name at the end.
- **Everything translatable and painted from the palettes**, as the rest of the Core.

### Accessibility

- **Every control reached by keyboard**, in a focus order, and by game controllers (Controlify).
- **The game's narrator reads the components.**
- **Text size apart from the GUI scale**, high-contrast palettes and palettes for colour blindness (contrast is already
  tested), and a setting for **less motion**.

### In the world and on the HUD

- **World screens that can be touched**: a click on a monitor in the world and it answers. J's Computers' mirrored
  monitor becomes one use of the kit.
- **A HUD the player arranges**: an editor to move and hide the HUD elements of every mod.

### The sound kit

The sound kit gathers what Sound Physics Remastered, Dynamic Surroundings, AmbientSounds and Presence Footsteps do, in
one place, for any mod.

- **Reverberation by the space**, worked out from the surroundings, their size and their materials: caves echo, the open
  air is dry; sealed rooms give each room's exact volume ([Sealed rooms](sealed-rooms.md)).
- **Absorption by material**: wool muffles, metal carries; **the air takes away the high notes with distance**, so a far
  sound arrives muffled.
- **The medium counts**: **in a vacuum there is no sound**, only what is touched, and inside a suit the breathing and
  the radio; under water everything is muffled; in thin air, as on Mars, sounds are weaker.
- **The speed of sound**: the flash of a far explosion is seen first, and the boom arrives after.
- **The Doppler effect** on what moves: vehicles, rockets, shots.
- **Big sounds heard far away**: the roar of a launch kilometres off, with a low rumble.
- **Sound by material**: footsteps and impacts by the material of each block and entity (a chest creaks, stone is
  rough), as data a resource pack can change.
- **Ambience**: soundscapes by biome, weather, time, dimension, indoors or out, depth, and by what is near (a forest,
  water, **the hum of a factory**).
- **Music that follows the moment**: layered tracks that come and go with the context (exploring space, stepping onto a
  new world), in sets per mod, under the game's music volume.
- **Sound through devices**: a sound played through another place, such as radios, intercoms and a helmet's radio.
- **Simple Voice Chat follows the same physics**, through a guarded adapter: voices are muffled by walls, and **in a
  vacuum players hear each other only by radio**.
- **Ringing ears**: after a close explosion everything is muffled and rings for a few seconds; ear protection prevents
  it; a setting turns it off.
- **Visual signs with a direction** for the sounds that matter, besides the alerts.
- **Worked out in the background** and cached by position, so it never weighs on the game.
- **A debugging overlay**: sound sources, occlusion rays and the reverberation of each place.

### Cost and tests

- **Drawing less**: still parts are kept in textures, and only what changed is drawn again.
- **Visual tests**: screens are drawn off-screen in the client tests and compared with reference pictures, so a change
  nobody meant is caught.
