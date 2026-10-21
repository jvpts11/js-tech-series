/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.developer.DeveloperMode;
import dev.jstech.core.developer.ThingTests;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * The server's side of the studio's tools: each service by its name, the Core's (the fields of a block, what machines
 * cost, the packets sent, where tags and recipes come from, rolls of a loot table, a structure caught, a block saved
 * and read back) and a mod's own. A question is answered only for a player who may use the developer's tools that read
 * or change the world; anyone else is told so.
 */
@TextHolder
public final class StudioServices {

    /** What an answer holds when the question was refused: why, as the player reads it. */
    public static final String REFUSED = "refused";
    /** The Core's services, by name. */
    public static final String BLOCK = BlockService.NAME;
    public static final String COST = CostService.NAME;
    public static final String PAYLOADS = PayloadService.NAME;
    public static final String SOURCES = SourcesService.NAME;
    public static final String LOOT = LootService.NAME;
    public static final String STRUCTURE = StructureService.NAME;
    public static final String SAVE = SaveService.NAME;

    private static final TextKey NO_SERVICE = TextKey.of("commands.jscore.developer.no_service",
            "The server has no tool of the studio's called %s; is the same J's Core on both sides?");
    private static final String VERSION = "1";
    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    private static final Map<String, IStudioService> SERVICES = new ConcurrentHashMap<>();

    private StudioServices() {
    }

    /**
     * Declares the service {@code name}, from a mod's constructor.
     *
     * @throws IllegalStateException when a service of that name is declared already
     */
    public static void declare(final String name, final IStudioService service) {
        if (SERVICES.putIfAbsent(Objects.requireNonNull(name, "name"), Objects.requireNonNull(service, "service"))
                != null) {
            throw new IllegalStateException("the studio's service " + name + " is declared twice");
        }
    }

    /** Hands the bus what this answers, and declares the Core's services, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToServer(StudioAskPayload.TYPE, StudioAskPayload.STREAM_CODEC, StudioServices::onAsk)
                .playToClient(StudioAnswerPayload.TYPE, StudioAnswerPayload.STREAM_CODEC, StudioServices::onAnswer));
        declare(BlockService.NAME, new BlockService());
        declare(CostService.NAME, new CostService());
        declare(PayloadService.NAME, new PayloadService());
        declare(SourcesService.NAME, new SourcesService());
        declare(LootService.NAME, new LootService());
        declare(StructureService.NAME, new StructureService());
        declare(SaveService.NAME, new SaveService());
    }

    /** What {@code player} is answered to {@code service} asked {@code args}, through {@code reply}; for a test too. */
    public static void ask(final ServerPlayer player, final String service, final CompoundTag args,
                           final Consumer<CompoundTag> reply) {
        if (!DeveloperMode.mayUseWorldTools(player)) {
            reply.accept(refused(GameText.resolve(ThingTests.NOT_ALLOWED)));
            return;
        }
        final IStudioService found = SERVICES.get(service);
        if (found == null) {
            reply.accept(refused(GameText.resolve(NO_SERVICE.with(service))));
            return;
        }
        try {
            found.answer(player, args, reply);
        } catch (final RuntimeException failed) {
            LOGGER.warn("The studio's service {} failed for {}", service, player.getGameProfile().getName(), failed);
            reply.accept(refused(String.valueOf(failed.getMessage())));
        }
    }

    private static CompoundTag refused(final String why) {
        final CompoundTag answer = new CompoundTag();
        answer.putString(REFUSED, why);
        return answer;
    }

    private static void onAsk(final StudioAskPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ask(player, payload.service(), payload.args(), answer -> PacketDistributor.sendToPlayer(player,
                        new StudioAnswerPayload(payload.id(), answer)));
            }
        });
    }

    /* Runs on a client only: the answer is only ever sent to one. */
    private static void onAnswer(final StudioAnswerPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> StudioAnswers.answered(payload.id(), payload.answer()));
    }
}
