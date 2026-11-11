---
section: autocrafting
order: 150
icon: jsc:crafting_table_card
---
# The Workshop and the personal-use cards

A Personal Computer can do more than run programs. Its Workshop program works with four personal-use cards. You put a card in the computer, and the program uses it for one kind of work: the Crafting Table Card, the Furnace Card, the Enchanting Card or the Anvil Card ([](jsc:expansion_cards)).

The Workshop has four tabs: Crafting, Furnace, Enchanting and Anvil. A tab whose card is not in the computer is dim, and it reads "no card".

## Using a card

1. Put the card in the Personal Computer.
2. Open the Workshop, and choose the tab of that card.

Table: The personal-use cards

| Card | In the Workshop | UPDATE action |
| --- | --- | --- |
| Crafting Table Card | Crafts a 3x3 grid with the game's crafting recipes. | None: a grid filled from the network is what a craft is for. |
| Furnace Card | Smelts with the game's smelting recipes, and no fuel. | SMELT: smelts an item held on the network. |
| Enchanting Card | Gives the three offers of an enchanting table, counting up to 15 bookshelves around it. | ENCHANT: enchants an item. |
| Anvil Card | Repairs, combines and renames with the game's anvil logic. | REPAIR, COMBINE and NAME. |

## How the cards work

Table: The Furnace Card by era

| Era | Speed |
| --- | --- |
| Vintage | does not smelt |
| Legacy | 2 |
| Transition | 3 |
| Standard | 4 |
| Advanced | 6 |

The speed grows with the era of the computer. Every number here is a first estimate, and it may change.

The cards' work costs the computer energy, and no lapis: smelting 2 kJ an item, enchanting 10 kJ for every level the enchanting table would take, repairing 2 kJ for every percent of durability given back, combining 10 kJ for every level the anvil would ask, and naming 5 kJ. The computer owes it and pays it off as the grid fills its store, and the cards' next paid work waits until it has. A server can make the work cost levels instead (half of what the game's own block takes, at least one), both, or nothing. An offer shows the level it needs and what it costs, and an offer that cannot be made reads "no offer".

Repair takes a material from storage. Each piece of material mends a quarter of the item's durability, and up to 4 pieces are used.

## UPDATE through the cards

An UPDATE changes an item that the network stores. It runs in three steps. First, the item leaves the storage servers for the card's drawer, as a SELECT takes it out. Then the card does its work. Last, the result goes back into the item's own server, and what has no room there stays at the card's output.

A smelt of more than one stack runs the three steps again, one stack at a time. A step that cannot be done sends the item back as it was.

An UPDATE belongs to the Personal Computer that asked for it. If that computer stops, or loses its card, the UPDATE is discarded, and what the card held is left at its output. The drawer does not survive the world being closed.

## If something goes wrong

> [!PROBLEM] The screen says "only a Personal Computer's own cards change items for the network".
> Ask from a Personal Computer that has the card.

> [!PROBLEM] The screen says "this computer has no", then the card's name.
> Put that card in the Personal Computer.

> [!PROBLEM] The screen says "somebody has to ask for it: the card works it for them, as they would at the block".
> Ask through a player: the card enchants and works the anvil as that player would.

> [!PROBLEM] The screen names the item, then says "does not smelt".
> The Furnace Card does not smelt in a Vintage computer. Use a Personal Computer from the Legacy era on.

> [!PROBLEM] The screen names the item, then says "takes no enchantment".
> Enchant an item that takes enchantments.

> [!PROBLEM] The screen names the item, then says "is not worn".
> Repair only an item that has lost durability.

> [!PROBLEM] The screen says "the network holds nothing that mends", then the item's name.
> Put a material that mends the item into storage on the network.

> [!PROBLEM] The screen says "the card could not do it to", then the item's name, then the reason: nothing to change, too expensive or too few levels.
> Check the item, and give the player who asks enough levels.

> [!PROBLEM] The screen says "the network was not holding", then the item's name.
> Put the item into storage on the network, and ask again.

> [!PROBLEM] The screen says "whoever asked for it was not here".
> Ask again while the player who asked is in the world.

> [!PROBLEM] The grid makes nothing.
> Fill the grid with a recipe that the game knows.

> [!PROBLEM] The screen says "Not enough levels for that offer".
> Give the player who asks more levels, or choose a cheaper offer.

> [!PROBLEM] The Workshop says "The computer is still drawing the energy for the last job".
> Wait while the grid fills the computer's store, or bring it more energy.

> [!SEE] jsc:expansion_cards, jsc:operations_catalogue, jsc:personal_computers, jsc:when_a_craft_goes_wrong
