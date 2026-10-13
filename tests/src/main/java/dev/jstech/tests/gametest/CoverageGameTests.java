/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.gui.help.HelpShelf;
import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideProse;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.guide.TextSize;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestFetchCommand;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What J's Computers' books must say something about, area by area: every command each system runs, in the book of
 * that system; every program; every system; every verb and clause of the network's query language, in Learning IQL;
 * every member of the Sigma library and every error the Sigma compiler reports, in The Book of Sigma. An area is held
 * to it once the slice writing its pages switches it on; until then the test counts what is still to be written and
 * says so in the log, so the gap is always known.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CoverageGameTests {

    private static final String ARENA = "empty";
    /** The areas held to it: every one, now that every book is written. */
    private static final Set<Area> ENFORCED = EnumSet.allOf(Area.class);
    /** The clauses of the query language, which its parser reads by their words. */
    private static final List<String> IQL_CLAUSES = List.of("FROM", "TO", "IF", "WHERE", "ORDER BY", "LIMIT",
            "PRIORITY");
    /** How many missing pages a failure names. */
    private static final int NAMED = 12;
    /** The commands this test mod adds to every shell, which no player's book has a page for. */
    private static final Set<String> TEST_COMMANDS = Set.of(TestFetchCommand.NAME);
    /** What separates the words of a piece of text. */
    private static final Pattern GAP = Pattern.compile("[^A-Za-z0-9_.#-]+");
    /** The points that end a word, as a sentence's last word ends. */
    private static final Pattern END = Pattern.compile("\\.+ ");

    private CoverageGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void books_coverEveryAreaTheirSlicesHaveSwitchedOn(final GameTestHelper helper) {
        final IGuideText text = new English();
        final Map<String, List<GuideEntry>> books = books();
        final Map<Area, List<String>> missing = new HashMap<>();
        missing.put(Area.COMMANDS, commands(books));
        // The programs this test mod installs for its own tests are no player's to read about.
        missing.put(Area.PROGRAMS, named(OsRegistry.programs().stream()
                .filter(program -> !program.id().getNamespace().equals(JsTests.MODID))
                .map(ProgramSpec::displayName).toList(), allEntries(books), text));
        missing.put(Area.SYSTEMS, named(OsRegistry.oses().stream().map(OsDef::displayName).toList(),
                allEntries(books), text));
        final List<String> iql = new ArrayList<>();
        for (final IqlVerb verb : IqlVerb.values()) {
            iql.add(verb.name());
        }
        iql.addAll(IQL_CLAUSES);
        missing.put(Area.IQL, words(iql, books.getOrDefault(ComputersGuide.LEARNING_IQL, List.of()), text));
        final List<String> members = new ArrayList<>();
        for (final TypeSpec type : SystemApi.types()) {
            for (final IMemberSpec member : type.members()) {
                members.add(type.name() + "." + member.id().name());
            }
        }
        missing.put(Area.SIGMA_LIBRARY, words(members, books.getOrDefault(ComputersGuide.BOOK_OF_SIGMA, List.of()),
                text));
        final List<String> errors = new ArrayList<>();
        for (final SigmaError error : SigmaError.values()) {
            errors.add(error.code());
        }
        missing.put(Area.SIGMA_ERRORS, words(errors, books.getOrDefault(ComputersGuide.BOOK_OF_SIGMA, List.of()),
                text));
        final List<String> wrong = new ArrayList<>();
        for (final Area area : Area.values()) {
            final List<String> left = missing.get(area);
            if (ENFORCED.contains(area) && !left.isEmpty()) {
                wrong.add(area + ": " + String.join(", ", left.subList(0, Math.min(NAMED, left.size())))
                        + (left.size() > NAMED ? " and " + (left.size() - NAMED) + " more" : ""));
            } else if (!left.isEmpty()) {
                JsTests.LOGGER.info("Coverage: {} still has {} without a page, until its slice switches it on",
                        area, left.size());
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every area switched on is covered; not " + wrong);
        helper.succeed();
    }

    /** Every book every mod declares, by its id, as the entries of the chapters it binds in print order. */
    private static Map<String, List<GuideEntry>> books() {
        final Map<String, List<GuideEntry>> byChapter = new HashMap<>();
        final Map<String, String> chapterOfSection = new HashMap<>();
        final List<ModGuide> guides = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            guides.addAll(content.declaredGuides());
        }
        for (final ModGuide guide : guides) {
            guide.declaredSections().forEach(section -> chapterOfSection.put(section.id(), section.chapter()));
        }
        for (final ModGuide guide : guides) {
            for (final GuideEntry entry : guide.declaredEntries()) {
                byChapter.computeIfAbsent(chapterOfSection.getOrDefault(entry.section(), ""),
                        chapter -> new ArrayList<>()).add(entry);
            }
        }
        final Map<String, List<GuideEntry>> books = new HashMap<>();
        for (final ModGuide guide : guides) {
            guide.declaredManuals().forEach(manual -> {
                final List<GuideEntry> entries = new ArrayList<>();
                byChapter.forEach((chapter, held) -> {
                    if (manual.holdsEvery() || manual.chapters().contains(chapter)) {
                        entries.addAll(held);
                    }
                });
                books.put(manual.id(), entries);
            });
        }
        return books;
    }

    private static List<GuideEntry> allEntries(final Map<String, List<GuideEntry>> books) {
        final Set<String> seen = new HashSet<>();
        final List<GuideEntry> out = new ArrayList<>();
        books.values().forEach(entries -> entries.stream().filter(entry -> seen.add(entry.id())).forEach(out::add));
        return out;
    }

    /** Every command each system runs that the book of that system prints no page of, as "system: command". */
    private static List<String> commands(final Map<String, List<GuideEntry>> books) {
        final List<String> missing = new ArrayList<>();
        for (final OsDef os : OsRegistry.oses()) {
            final KernelDef kernel = OsRegistry.getKernel(os.kernelId());
            if (kernel == null) {
                continue;
            }
            final Set<String> printed = new HashSet<>();
            for (final GuideEntry entry : books.getOrDefault(HelpShelf.bookFor(os.platform(), os.installMode()),
                    List.of())) {
                entry.blocks().stream().filter(block -> block instanceof GuideBlock.Command)
                        .forEach(block -> printed.add(((GuideBlock.Command) block).source()));
            }
            for (final ICliCommand command : CliCommands.commandsFor(kernel.shellFamily())) {
                if (!TEST_COMMANDS.contains(command.name()) && CommandGuidePages.runsOn(command.scope(), os)
                        && CommandGuidePages.pagesOf(os.platform(),
                        command.name()).stream().noneMatch(printed::contains)) {
                    missing.add(os.displayName() + ": " + command.name());
                }
            }
        }
        return missing;
    }

    /** The names no entry's title or explained word holds. */
    private static List<String> named(final List<String> names, final List<GuideEntry> entries,
                                      final IGuideText text) {
        final StringBuilder heads = new StringBuilder();
        for (final GuideEntry entry : entries) {
            heads.append(text.text(entry.titleKey())).append('\n');
            for (final GuideBlock block : entry.blocks()) {
                if (block instanceof GuideBlock.Define define) {
                    heads.append(text.text(define.termKey())).append('\n');
                }
            }
        }
        final String all = heads.toString().toLowerCase(Locale.ROOT);
        return names.stream().filter(name -> !name.isBlank() && !all.contains(name.toLowerCase(Locale.ROOT)))
                .distinct().toList();
    }

    /** The words that stand nowhere whole in a book's text, its tables, its listings and its sessions. */
    private static List<String> words(final List<String> wanted, final List<GuideEntry> entries,
                                      final IGuideText text) {
        final StringBuilder all = new StringBuilder(" ");
        for (final GuideEntry entry : entries) {
            all.append(GuideProse.wordsOf(entry, text)).append(' ');
            for (final GuideBlock block : entry.blocks()) {
                switch (block) {
                    case GuideBlock.Table table -> {
                        table.headKeys().forEach(key -> all.append(text.text(key)).append(' '));
                        table.rows().forEach(row -> row.cells().forEach(cell -> all.append(text.valueText(cell))
                                .append(' ')));
                    }
                    case GuideBlock.Listing listing -> all.append(listing.text()).append(' ');
                    case GuideBlock.Session session -> session.printed().forEach(line -> all.append(line).append(' '));
                    default -> {
                    }
                }
            }
        }
        // The point closing a sentence is no part of the word before it: "with its ComponentAction.Value."
        final String written = " " + END.matcher(GAP.matcher(all.toString()).replaceAll(" ") + " ").replaceAll(" ");
        return wanted.stream().filter(word -> !written.contains(" " + GAP.matcher(word).replaceAll(" ") + " "))
                .distinct().toList();
    }

    /** What the books must cover, each switched on by the slice that writes its pages. */
    private enum Area {
        COMMANDS, PROGRAMS, SYSTEMS, IQL, SIGMA_LIBRARY, SIGMA_ERRORS
    }

    /** The manuals' words in English, as the server has them. */
    private record English() implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            return Language.getInstance().getOrDefault(key, key);
        }

        @Override
        public int width(final String text, final TextSize size) {
            return text.length() * 6;
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return 0;
        }

        @Override
        public String amount(final long value, final String unit) {
            return value + " " + unit;
        }
    }
}
