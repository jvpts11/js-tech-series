/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.Platform;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ManTopicsTest {

    @Test
    void find_answersTheIntroTopicOnFreeBsd() {
        final Optional<ManTopics.Topic> found = ManTopics.find("intro", Platform.FREEBSD);
        assertTrue(found.isPresent());
        assertEquals("intro", found.get().name());
    }

    @Test
    void find_hasNothingByThatNameOnAPlatformThatNeverHadIt() {
        assertTrue(ManTopics.find("intro", Platform.UNIX).isEmpty());
        assertTrue(ManTopics.find("nowhere", Platform.FREEBSD).isEmpty());
    }

    @Test
    void onPlatform_listsIntroOnFreeBsdAloneAmongTheSystemsThatHaveNoOtherTopic() {
        assertTrue(ManTopics.onPlatform(Platform.FREEBSD).stream().anyMatch(t -> t.name().equals("intro")));
        assertFalse(ManTopics.onPlatform(Platform.UNIX).stream().anyMatch(t -> t.name().equals("intro")));
    }

    @Test
    void answersTo_matchesTheNameAndTheSummary() {
        final ManTopics.Topic intro = ManTopics.find("intro", Platform.FREEBSD).orElseThrow();
        assertTrue(intro.answersTo("intro"));
        assertTrue(intro.answersTo("commands"));
        assertFalse(intro.answersTo("something nowhere in the page"));
    }

    @Test
    void description_isNotEmpty() {
        final ManTopics.Topic intro = ManTopics.find("intro", Platform.FREEBSD).orElseThrow();
        assertFalse(intro.description().isEmpty());
        assertFalse(intro.seeAlso().isEmpty());
    }
}
