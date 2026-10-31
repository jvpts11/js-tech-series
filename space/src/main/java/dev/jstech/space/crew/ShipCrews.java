/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipDocks;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Who may do what aboard a starship, as its crew says ({@link ShipCrew}): its helm is a pilot's, and so is changing
 * its blocks, putting them in or breaking them; its fittings and the machines aboard are its crew's; its airlock's
 * door, its bunks and its docking collar let in whoever may come aboard. A player not let do something is told why
 * and the thing is not done. A ship nobody owns lets anyone do anything.
 */
public final class ShipCrews {

    private static final int CHUNK = 4;

    private ShipCrews() {
    }

    /** Hands the game's bus the checks aboard, once, from the mod's constructor: before anything else hears a click. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PlayerInteractEvent.RightClickBlock.class,
                ShipCrews::used);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BlockEvent.BreakEvent.class, ShipCrews::broken);
    }

    /** The crew of {@code ship}, as its flight console keeps it; none for a ship nobody owns. */
    public static Optional<ShipCrew> of(final ServerLevel level, final MovingStructure ship) {
        if (!(ship.motion() instanceof ShipFlight flight)) {
            return Optional.empty();
        }
        return flight.consoleIn(ship).filter(at -> level.getChunkSource().getChunkNow(at.getX() >> CHUNK,
                        at.getZ() >> CHUNK) != null).map(level::getBlockEntity)
                .filter(AirReserveEntity.class::isInstance).map(AirReserveEntity.class::cast)
                .flatMap(AirReserveEntity::crew);
    }

    /** {@code player}'s role aboard {@code ship}: a ship nobody owns lets anyone fly it. */
    public static CrewRole roleOf(final ServerPlayer player, final MovingStructure ship) {
        return ship.level() instanceof ServerLevel level ? of(level, ship).map(crew -> crew.roleOf(player))
                .orElse(CrewRole.PILOT) : CrewRole.PILOT;
    }

    /**
     * The role using a block of the kind {@code block} aboard a ship asks for: its helm a pilot, its airlock's door,
     * its bunks and its docking collar whoever may come aboard, its fittings and any machine aboard ({@code machine},
     * a block with something of its own that is no part of a ship's kit) its crew. Anything else asks for nothing,
     * unless a block is being put in ({@code placing}), which is a pilot's to do.
     */
    public static CrewRole needed(final Block block, final boolean machine, final boolean placing) {
        if (block == ShipKitBlocks.PILOT_SEAT.get()) {
            return CrewRole.PILOT;
        }
        if (block == ShipKitBlocks.AIRLOCK_DOOR.get() || block == ShipKitBlocks.BUNK_FRAME.get()
                || block == ShipKitBlocks.DOCKING_COLLAR.get()) {
            return CrewRole.PASSENGER;
        }
        if (block == ShipKitBlocks.FLIGHT_CONSOLE.get() || block == ShipKitBlocks.SUIT_LOCKER.get()
                || block == ShipKitBlocks.OXYGEN_PANEL.get() || block == ShipKitBlocks.AIRLOCK_CYCLER.get()
                || machine) {
            return CrewRole.CREW;
        }
        return placing ? CrewRole.PILOT : CrewRole.NONE;
    }

    /** What a player who may not do something that asks for {@code needed} aboard a ship of {@code crew} is told. */
    public static Text refusal(final ShipCrew crew, final CrewRole needed) {
        return switch (needed) {
            case PILOT -> CrewTexts.PILOTS_ONLY.text();
            case CREW -> CrewTexts.CREW_ONLY.text();
            case PASSENGER, NONE -> CrewTexts.NOT_ABOARD.with(crew.ownerName());
        };
    }

    /*
     * A block aboard a ship used: let be when its role is held; its airlock's door passed through; else refused, the
     * player told why. Its docking collar asks for a place aboard the other ship too.
     */
    private static void used(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        final BlockPos pos = event.getPos();
        if (!YardLayout.isYardBlock(pos.getX()) || !(MovingStructures.at(level, pos) instanceof MovingStructure ship)) {
            return;
        }
        final Optional<ShipCrew> crew = of(level, ship);
        if (crew.isEmpty()) {
            return;
        }
        final Block block = FormedPartBlock.originalAt(level, pos, level.getBlockState(pos)).getBlock();
        final boolean machine = level.getBlockEntity(pos) != null && !kit(block);
        final CrewRole needed = needed(block, machine, event.getItemStack().getItem() instanceof BlockItem);
        final CrewRole role = crew.get().roleOf(player);
        if (!role.holds(needed)) {
            refuse(event, player, refusal(crew.get(), needed));
            return;
        }
        if (block == ShipKitBlocks.DOCKING_COLLAR.get()) {
            final Optional<MovingStructure> other = ShipDocks.partner(ship);
            final Optional<ShipCrew> theirs = other.flatMap(them -> of(level, them));
            if (theirs.isPresent() && !theirs.get().roleOf(player).holds(CrewRole.PASSENGER)) {
                refuse(event, player, refusal(theirs.get(), CrewRole.PASSENGER));
            }
            return;
        }
        if (block == ShipKitBlocks.AIRLOCK_DOOR.get() && ShipHatches.through(player, ship, pos)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /* A block of a ship broken: a pilot's to do. */
    private static void broken(final BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !YardLayout.isYardBlock(event.getPos().getX())
                || !(MovingStructures.at(level, event.getPos()) instanceof MovingStructure ship)) {
            return;
        }
        final Optional<ShipCrew> crew = of(level, ship);
        if (crew.isPresent() && !crew.get().roleOf(player).holds(CrewRole.PILOT)) {
            event.setCanceled(true);
            player.displayClientMessage(GameText.component(refusal(crew.get(), CrewRole.PILOT)), true);
        }
    }

    private static void refuse(final PlayerInteractEvent.RightClickBlock event, final ServerPlayer player,
                               final Text why) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(GameText.component(why), true);
    }

    /* Whether {@code block} is of a starship's kit. */
    private static boolean kit(final Block block) {
        return ShipKitBlocks.all().stream().anyMatch(entry -> entry.get() == block);
    }
}
