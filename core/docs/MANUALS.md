# Manuals

How a mod on J's Core writes its manual: the book a player opens in the game to learn what each block and item is,
what it is for, how to get it, how to use it, and what can go wrong. The Core draws the book, numbers it, builds its
contents and its index, searches it, and opens it at an item's page when the player holds the manual key over the
item. A mod writes what the pages say.

## The words

- A **manual** is a book with a title, a style it is drawn in, and the chapters it holds.
- A **chapter** is everything one mod writes; a **section** groups a chapter's entries under a title ("Machines").
- An **entry** is one thing a player can look up, and starts on a page of its own. It is made of **blocks**:
  paragraphs, headings, figures, tables, recipes, numbered steps, warnings, problems and their fixes, words explained,
  links to other entries, and special blocks a mod draws itself.
- A **style** is only data: the colours, one page or two side by side, the size of a page and its columns, the
  fonts, what decorates the pages (a binder's rings, a drawing's grid and frame), the cover, how pages are numbered.

A mod's entries are written once and show in every manual that holds its chapter: its own manual, and the series'
manual that holds every chapter. Everything is numbered as technical manuals are: chapter 3, section 3.2, entry 3.2.6,
Figure 3-9, page 3-14. A set of drawings numbers its entries as drawings instead: JI-102 is the second drawing of the
second section, on as many sheets as it takes.

## The series' manuals

*Added 2026-10-05.*

| Manual | Item | What it holds | How it looks |
| --- | --- | --- | --- |
| Technical Reference | `jscore:technical_reference`, tab J's Core | Every chapter of every mod installed: the series, the Core, J's Computers, J's Industrial. | The navy binder, a tab for each chapter. Handed to each player once, the first time they join a world. |
| Guide to Operations | `jsc:guide_to_operations`, tab J's Computers | J's Computers' chapter. | The beige binder of the early computer manuals, with a cyan band. |
| Plant Drawings | `jsindustrial:plant_drawings`, tab J's Industrial | J's Industrial's chapter. | A slate folder of blueprints: a drawing list, then each machine drawn from three sides on its sheets. |

A chapter opens on two facing pages: its number, its title and what it is about on the left, its sections and the
pages they start on on the right. So it always opens on a left page, a page is left blank before it when it would not.

## Writing a chapter

*Added 2026-10-05.*

```java
public final class MyGuide {

    private static final ModGuide GUIDE = MyContent.CONTENT.guide();

    static {
        GUIDE.chapter().titled("My Mod").order(40).tab("mymod:guide/tab")
                .about("Kilns and ovens that fire clay with energy.").register();
    }

    public static final ModGuide.SectionRef MACHINES = GUIDE.section("machines").titled("Machines")
            .icon(() -> MyContent.KILN).register();

    static {
        GUIDE.page("kiln", MACHINES).titled("Kiln").icon(() -> MyContent.KILN).covers(() -> MyContent.KILN)
                .whatItIs("A machine that fires clay into bricks with energy.")
                .whatItIsFor("Making bricks faster than a furnace, with no fuel.")
                .figure(() -> MyContent.KILN, "The kiln, from the front")
                .table("Its numbers")
                .amount("Uses", KilnBlockEntity.FE_PER_TICK, "FE/t")
                .amount("Holds", KilnBlockEntity.CAPACITY, "FE")
                .property("Fits", "Anywhere a block fits")
                .howToGetIt("From the creative menu, My Mod tab.")
                .howToUseIt("Place it.", "Give it energy.", "Put clay in its left slot.")
                .recipes("mymod:firing")
                .whatCanGoWrong("It stops halfway.", "It has run out of energy. Give it a generator.")
                .define("FE", "Forge Energy, the energy every NeoForge mod shares.")
                .seeAlso("mymod:generator")
                .register();
    }

    private MyGuide() {
    }

    public static void declare() {
        // Loading the class declares it.
    }
}
```

Call `MyGuide.declare()` from your mod's constructor, before your content registers. The data generation then
writes the files a manual is read from, under `assets/mymod/guide/`, and every sentence to your English language
file, under keys made from the entry and the part (`mymod.guide.kiln.what`). Translate those keys like any other.

A chapter's `about` is the paragraph on its opening page. The series' chapters stand in this order: the series 0,
the Core 10, J's Computers 20, J's Industrial 30; an addon picks an order after them. Sections and entries share one
set of ids, so a section and an entry cannot both be `mymod:kilns`: the declaration stops and says so.

### The five parts, or running text

An entry is written in one of two forms. The Core's chapter and J's Industrial's drawings follow the same five
parts, in this order, each with its own method that puts the part's heading before its words:

