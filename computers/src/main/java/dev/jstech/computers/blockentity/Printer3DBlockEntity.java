/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.block.Printer3DBlock;
import dev.jstech.computers.machine.FileService;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.printer.Printers;
import dev.jstech.computers.teracoin.Printer3DModel;
import dev.jstech.computers.teracoin.TeracoinSettings;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.jstechseries.peripheral.IPeripheralEndpoint;
import dev.jstech.core.jstechseries.peripheral.PeripheralCableType;
import dev.jstech.core.jstechseries.peripheral.PeripheralLink;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A 3D printer (computers/docs/design/teracoin.md, "The 3D Printer"): a peripheral on the peripheral cable that prints
 * the items of the server's price list, the jobs its computer's Crypto Wallet sends it. Each item is paid as it starts,
 * from the wallet that sent its job, at once and with no block to wait for, the TRC paid going out of circulation; with
 * too little in the wallet, or the wallet's file not on its computer, it waits. It prints, then puts the item in its
 * output by itself; with the output full it waits there. TRC is all it costs: no filament and no energy.
 *
 * <p>What the players who see it draw (the item rising on the bed, the head on its row, the bed or the belt putting it
 * out) is worked out on their side from the phase it is in and the tick that phase began at, sent only as they change.
 */
public class Printer3DBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint {

    /* The output only gives: the printer puts its items there, a player, a hopper or a bus takes them. */
    private final FieldItemHandler output = fields().items("Output", OUTPUT_SLOTS).save().toClient().exposed()
            .dropsWhenBroken().accepts((index, stack) -> false);
    private final ValueField<List<Job>> queue = fields().value("Queue", Job.CODEC.listOf(), List.of()).save()
            .toClient();
    /* What it is doing, the tick that began at (a pause moves it on), how far in it is, and whether it is paused. */
    private final IntField phase = fields().integer("Phase", IDLE).save().toClient();
    private final LongField since = fields().longInteger("Since", 0L).save().toClient();
    private final IntField ticks = fields().integer("Ticks", 0).save();
    private final BoolField paused = fields().flag("Paused", false).save().toClient();
    /* How far into its phase it stopped, which the players who see it are sent while it is paused. */
    private final IntField held = fields().integer("Held", 0).save().toClient();
    /* What the wallet waited on holds, while it waits for TRC; how many items it ever printed; its next job's id. */
    private final LongField walletHolds = fields().longInteger("WalletHolds", 0L).toClient();
    private final IntField printed = fields().integer("Printed", 0).save().toClient();
    private final IntField nextId = fields().integer("NextId", 1).save();
    private final ValueField<String> linkedName = fields().value("LinkedName", Codec.STRING, "").toClient();
    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    /* Ticks until it tries again what it waits on: a wallet with the price, or its file. */
    private int retryIn;

    /** Nothing to print. */
    public static final int IDLE = 0;
    /** Printing an item, paid. */
    public static final int PRINTING = 1;
    /** Putting the finished item in its output. */
    public static final int EJECTING = 2;
    /** Waiting for the wallet of its job to hold the price of its next item. */
    public static final int WAITING_TRC = 3;
    /** Waiting for the wallet's file to be on the computer that sent the job. */
    public static final int WAITING_WALLET = 4;
    /** Waiting for room in its output. */
    public static final int OUTPUT_FULL = 5;
    /** How many items its output holds, in three places, before it waits. */
    public static final int OUTPUT_SLOTS = 3;
    /** The most jobs waiting in it at once. */
    public static final int QUEUE_MAX = 8;
    /** The most copies of one job. */
    public static final int MOST_COPIES = 64;
    private static final int RETRY = 20;

