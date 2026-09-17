.asm 3
.arch jsc:x86
.start Corpus.ValuesCorpus console

.struct Corpus.Vec
.field int X
.field int Y

.class Corpus.Held
.field Corpus.Vec Spot
.field static Corpus.Vec Origin

.method void .ctor() slots 0
    ldthis
    newobj  Corpus.Vec()
    stfld   Spot
    ret

.method static void .cctor() slots 0
    newobj  Corpus.Vec()
    stsfld  Corpus.Held.Origin
    ret

.class Corpus.ValuesCorpus
.field static Corpus.Vec Kept

.method static void Main() slots 39
    newobj  Corpus.Vec()
    stloc   0
    ldloc   0
    ldc.i4  1
    stfld   Corpus.Vec.X
    ldloc   0
    ldc.i4  2
    stfld   Corpus.Vec.Y
    ldloc   0
    copy
    stloc   1
    ldloc   1
    ldc.i4  9
    stfld   Corpus.Vec.X
    ldstr   ""
    ldloc   0
    ldfld   Corpus.Vec.X
    call    string.Concat(string, int) -> string
    ldloc   1
    ldfld   Corpus.Vec.X
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldloc   0
    copy
    stsfld  Corpus.ValuesCorpus.Kept
    newobj  Corpus.Held()
    stloc   2
    ldloc   2
    ldloc   0
    copy
    stfld   Corpus.Held.Spot
    ldloc   0
    copy
    stsfld  Corpus.Held.Origin
    ldstr   ""
    ldsfld  Corpus.ValuesCorpus.Kept
    ldfld   Corpus.Vec.X
    call    string.Concat(string, int) -> string
    ldloc   2
    ldfld   Corpus.Held.Spot
    ldfld   Corpus.Vec.Y
    call    string.Concat(string, int) -> string
    ldsfld  Corpus.Held.Origin
    ldfld   Corpus.Vec.X
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldloc   0
    copy
    call    Corpus.ValuesCorpus.Same(Corpus.Vec) -> Corpus.Vec
    copy
    stloc   3
    ldstr   ""
    ldloc   3
    ldfld   Corpus.Vec.X
    call    string.Concat(string, int) -> string
    ldloc   3
    ldfld   Corpus.Vec.Y
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldc.i4  3
    newarr  Vec
    stloc   4
    ldloc   4
    stloc   5
    ldc.i4  0
    stloc   6
    ldloc   5
    ldloc   6
    ldloc   0
    copy
    stelem
    ldloc   4
    stloc   7
    ldc.i4  1
    stloc   8
    ldloc   7
    ldloc   8
    ldloc   1
    copy
    stelem
    ldstr   ""
    ldloc   4
    ldc.i4  0
    ldelem
    ldfld   Corpus.Vec.X
    call    string.Concat(string, int) -> string
    ldloc   4
    ldc.i4  1
    ldelem
    ldfld   Corpus.Vec.X
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    newobj  List<Corpus.Vec>()
    stloc   9
    ldloc   9
    ldloc   0
    copy
    call    List.Add(Corpus.Vec) -> void
    ldloc   9
    ldloc   1
    copy
    call    List.Add(Corpus.Vec) -> void
    ldstr   ""
    ldloc   9
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldc.i4  0
    stloc   10
    ldloc   4
    stloc   11
    ldc.i4  0
    stloc   12
L1: ldloc   12
    ldloc   11
    ldlen
    bge     L3
    ldloc   11
    ldloc   12
    ldelem
    copy
    stloc   13
    ldloc   10
    ldloc   13
    ldfld   Corpus.Vec.X
    add
    stloc   10
L2: ldloc   12
    ldc.i4  1
    add
    stloc   12
    br      L1
L3: ldloc   9
    stloc   14
    ldc.i4  0
    stloc   15
L4: ldloc   15
    ldloc   14
    ldfld   List.Count
    bge     L6
    ldloc   14
    ldloc   15
    call    List.Get(int) -> Corpus.Vec
    copy
    stloc   16
    ldloc   10
    ldloc   16
    ldfld   Corpus.Vec.Y
    add
    stloc   10
L5: ldloc   15
    ldc.i4  1
    add
    stloc   15
    br      L4
L6: ldstr   ""
    ldloc   10
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldc.i4  4
    newarr  int
    stloc   17
    ldloc   17
    stloc   18
    ldc.i4  0
    stloc   19
    ldloc   18
    ldloc   19
    ldc.i4  5
    stelem
    ldloc   17
    stloc   20
    ldc.i4  3
    stloc   21
    ldloc   20
    ldloc   21
    ldc.i4  7
    stelem
    ldc.i4  0
    stloc   22
    ldloc   17
    stloc   23
    ldc.i4  0
    stloc   24
L7: ldloc   24
    ldloc   23
    ldlen
    bge     L9
    ldloc   23
    ldloc   24
    ldelem
    stloc   25
    ldloc   22
    ldloc   25
    add
    stloc   22
L8: ldloc   24
    ldc.i4  1
    add
    stloc   24
    br      L7
L9: ldstr   ""
    ldloc   22
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldc.i4  2
    newarr  string
    stloc   26
    ldloc   26
    stloc   27
    ldc.i4  0
    stloc   28
    ldloc   27
    ldloc   28
    ldstr   "a"
    stelem
    ldloc   26
    stloc   29
    ldc.i4  1
    stloc   30
    ldloc   29
    ldloc   30
    ldstr   "b"
    stelem
    ldstr   ""
    stloc   31
    ldloc   26
    stloc   32
    ldc.i4  0
    stloc   33
L10: ldloc   33
    ldloc   32
    ldlen
    bge     L12
    ldloc   32
    ldloc   33
    ldelem
    stloc   34
    ldloc   31
    ldloc   34
    call    string.Concat(string, string) -> string
    stloc   31
L11: ldloc   33
    ldc.i4  1
    add
    stloc   33
    br      L10
L12: ldloc   31
    call    Console.PrintLine(string) -> void
    newobj  List<string>()
    stloc   35
    ldloc   35
    ldstr   "x"
    call    List.Add(string) -> void
    ldloc   35
    stloc   36
    ldc.i4  0
    stloc   37
L13: ldloc   37
    ldloc   36
    ldfld   List.Count
    bge     L15
    ldloc   36
    ldloc   37
    call    List.Get(int) -> string
    stloc   38
    ldloc   38
    call    Console.PrintLine(string) -> void
L14: ldloc   37
    ldc.i4  1
    add
    stloc   37
    br      L13
L15: ret

.method static Corpus.Vec Same(Corpus.Vec) slots 2
    ldloc   0
    copy
    stloc   1
    ldloc   1
    copy
    ret

.method static void .cctor() slots 0
    newobj  Corpus.Vec()
    stsfld  Corpus.ValuesCorpus.Kept
    ret
