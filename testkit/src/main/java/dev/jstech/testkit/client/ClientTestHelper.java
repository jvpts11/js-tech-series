/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import dev.jstech.core.jstechseries.peripheral.IPeripheralEndpoint;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.slf4j.Logger;

/**
 * What a client test builds its sequence with, and the tools its steps call while they run.
 *
 * <p>A test method is handed a helper, queues steps ({@link #then}, {@link #thenServer}, {@link #thenWaitUntil},
 * ...) and returns; the runner then runs the steps on the client's thread, one tick at a time. Everything the player
 * does goes through the game's own paths: a right click is the game mode's use on a block (a packet to the integrated
 * server, which opens the menu, which the client turns into a screen); input to a screen is the screen's own
 * {@code mouseClicked} and {@code keyPressed}.
 *
 * <p>Positions are relative to the test's own area, as a GameTest's are (y 2 stands on the ground); {@link #abs}
 * maps them to the world. A mod with steps of its own extends this, and hands its helper to
 * {@link ClientTests#register(String, String, List, ClientTests.IHelperFactory)}.
 */
public class ClientTestHelper {

    private final Minecraft mc;
    private final ClientTests.Planned test;
    private final BlockPos origin;
    private final List<Queued> queue = new ArrayList<>();
    private final List<String> screenshots = new ArrayList<>();
    private final List<Runnable> afterTest = new ArrayList<>();

    /** A device's front is measured in sixty-fourths, from its top left as seen. */
    public static final int FRONT = 64;

    private static final Logger LOGGER = LogUtils.getLogger();
    /* How long a peripheral may take to link before it is clicked: a few ticks alone, far more on a loaded machine. */
    private static final int PERIPHERAL_LINK_WAIT = 200;
    private static final int LEAVING_TICKS = 600;
    private static final int REOPENING_TICKS = 1200;
    private static final int ARRIVAL_TICKS = 200;
    private static final int HOTBAR = 9;

    protected ClientTestHelper(final Minecraft mc, final ClientTests.Planned test, final BlockPos origin) {
        this.mc = mc;
        this.test = test;
        this.origin = origin;
    }

    /**
     * Registers {@code cleanup} to run once the test ends, whether it passed or failed, after its last step. Register
     * it right after the change it undoes. Cleanups run in the reverse order they were registered, each on its own so
     * one that throws does not stop the others. A cleanup that touches the server must not wait for it: it runs on the
     * client's thread.
     */
    public void afterTest(final Runnable cleanup) {
        afterTest.add(cleanup);
    }

    /** Runs {@code action} once, {@code delayTicks} after the step before it finished. */
    public ClientTestHelper then(final int delayTicks, final Runnable action) {
        queue.add(new Queued(delayTicks, "then", () -> {
            action.run();
            return true;
        }));
        return this;
    }

    /** Runs {@code action} on the integrated server's thread, and waits for it to end. */
    public ClientTestHelper thenServer(final int delayTicks, final Consumer<ServerLevel> action) {
        queue.add(new Queued(delayTicks, "server", new IStep() {
            private CompletableFuture<Void> pending;

            @Override
            public boolean tick() {
                if (pending == null) {
                    final MinecraftServer server = server();
                    pending = server.submit(() -> action.accept(serverLevel()));
                }
                if (pending.isCompletedExceptionally()) {
                    try {
                        pending.join();
                    } catch (final CompletionException e) {
                        // The server's own failure, not the wrapper round it.
                        if (e.getCause() instanceof RuntimeException cause) {
                            throw cause;
                        }
                        throw new ClientTestFailure("a step on the server failed", e.getCause());
                    }
                }
                return pending.isDone();
            }
        }));
        return this;
    }

    /** Asks {@code condition} every tick; fails the test when it is still false after {@code maxTicks}. */
    public ClientTestHelper thenWaitUntil(final BooleanSupplier condition, final int maxTicks, final String what) {
        return thenWaitUntil(condition, maxTicks, what, () -> "");
    }

