/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.guide.GuidePages;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Chapters, manuals and the item that opens one, to prove the Core's manuals: every kind of block an entry is made
 * of, a special block another mod draws, an entry an item's page opens to, links between entries, tables that run on
 * and that stack, a second book that binds a chapter written for it alone before the mod's main one, a link from one
 * book into the other, and a stack whose page its mod chooses.
 */
public final class TestGuide {

    /** The test manual's id. */
    public static final String MANUAL = "jstests:test_manual";
    /** The second book, which binds the shared chapter before the main one. */
    public static final String SECOND_BOOK = "jstests:second_book";
    /** The entries the tests open. */
    public static final String PRESS = "jstests:press";
    public static final String SWATCH = "jstests:swatch";
    public static final String LONG = "jstests:long";
    public static final String OTHER_BOOK = "jstests:other_book";
    public static final String SHARED_PAGE = "jstests:shared_page";
    public static final String TABLES = "jstests:tables";
    public static final String CODE = "jstests:code";
    /** The listings of the code entry, by the files they are. */
    public static final String HELLO = "Hello.sgs";
    public static final String QUERY = "top.iql";
    /** The name a sheet of paper takes to be the page of the shared entry. */
    public static final String SHARED_PAPER = "Shared Notes";
    /** The kind of special block the test mod draws. */
    public static final String SWATCH_KIND = "jstests:swatch";
    /** What the test manual reads from the test mod as it is laid out: the press's energy, and every item. */
    public static final String PRESS_ENERGY = "jstests:press_energy";
    public static final String ITEMS = "jstests:items";

    private static final ModGuide GUIDE = TestSounds.CONTENT.guide();

    static {
        GUIDE.chapter().titled("Test Mod").order(90).tab("#FFD04040").register();
    }

    /** A chapter written for the second book only: the series' reference leaves it out. */
    public static final GuideChapter SHARED = GUIDE.chapter("shared").titled("Shared Chapter").order(95)
            .tab("#FF40A0D0").about("A chapter that only the second test book binds.").ownBooksOnly().register();

    public static final ModGuide.SectionRef MACHINES = GUIDE.section("machines").titled("Test Machines")
            .icon(() -> TestPress.UPGRADE).register();
    public static final ModGuide.SectionRef IDEAS = GUIDE.section("ideas").titled("Test Ideas")
            .icon(() -> Items.PAPER).register();
    public static final ModGuide.SectionRef NOTES = GUIDE.section("notes").titled("Shared Notes")
            .icon(() -> Items.MAP).in(SHARED).register();

    public static final ItemEntry<ManualItem> MANUAL_ITEM = TestSounds.CONTENT.item("test_manual",
            properties -> new ManualItem(properties, MANUAL)).named("Test Manual").look(IItemLook.HANDMADE)
            .tab(TestItems.ITEMS).register();

