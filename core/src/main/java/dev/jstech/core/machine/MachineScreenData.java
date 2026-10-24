/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a machine's own screen ({@code MachineScreens}) shows that only the server knows: worked out on the server,
 * carried to the player while the screen is open and only when it changed, and read there through
 * {@link MachineMenu#screenData(Class)}. A machine gives it from {@link MachineBlockEntity#screenData()}.
 *
 * <pre>{@code
 * @Override
 * public MachineScreenData<MyReadings> screenData() {
 *     return new MachineScreenData<>(MyReadings.STREAM_CODEC, MyReadings.NONE, this::readings);
 * }
 * }</pre>
 *
 * @param codec what writes it
 * @param none  what the screen shows until the server's first word
 * @param read  works it out on the server, asked every tick the screen is open, so it keeps what it works out
 * @param <T>   what it is
 */
public record MachineScreenData<T>(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T none, Supplier<T> read) {

    public MachineScreenData {
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(none, "none");
        Objects.requireNonNull(read, "read");
    }
}
