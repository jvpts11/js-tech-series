---
section: library_machine
order: 454
---
# Gateways: conversando com computadores ComputerCraft

Um Gateway liga a rede aos computadores ComputerCraft conectados por cabo. Um programa pode listar esses computadores e os periféricos deles, chamar um método de um periférico, ligar, desligar ou reiniciar um computador, e enviar a ele uma mensagem. O bloco é explicado em [](jsc:network_gateway).

Table: Membros do Gateway

| Membro | O que faz |
| --- | --- |
| `Gateway.Online` | Sim quando esta máquina tem um Gateway online. |
| `Gateway.Current` | O nome do Gateway que o programa escolheu. |
| `Gateway.Names()` | Os nomes dos Gateways, como uma lista de texto. |
| `Gateway.Select(name)` | Escolhe o Gateway que as chamadas seguintes usam, pelo nome, sem diferenciar maiúsculas de minúsculas. |
| `Gateway.Computers()` | Os computadores ComputerCraft, como uma lista de CcComputer. |
| `Gateway.Peripherals()` | Os periféricos, como uma lista de CcPeripheral. |
| `Gateway.Call(peripheral, method, args)` | Pede a um Periférico que execute um Método, com até três argumentos depois dos dois nomes, e devolve o que ele retornar. |
| `Gateway.TurnOn(id)` | Liga o computador com esse número. |
| `Gateway.Shutdown(id)` | Desliga o computador com esse número. |
| `Gateway.Reboot(id)` | Reinicia o computador com esse número. |
| `Gateway.Send(id, text)` | Envia uma mensagem para esse computador. |
| `Gateway.OnMessage(handler)` | Define o manipulador que roda para cada mensagem de um computador ComputerCraft. |

Table: Registros do Gateway

| Registro | Campos | O que cada campo guarda |
| --- | --- | --- |
| `CcComputer` | `CcComputer.Id, CcComputer.Name, CcComputer.Label, CcComputer.Online` | O número, o nome e o rótulo do computador, e se ele está ligado. |
| `CcPeripheral` | `CcPeripheral.Name, CcPeripheral.Type, CcPeripheral.Methods` | O nome e o tipo do periférico, e os nomes dos seus métodos. |
| `GatewayMessage` | `GatewayMessage.From, GatewayMessage.Text, GatewayMessage.Tick` | O número do remetente, o texto e o tick em que chegou. |

O Gateway que uma chamada usa é o que o programa escolheu com Select. Se o programa não escolheu nenhum, usa-se o primeiro Gateway que a máquina lista.

Send dá à sua mensagem o nome de evento jsc_message do lado do ComputerCraft. Uma chamada através de um Gateway custa mais do que a maioria das coisas que um programa faz, e cada argumento soma ao seu custo.

Toda chamada é registrada no log do Gateway: chamada quando foi concluída, e o texto da recusa quando não foi.

## Se algo der errado

> [!PROBLEM] O programa para e diz que este computador não tem Gateway.
> Use uma máquina que tenha um Gateway.

> [!PROBLEM] O programa para e diz que este computador não tem nenhum Gateway com esse nome.
> Confira o nome com Gateway.Names.

> [!PROBLEM] O programa para e diz que a máquina não tem o lado do ComputerCraft, e pergunta se o ComputerCraft está instalado.
> Instale o ComputerCraft e depois inicie o jogo novamente.

> [!PROBLEM] Gateway.Call interrompe o programa com o texto de erro do ComputerCraft.
> O periférico recusou a chamada. Confira o nome do método e os argumentos em Gateway.Peripherals.

> [!SEE] jsc:network_gateway, jsc:computercraft_peripheral
