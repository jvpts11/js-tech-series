/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;

/**
 * The clients the player let in, each with the secret key the server made for it: drawn fresh from a secure source as
 * the client pairs, held in memory only, so every start of the game makes them new and none outlives it. A key is
 * compared in a time that does not depend on how much of it matches, and taken back at once.
 */
public final class PairedClients {

    private final List<PairedClient> clients = new CopyOnWriteArrayList<>();
    private final SecureRandom random = new SecureRandom();

    /** How many bytes a key is drawn from: 256 bits. */
    private static final int KEY_BYTES = 32;

    /**
     * Lets in the client {@code name} of version {@code version} with {@code permission}, on behalf of
     * {@code owner}, the player who let it in on a server, or none on the player's own game; its key is new.
     */
    public PairedClient pair(final String name, final String version, final McpPermission permission,
                             @Nullable final UUID owner, final long now) {
        final byte[] bytes = new byte[KEY_BYTES];
        this.random.nextBytes(bytes);
        final PairedClient client = new PairedClient(HexFormat.of().formatHex(bytes), name, version, permission,
                owner, now);
        this.clients.add(client);
        return client;
    }

    /** The client whose key is {@code key}, or none. */
    public Optional<PairedClient> find(@Nullable final String key) {
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }
        final byte[] given = key.getBytes(StandardCharsets.US_ASCII);
        PairedClient found = null;
        // Every key is compared, all the way, so how long the search takes says nothing of a key.
        for (final PairedClient client : this.clients) {
            if (MessageDigest.isEqual(client.key.getBytes(StandardCharsets.US_ASCII), given)) {
                found = client;
            }
        }
        return Optional.ofNullable(found);
    }

    /** Takes back the key of {@code client}: it is let in no more until the player lets it in again. */
    public boolean revoke(final PairedClient client) {
        return this.clients.remove(client);
    }

    /** Takes back every key, and says how many there were. */
    public int revokeAll() {
        final int count = this.clients.size();
        this.clients.clear();
        return count;
    }

    /** The clients let in, the first first. */
    public List<PairedClient> all() {
        return new ArrayList<>(this.clients);
    }

    /** A client the player let in. */
    public static final class PairedClient {

        private final String key;
        private final String name;
        private final String version;
        private final McpPermission permission;
        private final @Nullable UUID owner;
        private final long since;
        private final AtomicInteger calls = new AtomicInteger();

        private PairedClient(final String key, final String name, final String version,
                             final McpPermission permission, @Nullable final UUID owner, final long since) {
            this.key = key;
            this.name = name;
            this.version = version;
            this.permission = permission;
            this.owner = owner;
            this.since = since;
        }

        /** Its key, which only the answer to its pairing carries. */
        public String key() {
            return this.key;
        }

        public String name() {
            return this.name;
        }

        public String version() {
            return this.version;
        }

        public McpPermission permission() {
            return this.permission;
        }

        /** The player who let it in, on a server; none on the player's own game. */
        public Optional<UUID> owner() {
            return Optional.ofNullable(this.owner);
        }

        /** When it was let in, in milliseconds since the epoch. */
        public long since() {
            return this.since;
        }

        /** How many calls it made. */
        public int calls() {
            return this.calls.get();
        }

        /** Counts one more call. */
        public void called() {
            this.calls.incrementAndGet();
        }

        /** Its name and version, never its key. */
        @Override
        public String toString() {
            return this.name + " " + this.version;
        }
    }
}
