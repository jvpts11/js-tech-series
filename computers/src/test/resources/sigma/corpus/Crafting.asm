.asm 3
.arch jsc:x86
.start Corpus.CraftingCorpus console

.class Corpus.CraftingCorpus

.method static void Main() slots 4
    ldstr   "Kiln"
    call    CraftInterface.Named(string) -> CraftInterface
    stloc   0
    ldloc   0
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L1
    ldloc   0
    ldc.i4  1
    call    CraftInterface.Exclusive(bool) -> CraftInterface
    ldc.i4  4
    call    CraftInterface.MaxJobs(int) -> CraftInterface
    call    CraftInterface.Pause() -> CraftInterface
    pop
    ldloc   0
    call    CraftInterface.Resume() -> CraftInterface
    pop
    ldloc   0
    ldfld   CraftInterface.Name
    call    Console.PrintLine(string) -> void
L1: ldstr   "North"
    call    CraftRouter.Named(string) -> CraftRouter
    stloc   1
    ldloc   1
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L2
    ldloc   1
    ldstr   "gravel"
    call    CraftRouter.Only(string) -> CraftRouter
    ldc.i4  0
    call    CraftRouter.Fuzzy(bool) -> CraftRouter
    pop
    ldloc   1
    ldstr   "dirt"
    call    CraftRouter.AllBut(string) -> CraftRouter
    pop
    ldloc   1
    ldstr   "c:sands"
    call    CraftRouter.Tag(string) -> CraftRouter
    pop
    ldloc   1
    call    CraftRouter.Any() -> CraftRouter
    pop
    ldloc   1
    ldfld   CraftRouter.Name
    call    Console.PrintLine(string) -> void
L2: ldstr   "Mixer"
    call    CraftInterface.Named(string) -> CraftInterface
    stloc   2
    ldstr   "West"
    call    CraftRouter.Named(string) -> CraftRouter
    stloc   3
    ldloc   2
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L4
    ldloc   3
    ldnull
    ceq
    ldc.i4  0
    ceq
    br      L5
L4: ldc.i4  0
L5: brfalse L3
    ldloc   2
    ldstr   "Coarse dirt"
    ldstr   "gravel"
    ldloc   3
    call    CraftInterface.Route(string, string, CraftRouter) -> CraftInterface
    pop
    ldloc   2
    ldstr   "Coarse dirt"
    ldstr   "dirt"
    ldnull
    call    CraftInterface.Route(string, string, CraftRouter) -> CraftInterface
    pop
L3: ret
