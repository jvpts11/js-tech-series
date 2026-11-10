/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.client.PlayerTextScreen;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.player.SystemRun;
import dev.jstech.computers.os.player.SystemScreen;
import dev.jstech.computers.vm.program.InputSent;
import dev.jstech.core.text.TextBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * The text screen of a system a player wrote with a shell of its own (computers/docs/design/player-systems.md):
 * the machine sends what is on it whenever it changes, and the monitor sends back what is typed and clicked, which goes
 * to the system's {@code OnInput} or, when the system handed the input to a program, to that program's console.
 */
public final class PlayerScreenPayloads {

    private static final int MOST_TEXT = 64;
    /* The key code of Enter, which ends a line typed to a program's console. */
    private static final int ENTER = 257;

    private PlayerScreenPayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        registrar.playToClient(Screen.TYPE, Screen.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> PlayerTextScreen.accept(payload)));
        ComputerAccess.accept(registrar, Input.TYPE, Input.STREAM_CODEC,
                ComputerAccess.screenAt(Input::host, Input::monitor, MonitorSessionMenu.Phase.PLAYER_SCREEN),
                PlayerScreenPayloads::handleInput);
    }

    /** What is on the system's screen on that machine now. */
    public static Screen screenOf(final BlockPos host, final SystemRun run) {
        final SystemScreen screen = run.screen();
        return new Screen(host, screen.rows(), screen.foregrounds(), screen.backgrounds(), screen.cursorAt());
    }

    private static void handleInput(final Input payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.host()) instanceof IOsHost machine) || machine.systemRun() == null
                || machine.programs() == null) {
            return;
        }
        final MachinePrograms programs = machine.programs();
        final int focus = machine.systemRun().focus();
        if (focus == MachinePrograms.SYSTEM_PID || programs.byId(focus) == null) {
            programs.deliverSystemInput(payload.sent());
            return;
        }
        // A program the system handed the input to reads what is typed at its console, a line at a time.
        final SystemRun run = machine.systemRun();
        if (InputSent.CHAR.equals(payload.sent().kind())) {
            run.type(payload.sent().character());
        } else if (InputSent.KEY.equals(payload.sent().kind()) && payload.sent().key() == ENTER) {
            programs.byId(focus).process().offerInput(run.takeLine());
        }
    }

    /**
     * Server to client: the system's screen.
     *
     * @param rows   each row's text
     * @param fore   the colour of each character, row after row, as 0xRRGGBB
     * @param back   the colour behind each character, row after row
     * @param cursor the cursor's column and row
     */
    public record Screen(BlockPos host, List<String> rows, int[] fore, int[] back, int[] cursor)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Screen> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "player_screen"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Screen> STREAM_CODEC = StreamCodec.of(
                (buf, screen) -> {
                    buf.writeBlockPos(screen.host());
                    buf.writeVarInt(Math.min(screen.rows().size(), SystemScreen.ROWS));
                    for (int i = 0; i < screen.rows().size() && i < SystemScreen.ROWS; i++) {
                        buf.writeUtf(TextBounds.clip(screen.rows().get(i), SystemScreen.COLUMNS),
                                SystemScreen.COLUMNS * 4);
                    }
                    buf.writeVarIntArray(screen.fore());
                    buf.writeVarIntArray(screen.back());
                    buf.writeVarIntArray(screen.cursor());
                },
                buf -> {
                    final BlockPos host = buf.readBlockPos();
                    final int count = Math.min(buf.readVarInt(), SystemScreen.ROWS);
                    final List<String> rows = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        rows.add(buf.readUtf(SystemScreen.COLUMNS * 4));
                    }
                    final int cells = SystemScreen.ROWS * SystemScreen.COLUMNS;
                    return new Screen(host, rows, buf.readVarIntArray(cells), buf.readVarIntArray(cells),
                            buf.readVarIntArray(2));
                });

        public Screen {
            rows = List.copyOf(rows);
            fore = fore.clone();
            back = back.clone();
            cursor = cursor.clone();
        }

        @Override
        public CustomPacketPayload.Type<Screen> type() {
            return TYPE;
        }
    }

    /** Client to server: what was typed or clicked at the system's screen. */
    public record Input(BlockPos host, BlockPos monitor, InputSent sent) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Input> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "player_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> STREAM_CODEC = StreamCodec.of(
                (buf, input) -> {
                    buf.writeBlockPos(input.host());
                    buf.writeBlockPos(input.monitor());
                    buf.writeUtf(input.sent().kind(), MOST_TEXT);
                    buf.writeVarInt(input.sent().key());
                    buf.writeUtf(TextBounds.clip(input.sent().character(), 2), 8);
                    buf.writeVarInt(input.sent().x());
                    buf.writeVarInt(input.sent().y());
                    buf.writeVarInt(input.sent().button());
                },
                buf -> new Input(buf.readBlockPos(), buf.readBlockPos(), new InputSent(buf.readUtf(MOST_TEXT),
                        buf.readVarInt(), buf.readUtf(8), buf.readVarInt(), buf.readVarInt(), buf.readVarInt())));

        @Override
        public CustomPacketPayload.Type<Input> type() {
            return TYPE;
        }
    }
}
