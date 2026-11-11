/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.time.GameCalendar;
import java.util.List;
import java.util.Locale;

/**
 * msgsvc: the conversations a server's Messenger Service keeps, looked through and deleted by whoever is at that
 * server, at its own screen or over ssh, KVM or Remote Control. The conversations live on the server and never reach
 * the game's chat, so this is where they end; a deleted one leaves every Messenger on the network without a word to
 * anybody (computers/docs/design/programs.md).
 */
@TextHolder
public final class MsgsvcCommand implements ICliCommand {

    /** The command's own word, which is also what the Messenger Service is run by. */
    public static final String NAME = "msgsvc";
    /** The Messenger Service's package, as a machine lists what it has installed. */
    public static final String SERVICE = "jsc:messenger_service";
    private static final TextKey SUMMARY = TextKey.of("jsc.cli.msgsvc.summary",
            "the conversations this server's Messenger Service keeps");
    private static final TextKey HELP_LIST = TextKey.of("jsc.cli.msgsvc.help.list",
            "the conversations this server keeps");
    private static final TextKey HELP_DELETE = TextKey.of("jsc.cli.msgsvc.help.delete", "delete conversation N");
    private static final TextKey HELP_DELETE_ALL = TextKey.of("jsc.cli.msgsvc.help.delete_all",
            "delete every conversation");
    private static final TextKey HELP_STATUS = TextKey.of("jsc.cli.msgsvc.help.status",
            "the service, its disk and its memory");
    private static final TextKey CONVERSATION = TextKey.of("jsc.cli.msgsvc.head.conversation", "CONVERSATION");
    private static final TextKey LINES = TextKey.of("jsc.cli.msgsvc.head.lines", "LINES");
    private static final TextKey SIZE = TextKey.of("jsc.cli.msgsvc.head.size", "SIZE");
    private static final TextKey LAST_SAID = TextKey.of("jsc.cli.msgsvc.head.last_said", "LAST SAID");
    private static final TextKey EVERYBODY = TextKey.of("jsc.cli.msgsvc.everybody", "Everybody");
    private static final TextKey SAID_AT = TextKey.of("jsc.cli.msgsvc.said_at", "Day %s, %s");
    private static final TextKey NONE_KEPT = TextKey.of("jsc.cli.msgsvc.none_kept",
            "This server keeps no conversations.");
    private static final TextKey DELETED = TextKey.of("jsc.cli.msgsvc.deleted",
            "Deleted \"%s\": %s lines, %s freed. It leaves every Messenger.");
    private static final TextKey DELETED_ONE = TextKey.of("jsc.cli.msgsvc.deleted_one",
            "Deleted \"%s\": 1 line, %s freed. It leaves every Messenger.");
    private static final TextKey DELETED_ALL = TextKey.of("jsc.cli.msgsvc.deleted_all",
            "Deleted %s conversations: %s lines, %s freed. They leave every Messenger.");
    private static final TextKey NO_SUCH = TextKey.of("jsc.cli.msgsvc.no_such",
            "No conversation %s: msgsvc list numbers them.");
    private static final TextKey NOT_HERE = TextKey.of("jsc.cli.msgsvc.not_here",
            "This server runs no Messenger Service.");
    private static final TextKey STATUS_RUNNING = TextKey.of("jsc.cli.msgsvc.status.running",
            "Messenger Service on %s: running");
    private static final TextKey STATUS_STOPPED = TextKey.of("jsc.cli.msgsvc.status.stopped",
            "Messenger Service on %s: stopped");
    private static final TextKey STATUS_KEPT = TextKey.of("jsc.cli.msgsvc.status.kept", "Conversations");
    private static final TextKey STATUS_DISK = TextKey.of("jsc.cli.msgsvc.status.disk", "On the disk");
    private static final TextKey STATUS_MEMORY = TextKey.of("jsc.cli.msgsvc.status.memory", "In memory");
    private static final TextKey STATUS_OPEN = TextKey.of("jsc.cli.msgsvc.status.open", "Messengers open");
    private static final TextKey STATUS_COUNT = TextKey.of("jsc.cli.msgsvc.status.count", "%s, %s lines");
    /* Where each column of the list begins, the numbers lined up against the end of theirs. */
    private static final int NAME_AT = 3;
    private static final int LINES_AT = 25;
    private static final int LINES_WIDTH = 7;
    private static final int SIZE_WIDTH = 10;
    private static final int SAID_AT_COLUMN = 45;
    private static final int HELP_AT = 23;
    private static final long MEGABYTE = 1024L * 1024L;
    private static final String ALL = "--all";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.SOFTWARE;
    }

    @Override
    public Text summary() {
        return SUMMARY.text();
    }

    /** Nothing but the words it takes, which are typed as they are in every language. */
    @Override
    public Text usage() {
        return Text.literal("list|delete N|delete --all|status");
    }

    /** On the server that runs the service, which is the one machine whose disk keeps the conversations. */
    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().onHost(HostScope.SERVER).fromPackage(SERVICE);
    }

    @Override
    public void run(final CliContext ctx) {
        final IKeptConversations kept = ctx.computer().keptConversations();
        if (kept == null) {
            ctx.out().error(NOT_HERE);
            return;
        }
        final String verb = ctx.arg(0).toLowerCase(Locale.ROOT);
        switch (verb) {
            case "list" -> list(ctx, kept);
            case "delete" -> delete(ctx, kept, ctx.arg(1));
            case "status" -> status(ctx, kept);
            default -> help(ctx);
        }
    }

    private static void list(final CliContext ctx, final IKeptConversations kept) {
        final List<MessengerLog.Conversation> conversations = kept.list();
        if (conversations.isEmpty()) {
            ctx.out().dim(NONE_KEPT);
            return;
        }
        ctx.out().line(CliLine.of(new CliSpan("#", CliStyle.HEADER), CliSpan.pad(NAME_AT),
                CliSpan.of(CONVERSATION, CliStyle.HEADER), CliSpan.pad(LINES_AT + 2),
                CliSpan.of(LINES, CliStyle.HEADER),
                CliSpan.pad(LINES_AT + LINES_WIDTH + SIZE_WIDTH - 4), CliSpan.of(SIZE, CliStyle.HEADER),
                CliSpan.pad(SAID_AT_COLUMN), CliSpan.of(LAST_SAID, CliStyle.HEADER)));
        int number = 1;
        for (final MessengerLog.Conversation conversation : conversations) {
            ctx.out().line(CliLine.of(CliSpan.plain(String.valueOf(number)), CliSpan.pad(NAME_AT),
                    CliSpan.plain(nameOf(conversation)), CliSpan.pad(LINES_AT),
                    CliSpan.plain(CliText.padLeft(String.valueOf(conversation.lines()), LINES_WIDTH)
                            + CliText.padLeft(size(conversation.bytes()), SIZE_WIDTH)),
                    CliSpan.pad(SAID_AT_COLUMN), CliSpan.plain(saidAt(kept, conversation))));
            number++;
        }
    }

    private static void delete(final CliContext ctx, final IKeptConversations kept, final String which) {
        if (ALL.equals(which)) {
            final List<MessengerLog.Conversation> gone = kept.deleteAll();
            int lines = 0;
            long bytes = 0L;
            for (final MessengerLog.Conversation conversation : gone) {
                lines += conversation.lines();
                bytes += conversation.bytes();
            }
            ctx.out().ok(DELETED_ALL.with(gone.size(), lines, size(bytes)));
            return;
        }
        if (which.isEmpty()) {
            ctx.out().error(CliTexts.USAGE.with(NAME, Text.literal("delete N|delete " + ALL)));
            return;
        }
        final List<MessengerLog.Conversation> conversations = kept.list();
        final int index = numberOf(which);
        if (index < 1 || index > conversations.size()) {
            ctx.out().error(NO_SUCH.with(which));
            return;
        }
        final MessengerLog.Conversation gone = kept.delete(conversations.get(index - 1).room());
        if (gone == null) {
            ctx.out().error(NO_SUCH.with(which));
            return;
        }
        ctx.out().ok(gone.lines() == 1 ? DELETED_ONE.with(nameOf(gone), size(gone.bytes()))
                : DELETED.with(nameOf(gone), gone.lines(), size(gone.bytes())));
    }

    private static void status(final CliContext ctx, final IKeptConversations kept) {
        final IKeptConversations.Status status = kept.status();
        ctx.out().line((status.running() ? STATUS_RUNNING : STATUS_STOPPED).with(status.host()));
        int lines = 0;
        final List<MessengerLog.Conversation> conversations = kept.list();
        for (final MessengerLog.Conversation conversation : conversations) {
            lines += conversation.lines();
        }
        ctx.out().row(STATUS_KEPT.text(), STATUS_COUNT.with(conversations.size(), lines));
        ctx.out().row(STATUS_DISK, size(status.bytes()));
        ctx.out().row(STATUS_MEMORY, MessengerLog.sizeText(status.ramMb() * MEGABYTE));
        ctx.out().row(STATUS_OPEN, String.valueOf(status.people()));
    }

    private static void help(final CliContext ctx) {
        helpLine(ctx, NAME + " list", HELP_LIST);
        helpLine(ctx, NAME + " delete N", HELP_DELETE);
        helpLine(ctx, NAME + " delete " + ALL, HELP_DELETE_ALL);
        helpLine(ctx, NAME + " status", HELP_STATUS);
    }

    private static void helpLine(final CliContext ctx, final String typed, final TextKey what) {
        ctx.out().line(CliLine.of(CliSpan.plain(typed), CliSpan.pad(HELP_AT), CliSpan.of(what, CliStyle.PLAIN)));
    }

    /* What a conversation is called: everybody's, or its two people by the names they wrote. */
    private static Text nameOf(final MessengerLog.Conversation conversation) {
        return conversation.names().isEmpty() ? EVERYBODY.text()
                : Text.literal(String.join(", ", conversation.names()));
    }

    /* When a conversation was last spoken in, by the world's clock. */
    private static Text saidAt(final IKeptConversations kept, final MessengerLog.Conversation conversation) {
        final long clock = kept.clockAt(conversation.lastAt());
        return SAID_AT.with(GameCalendar.day(clock) + 1L, String.format(Locale.ROOT, "%02d:%02d",
                GameCalendar.hourOf(clock), GameCalendar.minuteOf(clock)));
    }

    private static String size(final long bytes) {
        return MessengerLog.sizeText(bytes);
    }

    private static int numberOf(final String typed) {
        try {
            return Integer.parseInt(typed.trim());
        } catch (final NumberFormatException notANumber) {
            return -1;
        }
    }
}
