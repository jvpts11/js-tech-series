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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The modules in a worn item, socket by socket, each with whether it is on, as the item carries them.
 *
 * @param sockets its sockets, in its host's order; an empty stack in an empty socket
 */
public record InstalledModules(List<Socketed> sockets) {

    public static final InstalledModules NONE = new InstalledModules(List.of());
    public static final Codec<InstalledModules> CODEC = Socketed.CODEC.listOf()
            .xmap(InstalledModules::new, InstalledModules::sockets);
    public static final StreamCodec<RegistryFriendlyByteBuf, InstalledModules> STREAM_CODEC =
            Socketed.STREAM_CODEC.apply(ByteBufCodecs.list()).map(InstalledModules::new, InstalledModules::sockets);

    public InstalledModules {
        sockets = List.copyOf(sockets);
    }

    /** As many sockets as {@code count}, those it has kept, the rest empty. */
    public InstalledModules sized(final int count) {
        final List<Socketed> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(i < sockets.size() ? sockets.get(i) : Socketed.EMPTY);
        }
        return new InstalledModules(out);
    }

    /** The same, with {@code socket} holding {@code module}, on or off. */
    public InstalledModules with(final int socket, final ItemStack module, final boolean on) {
        final List<Socketed> out = new ArrayList<>(sockets);
        out.set(socket, new Socketed(module.copy(), on));
        return new InstalledModules(out);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof InstalledModules that) || that.sockets.size() != sockets.size()) {
            return false;
        }
        for (int i = 0; i < sockets.size(); i++) {
            if (!sockets.get(i).same(that.sockets.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (final Socketed each : sockets) {
            hash = 31 * hash + ItemStack.hashItemAndComponents(each.module()) + (each.on() ? 1 : 0);
        }
        return hash;
    }

    /**
     * One socket.
     *
     * @param module the module in it, or an empty stack
     * @param on     whether it is on
     */
    public record Socketed(ItemStack module, boolean on) {

        public static final Socketed EMPTY = new Socketed(ItemStack.EMPTY, false);
        public static final Codec<Socketed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("module", ItemStack.EMPTY).forGetter(Socketed::module),
                Codec.BOOL.optionalFieldOf("on", true).forGetter(Socketed::on))
                .apply(instance, Socketed::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Socketed> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Socketed::module, ByteBufCodecs.BOOL, Socketed::on, Socketed::new);

        /** Whether it holds the same module, as on or off. */
        public boolean same(final Socketed other) {
            return on == other.on && ItemStack.matches(module, other.module);
        }
    }
}
