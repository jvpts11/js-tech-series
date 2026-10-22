# Manuals

The guide framework: the books a player opens in the game to learn what each block and item is, what it is for, how to
get it, how to use it and what can go wrong. Each mod writes its entries once; the Core draws them, numbers them, builds
the contents and the index, searches them, and shows them in every manual that holds them.

## What exists today

Everything this page designs is built, as of October 2026: the manuals with their chapters, sections and entries,
written in Markdown, the series' three manuals, the numbering, the styles as data, scenes in 3D and in motion, finding
one's way (back and forward, bookmarks, search, the recipe viewers), the marks of what is new, live reload, the export
to the web and the immersion. The Core's own chapter and the series' are Markdown files. What is left is under
*Still to build* at the end. How a mod uses it, with code: [Manuals](../MANUALS.md).

## The design

The framework gathers what GuideME, Patchouli, Modonomicon and Create's Ponder do, in one place, for any mod.

### In 3D and in motion

- **3D scenes on a page**: a structure or a multiblock the reader turns, zooms and goes through layer by layer (or hides
  layers of), brought in from a saved structure.
- **Animated scenes that explain how something works**, as Ponder does: step by step, with captions, blocks appearing,
  items moving along a pipe, a machine at work; with a timeline to pause and go back; written as data. On the page the
  scene sits in its own box: the step and its caption above ("2 of 3: it is pressed flat"), the timeline below with a
  mark for each step and the playhead, and buttons to step back, pause and step on.
- **Animated pictures** on a page, from sprite sheets.
- **Show it in the world**: from a page, a structure is projected into the world as ghosts, with the Structure Projector
  ([Multiblocks](multiblocks.md)).

### Finding one's way

- **Back and forward**, as in a browser, and the player's own **bookmarks**: ribbons hanging from the binder's top
  edge over the pages they mark, and a button on the toolbar that lists them and marks the open page.
- **Search reaches recipes and uses**: "what uses copper plate" finds the entries of the machines that take it.
- **JEI and EMI**: a key over an item in their lists opens its entry; a click on a recipe in a manual opens it in JEI or
  EMI.
- **"New" and "reached" marks**: an entry shows that the player has just come to it, or not yet. **Nothing is ever
  hidden**: a manual shows everything, as JEI and EMI show every recipe ([Progression](progression.md)). In the
  contents a newly reached entry not read yet shows NEW and one not reached shows "not yet", in place of the dotted
  leader; a chapter's tab holding something new carries a gold point.

### For those who write

- **Live reload**: pages change in the game as they are edited, with no restart.
- **Export to the web**: the same manual comes out as a static website, with search, so a mod's manual is also its
  online documentation, made from the same data.
- **Written in Markdown**: a manual is a folder of Markdown files, and the code only loads it. Each entry is a file in
  `assets/<mod>/jstech/manuals/<manual>/<language>/`, falling back to `en_us` where a language has none, so a
  translator copies the folder as they would a language file. A short header at the top of each file names the entry,
  its chapter, its number, its order and its icon; the manual and its chapters are files too. The text is CommonMark
  with tables; links between entries go by their names; and the manual's own blocks (a recipe, a scene, a multiblock, a
  figure, a callout of a note, a warning or a plan, a table whose amounts are read from the code by name, a listing, a
  terminal session) are written in the file in a syntax of their own. The index, the glossary, the search and the
  export to the web read the same files.

### Immersion

- **A sheet torn out**: a page or a drawing becomes an item, to hang on a wall in a frame or hand to someone: the Plant
  Drawings on the factory's wall. Tearing makes a copy, from a button on the toolbar: the manual stays whole. Framed,
  the sheet fills the frame as a map does, and its tooltip says where it was torn from.
- **On a lectern**, a manual can be read by whoever passes. Read from a lectern, the manual shows a strip below it
  with where the lectern is and buttons to take the manual and to close, as the game's lectern has.
- **The player's own notes** in a page's margin, kept per player: a yellow slip written in pencil blue, seen only by
  the one who wrote it, from a pencil button on the toolbar.
- **A tip at the right time**: the first time a player picks up an item, a quiet notice offers its page; a setting turns
  it off.

## Still to build

- **J's Computers' manuals** (the Guide to Operations and its books) move to Markdown too, as J's Computers' last step
  in this cycle; then the way of declaring entries in code goes, since no manual is left that uses it.