    /**
     * The same, with {@code diagnostics} asked only when the wait times out and added to the failure, for the state
     * that says why the wait never ended.
     */
    public ClientTestHelper thenWaitUntil(final BooleanSupplier condition, final int maxTicks, final String what,
                                          final Supplier<String> diagnostics) {
        queue.add(new Queued(0, "wait:" + what, new IStep() {
            private int waited;

            @Override
            public boolean tick() {
                if (condition.getAsBoolean()) {
                    return true;
                }
                if (++waited > maxTicks) {
                    throw new ClientTestFailure("timed out after " + maxTicks + " ticks waiting for " + what
                            + said(diagnostics));
                }
                return false;
            }
        }));
        return this;
    }

    /**
     * Asks {@code condition} on the server's thread, one question at a time, until it is true; fails after
     * {@code maxTicks} with {@code diagnostics}, also asked on the server. For anything read of the server's level:
     * {@code Level.getBlockEntity} and the like answer nothing from another thread.
     */
    public ClientTestHelper thenWaitUntilServer(final Predicate<ServerLevel> condition, final int maxTicks,
                                                final String what, final Function<ServerLevel, String> diagnostics) {
        queue.add(new Queued(0, "waitServer:" + what, new IStep() {
            private int waited;
            private CompletableFuture<Boolean> probe;

            @Override
            public boolean tick() {
                if (probe == null) {
                    final MinecraftServer server = server();
                    probe = server.submit(() -> condition.test(server.overworld()));
                }
                if (probe.isDone()) {
                    final boolean met;
                    try {
                        met = probe.join();
                    } catch (final CompletionException e) {
                        throw e.getCause() instanceof RuntimeException cause ? cause
                                : new ClientTestFailure("a question to the server failed", e.getCause());
                    }
                    probe = null;
                    if (met) {
                        return true;
                    }
                }
                if (++waited > maxTicks) {
                    String extra;
                    try {
                        final MinecraftServer server = server();
                        extra = server.submit(() -> diagnostics.apply(server.overworld())).join();
                    } catch (final RuntimeException e) {
                        extra = "(diagnostics failed: " + e + ")";
                    }
                    throw new ClientTestFailure("timed out after " + maxTicks + " ticks waiting for " + what
                            + (extra.isEmpty() ? "" : " - " + extra));
                }
                return false;
            }
        }));
        return this;
    }

    /** Waits until the open screen is a {@code type}. */
    public <T extends Screen> ClientTestHelper thenAwaitScreen(final Class<T> type, final int maxTicks) {
        return thenWaitUntil(() -> type.isInstance(mc.screen), maxTicks, "screen " + type.getSimpleName());
    }

    /**
     * Waits until the land round the player has reached their game and been drawn: every chunk within their view
     * distance there, and no section of it left to build. Land the server is still making arrives over many seconds,
     * from the player outwards, so a fixed wait photographs its far edge with holes in it.
     */
    public ClientTestHelper thenAwaitLandDrawn(final int maxTicks) {
        return thenWaitUntil(this::landDrawn, maxTicks, "the land round the player to reach their game and be drawn");
    }

    /**
     * Waits until no screen is open, and until the server has closed the player's window as well. A window closed on
     * the client reaches the server as a packet, and the game closes whatever window the server has open when that
     * packet lands, whichever window it was sent for: a step that opened the next window from the server before then
     * would have it closed at once, with its screen still up on the client.
     */
    public ClientTestHelper thenAwaitNoScreen(final int maxTicks) {
        thenWaitUntil(() -> mc.screen == null, maxTicks, "no screen");
        return thenWaitUntilServer(level -> serverPlayer().containerMenu == serverPlayer().inventoryMenu, maxTicks,
                "the server to close the window",
                level -> "still open: " + serverPlayer().containerMenu.getClass().getSimpleName());
    }

