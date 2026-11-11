.asm 3
.arch jsc:x86
.start Corpus.ToolkitCorpus console

.class Corpus.ToolkitCorpus

.method static void Main() slots 30
    call    Stopwatch.Start() -> Stopwatch
    stloc   0
    ldstr   ""
    ldsfld  Calendar.Hour
    call    string.Concat(string, int) -> string
    ldsfld  Calendar.Minute
    call    string.Concat(string, int) -> string
    ldsfld  Calendar.DayOfWeek
    call    string.Concat(string, int) -> string
    ldsfld  Calendar.DayOfSeason
    call    string.Concat(string, int) -> string
    ldsfld  Calendar.Day
    call    string.Concat(string, long) -> string
    ldsfld  Calendar.Week
    call    string.Concat(string, long) -> string
    ldsfld  Calendar.Year
    call    string.Concat(string, long) -> string
    ldsfld  Calendar.Season
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "C:\\logs"
    ldstr   "stock.log"
    call    Path.Combine(string, string) -> string
    stloc   1
    ldloc   1
    call    Path.FileName(string) -> string
    ldloc   1
    call    Path.Extension(string) -> string
    call    string.Concat(string, string) -> string
    ldloc   1
    call    Path.Folder(string) -> string
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "C:\\logs"
    call    Directory.Exists(string) -> bool
    ldc.i4  0
    ceq
    brfalse L1
    ldstr   "C:\\logs"
    call    Directory.Make(string) -> bool
    pop
L1: ldstr   "C:\\logs"
    call    Directory.Files(string) -> List<string>
    stloc   2
    ldc.i4  0
    stloc   3
L2: ldloc   3
    ldloc   2
    ldfld   List.Count
    bge     L4
    ldloc   2
    ldloc   3
    call    List.Get(int) -> string
    stloc   4
    ldloc   4
    ldstr   "C:\\logs"
    ldloc   4
    call    Path.Combine(string, string) -> string
    call    File.Size(string) -> long
    call    string.Concat(string, long) -> string
    ldloc   4
    call    File.Changed(string) -> long
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
L3: ldloc   3
    ldc.i4  1
    add
    stloc   3
    br      L2
L4: ldstr   "C:\\"
    call    Directory.Folders(string) -> List<string>
    stloc   5
    ldc.i4  0
    stloc   6
L5: ldloc   6
    ldloc   5
    ldfld   List.Count
    bge     L7
    ldloc   5
    ldloc   6
    call    List.Get(int) -> string
    stloc   7
    ldloc   7
    call    Console.PrintLine(string) -> void
L6: ldloc   6
    ldc.i4  1
    add
    stloc   6
    br      L5
L7: ldstr   "C:\\old"
    call    Directory.Delete(string) -> bool
    pop
    ldloc   1
    ldthis
    ldfn    Corpus.ToolkitCorpus.0lambda1(FileEvent) -> void
    call    File.Watch(string, Action<FileEvent>) -> Subscription
    stloc   8
    newobj  StringBuilder()
    stloc   9
    ldloc   9
    ldstr   "iron"
    call    StringBuilder.Append(string) -> StringBuilder
    ldstr   " ingot"
    call    StringBuilder.AppendLine(string) -> StringBuilder
    call    StringBuilder.Clear() -> StringBuilder
    pop
    ldloc   9
    call    StringBuilder.ToString() -> string
    ldloc   9
    ldfld   StringBuilder.Length
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "x64"
    ldstr   "x(\\d+)"
    call    Regex.IsMatch(string, string) -> bool
    brfalse L8
    ldstr   "x64"
    ldstr   "x(\\d+)"
    call    Regex.Match(string, string) -> List<string>
    stloc   10
    ldstr   "a1 b2"
    ldstr   "\\d"
    call    Regex.Matches(string, string) -> List<string>
    stloc   11
    ldstr   "a,b"
    ldstr   ","
    call    Regex.Split(string, string) -> List<string>
    stloc   12
    ldloc   10
    ldc.i4  1
    call    List.Get(int) -> string
    ldloc   11
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    ldloc   12
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    ldstr   "x64"
    ldstr   "\\d"
    ldstr   "#"
    call    Regex.Replace(string, string, string) -> string
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
L8: ldstr   "{\"items\": [1, \"two\", true]}"
    call    Json.Parse(string) -> JsonValue
    stloc   13
    ldloc   13
    ldstr   "items"
    call    JsonValue.Get(string) -> JsonValue
    stloc   14
    ldloc   13
    ldfld   JsonValue.Kind
    ldloc   13
    ldfld   JsonValue.Count
    call    string.Concat(string, int) -> string
    ldloc   13
    ldstr   "items"
    call    JsonValue.Has(string) -> bool
    call    string.Concat(string, bool) -> string
    ldloc   13
    call    JsonValue.Keys() -> List<string>
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    ldloc   14
    ldc.i4  0
    call    JsonValue.At(int) -> JsonValue
    call    JsonValue.AsNumber() -> double
    call    string.Concat(string, double) -> string
    ldloc   14
    ldc.i4  1
    call    JsonValue.At(int) -> JsonValue
    call    JsonValue.AsString() -> string
    call    string.Concat(string, string) -> string
    ldloc   14
    ldc.i4  2
    call    JsonValue.At(int) -> JsonValue
    call    JsonValue.AsBool() -> bool
    call    string.Concat(string, bool) -> string
    ldloc   13
    ldstr   "items"
    call    JsonValue.Get(string) -> JsonValue
    ldc.i4  0
    call    JsonValue.At(int) -> JsonValue
    call    JsonValue.AsString() -> string
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "a,b\n1,2"
    call    Csv.Parse(string) -> List<List<string>>
    stloc   15
    ldloc   15
    call    Csv.Write(List<List<string>>) -> string
    ldstr   "abc"
    call    Hash.Sha256(string) -> string
    call    string.Concat(string, string) -> string
    ldstr   "abc"
    call    Hash.Crc32(string) -> long
    call    string.Concat(string, long) -> string
    ldstr   "iron"
    call    Base64.Encode(string) -> string
    call    Base64.Decode(string) -> string
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    newobj  Queue<int>()
    stloc   16
    ldloc   16
    ldc.i4  1
    call    Queue.Enqueue(int) -> void
    newobj  Stack<int>()
    stloc   17
    ldloc   17
    ldc.i4  2
    call    Stack.Push(int) -> void
    newobj  Set<string>()
    stloc   18
    ldloc   18
    ldstr   "a"
    call    Set.Add(string) -> bool
    pop
    ldloc   16
    ldc.i4  1
    call    Queue.Contains(int) -> bool
    brfalse L14
    ldloc   17
    ldc.i4  2
    call    Stack.Contains(int) -> bool
    br      L15
