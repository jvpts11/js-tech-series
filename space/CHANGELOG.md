# Changelog of J's Space

Every change to J's Space, newest first, taken from the series' changelog
([CHANGELOG.md](../CHANGELOG.md)), which records each change once with the mods it concerns and keeps
the releases before 0.5.0a. This file is written from it by `./gradlew changelogs`.

## [Unreleased]

### API
- The promise: J's Space's API is at version 1, in `dev.jstech.space.api`, experimental. For now it holds
  only its number, `JsSpaceApi.VERSION`; each registry an add-on adds to (worlds and their generators and modifiers,
  landmarks, rocket parts, starship sections, kinds of satellite and probe, what can be discovered of a world) joins
  it with the model it holds, raising that number. Its photo is `core/src/test/resources/api/space.txt`.

### Added
- The mod: J's Space (`jsspace`) is a mod of the series, a jar of its own that requires J's Core alone, at
  the same version as every mod of the series. It adds nothing to the game yet.
- The settings of a world, `jsspace-server.toml`, none of which turns a part of the mod off: fourteen
  consequences, each on unless a server turns it off and saying what happens instead (launch damage, no launch in a
  thunderstorm, falling stages that leave craters, five failures of a rocket, cruise events, landing only once a
  world's essentials are known, dust on Mars, naming hidden worlds, debris, and the Laser Satellite as a weapon), and
  two rules of balance that start off, fast travel and J's Industrial's chains as the only path. They are on J's
  Core's settings screen, in English and Brazilian Portuguese; each is read by what it governs as that is built.
- Its mark and its tab: J's Space's logo, the word in the white of the Apollo rockets with a red livery band
  and a Saturn V beside it, and its square icon, in the mods list and the README; its creative tab, with the Saturn V
  on its button, shown once it holds a thing; and the night sky its advancement tabs will stand on, the Milky Way
  across a 400 by 200 sky drawn once and wide, with a 16-pixel tile of it.
- The Mission Handbook, the mod's own manual and the first thing in its tab, dressed as a crew's flight
  documents were: an off-white ring binder with the mission's emblem and its typed lines on the cover, cream pages
  typed in capitals, a tab at the edge for each section (Launch Operations, Flight, Suits and World Data) in its
  colour and lettered with its code, and the pages numbered by section (LO 2-3). It is written in Markdown, in English
  and Brazilian Portuguese, and its chapter is in the Technical Reference too. For now it holds the page that says
  how to read it; the other sections show as their pages are written.

## [0.5.2a] - 2026-10-14

Codename: Boron.

## [0.5.1a] - 2026-10-14

Codename: Boron.

## [0.5.0a] - 2026-10-13 - The Start Me Up Update

Codename: Boron.

### Added
- Design: The design of J's Space, in `space/docs/design`, before any of its code: one page per subject, from
  the universe, its twenty-six star systems and every world to minerals, travel, propellants, rockets, energy, life
  support, satellites, exploration, starships, ruins and Enigmatic sites, mining, cargo, terraforming and
  megastructures, and what it does with J's Computers; each page says how its system works, and the first page gives
  the order of building. J's Core's design follows it where they meet: its basic cable, pipes and battery, which any
  mod uses on its own, its containment of exotic matter, the dead stars that give exotic matter, and the space module
  of the closed exo-suits.
