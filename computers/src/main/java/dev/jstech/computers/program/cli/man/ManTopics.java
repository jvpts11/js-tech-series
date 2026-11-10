/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import dev.jstech.computers.os.Platform;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A manual page about something to know rather than a command to run.
 *
 * <p>FreeBSD's welcome points a newcomer at {@code man intro}, but a real FreeBSD has no {@code intro} a
 * player could also type at the prompt and have it quietly succeed: it is a page and nothing else. {@code
 * man}, {@code whatis} and {@code apropos} fall back here once a command by that name is not found, so a
 * topic answers to the same three tools a command does without ever appearing in a listing of commands, in
 * Tab's candidates, or as something the shell will run.
 */
@TextHolder
public final class ManTopics {

    private static final TextKey INTRO_SUMMARY =
            TextKey.of("jsc.cli.man.intro.summary", "introduction to the commands run at a shell prompt");
    private static final TextKey INTRO_ABOUT_1 = TextKey.of("jsc.cli.man.intro.about.1", "A command is a small"
            + " program that does one thing; a shell runs the one typed at its prompt and shows what it writes"
            + " back. Most of what a machine can do is one of these, joined to the others through the files and"
            + " the pipes that carry a line from one to the next.");
    private static final TextKey INTRO_ABOUT_2 = TextKey.of("jsc.cli.man.intro.about.2", "man <command> opens any"
            + " of their pages; apropos <word> searches every page this machine has for that word; whatis"
            + " <command> gives the one line a page opens with, which is enough to tell whether it is worth"
            + " reading at all.");

    /** The topic FreeBSD's welcome points a newcomer at, since the system has no interactive help of its own. */
    private static final Topic INTRO = new Topic("intro", INTRO_SUMMARY,
            List.of(INTRO_ABOUT_1, INTRO_ABOUT_2), List.of("man", "apropos", "whatis"),
            Set.of(Platform.FREEBSD, Platform.OPENBSD));

    private static final TextKey AFTERBOOT_SUMMARY = TextKey.of("jsc.cli.man.afterboot.summary",
            "things to check after the first complete boot");
    private static final TextKey AFTERBOOT_ABOUT_1 = TextKey.of("jsc.cli.man.afterboot.about.1", "OpenBSD comes up"
            + " at its terminal with only what it needs switched on, which is what secure by default means: rcctl"
            + " ls on lists what runs, rcctl enable switches a service on for every start and rcctl start starts it"
            + " now.");
    private static final TextKey AFTERBOOT_ABOUT_2 = TextKey.of("jsc.cli.man.afterboot.about.2", "Packages come"
            + " from the network's Mirror with pkg_add, are listed with pkg_info and taken off with pkg_delete. A"
            + " desktop installed comes up only once rcctl enable xenodm has switched its display manager on.");
    private static final TextKey AFTERBOOT_ABOUT_3 = TextKey.of("jsc.cli.man.afterboot.about.3", "syspatch installs"
            + " the system's own patches from the Mirror, which syspatch -c lists first; /etc/pf.conf holds the"
            + " firewall's rules, which pfctl -si reports on; and doas runs a command as another user.");

    /** The page OpenBSD's welcome points a newcomer at: what to do with a system fresh from its installer. */
    private static final Topic AFTERBOOT = new Topic("afterboot", AFTERBOOT_SUMMARY,
            List.of(AFTERBOOT_ABOUT_1, AFTERBOOT_ABOUT_2, AFTERBOOT_ABOUT_3),
            List.of("rcctl", "pkg_add", "syspatch", "doas"), Set.of(Platform.OPENBSD));

    private static final List<Topic> ALL = List.of(INTRO, AFTERBOOT);

    private ManTopics() {
    }

    /** The topic named that on a machine of that platform, or none. */
    public static Optional<Topic> find(final String name, final Platform platform) {
        for (final Topic topic : ALL) {
            if (topic.name().equals(name) && topic.platforms().contains(platform)) {
                return Optional.of(topic);
            }
        }
        return Optional.empty();
    }

    /** Every topic a machine of that platform has, for {@code apropos} to search. */
    public static List<Topic> onPlatform(final Platform platform) {
        final List<Topic> out = new ArrayList<>();
        for (final Topic topic : ALL) {
            if (topic.platforms().contains(platform)) {
                out.add(topic);
            }
        }
        return out;
    }

    /**
     * One topic: a name, the one line {@code whatis} answers with, a page's worth of paragraphs, what it
     * points to, and which platforms have it.
     */
    public record Topic(String name, TextKey summaryKey, List<TextKey> descriptionKeys, List<String> seeAlso,
                        Set<Platform> platforms) {

        /** The one line {@code whatis} and {@code apropos} answer with. */
        public Text summary() {
            return this.summaryKey.text();
        }

        /** The page's paragraphs, in the player's language where they are read. */
        public List<Text> description() {
            final List<Text> out = new ArrayList<>(this.descriptionKeys.size());
            for (final TextKey key : this.descriptionKeys) {
                out.add(key.text());
            }
            return out;
        }

        /** Whether this topic's name or summary answers to that word, the question {@code apropos} asks. */
        public boolean answersTo(final String text) {
            return ManPage.matches(this.name, summary(), text);
        }
    }
}
