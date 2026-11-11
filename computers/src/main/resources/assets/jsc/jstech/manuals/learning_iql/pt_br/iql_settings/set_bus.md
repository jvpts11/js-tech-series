---
section: iql_settings
order: 375
---
# SET BUS

SET BUS altera as configurações de um [barramento](jsc:buses) pelo prompt. Ele faz o que a janela do barramento faz: liga ou desliga o barramento, escolhe os itens que ele move, quantos e quando ([](jsc:bus_window)).

Escreva o nome do barramento como a janela dele mostra, entre aspas quando ele tiver espaços. Cada instrução altera um ajuste, então digite uma instrução para cada ajuste de que você precisar.

```iql bus.iql
SET BUS 'Ore in' ON
SET BUS 'Ore in' KEEP 16 MAX 64
```

Table: Os ajustes de SET BUS

| Instrução | O que faz |
| --- | --- |
| `SET BUS 'x' ON` | Liga o barramento. |
| `SET BUS 'x' OFF` | Desliga o barramento. |
| `SET BUS 'x' MODE CONTINUOUS` | O barramento move o tempo todo. |
| `SET BUS 'x' MODE ON DEMAND` | O barramento move somente enquanto o cabo dele leva um sinal de redstone. |
| `SET BUS 'x' FILTER ONLY a, b` | O filtro lista estes itens. |
| `SET BUS 'x' FILTER ALL BUT a, b` | O filtro lista todos os itens, exceto estes. |
| `SET BUS 'x' FILTER NONE` | O filtro não lista nenhum item. |
| `SET BUS 'x' FILTER TAG c:ores, c:logs` | O filtro lista os itens com estas tags. |
| `SET BUS 'x' MATCH FUZZY` | Correspondência aproximada: qualquer dano ou componente. |
| `SET BUS 'x' MATCH EXACT` | Correspondência exata. |
| `SET BUS 'x' KEEP 16 MAX 64` | Mantém 16 no baú e move no máximo 64 por vez. |
| `SET BUS 'x' MAX 64` | Move no máximo 64 por vez. |
| `SET BUS 'x' KEEP iron_ore 16 MAX iron_ore 64` | O mesmo, para um item listado. |
| `SET BUS 'x' PRIORITY 5` | Qual barramento vai primeiro. Um número inteiro, que pode ser negativo. |
| `SET BUS 'x' WHEN STOCK iron_ore < 512` | Espera enquanto a rede tiver menos de 512 do item. |
| `SET BUS 'x' WHEN STOCK TAG c:ores < 4096` | O mesmo, para uma tag. |
| `SET BUS 'x' WHEN TIME BETWEEN 18 AND 6` | Move somente entre estas horas. O intervalo pode passar da meia-noite. |
| `SET BUS 'x' AFTER BUS 'Coal in'` | Espera até o barramento indicado terminar. |
| `SET BUS 'x' ACCESS READ AND WRITE` | A rede pode tirar do inventário e colocar nele. |
| `SET BUS 'x' ACCESS READ ONLY` | A rede só pode tirar do inventário. |
| `SET BUS 'x' ACCESS WRITE ONLY` | A rede só pode colocar no inventário. |

Depois de WHEN STOCK, a única comparação permitida é o sinal <. KEEP e MAX aceitam um número inteiro que não seja negativo. As horas depois de WHEN TIME são números inteiros de 0 a 23. Uma lista de itens é separada por vírgulas, e um fluido é escrito com o seu tipo primeiro, como em FLUID water.

Table: As respostas de SET BUS

| Resposta | O que significa |
| --- | --- |
| `bus <name> set` | O ajuste foi salvo. |
| `no bus named <name> on this network` | Nenhum barramento com esse nome está na rede. |
| `the bus <name> cannot be set so: its era or its kind has no such setting` | Este barramento não oferece esse ajuste. |
| `<item> is not in the filter of the bus <name>` | O item precisa ser um dos que o filtro do barramento lista. |
| `no item or fluid named <item>` | O nome não é um item nem um fluido. |

## Se algo der errado

> [!PROBLEM] SET BUS precisa do nome do barramento, entre aspas quando ele tem espaços
> Digite o nome depois de BUS, e coloque entre aspas quando ele tiver espaços.

> [!PROBLEM] A resposta diz "SET BUS precisa de um ajuste: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY, WHEN, AFTER BUS ou ACCESS".
> Adicione uma palavra de ajuste depois do nome.

> [!PROBLEM] um barramento não tem o ajuste <word>
> Use um dos ajustes da tabela acima.

> [!PROBLEM] token inesperado: <word>
> Uma instrução tem um ajuste. Digite o próximo ajuste como uma nova instrução.

> [!PROBLEM] nenhum barramento chamado <name> nesta rede
> Confira o nome na janela do barramento.

> [!PROBLEM] esperava uma quantidade, mas veio: <text>
> Digite um número inteiro depois de KEEP ou MAX.

> [!PROBLEM] não é uma hora do dia: <text>
> Digite uma hora de 0 a 23.

> [!SEE] jsc:bus_filters_and_tags, jsc:bus_keep_max_and_priority, jsc:bus_conditions
