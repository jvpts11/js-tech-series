/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.iql.IqlRunStatement;
import dev.jstech.core.JsCore;
import dev.jstech.core.jstechseries.network.INetworkMember;
import dev.jstech.core.jstechseries.network.NetworkMembers;
import dev.jstech.core.jstechseries.network.NetworkMembership;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.operation.IOperationArgs;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.core.jstechseries.operation.OperationRequest;
import dev.jstech.core.jstechseries.operation.OperationType;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * How the Mainframe gives a request of another mod's kind of Operation to a machine of its network: which machine,
 * what the request carries when it is written as text, and what the network calls the machine.
 *
 * <p>A machine is named on the network by the name a player gave it on its own screen, or else by its block's name
 * and the first six characters of its identity on the network, as the Servers are ({@code Test accelerator-3fa2c1});
 * a request can also name it by where it stands ({@code 12, 64, -3}). A request that names no machine goes to the able
 * machine with the fewest of these requests in flight, the one that joined first when they are even.
 */
@TextHolder
public final class MemberRuns {

    public static final TextKey NO_SUCH_KIND = TextKey.of("jsc.operation.run.no_such_kind",
            "no mod registered a kind of Operation %s");
    public static final TextKey NOT_FROM_TEXT = TextKey.of("jsc.operation.run.not_from_text",
            "%s is asked for from its machine's own screen, never in the network's language");
    public static final TextKey NO_MACHINE = TextKey.of("jsc.operation.run.no_machine",
            "no machine on the network carries out %s");
    public static final TextKey NO_MACHINE_NAMED = TextKey.of("jsc.operation.run.no_machine_named",
            "no machine named %s is on the network");
    public static final TextKey CANNOT = TextKey.of("jsc.operation.run.cannot", "%s does not carry out %s");
    public static final TextKey NO_FIELD = TextKey.of("jsc.operation.run.no_field",
            "%s has no field %s; its fields are %s");
    public static final TextKey BAD_FIELDS = TextKey.of("jsc.operation.run.bad_fields",
            "the fields do not serve %s: %s");
    public static final TextKey NOT_RUNNING = TextKey.of("jsc.operation.run.not_running",
            "the Mainframe has no running system to give the work out");
    public static final TextKey QUEUED = TextKey.of("jsc.operation.run.queued", "%s queued for %s");

    /* How long the short identity after a block's name is, as the Servers' own. */
    private static final int SHORT_ID = 6;
    /* A position as the network writes one, three whole numbers apart by commas or spaces. */
    private static final Pattern POSITION = Pattern.compile("\\s*(-?\\d+)\\s*[, ]\\s*(-?\\d+)\\s*[, ]\\s*(-?\\d+)\\s*");

    private MemberRuns() {
    }

    /**
     * What a request came to: the Operation the Mainframe took on, or none, and what to say of it.
     *
     * @param operation the Operation taken on; null when the request was refused
     * @param said      that it was queued and for which machine, or why it was refused
     */
    public record Outcome(@Nullable MemberOperation operation, Text said) {

        /** Whether it was taken on. */
        public boolean ok() {
            return this.operation != null;
        }
    }

    /** Takes on the request a {@code RUN} statement writes, read as its kind says its requests are written. */
    public static Outcome run(final MainframeBlockEntity mainframe, final IqlRunStatement statement) {
        final Optional<OperationType<?>> kind = JsCore.operations().get(statement.kind());
        if (kind.isEmpty()) {
            return refused(NO_SUCH_KIND.with(statement.kind()));
        }
        return readAndRun(mainframe, kind.get(), statement);
    }

    /**
     * Takes on a request of {@code type} carrying {@code args}, for the machine named {@code machine} (by its name on
     * the network or its position), or for the least busy able one when that is empty.
     */
    public static <T extends IOperationArgs> Outcome run(final MainframeBlockEntity mainframe,
                                                         final OperationType<T> type, final T args,
                                                         final String machine, final OperationPriority priority) {
        return give(mainframe, type, args, machine, said(type, args), priority);
    }

    /** What the network calls {@code member}: the name a player gave it, or its block's name and short identity. */
    public static String nameOf(final ServerLevel level, final NetworkSystem.MemberNode member) {
        final Optional<INetworkMember> kept = NetworkMembers.at(level, BlockPos.of(member.pos()))
                .map(NetworkMembership::member);
        if (kept.isPresent() && kept.get() instanceof Nameable named && named.hasCustomName()
                && named.getCustomName() != null) {
            return named.getCustomName().getString();
        }
        final String block = kept.isPresent() && kept.get() instanceof BlockEntity entity
                ? GameText.of(entity.getBlockState().getBlock().getName()).english()
                : GameText.of(level.getBlockState(BlockPos.of(member.pos())).getBlock().getName()).english();
        return block + "-" + member.nodeUuid().value().toString().substring(0, SHORT_ID);
    }