    public Printer3DBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.PRINTER_3D_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final Printer3DBlockEntity printer) {
        if (level instanceof ServerLevel server) {
            printer.link.tick(server, pos);
            printer.tickPrint(server);
        }
    }

    /** The model this block is. */
    public Printer3DModel model() {
        return getBlockState().getBlock() instanceof Printer3DBlock block ? block.model()
                : Printer3DModel.PROSA_I3_MK3;
    }

    /**
     * Puts a job in the queue: {@code copies} of {@code item}, paid from the wallet at {@code wallet} on the computer
     * at {@code computer}. The job, or null when the queue is full or the item is not on the server's list.
     */
    @Nullable
    public Job submit(final Item item, final int copies, final BlockPos computer, final String wallet) {
        final List<Job> now = new ArrayList<>(queue.get());
        if (now.size() >= QUEUE_MAX || TeracoinSettings.priceOf(item) <= 0L || copies <= 0) {
            return null;
        }
        final Job job = new Job(nextId.get(), BuiltInRegistries.ITEM.getKey(item).toString(),
                Math.min(MOST_COPIES, copies), 0, computer.asLong(), wallet, WalletFiles.fileName(wallet));
        nextId.set(nextId.get() + 1);
        now.add(job);
        queue.set(List.copyOf(now));
        setChanged();
        return job;
    }

    /** The jobs waiting, the one printing first. */
    public List<Job> jobs() {
        return queue.get();
    }

    /** What it is doing now: {@link #IDLE}, {@link #PRINTING} and the others. */
    public int phase() {
        return phase.get();
    }

    public boolean paused() {
        return paused.get();
    }

    public void togglePaused() {
        if (level == null) {
            return;
        }
        final boolean pause = !paused.get();
        paused.set(pause);
        held.set(ticks.get());
        if (!pause) {
            // It takes up again where it stopped: the phase began as many ticks before now as it had run.
            since.set(level.getGameTime() - ticks.get());
        }
    }

    /** Drops the job printing now: an item already paid is lost with it, and the rest of it is never paid. */
    public void cancelCurrent() {
        final List<Job> now = new ArrayList<>(queue.get());
        if (now.isEmpty()) {
            return;
        }
        now.removeFirst();
        queue.set(List.copyOf(now));
        enter(IDLE);
        setChanged();
    }

    /**
     * How far into its phase it is, in ticks, on either side: on the players' side worked out from the tick the phase
     * began at and the level's clock, stopped while paused.
     */
    public int phaseTicks() {
        if (paused.get() || level == null) {
            return held.get();
        }
        return (int) Math.max(0L, level.getGameTime() - since.get());
    }

    /** How far the item printing now is, from 0 to 1; 1 once it is putting it out. */
    public double progress() {
        return switch (phase.get()) {
            case PRINTING -> Math.min(1.0, phaseTicks() / (double) model().itemTicks());
            case EJECTING, OUTPUT_FULL -> 1.0;
            default -> 0.0;
        };
    }

    /** The item of the job printing now, or none. */
    @Nullable
    public Item currentItem() {
        final List<Job> now = queue.get();
        return now.isEmpty() ? null : now.getFirst().itemOf();
    }

    /** What the wallet it waits on holds, in units, while it waits for TRC. */
    public long walletHolds() {
        return walletHolds.get();
    }

    /** How many items it printed, ever. */
    public int printed() {
        return printed.get();
    }

    /** The name of the computer it is linked to, or empty. */
    public String linkedName() {
        return linkedName.get();
    }

    public ItemStackHandler output() {
        return output;
    }

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
        linkedName.set("");
    }

    /** The linked computer's position, or null while unlinked. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    /* A tick of its work: the phase it is in runs on, and moves to the next when it is done. */
    private void tickPrint(final ServerLevel level) {
        updateLinkedName(level);
        if (paused.get()) {
            return;
        }
        final List<Job> now = queue.get();
        if (now.isEmpty()) {
            if (phase.get() != IDLE) {
                enter(IDLE);
            }
            return;
        }
        final Job job = now.getFirst();
        switch (phase.get()) {
            case PRINTING -> {
                if (count() >= model().itemTicks()) {
                    enter(EJECTING);
                }
            }
            case EJECTING, OUTPUT_FULL -> {
                if (phase.get() == EJECTING && count() < model().ejectTicks()) {
                    return;
                }
                if (!putOut(job)) {
                    if (phase.get() != OUTPUT_FULL) {
                        enter(OUTPUT_FULL);
                    }
                    return;
                }
                enter(IDLE);
            }
            default -> {
                if (--retryIn > 0) {
                    return;
                }
                retryIn = RETRY;
                start(level, job);
            }
        }
    }

    /* Counts a tick of the phase and answers how many it has run. */
    private int count() {
        ticks.set(ticks.get() + 1);
        return ticks.get();
    }

    /*
     * Starts the next item of {@code job}: paid from its wallet, read off the computer that sent the job, at once; or
     * it waits for the wallet's file, or for the price.
     */
    private void start(final ServerLevel level, final Job job) {
        final Item item = job.itemOf();
        final long price = item == null ? -1L : TeracoinSettings.priceOf(item);
        if (price <= 0L) {
            // An item the server took off its list since: the job cannot go on.
            cancelCurrent();
            return;
        }
        final Optional<WalletFiles.Wallet> wallet = walletOf(level, job);
        if (wallet.isEmpty()) {
            if (phase.get() != WAITING_WALLET) {
                enter(WAITING_WALLET);
            }
            return;
        }
        final TeracoinState state = TeracoinState.of(level.getServer());
        final long dayTime = level.getServer().overworld().getDayTime();
        if (!state.chain().pay(wallet.get().address(), wallet.get().key(), price, job.item(), level.getGameTime(),
                dayTime)) {
            walletHolds.set(state.chain().balance(wallet.get().address()));
            if (phase.get() != WAITING_TRC) {
                enter(WAITING_TRC);
            }
            return;
        }
        state.setDirty();
        enter(PRINTING);
    }

    /* The wallet of {@code job}, read off the computer that sent it, when that computer is loaded and has the file. */
    private static Optional<WalletFiles.Wallet> walletOf(final ServerLevel level, final Job job) {
        if (!(Loaded.blockEntity(level, BlockPos.of(job.computer())) instanceof IOsHost host)
                || host.services() == null) {
            return Optional.empty();
        }
        final FileService files = host.services().files();
        return files == null ? Optional.empty() : WalletFiles.read(files, job.wallet());
    }

    /* Puts the finished item in the output; the job goes on to its next copy, or off the queue. Whether it went in. */
    private boolean putOut(final Job job) {
        final Item item = job.itemOf();
        final ItemStack made = new ItemStack(item == null ? Items.AIR : item);
        if (!made.isEmpty() && !insertOne(made)) {
            return false;
        }
        printed.set(printed.get() + 1);
        final List<Job> now = new ArrayList<>(queue.get());
        final Job done = job.oneMore();
        if (done.done() >= done.copies()) {
            now.removeFirst();
        } else {
            now.set(0, done);
        }
        queue.set(List.copyOf(now));
        setChanged();
        return true;
    }

    /*
     * Puts {@code made} on the first place of the output that has room for it, past the rule that keeps everything
     * else out; whether one had room.
     */
    private boolean insertOne(final ItemStack made) {
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            final ItemStack held = output.getStackInSlot(slot);
            if (held.isEmpty()) {
                output.setStackInSlot(slot, made.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(held, made) && held.getCount() < held.getMaxStackSize()) {
                output.setStackInSlot(slot, held.copyWithCount(held.getCount() + 1));
                return true;
            }
        }
        return false;
    }

    /* Moves to {@code next}, its count from naught, begun now. */
    private void enter(final int next) {
        phase.set(next);
        ticks.set(0);
        held.set(0);
        since.set(level == null ? 0L : level.getGameTime());
        retryIn = 0;
    }

    /* The name of the computer it is linked to, for its window, kept only as it changes. */
    private void updateLinkedName(final ServerLevel level) {
        final BlockPos owner = link.ownerPos();
        final String name = owner != null && Loaded.blockEntity(level, owner) instanceof IOsHost host
                ? Printers.machineName(host) : "";
        if (!name.equals(linkedName.get())) {
            linkedName.set(name);
        }
    }

    /**
     * A job in the queue: {@code copies} of an item, {@code done} of them out, paid from the wallet at {@code wallet}
     * on the computer at {@code computer}, its file named {@code walletName} for the window.
     */
    public record Job(int id, String item, int copies, int done, long computer, String wallet, String walletName) {

        public static final Codec<Job> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(Job::id),
                Codec.STRING.fieldOf("item").forGetter(Job::item),
                Codec.INT.fieldOf("copies").forGetter(Job::copies),
                Codec.INT.fieldOf("done").forGetter(Job::done),
                Codec.LONG.fieldOf("computer").forGetter(Job::computer),
                Codec.STRING.fieldOf("wallet").forGetter(Job::wallet),
                Codec.STRING.fieldOf("walletName").forGetter(Job::walletName)
        ).apply(i, Job::new));

        /** The item it prints, or null for one no longer in the game. */
        @Nullable
        public Item itemOf() {
            final ResourceLocation id = ResourceLocation.tryParse(item);
            return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        }

        /** The same job with one more copy out. */
        public Job oneMore() {
            return new Job(id, item, copies, done + 1, computer, wallet, walletName);
        }
    }
}
