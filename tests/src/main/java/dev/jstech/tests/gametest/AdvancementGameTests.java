/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.advancement.JscEventTrigger;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.advancement.OperationMilestones;
import dev.jstech.computers.advancement.PendingAwards;
import dev.jstech.computers.advancement.ProgramTravels;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.ComputingOperations;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The advancements are earned by the player who caused what they name, and by nobody else.
 *
 * <p>Each test brings a real player into the world, because a machine acting as one, or a stand-in that is not a
 * server player, is exactly what must earn nothing. The player is taken out again at the end, so no test leaves a
 * stranger in the server's list for the next one.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class AdvancementGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private AdvancementGameTests() {
    }

    /** An event earns the advancement that names it, and leaves its neighbours alone. */
    @GameTest(template = ARENA)
    public static void award_earnsOnlyTheAdvancementThatNamesTheEvent(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            JscEvents.award(player, JscEvents.POST_PASSED);
            helper.assertTrue(done(player, "hardware/pc_master_race"), "passing POST earns PC Master Race");
            helper.assertFalse(done(player, "hardware/off_and_on_again"), "and nothing else in the tab");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** An advancement asking for every one of several things waits for the last of them. */
    @GameTest(template = ARENA)
    public static void award_everyDetailCountsOnItsOwn(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            JscEvents.award(player, JscEvents.ERA_BUILT, "vintage");
            JscEvents.award(player, JscEvents.ERA_BUILT, "legacy");
            helper.assertTrue(done(player, "hardware/vintage_build"), "a Vintage build earns its own");
            helper.assertFalse(done(player, "hardware/time_traveller"), "two eras of three are not every era");
            JscEvents.award(player, JscEvents.ERA_BUILT, "standard");
            helper.assertTrue(done(player, "hardware/time_traveller"), "the third era completes Time Traveller");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** A machine acting as a player earns nothing, and never becomes anybody's operator. */
    @GameTest(template = ARENA)
    public static void note_aFakePlayerNeverOperatesAMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = legacy(helper);
        final FakePlayer fake = FakePlayerFactory.getMinecraft(helper.getLevel());
        MachineOperators.note(computer, fake);
        helper.assertTrue(MachineOperators.of(computer).isEmpty(), "a fake player is nobody's operator");
        JscEvents.award(fake, JscEvents.POST_PASSED);
        helper.succeed();
    }

    /** What a machine does on its own is credited to whoever works it. */
    @GameTest(template = ARENA)
    public static void awardOperator_creditsWhoeverWorksTheMachine(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            final PersonalComputerBlockEntity computer = legacy(helper);
            MachineOperators.note(computer, player);
            helper.assertValueEqual(MachineOperators.of(computer).orElse(null), player,
                    "the one who used the machine is its operator");
            JscEvents.awardOperator(computer, JscEvents.DATACENTER_FORMED);
            helper.assertTrue(done(player, "hardware/someone_elses_computer"), "the operator earns it");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** A port built and installed earns Built from Ports for whoever works the machine that built it. */
    @GameTest(template = ARENA)
    public static void builtFromPorts_isEarnedByWhoeverWorksTheMachine(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            final PersonalComputerBlockEntity computer = legacy(helper);
            MachineOperators.note(computer, player);
            helper.assertFalse(done(player, "systems/built_from_ports"), "nothing is built yet");
            JscEvents.awardOperator(computer, JscEvents.BUILT_FROM_PORTS, "screenfetch");
            helper.assertTrue(done(player, "systems/built_from_ports"), "a port installed earns it");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** Putting a working build together earns the era it belongs to, for whoever is building it. */
    @GameTest(template = ARENA)
    public static void hardwareChanged_aWorkingLegacyBuildEarnsItsEra(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
            if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity computer)) {
                throw new IllegalStateException("no legacy personal computer at " + WHERE);
            }
            MachineOperators.note(computer, player);
            fit(computer);
            helper.assertTrue(done(player, "hardware/legacy_build"), "a working Legacy build earns its era");
            helper.assertFalse(done(player, "hardware/vintage_build"), "and not another era's");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** A scope says who is acting, the innermost one wins, and outside all of them nobody is. */
    @GameTest(template = ARENA)
    public static void acting_theInnermostScopeSaysWhoActs(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            final PersonalComputerBlockEntity nobodys = legacy(helper);
            final Optional<?>[] seen = new Optional<?>[2];
            Acting.as(player, () -> {
                seen[0] = Acting.current();
                Acting.asOperatorOf(nobodys, () -> seen[1] = Acting.current());
            });
            helper.assertValueEqual(seen[0], Optional.of(player.getUUID()), "the sender acts inside their scope");
            helper.assertTrue(seen[1].isEmpty(), "a machine nobody works hides whoever was acting around it");
            helper.assertTrue(Acting.current().isEmpty(), "outside every scope nobody acts");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** A finished Operation is credited to whoever asked for it, and a partial one earns the hidden one too. */
    @GameTest(template = ARENA)
    public static void operationSettled_isCreditedToWhoeverAskedForIt(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            final PersonalComputerBlockEntity mainframe = legacy(helper);
            final StorageKey logs = StorageKey.of(new ItemStack(Items.OAK_LOG));
            OperationMilestones.report(mainframe, player.getUUID(), ComputingOperations.SELECT,
                    new OperationRecord(OperationRecord.TYPE_SELECT, logs, 64L, 64L,
                            OperationRecord.STATUS_COMPLETED, List.of()));
            helper.assertTrue(done(player, "networks/select_from_chest"), "a finished SELECT earns its asker it");
            helper.assertFalse(done(player, "networks/two_out_of_three"), "a whole one is not a partial one");
            OperationMilestones.report(mainframe, player.getUUID(), ComputingOperations.SELECT,
                    new OperationRecord(OperationRecord.TYPE_SELECT, logs, 64L, 32L,
                            OperationRecord.STATUS_PARTIAL, List.of()));
            helper.assertTrue(done(player, "networks/two_out_of_three"), "a partial one earns the hidden one");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /**
     * An Operation finishing while whoever asked for it is away is theirs all the same: what it earned waits with the
     * world and is handed over when they join, once.
     */
    @GameTest(template = ARENA)
    public static void operationSettled_anAskerWhoIsAwayGetsItOnJoining(final GameTestHelper helper) {
        final UUID away = UUID.randomUUID();
        final PersonalComputerBlockEntity mainframe = legacy(helper);
        OperationMilestones.report(mainframe, away, ComputingOperations.SELECT,
                new OperationRecord(OperationRecord.TYPE_SELECT, StorageKey.of(new ItemStack(Items.OAK_LOG)), 64L,
                        64L, OperationRecord.STATUS_COMPLETED, List.of()));
        final PendingAwards pending = PendingAwards.of(helper.getLevel().getServer());
        helper.assertTrue(pending.waitingFor(away).contains(new PendingAwards.Award(JscEvents.SELECT_DONE, "")),
                "what a player away earned waits for them; got " + pending.waitingFor(away));
        final ServerPlayer player = join(helper, away);
        try {
            pending.deliver(player);
            helper.assertTrue(done(player, "networks/select_from_chest"), "joining, they are given it");
            helper.assertTrue(pending.waitingFor(away).isEmpty(), "and it is not kept to be given twice");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** A machine's operator who is away when it does something is given it on joining too. */
    @GameTest(template = ARENA)
    public static void awardOperator_anOperatorWhoIsAwayGetsItOnJoining(final GameTestHelper helper) {
        final UUID away = UUID.randomUUID();
        final PersonalComputerBlockEntity computer = legacy(helper);
        MachineOperators.note(computer, away);
        JscEvents.awardOperator(computer, JscEvents.DATACENTER_FORMED);
        final ServerPlayer player = join(helper, away);
        try {
            PendingAwards.of(helper.getLevel().getServer()).deliver(player);
            helper.assertTrue(done(player, "hardware/someone_elses_computer"), "the operator earns it on joining");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** The same program on a second computer earns Works on My Machine; again on the first does not. */
    @GameTest(template = ARENA)
    public static void ran_onlyASecondComputerCounts(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            ProgramTravels.ran(player, "sorter.asm", 1L);
            ProgramTravels.ran(player, "sorter.asm", 1L);
            helper.assertFalse(done(player, "sigma/works_on_my_machine"), "one computer twice is still one");
            ProgramTravels.ran(player, "sorter.asm", 2L);
            helper.assertTrue(done(player, "sigma/works_on_my_machine"), "a second computer earns it");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    /** Every event an advancement waits for is one the code can report: a misspelt id would never be earned. */
    @GameTest(template = ARENA)
    public static void advancements_waitOnlyForEventsTheCodeReports(final GameTestHelper helper) {
        final Set<String> reported = declaredEvents();
        int checked = 0;
        for (final AdvancementHolder holder : helper.getLevel().getServer().getAdvancements().getAllAdvancements()) {
            if (!JsComputers.MODID.equals(holder.id().getNamespace())) {
                continue;
            }
            for (final Criterion<?> criterion : holder.value().criteria().values()) {
                if (criterion.triggerInstance() instanceof JscEventTrigger.Instance instance) {
                    helper.assertTrue(reported.contains(instance.event()),
                            holder.id() + " waits for " + instance.event() + ", which nothing reports");
                    checked++;
                }
            }
        }
        helper.assertTrue(checked > 40, "the event advancements are loaded, " + checked + " criteria seen");
        helper.succeed();
    }

    private static boolean done(final ServerPlayer player, final String path) {
        final AdvancementHolder holder = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
        if (holder == null) {
            throw new IllegalStateException("no advancement " + path);
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /*
     * A server player of the test's own. It is never put on the server's player list the way a login does: a
     * listed player is announced to every mod, and a mod that greets players sends them data a test connection
     * cannot carry. It is only placed where the server looks a player up by id, which is all the advancements
     * need to find whoever works a machine, and taken out again by leave.
     */
    private static ServerPlayer join(final GameTestHelper helper) {
        return join(helper, UUID.randomUUID());
    }

    private static ServerPlayer join(final GameTestHelper helper, final UUID id) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(id, "advancer"), ClientInformation.createDefault());
        lookup(level.getServer().getPlayerList()).put(player.getUUID(), player);
        return player;
    }

    private static void leave(final ServerPlayer player) {
        lookup(player.server.getPlayerList()).remove(player.getUUID());
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> lookup(final PlayerList players) {
        try {
            final Field byId = PlayerList.class.getDeclaredField("playersByUUID");
            byId.setAccessible(true);
            return (Map<UUID, ServerPlayer>) byId.get(players);
        } catch (final ReflectiveOperationException missing) {
            throw new IllegalStateException("the server's player lookup is not where it was", missing);
        }
    }

    private static Set<String> declaredEvents() {
        final Set<String> events = new HashSet<>();
        for (final Field field : JscEvents.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                try {
                    events.add((String) field.get(null));
                } catch (final IllegalAccessException impossible) {
                    throw new IllegalStateException(impossible);
                }
            }
        }
        return events;
    }

    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no legacy personal computer at " + WHERE);
        }
        fit(computer);
        return computer;
    }

    private static void fit(final PersonalComputerBlockEntity computer) {
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_DUO_E4300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR2_2048.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
    }
}
