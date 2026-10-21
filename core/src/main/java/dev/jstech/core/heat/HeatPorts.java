/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.JsCore;
import dev.jstech.core.blockentity.IFaceGate;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.capability.CapabilityKind;
import dev.jstech.core.capability.CapabilityKinds;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.log.CoreLogs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Heat between blocks: the {@link #HEAT} capability every block that holds heat offers on its faces, kept for a block
 * entity of the Core's kit by {@link #keep}, and the level's heat ticked after every other thing in the tick.
 *
 * <pre>{@code
 * public CopperBlockEntity(BlockPos pos, BlockState state) {
 *     super(TYPE.get(), pos, state);
 *     HeatPorts.keep(this, heat);   // heat: a HeatBody, and the block entity an IHeatHolder
 * }
 * }</pre>
 */
public final class HeatPorts {

    /** A block's heat, by face, for its neighbours and heat lines. */
    public static final CapabilityKind<IHeatPort> HEAT = CapabilityKinds.declare(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "heat"), IHeatPort.class);
    /** The heat of a level flowing each tick, timed. */
    public static final Section SECTION = Section.builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "heat"),
            CoreLogs.HEAT).declare();

    private HeatPorts() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, HeatPorts::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                SECTION.run(() -> LevelHeat.of(level).tick(level));
            }
        });
    }

    /**
     * Keeps {@code body} in {@code blockEntity}: saved with it, sent to the players who see it as it changes, and woken
     * for the level's heat when it changes and each time the block is in a loaded world. Called from the block
     * entity's constructor, while its fields are declared.
     */
    public static <E extends SyncedBlockEntity & IHeatHolder> void keep(final E blockEntity, final HeatBody body) {
        final PartField field = blockEntity.fields().part("Heat", body).save().toClient();
        body.listening(field::changed, () -> LevelHeat.wake(blockEntity.getLevel(), blockEntity.getBlockPos()));
        blockEntity.fields().whenLoaded(level -> LevelHeat.of(level).wake(blockEntity.getBlockPos().asLong()));
        blockEntity.fields().whenNeighbourChanges((level, face) -> LevelHeat.of(level)
                .wake(blockEntity.getBlockPos().asLong()));
    }

    /** The heat of the block at {@code pos} as its {@code face} gives it; none for a block that holds none. */
    public static @Nullable IHeatPort at(final Level level, final BlockPos pos, final @Nullable Direction face) {
        return HEAT.at(level, pos, face);
    }

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        for (final ModContent content : ModContent.all()) {
            for (final BlockEntityType<?> type : content.declaredBlockEntityTypes()) {
                register(event, type);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void register(final RegisterCapabilitiesEvent event, final BlockEntityType<?> type) {
        final BlockEntityType<BlockEntity> any = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(HEAT.block(), any, (entity, side) ->
                entity instanceof IHeatHolder holder ? port(holder, side) : null);
    }

    /**
     * The heat {@code holder} offers through {@code side}: its body, unless it holds none or lets no heat through
     * that face.
     */
    public static @Nullable IHeatPort port(final IHeatHolder holder, final @Nullable Direction side) {
        final HeatBody body = holder.heatBody();
        return body != null && holder instanceof IFaceGate gate ? gate.throughFace(HEAT.block(), side, body) : body;
    }
}
