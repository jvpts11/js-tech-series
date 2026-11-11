/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Pages of the library chapter of the Book of Sigma on the tools that came with the third version: text built and
 * matched, JSON, CSV and codes, files and folders, more collections, and the calendar and a stopwatch.
 */
final class SigmaToolkitPages {

    private SigmaToolkitPages() {
    }

    /** Declares the section and the pages of the third version's tools, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("library_toolkit").titled("Tools for text, files and data")
                .in(chapter).register();
        text(guide, section);
        data(guide, section);
        files(guide, section);
        collections(guide, section);
        clock(guide, section);
    }

    private static void text(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("text_tools_library", section).titled("StringBuilder and Regex: building and matching text")
                .paragraph("Joining texts with + makes a new text every time. When you build a long text a piece at a"
                        + " time, use a StringBuilder: each piece is added at the same cost, however long the text"
                        + " already is. Both types live in System.Text and came with Sigma# 3.")
                .table("StringBuilder members", "Member", "What it does")
                .row(GuideCell.code("new StringBuilder(), StringBuilder.new()"), GuideCell.words("An empty builder."))
                .row(GuideCell.code("StringBuilder.Append(text)"), GuideCell.words("Adds the text at the end, and"
                        + " gives the builder back, so calls can follow one another."))
                .row(GuideCell.code("StringBuilder.AppendLine(text)"), GuideCell.words("Adds the text and a line"
                        + " break."))
                .row(GuideCell.code("StringBuilder.Clear()"), GuideCell.words("Empties the builder."))
                .row(GuideCell.code("StringBuilder.ToString()"), GuideCell.words("Everything added, as one text."))
                .row(GuideCell.code("StringBuilder.Length"), GuideCell.words("How many characters it holds."))
                .paragraph("A pattern finds text by its shape: \\d+ is one or more digits. Regex matches a pattern"
                        + " written as Java and C# write them.")
                .table("Regex members", "Member", "What it gives")
                .row(GuideCell.code("Regex.IsMatch(text, pattern)"), GuideCell.words("Yes when the pattern is found"
                        + " anywhere in the text."))
                .row(GuideCell.code("Regex.Match(text, pattern)"), GuideCell.words("The first match and its groups,"
                        + " the whole match first, or an empty list when there is none."))
                .row(GuideCell.code("Regex.Matches(text, pattern)"), GuideCell.words("Every match, in order."))
                .row(GuideCell.code("Regex.Replace(text, pattern, with)"), GuideCell.words("The text with every match"
                        + " replaced. $1 in the replacement stands for the first group."))
                .row(GuideCell.code("Regex.Split(text, pattern)"), GuideCell.words("The pieces between the matches."))
                .paragraph("A pattern costs a credit for every 4096 characters it reads. Some patterns read the same"
                        + " text many times over: a pattern that would read it more than 256 times over stops the"
                        + " program instead.")
                .listing("Builder.sgs",
                        "StringBuilder report = new StringBuilder();",
                        "foreach (string line in File.Read(\"stock.txt\").Split('\\n')) {",
                        "    if (Regex.IsMatch(line, \"^iron\")) {",
                        "        report.AppendLine(line);",
                        "    }",
                        "}",
                        "Console.PrintLine(report.ToString());")
                .define("Pattern", "A text that describes the shape of the text to find, such as \\d+ for a run of"
                        + " digits.")
                .ifSomethingGoesWrong(
                        "The program stops and says the text is not a pattern.",
                        "Check the pattern's brackets and backslashes. In a text, write a backslash twice: \"\\\\d\".",
                        "The program stops and says the pattern read the text too many times over.",
                        "Write a simpler pattern: a group repeated inside a repeated group is what reads without end.")
                .seeAlso("jsc:numbers_and_time", "jsc:data_library")
                .register();
    }

    private static void data(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("data_library", section).titled("Json, Csv, Hash and Base64: data in text")
                .paragraph("Texts often carry data: a JSON document, a CSV table, a code. These types read them, all"
                        + " in System.Text, and came with Sigma# 3. Reading costs what its instructions cost.")
                .table("Json and JsonValue members", "Member", "What it gives")
                .row(GuideCell.code("Json.Parse(text)"), GuideCell.words("The document as a JsonValue. A text that"
                        + " is not JSON stops the program with a FormatException."))
                .row(GuideCell.code("JsonValue.Get(name)"), GuideCell.words("An object's member, or null. You can"
                        + " write value[name] instead."))
                .row(GuideCell.code("JsonValue.At(number)"), GuideCell.words("An array's element, counted from 0."
                        + " You can write value[number] instead."))
                .row(GuideCell.code("JsonValue.Has(name)"), GuideCell.words("Yes when the object has that member."))
                .row(GuideCell.code("JsonValue.Keys()"), GuideCell.words("The names of an object's members, in order."))
                .row(GuideCell.code("JsonValue.AsString()"), GuideCell.words("A text, a number, true or false, or null,"
                        + " written as text."))
                .row(GuideCell.code("JsonValue.AsNumber()"), GuideCell.words("A number, or a text that writes one."))
                .row(GuideCell.code("JsonValue.AsBool()"), GuideCell.words("true or false."))
                .row(GuideCell.code("JsonValue.Kind"), GuideCell.words("object, array, string, number, bool or null."))
                .row(GuideCell.code("JsonValue.Count"), GuideCell.words("How many members or elements it holds, or"
                        + " the length of a text."))
                .paragraph("A JsonValue is read only. Build the text of a document yourself, with a StringBuilder.")
                .listing("Prices.sgs",
                        "JsonValue prices = Json.Parse(File.Read(\"prices.json\"));",
                        "double iron = prices[\"items\"][0][\"price\"].AsNumber();")
                .table("Csv, Hash and Base64 members", "Member", "What it gives")
                .row(GuideCell.code("Csv.Parse(text)"), GuideCell.words("The rows of a CSV table, each a list of its"
                        + " fields. A field in quotes may hold commas and line breaks."))
                .row(GuideCell.code("Csv.Write(rows)"), GuideCell.words("The rows as CSV, a line each, quoting the"
                        + " fields that need it."))
                .row(GuideCell.code("Hash.Sha256(text)"), GuideCell.words("The text's SHA-256 fingerprint, in"
                        + " hexadecimal."))
                .row(GuideCell.code("Hash.Crc32(text)"), GuideCell.words("The text's CRC-32 checksum, as a number."))
                .row(GuideCell.code("Base64.Encode(text)"), GuideCell.words("The text written in Base64."))
                .row(GuideCell.code("Base64.Decode(text)"), GuideCell.words("The text that Base64 holds. What is not"
                        + " Base64 stops the program with a FormatException."))
                .define("JSON", "A way of writing data as text: objects of named members, arrays, texts, numbers,"
                        + " true, false and null.")
                .define("CSV", "A table written as text: a row on each line, its fields parted by commas.")
                .ifSomethingGoesWrong(
                        "The program stops and says the text is not JSON.",
                        "Check the document at the character it names. Every text in JSON is written in double"
                                + " quotes.",
                        "Reading a member gives null.",
                        "The object has no member of that name. Check it with Has, or list them with Keys.")
                .seeAlso("jsc:text_tools_library", "jsc:files_and_file_handles")
                .register();
    }

    private static void files(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("folders_library", section).titled("Path, Directory and watching a file")
                .paragraph("Sigma# 3 adds what your program needs to find its way around the disks: taking paths"
                        + " apart, the folders, a file's size and when it last changed, and being told when it changes."
                        + " Path and Directory live in System.IO, beside File.")
                .table("Path members", "Member", "What it gives")
                .row(GuideCell.code("Path.Combine(folder, name)"), GuideCell.words("The name inside the folder,"
                        + " with the separator the folder is written with."))
                .row(GuideCell.code("Path.FileName(path)"), GuideCell.words("The name at the end of the path."))
                .row(GuideCell.code("Path.Extension(path)"), GuideCell.words("The name's ending with its dot, such"
                        + " as .log, or empty text."))
                .row(GuideCell.code("Path.Folder(path)"), GuideCell.words("The folder the path is in."))
                .table("Directory and File members", "Member", "What it does")
                .row(GuideCell.code("Directory.Exists(path)"), GuideCell.words("Yes when the folder is there."))
                .row(GuideCell.code("Directory.Make(path)"), GuideCell.words("Makes the folder."))
                .row(GuideCell.code("Directory.Delete(path)"), GuideCell.words("Deletes the folder."))
                .row(GuideCell.code("Directory.Files(path)"), GuideCell.words("The names of the files in it."))
                .row(GuideCell.code("Directory.Folders(path)"), GuideCell.words("The names of the folders in it."))
                .row(GuideCell.code("File.Size(path)"), GuideCell.words("The file's size in bytes, or -1 when it is"
                        + " not there."))
                .row(GuideCell.code("File.Changed(path)"), GuideCell.words("The world tick the file last changed in,"
                        + " which you compare with Time.Tick, or -1 when it is not there."))
                .row(GuideCell.code("File.Watch(path, handler)"), GuideCell.words("Calls the handler with a FileEvent"
                        + " each time the file changes, appears or goes. It gives a Subscription: let go of it to"
                        + " stop."))
                .row(GuideCell.code("FileEvent.Path, FileEvent.Changed, FileEvent.Previous"), GuideCell.words("The"
                        + " path watched, and the tick it changed in now and before, -1 for a file that is not there."))
                .paragraph("Size, Changed and Watch read the files on this machine's own drives. A watch costs nothing"
                        + " to set: the machine looks once a tick, as it does for a watch on the network.")
                .define("Separator", "The character between the folders of a path: a backslash on Frames and"
                        + " MC-DOS, a slash on the Unix systems.")
                .ifSomethingGoesWrong(
                        "File.Size gives -1 for a file on another computer's share.",
                        "Size, Changed and Watch read this machine's own drives only. Copy the file here first.",
                        "Directory.Delete gives no.",
                        "Delete the files inside the folder first.")
                .seeAlso("jsc:files_and_file_handles", "jsc:file_tree", "jsc:network_watches")
                .register();
    }

    private static void collections(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("more_collections_library", section).titled("Queue, Stack, Set, and Find and Sort with a function")
                .paragraph("Sigma# 3 adds three collections to List and Map, in System.Collections, and two calls on a"
                        + " list that take a function you write with =>.")
                .table("The new collections", "Member", "What it does")
                .row(GuideCell.code("Queue<T>.Enqueue(value), Dequeue(), Peek()"), GuideCell.words("A queue gives"
                        + " back first what it was given first. Dequeue takes it out; Peek only looks."))
                .row(GuideCell.code("Stack<T>.Push(value), Pop(), Peek()"), GuideCell.words("A stack gives back first"
                        + " what it was given last."))
                .row(GuideCell.code("Set<T>.Add(value), Remove(value), Items()"), GuideCell.words("A set holds each"
                        + " value once. Add gives no when it already held the value; Items lists them in the order"
                        + " they came."))
                .row(GuideCell.code("Count, Contains(value), Clear()"), GuideCell.words("On all three, as on a list."))
                .table("A list with a function", "Member", "What it does")
                .row(GuideCell.code("List<T>.Find(test)"), GuideCell.words("The first element the test says yes to,"
                        + " or the type's empty value: 0, false or null."))
                .row(GuideCell.code("List<T>.Sort(compare)"), GuideCell.words("Sorts the list by a comparison of two"
                        + " elements, which gives below 0 when the first comes first. Elements it finds equal keep"
                        + " their order."))
                .paragraph("Taking from an empty queue or stack stops the program. Find and Sort call your function"
                        + " once for each element they look at, and each call costs what its instructions cost.")
                .listing("Sorting.sgs",
                        "List<int> counts = new List<int>();",
                        "counts.Add(64); counts.Add(8); counts.Add(32);",
                        "counts.Sort((a, b) => a - b);",
                        "int big = counts.Find(n => n > 16);")
                .define("Queue", "A collection that gives back first what came in first, as a line of people does.")
                .define("Stack", "A collection that gives back first what came in last, as a pile of plates does.")
                .ifSomethingGoesWrong(
                        "The program stops and says the queue is empty.",
                        "Check Count before Dequeue or Peek.")
                .seeAlso("jsc:sigma_list_map")
                .register();
    }

    private static void clock(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("calendar_library", section).titled("Calendar and Stopwatch")
                .paragraph("Calendar reads the world's calendar where the machine stands, and a Stopwatch times a"
                        + " stretch of your program. Both live in System.Utils and came with Sigma# 3.")
                .table("Calendar members", "Member", "What it gives")
                .row(GuideCell.code("Calendar.Hour, Calendar.Minute"), GuideCell.words("The time the clock shows. A"
                        + " day starts at six in the morning."))
                .row(GuideCell.code("Calendar.Day, Calendar.Week, Calendar.Year"), GuideCell.words("Which day, week"
                        + " and year it is, counted from 0."))
                .row(GuideCell.code("Calendar.DayOfWeek, Calendar.DayOfSeason"), GuideCell.words("The day within the"
                        + " week and within the season, from 0."))
                .row(GuideCell.code("Calendar.Season"), GuideCell.words("spring, summer, autumn or winter."))
                .table("Stopwatch members", "Member", "What it gives")
                .row(GuideCell.code("Stopwatch.Start()"), GuideCell.words("A stopwatch started now."))
                .row(GuideCell.code("Stopwatch.Ticks"), GuideCell.words("The world ticks gone by since it started."))
                .row(GuideCell.code("Stopwatch.Credits"), GuideCell.words("The credits your program spent since it"
                        + " started."))
                .row(GuideCell.code("Stopwatch.Restart()"), GuideCell.words("Starts it again from now."))
                .listing("Timing.sgs",
                        "Stopwatch watch = Stopwatch.Start();",
                        "Report();",
                        "Console.PrintLine(\"The report cost \" + watch.Credits + \" credits\");")
                .define("Credit", "A unit of a program's work: an instruction costs one, and asking the machine costs"
                        + " more.")
                .ifSomethingGoesWrong(
                        "Stopwatch.Ticks stays at 0 ticks.",
                        "Your program has not let a tick go by: the clock moves between ticks, not inside one.")
                .seeAlso("jsc:numbers_and_time", "jsc:credits_per_tick")
                .register();
    }
}
