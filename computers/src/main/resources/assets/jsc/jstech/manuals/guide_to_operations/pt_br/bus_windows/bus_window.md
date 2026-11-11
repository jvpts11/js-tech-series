---
section: bus_windows
order: 129
icon: jsc:import_bus
---
# A janela de um barramento

Clique com o botão direito em um barramento colocado para abrir a sua janela. O título em maiúsculas no topo diz qual barramento é, como BARRAMENTO DE IMPORTAÇÃO ou ARMAZENAMENTO EXTERNO. Uma luz no canto superior direito mostra o estado do seu cabo, e o seu próprio inventário fica na mesma tela.

Table: As abas da janela

| Aba | O que mostra |
| --- | --- |
| CONFIGURAR | O nome do barramento e os seus ajustes, linha por linha. |
| ACTIVITY | Os movimentos e as retenções do barramento, explicados na página O que um barramento fez. |
| SOFTWARE | Os ajustes escritos como linhas de IQL e SIGMA, e qual programa definiu o quê. |

## A luz

A luz fica verde quando o cabo do barramento alcança uma rede. Passe o mouse sobre ela e diz LINKED. Fica vermelha quando o cabo não alcança rede nenhuma, e então diz OFFLINE.

## CONFIGURAR

O campo NAME vem primeiro. Digite um nome, com até 32 caracteres, e a mudança chega ao servidor na hora. O software acha o barramento por este nome, então dê um a cada barramento ([](jsc:programming_buses)).

Abaixo do nome, as linhas mostram os ajustes que a era do seu barramento tem. Vintage mostra os menos, e Advanced mostra os mais. Cada linha tem um rótulo, os seus controles e às vezes uma nota.

Table: Como você muda uma linha

| Tipo de linha | Como você a muda |
| --- | --- |
| Células do filtro | Clique numa célula com um item na mão para listá-lo. Com a mão vazia, clique nela para limpá-la. |
| Palavras de opção, como SÓ ESTES ou SOB DEMANDA | Clique na opção que você quer. |
| Números: KEEP, MAX e PRIORITY | Clique nos botões - ou +: PRIORITY muda de 1 em 1, e KEEP e MAX mudam de 16 em 16 com Shift pressionado. |
| Condições e tags | Clique no botão + para adicionar uma, e clique no chip de uma tag ou na extremidade direita da linha de uma condição para removê-la. |

## SOFTWARE

A aba SOFTWARE escreve os ajustes do barramento como instruções IQL e como chamadas SIGMA, para você copiá-los para um programa. ADDRESS mostra o nome que um programa usa, como bus://Ore in. A seção DEFINIDO POR PROGRAMAS lista qual programa definiu cada ajuste, com linhas como "Sigma: o manter".

> [!DEFINE] Sigma
> Uma linguagem de programação do mod. Suas chamadas definem barramentos do mesmo jeito que as instruções IQL.

Passe o mouse sobre um ajuste que um programa definiu, e ele diz qual programa o definiu. Mudar o ajuste na janela o tira do programa, à mão.

## Se algo der errado

> [!PROBLEM] Um programa não consegue achar o barramento.
> O barramento não tem nome, ou o programa usa outro. Dê ao barramento o nome que o programa usa, em CONFIGURAR. ADDRESS diz "Dê um nome ao barramento em CONFIGURAR para o software poder achá-lo." quando o nome está faltando.

> [!PROBLEM] A luz diz OFFLINE.
> O cabo do barramento não está em nenhuma rede. Conecte o cabo a uma rede ([](jsc:first_network)).

> [!PROBLEM] Um ajuste não é o que você definiu.
> Um programa definiu o ajuste. Passe o mouse sobre o ajuste para ver qual programa, e mude-o à mão para tomá-lo de volta.

> [!SEE] jsc:bus_activity, jsc:programming_buses