L14: ldc.i4  0
L15: brfalse L12
    ldloc   18
    ldstr   "a"
    call    Set.Contains(string) -> bool
    br      L13
L12: ldc.i4  0
L13: brfalse L10
    ldloc   18
    ldstr   "b"
    call    Set.Remove(string) -> bool
    ldc.i4  0
    ceq
    br      L11
L10: ldc.i4  0
L11: brfalse L9
    ldstr   ""
    ldloc   16
    call    Queue.Peek() -> int
    call    string.Concat(string, int) -> string
    ldloc   16
    call    Queue.Dequeue() -> int
    call    string.Concat(string, int) -> string
    ldloc   16
    ldfld   Queue.Count
    call    string.Concat(string, int) -> string
    ldloc   17
    call    Stack.Peek() -> int
    call    string.Concat(string, int) -> string
    ldloc   17
    call    Stack.Pop() -> int
    call    string.Concat(string, int) -> string
    ldloc   17
    ldfld   Stack.Count
    call    string.Concat(string, int) -> string
    ldloc   18
    ldfld   Set.Count
    call    string.Concat(string, int) -> string
    ldloc   18
    call    Set.Items() -> List<string>
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
L9: ldloc   16
    call    Queue.Clear() -> void
    ldloc   17
    call    Stack.Clear() -> void
    ldloc   18
    call    Set.Clear() -> void
    newobj  List<int>()
    stloc   19
    ldloc   19
    ldc.i4  3
    call    List.Add(int) -> void
    ldloc   19
    ldc.i4  1
    call    List.Add(int) -> void
    ldloc   19
    stloc   20
    ldthis
    ldfn    Corpus.ToolkitCorpus.0lambda2(int, int) -> int
    stloc   21
    ldc.i4  1
    stloc   22
L16: ldloc   22
    ldloc   20
    ldfld   List.Count
    bge     L19
    ldloc   20
    ldloc   22
    call    List.Get(int) -> int
    stloc   23
    ldloc   22
    ldc.i4  1
    sub
    stloc   24
L17: ldloc   24
    ldc.i4  0
    blt     L18
    ldloc   21
    ldloc   20
    ldloc   24
    call    List.Get(int) -> int
    ldloc   23
    callvirt Func.Invoke(int, int) -> int
    ldc.i4  0
    ble     L18
    ldloc   20
    ldloc   24
    ldc.i4  1
    add
    ldloc   20
    ldloc   24
    call    List.Get(int) -> int
    call    List.Set(int, int) -> void
    ldloc   24
    ldc.i4  -1
    add
    stloc   24
    br      L17
L18: ldloc   20
    ldloc   24
    ldc.i4  1
    add
    ldloc   23
    call    List.Set(int, int) -> void
    ldloc   22
    ldc.i4  1
    add
    stloc   22
    br      L16
L19: ldstr   ""
    ldloc   19
    stloc   25
    ldthis
    ldfn    Corpus.ToolkitCorpus.0lambda3(int) -> bool
    stloc   26
    ldc.i4  0
    stloc   29
    ldc.i4  0
    stloc   27
L20: ldloc   27
    ldloc   25
    ldfld   List.Count
    bge     L22
    ldloc   25
    ldloc   27
    call    List.Get(int) -> int
    stloc   28
    ldloc   26
    ldloc   28
    callvirt Func.Invoke(int) -> bool
    brfalse L21
    ldloc   28
    stloc   29
    br      L22
L21: ldloc   27
    ldc.i4  1
    add
    stloc   27
    br      L20
L22: ldloc   29
    call    string.Concat(string, int) -> string
    ldloc   0
    ldfld   Stopwatch.Ticks
    call    string.Concat(string, long) -> string
    ldloc   0
    ldfld   Stopwatch.Credits
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ldloc   0
    call    Stopwatch.Restart() -> Stopwatch
    pop
    ldloc   8
    ldfld   Subscription.Item
    call    Console.PrintLine(string) -> void
    ret

.method void 0lambda1(FileEvent) slots 1
    ldloc   0
    ldfld   FileEvent.Path
    ldloc   0
    ldfld   FileEvent.Changed
    call    string.Concat(string, long) -> string
    ldloc   0
    ldfld   FileEvent.Previous
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ret

.method int 0lambda2(int, int) slots 2
    ldloc   0
    ldloc   1
    sub
    ret

.method bool 0lambda3(int) slots 1
    ldloc   0
    ldc.i4  1
    cgt
    ret
