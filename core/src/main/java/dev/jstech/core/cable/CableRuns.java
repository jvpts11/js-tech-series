/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.payload.IPayloadGate;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Laying a whole run of cable at once: a player holding a cable drags from the block they lay it at to another, and the
 * run is laid along the way {@link RunPath} takes, each block joining the one before as any cable does. The run stops
 * at the first block it cannot be laid in (something in the way, a protected place, or no cable left in hand) and says
 * so. A face of the run is turned off afterwards as any end is, with a wrench.
 */
@TextHolder
public final class CableRuns {

    public static final TextKey PREVIEW = TextKey.of("jscore.cable.run.preview", "Laying a run of %s");
    public static final TextKey LAID = TextKey.of("jscore.cable.run.laid", "Laid %s of %s");
    public static final TextKey STOPPED = TextKey.of("jscore.cable.run.stopped", "Laid %s of %s: stopped at %s, %s");
    public static final TextKey IN_THE_WAY = TextKey.of("jscore.cable.run.in_the_way", "something is in the way");
    public static final TextKey PROTECTED = TextKey.of("jscore.cable.run.protected", "the place is protected");
    public static final TextKey NONE_LEFT = TextKey.of("jscore.cable.run.none_left", "no cable is left in hand");

    /** How far from the player, in blocks, the run may start: a little past their reach, where they just laid it. */
    public static final double START_REACH = 2.0;
    private static final String NETWORK_VERSION = "1";

    private CableRuns() {
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        // Only a player holding a cable, the run starting within their reach, is heard.
        final IPayloadGate<CableRunPayload> holding = (player, payload) -> player.getItemInHand(hand(payload))
                .getItem() instanceof CableItem && player.canInteractWithBlock(payload.from(), START_REACH);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> ClientPayloads.accept(
                event.registrar(NETWORK_VERSION), CableRunPayload.TYPE, CableRunPayload.STREAM_CODEC, holding,
                (payload, player, level) -> lay(player, level, payload.from(), payload.to(), hand(payload))));
    }

    /**
     * Lays the run of the cable in {@code player}'s {@code hand} from {@code from} to {@code to}, on the server, as far
     * as it can; how many blocks it laid. The run starts within the player's reach, or it is not laid at all.
     */
    public static int lay(final ServerPlayer player, final ServerLevel level, final BlockPos from, final BlockPos to,
                          final InteractionHand hand) {
        final ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof CableItem item) || player.level() != level
                || !player.canInteractWithBlock(from, START_REACH)) {
            return 0;
        }
        final CableType type = item.type();
        final List<BlockPos> path = path(from, to, type.runsStraight());
        int laid = 0;
        int done = 0;
        BlockPos stopped = null;
        TextKey why = null;
        for (final BlockPos pos : path) {
            if (level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.holds(type)) {
                done++;
                continue;
            }
            if (!level.isLoaded(pos) || !Protection.may(Actor.of(player), ProtectedAction.PLACE, level, pos)
                    || !level.mayInteract(player, pos)) {
                stopped = pos;
                why = PROTECTED;
                break;
            }
            if (!player.getAbilities().instabuild && held.isEmpty()) {
                stopped = pos;
                why = NONE_LEFT;
                break;
            }
            if (!CableItem.lay(level, pos, type)) {
                stopped = pos;
                why = IN_THE_WAY;
                break;
            }
            held.consume(1, player);
            laid++;
            done++;
            final BlockState state = level.getBlockState(pos);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            if (laid == 1) {
                final SoundType sound = state.getSoundType(level, pos, player);
                level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                        (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            }
        }
        final Text said = stopped == null ? LAID.with(done, path.size()) : STOPPED.with(done, path.size(),
                Text.literal(stopped.getX() + ", " + stopped.getY() + ", " + stopped.getZ()), why.text());
        player.displayClientMessage(GameText.component(said), true);
        return laid;
    }

    /** The blocks a run from {@code from} to {@code to} goes through, for a cable that runs only straight or not. */
    public static List<BlockPos> path(final BlockPos from, final BlockPos to, final boolean straight) {
        final List<BlockPos> path = new ArrayList<>();
        for (final RunPath.Step step : RunPath.between(new RunPath.Step(from.getX(), from.getY(), from.getZ()),
                new RunPath.Step(to.getX(), to.getY(), to.getZ()), straight)) {
            path.add(new BlockPos(step.x(), step.y(), step.z()));
        }
        return path;
    }

    private static InteractionHand hand(final CableRunPayload payload) {
        return payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
