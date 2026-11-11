---
section: engine_choice
order: 386
icon: jsc:mainframe
---
# NextgreIQL e EXPLAIN

NextgreIQL é o motor que mostra como ele planeja. Antes de qualquer trabalho ser feito, você pode pedir o plano de um CRAFT ou de um SELECT e ler quais passos ele daria e onde cada passo roda.

> [!DEFINE] Plano de consulta
> Os passos que o motor escolheu para responder a uma instrução, com onde cada passo roda e quanto custa em ticks.

Uma fabricação é feita em passos, e cada passo roda numa bancada ou numa máquina. Uma bancada é um Computador de Fabricação, onde rodam as receitas feitas à mão. Uma máquina é uma máquina de processamento, e ela roda as próprias receitas.

> [!DEFINE] Bancada
> Um Computador de Fabricação, onde rodam as receitas de fabricação feitas à mão.

> [!DEFINE] Máquina
> Uma máquina de processamento, que roda as próprias receitas.

Você também pode mudar um plano com dicas ([](jsc:nextgre_hints)), que são palavras escritas depois de um CRAFT.

> [!DEFINE] Dica
> Uma configuração escrita depois de um CRAFT que muda o plano que ele faz, como PREFER MACHINE.

Table: Versões do NextgreIQL por idade

| Idade do Mainframe | Versão do NextgreIQL |
| --- | --- |
| Legacy | 7.0 |
| Transition | 8.3 |
| Standard | 9.0 |
| Advanced | 16 |

## Mostrando um plano com EXPLAIN

EXPLAIN mostra o plano sem iniciá-lo. Escreva EXPLAIN antes de um CRAFT ou de um SELECT, e o motor responde com o plano em vez de fazer o trabalho. Só o NextgreIQL oferece EXPLAIN; o Midsoft recusa a palavra com uma mensagem de sintaxe.

1. Digite EXPLAIN CRAFT, a quantidade e o nome do item.
2. Pressione [[Enter]].

```iql explain_craft.iql
EXPLAIN CRAFT 64 iron_ingot
```

A resposta é uma tabela com uma linha para cada passo do plano. Suas colunas são estas.

Table: As colunas de um plano explicado

| Coluna | O que mostra |
| --- | --- |
| passo | O rótulo do passo e a sua quantidade, como mostra a tabela de passos abaixo. |
| onde | Se uma bancada ou uma máquina roda o passo, e qual delas. |
| estimativa | O custo em ticks, calculado antes de qualquer coisa rodar. |
| real | Em branco até o plano rodar. Depois mostra o tempo que o passo levou. |
| dicas | As dicas que moldaram o passo. |

Table: Os passos de um plano

| Rótulo do passo | O que significa |
| --- | --- |
| `Craft <item> x<n>` | Fabrica o item, na quantidade mostrada. |
| `Process <item> x<n>` | Processa o item numa máquina. |
| `Pull <item> x<n>` | Retira itens de um servidor de armazenamento. Há uma linha para cada servidor. |
| `Index Seek <item> x<n>` | Uma leitura de toda a quantidade, dos servidores nomeados depois de from. |
| `Missing <item> x<n>` | A rede não tem nada disso e também não o fabrica. |

Passos que rodam lado a lado são agrupados. O grupo diz quantos passos tem e quantos deles rodam ao mesmo tempo.

## Executando um plano com EXPLAIN ANALYZE

EXPLAIN ANALYZE CRAFT também faz a fabricação. O motor a inicia, e o plano preenche o tempo real de cada passo conforme ele roda.

A resposta informa o plano que o motor escolheu, quantos planos ele pesou e os oito primeiros caracteres da Operação. O Nextgre Planner Studio mostra os tempos conforme eles chegam ([](jsc:nextgre_planner_studio)).

## De onde vêm os itens

EXPLAIN SELECT mostra de onde os itens viriam, sem mover nada. A resposta dá a quantidade, o item, quantos servidores os entregariam e o custo em ticks.

## ANALYZE sozinho

Um ANALYZE simples, sem instrução depois dele, faz a manutenção do índice da rede ([](jsc:operations_catalogue)). Em seguida, o NextgreIQL colhe estatísticas para o Studio, e a resposta mostra os tipos de item, os servidores e as receitas que ele mediu.

O Studio mostra estas estatísticas: tipos de item indexados, servidores, receitas cronometradas, fabricações medidas e a hora do último ANALYZE.

## Como o motor escolhe um plano

O motor não adivinha um plano. Ele dá a cada plano candidato um custo em ticks, um para cada jeito que encontrou de fazer o item, e escolhe o mais barato.

A aba Regras do Studio lista as regras que moldam a ponderação: Pesar máquinas contra bancadas, Pesar cada receita do resultado, Usar tempos medidos, Contar passos que rodam ao mesmo tempo e Pesar os servidores mais rápidos. Você pode ligar ou desligar essas regras no Studio.

Todo plano fica guardado no Mainframe, e a aba Histórico do Studio os lista. Outros mods podem acrescentar regras, dicas e estatísticas ao NextgreIQL.

## Se algo der errado

> [!PROBLEM] A resposta é "EXPLAIN mostra os planos de CRAFT e de SELECT".
> Coloque EXPLAIN apenas antes de um CRAFT ou de um SELECT.

> [!PROBLEM] A resposta é "EXPLAIN precisa de uma instrução para explicar".
> Escreva o CRAFT ou o SELECT depois de EXPLAIN.

> [!PROBLEM] A resposta é "nenhum item se chama" e o nome que você digitou.
> Confira a grafia e use o nome curto, como iron_ingot.

> [!PROBLEM] A resposta diz que nada na rede sabe fabricar o item.
> Nenhuma receita o fabrica. Escolha outro item.

> [!PROBLEM] A resposta diz que todo plano para o item foi posto de lado.
> As regras do planejador rejeitaram todos os planos. Veja a aba Regras do Nextgre Planner Studio.

> [!PROBLEM] A resposta diz que a rede não conseguiu começar a fabricação.
> Confira o armazenamento e as máquinas da rede.

> [!SEE] jsc:nextgre_hints, jsc:nextgre_planner_studio
