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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.operation.payload.portable.PortableMapPayload;
import dev.jstech.computers.operation.payload.portable.PortableNetworkPayload;
import dev.jstech.computers.operation.payload.portable.PortableProspectPayload;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.portable.AlertKind;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableBuildSource;
import dev.jstech.computers.portable.PortableDevices;
import dev.jstech.computers.portable.PortableGallery;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.PortableMaps;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.portable.PortableNetwork;
import dev.jstech.computers.portable.PortablePicture;
import dev.jstech.computers.portable.PortableState;
import dev.jstech.computers.portable.PortableStorage;
import dev.jstech.computers.portable.Portables;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.jstechseries.builder.BuildJobs;
import dev.jstech.core.jstechseries.builder.BuilderPlan;
import dev.jstech.core.jstechseries.builder.BuilderState;
import dev.jstech.core.jstechseries.builder.SourceAskPayload;
import dev.jstech.core.jstechseries.builder.SourceCounts;
import dev.jstech.core.jstechseries.builder.SourceCountsPayload;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.text.Text;
import dev.jstech.core.tool.BuildSources;
import dev.jstech.core.worldgen.vein.Prospecting;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestStructures;
import dev.jstech.tests.TestTools;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The portables' apps (computers/docs/design/wireless.md): the network app requests items into the device's own
 * storage, as much as its room takes, and takes them to the inventory or returns them, and deposits the inventory; the
 * alerts reach the devices on the network that take their kind, a watched item below its level is told once, a failed
 * Operation is told; the Messenger is the network's; the gallery keeps a picture and sends it to a personal computer as
 * a PNG file; the maps mark the Mainframe and the access points; a device lost is let go; and a player carrying a
 * portable on a network builds from its storage, with J's Core's building tools and Multiblock Builder, only while an
 * access point reaches them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PortableAppsGameTests {

    private static final String ARENA = "empty";
    /* A Mainframe, a server stocked beside its cable, a router, the access line and an access point at its end. */
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    private static final BlockPos ACCESS_POINT = new BlockPos(5, 2, 2);
    private static final BlockPos COMPUTER = new BlockPos(4, 2, 3);
    private static final BlockPos NEAR = new BlockPos(5, 2, 6);
    /* The test baker's terminal, its box of three running south of it, clear of the network. */
    private static final BlockPos BAKER = new BlockPos(3, 3, 6);
    /* Far enough east that no access point reaches the player. */
    private static final int FAR = 400;

    private PortableAppsGameTests() {
    }

    /** A request fills the device's own storage from the network; Take brings it to the inventory, Return back. */
    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void network_requestsIntoTheDeviceThenTakesAndReturns(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        base.rack().getServerStorage(0).insert(Items.IRON_INGOT, 64);
        final ServerPlayer player = player(helper);
        final ItemStack phone = hold(player, PortableItems.QPHONE_5.get());
        final UUID id = Portables.id(phone);
        final StorageKey iron = StorageKey.of(Items.IRON_INGOT);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> act(helper, player, PortableActionPayload.item(id, Action.REQUEST, iron, 16)))
                .thenWaitUntil(() -> helper.assertValueEqual(devices.find(id).stored().getOrDefault(iron, 0L), 16L,
                        "the request lands in the device's own storage"))
                .thenExecute(() -> {
                    helper.assertValueEqual(count(player, Items.IRON_INGOT), 0, "never straight into the inventory");
                    act(helper, player, PortableActionPayload.item(id, Action.TAKE, iron, 10));
                    helper.assertValueEqual(count(player, Items.IRON_INGOT), 10, "Take brings it to the inventory");
                    helper.assertValueEqual(devices.find(id).stored().getOrDefault(iron, 0L), 6L, "six stay");
                    act(helper, player, PortableActionPayload.item(id, Action.RETURN, iron, 6));
                })
                .thenWaitUntil(() -> helper.assertValueEqual(stock(helper, base, iron), 54L,
                        "Return gives the rest back to the network"))
                .thenExecute(() -> helper.assertTrue(devices.find(id).stored().isEmpty(), "the device is empty"))
                .thenSucceed();
    }

    /** A device takes no more than its room: the Hightide Pocket holds four items. */
    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void network_requestStopsAtTheDevicesRoom(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        base.rack().getServerStorage(0).insert(Items.REDSTONE, 64);
        final ServerPlayer player = player(helper);
        final ItemStack pda = hold(player, PortableItems.HIGHTIDE_POCKET.get());
        final UUID id = Portables.id(pda);
        final StorageKey redstone = StorageKey.of(Items.REDSTONE);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, pda))
                .thenExecute(() -> {
                    final PortableNetworkPayload view = PortableNetwork.viewOf(helper.getLevel(), id, devices.find(id));
                    helper.assertTrue(view.online() && view.capacity() == 4 && view.free() == 4
                            && view.stock().stream().anyMatch(entry -> entry.key().equals(redstone)),
                            "the app sees the network's stock and four items of room; got " + view);
                    act(helper, player, PortableActionPayload.item(id, Action.REQUEST, redstone, 16));
                })
                .thenWaitUntil(() -> helper.assertValueEqual(devices.find(id).stored().getOrDefault(redstone, 0L),
                        4L, "four, as its room holds"))
                .thenExecuteAfter(20, () -> helper.assertValueEqual(stock(helper, base, redstone), 60L,
                        "the rest stays in the network"))
                .thenSucceed();
    }

    /** The Deposit hands a row of the inventory to the network. */
    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void network_depositsARowOfTheInventory(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        final ServerPlayer player = player(helper);
        final ItemStack phone = hold(player, PortableItems.QPHONE_5.get());
        final UUID id = Portables.id(phone);
        player.getInventory().setItem(20, new ItemStack(Items.COBBLESTONE, 20));
        final StorageKey cobblestone = StorageKey.of(Items.COBBLESTONE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> act(helper, player, PortableActionPayload.numbered(id, Action.DEPOSIT, "", 20)))
                .thenWaitUntil(() -> helper.assertValueEqual(stock(helper, base, cobblestone), 20L,
                        "the row went into the network"))
                .thenExecute(() -> {
                    helper.assertTrue(player.getInventory().getItem(20).isEmpty(), "and left the row");
                    player.getInventory().setItem(3, new ItemStack(Items.COBBLESTONE, 12));
                    player.getInventory().setItem(30, new ItemStack(Items.COBBLESTONE, 8));
                    act(helper, player, PortableActionPayload.of(id, Action.DEPOSIT_ALL));
                })
                .thenWaitUntil(() -> helper.assertValueEqual(stock(helper, base, cobblestone), 40L,
                        "Deposit all takes every row, the hotbar's too"))
                .thenExecute(() -> helper.assertTrue(player.getMainHandItem().getItem() instanceof PortableItem,
                        "never the portable itself"))
                .thenSucceed();
    }

    /** An alert from the log reaches the devices on the network that take its kind, and no others. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void alerts_reachTheDevicesThatTakeTheirKind(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        final ServerPlayer player = player(helper);
        final ItemStack phone = hold(player, PortableItems.QPHONE_5.get());
        final UUID id = Portables.id(phone);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    act(helper, player, PortableActionPayload.named(id, Action.TOGGLE_ALERT,
                            AlertKind.UPS.serializedName()));
                    NetworkEvents.alert(helper.getLevel(), base.mainframe().networkUuid(), EventLevel.WARN,
                            AlertKind.UPS, null, "", Text.literal("on its battery"));
                    helper.assertTrue(devices.find(id).alerts().isEmpty(), "a kind switched off is not told");
                    NetworkEvents.alert(helper.getLevel(), base.mainframe().networkUuid(), EventLevel.WARN,
                            AlertKind.POWER, null, "", Text.literal("worn"));
                    final List<PortableAlert> alerts = devices.find(id).alerts();
                    helper.assertTrue(alerts.size() == 1 && alerts.getFirst().kind() == AlertKind.POWER
                            && !alerts.getFirst().seen(), "a kind it takes is; got " + alerts);
                    base.mainframe().recordOperation(OperationRecord.TYPE_SELECT, new ItemStack(Items.IRON_INGOT),
                            16, 0, (byte) OperationStatus.FAILED.id(), List.of());
                    helper.assertTrue(devices.find(id).alerts().getFirst().kind() == AlertKind.OPERATIONS,
                            "an Operation that failed is told");
                })
                .thenSucceed();
    }

    /** A watched item below its level is told once, and only again after it came back up to its level. */
    @GameTest(template = ARENA, timeoutTicks = 1000)
    public static void alerts_aWatchedItemBelowItsLevelIsToldOnce(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        base.rack().getServerStorage(0).insert(Items.IRON_INGOT, 64);
        final ServerPlayer player = player(helper);
        final ItemStack phone = hold(player, PortableItems.SUNSUNG_NEBULA_S10.get());
        final UUID id = Portables.id(phone);
        final StorageKey iron = StorageKey.of(Items.IRON_INGOT);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> act(helper, player, PortableActionPayload.item(id, Action.WATCH, iron, 128)))
                .thenExecuteFor(140, () -> tick(player, phone))
                .thenExecute(() -> helper.assertValueEqual(itemAlerts(devices, id), 1L,
                        "64 iron, watched below 128, is told"))
                .thenExecuteFor(240, () -> tick(player, phone))
                .thenExecute(() -> helper.assertValueEqual(itemAlerts(devices, id), 1L, "once"))
                .thenSucceed();
    }

    /** The Messenger on a portable is the network's: what is said there lands in the server's room. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void messenger_speaksInTheNetworksService(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        base.rack().unitHost(0).installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
        final ServerPlayer player = player(helper);
        final ItemStack phone = hold(player, PortableItems.HIGHTIDE_WAVE.get());
        final UUID id = Portables.id(phone);
        PortableDevices.get(helper.getLevel().getServer()).install(id, MobileApp.MESSENGER);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecute(() -> base.rack().consoleOf(0).install(Programs.MESSENGER_SERVICE.toString()))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    act(helper, player, new PortableActionPayload(id, Action.SAY, MessengerLog.LOBBY,
                            "the furnace line is stuck", Optional.empty(), 0L));
                    final MessengerLog log = base.rack().messengerAt(0);
                    helper.assertTrue(log != null && log.room(MessengerLog.LOBBY, 4).stream().anyMatch(message ->
                                    "the furnace line is stuck".equals(message.text())),
                            "said in the network's lobby");
                })
                .thenSucceed();
    }

    /**
     * A camera's picture at its era's size joins the gallery, with its small copy, taking room by its bytes; sent to
     * a personal computer of the network, it is a PNG file in its pictures folder naming the picture the store keeps.
     */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void gallery_keepsAPictureAndSendsItAsAPngFile(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(COMPUTER);
        final ServerPlayer player = player(helper);
        final ItemStack phone = hold(player, PortableItems.QPHONE.get());
        final UUID id = Portables.id(phone);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        final MediaId media = keep(png(PortableModel.QPHONE.cameraWidth(), PortableModel.QPHONE.cameraHeight()));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null
                        && pc.networkUuid() != null, "the network and the computer are up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    helper.assertTrue(new PortableGallery().refuse(player, id.toString(), "picture.png", media) == null,
                            "a picture for the device in the hand is taken");
                    new PortableGallery().received(player, id.toString(), "picture.png", media, MediaInfo.STILL);
                })
                .thenWaitUntil(() -> helper.assertValueEqual(devices.find(id).pictures().size(), 1,
                        "the gallery keeps it"))
                .thenExecute(() -> {
                    final PortableState state = devices.find(id);
                    final PortablePicture picture = state.pictures().getFirst();
                    helper.assertTrue(picture.width() == 640 && picture.height() == 480
                                    && picture.thumbnail().length > 0,
                            "640 by 480, with its small copy; got " + picture.width() + "x" + picture.height());
                    helper.assertValueEqual(state.used(), PortableStorage.pictureWeight(PortableModel.QPHONE,
                            media.bytes()), "it takes room by its bytes");
                    act(helper, player, PortableActionPayload.numbered(id, Action.SEND_PICTURE,
                            pc.nodeUuid().asString(), picture.number()));
                    final String path = SystemLayout.PICTURES_DIR + "/" + picture.fileName();
                    final Optional<String> file = DiskFilesystem.read(pc.systemDisk(), path);
                    final RecordingFile named = file.map(RecordingFile::read).orElse(null);
                    helper.assertTrue(named != null && named.media().equals(media),
                            "a PNG file naming the picture lands in the computer's pictures; got " + file);
                })
                .thenSucceed();
    }

    /** The maps mark the network's Mainframe and its access point with its reach. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void maps_markTheMainframeAndTheAccessPoints(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        final ServerPlayer player = player(helper);
        final ItemStack phone = hold(player, PortableItems.QPHONE_X.get());
        final UUID id = Portables.id(phone);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    final PortableMapPayload map = PortableMaps.mapOf(helper.getLevel(), id);
                    helper.assertTrue(map.mainframe().isPresent()
                                    && map.mainframe().get().equals(base.mainframe().getBlockPos()),
                            "the Mainframe is marked; got " + map.mainframe());
                    helper.assertTrue(map.points().size() == 1 && map.points().getFirst().pos()
                                    .equals(helper.absolutePos(ACCESS_POINT))
                                    && map.points().getFirst().reach() == WirelessStandard.TRANSITION.reachBlocks(),
                            "the access point, with its reach; got " + map.points());
                    final PortableProspectPayload veins = PortableMaps.prospectOf(helper.getLevel(), id,
                            player.blockPosition());
                    final int near = Prospecting.prospect(helper.getLevel(), player.blockPosition(),
                            PortableMaps.PROSPECT_RADIUS).size();
                    helper.assertTrue(veins.veins().size() == Math.min(near, PortableProspectPayload.MOST_VEINS),
                            "prospecting lists the veins near, as many as fit, of " + near + "; got " + veins.veins());
                    for (int i = 1; i < veins.veins().size(); i++) {
                        helper.assertTrue(veins.veins().get(i).strength() <= veins.veins().get(i - 1).strength(),
                                "the strongest first; got " + veins.veins());
                    }
                })
                .thenSucceed();
    }

    /** Prospecting tells which way a vein lies as a compass reads it, clockwise from north. */
    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void prospect_bearingsReadAsACompass(final GameTestHelper helper) {
        helper.assertValueEqual(PortableMaps.bearing(new Vec3(0, 0, -1)), 0.0F, "north");
        helper.assertValueEqual(PortableMaps.bearing(new Vec3(1, 0, 0)), 90.0F, "east");
        helper.assertValueEqual(PortableMaps.bearing(new Vec3(0, 0, 1)), 180.0F, "south");
        helper.assertValueEqual(PortableMaps.bearing(new Vec3(-1, 0, 0)), 270.0F, "west");
        helper.succeed();
    }

    /** A device burned, fallen into the void or despawned is let go with all that was on it. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void lost_aDestroyedDeviceIsLetGo(final GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        final ItemStack burned = hold(player, PortableItems.QPHONE.get());
        final ItemStack fallen = hold(player, PortableItems.QPHONE_5.get());
        final ItemStack despawned = hold(player, PortableItems.QPHONE_X.get());
        final ItemEntity fire = drop(helper, burned.copy(), NEAR);
        fire.hurt(helper.getLevel().damageSources().inFire(), 10);
        helper.assertTrue(devices.find(Portables.id(burned)) == null, "burned, it is let go");
        final ItemEntity voided = drop(helper, fallen.copy(), NEAR);
        voided.setPos(voided.getX(), helper.getLevel().getMinBuildHeight() - 70, voided.getZ());
        voided.tick();
        helper.assertTrue(devices.find(Portables.id(fallen)) == null, "fallen into the void, it is let go");
        final ItemEntity old = drop(helper, despawned.copy(), NEAR);
        old.lifespan = 1;
        old.tick();
        old.tick();
        helper.assertTrue(devices.find(Portables.id(despawned)) == null, "despawned, it is let go");
        helper.succeed();
    }

    /** A device out of every inventory and dock longer than the server allows is let go; one seen since stays. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void lost_aDeviceUnseenTooLongIsLetGo(final GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        final ItemStack kept = hold(player, PortableItems.QPHONE.get());
        final UUID lost = Portables.id(hold(player, PortableItems.QPHONE_5.get()));
        final long now = helper.getLevel().getGameTime();
        devices.seen(Portables.id(kept), now + 10, player.getUUID());
        final List<UUID> gone = devices.eraseUnseenSince(now + 5, everyone -> true);
        helper.assertTrue(gone.contains(lost) && devices.find(lost) == null, "the unseen one is let go");
        helper.assertTrue(devices.find(Portables.id(kept)) != null, "the one seen since stays");
        helper.succeed();
    }

    /**
     * A device last in the inventory of a player who is away is in their inventory still, and one holding a wallet is
     * never let go, however long either goes unseen.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void lost_aDeviceOfAPlayerAwayOrHoldingAWalletStays(final GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        final PortableDevices devices = PortableDevices.get(helper.getLevel().getServer());
        final UUID carried = Portables.id(hold(player, PortableItems.QPHONE.get()));
        final UUID wallet = Portables.id(hold(player, PortableItems.QPHONE_5.get()));
        devices.keepWallet(wallet, "TRC1away", "key");
        final long now = helper.getLevel().getGameTime();
        devices.eraseUnseenSince(now + 5, nobody -> false);
        helper.assertTrue(devices.find(carried) != null, "its player away, the device they carried stays");
        final List<UUID> gone = devices.eraseUnseenSince(now + 5, everyone -> true);
        helper.assertTrue(gone.contains(carried) && devices.find(carried) == null,
                "with its player back and the device still unseen, it is let go");
        helper.assertTrue(devices.find(wallet) != null, "while the one holding a wallet always stays");
        helper.succeed();
    }

    /** A Multiblock Builder set to the network's storage builds from it while a portable joined to it is carried. */
    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void build_builderTakesFromTheNetworkThroughACarriedPortable(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        base.rack().getServerStorage(0).insert(Items.IRON_BLOCK, 64);
        final ServerPlayer player = player(helper);
        final ItemStack phone = carry(player, PortableItems.QPHONE_5.get());
        helper.setBlock(BAKER, TestStructures.BAKER.get());
        final BlockPos terminal = helper.absolutePos(BAKER);
        final BuilderState fromNetwork = BuilderState.DEFAULT.from(PortableBuildSource.ID);
        final StorageKey iron = StorageKey.of(Items.IRON_BLOCK);
        final ResourceLocation ironId = BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK);
        final int wanted = BuilderPlan.of(helper.getLevel(), terminal, player, fromNetwork).orElseThrow().wants()
                .size();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    final SourceAskPayload ask = new SourceAskPayload(PortableBuildSource.ID, List.of(ironId));
                    final SourceCountsPayload told = SourceCounts.answer(player, ask).orElseThrow();
                    helper.assertValueEqual(told.counts().getOrDefault(ironId, 0L), 64L,
                            "the server tells the builder's display what the network holds");
                    helper.assertTrue(SourceCounts.answer(player, ask).isEmpty(),
                            "and does not answer the same player again within a few ticks");
                    helper.assertTrue(BuilderPlan.of(helper.getLevel(), terminal, player, fromNetwork).orElseThrow()
                            .order().ready(), "the network's iron builds it all");
                    BuildJobs.start(player, terminal, fromNetwork);
                })
                .thenWaitUntil(() -> {
                    // The portable keeps saying it is on the network while the builder works.
                    tick(player, phone);
                    helper.assertTrue(BuilderPlan.of(helper.getLevel(), terminal, player, fromNetwork).orElseThrow()
                            .wants().isEmpty() && BuildJobs.building(player).isEmpty(), "still building");
                })
                .thenExecute(() -> helper.assertValueEqual(stock(helper, base, iron), 64L - wanted,
                        "each block placed came out of the network's storage"))
                .thenSucceed();
    }

    /** A building tool takes from the network's storage what the inventory lacks. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void build_toolTakesFromTheNetworkWhatTheInventoryLacks(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        base.rack().getServerStorage(0).insert(Items.STONE, 10);
        final ServerPlayer player = player(helper);
        final ItemStack phone = carry(player, PortableItems.QPHONE_5.get());
        final ItemStack wand = new ItemStack(TestTools.WAND.get());
        ItemStates.setMode(wand, TestTools.LINE);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        for (int x = 2; x <= 4; x++) {
            helper.setBlock(x, 2, 8, Blocks.STONE);
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    final BlockPos at = helper.absolutePos(new BlockPos(3, 2, 8));
                    player.gameMode.useItemOn(player, helper.getLevel(), wand, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(at).add(0, 0, -0.5), Direction.NORTH, at, false));
                    for (int x = 2; x <= 4; x++) {
                        helper.assertBlockPresent(Blocks.STONE, x, 2, 7);
                    }
                    helper.assertValueEqual(stock(helper, base, StorageKey.of(Items.STONE)), 7L,
                            "the line of three came out of the network, the inventory having none");
                })
                .thenSucceed();
    }

    /** Out of every access point's reach the network's storage is no place to build from, though it holds the block. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void build_networkIsNoSourceOutOfTheAccessPointsReach(final GameTestHelper helper) {
        final Stocked base = stocked(helper);
        base.rack().getServerStorage(0).insert(Items.STONE, 10);
        final ServerPlayer player = player(helper);
        final ItemStack phone = carry(player, PortableItems.QPHONE_5.get());
        final PortableBuildSource network = new PortableBuildSource();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(NEAR)).add(FAR, 0, 0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.mainframe().networkUuid() != null, "the network is up"))
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    helper.assertFalse(network.usable(player), "no access point reaches the player");
                    helper.assertValueEqual(BuildSources.available(player, Items.STONE), 0L,
                            "so nothing of the network's is there to build with");
                    helper.assertTrue(BuildSources.takeOne(player, Items.STONE) == null, "and nothing is taken");
                    player.setPos(Vec3.atCenterOf(helper.absolutePos(NEAR)));
                })
                .thenExecuteFor(Portables.LOOK_EVERY * 2, () -> tick(player, phone))
                .thenExecute(() -> {
                    helper.assertTrue(network.usable(player), "back in reach, the network is a source again");
                    helper.assertValueEqual(BuildSources.available(player, Items.STONE), 10L, "with its ten stone");
                    helper.assertValueEqual(stock(helper, base, StorageKey.of(Items.STONE)), 10L,
                            "none was taken while it was out of reach");
                })
                .thenSucceed();
    }

    /* The network of the tests: a running Mainframe, a stocked server beside its cable, the access line and point. */
    private static Stocked stocked(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        final ServerRackBlockEntity rack = world.placeSeededRack(RACK);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        helper.setBlock(ACCESS_POINT, ComputingModule.TRANSITION_ACCESS_POINT.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(AccessPointBlock.MOUNT, AccessPointMount.DESK));
        return new Stocked(mainframe, rack);
    }

    /* A fake player of the test's own, standing near the access point, with nothing on them. */
    private static ServerPlayer player(final GameTestHelper helper) {
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "portable-test"));
        player.getInventory().clearContent();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(NEAR)));
        return player;
    }

    /* A new device of {@code item} in {@code player}'s main hand, carried once, so it is on. */
    private static ItemStack hold(final ServerPlayer player, final PortableItem item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        final ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        tick(player, held);
        return held;
    }

    /* A new device of {@code item} at the end of {@code player}'s hotbar, carried once, so it is on; the hand free. */
    private static ItemStack carry(final ServerPlayer player, final PortableItem item) {
        player.getInventory().setItem(Inventory.getSelectionSize() - 1, new ItemStack(item));
        final ItemStack carried = player.getInventory().getItem(Inventory.getSelectionSize() - 1);
        tick(player, carried);
        return carried;
    }

    /* The game's tick of a carried device. */
    private static void tick(final ServerPlayer player, final ItemStack stack) {
        stack.getItem().inventoryTick(stack, player.level(), player, 0, true);
    }

    private static void act(final GameTestHelper helper, final ServerPlayer player,
                            final PortableActionPayload action) {
        Portables.act(action, player, helper.getLevel());
    }

    private static int count(final ServerPlayer player, final Item item) {
        return player.getInventory().countItem(item);
    }

    private static long stock(final GameTestHelper helper, final Stocked base, final StorageKey key) {
        return NetworkStorage.of(helper.getLevel(), base.mainframe().networkUuid()).query().getOrDefault(key, 0L);
    }

    private static long itemAlerts(final PortableDevices devices, final UUID id) {
        return devices.find(id).alerts().stream().filter(alert -> alert.kind() == AlertKind.ITEMS).count();
    }

    private static ItemEntity drop(final GameTestHelper helper, final ItemStack stack, final BlockPos at) {
        final Vec3 where = Vec3.atCenterOf(helper.absolutePos(at));
        final ItemEntity entity = new ItemEntity(helper.getLevel(), where.x, where.y, where.z, stack);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    /* A grey PNG {@code w} by {@code h}, as a camera of that size writes one. */
    private static byte[] png(final int w, final int h) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (final IOException unwritable) {
            throw new IllegalStateException(unwritable);
        }
    }

    /* Puts {@code bytes} in the server's store, as a camera's upload does. */
    private static MediaId keep(final byte[] bytes) {
        try {
            return MediaStore.current().orElseThrow(() -> new IllegalStateException("no media store"))
                    .put(bytes, "png");
        } catch (final IOException unwritable) {
            throw new IllegalStateException(unwritable);
        }
    }

    /* The network of the tests: its Mainframe and the rack whose server holds its stock. */
    private record Stocked(MainframeBlockEntity mainframe, ServerRackBlockEntity rack) {
    }
}
