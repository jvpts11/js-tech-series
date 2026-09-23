# Code style

The J's Tech Series is a set of NeoForge mods for Minecraft 1.21.1, written in Java 21 and built with
Gradle and ModDevGradle. Everything in the repository is written in English: code, comments, Javadoc, translation
keys and values, commit messages, branch names and issues. The style below is what a reviewer expects to
see in a pull request.

## File header

Every `.java` file starts with this block comment, above the `package` line:

```java
/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers...;
```

The SPDX tag identifies the licence; its full text lives in `COPYING.LESSER`, and the GPL it builds on in
`COPYING`. Do not use the line-comment form and
do not paste the long licence preamble. The last line names the mod the file belongs to: J's Core, J's
Computers or J's Industrial.

## Modelling

- Records for values and data-transfer types; fields are `final`; prefer immutability and expose as little
  as possible.
- Sealed interfaces and sealed classes for closed hierarchies, so a `switch` can be exhaustive and nothing
  extends what should not be extended.
- Wrapper types for strongly typed identifiers (a `NetworkUuid`, a `NodeUuid`) instead of raw `UUID`s
  travelling through the code.

## Naming

- Classes in `PascalCase`, methods and fields in `camelCase`, constants in `UPPER_SNAKE_CASE`.
- Translation keys follow the vanilla pattern: `block.jsc.macerator`, `item.jsc.aerogel`,
  `jsc.gui.operation.pending`.
- Enum constants read as what they are: `OperationStatus.PENDING`, `OperationStatus.COMPLETED_PARTIAL`.

## Text a player reads

Every sentence a player reads is translatable, the terminals included, and is written once, beside the code that
says it:

- Declare it as a `static final TextKey` field, with its key and its English, in a class marked `@TextHolder`:
  `private static final TextKey NO_SNAPSHOT = TextKey.of("jsc.ports.no_snapshot", "No snapshot available");`
  The English language file is generated from these declarations by `runData`; there is no language provider to
  edit. Every such class is loaded to be read, on a dedicated server too, so a class that exists only on a client
  (a screen) keeps its sentences in a small class of constants beside it.
- Put arguments in with `%s` (or `%1$s`, `%2$s`) and `key.with(...)`, which gives a `Text`. Words that are data
  (a file name, a path, a number, what the player typed) go in as `Text.literal(...)`, which is never translated.
- A `Text` is resolved where it is read: a screen in its player's language, the server in English, which is also
  the language of anything a machine writes down (a file, a pipe).
- Commands, flags, paths, code and brand names are not translated.

The build holds this in place. A test counts the fixed words a player reads in every source file, and a file may
never have more than it had: fixed prose added anywhere fails the build. Every translation must carry every key of
the English file and nothing else, so a missing sentence in any language fails it too.

## Blocks and items

A block or an item is declared once, through its mod's content (the core's `ModContent`), with everything about
it: what it is made from, its English name, how it and its item look, what it drops, the creative tab section that
shows it, the block tags it belongs to (such as the tool that mines it). Its block entity is registered with every
block that makes it. Nothing else lists it: the block state, the models, the English, the loot table and the tags are
generated from the declaration by `runData`.

A game test checks every declared block and item of every mod: that it was declared rather than registered by hand,
has its block state and models (and that no model is left over for an item that is gone), a name, a place in a
creative tab, the drop it declares, a tool that mines it when it needs one, and a block entity that accepts it.

## Colours

A colour belongs to a palette, not to the code that paints with it.

- A palette is a record whose every component is one colour, named for what it colours (`text`, `accent`, `edge`).
- Declare it once with the core's `Palettes.declare(mod, path, colours)`, as a `static final` in a class marked
  `@PaletteHolder`. The mod and the path say whose it is and what it colours, such as `jsc` and
  `desktop/frames_xp`.
- `runData` writes each palette to `assets/<mod>/palettes/<path>.json`, as `#AARRGGBB` per role. A resource pack
  replaces that file to recolour the screens; a role its file leaves out keeps the declared colour.
- Paint with `palette.get()` each time; never keep a copy of the colours, or a change of pack will not reach it.
- A palette holder is loaded only on a client, so it can be a client class.

The build holds this in place as it does text: a test counts the colours written into each source file as numbers
(`0xAARRGGBB`, outside a palette declaration), and a file may never have more than it had.

## Imports and layout

- No unused imports, no wildcard imports.
- Members in a readable order: fields, constructor, public methods, private methods.

## Comments

Comments explain why, not what. A comment must stand on its own: a reader of the public repository has
nothing but the code, so a comment that points at a document they cannot see explains nothing. Write the
technical reasoning out.

## Tests

- Pure logic, with no dependency on Minecraft or NeoForge, is tested with JUnit 5 under `src/test/java`,
  mirroring the package of the class under test. Such a test never imports `net.minecraft.*` or
  `net.neoforged.*`.
- Anything that touches the game (registries, items, blocks, block entities, menus, capabilities,
  networking, datagen, any runtime object) is a GameTest under `src/main/java/.../gametest/`, run with
  `runGameTestServer`. When in doubt, it is a GameTest.
- Test methods are named `methodName_expectedBehavior`. Use `@Test` and, for simple setup, `@BeforeEach`;
  do not use `@DisplayName` or `@Nested`. Helpers that need no instance state are `private static`.
- Run `runData` after any change to a block state property, a model, a texture or a language entry, and
  check that it finished successfully.

## Commits and branches

- A plain imperative title, no trailing period, followed by an optional body that explains what changed
  and why: `Add the energy network distribution`, `Fix the peripheral cable length clamp`. No type
  prefixes such as `feat:` or `fix:`.
- Branch names in English, in three parts: the mods the branch touches, the kind of work, and its name:
  `<mod>-<mod>/<kind>/<name>`. The mods are the subproject names in alphabetical order, joined by
  hyphens (no subproject name contains one); the kind is `feature`, `fix` or whatever the work is.
  Examples: `core/fix/payload-streamcodec`, `computers-core/feature/user-software`. The first part says
  at a glance which jars a branch changes, and the merge commit's message keeps it in the history.
- Commits carry the author's own authorship and nothing else.

## NeoForge 1.21.1

The APIs this project uses are the 1.21.1 ones. Newer NeoForge lines changed several of them, so before
copying a snippet from the web, check it against 21.1. In particular: data components instead of raw
item NBT; the block, entity and item capability system, never `LazyOptional`; `SavedData` with
`save(CompoundTag, HolderLookup.Provider)` and a `SavedData.Factory`; payloads registered through
`RegisterPayloadHandlersEvent`; screens with the `renderBg` / `renderLabels` / `render` signatures.
