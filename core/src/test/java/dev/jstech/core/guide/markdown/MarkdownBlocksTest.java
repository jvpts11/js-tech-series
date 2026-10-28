/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.guide.GuideBlock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarkdownBlocksTest {

    @Test
    void parse_takesTheFirstHeadingAsTheTitleAndJoinsAParagraphsLines() {
        final MarkdownBlocks.Parsed parsed = parse("""
                # Kiln

                ## What it is

                A machine that fires clay
                into bricks, **with energy**, see [the eras](mymod:eras).
                """);
        assertEquals(Optional.of("Kiln"), parsed.title());
        assertEquals(List.of(new GuideBlock.Heading("What it is"), new GuideBlock.Paragraph(
                "A machine that fires clay into bricks, with energy, see [the eras](mymod:eras).")), parsed.blocks());
        assertTrue(parsed.problems().isEmpty());
    }

    @Test
    void parse_readsNumberedStepsAndBulletsEachTheirOwnWay() {
        final List<GuideBlock> blocks = parse("""
                1. Place it.
                2. Give it energy,
                   from a generator.

                - One thing
                - Another
                """).blocks();
        assertEquals(new GuideBlock.Steps(List.of("Place it.", "Give it energy, from a generator.")), blocks.get(0));
        assertEquals(new GuideBlock.Paragraph("\u2022 One thing"), blocks.get(1));
        assertEquals(new GuideBlock.Paragraph("\u2022 Another"), blocks.get(2));
    }

    @Test
    void parse_readsTheAlertsAsNotesWarningsProblemsWordsAndSeeLines() {
        final List<GuideBlock> blocks = parse("""
                > A plain quote.

                > [!WARNING]
                > It burns.

                > [!PROBLEM] It stops halfway.
                > It ran out of energy.

                > [!PROBLEM]
                > It makes nothing.
                > It has no clay.

                > [!DEFINE] FE
                > Forge Energy.

                > [!SEE] mymod:generator, mymod:press
                """).blocks();
        assertEquals(new GuideBlock.Note("A plain quote."), blocks.get(0));
        assertEquals(new GuideBlock.Warning("It burns."), blocks.get(1));
        assertEquals(new GuideBlock.Problems(List.of(new GuideBlock.Problem("It stops halfway.",
                "It ran out of energy."), new GuideBlock.Problem("It makes nothing.", "It has no clay."))),
                blocks.get(2), "problems one after another make one list");
        assertEquals(new GuideBlock.Define("FE", "Forge Energy."), blocks.get(3));
        assertEquals(new GuideBlock.SeeAlso(List.of("mymod:generator", "mymod:press")), blocks.get(4));
    }

    @Test
    void parse_readsATableWithItsCaptionAndCodeCellsAsLiterals() {
        final List<GuideBlock> blocks = parse("""
                Table: Its numbers

                | Property | Value |
                | --- | --- |
                | Uses | {{mymod:kiln_use}} |
                | Model | `KX-1` |
                """).blocks();
        final GuideBlock.Table table = assertInstanceOf(GuideBlock.Table.class, blocks.get(0));
        assertEquals("Its numbers", table.captionKey());
        assertEquals(List.of("Property", "Value"), table.headKeys());
        assertEquals(new GuideBlock.GuideValue.Words("{{mymod:kiln_use}}"), table.rows().get(0).cells().get(1));
        assertEquals(new GuideBlock.GuideValue.Literal("KX-1"), table.rows().get(1).cells().get(1));
    }

    @Test
    void parse_readsAnItemsFigureAndAPictureTheSizeItsTitleSays() {
        final List<GuideBlock> blocks = parse("""
                ![The kiln, from the front](item:mymod:kiln)

                ![Its door opening](mymod:gui/guide/door "64x48 frames=8 ticks=2")

                ![A picture of no size](mymod:gui/guide/door)
                """).blocks();
        assertEquals(new GuideBlock.Figure("mymod:kiln", "The kiln, from the front"), blocks.get(0));
        assertEquals(new GuideBlock.Picture("mymod:gui/guide/door", "", "{}", 64, 48, "Its door opening", 8, 2),
                blocks.get(1));
        assertEquals(2, blocks.size(), "a picture that says no size is left out");
    }

    @Test
    void parse_readsAListingAndTheFencesOfTheManualsOwnBlocks() {
        final List<GuideBlock> blocks = parse("""
                ```java Kiln.java
                class Kiln {}
                ```
                ```recipes
                type: mymod:firing
                making: mymod:brick
                ```
                ```multiblock
                id: mymod:coke_oven
                caption: The coke oven
                ```
                ```table
                source: mymod:cable_reach
                caption: How far
                heads: Cable | Reach
                ```
                ```session
                look: mymod:guide/terminal/dos
                title: MC-DOS
                prompt: C:\\>
                > dir
                 Directory of C:\\
                ```
                ```plan
                caption: How to place it
                part: mymod:kiln | The kiln
                optional: A second one
                ```
                ```command
                id: mymod:command/dir
                ```
                """).blocks();
        assertEquals(new GuideBlock.Listing("Kiln.java", "java", List.of("class Kiln {}")), blocks.get(0));
        assertEquals(new GuideBlock.Recipes("mymod:firing", "mymod:brick"), blocks.get(1));
        assertEquals(new GuideBlock.Multiblock("mymod:coke_oven", GuideBlock.SceneView.CORNER, 120,
                "The coke oven"), blocks.get(2));
        assertEquals(new GuideBlock.Table("How far", List.of("Cable", "Reach"), List.of(), "mymod:cable_reach"),
                blocks.get(3));
        assertEquals(new GuideBlock.Session("mymod:guide/terminal/dos", "MC-DOS", List.of(
                new GuideBlock.SessionLine("C:\\>", "dir", true),
                new GuideBlock.SessionLine("", " Directory of C:\\", false))), blocks.get(4));
        assertEquals(new GuideBlock.Plan("How to place it", List.of(new GuideBlock.PlanPart("mymod:kiln",
                "The kiln", false), new GuideBlock.PlanPart("", "A second one", true))), blocks.get(5));
        assertEquals(new GuideBlock.Command("mymod:command/dir"), blocks.get(6));
    }

    @Test
    void parse_readsABlockWrittenAsJsonThroughWhatReadsIt() {
        final GuideBlock scene = new GuideBlock.Note("read from JSON");
        final IJsonBlocks json = new IJsonBlocks() {
            @Override
            public Set<String> kinds() {
                return Set.of("scene");
            }

            @Override
            public Optional<GuideBlock> read(final String kind, final String written) {
                return written.contains("blocks") ? Optional.of(scene) : Optional.empty();
            }
        };
        final MarkdownBlocks.Parsed parsed = MarkdownBlocks.parse("""
                ```scene
                {"blocks": [], "height": 80, "caption": ""}
                ```
                ```scene
                {"nothing": true}
                ```
                """, 1, json);
        assertEquals(List.of(scene), parsed.blocks());
        assertEquals(1, parsed.problems().size(), "JSON that reads as nothing is said");
    }

    @Test
    void parse_readsTheBreaksAndLeavesOtherCommentsOut() {
        assertEquals(List.of(new GuideBlock.Break(GuideBlock.BreakKind.COLUMN),
                new GuideBlock.Break(GuideBlock.BreakKind.PAGE)), parse("""
                <!-- column -->
                <!-- a note to whoever writes -->
                <!-- page -->
                """).blocks());
    }

    @Test
    void parse_saysOnWhichLineAFileIsWrong() {
        final MarkdownBlocks.Parsed parsed = MarkdownBlocks.parse("""
                # Title

                > [!PROBLEM] Only the problem.

                ```recipes
                making: mymod:brick
                ```
                """, 5, IJsonBlocks.NONE);
        assertEquals(List.of(7, 9), parsed.problems().stream().map(ManualFileException::line).toList());
    }

    @Test
    void parse_readsACautionAsACautionAndAnImportantLineAsAWarning() {
        final List<GuideBlock> blocks = parse("""
                > [!CAUTION]
                > No launch in a thunderstorm.

                > [!IMPORTANT] It burns.
                """).blocks();
        assertEquals(new GuideBlock.Caution("No launch in a thunderstorm."), blocks.get(0));
        assertEquals(new GuideBlock.Warning("It burns."), blocks.get(1));
    }

    @Test
    void parse_readsAListOpeningWithABoxAsAChecklist() {
        final List<GuideBlock> blocks = parse("""
                - [ ] Rocket ... on pad
                - [!] Cryo tanks ..... venting
                - [ ] Climb

                1. Light the engines.
                """).blocks();
        assertEquals(new GuideBlock.Checklist(List.of(new GuideBlock.Check("Rocket", "on pad", false),
                new GuideBlock.Check("Cryo tanks", "venting", true), new GuideBlock.Check("Climb", "", false))),
                blocks.get(0));
        assertEquals(new GuideBlock.Steps(List.of("Light the engines.")), blocks.get(1),
                "a list with no box stays as it was");
    }

    @Test
    void parse_readsCardsCountingTheirStepsOnFromTheCardBefore() {
        final List<GuideBlock> blocks = parse("""
                ```card
                title: Leaving a world 1/2
                [ ] On the pad: tower beside, hoses on
                [ ] Ignition: engines lit
                ```

                ```card
                title: Leaving a world 2/2
                - [!] Staging: each spent stage comes loose
                ```
                """).blocks();
        assertEquals(new GuideBlock.Card("Leaving a world 1/2", 1, List.of(
                new GuideBlock.CardStep("On the pad", "tower beside, hoses on", false),
                new GuideBlock.CardStep("Ignition", "engines lit", false))), blocks.get(0));
        assertEquals(new GuideBlock.Card("Leaving a world 2/2", 3, List.of(
                new GuideBlock.CardStep("Staging", "each spent stage comes loose", true))), blocks.get(1));
    }

    private static MarkdownBlocks.Parsed parse(final String body) {
        return MarkdownBlocks.parse(body, 1, IJsonBlocks.NONE);
    }
}