    /**
     * Saves and leaves the world, opens it again from its files and waits until the player is back in: a real
     * reload, so what the test checks afterwards went through the save. Any open screen is closed.
     */
    public ClientTestHelper thenSaveAndReload(final int delayTicks) {
        queue.add(new Queued(delayTicks, "saveAndReload", new IStep() {
            private int phase;
            private int waited;

            @Override
            public boolean tick() {
                waited++;
                switch (phase) {
                    case 0 -> {
                        if (mc.level == null) {
                            throw new ClientTestFailure("no world to reload");
                        }
                        mc.level.disconnect();
                        mc.disconnect(new TitleScreen());
                        phase = 1;
                        waited = 0;
                    }
                    case 1 -> {
                        if (mc.level == null && mc.getSingleplayerServer() == null) {
                            mc.createWorldOpenFlows().openWorld(ClientTestRunner.WORLD_NAME, () -> {
                                throw new ClientTestFailure("the test world could not be opened again");
                            });
                            phase = 2;
                            waited = 0;
                        } else if (waited > LEAVING_TICKS) {
                            throw new ClientTestFailure("leaving the world took more than " + LEAVING_TICKS
                                    + " ticks");
                        }
                    }
                    case 2 -> {
                        final MinecraftServer server = mc.getSingleplayerServer();
                        if (mc.level != null && mc.player != null && mc.screen == null && server != null
                                && server.isReady() && mc.player.tickCount > 20) {
                            return true;
                        }
                        if (waited > REOPENING_TICKS) {
                            throw new ClientTestFailure("opening the world again took more than " + REOPENING_TICKS
                                    + " ticks");
                        }
                    }
                    default -> throw new IllegalStateException("bad reload phase " + phase);
                }
                return false;
            }
        }));
        return this;
    }

    /**
     * Moves the player to {@code relative}, their feet, looking toward {@code facing}, and waits for the client to have
     * got there. The move is the server's, and the client learns of it a packet later; the steps that follow click
     * from where the client thinks the player stands, so a click sent before the move arrived would aim from the old
     * place.
     */
    public ClientTestHelper thenTeleport(final int delayTicks, final BlockPos relative, final Direction facing) {
        thenServer(delayTicks, level -> teleport(relative, facing));
        return thenWaitUntil(() -> player() != null
                        && player().position().distanceTo(Vec3.atBottomCenterOf(abs(relative))) < 2.0,
                ARRIVAL_TICKS, "the client's player to arrive at " + relative);
    }

    /**
     * Right-clicks the block at {@code relative} with the empty hand, the way a player opens its screen. A peripheral
     * of the Core is waited for until it has linked to its machine: one clicked before opens nothing.
     */
    public ClientTestHelper thenRightClick(final int delayTicks, final BlockPos relative) {
        thenWaitUntilServer(level -> !(level.getBlockEntity(abs(relative)) instanceof IPeripheralEndpoint peripheral)
                        || peripheral.linkedOwner().isPresent(), PERIPHERAL_LINK_WAIT,
                "the peripheral at " + relative + " to link to its machine before it is clicked",
                level -> "it is linked to nothing");
        return then(delayTicks, () -> rightClick(relative));
    }

    /**
     * Right-clicks the front of the device at {@code relative} at {@code x}, {@code y} sixty-fourths from its top
     * left as seen, as {@link #rightClickFront} does.
     */
    public ClientTestHelper thenRightClickFront(final int delayTicks, final BlockPos relative, final double x,
                                                final double y) {
        return then(delayTicks, () -> rightClickFront(relative, x, y, 0));
    }

    /** Takes a screenshot named after the test and {@code label}. */
    public ClientTestHelper thenScreenshot(final int delayTicks, final String label) {
        return then(delayTicks, () -> screenshot(label));
    }

    /**
     * Fails, after {@code delayTicks}, unless the GUI's rectangle {@code x}, {@code y}, {@code width} by
     * {@code height} looks as the reference picture named after the test and {@code label} does.
     */
    public ClientTestHelper thenMatchesReference(final int delayTicks, final String label, final int x, final int y,
                                                 final int width, final int height) {
        return then(delayTicks, () -> matchesReference(label, x, y, width, height));
    }

    /** Asserts {@code condition} after {@code delayTicks}. */
    public ClientTestHelper thenAssert(final int delayTicks, final BooleanSupplier condition, final String message) {
        return then(delayTicks, () -> assertTrue(condition.getAsBoolean(), message));
    }

    /** Queues sneak, place, release for {@code relative}: three ticks, the way a player does it. */
    public ClientTestHelper thenPlace(final int delayTicks, final BlockPos relative) {
        return thenPlaceAgainst(delayTicks, relative, Direction.UP);
    }

    /** The same, placing against the neighbour {@code face} points away from. */
    public ClientTestHelper thenPlaceAgainst(final int delayTicks, final BlockPos relative, final Direction face) {
        then(delayTicks, () -> sneak(true));
        then(2, () -> placeAgainst(relative, face));
        return then(1, () -> sneak(false));
    }

