---
section: engine_choice
order: 388
icon: jsc:mainframe
---
# Prophet YourIQL: mantendo um nível

O Prophet YourIQL é o motor que mantém um nível por você. Em vez de pedir uma fabricação a cada vez, você escreve KEEP steel_ingot >= 10000 uma vez só. Depois o motor calcula o que precisa fabricar para manter essa quantidade de lingotes de aço na rede.

> [!DEFINE] Estado
> Um nível que o motor é instruído a manter, escrito como uma instrução KEEP.

> [!DEFINE] Faixa
> A faixa entre um nível inferior e um superior, que um estado mantém.

> [!DEFINE] Observador
> Uma instrução que espera uma condição, e depois executa uma ação quando a condição se torna verdadeira.

Table: Versões do Prophet YourIQL por idade

| Idade do Mainframe | Versão do Prophet YourIQL |
| --- | --- |
| Legacy | 3.23 |
| Transition | 5.0 |
| Standard | 5.6 |
| Advanced | 8.0 |

A ferramenta própria dele é o Prophet Reactive Console ([](jsc:prophet_console)), que precisa do YourIQL 3.23 ou mais novo.

## As instruções

Table: As instruções do Prophet YourIQL

| Instrução | O que faz |
| --- | --- |
| `KEEP item >= n` | Mantém pelo menos n do item. |
| `KEEP item BETWEEN a AND b` | Mantém o item dentro de uma faixa, de a até b. |
| `WATCH` | Executa uma ação uma vez quando uma condição se torna verdadeira. |
| `FORGET item` | Para de manter o item. |
| `FORGET WATCH n` | Remove o observador com esse número. |
| `SHOW STATES` | Lista os estados mantidos e os observadores em uma tabela. |

Qualquer outra instrução vai para a linguagem básica sem alterações.

## Manter um nível

1. Digite KEEP, o nome curto do item, >= e um número.
2. Leia a resposta, que informa o item e o nível dele.

```iql keep_steel.iql
KEEP steel_ingot >= 10000
```

KEEP com >= tem exatamente quatro palavras, e >= precisa ficar como uma palavra própria, com espaços em volta. KEEP com BETWEEN tem exatamente seis palavras, e o número superior não pode ser menor que o inferior.

Os números são inteiros escritos em algarismos, sem sinal de menos. A resposta diz que o item é "mantido em" um nível "ou mais", ou "mantido entre" dois níveis.

Manter o mesmo item de novo substitui a faixa dele.

## A faixa e Acima

KEEP BETWEEN faz o motor fabricar quando o nível cai abaixo da faixa. O número superior só mostra onde o nível deixa de estar certo. Nada é jogado fora para baixar um nível, então passar da faixa nunca é corrigido removendo itens.

## Como o motor mantém um nível

A cada vez que olha, o motor conta o que a rede tem e o que já está a caminho. Trabalho a caminho é uma Operação que começou e ainda não terminou.

Quando o nível está abaixo da faixa, o motor pede à rede que fabrique o que falta. Ele pede no máximo um lote por fabricação, e o tamanho do lote é uma configuração. O pedido passa pela porta de fabricação da rede, sob o nome Prophet YourIQL ([](jsc:how_autocrafting_works)).

Se nada na rede consegue fabricar o item, o estado mostra Não é possível manter. O motor tenta de novo a cada 600 ticks, que são 30 segundos.

## As configurações

O Prophet Reactive Console define três coisas para este motor.

Table: As configurações do Prophet YourIQL

| Configuração | Opções | Padrão |
| --- | --- | --- |
| Com que frequência olha | A cada 1, 2, 5 ou 10 segundos | 1 segundo |
| Maior lote por fabricação | 64, 256, 1.024 ou 4.096 itens | 1.024 itens |
| Reagindo | Ligado ou desligado | Ligado |

Um valor que não está na lista volta ao padrão. O motor só reage enquanto está rodando, então um motor parado ou substituído deixa de reagir.

Os estados e os observadores são salvos junto com o Mainframe. O motor guarda 48 amostras de nível para o gráfico de cada estado, as 32 últimas reações, e até 4 Operações de cada estado no console.

## Se algo der errado

> [!PROBLEM] A resposta é "sintaxe: KEEP item >= n, ou KEEP item BETWEEN a AND b".
> Escreva KEEP com >= e um número, ou com BETWEEN e seus dois números.

> [!PROBLEM] A resposta é "nenhum item se chama" seguido do nome que você digitou.
> Confira a grafia do nome curto do item.

> [!PROBLEM] Um estado mostra "Não é possível manter".
> Nada na rede consegue fabricar o item. Olhe o estado em SHOW STATES, e confira se a rede consegue fabricá-lo.

> [!PROBLEM] Um estado mostra "Acima".
> Nada está errado. O motor não remove itens para baixar um nível.

> [!SEE] jsc:prophet_watch, jsc:prophet_console
