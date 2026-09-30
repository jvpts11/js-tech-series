.asm 3
.arch jsc:x86
.start Corpus.OldNamesCorpus console

.class Corpus.OldNamesCorpus

.method static int Main(string[]) slots 16
    ldstr   "How many ingots?"
    call    Console.PrintLine(string) -> void
    ldc.i4  62
    call    Console.Print(char) -> void
    ldc.i4  32
    call    Console.Print(char) -> void
    call    Console.ReadLine() -> string
    stloc   1
    call    Console.Read() -> int
    stloc   2
    call    Console.Scan(out int) -> int
    stloc   3
    call    Console.Scan(out int) -> int
    stloc   3
    add
    ldloc   2
    add
    stloc   4
    ldloc   1
    call    Convert.ToDouble(string) -> double
    stloc   5
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
    stloc   6
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
    stloc   7
    ldc.i4  1987
    conv.i8
    call    Random.Seed(long) -> void
    ldc.i4  32768
    call    Random.Next(int) -> int
    stloc   8
    ldloc   1
    ldfld   string.Length
    stloc   9
    ldloc   1
    ldstr   "ingot"
    call    string.IndexOf(string) -> int
    stloc   10
    ldstr   ""
    ldloc   6
    call    Convert.ToString(object) -> string
    call    string.Concat(string, string) -> string
    ldstr   ": "
    call    string.Concat(string, string) -> string
    ldloc   8
    ldloc   9
    add
    ldloc   10
    add
    call    string.Concat(string, int) -> string
    ldstr   ", "
    call    string.Concat(string, string) -> string
    ldloc   7
    ldloc   5
    add
    call    string.Concat(string, double) -> string
    stloc   11
    ldstr   ""
    ldloc   11
    call    string.Concat(string, string) -> string
    ldstr   "\n"
    call    string.Concat(string, string) -> string
    call    Console.Print(string) -> void
    ldstr   ""
    ldstr   "%-12s"
    ldloc   11
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%5d"
    ldloc   6
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%05.2f"
    ldloc   7
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%#x"
    ldloc   8
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%e"
    ldloc   5
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%u"
    ldloc   9
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "\n"
    call    string.Concat(string, string) -> string
    call    Console.Print(string) -> void
    ldloc   1
    ldloc   11
    call    string.Compare(string, string) -> int
    ldloc   11
    ldloc   1
    call    string.Compare(string, string) -> int
    add
    ldloc   1
    ldc.i4  0
    call    Convert.ToInt(string, int) -> int
    add
    ldloc   1
    ldc.i4  1
    neg
    call    Convert.ToInt(string, int) -> int
    add
    stloc   12
    ldc.i4  97
    call    char.ToUpper(char) -> char
    stloc   13
    ldc.i4  122
    call    char.ToUpper(char) -> char
    call    char.ToLower(char) -> char
    stloc   14
    ldloc   13
    call    char.IsDigit(char) -> bool
    brtrue  L11
    ldloc   14
    call    char.IsLetter(char) -> bool
    br      L12
L11: ldc.i4  1
L12: brtrue  L9
    ldc.i4  32
    call    char.IsWhiteSpace(char) -> bool
    br      L10
L9: ldc.i4  1
L10: brtrue  L7
    ldloc   13
    call    char.ToLower(char) -> char
    conv.i4
    ldc.i4  97
    conv.i4
    ceq
    br      L8
L7: ldc.i4  1
L8: brtrue  L5
    ldc.i4  49
    call    char.IsDigit(char) -> bool
    br      L6
L5: ldc.i4  1
L6: brtrue  L3
    ldc.i4  98
    call    char.IsLetter(char) -> bool
    br      L4
L3: ldc.i4  1
L4: brtrue  L1
    ldc.i4  9
    call    char.IsWhiteSpace(char) -> bool
    br      L2
L1: ldc.i4  1
L2: stloc   15
    ldloc   12
    ldc.i4  16
    call    Convert.ToString(int, int) -> string
    ldloc   8
    ldc.i4  2
    call    Convert.ToString(int, int) -> string
    call    string.Concat(string, string) -> string
    ldloc   13
    call    string.FromChar(char) -> string
    call    string.Concat(string, string) -> string
    ldloc   14
    call    string.FromChar(char) -> string
    call    string.Concat(string, string) -> string
    ldloc   15
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldloc   0
    ldlen
    ldc.i4  1
    cgt
    brfalse L13
    ldc.i4  0
    call    Program.Exit(int) -> void
L13: ldloc   0
    ldlen
    ret
