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
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;
import net.minecraft.world.item.Items;

/**
 * A chapter, a manual and the item that opens it, to prove the Core's manuals: every kind of block an entry is made
 * of, a special block another mod draws, an entry an item's page opens to, and links between entries.
 */
public final class TestGuide {

    /** The test manual's id. */
    public static final String MANUAL = "jstests:test_manual";
    /** The entries the tests open. */
    public static final String PRESS = "jstests:press";
    public static final String SWATCH = "jstests:swatch";
    public static final String LONG = "jstests:long";
    /** The kind of special block the test mod draws. */
    public static final String SWATCH_KIND = "jstests:swatch";

    private static final ModGuide GUIDE = TestSounds.CONTENT.guide();

    static {
        GUIDE.chapter().titled("Test Mod").order(90).tab("#FFD04040").register();
    }

    public static final ModGuide.SectionRef MACHINES = GUIDE.section("machines").titled("Test Machines")
            .icon(() -> TestPress.UPGRADE).register();
    public static final ModGuide.SectionRef IDEAS = GUIDE.section("ideas").titled("Test Ideas")
            .icon(() -> Items.PAPER).register();

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
        GUIDE.manual("test_manual").titled("Test Manual").cover("J's Tech Series", "Test Library")
                .edition("First Test Edition").partNumber("JTS-TEST").style(CoreGuide.BINDER).chapters("jstests")
                .about("This manual holds the test mod's chapter, to prove the Core's manuals.").priority(-1)
                .register();
    }

    private TestGuide() {
    }

    /** Declares the chapter, its entries, the manual and its item, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }
}
