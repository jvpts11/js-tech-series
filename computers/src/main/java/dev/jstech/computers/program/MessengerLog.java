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
import org.jetbrains.annotations.Nullable;

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

    private static final long KILOBYTE = 1024L;

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

    /**
     * Whether {@code who} may be in a room: any room but a private one, and a private one only for the two people
     * it is between. A room's name comes from the player's screen, and naming a pair is not joining it.
     */
    public static boolean mayEnter(final String room, final String who) {
        if (!isPrivate(room)) {
            return true;
        }
        final int bar = room.indexOf(PAIR.charAt(0));
        return who != null && (room.substring(0, bar).equalsIgnoreCase(who)
                || room.substring(bar + 1).equalsIgnoreCase(who));
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

    /**
     * Somebody with the messenger open: the room on their screen, when they last said so, and whether their screen is
     * the service's own server, where the kept conversations are theirs to delete.
     */
    private record Presence(String room, long at, boolean atServer) {
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

    /**
     * The rooms {@code who} is shown: every room anything has been said in but the private ones between other
     * people, whose names alone would tell who talks to whom.
     */
    public List<String> roomsFor(final String who) {
        final List<String> shown = new ArrayList<>();
        for (final String room : rooms()) {
            if (mayEnter(room, who)) {
                shown.add(room);
            }
        }
        return List.copyOf(shown);
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
        return connect(who, room, at, false);
    }

    /** {@link #connect(String, String, long)}, saying too whether their messenger is open at the service's server. */
    public boolean connect(final String who, final String room, final long at, final boolean atServer) {
        if (who == null || who.isBlank()) {
            return false;
        }
        return connected.put(who, new Presence(room == null || room.isBlank() ? LOBBY : room.toLowerCase(Locale.ROOT),
                at, atServer)) == null;
    }

    /** Whether somebody has their messenger open at the service's own server. */
    public boolean atServer(final String who) {
        final Presence presence = connected.get(who);
        return presence != null && presence.atServer();
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

    /**
     * Every conversation the service keeps, in the order each was first spoken in: the room, the names of who is in it
     * as they wrote them, how many lines were said, what they weigh and when the last one was said.
     */
    public List<Conversation> conversations() {
        final Map<String, List<Message>> byRoom = new LinkedHashMap<>();
        for (final Message message : messages) {
            byRoom.computeIfAbsent(message.room(), room -> new ArrayList<>()).add(message);
        }
        final List<Conversation> out = new ArrayList<>();
        for (final Map.Entry<String, List<Message>> room : byRoom.entrySet()) {
            final List<Message> said = room.getValue();
            long weight = 0L;
            for (final Message message : said) {
                weight += message.bytes();
            }
            out.add(new Conversation(room.getKey(), namesIn(room.getKey()), said.size(), weight,
                    said.getLast().at()));
        }
        return List.copyOf(out);
    }

    /**
     * Throws away everything said in {@code room}; answers what was thrown away, or null when the service keeps no
     * such conversation. The room goes with it, from every list it was on.
     */
    @Nullable
    public Conversation delete(final String room) {
        final String wanted = room == null ? "" : room.toLowerCase(Locale.ROOT);
        for (final Conversation conversation : conversations()) {
            if (conversation.room().equals(wanted)) {
                messages.removeIf(message -> message.room().equals(wanted));
                bytes -= conversation.bytes();
                return conversation;
            }
        }
        return null;
    }

    /** Throws away every conversation; answers what was thrown away, oldest first. */
    public List<Conversation> deleteAll() {
        final List<Conversation> gone = conversations();
        clear();
        return gone;
    }

    /** What something weighs on a disk, in the unit that keeps the number short: 640 B, 12 KB, 1.4 MB. */
    public static String sizeText(final long bytes) {
        if (bytes < KILOBYTE) {
            return bytes + " B";
        }
        if (bytes < KILOBYTE * KILOBYTE) {
            return Math.round(bytes / (double) KILOBYTE) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (double) (KILOBYTE * KILOBYTE));
    }

    /** Throws away every message, which is the only way a full service is emptied. */
    public void clear() {
        messages.clear();
        bytes = 0L;
    }

    /*
     * The names of a private room as its two wrote them wherever they spoke, since the room's own name is folded to
     * lower case; the lobby is everybody's and names nobody.
     */
    private List<String> namesIn(final String room) {
        if (!isPrivate(room)) {
            return List.of();
        }
        final int bar = room.indexOf(PAIR.charAt(0));
        final List<String> names = new ArrayList<>();
        for (final String folded : List.of(room.substring(0, bar), room.substring(bar + 1))) {
            String written = folded;
            for (final Message message : messages) {
                if (message.from().equalsIgnoreCase(folded)) {
                    written = message.from();
                    break;
                }
            }
            names.add(written);
        }
        return List.copyOf(names);
    }

    /**
     * One conversation the service keeps.
     *
     * @param room   its room, {@link #LOBBY} or two names
     * @param names  who is in it as they wrote their names; none for the lobby, which is everybody's
     * @param lines  how many things were said in it
     * @param bytes  what it weighs on the disk
     * @param lastAt the game time the last of them was said at
     */
    public record Conversation(String room, List<String> names, int lines, long bytes, long lastAt) {

        public Conversation {
            names = List.copyOf(names);
        }
    }
}
