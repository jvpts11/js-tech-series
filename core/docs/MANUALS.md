# Manuals

How a mod on J's Core writes its manual: the book a player opens in the game to learn what each block and item is,
what it is for, how to get it, how to use it, and what can go wrong. The Core draws the book, numbers it, builds its
contents and its index, searches it, and opens it at an item's page when the player holds the manual key over the
item. A mod writes what the pages say.

## The words

- A **manual** is a book with a title, a style it is drawn in, and the chapters it holds.
- A **chapter** is a run of one mod's sections under a title. Every mod has a main chapter, and may write more: each
  is bound into the books that name it, and one chapter can be bound into several books. A **section** groups a
  chapter's entries under a title ("Machines").
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

### More chapters, more books

*Added 2026-10-11.*

A mod with a lot to explain writes more than one chapter, and more than one book. The main chapter, declared with
`chapter()`, is named by your mod's id (`mymod`); any other, declared with `chapter("path")`, is `mymod:path`. A
section says which chapter it sits in with `in(...)`; without it, it sits in the main one.

```java
// A chapter written for one book only: the series' Technical Reference leaves it out.
public static final GuideChapter SHELL = GUIDE.chapter("shell").titled("The Shell").order(50)
        .about("Typing commands, and what each one does.").ownBooksOnly().register();

public static final ModGuide.SectionRef COMMANDS = GUIDE.section("commands").titled("Commands")
        .icon(() -> MyContent.TERMINAL).in(SHELL).register();

// Two books bind it, each in its own order; the first also binds the main chapter after it.
GUIDE.manual("handbook").titled("The Handbook").style(CoreGuide.BINDER).chapters(SHELL.id(), "mymod").register();
GUIDE.manual("pocket_guide").titled("Pocket Guide").style(CoreGuide.BINDER).chapters(SHELL.id()).register();
```

A book prints the chapters it names in the order it names them, and numbers them by that order: the same chapter is
chapter 1 of one book and chapter 4 of another, and its entries' numbers follow. A chapter declared with
`ownBooksOnly()` stays out of the manuals that hold every chapter, such as the Technical Reference; leave it out for
a chapter everyone should find there.

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

Explain a word where an entry first uses it, with `define`: the index lists every such word with its page.

