/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.jstech.core.JsCore;
import dev.jstech.core.developer.DeveloperMode;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The MCP server of a dedicated server: on the server's computer only, turned on by an operator from inside the game
 * ({@code /jstech mcp server on}), never by a file; each client that asks to come in asked about to the operators in
 * the game, and its tools run for the operator who let it in, while they are on. It reads and changes the server's
 * world; it never reaches a player's game. On a player's own game the studio's MCP server is the one.
 */
public final class McpOnServer {

    /** How long the operators are given to answer, in seconds, and how far an operator's look reaches. */
    public static final int ANSWER_SECONDS = 60;
    private static final double REACH = 20.0;
    private static final String VERSION = "1";

    private static final AtomicInteger NEXT = new AtomicInteger();
    private static final Map<Integer, CompletableFuture<IMcpHost.Pairing>> WAITING = new ConcurrentHashMap<>();
    private static volatile @Nullable McpServer running;
    /** What asks the player on a player's game, which the client hands in. */
    private static volatile @Nullable IPrompt prompt;

    private McpOnServer() {
    }

    /** Hands the buses what this answers, and declares the server's tools, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(McpPairAskPayload.TYPE, McpPairAskPayload.STREAM_CODEC, McpOnServer::onAsk)
                .playToServer(McpPairAnswerPayload.TYPE, McpPairAnswerPayload.STREAM_CODEC, McpOnServer::onAnswer));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> stop());
        if (FMLEnvironment.dist.isDedicatedServer()) {
            NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> event.getDispatcher().register(
                    Commands.literal("jstech").then(Commands.literal("mcp").then(Commands.literal("server")
                            .then(Commands.literal("on").executes(McpOnServer::turnOn))
                            .then(Commands.literal("off").executes(McpOnServer::turnOff))
                            .then(Commands.literal("status").executes(McpOnServer::status))
                            .then(Commands.literal("cut").executes(McpOnServer::cut))))));
        }
        final WorldTools.IAsk ask = McpOnServer::ask;
        WorldTools.of(ask, McpOnServer::lookedAt).forEach(tool -> McpTools.declare(McpSide.SERVER, JsCore.MODID,
                tool));
        CoreTools.of(() -> McpFolders.of(ServerLifecycleHooks.getCurrentServer(), null), McpOnServer::command)
                .forEach(tool -> McpTools.declare(McpSide.SERVER, JsCore.MODID, tool));
    }

    /** Hands in what asks the player on a player's game whether a client of a server may come in. */
    @ApiStatus.Internal
    public static void prompt(final IPrompt asker) {
        prompt = asker;
    }

    /** The server running, or none. */
    public static Optional<McpServer> running() {
        return Optional.ofNullable(running);
    }

    /** The numbers of the questions waiting for an operator's answer; for a test. */
    @ApiStatus.Internal
    public static Set<Integer> waiting() {
        return Set.copyOf(WAITING.keySet());
    }

    /**
     * Answers the question numbered {@code id} as {@code player} did from the game, letting the client do
     * {@code permission}, or refusing it for none; an answer from a player who may not use the world's tools is
     * dropped. For the answer that comes from the game, and for a test.
     */
    @ApiStatus.Internal
    public static void answer(final ServerPlayer player, final int id, final Optional<McpPermission> permission) {
        if (!DeveloperMode.mayUseWorldTools(player)) {
            return;
        }
        final CompletableFuture<IMcpHost.Pairing> waiting = WAITING.remove(id);
        if (waiting != null) {
            waiting.complete(permission.map(given -> IMcpHost.Pairing.allowed(given, player.getUUID()))
                    .orElseGet(() -> IMcpHost.Pairing.refused(McpWords.PAIR_REFUSED.text())));
        }
    }

    /** The Core's version, which an MCP server reports. */
    public static String coreVersion() {
        return ModList.get().getModContainerById(JsCore.MODID).map(container -> container.getModInfo().getVersion()
                .toString()).orElse("");
    }

