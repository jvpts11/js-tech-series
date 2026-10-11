/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Pages of the language chapter of the Learning IQL book: the verbs and their clauses, the settings that reach buses,
 * interfaces and redstone interfaces, saved views, procedures and jobs, the limits, and every refusal the language
 * gives.
 */
final class IqlLanguagePages {

    private IqlLanguagePages() {
    }

    /** Declares the sections and entries of the language chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        verbs(guide, chapter);
        settings(guide, chapter);
        savedObjects(guide, chapter);
        limitsAndErrors(guide, chapter);
    }

    private static void verbs(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("iql_verbs").titled("The verbs").in(chapter).register();
        namesQuotesColumns(guide, section);
        queryAndSelect(guide, section);
        count(guide, section);
        insertDeleteMoveDrop(guide, section);
        craft(guide, section);
        updateCards(guide, section);
        lockUnlock(guide, section);
        maintenance(guide, section);
        clausesPriorityIfWith(guide, section);
    }

    private static void namesQuotesColumns(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_names_quotes_columns", section).titled("Names, quotes and columns")
                .paragraph("Every statement starts with a verb: the word for what the statement does, such as"
                        + " SELECT or QUERY. The verb is followed by the item or the object the statement works on."
                        + " Clause words come after that. Each one adds a part: WHERE picks rows, ORDER BY sorts"
                        + " them and LIMIT cuts them short.")
                .paragraph("Verbs and clause words do not care about capitals, so query items and QUERY Items mean"
                        + " the same thing. A name with no colon is a vanilla item: cobblestone means"
                        + " minecraft:cobblestone. Read the three kinds of text below, because IQL reads each kind"
                        + " in its own way.")
                .table("The three kinds of text", "Kind", "How you write it")
                .row(GuideCell.words("Word"), GuideCell.words("Any run of characters with no space, bracket,"
                        + " quote or comparison sign, such as iron_ingot or qty."))
                .row(GuideCell.words("Number"), GuideCell.words("A whole number, such as 64. A number with a"
                        + " percent sign is a percentage."))
                .row(GuideCell.words("Quoted text"), GuideCell.words("Text in single or double quotes, such as"
                        + " 'Old Faithful'. It may hold spaces."))
                .paragraph("A sign such as = or < joins to a word that has no space around it. So qty<100 reads as"
                        + " three parts: qty, < and 100. Put a space on each side of a sign, and you avoid the"
                        + " problem.")
                .subheading("Where each kind is accepted")
                .table("What each place in a statement takes", "Where it stands in the statement",
                        "What IQL takes there")
                .row(GuideCell.words("The item after the verb"), GuideCell.words("A plain word. Quoted text is"
                        + " refused."))
                .row(GuideCell.words("The name after FROM or TO"), GuideCell.words("A plain word, with no space."))
                .row(GuideCell.words("The name of a bus, interface or router after SET or RENAME"),
                        GuideCell.words("A word or quoted text, so 'Ore in' works."))
                .row(GuideCell.words("The name of an anvil after SET NAME"), GuideCell.words("Quoted text, at"
                        + " most 50 characters."))
                .row(GuideCell.words("The value in WHERE or IF"), GuideCell.words("A word, a number or quoted text."))
                .row(GuideCell.words("The field name in WHERE or IF"), GuideCell.words("A plain word."))
                .row(GuideCell.words("The name of a saved statement, after CREATE or DROP"),
                        GuideCell.words("A plain word, at most 64 characters."))
                .paragraph("Item names have no spaces, so an item is always a plain word. A bus name can have"
                        + " spaces, so you put it in quotes after SET: SET BUS 'Ore in' ON.")
                .subheading("Columns and fields")
                .paragraph("A column is one field that a QUERY table shows. You can name the same field in WHERE"
                        + " and in ORDER BY. These are the tables and their columns:")
                .table("The tables and their columns", "Table", "Columns")
                .row(GuideCell.code("items"), GuideCell.words("item, qty, server, tag, enchant"))
                .row(GuideCell.code("servers"), GuideCell.words("name, used, free, era"))
                .row(GuideCell.code("disks"), GuideCell.words("disk, server, tier, size, used"))
                .row(GuideCell.code("operations"), GuideCell.words("id, verb, state, item, moved, requested,"
                        + " priority"))
                .row(GuideCell.code("recipes"), GuideCell.words("recipe, where, machine, chance"))
                .row(GuideCell.code("computers"), GuideCell.words("name, kind, era, state, os"))
                .paragraph("An item has more fields than its table shows. A WHERE clause can compare any of these:")
                .table("Fields of an item", "Field", "What it holds")
                .row(GuideCell.code("item"), GuideCell.words("The item's internal name, without its namespace, such as"
                        + " iron_ingot."))
                .row(GuideCell.code("name"), GuideCell.words("The display name the game shows."))
                .row(GuideCell.code("qty"), GuideCell.words("The quantity. count and amount mean the same."))
                .row(GuideCell.code("server"), GuideCell.words("The server the row belongs to."))
                .row(GuideCell.code("damaged"), GuideCell.words("true or false."))
                .row(GuideCell.code("durability"), GuideCell.words("The durability as a percent, from 0 to 100."
                        + " An item that cannot wear out gives 100."))
                .row(GuideCell.code("tag"), GuideCell.words("The first tag of the item."))
                .row(GuideCell.code("enchant"), GuideCell.words("The enchantments, written as text."))
                .paragraph("A comment starts with two dashes, --, and runs to the end of the line. A line that starts"
                        + " with the dashes does nothing when a file runs. The Management Studio can also ask you for"
                        + " a value when a script runs: its templates are in ([](jsc:studio_templates)).")
                .define("verb", "The word that starts a statement and says what it does, such as SELECT or QUERY.")
                .define("clause", "A word that adds a part to a statement, such as WHERE, ORDER BY or LIMIT. Clauses"
                        + " come after the item.")
                .define("column", "One field of a table that a QUERY shows. The same names work in WHERE and in"
                        + " ORDER BY.")
                .ifSomethingGoesWrong("expected an item, got: 'Old Faithful'",
                        "Item ids have no spaces. Type the item's id as one plain word, with no quotes.",
                        "expected a location after FROM, got: Ore in",
                        "Type the name as one word, with no space, after FROM or TO.",
                        "A row you expect is missing from the answer.",
                        "A field you typed does not exist in the table, so its comparison is false. Check the"
                                + " spelling against the tables above.")
                .seeAlso("jsc:query_and_select", "jsc:clauses_priority_if_with")
                .register();
    }

    private static void queryAndSelect(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("query_and_select", section).titled("QUERY and SELECT")
                .paragraph("QUERY reads the network and shows you what it holds. It moves nothing. SELECT is an"
                        + " Operation that brings items out of the network into this computer's own storage, its"
                        + " [local storage](jsc:local_storage). Both take WHERE, ORDER BY and LIMIT, which choose the"
                        + " rows you work on.")
                .subheading("QUERY")
                .paragraph("Name the object after QUERY: the table you want to read. You can also write an item, or a"
                        + " star for every item.")
                .table("Objects you can query", "Object", "What the rows are")
                .row(GuideCell.code("items, or *"), GuideCell.words("Every item the network holds."))
                .row(GuideCell.code("servers"), GuideCell.words("Every server on the network."))
                .row(GuideCell.code("operations"), GuideCell.words("The Operations that run or wait."))
                .row(GuideCell.code("computers"), GuideCell.words("The computers on the network."))
                .row(GuideCell.code("recipes"), GuideCell.words("The recipes the network knows."))
                .row(GuideCell.code("disks"), GuideCell.words("The disks of the network."))
                .paragraph("Any other name gives 0 rows and no error. After a QUERY, the reply counts the rows, such as"
                        + " \"12 rows\" or \"1 row\". To read the rows themselves, open the table view in the"
                        + " Management Studio ([](jsc:studio_queries)).")
                .listingIn("iql", "stock.iql", "QUERY items WHERE qty > 100 ORDER BY qty DESC LIMIT 5")
                .subheading("SELECT")
                .paragraph("SELECT takes items out of the network. You type a quantity, the item and, if you want, a"
                        + " server. A quantity of ALL, or no quantity at all, asks for everything the network can"
                        + " give. SELECT 64 iron_ingot and SELECT ALL iron_ingot do the same thing.")
                .steps("Type SELECT, a quantity and the item, such as SELECT 64 iron_ingot.",
                        "To take from one server only, add FROM and the server's name as one word.",
                        "Press [[Enter]], then read the reply.")
                .table("The replies of SELECT", "Reply", "What it means")
                .row(GuideCell.code("SELECT queued: <quantity> <item> -> local storage"),
                        GuideCell.words("The items are on their way to this computer."))
                .row(GuideCell.code("SELECT queued: <quantity> <item> from <server> -> local storage"),
                        GuideCell.words("The same, taken from that one server."))
                .row(GuideCell.code("nothing to select"), GuideCell.words("You typed SELECT *, and the network"
                        + " holds no item."))
                .row(GuideCell.code("no server named '<server>'"), GuideCell.words("No server has that name."
                        + " Check its spelling."))
                .paragraph("A star asks for every item type at once: SELECT * queues one move for each item type the"
                        + " network holds, up to 256 types. The reply then says \"12 item types\" or \"1 item type\".")
                .subheading("WHERE: choosing rows")
                .paragraph("WHERE keeps only the rows that match a condition. A condition compares a field with a"
                        + " value, with an operator between them. qty > 100 keeps the rows whose quantity is over"
                        + " 100.")
                .table("Operators", "Operator", "What it tests")
                .row(GuideCell.code("=  or  =="), GuideCell.words("Equal. Numbers compare as numbers; words compare"
                        + " without regard to capitals."))
                .row(GuideCell.code("!="), GuideCell.words("Not equal."))
                .row(GuideCell.code("<  >  <=  >="), GuideCell.words("Less than, more than, at most, at least."))
                .row(GuideCell.code("contains"), GuideCell.words("The text holds the word, without regard to"
                        + " capitals."))
                .row(GuideCell.code("has"), GuideCell.words("The same test as contains."))
                .row(GuideCell.code("like"), GuideCell.words("A pattern, where * stands for any run of characters."))
                .paragraph("Join conditions with AND, OR and NOT, and use brackets to group them. NOT binds tightest,"
                        + " then AND, then OR. A condition can have at most 64 levels of NOT and brackets, and at most"
                        + " 256 comparisons. A condition ends at the first word it cannot use, such as IF, ORDER or"
                        + " LIMIT.")
                .listingIn("iql", "rare.iql", "QUERY items WHERE name contains \"rare\" AND NOT damaged = true")
                .subheading("ORDER BY and LIMIT")
                .paragraph("ORDER BY sorts the rows by one field: smallest first, or A to Z for words. ASC is the"
                        + " default, and DESC sorts the other way. LIMIT keeps only the first rows. It takes a row"
                        + " count, not a percentage.")
                .table("Sorting a QUERY", "Field", "How the rows are sorted")
                .row(GuideCell.code("qty, count, amount, quantity"), GuideCell.words("By number."))
                .row(GuideCell.code("name, item"), GuideCell.words("By word, without regard to capitals."))
                .row(GuideCell.code("server"), GuideCell.words("By word."))
                .row(GuideCell.words("Any other field"), GuideCell.words("Not sorted: the order stays as it was."))
                .paragraph("With ORDER BY and LIMIT together, the rows are sorted first, and LIMIT keeps the first ones"
                        + " of the sorted list. By default a QUERY reads up to 4,096 rows before it sorts them.")
                .paragraph("For SELECT and the other actions, WHERE, ORDER BY and LIMIT choose which item types the"
                        + " statement works on. The sort uses the item fields listed above, and LIMIT then takes the"
                        + " first item types.")
                .ifSomethingGoesWrong("FROM is not valid on a read",
                        "A QUERY has no FROM. Move the server into WHERE, as in QUERY items WHERE server ="
                                + " \"Main Storage\".",
                        "unknown item: <name>, for a SELECT that names an item the network holds",
                        "The WHERE kept no row for the item, so the reply says unknown item even though the item"
                                + " exists. Check the WHERE, the server after FROM and the stock with QUERY items.",
                        "expected a field name",
                        "Put a field name before the operator, such as qty.",
                        "unknown operator: <text>",
                        "Use one of the operators in the table above.",
                        "no server named '<server>'",
                        "Check the server's name: QUERY servers lists them.")
                .seeAlso("jsc:iql_names_quotes_columns", "jsc:iql_count", "jsc:iql_limits")
                .register();
    }

    private static void count(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_count", section).titled("COUNT")
                .paragraph("COUNT is written like a query, but it does not add up the quantities. It works the same as"
                        + " QUERY: it lists the rows that match, and the reply tells you how many rows there are.")
                .listingIn("iql", "count.iql", "COUNT items WHERE qty > 100")
                .paragraph("So the reply is a row count, such as \"12 rows\". To see each row's quantity, read the qty"
                        + " column of a QUERY items, or open the table view in the Management Studio"
                        + " ([](jsc:studio_queries)).")
                .paragraph("COUNT reads an IF clause, but it ignores it. Your rows are listed whatever the IF says.")
                .ifSomethingGoesWrong("expected an item",
                        "COUNT needs an object or an item after it, such as COUNT items.",
                        "<file>: QUERY/COUNT are not supported by 'run', use 'operation' instead",
                        "A file that runs at the prompt refuses QUERY and COUNT. Type the read at the prompt, one"
                                + " statement at a time.")
                .seeAlso("jsc:query_and_select")
                .register();
    }

    private static void insertDeleteMoveDrop(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("insert_delete_move_drop", section).titled("INSERT, DELETE, MOVE and DROP")
                .paragraph("These four verbs move items or destroy them. INSERT brings items into the network. DELETE"
                        + " sends them out of the network. MOVE carries them from one place to another. DROP destroys"
                        + " them. Where you type a quantity, it is a number, ALL, or nothing.")
                .paragraph("A bus is a block that moves items between the network and a chest or machine beside it."
                        + " That chest or machine is the bus's external inventory ([](jsc:buses)).")
                .table("The four item verbs", "Verb", "Form and what it does")
                .row(GuideCell.code("INSERT"), GuideCell.words("INSERT [quantity] item [FROM bus]. Brings the items"
                        + " this computer holds into the network, or imports them from a bus."))
                .row(GuideCell.code("DELETE"), GuideCell.words("DELETE [quantity] item TO bus. Sends the items the"
                        + " network holds out to a bus."))
                .row(GuideCell.code("MOVE"), GuideCell.words("MOVE [quantity] item FROM source TO destination."
                        + " Carries items between two servers, or to and from a bus."))
                .row(GuideCell.code("DROP"), GuideCell.words("DROP [quantity] item. Destroys the items the network"
                        + " holds."))
                .subheading("INSERT")
                .paragraph("Without FROM, INSERT pushes what this computer holds into the network. With FROM and the"
                        + " name of a bus, it imports from that bus instead. Whatever the network cannot keep goes"
                        + " back to the bus.")
                .table("The replies of INSERT", "Reply", "What it means")
                .row(GuideCell.code("INSERT queued: <quantity> <item> -> network"),
                        GuideCell.words("The items are on their way into the network."))
                .row(GuideCell.code("nothing to push"), GuideCell.words("There is nothing for INSERT to push."))
                .row(GuideCell.code("this computer holds no <item>"), GuideCell.words("This computer has none of"
                        + " the item. Put it into this computer first."))
                .row(GuideCell.code("MOVE queued: import from <bus>"), GuideCell.words("The import has started. The"
                        + " reply says MOVE, although you typed INSERT: that is the network's own wording."))
                .row(GuideCell.code("nothing to import from <bus>"), GuideCell.words("The bus holds nothing to"
                        + " import."))
                .row(GuideCell.code("the bus '<bus>' touches no inventory"), GuideCell.words("The bus has no chest"
                        + " or machine beside it to take from."))
                .subheading("DELETE")
                .paragraph("DELETE sends items out of the network. It needs TO and the name of a bus, and a server name"
                        + " is not valid there. Only items the network holds are sent, so a second DELETE does nothing"
                        + " once the stock runs out. FROM is read, but DELETE does not use it.")
                .table("The replies of DELETE", "Reply", "What it means")
                .row(GuideCell.code("DELETE queued: <quantity> <item>"), GuideCell.words("The items are on their way"
                        + " out to the bus."))
                .row(GuideCell.code("nothing to delete"), GuideCell.words("The network holds none of the item."))
                .row(GuideCell.code("no bus named '<bus>'"), GuideCell.words("No bus has that name on the network."
                        + " Check the name in the bus's window."))
                .subheading("MOVE")
                .paragraph("MOVE needs both FROM and TO. Between two servers, it moves items inside the network. A bus"
                        + " after TO sends items out, and a bus after FROM brings them in.")
                .table("The replies of MOVE", "Reply", "What it means")
                .row(GuideCell.code("MOVE queued: <quantity> <item> -> <bus>"), GuideCell.words("The items are on"
                        + " their way to that bus."))
                .row(GuideCell.code("nothing to move"), GuideCell.words("The source holds none of the item."))
                .row(GuideCell.code("nothing to move to <bus>"), GuideCell.words("The source holds nothing to send"
                        + " to that bus."))
                .row(GuideCell.code("no server or bus named '<name>'"), GuideCell.words("Neither a server nor a bus"
                        + " has that name."))
                .row(GuideCell.code("the destination server is unavailable"), GuideCell.words("The destination cannot"
                        + " take items now."))
                .row(GuideCell.code("could not start the MOVE"), GuideCell.words("The network did not start the"
                        + " move."))
                .subheading("DROP")
                .warning("A DROP destroys the items for good. They leave the network and are not kept.")
                .paragraph("DROP takes no TO. It destroys only the items the network holds, so you cannot drop what"
                        + " you do not have on the network. The Management Studio asks you to confirm a DROP, and a"
                        + " DELETE with ALL, before it runs them.")
                .table("The replies of DROP", "Reply", "What it means")
                .row(GuideCell.code("DROP queued: <quantity> <item>"), GuideCell.words("The items are being "
                        + "destroyed."))
                .row(GuideCell.code("nothing to drop"), GuideCell.words("The network holds none of the item."))
                .ifSomethingGoesWrong("DELETE exports out of the network and needs a TO destination",
                        "Add TO and the name of a bus.",
                        "MOVE is internal and needs both FROM and TO",
                        "Name the place to take from after FROM, and the place to put the items in after TO.",
                        "DROP has no TO destination",
                        "Remove the TO. DROP only destroys items.",
                        "expected a location after FROM, got: <text>",
                        "Type the name as one word, with no space.")
                .seeAlso("jsc:buses", "jsc:set_bus", "jsc:query_and_select")
                .register();
    }

    private static void craft(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_craft", section).titled("CRAFT")
                .paragraph("CRAFT asks the network to make items from its recipes. You do not make the items by hand."
                        + " The network uses the patterns it has learned ([](jsc:patterns)) and the machines that carry"
                        + " them out.")
                .listingIn("iql", "torches.iql", "CRAFT 64 torch")
                .paragraph("CRAFT needs an engine running on the Mainframe, because an engine works out how to make the"
                        + " craft. Without one, the network refuses the statement. What an"
                        + " [engine](jsc:iql_engines_overview) is, and how you start one, is explained there.")
                .steps("Type CRAFT, a quantity and the item, such as CRAFT 64 torch.",
                        "Press [[Enter]], then read the reply.")
                .table("The replies of CRAFT", "Reply", "What it means")
                .row(GuideCell.code("CRAFT queued: <quantity> <item>"), GuideCell.words("The craft has started."))
                .row(GuideCell.code("Network Operations Service unavailable"), GuideCell.words("No engine runs on"
                        + " the Mainframe."))
                .row(GuideCell.code("the network has no running Mainframe"), GuideCell.words("The network has no"
                        + " Mainframe running."))
                .row(GuideCell.code("no pattern crafts <item>"), GuideCell.words("No recipe on the network makes"
                        + " the item."))
                .row(GuideCell.code("unknown item: <name>"), GuideCell.words("The name is not an item."))
                .paragraph("With no quantity, the reply says ALL. PRIORITY works on a CRAFT as it does on the other"
                        + " actions ([](jsc:clauses_priority_if_with)). FROM and TO are read, but CRAFT does not use"
                        + " them. The recipes are the patterns the network has learned, such as those a Crafting"
                        + " Interface holds ([](jsc:crafting_interfaces)).")
                .ifSomethingGoesWrong("Network Operations Service unavailable",
                        "Start an engine on the Mainframe ([](jsc:iql_engines_overview)).",
                        "no pattern crafts <item>",
                        "Teach the network a recipe with a Pattern Encoder ([](jsc:pattern_encoders)), then craft"
                                + " again.",
                        "unknown item: <name>",
                        "Check the spelling, and use the item's id, such as iron_ingot.")
                .seeAlso("jsc:asking_for_a_craft", "jsc:crafting_interfaces")
                .register();
    }

    private static void updateCards(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("update_cards", section).titled("UPDATE with the cards")
                .paragraph("UPDATE changes an item the network holds. A card does the work: it sits in a Personal"
                        + " Computer ([](jsc:personal_computers)), and you run the statement from that computer. The"
                        + " cards are explained on their own page ([](jsc:workshop)).")
                .paragraph("Only the Personal Computer's own cards change items for the network. A Furnace Card"
                        + " smelts, an Enchanting Card enchants and an Anvil Card repairs, combines and names.")
                .listingIn("iql", "name.iql", "UPDATE diamond_sword SET NAME 'Old Faithful'")
                .table("The five actions of UPDATE", "Action", "Card, and what it needs")
                .row(GuideCell.code("SMELT"), GuideCell.words("Furnace Card, for an item that smelts, with no"
                        + " experience cost and no WITH."))
                .row(GuideCell.code("ENCHANT"), GuideCell.words("Enchanting Card, for an item that takes an"
                        + " enchantment, paid with experience, and OFFER picks one of the offers."))
                .row(GuideCell.code("REPAIR"), GuideCell.words("Anvil Card, for a damaged item, paid with experience,"
                        + " and WITH may name the material."))
                .row(GuideCell.code("COMBINE"), GuideCell.words("Anvil Card, paid with experience, and WITH names the"
                        + " item it combines with."))
                .row(GuideCell.code("NAME"), GuideCell.words("Anvil Card, paid with experience, for a name in quotes"
                        + " of at most 50 characters."))
                .paragraph("A paid action takes the experience of the player who sent the statement, so somebody"
                        + " has to send it. The Crafting Table Card has no UPDATE action: a grid filled from the"
                        + " network is what CRAFT is for ([](jsc:iql_craft)).")
                .subheading("ENCHANT and its offers")
                .paragraph("Without OFFER, ENCHANT lists the three offers and changes nothing. Each offer shows a clue,"
                        + " the level it needs and the levels it costs. Type OFFER with 1, 2 or 3 to take one.")
                .listingIn("iql", "enchant.iql", "UPDATE diamond_sword SET ENCHANT OFFER 2")
                .subheading("Which copy, and how many")
                .paragraph("An item can exist in several copies, with different damage, enchantments or names. With no"
                        + " ORDER BY, REPAIR picks the most worn copy, and the other actions pick the copy the network"
                        + " holds most of. With no quantity, UPDATE changes one item.")
                .paragraph("REPAIR without WITH uses the material the network holds most of.")
                .steps("Put the card in the Personal Computer that runs the statement.",
                        "Type UPDATE, the item, SET and the action, such as UPDATE iron_sword SET REPAIR.",
                        "Press [[Enter]], then read the reply.")
                .table("The replies of UPDATE", "Reply", "What it means")
                .row(GuideCell.code("UPDATE queued: <quantity> <item>, SET <ACTION>"), GuideCell.words("The change"
                        + " has started."))
                .row(GuideCell.code("The offers for the <item>:"), GuideCell.words("The answer to ENCHANT without"
                        + " OFFER. One line follows for each offer."))
                .row(GuideCell.code("the network holds no <item>"), GuideCell.words("No copy of the item is on the"
                        + " network."))
                .row(GuideCell.code("could not start the UPDATE"), GuideCell.words("The network did not start the"
                        + " change."))
                .ifSomethingGoesWrong("this computer has no <card name>",
                        "Put that card into this Personal Computer, then run the statement again.",
                        "only a Personal Computer's own cards change items for the network",
                        "Run the statement on a Personal Computer that holds the card.",
                        "somebody has to ask for it: the card is paid for with their experience",
                        "Run the statement from a player's terminal, so that a player pays the experience.",
                        "the <item> does not smelt",
                        "Pick an item that smelts.",
                        "the <item> takes no enchantment",
                        "Pick an item that takes an enchantment.",
                        "the <item> is not worn",
                        "REPAIR needs a damaged copy, so pick an item that is worn.",
                        "the network holds nothing that mends the <item>",
                        "Put a material on the network, or name one after WITH.",
                        "COMBINE needs WITH and the item it adds",
                        "Add WITH and the second item.",
                        "a name is at most 50 characters long",
                        "Shorten the name to 50 characters or fewer.",
                        "unknown action: <word> (expected SMELT, ENCHANT, REPAIR, COMBINE or NAME)",
                        "Type one of the five action words.")
                .seeAlso("jsc:workshop", "jsc:clauses_priority_if_with", "jsc:personal_computers")
                .register();
    }

    private static void lockUnlock(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("lock_unlock", section).titled("LOCK and UNLOCK")
                .paragraph("LOCK holds items where they are. Another Operation that asks for the same items then waits,"
                        + " instead of taking them. UNLOCK lets them go again. Use LOCK to keep stock in place while"
                        + " you need it.")
                .listingIn("iql", "hold.iql", "LOCK 64 iron_ingot", "UNLOCK iron_ingot")
                .paragraph("With no quantity, or with ALL, LOCK holds every free item of that kind. UNLOCK uses only "
                        + "the"
                        + " item, so a quantity you type after UNLOCK has no effect.")
                .paragraph("LOCK and UNLOCK work with no engine. PRIORITY is accepted on them, but it changes nothing."
                        + " The Task Manager shows what is locked ([](jsc:system_monitor_and_task_manager)).")
                .table("The replies of LOCK and UNLOCK", "Reply", "What it means")
                .row(GuideCell.code("LOCK held <quantity> <item> (concurrent operations will wait)"),
                        GuideCell.words("The items are held, and other Operations wait for them."))
                .row(GuideCell.code("<item> is already locked"), GuideCell.words("You already locked this item by"
                        + " hand."))
                .row(GuideCell.code("nothing to lock: the network holds no free <item>"),
                        GuideCell.words("Every copy is in use, or none is on the network."))
                .row(GuideCell.code("UNLOCK released <quantity> <item>"), GuideCell.words("The hold is gone."))
                .row(GuideCell.code("<item> is not locked"), GuideCell.words("There was no hold to release."))
                .ifSomethingGoesWrong("<item> is already locked",
                        "The item stays held. Type UNLOCK with its name when you want it free.",
                        "nothing to lock: the network holds no free <item>",
                        "Wait until the network has free stock, then lock it again.",
                        "<item> is not locked",
                        "Check the item's name. UNLOCK only releases a hold that you put by hand.")
                .seeAlso("jsc:operation_states", "jsc:states_and_queues")
                .register();
    }

    private static void maintenance(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("maintenance_analyze_vacuum_reindex", section).titled("ANALYZE, VACUUM and REINDEX")
                .paragraph("The network keeps an index: its record of what its disks hold. ANALYZE, VACUUM and REINDEX"
                        + " look after that index. Each one may take one word after it, such as an object's name, and"
                        + " the network then ignores that word.")
                .define("index", "The network's record of what its disks hold. The maintenance verbs keep it up to"
                        + " date.")
                .paragraph("You run maintenance on the Mainframe. Typed on any other computer, it is refused. It needs"
                        + " no engine.")
                .table("The three maintenance verbs", "Verb", "What it does, and its reply")
                .row(GuideCell.code("ANALYZE"), GuideCell.words("Reconciles the index. The reply"
                        + " is \"ANALYZE complete - <n> types reconciled\", with the number of item types it holds."))
                .row(GuideCell.code("VACUUM"), GuideCell.words("Frees the ghost entries. The reply is \"VACUUM freed"
                        + " <n> ghost entries\", with the number freed."))
                .row(GuideCell.code("REINDEX"), GuideCell.words("Rebuilds the index from the disks. The reply is"
                        + " \"REINDEX started - rebuilding the catalog from disks\", and the new index replaces the"
                        + " old one a short time later."))
                .ifSomethingGoesWrong("maintenance runs on the Mainframe only",
                        "Type the command at the Mainframe itself.",
                        "the network has no running Mainframe",
                        "Put a Mainframe on the network and let it run ([](jsc:mainframes)).",
                        "unexpected token: <word>",
                        "A maintenance verb takes one word at most. Remove the extra word.")
                .seeAlso("jsc:mainframes", "jsc:iql_limits")
                .register();
    }

    private static void clausesPriorityIfWith(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("clauses_priority_if_with", section).titled("PRIORITY, IF, OFFER and WITH")
                .paragraph("The words after the item are clauses. Each verb accepts some of them. A clause that the "
                        + "verb"
                        + " does not accept is refused with a message, such as \"FROM is not valid on a read\"."
                        + " The table shows what each verb accepts.")
                .table("Clauses each verb accepts", "Verb", "Clauses it accepts")
                .row(GuideCell.code("QUERY"), GuideCell.words("WHERE, ORDER BY and LIMIT. FROM, TO, IF and PRIORITY"
                        + " are refused."))
                .row(GuideCell.code("SELECT"), GuideCell.words("FROM (optional), WHERE, IF, ORDER BY, LIMIT and"
                        + " PRIORITY. TO is refused."))
                .row(GuideCell.code("INSERT"), GuideCell.words("FROM (optional, a bus name), WHERE, IF, ORDER BY,"
                        + " LIMIT and PRIORITY. TO is refused."))
                .row(GuideCell.code("DELETE"), GuideCell.words("TO (required), WHERE, IF, ORDER BY, LIMIT and"
                        + " PRIORITY. FROM is read, but has no effect."))
                .row(GuideCell.code("MOVE"), GuideCell.words("FROM and TO (both required), WHERE, IF, ORDER BY, LIMIT"
                        + " and PRIORITY."))
                .row(GuideCell.code("DROP"), GuideCell.words("WHERE, IF, ORDER BY, LIMIT and PRIORITY, with FROM"
                        + " ignored and TO refused."))
                .row(GuideCell.code("CRAFT"), GuideCell.words("WHERE, IF, ORDER BY, LIMIT and PRIORITY. FROM and TO"
                        + " are read, but have no effect."))
                .row(GuideCell.code("COUNT"), GuideCell.words("WHERE, ORDER BY and LIMIT. IF, FROM, TO and PRIORITY"
                        + " are read, but have no effect."))
                .row(GuideCell.code("LOCK, UNLOCK"), GuideCell.words("WHERE, IF, ORDER BY, LIMIT and PRIORITY, which"
                        + " has no effect."))
                .row(GuideCell.code("UPDATE"), GuideCell.words("SET (required), FROM (optional, a server), WHERE, IF,"
                        + " ORDER BY, LIMIT and PRIORITY. TO is refused."))
                .row(GuideCell.code("ANALYZE, VACUUM, REINDEX"), GuideCell.words("No clause. One object word, which"
                        + " is ignored."))
                .subheading("PRIORITY")
                .paragraph("PRIORITY sets the level of each Operation a statement queues ([](jsc:priorities)). The"
                        + " default is MEDIUM, and you write the level after the word, such as PRIORITY HIGH.")
                .table("The five levels", "Level", "Short label in the Task Manager")
                .row(GuideCell.code("LOW"), GuideCell.code("LOW"))
                .row(GuideCell.code("MEDIUM_LOW"), GuideCell.code("MED-"))
                .row(GuideCell.code("MEDIUM"), GuideCell.words("MED, the default"))
                .row(GuideCell.code("MEDIUM_HIGH"), GuideCell.code("MED+"))
                .row(GuideCell.code("HIGH"), GuideCell.code("HIGH"))
                .paragraph("NORMAL means MEDIUM. A hyphen reads as an underscore, and capitals do not matter, so"
                        + " medium-high is MEDIUM_HIGH.")
                .subheading("IF")
                .paragraph("IF runs the statement only when a condition holds. The condition uses the words of a WHERE"
                        + " condition ([](jsc:query_and_select)). The network checks it against its stock of the named"
                        + " item before the statement runs.")
                .table("The fields an IF can check", "Field", "What the check compares")
                .row(GuideCell.code("qty, count, amount"), GuideCell.words("The total the network holds of the item,"
                        + " every copy counted."))
                .row(GuideCell.code("item"), GuideCell.words("The item's id, or * for any item."))
                .row(GuideCell.code("name"), GuideCell.words("The display name, or the item as you typed it when the"
                        + " network holds none."))
                .row(GuideCell.words("Any other field"), GuideCell.words("Never true, so the statement does not run."))
                .paragraph("When the condition does not hold, the statement does not run. The reply is not an error:"
                        + " \"the IF did not hold, so the SELECT was not run\", with the verb you typed. A file goes on"
                        + " to its next line.")
                .subheading("WITH")
                .paragraph("WITH gives UPDATE its second item: the material for REPAIR, or the item COMBINE adds. It"
                        + " comes after the action word, and no other statement takes it.")
                .subheading("OFFER")
                .paragraph("OFFER takes one of the three enchantment offers, after ENCHANT. You type 1, 2 or 3 after "
                        + "the"
                        + " word. ([](jsc:update_cards)) shows what each offer means.")
                .ifSomethingGoesWrong("<ACTION> takes no WITH",
                        "Remove WITH. SMELT, ENCHANT and NAME take none.",
                        "COMBINE needs WITH and the item it adds",
                        "Add WITH and the second item.",
                        "unexpected token: WITH",
                        "Put WITH after the action word of an UPDATE.",
                        "an offer is 1, 2 or 3, not <n>",
                        "Type 1, 2 or 3 after OFFER.",
                        "an offer after OFFER (1, 2 or 3)",
                        "Type the number of the offer after the word OFFER.",
                        "unknown priority level: <word> (expected LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH or HIGH)",
                        "Type one of the five level words.",
                        "IF is not valid on a read",
                        "A QUERY cannot have IF. Use WHERE to pick the rows instead.")
                .seeAlso("jsc:priorities", "jsc:update_cards", "jsc:iql_count")
                .register();
    }

    private static void settings(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("iql_settings").titled("Setting things").in(chapter)
                .register();
        setBus(guide, section);
        setInterface(guide, section);
        setRedstone(guide, section);
    }

    private static void setBus(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("set_bus", section).titled("SET BUS")
                .paragraph("SET BUS changes the settings of a [bus](jsc:buses) from the prompt. It does what the bus's"
                        + " window does, as one statement: switch the bus on or off, choose the items it moves, how"
                        + " many, and when ([](jsc:bus_window)).")
                .paragraph("Write the bus's name as its window shows it, in quotes when it has spaces. Each statement"
                        + " changes one setting, so you type one statement for each setting you need.")
                .listingIn("iql", "bus.iql", "SET BUS 'Ore in' ON", "SET BUS 'Ore in' KEEP 16 MAX 64")
                .table("The settings of SET BUS", "Statement", "What it does")
                .row(GuideCell.code("SET BUS 'x' ON"), GuideCell.words("Switches the bus on."))
                .row(GuideCell.code("SET BUS 'x' OFF"), GuideCell.words("Switches the bus off."))
                .row(GuideCell.code("SET BUS 'x' MODE CONTINUOUS"), GuideCell.words("The bus moves all the time."))
                .row(GuideCell.code("SET BUS 'x' MODE ON DEMAND"), GuideCell.words("The bus moves only while its"
                        + " cable carries a redstone signal."))
                .row(GuideCell.code("SET BUS 'x' FILTER ONLY a, b"), GuideCell.words("The filter lists these items."))
                .row(GuideCell.code("SET BUS 'x' FILTER ALL BUT a, b"), GuideCell.words("The filter lists every"
                        + " item except these."))
                .row(GuideCell.code("SET BUS 'x' FILTER NONE"), GuideCell.words("The filter lists no item."))
                .row(GuideCell.code("SET BUS 'x' FILTER TAG c:ores, c:logs"), GuideCell.words("The filter lists the"
                        + " items with these tags."))
                .row(GuideCell.code("SET BUS 'x' MATCH FUZZY"), GuideCell.words("A loose match: any damage or"
                        + " components."))
                .row(GuideCell.code("SET BUS 'x' MATCH EXACT"), GuideCell.words("An exact match."))
                .row(GuideCell.code("SET BUS 'x' KEEP 16 MAX 64"), GuideCell.words("Keeps 16 in the chest, and moves at"
                        + " most 64 at a time."))
                .row(GuideCell.code("SET BUS 'x' MAX 64"), GuideCell.words("Moves at most 64 at a time."))
                .row(GuideCell.code("SET BUS 'x' KEEP iron_ore 16 MAX iron_ore 64"), GuideCell.words("The same, for"
                        + " one listed item."))
                .row(GuideCell.code("SET BUS 'x' PRIORITY 5"), GuideCell.words("Which bus goes first. A whole number,"
                        + " which may be negative."))
                .row(GuideCell.code("SET BUS 'x' WHEN STOCK iron_ore < 512"), GuideCell.words("Waits while the network"
                        + " holds fewer than 512 of the item."))
                .row(GuideCell.code("SET BUS 'x' WHEN STOCK TAG c:ores < 4096"), GuideCell.words("The same, for a"
                        + " tag."))
                .row(GuideCell.code("SET BUS 'x' WHEN TIME BETWEEN 18 AND 6"), GuideCell.words("Moves only"
                        + " between these hours. The range may cross midnight."))
                .row(GuideCell.code("SET BUS 'x' AFTER BUS 'Coal in'"), GuideCell.words("Waits until the named bus"
                        + " finishes."))
                .row(GuideCell.code("SET BUS 'x' ACCESS READ AND WRITE"), GuideCell.words("The network may take from"
                        + " and put into the inventory."))
                .row(GuideCell.code("SET BUS 'x' ACCESS READ ONLY"), GuideCell.words("The network may only take from"
                        + " the inventory."))
                .row(GuideCell.code("SET BUS 'x' ACCESS WRITE ONLY"), GuideCell.words("The network may only put into"
                        + " the inventory."))
                .paragraph("The stock threshold after WHEN STOCK takes only the < sign. KEEP and MAX take a whole "
                        + "number"
                        + " that is not negative. The hours after WHEN TIME are whole numbers from 0 to 23. A list of"
                        + " items is separated by commas, and a fluid is written with its kind first, as in FLUID"
                        + " water.")
                .table("The replies of SET BUS", "Reply", "What it means")
                .row(GuideCell.code("bus <name> set"), GuideCell.words("The setting is saved."))
                .row(GuideCell.code("no bus named <name> on this network"), GuideCell.words("No bus of that name is on"
                        + " the network."))
                .row(GuideCell.code("the bus <name> cannot be set so: its era or its kind has no such setting"),
                        GuideCell.words("This bus does not offer that setting."))
                .row(GuideCell.code("<item> is not in the filter of the bus <name>"), GuideCell.words("The item must"
                        + " be one the bus's filter lists."))
                .row(GuideCell.code("no item or fluid named <item>"), GuideCell.words("The name is not an item or a"
                        + " fluid."))
                .ifSomethingGoesWrong("SET BUS needs the bus's name, in quotes when it has spaces",
                        "Type the name after BUS, and quote it when it has spaces.",
                        "The reply says \"SET BUS needs a setting: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY,"
                                + " WHEN, AFTER BUS or ACCESS\".",
                        "Add one setting word after the name.",
                        "a bus has no setting <word>",
                        "Use one of the settings in the table above.",
                        "unexpected token: <word>",
                        "A statement has one setting. Type the next setting as a new statement.",
                        "no bus named <name> on this network",
                        "Check the name in the bus's window.",
                        "expected a count, got: <text>",
                        "Type a whole number after KEEP or MAX.",
                        "not an hour of the day: <text>",
                        "Type an hour from 0 to 23.")
                .seeAlso("jsc:bus_filters_and_tags", "jsc:bus_keep_max_and_priority", "jsc:bus_conditions")
                .register();
    }

    private static void setInterface(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("set_interface", section).titled("SET INTERFACE, SET ROUTER, RENAME, PAUSE and RESUME")
                .paragraph("A Crafting Interface holds the patterns and asks the network for crafts. A Crafting Input"
                        + " Router sends the inputs of a pattern to a router that you choose. The router must sit on"
                        + " the same cable ([](jsc:crafting_interfaces)).")
                .paragraph("These statements change an interface or a router, or their names. When a name has"
                        + " spaces, you write it in quotes.")
                .table("The statements for interfaces and routers", "Statement", "What it does")
                .row(GuideCell.code("SET INTERFACE 'x' EXCLUSIVE ON"), GuideCell.words("The interface crafts one"
                        + " recipe at a time."))
                .row(GuideCell.code("SET INTERFACE 'x' EXCLUSIVE OFF"), GuideCell.words("The interface may run"
                        + " several recipes."))
                .row(GuideCell.code("SET INTERFACE 'x' MAX JOBS 4"), GuideCell.words("At most 4 crafts at a time."
                        + " 0 means as many as come."))
                .row(GuideCell.code("SET INTERFACE 'x' MAX JOBS AUTO"), GuideCell.words("The same as 0."))
                .row(GuideCell.code("SET INTERFACE 'x' ROUTE 'pattern' INPUT item TO ROUTER 'router'"),
                        GuideCell.words("Sends that input of the pattern to the router you name."))
                .row(GuideCell.code("SET INTERFACE 'x' ROUTE 'pattern' INPUT item AUTO"),
                        GuideCell.words("Sends that input without naming a router."))
                .row(GuideCell.code("SET ROUTER 'x' setting"), GuideCell.words("Applies any setting of SET BUS"
                        + " ([](jsc:set_bus)) to a Crafting Input Router."))
                .row(GuideCell.code("PAUSE INTERFACE 'x'"), GuideCell.words("Pauses the interface."))
                .row(GuideCell.code("RESUME INTERFACE 'x'"), GuideCell.words("Starts the paused interface again."))
                .row(GuideCell.code("RENAME INTERFACE 'x' TO 'new name'"), GuideCell.words("Gives the interface a new"
                        + " name. RENAME ROUTER does the same for a router."))
                .table("The replies", "Reply", "What it means")
                .row(GuideCell.code("<name> set"), GuideCell.words("The change is saved."))
                .row(GuideCell.code("<old> is now called <new>"), GuideCell.words("The rename worked."))
                .row(GuideCell.code("no Crafting Interface named <name> on this network"), GuideCell.words("No "
                        + "interface"
                        + " of that name is on the network."))
                .row(GuideCell.code("no Crafting Input Router named <name> on this network"),
                        GuideCell.words("No router of that name is on the network."))
                .row(GuideCell.code("the interface <name> holds no pattern called <pattern>"),
                        GuideCell.words("The interface has no pattern of that name."))
                .row(GuideCell.code("<input> is not an input of <pattern>"), GuideCell.words("Name one of the"
                        + " pattern's inputs."))
                .row(GuideCell.code("no router named <router> on the cable of the interface <name>"),
                        GuideCell.words("The router is not on the cable of that interface."))
                .ifSomethingGoesWrong("SET INTERFACE needs the interface's name, in quotes when it has spaces",
                        "Type the interface's name after INTERFACE.",
                        "SET ROUTER needs the router's name, in quotes when it has spaces",
                        "Type the router's name after ROUTER.",
                        "SET INTERFACE needs a setting: EXCLUSIVE, MAX JOBS or ROUTE",
                        "Add one of those three settings after the name.",
                        "an interface has no setting <word>",
                        "Use EXCLUSIVE, MAX JOBS or ROUTE.",
                        "RENAME needs TO and the new name",
                        "Write TO and the new name after the old name.",
                        "expected the pattern's name",
                        "Type the pattern's name after ROUTE.",
                        "expected one of the pattern's inputs",
                        "Type the item of the input after INPUT, or AUTO.",
                        "expected the router's name",
                        "Type the router's name after TO ROUTER, or write AUTO.")
                .seeAlso("jsc:crafting_interfaces", "jsc:pattern_encoders", "jsc:set_bus")
                .register();
    }

    private static void setRedstone(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("set_redstone", section).titled("SET REDSTONE")
                .paragraph("A [Redstone Interface](jsc:redstone_interfaces) joins the network to redstone signals in "
                        + "the"
                        + " world. SET REDSTONE tells it whether to read a signal or to emit one, and at what"
                        + " strength.")
                .table("The two forms of SET REDSTONE", "Statement", "What it does")
                .row(GuideCell.code("SET REDSTONE 'x' IN"), GuideCell.words("The interface reads a signal."))
                .row(GuideCell.code("SET REDSTONE 'x' OUT 15"), GuideCell.words("The interface emits a signal of the"
                        + " strength you type, from 0 to 15."))
                .paragraph("The strength is a whole number from 0 to 15. The name you type must be one of the"
                        + " interfaces this computer is linked to.")
                .table("The replies of SET REDSTONE", "Reply", "What it means")
                .row(GuideCell.code("Redstone Interface <name> reads"), GuideCell.words("The interface reads a"
                        + " signal now."))
                .row(GuideCell.code("Redstone Interface <name> emits <strength>"), GuideCell.words("The interface emits"
                        + " that strength now."))
                .row(GuideCell.code("no Redstone Interface named <name> is linked to this machine"),
                        GuideCell.words("This computer has no interface of that name."))
                .ifSomethingGoesWrong("SET REDSTONE needs the interface's name, in quotes when it has spaces",
                        "Type the interface's name after REDSTONE.",
                        "SET REDSTONE needs IN, or OUT and a strength from 0 to 15",
                        "Add IN, or OUT followed by a number.",
                        "not a redstone strength, which runs from 0 to 15: <text>",
                        "Type a whole number from 0 to 15.",
                        "no Redstone Interface named <name> is linked to this machine",
                        "Use the name of an interface that this computer is linked to.")
                .seeAlso("jsc:redstone_interfaces", "jsc:set_bus")
                .register();
    }

    private static void savedObjects(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("iql_saved_objects").titled("Saving queries").in(chapter)
                .register();
        views(guide, section);
        procedures(guide, section);
        jobs(guide, section);
    }

    private static void views(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_views", section).titled("Views")
                .define("view", "A saved statement with a name. Each time you type the name, the network runs the saved"
                        + " statement again.")
                .paragraph("A view is a statement that you save under a name. You write it once, with CREATE VIEW."
                        + " Then each time you type the name, the network runs the saved statement again, so the answer"
                        + " is what it is now.")
                .paragraph("The saved statement is kept exactly as you typed it. It can be any statement, though it is"
                        + " usually a QUERY.")
                .listingIn("iql", "views.iql", "CREATE VIEW low_iron AS QUERY items WHERE item = iron_ingot AND qty < "
                        + "100")
                .steps("Type CREATE VIEW, a name, AS and the statement, all on one line.",
                        "Press [[Enter]] to save the view.",
                        "Type QUERY and the name to run it, such as QUERY low_iron.")
                .paragraph("Saved statements are kept on the Mainframe. A name is unique for each kind "
                        + "of"
                        + " saved statement, and capitals do not matter.")
                .paragraph("Views need the [Midsoft IQL Server](jsc:midsoft_iql_server) on the Mainframe. Other engines"
                        + " refuse them.")
                .paragraph("To remove a view, type DROP VIEW and its name, such as DROP VIEW low_iron.")
                .table("The replies for views", "Reply", "What it means")
                .row(GuideCell.code("view <name> dropped"), GuideCell.words("The view is gone."))
                .row(GuideCell.code("no view named <name>"), GuideCell.words("No view has that name."))
                .row(GuideCell.code("DROP VIEW needs a name"), GuideCell.words("Type the name after DROP VIEW."))
                .ifSomethingGoesWrong("CREATE expects VIEW, PROCEDURE, or JOB",
                        "Type VIEW, PROCEDURE or JOB after CREATE.",
                        "CREATE VIEW needs a name and an AS body",
                        "Type a name after VIEW, then AS and the statement. The same holds for PROCEDURE and JOB.",
                        "expected AS after the VIEW name",
                        "Type the word AS after the name.",
                        "CREATE VIEW needs a body after AS",
                        "Type the statement after AS.",
                        "a name is at most 64 characters long",
                        "Shorten the name to 64 characters or fewer.",
                        "the network's engine keeps no views, procedures or jobs",
                        "Use an engine that keeps them: the Midsoft IQL Server ([](jsc:midsoft_iql_server)).")
                .seeAlso("jsc:iql_procedures", "jsc:iql_jobs", "jsc:midsoft_iql_server")
                .register();
    }

    private static void procedures(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_procedures", section).titled("Procedures and EXEC")
                .define("procedure", "A list of statements saved under one name. EXEC runs the statements in order.")
                .paragraph("A procedure is a list of statements saved under one name. When you run it with EXEC, the"
                        + " statements run one after another, so one name does several things for you.")
                .listingIn("iql", "restock.iql", "CREATE PROCEDURE restock AS { SELECT 64 iron_ingot; SELECT 64 "
                        + "gold_ingot }")
                .steps("Type CREATE PROCEDURE, a name, AS, and the statements in braces, with a semicolon after each"
                                + " statement but the last.",
                        "Press [[Enter]] to save the procedure.",
                        "Type EXEC and the name to run it, such as EXEC restock.")
                .paragraph("A procedure can also hold a single statement, without braces. PROC means the same as"
                        + " PROCEDURE, and CALL means the same as EXEC.")
                .paragraph("The statements run in order. If one fails, the procedure stops there, and the statements"
                        + " before it have already run. Procedures need the same engine as views ([](jsc:iql_views)).")
                .table("The replies for procedures", "Reply", "What it means")
                .row(GuideCell.code("procedure <name> ran <n> statements"),
                        GuideCell.words("Every statement ran, and the reply counts them."))
                .row(GuideCell.code("procedure <name> stopped at statement <n>: <error>"), GuideCell.words("The"
                        + " statement with that number failed. The message after it says why."))
                .row(GuideCell.code("no procedure named <name>"), GuideCell.words("No procedure has that name."))
                .row(GuideCell.code("procedure <name> dropped"), GuideCell.words("The procedure is gone."))
                .paragraph("A procedure can run another procedure or a view, up to 32 levels deep. One request may run "
                        + "at"
                        + " most 4,096 statements. Past either limit, the network stops and shows the message.")
                .ifSomethingGoesWrong("CREATE PROCEDURE needs a name and an AS body",
                        "Type a name after PROCEDURE, then AS and the statements.",
                        "procedure <name> stopped at statement <n>: <error>",
                        "Fix the statement that failed, then run EXEC again.",
                        "IQL recursion too deep (a procedure or view referencing itself?)",
                        "A procedure runs itself, directly or through another. Remove the loop.",
                        "IQL stopped after 4096 statements for one request (procedures running others too many times?)",
                        "Give the procedures fewer statements to run, or split the work into several requests.",
                        "EXEC needs a procedure name",
                        "Type the name after EXEC.",
                        "EXEC takes a single procedure name",
                        "Run one procedure at a time, with one EXEC for each name.",
                        "no procedure named <name>",
                        "Check the name, or create the procedure first.",
                        "DROP PROCEDURE needs a name",
                        "Type the name after DROP PROCEDURE.")
                .seeAlso("jsc:iql_views", "jsc:iql_jobs", "jsc:midsoft_iql_server")
                .register();
    }

    private static void jobs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_jobs", section).titled("Jobs, EVERY and WHEN")
                .define("job", "A statement that the network runs on its own, each time its trigger fires.")
                .define("tick", "The game's beat. 20 ticks make one second.")
                .paragraph("A job is a statement that the network runs on its own. You give it a trigger: a time"
                        + " interval with EVERY, or a condition with WHEN. Each time the trigger fires, the statement"
                        + " runs again.")
                .paragraph("Jobs are kept by the [Automation Engine](jsc:automation_engine), which must be installed on"
                        + " the Mainframe. Jobs also need the [Midsoft IQL Server](jsc:midsoft_iql_server).")
                .listingIn("iql", "jobs.iql", "CREATE JOB restock AS SELECT 64 iron_ingot EVERY 30s",
                        "CREATE JOB low_gold AS SELECT 64 gold_ingot WHEN qty(gold_ingot) < 100")
                .paragraph("The trigger word is the last EVERY or WHEN that stands outside quotes and braces. Jobs run"
                        + " only while the Automation Engine is installed and running. A paused job stays idle until "
                                + "you"
                        + " restart it from the Processes tab. A job is credited to the player who created it.")
                .subheading("EVERY: a time interval")
                .table("Writing a duration", "Written as", "Means", "Ticks")
                .row(GuideCell.code("30"), GuideCell.words("30 seconds, since a bare number counts seconds"),
                        GuideCell.words("600"))
                .row(GuideCell.code("100t"), GuideCell.words("100 ticks"), GuideCell.words("100"))
                .row(GuideCell.code("30s"), GuideCell.words("30 seconds"), GuideCell.words("600"))
                .row(GuideCell.code("5m"), GuideCell.words("5 minutes"), GuideCell.words("6,000"))
                .row(GuideCell.code("1h"), GuideCell.words("1 hour"), GuideCell.words("72,000"))
                .paragraph("The network checks the jobs every 10 ticks, so an interval is exact to about 10 ticks. The"
                        + " first check starts the clock, so a job does not fire at once.")
                .subheading("WHEN: a condition")
                .paragraph("WHEN fires the job once, when its condition turns from false to true. It does not fire "
                        + "again"
                        + " while the condition stays true. A condition that is already true fires at the first check.")
                .paragraph("The only field a WHEN can read is qty(item), which is the network's total of that item. A"
                        + " WHEN with any other field never fires.")
                .paragraph("A CREATE JOB whose statement is SET BUS with WHEN TIME does not make a job. It sets the"
                        + " hours of the bus instead ([](jsc:set_bus)).")
                .table("The replies for jobs", "Reply", "What it means")
                .row(GuideCell.code("job <name> dropped"), GuideCell.words("The job no longer runs."))
                .row(GuideCell.code("no job named <name>"), GuideCell.words("No job has that name."))
                .row(GuideCell.code("DROP JOB needs a name"), GuideCell.words("Type the name after DROP JOB."))
                .ifSomethingGoesWrong("CREATE JOB needs a trigger: EVERY <duration> or WHEN <condition>",
                        "Add EVERY and a duration, or WHEN and a condition, after the statement.",
                        "CREATE JOB needs a body before its trigger",
                        "Put the statement before EVERY or WHEN.",
                        "CREATE JOB trigger needs a value",
                        "Type the duration or the condition after the trigger word.",
                        "jobs are kept by the Automation Engine; install it on the Mainframe first",
                        "Install the Automation Engine on the Mainframe, then create the job.",
                        "not a duration: <text>",
                        "Type a number, with t, s, m or h after it if you want, such as 30s.",
                        "empty duration",
                        "Type a duration after EVERY, such as 30s.",
                        "duration must be >= 0: <text>",
                        "Type a duration of 0 or more.",
                        "unknown duration unit '<unit>' in: <text>",
                        "Use t, s, m or h as the unit, or no unit for seconds.",
                        "The job never fires, and its interval is 0.",
                        "Use an interval above zero, such as 30s.",
                        "The job never fires, and no error appears.",
                        "Its WHEN condition does not read. Check the words and the spelling of the condition.",
                        "The job does not fire at all.",
                        "Check that the Automation Engine is installed and running, and that the job is not paused.",
                        "no job named <name>",
                        "Check the name, or create the job first.")
                .seeAlso("jsc:automation_engine", "jsc:iql_procedures", "jsc:set_bus")
                .register();
    }

    private static void limitsAndErrors(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("iql_limits_and_errors").titled("Limits and errors")
                .in(chapter).register();
        limits(guide, section);
        errors(guide, section);
    }

    private static void limits(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_limits", section).titled("Limits")
                .paragraph("The network sets limits on what one statement can take. Some limits cut a result short.")
                .paragraph("Others refuse the statement, or stop it part way, and show a message.")
                .paragraph("The table gives each limit and what happens when you pass it.")
                .table("The limits of IQL", "Limit", "Value", "What happens when you pass it")
                .row(GuideCell.words("Rows that a QUERY reads, by default"), GuideCell.words("4,096 rows"),
                        GuideCell.words("A LIMIT below that number cuts the rows shorter."))
                .row(GuideCell.words("Item types one * statement covers"), GuideCell.words("256 item types"),
                        GuideCell.words("A LIMIT above that is capped, and the reply says how many were shown."))
                .row(GuideCell.words("Levels of NOT and brackets in a condition"), GuideCell.words("64 levels"),
                        GuideCell.words("The statement is refused."))
                .row(GuideCell.words("Comparisons in one condition"), GuideCell.words("256 comparisons"),
                        GuideCell.words("The statement is refused."))
                .row(GuideCell.words("Length of the name of a saved statement"), GuideCell.words("64 characters"),
                        GuideCell.words("The statement is refused."))
                .row(GuideCell.words("Length of the new name an anvil gives"), GuideCell.words("50 characters"),
                        GuideCell.words("The statement is refused."))
                .row(GuideCell.words("Enchantment offers, numbered"), GuideCell.words("1 to 3"),
                        GuideCell.words("Any other number is refused."))
                .row(GuideCell.words("Procedures that run procedures"), GuideCell.words("32 levels"),
                        GuideCell.words("The network stops with a recursion message."))
                .row(GuideCell.words("Statements in one request"), GuideCell.words("4,096 statements"),
                        GuideCell.words("The network stops and says how many it ran."))
                .row(GuideCell.words("Rows that a job's query reads"), GuideCell.words("64 rows"),
                        GuideCell.words("The job reads at most 64 rows."))
                .row(GuideCell.words("How often the network checks the jobs"), GuideCell.words("every 10 ticks"),
                        GuideCell.words("An interval is exact to about 10 ticks."))
                .row(GuideCell.words("Redstone strength"), GuideCell.words("0 to 15"),
                        GuideCell.words("Any other number is refused."))
                .row(GuideCell.words("Hour of the day in SET BUS"), GuideCell.words("0 to 23"),
                        GuideCell.words("Any other hour is refused."))
                .row(GuideCell.words("Quantity of an item"), GuideCell.words("A whole number of zero or more, or ALL"),
                        GuideCell.words("A percentage or a negative number is refused. For an action, no number"
                                + " and ALL do the same."))
                .row(GuideCell.words("Row count after LIMIT"), GuideCell.words("A whole number"),
                        GuideCell.words("A percentage is refused. A LIMIT of 0 means no limit."))
                .paragraph("Some limits protect the network, not your statement. If a QUERY reaches 4,096 rows, use a"
                        + " WHERE to narrow the rows you read, so the rows you need are not cut short.")
                .seeAlso("jsc:iql_errors", "jsc:query_and_select")
                .register();
    }

    private static void errors(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_errors", section).titled("Error messages and their fixes")
                .paragraph("A refusal is an error message that stops a statement before it runs. You read it from the"
                        + " start: the first words say what went wrong, and the words in angle brackets show where your"
                        + " own text goes. Find the message below, and read the fix beside it.")
                .subheading("The form of a statement")
                .table("Refusals of the form", "The message", "What it means, and the fix")
                .row(GuideCell.code("empty statement"), GuideCell.words("The line holds nothing. Type a statement."))
                .row(GuideCell.code("unknown verb: <word>"), GuideCell.words("The first word is not a verb. Check its"
                        + " spelling against the verbs in this book."))
                .row(GuideCell.code("unexpected token: <text>"), GuideCell.words("A word or sign stands where the"
                        + " statement does not take it. Remove it, or use a verb that takes it."))
                .row(GuideCell.code("percentage quantities are not supported yet: <quantity>"),
                        GuideCell.words("Type a whole number, or ALL."))
                .row(GuideCell.code("LIMIT takes a row count, not a percentage: <quantity>"),
                        GuideCell.words("Type a whole number of rows after LIMIT."))
                .row(GuideCell.code("unknown priority level: <word> (expected LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH or"
                        + " HIGH)"), GuideCell.words("Type one of the five levels."))
                .row(GuideCell.code("DELETE exports out of the network and needs a TO destination"),
                        GuideCell.words("Add TO and the name of a bus."))
                .row(GuideCell.code("MOVE is internal and needs both FROM and TO"), GuideCell.words("Add FROM and TO."))
                .row(GuideCell.code("<VERB> has no TO destination"), GuideCell.words("Remove TO. SELECT, INSERT, DROP"
                        + " and UPDATE take none."))
                .row(GuideCell.code("<VERB> is not valid on a read"), GuideCell.words("A QUERY refuses FROM, TO, IF and"
                        + " PRIORITY. Put the server into WHERE instead."))
                .row(GuideCell.code("UPDATE needs SET and what the card is to do, after the item"),
                        GuideCell.words("Add SET, then the action word."))
                .row(GuideCell.code("unknown action: <word> (expected SMELT, ENCHANT, REPAIR, COMBINE or NAME)"),
                        GuideCell.words("Use one of the five action words."))
                .row(GuideCell.code("an offer is 1, 2 or 3, not <n>"), GuideCell.words("Type 1, 2 or 3."))
                .row(GuideCell.code("a name is at most 50 characters long"), GuideCell.words("Shorten the new name of"
                        + " the anvil to 50 characters or fewer."))
                .row(GuideCell.code("a condition nests at most 64 NOTs and brackets deep"),
                        GuideCell.words("Remove some NOT or brackets."))
                .row(GuideCell.code("a condition compares at most 256 things"), GuideCell.words("Shorten the "
                        + "condition."))
                .row(GuideCell.code("<ACTION> takes no WITH"), GuideCell.words("Only REPAIR and COMBINE take WITH,"
                        + " so remove it."))
                .row(GuideCell.code("COMBINE needs WITH and the item it adds"), GuideCell.words("Add WITH and the "
                        + "second"
                        + " item."))
                .row(GuideCell.code("unknown operator: <text>"), GuideCell.words("Use one of the operators of WHERE"
                        + " ([](jsc:query_and_select))."))
                .row(GuideCell.code("unknown duration unit '<unit>' in: <text>"), GuideCell.words("Use t, s, m or h as"
                        + " the unit, or none for seconds."))
                .table("Missing parts: expected", "The message", "What it means, and the fix")
                .row(GuideCell.code("expected a verb"), GuideCell.words("Type a verb at the start, such as QUERY."))
                .row(GuideCell.code("expected an item"), GuideCell.words("Type the item's id after the verb."))
                .row(GuideCell.code("expected an object to query"), GuideCell.words("Type the object after QUERY, such"
                        + " as items."))
                .row(GuideCell.code("expected a quantity, got: -5"), GuideCell.words("A quantity is a whole number of"
                        + " zero or more."))
                .row(GuideCell.code("expected a location after FROM"), GuideCell.words("Type the server's or the bus's"
                        + " name as one word."))
                .row(GuideCell.code("expected a location after TO"), GuideCell.words("Type the bus's name as one "
                        + "word."))
                .row(GuideCell.code("expected BY"), GuideCell.words("Type BY after ORDER."))
                .row(GuideCell.code("expected a sort field after ORDER BY"), GuideCell.words("Type a field name after"
                        + " ORDER BY."))
                .row(GuideCell.code("expected a level after PRIORITY (LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH)"),
                        GuideCell.words("Type one of the five levels after PRIORITY."))
                .row(GuideCell.code("expected a row count after LIMIT"), GuideCell.words("Type a whole number after"
                        + " LIMIT."))
                .row(GuideCell.code("expected a field name"), GuideCell.words("Type a field name, such as qty."))
                .row(GuideCell.code("expected a comparison operator"), GuideCell.words("Type an operator, such as = or"
                        + " >."))
                .row(GuideCell.code("expected a value"), GuideCell.words("Type a value after the operator."))
                .row(GuideCell.code("expected an action after SET (SMELT, ENCHANT, REPAIR, COMBINE, NAME)"),
                        GuideCell.words("Type one action word after SET."))
                .row(GuideCell.code("expected an offer after OFFER (1, 2 or 3)"), GuideCell.words("Type 1, 2 or 3."))
                .row(GuideCell.code("expected a name in quotes after NAME"), GuideCell.words("Put the new name in"
                        + " quotes."))
                .row(GuideCell.code("expected the pattern's name"), GuideCell.words("Type the pattern's name."))
                .row(GuideCell.code("expected one of the pattern's inputs"), GuideCell.words("Type one of the pattern's"
                        + " inputs."))
                .row(GuideCell.code("expected the router's name"), GuideCell.words("Type the router's name."))
                .row(GuideCell.code("expected a list of items"), GuideCell.words("Type at least one item or tag."))
                .row(GuideCell.code("expected a count"), GuideCell.words("Type a whole number."))
                .subheading("The network's answers")
                .table("Refusals from the network", "The message", "What it means, and the fix")
                .row(GuideCell.code("Network Operations Service unavailable"), GuideCell.words("No engine that the"
                        + " statement needs runs on the Mainframe. Start one ([](jsc:iql_engines_overview))."))
                .row(GuideCell.code("the network has no running Mainframe"), GuideCell.words("A Mainframe must run on"
                        + " the network ([](jsc:mainframes))."))
                .row(GuideCell.code("syntax: <reason>"), GuideCell.words("The engine could not read the statement. The"
                        + " reason after it says what is wrong."))
                .row(GuideCell.code("unknown item: <name>"), GuideCell.words("The name is not an item, or the WHERE "
                        + "kept"
                        + " no row for it. Check the spelling and the WHERE."))
                .row(GuideCell.code("no server named '<server>'"), GuideCell.words("The server does not exist. Check "
                        + "its"
                        + " name."))
                .row(GuideCell.code("no server or bus named '<name>'"), GuideCell.words("Neither a server nor a bus has"
                        + " that name."))
                .row(GuideCell.code("no bus named '<bus>'"), GuideCell.words("No bus has that name on the network."))
                .row(GuideCell.code("the bus '<bus>' touches no inventory"), GuideCell.words("The bus has no chest or"
                        + " machine beside it."))
                .row(GuideCell.code("the network holds no <item>"), GuideCell.words("No copy of the item is on the"
                        + " network."))
                .row(GuideCell.code("no pattern crafts <item>"), GuideCell.words("No recipe on the network makes the"
                        + " item."))
                .row(GuideCell.code("this computer has no <card name>"), GuideCell.words("Put that card into this"
                        + " Personal Computer."))
                .row(GuideCell.code("only a Personal Computer's own cards change items for the network"),
                        GuideCell.words("Run the statement on a Personal Computer."))
                .row(GuideCell.code("somebody has to ask for it: the card is paid for with their experience"),
                        GuideCell.words("Run the statement from a player's terminal."))
                .row(GuideCell.code("the <item> does not smelt"), GuideCell.words("Pick an item that smelts."))
                .row(GuideCell.code("the <item> takes no enchantment"), GuideCell.words("Pick an item that takes an"
                        + " enchantment."))
                .row(GuideCell.code("the <item> is not worn"), GuideCell.words("REPAIR needs a damaged copy."))
                .row(GuideCell.code("the network holds nothing that mends the <item>"), GuideCell.words("Put a material"
                        + " on the network, or name one after WITH."))
                .row(GuideCell.code("maintenance runs on the Mainframe only"), GuideCell.words("Type the command at the"
                        + " Mainframe itself."))
                .row(GuideCell.code("the network's engine keeps no views, procedures or jobs"),
                        GuideCell.words("Use the [Midsoft IQL Server](jsc:midsoft_iql_server)."))
                .row(GuideCell.code("jobs are kept by the Automation Engine; install it on the Mainframe first"),
                        GuideCell.words("Install the Automation Engine on the Mainframe."))
                .row(GuideCell.code("the destination server is unavailable"), GuideCell.words("The destination cannot"
                        + " take items now."))
                .row(GuideCell.code("could not start the MOVE"), GuideCell.words("The network did not start the move."))
                .row(GuideCell.code("could not start the UPDATE"), GuideCell.words("The network did not start the"
                        + " change."))
                .row(GuideCell.code("nothing to lock: the network holds no free <item>"),
                        GuideCell.words("Wait for free stock, then lock it."))
                .row(GuideCell.code("<item> is already locked"), GuideCell.words("The hold is already in place."))
                .row(GuideCell.code("<item> is not locked"), GuideCell.words("There was no hold to release."))
                .subheading("Settings of buses, interfaces and redstone")
                .table("Refusals of the settings", "The message", "What it means, and the fix")
                .row(GuideCell.code("SET BUS needs the bus's name, in quotes when it has spaces"),
                        GuideCell.words("Type the bus's name after BUS."))
                .row(GuideCell.code("SET BUS needs a setting: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY,"
                        + " WHEN, AFTER BUS or ACCESS"), GuideCell.words("Add one setting word after the name."))
                .row(GuideCell.code("a bus has no setting <word>"), GuideCell.words("Use a setting from the table of"
                        + " SET BUS."))
                .row(GuideCell.code("not an hour of the day: <text>"), GuideCell.words("Type an hour from 0 to 23."))
                .row(GuideCell.code("SET INTERFACE needs the interface's name, in quotes when it has spaces"),
                        GuideCell.words("Type the interface's name after INTERFACE."))
                .row(GuideCell.code("SET ROUTER needs the router's name, in quotes when it has spaces"),
                        GuideCell.words("Type the router's name after ROUTER."))
                .row(GuideCell.code("SET INTERFACE needs a setting: EXCLUSIVE, MAX JOBS or ROUTE"),
                        GuideCell.words("Add one of those three settings after the name."))
                .row(GuideCell.code("an interface has no setting <word>"), GuideCell.words("Use EXCLUSIVE, MAX JOBS"
                        + " or ROUTE."))
                .row(GuideCell.code("RENAME needs TO and the new name"), GuideCell.words("Write TO and the new name"
                        + " after the old name."))
                .row(GuideCell.code("SET REDSTONE needs the interface's name, in quotes when it has spaces"),
                        GuideCell.words("Type the interface's name after REDSTONE."))
                .row(GuideCell.code("SET REDSTONE needs IN, or OUT and a strength from 0 to 15"),
                        GuideCell.words("Add IN, or OUT followed by a number."))
                .row(GuideCell.code("not a redstone strength, which runs from 0 to 15: <text>"),
                        GuideCell.words("Type a whole number from 0 to 15."))
                .subheading("Saved statements, durations and files")
                .table("Messages of saved statements, durations and files", "The message", "What it means, and the fix")
                .row(GuideCell.code("CREATE <TYPE> needs a name and an AS body"), GuideCell.words("Type a name, then AS"
                        + " and the body, in that order. TYPE is VIEW, PROCEDURE or JOB."))
                .row(GuideCell.code("expected AS after the <TYPE> name"), GuideCell.words("Type AS after the name."))
                .row(GuideCell.code("CREATE <TYPE> needs a body after AS"), GuideCell.words("Type the body after AS."))
                .row(GuideCell.code("CREATE JOB needs a trigger: EVERY <duration> or WHEN <condition>"),
                        GuideCell.words("Add EVERY and a duration, or WHEN and a condition."))
                .row(GuideCell.code("CREATE JOB needs a body before its trigger"), GuideCell.words("Put the statement"
                        + " before EVERY or WHEN."))
                .row(GuideCell.code("CREATE JOB trigger needs a value"), GuideCell.words("Type the duration or the"
                        + " condition after the trigger word."))
                .row(GuideCell.code("DROP <TYPE> needs a name"), GuideCell.words("Type the name after the kind."))
                .row(GuideCell.code("EXEC needs a procedure name"), GuideCell.words("Type the name after EXEC."))
                .row(GuideCell.code("EXEC takes a single procedure name"), GuideCell.words("Run one procedure per "
                        + "EXEC."))
                .row(GuideCell.code("empty duration"), GuideCell.words("Type a duration after EVERY, such as 30s."))
                .row(GuideCell.code("not a duration: <text>"), GuideCell.words("Type a whole number, with t, s, m or h "
                        + "after"
                        + " it. A decimal such as 1.5s is refused."))
                .row(GuideCell.code("duration must be >= 0: <text>"), GuideCell.words("Type a duration of 0 or more."))
                .row(GuideCell.code("no view named <name>"), GuideCell.words("No view has that name."))
                .row(GuideCell.code("no procedure named <name>"), GuideCell.words("No procedure has that name."))
                .row(GuideCell.code("no job named <name>"), GuideCell.words("No job has that name."))
                .row(GuideCell.code("procedure <name> stopped at statement <n>: <error>"), GuideCell.words("Fix the"
                        + " statement that failed, then run EXEC again."))
                .row(GuideCell.code("IQL recursion too deep (a procedure or view referencing itself?)"),
                        GuideCell.words("A saved statement runs itself. Remove the loop."))
                .row(GuideCell.code("IQL stopped after 4096 statements for one request (procedures running others too"
                        + " many times?)"), GuideCell.words("Give the procedures fewer statements to run."))
                .row(GuideCell.code("the IF did not hold, so the <VERB> was not run"), GuideCell.words("Not a failure:"
                        + " the IF was false. Check the value of the field it compares."))
                .row(GuideCell.code("<file>: syntax error in '<statement>': <reason>"), GuideCell.words("A line of the"
                        + " file is not a statement. Nothing runs until the line is fixed."))
                .row(GuideCell.code("<file>: only .iql files can be run (got .<ext>)"), GuideCell.words("Only a file "
                        + "that"
                        + " ends in .iql runs. Rename the file."))
                .row(GuideCell.code("<file>: nothing to run"), GuideCell.words("The file holds no statement."))
                .row(GuideCell.code("<file>: QUERY/COUNT are not supported by 'run', use 'operation' instead"),
                        GuideCell.words("A file cannot run a read. Type the read at the prompt."))
                .paragraph("A file runs one statement per line. Blank lines and lines that start with -- are left out. "
                        + "A"
                        + " semicolon does not split a line in a file, so put one statement on each line. The file "
                                + "stops"
                        + " at the first statement that fails, and the statements above it have already run."
                        + " The whole file is read first, so a syntax error on any line means nothing runs.")
                .ifSomethingGoesWrong("a name is at most 50 characters long",
                        "Shorten the name. Only the new name of an anvil has this limit.",
                        "unknown item: <name>",
                        "Check the spelling, then check that the WHERE keeps the row you want.")
                .seeAlso("jsc:iql_limits", "jsc:query_and_select", "jsc:iql_engines_overview")
                .register();
    }
}
