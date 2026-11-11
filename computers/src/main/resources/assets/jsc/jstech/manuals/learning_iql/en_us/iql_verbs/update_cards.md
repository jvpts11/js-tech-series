---
section: iql_verbs
order: 370
---
# UPDATE with the cards

UPDATE changes an item the network holds. A card does the work: it sits in a Personal Computer ([](jsc:personal_computers)), and you run the statement from that computer. The cards are explained on their own page ([](jsc:workshop)).

Only the Personal Computer's own cards change items for the network. A Furnace Card smelts, an Enchanting Card enchants and an Anvil Card repairs, combines and names.

```iql name.iql
UPDATE diamond_sword SET NAME 'Old Faithful'
```

Table: The five actions of UPDATE

| Action | Card, and what it needs |
| --- | --- |
| `SMELT` | Furnace Card, for an item that smelts, with no experience cost and no WITH. |
| `ENCHANT` | Enchanting Card, for an item that takes an enchantment, paid with experience, and OFFER picks one of the offers. |
| `REPAIR` | Anvil Card, for a damaged item, paid with experience, and WITH may name the material. |
| `COMBINE` | Anvil Card, paid with experience, and WITH names the item it combines with. |
| `NAME` | Anvil Card, paid with experience, for a name in quotes of at most 50 characters. |

A paid action takes the experience of the player who sent the statement, so somebody has to send it. The Crafting Table Card has no UPDATE action: a grid filled from the network is what CRAFT is for ([](jsc:iql_craft)).

## ENCHANT and its offers

Without OFFER, ENCHANT lists the three offers and changes nothing. Each offer shows a clue, the level it needs and the levels it costs. Type OFFER with 1, 2 or 3 to take one.

```iql enchant.iql
UPDATE diamond_sword SET ENCHANT OFFER 2
```

## Which copy, and how many

An item can exist in several copies, with different damage, enchantments or names. With no ORDER BY, REPAIR picks the most worn copy, and the other actions pick the copy the network holds most of. With no quantity, UPDATE changes one item.

REPAIR without WITH uses the material the network holds most of.

1. Put the card in the Personal Computer that runs the statement.
2. Type UPDATE, the item, SET and the action, such as UPDATE iron_sword SET REPAIR.
3. Press [[Enter]], then read the reply.

Table: The replies of UPDATE

| Reply | What it means |
| --- | --- |
| `UPDATE queued: <quantity> <item>, SET <ACTION>` | The change has started. |
| `The offers for the <item>:` | The answer to ENCHANT without OFFER. One line follows for each offer. |
| `the network holds no <item>` | No copy of the item is on the network. |
| `could not start the UPDATE` | The network did not start the change. |

## The power of a rack's bays

The power table takes an UPDATE of its own: the bays of the racks with a PDU. SET on = no switches off every bay its WHERE keeps, and SET on = yes switches them on, as their PWR switches would; it needs no card. A job running it turns servers on and off by rule. A server the rack has no energy for, or whose supply failed, does not start, and the reply counts it.

```iql power.iql
UPDATE power SET on = no WHERE server = 'VAULT-01'
```

## The speed of Create's machines

With Create installed, the create_kinetics table takes an UPDATE too ([](jsc:create)). SET target_speed = 64 aims every Rotation Speed Controller its WHERE keeps at 64 RPM, and a minus sign turns it the other way. A block it keeps that is no Rotation Speed Controller, such as a motor, is left as it is, and the reply counts it.

```iql kinetics.iql
UPDATE create_kinetics SET target_speed = 64
```

## If something goes wrong

> [!PROBLEM] this computer has no <card name>
> Put that card into this Personal Computer, then run the statement again.

> [!PROBLEM] only a Personal Computer's own cards change items for the network
> Run the statement on a Personal Computer that holds the card.

> [!PROBLEM] somebody has to ask for it: the card works it for them, as they would at the block
> Run the statement from a player's terminal, so that the card works for that player.

> [!PROBLEM] the <item> does not smelt
> Pick an item that smelts.

> [!PROBLEM] the <item> takes no enchantment
> Pick an item that takes an enchantment.

> [!PROBLEM] the <item> is not worn
> REPAIR needs a damaged copy, so pick an item that is worn.

> [!PROBLEM] the network holds nothing that mends the <item>
> Put a material on the network, or name one after WITH.

> [!PROBLEM] COMBINE needs WITH and the item it adds
> Add WITH and the second item.

> [!PROBLEM] a name is at most 50 characters long
> Shorten the name to 50 characters or fewer.

> [!PROBLEM] unknown action: <word> (expected SMELT, ENCHANT, REPAIR, COMBINE or NAME)
> Type one of the five action words.

> [!PROBLEM] no bay of a rack with a PDU on this network is so
> Mount a PDU in the rack, and check the WHERE with QUERY power first.

> [!PROBLEM] no block of Create's this network's computers read is so
> Check the WHERE with QUERY create_kinetics first.

> [!SEE] jsc:workshop, jsc:clauses_priority_if_with, jsc:personal_computers, jsc:rack_equipment
