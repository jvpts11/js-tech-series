.asm 3
.arch jsc:x86
.start Corpus.RedstoneCorpus console

.class Corpus.RedstoneCorpus

.method static void Main() slots 2
    ldstr   "Gate"
    call    Redstone.Named(string) -> Redstone
    stloc   0
    ldloc   0
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L1
    ldloc   0
    call    Redstone.In() -> Redstone
    ldc.i4  15
    call    Redstone.Out(int) -> Redstone
    pop
    ldloc   0
    ldfld   Redstone.Name
    ldloc   0
    call    Redstone.Level() -> int
    call    Convert.ToString(object) -> string
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
L1: ldstr   "Lever"
    call    Redstone.Named(string) -> Redstone
    stloc   1
    ldloc   1
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L2
    ldloc   1
    ldfld   Redstone.Name
    call    Console.PrintLine(string) -> void
L2: ret
