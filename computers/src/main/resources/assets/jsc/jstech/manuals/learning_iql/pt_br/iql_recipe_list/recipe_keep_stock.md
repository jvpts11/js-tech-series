---
section: iql_recipe_list
order: 400
icon: jsc:mainframe
---
# Manter um estoque de um item

Um estoque é quanto de um item a rede guarda. Muitas vezes você quer que um número fique onde está: 512 lingotes de ferro prontos para a próxima construção, e nunca menos. O IQL tem três formas de pedir isso, e elas funcionam de maneiras diferentes. Escolha a que se encaixa no que você precisa.

> [!DEFINE] Estoque
> Quantos de um item a rede inteira guarda, contados em todos os seus servidores.

Table: As formas de manter um estoque

| Forma | Era | O que precisa |
| --- | --- | --- |
| KEEP, um nível que o Prophet YourIQL mantém | Legacy e mais novas | Prophet YourIQL em execução no Mainframe |
| Uma tarefa que reabastece abaixo de uma linha | Legacy e mais novas | O Midsoft IQL Server em execução, e o Motor de Automação instalado |
| Um IF em uma ação, uma vez | Vintage e mais novas | Um motor que executa ações, como o Midsoft IQL Server |

KEEP pertence ao Prophet YourIQL ([](jsc:prophet_youriql)). Você informa o item e o nível, e o motor calcula as fabricações que chegam ao nível e o mantêm ali.

## KEEP: informe o nível, e deixe o motor mantê-lo

O motor conta o que já está a caminho: os itens sendo fabricados ou movidos em direção ao estoque. Assim, ele pede apenas o que ainda falta.

1. Instale o Prophet YourIQL no Mainframe e confirme que o Mainframe está ligado.
2. Abra o Prophet Reactive Console em um computador da rede.
3. Digite a linha KEEP no console e pressione [[Enter]].
4. Digite SHOW STATES para ver o item e o status dele.

Table: Linhas para KEEP

| Linha | O que pede |
| --- | --- |
| `KEEP iron_ingot >= 512` | Pelo menos 512 lingotes de ferro, fabricados conforme necessário. |
| `KEEP iron_ingot BETWEEN 256 AND 512` | Entre 256 e 512 lingotes de ferro. |

Os espaços em volta de >= são obrigatórios, e também as palavras BETWEEN e AND. Um nome sem dois pontos significa minecraft:, então iron_ingot é o lingote de ferro vanilla. O item de um mod precisa do nome completo, com o nome do mod antes dos dois pontos.

Quando o nível é mantido, o console confirma com uma linha que termina em "mantido em 512 ou mais".

O motor olha a rede em ticks. Um tick é um passo do jogo, e 20 ticks formam um segundo.

> [!DEFINE] Tick
> Um passo do jogo: 20 deles fazem um segundo.

Table: Configurações do motor

| Configuração | Opções | Padrão |
| --- | --- | --- |
| Olhar a rede a cada | 20, 40, 100 ou 200 ticks: 1, 2, 5 ou 10 segundos | 20 ticks, 1 segundo |
| Pedir no máximo, em uma fabricação | 64, 256, 1.024 ou 4.096 itens | 1.024 itens |

Duas configurações mudam como o Prophet funciona: o intervalo de observação e o lote por fabricação. O Prophet só reage enquanto o Mainframe está ligado, e só quando a reação está ativada.

Um observador, escrito WATCH item < n DO instrução, executa essa instrução uma vez quando um nível cruza a linha que você definiu. Dentro de um observador, CRAFT item TO n fabrica o item até um nível.

Digite SHOW STATES para ver o status de cada estado. Não é possível manter significa que a rede não consegue alcançar o nível no momento.

## Uma tarefa: reabastecer quando o estoque cai abaixo de uma linha

Uma tarefa é uma instrução salva que a rede executa sozinha ([](jsc:iql_jobs)). O Motor de Automação guarda as tarefas e as dispara na hora certa, e só faz isso enquanto o Mainframe está ligado.

