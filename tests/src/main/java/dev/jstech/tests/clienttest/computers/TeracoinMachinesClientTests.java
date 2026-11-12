/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.AsicBlock;
import dev.jstech.computers.block.MiningComputerBlock;
import dev.jstech.computers.blockentity.AsicBlockEntity;
import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.client.AsicScreen;
import dev.jstech.computers.client.MiningComputerScreen;
import dev.jstech.computers.client.Printer3DScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.teracoin.FoundBlock;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.lwjgl.glfw.GLFW;

/**
 * Teracoin's machines in their own windows (computers/docs/design/teracoin.md), each as its mock shows it: the Forge
 * S1 and A2 ASICs mining alone to a wallet's address, a mining rig of each age with its board, memory, supply and
 * disk and cards on its risers, and the two 3D printers printing a wallet's order of diamonds.
 */
public final class TeracoinMachinesClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    /* Long enough for a machine's window to be told how it runs and to draw it. */
    private static final int SHOWN = 40;
    private static final int MINING_WAIT = 400;
    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    /* The player stands south of the machine, which faces them. */
    private static final BlockPos PLAYER = new BlockPos(5, 2, 5);
    /* The computer the printer links to, beside it. */
    private static final BlockPos COMPUTER = MACHINE.east();
    private static final int COPIES = 12;
    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    private TeracoinMachinesClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void asic_forgeS1_minesAloneToItsAddress(final ClientTestContext ctx) {
        asic(ctx, ComputingModule.ASIC_FORGE_S1, "asic-forge-s1");
    }

    @ClientTest(timeoutTicks = 1200)
    public static void asic_forgeA2_minesAloneToItsAddress(final ClientTestContext ctx) {
        asic(ctx, ComputingModule.ASIC_FORGE_A2, "asic-forge-a2");
    }

    @ClientTest(timeoutTicks = 900)
    public static void rig_transition_showsItsCardsOnItsRisers(final ClientTestContext ctx) {
        rig(ctx, new Rig(ComputingModule.MINING_COMPUTER_TRANSITION, HardwareItems.MOTHERBOARD_MNG_TRANSITION,
                HardwareItems.CPU_INTEGRA_PENTIX_D_805, HardwareItems.RAM_DDR2_1024, HardwareItems.PSU_500B,
                HardwareItems.MINING_RISER_TRANSITION, HardwareItems.GPU_VERTEX_7600_GT), "rig-transition");
    }

    @ClientTest(timeoutTicks = 900)
    public static void rig_standard_showsItsCardsOnItsRisers(final ClientTestContext ctx) {
        rig(ctx, new Rig(ComputingModule.MINING_COMPUTER_STANDARD, HardwareItems.MOTHERBOARD_MNG_STANDARD,
                HardwareItems.CPU_INTEGRA_PENTIX_G3258, HardwareItems.RAM_DDR3_4096, HardwareItems.PSU_850G,
                HardwareItems.MINING_RISER_STANDARD, HardwareItems.GPU_VERTEX_GT_730), "rig-standard");
    }

    @ClientTest(timeoutTicks = 900)
    public static void rig_advanced_showsItsCardsOnItsRisers(final ClientTestContext ctx) {
        rig(ctx, new Rig(ComputingModule.MINING_COMPUTER_ADVANCED, HardwareItems.MOTHERBOARD_MNG_ADVANCED,
                HardwareItems.CPU_INTEGRA_PENTIX_G4560, HardwareItems.RAM_DDR4_8192, HardwareItems.PSU_1000G,
                HardwareItems.MINING_RISER_ADVANCED, HardwareItems.GPU_RADIANCE_RX_550), "rig-advanced");
    }

    @ClientTest(timeoutTicks = 1500)
    public static void printer3d_prosa_printsAWalletsOrder(final ClientTestContext ctx) {
        printer(ctx, ComputingModule.PROSA_I3_MK3, "printer3d-prosa");
    }

    @ClientTest(timeoutTicks = 1500)
    public static void printer3d_rattan_printsAWalletsOrder(final ClientTestContext ctx) {
        printer(ctx, ComputingModule.RATTAN_LAB_X1, "printer3d-rattan");
    }

    /* An ASIC facing the player, paying a new wallet's address alone and fed its watts, its window open. */
    private static void asic(final ClientTestContext ctx, final BlockEntry<AsicBlock> block, final String label) {
        ctx.afterTest(TestComputerEnergy::letGo);
        // An empty inventory under the window, as its mock draws it; another test may have left something in it.
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> world.setBlock(MACHINE, block.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH)))
                .thenServer(SETTLE, level -> {
                    final AsicBlockEntity asic = asicAt(ctx, level);
                    asic.setAddress(TeracoinState.of(level.getServer()).chain().createWallet(RANDOM).address());
                    asic.setAlone();
                    TestComputerEnergy.keepFed(asic.energyPort());
                })
                .thenWaitUntilServer(level -> asicAt(ctx, level).mining(), MINING_WAIT, "the ASIC to mine on its watts",
                        level -> "status=" + asicAt(ctx, level).status())
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenRightClick(SETTLE, MACHINE)
                .thenAwaitScreen(AsicScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SHOWN, label)
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /* A rig of one age with its board, memory, supply and disk, every riser seated, four of them carrying a card. */
    private static void rig(final ClientTestContext ctx, final Rig parts, final String label) {
        // An empty inventory under the window, as its mock draws it; another test may have left something in it.
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> {
                    world.setBlock(MACHINE, parts.block().get().defaultBlockState()
                            .setValue(MiningComputerBlock.FACING, Direction.SOUTH));
                    final MiningComputerBlockEntity rig = world.blockEntity(MACHINE, MiningComputerBlockEntity.class);
                    rig.getHardware().setStackInSlot(MiningComputerBlockEntity.MOTHERBOARD_SLOT, stack(parts.board()));
                    rig.getHardware().setStackInSlot(MiningComputerBlockEntity.CPU_SLOT, stack(parts.cpu()));
                    rig.getHardware().setStackInSlot(MiningComputerBlockEntity.RAM_SLOTS_START, stack(parts.ram()));
                    rig.getHardware().setStackInSlot(MiningComputerBlockEntity.RAM_SLOTS_START + 1,
                            stack(parts.ram()));
                    rig.getHardware().setStackInSlot(MiningComputerBlockEntity.PSU_SLOT, stack(parts.psu()));
                    rig.getHardware().setStackInSlot(MiningComputerBlockEntity.DISK_SLOT,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    for (int i = 0; i < rig.riserCount(); i++) {
                        rig.risers().setStackInSlot(i, stack(parts.riser()));
                    }
                    for (int i = 0; i < Math.min(4, rig.riserCount()); i++) {
                        final TextKey refused = rig.seatRefusal(i, stack(parts.card()));
                        if (refused != null) {
                            throw new IllegalStateException("the card is refused riser " + i + ": "
                                    + GameText.resolve(refused));
                        }
                        rig.seat(i, stack(parts.card()));
                    }
                })
                .thenWaitUntilServer(level -> rigAt(ctx, level).seatedCards() == Math.min(4, rigAt(ctx, level)
                        .riserCount()), SCREEN_WAIT, "four cards to stand on the rig's risers", level -> {
                            final MiningComputerBlockEntity rig = rigAt(ctx, level);
                            return "risers=" + rig.riserCount() + " riser0=" + rig.hasRiser(0) + " card0="
                                    + rig.cardOn(0) + " part0=" + rig.cardPart(0) + " off=" + rig.switchedOff();
                        })
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenServer(SETTLE, level -> MiningComputerBlock.open(ctx.serverPlayer(), ctx.abs(MACHINE),
                        (MiningComputerBlockEntity) level.getBlockEntity(ctx.abs(MACHINE))))
                .thenAwaitScreen(MiningComputerScreen.class, SCREEN_WAIT)
                // The screen's slots fill the client's copy of the rig; its cards, on no slot, must stay on it.
                .thenWaitUntil(() -> ctx.screen(MiningComputerScreen.class).getMenu().seatedCards() == 4,
                        SCREEN_WAIT, "the screen to show the four cards on the risers")
                .then(SETTLE, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SHOWN, label)
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /*
     * A 3D printer facing the player beside a running computer whose wallet has the TRC for twelve diamonds, the
     * order sent from that wallet and its window open as it prints.
     */
    private static void printer(final ClientTestContext ctx, final BlockEntry<? extends Block> block,
                                final String label) {
        // An empty inventory under the window, as its mock draws it; another test may have left something in it.
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER);
                    world.setBlock(MACHINE, block.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                })
                .thenWaitUntilServer(level -> pcAt(ctx, level).enabledEndpoints().contains(ctx.abs(MACHINE).asLong()),
                        SCREEN_WAIT, "the 3D printer to link to the computer beside it", level -> "")
                .thenServer(SETTLE, level -> {
                    final PersonalComputerBlockEntity pc = pcAt(ctx, level);
                    final TeracoinState state = TeracoinState.of(level.getServer());
                    final TeracoinChain.Keys keys = state.chain().createWallet(RANDOM);
                    final String path = WalletFiles.write(pc.services().files(), WalletFiles.folder(pc), "main",
                            keys, (char) 0);
                    state.chain().creditAlone(new FoundBlock(state.chain().height(), level.getGameTime(), 0L,
                            Trc.of(1_000), 1L, "test", "test", keys.address(), "", false));
                    state.setDirty();
                    printerAt(ctx, level).submit(Items.DIAMOND, COPIES, pc.getBlockPos(), path);
                })
                .thenWaitUntilServer(level -> printerAt(ctx, level).phase() == Printer3DBlockEntity.PRINTING,
                        MINING_WAIT, "the 3D printer to start printing", level -> "phase=" + printerAt(ctx, level)
                                .phase())
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenRightClick(SETTLE, MACHINE)
                .thenAwaitScreen(Printer3DScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SHOWN, label)
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    private static AsicBlockEntity asicAt(final ClientTestContext ctx, final ServerLevel level) {
        return (AsicBlockEntity) level.getBlockEntity(ctx.abs(MACHINE));
    }

    private static MiningComputerBlockEntity rigAt(final ClientTestContext ctx, final ServerLevel level) {
        return (MiningComputerBlockEntity) level.getBlockEntity(ctx.abs(MACHINE));
    }

    private static Printer3DBlockEntity printerAt(final ClientTestContext ctx, final ServerLevel level) {
        return (Printer3DBlockEntity) level.getBlockEntity(ctx.abs(MACHINE));
    }

    private static PersonalComputerBlockEntity pcAt(final ClientTestContext ctx, final ServerLevel level) {
        return (PersonalComputerBlockEntity) level.getBlockEntity(ctx.abs(COMPUTER));
    }

    private static ItemStack stack(final DeferredItem<? extends Item> item) {
        return new ItemStack(item.get());
    }

    /* The parts of a rig of one age. */
    private record Rig(BlockEntry<MiningComputerBlock> block, DeferredItem<Item> board, DeferredItem<Item> cpu,
                       DeferredItem<Item> ram, DeferredItem<Item> psu, DeferredItem<? extends Item> riser,
                       DeferredItem<Item> card) {
    }
}
