/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.item.ItemMode;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.PhysicsSight;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The physics tool, which moves moving structures by hand: aimed at one, it grabs it where it is aimed and holds that
 * point before the player as they move and look, as a stiff spring that bears its weight; it pushes it with a blow
 * where it is aimed; or it freezes it where it stands, in the air too, and lets it go again. Its strength, from a
 * tenth to the whole, is how hard it holds and pushes. The wheel changes its mode, and with shift its strength.
 */
@TextHolder
public final class PhysicsTool {

    public static final TextKey GRAB = TextKey.of("jscore.physics_tool.grab", "Grab");
    public static final TextKey PUSH = TextKey.of("jscore.physics_tool.push", "Push");
    public static final TextKey FREEZE = TextKey.of("jscore.physics_tool.freeze", "Freeze");
    public static final TextKey HEAD = TextKey.of("jscore.physics_tool.head", "PHYSICS TOOL");
    public static final TextKey STRENGTH_IS = TextKey.of("jscore.physics_tool.strength", "Strength: %s%%");
    public static final TextKey SCROLL_MODE = TextKey.of("jscore.physics_tool.scroll_mode", "Scroll: the mode");
    public static final TextKey SCROLL_STRENGTH = TextKey.of("jscore.physics_tool.scroll_strength", "Shift+scroll:");
    public static final TextKey THE_STRENGTH = TextKey.of("jscore.physics_tool.the_strength", "the strength");
    public static final TextKey NAMED = TextKey.of("jscore.physics_tool.named", "%s, %s");
    public static final TextKey HELD_AWAY = TextKey.of("jscore.physics_tool.held_away", "held %s m from you");
    public static final TextKey FROZEN = TextKey.of("jscore.physics_tool.frozen", "%s is frozen where it stands");
    public static final TextKey FREED = TextKey.of("jscore.physics_tool.freed", "%s moves again");
    public static final TextKey NOTHING_AIMED = TextKey.of("jscore.physics_tool.nothing_aimed",
            "Aim at a moving structure");

    /** The tool's modes, in the order the wheel goes through them. */
    public static final ItemMode GRAB_MODE = ItemMode.of("grab", GRAB);
    public static final ItemMode PUSH_MODE = ItemMode.of("push", PUSH);
    public static final ItemMode FREEZE_MODE = ItemMode.of("freeze", FREEZE);
    /** The least, the most and the first strength of the tool, in hundredths, and the step the wheel moves it by. */
    public static final int LEAST = 10;
    public static final int MOST = 100;
    public static final int FIRST = 40;
    public static final int STEP = 10;
    /** How far the tool reaches, in blocks. */
    public static final double REACH = 48;
    /** The tool's strength, in hundredths of its whole. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STRENGTH =
            CoreItems.CONTENT.component("physics_tool_strength", Codec.intRange(LEAST, MOST), ByteBufCodecs.VAR_INT);
    /** The physics tool. */
    public static final ItemEntry<PhysicsToolItem> ITEM = CoreItems.CONTENT.item("physics_tool",
                    properties -> new PhysicsToolItem(properties.stacksTo(1))).named("Physics Tool")
            .look(IItemLook.HANDMADE).modes(GRAB_MODE, PUSH_MODE, FREEZE_MODE).component(STRENGTH, FIRST)
            .tag(PhysicsSight.SEES_PHYSICS).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    /** Its model in the hand: the gunmetal body, the cyan window and the claws round the emitter. */
    public static final DeclaredModel MODEL = CoreItems.CONTENT.model("physics_tool")
            .file("jsmodels/physics_tool.geo.json")
            .texture("textures/model/physics_tool.png")
            .register();
    private static final String NETWORK_VERSION = "1";
    /*
     * The structure each player holds with the tool, by the player, the server's and the player's game's apart: a
     * player is equal to another of the same entity id, which the server's and the game's own are in one game.
     */
    private static final Map<Player, Grab> SERVER_GRABS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, Grab> CLIENT_GRABS = Collections.synchronizedMap(new WeakHashMap<>());

    static {
        // Every moving structure built on the Core's kit is moved by it, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, ITEM::get);
    }

    private PhysicsTool() {
    }

    /** Declares the tool, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(NETWORK_VERSION)
                .playToServer(PhysicsToolScrollPayload.TYPE, PhysicsToolScrollPayload.STREAM_CODEC,
                        PhysicsTool::onScroll));
    }

    /** The tool's strength in {@code stack}, from {@link #LEAST} to {@link #MOST} hundredths. */
    public static int strength(final ItemStack stack) {
        return Math.clamp(stack.getOrDefault(STRENGTH.get(), FIRST), LEAST, MOST);
    }

    /** The mode the tool in {@code stack} is in. */
    public static ItemMode mode(final ItemStack stack) {
        return ItemStates.mode(stack).orElse(GRAB_MODE);
    }

    /** What {@code player} holds with the tool now, or null. */
    public static @Nullable Grab grabbed(final Player player) {
        return grabs(player).get(player);
    }

    /** The moving structure {@code player} aims the tool at, and where, within its reach; null for none. */
    public static @Nullable Aim aim(final Player player) {
        final Level level = player.level();
        final Vec3 eye = player.getEyePosition();
        final BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(REACH)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !YardLayout.isYardBlock(hit.getBlockPos().getX())) {
            return null;
        }
        final IStructureBody body = MovingStructures.at(level, hit.getBlockPos());
        return body == null ? null : new Aim(body, hit.getLocation());
    }

    /** {@code player} grabs what they aim at, at the point aimed at, as far from their eyes as it is now. */
    static void grab(final Player player, final Aim aim) {
        grabs(player).put(player, new Grab(aim.body(), MovingStructures.toYard(aim.body(), aim.at()),
                aim.at().distanceTo(player.getEyePosition())));
    }

    /** {@code player} lets go of what they held. */
    static @Nullable Grab letGo(final Player player) {
        return grabs(player).remove(player);
    }

    private static Map<Player, Grab> grabs(final Player player) {
        return player.level().isClientSide() ? CLIENT_GRABS : SERVER_GRABS;
    }

    /* Runs on the server: a player turned the wheel with the tool in the main hand. */
    private static void onScroll(final PhysicsToolScrollPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            final ItemStack stack = player.getMainHandItem();
            if (!stack.is(ITEM.get()) || payload.steps() == 0) {
                return;
            }
            if (payload.strength()) {
                stack.set(STRENGTH.get(), Math.clamp(strength(stack) + STEP * (long) payload.steps(), LEAST, MOST));
            } else {
                for (int i = 0; i < Math.abs(payload.steps()); i++) {
                    ItemStates.cycleMode(stack, payload.steps() < 0);
                }
            }
        });
    }

    /**
     * Where the tool is aimed: the structure, and the place of the world it is aimed at.
     *
     * @param body the structure
     * @param at   the place aimed at, in the world
     */
    public record Aim(IStructureBody body, Vec3 at) {
    }

    /**
     * What a player holds with the tool: the structure, the place of its yard held, and how far before the player's
     * eyes it is held.
     *
     * @param body     the structure
     * @param place    the place held, in its yard
     * @param distance how far before the eyes it is held, in blocks
     */
    public record Grab(IStructureBody body, Vec3 place, double distance) {

        /** Where the place held is in the world now. */
        public Vec3 heldAt() {
            return MovingStructures.toWorld(body, place);
        }
    }
}
