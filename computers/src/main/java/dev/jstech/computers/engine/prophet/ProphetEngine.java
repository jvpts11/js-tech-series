/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.prophet;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.crafting.PendingCraftOperation;
import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.engine.EngineDef;
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IIqlView;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.ShortId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Prophet YourIQL, the state-oriented engine: told what state the network is to keep, it works out the Operations to
 * get there and stay there.
 *
 * <p>Its dialect is the network's language with the words for that: {@code KEEP item >= n} or
 * {@code KEEP item BETWEEN a AND b} holds an item at a level, counting what is already on its way, and
 * {@code WATCH item < n DO statement} runs a statement once when a level crosses a line ({@code CRAFT item TO n}
 * makes up to a level). {@code FORGET} lets either go and {@code SHOW STATES} lists them. A craft it is asked for it
 * plans as the network does.
 *
 * <p>On every tick of its Mainframe it looks again only when its interval says so, and then does only what changed
 * (see {@link ProphetStates}): on a network standing still a look is one comparison. The states and watches live in
 * the Mainframe's save, under the engine's package.
 */
public final class ProphetEngine implements INetworkEngine {

    /** Each Mainframe's states while it is loaded, read from its save the first time they are asked for. */
    private final Map<MainframeBlockEntity, ProphetMind> minds = new WeakHashMap<>();

    /** How often the states that could not hold try again, in ticks. */
    private static final long RETRY_TICKS = 600L;
    /** Who the Operations it starts are for, as the Operations log names them. */
    private static final String ASKER = "Prophet YourIQL";
    private static final List<String> COLUMNS = List.of("state", "status", "held", "coming", "last");
    private static final String MINECRAFT = "minecraft:";
    /** What separates the kind of a stored key from the id of what it is. */
    private static final char KIND_MARK = '|';
    private static final int ACTION_ROWS = 64;

    @Override
    public EngineDef def() {
        return NetworkEngines.PROPHET_YOURIQL;
    }

    @Override
    public IqlEngine.Outcome query(final MainframeBlockEntity core, final IIqlView caller, final String statement,
                                   final int rowLimit) {
        final ProphetStatement parsed = ProphetStatement.parse(statement);
        if (parsed == null) {
            return new IqlEngine(core, caller, rowLimit, false).run(statement);
        }
        final ProphetMind mind = mind(core);
        final ProphetStates states = mind.states();
        return switch (parsed) {
            case ProphetStatement.Keep keep -> {
                final StorageKey key = StorageKey.byName(keep.item());
                if (key == null) {
                    yield fail(ProphetTexts.UNKNOWN_ITEM.with(keep.item()));
                }
                states.keep(key.id(), keep.lower(), keep.upper());
                save(core, mind);
                final Text name = GameText.of(key.displayName());
                yield ok(keep.upper() == ProphetStatement.UNBOUNDED ? ProphetTexts.KEPT.with(name, keep.lower())
                        : ProphetTexts.KEPT_BAND.with(name, keep.lower(), keep.upper()));
            }
            case ProphetStatement.Watch watch -> {
                final StorageKey key = StorageKey.byName(watch.item());
                if (key == null) {
                    yield fail(ProphetTexts.UNKNOWN_ITEM.with(watch.item()));
                }
                final ProphetStates.WatchState set = states.watch(key.id(), watch.comparison(), watch.threshold(),
                        watch.action());
                save(core, mind);
                yield ok(ProphetTexts.WATCHING.with(set.number(), Text.literal(written(set))));
            }
            case ProphetStatement.Forget forget -> {
                // A name as a player writes it, or a key as the console holds it.
                final StorageKey named = StorageKey.byName(forget.item());
                final StorageKey key = named != null ? named : StorageKey.byId(forget.item());
                final boolean gone = key != null && states.forget(key.id());
                if (gone) {
                    save(core, mind);
                }
                yield gone ? ok(ProphetTexts.FORGOTTEN.with(GameText.of(key.displayName())))
                        : fail(ProphetTexts.NOT_KEPT.with(forget.item()));
            }
            case ProphetStatement.ForgetWatch forget -> {
                final boolean gone = states.forgetWatch(forget.number());
                if (gone) {
                    save(core, mind);
                }
                yield gone ? ok(ProphetTexts.WATCH_FORGOTTEN.with(forget.number()))
                        : fail(ProphetTexts.NO_WATCH.with(forget.number()));
            }
            case ProphetStatement.ShowStates show -> listed(mind);
            case ProphetStatement.Malformed bad -> fail(switch (bad.what()) {
                case ProphetStatement.BAD_WATCH -> ProphetTexts.USAGE_WATCH.text();
                case ProphetStatement.BAD_FORGET -> ProphetTexts.USAGE_FORGET.text();
                default -> ProphetTexts.USAGE_KEEP.text();
            });
        };
    }

