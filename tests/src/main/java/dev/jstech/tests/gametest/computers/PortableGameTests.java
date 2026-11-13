/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PortableItems;
import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.block.ChargingDockBlock;
import dev.jstech.computers.blockentity.ChargingDockBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.computers.operation.payload.NodeLink;
import dev.jstech.computers.operation.payload.network.NetworkPayloads;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.portable.AlertKind;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableBattery;
import dev.jstech.computers.portable.PortableDevices;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.PortableLink;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.portable.PortableState;
import dev.jstech.computers.portable.Portables;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.ShortId;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The portable devices and their docks (computers/docs/design/wireless.md): a device carried takes its id, its state
 * with the apps it comes with, and switches on; it draws its battery and switches off flat; it joins the network of the
 * nearest access point that reaches its player at the older end's speed and shows on the Network Manager; its store
 * installs only while the Mirror serves; its alerts are seen and let go; and a dock of its era charges it from the
 * energy behind it, refuses another era's, and gives it back to be switched on again.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PortableGameTests {

    private static final String ARENA = "empty";
    /* The batch whose draw is on, which ComputerEnergyGameTests turns on and off round it. */
    private static final String ENERGY = TestComputerEnergy.BATCH;
    /* The chain the network meets the Mainframe by, the access point at its end, and where its player stands. */
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos ACCESS_POINT = new BlockPos(5, 2, 2);
    private static final BlockPos NEAR = new BlockPos(5, 2, 6);
    private static final BlockPos DOCK = new BlockPos(3, 2, 3);
    private static final int SETTLE = 60;

    private PortableGameTests() {
    }

    /** A device carried the first time takes an id, comes full and on, with the apps its kind and era come with. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void portable_carriedTakesItsIdAndItsAppsAndSwitchesOn(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, NEAR);
        final ItemStack phone = carry(player, PortableItems.QPHONE.get());
        final UUID id = Portables.id(phone);
        helper.assertTrue(id != null, "it takes an id");
        helper.assertTrue(Portables.isOn(phone), "and switches on");
        helper.assertValueEqual(Portables.battery(phone, PortableModel.QPHONE),
                PortableBattery.capacity(PortableModel.QPHONE), "full");
        final PortableState state = PortableDevices.get(helper.getLevel().getServer()).find(id);
        helper.assertTrue(state != null && state.installed().equals(EnumSet.of(MobileApp.NETWORK, MobileApp.ALERTS,
                        MobileApp.CAMERA, MobileApp.GALLERY, MobileApp.MAPS)),
                "a Transition smartphone comes with the network, the alerts, the camera, the gallery and Maps; got "
                        + (state == null ? null : state.installed()));
        final ItemStack pda = carry(player, PortableItems.HIGHTIDE_POCKET.get());
        final PortableState pdaState = PortableDevices.get(helper.getLevel().getServer()).find(Portables.id(pda));
        helper.assertTrue(pdaState != null && pdaState.installed().equals(EnumSet.of(MobileApp.NETWORK,
                MobileApp.ALERTS)), "a Legacy PDA has no camera, no GPS, nothing of later eras");
        helper.succeed();
    }

    /** A device on draws its battery while carried, in steps, and switches off when it is flat. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void portable_drawsItsBatteryAndSwitchesOffFlat(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, NEAR);
        final ItemStack phone = carry(player, PortableItems.QPHONE_5.get());
        Portables.setBattery(phone, PortableModel.QPHONE_5, PortableBattery.DRAWN_A_TICK * Portables.DRAW_EVERY);
        helper.startSequence()
                .thenExecuteFor(Portables.DRAW_EVERY + 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    helper.assertValueEqual(Portables.battery(phone, PortableModel.QPHONE_5), 0, "drawn flat");
                    helper.assertFalse(Portables.isOn(phone), "and switched off");
                    tick(player, phone);
                    helper.assertFalse(Portables.isOn(phone), "a flat device stays off");
                })
                .thenSucceed();
    }

    /**
     * Carried where an access point reaches its player, a device joins that network at the older end's speed: a qPhone
     * X through a Transition access point goes at 802.11n; the Network Manager lists it as a smartphone on Wi-Fi.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void portable_joinsTheAccessPointInReachAtTheOlderSpeed(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final ServerPlayer player = player(helper, NEAR);
        final ItemStack phone = carry(player, PortableItems.QPHONE_X.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 3, () -> tick(player, phone))
                .thenExecute(() -> {
                    final PortableLink link = phone.get(ComputingComponents.PORTABLE_LINK.get());
                    helper.assertTrue(link != null && link.speed() == WirelessStandard.TRANSITION
                                    && link.network().equals(ShortId.of(mainframe.networkUuid().asString()))
                                    && link.accessPoint().startsWith("AP-"),
                            "linked through the access point at 802.11n; got " + link);
                    final NetworkNodeInfo node = NetworkPayloads.collectNetworkManager(helper.getLevel(), mainframe)
                            .nodes().stream().filter(found -> found.kind() == NetworkNodeInfo.KIND_SMARTPHONE)
                            .findFirst().orElse(null);
                    final NodeLink.Wifi wifi = node == null ? null : node.link().wifi();
                    helper.assertTrue(node != null && node.online() && "qphone_x".equals(node.detail())
                                    && wifi != null && "advanced".equals(wifi.card())
                                    && "transition".equals(wifi.accessPoint()),
                            "the Network Manager lists it as a smartphone on Wi-Fi; got " + node);
                })
                .thenSucceed();
    }

    /** Out of every access point's reach, a device is on no network and its link says so. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void portable_outOfReachHasNoLink(final GameTestHelper helper) {
        network(helper);
        final ServerPlayer player = player(helper, NEAR);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(NEAR)).add(200, 0, 0));
        final ItemStack phone = carry(player, PortableItems.QPHONE.get());
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> helper.assertTrue(phone.get(ComputingComponents.PORTABLE_LINK.get()) == null,
                        "no link out of reach"))
                .thenSucceed();
    }

    /**
     * The store installs an app the device can have only while its network's Mirror serves; a device cannot have an
     * app of a later era or of hardware it lacks.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void store_installsOnlyWhileTheMirrorServes(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final ServerPlayer player = player(helper, NEAR);
        final ItemStack phone = carry(player, PortableItems.QPHONE.get());
        final UUID id = Portables.id(phone);
        player.setItemInHand(InteractionHand.MAIN_HAND, phone);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    install(helper, player, id, MobileApp.CRYPTO_WALLET);
                    helper.assertFalse(devices.find(id).installed().contains(MobileApp.CRYPTO_WALLET),
                            "no Mirror, nothing installs");
                    mainframe.installMirror();
                })
                .thenWaitUntil(() -> helper.assertTrue(mainframe.isMirrorActive(), "the Mirror serves"))
                .thenExecute(() -> {
                    install(helper, player, id, MobileApp.CRYPTO_WALLET);
                    install(helper, player, id, MobileApp.MESSENGER);
                    helper.assertTrue(devices.find(id).installed().contains(MobileApp.CRYPTO_WALLET),
                            "the Crypto Wallet installs from the Transition on");
                    helper.assertFalse(devices.find(id).installed().contains(MobileApp.MESSENGER),
                            "the Messenger is the Standard's");
                })
                .thenSucceed();
    }

    /** Alerts come in unseen, are marked seen when looked at, and go when let go. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void alerts_areSeenAndLetGo(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, NEAR);
        final ItemStack phone = carry(player, PortableItems.SUNSUNG_NEBULA_S10.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, phone);
        final UUID id = Portables.id(phone);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        for (int k = 0; k < PortableState.MOST_ALERTS + 3; k++) {
            devices.deliver(id, new PortableAlert(AlertKind.UPS, Text.literal("UPS on battery"),
                    Text.literal("PowerGuard 1500"), k, false));
        }
        helper.assertValueEqual(devices.find(id).alerts().size(), PortableState.MOST_ALERTS, "as many as are kept");
        // Newest first: the last delivered leads, and the three oldest are the ones gone.
        helper.assertValueEqual(devices.find(id).alerts().getFirst().at(), (long) PortableState.MOST_ALERTS + 2,
                "the newest leads");
        helper.assertValueEqual(devices.find(id).alerts().getLast().at(), 3L, "the oldest kept is the fourth");
        helper.assertValueEqual(devices.find(id).unseen(), PortableState.MOST_ALERTS, "none seen yet");
        Portables.act(PortableActionPayload.of(id, PortableActionPayload.Action.SEE_ALERTS), player,
                helper.getLevel());
        helper.assertValueEqual(devices.find(id).unseen(), 0, "seen once looked at");
        Portables.act(PortableActionPayload.of(id, PortableActionPayload.Action.CLEAR_ALERTS), player,
                helper.getLevel());
        helper.assertTrue(devices.find(id).alerts().isEmpty(), "let go");
        helper.succeed();
    }

    /**
     * With the energy drawn, a dock charges the portable in it only from the energy brought behind it, its lamp lit
     * while it does; no energy comes in at its front.
     */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = ENERGY)
    public static void dock_chargesFromTheEnergyBehindIt(final GameTestHelper helper) {
        final ChargingDockBlockEntity dock = dock(helper, PortableItems.TRANSITION_CHARGING_DOCK.get());
        final ItemStack phone = new ItemStack(PortableItems.QPHONE.get());
        Portables.setBattery(phone, PortableModel.QPHONE, 0);
        dock.put(phone);
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertValueEqual(Portables.battery(dock.held(), PortableModel.QPHONE), 0,
                            "no energy, no charge");
                    helper.assertTrue(dock.energyPort(Direction.EAST) == null, "nothing at its front");
                    TestComputerEnergy.feed(dock.energyPort(Direction.WEST));
                })
                .thenWaitUntil(() -> helper.assertTrue(Portables.battery(dock.held(), PortableModel.QPHONE) > 0
                        && dock.isCharging(), "fed from behind, it charges"))
                .thenSucceed();
    }

    /** A dock holds a portable of its era alone; one of another era is left in the hand. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void dock_refusesAPortableOfAnotherEra(final GameTestHelper helper) {
        final ChargingDockBlockEntity dock = dock(helper, PortableItems.TRANSITION_CHARGING_DOCK.get());
        final ServerPlayer player = player(helper, DOCK.south(2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortableItems.HIGHTIDE_POCKET.get()));
        helper.useBlock(DOCK, player);
        helper.assertTrue(dock.held().isEmpty(), "a Legacy PDA does not go in a Transition dock");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortableItems.HIGHTIDE_POCKET_PRO.get()));
        helper.useBlock(DOCK, player);
        helper.assertTrue(dock.held().is(PortableItems.HIGHTIDE_POCKET_PRO.get())
                && player.getMainHandItem().isEmpty(), "a Transition one does");
        helper.succeed();
    }

    /**
     * A portable stood in a dock is switched off and charged; taken out with an empty hand, it comes on again once it
     * is carried; a dock broken lets it fall.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void dock_switchesItOffAndGivesItBack(final GameTestHelper helper) {
        final ChargingDockBlockEntity dock = dock(helper, PortableItems.STANDARD_CHARGING_DOCK.get());
        final ServerPlayer player = player(helper, DOCK.south(2));
        final ItemStack phone = carry(player, PortableItems.QPHONE_5.get());
        Portables.setBattery(phone, PortableModel.QPHONE_5, PortableBattery.capacity(PortableModel.QPHONE_5) / 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, phone);
        helper.useBlock(DOCK, player);
        helper.assertFalse(Portables.isOn(dock.held()), "switched off in the dock");
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(Portables.battery(dock.held(), PortableModel.QPHONE_5)
                            > PortableBattery.capacity(PortableModel.QPHONE_5) / 2, "charging");
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    helper.useBlock(DOCK, player);
                    helper.assertTrue(dock.held().isEmpty(), "taken out");
                    final ItemStack back = player.getInventory().items.stream()
                            .filter(stack -> stack.getItem() instanceof PortableItem).findFirst()
                            .orElse(ItemStack.EMPTY);
                    tick(player, back);
                    helper.assertTrue(Portables.isOn(back), "on again once carried");
                    dock.put(new ItemStack(PortableItems.QPAD_AIR.get()));
                    helper.setBlock(DOCK, Blocks.AIR);
                })
                .thenExecuteAfter(2, () -> {
                    final AABB around = new AABB(helper.absolutePos(DOCK)).inflate(2.0);
                    helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                                    .anyMatch(item -> item.getItem().is(PortableItems.QPAD_AIR.get())),
                            "a dock broken lets its portable fall");
                })
                .thenSucceed();
    }

    /** A dock takes its energy through its back alone. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void dock_takesItsEnergyBehindItAlone(final GameTestHelper helper) {
        dock(helper, PortableItems.ADVANCED_CHARGING_DOCK.get());
        final ChargingDockBlock block = PortableItems.ADVANCED_CHARGING_DOCK.get();
        helper.assertTrue(block.accepts(helper.getBlockState(DOCK), Direction.WEST, SharedEnergy.LOW_VOLTAGE_LINE),
                "behind it");
        helper.assertFalse(block.accepts(helper.getBlockState(DOCK), Direction.EAST, SharedEnergy.LOW_VOLTAGE_LINE),
                "not in front");
        helper.succeed();
    }

    /* A running Mainframe, a router onto the access line, and a Transition desk access point at its end. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        helper.setBlock(ACCESS_POINT, ComputingModule.TRANSITION_ACCESS_POINT.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(AccessPointBlock.MOUNT, AccessPointMount.DESK));
        return mainframe;
    }

    /* A dock of {@code block} at its place, facing east, its back to the west. */
    private static ChargingDockBlockEntity dock(final GameTestHelper helper, final ChargingDockBlock block) {
        helper.setBlock(DOCK, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        return TestWorldBuilder.forGameTest(helper).blockEntity(DOCK, ChargingDockBlockEntity.class);
    }

    /* A fake player of the test's own, standing at {@code at}, with nothing on them. */
    private static ServerPlayer player(final GameTestHelper helper, final BlockPos at) {
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "portable-test"));
        player.getInventory().clearContent();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(at)));
        return player;
    }

    /* A new device of {@code item} in {@code player}'s inventory, carried once. */
    private static ItemStack carry(final ServerPlayer player, final PortableItem item) {
        final ItemStack stack = new ItemStack(item);
        player.getInventory().add(stack);
        final ItemStack held = player.getInventory().items.stream().filter(found -> found.is(item))
                .reduce((first, second) -> second).orElse(stack);
        tick(player, held);
        return held;
    }

    /* The game's tick of a carried device. */
    private static void tick(final ServerPlayer player, final ItemStack stack) {
        stack.getItem().inventoryTick(stack, player.level(), player, 0, false);
    }

    /* Installs {@code app} on the device {@code device}, as its store does. */
    private static void install(final GameTestHelper helper, final ServerPlayer player, final UUID device,
                                final MobileApp app) {
        Portables.act(PortableActionPayload.named(device, PortableActionPayload.Action.INSTALL,
                app.serializedName()), player, helper.getLevel());
    }
}
