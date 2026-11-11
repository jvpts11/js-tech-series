---
section: engine_programs
order: 399
---
# Prophet Reactive Console

O Prophet Reactive Console mantém o estoque da rede onde você quer. Você diz a ele quais itens manter. Quando um nível cai, ele pede à rede, por conta própria, o que está faltando.

O console precisa do motor YourIQL, versão 3.23, rodando no Mainframe. As instruções próprias dele são explicadas em [o motor YourIQL](jsc:prophet_youriql).

> [!DEFINE] Estado
> Um item que a rede é instruída a manter. Na lista de Estados, cada estado é uma linha KEEP.

> [!DEFINE] Faixa
> A faixa dentro da qual um estado é mantido: pelo menos um número, ou entre dois números.

> [!DEFINE] Nível
> O que a rede conta para um item: a quantidade em uso, mais a quantidade a caminho, mais a quantidade que chega.

## O prompt

Digite uma instrução do YourIQL no prompt, depois pressione Check ou Apply. Check lê a instrução sem mudar nada, e responde "A instrução está bem escrita." quando ela está certa. Apply executa a instrução.

Uma instrução que não começa com KEEP, WATCH, FORGET ou SHOW STATES vai para o motor IQL normal, explicado em [os motores](jsc:iql_engines_overview).

## As abas

Table: As quatro abas

| Aba | O que mostra |
| --- | --- |
| Estados | Os estados que a rede deve manter, uma linha cada. |
| Assinaturas | Os observadores, cada um com seu número e status. |
| Reações | O que o motor fez há pouco. |
| Configurações | Com que frequência o motor olha, quanto pede de cada vez, e se reage. |

## As instruções

Table: As instruções do YourIQL

| Instrução | Forma | O que faz |
| --- | --- | --- |
| KEEP | `KEEP <item> >= <n>` | Mantém pelo menos <n> do item, contando o que está a caminho. |
| KEEP com faixa | `KEEP <item> BETWEEN <a> AND <b>` | Mantém o nível entre <a> e <b>. |
| WATCH | `WATCH <item> < <n> DO <statement>` | Executa a instrução uma vez, quando o nível cruza a linha. As comparações são <, <=, > e >=. |
| WATCH com CRAFT | `WATCH <item> < <n> DO CRAFT <item> TO <n>` | Fabrica o item até o nível <n>, quando o observador dispara. |
| FORGET | `FORGET <item>` | Para de manter o item. |
| FORGET WATCH | `FORGET WATCH <n>` | Remove o observador de número <n>. |
| SHOW STATES | `SHOW STATES` | Lista os estados e os observadores. |

Um número é um inteiro, zero ou mais. Um limite superior em uma faixa só indica onde o nível deixa de estar onde deveria: nada é jogado fora para baixá-lo. A receita [manter um estoque de um item](jsc:recipe_keep_stock) mostra uma linha KEEP em funcionamento.

## A aba Estados

A aba Estados lista cada estado, com a situação dele e o que está sendo feito por ele. Abaixo da lista, o gráfico do estado selecionado mostra o nível dele.

Table: As colunas da aba Estados

| Coluna | O que mostra |
| --- | --- |
| ESTADO DESEJADO | A faixa, ou o mínimo, que você pediu. |
| STATE | Em que pé está o estado, como uma das palavras da tabela abaixo. |
| NOW | O nível, comparado com a sua faixa. |
| WORK | As Operações a caminho, como "<n> a caminho", ou "nenhuma operação". |
| ÚLTIMA REAÇÃO | A última coisa feita para o estado. |

Table: Os estados

| Status | Significado |
| --- | --- |
| Novo | Ainda não verificado. |
| Estoque | Dentro da sua faixa, sem nada a caminho. |
| Trabalhando | Há trabalho a caminho para trazê-lo para a faixa, ou para mantê-lo ali. |
| Não é possível manter | Abaixo da sua faixa, e nada pôde ser posto em andamento. |
| Acima | Acima da sua faixa. Nada é jogado fora para baixá-lo. |
| Armado | Um observador, esperando sua condição. |
| Disparado | Um observador que disparou, e espera a condição deixar de valer. |

