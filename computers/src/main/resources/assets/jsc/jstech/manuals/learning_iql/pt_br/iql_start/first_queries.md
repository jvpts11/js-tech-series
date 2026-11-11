---
section: iql_start
order: 364
---
# Suas primeiras consultas, passo a passo

Este verbete leva você de uma rede sem nada digitado até suas primeiras respostas e seu primeiro trabalho. Cada passo mostra o que você digita e o que a rede responde. Se a sua resposta for diferente, leia os problemas no final.

## Antes de começar

1. Ligue o computador à rede com um cabo de dados ([](jsc:first_network)).
2. Ligue o Mainframe ([](jsc:mainframes)).
3. Digite iqlengine status e pressione [[Enter]].

A resposta diz "Midsoft IQL Server: em execução" quando o servidor está pronto para responder. Se disser parado, inicie-o com iqlengine start ([](jsc:engine_services)).

> [!DEFINE] servidor
> Um computador da rede que guarda itens em seus discos.

Digite net para ver um resumo de toda a rede. Ele tem títulos para a rede, o Mainframe, os servidores, os computadores, os subframes e os tipos que indexa.

## Fazendo perguntas

Table: Suas primeiras leituras

| Você digita | A resposta |
| --- | --- |
| `iql QUERY disks` | Uma linha para cada disco, ou "nenhuma linha" |
| `iql QUERY servers` | Uma linha para cada servidor |
| `iql QUERY items WHERE qty > 100` | Os itens guardados em quantidades acima de 100 |
| `iql QUERY items ORDER BY qty DESC LIMIT 2` | As 2 linhas com a maior quantidade |

Cada linha de resposta ocupa uma única linha de texto. O nome e os detalhes ficam à esquerda, e a quantidade fica à direita. A linha de comando mostra no máximo 64 linhas, então uma resposta mais longa é cortada.

> [!DEFINE] tabela
> Uma lista que um QUERY lê, como discos, servidores ou itens.

As três tabelas dessas primeiras leituras têm estes campos. Cada campo é uma informação sobre uma linha.

Table: Os campos de três tabelas

| Tabela | Campos |
| --- | --- |
| disks | `disk, server, tier, size, used` |
| servers | `name, used, free, era` |
| items | `item, qty, server, tag, enchant` |

ORDER BY ordena as linhas por um campo antes de serem mostradas. DESC inverte a ordem. LIMIT 2 interrompe a resposta depois de 2 linhas. As outras tabelas estão listadas em [QUERY e SELECT](jsc:query_and_select).

## Retirando itens

1. Digite iql SELECT 64 iron_ingot e pressione [[Enter]].
2. Leia a resposta: ela diz que o SELECT está na fila e para onde os itens vão.
3. Digite iql CRAFT 64 torch e pressione [[Enter]].
4. Leia a resposta: ela diz que o CRAFT está na fila.

Table: Seu primeiro trabalho

| Você digita | A resposta |
| --- | --- |
| `iql SELECT 64 iron_ingot` | `SELECT queued: 64 Iron Ingot -> local storage` |
| `iql CRAFT 64 torch` | `CRAFT queued: 64 Torch` |

SELECT sem FROM pega os itens de qualquer lugar da rede. Para pegá-los de um servidor, escreva FROM e o nome do servidor, como o QUERY servers o lista. A resposta nomeia o item pelo nome de exibição, e os itens chegam ao armazenamento local: o armazenamento próprio deste computador.

CRAFT fabrica itens, e funciona apenas para itens para os quais a rede tem um padrão ([](jsc:how_autocrafting_works)).

## Acompanhando o trabalho

Digite interac ops no prompt para listar o trabalho que ainda está em execução. O programa Gerenciador de Rede também lista as Operações em andamento ([](jsc:network_manager)).

## Se algo der errado

> [!PROBLEM] A resposta diz "sintaxe: esperado um item".
> Escreva o nome do item depois da ação, como iron_ingot.

> [!PROBLEM] A resposta diz "sintaxe: quantidades em porcentagem ainda não são suportadas".
> Escreva um número de itens, como 50 items.

> [!PROBLEM] A resposta diz "sintaxe: SELECT não tem destino TO".
> SELECT traz itens até você. Omita TO. Para enviar itens entre dois lugares, use MOVE.

> [!PROBLEM] A resposta diz "sintaxe: DELETE exporta itens para fora da rede e precisa de um destino TO".
> Acrescente TO, seguido da posição onde os itens vão.

> [!PROBLEM] A resposta diz "sintaxe: MOVE é interno e precisa de FROM e TO".
> Informe os dois lugares, FROM de onde os itens vêm e TO para onde vão.

> [!PROBLEM] A resposta diz "sintaxe: FROM não é válido em uma leitura".
> Uma consulta apenas lê. Omita FROM, ou use uma ação que move itens.

> [!PROBLEM] A resposta diz "sintaxe: nível de prioridade desconhecido".
> Use um dos níveis que a resposta lista: LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH ou HIGH.

> [!PROBLEM] A resposta diz "sintaxe: verbo desconhecido", seguido de uma palavra.
> Confira a grafia da palavra de ação. As palavras que existem estão no [catálogo](jsc:operations_catalogue).

> [!PROBLEM] A resposta diz "item desconhecido", seguido do nome.
> A rede não conhece esse nome. Confira a grafia, ou escreva o nome completo, como minecraft:iron_ingot.

> [!PROBLEM] A resposta diz "nenhum servidor chamado", seguido do nome.
> Nenhum servidor tem esse nome. Use QUERY servers para ver os nomes.

> [!PROBLEM] A resposta diz "sintaxe: esperado um objeto para consultar".
> Informe o que consultar depois de QUERY, como disks ou items.

> [!PROBLEM] A resposta diz "nenhum padrão fabrica", seguido do nome do item.
> A rede não tem padrão para esse item ([](jsc:how_autocrafting_works)).

> [!PROBLEM] A resposta diz "Network Operations Service indisponível".
> O Midsoft IQL Server está parado. Inicie-o com iqlengine start.

> [!PROBLEM] A resposta diz "a rede não tem um Mainframe em execução".
> Ligue o Mainframe.

> [!SEE] jsc:query_and_select, jsc:operations_catalogue, jsc:iql_errors
