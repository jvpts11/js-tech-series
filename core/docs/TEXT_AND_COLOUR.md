# Text and colour

Two things a player sees on every screen and that a mod on J's Core never writes as plain values in code: the
sentences they read, and the colours they are painted in. Both are declared once, in code, and both end up in
files that someone else (a translator, a resource pack) can change without touching your mod.

## Sentences a player reads

*Added 2026-09-23.*

A sentence a player reads is a **text key**: an id for the sentence, and the sentence in English. The game
shows each player the sentence in their own language, looked up by the id in a language file; English is the
one your code says, and every other language is a file a translator writes.

```java
@TextHolder
public final class MyTexts {

    public static final TextKey READY = TextKey.of("mymod.kiln.ready", "Ready");
    public static final TextKey HEAT = TextKey.of("mymod.kiln.heat", "Heat: %s degrees");

    private MyTexts() {
    }
}
```

- The key is dotted (at least one dot), lower case by convention, starting with your mod's id so it never
  collides with another mod's.
- `@TextHolder` marks a class whose `static final TextKey` fields (and those inside its enum constants) are
  written to your English language file by the data generation. A key declared outside a `@TextHolder` class
  never reaches the file, and the player sees the key itself.
- `%s` marks where a value goes; `%1$s`, `%2$s` name them by position when a language needs to put them in
  another order; `%%` is a percent sign. Nothing else is read.

To show one:

```java
Component line = GameText.component(MyTexts.HEAT.with(heat));
player.sendSystemMessage(line);
```

`with(...)` fills the `%s` marks and gives back a `Text`, a sentence that has not been put into any language
yet. That is what travels between server and player: the server never decides the language, each player's game
does. `GameText.component` turns a `Text` into the game's `Component`, in the language of the game it runs in;
`GameText.resolve` gives it as a plain `String`. `Text.literal("...")` marks text that is data rather than a
sentence (a file name, a number a player typed), so nothing tries to translate it.

### Other languages

Every other language is a file you write by hand next to the generated English, for example
`src/main/resources/assets/mymod/lang/pt_br.json`, with the same keys and the sentences in that language. Keep
the `%s` marks: a translation with a different number of them breaks the line it is used in.

### What the data generation writes

Running the data generation writes `assets/<mod>/lang/en_us.json` with every key of every `@TextHolder` class
of your mod, and with the names of your blocks, items, fluids, entities, creative tabs, the subtitles of your
sounds, the titles of your recipe kinds and the names of your settings. One key with two different English
sentences stops the run, so two classes cannot disagree about what a key says.

## Colours a screen paints with

*Added 2026-09-23.*

A **palette** is a set of named colours a screen paints with: its text, its panels, its accent. It is declared
once as a record whose components are the colour roles:

```java
@PaletteHolder
public final class KilnScreen extends AbstractContainerScreen<KilnMenu> {

    private static final Palette<Colours> PALETTE = Palettes.declare(MyMod.MODID, "screen/kiln",
            new Colours(0xFF202020, 0xFFE0E0E0, 0xFFFF8030));

    private record Colours(int panel, int text, int flame) {
    }

    // when painting:
    // Grounds.fill(g, x, y, x + w, y + h, PALETTE.get().panel());
}
```

- Colours are written `0xAARRGGBB`: alpha (how opaque, `FF` is fully), then red, green and blue.
- Every component of the record is an `int`, and its name is the role. A record with anything else is refused.
- The data generation writes the palette to `assets/mymod/palettes/screen/kiln.json`, one line per role
  (`"panel": "#FF202020"`).
- A **resource pack** can ship its own copy of that file and change any role; a role its file leaves out keeps
  the colour you declared, and a file that cannot be read is logged and ignored. So read the palette with
  `PALETTE.get()` every time you paint, never once into a field: the colours change when packs are reloaded,
  even on a screen that is open.
- `Palettes.derive(...)` keeps colours worked out from a palette (a lighter edge, a blend) and works them out
  again only when the palette changes.
- `@PaletteHolder` marks classes whose palettes must be known even before anything uses them, so the data
  generation and the pack reload find them. They are client classes: never load one on a dedicated server.

## Rules the series keeps

The mods of the series hold themselves to these with tests that fail the build. A mod on the Core is not
checked by them, but the reasons apply to it as well:

- **No sentence a player reads as a string literal in code.** It cannot be translated. (`HardcodedTextTest`)
- **Every language file has exactly the English keys,** with the same `%s` marks. (`TranslationCoverageTest`)
- **No colour as a literal outside `Palettes.declare`.** A resource pack could not change it.
  (`HardcodedColorTest`)
- **Colours readable on what they are painted on.** `ColorContrast.ratio(text, ground)` gives the contrast
  ratio, from 1 (none) to 21 (black on white); the series asks for 4.5 or more for text and 3 or more for
  secondary text and marks.

## What can go wrong

- **The player sees `mymod.kiln.ready` instead of a sentence.** The class holding the key has no `@TextHolder`,
  or the data generation has not run since the key was added.
- **A translated line shows `%s` or misses a value.** The translation does not have the same marks as the
  English.
- **A resource pack's colours do not show.** The palette is read once into a field instead of with `get()` at
  paint time, or the pack's file is not at `assets/<namespace>/palettes/<path>.json`.
- **The dedicated server crashes loading a screen class.** A `@PaletteHolder` class was touched from code that
  runs on the server.
