/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.multiblock.ShapeMatch;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The Large Scale Printer at work (computers/docs/design/peripherals.md): the book factory of the Legacy, the only
 * printer of books. Its feeder takes paper by hand, a bus or a hopper; a computer on the cable at its peripheral port
 * sends it books, as to any printer in its Print dialog; each copy takes 3 sheets and 2 seconds, and goes straight
 * into the computer's network or into the chest beside its outlet, as its switch says, staying in its outlet when
 * neither has room. A server may turn the printing of enchanted books off: it then refuses them, saying why.
 */
@TextHolder
public final class LargeScalePrinterBlockEntity extends ChainMachineBlockEntity {

    private final ValueField<List<BookJob>> queue;
    private final ValueField<List<Row>> rows;
    private final IntField done;
    private final IntField printed;
    private final IntField nextId;
    private final BoolField toNetwork;
    private final BoolField paused;
    private final DerivedInt sheets;
    private final DerivedInt shown;
    /* Whether the server lets it print enchanted books, for its window: 1 when it does. */
    private final DerivedInt enchanted;

    /** Its feeder's places, which a bus or a hopper fills through the inlet: thirty stacks of paper. */
    public static final int INPUTS = 30;
    /** The places a book waits in when neither the network nor the chest takes it. */
    public static final int OUTPUTS = 3;
    /** The sheets one book takes. */
    public static final int SHEETS_A_BOOK = 3;
    /** The most jobs waiting at once. */
    public static final int QUEUE_MAX = 16;
    /* How far through a book it is, in thousandths, as the players' games draw it. */
    private static final int WHOLE = 1_000;
    private static final int TICKS_A_SECOND = 20;
    private static final TextKey NO_PAPER = TextKey.of("jsc.large_scale_printer.no_paper",
            "Its feeder needs 3 sheets of paper for a book.");
    private static final TextKey ENCHANTED_OFF = TextKey.of("jsc.large_scale_printer.enchanted_off",
            "This server does not let enchanted books be printed.");

