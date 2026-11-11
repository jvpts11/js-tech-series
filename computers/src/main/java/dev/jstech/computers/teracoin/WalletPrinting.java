/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.machine.FileService;
import dev.jstech.computers.operation.payload.teracoin.WalletRequestPayload;
import dev.jstech.computers.operation.payload.teracoin.WalletStatePayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * The Crypto Wallet's Print tab (computers/docs/design/teracoin.md, "The 3D Printer"): the server's price list, the 3D
 * printers linked to the computer, a print paid from the wallet, and the wallet's Auto print, which prints by itself as
 * the wallet's balance grows, while the computer is on with the wallet's file in it.
 */
public final class WalletPrinting {

    private WalletPrinting() {
    }

    /** What the Print tab of {@code wallet}, on {@code host}, shows. */
    public static WalletStatePayload.Print state(final ServerLevel level, final IOsHost host,
                                                 final WalletFiles.Wallet wallet) {
        final List<WalletStatePayload.Price> prices = new ArrayList<>();
        for (final TeracoinSettings.Price price : TeracoinSettings.prices()) {
            prices.add(new WalletStatePayload.Price(BuiltInRegistries.ITEM.getKey(price.item()).toString(),
                    price.units()));
        }
        final List<WalletStatePayload.Printer> printers = new ArrayList<>();
        for (final Printer3DBlockEntity printer : printers(level, host)) {
            printers.add(new WalletStatePayload.Printer(printer.getBlockPos().asLong(), printer.model().name(),
                    printer.jobs().size()));
        }
        final ComputerConsoleState console = host.console();
        final WalletStatePayload.Auto auto = console == null ? WalletStatePayload.Auto.OFF
                : console.autoPrints().of(wallet.path()).map(a -> new WalletStatePayload.Auto(a.item(), a.printer(),
                a.keep(), a.lastDayTime(), a.lastCount(), a.lastBlock())).orElse(WalletStatePayload.Auto.OFF);
        return new WalletStatePayload.Print(printers, prices, auto);
    }

    /** A print, or a change to Auto print, from the wallet the request names; what it did, as a note. */
    public static int act(final ServerLevel level, final IOsHost host, final FileService files,
                          final WalletRequestPayload payload) {
        final Optional<WalletFiles.Wallet> wallet = WalletFiles.read(files, payload.wallet());
        if (wallet.isEmpty()) {
            return WalletStatePayload.NOTE_NOT_YOURS;
        }
        final ComputerConsoleState console = host.console();
        if (payload.action() == WalletRequestPayload.AUTO_PRINT && payload.text().isEmpty()) {
            if (console != null) {
                console.autoPrints().clear(wallet.get().path());
            }
            return WalletStatePayload.NOTE_AUTO_OFF;
        }
        final Item item = itemOf(payload.text());
        if (item == null || TeracoinSettings.priceOf(item) <= 0L) {
            return WalletStatePayload.NOTE_NOT_PRINTABLE;
        }
        final Printer3DBlockEntity printer = linked(level, host, payload.target());
        if (printer == null) {
            return WalletStatePayload.NOTE_NO_PRINTER;
        }
        if (payload.action() == WalletRequestPayload.AUTO_PRINT) {
            if (console == null || !console.autoPrints().set(new AutoPrints.Auto(wallet.get().path(),
                    payload.text(), payload.target(), Math.max(0L, payload.amount()), 0L, 0, 0))) {
                return WalletStatePayload.NOTE_QUEUE_FULL;
            }
            return WalletStatePayload.NOTE_AUTO_SET;
        }
        final Printer3DBlockEntity.Job job = printer.submit(item, Math.max(1, payload.count()),
                payload.hostPos(), wallet.get().path());
        return job == null ? WalletStatePayload.NOTE_QUEUE_FULL : WalletStatePayload.NOTE_PRINTING;
    }

    /**
     * A tick of the computer {@code host}, standing at {@code at}: once a second, each of its wallets' Auto prints
     * puts in its printer as many of its item as the wallet pays past what it keeps and what already waits there.
     */
    public static void tickAuto(final IOsHost host, final ServerLevel level, final BlockPos at) {
        final ComputerConsoleState console = host.console();
        if (console == null || console.autoPrints().all().isEmpty() || !console.autoPrints().secondDue()
                || !host.isRunning() || host.services() == null || host.services().files() == null) {
            return;
        }
        final FileService files = host.services().files();
        final TeracoinChain chain = TeracoinState.of(level.getServer()).chain();
        for (final AutoPrints.Auto auto : console.autoPrints().all()) {
            final Optional<WalletFiles.Wallet> wallet = WalletFiles.read(files, auto.wallet());
            final Item item = itemOf(auto.item());
            final Printer3DBlockEntity printer = linked(level, host, auto.printer());
            if (wallet.isEmpty() || item == null || printer == null) {
                continue;
            }
            final int count = AutoPrints.toPrint(chain.balance(wallet.get().address()), auto.keep(),
                    TeracoinSettings.priceOf(item), unpaid(printer, at, auto.wallet()),
                    Printer3DBlockEntity.MOST_COPIES);
            if (count > 0 && printer.submit(item, count, at, auto.wallet()) != null) {
                console.autoPrints().set(auto.printed(level.getServer().overworld().getDayTime(), count,
                        chain.height()));
            }
        }
    }

    /** The 3D printers linked to {@code host}, in the order of where they stand. */
    public static List<Printer3DBlockEntity> printers(final ServerLevel level, final IOsHost host) {
        final List<Long> linked = new ArrayList<>(host.enabledEndpoints());
        Collections.sort(linked);
        final List<Printer3DBlockEntity> out = new ArrayList<>();
        for (final long pos : linked) {
            if (Loaded.blockEntity(level, BlockPos.of(pos)) instanceof Printer3DBlockEntity printer) {
                out.add(printer);
            }
        }
        return out;
    }

    /* The 3D printer linked to {@code host} standing at {@code pos}, or none. */
    @Nullable
    private static Printer3DBlockEntity linked(final ServerLevel level, final IOsHost host, final long pos) {
        for (final Printer3DBlockEntity printer : printers(level, host)) {
            if (printer.getBlockPos().asLong() == pos) {
                return printer;
            }
        }
        return null;
    }

    /*
     * How many items the wallet at {@code wallet} of the computer at {@code at} has waiting in {@code printer} and not
     * yet paid: every copy not out, but the one printing now, paid as it started.
     */
    private static int unpaid(final Printer3DBlockEntity printer, final BlockPos at, final String wallet) {
        int unpaid = 0;
        final List<Printer3DBlockEntity.Job> jobs = printer.jobs();
        for (int i = 0; i < jobs.size(); i++) {
            final Printer3DBlockEntity.Job job = jobs.get(i);
            if (job.computer() != at.asLong() || !job.wallet().equals(wallet)) {
                continue;
            }
            unpaid += job.copies() - job.done();
            if (i == 0 && printer.phase() != Printer3DBlockEntity.IDLE
                    && printer.phase() != Printer3DBlockEntity.WAITING_TRC
                    && printer.phase() != Printer3DBlockEntity.WAITING_WALLET) {
                unpaid--;
            }
        }
        return Math.max(0, unpaid);
    }

    @Nullable
    private static Item itemOf(final String id) {
        final ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
    }
}
