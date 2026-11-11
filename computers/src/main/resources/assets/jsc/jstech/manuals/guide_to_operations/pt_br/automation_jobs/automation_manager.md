---
section: automation_jobs
order: 153
---
# O Gerenciador de Automação

O Gerenciador de Automação é a janela onde você escreve e acompanha os jobs, sem digitar IQL. É um programa de área de trabalho, então você o abre pela área de trabalho, como os outros programas ([](jsc:programs)).

Ele roda no Frames, no Linux e no FreeBSD. Não roda no UNIX System V. Ele precisa de um Mainframe na rede: sem um, a janela diz "sem Mainframe".

Table: As partes da janela

| Parte | O que faz |
| --- | --- |
| A faixa superior | Verde, com o nome do Motor e "online", enquanto o Motor de Automação roda; caso contrário âmbar, com "Nenhum motor - instale o Motor de Automação no Mainframe". |
| A contagem de jobs | Quantos jobs a rede mantém, como 3 jobs. |
| A lista de jobs | Uma linha para cada job, com as colunas abaixo. |
| NOVO JOB | O formulário que escreve um novo job, em quatro tipos. |

## A lista de jobs

Table: As colunas da lista de jobs

| Coluna | O que mostra |
| --- | --- |
| JOB | O nome do job. |
| TYPE | O tipo de job: Repor abaixo de, Fabricação em Lote, Mover Periodicamente ou Personalizado. |
| TRIGGER | O intervalo, como every 5m, ou a condição. |
| ACT | = pausa o job, > retoma um job pausado, e x apaga o job na hora, sem perguntar. |

A janela descobre o tipo de um job pela sua instrução e pelo seu gatilho, então um job que você digita em IQL aparece da mesma forma. Uma instrução que começa com CRAFT é Repor abaixo de quando o gatilho é WHEN, e Fabricação em Lote nos outros casos. Uma instrução que não começa nem com CRAFT nem com MOVE é Personalizado.

## Criando um job

1. Digite um nome em Name. Use uma palavra só, sem espaços.
2. Escolha o tipo: Repor abaixo de, Fabricação em Lote, Mover ou IQL.
3. Preencha os campos que o tipo mostra.
4. Clique em Criar job. Os campos são limpos, e o novo job aparece na lista.

Table: Os quatro tipos

| Tipo | Campos | O que o job faz |
| --- | --- | --- |
| Repor abaixo de | Name, Item id, When below | Fabrica a quantidade do item quando o estoque dele na rede cai abaixo da quantidade. |
| Fabricação em Lote | Name, Item id, Amount, Every | Fabrica a quantidade do item a cada intervalo. |
| Mover | Name, Item (blank=all), Amount, From, To, Every | Move a quantidade do item de um lugar para o outro a cada intervalo; um item em branco move tudo. |
| IQL | Name, Script (on Mainframe disk), Every | Executa um arquivo .iql do disco de sistema do Mainframe a cada intervalo. |

O campo Script lista os arquivos .iql do disco de sistema do Mainframe. Se o disco não tiver nenhum, o campo diz "nenhum arquivo .iql no disco de sistema do Mainframe".

Um job Repor abaixo de conta o estoque que está na rede agora. A janela acrescenta a nota "Não conta o que já está a caminho." Ela também mostra uma dica que começa com "Prophet YourIQL conta isso: KEEP", com o item e a quantidade. O Prophet é explicado em ([](jsc:prophet_youriql)).

O campo Amount aceita só dígitos, e um Amount em branco conta como 1 item. Uma quantidade abaixo de 1 item vira 1 item. O campo Every aceita um intervalo como 30s ou 5m, e ele precisa ser maior que zero. As formas são as de ([](jsc:automation_engine)).

Criar um job com o nome de um job existente substitui esse job pelo novo.

## Se algo der errado

> [!PROBLEM] O chat diz "os jobs ficam com o Motor de Automação; instale-o primeiro no Mainframe".
> Instale o Motor de Automação no Mainframe.

> [!PROBLEM] O chat diz "Dê um nome ao job."
> Digite um nome no campo Name.

> [!PROBLEM] O chat diz "Repor abaixo de precisa de um item."
> Digite um Item id, como iron_ingot.

> [!PROBLEM] O chat diz "Fabricação em Lote precisa de um item e um intervalo válido (ex.: 30s, 5m)."
> Digite um item e um intervalo como 30s ou 5m.

> [!PROBLEM] O chat diz "Mover Periodicamente precisa de DE, PARA e um intervalo válido (ex.: 30s)."
> Digite as posições From e To, e um intervalo como 30s.

> [!PROBLEM] O chat diz "Um job de Script IQL precisa de um arquivo .iql e um intervalo válido (ex.: 30s)."
> Escolha um script na lista, e digite um intervalo como 30s.

> [!PROBLEM] O chat diz que o script não foi encontrado no disco do Mainframe.
> Salve o arquivo .iql no disco de sistema do Mainframe, depois escolha-o.

> [!PROBLEM] A faixa diz "Nenhum motor" mesmo com o motor instalado.
> A faixa só mostra o motor enquanto o Mainframe roda. Ligue o Mainframe.

> [!SEE] jsc:automation_engine, jsc:programs