    @Override
    public void tick(final MainframeBlockEntity core, final ServerLevel level) {
        final ProphetMind mind = mind(core);
        final long now = level.getGameTime();
        if (!mind.due(now) || !mind.reacting()) {
            return;
        }
        if (mind.retryDue(now, RETRY_TICKS)) {
            mind.states().retry();
        }
        final List<ProphetStates.Reaction> reactions = mind.states().evaluate(core.networkIndex().version(), now,
                item -> held(core, mind, item));
        for (final ProphetStates.Reaction reaction : reactions) {
            if (reaction.kind() == ProphetStates.Reaction.CRAFT) {
                keepUp(core, mind, reaction.item(), reaction.amount(), now);
            } else {
                fire(core, level, mind, reaction, now);
            }
        }
        if (!reactions.isEmpty()) {
            save(core, mind);
        }
    }

    /** The states kept on {@code core}, as the console lists them. */
    public List<StateRow> states(final MainframeBlockEntity core) {
        final ProphetMind mind = mind(core);
        final List<StateRow> out = new ArrayList<>();
        for (final ProphetStates.KeepState keep : mind.states().keeps()) {
            final StorageKey key = mind.key(keep.item());
            final List<Text> operations = new ArrayList<>();
            for (final INetworkOperation operation : mind.operations(keep.item())) {
                operations.add(Text.literal("#" + ShortId.of(operation.operationId().toString()) + " "
                        + OperationRecord.statusName(operation.liveRecord().status())));
            }
            final Text last = mind.lastReaction(keep.item());
            out.add(new StateRow(keep.item(), key == null ? Text.literal(keep.item()) : GameText.of(key.displayName()),
                    written(keep), ProphetTexts.status(keep.status()), tone(keep.status()), keep.held(),
                    keep.inFlight(), keep.lower(), keep.upper(), last == null ? Text.EMPTY : last, keep.samples(),
                    operations));
        }
        return out;
    }

    /** The watches set on {@code core}, as the console lists them. */
    public List<WatchRow> watches(final MainframeBlockEntity core) {
        final List<WatchRow> out = new ArrayList<>();
        for (final ProphetStates.WatchState watch : mind(core).states().watches()) {
            out.add(new WatchRow(watch.number(), written(watch), watch.status() == ProphetStates.Status.FIRED,
                    watch.armed(), watch.fired()));
        }
        return out;
    }

    /** What it did lately on {@code core}, the newest first. */
    public List<ReactionRow> reactions(final MainframeBlockEntity core) {
        final List<ReactionRow> out = new ArrayList<>();
        for (final ProphetMind.Reacted reacted : mind(core).reactions()) {
            out.add(new ReactionRow(reacted.at(), reacted.what()));
        }
        return out;
    }

    /** How it looks and reacts on {@code core}. */
    public Settings settings(final MainframeBlockEntity core) {
        final ProphetMind mind = mind(core);
        return new Settings(mind.interval(), mind.maxBatch(), mind.reacting());
    }

    /** Sets how it looks and reacts on {@code core}; values not on the lists fall back to the defaults. */
    public void configure(final MainframeBlockEntity core, final Settings settings) {
        final ProphetMind mind = mind(core);
        mind.interval(settings.interval());
        mind.maxBatch(settings.maxBatch());
        mind.reacting(settings.reacting());
        mind.states().touch();
        save(core, mind);
    }

    /* Asks for what a state lacks, at most a batch at a time, and counts it as on its way until it settles. */
    private void keepUp(final MainframeBlockEntity core, final ProphetMind mind, final String item,
                        final long lacking, final long now) {
        final StorageKey key = mind.key(item);
        final long amount = Math.min(lacking, mind.maxBatch());
        final AtomicReference<INetworkOperation> started = new AtomicReference<>();
        final INetworkOperation operation = key == null || amount <= 0 ? null
                : core.networkOperations().craft(CraftRequest.of(key, amount, true, ASKER,
                        () -> settled(core, mind, item, amount, started.get())));
        started.set(operation);
        if (operation == null) {
            mind.states().cannotHold(item);
            mind.reacted(now, item, ProphetTexts.NOTHING_MAKES.with(key == null ? Text.literal(item)
                    : GameText.of(key.displayName())));
            return;
        }
        mind.states().started(item, amount);
        mind.started(item, operation);
        mind.reacted(now, item, ProphetTexts.ASKED_FOR.with(amount, GameText.of(key.displayName()),
                ShortId.of(operation.operationId().toString())));
    }

