# Fonts

J's Core lets any mod built on it draw text in fonts of its own, and put text on a monospace grid the way a
terminal does. This page explains both from the start: what a font is to the game, how a mod declares one, what the
data generation makes of it, and how the grid painter uses it.

## What a font is to the game

Minecraft draws text from fonts described by files under `assets/<namespace>/font/`. The kind the Core uses is the
bitmap font: one picture cut into equal cells, with a list saying which character each cell holds. The game measures
each character by its lit pixels and moves the pen that far plus one pixel, and a character the font lacks is looked
for in the next font the file lists. A piece of text picks its font by name through its style, so any text in the
game can be drawn in a mod's font once that font exists.

Drawing such a picture by hand is slow, and the fonts worth having already exist: decades of free bitmap fonts made
for terminals and consoles, with licences that let them be shipped. So a mod does not draw its fonts; it names one.

## Declaring a font

A font is declared once, in the mod's content, like a block or a sound:

```java
public static final CellFont TERMINAL = CONTENT.font("terminal")
        .bdf("font/source/terminal.bdf")
        .cell(6, 10)
        .baseline(8)
        .licence("Public domain")
        .credit("Who drew it, and where it comes from")
        .register();
```

- `bdf` names the source: a font in the Glyph Bitmap Distribution Format (BDF), the plain text format the X Window
  System keeps its bitmap fonts in, under the mod's `assets` folder. BDF is the first format the Core reads.
- `cell` is the box every glyph sits in, in pixels, and `baseline` how many of its rows are above the baseline. Both
  are checked against the source when the data is generated, so a declaration that no longer matches its font
  stops the generation instead of drawing every letter a pixel out.
- `licence` and `credit` travel with the font: whoever draws in it can say what it is and who made it. A font with
  either missing is refused. The font's source is also listed in `ASSET_REGISTRY.md` and its notice reproduced in
  `THIRD_PARTY_NOTICES.md`, as for every asset from outside the project.

The source is read only by the data generation; it is left out of the mod's jar. To keep it out, the mod's
`build.gradle` excludes `**/*.bdf` from its resources, as the Core's does.

## What the data generation makes

`runData` reads the source and writes two files:

- `assets/<mod>/textures/font/<name>.png`: the picture, every glyph in its own cell, placed in the cell where the
  font puts it against the pen and the baseline. A glyph that reaches outside the cell is cut to it, and control
  characters are left out.
- `assets/<mod>/font/<name>.json`: the font file. It lists, in the order the game tries them, the characters that
  draw nothing with the width each moves the pen (the game would otherwise make a space one pixel wide), the picture
  set so the font's baseline lies on the same line as the game's own letters, and the game's default font for every
  character this one lacks.

A blank glyph for a letter or a digit is a glyph the font's makers left empty, not a space, so it is left out and the
game's font draws it. A resource pack can replace either file, and the grid follows whatever the loaded packs say.

## The grid painter

A terminal puts each character in a cell of its own, all the cells one width: columns of figures line up, a bar made
of one character holds still as the line redraws, and a frame is a frame. The game's fonts move the pen by each
glyph's width, so the Core's `GridPainter` places the characters itself:

- A run of characters that each move the pen exactly one cell is drawn as one string, which lands on the grid.
- A character of the grid's font that is narrower or wider than a cell is drawn at its cell's left edge, where the
  font's picture already holds its place.
- A character the grid's font lacks is drawn in the game's font, in the middle of its cell, on the same baseline.
- The box lines (U+2500 to U+257F) and the blocks (U+2580 to U+259F) are drawn by the painter as filled rectangles,
  to the width of the cell and the height of the row, the way terminal programs draw them. A font's own box glyphs
  are drawn for its own cell height and leave gaps when the rows are spaced any other way; these join from cell to
  cell and from row to row at any spacing. Single lines run on the column left of the middle and the row below it,
  as the classic terminal fonts draw them; a heavy line is two pixels thick; a double line is two single lines a
  pixel apart; the three shades are the whole cell in a quarter, a half and three quarters of the colour.

```java
private final GridPainter<CliStyle> painter = new GridPainter<>(CoreFonts.FIXED_6X10);

painter.draw(graphics, font, rows, row -> row.spans(), x, y, 10, style -> colourOf(style), ground);
```

`draw` takes the rows, a way to turn one into its spans of styled text, where to start, the row pitch and the colour
of each style. Each row is laid out once and remembered for as long as the caller keeps the object it stands for;
`drawOnce` draws a row that changes too often to remember, such as the line being typed. The text is drawn plain,
with no shadow; the colour of the ground is taken so a shadow worked out from it can come back in one place.

The pure parts are separate from the game and tested on their own: `BdfReader` reads a font, `FontSheet` lays it out,
`CellGlyphs` gives the rectangles of a box line or a block for any cell, and `GridLayout` turns a row into the pieces
the painter draws.

## The Core's fonts

`CoreFonts` carries Misc Fixed, the X Window System's fixed font, from Markus Kuhn's ucs-fonts as the X.Org
Foundation ships it, in the public domain, in three sizes of one design:

| Font | Cell | Above the baseline | Characters |
| --- | --- | --- | --- |
| `FIXED_6X10` | 6 x 10 | 8 | nearly 1,600: Latin, Greek, Cyrillic, Hebrew, runes, Braille, symbols, boxes and blocks |
| `FIXED_9X15` | 9 x 15 | 12 | nearly 4,800 |
| `FIXED_10X20` | 10 x 20 | 16 | over 5,200 |

Any mod can draw its terminal-like views in them, and J's Computers draws every one of its terminals in them: the
consoles, the terminal windows, the console editors and the text-mode installers. A bitmap font drawn at a scale where
one of its pixels is not a whole number of the screen's comes out smeared, so a terminal picks, for the screen it is
on, the size and the whole number of screen pixels to each font pixel that draw the widest cell its eighty columns
still fit: on a monitor's glass, the 9x15 at one pixel each at GUI scale 2, the 6x10 at two at GUI scale 3, the 9x15
at two at GUI scale 4. A terminal window on a desktop draws in the 6x10, one GUI pixel to each of its own.

A `CommandLine` of the Core's toolkit writes the line being typed in such a font too, with `setCellFont`.
