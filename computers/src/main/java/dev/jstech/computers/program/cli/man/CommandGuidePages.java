/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import dev.jstech.computers.os.InstallMode;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CommandScope;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.LiveInstallCommands;
import dev.jstech.core.guide.GuideCommand;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every command's page in the manuals, read from the command itself: the same words its manual page at a prompt is
 * made of ({@link ManPage}), so what a manual says about a command and what a machine says never disagree.
 *
 * <p>A page is registered for each family of systems that has the command, under {@code jsc:command/<family>/<name>}
 * ({@code jsc:command/mc_dos/dir}, {@code jsc:command/frames/dir}, {@code jsc:command/linux/ls}), and one for each verb
 * of a live install medium under {@code jsc:command/live/<name>}. An entry prints one with
 * {@code .command(CommandGuidePages.id(Platform.MC_DOS, "dir"))}. Each page names every system the command runs on.
 */
@TextHolder
public final class CommandGuidePages {

    private static final TextKey FROM_PACKAGE = TextKey.of("jsc.guide.command.from_package",
            "%s, once %s is installed");
    private static final TextKey INSTALL_MEDIUM = TextKey.of("jsc.guide.command.install_medium",
            "the %s install medium");
    private static final String PREFIX = "jsc:command/";
    private static final String LIVE = "live";

    /** Every page registered for one command, by each of their ids: dir's page for MC-DOS and its page for Frames. */
    private static Map<String, List<String>> alike = Map.of();

    private CommandGuidePages() {
    }

    /**
     * Registers the page of every command of every system, once the systems and every mod's commands are known: what
     * each machine runs is the commands of its kernel's family that say they are on its system.
     */
    public static void register() {
        final Map<String, ICliCommand> pages = new LinkedHashMap<>();
        // The systems a command runs on, by the command; a command in two families' pages is one command.
        final Map<String, List<OnSystem>> found = new HashMap<>();
        for (final OsDef os : OsRegistry.oses()) {
            final KernelDef kernel = OsRegistry.getKernel(os.kernelId());
            if (kernel == null) {
                continue;
            }
            for (final ICliCommand command : CliCommands.commandsFor(kernel.shellFamily())) {
                if (runsOn(command.scope(), os)) {
                    pages.putIfAbsent(id(os.platform(), command.name()), command);
                    found.computeIfAbsent(key(command), key -> new ArrayList<>()).add(new OnSystem(os.displayName(),
                            command.scope().fromAPackage() ? packageName(command.scope().packageId()) : ""));
                }
            }
        }
        final Map<String, List<Text>> systems = new HashMap<>();
        found.forEach((key, on) -> systems.put(key, named(on)));
        final List<Text> media = new ArrayList<>();
        for (final OsDef os : OsRegistry.oses()) {
            if (os.installMode() != InstallMode.GUIDED) {
                media.add(INSTALL_MEDIUM.with(os.displayName()));
            }
        }
        for (final ICliCommand command : LiveInstallCommands.all()) {
            pages.putIfAbsent(liveId(command.name()), command);
            systems.put(key(command), media);
        }
        pages.forEach((id, command) -> GuideFacts.command(id, () -> page(command,
                systems.getOrDefault(key(command), List.of()))));
        final Map<String, List<String>> byCommand = new HashMap<>();
        pages.forEach((id, command) -> byCommand.computeIfAbsent(key(command), key -> new ArrayList<>()).add(id));
        final Map<String, List<String>> same = new HashMap<>();
        pages.forEach((id, command) -> same.put(id, List.copyOf(byCommand.get(key(command)))));
        alike = Map.copyOf(same);
    }

    /**
     * The ids of every page of the command typed by that name on a system of that family: its own first, then the
     * pages other families have for the same command, so an entry printing any of them is that command's page there.
     */
    public static List<String> pagesOf(final Platform platform, final String name) {
        final String own = id(platform, name);
        final List<String> out = new ArrayList<>();
        out.add(own);
        for (final String other : alike.getOrDefault(own, List.of())) {
            if (!other.equals(own)) {
                out.add(other);
            }
        }
        return out;
    }

    /** The id of the page of a command of that family of systems. */
    public static String id(final Platform platform, final String name) {
        return PREFIX + platform.serializedName() + "/" + name;
    }

    /** The id of the page of a verb of a live install medium. */
    public static String liveId(final String name) {
        return PREFIX + LIVE + "/" + name;
    }

    /** A command's page, made of what the command says about itself, run on those systems. */
    public static GuideCommand page(final ICliCommand command, final List<Text> systems) {
        final List<GuideCommand.Option> options = new ArrayList<>();
        for (final ICliCommand.Option option : command.options()) {
            options.add(new GuideCommand.Option(option.flag(), option.what()));
        }
        final List<GuideCommand.Example> examples = new ArrayList<>();
        for (final ICliCommand.Example example : command.examples()) {
            examples.add(new GuideCommand.Example(example.line(), example.what()));
        }
        return new GuideCommand(command.name(), command.summary(), command.usage(), command.description(), options,
                examples, systems, command.seeAlso());
    }

    /** Whether a command is on a system, as its shell asks before anything about the machine. */
    public static boolean runsOn(final CommandScope scope, final OsDef os) {
        return scope.onSystem(os.platform(), os.familyRank())
                && (scope.manager() == null || scope.manager() == os.packageManager());
    }

    /**
     * The systems as a page names them, each as a reader knows it and the package to install first after it; when
     * every one needs the same package it is said once, after the last, so a long list does not repeat it.
     */
    private static List<Text> named(final List<OnSystem> systems) {
        final String first = systems.getFirst().packageName();
        final boolean onePackage = systems.size() > 1 && !first.isEmpty()
                && systems.stream().allMatch(system -> system.packageName().equals(first));
        final List<Text> out = new ArrayList<>();
        for (int i = 0; i < systems.size(); i++) {
            final OnSystem system = systems.get(i);
            final boolean said = !system.packageName().isEmpty() && (!onePackage || i == systems.size() - 1);
            out.add(said ? FROM_PACKAGE.with(system.name(), system.packageName()) : Text.literal(system.name()));
        }
        return out;
    }

    /** A package's name as it is typed to install it: the path of its id. */
    private static String packageName(final String packageId) {
        final int colon = packageId.indexOf(':');
        return colon < 0 ? packageId : packageId.substring(colon + 1);
    }

    /** What tells one command from another: what it is, and the word that runs it. */
    private static String key(final ICliCommand command) {
        return command.getClass().getName() + "/" + command.name();
    }

    /**
     * A system a command runs on.
     *
     * @param name        the system as a reader knows it
     * @param packageName the package to install first for the command, or empty when the system has it already
     */
    private record OnSystem(String name, String packageName) {
    }
}
