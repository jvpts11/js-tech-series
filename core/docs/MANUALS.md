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
- A **style** is only data: the colours, one page or two side by side, the size of a page, the fonts, the binder's
  rings, how pages are numbered.

A mod's entries are written once and show in every manual that holds its chapter: its own manual, and the series'
manual that holds every chapter. Everything is numbered as technical manuals are: chapter 3, section 3.2, entry 3.2.6,
Figure 3-9, page 3-14.

## Writing a chapter

*Added 2026-10-05.*

```java
public final class MyGuide {

    private static final ModGuide GUIDE = MyContent.CONTENT.guide();

    static {
        GUIDE.chapter().titled("My Mod").order(10).tab("mymod:guide/tab").register();
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

### The five parts

Every entry of the series follows the same five parts, in this order, each with its own method that puts the part's
heading before its words:

1. `whatItIs`: what the thing is, from zero.
2. `whatItIsFor`: why a player would want it.
3. `howToGetIt`: where it comes from.
4. `howToUseIt`: numbered steps.
5. `whatCanGoWrong`: each problem as a player sees it, then its fix.

A player who knows the thing skips to the table or to what can go wrong; one who does not reads it from the top.
Explain a word where an entry first uses it, with `define`: the index lists every such word with its page, which
makes the index the manual's glossary.

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
| `custom(kind, height, json)` | A special block your mod draws (below). |

`covers(item)` says which items this entry is the page of: holding the manual key over one of them opens here.

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
        .priority(5)
        .register();
```

Give the player an item that opens it: a `ManualItem`, declared like any item,
`CONTENT.item("handbook", props -> new ManualItem(props, "mymod:handbook"))`.

When several manuals hold an item's entry, the manual key opens the one with the highest `priority`.

## Styles

*Added 2026-10-05.*

The Core brings one style, `jscore:binder`: two cream pages side by side in navy vinyl covers with three rings
through them, the chapter tabs at the right edge, tables in the terminal font, pages numbered by chapter. A style is a
file, `assets/<namespace>/guide/styles/<path>.json`, so a resource pack can replace it and a mod can bring its own:

```json
{
  "palette": "jscore:guide/binder",
  "colours": { "paper": "#FFFAF6EA" },
  "spread": true,
  "page_width": 166,
  "page_height": 201,
  "margin": 10,
  "rings": true,
  "table_font": "jscore:fixed_6x10",
  "folios": "chapter_page"
}
```

- `palette` names a declared palette ([Text and colour](TEXT_AND_COLOUR.md)) whose roles colour the book: `cover`,
  `coverEdge`, `paper`, `gutter`, `ink`, `faint`, `heading`, `link`, `rule`, `shade`, `highlight`, `ring`, `label`,
  `labelInk`, `tab`, `tabInk`, `warning`. `colours` sets any of them outright, so a style needs no code at all.
- `spread` is two pages at a time, or one; `folios` is `chapter_page` (3-14) or `sequential` (1, 2, 3).
- A chapter's tab takes its colour from the chapter's `tab`: a palette's id whose role `tab` it is, or a colour
  written `#AARRGGBB`.

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
the manual still opens.

## What a player does with it

- Using the manual item opens it at its cover; a click opens it to its contents.
- The arrows, the arrow keys, Page Up and Page Down and the mouse wheel turn the pages; Home goes to the contents.
- A line of the contents, a number after "See", a line of the index and a chapter's tab each go to their page.
- The magnifier on the top edge turns the left page into a search of the index that filters as the player types; the
  right page shows the index around the first thing found.
- Holding **M** (the player can change it, "Open Its Page in the Manual") over an item in any inventory for a moment
  opens the manual at its page; pressing it with the item in hand does the same. An item with a page says so at the
  foot of its tooltip: "Hold [M] to open its page in the manual".

## What can go wrong

- **The manual does not open.** Its file or its style's file is missing or cannot be read: the log names the file.
  Run the data generation.
- **A manual names "air" and the data generation stops.** A block with no item was named as an icon, a figure or an
  item an entry covers. Name the block's item, or another item.
- **An entry is missing from a manual.** Its section is missing, or the manual does not hold its chapter.
- **A "See" link shows a path instead of a number.** The entry it names is in no chapter of this manual.
- **The manual key does nothing over an item.** No entry `covers` it, or the key is bound to something else.
