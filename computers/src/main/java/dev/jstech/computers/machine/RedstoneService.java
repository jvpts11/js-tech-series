/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.core.signal.Frequency;
import dev.jstech.core.signal.Heard;
import dev.jstech.core.signal.IRadio;
import dev.jstech.core.signal.RadioBlockEntity;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.SignalRadio;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The Redstone Interfaces linked to the machine, as what runs on it reaches them: one found by the name it answers to,
 * whatever the case of its letters, and what stands in front of its lens, a J's Core sensor or radio. A program reaches
 * only the interfaces of the machine it runs on, as a name is unique among one computer's interfaces and not across a
 * network.
 */
public final class RedstoneService {

    private final AbstractComputerBlockEntity machine;
    private final ServerLevel level;

    /** A J's Core radio in front of an interface, as a program reads it: what it does, its frequency and its value. */
    public record Radio(String role, String band, int frequency, int value, boolean clash) {
    }

    public RedstoneService(final AbstractComputerBlockEntity machine, final ServerLevel level) {
        this.machine = machine;
        this.level = level;
    }

    /** The interface linked to the machine that answers to {@code name}, or null when none does. */
    @Nullable
    public RedstoneInterfaceBlockEntity named(final String name) {
        return RedstoneInterfaceBlockEntity.linkedTo(this.level, this.machine, name);
    }

    /** Every enabled interface linked to the machine. */
    public List<RedstoneInterfaceBlockEntity> all() {
        return RedstoneInterfaceBlockEntity.allLinkedTo(this.level, this.machine);
    }

    /** What the interface {@code sensor} has in front of its lens reads; none when nothing there says anything. */
    public List<Reading> sensed(final RedstoneInterfaceBlockEntity sensor) {
        return sensor.sensed(this.level);
    }

    /** The J's Core radio in front of the lens of {@code sensor}, or null when it faces none. */
    @Nullable
    public RadioBlockEntity radioBefore(final RedstoneInterfaceBlockEntity sensor) {
        if (!this.level.isLoaded(sensor.front())) {
            return null;
        }
        return this.level.getBlockEntity(sensor.front()) instanceof RadioBlockEntity radio ? radio : null;
    }

    /** {@code radio} as a program reads it: what it sends while it transmits, what it hears while it receives. */
    public Radio read(final RadioBlockEntity radio) {
        final int number = SignalRadio.frequencyAt(this.level, radio.getBlockPos()).map(Frequency::number)
                .orElse(radio.tuning().number());
        final String role = switch (radio.role()) {
            case TRANSMITTER -> "transmitter";
            case RECEIVER -> "receiver";
            case RELAY -> "relay";
        };
        return new Radio(role, radio.tuning().band().serializedName(), number,
                radio.role() == IRadio.Role.TRANSMITTER ? radio.value() : radio.signal(), radio.clashing());
    }

    /** Every frequency heard where the machine stands, as a signal probe there would show them. */
    public List<Heard> heard() {
        return SignalRadio.heardAt(this.level, this.machine.getBlockPos(), null);
    }
}
