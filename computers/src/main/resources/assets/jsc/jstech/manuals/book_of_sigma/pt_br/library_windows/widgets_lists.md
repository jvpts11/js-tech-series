---
section: library_windows
order: 472
---
# Listas, escolhas, tabelas, árvores e menus

Listas e escolhas guardam muitas entradas, e o jogador escolhe uma. O valor Selected diz ao seu programa qual foi escolhida. Em um RadioGroup, um ComboBox ou um TabView a contagem começa em 1, e 0 significa que nada foi escolhido em um RadioGroup ou em um ComboBox.

> [!DEFINE] Nó
> Uma entrada em uma árvore. Um nó pode ser filho de outro nó.

> [!DEFINE] Seção
> Uma parte de uma barra de status, com texto próprio.

Table: Listas e escolhas

| Widget | Cria | Membros |
| --- | --- | --- |
| `ListBox` | `ListBox.new()` | `ListBox.Add(text), ListBox.Add(text, right text), ListBox.Clear, ListBox.Count, ListBox.Selected, ListBox.OnSelect` |
| `RadioGroup` | `RadioGroup.new()` | `RadioGroup.Add, RadioGroup.Clear, RadioGroup.Count, RadioGroup.Selected, RadioGroup.Across, RadioGroup.OnSelect` |
| `ComboBox` | `ComboBox.new()` | `ComboBox.Add, ComboBox.Clear, ComboBox.Count, ComboBox.Selected, ComboBox.OnSelect` |
| `TabView` | `TabView.new()` | `TabView.Add(title, page), TabView.Clear, TabView.Count, TabView.Selected, TabView.OnSelect` |

RadioGroup.Across é sim para dispor as escolhas lado a lado em vez de em coluna. Um ComboBox é uma lista suspensa e não tem Across. TabView.Selected vai de 1 até a contagem, e a primeira página adicionada é a que aparece primeiro. RadioGroup, ComboBox e TabView são da versão 2.

Table: Tabelas e árvores

| Widget | Cria | Membros |
| --- | --- | --- |
| `Table` | `Table.new()` | `Table.AddColumn, Table.AddRow, Table.Clear, Table.Cell, Table.Count, Table.Selected, Table.OnSelect` |
| `TreeView` | `TreeView.new()` | `TreeView.Add, TreeView.Clear, TreeView.NodeText, TreeView.Count, TreeView.Selected, TreeView.OnSelect` |

Table.AddRow aceita uma lista de textos, ou de um a quatro textos. Table.Cell(row, column) devolve o texto daquele lugar. TreeView.Add devolve o número do nó que criou, e TreeView.Add(text, parent) coloca o nó sob outro. TreeView.NodeText devolve o texto de um nó pelo número dele.

Table: Menus e barras

| Widget | Cria | Membros |
| --- | --- | --- |
| `MenuBar` | `MenuBar.new()` | `MenuBar.Add(menu, entry), MenuBar.Clear, MenuBar.Picked, MenuBar.PickedMenu, MenuBar.OnPick` |
| `ContextMenu` | `ContextMenu.new(owner)` | `ContextMenu.Add, ContextMenu.Clear, ContextMenu.Picked, ContextMenu.OnPick` |
| `StatusBar` | `StatusBar.new(), StatusBar.new(text)` | `StatusBar.Text, StatusBar.Add, StatusBar.Clear` |

Um ContextMenu abre com o outro botão do mouse sobre o widget para o qual foi criado. Um menu guarda 64 entradas.

Table: Outros widgets

| Widget | Cria | Membros |
| --- | --- | --- |
| `TextArea` | `TextArea.new()` | `TextArea.Text, TextArea.OnChange` |
| `NumberBox` | `NumberBox.new()` | `NumberBox.Value, NumberBox.Least, NumberBox.Most, NumberBox.Step, NumberBox.OnChange` |
| `Slider` | `Slider.new()` | `Slider.Value, Slider.Least, Slider.Most, Slider.Step, Slider.OnChange` |
| `GroupBox` | `GroupBox.new()` | `GroupBox.Text, GroupBox.Content` |
| `ScrollView` | `ScrollView.new()` | `ScrollView.Content` |

TextArea guarda no máximo 4.096 caracteres. Uma lista, uma tabela, uma árvore ou um seletor de itens guarda no máximo 1.024 linhas. Uma visão em abas guarda 64 páginas, uma tabela 8 colunas, e uma barra de status 4 seções.

## Se algo der errado

> [!PROBLEM] O programa para e diz que uma lista guarda no máximo 1.024 linhas.
> Limpe a lista, ou mostre menos linhas.

> [!PROBLEM] O programa para e diz que um menu guarda no máximo 64 entradas.
> Use menos entradas, ou divida-as entre dois menus.

> [!PROBLEM] O programa para e diz que uma visão em abas guarda no máximo 64 páginas.
> Mostre menos páginas, ou coloque algumas delas em outra janela.

> [!PROBLEM] O programa para e diz que uma tabela tem no máximo 8 colunas.
> Use 8 colunas ou menos.

> [!PROBLEM] O programa para e diz que uma barra de status mostra no máximo 4 seções.
> Use 4 seções ou menos.

> [!PROBLEM] O programa para e diz que não existe nenhum nó com esse número nesta árvore.
> Use um número que o TreeView.Add devolveu para você.

> [!PROBLEM] O programa para e diz que não existe nenhuma linha e coluna com esses números nesta tabela.
> Confira primeiro os números de linha e coluna com Table.Count.

> [!SEE] jsc:widgets_basic, jsc:windows_and_layout