    /** Queues sneak, right click on {@code face} of the block at {@code relative}, release. */
    public ClientTestHelper thenSneakClick(final int delayTicks, final BlockPos relative, final Direction face) {
        then(delayTicks, () -> sneak(true));
        then(2, () -> rightClick(relative, face));
        return then(1, () -> sneak(false));
    }

    /** Queues a step on the server that fills the hotbar's first slots with {@code stacks} and empties the rest. */
    public ClientTestHelper thenGive(final int delayTicks, final ItemStack... stacks) {
        return thenServer(delayTicks, level -> {
            for (int i = 0; i < HOTBAR; i++) {
                give(i, i < stacks.length ? stacks[i].copy() : ItemStack.EMPTY);
            }
        });
    }

    /**
     * Fails unless the GUI's rectangle looks as the reference picture {@code <test>-<label>} does, among the
     * resources of the test's mod; what was drawn, and where it differs, is left in the run's folder.
     */
    public void matchesReference(final String label, final int x, final int y, final int width, final int height) {
        ReferencePictures.check(mc, test.modid(), pictureName(label), x, y, width, height);
    }

    /** A copy of the GUI's rectangle of the last frame; close it when done. */
    public NativeImage capture(final int x, final int y, final int width, final int height) {
        return ReferencePictures.capture(mc, x, y, width, height);
    }

    /** Fails unless two pictures of {@code label} look the same, as a reference picture is compared. */
    public void assertSamePicture(final String label, final NativeImage drawn, final NativeImage other) {
        ReferencePictures.same(mc, pictureName(label), drawn, other);
    }

    public Minecraft mc() {
        return mc;
    }

    /** The test's name, its class's and its method's. */
    public String testName() {
        return test.name();
    }

    public BlockPos origin() {
        return origin;
    }

    public BlockPos abs(final BlockPos relative) {
        return origin.offset(relative);
    }

