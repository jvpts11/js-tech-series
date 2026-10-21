/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.mojang.brigadier.Command;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.studio.StudioScreen;
import dev.jstech.core.developer.DeveloperMode;
import dev.jstech.core.developer.mcp.CoreTools;
import dev.jstech.core.developer.mcp.DeveloperMcpLogger;
import dev.jstech.core.developer.mcp.GameCommands;
import dev.jstech.core.developer.mcp.IMcpHost;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpCallLog;
import dev.jstech.core.developer.mcp.McpFolders;
import dev.jstech.core.developer.mcp.McpOnServer;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpProtocol;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpServer;
import dev.jstech.core.developer.mcp.McpSide;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.developer.mcp.McpTools;
import dev.jstech.core.developer.mcp.McpWords;
import dev.jstech.core.developer.mcp.WorldTools;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The MCP server of the player's game: turned on by the player, from inside the game, for that run only, and only
 * while developer mode is on; each client asked about on the screen; its tools read the game and act as the player;
 * a mark on the screen while one is let in and while it acts; any key the player presses takes the controls back,
 * and the key "Cut every agent off" takes back every key at once.
 */
public final class McpGame {

    /** How long a question waits for a screen it may open over, in seconds, beside the time it gives the player. */
    private static final int WAITS = 10;

    private static final List<McpCall> ACTING = new CopyOnWriteArrayList<>();
    private static final Deque<Question> QUESTIONS = new ArrayDeque<>();
    private static volatile @Nullable McpServer running;

    private McpGame() {
    }