1. `whatItIs`: what the thing is, from zero.
2. `whatItIsFor`: why a player would want it.
3. `howToGetIt`: where it comes from.
4. `howToUseIt`: numbered steps.
5. `whatCanGoWrong`: each problem as a player sees it, then its fix.

A player who knows the thing skips to the table or to what can go wrong; one who does not reads it from the top.

*Added 2026-10-06.* J's Computers' chapter is a guide instead: it explains the idea before the part, in
`paragraph`s and `subheading`s, the way a person tells it, and ends an entry with
`ifSomethingGoesWrong(problem, fix, ...)`, the same problems and fixes under "If something goes wrong". Pick the
form that suits your chapter and keep to it in every entry.

Explain a word where an entry first uses it, with `define`: the index lists every such word with its page, which
makes the index the manual's glossary.

### Links inside a sentence

*Added 2026-10-06.*

Any sentence of an entry (a paragraph, a step, a note, a warning, a fix, a definition) can lead to another page in
its own words. Write the words in square brackets and the target after them in round ones:

```java
.paragraph("Every disk is counted in items of its era; [the eras](mymod:eras) explain why.")
.paragraph("Give it energy first ([](mymod:generator)).")
```

The first reads "the eras (2.1.4)", the second "(2.3.1)": the words and the number, or the number alone, drawn as a
link the reader clicks. The number is the one the target has in the manual being read, so the same sentence is
right in your own manual and in one holding every chapter; a target that manual does not hold reads as its words
alone. A target is an entry or a section (`namespace:path`) or a chapter (its namespace). A translation keeps the
brackets and the target and translates the words. Read as text, in a help program, a link is written as its words
and number, and the entry lists every page it led to under "See also" at its end.

### The plate under the title

*Added 2026-10-06.*

