/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import dev.jstech.core.JsCore;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.capability.CapabilityKind;
import dev.jstech.core.capability.CapabilityKinds;
import dev.jstech.core.content.ModContent;
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
 * Mechanical power between blocks: the {@link #MECHANICAL} capability every part that turns offers, kept for a block
 * entity of the Core's kit by {@link #keep}, and the level's mechanics turned after every other thing in the tick.
 *
 * <pre>{@code
 * public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
 *     super(TYPE.get(), pos, state);
 *     MechanicalPorts.keep(this);   // joined again as it is placed, loaded or broken
 * }
 * }</pre>
 */
public final class MechanicalPorts {

    /** A block's part in a network of mechanical power, by face. */
    public static final CapabilityKind<IMechanicalPart> MECHANICAL = CapabilityKinds.declare(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "mechanical"), IMechanicalPart.class);

    private MechanicalPorts() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, MechanicalPorts::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                LevelMechanics.of(level).tick(level);
            }
        });
    }

    /**
     * Keeps {@code blockEntity}, a part or the holder of one, in the level's mechanics: its network is worked out again
     * each time it is in a loaded world, when it is broken, and when a block beside it changes. Called from its
     * constructor.
     */
    public static void keep(final SyncedBlockEntity blockEntity) {
        blockEntity.fields().whenLoaded(level -> LevelMechanics.changed(level, blockEntity.getBlockPos()));
        blockEntity.fields().whenBroken((level, pos) -> LevelMechanics.changed(level, pos));
        blockEntity.fields().whenNeighbourChanges((level, face) -> LevelMechanics.changed(level,
                blockEntity.getBlockPos()));
    }

    /** The part at {@code pos} as its {@code face} meets it; none for a block that does not turn. */
    public static @Nullable IMechanicalPart at(final Level level, final BlockPos pos,
                                               final @Nullable Direction face) {
        return MECHANICAL.at(level, pos, face);
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
        event.registerBlockEntity(MECHANICAL.block(), any, (entity, side) -> entity instanceof IMechanicalPart part
                ? part : entity instanceof IMechanicalHolder holder ? holder.mechanicalPart() : null);
    }
}