    /** Hands the game's bus what this answers and declares the tools of the player's game, once, from the client. */
    @ApiStatus.Internal
    public static void listen(final List<McpTool> studioTools) {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> askNext());
        NeoForge.EVENT_BUS.addListener(InputEvent.Key.class, McpGame::onKey);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, event -> {
            if (Minecraft.getInstance().screen == null) {
                mark(event.getGuiGraphics());
            }
        });
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, event -> mark(event.getGuiGraphics()));
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> event.getDispatcher().register(
                Commands.literal("jstech").then(Commands.literal("mcp")
                        .then(Commands.literal("on").executes(context -> say(turn(true))))
                        .then(Commands.literal("off").executes(context -> say(turn(false))))
                        .then(Commands.literal("status").executes(context -> say(status())))
                        .then(Commands.literal("cut").executes(context -> say(McpTexts.CUT.with(cutAll())))))));
        McpOnServer.prompt(McpGame::ask);
        PlayTasks.listen();
        ChatLines.listen();
        WorldTools.of(McpGame::askStudio, McpGame::lookedAt).forEach(McpGame::declare);
        CoreTools.of(() -> McpFolders.of(Minecraft.getInstance().getSingleplayerServer(),
                Minecraft.getInstance().gameDirectory.toPath()), McpGame::command).forEach(McpGame::declare);
        studioTools.forEach(McpGame::declare);
        // The tools to play the game as the player, for automated tests: seeing, acting, the test world, waiting.
        PlaySeeing.all().forEach(McpGame::declare);
        PlayActing.all().forEach(McpGame::declare);
        PlayWorld.all().forEach(McpGame::declare);
        PlayWaiting.all().forEach(McpGame::declare);
    }

    /** The server running, or none. */
    public static Optional<McpServer> running() {
        return Optional.ofNullable(running);
    }

    /** Turns the server on or off, as the player asks from the game; what it says of itself. */
    public static Text turn(final boolean on) {
        if (!on) {
            final McpServer server = running;
            running = null;
            if (server != null) {
                server.protocol().revokeAll();
                server.stop();
            }
            return McpTexts.OFF.text();
        }
        if (!DeveloperMode.on()) {
            return McpTexts.NEEDS_MODE.text();
        }
        final McpServer already = running;
        if (already != null) {
            return McpTexts.ON.with(already.port());
        }
        final int port = DeveloperMode.mcpPort();
        try {
            final McpServer server = McpServer.start(new McpProtocol(new GameHost(), System::currentTimeMillis,
                    new DeveloperMcpLogger()), port);
            running = server;
            return McpTexts.ON.with(server.port());
        } catch (final IOException failed) {
            return McpTexts.FAILED.with(port, String.valueOf(failed.getMessage()));
        }
    }

    /** What the server says of itself now. */
    public static Text status() {
        final McpServer server = running;
        return server == null ? McpTexts.IS_OFF.text() : McpTexts.IS_ON.with(server.port(),
                server.protocol().clients().all().size());
    }

    /** Takes back every client's key, and says how many there were. */
    public static int cutAll() {
        ACTING.forEach(McpCall::takeBack);
        final McpServer server = running;
        return server == null ? 0 : server.protocol().revokeAll();
    }

    /** The calls acting now. */
    public static List<McpCall> acting() {
        return List.copyOf(ACTING);
    }

    /**
     * Asks the player whether the client {@code name} of {@code version} may come in, on the server's computer when
     * {@code onServer}; the question opens over whatever screen is open, once no container's menu is.
     */
    public static void ask(final String name, final String version, final boolean onServer,
                           final Consumer<Optional<McpPermission>> answered) {
        synchronized (QUESTIONS) {
            QUESTIONS.addLast(new Question(name, version, onServer, answered, System.currentTimeMillis()));
        }
    }

    private static void declare(final McpTool tool) {
        McpTools.declare(McpSide.GAME, JsCore.MODID, tool);
    }

    private static int say(final Text said) {
        final Minecraft game = Minecraft.getInstance();
        if (game.player != null) {
            game.player.displayClientMessage(GameText.component(said), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    /* Opens the next question, when no question is open and no container's menu would be closed by it. */
    private static void askNext() {
        final Minecraft game = Minecraft.getInstance();
        if (game.screen instanceof McpPairingScreen || game.screen instanceof AbstractContainerScreen<?>) {
            synchronized (QUESTIONS) {
                final long now = System.currentTimeMillis();
                QUESTIONS.removeIf(question -> {
                    final boolean stale = now - question.asked() > TimeUnit.SECONDS.toMillis(WAITS
                            + McpOnServer.ANSWER_SECONDS);
                    if (stale) {
                        question.answered().accept(Optional.empty());
                    }
                    return stale;
                });
            }
            return;
        }
        final Question next;
        synchronized (QUESTIONS) {
            next = QUESTIONS.pollFirst();
        }
        if (next != null) {
            game.setScreen(new McpPairingScreen(next.name(), next.version(), next.onServer(), game.screen,
                    next.answered()));
        }
    }

    private static void onKey(final InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (KeyActionsClient.mapping(CoreKeys.CUT_AGENTS).matches(event.getKey(), event.getScanCode())) {
            if (running != null && !running.protocol().clients().all().isEmpty()) {
                say(McpTexts.CUT.with(cutAll()));
            }
            return;
        }
        // Any other key the player presses while an agent acts takes the controls back.
        ACTING.forEach(McpCall::takeBack);
    }

    private static void mark(final GuiGraphics g) {
        final McpServer server = running;
        if (server == null) {
            return;
        }
        final int agents = server.protocol().clients().all().size();
        String acting = null;
        if (!ACTING.isEmpty()) {
            final McpCall call = ACTING.getFirst();
            acting = GameText.resolve(McpTexts.ACTING.with(call.client().name(), call.tool().name() + " "
                    + McpCallLog.summary(call.args())));
        }
        final Minecraft game = Minecraft.getInstance();
        // The studio's foot says the server is on, so its corner is left to the studio's own words.
        McpMark.draw(g, game.font, game.getWindow().getGuiScaledWidth(), agents, !(game.screen instanceof StudioScreen),
                acting, KeyActionsClient.mapping(CoreKeys.CUT_AGENTS).getTranslatedKeyMessage().getString());
    }

    private static CompletableFuture<CompoundTag> askStudio(final McpCall call, final String service,
                                                            final CompoundTag args) {
        final CompletableFuture<CompoundTag> answer = new CompletableFuture<>();
        final Minecraft game = Minecraft.getInstance();
        game.execute(() -> {
            if (game.getConnection() == null) {
                final CompoundTag refused = new CompoundTag();
                refused.putString(StudioServices.REFUSED, McpTexts.NO_WORLD.text().english());
                answer.complete(refused);
            } else {
                StudioAnswers.ask(service, args, answer::complete);
            }
        });
        return answer;
    }

    private static CompletableFuture<Optional<BlockPos>> lookedAt(final McpCall call) {
        final Minecraft game = Minecraft.getInstance();
        return game.submit(() -> game.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? Optional.of(hit.getBlockPos().immutable()) : Optional.<BlockPos>empty());
    }

    /*
     * A command let through: in a world of the player's own, run on its server as the player with a cheater's level;
     * on a server whose developer mode is on, sent as the player's own command, for an operator.
     */
    static CompletableFuture<McpResult> command(final McpCall call, final String command) {
        final Minecraft game = Minecraft.getInstance();
        return game.submit(() -> {
            final IntegratedServer own = game.getSingleplayerServer();
            if (game.player == null || !DeveloperMode.onServer()) {
                return CompletableFuture.completedFuture(McpResult.failed(McpWords.COMMAND_NOT_HERE.text()));
            }
            if (own != null) {
                return own.submit(() -> own.getPlayerList().getPlayer(game.player.getUUID())).thenCompose(player ->
                        player == null ? CompletableFuture.completedFuture(McpResult.failed(McpTexts.NO_WORLD.text()))
                                : GameCommands.run(player, command, GameCommands.CHEATS));
            }
            if (!game.player.hasPermissions(GameCommands.CHEATS)) {
                return CompletableFuture.completedFuture(McpResult.failed(McpWords.COMMAND_NOT_HERE.text()));
            }
            game.player.connection.sendCommand(command);
            return CompletableFuture.completedFuture(McpResult.text(McpTexts.SENT.text().english()));
        }).thenCompose(future -> future);
    }

    /**
     * A question waiting for its screen.
     *
     * @param name     who asks
     * @param version  its version
     * @param onServer whether it is on a server's computer
     * @param answered what is handed the answer
     * @param asked    when it asked, in milliseconds since the epoch
     */
    private record Question(String name, String version, boolean onServer,
                            Consumer<Optional<McpPermission>> answered, long asked) {
    }

    /** The player's game, as its MCP server needs it. */
    private static final class GameHost implements IMcpHost {

        @Override
        public McpSide side() {
            return McpSide.GAME;
        }

        @Override
        public String version() {
            return McpOnServer.coreVersion();
        }

        @Override
        public Optional<Text> closed() {
            return DeveloperMode.on() ? Optional.empty() : Optional.of(McpWords.OFF.text());
        }

        @Override
        public CompletableFuture<Pairing> pair(final String name, final String version) {
            final CompletableFuture<Pairing> answer = new CompletableFuture<>();
            ask(name, version, false, permission -> answer.complete(permission.map(given -> Pairing.allowed(given,
                    null)).orElseGet(() -> Pairing.refused(McpWords.PAIR_REFUSED.text()))));
            return answer.completeOnTimeout(Pairing.refused(McpWords.PAIR_TIMED_OUT.text()), WAITS
                    + McpOnServer.ANSWER_SECONDS, TimeUnit.SECONDS);
        }

        @Override
        public void began(final McpCall call) {
            ACTING.add(call);
        }

        @Override
        public void ended(final McpCall call) {
            ACTING.remove(call);
        }
    }
}
