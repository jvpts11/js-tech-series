/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.module.InstalledModules;
import dev.jstech.core.module.Modules;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModules;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The Core's modules in worn things, through the test mod's helmet: a module goes only into an empty socket of its
 * kind and comes out again; worn and on, each does what it does for its wearer, spending the helmet's energy; turned
 * off, or out of energy, what it did goes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ModuleGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_modules";

    private ModuleGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void modules_goOnlyIntoAnEmptySocketOfTheirKind(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ItemStack helmet = new ItemStack(TestModules.HELMET.get());
        helper.assertTrue(!Modules.install(level, helmet, 0, new ItemStack(TestModules.SPEED_MODULE.get())),
                "a core module does not go into the visor's socket");
        final ItemStack speed = new ItemStack(TestModules.SPEED_MODULE.get(), 2);
        helper.assertTrue(Modules.install(level, helmet, 1, speed) && speed.getCount() == 1,
                "it goes into a core's socket, one of the stack");
        helper.assertTrue(!Modules.install(level, helmet, 1, speed), "but not into a socket already taken");
        final InstalledModules installed = Modules.installed(level, helmet);
        helper.assertTrue(installed.sockets().size() == 3 && installed.sockets().get(1).on(),
                "the helmet has its three sockets, the module on in the second");
        final ItemStack out = Modules.remove(level, helmet, 1);
        helper.assertTrue(out.is(TestModules.SPEED_MODULE.get()) && Modules.installed(level, helmet).sockets()
                .get(1).module().isEmpty(), "and it comes out again");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void modules_workForTheirWearerWhileOnAndCharged(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = player(helper, "module-wearer");
        final ItemStack helmet = new ItemStack(TestModules.HELMET.get());
        charge(helmet);
        Modules.install(level, helmet, 0, new ItemStack(TestModules.SIGHT_MODULE.get()));
        Modules.install(level, helmet, 1, new ItemStack(TestModules.SPEED_MODULE.get()));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        final double walking = player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        Modules.work(player);
        final ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > walking * 1.19,
                "the speed module makes its wearer faster: " + player.getAttributeValue(Attributes.MOVEMENT_SPEED));
        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "the visor lets them see in the dark");
        helper.assertTrue(energy(worn) == TestModules.HELMET_ENERGY - TestModules.SPEED_ENERGY
                - TestModules.SIGHT_ENERGY, "spending the helmet's energy: " + energy(worn));
        Modules.setOn(level, worn, 1, false);
        Modules.work(player);
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - walking) < 1.0e-9,
                "turned off, what it did goes");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void modules_doNothingOutOfEnergy(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = player(helper, "module-wearer-flat");
        final ItemStack helmet = new ItemStack(TestModules.HELMET.get());
        Modules.install(level, helmet, 1, new ItemStack(TestModules.SPEED_MODULE.get()));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        final double walking = player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        Modules.work(player);
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - walking) < 1.0e-9,
                "with the helmet empty the module does nothing");
        leave(helper, player);
        helper.succeed();
    }

    /* Fills {@code helmet} with energy, as a charger would, a transfer at a time. */
    private static void charge(final ItemStack helmet) {
        final IEnergyStorage energy = helmet.getCapability(Capabilities.EnergyStorage.ITEM);
        while (energy != null && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {
            // Each transfer takes what the helmet takes at once.
        }
    }

    private static int energy(final ItemStack stack) {
        final IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return energy == null ? -1 : energy.getEnergyStored();
    }

    /* A player of a game connected to the server, in survival, in the test's arena. */
    private static ServerPlayer player(final GameTestHelper helper, final String name) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                name), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        for (int id = 0; id <= 8; id++) {
            player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id));
        }
        player.setGameMode(GameType.SURVIVAL);
        final Vec3 there = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        player.moveTo(there.x, there.y, there.z, 0, 0);
        return player;
    }

    private static void leave(final GameTestHelper helper, final ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }
}
