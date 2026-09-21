/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What a messenger service is actually made of: the conversations it is keeping and what they weigh.
 *
 * <p>The weight is the point of this program rather than a detail of it. A service that costs a fixed
 * amount is a number somebody wrote down; this one grows on the disk as it keeps what people said, and in
 * the machine's memory as more of them are connected to it at once. A busy chat on a small Server really
 * runs that Server out of room, and the player can watch it happen.
 *
 * <p>This class is pure and carries no Minecraft dependency, so the arithmetic that matters is testable
 * without a world anywhere near it.
 */
public final class MessengerLog {

    /** How many messages the service keeps before the oldest start falling off the end. */
    public static final int MAX_MESSAGES = 500;

    /** The longest one message may be. */
    public static final int MAX_TEXT = 200;

    /** What the service costs in memory before anybody has connected to it. */
    public static final int BASE_RAM_MB = 8;

    /** What each connected person adds to that, which is what makes a busy chat expensive. */
    public static final int RAM_PER_PERSON_MB = 2;

    /** What each message in a room's name costs beyond its own text, for the sender and the timestamp. */
    private static final int OVERHEAD_BYTES = 24;

    /** The room everybody is in unless they are talking to one person. */
    public static final String LOBBY = "lobby";

    /** What joins the two names of a room where two people are talking to each other. */
    private static final String PAIR = "|";

    /**
     * The room two people share, whichever of them opened it.
     *
     * <p>Named from both, in a fixed order, because the alternative is each of them talking into a room
     * named after the other and neither ever hearing a word.
     */
    public static String privateRoom(final String a, final String b) {
        final String first = a == null ? "" : a.toLowerCase(Locale.ROOT);
        final String second = b == null ? "" : b.toLowerCase(Locale.ROOT);
        return first.compareTo(second) <= 0 ? first + PAIR + second : second + PAIR + first;
    }

    /** Whether a room is a conversation between two people rather than the room everybody is in. */
    public static boolean isPrivate(final String room) {
        return room != null && room.indexOf(PAIR.charAt(0)) >= 0;
    }

    /** The other person in a private room, from the point of view of {@code me}. */
    public static String otherIn(final String room, final String me) {
        if (!isPrivate(room)) {
            return "";
        }
        final int bar = room.indexOf(PAIR.charAt(0));
        final String first = room.substring(0, bar);
        final String second = room.substring(bar + 1);
        return first.equalsIgnoreCase(me) ? second : first;
    }

    /** One thing somebody said. */
    public record Message(String room, String from, String text, long at, boolean nudge) {

        public Message {
            if (room == null || room.isBlank()) {
                throw new IllegalArgumentException("a message belongs to a room");
            }
            if (from == null || from.isBlank()) {
                throw new IllegalArgumentException("a message has somebody who said it");
            }
            if (text == null) {
                throw new IllegalArgumentException("a message has words, even if none of them");
            }
        }

        /** What this message costs on the disk. */
        public int bytes() {
            return text.getBytes(StandardCharsets.UTF_8).length
                    + from.getBytes(StandardCharsets.UTF_8).length
                    + room.getBytes(StandardCharsets.UTF_8).length + OVERHEAD_BYTES;
        }
    }

    /** How long somebody may say nothing at all before the service stops holding a place for them. */
    public static final long IDLE_TICKS = 200L;

    private final List<Message> messages = new ArrayList<>();
    /** Who has it open: which room each of them is looking at, and when each was last heard from. */
    private final Map<String, Presence> connected = new LinkedHashMap<>();
    private long bytes;

    /** Somebody with the messenger open: the room on their screen, and when they last said so. */
    private record Presence(String room, long at) {
    }

    /**
     * Keeps what somebody said, dropping the oldest once the service is full.
     *
     * <p>Answers the message as it was kept, with its text cut to what one message may hold, or null when
     * there was nothing to keep.
     */
    public Message say(final String room, final String from, final String text,
                       final long at, final boolean nudge) {
        if (from == null || from.isBlank()) {
            return null;
        }
        final String said = text == null ? "" : text.strip();
        if (said.isEmpty() && !nudge) {
            return null;
        }
        final Message message = new Message(
                room == null || room.isBlank() ? LOBBY : room.toLowerCase(Locale.ROOT), from,
                said.length() > MAX_TEXT ? said.substring(0, MAX_TEXT) : said, at, nudge);
        messages.add(message);
        bytes += message.bytes();
        while (messages.size() > MAX_MESSAGES) {
            bytes -= messages.remove(0).bytes();
        }
        return message;
    }