*Added 2026-10-11.* Every word explained with `define` also goes into the manual's glossary, printed between its last
chapter and its index on pages numbered GL-1, GL-2: alphabetical under its letter, each word with the page that
explains it and what it means set in under it. A word too long for its line goes on under itself, whole, with its
page on the last line, and what a word means runs on to the next page only with two lines of it at least on either
side. In every other entry, the first time a sentence uses a word the glossary holds, the word is drawn as a link to
the entry that explains it, so a reader meeting it there is one click away from what it means. A word is found whole
and whatever its case, the longest first ("network engine" before "network"); words already in a link and the words
an entry explains itself are left alone. The contents end with a line leading to the glossary. A manual whose
entries explain no word has none.

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
right in your own manual and in one holding every chapter. A target that manual does not hold but another book does
reads as its words and that book, "the shell (in The Handbook)", and a click opens that book at the target
(*added 2026-10-11*); a target no book holds reads as its words alone. A target is an entry or a section
(`namespace:path`) or a chapter (its id: the mod's id for the main chapter). A translation keeps the brackets and the
target and translates the words. Read as text, in a help program, a link is written as its words
and number, and the entry lists every page it led to under "See also" at its end.

*Added 2026-10-11.* A key of the keyboard is written between double brackets and drawn as a key, its name never
broken: `"To save, press [[Ctrl]]+[[S]]."` Read as text it is the key's name in single brackets, "[Ctrl]+[S]". A
translation may rename a key as a keyboard of its language names it.

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
| `table(caption)`, then `amount(label, value, unit)`, `property(label, words)`, `fixed(label, data)` | A numbered table of properties, headed "Property" and "Value". An amount is read from your code and written in the reader's way ("12,000 FE"); words are translated; fixed values (a model number) are not. |
| `table(caption, heads...)`, then `row(cells...)` | A numbered table with heads of its own ("Command", "What it does"), a cell a column in each row: `GuideCell.words(english)`, `GuideCell.words(sharedKey)`, `GuideCell.code(text)` or `GuideCell.amount(value, unit)`. See [Tables](#tables). |
| `tableFrom(source, caption, heads...)` | A numbered table whose rows your mod gives as the manual is laid out. See [Figures read from your mod](#figures-read-from-your-mod). |
| `command(source)` | A command's page, read from the command as the manual is laid out. See [Commands](#commands). |
| `scene(caption, height)`, then `place(...)`, `view(...)`, `layers()`, `balloon(...)` | A scene of blocks drawn as the game draws them, which the reader turns and brings nearer, numbered with the figures. See [Scenes](#scenes). |
| `listing(name, lines...)`, `listingIn(language, name, lines...)` | A numbered listing of code ("Listing 3-4."), headed by its file's name, its lines numbered and coloured by its language, with a Copy button. See [Listings of code](#listings-of-code). |
| `session(look, title)`, then `typed(prompt, text)`, `answer(lines...)` | A session at a terminal, in the colours of the terminal's palette, under its name: what was typed after each prompt and what the system answered. See [Sessions at a terminal](#sessions-at-a-terminal). |
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

### Tables

*Added 2026-10-11.*

```java
.table("Commands for files", "Command", "What it does")
.row(GuideCell.code("dir"), GuideCell.words("Lists what is in the folder you are in."))
.row(GuideCell.code("del"), GuideCell.words("Deletes a file."))
```

A table is laid out to fit the page, never the other way round:

- Words are written small and a column of nothing but data (code and amounts) in the style's code font, one cell a
  character, so its figures line up. A cell wraps between words only: every column is at least as wide as its longest
  word, and the room left over is shared out by how much each column holds.
- When the columns cannot stand side by side (their longest words are wider together than the page, or a column of
  words would be left narrower than about nine letters), each row is laid out under the one before it instead: its
  first cell in the heading's colour with its second beside it, then each further cell set in beside its column's
  head. A table of error codes with what each says, means and how to fix it reads this way.
- A table that fits on a page is never parted: when the room left on this page is too little, all of it moves to the
  next. A table taller than a page runs on, its caption said again with "(continued)" and its head over each part,
  and keeps at least three rows on either side of where it parts; a row taller than a page is cut between two of its
  lines rather than run past the page's foot.

Keep a table to what a reader looks up. Two or three columns read best; long explanations belong in the text, or in a
table that will stack.

A word wider than any room a table has for it (a stacked row's first cell, or a cell beside its head) is cut where
the room ends, and the laid-out book lists it in `GuideBook.cut()`. The test mod lays every book of every mod and
keeps that list empty, with nothing drawn under a page's foot and no page left empty between two that are not.

### Listings of code

*Added 2026-10-11.*

```java
.listing("Hello.sgs", "using System.IO.*;", "", "namespace Hello;", "", "class Program {",
        "  static void Main() {", "    Console.PrintLine(\"Hello, world!\");", "  }", "}")
```

A listing is printed in the code font on a paper of its own, under a strip with its number, its file's name and a
Copy button that puts the whole code on the clipboard, which the game's editors paste. Its lines are numbered; a line
too long for the page goes on under itself, set in two places, unnumbered, broken after a space or an opening bracket
and never inside a word. A listing that fits on a page moves on whole; a longer one runs on under its strip, marked
"(continued)". A click on a listing opens it over the whole book with no line broken (the wheel scrolls it down, and
across with Shift), with Copy and Close.

The listing is coloured by its language: the extension of its file's name, or the language `listingIn` names. A
language the game compiles colours itself, as its editors colour it; another registers how it is coloured, while the
game loads, with `GuideHighlighters.register("iql", lines -> GuideCode.runs(lines, spans))`, the spans saying which
characters are keywords, names, quoted text, numbers or comments. Code reads the same in every language and is not
translated.

### Sessions at a terminal

*Added 2026-10-11.*

```java
.session("jsc:guide/terminal/dos", "MC-DOS 6.22")
.typed("C:\\>", "sgsc Hello.sgs").answer("Hello.sgs: 0 errors")
.typed("C:\\>", "sigma run Hello.asm").answer("Hello, world!")
```

A session shows a terminal as the system draws it, so a reader knows the screen when they meet it: the terminal's
name on a strip, then each line in the code font, the prompt, what was typed and what the system said each in the
terminal's own colour. A line too long for the page goes on at the next row's first character, as a terminal wraps
it. The colours are a palette of the `GuidePalettes.Terminal` kind (`ground`, `text`, `prompt`, `typed`) a mod
declares for each of its terminals; J's Computers declares `jsc:guide/terminal/dos`, `bash`, `zsh` and `sh`, and a
session naming no palette the game knows takes the Core's `jscore:guide/terminal`. What a terminal prints is the same
in every language and is not translated.

### Figures read from your mod

*Added 2026-10-11.*

A number written into a sentence by hand goes out of date the day the code changes. Say instead where the manual reads
it, with `GuideFacts`, while the game loads, and name it in the sentence by its id between double braces:

```java
GuideFacts.value("mymod:memory_slots", () -> new GuideBlock.GuideValue.Amount(PcSpec.MEMORY_SLOTS, ""));
GuideFacts.rows("mymod:cable_reach", () -> CableTier.all().stream().map(tier -> new GuideBlock.TableRow(List.of(
        new GuideBlock.GuideValue.Words(tier.nameKey()), new GuideBlock.GuideValue.Amount(tier.reach(), "m"))))
        .toList());

.paragraph("It takes up to " + GuideFacts.token("mymod:memory_slots") + " memory modules.")
.tableFrom("mymod:cable_reach", "How far each cable reaches", "Cable", "Reach")
```

The value is written the reader's way as the page is laid out ("12,000 FE"), and a translation keeps the braces where
its own language puts the figure. A table read from a source holds no rows in its file: its mod gives them each time a
manual is laid out, so a part another mod adds to the family, or a setting the player changed, is in the next book
opened. A row of the wrong number of cells is left out, with a line in the log; a name no mod registered is printed
as written, braces and all, so it is seen and fixed. The answers are asked for in the player's game: register from the
client's setup anything only the client knows.

A cell a source gives may also be `GuideBlock.GuideValue.Said`, words your mod says as a `Text` (a `TextKey`'s
sentence, or a literal): it is put in the reader's language as the page is laid out. Only a source answers with
these; a manual's files never hold one.

### Scenes

*Added 2026-10-11.*

```java
.scene("A furnace and a chest on grass. Drag them to see them from behind.", 90)
.place(0, 0, 0, () -> Blocks.GRASS_BLOCK)
.place(1, 0, 0, () -> Blocks.GRASS_BLOCK)
.place(0, 1, 0, () -> Blocks.FURNACE, "facing=south,lit=true", "")
.place(1, 1, 0, () -> Blocks.CHEST, "facing=south", "{Items:[{Slot:0b,id:\"minecraft:diamond\",count:1}]}")
.view(-30, 25, 1)
.layers()
.balloon(1, GuideBlock.ScenePart.of(0, 1, 0).box(0, 0, 15, 16, 16, 16), "The furnace's front",
        "It glows while it burns")
.balloon(2, GuideBlock.ScenePart.of(1, 1, 0), "A chest")
```

A scene is a few blocks in a small grid (up to 32 each way), drawn on the page as the game draws them in a world:
each block's model shaded by the blocks round it, and each block entity through its renderer, GeckoLib's models among
them, in full light. `place` puts a block at a place of the grid (across, up, and back towards the north), in the
state its properties give, with what its block entity holds written as a compound tag; both may be empty.

The reader drags the scene to turn it (across turns it round, up and down tilts it), turns the wheel over it to bring
it nearer or send it farther, within limits, and presses Reset on the strip under it to go back to how the book
shows it. `view(yaw, pitch, zoom)` sets that: turned by no degrees the scene is seen from its south side, head on; a
positive tilt looks down on it; a zoom of 1 fits the whole scene, turned any way, in its frame. A scene declared with
`layers()` has two more buttons on its strip, which hide its top layer or show the next, to see inside a multiblock.

Each balloon is numbered and pinned to a part: a whole block (`ScenePart.of(x, y, z)`), or a box inside one in its
sixteen pixels each way (`box(...)`), such as the tray of a drive. It stands at the frame's side nearer its part,
with a line to the part's middle, and its line of the legend under the scene says what the part is. The pointer on a
balloon, or on its line of the legend, outlines the part and lights both, and its tooltip adds the second sentence
given, such as what to do with the part. The legend is set in two columns when every line of it fits half the
column.

A scene is drawn into a picture of its own only when it is turned, brought nearer, peeled, or shown at another size,
so a page holding one costs nothing frame to frame. Read as text, in a help program, a scene is its numbered caption
and the list of its balloons. A block's state or data the game cannot read is drawn as air, with a line in the log;
the test mod checks every scene every mod declares for that.

### Commands

*Added 2026-10-11.*

A mod whose computers run commands writes what each command is once, on the command, and the manual prints the
command's page from it, so the book and the command's own help at a prompt never disagree. Register each command's
page with `GuideFacts.command`, as a `GuideCommand` answered from the command, and print it in an entry with
`command(source)`:

```java
GuideFacts.command("mymod:command/scan", () -> new GuideCommand("scan", Text.literal("list what is near"),
        Text.literal("[radius]"), List.of(MyTexts.SCAN_ABOUT.text()),
        List.of(new GuideCommand.Option("-a", MyTexts.SCAN_ALL.text())),
        List.of(new GuideCommand.Example("scan 16", MyTexts.SCAN_16.text())),
        List.of(Text.literal("ScanOS 2")), List.of("probe")));

.command("mymod:command/scan")
```

The page reads, in this order: what the command is for, as a sentence; "How to type it", its name and what it takes
in the code font; what it does, a paragraph each; "Options" and "Examples", each a table of what is typed beside what
it does, laid out as any table is but with no caption or number of its own; "Runs on" and the systems; and "Read
next", each command named leading to the page of it this manual holds. A source no mod registered prints a line
saying the page is missing, so the gap is seen and mended. Read as text, in a help program, the page is the same
parts as paragraphs, a line of code and two tables.

J's Computers registers a page for every command of every system, under `jsc:command/<family>/<name>` (the family
of systems: `mc_dos`, `mc_net`, `frames`, `linux`, `unix`, `freebsd`), and for every verb of a live install medium
under `jsc:command/live/<name>`; `CommandGuidePages.id(Platform.MC_DOS, "dir")` writes one. Its pages are made of
what the command says about itself, the same words `man`, `help` and `/?` print, and name every system that has the
command, with the package to install first where one is needed. A command an addon registers with `CliCommands`
gets its page the same way.

## Writing a manual

*Added 2026-10-05.*

```java
GUIDE.manual("handbook").titled("My Mod Handbook")
        .cover("My Mod", "Reference Library")
        .edition("First Edition")
        .partNumber("MM-0001")
        .style(CoreGuide.BINDER)
        .chapters("mymod")                      // the chapters' ids, in print order; "*" for every chapter of every mod
        .about("This handbook holds everything My Mod adds.")
        .icon("mymod:gui/guide/cover_mark")      // a 32 by 32 texture printed on the cover
        .priority(5)
        .register();
```

The cover prints the first of its `cover` lines bold and the others small, the title large, and the edition under
it; an edition may hold `%s`, which is the number of sheets the manual has ("Set A - Sheets 1 to %s"). The part
number stands at the foot of a binder's cover as "Part No. MM-0001". The Technical Reference, which holds every
chapter, has priority 100; the series' own manuals 50. A book has a tab for each chapter it holds; a manual holding
every chapter has a tab for each mod instead, which opens the mod's first chapter, so a mod with many chapters does
not crowd the edge.

Give the player an item that opens it: a `ManualItem`, declared like any item,
`CONTENT.item("handbook", props -> new ManualItem(props, "mymod:handbook"))`. To hand it to each player once, the
first time they join a world, call `GuideGifts.giveOnFirstJoin("mymod:handbook", MyContent.HANDBOOK)` while the game
loads: it goes into the first free place above the hotbar, and the world keeps who was given it.

When several manuals hold an item's entry, the manual key opens one that names the item's chapter before one that
holds every chapter, so a mod's item opens in the mod's own manual when it has one, and in the Technical Reference
when it has not. Among manuals alike, it opens the one with the highest `priority`.

*Added 2026-10-11.* Some items stand for different things from stack to stack, such as an installation disc that
holds one system or another. Say which entry a given stack is the page of with `GuidePages.byStack`, while the game
loads; the manual key over that stack opens that entry, in the book that holds it, and a stack you answer nothing
for (an empty id) opens its item's own page:

```java
GuidePages.byStack(MyContent.INSTALL_DISC.get(), stack -> "mymod:install_" + MyDiscs.systemOn(stack));
```

## Styles

*Added 2026-10-05.*

The Core brings one style, `jscore:binder`: two cream pages side by side in navy vinyl covers with three rings
through them, the chapter tabs at the right edge, code and figures in the small terminal font, pages numbered by
chapter. A style is a
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
  "code_font": "jscore:fixed_5x8",
  "folios": "drawing",
  "drawing_prefix": "JI"
}
```

- `palette` names a declared palette ([Text and colour](TEXT_AND_COLOUR.md)) of the `GuidePalettes.Binder` kind,
  whose roles colour the book: `cover`, `coverEdge`, `paper`, `gutter`, `ink`, `faint`, `heading`, `link`, `rule`,
  `shade`, `highlight`, `ring`, `label`, `labelInk`, `tab`, `tabInk`, `warning`, `accent` (notes, what can go wrong,
  a link pointed at on a drawing), `number` (the numbers of steps and balloons), `band`, `grid`, `spine`,
  `coverLine` (the cover's first line), and a listing's `codePaper`, `codeHead` (its strip), `codeKeyword`,
  `codeName`, `codeText`, `codeNumber`, `codeComment` and `codeCopied` (its Copy button once it has copied).
  `colours` sets any of them outright, so a style needs no code at all.
- `pages`: two at a time or one, their size, the margin, and how many columns the text runs in.
- `decor`: a binder's `rings`; a drawing's `grid`, `frame` and `title_block`; `upper_headings`; `traced` blocks;
  `plain_numbers` for steps without a full stop; `small_text` for notes lettered small.
- `cover`: a `binder` with its spine, rivets and tabs, or a `folder` with the edges of its sheets showing; on a
  `label` or not; with a `band` or not.
- `body_font` and `code_font`: the fonts the running text, and the code and figures, are written in; empty for the
  game's own. The code font is drawn at its own size, so it should be a small one with lines no taller than eight
  pixels, as `jscore:fixed_5x8` is.
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
`Code`, `Picture`, `Recipes`, `Links`), so a viewer handles each one and the compiler says when a new one comes.
Figures and tables keep their chapter's numbers ("Table 3-7"); a block seen from three sides becomes its picture and
its legend, a plan its parts, a scene its caption and the list of its balloons, a listing or a session at a terminal
the lines it holds, a key its name in brackets ("[Ctrl]"), and a page break or a special block is left out. J's
Computers reads the manuals this way in its help programs.

*Added 2026-10-11.* `TextTable.shape(heads, rows, room, gap, least, measure)` lays a table out for a reader that
writes in lines, whatever its letters measure (a terminal's characters, a window's pixels): its columns side by side
when they fit, each as wide as its longest word at least and its cells wrapped between words, or else each row set
out under the one before, each further cell after its column's head. Each line holds cells with where they start and
what they are (a head, the first cell of a row, another cell), and `padded()` writes a line for letters of one
width. `reader.commandPage(source)` names the entry printing a command's page read from that source, which a
system's help shows for the command in place of the page made of its words alone.

## What a player does with it

- Using the manual item opens it at its cover the first time; after that, at the page it was closed at, the game
  closed and opened in between: each client keeps where every book was left, in `jstech/manuals.json` in the game's
  folder. A click on the cover opens it to its contents.
- The arrows, the arrow keys, Page Up and Page Down and the mouse wheel turn the pages; Home goes to the contents.
- A line of the contents, a number after "See", a link in a sentence, a line of the search and a chapter's tab each
  go to their page; so does an item on the plate under an entry's title whose page is another.
- The top edge holds four buttons: the contents, Back and Forward at its left, the search at its right. Back returns
  to where the reader jumped from (a link, a tab, the contents, a line of the search), and so do the mouse's back
  button and Backspace; Forward comes back again. Turning pages one by one leaves no mark. Each book keeps its own way
  back while the game runs.
- Every control lights and is framed under the pointer, sinks while it is held, and fades when it leads nowhere;
  the pointer turns into a hand over whatever takes a click, a link's paper lights behind it, and the game's tooltip
  says where Back and Forward lead. A button's mark is drawn in whichever of the page's ink and the label's colour
  reads better on the style's cover, which a test checks for every style the series has.
- A manual sounds like a book of the game: its page turns whatever turned it, it is heard opening and laid down when
  it closes, and its buttons and tabs click as the game's buttons do. The sounds are the game's own, played in the
  Manuals channel of the Sound Mixer, so a player can turn the books down apart from everything else.
- The magnifier turns the left page into a search of the whole manual that finds as the player types: the titles and
  explained words of the index first, then every entry whose pages hold the words, with the line they were found on;
  the right page shows where the first thing found stands.
- Holding **M** (the player can change it, "Open Its Page in the Manual") for a moment over an item in any inventory,
  or with the item in hand, opens the manual at its page. While it is held a bar fills in the item's tooltip, from one
  side to the other, or under the crosshair, in the look of the manual it opens. The key has to be let go before it
  opens a page again.
  An item with a page says so at the foot of its tooltip: "Hold [M] to open its page in the manual".
- On a computer of J's Computers, every system's help reads the same manuals beside its own commands.

## What can go wrong

- **The manual does not open.** Its file or its style's file is missing or cannot be read: the log names the file.
  Run the data generation.
- **A manual names "air" and the data generation stops.** A block with no item was named as an icon, a figure or an
  item an entry covers. Name the block's item, or another item.
- **An entry is missing from a manual.** Its section is missing, or the manual does not hold its chapter: a book
  holds only the chapters it names, and a section without `in(...)` sits in the mod's main chapter.
- **A chapter shows in the Technical Reference though it was written for one book.** Declare it `ownBooksOnly()`.
- **A "See" link shows a path instead of a number.** The entry it names is in no chapter of this manual, nor of any
  other book.
- **The manual key does nothing over an item.** No entry `covers` it, or the key is bound to something else.
- **A drawing runs on to a sheet with a line or two.** Its text is longer than its column: shorten it, or move a
  `nextColumn` or `nextPage` so each sheet holds what it was written for. A translation that runs longer simply goes
  on to the next sheet.
- **The declaration stops on an id named twice.** A section and an entry share an id; rename one.
