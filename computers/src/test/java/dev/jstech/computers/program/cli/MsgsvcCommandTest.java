/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.MessengerLog;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MsgsvcCommandTest {

    private CliShell shell;
    private Fake computer;

    /* Four conversations, said at noon on the fourth day and thereabouts, the clock running with the game. */
    private static final long DAY = 24_000L;

    @BeforeEach
    void setUp() {
        this.shell = new CliShell(BuiltinCommands.all(), 80);
        this.computer = new Fake();
        final MessengerLog log = this.computer.log;
        log.say("lobby", "Dev", "morning everybody", 3 * DAY + 6_000L, false);
        log.say(MessengerLog.privateRoom("Dev", "Alex"), "Alex", "lunch?", 3 * DAY + 3_000L, false);
        log.say(MessengerLog.privateRoom("Dev", "Alex"), "Dev", "yes", 3 * DAY + 3_200L, false);
        log.say(MessengerLog.privateRoom("Dev", "Sam"), "Sam", "hi", 1 * DAY + 12_000L, false);
    }

    @Test
    void list_numbersEveryConversationWithItsLinesSizeAndLastSaid() {
        final String said = this.said("msgsvc list");
        final String[] lines = said.split("\n");
        assertEquals(4, lines.length, said);
        assertTrue(lines[0].startsWith("#  CONVERSATION"), said);
        assertTrue(lines[1].startsWith("1  Everybody"), said);
        assertTrue(lines[1].endsWith("Day 4, 12:00"), said);
        assertTrue(lines[2].startsWith("2  Alex, Dev"), said);
        assertTrue(lines[2].contains("      2"), said);
        assertTrue(lines[3].startsWith("3  Dev, Sam"), said);
        assertTrue(lines[3].endsWith("Day 2, 18:00"), said);
    }

    @Test
    void delete_takesTheNumberedConversationAndSaysWhatWasFreed() {
        final String said = this.said("msgsvc delete 2");
        assertTrue(said.startsWith("Deleted \"Alex, Dev\": 2 lines,"), said);
        assertTrue(said.contains("It leaves every Messenger."), said);
        assertEquals(2, this.computer.log.conversations().size());
        assertTrue(this.said("msgsvc delete 7").contains("No conversation 7"));
    }

    @Test
    void deleteAll_emptiesTheServiceAndCountsWhatWent() {
        final String said = this.said("msgsvc delete --all");
        assertTrue(said.startsWith("Deleted 3 conversations: 4 lines,"), said);
        assertTrue(this.computer.log.conversations().isEmpty());
        assertEquals("This server keeps no conversations.\n", this.said("msgsvc list"));
    }

    @Test
    void status_saysWhereTheServiceRunsAndWhatItWeighs() {
        final String said = this.said("msgsvc status");
        assertTrue(said.startsWith("Messenger Service on srv01: running"), said);
        assertTrue(said.contains("3, 4 lines"), said);
    }

    @Test
    void msgsvc_isNoCommandOffTheServiceOrOffAServer() {
        this.computer.installed.clear();
        assertTrue(this.said("msgsvc list").contains("command not found"));
        this.computer.installed.add(new ICliComputer.ProgramInfo("msgsvc", MsgsvcCommand.SERVICE));
        this.computer.server = false;
        assertTrue(this.said("msgsvc list").contains("command not found"));
    }

    @Test
    void msgsvc_alone_listsItsVerbs() {
        final String said = this.said("msgsvc");
        assertTrue(said.contains("msgsvc delete --all"), said);
        assertTrue(said.contains("the service, its disk and its memory"), said);
    }

    private String said(final String line) {
        final StringBuilder text = new StringBuilder();
        for (final CliLine each : this.shell.run(line, this.computer).lines()) {
            text.append(each.text()).append('\n');
        }
        return text.toString();
    }

    /* A rack server running the Messenger Service, its conversations in a log of its own. */
    private static final class Fake implements ICliComputer {

        private final MessengerLog log = new MessengerLog();
        private final List<ProgramInfo> installed = new ArrayList<>(
                List.of(new ProgramInfo("msgsvc", MsgsvcCommand.SERVICE)));
        private boolean server = true;

        @Override
        public String name() {
            return "srv01";
        }

        @Override
        public Platform platform() {
            return Platform.LINUX;
        }

        @Override
        public boolean hostIs(final HostScope scope) {
            return scope == HostScope.ANY || this.server && scope == HostScope.SERVER;
        }

        @Override
        public List<ProgramInfo> programs() {
            return List.copyOf(this.installed);
        }

        @Override
        public IKeptConversations keptConversations() {
            return new IKeptConversations() {
                @Override
                public List<MessengerLog.Conversation> list() {
                    return Fake.this.log.conversations();
                }

                @Override
                public MessengerLog.Conversation delete(final String room) {
                    return Fake.this.log.delete(room);
                }

                @Override
                public List<MessengerLog.Conversation> deleteAll() {
                    return Fake.this.log.deleteAll();
                }

                @Override
                public Status status() {
                    return new Status("srv01", true, Fake.this.log.historyBytes(), Fake.this.log.ramMb(), 0);
                }

                @Override
                public long clockAt(final long at) {
                    return at;
                }
            };
        }
    }
}
