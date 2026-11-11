---
section: library_network
order: 445
---
# A rede: o que ela guarda e ser avisado quando isso muda

A rede é todo computador e dispositivo ligado por cabo de dados a um único Mainframe, a máquina que executa as operações da rede. Um programa em uma máquina que está na rede pode perguntar o que a rede guarda, e pode pedir para ser avisado quando isso muda.

Uma máquina que não está em nenhuma rede não consegue fazer nada disso. Só Network.Online e Network.Current respondem ali, e qualquer outra chamada interrompe o programa.

Table: Membros de Network

| Membro | O que faz |
| --- | --- |
| `Network.Online` | Sim quando a máquina está em uma rede. |
| `Network.Current` | O id curto da rede em que a máquina está, ou null quando ela não está em nenhuma. |
| `Network.Capacity` | O que os servidores da rede conseguem guardar, como um inteiro longo. |
| `Network.Used` | O que os servidores da rede guardam, como um inteiro longo. |
| `Network.Total(item)` | O número desse item em toda a rede. |
| `Network.Types()` | Os nomes dos itens que a rede guarda, como uma lista. |
| `Network.Find(item)` | Uma lista de HoldingInfo, um para cada servidor que guarda o item. |
| `Network.Servers()` | Uma lista de ServerInfo, uma para cada servidor. |
| `Network.Watch(item, handler)` | Chama o manipulador quando o total da rede desse item muda. Devolve uma Subscription. |
| `Network.WatchBelow(item, threshold, handler)` | Chama o manipulador quando o total cai até o limite ou abaixo dele. Devolve uma Subscription. |
| `Network.WatchAbove(item, threshold, handler)` | Chama o manipulador quando o total sobe até o limite ou acima dele. Devolve uma Subscription. |
| `Network.Computers()` | Uma lista dos outros computadores da rede, como valores RemoteComputer. |
| `Network.Computer(host)` | O computador com esse nome de host, ou null quando não há nenhum. |
| `Network.OnEvent(handler)` | Chama o manipulador com um NetworkEvent cada vez que uma entrada é escrita no log de eventos da rede. Adicionado na versão 3. |

Todo membro de Rede está disponível apenas no Sigma#. Total conta cada servidor que guarda o item.

## O que os registros guardam

Table: Registros de Network

| Registro | Campos | O que cada campo guarda |
| --- | --- | --- |
| `HoldingInfo` | `HoldingInfo.Server, HoldingInfo.Quantity` | O nome de um servidor, e quanto do item ele guarda. |
| `ServerInfo` | `ServerInfo.Name, ServerInfo.Stored, ServerInfo.Capacity` | O nome de um servidor, o que ele guarda e o que consegue guardar. |
| `StockEvent` | `StockEvent.Item, StockEvent.Total, StockEvent.Previous` | O item, o seu novo total e o seu total antes da mudança. |
| `Subscription` | `Subscription.Id, Subscription.Item` | O número do observador, e o item que ele observa. |
| `NetworkEvent` | `NetworkEvent.Level, NetworkEvent.Machine, NetworkEvent.Text, NetworkEvent.Tick` | A gravidade da entrada (info, warn ou error), a máquina de que ela trata, o que ela diz e o tick em que foi escrita. |

## Observadores

Um observador é mantido pelo seu programa, não pela máquina. A cada tick, a máquina é consultada sobre os totais de tudo o que está sendo observado, e os observadores que disparam rodam na ordem em que você os definiu.

Um observador não dispara na sua primeira leitura. Um programa que começa com um total baixo não é avisado de que ele caiu. WatchBelow dispara uma vez quando o total chega ao limite ou abaixo dele, depois de ter estado acima. Ele se rearma quando o total volta a ficar acima, para que possa disparar de novo. WatchAbove funciona ao contrário.

A biblioteca não tem uma chamada que pare um observador. Os observadores são mantidos quando o mundo é salvo e carregado de novo.

## Os eventos da rede

Network.OnEvent avisa o seu programa de cada entrada escrita no log de eventos da rede, no momento em que é escrita: um UPS que passou para a bateria, uma fonte de alimentação que se degradou, um conflito entre Mainframes. Um único manipulador ouve todas as entradas, então uma nova chamada substitui a anterior. Ouvir não custa nada, o manipulador roda com os créditos do seu programa, e ele é mantido quando o mundo é salvo, como um observador.

> [!DEFINE] Total da rede
> A soma de um item em todos os servidores da rede.

> [!DEFINE] Limite
> O número em que um observador está definido. Below e Above disparam ao cruzá-lo, e não enquanto o total permanece ali.

> [!DEFINE] Rearmar
> Um observador volta a poder disparar quando o total retorna para o outro lado do limite.

## Se algo der errado

> [!PROBLEM] O programa para e diz que este computador não está em uma rede.
> Verifique primeiro Network.Online, e chame os outros membros só quando ele responder sim.

> [!PROBLEM] Network.Current devolve null.
> A máquina não está em nenhuma rede. Verifique primeiro Network.Online.

> [!PROBLEM] Network.Computer devolve null.
> Nenhum computador da rede tem esse nome de host. Veja os nomes em Network.Computers.

> [!PROBLEM] Um observador não dispara depois que você o define.
> Um observador dispara em uma mudança depois da sua primeira leitura. Espere o total mudar.

> [!SEE] jsc:what_a_network_is, jsc:first_network, jsc:network_identity