    /*
     * Work set going for a state settled. What it made arrives; work that made nothing at all (no way to make it
     * from what the network holds) leaves the state unable to hold until something changes, rather than asking again
     * each time the network moves.
     */
    private static void settled(final MainframeBlockEntity core, final ProphetMind mind, final String item,
                                final long amount, @Nullable final INetworkOperation operation) {
        mind.states().settled(item, amount, core.networkIndex().version());
        final boolean madeNothing = operation instanceof PendingCraftOperation pending
                ? pending.delivered() == null || pending.delivered().delivered() <= 0
                : operation instanceof NetworkCraftOperation craft && craft.delivered() <= 0;
        if (madeNothing) {
            mind.states().cannotHold(item);
        }
    }

    /* Runs what a watch says: CRAFT item TO n makes up to that level; anything else is a statement. */
    private void fire(final MainframeBlockEntity core, final ServerLevel level, final ProphetMind mind,
                      final ProphetStates.Reaction reaction, final long now) {
        final String[] words = reaction.action().strip().split("\\s+");
        final int number = (int) reaction.amount();
        if (words.length == 4 && words[0].equalsIgnoreCase("CRAFT") && words[2].equalsIgnoreCase("TO")) {
            final StorageKey key = StorageKey.byName(words[1]);
            final long target = digits(words[3]);
            final long lacking = key == null || target < 0 ? 0L
                    : target - core.networkIndex().grossAvailable(key, null);
            if (lacking <= 0) {
                mind.reacted(now, "", ProphetTexts.NOTHING_TO_DO.with(number));
                return;
            }
            final long amount = Math.min(lacking, mind.maxBatch());
            final INetworkOperation operation = core.networkOperations().craft(CraftRequest.of(key, amount, true,
                    ASKER, null));
            mind.reacted(now, "", operation == null
                    ? ProphetTexts.WATCH_REFUSED.with(number, ProphetTexts.NOTHING_MAKES.with(
                            GameText.of(key.displayName())))
                    : ProphetTexts.WATCH_FIRED.with(number, ProphetTexts.ASKED_FOR.with(amount,
                            GameText.of(key.displayName()), ShortId.of(operation.operationId().toString()))));
            return;
        }
        final IqlEngine.Outcome outcome = query(core, IqlEngine.viewOf(new ServerCliComputer(core, level)),
                reaction.action(), ACTION_ROWS);
        mind.reacted(now, "", (outcome.ok() ? ProphetTexts.WATCH_FIRED : ProphetTexts.WATCH_REFUSED)
                .with(number, outcome.said()));
    }

    private IqlEngine.Outcome listed(final ProphetMind mind) {
        final List<List<Text>> rows = new ArrayList<>();
        final List<ICliComputer.StoredItem> lines = new ArrayList<>();
        for (final ProphetStates.KeepState keep : mind.states().keeps()) {
            final Text last = mind.lastReaction(keep.item());
            rows.add(List.of(Text.literal(written(keep)), ProphetTexts.status(keep.status()),
                    Text.literal(Long.toString(keep.held())), Text.literal(Long.toString(keep.inFlight())),
                    last == null ? Text.EMPTY : last));
            lines.add(new ICliComputer.StoredItem(Text.literal(written(keep)), keep.held(),
                    ProphetTexts.status(keep.status())));
        }
        for (final ProphetStates.WatchState watch : mind.states().watches()) {
            rows.add(List.of(Text.literal(written(watch)), ProphetTexts.status(watch.status()), Text.EMPTY,
                    Text.EMPTY, Text.EMPTY));
            lines.add(new ICliComputer.StoredItem(Text.literal(written(watch)), watch.fired(),
                    ProphetTexts.status(watch.status())));
        }
        return new IqlEngine.Outcome(true, ProphetTexts.LISTED.with(mind.states().keeps().size(),
                mind.states().watches().size()), lines, new IqlTable(COLUMNS, rows));
    }

