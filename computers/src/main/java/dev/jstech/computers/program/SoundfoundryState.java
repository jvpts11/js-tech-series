/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.audio.media.MediaId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.random.RandomGenerator;

/**
 * What Soundfoundry keeps on a machine: its playlist, the song it is on, whether it shuffles and repeats, how loud it
 * plays and to which side, and the songs it is fetching over the network.
 *
 * <p>It belongs to the machine rather than to the window: the music plays on with the screen closed, so it is the
 * machine that has to know what comes next when a song ends.
 *
 * <p>Shuffling goes through the whole list in an order of its own before any song comes round again, the way the
 * players of the time did; without repeat, the music stops once the last song of the order, or of the list, has
 * played.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SoundfoundryState {

    private final List<String> songs = new ArrayList<>();
    /** The order shuffling plays the list in, made when it is first needed and forgotten when the list changes. */
    private final List<Integer> order = new ArrayList<>();
    private int current = -1;
    private boolean shuffle;
    private boolean repeat;
    private int volume = DEFAULT_VOLUME;
    private int balance;
    /** Counts every change to the list, so a screen asks for the list only when it has changed. */
    private int revision;
    /** The songs being fetched over the network, and those fetched or given up on, oldest first. */
    private final List<SongDownload> downloads = new ArrayList<>();
    /** The Soundfoundry Server it streams from, as the network knows it; empty for the first one found. */
    private String server = "";

    /** The most songs a playlist holds. */
    public static final int MAX_SONGS = 500;
    /** The most downloads it lists, those coming in and those finished together. */
    public static final int MAX_DOWNLOADS = 50;
    /** Its loudest, as a share out of this. */
    public static final int MAX_VOLUME = 100;
    /** How far its balance goes to either side: minus this is all left, this is all right. */
    public static final int MAX_BALANCE = 100;
    /** How loud it starts. */
    public static final int DEFAULT_VOLUME = 80;

    /** The playlist's songs, by the paths of their files. */
    public List<String> songs() {
        return List.copyOf(songs);
    }

    /** How many songs the playlist holds. */
    public int size() {
        return songs.size();
    }

    /** The path of the song at that place in the list. */
    public String song(final int index) {
        return songs.get(index);
    }

    /** The place of the song it is on, or -1 for none. */
    public int current() {
        return current;
    }

    /** Counts the changes to the list. */
    public int revision() {
        return revision;
    }

    public boolean shuffle() {
        return shuffle;
    }

    public boolean repeat() {
        return repeat;
    }

    /** How loud it plays, from 0 to {@link #MAX_VOLUME}. */
    public int volume() {
        return volume;
    }

    /** To which side it plays, from minus {@link #MAX_BALANCE} (all left) to {@link #MAX_BALANCE} (all right). */
    public int balance() {
        return balance;
    }

    /**
     * How loud a place playing that side of a recording plays, from its balance: turning it towards one side turns
     * the other down, and a place playing both sides stays as it is.
     */
    public float gainFor(final StereoSide side) {
        return switch (side) {
            case LEFT -> balance > 0 ? 1.0F - balance / (float) MAX_BALANCE : 1.0F;
            case RIGHT -> balance < 0 ? 1.0F + balance / (float) MAX_BALANCE : 1.0F;
            case BOTH -> 1.0F;
        };
    }

    /** The Soundfoundry Server the player picked, or {@code ""} for whichever the network finds first. */
    public String server() {
        return server;
    }

    public void setServer(final String value) {
        server = value == null ? "" : value;
    }

    public void setShuffle(final boolean value) {
        shuffle = value;
        order.clear();
    }

    public void setRepeat(final boolean value) {
        repeat = value;
    }

    public void setVolume(final int value) {
        volume = Math.clamp(value, 0, MAX_VOLUME);
    }

    public void setBalance(final int value) {
        balance = Math.clamp(value, -MAX_BALANCE, MAX_BALANCE);
    }

    /**
     * Puts songs at the end of the list, as many as it has room for.
     *
     * @return how many went in
     */
    public int add(final Collection<String> paths) {
        int added = 0;
        for (final String path : paths) {
            if (songs.size() >= MAX_SONGS) {
                break;
            }
            if (path != null && !path.isBlank()) {
                songs.add(path);
                added++;
            }
        }
        if (added > 0) {
            changed();
        }
        return added;
    }

    /**
     * Puts those songs in place of the list, on the one at {@code index}: an album, a playlist or a page's songs,
     * played from where the player picked, take the whole list the way a streaming player's queue is its context.
     */
    public void replace(final Collection<String> paths, final int index) {
        songs.clear();
        current = -1;
        changed();
        add(paths);
        select(index);
    }

    /** Takes the songs at those places out of the list; the song it was on is forgotten if it is one of them. */
    public void remove(final Set<Integer> indexes) {
        keep(index -> !indexes.contains(index));
    }

    /** Keeps only the songs at those places. */
    public void keepOnly(final Set<Integer> indexes) {
        keep(indexes::contains);
    }

    /** Empties the list. */
    public void clear() {
        songs.clear();
        current = -1;
        changed();
    }

    /** Puts the list in that order, keeping the song it was on. */
    public void sort(final Comparator<String> by) {
        final String on = current >= 0 ? songs.get(current) : null;
        final int sameBefore = on == null ? 0 : sameBefore(current);
        songs.sort(by);
        current = on == null ? -1 : nth(on, sameBefore);
        changed();
    }

    /** Turns the list round, keeping the song it was on. */
    public void reverse() {
        Collections.reverse(songs);
        if (current >= 0) {
            current = songs.size() - 1 - current;
        }
        changed();
    }

    /** Makes the song at that place the one it is on; a place outside the list forgets it. */
    public void select(final int index) {
        current = index >= 0 && index < songs.size() ? index : -1;
    }

    /**
     * The place of the song after the one it is on, or -1 when there is none to go to.
     *
     * @param wrap whether to go round from the end: a player asking for the next song always does, the end of a song
     *             only with repeat on
     */
    public int next(final boolean wrap, final RandomGenerator random) {
        if (songs.isEmpty()) {
            return -1;
        }
        if (!shuffle) {
            if (current + 1 < songs.size()) {
                return current + 1;
            }
            return wrap ? 0 : -1;
        }
        final int at = orderOf(random).indexOf(current);
        if (at + 1 < order.size()) {
            return order.get(at + 1);
        }
        if (!wrap) {
            return -1;
        }
        // A new order for the next time round, which does not start with the song that just ended.
        reshuffle(random);
        if (order.size() > 1 && order.getFirst() == current) {
            Collections.swap(order, 0, 1);
        }
        return order.getFirst();
    }

    /** The downloads, oldest first: those coming in and those finished. */
    public List<SongDownload> downloads() {
        return Collections.unmodifiableList(downloads);
    }

    /** Whether that recording is already coming in. */
    public boolean downloading(final MediaId media) {
        for (final SongDownload download : downloads) {
            if (download.active() && download.media().equals(media)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lists a download, making room by forgetting the oldest finished one when the list is full.
     *
     * @return false when the list is full of downloads still coming in
     */
    public boolean addDownload(final SongDownload download) {
        if (downloads.size() >= MAX_DOWNLOADS) {
            final int finished = firstFinished();
            if (finished < 0) {
                return false;
            }
            downloads.remove(finished);
        }
        downloads.add(download);
        return true;
    }

    /** Takes the downloads at those places off the list, stopping any still coming in. */
    public void removeDownloads(final Set<Integer> indexes) {
        final List<SongDownload> left = new ArrayList<>(downloads.size());
        for (int i = 0; i < downloads.size(); i++) {
            if (!indexes.contains(i)) {
                left.add(downloads.get(i));
            }
        }
        downloads.clear();
        downloads.addAll(left);
    }

    /** Forgets every download that is finished, done or given up on. */
    public void clearFinishedDownloads() {
        downloads.removeIf(download -> !download.active());
    }

    /** Forgets every download, for a machine put back as it was saved. */
    void forgetDownloads() {
        downloads.clear();
    }

    /** The place of the song before the one it is on, or the start of the list. */
    public int previous(final RandomGenerator random) {
        if (songs.isEmpty()) {
            return -1;
        }
        if (!shuffle) {
            return current > 0 ? current - 1 : repeat ? songs.size() - 1 : 0;
        }
        final int at = orderOf(random).indexOf(current);
        return at > 0 ? order.get(at - 1) : Math.max(0, current);
    }

    private List<Integer> orderOf(final RandomGenerator random) {
        if (order.size() != songs.size()) {
            reshuffle(random);
            // The song it is on starts the order, so the rest of the list follows it before anything repeats.
            if (current >= 0) {
                order.remove(Integer.valueOf(current));
                order.addFirst(current);
            }
        }
        return order;
    }

    private void reshuffle(final RandomGenerator random) {
        order.clear();
        for (int i = 0; i < songs.size(); i++) {
            order.add(i);
        }
        for (int i = order.size() - 1; i > 0; i--) {
            Collections.swap(order, i, random.nextInt(i + 1));
        }
    }

    private void keep(final IntPredicate kept) {
        final List<String> left = new ArrayList<>(songs.size());
        int newCurrent = -1;
        for (int i = 0; i < songs.size(); i++) {
            if (kept.test(i)) {
                if (i == current) {
                    newCurrent = left.size();
                }
                left.add(songs.get(i));
            }
        }
        songs.clear();
        songs.addAll(left);
        current = newCurrent;
        changed();
    }

    /* How many songs of the same path come before this one, so a sort finds this very one again. */
    private int sameBefore(final int index) {
        int count = 0;
        for (int i = 0; i < index; i++) {
            if (songs.get(i).equals(songs.get(index))) {
                count++;
            }
        }
        return count;
    }

    private int nth(final String path, final int skip) {
        int seen = 0;
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).equals(path) && seen++ == skip) {
                return i;
            }
        }
        return -1;
    }

    private void changed() {
        order.clear();
        revision++;
    }

    private int firstFinished() {
        for (int i = 0; i < downloads.size(); i++) {
            if (!downloads.get(i).active()) {
                return i;
            }
        }
        return -1;
    }
}