Quando o nível está abaixo da faixa, o motor pede à rede a diferença como uma fabricação. Se nada pôde ser posto em andamento, o estado mostra Não é possível manter, e o motor tenta de novo depois de 600 ticks (30 segundos).

Um estado acima da sua faixa, sem nada a caminho, mostra Acima. Com trabalho a caminho, mostra Trabalhando.

## A aba Assinaturas

A aba Assinaturas lista os observadores. Cada observador mostra seu número, seu status, Armado ou Disparado, e quantas vezes disparou, como "disparou <n> vezes". Pressione Forget para remover um observador. O número é o que FORGET WATCH usa.

## A aba Reações

A aba Reações lista o que o motor fez há pouco, com quanto tempo faz, como "<n> s atrás" ou "<n> min atrás". Ela guarda no máximo 32 reações.

Table: As reações

| Reação | O que aconteceu |
| --- | --- |
| pediu <n> <item>, Operação <id> | O motor pediu à rede uma fabricação, que agora é uma Operação. |
| nada na rede fabrica <item> | O motor queria um item que nenhuma receita da rede fabrica. |
| observador <n> disparou: <statement> | Um observador disparou, e sua instrução foi executada. |
| observador <n> disparou, e foi recusado: <reason> | Um observador disparou, mas sua instrução foi recusada. |
| observador <n> disparou sem nada a fabricar | Um observador disparou, e não havia nada a fabricar. |

## A aba Configurações

A aba Configurações define com que frequência o motor olha para a rede, quanto ele pede de uma vez, e se ele reage ou não.

Table: As configurações

| Configuração | Opções | Padrão |
| --- | --- | --- |
| Verificar a rede a cada <n> s | 1, 2, 5 ou 10 segundos | 1 segundo (20 ticks) |
| Pedir no máximo, em uma fabricação | 64, 256, 1.024 ou 4.096 itens | 1.024 itens |
| Reagir a mudanças | Ligado ou Pausado | Ligado |

Quando Reagir a mudanças está Pausado, o motor não faz reação alguma. Os estados continuam sendo mostrados.

## Se algo der errado

> [!PROBLEM] O console se recusa a abrir e diz que nenhum motor compatível foi encontrado nesta rede.
> Execute o motor YourIQL no Mainframe ([](jsc:engine_services)).

> [!PROBLEM] A instrução é recusada: "sintaxe: KEEP item >= n, ou KEEP item BETWEEN a AND b".
> Escreva uma linha KEEP em uma destas duas formas.

> [!PROBLEM] A instrução é recusada: "sintaxe: WATCH item < n DO instrução".
> Escreva a comparação como um de <, <=, > ou >=, e coloque DO antes da instrução.

> [!PROBLEM] A instrução é recusada: "sintaxe: FORGET item, ou FORGET WATCH n".
> Escreva FORGET com um item, ou FORGET WATCH com um número.

> [!PROBLEM] A instrução é recusada: "nenhum item se chama <name>".
> Confira a grafia do nome do item.

> [!PROBLEM] FORGET é recusado: "<name> não é mantido".
> Esqueça apenas um item que uma linha KEEP mantém. SHOW STATES os lista.

> [!PROBLEM] FORGET WATCH é recusado: "nenhum watch tem o número <n>".
> Escreva SHOW STATES para ver o número de cada observador.

> [!PROBLEM] A aba Reações diz "observador <n> disparou, e foi recusado: <reason>".
> Esqueça esse observador com FORGET WATCH, depois defina-o de novo com uma instrução que funcione.

> [!PROBLEM] Um estado mostra "Não é possível manter".
> Espere: o motor tenta de novo depois de 600 ticks (30 segundos). Confira se a rede tem os itens que a fabricação precisa.

> [!PROBLEM] A aba Reações fica vazia, e nenhum estado reage.
> Abra a aba Configurações e defina "Reagir às mudanças" como On.

> [!SEE] jsc:prophet_youriql, jsc:prophet_watch, jsc:recipe_keep_stock
