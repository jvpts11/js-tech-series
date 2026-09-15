.asm 2
.start Corpus.LibraryCorpus console

.class Corpus.LibraryCorpus

.method static void Main() slots 27
    ldc.i4  3
    neg
    call    Math.Abs(int) -> int
    stloc   0
    ldc.r8  2.5
    neg
    call    Math.Abs(double) -> double
    stloc   1
    ldc.i4  1
    ldc.i4  2
    call    Math.Min(int, int) -> int
    stloc   2
    ldc.r8  1.5
    ldc.r8  2.5
    call    Math.Min(double, double) -> double
    stloc   3
    ldc.i4  1
    ldc.i4  2
    call    Math.Max(int, int) -> int
    stloc   4
    ldc.r8  1.5
    ldc.r8  2.5
    call    Math.Max(double, double) -> double
    stloc   5
    ldc.i4  5
    ldc.i4  0
    ldc.i4  3
    call    Math.Clamp(int, int, int) -> int
    stloc   6
    ldc.r8  5.5
    ldc.r8  0.5
    ldc.r8  3.5
    call    Math.Clamp(double, double, double) -> double
    stloc   7
    ldc.r8  2.7
    call    Math.Floor(double) -> double
    ldc.r8  2.1
    call    Math.Ceil(double) -> double
    add
    ldc.r8  2.5
    call    Math.Round(double) -> double
    add
    ldc.r8  9.0
    call    Math.Sqrt(double) -> double
    add
    ldc.r8  2.0
    ldc.r8  3.0
    call    Math.Pow(double, double) -> double
    add
    stloc   8
    ldstr   ""
    ldloc   0
    call    string.Concat(string, int) -> string
    ldloc   1
    call    string.Concat(string, double) -> string
    ldloc   2
    call    string.Concat(string, int) -> string
    ldloc   3
    call    string.Concat(string, double) -> string
    ldloc   4
    call    string.Concat(string, int) -> string
    ldloc   5
    call    string.Concat(string, double) -> string
    ldloc   6
    call    string.Concat(string, int) -> string
    ldloc   7
    call    string.Concat(string, double) -> string
    ldloc   8
    call    string.Concat(string, double) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "4"
    call    Convert.ToInt(string) -> int
    stloc   9
    ldstr   "5"
    call    Convert.ToLong(string) -> long
    stloc   10
    ldstr   "1.5"
    call    Convert.ToFloat(string) -> float
    stloc   11
    ldstr   "2.5"
    call    Convert.ToDouble(string) -> double
    stloc   12
    ldstr   "true"
    call    Convert.ToBool(string) -> bool
    stloc   13
    ldloc   9
    call    Convert.ToString(object) -> string
    stloc   14
    ldstr   ""
    ldloc   9
    call    string.Concat(string, int) -> string
    ldloc   10
    call    string.Concat(string, long) -> string
    ldloc   11
    call    string.Concat(string, float) -> string
    ldloc   12
    call    string.Concat(string, double) -> string
    ldloc   13
    call    string.Concat(string, bool) -> string
    ldloc   14
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "6"
    call    Convert.TryInt(string, out int) -> bool
    stloc   15
    brfalse L2
    ldstr   "7"
    call    Convert.TryLong(string, out long) -> bool
    stloc   16
    br      L3
L2: ldc.i4  0
L3: brfalse L1
    ldstr   ""
    ldloc   15
    call    string.Concat(string, int) -> string
    ldloc   16
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
L1: ldstr   "8.5"
    call    Convert.TryDouble(string, out double) -> bool
    stloc   17
    brfalse L5
    ldstr   "false"
    call    Convert.TryBool(string, out bool) -> bool
    stloc   18
    br      L6
L5: ldc.i4  0
L6: brfalse L4
    ldstr   ""
    ldloc   17
    call    string.Concat(string, double) -> string
    ldloc   18
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
L4: ldc.i4  42
    conv.i8
    call    Random.Seed(long) -> void
    ldc.i4  6
    call    Random.Next(int) -> int
    stloc   19
    call    Random.NextDouble() -> double
    stloc   20
    ldsfld  Time.Tick
    ldsfld  Time.DayTime
    add
    ldsfld  Time.Day
    add
    ldc.i4  20
    call    Time.Ticks(int) -> long
    add
    stloc   21
    ldstr   ""
    ldloc   19
    call    string.Concat(string, int) -> string
    ldloc   20
    call    string.Concat(string, double) -> string
    ldloc   21
    call    string.Concat(string, long) -> string
    call    Console.Print(string) -> void
    call    Console.Clear() -> void
    call    Console.HasLine() -> bool
    brfalse L7
    call    Console.ReadLine() -> string
    stloc   22
    call    Console.ReadInt() -> int
    stloc   23
    call    Console.ReadLong() -> long
    stloc   24
    call    Console.ReadDouble() -> double
    stloc   25
    call    Console.ReadBool() -> bool
    stloc   26
    ldloc   22
    ldloc   23
    call    string.Concat(string, int) -> string
    ldloc   24
    call    string.Concat(string, long) -> string
    ldloc   25
    call    string.Concat(string, double) -> string
    ldloc   26
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
L7: ret
