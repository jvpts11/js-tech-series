/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JavaOps;
import dev.jstech.core.jstechseries.command.SeriesCommands;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.AuditLog;
import dev.jstech.core.protection.IProtectionAdapter;
import dev.jstech.core.protection.MachineActors;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.protection.ProtectionBalance;
import dev.jstech.core.protection.ProtectionCommands;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.ITeamSource;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.team.Trust;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestClaims;
import dev.jstech.tests.TestEntities;
import dev.jstech.tests.TestPermissions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;

/**
 * Permissions and protection: a node for every command and for what others own, held by the level each was declared
 * with and written out for LuckPerms; the one question every protection answers, any denial stopping the work and the
 * world's own work kept inside its claim; trust by name in three levels; an owned thing kept from strangers; the limit
 * of robots each player has; the audit log; and damage between players held to the server's rule and the teams.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProtectionGameTests {

    private static final String ARENA = "empty";
    /** The one player the test protection stops, and only where it fences. */
    private static final UUID FENCED = UUID.fromString("6a737465-7374-7300-0000-000000000001");
    /** The claim's owner, and where its claim lies: every place west of x 0. */
    private static final UUID CLAIMANT = UUID.fromString("6a737465-7374-7300-0000-000000000002");

    private ProtectionGameTests() {
    }

    @GameTest(template = ARENA)
    public static void permissions_eachCommandHasANodeHeldByItsLevel(final GameTestHelper helper) {
        helper.assertTrue(SeriesCommands.node("config").map(node -> node.getNodeName())
                .equals(Optional.of("jscore.command.config")), "a command's node is named after it");
        helper.assertTrue(PermissionAPI.getRegisteredNodes().contains(CorePermissions.EDIT_SETTINGS),
                "the series' nodes are handed to the game's permission system");
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "jstests-permissions"));
        helper.assertFalse(CorePermissions.has(player, CorePermissions.USE_OTHERS),
                "a player who is no operator does not pass owners");
        helper.assertTrue(CorePermissions.has(player, CorePermissions.BUILD_MULTIBLOCKS),
                "but builds multiblocks, which every player may");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void permissions_areAskedWithThePlacesDimensionAndClaim(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos inside = helper.absolutePos(new BlockPos(1, 2, 1));
        final BlockPos outside = helper.absolutePos(new BlockPos(4, 2, 4));
        final AABB claim = new AABB(inside);
        TestClaims.claim(claim);
        try {
            final ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(),
                    "jstests-contexts"));
            final PermissionDynamicContext<?>[] there = CorePermissions.contextsAt(level, inside);
            helper.assertTrue(there.length == 2 && there[0].getDynamic() == CorePermissions.DIMENSION
                    && there[0].getSerializedValue().equals(level.dimension().location().toString()),
                    "the permission is told the place's dimension");
            helper.assertTrue(there[1].getDynamic() == CorePermissions.CLAIM
                    && there[1].getSerializedValue().equals(TestClaims.CLAIMANT.toString()),
                    "and the claim it lies in, by its owner");
            helper.assertTrue(CorePermissions.contextsAt(level, outside)[1].getSerializedValue()
                    .equals(CorePermissions.NO_CLAIM), "outside every claim, none");
            helper.assertTrue(CorePermissions.has(player, TestPermissions.IN_A_CLAIM, level, inside)
                    && !CorePermissions.has(player, TestPermissions.IN_A_CLAIM, level, outside),
                    "a node granted by the claim's context is held in the claim and not beside it");
            helper.assertTrue(CorePermissions.has(player.getUUID(), TestPermissions.IN_A_CLAIM, level, inside),
                    "and so for a machine there acting for its owner");
        } finally {
            TestClaims.unclaim(claim);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dumpPermissions_writesTheNodesAndTheLuckPermsGroups(final GameTestHelper helper)
            throws IOException {
        final MinecraftServer server = helper.getLevel().getServer();
        final Path folder = FMLPaths.GAMEDIR.get().resolve(ProtectionCommands.FOLDER);
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                    "jstech dump permissions");
            final String nodes = Files.readString(folder.resolve("nodes.md"));
            final String groups = Files.readString(folder.resolve("luckperms.txt"));
            helper.assertTrue(nodes.contains("jscore.config.edit") && nodes.contains("jscore.command.audit"),
                    "the list names every node");
            helper.assertTrue(groups.contains("lp group jstech-admins permission set jscore.config.edit true"),
                    "an operator's node goes to the admins' group");
            helper.assertTrue(groups.contains("lp group jstech-players permission set jscore.multiblock.build true"),
                    "every player's to the players' group");
        } finally {
            Files.deleteIfExists(folder.resolve("nodes.md"));
            Files.deleteIfExists(folder.resolve("luckperms.txt"));
            Files.deleteIfExists(folder);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void protection_anyDenialStopsTheWorkAndTheWorldStaysInItsClaim(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        if (!Protection.names().contains(FenceForTests.NAME)) {
            Protection.register(FenceForTests.NAME, new FenceForTests());
        }
        final BlockPos west = new BlockPos(-3, 64, 5);
        final BlockPos alsoWest = new BlockPos(-40, 64, 5);
        final BlockPos east = new BlockPos(3, 64, 5);
        helper.assertFalse(Protection.may(Actor.forOwner(FENCED), ProtectedAction.BREAK, level, west),
                "the protection stops the player it fences, owner of a machine or not");
        helper.assertTrue(Protection.stoppedBy(Actor.forOwner(FENCED), ProtectedAction.BREAK, level, west)
                .equals(Optional.of(FenceForTests.NAME)), "and says which protection stopped it");
        helper.assertTrue(Protection.may(Actor.forOwner(CLAIMANT), ProtectedAction.BREAK, level, west),
                "the claim's owner may work in it");
        helper.assertTrue(Protection.may(Actor.world(alsoWest), ProtectedAction.FLOW, level, west),
                "the world's own work may move inside one claim");
        helper.assertFalse(Protection.may(Actor.world(east), ProtectedAction.FLOW, level, west),
                "but not into it from outside");
        helper.assertTrue(Protection.may(Actor.world(west), ProtectedAction.FLOW, level, east),
                "out of a claim into no claim it may");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void trust_letsEachLevelDoWhatItHolds(final GameTestHelper helper) {
        final ServerPlayer owner = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "jstests-trusting"));
        final ServerPlayer friend = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "jstests-trusted"));
        final Ownership mine = Ownership.of(owner.getUUID());
        helper.assertFalse(mine.mayUse(friend) || mine.maySetUp(friend) || mine.mayBreak(friend),
                "a stranger may do nothing with a private thing");
        final Ownership setUp = mine.withTrust(friend.getUUID(), Trust.SET_UP);
        helper.assertTrue(setUp.mayUse(friend) && setUp.mayOpen(friend) && setUp.maySetUp(friend),
                "trusted to set it up, a player may use it and set it up");
        helper.assertFalse(setUp.mayBreak(friend), "but not break it");
        helper.assertTrue(setUp.withTrust(friend.getUUID(), Trust.BREAK).mayBreak(friend), "trusted to break, may");
        helper.assertFalse(setUp.withoutTrust(friend.getUUID()).mayUse(friend), "no longer trusted, may not");
        final Ownership back = Ownership.CODEC.parse(JavaOps.INSTANCE,
                Ownership.CODEC.encodeStart(JavaOps.INSTANCE, setUp).getOrThrow()).getOrThrow();
        helper.assertTrue(back.equals(setUp), "the trust is saved with the owner");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ownedEntity_isKeptFromStrangers(final GameTestHelper helper) {
        final UUID owner = UUID.randomUUID();
        final TestEntities.Robot robot = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(1, 2, 1));
        robot.giveTo(owner);
        final ServerPlayer stranger = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "jstests-stranger"));
        helper.assertTrue(NeoForge.EVENT_BUS.post(new AttackEntityEvent(stranger, robot)).isCanceled(),
                "a stranger may not attack another's robot");
        final ServerPlayer ownerInPerson = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(owner, "jstests-robot-owner"));
        helper.assertFalse(NeoForge.EVENT_BUS.post(new AttackEntityEvent(ownerInPerson, robot)).isCanceled(),
                "its owner may");
        helper.assertTrue(robot.actingPlayer().getUUID().equals(owner),
                "the robot acts in the world under its owner's id");
        robot.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void limits_stopAtTheMostAndComeBackAsThingsGo(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID owner = UUID.randomUUID();
        final int before = ProtectionBalance.robotsPerPlayer();
        ProtectionBalance.setRobotsPerPlayer(1);
        try {
            final TestEntities.Robot first = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(1, 2, 1));
            final TestEntities.Robot second = helper.spawn(TestEntities.ROBOT.get(), new BlockPos(2, 2, 2));
            helper.assertTrue(first.giveTo(owner), "the first robot is theirs");
            helper.assertFalse(second.giveTo(owner), "a second goes past the limit");
            helper.assertTrue(PlayerLimits.count(server, owner, PlayerLimits.ROBOTS) == 1, "one is counted");
            first.discard();
            helper.assertTrue(PlayerLimits.count(server, owner, PlayerLimits.ROBOTS) == 0,
                    "a robot gone gives its place back");
            helper.assertTrue(second.giveTo(owner), "so another may be theirs");
            second.discard();
        } finally {
            ProtectionBalance.setRobotsPerPlayer(before);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void audit_writesWhatOthersDoWhileKept(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID owner = UUID.randomUUID();
        final UUID visitor = UUID.randomUUID();
        final ResourceLocation thing = ResourceLocation.withDefaultNamespace("furnace");
        final BlockPos pos = new BlockPos(7, 64, 7);
        final AuditLog log = AuditLog.of(level.getServer());
        AuditLog.record(level, pos, visitor, AuditLog.Kind.USE, thing, owner);
        helper.assertTrue(log.newest(entry -> entry.actor().equals(visitor), 0, 1).isEmpty(),
                "nothing is written while the log is off");
        final boolean before = ProtectionBalance.audit();
        ProtectionBalance.setAudit(true);
        try {
            AuditLog.record(level, pos, visitor, AuditLog.Kind.BREAK, thing, owner);
            AuditLog.record(level, pos, owner, AuditLog.Kind.CHANGE, thing, owner);
            helper.assertTrue(log.newest(entry -> entry.actor().equals(visitor), 0, 5).size() == 1,
                    "what a visitor does is written down");
            helper.assertTrue(log.newest(entry -> entry.actor().equals(owner), 0, 5).isEmpty(),
                    "what an owner does to their own is not");
        } finally {
            ProtectionBalance.setAudit(before);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void damage_followsTheServersRuleAndTheTeams(final GameTestHelper helper) {
        final ServerPlayer victim = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "jstests-victim"));
        final UUID attacker = UUID.randomUUID();
        helper.assertTrue(Protection.mayHurt(Actor.world(null), victim), "the world may always hurt a player");
        helper.assertTrue(Protection.mayHurt(Actor.forOwner(attacker), victim) == victim.server.isPvpAllowed(),
                "another's machine may, as the server's PvP rule says");
        final ITeamSource before = CoreTeams.source();
        CoreTeams.use((server, player) -> "jstests:crew");
        try {
            helper.assertFalse(Protection.mayHurt(Actor.forOwner(attacker), victim), "a teammate's may not");
            ProtectionBalance.setTeamDamage(true);
            helper.assertTrue(Protection.mayHurt(Actor.forOwner(attacker), victim) == victim.server.isPvpAllowed(),
                    "unless the server lets teammates hurt each other");
        } finally {
            ProtectionBalance.setTeamDamage(false);
            CoreTeams.use(before);
        }
        helper.assertTrue(MachineActors.standIn(helper.getLevel(), attacker).getUUID().equals(attacker),
                "a machine's stand-in carries its owner's id");
        helper.succeed();
    }

    /** A protection of the test mod: it fences one player out, and everything west of x 0 is one claim. */
    private static final class FenceForTests implements IProtectionAdapter {

        static final String NAME = "jstests:fence";

        @Override
        public Verdict ask(final Actor actor, final ProtectedAction action, final ServerLevel level,
                           final BlockPos pos) {
            if (actor.who().equals(Optional.of(FENCED))) {
                return Verdict.DENY;
            }
            if (claimOwner(level, pos).isEmpty()) {
                return Verdict.ABSTAIN;
            }
            return switch (actor) {
                case Actor.World world -> Protection.sameClaim(this, level, world.origin(), pos) ? Verdict.ALLOW
                        : Verdict.DENY;
                default -> actor.who().equals(Optional.of(CLAIMANT)) ? Verdict.ALLOW : Verdict.ABSTAIN;
            };
        }

        @Override
        public Optional<UUID> claimOwner(final ServerLevel level, final BlockPos pos) {
            // Only the far test positions: the arenas the other tests run in are never this far west.
            return pos.getX() < 0 && pos.getX() > -100 && pos.getY() == 64 && pos.getZ() == 5 ? Optional.of(CLAIMANT)
                    : Optional.empty();
        }
    }
}
