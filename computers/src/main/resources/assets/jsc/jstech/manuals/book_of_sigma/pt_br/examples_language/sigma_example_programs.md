---
section: examples_language
order: 511
---
# Programas

Um programa pode iniciar outros programas, falar com eles e esperar por eles. Este exemplo dá nome a si mesmo, inicia três programas, envia uma mensagem a um deles, espera dois, para o terceiro e roda uma thread ao seu lado.

Um processo é um programa que roda na máquina e aparece na sua lista de processos. Iniciar um custa mais do que a maioria das chamadas, porque a máquina cria um processo inteiro ([](jsc:sigma_running)). Se você iniciar um processo, pode esperar por ele, ou deixá-lo rodando.

## O programa

```sgs Programs.sgs
using System.*;
using System.IO.*;
using System.Collections.*;
using System.Execution.*;
using System.Threading.*;
namespace Corpus;
class ProgramsCorpus {
    static void Main() {
        Program.SetName("corpus");
        string name = Program.Name;
        List<string> args = Program.Args;
        long dropped = Program.DroppedEvents;
        Process self = Program.Current;
        Process one = Program.Start("C:\\tool.asm");
        Process two = Program.Start("C:\\tool.asm", args);
        Process three = Program.Start("C:\\tool.asm", args, "lab");
        List<string> said = Program.Shell("dir");
        Program.OnMessage(m => Console.PrintLine(m.Text + m.From + m.Tick));
        Console.PrintLine(name + dropped + self.Id + said.Count);

        int id = one.Id;
        string oneName = one.Name;
        string host = one.Host;
        if (one.Running) {
            one.Wait();
        }
        bool done = two.Wait(20);
        int code = two.ExitCode;
        three.Kill();
        List<string> output = two.Output();
        bool sent = Process.Send(id, "hello");
        Console.PrintLine(oneName + host + done + code + output.Count + sent);

        Thread worker = Thread.Start(() => {
            Thread.Sleep(5);
            Thread.Yield();
        });
        Thread current = Thread.Current;
        int threadId = worker.Id;
        if (worker.Running) {
            worker.Join();
        }
        bool joined = worker.Join(10);
        worker.Stop();
        Console.PrintLine("" + current.Id + threadId + joined);
        Program.Exit(0);
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `Program.SetName("corpus")` | Dá nome a este programa, para que a lista de processos mostre o nome. Não custa nada. |
| `Program.Name, Args, DroppedEvents, Current` | Lê o nome do programa, os seus argumentos, o número de eventos perdidos e o seu próprio processo. |
| `Program.Start(...)` | Inicia um programa em um caminho, sem argumentos, com uma lista de argumentos, ou com uma lista e um terceiro texto. |
| `Program.Shell("dir")` | Executa uma linha no prompt da própria máquina, até o fim, e devolve o que ela imprimiu. |
| `Program.OnMessage(...)` | Pede para ser chamado a cada mensagem, e a lambda imprime a mensagem. |
| `one.Id, Name, Host` | O número, o nome e o computador de outro processo. |
| `one.Running, ExitCode` | Informa se o processo ainda está rodando, e o número que ele devolveu ao terminar. |
| `one.Wait(), two.Wait(20)` | Espera até o processo terminar, ou espera até um tempo e responde se ele terminou. |
| `three.Kill()` | Para o processo. |
| `two.Output()` | Devolve as linhas que o processo imprimiu. |
| `Process.Send(id, "hello")` | Envia o texto hello ao processo com esse número. |
| `Thread.Start(() => { ... })` | Inicia uma thread: um bloco de código que roda ao lado do programa, revezando com ele. |
| `Thread.Sleep(5), Thread.Yield()` | Pausa a thread por um curto tempo, ou deixa o outro código tomar a sua vez. |
| `Thread.Current, worker.Id, Running` | A thread que executa este código, o número do worker e se ela está rodando. |
| `worker.Join(), Join(10), Stop()` | Espera a thread terminar, espera um tempo, ou para a thread. |
| `Program.Exit(0)` | Encerra o programa e devolve 0 à máquina como o seu código de saída. |

> [!DEFINE] Código de saída
> Um número que um programa devolve ao terminar. 0 normalmente significa sucesso.

## Se algo der errado

> [!PROBLEM] O programa espera, e não segue, porque o processo não terminou.
> Use Wait com um tempo, que responde false quando o tempo acaba.

> [!PROBLEM] O compilador do Sigma recusa Thread, porque estas máquinas executam uma coisa de cada vez.
> Escreva o programa em Sigma#, a linguagem completa.

> [!SEE] jsc:sigma_running, jsc:sigma_library
