/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.live.ScreenFace;
import dev.jstech.core.live.ScreenTouchPayload;
import dev.jstech.core.live.ScreenTouches;
import dev.jstech.core.live.TouchArea;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.DeclarationError;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Screens in the world on the player's game: the pixel of a block's screen under the crosshair, which the screen's
 * picture reads to light what is under it as the mouse lights a button ({@link #pointerOn}); the part of the screen
 * there ({@link #targets}), whose press the action bar names ("Right click: Start"); and a right click there, which
 * presses the screen, that part named, instead of using the block or the item in hand.
 */
public final class ScreenTouchClient {

    private static final List<Declared> DECLARED = new CopyOnWriteArrayList<>();
    /* While the crosshair stays on a part, its hint is said again this often, before the game's message fades. */
    private static final int HINT_AGAIN_TICKS = 40;

    /* Each block's parts, worked out the first time one is asked for, once every block is registered. */
    private static volatile @Nullable Map<Block, ITouchTargets> byBlock;
    private static @Nullable Component hint;
    private static @Nullable Component said;
    private static int saidFor;

    private ScreenTouchClient() {
    }

    /** Listens for the use key and the game's ticks, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, ScreenTouchClient::onUse);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> sayHint());
    }

    /**
     * Declares what the parts of the screens of {@code block} are on the player's game, from the mod's client
     * constructor: the part at a pixel, its name for the server and what a press on it does for the action bar.
     */
    public static void targets(final Supplier<? extends Block> block, final ITouchTargets targets) {
        DECLARED.add(new Declared(Objects.requireNonNull(block, "block"), Objects.requireNonNull(targets, "targets")));
        byBlock = null;
    }

    /** The screen pixel the crosshair is on now, if it is on a block's screen and no screen is open over the game. */
    public static Optional<Pointed> pointed() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.screen != null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        final BlockPos pos = hit.getBlockPos();
        final Direction face = hit.getDirection();
        return ScreenTouches.at(minecraft.level.getBlockState(pos), face).flatMap(touch -> {
            final Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            final double[] point = ScreenFace.point(ScreenTouches.sideOf(face), local.x, local.y, local.z);
            return ScreenFace.pixel(touch.area(), point).map(pixel -> new Pointed(pos, face, pixel[0], pixel[1],
                    touch.area()));
        });
    }

    /**
     * Where the crosshair is on the screen of the block at {@code pos}, as {@code {x, y}} in its pixels: what its
     * picture hands its components as the mouse, so the one under the crosshair lights; empty when it is elsewhere.
     */
    public static Optional<int[]> pointerOn(final BlockPos pos) {
        return pointed().filter(at -> at.pos().equals(pos)).map(at -> new int[] {at.x(), at.y()});
    }

    /** The part of a block's screen under the crosshair now, if its block declared its parts. */
    public static Optional<TouchTarget> target() {
        return pointed().flatMap(ScreenTouchClient::targetAt);
    }

    /** What the action bar says now of the part under the crosshair, or none; for a test to read. */
    public static Optional<Component> hint() {
        return Optional.ofNullable(hint);
    }

    /* A right click with the crosshair on a screen presses it there, and nothing else happens. */
    private static void onUse(final InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        pointed().ifPresent(at -> {
            event.setCanceled(true);
            event.setSwingHand(true);
            final String target = targetAt(at).map(TouchTarget::name).orElse("");
            PacketDistributor.sendToServer(new ScreenTouchPayload(at.pos(), at.face(), at.x(), at.y(), target));
        });
    }

    /*
     * While the crosshair is on a part a press works, the action bar says what a press does, with the key that
     * presses: said when the part changes, and again before the game's own message would fade.
     */
    private static void sayHint() {
        hint = target().flatMap(TouchTarget::does).map(ScreenTouchClient::hintOf).orElse(null);
        if (hint == null) {
            said = null;
            return;
        }
        saidFor++;
        if (!hint.equals(said) || saidFor >= HINT_AGAIN_TICKS) {
            Minecraft.getInstance().gui.setOverlayMessage(hint, false);
            said = hint;
            saidFor = 0;
        }
    }

    private static Component hintOf(final Component does) {
        final KeyMapping use = Minecraft.getInstance().options.keyUse;
        final boolean rightClick = use.getKey().equals(InputConstants.Type.MOUSE.getOrCreate(
                InputConstants.MOUSE_BUTTON_RIGHT));
        final Text key = rightClick ? TouchTexts.RIGHT_CLICK.text() : GameText.of(use.getTranslatedKeyMessage());
        return GameText.component(TouchTexts.HINT.with(key, GameText.of(does)));
    }

    private static Optional<TouchTarget> targetAt(final Pointed at) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Optional.empty();
        }
        final ITouchTargets targets = known().get(minecraft.level.getBlockState(at.pos()).getBlock());
        return targets == null ? Optional.empty() : targets.at(at.pos(), at.x(), at.y());
    }

    private static Map<Block, ITouchTargets> known() {
        Map<Block, ITouchTargets> known = byBlock;
        if (known == null) {
            known = new IdentityHashMap<>();
            for (final Declared declared : DECLARED) {
                final Block block = declared.block().get();
                if (known.putIfAbsent(block, declared.targets()) != null) {
                    final String id = BuiltInRegistries.BLOCK.getKey(block).toString();
                    throw DeclarationError.of(BuiltInRegistries.BLOCK.getKey(block).getNamespace(),
                            "the parts of the touch screens of " + id, "are declared twice",
                            "declare them once for the block");
                }
            }
            byBlock = known;
        }
        return known;
    }

    /**
     * The pixel of a block's screen the crosshair is on.
     *
     * @param pos  the block
     * @param face the face its screen is on
     * @param x    the pixel across
     * @param y    the pixel down
     * @param area where on the face the screen is
     */
    public record Pointed(BlockPos pos, Direction face, int x, int y, TouchArea area) {
    }

    private record Declared(Supplier<? extends Block> block, ITouchTargets targets) {
    }
}
