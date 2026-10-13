# Manuals

The guide framework: the books a player opens in the game to learn what each block and item is, what it is for, how to
get it, how to use it and what can go wrong. Each mod writes its entries once; the Core draws them, numbers them, builds
the contents and the index, searches them, and shows them in every manual that holds them.

## What exists today

([Manuals](../MANUALS.md).)

- **Manuals, chapters, sections and entries.** A chapter is everything one mod writes; an entry is one thing a player
  can look up, made of blocks: paragraphs, headings, figures, numbered tables (with amounts read from the code and
  written in the reader's way), recipes, steps, warnings, problems and their fixes, words explained, links, and blocks a
  mod draws itself.
- **The series' manuals**: the **Technical Reference** (every chapter of every mod installed, in a navy binder), J's
  Computers' **Guide to Operations** (a beige binder of the early computer manuals) and J's Industrial's **Plant
  Drawings** (a folder of blueprints, each machine drawn from three sides). A mod's entries show in its own manual and
  in the Technical Reference.
- **Numbered as technical manuals are**: chapter 3, section 3.2, entry 3.2.6, Figure 3-9, page 3-14; a set of drawings
  numbers its entries as drawings (JI-102).
- **Two ways of writing**: five parts in order (what it is, what it is for, how to get it, how to use it, what can go
  wrong), or running text that tells the idea before the part and ends with what to do if something goes wrong.
- **Links inside a sentence**, numbered by the manual being read; an **index** that lists every word explained, which
  makes it the glossary; a **plate** under the title with the items the entry is about; **pictures**, as images or as
  drawings made as the page is shown (J's Computers' real firmware screens); a block **seen from three sides** with
  numbered callouts; **plans** of blocks to place.
- **Styles are data**: colours, pages, columns, fonts, decorations (a binder's rings, a drawing's grid, frame and title
  block), covers, numbering; a resource pack can replace them.
- **The manual key** over an item opens its entry, in the item's own manual when it has one; a manual is handed to each
  player once, the first time they join; a manual reopens where it was left.

## To build

The framework gathers what GuideME, Patchouli, Modonomicon and Create's Ponder do, in one place, for any mod.

### In 3D and in motion

- **3D scenes on a page**: a structure or a multiblock the reader turns, zooms and goes through layer by layer (or hides
  layers of), brought in from a saved structure.
- **Animated scenes that explain how something works**, as Ponder does: step by step, with captions, blocks appearing,
  items moving along a pipe, a machine at work; with a timeline to pause and go back; written as data.
- **Animated pictures** on a page, from sprite sheets.
- **Show it in the world**: from a page, a structure is projected into the world as ghosts, with the Structure Projector
  ([Multiblocks](multiblocks.md)).

### Finding one's way

- **Back and forward**, as in a browser, and the player's own **bookmarks**.
- **Search reaches recipes and uses**: "what uses copper plate" finds the entries of the machines that take it.
- **JEI and EMI**: a key over an item in their lists opens its entry; a click on a recipe in a manual opens it in JEI or
  EMI.
- **"New" and "reached" marks**: an entry shows that the player has just come to it, or not yet. **Nothing is ever
  hidden**: a manual shows everything, as JEI and EMI show every recipe ([Progression](progression.md)).

### For those who write

- **Live reload**: pages change in the game as they are edited, with no restart.
- **Export to the web**: the same manual comes out as a static website, with search, so a mod's manual is also its
  online documentation, made from the same data.

### Immersion

- **A sheet torn out**: a page or a drawing becomes an item, to hang on a wall in a frame or hand to someone: the Plant
  Drawings on the factory's wall.
- **On a lectern**, a manual can be read by whoever passes.
- **The player's own notes** in a page's margin, kept per player.
- **A tip at the right time**: the first time a player picks up an item, a quiet notice offers its page; a setting turns
  it off.
