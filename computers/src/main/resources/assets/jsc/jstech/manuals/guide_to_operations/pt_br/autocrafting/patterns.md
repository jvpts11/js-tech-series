---
section: autocrafting
order: 142
---
# Padrões

Um padrão é uma receita que a rede pode usar, e há três tipos. Cada tipo serve a um trabalho diferente, e você escreve cada um na sua própria aba do Pattern Studio ([](jsc:pattern_studio)).

## Padrões de bancada

Um padrão de bancada é uma grade 3x3 de nove células e um resultado. Uma célula que a receita deixa vazia continua vazia. Você pode dar ao padrão um nome de até 64 caracteres e uma nota de até 256 caracteres, ou deixar os dois de fora: um padrão sem nome fica com o nome do seu resultado.

Uma célula pode aceitar uma tag de item, como qualquer tipo de tábua, em vez de um item exato. Quando uma fabricação é planejada, a rede usa o que tiver dessa tag em estoque. A dica "Clique com o botão direito em uma célula: a tag que ela aceita" mostra o que uma célula recebe.

Um padrão de bancada guarda somente itens. Você coloca fluidos e substâncias químicas nos padrões de máquina.

## Padrões de máquina

Um padrão de máquina diz o que entra em uma máquina e o que sai dela. Você escreve as suas entradas e as suas saídas, e cada uma tem uma quantidade por execução: uma entrada de 1 significa um desse item a cada vez que a máquina roda.

Table: Partes de um padrão de máquina

| Parte | O que guarda |
| --- | --- |
| Entrada | Um item, um fluido ou uma substância química, com a quantidade que consome por execução (no mínimo 1). |
| Saída | Um item, um fluido ou uma substância química, com a quantidade que ele entrega por execução e uma chance em porcentagem. |
| Tempo limite | Por quanto tempo a fabricação espera enquanto nada se move na interface, em ticks (o padrão é 200, o que equivale a 10 segundos). |

A chance de uma saída fica entre 1 e 100 por cento. Uma saída de 100 sempre sai. Uma chance menor cria um subproduto que pode ou não sair. A rede conta um subproduto pela quantidade esperada quando planeja a fabricação, mas um subproduto nunca bloqueia uma fabricação: a quantidade real é contada quando a máquina roda.

Qualquer outra coisa que sai da máquina é uma saída inesperada. Ela vai para a rede, e nunca faz uma fabricação falhar.

Uma entrada pode ser marcada como estimada quando a sua quantidade veio de um medidor que mede por tick, e não do autor do padrão. O Pattern Studio então mostra "por execução (estimada)" depois da quantidade, e a receita continua rodando.

Um padrão de máquina só funciona a partir de uma Interface de Fabricação que o contenha. A interface é descrita em [](jsc:crafting_interfaces).

## Padrões multietapas

Um padrão multietapas é um pipeline: uma lista ordenada de etapas, e cada etapa é exatamente um padrão de bancada ou um padrão de máquina. A etapa 1 pega as suas entradas da rede. Cada etapa seguinte pega a saída da etapa anterior, e só começa quando essa etapa tiver feito o seu resultado.

> [!DEFINE] Pipeline
> Um padrão multietapas: uma lista ordenada de etapas que a rede executa uma após a outra.

Fluidos funcionam somente dentro de etapas de máquina. Um pipeline não pode conter outro pipeline, e comporta até 16 etapas.

A rede calcula quanto cada etapa precisa produzir, começando pela última e voltando. Por exemplo, uma etapa que faz um bloco de ferro precisa de nove lingotes da etapa anterior, e uma etapa que faz um lingote precisa de nove pepitas da etapa anterior.

## Se algo der errado

> [!PROBLEM] O pipeline não tem estágios.
> Adicione um padrão de bancada ou de máquina como etapa.

> [!PROBLEM] A tela diz "Um pipeline não pode conter outro pipeline".
> Coloque as etapas do pipeline interno diretamente, um padrão de bancada ou de máquina cada.

> [!PROBLEM] Um pipeline com 16 etapas recusa outra.
> Remova uma etapa antes de adicionar outra: um pipeline comporta no máximo 16 etapas.

> [!SEE] jsc:pattern_studio, jsc:pattern_encoders, jsc:crafting_interfaces, jsc:fluids, jsc:when_a_craft_goes_wrong
