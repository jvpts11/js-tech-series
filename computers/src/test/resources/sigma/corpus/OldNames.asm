.asm 3
.arch jsc:x86
.start Corpus.OldNamesCorpus console

.class Corpus.OldNamesCorpus

.method static int Main(string[]) slots 24
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
    stloc   5
    ldloc   5
    ldstr   "!"
    call    string.Concat(string, string) -> string
    stloc   5
    ldstr   "notes.txt"
    ldstr   "a+"
    call    File.Open(string, string) -> FILE
    stloc   6
    ldloc   6
    call    FILE.ReadLine(out string) -> bool
    stloc   7
    brfalse L5
    ldloc   6
    ldfld   FILE.AtEnd
    ldc.i4  0
    ceq
    br      L6
L5: ldc.i4  0
L6: brfalse L3
    ldloc   6
    call    FILE.ReadLine(out string) -> bool
    stloc   7
    br      L4
L3: ldc.i4  0
L4: brfalse L1
    ldloc   6
    ldfld   FILE.AtEnd
    br      L2
L1: ldc.i4  0
L2: stloc   8
    ldloc   6
    ldloc   5
    call    FILE.Write(string) -> void
    ldloc   6
    ldc.i4  46
    call    FILE.Write(char) -> void
    ldloc   6
    ldstr   "x"
    call    FILE.Write(string) -> void
    ldloc   6
    ldc.i4  121
    call    FILE.Write(char) -> void
    ldloc   6
    call    FILE.Read() -> int
    ldloc   6
    call    FILE.Read() -> int
    add
    ldloc   6
    ldfld   FILE.Position
    add
    ldloc   6
    ldfld   FILE.Position
    add
    ldloc   6
    call    FILE.Scan(out int) -> int
    stloc   3
    add
    stloc   9
    ldloc   6
    ldc.i4  0
    call    FILE.Seek(int) -> void
    ldloc   6
    ldc.i4  2
    call    FILE.Seek(int) -> void
    ldloc   6
    ldc.i4  1
    call    FILE.Seek(int) -> void
    ldloc   6
    ldstr   ""
    ldstr   "%05d"
    ldloc   9
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "\n"
    call    string.Concat(string, string) -> string
    call    FILE.Write(string) -> void
    ldloc   6
    call    FILE.Scan(out int) -> int
    stloc   3
    stloc   10
    ldloc   6
    call    FILE.Close() -> void
    ldstr   "notes.txt"
    ldstr   "r"
    call    File.Open(string, string) -> FILE
    stloc   11
    ldloc   11
    call    FILE.Close() -> void
    ldstr   "old.txt"
    call    File.Delete(string) -> bool
    brtrue  L9
    ldstr   "a.txt"
    ldstr   "b.txt"
    call    File.Move(string, string) -> bool
    br      L10
L9: ldc.i4  1
L10: brtrue  L7
    ldstr   "b.txt"
    ldstr   "c.txt"
    call    File.Move(string, string) -> bool
    br      L8
L7: ldc.i4  1
L8: stloc   12
    ldloc   1
    call    Convert.ToDouble(string) -> double
    stloc   13
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
    stloc   14
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
    stloc   15
    ldc.i4  1987
    conv.i8
    call    Random.Seed(long) -> void
    ldc.i4  32768
    call    Random.Next(int) -> int
    stloc   16
    ldloc   1
    ldfld   string.Length
    stloc   17
    ldloc   1
    ldstr   "ingot"
    call    string.IndexOf(string) -> int
    stloc   18
    ldstr   ""
    ldloc   14
    call    Convert.ToString(object) -> string
    call    string.Concat(string, string) -> string
    ldstr   ": "
    call    string.Concat(string, string) -> string
    ldloc   16
    ldloc   17
    add
    ldloc   18
    add
    call    string.Concat(string, int) -> string
    ldstr   ", "
    call    string.Concat(string, string) -> string
    ldloc   15
    ldloc   13
    add
    call    string.Concat(string, double) -> string
    stloc   19
    ldstr   ""
    ldloc   19
    call    string.Concat(string, string) -> string
    ldstr   "\n"
    call    string.Concat(string, string) -> string
    call    Console.Print(string) -> void
    ldstr   ""
    ldstr   "%-12s"
    ldloc   19
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%5d"
    ldloc   14
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%05.2f"
    ldloc   15
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%#x"
    ldloc   16
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%e"
    ldloc   13
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "|"
    call    string.Concat(string, string) -> string
    ldstr   "%u"
    ldloc   17
    call    string.Printf(string, object) -> string
    call    string.Concat(string, string) -> string
    ldstr   "\n"
    call    string.Concat(string, string) -> string
    call    Console.Print(string) -> void
    ldloc   1
    ldloc   19
    call    string.Compare(string, string) -> int
    ldloc   19
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
    stloc   20
    ldc.i4  97
    call    char.ToUpper(char) -> char
    stloc   21
    ldc.i4  122
    call    char.ToUpper(char) -> char
    call    char.ToLower(char) -> char
    stloc   22
    ldloc   21
    call    char.IsDigit(char) -> bool
    brtrue  L21
    ldloc   22
    call    char.IsLetter(char) -> bool
    br      L22
L21: ldc.i4  1
L22: brtrue  L19
    ldc.i4  32
    call    char.IsWhiteSpace(char) -> bool
    br      L20
L19: ldc.i4  1
L20: brtrue  L17
    ldloc   21
    call    char.ToLower(char) -> char
    conv.i4
    ldc.i4  97
    conv.i4
    ceq
    br      L18
L17: ldc.i4  1
L18: brtrue  L15
    ldc.i4  49
    call    char.IsDigit(char) -> bool
    br      L16
L15: ldc.i4  1
L16: brtrue  L13
    ldc.i4  98
    call    char.IsLetter(char) -> bool
    br      L14
L13: ldc.i4  1
L14: brtrue  L11
    ldc.i4  9
    call    char.IsWhiteSpace(char) -> bool
    br      L12
L11: ldc.i4  1
L12: stloc   23
    ldloc   20
    ldc.i4  16
    call    Convert.ToString(int, int) -> string
    ldloc   16
    ldc.i4  2
    call    Convert.ToString(int, int) -> string
    call    string.Concat(string, string) -> string
    ldloc   21
    call    string.FromChar(char) -> string
    call    string.Concat(string, string) -> string
    ldloc   22
    call    string.FromChar(char) -> string
    call    string.Concat(string, string) -> string
    ldloc   23
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldloc   0
    ldlen
    ldc.i4  1
    cgt
    brfalse L23
    ldc.i4  0
    call    Program.Exit(int) -> void
L23: ldloc   0
    ldlen
    ret
