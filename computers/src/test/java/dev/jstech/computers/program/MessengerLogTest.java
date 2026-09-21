/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class MessengerLogTest {

    private MessengerLog log;

    @BeforeEach
    void setUp() {
        log = new MessengerLog();
    }

    @Test
    void aNewService_isEmptyAndCostsOnlyItsFloor() {
        assertEquals(0, log.size());
        assertEquals(0L, log.historyBytes());
        assertEquals(MessengerLog.BASE_RAM_MB, log.ramMb());
        assertTrue(log.connected().isEmpty());
    }

    @Test
    void say_keepsWhatWasSaid() {
        final MessengerLog.Message message = log.say("lobby", "ada", "the smelter stalled", 100L, false);
        assertNotNull(message);
        assertEquals("ada", message.from());
        assertEquals("the smelter stalled", message.text());
        assertEquals(1, log.size());
    }

    @Test
    void say_refusesAMessageFromNobody() {
        assertNull(log.say("lobby", "", "hello", 1L, false));
        assertNull(log.say("lobby", null, "hello", 1L, false));
        assertEquals(0, log.size());
    }

    @Test
    void say_refusesAnEmptyMessageThatIsNotANudge() {
        assertNull(log.say("lobby", "ada", "   ", 1L, false));
        assertNotNull(log.say("lobby", "ada", "", 1L, true), "a nudge has no words and is still a message");
    }

    @Test
    void say_cutsAMessageToWhatOneMayHold() {
        final String long1 = "x".repeat(MessengerLog.MAX_TEXT + 50);
        final MessengerLog.Message message = log.say("lobby", "ada", long1, 1L, false);
        assertNotNull(message);
        assertEquals(MessengerLog.MAX_TEXT, message.text().length());
    }

    @Test
    void theHistory_growsWithWhatIsSaid() {
        final long before = log.historyBytes();
        log.say("lobby", "ada", "a line of talk", 1L, false);
        final long after = log.historyBytes();
        assertTrue(after > before, "keeping a message should cost something");
        log.say("lobby", "grace", "another line of talk", 2L, false);
        assertTrue(log.historyBytes() > after, "the history should keep growing");
    }

    @Test
    void theHistory_stopsGrowingOnceTheServiceIsFull() {
        for (int i = 0; i < MessengerLog.MAX_MESSAGES; i++) {
            log.say("lobby", "ada", "message " + i, i, false);
        }
        final long full = log.historyBytes();
        assertEquals(MessengerLog.MAX_MESSAGES, log.size());
        for (int i = 0; i < 50; i++) {
            log.say("lobby", "ada", "message " + i, i, false);
        }
        assertEquals(MessengerLog.MAX_MESSAGES, log.size(), "the oldest should fall off the end");
        assertTrue(Math.abs(log.historyBytes() - full) < full / 4,
                "a full service should stay about the same weight");
    }

    @Test
    void theMemoryCost_growsWithHowManyAreConnected() {
        assertEquals(MessengerLog.BASE_RAM_MB, log.ramMb());
        log.connect("ada", MessengerLog.LOBBY, 0L);
        assertEquals(MessengerLog.BASE_RAM_MB + MessengerLog.RAM_PER_PERSON_MB, log.ramMb());
        log.connect("grace", MessengerLog.LOBBY, 0L);
        log.connect("linus", MessengerLog.LOBBY, 0L);
        assertEquals(MessengerLog.BASE_RAM_MB + 3 * MessengerLog.RAM_PER_PERSON_MB, log.ramMb());
        log.disconnect("grace");
        assertEquals(MessengerLog.BASE_RAM_MB + 2 * MessengerLog.RAM_PER_PERSON_MB, log.ramMb());
    }

    @Test
    void connect_andDisconnect_sayWhetherAnythingChanged() {
        assertTrue(log.connect("ada", MessengerLog.LOBBY, 0L));
        assertFalse(log.connect("ada", MessengerLog.LOBBY, 1L), "connecting twice is not news");
        assertTrue(log.disconnect("ada"));
        assertFalse(log.disconnect("ada"));
        assertFalse(log.connect("", MessengerLog.LOBBY, 0L));
        assertFalse(log.connect(null, MessengerLog.LOBBY, 0L));
    }

    @Test
    void roomOf_isWhateverThePersonLastLookedAt() {
        final String pair = MessengerLog.privateRoom("ada", "grace");
        log.connect("ada", pair, 0L);
        log.connect("grace", MessengerLog.LOBBY, 0L);
        assertEquals(pair, log.roomOf("ada"), "a window says which conversation it is showing");
        assertEquals(MessengerLog.LOBBY, log.roomOf("grace"));
        assertEquals(MessengerLog.LOBBY, log.roomOf("linus"), "somebody who is not there is nowhere");
        log.connect("ada", MessengerLog.LOBBY, 1L);
        assertEquals(MessengerLog.LOBBY, log.roomOf("ada"), "and it moves when they move");
        log.connect("ada", "", 2L);
        assertEquals(MessengerLog.LOBBY, log.roomOf("ada"), "a window that names no room is in the lobby");
    }

    @Test
    void forgetIdle_letsGoOfSomebodyWhoHasGoneQuiet() {
        /*
         * A window says goodbye when it is closed, but a player who walked away or whose client crashed
         * never got to. Without this the service holds memory for them for ever.
         */
        log.connect("ada", MessengerLog.LOBBY, 100L);
        log.connect("grace", MessengerLog.LOBBY, 100L);
        assertEquals(0, log.forgetIdle(100L + MessengerLog.IDLE_TICKS),
                "nobody is let go of before their time is up");
        assertEquals(2, log.connected().size());
        assertEquals(2, log.forgetIdle(100L + MessengerLog.IDLE_TICKS + 1));
        assertEquals(0, log.connected().size());
        assertEquals(MessengerLog.BASE_RAM_MB, log.ramMb(),
                "and the memory they were costing goes with them");
    }

    @Test
    void forgetIdle_keepsSomebodyWhoHasJustBeenHeardFrom() {
        log.connect("ada", MessengerLog.LOBBY, 100L);
        log.connect("grace", MessengerLog.LOBBY, 100L);
        log.connect("ada", MessengerLog.LOBBY, 500L); // ada said something again
        log.forgetIdle(500L);
        assertEquals(List.of("ada"), log.connected(), "grace went quiet, ada did not");
    }

    @Test
    void room_givesBackOnlyThatRoomsMessagesOldestFirst() {
        log.say("lobby", "ada", "one", 1L, false);
        log.say("smelter", "grace", "two", 2L, false);
        log.say("lobby", "linus", "three", 3L, false);
        final List<MessengerLog.Message> lobby = log.room("lobby", 10);
        assertEquals(2, lobby.size());
        assertEquals("one", lobby.get(0).text());
        assertEquals("three", lobby.get(1).text());
        assertEquals(1, log.room("smelter", 10).size());
    }

    @Test
    void room_givesTheNewestWhenThereAreMoreThanAsked() {
        for (int i = 0; i < 10; i++) {
            log.say("lobby", "ada", "message " + i, i, false);
        }
        final List<MessengerLog.Message> last = log.room("lobby", 3);
        assertEquals(3, last.size());
        assertEquals("message 7", last.get(0).text());
        assertEquals("message 9", last.get(2).text());
    }

    @Test
    void roomNames_areCaseInsensitive() {
        log.say("Smelter", "ada", "one", 1L, false);
        assertEquals(1, log.room("smelter", 10).size());
        assertEquals(1, log.room("SMELTER", 10).size());
    }

    @Test
    void aMessageWithNoRoom_landsInTheLobby() {
        final MessengerLog.Message message = log.say("", "ada", "hello", 1L, false);
        assertNotNull(message);
        assertEquals(MessengerLog.LOBBY, message.room());
    }

    @Test
    void rooms_listTheLobbyAndEveryOtherOneSpokenIn() {
        log.say("smelter", "ada", "one", 1L, false);
        log.say("east", "grace", "two", 2L, false);
        final List<String> rooms = log.rooms();
        assertTrue(rooms.contains(MessengerLog.LOBBY));
        assertTrue(rooms.contains("smelter"));
        assertTrue(rooms.contains("east"));
    }

    @Test
    void restore_bringsAHistoryBackWithItsWeight() {
        log.say("lobby", "ada", "one", 1L, false);
        log.say("lobby", "grace", "two", 2L, false);
        final long weight = log.historyBytes();
        final List<MessengerLog.Message> kept = log.all();

        final MessengerLog other = new MessengerLog();
        other.restore(kept);
        assertEquals(2, other.size());
        assertEquals(weight, other.historyBytes());
    }

    @Test
    void restore_ofNothingLeavesAnEmptyService() {
        log.say("lobby", "ada", "one", 1L, false);
        log.restore(null);
        assertEquals(0, log.size());
        assertEquals(0L, log.historyBytes());
    }

    @Test
    void restore_cutsAHistoryLongerThanTheServiceHolds() {
        final MessengerLog big = new MessengerLog();
        for (int i = 0; i < MessengerLog.MAX_MESSAGES + 100; i++) {
            big.say("lobby", "ada", "message " + i, i, false);
        }
        log.restore(big.all());
        assertEquals(MessengerLog.MAX_MESSAGES, log.size());
    }

    @Test
    void clear_emptiesTheServiceAndItsWeight() {
        log.say("lobby", "ada", "one", 1L, false);
        log.clear();
        assertEquals(0, log.size());
        assertEquals(0L, log.historyBytes());
    }

    @Test
    void aMessage_refusesNonsense() {
        assertThrows(IllegalArgumentException.class,
                () -> new MessengerLog.Message("", "ada", "x", 1L, false));
        assertThrows(IllegalArgumentException.class,
                () -> new MessengerLog.Message("lobby", "", "x", 1L, false));
        assertThrows(IllegalArgumentException.class,
                () -> new MessengerLog.Message("lobby", "ada", null, 1L, false));
    }

    @Test
    void privateRoom_isTheSameRoomWhicheverSideOpensIt() {
        /*
         * The whole point: two people must land in one room. Named from one side only, each would be
         * talking into a room named after the other and neither would ever hear a word.
         */
        assertEquals(MessengerLog.privateRoom("ada", "grace"), MessengerLog.privateRoom("grace", "ada"));
        assertEquals(MessengerLog.privateRoom("Ada", "GRACE"), MessengerLog.privateRoom("grace", "ada"));
    }

    @Test
    void privateRoom_isTellableFromTheLobby() {
        assertTrue(MessengerLog.isPrivate(MessengerLog.privateRoom("ada", "grace")));
        assertFalse(MessengerLog.isPrivate(MessengerLog.LOBBY));
        assertFalse(MessengerLog.isPrivate("smelter"));
        assertFalse(MessengerLog.isPrivate(null));
    }

    @Test
    void otherIn_namesWhoeverIsNotYou() {
        final String room = MessengerLog.privateRoom("ada", "grace");
        assertEquals("grace", MessengerLog.otherIn(room, "ada"));
        assertEquals("ada", MessengerLog.otherIn(room, "grace"));
        assertEquals("", MessengerLog.otherIn(MessengerLog.LOBBY, "ada"),
                "the room everybody is in is nobody's in particular");
    }

    @Test
    void aPrivateRoom_keepsItsOwnMessages() {
        final String room = MessengerLog.privateRoom("ada", "grace");
        log.say(room, "ada", "just between us", 1L, false);
        log.say(MessengerLog.LOBBY, "linus", "hello everybody", 2L, false);
        assertEquals(1, log.room(room, 10).size());
        assertEquals("just between us", log.room(room, 10).get(0).text());
        assertEquals(1, log.room(MessengerLog.LOBBY, 10).size());
    }

    @Test
    void aMessage_knowsWhatItCosts() {
        final MessengerLog.Message message =
                new MessengerLog.Message("lobby", "ada", "hello", 1L, false);
        assertTrue(message.bytes() > "hello".length(), "a message costs more than its own words");
    }
}