    /* Reads what the statement gives each field into the kind's arguments, then gives the request out. */
    private static <T extends IOperationArgs> Outcome readAndRun(final MainframeBlockEntity mainframe,
                                                                 final OperationType<T> type,
                                                                 final IqlRunStatement statement) {
        final MapCodec<T> text = type.text();
        if (text == null) {
            return refused(NOT_FROM_TEXT.with(type.name()));
        }
        // A codec built of fields can name one more than once; each is said once.
        final Set<String> fields = new LinkedHashSet<>();
        text.keys(JsonOps.INSTANCE).forEach(key -> fields.add(key.getAsString()));
        final JsonObject written = new JsonObject();
        for (final IqlRunStatement.Field field : statement.fields()) {
            if (!fields.isEmpty() && !fields.contains(field.name())) {
                return refused(NO_FIELD.with(type.name(), field.name(), String.join(", ", fields)));
            }
            switch (field.value()) {
                case Long number -> written.addProperty(field.name(), number);
                case Double number -> written.addProperty(field.name(), number);
                case Boolean truth -> written.addProperty(field.name(), truth);
                default -> written.addProperty(field.name(), String.valueOf(field.value()));
            }
        }
        final DataResult<T> read = text.codec().parse(JsonOps.INSTANCE, written);
        if (read.result().isEmpty()) {
            return refused(BAD_FIELDS.with(type.name(), read.error().map(DataResult.Error::message).orElse("")));
        }
        return give(mainframe, type, read.result().get(), statement.machine(), statement.asked(),
                statement.priority());
    }

    /* Finds the machine, and gives it the request to wait for a queue on the Mainframe. */
    private static <T extends IOperationArgs> Outcome give(final MainframeBlockEntity mainframe,
                                                           final OperationType<T> type, final T args,
                                                           final String machine, final String asked,
                                                           final OperationPriority priority) {
        final NetworkUuid network = mainframe.networkUuid();
        if (!(mainframe.getLevel() instanceof ServerLevel level) || network == null) {
            return refused(NOT_RUNNING.text());
        }
        final NetworkSystem system = NetworkSystem.get(level);
        final NetworkSystem.MemberNode chosen;
        if (machine.isBlank()) {
            chosen = leastBusy(mainframe, system.membersCarryingOut(network, type.id()));
            if (chosen == null) {
                return refused(NO_MACHINE.with(type.name()));
            }
        } else {
            chosen = named(level, system.membersOf(network), machine);
            if (chosen == null) {
                return refused(NO_MACHINE_NAMED.with(machine));
            }
            if (!chosen.carriesOut(type.id())) {
                return refused(CANNOT.with(nameOf(level, chosen), type.name()));
            }
        }
        final Optional<INetworkMember> member = NetworkMembers.at(level, BlockPos.of(chosen.pos()))
                .map(NetworkMembership::member);
        if (member.isEmpty()) {
            return refused(NO_MACHINE_NAMED.with(machine.isBlank() ? nameOf(level, chosen) : machine));
        }
        final String name = nameOf(level, chosen);
        final MemberOperation operation = mainframe.submitMemberRun(new OperationRequest<>(type, args, member.get()),
                chosen.pos(), name, asked, priority);
        if (operation == null) {
            return refused(NOT_RUNNING.text());
        }
        return new Outcome(operation, QUEUED.with(type.name(), name));
    }

    /* The able machine with the fewest of these requests in flight on {@code mainframe}; the first joined of a tie. */
    @Nullable
    private static NetworkSystem.MemberNode leastBusy(final MainframeBlockEntity mainframe,
                                                      final List<NetworkSystem.MemberNode> able) {
        NetworkSystem.MemberNode best = null;
        int fewest = Integer.MAX_VALUE;
        for (final NetworkSystem.MemberNode member : able) {
            int busy = 0;
            for (final INetworkOperation operation : mainframe.liveOperations()) {
                if (operation instanceof MemberOperation given && !given.isDone()
                        && given.machinePos() == member.pos()) {
                    busy++;
                }
            }
            if (busy < fewest) {
                best = member;
                fewest = busy;
            }
        }
        return best;
    }

    /* The member of {@code members} that {@code machine} names, by where it stands or by its name; none for none. */
    @Nullable
    private static NetworkSystem.MemberNode named(final ServerLevel level,
                                                  final List<NetworkSystem.MemberNode> members, final String machine) {
        final Matcher position = POSITION.matcher(machine);
        if (position.matches()) {
            final long pos = BlockPos.asLong(Integer.parseInt(position.group(1)), Integer.parseInt(position.group(2)),
                    Integer.parseInt(position.group(3)));
            for (final NetworkSystem.MemberNode member : members) {
                if (member.pos() == pos) {
                    return member;
                }
            }
            return null;
        }
        final String wanted = machine.strip().toLowerCase(Locale.ROOT);
        for (final NetworkSystem.MemberNode member : members) {
            if (nameOf(level, member).toLowerCase(Locale.ROOT).equals(wanted)) {
                return member;
            }
        }
        return null;
    }

    /* What a request carries, as the network's language would write it, for the log; nothing for a kind it cannot. */
    private static <T extends IOperationArgs> String said(final OperationType<T> type, final T args) {
        final MapCodec<T> text = type.text();
        if (text == null) {
            return "";
        }
        final Optional<JsonElement> written = text.codec().encodeStart(JsonOps.INSTANCE, args).result();
        if (written.isEmpty() || !written.get().isJsonObject()) {
            return "";
        }
        final List<String> each = new ArrayList<>();
        for (final Map.Entry<String, JsonElement> field : written.get().getAsJsonObject().entrySet()) {
            final JsonElement value = field.getValue();
            each.add(field.getKey() + " " + (value.isJsonPrimitive() ? value.getAsString() : value.toString()));
        }
        return String.join(", ", each);
    }

    private static Outcome refused(final Text why) {
        return new Outcome(null, why);
    }
}
