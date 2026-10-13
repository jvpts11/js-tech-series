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
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.block.PrinterBlock;
import dev.jstech.computers.client.audio.MachineSoundSources;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrintedDocuments;
import dev.jstech.computers.printer.PrinterModel;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A printer: a peripheral of a computer, linked over the peripheral cable like a drive, that prints whatever the
 * computer's programs send it. Each document waits in the queue in the order it came; the printer takes a sheet of
 * paper from its tray for every page, takes as long over a page as its sound lasts, and puts the finished document in
 * its output as a Printed Paper, one for each copy asked for. With no paper in the tray, or no room in the output, the
 * queue waits; paused, it waits too.
 *
 * <p>The players who see it are sent what it prints, how far it is, its queue as rows, and whether it is printing,
 * waiting or paused; the documents themselves stay on the server until they come out.
 */
public class PrinterBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint, IAudible, GeoBlockEntity {

    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    /* The tray takes plain paper only, a sheet a page. */
    private final FieldItemHandler paper = fields().items("Paper", 1).save().toClient().dropsWhenBroken()
            .accepts((index, stack) -> stack.is(Items.PAPER));
    /* The output only gives: the printer puts its sheets there, a player takes them. */
    private final FieldItemHandler output = fields().items("Output", OUTPUT_SLOTS).save().toClient()
            .dropsWhenBroken().slotLimit(1).accepts((index, stack) -> false);
    private final ValueField<List<PrintJob>> queue =
            fields().value("Queue", PrintJob.CODEC.listOf(), List.of()).save();
    private final ValueField<List<QueueRow>> rows =
            fields().value("QueueRows", QueueRow.CODEC.listOf(), List.of()).toClient();
    /** How far into the current page the printer is, in ticks. */
    private final IntField pageTicks = fields().integer("PageTicks", 0).save();
    /** How many sheets of the current job are out, across all its copies. */
    private final IntField sheetsDone = fields().integer("SheetsDone", 0).save().toClient();
    private final BoolField paused = fields().flag("Paused", false).save().toClient();
    private final BoolField printing = fields().flag("Printing", false).toClient();
    private final ValueField<Wait> waiting = fields().value("Waiting", Wait.CODEC, Wait.NONE).toClient();
    /** The number the next document gets, which its request id in lp and lpstat is made of. */
    private final IntField nextId = fields().integer("NextId", 1).save();
    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);

    /** The most documents waiting in a printer at once. */
    public static final int QUEUE_MAX = 16;
    /** How many printed documents the output holds before the printer waits for one to be taken. */
    public static final int OUTPUT_SLOTS = 3;

    public PrinterBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.PRINTER_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final PrinterBlockEntity printer) {
        if (level instanceof ServerLevel server) {
            printer.link.tick(server, pos);
            printer.tickPrint();
        }
    }

    /** The printer this block is, by its era. */
    public PrinterModel model() {
        return getBlockState().getBlock() instanceof PrinterBlock block ? block.model()
                : PrinterModel.PAKARD_LASERJOT_1102;
    }

    /**
     * Puts a document in the queue and answers the job it became, or null when the queue is full. A job can be queued
     * with the tray empty: it waits for paper.
     *
     * @param user who sent it, as {@code lpstat} names a request's owner
     */
    @Nullable
    public PrintJob submit(final PrintedDocument document, final int copies, final String user, final long time) {
        if (queue.get().size() >= QUEUE_MAX) {
            return null;
        }
        final PrintJob job = new PrintJob(nextId.get(), document.printedBy(model()), Math.max(1, Math.min(99, copies)),
                user, time);
        nextId.set(nextId.get() + 1);
        final List<PrintJob> now = new ArrayList<>(queue.get());
        now.add(job);
        setQueue(now);
        return job;
    }

    /** The jobs waiting, the one printing first. */
    public List<PrintJob> jobs() {
        return queue.get();
    }

    /** The queue as the players see it: on the client the rows the server sent, on the server the same. */
    public List<QueueRow> queueRows() {
        return rows.get();
    }

    /** Drops the job printing now; the sheets it already used are spent. */
    public void cancelCurrent() {
        final List<PrintJob> now = new ArrayList<>(queue.get());
        if (now.isEmpty()) {
            return;
        }
        now.removeFirst();
        pageTicks.set(0);
        sheetsDone.set(0);
        setQueue(now);
    }

    /** Drops the job with that id, wherever it is in the queue; whether there was one. */
    public boolean cancel(final int id) {
        final List<PrintJob> now = new ArrayList<>(queue.get());
        for (int i = 0; i < now.size(); i++) {
            if (now.get(i).id() == id) {
                if (i == 0) {
                    cancelCurrent();
                } else {
                    now.remove(i);
                    setQueue(now);
                }
                return true;
            }
        }
        return false;
    }

    public void togglePaused() {
        paused.set(!paused.get());
    }

    public boolean paused() {
        return paused.get();
    }

    /** Whether a page is coming out now. */
    public boolean printing() {
        return printing.get();
    }

    /** Why the queue is waiting, {@link Wait#NONE} while it is not. */
    public Wait waiting() {
        return waiting.get();
    }

    /** How many sheets of the current job are out. */
    public int sheetsDone() {
        return sheetsDone.get();
    }

    /** How many sheets of paper are in the tray. */
    public int paperCount() {
        return paper.getStackInSlot(0).getCount();
    }

    public ItemStackHandler paper() {
        return paper;
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
    }

    /** The linked computer's position, or null while unlinked. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    /* The rows the players see are not saved; a printer coming back from its save makes them again from its queue. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            MachineSoundSources.track(this);
        } else {
            setQueue(queue.get());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) {
            MachineSoundSources.untrack(this);
        }
    }

    @Override
    public double audioX() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double audioY() {
        return worldPosition.getY() + 0.5;
    }

    @Override
    public double audioZ() {
        return worldPosition.getZ() + 0.5;
    }

    /** The page coming out, while one does. */
    @Override
    public List<LoopRequest> loops() {
        return printing.get() ? List.of(LoopRequest.of(sound(model()))) : List.of();
    }

    /* The page sliding out of the printer, looping while it prints; the lamps are shown and hidden by the renderer. */
    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "printer", 0, state -> printing.get()
                ? state.setAndContinue(RawAnimation.begin().thenLoop(clip(model()))) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    /** The sound a printer makes while a page comes out. */
    public static SoundKey sound(final PrinterModel model) {
        return switch (model) {
            case EPSILON_FX_80 -> ComputingSounds.PRINTER_EPSILON_FX_80;
            case PAKARD_DESKJOT_940 -> ComputingSounds.PRINTER_PAKARD_DESKJOT_940;
            case PAKARD_FOTOSMART_C4280 -> ComputingSounds.PRINTER_PAKARD_FOTOSMART_C4280;
            case PAKARD_LASERJOT_1102 -> ComputingSounds.PRINTER_PAKARD_LASERJOT_1102;
            case EPSILON_ECOTONK_ET_2720 -> ComputingSounds.PRINTER_EPSILON_ECOTONK_ET_2720;
        };
    }

    /** The animation of a printer's page coming out. */
    public static String clip(final PrinterModel model) {
        return "animation.printer.print_" + model.era().serializedName();
    }

    /*
     * A page at a time: with a job, paper in the tray and, before a copy starts, a free place in the output, the
     * printer counts the page's ticks, takes a sheet when the page is out, and hands each copy over when its last page
     * is.
     */
    private void tickPrint() {
        final List<PrintJob> now = queue.get();
        if (now.isEmpty() || paused.get()) {
            printing.set(false);
            waiting.set(now.isEmpty() ? Wait.NONE : Wait.PAUSED);
            return;
        }
        final PrintJob job = now.getFirst();
        final int sheets = job.document().sheets();
        if (paper.getStackInSlot(0).isEmpty()) {
            stall(Wait.NO_PAPER);
            return;
        }
        if (sheetsDone.get() % sheets == 0 && pageTicks.get() == 0 && freeOutput() < 0) {
            stall(Wait.OUTPUT_FULL);
            return;
        }
        waiting.set(Wait.NONE);
        printing.set(true);
        pageTicks.set(pageTicks.get() + 1);
        if (pageTicks.get() < model().pageTicks()) {
            return;
        }
        pageTicks.set(0);
        paper.extractItem(0, 1, false);
        sheetsDone.set(sheetsDone.get() + 1);
        if (sheetsDone.get() % sheets == 0) {
            deliver(job);
            if (sheetsDone.get() >= sheets * job.copies()) {
                sheetsDone.set(0);
                final List<PrintJob> rest = new ArrayList<>(queue.get());
                rest.removeFirst();
                setQueue(rest);
                // The last page is out: the printer falls quiet now, not a tick later.
                printing.set(!rest.isEmpty());
            }
        }
    }

    private void stall(final Wait why) {
        printing.set(false);
        waiting.set(why);
    }

    /* One copy of the job, as the sheet that carries it, into the first free place in the output. */
    private void deliver(final PrintJob job) {
        final int slot = freeOutput();
        if (slot < 0) {
            return;
        }
        final ItemStack sheet = new ItemStack(ComputingModule.PRINTED_PAPER.get());
        sheet.set(ComputingComponents.PRINTED_DOCUMENT.get(), job.document());
        output.setStackInSlot(slot, sheet);
    }

    private int freeOutput() {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (output.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void setQueue(final List<PrintJob> jobs) {
        queue.set(List.copyOf(jobs));
        final List<QueueRow> shown = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            final PrintJob job = jobs.get(i);
            shown.add(new QueueRow(job.id(), job.document().title(), job.document().from(),
                    job.document().sheets() * job.copies(), i == 0));
        }
        rows.set(List.copyOf(shown));
    }

    /** Why a printer with jobs is not printing. */
    public enum Wait implements IStableId {
        NONE(0),
        PAUSED(1),
        NO_PAPER(2),
        OUTPUT_FULL(3);

        private final int id;

        private static final StableIds<Wait> IDS = StableIds.of(Wait.class);
        /** A reason sent as the byte of its id; an id no reason declares reads as {@link #NONE}. */
        private static final Codec<Wait> CODEC = Codec.BYTE.xmap(b -> IDS.byId(b, NONE), w -> (byte) w.id());

        Wait(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }
    }

    /**
     * A document waiting in a printer.
     *
     * @param id        its number at this printer
     * @param document  what it prints
     * @param copies    how many copies
     * @param user      who sent it
     * @param submitted the world's time when it was sent
     */
    public record PrintJob(int id, PrintedDocument document, int copies, String user, long submitted) {

        public static final Codec<PrintJob> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(PrintJob::id),
                PrintedDocuments.CODEC.fieldOf("document").forGetter(PrintJob::document),
                Codec.INT.optionalFieldOf("copies", 1).forGetter(PrintJob::copies),
                Codec.STRING.optionalFieldOf("user", "").forGetter(PrintJob::user),
                Codec.LONG.optionalFieldOf("submitted", 0L).forGetter(PrintJob::submitted)
        ).apply(i, PrintJob::new));
    }

    /**
     * A row of the queue as the printer's window shows it.
     *
     * @param id       the job's number
     * @param title    the document's title
     * @param from     the machine it came from
     * @param sheets   how many sheets it takes, its copies counted
     * @param printing whether it is the one printing now
     */
    public record QueueRow(int id, String title, String from, int sheets, boolean printing) {

        public static final Codec<QueueRow> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(QueueRow::id),
                Codec.STRING.fieldOf("title").forGetter(QueueRow::title),
                Codec.STRING.fieldOf("from").forGetter(QueueRow::from),
                Codec.INT.fieldOf("sheets").forGetter(QueueRow::sheets),
                Codec.BOOL.fieldOf("printing").forGetter(QueueRow::printing)
        ).apply(i, QueueRow::new));
    }
}
