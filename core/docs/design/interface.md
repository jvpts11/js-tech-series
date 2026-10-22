# The interface

What players see and hear: screens and their components, themes, fonts, motion, what is drawn over the game and in the
world, keys, and sound. Every mod of the series draws through it, so they look and behave as one.

## What exists today

Everything this page designs is built, as of October 2026: screens declared in files with the layout engine, styles
and the tools that build them, the components, tooltips, fonts, motion, the HUD and its editor, notifications, the
camera, accessibility and the sound kit. How a mod uses it, with code: [Screens](../SCREENS.md),
[UI components](../UI_COMPONENTS.md), [Tooltips](../TOOLTIPS.md), [Fonts](../FONTS.md), [Motion](../MOTION.md),
[Overlays](../OVERLAYS.md), [Notifications](../NOTIFICATIONS.md), [The camera](../CAMERA.md) and
[Sound](../SOUND.md). What is not built yet is under *Still to build* at the end.

## The design

The toolkit gathers in one place, for any mod, what the best screen and settings libraries do.

### How a screen is made

- **Screens declared as data**: a screen is a JSON file in the game's own data format, read with codecs as data packs
  and resource packs are; the Java keeps only the logic. There is no style sheet: a screen says what it holds and where,
  and its look comes from the style the player picks (below), as J's Computers' screens already show. It stays
  explicit: no annotations doing things behind the scenes.
- **A flex and grid layout engine**: gaps, margins, smallest and largest sizes, growing; it fits the font, the GUI scale
  and the window. It stays testable: the overlap test runs on the layout worked out.
- **Java from end to end**: the layout, the drawing and the tools are Java inside the Core's jar, with nothing native,
  no browser and no other language, so anyone who knows Java can read and fix any of it.
- **Binding to data**: a component is bound to a synced value of the menu and follows it, with no hand-written packets.

### Styles

Everything on a screen is drawn through a **style**, and the player picks it.

- **No component has a look of its own**: the sprites it is cut from, its colours, the font, the spacing, the corners,
  the shadows, the motion and the sound of a click all come from the style.
- **The default style is the game's own**: the game's buttons, sliders, text fields, tabs and check boxes, the
  tooltip's purple frame, the menus' blurred ground and the separators, cut as the game cuts them and in the game's
  colours. It is drawn from the game's own sprites, so a resource pack that repaints the game's widgets repaints the
  Core's too. A machine's screen keeps the game's inventory below the machine's face ([Machines](machines.md)).
- **Styles that come with the Core**: J's Tech, the series' dark look; high contrast; styles for colour blindness; and
  the era styles (scanlines on a Vintage screen, glass on an Advanced one).
- **The player chooses**, under "This player" in the settings: one style for everything and, if they want, another
  for a given mod, changed at once with a preview. A mod only sets the default of its own screens, and the server
  imposes nothing. The game's style is the default everywhere, except on the screens of J's Computers' computing
  blocks, which wear their era's style by default.
- **Resource packs and add-ons** replace any style or add new ones: a style is a data file with its sprites, beside
  the palettes.
- **What a style does not change**: the drawing of a machine's face, which is its mod's art (a resource pack changes it
  as textures), though the face's common pieces (meters, lamps, displays, switches) follow the style; and the inside of
  a computer, whose look is the installed system's skin. That look belongs to the machine: everyone sees it the same on
  the monitor in the world, and it is personalised from that system's own settings (wallpaper, accent colour, light or
  dark theme), as on a real computer.
- **Tested in every style**: the overlap and contrast tests run in every installed style, since a style can change the
  spacing and the colours.

### For those who make screens

These are helpers inside the game, not a workshop beside it: each opens over the real screen, with its machine running
behind it, keeps the file as the source, and points out what is wrong. Their cards are the game's own grey windows.
They open only with the Core's developer mode on, and they are tools of the studio
([For those who make mods](developers.md)).

- **Live reload**: a screen's file is edited and the screen changes with no restart.
- **An inspector**, as a browser's "inspect element", on a developer's key: the outline of every component, layout
  boxes and components apart. Beside the screen, as the recipe book sits beside the inventory: the screen's tree, with
  the arrows that open and close each part; the problems found (a text with no translation in a language, a component
  the narrator has nothing to read for), marked on the screen and in the tree; and quick tests of the GUI scale, the
  language and the machine's state (working, idle, jammed). The component under the pointer gets the game's tooltip:
  its box, the values it is bound to, its style, when it redraws, and the file and line it is declared at.
- **An editor on the screen itself**: the open screen, its machine working, with a bar of a few buttons (undo, redo,
  snapping, add, save, done). The chosen component gets its handles and the guides that line it up with the others,
  and a card beside it with only what that component has. A component is added from a short list searched by typing,
  never a palette always open. Every change is written to the screen's file.
- **A new screen from nothing**: for the block looked at, from a starting point (a machine with the inventory below, a
  screen with no inventory, a tab of a screen, a HUD element, a copy of another screen), shown at once where the screen
  will open. Create writes the file in its place and leaves it open in the editor.

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
- **Shake and smoothing as one service**: every mod's shakes (a blast, a launch, a machine's rumble) add up in one
  place, each wandering smoothly and dying away, felt less the farther it comes from; and each mod's lean of the view
  (a cockpit banking, a recoil) eased there and back. The player's shake setting and less movement apply to all.
- **Riders lean with what they ride**: an aircraft's pilot, as other players see them, tilts with its bank.
- **A hook for a mod's third-person camera**: how far back it stands, over which shoulder and round which place,
  brought nearer by walls as the game's own is, and eased from one view to another.
- **An editor of camera paths** in the studio: flown and recorded into the path's file ([Developers](developers.md)).

The motion kit fades a whole window as one picture, drawn off the screen first, and has a spring among its curves,
which goes past its end and settles there.

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
- **Text size apart from the GUI scale**, the high-contrast style and the styles for colour blindness
  ([Styles](#styles); contrast is already tested), and a setting for **less motion**.

### In the world and on the HUD

- **World screens that can be touched**: a click on a monitor in the world and it answers. J's Computers' mirrored
  monitor becomes one use of the kit.
- **A HUD the player arranges**: an editor to move, resize and hide the HUD elements of every mod; the devices shown
  on the HUD while held (the Geiger counter, the dosimeter, the hazard map) go wherever the player puts them, at the
  size the player chooses.

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

## Still to build

- **A schema of screen files**: a JSON schema written from the screens' codecs by the data generation, so an editor
  completes a screen file as it is typed.
- **Components animated in one call**, by the motion kit's curves.
- **Fonts from TrueType and OpenType files**, turned into the game's font by the data generation, with their licence
  and credit required as for the bitmap ones.
- **Jade in the player's style**: what Jade shows drawn in the style the player picked.
- **Every mod's settings**: the settings screen shows the settings of mods that do not use the Core beside the Core's.