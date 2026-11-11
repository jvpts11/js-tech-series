/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the network's social programs say round what people wrote: the Messenger and Knot. People's names, what
 * they said, file names and revision numbers such as r3 are data. Kept apart from the windows so the language
 * generator can read it on a server too, where windows do not exist.
 */
@TextHolder
final class SocialTexts {

    // Where a service runs, on either program's status bar.
    static final TextKey ON_HOST = TextKey.of("jsc.social.on_host", "on %s");
    static final TextKey THE_NETWORK = TextKey.of("jsc.social.the_network", "the network");
    static final TextKey NO_SERVICE = TextKey.of("jsc.social.no_service", "no service");

    // The Messenger.
    static final TextKey SEND = TextKey.of("jsc.messenger.send", "Send");
    static final TextKey NUDGE = TextKey.of("jsc.messenger.nudge", "Nudge");
    static final TextKey ON_THE_NETWORK = TextKey.of("jsc.messenger.on_the_network", "On the network");
    static final TextKey NO_SERVICE_HEADING = TextKey.of("jsc.messenger.no_service", "No service");
    static final TextKey EVERYBODY = TextKey.of("jsc.messenger.everybody", "Everybody");
    static final TextKey NOBODY_ELSE = TextKey.of("jsc.messenger.nobody_else", "nobody else");
    static final TextKey SENT_A_NUDGE = TextKey.of("jsc.messenger.sent_a_nudge", "%s sent a nudge");
    static final TextKey NOTHING_SAID = TextKey.of("jsc.messenger.nothing_said", "Nothing said yet");
    static final TextKey NO_MESSENGER =
            TextKey.of("jsc.messenger.no_messenger", "No Messenger Service on this network");
    // The conversations the service keeps, at its own server.
    static final TextKey KEPT_CONVERSATIONS = TextKey.of("jsc.messenger.kept", "Kept conversations");
    static final TextKey KEPT_TITLE = TextKey.of("jsc.messenger.kept.title", "Messenger Service - kept conversations");
    static final TextKey KEPT_INTRO = TextKey.of("jsc.messenger.kept.intro", "What this server keeps for the network.");
    static final TextKey KEPT_CONVERSATION = TextKey.of("jsc.messenger.kept.conversation", "Conversation");
    static final TextKey KEPT_LINES = TextKey.of("jsc.messenger.kept.lines", "Lines");
    static final TextKey KEPT_SIZE = TextKey.of("jsc.messenger.kept.size", "Size");
    static final TextKey KEPT_LAST_SAID = TextKey.of("jsc.messenger.kept.last_said", "Last said");
    static final TextKey KEPT_SAID_AT = TextKey.of("jsc.messenger.kept.said_at", "Day %s, %s");
    static final TextKey KEPT_ON_DISK = TextKey.of("jsc.messenger.kept.on_disk", "%s kept on this server's disk.");
    static final TextKey KEPT_LEAVES = TextKey.of("jsc.messenger.kept.leaves",
            "A deleted conversation leaves every Messenger on the network.");
    static final TextKey KEPT_NONE = TextKey.of("jsc.messenger.kept.none", "This server keeps no conversations.");
    static final TextKey KEPT_DELETE = TextKey.of("jsc.messenger.kept.delete", "Delete");
    static final TextKey KEPT_DELETE_ALL = TextKey.of("jsc.messenger.kept.delete_all", "Delete all");
    static final TextKey KEPT_CLOSE = TextKey.of("jsc.messenger.kept.close", "Close");

    // Knot.
    static final TextKey PUSH = TextKey.of("jsc.knot.push", "Push");
    static final TextKey PULL = TextKey.of("jsc.knot.pull", "Pull");
    static final TextKey REFRESH = TextKey.of("jsc.knot.refresh", "Refresh");
    static final TextKey FILE = TextKey.of("jsc.knot.file", "File");
    static final TextKey NOTHING_PUSHED = TextKey.of("jsc.knot.nothing_pushed", "Nothing pushed yet");
    static final TextKey NO_KNOTHUB = TextKey.of("jsc.knot.no_knothub", "No KnotHub on this network");
    static final TextKey NO_CHANGE = TextKey.of("jsc.knot.no_change", "No change in r%s");
    static final TextKey PICK_A_REVISION = TextKey.of("jsc.knot.pick_a_revision", "Pick a revision");
    static final TextKey ONE_REVISION = TextKey.of("jsc.knot.one_revision", "%s revision");
    static final TextKey REVISIONS = TextKey.of("jsc.knot.revisions", "%s revisions");

    private SocialTexts() {
    }
}