    static {
        // The press has no item of its own, so its page is opened from its upgrade, which it is drawn by too.
        GUIDE.page("press", MACHINES).titled("Test Press").icon(() -> TestPress.UPGRADE)
                .covers(() -> TestPress.UPGRADE)
                .whatItIs("A machine of the test mod that presses two inputs into two outputs.")
                .whatItIsFor("Proving that the Core's machines work, with every slot and tank they can have.")
                .figure(() -> TestPress.UPGRADE, "The test press's speed upgrade")
                .table("Its numbers")
                .amount("Energy a tick", TestPress.ENERGY_PER_TICK, "FE/t")
                .property("Found in", "Development worlds only")
                .fixed("Model", "TP-1")
                .howToGetIt("From the test items tab, in a development world.")
                .howToUseIt("Place it.", "Give it energy.", "Put what it presses in its inputs.")
                .recipes(TestPress.PRESSING.id().toString())
                .whatCanGoWrong("It never starts.", "Nothing it holds matches a recipe of its kind.")
                .define("Test Energy", "The energy the test press spends, which is plain FE.")
                .seeAlso(SWATCH)
                .register();
        GUIDE.page("swatch", IDEAS).titled("Colour Swatch").icon(() -> Items.RED_DYE)
                .whatItIs("A special block the test mod draws itself, as another mod would.")
                .whatItIsFor("Proving that a mod can register a kind of block for the manuals.")
                .custom(SWATCH_KIND, 24, "{\"colour\":\"#FFD04040\"}")
                .howToGetIt("It is drawn in this manual, and nowhere else.")
                .howToUseIt("Look at it.")
                .warning("Nothing in the world is this colour on purpose.")
                .whatCanGoWrong("The swatch is blank.", "The test mod's renderer is not registered on this game.")
                .seeAlso(PRESS)
                .register();
        GUIDE.page("long", IDEAS).titled("A Long Entry").icon(() -> Items.BOOK)
                .whatItIs("An entry long enough to run on to the pages after its first, to prove the layout.")
                .paragraph(("Every paragraph of this entry is long on purpose, so the manual must carry it over to a"
                        + " new page where it runs out of room, as a printed manual would. ").repeat(6))
                .steps("One.", "Two.", "Three.", "Four.", "Five.", "Six.", "Seven.", "Eight.")
                .whatItIsFor("Nothing but this test.")
                .howToGetIt("Read it.")
                .howToUseIt("Turn the page.")
                .whatCanGoWrong("It ends on its first page.", "The layout lost a page.")
                .register();
        GUIDE.page("tables", IDEAS).titled("Tables").icon(() -> Items.WRITABLE_BOOK)
                .whatItIs("An entry of tables: one of commands long enough to run on to the next page, and one too"
                        + " wide for its columns to stand side by side.")
                .table("Commands for files", "Command", "What it does")
                .row(GuideCell.code("dir"), GuideCell.words("Lists what is in the folder you are in."))
                .row(GuideCell.code("cd"), GuideCell.words("Shows the folder you are in, or moves to another."))
                .row(GuideCell.code("type"), GuideCell.words("Prints a text file."))
                .row(GuideCell.code("write"), GuideCell.words("Writes a line into a file."))
                .row(GuideCell.code("del"), GuideCell.words("Deletes a file."))
                .row(GuideCell.code("mkdir"), GuideCell.words("Makes a folder."))
                .row(GuideCell.code("rmdir"), GuideCell.words("Removes an empty folder."))
                .row(GuideCell.code("copy"), GuideCell.words("Copies a file."))
                .row(GuideCell.code("move"), GuideCell.words("Moves a file to another folder."))
                .row(GuideCell.code("ren"), GuideCell.words("Gives a file another name."))
                .row(GuideCell.code("format"), GuideCell.words("Erases a whole drive. It asks first."))
                .row(GuideCell.code("tree"), GuideCell.words("Draws the folders under this one."))
                .table("Compile errors", "Code", "Message", "Means", "Fix")
                .row(GuideCell.code("S1001"), GuideCell.words("unterminated string"),
                        GuideCell.words("A string was opened with a quote and the line ended before it was closed."),
                        GuideCell.words("Close the string with a quote on the same line."))
                .row(GuideCell.code("S3001"), GuideCell.words("unknown name 'Consol'"),
                        GuideCell.words("Nothing your program can see has that name: it is misspelled, or its"
                                + " namespace has no using line."),
                        GuideCell.words("Check the spelling, or add the using line."))
                .paragraph("The test press spends " + GuideFacts.token(PRESS_ENERGY) + " while it works, a figure"
                        + " read from the test mod as this page is laid out, as the table under it is.")
                .tableFrom(ITEMS, "Every item of the test mod", "Item", "Stack")
                .whatItIsFor("Proving the layout of tables.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Read the tables.")
                .whatCanGoWrong("A word is cut in two.", "A column was left narrower than its longest word.")
                .register();
        GUIDE.page("code", IDEAS).titled("Code").icon(() -> Items.COMMAND_BLOCK)
                .whatItIs("An entry of listings: a program, a query, and a line too long for its page.")
                .listing(HELLO, "using System.IO.*;", "", "namespace Hello;", "", "class Program {",
                        "  static void Main() {", "    Console.PrintLine(\"Hello, world! This line is long enough to go"
                                + " on under itself.\");", "  }", "}")
                .listing(QUERY, "-- the five commonest items", "QUERY items", "  ORDER BY count DESC", "  LIMIT 5;")
                .whatItIsFor("Proving the layout of listings, their colours and their Copy button.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Read the listings.", "Copy one.", "Open one over the whole book.")
                .whatCanGoWrong("A listing is one colour.", "Its language has no colours registered for the manuals.")
                .register();
        GUIDE.page("other_book", IDEAS).titled("A Link to Another Book").icon(() -> Items.COMPASS)
                .whatItIs("An entry whose words lead to [a page of another book](jstests:shared_page), which this"
                        + " manual does not hold.")
                .whatItIsFor("Proving that a link names the book holding its page and opens it there.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Click the link.")
                .whatCanGoWrong("The link leads nowhere.", "No book holds its page.")
                .register();
        GUIDE.page("shared_page", NOTES).titled("A Shared Page").icon(() -> Items.MAP)
                .whatItIs("A page of a chapter that only the second test book binds.")
                .whatItIsFor("Proving that a book holds the chapters it names, in the order it names them.")
                .howToGetIt("Open the second test book, or hold the manual key over a sheet of paper named "
                        + SHARED_PAPER + ".")
                .howToUseIt("Read it.")
                .whatCanGoWrong("It shows in the test manual.", "That manual holds a chapter it does not name.")
                .register();
        GUIDE.manual("test_manual").titled("Test Manual").cover("J's Tech Series", "Test Library")
                .edition("First Test Edition").partNumber("JTS-TEST").style(CoreGuide.BINDER).chapters("jstests")
                .about("This manual holds the test mod's chapter, to prove the Core's manuals.").priority(-1)
                .register();
        GUIDE.manual("second_book").titled("Second Test Book").cover("J's Tech Series", "Test Library")
                .edition("First Test Edition").style(CoreGuide.BINDER).chapters(SHARED.id(), "jstests")
                .about("This book binds the shared chapter before the test mod's main one.").priority(-2)
                .register();
    }

    private TestGuide() {
    }

    /**
     * Declares the chapters, their entries, the manuals and the item, before the test mod's content is registered,
     * and says which page a named sheet of paper is.
     */
    public static void declare() {
        // Loading the class declares the rest; a sheet of paper named for it is the shared page's.
        GuidePages.byStack(Items.PAPER, stack -> stack.has(DataComponents.CUSTOM_NAME)
                && SHARED_PAPER.equals(stack.getHoverName().getString()) ? SHARED_PAGE : "");
        GuideFacts.value(PRESS_ENERGY, () -> new GuideBlock.GuideValue.Amount(TestPress.ENERGY_PER_TICK, "FE/t"));
        GuideFacts.rows(ITEMS, TestGuide::itemRows);
    }

    /** Every item of the test mod, in the order of their ids: its name, and how many make a stack. */
    public static List<GuideBlock.TableRow> itemRows() {
        return BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(JsTests.MODID))
                .sorted().map(BuiltInRegistries.ITEM::get)
                .map(item -> new GuideBlock.TableRow(List.of(new GuideBlock.GuideValue.Words(item.getDescriptionId()),
                        new GuideBlock.GuideValue.Amount(new ItemStack(item).getMaxStackSize(), ""))))
                .toList();
    }
}
