---
section: library_reach
order: 467
---
# EventLog e Channel: falar com outros programas

O seu programa pode escrever no log de eventos da rede e pode enviar valores para programas em outros computadores. Você faz as duas coisas pelo EventLog e pelo Channel, tipos embutidos do System.Network. Os dois vieram com o Sigma# 3, e os dois precisam que a máquina esteja em uma rede: numa máquina que não está em nenhuma, uma chamada para o programa com uma NetworkException, que você pode capturar.

Table: Membros do EventLog e do Channel

| Membro | O que faz |
| --- | --- |
| `EventLog.Write(level, text)` | Escreve uma entrada no log de eventos da rede, a partir desta máquina, no nível info, warn ou error. A entrada é uma linha de até 256 caracteres. Todo programa que escuta pelo Network.OnEvent a ouve. |
| `Channel.Listen(name, handler)` | Chama o handler com um ChannelMessage cada vez que um valor é enviado no canal com esse nome. Uma chamada posterior para o mesmo canal toma o lugar da anterior. |
| `Channel.Close(name)` | Deixa de ouvir o canal. |
| `Channel.Send(name, value)` | Envia uma cópia do valor no canal para os programas das outras máquinas que o ouvem: nesta rede, e nas redes federadas com ela que a deixam enviar para lá. |

Table: Registros do Channel

| Registro | Campos | O que cada campo guarda |
| --- | --- | --- |
| `ChannelMessage` | `ChannelMessage.Channel, ChannelMessage.From, ChannelMessage.Value, ChannelMessage.Tick` | O canal por onde veio, a máquina que o enviou, a cópia do valor e o tick em que chegou. |

O nome de um canal tem até 64 caracteres. Um valor viaja como cópia, como faz o Data de um GenericComponent: texto, números, sim e não, e listas, mapas e registros deles, até 32 KB ao todo. Quando você muda a sua cópia, nada muda em quem enviou.

Um valor leva um tick para atravessar a sua rede, mais um quando o computador que o envia está no Wi-Fi, e um para cada fronteira entre redes federadas que atravessa. Enviar custa 200 créditos, como pedir uma Operation; uma entrada no log custa 100 créditos, como uma escrita num arquivo. Escutar não custa nada, o handler roda com os créditos do seu programa, e o que o seu programa ouve é guardado quando o mundo é salvo.

```sgs Door.sgs
Channel.Listen("door", message =>
        Console.PrintLine(message.From + " says " + message.Value));
Channel.Send("door", "open");
EventLog.Write("warn", "The north door is open");
```

## Se algo der errado

> [!PROBLEM] O programa para e diz que este computador não está em uma rede.
> O EventLog e o Channel precisam de uma rede. Confira o Network.Online primeiro.

> [!PROBLEM] Um programa nunca ouve o que você envia.
> Ele só ouve um canal que escuta, pelo mesmo nome, e só de outra máquina. Através de uma federação, confira se ela deixa a rede de quem envia mandar para a rede de quem escuta.

> [!SEE] jsc:network_watches, jsc:wan_gateways
