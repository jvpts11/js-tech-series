---
section: network_tools
order: 88
icon: jsc:cd_rom
---
# Análise de Armazenamento

A Análise de Armazenamento mostra para onde foi o armazenamento da rede. É um painel do conteúdo da rede: os totais, os tipos de item mais numerosos, os tipos com estoque baixo e o quanto cada servidor está cheio. Você a abre quando quer saber do que a rede está em falta.

Table: Blocos da Análise de Armazenamento

| Bloco | O que conta |
| --- | --- |
| TYPES | A quantidade de tipos de item que a rede guarda. |
| TOTAL | O total do conteúdo da rede. |
| LOW | A quantidade de tipos de item com estoque baixo. |

Table: Seções da Análise de Armazenamento

| Seção | O que mostra |
| --- | --- |
| PRINCIPAIS ITENS | Os tipos maiores, em um gráfico de barras. |
| ESTOQUE BAIXO | Os tipos abaixo do limite, ou todos em estoque quando nenhum estiver. |
| POR SERVIDOR | O quanto cada servidor está cheio. |

> [!DEFINE] Limite
> A quantidade abaixo da qual um tipo de item aparece como estoque baixo. Começa em 64 itens.

Use o controle com o rótulo Limite para mudar a quantidade. Clique em um tipo para ver a sua visão detalhada. A visão detalhada tem um botão Voltar, o total do tipo, os lugares onde ele fica ARMAZENADO EM, para o que ele é USADO PARA FAZER, e os [barramentos](jsc:buses) que filtram esse tipo. Um tipo que nenhum barramento filtra é marcado como não estando em nenhum filtro de barramento.

O campo de pesquisa, que diz pesquisar item..., restringe a lista. O painel pergunta à rede de novo por conta própria, de tempos em tempos, para que os números acompanhem a rede.

## Se algo der errado

> [!PROBLEM] A lista diz que não há correspondência.
> Nenhum tipo de item corresponde à pesquisa. Tente uma palavra mais curta.

> [!PROBLEM] O painel não mostra nenhum número.
> A rede ainda não tem itens. Coloque itens na rede, e os números vão acompanhar.

> [!SEE] jsc:buses, jsc:items_as_data, jsc:storage