    private static int turnOn(final CommandContext<CommandSourceStack> context) {
        final ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(GameText.component(McpWords.FROM_THE_GAME));
            return 0;
        }
        if (!DeveloperMode.mayUseWorldTools(player)) {
            context.getSource().sendFailure(GameText.component(McpWords.OFF));
            return 0;
        }
        if (running != null) {
            return status(context);
        }
        final int port = DeveloperMode.serverMcpPort();
        try {
            running = McpServer.start(new McpProtocol(new Host(player.server), System::currentTimeMillis,
                    new DeveloperMcpLogger()), port);
        } catch (final IOException failed) {
            context.getSource().sendFailure(GameText.component(McpWords.SERVER_FAILED.with(port,
                    String.valueOf(failed.getMessage()))));
            return 0;
        }
        context.getSource().sendSuccess(() -> GameText.component(McpWords.SERVER_ON.with(port)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int turnOff(final CommandContext<CommandSourceStack> context) {
        if (!allowed(context)) {
            return 0;
        }
        stop();
        context.getSource().sendSuccess(() -> GameText.component(McpWords.SERVER_OFF), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int status(final CommandContext<CommandSourceStack> context) {
        if (!allowed(context)) {
            return 0;
        }
        final McpServer server = running;
        if (server == null) {
            context.getSource().sendSuccess(() -> GameText.component(McpWords.SERVER_IS_OFF), false);
            return Command.SINGLE_SUCCESS;
        }
        final List<PairedClients.PairedClient> clients = server.protocol().clients().all();
        context.getSource().sendSuccess(() -> GameText.component(McpWords.SERVER_IS_ON.with(server.port(),
                clients.size())), false);
        for (final PairedClients.PairedClient client : clients) {
            context.getSource().sendSuccess(() -> GameText.component(McpWords.SERVER_CLIENT.with(client.name(),
                    client.version(), client.permission() == McpPermission.ACT ? McpWords.READS_AND_ACTS
                            : McpWords.READS, client.calls())), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int cut(final CommandContext<CommandSourceStack> context) {
        if (!allowed(context)) {
            return 0;
        }
        final McpServer server = running;
        final int count = server == null ? 0 : server.protocol().revokeAll();
        context.getSource().sendSuccess(() -> GameText.component(McpWords.SERVER_CUT.with(count)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean allowed(final CommandContext<CommandSourceStack> context) {
        final ServerPlayer player = context.getSource().getPlayer();
        if (player != null && !DeveloperMode.mayUseWorldTools(player)) {
            context.getSource().sendFailure(GameText.component(McpWords.OFF));
            return false;
        }
        // The server's console may stop it and look at it, never turn it on.
        return true;
    }

    private static void stop() {
        final McpServer server = running;
        running = null;
        if (server != null) {
            server.protocol().revokeAll();
            server.stop();
        }
    }

    /* The operator who let the call's client in, while they are on the server. */
    private static Optional<ServerPlayer> ownerOf(final McpCall call) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        final Optional<UUID> owner = call.client().owner();
        if (server == null || owner.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(server.getPlayerList().getPlayer(owner.get()));
    }

    private static CompletableFuture<CompoundTag> ask(final McpCall call, final String service,
                                                      final CompoundTag args) {
        final CompletableFuture<CompoundTag> answer = new CompletableFuture<>();
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            answer.complete(refusal(McpWords.OWNER_AWAY.text()));
            return answer;
        }
        server.execute(() -> {
            final Optional<ServerPlayer> owner = ownerOf(call);
            if (owner.isEmpty()) {
                answer.complete(refusal(McpWords.OWNER_AWAY.text()));
            } else {
                StudioServices.ask(owner.get(), service, args, answer::complete);
            }
        });
        return answer;
    }

    /* The block the operator who let the client in looks at. */
    private static CompletableFuture<Optional<BlockPos>> lookedAt(final McpCall call) {
        final CompletableFuture<Optional<BlockPos>> found = new CompletableFuture<>();
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            found.complete(Optional.empty());
            return found;
        }
        server.execute(() -> found.complete(ownerOf(call).flatMap(owner -> {
            final Vec3 eye = owner.getEyePosition();
            final BlockHitResult hit = owner.level().clip(new ClipContext(eye, eye.add(owner.getLookAngle()
                    .scale(REACH)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, owner));
            return hit.getType() == HitResult.Type.BLOCK ? Optional.of(hit.getBlockPos()) : Optional.empty();
        })));
        return found;
    }

    private static CompletableFuture<McpResult> command(final McpCall call, final String command) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        final CompletableFuture<McpResult> done = new CompletableFuture<>();
        if (server == null) {
            done.complete(McpResult.failed(McpWords.OWNER_AWAY.text()));
            return done;
        }
        server.execute(() -> {
            final Optional<ServerPlayer> owner = ownerOf(call);
            if (owner.isEmpty() || !DeveloperMode.mayUseWorldTools(owner.get())) {
                done.complete(McpResult.failed(owner.isEmpty() ? McpWords.OWNER_AWAY.text()
                        : McpWords.COMMAND_NOT_HERE.text()));
                return;
            }
            // An operator's own level: a command they could not run themselves is refused as theirs would be.
            GameCommands.run(owner.get(), command, 0).whenComplete((result, failed) -> done.complete(
                    failed == null ? result : McpResult.failed(McpWords.WENT_WRONG.with(failed.getMessage()))));
        });
        return done;
    }

    private static CompoundTag refusal(final Text why) {
        final CompoundTag out = new CompoundTag();
        out.putString(StudioServices.REFUSED, why.english());
        return out;
    }

    /* Runs on a player's game: a server asks whether a client on its computer may come in. */
    private static void onAsk(final McpPairAskPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final IPrompt asker = prompt;
            if (asker != null) {
                asker.ask(payload.name(), payload.version(), true, permission -> PacketDistributor.sendToServer(
                        new McpPairAnswerPayload(payload.id(), McpPairAnswerPayload.of(permission))));
            }
        });
    }

    private static void onAnswer(final McpPairAnswerPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                answer(player, payload.id(), payload.permission());
            }
        });
    }

    /** What asks the player whether a client may come in, on the screen of the player's game. */
    @FunctionalInterface
    public interface IPrompt {

        /**
         * Asks about the client {@code name} of {@code version}, on the server's computer when {@code onServer}, and
         * hands {@code answered} what the player let it do, or none.
         */
        void ask(String name, String version, boolean onServer, Consumer<Optional<McpPermission>> answered);
    }

    /** The dedicated server, as its MCP server needs it. */
    private record Host(MinecraftServer server) implements IMcpHost {

        @Override
        public McpSide side() {
            return McpSide.SERVER;
        }

        @Override
        public String version() {
            return coreVersion();
        }

        @Override
        public Optional<Text> closed() {
            return DeveloperMode.onServer() ? Optional.empty() : Optional.of(McpWords.OFF.text());
        }

        @Override
        public CompletableFuture<Pairing> pair(final String name, final String version) {
            final CompletableFuture<Pairing> answer = new CompletableFuture<>();
            this.server.execute(() -> {
                final List<ServerPlayer> operators = this.server.getPlayerList().getPlayers().stream()
                        .filter(DeveloperMode::mayUseWorldTools).toList();
                if (operators.isEmpty()) {
                    answer.complete(Pairing.refused(McpWords.NOBODY_TO_ASK.text()));
                    return;
                }
                final int id = NEXT.incrementAndGet();
                WAITING.put(id, answer);
                operators.forEach(operator -> PacketDistributor.sendToPlayer(operator, new McpPairAskPayload(id, name,
                        version)));
            });
            return answer.completeOnTimeout(Pairing.refused(McpWords.PAIR_TIMED_OUT.text()), ANSWER_SECONDS,
                    TimeUnit.SECONDS).whenComplete((pairing, failed) -> WAITING.values().remove(answer));
        }

        @Override
        public void began(final McpCall call) {
            // The server shows nothing of a call: its operators read the log with /jstech mcp server status.
        }

        @Override
        public void ended(final McpCall call) {
            // As above.
        }
    }
}
