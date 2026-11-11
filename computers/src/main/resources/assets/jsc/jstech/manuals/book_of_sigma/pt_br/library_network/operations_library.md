---
section: library_network
order: 449
---
# Operações em um programa

Uma Operação ([](jsc:what_operations_are)) é um trabalho que a rede faz. Um SELECT traz um item da rede para esta máquina, um INSERT envia itens no sentido contrário, e um CRAFT pede à rede que fabrique um item. Operations é o tipo que um programa usa para pedir esses trabalhos e para olhar os trabalhos em andamento.

Toda chamada de Operations precisa de uma rede, então confira primeiro o Network.Online quando não tiver certeza. Um pedido que a rede não consegue aceitar não para o programa: Ok fica falso, e a mensagem diz o motivo. Os tipos de Operação estão em [](jsc:operations_catalogue).

Table: Membros de Operations

| Membro | O que faz |
| --- | --- |
| `Operations.Pull(item, amount)` | Pede à rede que traga o item para o armazenamento desta máquina (SELECT). |
| `Operations.Push(item, amount)` | Envia itens do armazenamento desta máquina para a rede (INSERT). |
| `Operations.Craft(item, amount)` | Pede à rede que fabrique o item (CRAFT). |
| `Operations.Cancel(id)` | Cancela a Operação com esse id. |
| `Operations.Reprioritise(id, priority)` | Altera a urgência com que a Operação é executada. |
| `Operations.Update(item, action, amount)` | Pede um UPDATE com a ação, como SMELT. Adicionado na versão 2. |
| `Operations.Get(id)` | O OperationInfo da Operação com esse id, ou nada quando ela não está mais em andamento. |
| `Operations.List()` | Cada Operação em andamento, como uma lista de OperationInfo. |
| `Operations.OnSettled(handler)` | Chama o manipulador com o OperationInfo de cada Operação da rede que termina. Adicionado na versão 3. |
| `Operations.OnSettled(id, handler)` | Chama o manipulador uma vez, quando a Operação com esse id termina. Adicionado na versão 3. |

Pull, Push, Craft, Cancel, Reprioritise e Update devolvem um AskResult. Get e List devolvem OperationInfo. Cancel aceita o início de um id, desde que esse início tenha pelo menos o tamanho da forma curta do id. Get precisa do id inteiro e ignora maiúsculas e minúsculas.

## O que os resultados guardam

Table: Registros de Operações

| Registro | Campos | O que cada campo guarda |
| --- | --- | --- |
| `AskResult` | `AskResult.Ok, AskResult.Message` | Ok: sim quando a rede aceitou o pedido. Message: a resposta da rede, em inglês. |
| `OperationInfo` | `OperationInfo.Id, OperationInfo.Type, OperationInfo.Item, OperationInfo.Moved, OperationInfo.Requested, OperationInfo.Status, OperationInfo.Priority` | Id: o id a usar. Type e Status: texto em letras minúsculas. Item: o item. Moved e Requested: quanto foi movido e quanto foi pedido. |
| `AskResult` | `AskResult.Id` | O id curto da Operação que o pedido pôs em andamento, que Operations.OnSettled pode esperar; vazio quando ele não pôs nenhuma em andamento. Adicionado na versão 3. |

Table: Prioridades de uma Operação

| Nível | Formas de escrever |
| --- | --- |
| Baixa | `low` |
| Média baixa | `medium_low` |
| Média | `medium, or normal` |
| Média alta | `medium_high` |
| Alta | `high` |

Os cinco níveis de Operação na tabela não estão na mesma escala da prioridade de um programa, que é escrita como low, medium ou high.

## Mensagens que a rede responde

Table: Mensagens de um AskResult

| Mensagem | O que significa |
| --- | --- |
| item desconhecido: <item> | A rede não conhece o item. |
| a rede não tem um Mainframe em execução | Nenhum Mainframe está em execução na rede. |
| este computador não tem <item> | Um envio de um item que esta máquina não tem. |
| nada para enviar | O envio não tem itens para mandar. |
| nenhum padrão fabrica <item> | Nenhum padrão fabrica esse item. |
| a rede não tem espaço para <item> | O armazenamento da rede não tem espaço para o item. |
| nenhuma operação <id> em andamento (veja 'ops') | Nenhuma Operação com esse id está em execução. |
| prioridade inexistente: <name> | O nome da prioridade não é um dos níveis acima. |

## Ser avisado quando uma Operação termina

Operations.OnSettled avisa o seu programa quando uma Operação da rede termina, concluída ou não: o seu Status diz concluída, parcial, falhou ou descartada. Com um id, ele espera aquela única Operação e avisa uma vez; Pull, Push e Craft respondem com o id em AskResult.Id, então um programa pode pedir uma fabricação e ser avisado quando ela estiver pronta. Ouvir não custa nada, e os manipuladores são mantidos quando o mundo é salvo.

## Se algo der errado

> [!PROBLEM] O programa para e diz que este computador não está em uma rede.
> Confira primeiro o Network.Online.

> [!PROBLEM] AskResult diz que nenhum padrão fabrica o item.
> Adicione um padrão para o item no Pattern Studio ([](jsc:pattern_studio)).

> [!PROBLEM] AskResult diz que a rede não tem um Mainframe em execução.
> Inicie um Mainframe na rede e depois pergunte de novo.

> [!PROBLEM] AskResult diz que a rede não tem espaço para o item.
> Adicione armazenamento à rede, como um servidor de armazenamento ([](jsc:storage_servers)).

> [!PROBLEM] Operations.Get não devolve nada.
> A Operação terminou ou foi cancelada. Use Operations.List para ver o que está em execução.

> [!SEE] jsc:what_operations_are, jsc:operations_catalogue, jsc:states_and_queues