    public MinecraftServer server() {
        final MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            throw new ClientTestFailure("no integrated server");
        }
        return server;
    }

    public ServerLevel serverLevel() {
        return server().overworld();
    }

    public ServerPlayer serverPlayer() {
        final List<ServerPlayer> players = server().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            throw new ClientTestFailure("no player on the integrated server");
        }
        return players.getFirst();
    }

    public LocalPlayer player() {
        if (mc.player == null) {
            throw new ClientTestFailure("no client player");
        }
        return mc.player;
    }

    /** The open screen as {@code type}, or a failure naming what is open instead. */
    public <T extends Screen> T screen(final Class<T> type) {
        if (type.isInstance(mc.screen)) {
            return type.cast(mc.screen);
        }
        throw new ClientTestFailure("expected " + type.getSimpleName() + " to be open but found "
                + (mc.screen == null ? "no screen" : mc.screen.getClass().getSimpleName()));
    }

    /** The open screen as {@code type}, or null when something else, or nothing, is open. Never fails. */
    public <T extends Screen> T openScreen(final Class<T> type) {
        return type.isInstance(mc.screen) ? type.cast(mc.screen) : null;
    }

    /** On the server: moves the player to {@code relative} looking toward {@code facing}. */
    public void teleport(final BlockPos relative, final Direction facing) {
        final BlockPos at = abs(relative);
        serverPlayer().teleportTo(serverLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, facing.toYRot(), 0.0F);
    }

    /** On the server: puts {@code stack} in the player's main hand. */
    public void hold(final ItemStack stack) {
        serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    /** On the server: puts {@code stack} in the inventory's slot {@code slot}, 0 to 8 the hotbar's. */
    public void give(final int slot, final ItemStack stack) {
        serverPlayer().getInventory().setItem(slot, stack);
    }

    /** On the client: selects the hotbar's slot {@code slot}; the next use sends the choice to the server first. */
    public void selectHotbar(final int slot) {
        player().getInventory().selected = slot;
    }

    /**
     * On the client: uses the held item on the block at {@code relative}, on the face that looks at the player, the
     * same path a real click takes, so the server's checks of reach and of the hit apply.
     */
    public void rightClick(final BlockPos relative) {
        final Vec3 center = Vec3.atCenterOf(abs(relative));
        rightClick(relative, Direction.getNearest(player().getEyePosition().subtract(center)));
    }

    /** On the client: uses the held item on the face {@code face} of the block at {@code relative}. */
    public void rightClick(final BlockPos relative, final Direction face) {
        final BlockPos target = abs(relative);
        final Vec3 location = Vec3.atCenterOf(target).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        final BlockHitResult hit = new BlockHitResult(location, face, target, false);
        Objects.requireNonNull(mc.gameMode, "no game mode").useItemOn(player(), InteractionHand.MAIN_HAND, hit);
    }

    /**
     * On the client: uses the held item on the front of the device at {@code relative}, the face it turns to whoever
     * placed it, at {@code x}, {@code y} sixty-fourths from the front's top left as seen and {@code depth}
     * sixty-fourths behind its face: aimed from the player's eyes at that point and landing where the look crosses
     * the face of the block, as the game's own aim would, so a part set back in the front is pressed where it is
     * drawn.
     */
    public void rightClickFront(final BlockPos relative, final double x, final double y, final int depth) {
        final BlockPos at = abs(relative);
        final Direction facing = facingOf(at);
        final Vec3 out = Vec3.atLowerCornerOf(facing.getNormal());
        final Vec3 faceCentre = Vec3.atCenterOf(at).add(out.scale(0.5));
        final Vec3 target = frontPoint(at, x, y).subtract(out.scale(depth / (double) FRONT));
        final Vec3 eye = player().getEyePosition();
        final Vec3 look = target.subtract(eye);
        final Vec3 hit = eye.add(look.scale(out.dot(faceCentre.subtract(eye)) / out.dot(look)));
        Objects.requireNonNull(mc.gameMode, "no game mode")
                .useItemOn(player(), InteractionHand.MAIN_HAND, new BlockHitResult(hit, facing, at, false));
    }

    /**
     * On the client: turns the player's head to the point {@code x}, {@code y} sixty-fourths from the top left of the
     * front of the device at {@code relative}, as seen, so the crosshair rests on what is drawn there.
     */
    public void lookAtFront(final BlockPos relative, final double x, final double y) {
        lookAtFront(relative, x, y, 0);
    }

    /**
     * On the client: turns the player to look at the point ({@code x}, {@code y}) of a block's front, {@code out}
     * sixty-fourths out in front of its face, where a part standing out of the block stands.
     */
    public void lookAtFront(final BlockPos relative, final double x, final double y, final double out) {
        final BlockPos at = abs(relative);
        final Vec3 normal = Vec3.atLowerCornerOf(facingOf(at).getNormal());
        lookAt(frontPoint(at, x, y).add(normal.scale(out / FRONT)));
    }

    /** On the client: turns the player's head to the world's point {@code point}, so the crosshair rests on it. */
    public void lookAt(final Vec3 point) {
        final Vec3 look = point.subtract(player().getEyePosition());
        player().setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player().setXRot((float) -Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z))));
    }

    /**
     * On the client: places the held block item at {@code relative} by clicking the top of the block below it, as a
     * player builds on the ground. It is turned as the player faces, so teleport with the right facing first; hold
     * sneak ({@link #sneak}) when the block below has a screen, as a player would.
     */
    public void place(final BlockPos relative) {
        placeAgainst(relative, Direction.UP);
    }

    /**
     * On the client: places the held block item at {@code relative} by clicking the neighbour on the side
     * {@code face} points away from ({@code UP} clicks the block below, {@code NORTH} the block to the south).
     */
    public void placeAgainst(final BlockPos relative, final Direction face) {
        rightClick(relative.relative(face.getOpposite()), face);
    }

    /**
     * On the client: presses or lets go of the sneak key. The key reaches the server on the next tick, so wait a tick
     * before the click that depends on it.
     */
    public void sneak(final boolean down) {
        mc.options.keyShift.setDown(down);
    }

    /** Presses and lets go of the left mouse button at the screen's point ({@code x}, {@code y}). */
    public void click(final double x, final double y) {
        final Screen screen = screen(Screen.class);
        screen.mouseClicked(x, y, 0);
        screen.mouseReleased(x, y, 0);
    }

    /**
     * Rests the pointer at the screen's point ({@code x}, {@code y}) without pressing anything, as moving the mouse
     * there would. The move is handed to the handler the game gave the window for the cursor's moves, so what follows
     * is what a real move does: the next frame is drawn with the pointer there.
     */
    public void pointAt(final double x, final double y) {
        final Window window = Minecraft.getInstance().getWindow();
        final long handle = window.getWindow();
        final GLFWCursorPosCallback moved = GLFW.glfwSetCursorPosCallback(handle, null);
        GLFW.glfwSetCursorPosCallback(handle, moved);
        if (moved != null) {
            moved.invoke(handle, x * window.getGuiScale(), y * window.getGuiScale());
        }
    }

    /** Presses and lets go of the right mouse button at the screen's point ({@code x}, {@code y}). */
    public void rightClick(final double x, final double y) {
        final Screen screen = screen(Screen.class);
        screen.mouseClicked(x, y, 1);
        screen.mouseReleased(x, y, 1);
    }

    /** Clicks at ({@code x}, {@code y}) from the open container screen's top left. */
    public void clickGui(final int x, final int y) {
        final AbstractContainerScreen<?> screen = screen(AbstractContainerScreen.class);
        click(screen.getGuiLeft() + x + 0.5, screen.getGuiTop() + y + 0.5);
    }

    /** Turns the wheel {@code delta} notches at ({@code x}, {@code y}) from the container screen's top left. */
    public void scrollGui(final int x, final int y, final double delta) {
        final AbstractContainerScreen<?> screen = screen(AbstractContainerScreen.class);
        screen.mouseScrolled(screen.getGuiLeft() + x + 0.5, screen.getGuiTop() + y + 0.5, 0.0, delta);
    }

    /** Drags the left button from ({@code x1}, {@code y1}) to ({@code x2}, {@code y2}) of the container screen. */
    public void dragGui(final int x1, final int y1, final int x2, final int y2) {
        final AbstractContainerScreen<?> screen = screen(AbstractContainerScreen.class);
        final double sx = screen.getGuiLeft() + x1 + 0.5;
        final double sy = screen.getGuiTop() + y1 + 0.5;
        final double ex = screen.getGuiLeft() + x2 + 0.5;
        final double ey = screen.getGuiTop() + y2 + 0.5;
        screen.mouseClicked(sx, sy, 0);
        screen.mouseDragged(ex, ey, 0, ex - sx, ey - sy);
        screen.mouseReleased(ex, ey, 0);
    }

    /** Presses and lets go of a key on the open screen. */
    public void key(final int keyCode) {
        key(keyCode, 0);
    }

    /** The same, with modifiers held: {@code GLFW_MOD_CONTROL} and the like, joined with {@code |}. */
    public void key(final int keyCode, final int modifiers) {
        final Screen screen = screen(Screen.class);
        screen.keyPressed(keyCode, 0, modifiers);
        screen.keyReleased(keyCode, 0, modifiers);
    }

    /** Types {@code text} into the open screen, a character at a time. */
    public void type(final String text) {
        final Screen screen = screen(Screen.class);
        for (final char c : text.toCharArray()) {
            screen.charTyped(c, 0);
        }
    }

    /**
     * Saves the frame now as {@code ct-<test>-<label>.png} in the game's screenshots folder, or fails the test. It is
     * written here and now, not by the game's own screenshot, which writes it later on another thread and only logs a
     * failure: a picture another program holds open would keep the last run's in its place, unnoticed.
     */
    public String screenshot(final String label) {
        final String name = "ct-" + pictureName(label) + ".png";
        final File folder = new File(mc.gameDirectory, Screenshot.SCREENSHOT_DIR);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new ClientTestFailure("the screenshots folder " + folder + " could not be made");
        }
        try (NativeImage frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            frame.writeToFile(new File(folder, name));
        } catch (final IOException failed) {
            throw new ClientTestFailure("the screenshot " + name + " could not be written", failed);
        }
        screenshots.add(name);
        return name;
    }

    /**
     * The colour on the screen now at the GUI's point ({@code x}, {@code y}), as ARGB. It reads the last frame
     * drawn, so it proves what the player sees.
     */
    public int pixel(final int x, final int y) {
        try (FrameSampler frame = sampleFrame()) {
            return frame.pixel(x, y);
        }
    }

    /**
     * One copy of the last frame drawn, to read many pixels from; close it when done. Reading a grid through
     * {@link #pixel} would copy the whole frame once a sample.
     */
    public FrameSampler sampleFrame() {
        return new FrameSampler(mc.getWindow(), Screenshot.takeScreenshot(mc.getMainRenderTarget()));
    }

    public void assertTrue(final boolean condition, final String message) {
        if (!condition) {
            throw new ClientTestFailure(message);
        }
    }

    public void assertEquals(final Object expected, final Object actual, final String message) {
        if (!Objects.equals(expected, actual)) {
            throw new ClientTestFailure(message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    public void fail(final String message) {
        throw new ClientTestFailure(message);
    }

    List<Queued> steps() {
        return queue;
    }

    List<String> screenshotsTaken() {
        return screenshots;
    }

    void runAfterTest() {
        for (int i = afterTest.size() - 1; i >= 0; i--) {
            try {
                afterTest.get(i).run();
            } catch (final RuntimeException e) {
                LOGGER.error("{} a cleanup of {} failed", ClientTestReport.MARK, test.name(), e);
            }
        }
        afterTest.clear();
    }

    private String pictureName(final String label) {
        return test.name().replace('.', '_') + "-" + label;
    }

    private static String said(final Supplier<String> diagnostics) {
        String extra;
        try {
            extra = diagnostics.get();
        } catch (final RuntimeException e) {
            extra = "(diagnostics failed: " + e + ")";
        }
        return extra.isEmpty() ? "" : " - " + extra;
    }

    /*
     * Whether every chunk within the player's view distance, less the ring at its edge the server may send last, is in
     * their game, and nothing is left to build for drawing; the server sends a round view, not a square one.
     */
    private boolean landDrawn() {
        final ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return false;
        }
        final int reach = Math.max(1, mc.options.getEffectiveRenderDistance() - 1);
        final ChunkPos at = mc.player.chunkPosition();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz <= reach * reach && !level.getChunkSource().hasChunk(at.x + dx, at.z + dz)) {
                    return false;
                }
            }
        }
        return mc.levelRenderer.hasRenderedAllSections();
    }

    /* Which way the device at that place of the world faces: the face it turns to whoever placed it. */
    private Direction facingOf(final BlockPos at) {
        return Objects.requireNonNull(mc.level, "no level").getBlockState(at)
                .getValue(HorizontalDirectionalBlock.FACING);
    }

    /* The point x, y sixty-fourths from the top left of the front of the device at that place, as seen. */
    private Vec3 frontPoint(final BlockPos at, final double x, final double y) {
        final Direction facing = facingOf(at);
        final Vec3 out = Vec3.atLowerCornerOf(facing.getNormal());
        final Vec3 right = Vec3.atLowerCornerOf(facing.getCounterClockWise().getNormal());
        return Vec3.atCenterOf(at).add(out.scale(0.5)).add(right.scale(x / FRONT - 0.5)).add(0, 0.5 - y / FRONT, 0);
    }

    /** One step of a test; it answers true once it has finished, a step done at once on its first tick. */
    @FunctionalInterface
    public interface IStep {

        boolean tick();
    }

    /** A copy of one frame, read by the GUI's points. */
    public static final class FrameSampler implements AutoCloseable {

        private final Window window;
        private final NativeImage image;

        private FrameSampler(final Window window, final NativeImage image) {
            this.window = window;
            this.image = image;
        }

        /** The colour at the GUI's point ({@code x}, {@code y}), as ARGB. */
        public int pixel(final int x, final int y) {
            final double scale = window.getGuiScale();
            final int px = (int) Math.min(window.getWidth() - 1, x * scale);
            final int py = (int) Math.min(window.getHeight() - 1, y * scale);
            final int abgr = image.getPixelRGBA(px, py);
            final int r = abgr & 0xFF;
            final int g = (abgr >> 8) & 0xFF;
            final int b = (abgr >> 16) & 0xFF;
            final int a = (abgr >>> 24) & 0xFF;
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        @Override
        public void close() {
            image.close();
        }
    }

    /* A step queued, with the ticks to wait before it and what it is called in a failure. */
    record Queued(int delay, String label, IStep step) {
    }
}