> [!DEFINE] Motor de Automação
> O serviço do Mainframe que guarda as tarefas e as dispara na hora certa.

Uma tarefa com um gatilho WHEN dispara na borda de subida da sua condição: o momento em que a condição passa de falsa para verdadeira. Ela dispara uma vez nesse momento, e não de novo enquanto a condição continuar verdadeira.

> [!DEFINE] Borda de subida
> O momento em que uma condição passa de falsa para verdadeira.

A tarefa fabrica um item a partir de um padrão, a receita que a rede guarda para esse item.

> [!DEFINE] Padrão
> A receita que a rede guarda para um item, que diz a ela o que fabricar esse item.

1. Instale o Motor de Automação no Mainframe ([](jsc:automation_engine)).
2. Confira se o Mainframe está ligado e se o Midsoft IQL Server está em execução.
3. Confirme que a rede tem um padrão para o item ([](jsc:patterns)).
4. Digite a tarefa no prompt, com iql na frente ([](jsc:where_to_type_iql)).

```table
{"caption": "Uma linha para uma tarefa", "rows": [[{"words": "Reabastecer 64 quando abaixo de 512"}, {"literal": "CREATE JOB restock_iron AS CRAFT 64 iron_ingot WHEN qty(iron_ingot) < 512"}]]}
```

A resposta diz "tarefa restock_iron criada". A tarefa confere o estoque a cada meio segundo, mais ou menos. Se o estoque já estiver abaixo de 512 na primeira conferência, a tarefa dispara na hora.

A tarefa olha apenas o estoque da rede, e não o que está a caminho. Ela volta a disparar somente depois que o estoque chegar a 512 ou mais e cair abaixo disso outra vez.

O Gerenciador de Automação escreve a mesma tarefa para você, sem nenhum IQL. Seu formulário se chama Reabastecer abaixo: você informa um item e uma quantidade, e ele cria uma tarefa que fabrica essa quantidade quando o estoque cai abaixo do mesmo valor ([](jsc:automation_manager)).

## Uma guarda IF: uma fabricação, quando o estoque está baixo

Uma cláusula IF em uma ação executa essa ação uma vez, e só se a condição valer. Ela não volta sozinha, então use-a para um pedido único, e não para uma regra permanente.

```table
{"caption": "Uma linha com IF", "rows": [[{"words": "Fabricar 64 apenas quando o estoque estiver abaixo de 100"}, {"literal": "CRAFT 64 iron_ingot IF qty < 100"}]]}
```

O IF lê o estoque total da rede do item que a ação nomeia. Quando a condição não vale, a resposta diz "o IF não se cumpriu, então o CRAFT não foi executado". Isso não é um erro: a linha foi aceita e não fez nada.

## Se algo der errado

> [!PROBLEM] A resposta diz "sintaxe: KEEP item >= n, ou KEEP item BETWEEN a AND b".
> Escreva os espaços em volta de >= e as palavras BETWEEN e AND, e informe um nível com cada um.

> [!PROBLEM] A resposta diz "nenhum item se chama steel_ingt".
> Confira o id do item. O item de um mod precisa do nome do mod antes dos dois pontos.

> [!PROBLEM] A resposta diz "os jobs ficam com o Motor de Automação; instale-o primeiro no Mainframe".
> Instale o Motor de Automação no Mainframe ([](jsc:automation_engine)).

> [!PROBLEM] A resposta diz "o engine da rede não guarda views, procedimentos nem jobs".
> O Prophet YourIQL e o NextgreIQL não guardam tarefas. Use o Midsoft IQL Server para uma tarefa ([](jsc:engines)).

> [!PROBLEM] SHOW STATES diz Não é possível manter.
> A rede não consegue alcançar o nível. Dê à rede um padrão para o item e os materiais que esse padrão usa.

> [!PROBLEM] Nada dispara enquanto o Mainframe está desligado.
> Ligue o Mainframe. As tarefas e o Prophet YourIQL esperam por ele.

> [!SEE] jsc:iql_jobs, jsc:prophet_youriql, jsc:automation_manager, jsc:engines
