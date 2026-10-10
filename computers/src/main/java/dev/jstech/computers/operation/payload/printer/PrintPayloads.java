/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.printer;

import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.client.os.PrintReplies;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.Printers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Printing from a computer's programs: a Print dialog asks for the printers it can use, and a program sends a
 * document, which the server lays out and puts in the chosen printer's queue, answering what became of it.
 */
public final class PrintPayloads {

    private PrintPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestPrintersPayload.TYPE, RequestPrintersPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestPrintersPayload::hostPos), PrintPayloads::handleRequestPrinters);
        registrar.playToClient(PrintersPayload.TYPE, PrintersPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(PrintPayloads::handlePrinters));
        ComputerAccess.accept(registrar, PrintPayload.TYPE, PrintPayload.STREAM_CODEC,
                ComputerAccess.machine(PrintPayload::hostPos), PrintPayloads::handlePrint);
        registrar.playToClient(PrintedPayload.TYPE, PrintedPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(PrintPayloads::handlePrinted));
    }

    private static void handleRequestPrinters(final RequestPrintersPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        final List<PrintersPayload.Row> rows = new ArrayList<>();
        String machine = "";
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost host) {
            machine = Printers.machineName(host);
            for (final PrinterBlockEntity printer : Printers.of(level, host)) {
                if (rows.size() >= PrintersPayload.MAX_PRINTERS) {
                    break;
                }
                rows.add(new PrintersPayload.Row(printer.getBlockPos().asLong(), printer.model().serializedName(),
                        Printers.status(printer), Printers.port(printer), printer.paperCount()));
            }
        }
        PacketDistributor.sendToPlayer(player, new PrintersPayload(payload.hostPos(), machine, rows));
    }

    private static void handlePrint(final PrintPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost host)) {
            return;
        }
        // The picture is kept on the printed sheet as it came, so what does not read as a picture is not printed.
        if (!payload.picture().isEmpty() && PixImage.decode(payload.picture()) == null) {
            PacketDistributor.sendToPlayer(player, new PrintedPayload(payload.hostPos(), false,
                    Printers.NOTHING.text()));
            return;
        }
        final String machine = Printers.machineName(host);
        final PrintedDocument document = payload.picture().isEmpty()
                ? Printers.text(payload.title(), machine, payload.program(), payload.text(), payload.landscape(),
                        payload.fromPage(), payload.toPage())
                : PrintedDocument.picture(payload.title(), machine, payload.program(), payload.picture(),
                        payload.pictureName());
        final Printers.Result result = Printers.print(level, Printers.at(level, host, payload.printer()), document,
                payload.copies(), player.getGameProfile().getName());
        PacketDistributor.sendToPlayer(player, new PrintedPayload(payload.hostPos(), result.ok(), result.message()));
    }

    private static void handlePrinters(final PrintersPayload payload, final Player player) {
        PrintReplies.acceptPrinters(payload);
    }

    private static void handlePrinted(final PrintedPayload payload, final Player player) {
        PrintReplies.acceptPrinted(payload);
    }
}
