---
section: engine_programs
order: 398
---
# Nextgre Planner Studio

O Nextgre Planner Studio mostra como o NextgreIQL planeja uma fabricação e quanto tempo leva cada passo do plano. Use-o quando uma fabricação demorar mais do que você espera, ou quando quiser ajustar o planejador.

O Studio precisa do motor NextgreIQL, versão 7.0, rodando no Mainframe. O motor é explicado em [o motor NextgreIQL](jsc:nextgre_iql).

> [!DEFINE] Planejador
> A parte do NextgreIQL que escolhe como uma fabricação é feita, pesando as receitas que ele conhece.

> [!DEFINE] Faixa
> Uma máquina que pode rodar ao mesmo tempo que outras máquinas.

> [!DEFINE] Bancada
> Uma receita feita à mão numa bancada de fabricação, em oposição a uma que roda numa máquina.

Table: As quatro abas

| Aba | O que mostra |
| --- | --- |
| Explain | O plano de uma instrução, caixa por caixa. |
| Estatísticas | O que o planejador mediu nesta rede. |
| Regras | As regras do planejador, que você pode ligar e desligar. |
| Histórico | Todo plano que o planejador fez recentemente. |

## Explain

Digite um CRAFT ou um SELECT e depois pressione [[F7]] para Explain, ou [[Shift]]+[[F7]] para Explain Analyze. Explain planeja a instrução sem rodá-la. Explain Analyze também a roda, e as caixas se preenchem conforme a fabricação avança.

O plano é uma árvore de caixas. Cada caixa é um passo, com o seu nome e o seu tempo. Antes de a fabricação rodar, uma caixa mostra "est <n> s", a estimativa em segundos (20 ticks formam um segundo). Depois de Explain Analyze, ela mostra "est <n> s, actual <n> s".

Table: O texto numa caixa

| Texto | O que o passo faz |
| --- | --- |
| `Craft <item> x<quantity>` | Fabrica o item a partir de uma receita. |
| `Process <item> x<quantity>` | Fabrica o item numa máquina. |
| `Pull <item> x<quantity>` | Pega os itens brutos do armazenamento. |
| `Missing <item> x<quantity>` | Nada na rede tem o item. |
| `Index Seek <item> x<quantity>` | Lê o item pelo índice. |

Abaixo do nome, a caixa diz onde o passo roda: "numa bancada, <item> execuções" ou "numa máquina, <item> execuções". Ela também diz "de <server>" para o servidor que entrega os itens, e "<n> passos, <n> ao mesmo tempo" quando passos rodam lado a lado. As dicas da instrução são marcadas nas caixas que elas mudaram.

O cabeçalho mostra "Planejamento <n> ms" e "Execução <n>". Ao lado da árvore, PLANOS PESADOS lista cada plano que o planejador pesou, com o seu custo em ticks. O plano escolhido mostra "escolhido". Um plano posto de lado mostra "posto de lado por", e o motivo.

Um plano parcial mostra "faz <n> de <n> solicitados". Um plano que não pode ser feito mostra "não dá para fazer: faltam <n> <item>".

## Estatísticas

A aba Estatísticas mostra o que o planejador mediu nesta rede. Pressione Colher Estatísticas para medir de novo. A resposta diz "estatísticas colhidas: <n> tipos de item em <n> servidores, <n> receitas cronometradas".

Table: As estatísticas

| Linha | O que conta |
| --- | --- |
| Tipos de item indexados | Os tipos de item que o índice conhece. |
| Servidores | Os servidores de armazenamento que o planejador conhece. |
| Receitas cronometradas | As receitas que têm um tempo medido. |
| Fabricações medidas | As fabricações que o planejador mediu. |
| Último ANALYZE | "nunca", ou há quantos minutos o ANALYZE rodou pela última vez. |
| <recipe> numa bancada, <recipe> numa máquina | O tempo medido de uma receita. |

## Regras

Uma regra é uma escolha que o planejador faz ao pesar planos. Você pode ligar ou desligar cada regra para este Mainframe. As regras que outros mods acrescentam aparecem listadas em REGRAS DE OUTROS MODS.

Table: As regras próprias do planejador

