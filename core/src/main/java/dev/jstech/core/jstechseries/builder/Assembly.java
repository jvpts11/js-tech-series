/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

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
import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;

/**
 * The kind of Operation that sets a machine building a plan by itself, the link between the network and a machine
 * that lays a planned build's blocks from what comes in through its ports: {@code RUN jscore:assemble ON 'Hangar
 * Terminal-3fa2c1'} builds the plan the machine has, and {@code RUN jscore:assemble WITH plan = 'Lunar 1'} one of its
 * plans by name. The machine is an {@link IAssembler}; while it waits for blocks, the network brings them.
 */
@TextHolder
@ApiStatus.Experimental
public final class Assembly {

    public static final TextKey NAME = TextKey.of("jscore.operation.assemble", "Assemble");
    /** Building a plan by itself, at a machine of category C that carries it out. */
    public static final OperationType<Args> ASSEMBLE = new OperationType<>("jscore:assemble", NAME, Args.class,
            OperationCategory.CONSTRUCTION, IndustrialTier.T1, EnumSet.of(NetworkCategory.C), Args.TEXT,
            request -> request.machine().orElse(null) instanceof IAssembler assembler
                    ? assembler.assemble(request) : OperationStatus.FAILED);

    private Assembly() {
    }

    /** Registers the kind when the kinds are gathered, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterOperationTypesEvent.class, event -> event.operations().register(ASSEMBLE));
    }

    /**
     * What a request to build carries: the name of the machine's plan to build, or none for the plan it was given.
     *
     * @param plan the plan's name
     */
    public record Args(Optional<String> plan) implements IOperationArgs {

        public static final MapCodec<Args> TEXT = RecordCodecBuilder.mapCodec(args -> args.group(
                Codec.STRING.optionalFieldOf("plan").forGetter(Args::plan)
        ).apply(args, Args::new));
    }
}
