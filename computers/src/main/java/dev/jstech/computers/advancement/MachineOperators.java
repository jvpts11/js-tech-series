/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import dev.jstech.computers.JsComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Who is working a machine: the last player to place it or to use it (open its monitor, its terminal, its assembly).
 *
 * <p>Most of what a machine does happens far from any player: a self-test finishing, a scheduler completing an
 * Operation, a job firing on its own, a cluster coming online. Those events are credited to the machine's operator,
 * which is the rule the advancements follow ("whoever is working the machine", "whoever set it up"). The operator
 * is kept on the block entity as an attachment, so it is saved with the machine and survives a restart, and a job
 * that fires a week later still knows who set it up.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class MachineOperators {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JsComputers.MODID);

    private static final UUID NOBODY = new UUID(0L, 0L);

    public static final Supplier<AttachmentType<UUID>> OPERATOR = ATTACHMENT_TYPES.register("operator",
            () -> AttachmentType.builder(() -> NOBODY).serialize(UUIDUtil.CODEC, id -> !NOBODY.equals(id)).build());

    private MachineOperators() {
    }

    public static void register(final IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    /** Records {@code player} as the one working this machine. Machines acting as players never count. */
    public static void note(@Nullable final BlockEntity machine, @Nullable final Player player) {
        if (machine == null || !(player instanceof ServerPlayer) || player instanceof FakePlayer) {
            return;
        }
        note(machine, player.getUUID());
    }

    /** The same, for a player known by their id: the one acting in a scope, see {@link Acting}. */
    public static void note(@Nullable final BlockEntity machine, final UUID player) {
        if (machine != null && !NOBODY.equals(player) && !player.equals(machine.getExistingDataOrNull(OPERATOR))) {
            machine.setData(OPERATOR, player);
            machine.setChanged();
        }
    }

    /** The same, for the machine standing at {@code pos}. */
    public static void note(final Level level, final BlockPos pos, @Nullable final Player player) {
        note(level.getBlockEntity(pos), player);
    }

    /** Who works the machine, online or not, when anybody does. */
    public static Optional<UUID> idOf(@Nullable final BlockEntity machine) {
        final UUID id = machine == null ? null : machine.getExistingDataOrNull(OPERATOR);
        return id == null || NOBODY.equals(id) ? Optional.empty() : Optional.of(id);
    }

    /** The machine's operator, when there is one and they are online to be told. */
    public static Optional<ServerPlayer> of(@Nullable final BlockEntity machine) {
        if (machine == null || !(machine.getLevel() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        final UUID id = machine.getExistingDataOrNull(OPERATOR);
        return id == null ? Optional.empty() : online(level.getServer(), id);
    }

    /** The operator of the machine standing at {@code pos}. */
    public static Optional<ServerPlayer> at(final Level level, final BlockPos pos) {
        return of(level.getBlockEntity(pos));
    }

    /** A block with a machine in it is operated first by whoever put it there. */
    @SubscribeEvent
    public static void onPlaced(final BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && event.getLevel() instanceof Level level
                && !level.isClientSide()) {
            final BlockEntity machine = level.getBlockEntity(event.getPos());
            if (ours(machine)) {
                note(machine, player);
            }
        }
    }

    /** Using a machine makes the one using it its operator; a block that fronts for another notes that one itself. */
    @SubscribeEvent
    public static void onUsed(final PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            final BlockEntity machine = event.getLevel().getBlockEntity(event.getPos());
            if (ours(machine)) {
                note(machine, event.getEntity());
            }
        }
    }

    private static boolean ours(@Nullable final BlockEntity machine) {
        final ResourceLocation type = machine == null ? null
                : BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(machine.getType());
        return type != null && JsComputers.MODID.equals(type.getNamespace());
    }

    private static Optional<ServerPlayer> online(@Nullable final MinecraftServer server, final UUID id) {
        return server == null ? Optional.empty() : Optional.ofNullable(server.getPlayerList().getPlayer(id));
    }
}