| Regra | O que faz |
| --- | --- |
| Pesar máquinas contra bancadas | Também planeja a fabricação com receitas de máquina antes das receitas de bancada, e mantém a mais barata. |
| Pesar cada receita do resultado | Planeja a fabricação uma vez com cada receita de bancada que fabrica o que foi pedido, e mantém a mais barata. |
| Usar tempos medidos | Calcula cada receita pelo tempo que foi medido para ela levar nesta rede, em vez de um palpite. |
| Contar passos que rodam ao mesmo tempo | Considera os passos que não esperam uns pelos outros como rodando lado a lado. |
| Pesar os servidores mais rápidos | Também planeja pegar os materiais brutos dos servidores que os entregam mais rápido. |

## Dicas

Uma dica vai depois de um CRAFT e muda o plano dele. As dicas embutidas são estas. As dicas são explicadas por completo em [as dicas do NextgreIQL](jsc:nextgre_hints).

Table: As dicas embutidas

| Dica | Recebe um valor | O que faz |
| --- | --- | --- |
| `PREFER SOURCE <server>` | Sim, um nome de servidor | Pega os materiais brutos primeiro do servidor nomeado. |
| `AVOID SOURCE <server>` | Sim, um nome de servidor | Não pega nada do servidor nomeado enquanto outro o tiver. |
| `MAX PARALLEL <n>` | Sim, um número inteiro acima de 0 | Roda no máximo essa quantidade de etapas da fabricação ao mesmo tempo. |
| `PREFER MACHINE` | Não | Fabrica numa máquina tudo o que tanto uma máquina quanto uma bancada fabricam. |
| `PREFER BENCH` | Não | Fabrica numa bancada tudo o que tanto uma máquina quanto uma bancada fabricam. |

## Histórico

Todo plano que o NextgreIQL fez recentemente fica guardado no Mainframe. Clique em um plano para mostrá-lo na aba Explain. A coluna Estado mostra "planejado", "em execução", "concluído" ou "ficou aquém". Até que uma fabricação seja planejada, a aba Histórico diz "Nenhum plano ainda. Toda fabricação que o NextgreIQL planeja fica guardada aqui."

## Se algo der errado

> [!PROBLEM] O programa se recusa a abrir e diz que nenhum motor compatível foi encontrado nesta rede.
> Rode o NextgreIQL, versão 7.0, no Mainframe ([](jsc:engine_services)).

> [!PROBLEM] O Studio diz "EXPLAIN precisa de uma instrução para explicar".
> Escreva um CRAFT ou um SELECT depois de EXPLAIN.

> [!PROBLEM] O Studio diz "EXPLAIN mostra os planos de CRAFT e de SELECT".
> Explique um CRAFT ou um SELECT. Outras instruções não têm plano para mostrar.

> [!PROBLEM] O Studio diz "as dicas mudam o plano de um CRAFT".
> Coloque a dica depois de um CRAFT, e não depois de outra instrução.

> [!PROBLEM] O Studio diz "MAX PARALLEL pede um número inteiro acima de zero".
> Escreva um número inteiro acima de 0 depois de MAX PARALLEL.

> [!PROBLEM] O Studio diz "nenhum servidor da rede se chama <name>".
> Confira o nome do servidor no Pesquisador de Objetos ([](jsc:studio_explorer)).

> [!PROBLEM] O Studio diz "nenhum item se chama <name>".
> Confira a grafia do nome do item.

> [!PROBLEM] O Studio diz "nada na rede sabe fazer <name>".
> Escolha um item que uma receita da rede fabrica.

> [!PROBLEM] O Studio diz "todo plano para <name> foi posto de lado".
> Veja PLANOS PESADOS para entender por que cada plano foi posto de lado, e também a aba Regras.

> [!PROBLEM] O Studio diz "a rede não conseguiu começar a fabricação de <name>".
> Veja as Operações no Monitor de Atividade ([](jsc:studio_activity)).

> [!PROBLEM] Um plano mostra "não dá para fazer: faltam <n> <item>".
> A rede não tem itens suficientes desse tipo. Traga os itens que faltam e depois explique de novo.

> [!SEE] jsc:nextgre_iql, jsc:nextgre_hints, jsc:craft_planner