    public LargeScalePrinterBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
        this.queue = fields().value("Queue", BookJob.CODEC.listOf(), List.<BookJob>of()).save();
        this.rows = fields().value("Rows", Row.CODEC.listOf(), List.<Row>of()).toClient();
        this.done = fields().integer("Done", 0).save().toClient();
        this.printed = fields().integer("Printed", 0).save().toClient();
        this.nextId = fields().integer("NextId", 1).save();
        this.toNetwork = fields().flag("ToNetwork", true).save().toClient();
        this.paused = fields().flag("Paused", false).save().toClient();
        this.sheets = fields().derived("Sheets", this::paperCount).toClient();
        this.shown = fields().derived("Shown", () -> getMaxProgress() <= 0 ? 0
                : (int) ((long) getProgress() * WHOLE / getMaxProgress())).toClient();
        this.enchanted = fields().derived("Enchanted", () -> ComputersServerConfig.printEnchantedBooks() ? 1 : 0)
                .toClient();
    }

    /** Whether the server lets it print enchanted books, as its window says it. */
    public boolean enchantedAllowed() {
        return enchanted.getAsInt() == 1;
    }

    /**
     * Puts {@code job} in the queue, numbered; the job as queued, or empty when the queue is full or the server does
     * not let it print an enchanted book.
     */
    public Optional<BookJob> submit(final BookJob job) {
        if (queue.get().size() >= QUEUE_MAX || refused(job)) {
            return Optional.empty();
        }
        final BookJob numbered = job.numbered(nextId.get());
        nextId.set(nextId.get() + 1);
        final List<BookJob> now = new ArrayList<>(queue.get());
        now.add(numbered);
        setQueue(now);
        return Optional.of(numbered);
    }

    /** Whether the server's setting keeps {@code job} from printing: an enchanted book where they are off. */
    public static boolean refused(final BookJob job) {
        return job.kind() == BookJob.Kind.ENCHANTED && !ComputersServerConfig.printEnchantedBooks();
    }

    /** Drops the job printing now; the paper it already used is spent. */
    public void cancelCurrent() {
        final List<BookJob> now = new ArrayList<>(queue.get());
        if (!now.isEmpty()) {
            now.removeFirst();
            done.set(0);
            setQueue(now);
        }
    }

    public void togglePaused() {
        paused.set(!paused.get());
    }

    /** Sends the books into the network, or into the chest beside the outlet. */
    public void toggleDestination() {
        toNetwork.set(!toNetwork.get());
    }

    public boolean paused() {
        return paused.get();
    }

    /** Whether its books go into the network rather than the chest beside it. */
    public boolean toNetwork() {
        return toNetwork.get();
    }

    /** The jobs waiting, the one printing first. */
    public List<BookJob> jobs() {
        return queue.get();
    }

    /** The queue as the players see it. */
    public List<Row> rows() {
        return rows.get();
    }

    /** How many copies of the job printing now are out. */
    public int done() {
        return done.get();
    }

    /** How many books it has printed, all told. */
    public int printed() {
        return printed.get();
    }

    /** How many sheets its feeder holds, as the players see it. */
    public int sheets() {
        return sheets.getAsInt();
    }

    /** How far through the book printing now it is, from 0 to 1. */
    public float progress() {
        return shown.getAsInt() / (float) WHOLE;
    }

    /** Where its peripheral port stands while it is formed, or null. */
    @Nullable
    public BlockPos port(final ServerLevel level) {
        final Optional<ShapeMatch.Formed> formed = formed() ? structure().structure() : Optional.empty();
        if (formed.isEmpty()) {
            return null;
        }
        for (final long at : formed.get().parts()) {
            if (level.getBlockState(BlockPos.of(at)).is(ChainMachineBlocks.PERIPHERAL_PORT.get())) {
                return BlockPos.of(at);
            }
        }
        return null;
    }

    /* The rows the players see are not saved; a printer coming back from its save makes them again from its queue. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            setQueue(queue.get());
            LargeScalePrinters.loaded(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            LargeScalePrinters.gone(this);
        }
    }

    /** The paper in its feeder, counted on the server. */
    public int paperCount() {
        int count = 0;
        for (int slot = 0; slot < layout().firstOutput(); slot++) {
            final ItemStack stack = getInventory().getStackInSlot(slot);
            if (stack.is(Items.PAPER)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /* The book the first job prints, from 3 sheets of the feeder, in its 2 seconds; nothing paused or without paper. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        final List<BookJob> now = queue.get();
        if (paused.get() || now.isEmpty()) {
            return Optional.empty();
        }
        final BookJob job = now.getFirst();
        if (refused(job)) {
            note(ENCHANTED_OFF.text());
            cancelCurrent();
            return Optional.empty();
        }
        final List<Take> takes = paper(input.items());
        if (takes.isEmpty()) {
            note(NO_PAPER.text());
            return Optional.empty();
        }
        final ItemStack book = job.book(level.registryAccess());
        if (book.isEmpty()) {
            cancelCurrent();
            return Optional.empty();
        }
        return run(level, input, new Processing(takes, List.of(), List.of(book), List.of(),
                machine().seconds() * TICKS_A_SECOND, 0), null);
    }

    /* A copy came out: the job moves on, and once its last copy is out the next job starts. */
    @Override
    protected void madeThings(final int count, final long gameTime) {
        super.madeThings(count, gameTime);
        printed.set(printed.get() + count);
        JscEvents.awardOperator(this, JscEvents.BOOK_PRINTED);
        final List<BookJob> now = queue.get();
        if (now.isEmpty()) {
            return;
        }
        done.set(done.get() + count);
        if (done.get() >= now.getFirst().copies()) {
            final List<BookJob> rest = new ArrayList<>(now);
            rest.removeFirst();
            done.set(0);
            setQueue(rest);
        } else {
            setQueue(now);
        }
    }

    /* After each tick's work, the books waiting in the outlet go where the switch sends them, as far as they fit. */
    @Override
    protected void settled(final ServerLevel level, final MachineStatus status) {
        super.settled(level, status);
        for (int slot = layout().firstOutput(); slot < layout().firstOutput() + OUTPUTS; slot++) {
            final ItemStack book = getInventory().getStackInSlot(slot);
            if (!book.isEmpty()) {
                final int put = toNetwork.get() ? intoNetwork(level, book) : intoChest(level, book);
                if (put > 0) {
                    getInventory().extractItem(slot, put, false);
                }
            }
        }
    }

    /* Three sheets from the feeder's places, the first places first; none when it holds fewer. */
    private static List<Take> paper(final List<ItemStack> items) {
        final List<Take> takes = new ArrayList<>();
        int wanted = SHEETS_A_BOOK;
        for (int slot = 0; slot < items.size() && wanted > 0; slot++) {
            final ItemStack stack = items.get(slot);
            if (stack.is(Items.PAPER)) {
                final int taken = Math.min(wanted, stack.getCount());
                takes.add(new Take(slot, taken));
                wanted -= taken;
            }
        }
        return wanted > 0 ? List.of() : takes;
    }

    /* How many of {@code book} the network of the computer at its peripheral port took. */
    private int intoNetwork(final ServerLevel level, final ItemStack book) {
        final AbstractComputerBlockEntity computer = computer(level);
        if (computer == null || computer.networkUuid() == null) {
            return 0;
        }
        return (int) NetworkStorage.of(level, computer.networkUuid()).insert(StorageKey.of(book), book.getCount());
    }

    /* How many of {@code book} the container beside its outlet took. */
    private int intoChest(final ServerLevel level, final ItemStack book) {
        final Optional<ShapeMatch.Formed> formed = formed() ? structure().structure() : Optional.empty();
        if (formed.isEmpty()) {
            return 0;
        }
        final Set<Long> parts = Set.copyOf(formed.get().parts());
        for (final long at : formed.get().parts()) {
            final BlockPos outlet = BlockPos.of(at);
            if (!level.getBlockState(outlet).is(ChainMachineBlocks.ITEM_OUTLET.get())) {
                continue;
            }
            for (final Direction side : Direction.values()) {
                final BlockPos beside = outlet.relative(side);
                if (parts.contains(beside.asLong())) {
                    continue;
                }
                final IItemHandler chest = level.getCapability(Capabilities.ItemHandler.BLOCK, beside,
                        side.getOpposite());
                // A container that takes none, full or refusing books, leaves the others beside the outlets to try.
                if (chest != null) {
                    final ItemStack left = ItemHandlerHelper.insertItemStacked(chest, book.copy(), false);
                    final int put = book.getCount() - left.getCount();
                    if (put > 0) {
                        return put;
                    }
                }
            }
        }
        return 0;
    }

    /* The computer its peripheral port is linked to, or null. */
    @Nullable
    private AbstractComputerBlockEntity computer(final ServerLevel level) {
        final BlockPos port = port(level);
        if (port == null) {
            return null;
        }
        final OptionalLong owner = PeripheralLinks.discoverOwner(level, port.asLong());
        return owner.isPresent() && level.getBlockEntity(BlockPos.of(owner.getAsLong()))
                instanceof AbstractComputerBlockEntity computer ? computer : null;
    }

    private void setQueue(final List<BookJob> jobs) {
        queue.set(List.copyOf(jobs));
        final List<Row> shownRows = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            final BookJob job = jobs.get(i);
            shownRows.add(new Row(job.title(), job.kind(), job.copies(), job.from(), job.pages().size(), i == 0));
        }
        rows.set(List.copyOf(shownRows));
    }

    /**
     * A row of the queue as the printer's window shows it.
     *
     * @param title    the book's title
     * @param kind     what kind of book
     * @param copies   how many copies
     * @param from     the machine it came from
     * @param pages    how many pages a copy has
     * @param printing whether it is the one printing now
     */
    public record Row(String title, BookJob.Kind kind, int copies, String from, int pages, boolean printing) {

        public static final Codec<Row> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("title").forGetter(Row::title),
                StableCodecs.byName(BookJob.Kind.class).fieldOf("kind").forGetter(Row::kind),
                Codec.INT.fieldOf("copies").forGetter(Row::copies),
                Codec.STRING.fieldOf("from").forGetter(Row::from),
                Codec.INT.fieldOf("pages").forGetter(Row::pages),
                Codec.BOOL.fieldOf("printing").forGetter(Row::printing)
        ).apply(i, Row::new));
    }
}
