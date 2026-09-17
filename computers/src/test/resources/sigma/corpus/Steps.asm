.asm 3
.arch jsc:x86
.start Corpus.StepsCorpus console

.class Corpus.Counter
.field int Value
.field long Big
.field float Small
.field double Wide
.field static int Shared

.class Corpus.StepsCorpus
.field static int Total

.method static void Main() slots 64
    newobj  0closure1()
    stloc   0
    ldc.i4  0
    stloc   1
    ldloc   1
    dup
    ldc.i4  1
    add
    stloc   1
    pop
    ldloc   1
    dup
    ldc.i4  1
    sub
    stloc   1
    pop
    ldloc   1
    ldc.i4  1
    add
    dup
    stloc   1
    pop
    ldloc   1
    ldc.i4  1
    sub
    dup
    stloc   1
    pop
    ldloc   1
    dup
    ldc.i4  1
    add
    stloc   1
    stloc   2
    ldloc   1
    ldc.i4  1
    add
    dup
    stloc   1
    stloc   3
    ldloc   1
    dup
    ldc.i4  1
    sub
    stloc   1
    stloc   4
    ldloc   1
    ldc.i4  1
    sub
    dup
    stloc   1
    stloc   5
    ldstr   ""
    ldloc   2
    call    string.Concat(string, int) -> string
    ldloc   3
    call    string.Concat(string, int) -> string
    ldloc   4
    call    string.Concat(string, int) -> string
    ldloc   5
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldc.i4  0
    conv.i8
    stloc   6
    ldc.i4  0
    conv.r4
    stloc   7
    ldc.i4  0
    conv.r8
    stloc   8
    ldloc   6
    dup
    ldc.i8  1
    add
    stloc   6
    pop
    ldloc   7
    dup
    ldc.r4  1.0
    add
    stloc   7
    pop
    ldloc   8
    dup
    ldc.r8  1.0
    sub
    stloc   8
    pop
    ldstr   ""
    ldloc   6
    call    string.Concat(string, long) -> string
    ldloc   7
    call    string.Concat(string, float) -> string
    ldloc   8
    call    string.Concat(string, double) -> string
    call    Console.PrintLine(string) -> void
    ldsfld  Corpus.StepsCorpus.Total
    dup
    ldc.i4  1
    add
    stsfld  Corpus.StepsCorpus.Total
    pop
    ldsfld  Corpus.StepsCorpus.Total
    ldc.i4  1
    sub
    dup
    stsfld  Corpus.StepsCorpus.Total
    pop
    ldsfld  Corpus.StepsCorpus.Total
    dup
    ldc.i4  1
    add
    stsfld  Corpus.StepsCorpus.Total
    stloc   9
    ldsfld  Corpus.Counter.Shared
    dup
    ldc.i4  1
    add
    stsfld  Corpus.Counter.Shared
    pop
    ldsfld  Corpus.Counter.Shared
    ldc.i4  1
    add
    dup
    stsfld  Corpus.Counter.Shared
    stloc   10
    ldstr   ""
    ldloc   9
    call    string.Concat(string, int) -> string
    ldloc   10
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    newobj  Corpus.Counter()
    stloc   11
    ldloc   11
    dup
    ldfld   Corpus.Counter.Value
    dup
    stloc   12
    ldc.i4  1
    add
    stfld   Corpus.Counter.Value
    ldloc   12
    pop
    ldloc   11
    dup
    ldfld   Corpus.Counter.Value
    ldc.i4  1
    sub
    dup
    stloc   13
    stfld   Corpus.Counter.Value
    ldloc   13
    pop
    ldloc   11
    dup
    ldfld   Corpus.Counter.Value
    dup
    stloc   15
    ldc.i4  1
    add
    stfld   Corpus.Counter.Value
    ldloc   15
    stloc   14
    ldloc   11
    dup
    ldfld   Corpus.Counter.Value
    ldc.i4  1
    add
    dup
    stloc   17
    stfld   Corpus.Counter.Value
    ldloc   17
    stloc   16
    ldloc   11
    dup
    ldfld   Corpus.Counter.Big
    dup
    stloc   18
    ldc.i8  1
    add
    stfld   Corpus.Counter.Big
    ldloc   18
    pop
    ldloc   11
    dup
    ldfld   Corpus.Counter.Small
    dup
    stloc   19
    ldc.r4  1.0
    sub
    stfld   Corpus.Counter.Small
    ldloc   19
    pop
    ldloc   11
    dup
    ldfld   Corpus.Counter.Wide
    dup
    stloc   20
    ldc.r8  1.0
    add
    stfld   Corpus.Counter.Wide
    ldloc   20
    pop
    ldstr   ""
    ldloc   14
    call    string.Concat(string, int) -> string
    ldloc   16
    call    string.Concat(string, int) -> string
    ldloc   11
    ldfld   Corpus.Counter.Value
    call    string.Concat(string, int) -> string
    ldloc   11
    ldfld   Corpus.Counter.Big
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ldc.i4  4
    newarr  int
    stloc   21
    ldloc   21
    stloc   23
    ldc.i4  0
    stloc   24
    ldloc   23
    ldloc   24
    ldloc   23
    ldloc   24
    ldelem
    dup
    stloc   22
    ldc.i4  1
    add
    stelem
    ldloc   22
    pop
    ldloc   21
    stloc   26
    ldc.i4  1
    stloc   27
    ldloc   26
    ldloc   27
    ldloc   26
    ldloc   27
    ldelem
    ldc.i4  1
    sub
    dup
    stloc   25
    stelem
    ldloc   25
    pop
    ldloc   21
    stloc   30
    ldc.i4  2
    stloc   31
    ldloc   30
    ldloc   31
    ldloc   30
    ldloc   31
    ldelem
    dup
    stloc   29
    ldc.i4  1
    add
    stelem
    ldloc   29
    stloc   28
    ldloc   21
    stloc   34
    ldc.i4  3
    stloc   35
    ldloc   34
    ldloc   35
    ldloc   34
    ldloc   35
    ldelem
    ldc.i4  1
    add
    dup
    stloc   33
    stelem
    ldloc   33
    stloc   32
    ldstr   ""
    ldloc   28
    call    string.Concat(string, int) -> string
    ldloc   32
    call    string.Concat(string, int) -> string
    ldloc   21
    ldc.i4  0
    ldelem
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    newobj  List<int>()
    stloc   36
    ldloc   36
    ldc.i4  1
    call    List.Add(int) -> void
    ldloc   36
    stloc   38
    ldc.i4  0
    stloc   39
    ldloc   38
    ldloc   39
    ldloc   38
    ldloc   39
    call    List.Get(int) -> int
    dup
    stloc   37
    ldc.i4  1
    add
    call    List.Set(int, int) -> void
    ldloc   37
    pop
    ldloc   36
    stloc   42
    ldc.i4  0
    stloc   43
    ldloc   42
    ldloc   43
    ldloc   42
    ldloc   43
    call    List.Get(int) -> int
    dup
    stloc   41
    ldc.i4  1
    sub
    call    List.Set(int, int) -> void
    ldloc   41
    stloc   40
    ldstr   ""
    ldloc   40
    call    string.Concat(string, int) -> string
    ldloc   36
    ldc.i4  0
    call    List.Get(int) -> int
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    newobj  Map<string, int>()
    stloc   44
    ldloc   44
    ldstr   "a"
    ldc.i4  1
    call    Map.Put(string, int) -> void
    ldloc   44
    stloc   46
    ldstr   "a"
    stloc   47
    ldloc   46
    ldloc   47
    ldloc   46
    ldloc   47
    call    Map.Get(string) -> int
    dup
    stloc   45
    ldc.i4  1
    add
    call    Map.Put(string, int) -> void
    ldloc   45
    pop
    ldloc   44
    stloc   50
    ldstr   "a"
    stloc   51
    ldloc   50
    ldloc   51
    ldloc   50
    ldloc   51
    call    Map.Get(string) -> int
    ldc.i4  1
    add
    dup
    stloc   49
    call    Map.Put(string, int) -> void
    ldloc   49
    stloc   48
    ldstr   ""
    ldloc   48
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldloc   0
    ldc.i4  0
    stfld   0closure1.kept
    ldloc   0
    ldfn    0closure1.0lambda1() -> void
    stloc   52
    ldloc   52
    callvirt Action.Invoke() -> void
    ldstr   ""
    ldloc   0
    ldfld   0closure1.kept
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldc.i4  1
    stloc   53
    ldloc   53
    ldc.i4  2
    add
    stloc   53
    ldloc   53
    ldc.i4  1
    sub
    stloc   53
    ldloc   53
    ldc.i4  3
    mul
    stloc   53
    ldloc   53
    ldc.i4  2
    div
    stloc   53
    ldloc   53
    ldc.i4  4
    rem
    stloc   53
    ldloc   53
    ldc.i4  6
    and
    stloc   53
    ldloc   53
    ldc.i4  1
    or
    stloc   53
    ldloc   53
    ldc.i4  3
    xor
    stloc   53
    ldloc   53
    ldc.i4  2
    shl
    stloc   53
    ldloc   53
    ldc.i4  1
    shr
    stloc   53
    ldstr   ""
    ldloc   53
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "a"
    stloc   54
    ldloc   54
    ldstr   "b"
    call    string.Concat(string, string) -> string
    stloc   54
    ldloc   54
    ldloc   53
    call    string.Concat(string, int) -> string
    stloc   54
    ldloc   54
    call    Console.PrintLine(string) -> void
    ldloc   11
    dup
    ldfld   Corpus.Counter.Value
    ldc.i4  5
    add
    stfld   Corpus.Counter.Value
    ldsfld  Corpus.Counter.Shared
    ldc.i4  2
    add
    stsfld  Corpus.Counter.Shared
    ldloc   21
    stloc   55
    ldc.i4  0
    stloc   56
    ldloc   55
    ldloc   56
    ldloc   55
    ldloc   56
    ldelem
    ldc.i4  3
    add
    stelem
    ldloc   36
    stloc   57
    ldc.i4  0
    stloc   58
    ldloc   57
    ldloc   58
    ldloc   57
    ldloc   58
    call    List.Get(int) -> int
    ldc.i4  4
    add
    call    List.Set(int, int) -> void
    ldloc   11
    dup
    ldfld   Corpus.Counter.Value
    ldc.i4  1
    add
    stfld   Corpus.Counter.Value
    ldloc   11
    ldfld   Corpus.Counter.Value
    stloc   59
    ldloc   21
    stloc   61
    ldc.i4  0
    stloc   62
    ldloc   61
    ldloc   62
    ldloc   61
    ldloc   62
    ldelem
    ldc.i4  1
    add
    stelem
    stloc   60
    ldc.i4  7
    dup
    stloc   1
    stloc   63
    ldstr   ""
    ldloc   59
    call    string.Concat(string, int) -> string
    ldloc   60
    call    string.Concat(string, int) -> string
    ldloc   63
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ret

.class 0closure1
.field int kept

.method void 0lambda1() slots 2
    ldthis
    ldfld   0closure1.kept
    ldthis
    ldthis
    ldfld   0closure1.kept
    ldc.i4  1
    add
    stfld   0closure1.kept
    pop
    ldthis
    ldfld   0closure1.kept
    ldthis
    ldthis
    ldfld   0closure1.kept
    ldc.i4  1
    add
    stfld   0closure1.kept
    stloc   0
    ldthis
    ldthis
    ldfld   0closure1.kept
    ldc.i4  1
    add
    stfld   0closure1.kept
    ldthis
    ldfld   0closure1.kept
    stloc   1
    ldthis
    ldfld   0closure1.kept
    ldthis
    ldthis
    ldfld   0closure1.kept
    ldc.i4  1
    sub
    stfld   0closure1.kept
    pop
    ldstr   ""
    ldloc   0
    call    string.Concat(string, int) -> string
    ldloc   1
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ret
