/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationBoardTest {

    private static final long LASTS = 5_000L;

    private NotificationBoard<String> board;

    @BeforeEach
    void setUp() {
        board = new NotificationBoard<>();
    }

    @Test
    void post_showsANewNotificationAndSoundsIt() {
        final NotificationBoard.Posted<String> posted = post("Research done", 0L);

        assertTrue(posted.shown());
        assertTrue(posted.audible());
        assertEquals(List.of("Research done"), shownItems(1_000L));
    }

    @Test
    void showing_dropsANotificationOnceItsTimeIsUp() {
        post("Research done", 0L);

        assertTrue(shownItems(LASTS).isEmpty());
        assertEquals(1, board.history().size());
    }

    @Test
    void post_groupsAlikeNotificationsIntoOneWithACount() {
        post("On battery", 0L);
        post("Craft done", 100L);
        final NotificationBoard.Posted<String> again = post("On battery", 200L);

        assertEquals(2, again.entry().count());
        assertFalse(again.audible(), "an alike one still on the screen does not sound again");
        assertEquals(List.of("On battery", "Craft done"), board.history().stream()
                .map(NotificationBoard.Entry::item).toList());
    }

    @Test
    void post_soundsAnAlikeNotificationAgainOnceTheFirstLeftTheScreen() {
        post("On battery", 0L);

        final NotificationBoard.Posted<String> again = post("On battery", LASTS + 1L);

        assertEquals(2, again.entry().count());
        assertTrue(again.audible());
    }

    @Test
    void post_startsAFreshGroupAfterAMinute() {
        post("On battery", 0L);

        final NotificationBoard.Posted<String> later = post("On battery", NotificationBoard.GROUP_MILLIS + 1L);

        assertEquals(1, later.entry().count());
        assertEquals(2, board.history().size());
    }

    @Test
    void silence_keepsAKindOrAModOffTheScreenButInTheHistory() {
        board.silence(Set.of("jstests:battery"), Set.of("jsc"));

        final NotificationBoard.Posted<String> byKind = board.post("On battery", "battery", "jstests:battery",
                "jstests", NotificationLevel.WARNING, LASTS, 0L);
        final NotificationBoard.Posted<String> byMod = board.post("Craft done", "craft", "jsc:craft", "jsc",
                NotificationLevel.INFO, LASTS, 0L);

        assertFalse(byKind.shown() || byKind.audible());
        assertFalse(byMod.shown() || byMod.audible());
        assertTrue(shownItems(1L).isEmpty());
        assertEquals(2, board.history().size());
    }

    @Test
    void doNotDisturb_holdsEverythingAndDoesNotLetItOutLater() {
        board.setDoNotDisturb(true);
        post("Overvoltage", 0L);

        board.setDoNotDisturb(false);

        assertTrue(shownItems(1L).isEmpty(), "what came while not to be disturbed stays off the screen");
        assertEquals(1, board.history().size());
    }

    @Test
    void showing_showsAtMostFiveNewestFirst() {
        for (int i = 0; i < 7; i++) {
            post("Note " + i, i);
        }

        assertEquals(List.of("Note 6", "Note 5", "Note 4", "Note 3", "Note 2"), shownItems(10L));
    }

    @Test
    void dismiss_takesItOffTheScreenButKeepsIt() {
        final NotificationBoard.Posted<String> posted = post("Overvoltage", 0L);

        board.dismiss(posted.entry().id());

        assertTrue(shownItems(1L).isEmpty());
        assertEquals(1, board.history().size());
    }

    @Test
    void forget_takesItOutOfTheHistory() {
        final NotificationBoard.Posted<String> posted = post("Overvoltage", 0L);

        board.forget(posted.entry().id());

        assertTrue(board.history().isEmpty());
    }

    @Test
    void setMostKept_dropsTheOldestFirst() {
        post("First", 0L);
        post("Second", 1L);
        post("Third", 2L);

        board.setMostKept(2);

        assertEquals(List.of("Third", "Second"), board.history().stream().map(NotificationBoard.Entry::item)
                .toList());
    }

    @Test
    void left_goesFromOneToNothingOverItsTime() {
        final NotificationBoard.Entry<String> entry = post("Overvoltage", 0L).entry();

        assertEquals(1f, NotificationBoard.left(entry, 0L));
        assertEquals(0.5f, NotificationBoard.left(entry, LASTS / 2));
        assertEquals(0f, NotificationBoard.left(entry, LASTS * 2));
    }

    private NotificationBoard.Posted<String> post(final String title, final long now) {
        return board.post(title, title, "jstests:" + title, "jstests", NotificationLevel.INFO, LASTS, now);
    }

    private List<String> shownItems(final long now) {
        return board.showing(now).stream().map(NotificationBoard.Entry::item).toList();
    }
}
