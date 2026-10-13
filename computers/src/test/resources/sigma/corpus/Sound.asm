.asm 3
.arch jsc:x86
.start Corpus.SoundCorpus console

.class Corpus.SoundCorpus

.method static void Main() slots 4
    ldc.i4  880
    ldc.i4  200
    call    Sound.Beep(int, int) -> void
    ldstr   "C4:250 E4 G4 C4+E4+G4:500"
    call    Sound.Tones(string) -> bool
    stloc   0
    ldstr   "Users/Public/Music/intro.ogg"
    call    Sound.Play(string) -> bool
    stloc   1
    call    Sound.Stop() -> void
    ldstr   "Desk left"
    call    Speaker.Named(string) -> Speaker
    stloc   2
    ldloc   2
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L1
    ldloc   2
    ldstr   "Users/Public/Music/alarm.wav"
    call    Speaker.Play(string) -> bool
    stloc   3
    ldloc   2
    ldfld   Speaker.Name
    ldloc   3
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
L1: ldstr   ""
    ldloc   0
    call    string.Concat(string, bool) -> string
    ldloc   1
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ret