    /** Everything said in a room, oldest first, at most {@code limit} of them. */
    public List<Message> room(final String name, final int limit) {
        final String wanted = name == null || name.isBlank() ? LOBBY : name.toLowerCase(Locale.ROOT);
        final List<Message> out = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0 && out.size() < limit; i--) {
            if (messages.get(i).room().equals(wanted)) {
                out.add(0, messages.get(i));
            }
        }
        return out;
    }

    /** Every room anything has been said in, in the order they were first spoken in. */
    public List<String> rooms() {
        final Set<String> seen = new LinkedHashSet<>();
        seen.add(LOBBY);
        for (final Message message : messages) {
            seen.add(message.room());
        }
        return List.copyOf(seen);
    }

    /** Everything kept, oldest first, for writing the whole lot down. */
    public List<Message> all() {
        return List.copyOf(messages);
    }

    /** Takes a whole history back, for a service being read off a disk. */
    public void restore(final List<Message> kept) {
        messages.clear();
        bytes = 0L;
        if (kept == null) {
            return;
        }
        for (final Message message : kept) {
            if (message != null) {
                messages.add(message);
                bytes += message.bytes();
            }
        }
        while (messages.size() > MAX_MESSAGES) {
            bytes -= messages.remove(0).bytes();
        }
    }

    /** How many messages are being kept. */
    public int size() {
        return messages.size();
    }

    /** What the history weighs on the disk, in bytes; this is what grows as people talk. */
    public long historyBytes() {
        return bytes;
    }

    /**
     * Says somebody has the messenger open on {@code room}, at {@code at}; answers whether that was news.
     *
     * <p>Three things are kept and each earns its place. The name is who is there. The moment is so a
     * window that goes quiet can be let go of, see {@link #forgetIdle}. The room is so that what one
     * person says can be told to the people looking at that conversation without dragging everybody else's
     * window into it, which is what {@link #roomOf} is for.
     */
    public boolean connect(final String who, final String room, final long at) {
        if (who == null || who.isBlank()) {
            return false;
        }
        return connected.put(who,
                new Presence(room == null || room.isBlank() ? LOBBY : room.toLowerCase(Locale.ROOT), at))
                == null;
    }

    /** The room somebody has open, or the lobby when they have none open at all. */
    public String roomOf(final String who) {
        final Presence presence = connected.get(who);
        return presence == null ? LOBBY : presence.room();
    }

    /** Says somebody closed it; answers whether that was news. */
    public boolean disconnect(final String who) {
        return who != null && connected.remove(who) != null;
    }

    /**
     * Lets go of everybody who has said nothing for a while.
     *
     * <p>A window says goodbye when it is closed, but a player who walked away from the screen, whose
     * client crashed, or who simply pressed escape never got to. Without this the service holds a place in
     * memory for each of them for ever, and the one number this whole program is about climbs with people
     * who are not there. Answers how many were let go.
     */
    public int forgetIdle(final long now) {
        int gone = 0;
        final var walk = connected.entrySet().iterator();
        while (walk.hasNext()) {
            final var entry = walk.next();
            if (now - entry.getValue().at() > IDLE_TICKS) {
                walk.remove();
                gone++;
            }
        }
        return gone;
    }

    /** Who has it open right now. */
    public List<String> connected() {
        return List.copyOf(connected.keySet());
    }

    /**
     * What the service is costing in memory right now.
     *
     * <p>A floor plus a share for everybody connected: the service has to hold a conversation open for
     * each of them, so a room with ten people in it really is more expensive than a room with one.
     */
    public int ramMb() {
        return BASE_RAM_MB + connected.size() * RAM_PER_PERSON_MB;
    }

    /** Throws away every message, which is the only way a full service is emptied. */
    public void clear() {
        messages.clear();
        bytes = 0L;
    }
}
