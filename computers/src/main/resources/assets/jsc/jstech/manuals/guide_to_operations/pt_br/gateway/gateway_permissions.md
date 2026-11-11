---
section: gateway
order: 166
---
# O que o ComputerCraft pode fazer

Um programa ComputerCraft pode pedir ao Gateway dois tipos de coisa: ler o que a rede guarda, e iniciar operações que movem ou fabricam itens. Você decide quais tipos ele pode pedir, com quatro configurações no host.

Table: As quatro configurações

| Configuração | O que permite | Padrão |
| --- | --- | --- |
| Ler a rede | As chamadas de leitura: capacity, used, types, total, find, servers, computers, operation, operations e watch. | Ligada |
| Operações | As chamadas pull, push, craft, cancel e run. | Ligada |
| Teto de prioridade | A prioridade mais alta que uma requisição pode ter. Uma requisição mais alta é reduzida a ela. | MEDIUM |
| Chamadas por tick | Quantas chamadas o Gateway atende em um tick do jogo, que equivale a um vigésimo de segundo. | 8 |

As opções não cobrem unwatch, log, send, online, name nem host. Unwatch e log ainda contam para o limite de chamadas, e send não.

Uma requisição indica a sua prioridade com uma palavra: low, medium ou high. Uma requisição sem palavra, ou com uma palavra que o Gateway não conhece, roda como medium. Um teto menor que essa prioridade então a reduz. [Prioridades](jsc:priorities) explicam a ordem em que a rede as atende.

Uma quantidade de 0 ou menos significa tudo: puxa tudo de um nome, ou envia tudo do buffer desse nome. O Gerenciador de Gateway mostra isso como pull all. O que um push não consegue mover volta para o buffer.

A chamada run inicia um programa compilado em outro computador da rede. Você informa o nome do computador, o nome do programa e os argumentos, se houver, e ela devolve um número de processo. Ela não recebe prioridade.

## Recusas

Uma requisição recusada volta ao programa como texto, e o log do Gateway a registra como DENIED.

Table: Recusas e o que significam

| Recusa | O que significa |
| --- | --- |
| `denied: reading the network is off` | A opção de leitura está desligada. |
| `denied: operations are off` | A opção de operações está desligada. |
| `busy: 8 calls a tick` | O programa chamou com mais frequência do que o limite de chamadas em um tick. O número é o limite. |
| `which item?` | A requisição não nomeou nenhum item. |
| `unknown item: <name>` | O jogo não conhece nenhum item com esse nome, e fluidos e substâncias químicas dizem fluido desconhecido ou substância química desconhecida. |
| `the computer is not on a network` | O host não tem rede para atender a requisição. |
| `the network has no running Mainframe` | Um Mainframe em execução precisa estar na rede. |
| `could not start the <type>` | A rede recusou iniciar a operação, como um SELECT ou um INSERT. |
| `the buffer holds no <name>` | Um push pediu um item que o buffer não tem. |
| `no pattern crafts <name>` | Nenhum padrão fabrica esse item, então nenhuma fabricação pode começar. |
| `more than one operation starts with <id>; give more of the id` | O id que você informou corresponde a duas operações. |
| `<name>: no such computer on this network` | O computador que você nomeou não está nesta rede. |
| `<name> is powered off` | O computador ao qual você pediu para executar um programa está desligado. |
| `<name> does not take programs from other computers` | O computador recusa programas que vêm de outro computador. |
| `<name>: nothing installed runs a program of this kind` | O computador não tem nada que possa executar esse programa. |
| `<name>: <n> MB will not fit in <m> MB of free memory` | O computador tem pouca memória livre para o programa. |

## Se algo der errado

> [!PROBLEM] A mensagem "negado: leitura da rede está desligada" aparece.
> Ative a opção de leitura na aba Permissões, ou digite gateway <name> set read on na linha de comando.

> [!PROBLEM] A mensagem "negado: operações estão desligadas" aparece.
> Ative a opção de operações na aba Permissões, ou digite gateway <name> set operations on na linha de comando.

> [!PROBLEM] A mensagem "ocupado: 8 chamadas por tick" aparece.
> O programa fez mais chamadas do que o limite em um tick. Aumente o limite para 16, ou deixe o programa mais lento.

> [!PROBLEM] Um programa pede uma prioridade e recebe uma menor.
> O teto de prioridade é menor que a requisição, então a requisição é reduzida a ele. Aumente o teto na aba Permissões.

> [!SEE] jsc:gateway_manager, jsc:states_and_queues