On a binder's page the items an entry talks about stand on a plate under its title, at their own size: the ones it
is the page of, or the ones `shows(item...)` names when they are others, such as every part of a first computer on
the page that builds one. A row holds what fits the column (eight on the Core's binder); a family longer than that
turns over to its next row every two seconds. The pointer on an item shows its tooltip, and a click on one whose
page is another entry goes there. A set of drawings has no plate, since its views show the machine. Read as text, the
plate is a line, "Items: ...", naming the first eight and how many more.

### Pictures

*Added 2026-10-06.*

A picture is numbered with the figures ("Figure 3-2.") and captioned like them, and comes two ways:

- `picture(texture, width, height, caption)`: an image your mod ships, a texture `namespace:path` under
  `textures/`, drawn at that size (a width of 0 takes the column's).
- `drawing(kind, height, json, caption)`: a drawing a renderer of yours makes as the page is shown, registered like a
  special block's (below). The picture then never falls behind what it shows and reads in the reader's language:
  J's Computers draws its firmware's real screens this way, `drawing("jsc:firmware", 97, "{\"look\":\"uefi\"}", ...)`.

### The other blocks

| Method | What it adds |
| --- | --- |
| `paragraph(english)`, `subheading(english)` | Running text, and a heading of the entry's own. |
| `figure(item, caption)` | The item drawn large in a frame, numbered "Figure 3-9." |
| `table(caption)`, then `amount(label, value, unit)`, `property(label, words)`, `fixed(label, data)` | A numbered table. An amount is read from your code and written in the reader's way ("12,000 FE"); words are translated; fixed values (a model number) are not. |
| `recipes(type)`, `recipes(type, making)` | Every recipe of a type the world holds, or those making one item, with the time and energy over the arrow. |
| `steps(english...)` | Numbered steps of the entry's own. |
| `warning(english)` | A warning in a box. |
| `seeAlso(entries...)` | "See 3.2.2 and 3.4.3", each number a link. |
| `note(english)` | A line set apart in the style's accent: what to read next, a hint. |
| `views(block)`, then `callout(number, view, u, v, english)` | The block from above, the front and the side, with the width of one block under the front. Each callout is a numbered balloon pointing at a place of one face (`u`, `v` in the face's sixteen pixels), and a line of the legend under the views. |
| `plan(caption)`, then `planPart(block, english)`, `planOptional(english)` | Blocks seen from above side by side as they are to be placed, each named under it; an optional place is outlined in dots. |
| `nextColumn()`, `nextPage()` | On a page of two columns, what follows starts in the next column, or on the next page (the next sheet of a drawing). A page of one column runs straight on. |
| `custom(kind, height, json)` | A special block your mod draws (below). |
| `picture(texture, width, height, caption)`, `drawing(kind, height, json, caption)` | A numbered picture: an image, or a drawing your mod's renderer makes (above). |
| `shows(item...)` | The items on the plate under the title, when they are others than those the entry is the page of. |
| `ifSomethingGoesWrong(problem, fix, ...)` | The end of an entry in running text: each problem, then its fix. |

`covers(item)` says which items this entry is the page of: holding the manual key over one of them opens here.
`coversAll(family)` does the same for a whole family, read when the files are written, so a part added to the family
later is the page of the entry with nothing more to write:

```java
.coversAll(() -> MyContent.CONTENT.declaredItems().stream().map(DeferredItem::get)
        .filter(item -> item instanceof KilnPartItem).map(ItemLike.class::cast).toList())
```

A block is drawn in its views and plans from the faces of its model: the top, the side facing north, and the side
facing east. A style that traces blocks draws those faces as line work, an outline and every edge where one shade
meets another; any other draws them as they look.

## Writing a manual

*Added 2026-10-05.*

```java
GUIDE.manual("handbook").titled("My Mod Handbook")
        .cover("My Mod", "Reference Library")
        .edition("First Edition")
        .partNumber("MM-0001")
        .style(CoreGuide.BINDER)
        .chapters("mymod")                      // or "*" for every chapter of every mod
        .about("This handbook holds everything My Mod adds.")
        .icon("mymod:gui/guide/cover_mark")      // a 32 by 32 texture printed on the cover
        .priority(5)
        .register();
```

The cover prints the first of its `cover` lines bold and the others small, the title large, and the edition under
it; an edition may hold `%s`, which is the number of sheets the manual has ("Set A - Sheets 1 to %s"). The part
number stands at the foot of a binder's cover as "Part No. MM-0001". The Technical Reference, which holds every
chapter, has priority 100; the series' own manuals 50.

Give the player an item that opens it: a `ManualItem`, declared like any item,
`CONTENT.item("handbook", props -> new ManualItem(props, "mymod:handbook"))`. To hand it to each player once, the
first time they join a world, call `GuideGifts.giveOnFirstJoin("mymod:handbook", MyContent.HANDBOOK)` while the game
loads: it goes into the first free place above the hotbar, and the world keeps who was given it.

When several manuals hold an item's entry, the manual key opens one that names the item's chapter before one that
holds every chapter, so a mod's item opens in the mod's own manual when it has one, and in the Technical Reference
when it has not. Among manuals alike, it opens the one with the highest `priority`.

## Styles

*Added 2026-10-05.*

The Core brings one style, `jscore:binder`: two cream pages side by side in navy vinyl covers with three rings
through them, the chapter tabs at the right edge, tables in the terminal font, pages numbered by chapter. A style is a
file, `assets/<namespace>/guide/styles/<path>.json`, so a resource pack can replace it and a mod can bring its own.
Two helpers make the usual ones in code:

- `CoreGuide.binder(palette, band)`: a binder in your palette. With `band`, its words stand on the cover itself and a
  stripe of the band's colour crosses its foot (J's Computers' beige Guide to Operations); without, they stand on a
  pasted label (the navy Technical Reference).
- `CoreGuide.drawings(palette, prefix)`: a folder of drawings. One wide sheet at a time in two columns, the grid, the
  frame with its zones numbered along the top and lettered down the side, the title block in the corner (the set, the
  drawing's title, its number, which sheet of how many, its revision), headings in capitals, notes lettered small,
  blocks traced as line work, and each entry a drawing numbered after `prefix` (J's Industrial's Plant Drawings).

```json
{
  "palette": "jsindustrial:guide/drawings",
  "pages": { "spread": false, "width": 340, "height": 201, "margin": 15, "columns": 2 },
  "decor": { "grid": true, "frame": true, "title_block": true, "upper_headings": true, "traced": true,
             "plain_numbers": true, "small_text": true },
  "cover": { "kind": "folder", "label": true, "band": true },
  "table_font": "jscore:fixed_6x10",
  "folios": "drawing",
  "drawing_prefix": "JI"
}
```

- `palette` names a declared palette ([Text and colour](TEXT_AND_COLOUR.md)) of the `GuidePalettes.Binder` kind,
  whose roles colour the book: `cover`, `coverEdge`, `paper`, `gutter`, `ink`, `faint`, `heading`, `link`, `rule`,
  `shade`, `highlight`, `ring`, `label`, `labelInk`, `tab`, `tabInk`, `warning`, `accent` (notes, what can go wrong,
  a link pointed at on a drawing), `number` (the numbers of steps and balloons), `band`, `grid`, `spine` and
  `coverLine` (the cover's first line). `colours` sets any of them outright, so a style needs no code at all.
- `pages`: two at a time or one, their size, the margin, and how many columns the text runs in.
- `decor`: a binder's `rings`; a drawing's `grid`, `frame` and `title_block`; `upper_headings`; `traced` blocks;
  `plain_numbers` for steps without a full stop; `small_text` for notes lettered small.
- `cover`: a `binder` with its spine, rivets and tabs, or a `folder` with the edges of its sheets showing; on a
  `label` or not; with a `band` or not.
- `folios` is `chapter_page` (3-14), `sequential` (1, 2, 3) or `drawing` (JI-102, with `drawing_prefix`). A set of
  drawings opens with its drawing list in place of contents, the manual's `about` lines as the notes under it, and
  has no index pages: its search still finds every entry by name.
- A chapter's tab takes its colour from the chapter's `tab`: a palette's id whose role `tab` it is, or a colour
  written `#AARRGGBB`.
- `hold_bar` is the bar that fills while the manual key is held over an item this manual opens: `plain` (a thin bar
  in the series' accent, the default), `blocks` (blue blocks lighting one after another in a dark, rimmed track, as
  a desktop of the 2000s loaded; J's Computers) or `hazard` (black and yellow stripes, as a machine's guard is
  marked; J's Industrial). In code, `CoreGuide.binder(...).withHoldBar(GuideStyle.HoldBar.BLOCKS)`. Its colours are
  the palette `jscore:guide/hold_bar`.

## A special block

*Added 2026-10-05.*

For a block the ready ones do not cover (a multiblock turning in three dimensions, a chart), register a renderer on
the client through the API, and name its kind in an entry:

```java
// client setup
GuideBlockRenderers.register(ResourceLocation.fromNamespaceAndPath("mymod", "structure"),
        (graphics, font, x, y, width, height, data, mouseX, mouseY) -> drawStructure(graphics, x, y, data));

// the entry
.custom("mymod:structure", 80, "{\"structure\":\"mymod:furnace\"}")
```

The page keeps the room and hands your renderer the JSON as a tag. A game without your mod leaves the room blank, and
the manual still opens. The same renderer draws a `drawing` picture of its kind, given the column's width and the
picture's height.

## Reading a manual as text

*Added 2026-10-06.*

A help program, or a viewer at a terminal, shows a manual as text rather than as pages. `ManualReader` reads one
that way, from the same declarations, so nothing is written twice:

```java
ManualReader reader = new ManualReader(manual, library.contentsOf(manual), words);
reader.tree();                        // chapters, sections and entries, numbered as on paper ("3.2.6")
reader.article("mymod:press");        // the entry: headings, paragraphs, steps, tables, links
reader.find("press");                 // an entry by its id's last part, its number or its title
reader.search("steam pressure");      // the entries holding every word
reader.index();                       // titles and explained words, alphabetically
```

`words` is the `IGuideText` that puts the keys in the reader's language, as the binder's screen does, and names items
with `itemName`. An article's pieces are a sealed set (`Parts`, `Heading`, `Paragraph`, `Item`, `Term`, `Table`,
`Picture`, `Recipes`, `Links`), so a viewer handles each one and the compiler says when a new one comes. Figures and tables keep their chapter's numbers
("Table 3-7"); a block seen from three sides becomes its picture and its legend, a plan its parts, and a page break
or a special block is left out. J's Computers reads the manuals this way in its help programs.

## What a player does with it

- Using the manual item opens it at its cover the first time; after that, at the page it was closed at, for as long
  as the game runs. A click on the cover opens it to its contents.
- The arrows, the arrow keys, Page Up and Page Down and the mouse wheel turn the pages; Home goes to the contents.
- A line of the contents, a number after "See", a link in a sentence, a line of the index and a chapter's tab each
  go to their page; so does an item on the plate under an entry's title whose page is another.
- The magnifier on the top edge turns the left page into a search of the index that filters as the player types; the
  right page shows the index around the first thing found.
- Holding **M** (the player can change it, "Open Its Page in the Manual") for a moment over an item in any inventory,
  or with the item in hand, opens the manual at its page. While it is held a small bar fills under the slot, or
  under the crosshair, in the look of the manual it opens. The key has to be let go before it opens a page again.
  An item with a page says so at the foot of its tooltip: "Hold [M] to open its page in the manual".
- On a computer of J's Computers, every system's help reads the same manuals beside its own commands.

## What can go wrong

- **The manual does not open.** Its file or its style's file is missing or cannot be read: the log names the file.
  Run the data generation.
- **A manual names "air" and the data generation stops.** A block with no item was named as an icon, a figure or an
  item an entry covers. Name the block's item, or another item.
- **An entry is missing from a manual.** Its section is missing, or the manual does not hold its chapter.
- **A "See" link shows a path instead of a number.** The entry it names is in no chapter of this manual.
- **The manual key does nothing over an item.** No entry `covers` it, or the key is bound to something else.
- **A drawing runs on to a sheet with a line or two.** Its text is longer than its column: shorten it, or move a
  `nextColumn` or `nextPage` so each sheet holds what it was written for. A translation that runs longer simply goes
  on to the next sheet.
- **The declaration stops on an id named twice.** A section and an entry share an id; rename one.
