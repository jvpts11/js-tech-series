# AI contribution policy

Contributions made with the help of artificial intelligence are welcome in the J's Tech Series, on the
following conditions. They exist to keep the code from becoming a mess, to keep pull requests reviewable,
and to keep the people who work here respected. A pull request that does not meet them is rejected,
whatever it contains.

**These rules apply to the author of the mod as well.** They are not a bar set for other people. Every
line written with an agent in this repository, including the author's own, is read and verified by a human
before it lands, and the author is held to rules 2, 4, 7, 11 and 13 exactly like anybody else.

## 1. One agent at a time, no agent teams

Teams of agents produce slop: bad, broken code that looks finished. That is not a guess; it is what
happens in production with this kind of workflow. A contributor may use a single agent at a time. Running
several agents in parallel, or having one agent orchestrate others, is not allowed, both because it is
unproductive and because it strongly tends to produce bad code.

## 2. Every change is verified by a human, and the pull request proves it

Every pull request must state:

1. which agent was used, and which tool it ran in;
2. every file the change touched, created or deleted;
3. a precise explanation of what was changed and why.

A pull request without this is rejected regardless of its content. This is how the mods of the J's Tech
Series make sure the code is sound and does not absorb the structural problems of somebody else's mistakes
or lack of attention.

## 3. Write for the humans who work here

There are human contributors here. Do not build features, improvements or anything else that an ordinary
person could not read in an IDE such as IntelliJ or Eclipse. There are people here who truly love writing
code by hand, and changes that ignore them are, most of the time, completely unmaintainable. This applies
equally to contributors who use these tools and to contributors who do not: what matters is that everyone
here works with mutual respect. Handmade code can be bad, and that is fine, everyone makes mistakes. What
is not allowed is disrespect towards the people who actually tried that way.

## 4. Vibe coding is strictly forbidden

If you contribute a feature and do not understand what your agent did, do not spend time writing a pull
request; that is better than spending the mod author's time and the contributors' time. If you know nothing
about programming and still want to contribute, you can: art, translation, sound and the other parts that
need no code are open to you, if you feel comfortable with them. Otherwise there are many online courses,
some of them free, that teach Java. Until then, we ask you, sincerely, not to vibe code in this project, out
of respect for everyone who is here.

## 5. AI-generated assets are temporary

An asset of any kind (a texture, a model, an animation, a sound, anything) made with an AI may be used, but only
as a temporary asset. It is there to help the mods be built and played while they are being built, not to stay:
every AI-generated asset is meant to be replaced, sooner or later, by one a person made. It is registered as
AI-generated (rule 5b), so everybody knows which assets are still waiting for that.

A hand-made replacement for an AI-generated asset is always welcome, and it wins. Prefer making your asset by
hand from the start, even if an AI guides you through a proper tool: Photoshop, GIMP if you want open source,
Blender, Blockbench. What matters is preference for, and respect towards, the real artists who contribute to
this project and to so many others in this game. If you need an artist, try looking for someone around you who
can make the assets you need; it helps everyone, you, the artist and the people who contribute here.

### 5a. What a mod's data generation writes is code, not art

The JSON a mod's data generation writes from the declarations in its code (block states, block and item models,
the language files, loot tables, the colour palettes) is not an asset anybody drew. It is the output of the
code, in the same family as the datagen every NeoForge mod ships, it changes when the code does, and it is
registered as datagen.

This covers the mod's own data generation and nothing else. A script that draws the pixels of a texture or builds
a model is not datagen: when an AI wrote it or made what it produced, what it produced is an AI-generated asset
under rule 5, however deterministic the script is. The textures, icons, logos, models and animations the series
ships today were made that way, and they are registered as AI-generated: they are temporary.

### 5b. Every asset is in the asset registry

[ASSET_REGISTRY.md](ASSET_REGISTRY.md) lists every asset of the series: where it is in the repository, what it
is, where and how the mods use it, whether it is AI-generated, and, when it is not, who made it or, for a
third-party asset, its licence and source.

A pull request that adds, replaces or removes an asset updates the registry in the same pull request. A pull
request with an asset the registry does not account for, or with an entry that does not say where its asset came
from and who made it, is rejected immediately, whatever else it contains. The build checks this too: a test
fails on any asset no entry covers.

## 6. No third-party assets

If you contribute a texture, a model or a sound that is not AI-generated, it has to be yours. Do not bring
assets from other mods, from other games or from anywhere else. A well-known mod went through exactly this,
with assets taken from other mods and things that could not fit its licence, and we are not going through it
because of somebody's idiocy. The test is simple, and it is the one that matters: everything that enters
this repository has to be something we can ship under the mod's licence.

### 6a. Licensed third-party assets, on four conditions

There is one narrow way a third-party asset belongs here, and it exists because the series uses it. An asset
you did not make is allowed only when all four of these hold:

1. its licence explicitly permits redistribution;
2. that licence is compatible with the licence of the mod shipping it;
3. the licence text and the attribution travel with the file, in the repository and in the built jar, and its
   entry in the asset registry names both;
4. the user can replace it, through a config entry, with a file of their own.

The reference case is the DOOM addon, which ships Freedoom (released under the modified BSD licence
precisely so that people can reuse it) with a TOML entry letting a player point at a WAD they own instead.
No commercial asset is ever distributed by this project; if a player wants one, the player supplies it.

Four conditions, all of them, or the answer is no. A sprite pulled out of another mod fails every one of
them, and that is the case rule 6 exists to kill. Convenience is not a fifth condition, and neither is "the
author probably would not mind".

## 7. No agentic loops, no goals

Loops are absolutely forbidden here. People leave an agent looping for days on end and the mess it made is
discovered at the worst possible moment. Every output and every action of an agent has to be verified by the
person running it, as it happens. "Goals" and any other feature that lets an agent run on its own towards
something are forbidden for the same reason: nobody here is going to spend an afternoon tracing code that
appeared out of nowhere. It is also how we make sure that whoever contributes knows what the code is doing.

## 8. No workflows, no sub-agents

Do not use workflows or sub-agents unless the task is intensively mechanical: renames, comment adjustments,
things of that kind that can be automated safely. For anything else both produce slop of the worst kind and
cost us time and patience. Keep them out of your contribution process, even if that costs you more tokens or
a little more time checking by hand, in your IDE, what your agent did. It is far better for everyone.

## 9. Disclosure goes in the pull request, never in the commit

The pull request says which agent and which tool were used (rule 2). Commits carry only the human author:
no AI trailers, no co-author lines, no traces of the process in the history.

## 10. Small, focused pull requests

An agent makes a huge diff cheap to produce and expensive to review. One feature or one fix per pull
request, of a size a person can review in one sitting. A pull request that mixes several things, or that is
too large to read, is sent back to be split.

## 11. Be ready to explain any part of it

A reviewer may ask you to explain, or to reproduce, any part of your pull request. If you cannot explain a
change, rule 4 applies to it.

## 12. AI-written text is reviewed too

Pull request descriptions, issues and documentation written with an AI are read and corrected by you before
they are sent. Do not paste a wall of generated text and expect somebody else to find the meaning in it.

## 13. Provenance and licence

By contributing you guarantee that the code you submit does not reproduce third-party code the model may
have memorised, and that you can license it under the mod's licence, LGPL-3.0-only.

## 14. Agent files and generated documents stay out of the repository

Your agent's configuration and instruction files never enter the repository; the ignore rules already keep
the known ones out, and you keep the rest out. The same goes for documents your agent generated: plans,
notes, summaries, reports. They pollute the repository and help nobody. Keep them in a local folder that
does not go to git.
