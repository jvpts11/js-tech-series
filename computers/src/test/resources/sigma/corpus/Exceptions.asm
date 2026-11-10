.asm 4
.arch jsc:x86
.start Corpus.ExceptionsCorpus console

.class Corpus.Refused : InvalidOperationException
.source Exceptions.sgs

.method void .ctor(string, Exception) slots 2
    ldthis
.line 5
    ldloc   0
    ldloc   1
    call    InvalidOperationException..ctor(string, Exception) -> void
    ret

.class Corpus.ExceptionsCorpus
.source Exceptions.sgs

.method static void Check(int) slots 2
.line 9
    ldloc   0
    stloc   1
    ldloc   1
.line 10
    ldc.i4  0
    beq     L2
    ldloc   1
.line 11
    ldc.i4  1
    beq     L3
    ldloc   1
.line 12
    ldc.i4  2
    beq     L4
    ldloc   1
.line 13
    ldc.i4  3
    beq     L5
    ldloc   1
.line 14
    ldc.i4  4
    beq     L6
    ldloc   1
.line 15
    ldc.i4  5
    beq     L7
    ldloc   1
.line 16
    ldc.i4  6
    beq     L8
    ldloc   1
.line 17
    ldc.i4  7
    beq     L9
    ldloc   1
.line 18
    ldc.i4  8
    beq     L10
    ldloc   1
.line 19
    ldc.i4  9
    beq     L11
    ldloc   1
.line 20
    ldc.i4  10
    beq     L12
    ldloc   1
.line 21
    ldc.i4  11
    beq     L13
    br      L14
.line 10
L2: newobj  Exception()
    throw
.line 11
L3: ldstr   "nothing"
    newobj  NullReferenceException(string)
    throw
.line 12
L4: ldstr   "not now"
    newobj  InvalidOperationException(string)
    throw
.line 13
L5: ldstr   "freed"
    newobj  ObjectDisposedException(string)
    throw
.line 14
L6: ldstr   "zero"
    newobj  DivideByZeroException(string)
    throw
.line 15
L7: ldstr   "past the end"
    newobj  IndexOutOfRangeException(string)
    throw
.line 16
L8: ldstr   "wrong"
    newobj  ArgumentException(string)
    throw
.line 17
L9: ldstr   "too big"
    newobj  ArgumentOutOfRangeException(string)
    throw
.line 18
L10: ldstr   "not a number"
    newobj  FormatException(string)
    throw
.line 19
L11: ldstr   "disk"
    newobj  IOException(string)
    throw
.line 20
L12: ldstr   "missing"
    newobj  FileNotFoundException(string)
    throw
.line 21
L13: ldstr   "no network"
    newobj  NetworkException(string)
    throw
.line 22
L14: ldstr   "refused"
    ldstr   "under"
    newobj  Exception(string)
    newobj  Corpus.Refused(string, Exception)
    throw
L1: ret

.method static void Main() slots 5
.try L4 L7 catch IOException L7 L8
.try L4 L7 catch Exception L8 L6
.try L4 L6 finally L6 L11
.line 27
    ldc.i4  0
    stloc   0
L1: ldloc   0
    ldc.i4  12
    cgt
    ldc.i4  0
    ceq
    brfalse L3
.line 29
L4: ldloc   0
    call    Corpus.ExceptionsCorpus.Check(int) -> void
    leave   L11
L7: stloc   1
    ldloc   1
    stloc   2
.line 31
    ldstr   "io "
    ldloc   2
    ldfld   Exception.Message
    call    string.Concat(string, string) -> string
    ldstr   " "
    call    string.Concat(string, string) -> string
    ldloc   2
    ldfld   Exception.File
    call    string.Concat(string, string) -> string
    ldstr   ":"
    call    string.Concat(string, string) -> string
    ldloc   2
    ldfld   Exception.Line
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    leave   L11
L8: stloc   3
    ldloc   3
    stloc   4
.line 33
    ldloc   4
    ldfld   Exception.Message
    ldstr   " "
    call    string.Concat(string, string) -> string
    ldloc   4
    ldfld   Exception.Trace
    ldlen
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
.line 34
    ldloc   4
    ldfld   Exception.Inner
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L9
.line 35
    ldstr   "under "
    ldloc   4
    ldfld   Exception.Inner
    ldfld   Exception.Message
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
L9: leave   L11
.line 38
L6: ldstr   "checked "
    ldloc   0
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    endfinally
.line 27
L11: ldloc   0
    dup
    ldc.i4  1
    add
    stloc   0
    pop
    br      L1
L3: ret

.class Exception
.field string Message
.field string File
.field int Line
.field string[] Trace
.field Exception Inner

.method void .ctor() slots 0
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    stfld   Message
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    stfld   Message
    ldthis
    ldloc   1
    stfld   Inner
    ret

.class NullReferenceException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class InvalidOperationException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class ObjectDisposedException : InvalidOperationException

.method void .ctor() slots 0
    ldthis
    call    InvalidOperationException..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    InvalidOperationException..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    InvalidOperationException..ctor(string, Exception) -> void
    ret

.class DivideByZeroException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class IndexOutOfRangeException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class ArgumentException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class ArgumentOutOfRangeException : ArgumentException

.method void .ctor() slots 0
    ldthis
    call    ArgumentException..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    ArgumentException..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    ArgumentException..ctor(string, Exception) -> void
    ret

.class FormatException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class IOException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class FileNotFoundException : IOException

.method void .ctor() slots 0
    ldthis
    call    IOException..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    IOException..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    IOException..ctor(string, Exception) -> void
    ret

.class NetworkException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class AccessViolationException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret
