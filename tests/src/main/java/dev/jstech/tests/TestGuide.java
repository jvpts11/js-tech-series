/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Chapters, manuals and the item that opens one, to prove the Core's manuals: every kind of block an entry is made
 * of, a special block another mod draws, an entry an item's page opens to, links between entries, tables that run on
 * and that stack, a scene the reader turns, a command's page read from the command, a second book that binds a chapter
 * written for it alone before the mod's main one, a link from one book into the other, a stack whose page its mod
 * chooses, and a book in the look of each of J's Computers' books.
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
    public static final String COMMAND = "jstests:command";
    public static final String SCENE = "jstests:scene";
    public static final String MULTIBLOCK = "jstests:multiblock";
    public static final String STEPS = "jstests:steps";
    public static final String STRUCTURE = "jstests:structure";
    public static final String ANIMATION = "jstests:animation";
    /** How long each step of the scene in steps is shown, short so a test sees it play. */
    public static final int STEP_TICKS = 20;
    /**
     * The hut the structure entry draws, saved with the structure block as
     * {@code assets/jstests/jstech/structures/test_hut.nbt}.
     */
    public static final ResourceLocation HUT = ResourceLocation.fromNamespaceAndPath("jstests", "test_hut");
    /** The moving picture's frame, drawn twice its texture's pixels, and how long each frame is shown. */
    public static final int ANIMATION_WIDTH = 64;
    public static final int ANIMATION_HEIGHT = 32;
    public static final int ANIMATION_TICKS = 10;
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
    /**
     * The styles of J's Computers' books, each drawing a test book that holds the test chapters, so their pages, the
     * marks of their chapters and their ribbon can be seen with chapters in them.
     */
    public static final List<String> BOOK_STYLES = List.of("frames_resource_kit", "linux_desk_reference",
            "linux_by_hand", "daemon_handbook", "learning_iql", "book_of_sigma");

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
    /** A section whose every entry needs knowledge no test gives: the contents say "not yet" beside it. */
    public static final ModGuide.SectionRef LOCKED = GUIDE.section("locked").titled("Locked Ideas")
            .icon(() -> Items.IRON_BARS).register();

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
                .paragraph("Keys are drawn as keys: [[Ctrl]]+[[S]], and [[Page Down]] turns this manual's page.")
                .listing(HELLO, "using System.IO.*;", "", "namespace Hello;", "", "class Program {",
                        "  static void Main() {", "    Console.PrintLine(\"Hello, world! This line is long enough to go"
                                + " on under itself.\");", "  }", "}")
                .listing(QUERY, "-- the five commonest items", "QUERY items", "  ORDER BY count DESC", "  LIMIT 5;")
                .session("jsc:guide/terminal/dos", "MC-DOS 6.22").typed("C:\\>", "sgsc Hello.sgs")
                .answer("Hello.sgs: 0 errors").typed("C:\\>", "sigma run Hello.asm").answer("Hello, world!")
                .session("jsc:guide/terminal/bash", "Ubuntu  -  bash").typed("player@pc:~$ ", "apt install vim")
                .answer("Reading package lists...", "Setting up vim (9.1) ...").typed("player@pc:~$ ", "")
                .whatItIsFor("Proving the layout of listings, their colours and their Copy button.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Read the listings.", "Copy one.", "Open one over the whole book.")
                .whatCanGoWrong("A listing is one colour.", "Its language has no colours registered for the manuals.")
                .register();
        GUIDE.page("scene", IDEAS).titled("A Scene").icon(() -> Items.FURNACE)
                .whatItIs("An entry with a scene the reader turns: a furnace and a chest standing on grass.")
                .scene("A furnace and a chest on grass. Drag them to see them from behind.", 90)
                .place(0, 0, 0, () -> Blocks.GRASS_BLOCK)
                .place(1, 0, 0, () -> Blocks.GRASS_BLOCK)
                .place(0, 1, 0, () -> Blocks.FURNACE, "facing=south,lit=true", "")
                .place(1, 1, 0, () -> Blocks.CHEST, "facing=south",
                        "{Items:[{Slot:0b,id:\"minecraft:diamond\",count:1}]}")
                .view(-30, 25, 1)
                .layers()
                .balloon(1, GuideBlock.ScenePart.of(0, 1, 0).box(0, 0, 15, 16, 16, 16), "The furnace's front",
                        "It glows while it burns")
                .balloon(2, GuideBlock.ScenePart.of(1, 1, 0), "A chest")
                .balloon(3, GuideBlock.ScenePart.of(0, 0, 0), "Grass")
                .whatItIsFor("Proving the scenes: how they are drawn and turned, their balloons and their layers.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Drag the scene.", "Turn the wheel over it.", "Point at a balloon.", "Hide a layer.")
                .whatCanGoWrong("The scene is empty.", "None of its blocks is one the game knows.")
                .register();
        GUIDE.page("multiblock", IDEAS).titled("A Multiblock").icon(() -> Items.BLAST_FURNACE)
                .whatItIs("An entry with the test furnace drawn from its pattern as it stands now, its terminal in"
                        + " its place.")
                .multiblock("The test furnace, from its pattern. Peel it layer by layer.", TestMultiblocks.FURNACE_ID,
                        100)
                .whatItIsFor("Proving that a manual draws a multiblock from the pattern in force.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Drag it.", "Hide a layer.")
                .whatCanGoWrong("No figure shows.", "The multiblock is not installed, or it is too big for a scene.")
                .register();
        GUIDE.page("steps", IDEAS).titled("A Scene in Steps").icon(() -> Items.CHEST)
                .whatItIs("An entry with a scene shown step by step: a furnace put on grass, a chest put beside it,"
                        + " then the furnace taken away.")
                .scene("A furnace and a chest put on grass in three steps.", 90)
                .place(0, 0, 0, () -> Blocks.GRASS_BLOCK)
                .place(1, 0, 0, () -> Blocks.GRASS_BLOCK)
                .step("Put a furnace on the grass", STEP_TICKS)
                .place(0, 1, 0, () -> Blocks.FURNACE, "facing=south", "")
                .step("Put a chest beside it", STEP_TICKS)
                .place(1, 1, 0, () -> Blocks.CHEST, "facing=south", "")
                .step("Take the furnace away", STEP_TICKS)
                .take(0, 1, 0)
                .whatItIsFor("Proving the scenes shown step by step: their timeline and the sentence of each step.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Watch it play.", "Pause it.", "Go a step back.", "Show it in the world.")
                .whatCanGoWrong("It does not play.", "The reader asked for less movement: press Play.")
                .register();
        GUIDE.page("structure", IDEAS).titled("A Saved Structure").icon(() -> Items.STRUCTURE_BLOCK)
                .whatItIs("An entry with a hut saved with the game's structure block, read from its file.")
                .structure("A hut, saved with the structure block.", HUT, 100)
                .whatItIsFor("Proving that a manual draws a structure saved in the mod's assets.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Drag it.", "Hide a layer.")
                .whatCanGoWrong("No figure shows.", "Its file is missing, or it is too big for a scene.")
                .register();
        GUIDE.page("locked_idea", LOCKED).titled("A Locked Idea").icon(() -> Items.IRON_BARS)
                .needs(TestProgression.UNREACHED.id())
                .whatItIs("An entry its reader's team cannot do until it knows what no test gives.")
                .whatItIsFor("Proving that the contents say not yet beside what cannot be done yet, and hide nothing.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Read it.")
                .whatCanGoWrong("The contents say nothing beside it.", "Your team knows what it needs already.")
                .register();
        GUIDE.page("animation", IDEAS).titled("A Moving Picture").icon(() -> Items.REDSTONE_LAMP)
                .whatItIs("An entry with a picture that moves, two frames one under the other.")
                .animation("jstests:guide/blink", ANIMATION_WIDTH, ANIMATION_HEIGHT, 2, ANIMATION_TICKS,
                        "A lamp that blinks, two frames shown in turn.")
                .whatItIsFor("Proving that a picture's frames are shown in turn.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Watch it.")
                .whatCanGoWrong("It stands still.", "The reader asked for less movement.")
                .register();
        GUIDE.page("command", IDEAS).titled("A Command's Page").icon(() -> Items.COMMAND_BLOCK_MINECART)
                .whatItIs("An entry that prints a command's page from the command itself, the way its manual page"
                        + " at a prompt is made.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "dir"))
                .whatItIsFor("Proving that a manual and a machine say the same about a command.")
                .howToGetIt("Open the test manual.")
                .howToUseIt("Read it.")
                .whatCanGoWrong("The page says it is missing.", "No mod registered the command's page.")
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
        for (final String style : BOOK_STYLES) {
            GUIDE.manual("styled_" + style).titled("Styled Test Book").style("jsc:" + style)
                    .chapters("jstests", SHARED.id())
                    .about("This book holds the test chapters in the look of one of J's Computers' books.")
                    .priority(-3).register();
        }
    }

    private TestGuide() {
    }

    /** The test book drawn in that style of J's Computers' books. */
    public static String styledBook(final String style) {
        return "jstests:styled_" + style;
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
