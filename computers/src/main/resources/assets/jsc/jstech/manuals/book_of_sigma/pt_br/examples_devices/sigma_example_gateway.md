---
section: examples_devices
order: 508
---
# Gateway

Um Gateway liga a rede ao ComputerCraft: os computadores e periféricos de outro mod, pelos fios que eles usam. Este exemplo lê o Gateway, chama um periférico, liga e desliga um computador, envia uma mensagem e recebe mensagens.

Se você não tiver o ComputerCraft instalado, o Gateway não tem um lado que alcance os seus computadores.

As permissões de um Gateway limitam o que ele deixa passar ([](jsc:gateway_permissions)).

## O programa

```sgs Gateway.sgs
using System.*;
using System.IO.*;
using System.Collections.*;
using System.Network.*;
namespace Corpus;
class GatewayCorpus {
    static void Main() {
        bool online = Gateway.Online;
        string current = Gateway.Current;
        List<string> names = Gateway.Names();
        bool chosen = Gateway.Select("east");
        Console.PrintLine("" + online + current + names.Count + chosen);
        foreach (CcComputer computer in Gateway.Computers()) {
            Console.PrintLine("" + computer.Id + computer.Name + computer.Label + computer.Online);
        }
        foreach (CcPeripheral device in Gateway.Peripherals()) {
            Console.PrintLine(device.Name + device.Type + device.Methods.Count);
        }

        object none = Gateway.Call("left", "getName");
        object one = Gateway.Call("left", "setOutput", true);
        object two = Gateway.Call("left", "setOutput", "top", true);
        object three = Gateway.Call("left", "setOutput", "top", true, 1);
        bool powered = Gateway.TurnOn(1) && Gateway.Shutdown(1) && Gateway.Reboot(1);
        bool sent = Gateway.Send(1, "hello");
        Gateway.OnMessage(Heard);
        Console.PrintLine("" + none + one + two + three + powered + sent);
    }

    static void Heard(GatewayMessage message) {
        Console.PrintLine("" + message.From + message.Text + message.Tick);
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `Gateway.Online` | Informa se o Gateway está online. |
| `Gateway.Current` | O nome do Gateway que está selecionado. |
| `Gateway.Names()` | Os nomes dos Gateways da máquina. |
| `Gateway.Select("east")` | Seleciona o Gateway chamado east, e responde true quando ele é escolhido. |
| `Gateway.Computers()` | Lista os computadores do ComputerCraft, cada um com seu número, nome, rótulo e se está online. |
| `Gateway.Peripherals()` | Lista os periféricos, cada um com seu nome, seu tipo e seu número de métodos. |
| `Gateway.Call("left", "getName")` | Chama o método getName no periférico left, sem argumento. |
| `Gateway.Call(...)` | Chama o mesmo método com um argumento, com dois e com três. |
| `Gateway.TurnOn(1), Shutdown(1), Reboot(1)` | Liga, desliga ou reinicia o computador ComputerCraft de número 1, e responde true ou false. |
| `Gateway.Send(1, "hello")` | Envia o texto hello para o computador de número 1. |
| `Gateway.OnMessage(Heard)` | Define Heard como o método chamado a cada mensagem que chega. |
| `Heard(GatewayMessage message)` | Imprime o número do remetente, o texto e o tick em que chegou. |

> [!DEFINE] ComputerCraft
> Outro mod com computadores e periféricos próprios, que um Gateway alcança.

> [!DEFINE] Periférico
> Um dispositivo ligado a um computador que este controla pelos seus métodos.

> [!DEFINE] Teto de prioridade
> A prioridade mais alta de trabalho que um Gateway deixa passar para a rede.

## Se algo der errado

> [!PROBLEM] A máquina não tem Gateway, e o programa para com "este computador não tem um Gateway".
> Coloque um Gateway na rede da máquina, depois selecione-o com Select.

> [!PROBLEM] O nome dado a Select não é um Gateway, e o programa para com "este computador não tem um Gateway chamado east".
> Use um nome da lista que Names fornece.

> [!PROBLEM] Uma chamada vem antes de qualquer Gateway ser selecionado, e o programa para com "selecione um gateway primeiro".
> Chame Select primeiro, com o nome de um Gateway.

> [!PROBLEM] O ComputerCraft não está instalado, então o Gateway não tem um lado para ele, e o programa para.
> Instale o ComputerCraft, porque o Gateway funciona somente com ele.

> [!PROBLEM] O Gateway não tem um Mainframe com quem trabalhar, e o programa para com "nenhum Mainframe nesta rede".
> Coloque um Mainframe na rede.

> [!SEE] jsc:network_gateway, jsc:gateway_permissions, jsc:sigma_library
