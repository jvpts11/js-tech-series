/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.client.effect.EffectTools;
import dev.jstech.core.command.CommandRoot;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Vector3f;

/**
 * Effects fired from the server, for whoever is near enough to see them: the server says which effect, where and
 * which way, and each player's game plays it from its file, culled by distance. A player's own game plays effects
 * of its own with {@code ClientEffects}.
 *
 * <pre>{@code
 * Effects.at(level, rl("arc_flash"), Vec3.atCenterOf(pos)).send();
 * Effects.at(level, rl("laser"), muzzle).to(target).colour(LASER_RED).send();
 * Effects.onBlock(level, rl("cooling_tower_steam"), towerTop).send();
 * }</pre>
 * The commands {@code /jstech effect edit <effect>}, {@code new} and {@code play <effect>} open the effects editor
 * on it at the block looked at, the card of a new one there, or play it there.
 */
public final class Effects {

    private static final String VERSION = "1";
    private static final String EFFECT = "effect";
    private static final int DEFAULT_REACH = 64;

    private Effects() {
    }

    /** Hands the buses what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, Effects::onRegisterPayloads);
        CommandRoot.CORE.declare(EFFECT, CommandRoot.GAME_MASTERS, (node, context) -> node
                .then(Commands.literal("edit").then(Commands.argument(EFFECT, ResourceLocationArgument.id())
                        .executes(command -> tool(command, EffectToolPayload.Tool.EDIT))))
                .then(Commands.literal("new").executes(command -> tool(command, EffectToolPayload.Tool.NEW)))
                .then(Commands.literal("play").then(Commands.argument(EFFECT, ResourceLocationArgument.id())
                        .executes(command -> tool(command, EffectToolPayload.Tool.PLAY)))));
    }

    /** Fires the effect {@code effect} at {@code at}, facing up, as the builder goes on to say. */
    public static Fire at(final ServerLevel level, final ResourceLocation effect, final Vec3 at) {
        return new Fire(level, effect, at, false);
    }

    /** Fires the effect {@code effect} on the block at {@code pos}, where its file anchors it. */
    public static Fire onBlock(final ServerLevel level, final ResourceLocation effect, final BlockPos pos) {
        return new Fire(level, effect, Vec3.atLowerCornerOf(pos), true);
    }

    private static int tool(final CommandContext<CommandSourceStack> command, final EffectToolPayload.Tool tool)
            throws CommandSyntaxException {
        final ServerPlayer player = command.getSource().getPlayerOrException();
        final ResourceLocation effect = tool == EffectToolPayload.Tool.NEW
                ? ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "none")
                : ResourceLocationArgument.getId(command, EFFECT);
        PacketDistributor.sendToPlayer(player, new EffectToolPayload(tool, effect));
        return 1;
    }

    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(EffectPayload.TYPE, EffectPayload.STREAM_CODEC, Effects::onEffect);
        registrar.playToClient(EffectToolPayload.TYPE, EffectToolPayload.STREAM_CODEC, Effects::onTool);
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onEffect(final EffectPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> ClientEffects.receive(payload));
    }

    /* Runs on a client only, as the one above. */
    private static void onTool(final EffectToolPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> EffectTools.receive(payload));
    }

    /** An effect being fired: say more, then {@link #send()}. */
    public static final class Fire {

        private final ServerLevel level;
        private final ResourceLocation effect;
        private final Vec3 at;
        private final boolean block;
        private Vector3f way = new Vector3f(0, 1, 0);
        private Optional<Vec3> to = Optional.empty();
        private int colour;
        private float scale = 1.0F;
        private int follow = -1;
        private int reach = DEFAULT_REACH;

        private Fire(final ServerLevel level, final ResourceLocation effect, final Vec3 at, final boolean block) {
            this.level = Objects.requireNonNull(level, "level");
            this.effect = Objects.requireNonNull(effect, "effect");
            this.at = Objects.requireNonNull(at, "at");
            this.block = block;
        }

        /** It faces {@code direction}. */
        public Fire facing(final Vec3 direction) {
            final Vec3 unit = direction.normalize();
            this.way = new Vector3f((float) unit.x, (float) unit.y, (float) unit.z);
            return this;
        }

        /** A beam or an arc of it goes to {@code target}. */
        public Fire to(final Vec3 target) {
            this.to = Optional.of(target);
            return this;
        }

        /** Its colour is laid over by {@code argb}. */
        public Fire colour(final int argb) {
            this.colour = argb;
            return this;
        }

        /** It is {@code factor} times as big. */
        public Fire scale(final float factor) {
            this.scale = factor;
            return this;
        }

        /** It follows {@code entity}, where it stands. */
        public Fire following(final Entity entity) {
            this.follow = entity.getId();
            return this;
        }

        /** It is sent to the players within {@code blocks}; 64 if it says none. */
        public Fire reach(final int blocks) {
            this.reach = blocks;
            return this;
        }

        /** Sends it to the players near enough to see it. */
        public void send() {
            PacketDistributor.sendToPlayersNear(this.level, null, this.at.x, this.at.y, this.at.z, this.reach,
                    payload());
        }

        /** What it sends. */
        public EffectPayload payload() {
            return new EffectPayload(this.effect, this.at, this.way, this.to, this.colour, this.scale, this.follow,
                    this.block);
        }
    }
}
