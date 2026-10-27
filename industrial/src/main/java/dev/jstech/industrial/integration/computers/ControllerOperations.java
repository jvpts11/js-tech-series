/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.jstechseries.network.NetworkCategory;
import dev.jstech.core.jstechseries.operation.IOperationArgs;
import dev.jstech.core.jstechseries.operation.OperationCategory;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.jstechseries.operation.OperationType;
import dev.jstech.core.jstechseries.operation.RegisterOperationTypesEvent;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.EnumSet;
import java.util.Optional;

/**
 * What the network asks of an Industrial Controller Computer, through {@code RUN}: setting a value of a machine it
 * reads, or what the machine does with redstone; opening or closing a valve; and having a capsule station of a
 * pneumatic post send what waits in it to an address. The machine is named as the {@code machines} table names it
 * ({@code 'Compressor #1'}) or by where it stands ({@code '12, 64, -3'}); {@code ON} names the controller, and without
 * it the request goes to the controller that reads the machine named.
 */
@TextHolder
public final class ControllerOperations {

    public static final TextKey MACHINE_SET_NAME = TextKey.of("jsindustrial.operation.machine_set", "Set a machine");
    public static final TextKey VALVE_SET_NAME = TextKey.of("jsindustrial.operation.valve_set", "Set a valve");
    public static final TextKey STATION_SEND_NAME = TextKey.of("jsindustrial.operation.station_send",
            "Send a capsule");
    /** {@code RUN jsindustrial:machine_set WITH machine = 'Compressor #1', parameter = 'on', value = 0}. */
    public static final OperationType<MachineSetArgs> MACHINE_SET = new OperationType<>("jsindustrial:machine_set",
            MACHINE_SET_NAME, MachineSetArgs.class, OperationCategory.INDUSTRIAL, IndustrialTier.T1,
            EnumSet.of(NetworkCategory.C), MachineSetArgs.TEXT, request ->
            request.machine().orElse(null) instanceof IndustrialControllerBlockEntity controller
                    ? controller.machineSet(request) : OperationStatus.FAILED);
    /** {@code RUN jsindustrial:valve_set WITH valve = '12, 64, -3', open = false}. */
    public static final OperationType<ValveSetArgs> VALVE_SET = new OperationType<>("jsindustrial:valve_set",
            VALVE_SET_NAME, ValveSetArgs.class, OperationCategory.INDUSTRIAL, IndustrialTier.T1,
            EnumSet.of(NetworkCategory.C), ValveSetArgs.TEXT, request ->
            request.machine().orElse(null) instanceof IndustrialControllerBlockEntity controller
                    ? controller.valveSet(request) : OperationStatus.FAILED);
    /** {@code RUN jsindustrial:station_send WITH station = '14, 64, -3', to = 3}. */
    public static final OperationType<StationSendArgs> STATION_SEND = new OperationType<>(
            "jsindustrial:station_send", STATION_SEND_NAME, StationSendArgs.class, OperationCategory.INDUSTRIAL,
            IndustrialTier.T1, EnumSet.of(NetworkCategory.C), StationSendArgs.TEXT, request ->
            request.machine().orElse(null) instanceof IndustrialControllerBlockEntity controller
                    ? controller.stationSend(request) : OperationStatus.FAILED);

    /** Why a controller will not take a request: it is switched off. */
    public static final TextKey SWITCHED_OFF = TextKey.of("jsindustrial.operation.switched_off",
            "%s is switched off");
    /** Why a controller will not take a request: it reads no machine of that name. */
    public static final TextKey NOT_READ = TextKey.of("jsindustrial.operation.not_read",
            "the controller reads no machine named %s");
    public static final TextKey NOTHING_TO_SET = TextKey.of("jsindustrial.operation.nothing_to_set",
            "say a parameter and its value, or redstone");
    public static final TextKey NOT_SETTABLE = TextKey.of("jsindustrial.operation.not_settable",
            "%s has no value %s that can be set");
    public static final TextKey NO_REDSTONE = TextKey.of("jsindustrial.operation.no_redstone",
            "%s does nothing with redstone, or not %s: ignore, high, low or pulse");
    public static final TextKey NOT_BY_HAND = TextKey.of("jsindustrial.operation.not_by_hand",
            "%s is not opened and closed by hand");
    public static final TextKey NOT_A_STATION = TextKey.of("jsindustrial.operation.not_a_station",
            "%s is no capsule station");
    public static final TextKey NOTHING_WAITS = TextKey.of("jsindustrial.operation.nothing_waits",
            "nothing waits in %s to be sent");
    public static final TextKey NO_STATION_THERE = TextKey.of("jsindustrial.operation.no_station_there",
            "no station of address %s is on the tubes of %s");

    private ControllerOperations() {
    }

    /** Registers the kinds; the mod's listener on the event. */
    static void register(final RegisterOperationTypesEvent event) {
        event.operations().register(MACHINE_SET);
        event.operations().register(VALVE_SET);
        event.operations().register(STATION_SEND);
    }

    /**
     * What a request to set a machine carries: the machine, and either one of its values that can be set with what to
     * set it to, or what it does with redstone ({@code ignore}, {@code high}, {@code low}, {@code pulse}).
     */
    public record MachineSetArgs(String machine, Optional<String> parameter, Optional<Double> value,
                                 Optional<String> redstone) implements IOperationArgs {

        public static final MapCodec<MachineSetArgs> TEXT = RecordCodecBuilder.mapCodec(args -> args.group(
                Codec.STRING.fieldOf("machine").forGetter(MachineSetArgs::machine),
                Codec.STRING.optionalFieldOf("parameter").forGetter(MachineSetArgs::parameter),
                Codec.DOUBLE.optionalFieldOf("value").forGetter(MachineSetArgs::value),
                Codec.STRING.optionalFieldOf("redstone").forGetter(MachineSetArgs::redstone)
        ).apply(args, MachineSetArgs::new));
    }

    /** What a request to open or close a valve carries: the valve, and whether it opens. */
    public record ValveSetArgs(String valve, boolean open) implements IOperationArgs {

        public static final MapCodec<ValveSetArgs> TEXT = RecordCodecBuilder.mapCodec(args -> args.group(
                Codec.STRING.fieldOf("valve").forGetter(ValveSetArgs::valve),
                Codec.BOOL.fieldOf("open").forGetter(ValveSetArgs::open)
        ).apply(args, ValveSetArgs::new));
    }

    /** What a request to send a capsule carries: the station, and the address it sends what waits in it to. */
    public record StationSendArgs(String station, int to) implements IOperationArgs {

        public static final MapCodec<StationSendArgs> TEXT = RecordCodecBuilder.mapCodec(args -> args.group(
                Codec.STRING.fieldOf("station").forGetter(StationSendArgs::station),
                Codec.INT.fieldOf("to").forGetter(StationSendArgs::to)
        ).apply(args, StationSendArgs::new));
    }
}
