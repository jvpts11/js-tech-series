---
section: bus_settings
order: 128
---
# Condições

Uma condição é uma regra que um barramento verifica antes de se mover. Um barramento Standard ou superior pode ter até 4 condições, e todas elas precisam valer para que o barramento se mova. Um barramento cuja condição não vale espera, e a aba ACTIVITY informa isso.

O barramento verifica as condições a cada 20 ticks, ou seja, 1 segundo, e guarda o resultado até a próxima verificação. Você também pode escrever condições em IQL ([](jsc:iql)) com WHEN.

Table: Os três tipos de condição

| Tipo | O que a janela diz | Vale quando |
| --- | --- | --- |
| `STOCK` | só enquanto a rede tiver menos de 64 minecraft:iron_ore | A rede tem menos do que esse número. |
| `HOURS` | só entre 18:00 e 06:00 | O relógio do jogo está entre as duas horas. |
| `AFTER` | depois que o barramento Coal in terminar | O barramento nomeado não se move há 40 ticks. |

## STOCK

STOCK conta o que a rede inteira guarda. A condição se mantém enquanto esse total estiver abaixo do número que você definir. O número começa em 64 itens e muda de 1 em 1 item; com Shift pressionado, muda de 64 em 64 itens.

> [!DEFINE] Estoque
> Quantas unidades de um item a rede inteira possui, e não a quantidade em um único baú.

Escreva uma tag depois de um sinal #, como #c:ores, e STOCK conta todo item possuído que tenha a tag. Na janela, clique no campo de item com um item na mão para escrever o ID desse item nele.

## HOURS

HOURS vale entre duas horas do dia. Você define FROM e TO, e quando TO vem antes de FROM, a janela passa da meia-noite. O padrão é de 18:00 a 06:00, a noite.

As horas seguem o relógio do jogo, e não o relógio real. 06:00 é o início de cada dia do jogo. Se FROM e TO forem a mesma hora, a condição vale o dia inteiro.

Um barramento tem apenas uma janela de horário. Adicionar uma segunda é recusado, e a barra de ação mostra "Um barramento tem uma só janela de horário. Remova-a antes de pôr outra." Pelo IQL, uma nova janela de horário substitui a antiga.

## AFTER

AFTER espera por outro barramento na mesma rede. Ela vale depois que o barramento nomeado não se move há 40 ticks, ou 2 segundos. Terminado aqui significa não se mover por esse tempo, e não que o trabalho esteja concluído.

Se nenhum barramento tiver esse nome, nada segura este barramento, então a condição vale. Digite o nome na janela, com até 32 caracteres. O nome não diferencia maiúsculas de minúsculas.

## Adicionar e remover

1. Abra a janela do barramento e vá para CONFIGURAR.
2. Clique em + condição e escolha STOCK, HOURS ou AFTER.
3. Preencha o editor, usando os botões - e + para os números.
4. Para remover uma condição, clique na extremidade direita da linha dela.

## Se algo der errado

> [!PROBLEM] O barramento espera e não se move.
> Abra a aba Activity dele. Uma linha que diz "uma condição o segura" significa que uma condição não está cumprida, então o Barramento espera. Leia a frase de cada condição em CONFIGURE ([](jsc:bus_activity)).

> [!PROBLEM] A janela recusa uma condição STOCK.
> Uma contagem abaixo de 1, um ID de item desconhecido ou uma tag inválida é recusado. A janela não mostra nada quando recusa.

> [!PROBLEM] O barramento não aceita uma quinta condição.
> Um barramento tem até 4 condições. Remova uma primeiro e depois adicione a nova.

> [!PROBLEM] A condição HOURS vale o dia inteiro.
> FROM e TO são a mesma hora. Defina duas horas diferentes.

> [!SEE] jsc:bus_activity
