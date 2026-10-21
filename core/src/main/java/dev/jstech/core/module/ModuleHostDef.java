/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * An item that takes modules, as a data pack or a mod declares it: its sockets, each of a kind, and whether its modules
 * work while it is worn, as a suit's, or while it is held in the main hand, as a tool's. Its modules run on the energy
 * the item holds.
 *
 * @param sockets its sockets, in order, as many of each kind as each says
 * @param held    whether its modules work while it is held in the main hand rather than worn
 */
public record ModuleHostDef(List<Socket> sockets, boolean held) {

    public static final Codec<ModuleHostDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Socket.CODEC.listOf().fieldOf("sockets").forGetter(ModuleHostDef::sockets),
            Codec.BOOL.optionalFieldOf("held", false).forGetter(ModuleHostDef::held))
            .apply(instance, ModuleHostDef::new));

    public ModuleHostDef {
        sockets = List.copyOf(sockets);
    }

    /** A worn item with {@code sockets}. */
    public static ModuleHostDef worn(final List<Socket> sockets) {
        return new ModuleHostDef(sockets, false);
    }

    /** A tool with {@code sockets}, whose modules work while it is held in the main hand. */
    public static ModuleHostDef tool(final List<Socket> sockets) {
        return new ModuleHostDef(sockets, true);
    }

    /** Its sockets one by one, each by its kind: as many entries as sockets. */
    public List<String> kinds() {
        final List<String> kinds = new ArrayList<>();
        for (final Socket socket : sockets) {
            for (int i = 0; i < socket.count(); i++) {
                kinds.add(socket.kind());
            }
        }
        return kinds;
    }

    /**
     * Sockets of one kind.
     *
     * @param kind  the kind of module they take
     * @param count how many
     */
    public record Socket(String kind, int count) {

        public static final Codec<Socket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("kind").forGetter(Socket::kind),
                Codec.intRange(1, 27).optionalFieldOf("count", 1).forGetter(Socket::count))
                .apply(instance, Socket::new));
    }
}
