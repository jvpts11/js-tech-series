.asm 3
.arch jsc:x86
.start Corpus.OldNamesCorpus console

.class Corpus.OldNamesCorpus

.method static void Main() slots 8
    ldstr   "How many ingots?"
    call    Console.PrintLine(string) -> void
    call    Console.ReadLine() -> string
    stloc   0
    ldloc   0
    call    Convert.ToDouble(string) -> double
    stloc   1
    ldc.i4  3
    neg
    call    Math.Abs(int) -> int
    ldc.i4  4
    ldc.i4  5
    call    Math.Min(int, int) -> int
    add
    ldc.i4  1
    ldc.i4  2
    call    Math.Max(int, int) -> int
    add
    stloc   2
    ldc.i4  3
    conv.r8
    ldc.i4  2
    conv.r8
    call    Math.Pow(double, double) -> double
    call    Math.Sqrt(double) -> double
    ldc.r8  2.5
    call    Math.Floor(double) -> double
    add
    ldc.r8  0.5
    neg
    call    Math.Abs(double) -> double
    add
    ldc.r8  1.5
    ldc.i4  2
    conv.r8
    call    Math.Min(double, double) -> double
    add
    ldc.r8  0.5
    ldc.i4  1
    conv.r8
    call    Math.Max(double, double) -> double
    add
    stloc   3
    ldc.i4  1987
    conv.i8
    call    Random.Seed(long) -> void
    ldc.i4  32768
    call    Random.Next(int) -> int
    stloc   4
    ldloc   0
    ldfld   string.Length
    stloc   5
    ldloc   0
    ldstr   "ingot"
    call    string.IndexOf(string) -> int
    stloc   6
    ldstr   ""
    ldloc   2
    call    Convert.ToString(object) -> string
    call    string.Concat(string, string) -> string
    ldstr   ": "
    call    string.Concat(string, string) -> string
    ldloc   4
    ldloc   5
    add
    ldloc   6
    add
    call    string.Concat(string, int) -> string
    ldstr   ", "
    call    string.Concat(string, string) -> string
    ldloc   3
    ldloc   1
    add
    call    string.Concat(string, double) -> string
    stloc   7
    ldstr   ""
    ldloc   7
    call    string.Concat(string, string) -> string
    ldstr   "\n"
    call    string.Concat(string, string) -> string
    call    Console.Print(string) -> void
    ldc.i4  0
    call    Program.Exit(int) -> void
    ret
