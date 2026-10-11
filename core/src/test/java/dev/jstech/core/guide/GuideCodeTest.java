/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuideCodeTest {

    @Test
    void runs_coverEachLineEndToEndFillingTheGapsWithPlain() {
        final List<String> lines = List.of("int x = 5;");

        final List<List<GuideCode.Run>> runs = GuideCode.runs(lines, List.of(
                new GuideCode.Span(1, 1, 3, GuideCode.Ink.KEYWORD), new GuideCode.Span(1, 9, 1,
                        GuideCode.Ink.NUMBER)));

        assertEquals(List.of(new GuideCode.Run(0, 3, GuideCode.Ink.KEYWORD), new GuideCode.Run(3, 5,
                GuideCode.Ink.PLAIN), new GuideCode.Run(8, 1, GuideCode.Ink.NUMBER), new GuideCode.Run(9, 1,
                GuideCode.Ink.PLAIN)), runs.getFirst());
    }

    @Test
    void runs_cutAPieceAtTheEndOfItsLineAndDropOneOnNoLine() {
        final List<String> lines = List.of("ab");

        final List<List<GuideCode.Run>> runs = GuideCode.runs(lines, List.of(
                new GuideCode.Span(1, 2, 10, GuideCode.Ink.TEXT), new GuideCode.Span(4, 1, 1, GuideCode.Ink.NAME)));

        assertEquals(List.of(new GuideCode.Run(0, 1, GuideCode.Ink.PLAIN), new GuideCode.Run(1, 1,
                GuideCode.Ink.TEXT)), runs.getFirst());
    }

    @Test
    void role_colourEachKindInItsRoleOfTheStyle() {
        assertEquals(GuideStyle.CODE_KEYWORD, GuideCode.role(GuideCode.Ink.KEYWORD));
        assertEquals(GuideStyle.INK, GuideCode.role(GuideCode.Ink.SYMBOL));
        assertEquals(GuideStyle.CODE_COMMENT, GuideCode.role(GuideCode.Ink.COMMENT));
    }
}
