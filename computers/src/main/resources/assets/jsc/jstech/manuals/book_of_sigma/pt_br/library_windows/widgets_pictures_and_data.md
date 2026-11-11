---
section: library_windows
order: 473
---
# Área de desenho, imagens, gráficos, logs, itens e componentes

Alguns widgets mostram imagens, números e registros. Uma Canvas é uma área onde você desenha, uma Image mostra uma imagem do disco, e um Chart traça números. Cada argumento de cor é um número inteiro.

> [!DEFINE] Traço
> Um comando de desenho em uma área de desenho.

> [!DEFINE] Componente
> Uma parte que um mod adiciona a uma janela, com um nome de tipo. Ela guarda valores, não manipuladores.

Table: Imagens, gráficos e logs

| Widget | Cria | Membros |
| --- | --- | --- |
| `Canvas` | `Canvas.new(), Canvas.new(width, height)` | `Canvas.Clear(colour), Canvas.FillRect, Canvas.DrawLine, Canvas.DrawText, Canvas.SetPixel, Canvas.ClickX, Canvas.ClickY, Canvas.OnClick` |
| `Image` | `Image.new(), Image.new(path)` | `Image.Path` |
| `Chart` | `Chart.new(), Chart.new(least, most)` | `Chart.Text, Chart.Least, Chart.Most, Chart.Add(number), Chart.Clear, Chart.Count` |
| `LogView` | `LogView.new()` | `LogView.Add(text), LogView.Clear, LogView.Count` |

Um gráfico guarda 120 pontos, e quando está cheio o ponto mais antigo sai. Uma visão de log guarda 256 linhas, e a linha mais antiga sai primeiro. Image, Chart, LogView e os widgets abaixo são da versão 2.

Table: Itens e componentes

| Widget | Cria | Membros |
| --- | --- | --- |
| `ItemSlot` | `ItemSlot.new(), ItemSlot.new(item, amount)` | `ItemSlot.Item, ItemSlot.Amount, ItemSlot.OnClick` |
| `ItemPicker` | `ItemPicker.new()` | `ItemPicker.Add(item), ItemPicker.Clear, ItemPicker.Count, ItemPicker.Selected, ItemPicker.Picked, ItemPicker.OnSelect` |
| `OperationView` | `OperationView.new()` | `OperationView.Show(OperationInfo), OperationView.Text, OperationView.State, OperationView.Done, OperationView.Total, OperationView.Computer` |
| `GenericComponent` | `GenericComponent.new(kind)` | `GenericComponent.Kind, GenericComponent.Data, GenericComponent.OnAction` |

Um ItemSlot mostra um item do jeito que o jogo o mostra. Um ItemPicker mostra uma grade de itens, com uma linha de busca acima. Um OperationView mostra uma Operação como ela está, com o texto, o estado, as quantidades feitas e totais, e o computador.

Um GenericComponent é um tipo que um mod adiciona. O Data dele é uma cópia do valor que você passa, e OnAction entrega um ComponentAction, com o ComponentAction.Name e o ComponentAction.Value. O valor de um componente pesa no máximo 32 quilobytes (KB), aninha no máximo 16 níveis de profundidade, e tem no máximo 4.096 partes.

## Se algo der errado

> [!PROBLEM] A área de desenho para de aceitar traços.
> Uma área de desenho guarda no máximo 4.096 traços. Limpe-a, e desenhe menos traços.

> [!PROBLEM] O programa para e diz que um componente guarda valores, não manipuladores.
> Dê um valor ao componente, e escute as ações dele com OnAction.

> [!PROBLEM] O programa para e diz que o valor do componente pesa demais.
> Mantenha o valor abaixo de 32 quilobytes (KB), ou divida-o em vários componentes.

> [!PROBLEM] O programa para e diz que um valor que contém a si mesmo não pode ser entregue a um componente.
> Construa um valor que não contenha a si mesmo.

> [!SEE] jsc:monitors, jsc:operations_catalogue
