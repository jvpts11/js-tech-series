---
section: examples_network
order: 502
---
# Rede

Este exemplo lê a rede: quanto ela guarda, quais servidores mantêm um item e quais computadores estão nela. Ele também observa um estoque, pede tarefas à rede e inicia um programa em outro computador.

A rede é todo computador e dispositivo ligado por cabo de dados a um Mainframe ([](jsc:what_a_network_is)). O exemplo a lê pelo tipo Network da biblioteca, e pede ao Mainframe pelo seu tipo Mainframe.

## O programa

```sgs Network.sgs
using System.*;
using System.IO.*;
using System.Collections.*;
using System.Network.*;
using System.Operations.*;
using System.Execution.*;
namespace Corpus;
class NetworkCorpus {
    static void Main() {
        bool online = Network.Online;
        string current = Network.Current;
        long stored = Network.Capacity + Network.Used + Network.Total("minecraft:iron_ingot");
        List<string> types = Network.Types();
        Console.PrintLine("" + online + current + stored + types.Count);
        foreach (HoldingInfo holding in Network.Find("minecraft:iron_ingot")) {
            Console.PrintLine(holding.Server + holding.Quantity);
        }
        foreach (ServerInfo server in Network.Servers()) {
            Console.PrintLine(server.Name + server.Stored + server.Capacity);
        }
        Subscription any = Network.Watch("minecraft:iron_ingot", Told);
        Subscription below = Network.WatchBelow("minecraft:iron_ingot", 10, Told);
        Subscription above = Network.WatchAbove("minecraft:iron_ingot", 100, Told);
        Console.PrintLine("" + any.Id + any.Item + below.Id + above.Id);

        RemoteComputer desk = Network.Computer("desk");
        List<RemoteComputer> all = Network.Computers();
        Console.PrintLine(desk.Host + desk.Name + desk.Type + desk.Os + desk.Online + all.Count);
        List<string> args = new List<string>();
        Process one = desk.Start("C:\\tool.asm");
        Process two = desk.Start("C:\\tool.asm", args);
        Process three = desk.Start("C:\\tool.asm", args, "lab");
        List<string> shell = desk.Shell("dir");
        bool sent = desk.Send(one.Id, "hi");
        List<Process> processes = desk.Processes();
        Console.PrintLine("" + two.Id + three.Id + shell.Count + sent + processes.Count);

        IqlResult result = Iql.Run("QUERY items");
        List<Map<string, object>> rows = Iql.Query("QUERY items");
        IqlResult exec = Iql.Exec("restock");
        IqlResult execWith = Iql.Exec("restock", args);
        IqlResult file = Iql.RunFile("C:\\job.iql");
        Console.PrintLine("" + result.Ok + result.Message + result.Rows.Count + rows.Count);
        Console.PrintLine("" + exec.Ok + execWith.Ok + file.Ok);

        bool mainframe = Mainframe.Online;
        int peak = Mainframe.PeakToday;
        WorkStat stat = Mainframe.Stats("PULL");
        List<WorkStat> work = Mainframe.Work();
        Console.PrintLine("" + mainframe + peak + stat.Type + stat.Count + stat.AverageWait + stat.AverageRun);
        Console.PrintLine("" + stat.ShortfallPercent + stat.Moved + work.Count);

        AskResult pulled = Operations.Pull("minecraft:oak_log", 64);
        AskResult pushed = Operations.Push("minecraft:oak_log", 64);
        AskResult crafted = Operations.Craft("minecraft:stick", 4);
        AskResult cancelled = Operations.Cancel("op-1");
        AskResult raised = Operations.Reprioritise("op-1", "HIGH");
        OperationInfo info = Operations.Get("op-1");
        List<OperationInfo> list = Operations.List();
        Console.PrintLine("" + pulled.Ok + pulled.Message + pushed.Ok + crafted.Ok + cancelled.Ok + raised.Ok);
        Console.PrintLine(info.Id + info.Type + info.Item + info.Moved + info.Requested + info.Status + info.Priority);
        Console.PrintLine("" + list.Count);
    }

    static void Told(StockEvent told) {
        Console.PrintLine(told.Item + told.Total + told.Previous);
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `Network.Online` | Informa se este computador está em uma rede. |
| `Network.Capacity, Used, Total(...)` | Quanto a rede pode guardar, quanto ela guarda e quanto dá um item. |
| `Network.Types()` | Os tipos de item que a rede guarda. |
| `Network.Find(...)` | Cada servidor que guarda o item, com a quantidade que guarda. |
| `Network.Servers()` | Cada servidor, com o seu nome, o que armazena e a sua capacidade. |
| `Network.Watch, WatchBelow, WatchAbove` | Pede para ser avisado quando um estoque mudar, ou cair abaixo ou subir acima de um número. |
| `Network.Computer(...)` | Encontra um computador da rede pelo nome. |
| `Network.Computers()` | Lista cada computador da rede. |
| `desk.Start(...)` | Inicia um programa nesse computador, com ou sem argumentos. |
| `desk.Shell(...)` | Executa uma linha no prompt daquele computador e devolve o que ele imprimiu. |
| `desk.Send(...)` | Envia um texto a um dos seus processos. |
| `desk.Processes()` | Lista os seus processos. |
| `Iql.Run(...)` | Executa uma instrução da linguagem de consulta da rede (IQL). Não para o programa quando a instrução é recusada. |
| `Iql.Query(...)` | Executa uma consulta e devolve as suas linhas. Para o programa quando a consulta é recusada. |
| `Iql.Exec(...)` | Executa um procedimento pelo nome, com ou sem uma lista de argumentos. |
| `Iql.RunFile(...)` | Executa cada instrução de um arquivo. |
| `Mainframe.Online, PeakToday` | Se o Mainframe está online, e o seu pico do dia. |
| `Mainframe.Stats(...), Work()` | O trabalho feito para um tipo, e o trabalho feito para todos os tipos. |
| `Operations.Pull, Push, Craft` | Pede à rede uma tarefa de um tipo. Cada uma responde com uma bandeira Ok e uma Message. |
| `Operations.Cancel, Reprioritise` | Cancela uma tarefa, ou muda a sua prioridade. |
| `Operations.Get, List()` | Lê uma tarefa, ou todas as tarefas. |

Observar é melhor que perguntar. O manipulador Told é chamado quando o estoque muda, então o programa não precisa perguntar a cada tick. Told imprime o item, o total e o estoque antes da mudança.

> [!DEFINE] Estoque
> O estoque de um item que um servidor guarda, com a quantidade.

> [!DEFINE] Assinatura
> Um pedido para que a rede chame um manipulador quando um estoque mudar.

> [!DEFINE] Solicitação
> Um pedido enviado à rede. Ele responde se foi aceito e, se não foi, por quê, sem parar o programa.

> [!DEFINE] IQL
> A linguagem de consulta da rede: instruções curtas, escritas como texto, que leem ou alteram a rede.

## Se algo der errado

> [!PROBLEM] Online é false: este computador não está em uma rede.
> Conecte o computador à rede com um cabo de dados até um Mainframe ([](jsc:first_network)).

> [!PROBLEM] Uma consulta é recusada, e o programa para com uma mensagem do motor de consultas da rede.
> Leia a mensagem, ou use Iql.Run, que não para em uma instrução recusada.

> [!PROBLEM] Get dá null para uma tarefa que já terminou.
> Teste a resposta contra null antes de ler os seus campos.

> [!SEE] jsc:iql, jsc:mainframes, jsc:sigma_library
