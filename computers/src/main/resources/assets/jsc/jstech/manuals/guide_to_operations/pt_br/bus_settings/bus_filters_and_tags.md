---
section: bus_settings
order: 126
icon: jsc:export_bus
---
# Filtros, tags e correspondência

Um filtro é a lista das coisas que um barramento recebe ordem de mover. Ele tem cinco espaços, e cada espaço guarda um item, um fluido ou uma substância química. Um recipiente de fluido cheio seleciona o seu fluido.

As duas opções da lista são SÓ ESTES e EXCETO ESTES. SÓ ESTES move o que a lista nomeia. EXCETO ESTES move tudo o que a lista não nomeia.

## O que se move

Table: O que se move por um filtro

| Filtro | O que se move |
| --- | --- |
| Vazio, SÓ ESTES, num Barramento de Importação | Tudo, porque uma lista vazia aceita tudo. |
| Vazio, SÓ ESTES, num Barramento de Exportação | Nada. Um Barramento de Exportação envia apenas o que ele lista. |
| Vazio, EXCETO ESTES | Tudo. |
| Itens listados, SÓ ESTES | Apenas os itens listados. |
| Itens listados, EXCETO ESTES | Tudo, exceto os itens listados. |

## Definir o filtro na janela

1. Segure um item na mão.
2. Clique em uma célula do filtro com o item, e a célula o lista.
3. Para limpar uma célula, esvazie a mão e clique na célula.
4. Escolha SÓ ESTES ou EXCETO ESTES.

Num barramento Transition, o botão + item lista o item no primeiro espaço livre.

## Tags

Barramentos Advanced também correspondem a tags. Uma tag nomeia um grupo de itens, como c:ores, e uma tag no barramento corresponde a todo item que a tenha. A janela aceita até 4 tags. Clique em + tag e digite a tag, com ou sem o sinal #.

Uma tag conta como uma entrada listada para SÓ ESTES e EXCETO ESTES, do mesmo modo que um item.

## Correspondência aproximada

Um filtro normalmente corresponde a um item exatamente. Um barramento Advanced pode corresponder de forma aproximada, o que a janela chama de FUZZY.

> [!DEFINE] Correspondência aproximada
> Corresponder a um item como o item simples que ele é, qualquer que sejam o seu dano e os seus componentes.

Com FUZZY, uma ferramenta danificada ou encantada corresponde à ferramenta simples. Um fluido corresponde ao seu fluido, e não ao seu balde. A nota da janela diz "Aproximado: um item serve seja qual for o seu dano e os seus componentes."

## Se algo der errado

> [!PROBLEM] O Barramento de Exportação não envia nada.
> O filtro está vazio e definido como SÓ ESTES, então não lista nenhum item. Liste os itens ou escolha EXCETO ESTES.

> [!PROBLEM] O Barramento de Importação puxa mais do que você queria.
> O filtro está vazio, e uma lista vazia aceita tudo. Liste os itens que você quer, com SÓ ESTES.

> [!PROBLEM] Uma ferramenta danificada ou encantada não se move.
> Escolha FUZZY em MATCH em um Barramento Advanced, para que uma ferramenta danificada ou encantada combine com o item comum dela.

> [!SEE] jsc:bus_keep_max_and_priority
