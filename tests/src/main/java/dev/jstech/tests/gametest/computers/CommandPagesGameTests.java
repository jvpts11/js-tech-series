/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.LiveInstallCommands;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.computers.program.cli.man.ManPage;
import dev.jstech.computers.program.cli.man.ManualEntries;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideCommand;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.text.ITextLanguage;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextFormat;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestGuide;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every command's page in the manuals as the game holds it once loaded: one for every command every system runs, each
 * saying what the command says about itself, the words its manual page at a prompt is made of, and naming that system;
 * one for every verb of a live install medium; and a manual reading a command's page the way that manual page reads.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CommandPagesGameTests {

    private static final String ARENA = "empty";
    /** How many wrong pages a failure names. */
    private static final int NAMED = 12;

    private CommandPagesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void pages_sayWhatEveryCommandOfEverySystemSaysAboutItself(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        int pages = 0;
        for (final OsDef os : OsRegistry.oses()) {
            final KernelDef kernel = OsRegistry.getKernel(os.kernelId());
            if (kernel == null) {
                continue;
            }
            for (final ICliCommand command : CliCommands.commandsFor(kernel.shellFamily())) {
                if (!CommandGuidePages.runsOn(command.scope(), os)) {
                    continue;
                }
                pages++;
                final String id = CommandGuidePages.id(os.platform(), command.name());
                final Optional<GuideCommand> page = GuideFacts.commandOf(id);
                if (page.isEmpty()) {
                    wrong.add(id + " has no page");
                } else if (!saysTheSame(page.get(), command)) {
                    wrong.add(id + " says otherwise than the " + command.name() + " of " + os.displayName());
                } else if (page.get().systems().stream().noneMatch(system -> system.english()
                        .contains(os.displayName()))) {
                    wrong.add(id + " does not name " + os.displayName());
                }
            }
        }
        for (final ICliCommand verb : LiveInstallCommands.all()) {
            if (GuideFacts.commandOf(CommandGuidePages.liveId(verb.name())).isEmpty()) {
                wrong.add("the live medium's " + verb.name() + " has no page");
            }
        }
        helper.assertTrue(pages > 100, "every system runs its commands: " + pages + " found");
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " pages are wrong: "
                + String.join("; ", wrong.subList(0, Math.min(NAMED, wrong.size()))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void manual_readsACommandsPageAsItsManPageReadsIt(final GameTestHelper helper) {
        final OsDef dos = OsRegistry.oses().stream().filter(os -> os.platform() == Platform.MC_DOS).findFirst()
                .orElseThrow();
        // A command with options and examples, so every part of its page is compared.
        final ICliCommand command = CliCommands.commandsFor(ShellFamily.DOS).stream()
                .filter(candidate -> CommandGuidePages.runsOn(candidate.scope(), dos))
                .filter(candidate -> !candidate.options().isEmpty() && !candidate.examples().isEmpty())
                .findFirst().orElseThrow();
        final GuideEntry entry = new GuideEntry("jstests:command_page", "jstests:command_pages", 0,
                "jstests.command_page.title", "", List.of(), List.of(new GuideBlock.Command(
                CommandGuidePages.id(Platform.MC_DOS, command.name()))));
        final ManualReader reader = new ManualReader(new GuideManual("jstests:command_manual", "manual.title",
                List.of(), "", "", "jscore:binder", List.of("jstests"), List.of(), 0, ""), new GuideContents(List.of(
                new GuideContents.Chapter(new GuideChapter("jstests", 0, "chapter.title", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("jstests:command_pages", 0, "section.title", ""),
                                List.of(entry)))))), new English());

        final List<ManualReader.Piece> pieces = reader.article(entry.id()).orElseThrow().pieces();
        final String synopsis = ManPage.synopsis(command, CliStyle.PLAIN).text(ITextLanguage.ENGLISH).strip();
        helper.assertTrue(pieces.contains(new ManualReader.Piece.Code("", "", List.of(synopsis))),
                "the manual types " + command.name() + " as its manual page does: " + synopsis);
        final List<CliLine> manOptions = ManPage.of(command).stream()
                .filter(section -> section.heading() == ManPage.Heading.OPTIONS).findFirst().orElseThrow().lines();
        final ManualReader.Piece.Table options = pieces.stream().filter(piece -> piece instanceof
                ManualReader.Piece.Table).map(piece -> (ManualReader.Piece.Table) piece).findFirst().orElseThrow();
        helper.assertTrue(options.rows().size() * 2 == manOptions.size(), "an option a row, as the page has them");
        for (int i = 0; i < options.rows().size(); i++) {
            final List<String> cells = options.rows().get(i).cells();
            helper.assertTrue(cells.get(0).equals(manOptions.get(2 * i).text(ITextLanguage.ENGLISH))
                    && cells.get(1).equals(manOptions.get(2 * i + 1).text(ITextLanguage.ENGLISH).strip()),
                    "the option " + cells.get(0) + " says what the manual page says");
        }
        helper.succeed();
    }

    /*
     * The entry printing a command's page is that command's page on every system running it: the test manual prints
     * MC-DOS's dir, and Frames, whose dir is the same command, finds the same entry; a word no command answers to
     * finds none.
     */
    @GameTest(template = ARENA)
    public static void commandEntry_isTheCommandsPageOnEverySystemRunningIt(final GameTestHelper helper) {
        helper.assertTrue(CommandGuidePages.pagesOf(Platform.FRAMES, "dir").contains(
                CommandGuidePages.id(Platform.MC_DOS, "dir")), "Frames' dir is MC-DOS's dir");
        final Optional<String> dir = ManualEntries.commandEntry(Platform.MC_DOS, "dir");
        helper.assertTrue(dir.isPresent() && !dir.get().equals(TestGuide.COMMAND),
                "MC-DOS finds the entry of its own book printing dir's page, before the test mod's: " + dir);
        helper.assertTrue(ManualEntries.commandEntry(Platform.FRAMES, "DIR").equals(dir),
                "and so does Frames, whatever the word's case");
        helper.assertTrue(ManualEntries.commandEntry(Platform.MC_DOS, "nonsense").isEmpty(),
                "a word no command answers to finds no entry");
        helper.succeed();
    }

    /*
     * Every command page a book prints is one the game has. A page printed under a system the command never runs on
     * has no page behind it, and the book would show a line saying so where the page should be.
     */
    @GameTest(template = ARENA)
    public static void books_printOnlyCommandPagesThatExist(final GameTestHelper helper) {
        final List<String> missing = new ArrayList<>();
        int printed = 0;
        for (final ModContent content : ModContent.all()) {
            for (final ModGuide guide : content.declaredGuides()) {
                for (final GuideEntry entry : guide.declaredEntries()) {
                    for (final GuideBlock block : entry.blocks()) {
                        if (block instanceof GuideBlock.Command command) {
                            printed++;
                            if (GuideFacts.commandOf(command.source()).isEmpty()) {
                                missing.add(entry.id() + " prints " + command.source());
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(printed > 100, "the books print the commands' pages: " + printed + " found");
        helper.assertTrue(missing.isEmpty(), missing.size() + " printed pages do not exist: "
                + String.join("; ", missing.subList(0, Math.min(NAMED, missing.size()))));
        helper.succeed();
    }

    /** Whether a command's page says what the command says about itself, word for word in English. */
    private static boolean saysTheSame(final GuideCommand page, final ICliCommand command) {
        return page.name().equals(command.name())
                && page.summary().english().equals(command.summary().english())
                && page.usage().english().equals(command.usage().english())
                && english(page.description()).equals(english(command.description()))
                && page.options().stream().map(option -> option.flag() + " " + option.what().english()).toList()
                .equals(command.options().stream().map(option -> option.flag() + " " + option.what().english())
                        .toList())
                && page.examples().stream().map(example -> example.line() + " " + example.what().english()).toList()
                .equals(command.examples().stream().map(example -> example.line() + " " + example.what().english())
                        .toList())
                && page.seeAlso().equals(command.seeAlso());
    }

    private static List<String> english(final List<Text> texts) {
        return texts.stream().map(Text::english).toList();
    }

    /** The manuals' words in the language the server has loaded, which is English. */
    private static final class English implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            final String pattern = Language.getInstance().getOrDefault(key, key);
            if (args.length == 0) {
                return pattern;
            }
            final List<String> written = new ArrayList<>();
            for (final Object arg : args) {
                written.add(String.valueOf(arg));
            }
            return TextFormat.apply(pattern, written);
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
