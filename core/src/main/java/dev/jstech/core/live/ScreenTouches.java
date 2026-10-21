/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

import dev.jstech.core.util.DeclarationError;
import dev.jstech.core.util.Loaded;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * The blocks whose faces show screens that answer the crosshair ({@link ITouchScreen}), declared once for both sides:
 * the player's game finds the pixel under the crosshair and sends a right click's press there, and the server checks
 * that the player is near and that the pixel is on the screen before the block hears it.
 *
 * <pre>{@code
 * ScreenTouches.declare(PANEL, new ITouchScreen() {
 *     public Optional<TouchArea> area(BlockState state, Direction face) {
 *         return face == state.getValue(FACING) ? Optional.of(TouchArea.whole(64, 32)) : Optional.empty();
 *     }
 *     public void pressed(ServerPlayer player, BlockPos pos, Direction face, int x, int y) {
 *         PanelLayout.buttonAt(x, y).ifPresent(button -> button.press(player.serverLevel(), pos));
 *     }
 * });
 * }</pre>
 */
public final class ScreenTouches {

    private static final List<Declared> DECLARED = new CopyOnWriteArrayList<>();
    private static final String VERSION = "1";
    /** How far from the block's middle a player may press its screen: a little past the game's own reach. */
    private static final double REACH = 8.0;

    /* Each block's screens, worked out the first time one is asked for, once every block is registered. */
    private static volatile @Nullable Map<Block, ITouchScreen> byBlock;

    private ScreenTouches() {
    }

    /**
     * Declares that the faces of the block {@code block} gives show the screens {@code screen} says, from the mod's
     * constructor, before the block is registered.
     */
    public static void declare(final Supplier<? extends Block> block, final ITouchScreen screen) {
        DECLARED.add(new Declared(block, screen));
        byBlock = null;
    }

    /**
     * The screen of {@code state}'s face looking {@code face}, if its block shows one there.
     *
     * @throws IllegalStateException when a block's screens were declared twice
     */
    public static Optional<Touch> at(final BlockState state, final Direction face) {
        final ITouchScreen screen = known().get(state.getBlock());
        if (screen == null) {
            return Optional.empty();
        }
        return screen.area(state, face).map(area -> new Touch(screen, area));
    }

    /** Hands the Core's bus the payload of a press, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToServer(ScreenTouchPayload.TYPE, ScreenTouchPayload.STREAM_CODEC, ScreenTouches::onPressed));
    }

    /** The side a face looks out to, as the pure arithmetic of faces names it. */
    public static ScreenFace.Side sideOf(final Direction face) {
        return switch (face) {
            case NORTH -> ScreenFace.Side.NORTH;
            case SOUTH -> ScreenFace.Side.SOUTH;
            case EAST -> ScreenFace.Side.EAST;
            case WEST -> ScreenFace.Side.WEST;
            case UP -> ScreenFace.Side.UP;
            case DOWN -> ScreenFace.Side.DOWN;
        };
    }

    private static Map<Block, ITouchScreen> known() {
        Map<Block, ITouchScreen> known = byBlock;
        if (known == null) {
            known = new IdentityHashMap<>();
            for (final Declared declared : DECLARED) {
                final Block block = declared.block().get();
                if (known.putIfAbsent(block, declared.screen()) != null) {
                    final String id = BuiltInRegistries.BLOCK.getKey(block).toString();
                    throw DeclarationError.of(BuiltInRegistries.BLOCK.getKey(block).getNamespace(),
                            "the touch screens of " + id, "are declared twice", "declare them once for the block");
                }
            }
            byBlock = known;
        }
        return known;
    }

    /* Runs on the server: a player near enough pressing a pixel that is on the block's screen. */
    private static void onPressed(final ScreenTouchPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            final BlockPos pos = payload.pos();
            if (!Loaded.at(player.serverLevel(), pos)
                    || player.position().distanceTo(Vec3.atCenterOf(pos)) > REACH) {
                return;
            }
            at(player.serverLevel().getBlockState(pos), payload.face()).ifPresent(touch -> {
                if (payload.x() >= 0 && payload.y() >= 0 && payload.x() < touch.area().width()
                        && payload.y() < touch.area().height()) {
                    touch.screen().pressed(player, pos, payload.face(), payload.x(), payload.y());
                }
            });
        });
    }

    /**
     * A block's screen on one face.
     *
     * @param screen what the block declared
     * @param area   where on the face it is
     */
    public record Touch(ITouchScreen screen, TouchArea area) {
    }

    private record Declared(Supplier<? extends Block> block, ITouchScreen screen) {
    }
}