    /* How a state's chip is coloured: held, at work, in trouble, or waiting. */
    private static byte tone(final ProphetStates.Status status) {
        return switch (status) {
            case HOLDING -> StateRow.TONE_GOOD;
            case WORKING, FIRED -> StateRow.TONE_WORKING;
            case CANNOT_HOLD, OVER -> StateRow.TONE_BAD;
            case NEW, ARMED -> StateRow.TONE_WAITING;
        };
    }

    private long held(final MainframeBlockEntity core, final ProphetMind mind, final String item) {
        final StorageKey key = mind.key(item);
        return key == null ? 0L : core.networkIndex().grossAvailable(key, null);
    }

    /** How a state is written, the item by its short name. */
    static String written(final ProphetStates.KeepState keep) {
        return keep.upper() == ProphetStatement.UNBOUNDED
                ? "KEEP " + shortName(keep.item()) + " >= " + keep.lower()
                : "KEEP " + shortName(keep.item()) + " BETWEEN " + keep.lower() + " AND " + keep.upper();
    }

    /** How a watch is written, the item by its short name. */
    static String written(final ProphetStates.WatchState watch) {
        return "WATCH " + shortName(watch.item()) + " " + watch.comparison().symbol() + " " + watch.threshold()
                + " DO " + watch.action();
    }

    /* An item's id as a player writes it: without the kind a stored key carries, nor the game's own namespace. */
    private static String shortName(final String item) {
        final String id = item.substring(item.indexOf(KIND_MARK) + 1);
        return id.startsWith(MINECRAFT) ? id.substring(MINECRAFT.length()) : id;
    }

    private static long digits(final String word) {
        try {
            return Long.parseLong(word.toLowerCase(Locale.ROOT));
        } catch (final NumberFormatException e) {
            return -1L;
        }
    }

    private static IqlEngine.Outcome ok(final Text said) {
        return new IqlEngine.Outcome(true, said, List.of());
    }

    private static IqlEngine.Outcome fail(final Text said) {
        return new IqlEngine.Outcome(false, said, List.of());
    }

    private ProphetMind mind(final MainframeBlockEntity core) {
        return minds.computeIfAbsent(core, mainframe -> ProphetMind.load(mainframe.engineData(def().program())));
    }

    private void save(final MainframeBlockEntity core, final ProphetMind mind) {
        final CompoundTag tag = core.engineData(def().program());
        for (final String old : List.copyOf(tag.getAllKeys())) {
            tag.remove(old);
        }
        mind.save(tag);
        core.markEngineDataChanged();
    }

    /**
     * A state as the console lists it.
     *
     * @param item       the item's id
     * @param name       the item's name
     * @param written    the state as written
     * @param status     where it stands, in words
     * @param tone       how its chip is coloured: one of the {@code TONE_} constants
     * @param held       what the network held when last looked at
     * @param inFlight   what is on its way
     * @param lower      the bottom of its band
     * @param upper      the top of its band, or {@link ProphetStatement#UNBOUNDED}
     * @param last       the last thing done for it
     * @param samples    the levels seen lately, the oldest first
     * @param operations the Operations set going for it lately, each with where it stands
     */
    public record StateRow(String item, Text name, String written, Text status, byte tone, long held, long inFlight,
                           long lower, long upper, Text last, List<ProphetStates.Sample> samples,
                           List<Text> operations) {

        public static final byte TONE_GOOD = 0;
        public static final byte TONE_WORKING = 1;
        public static final byte TONE_BAD = 2;
        public static final byte TONE_WAITING = 3;
    }

    /**
     * A watch as the console lists it.
     *
     * @param number  its number
     * @param written the watch as written
     * @param fired   whether it fired and waits for its condition to stop
     * @param armed   whether it waits for its condition
     * @param times   how many times it fired
     */
    public record WatchRow(int number, String written, boolean fired, boolean armed, int times) {
    }

    /**
     * Something it did.
     *
     * @param at   the game time
     * @param what what it did
     */
    public record ReactionRow(long at, Text what) {
    }

    /**
     * How it looks and reacts.
     *
     * @param interval how often it looks, in ticks
     * @param maxBatch the most it asks for in one craft
     * @param reacting whether it reacts at all
     */
    public record Settings(int interval, long maxBatch, boolean reacting) {
    }
}
