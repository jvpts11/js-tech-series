/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The Sigma reference, a chapter of the Guide to Operations (computers/docs/design/sigma.md): what a program can reach
 * and what each thing costs, namespace by namespace, written from the library itself, with the way into the Book of
 * Sigma for how a program is written, its errors and its versions. Every system's help reads it, as it reads every
 * manual: info sigma names its first page.
 */
public final class SigmaReferencePages {

    private SigmaReferencePages() {
    }

    /** Declares the reference's pages in its chapter: the way in, then one page for each namespace of the library. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef overview = guide.section("sigma_reference_overview").titled("The reference")
                .in(chapter).register();
        guide.page("sigma", overview).titled("Sigma and Sigma#")
                .paragraph("This chapter is the reference of Sigma and Sigma#, the languages the programs of your"
                        + " computers are written in. Each page after this one is a namespace of the library: every"
                        + " type and call in it, what each call gives back, what it costs in credits beyond the"
                        + " instruction that makes it, and the version of the language it came in.")
                .paragraph("You can read it in this binder or on any of your computers, through the system's help:"
                        + " info sigma on a Linux, the Help of a desktop, or the manuals of the terminal. The pages"
                        + " are written from the library itself, so they say what your machines run.")
                .paragraph("The Book of Sigma teaches the languages: the files a program is made of and the forms it"
                        + " takes ([](jsc:sigma_first_program), [](jsc:sigma_entry_points)), every error the compiler"
                        + " gives ([](jsc:sigma_reading_an_error)), the costs of running ([](jsc:call_costs)), the"
                        + " debugger ([](jsc:sigma_debugging)) and what each version brought"
                        + " ([](jsc:sigma_version_three)).")
                .seeAlso("jsc:sigma_first_program", "jsc:call_costs", "jsc:sigma_debugging")
                .register();
        final ModGuide.SectionRef library = guide.section("sigma_reference_library").titled("The library")
                .in(chapter).register();
        for (final String namespace : SigmaReferenceFacts.namespaces()) {
            guide.page("sigma_reference_" + SigmaReferenceFacts.pathOf(namespace), library).titled(namespace)
                    .paragraph("Every type of " + namespace + " and each of its calls, as you call them from Sigma#;"
                            + " Sigma has a part of the library, as the Book of Sigma says. Costs is what a call adds"
                            + " to the instruction that makes it, and Since is the version of the language it came in.")
                    .tableFrom(SigmaReferenceFacts.of(namespace), "The calls of " + namespace, "Call", "Gives",
                            "Costs", "Since")
                    .register();
        }
    }
}
