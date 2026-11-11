---
section: library_windows
order: 470
---
# Janelas, linhas e colunas

Uma janela é uma caixa na área de trabalho, com um título e um widget dentro. Um programa cria uma Window, coloca um widget nela e a mostra. A barra de tarefas lista as janelas abertas. As telas que as exibem são explicadas em [](jsc:monitors) e [](jsc:desktops).

> [!DEFINE] Widget
> Uma parte de uma janela, como um botão, um rótulo ou uma lista.

> [!DEFINE] Peso
> A regra de uma linha ou coluna para dividir o espaço sobrando entre os seus widgets.

Table: Membros da janela

| Membro | O que faz |
| --- | --- |
| `Window.new(title, width, height)` | Cria uma janela com esse título e tamanho. Somente Sigma#. |
| `Window.Title` | O título da janela. |
| `Window.Content` | O widget que a janela contém. |
| `Window.Open` | Sim enquanto a janela estiver exibida. |
| `Window.Show()` | Mostra a janela na área de trabalho e na barra de tarefas. |
| `Window.Close()` | Fecha a janela. |
| `Window.Add(widget, x, y, width, height)` | Coloca um widget em uma posição exata da janela. |
| `Window.OnClose` | O evento que é executado quando a janela fecha. |

## Linhas e colunas

Uma linha põe os seus widgets lado a lado, e uma coluna os empilha. Uma linha divide a sua largura entre os seus widgets, e uma coluna divide a sua altura.

Table: Membros de Row e Column

| Membro | O que faz |
| --- | --- |
| `Row.new()` | Cria uma linha vazia. Column.new() cria uma coluna vazia. |
| `Row.Add(widget)` | Adiciona um widget no seu tamanho próprio. |
| `Row.Add(widget, weight)` | Adiciona um widget que divide o espaço que sobra, pelo seu peso (1 ou mais). |
| `Row.Spacing` | O espaço entre os widgets, 4 por padrão. |
| `Row.Clear()` | Remove os widgets da linha. |
| `Column.Add` | O mesmo que Row.Add, para uma coluna. |
| `Column.Spacing` | O mesmo que Row.Spacing, para uma coluna. |
| `Column.Clear()` | Remove os widgets da coluna. |

MessageBox.Show(title, text) abre uma janela que a máquina faz para um título e uma linha de texto. Somente Sigma#.

## Widgets em geral

Todo widget tem Widget.Visible e Widget.Enabled, que são sim ou não, e Widget.Width e Widget.Height, que são números inteiros. Uma largura ou altura de 0 usa o que o widget precisa. Qualquer outro valor fica entre 8 e o máximo que a janela permite.

Table: Limites de janelas e widgets

| Elemento | Máximo |
| --- | --- |
| Janelas abertas por programa | 8 |
| Widgets em uma janela, contando os que estão em linhas e colunas | 256 |
| Widgets em uma linha ou coluna | 256 |
| Largura da janela | 60 a 640, 240 por padrão |
| Altura da janela | 60 a 360, 160 por padrão |

## Se algo der errado

> [!PROBLEM] O programa para e diz que um programa pode ter 8 janelas abertas ao mesmo tempo.
> Feche uma janela que você não precisa mais com Window.Close(), antes de mostrar outra.

> [!PROBLEM] O programa para e diz que uma janela contém no máximo 256 widgets.
> Use menos widgets. Os widgets dentro de linhas e colunas também contam.

> [!PROBLEM] O programa para e diz que uma linha ou uma coluna não pode conter a si mesma.
> Não coloque uma linha ou uma coluna dentro dela mesma, nem dentro de uma linha ou coluna que ela contenha.

> [!PROBLEM] O programa para e diz que um widget não pode conter a si mesmo.
> Dê a cada widget um lugar na janela, e nunca o coloque dentro dele mesmo.

> [!PROBLEM] O programa para e diz que uma caixa de diálogo abre sobre uma janela que está aberta.
> Mostre a janela primeiro e depois abra a caixa de diálogo sobre ela.

> [!PROBLEM] O programa para e diz que esta janela já tem uma caixa de diálogo aberta sobre ela.
> Feche a caixa de diálogo aberta primeiro e depois abra a próxima.

> [!PROBLEM] O programa para e diz que um valor não pode ser escrito pelo programa.
> Escreva apenas os valores que o programa pode escrever, como os que ele mesmo criou.

> [!PROBLEM] O programa para e diz "um componente <kind> não aceita esse valor".
> Confira o tipo do widget. Cada tipo aceita apenas os seus próprios valores.

> [!SEE] jsc:monitors, jsc:desktops, jsc:widgets_basic
