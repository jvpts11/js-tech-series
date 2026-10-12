/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaSemantics;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every whole program the manuals print in Σ or Σ# compiles as printed, so a reader who types one in gets a program
 * and not a page of errors; a change to the language that breaks one shows here, with the listing's name.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SigmaListingsGameTests {

    private static final String ARENA = "empty";

    private SigmaListingsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void wholeProgramListings_compileAsPrinted(final GameTestHelper helper) {
        final List<String> broken = new ArrayList<>();
        int checked = 0;
        for (final ModContent content : ModContent.all()) {
            for (final ModGuide guide : content.declaredGuides()) {
                for (final GuideEntry entry : guide.declaredEntries()) {
                    for (final GuideBlock block : entry.blocks()) {
                        if (block instanceof GuideBlock.Listing listing && wholeProgram(listing)) {
                            checked++;
                            final LanguageLevel level = "sg".equals(listing.language())
                                    ? LanguageLevel.SIGMA : LanguageLevel.SIGMA_SHARP;
                            final List<SourceFile> source = List.of(new SourceFile(listing.name(), listing.text()));
                            // A listing of types alone, with no place to start, is checked as types and not run.
                            final SigmaSemantics.Result result = startsSomewhere(listing.text())
                                    ? SigmaSemantics.checkProgram(source, level) : SigmaSemantics.check(source);
                            if (!result.ok()) {
                                broken.add(entry.id() + " " + listing.name() + ": "
                                        + String.join(" | ", result.lines()));
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(checked > 0, "the manuals print whole programs to check");
        helper.assertTrue(broken.isEmpty(), "every whole program listing compiles; not " + broken);
        helper.succeed();
    }

    /* Whether a listing has a place for a program to start: a Main, or a script. */
    private static boolean startsSomewhere(final String text) {
        return text.contains(" Main(") || text.contains("IScript") || text.contains(": Script");
    }

    /* A listing of the languages that stands on its own: its namespace and a type of its own. */
    private static boolean wholeProgram(final GuideBlock.Listing listing) {
        final String text = listing.text();
        return ("sg".equals(listing.language()) || "sgs".equals(listing.language()))
                && text.contains("namespace ") && (text.contains("class ") || text.contains("struct "));
    }
}
