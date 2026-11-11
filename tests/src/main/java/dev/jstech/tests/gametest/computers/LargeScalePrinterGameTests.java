/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.made;

import dev.jstech.computers.os.fs.BookFile;
import dev.jstech.computers.os.fs.ScanFile;
import dev.jstech.computers.printer.BookJob;
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Large Scale Printer (computers/docs/design/peripherals.md): formed, it prints a book from every 3 sheets of paper
 * in its feeder, a signed .book as written books that are copies of the one scanned, and a .ench as enchanted books
 * with each enchantment at its level; with no paper a job waits; its queue holds 16 jobs and refuses more.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LargeScalePrinterGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 20;

    private LargeScalePrinterGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void signedBook_comesOutAsCopiesFromThreeSheetsEach(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("large_scale_printer"));
        final BookFile book = new BookFile("Mine Log", "Steve", List.of("Day 1: iron.", "Day 2: diamonds."));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final LargeScalePrinterBlockEntity printer = printer(helper, terminal);
                    printer.getInventory().insertItem(0, new ItemStack(Items.PAPER, 7), false);
                    helper.assertTrue(printer.submit(BookJob.of(book, "log.book", 2, "dev", "dev")).isPresent(),
                            "the printer takes the book");
                })
                .thenWaitUntil(() -> helper.assertTrue(made(printer(helper, terminal), Items.WRITTEN_BOOK)
                        .getCount() == 2, "two written books come out"))
                .thenExecute(() -> {
                    final LargeScalePrinterBlockEntity printer = printer(helper, terminal);
                    final WrittenBookContent content = made(printer, Items.WRITTEN_BOOK)
                            .get(DataComponents.WRITTEN_BOOK_CONTENT);
                    helper.assertTrue(content != null && "Mine Log".equals(content.title().raw())
                            && "Steve".equals(content.author()) && content.pages().size() == 2,
                            "each the book scanned: " + content);
                    helper.assertTrue(content.generation() > 0, "a copy, not the original");
                    helper.assertTrue(printer.paperCount() == 1, "3 sheets a book; left " + printer.paperCount());
                    helper.assertTrue(printer.jobs().isEmpty(), "and the queue is done");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void enchantedBook_comesOutWithItsEnchantmentAtItsLevel(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("large_scale_printer"));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final LargeScalePrinterBlockEntity printer = printer(helper, terminal);
                    printer.getInventory().insertItem(0, new ItemStack(Items.PAPER, 3), false);
                    helper.assertTrue(printer.submit(BookJob.enchanted(List.of(new ScanFile.Enchanted("",
                            "minecraft:sharpness", 5)), "sharp.ench", 1, "dev", "dev")).isPresent(),
                            "the printer takes the enchanted book");
                })
                .thenWaitUntil(() -> helper.assertTrue(!made(printer(helper, terminal), Items.ENCHANTED_BOOK)
                        .isEmpty(), "an enchanted book comes out"))
                .thenExecute(() -> {
                    final ItemEnchantments stored = made(printer(helper, terminal), Items.ENCHANTED_BOOK)
                            .getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                    helper.assertTrue(stored.size() == 1 && stored.entrySet().iterator().next().getIntValue() == 5,
                            "with Sharpness at level 5: " + stored);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void job_waitsForPaperAndTheQueueHoldsSixteen(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("large_scale_printer"));
        final BookJob job = BookJob.of(new BookFile("", "", List.of("draft")), "draft.book", 1, "dev", "dev");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final LargeScalePrinterBlockEntity printer = printer(helper, terminal);
                    for (int i = 0; i < LargeScalePrinterBlockEntity.QUEUE_MAX; i++) {
                        helper.assertTrue(printer.submit(job).isPresent(), "the queue takes job " + (i + 1));
                    }
                    helper.assertTrue(printer.submit(job).isEmpty(), "and refuses one more");
                })
                .thenExecuteAfter(SETTLE * 3, () -> {
                    final LargeScalePrinterBlockEntity printer = printer(helper, terminal);
                    helper.assertTrue(printer.printed() == 0 && made(printer, Items.WRITABLE_BOOK).isEmpty(),
                            "with no paper nothing comes out");
                    helper.assertTrue(printer.jobs().size() == LargeScalePrinterBlockEntity.QUEUE_MAX,
                            "and the jobs wait");
                })
                .thenSucceed();
    }

    private static LargeScalePrinterBlockEntity printer(final GameTestHelper helper, final BlockPos terminal) {
        return (LargeScalePrinterBlockEntity) machineAt(helper, terminal);
    }
}
