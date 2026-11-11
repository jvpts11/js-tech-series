---
section: bus_programming
order: 131
icon: jsc:export_bus
---
# Programando barramentos

Programas podem definir um barramento, assim como a janela pode. Um programa encontra um barramento pelo nome, na mesma rede. Dê ao barramento o nome antes, na janela dele ([](jsc:bus_window)).

Programas definem um barramento em IQL ou em Sigma. IQL é a linguagem de consulta da rede ([](jsc:iql)). Sigma é uma linguagem de programação, e a linguagem irmã dela é a Sigma#. O Livro do Sigma cobre as duas ([](jsc:sigma_two_languages)).

Um nome corresponde independentemente de maiúsculas e minúsculas, e o primeiro barramento com esse nome na rede é o usado. Coloque um nome entre aspas quando ele tiver espaços.

Itens vanilla são escritos pelo nome curto, como iron_ore. Itens de outros mods são escritos por completo, como namespace:path. Fluidos são escritos com a palavra FLUID antes, como em FLUID water.

## Instruções IQL

Table: Instruções IQL para um barramento

| Instrução | O que define |
| --- | --- |
| `SET BUS 'Ore in' ON` | Liga o barramento. |
| `SET BUS 'Ore in' OFF` | Desliga o barramento. |
| `SET BUS 'Ore in' MODE ON DEMAND` | Move itens somente enquanto houver um sinal de redstone. |
| `SET BUS 'Ore in' FILTER ONLY iron_ore, coal` | Lista os itens que o barramento move. |
| `SET BUS 'Ore in' FILTER ALL BUT dirt` | Move tudo, exceto os itens listados. |
| `SET BUS 'Ore in' FILTER NONE` | Limpa o filtro. |
| `SET BUS 'Ore in' FILTER TAG c:ores, c:logs` | Lista tags, em um barramento Advanced. |
| `SET BUS 'Ore in' MATCH FUZZY` | Faz correspondência aproximada, em um barramento Advanced. |
| `SET BUS 'Ore in' KEEP 16 MAX 64` | Define KEEP e MAX para o barramento inteiro. |
| `SET BUS 'Ore in' KEEP iron_ore 16 MAX iron_ore 64` | Define KEEP e MAX para um item listado, em um barramento Transition. |
| `SET BUS 'Ore in' PRIORITY 5` | Define a prioridade, em um barramento Standard ou posterior. |
| `SET BUS 'Ore in' WHEN STOCK iron_ore < 512` | Adiciona uma condição STOCK. |
| `SET BUS 'Ore in' WHEN STOCK TAG c:ores < 4096` | Adiciona uma condição STOCK para uma tag. |
| `SET BUS 'Ore in' WHEN TIME BETWEEN 18:00 AND 06:00` | Define a condição HOURS e substitui a anterior. |
| `SET BUS 'Ore in' AFTER BUS 'Coal in'` | Adiciona uma condição AFTER. |
| `SET BUS 'Chest wall' ACCESS READ ONLY` | Define o que a rede pode fazer com um Barramento de Armazenamento Externo. |

## Chamadas Sigma

Table: Chamadas Sigma para um barramento

| Chamada | O que define |
| --- | --- |
| `bus("Ore in").Only("iron_ore, coal");` | Lista os itens que o barramento move. |
| `bus("Ore in").AllBut("dirt");` | Move tudo, exceto os itens listados. |
| `bus("Ore in").Tag("c:ores");` | Lista uma tag. |
| `bus("Ore in").Fuzzy(true);` | Faz correspondência aproximada. |
| `bus("Ore in").Keep(16).Max(64);` | Define KEEP e MAX para o barramento inteiro. |
| `bus("Ore in").Item("iron_ore").Keep(16);` | Define KEEP para um item listado. |
| `bus("Ore in").Priority(5);` | Define a prioridade. |
| `bus("Ore in").WhenStock("iron_ore", 512);` | Adiciona uma condição STOCK. |
| `bus("Ore in").Between(18, 6);` | Adiciona a condição HOURS, de 18:00 a 06:00. |
| `bus("Ore in").After(bus("Coal in"));` | Adiciona uma condição AFTER. |
| `bus("Ore in").OnDemand();` | Move somente com um sinal de redstone. |

A aba SOFTWARE de um barramento mostra as linhas IQL e SIGMA que o configurariam do jeito que ele está agora, para você copiá-las ([](jsc:bus_window)). Ela escreve uma condição HOURS em forma de Job, como CREATE JOB bus_hours AS SET BUS 'Ore in' ON WHEN TIME BETWEEN 18:00 AND 06:00. O motor mantém esse Job como a condição HOURS do barramento.

## Quando uma instrução é recusada

Table: Mensagens de uma instrução de barramento

| Mensagem | O que significa |
| --- | --- |
| SET BUS precisa do nome do barramento, entre aspas quando ele tiver espaços | A instrução não tem nome de barramento, ou tem um nome com espaços e sem aspas. |
| SET BUS precisa de uma configuração: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY, WHEN, AFTER BUS ou ACCESS | A instrução não nomeia nenhuma configuração. Escreva uma das palavras listadas. |
| o barramento não tem configuração, e depois a palavra que você digitou | A palavra depois do nome do barramento não é uma configuração. Confira a grafia. |
| nenhum barramento chamado Ore in nesta rede | Nenhum barramento tem esse nome na rede em que o programa roda. |
| barramento Ore in configurado | A instrução funcionou, e o barramento está configurado. |
| o barramento Ore in não pode ser configurado assim: a era ou o tipo dele não tem essa configuração | O barramento não tem essa configuração, como PRIORITY em um barramento Legacy, ou POWER em um Barramento de Armazenamento Externo. |
| nenhum item ou fluido nomeado, e depois o nome que você digitou | O filtro não conhece esse nome, e itens de outros mods precisam do nome completo. |
| Carvão não está no filtro do barramento Ore in | KEEP ou MAX para um item precisa que esse item esteja antes no filtro. |
| 25 não é uma hora do dia | Uma hora para Between deve estar de 0 a 23. |

## Se algo der errado

> [!PROBLEM] A linha de um programa diz que nenhum barramento tem esse nome.
> Confira o nome do barramento em CONFIGURE e veja se o barramento está na rede em que o programa roda.

> [!PROBLEM] O barramento não aceita uma quinta condição.
> Um barramento comporta até 4 condições. Remova uma primeiro, depois defina a nova.

> [!PROBLEM] Uma configuração não muda.
> A era do barramento não tem essa configuração. Veja quais configurações cada era tem em Barramentos por era ([](jsc:buses_by_era)).

> [!PROBLEM] Definir uma hora é recusado.
> Use horas de 0 a 23, como 18 para as seis da tarde.

> [!SEE] jsc:iql, jsc:what_iql_is, jsc:buses_library
