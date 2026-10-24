# Recipe viewers

*Added 2026-10-21.*

JEI, EMI and REI are mods that show players every recipe in the game. The Core shows your recipes in whichever of
them is installed, from one declaration: you never write a JEI category, an EMI recipe or a REI display. None of the
three is a dependency of your mod or of the Core; each bridge loads only when its viewer is there.

What the viewers show with nothing written by you:

- **Every processing machine's recipes**, each kind on its own page with its machines beside it ([Machines](MACHINES.md)),
  and the button that moves a recipe into the open machine: JEI's "+", EMI's fill button, REI's "+".
- **Every multiblock** the Structure Projector can show, with its preview, its materials and its terminal.
- **The microblocks' rules** among the crafting recipes, one entry each going through every block.
- **The crafting recipes that ask for knowledge**, with a padlock while the player's team lacks it (in JEI and EMI).

## Any kind of recipe

A kind of recipe that is not a machine's is declared once with `RecipeViews`, from your mod's constructor, and is
shown in all three viewers alike. It may be a type of the game's recipes, each recipe in its holder:

```java
public static final RecipeViewKind<RecipeHolder<SmelterRecipe>> SMELTING = RecipeViews
        .recipes(MODID, "smelting", SMELTING_TITLE, ModRecipes.SMELTING)
        .workstations(() -> List.of(new ItemStack(ModBlocks.SMELTER)))
        .size(82, 26)
        .layout((holder, page) -> page.input(0, 4, holder.value().input())
                .arrow(26, 5, holder.value().ticks())
                .output(60, 4, holder.value().result()))
        .declare();
```

or anything else your mod lists, each a value of a class of yours:

```java
public static final RecipeViewKind<Fuel> FUELS = RecipeViews.of(MODID, "fuels", FUELS_TITLE, Fuel.class)
        .recipes(level -> List.of(new Fuel(new ItemStack(Items.COAL), 1600)))
        .icon(() -> new ItemStack(Items.FURNACE))
        .size(120, 32)
        .layout((fuel, page) -> page.input(0, 0, List.of(fuel.item()))
                .line(24, 5, 96, GameText.component(BURNS.with(fuel.ticks())), TEXT_COLOUR))
        .declare();
```

The title is a sentence of your mod's, declared in a `@TextHolder` class like any other. A page is laid out in pixels
from its top left, a slot's place being the top left of its frame of 18 pixels:

| On a page | What it is |
| --- | --- |
| `input(x, y, items)`, `input(x, y, ingredient)` | A slot of what the recipe takes, showing each item in turn |
| `fluidInput(x, y, fluids, capacity)` | A slot of a fluid it takes, drawn full at `capacity` |
| `catalyst(x, y, items)` | A slot of what it needs but does not use up |
| `output(x, y, item)`, `output(x, y, items)` | A slot of what it makes |
| `fluidOutput(x, y, fluid, capacity)` | A slot of a fluid it makes |
| `chance(chance)` | The last slot's output comes by that chance, from 0 to 1 |
| `tooltip(line)` | A line added to the last slot's tooltip |
| `line(x, y, width, text, colour)` | A line of text |
| `sprite(sprite, x, y, width, height)` | A sprite of the game's GUI atlas |
| `arrow(x, y, ticks)` | The arrow between, filling in `ticks`, or not filling at 0 |

`size` sets one size for every page; `sizedBy` works it out from all of the kind's recipes, as the machines' pages
are as large as their largest recipe. Each viewer draws the page with its own slots, arrow and text, and shows a
chance and a catalyst its own way. A kind's id is its category's id in every viewer, so a bookmark names the kind.

## Screens and viewers

A screen that implements `IKeepsViewersClear` returns the rectangles (in screen pixels) that must stay clear, and
every viewer keeps its lists off them. A viewer opened from a screen that is not a container's, such as a manual
page, comes back to that screen when it closes, whichever viewer it is.

`RecipeViewers.showRecipesMadeIn(machineStack)` opens whichever viewer is installed on the recipes a machine works, as
its workstation: JEI on what the machine is the catalyst of, EMI and REI on its uses, its own recipes first. Every
machine's screen does it from its progress arrow (*added 2026-10-24*).

## What can go wrong

- **A kind has no page.** It was declared after the game registered its things, or its recipes come from a level
  the viewer had none of yet: the viewers read the recipes the player's game was sent as it joined the world.
- **Two kinds share an id.** The game stops loading with a paragraph naming the kind; give each its own name.
- **A slot shows nothing.** It holds no item and no fluid: an ingredient that matches nothing, or an empty stack.
