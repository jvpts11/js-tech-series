---
section: iql_verbs
order: 368
---
# INSERT, DELETE, MOVE e DROP

Estes quatro verbos movem itens ou os destroem. INSERT traz itens para dentro da rede. DELETE os envia para fora da rede. MOVE leva itens de um lugar para outro. DROP os destrói. Onde você digitar uma quantidade, ela é um número, ALL ou nada.

Um barramento é um bloco que move itens entre a rede e um baú ou uma máquina ao lado dele. Esse baú ou essa máquina é o inventário externo do barramento ([](jsc:buses)).

Table: Os quatro verbos de item

| Verbo | Forma e o que faz |
| --- | --- |
| `INSERT` | INSERT [quantity] item [FROM bus]. Traz os itens que este computador tem para dentro da rede, ou os importa de um barramento. |
| `DELETE` | DELETE [quantity] item TO bus. Envia os itens que a rede tem para fora, para um barramento. |
| `MOVE` | MOVE [quantity] item FROM source TO destination. Leva itens entre dois servidores, ou de e para um barramento. |
| `DROP` | DROP [quantity] item. Destrói os itens que a rede tem. |

## INSERT

Sem FROM, INSERT empurra o que este computador tem para dentro da rede. Com FROM e o nome de um barramento, ele importa desse barramento. O que a rede não consegue guardar volta para o barramento.

Table: As respostas de INSERT

| Resposta | O que significa |
| --- | --- |
| `INSERT queued: <quantity> <item> -> network` | Os itens estão a caminho da rede. |
| `nothing to push` | Não há nada para INSERT empurrar. |
| `this computer holds no <item>` | Este computador não tem o item. Coloque-o neste computador primeiro. |
| `MOVE queued: import from <bus>` | A importação começou. A resposta diz MOVE, embora você tenha digitado INSERT: essa é a forma de dizer da própria rede. |
| `nothing to import from <bus>` | O barramento não tem nada para importar. |
| `the bus '<bus>' touches no inventory` | O barramento não tem nenhum baú ou máquina ao lado para retirar itens. |

## DELETE

DELETE envia itens para fora da rede. Ele precisa de TO e do nome de um barramento, e um nome de servidor não é válido ali. Só são enviados os itens que a rede possui, então um segundo DELETE não faz nada quando o estoque acaba. DELETE aceita FROM, mas o ignora.

Table: As respostas de DELETE

| Resposta | O que significa |
| --- | --- |
| `DELETE queued: <quantity> <item>` | Os itens estão a caminho do barramento. |
| `nothing to delete` | A rede não tem nada desse item. |
| `no bus named '<bus>'` | Nenhum barramento tem esse nome na rede. Confira o nome na janela do barramento. |

## MOVE

MOVE precisa de FROM e TO. Entre dois servidores, ele move itens dentro da rede. Um barramento depois de TO envia itens para fora, e um barramento depois de FROM os traz para dentro.

Table: As respostas de MOVE

| Resposta | O que significa |
| --- | --- |
| `MOVE queued: <quantity> <item> -> <bus>` | Os itens estão a caminho desse barramento. |
| `nothing to move` | A origem não tem nada desse item. |
| `nothing to move to <bus>` | A origem não tem nada para enviar a esse barramento. |
| `no server or bus named '<name>'` | Nem um servidor nem um barramento têm esse nome. |
| `the destination server is unavailable` | O destino não pode receber itens agora. |
| `could not start the MOVE` | A rede não iniciou a movimentação. |

## DROP

> [!WARNING] Um DROP destrói os itens para sempre. Eles saem da rede e não são guardados.

DROP não aceita TO. Ele destrói apenas os itens que a rede tem, então você não pode descartar o que não tem na rede. Ele roda assim que você o digita, sem pergunta, como num banco de dados de verdade. O Management Studio só pergunta antes de um DELETE com ALL.

Table: As respostas de DROP

| Resposta | O que significa |
| --- | --- |
| `DROP queued: <quantity> <item>` | Os itens estão sendo destruídos. |
| `nothing to drop` | A rede não tem nada desse item. |

## Se algo der errado

> [!PROBLEM] DELETE exporta para fora da rede e precisa de um destino TO
> Adicione TO e o nome de um barramento.

> [!PROBLEM] MOVE é interno e precisa de FROM e TO
> Informe a posição de origem depois de FROM, e a posição de destino depois de TO.

> [!PROBLEM] DROP não tem destino TO
> Remova o TO. DROP apenas destrói itens.

> [!PROBLEM] esperava uma posição depois de FROM, recebeu: <text>
> Digite o nome como uma palavra só, sem espaço.

> [!SEE] jsc:buses, jsc:set_bus, jsc:query_and_select
