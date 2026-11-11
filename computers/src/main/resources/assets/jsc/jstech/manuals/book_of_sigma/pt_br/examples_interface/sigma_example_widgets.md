---
section: examples_interface
order: 514
---
# Widgets

Este exemplo mostra o resto dos widgets: áreas de texto e caixas de números, escolhas, abas, tabelas, árvores, menus, barras de status, imagens, gráficos, logs, caixas de diálogo de arquivos, slots de itens, operações e componentes que um mod acrescenta.

Alguns widgets desenham imagens, gráficos, itens ou componentes. Uma máquina só com o seu terminal não consegue desenhá-los, então criar um ali para o programa, e a mensagem informa isso.

O manipulador é um método que você escreve e nomeia no widget.

Widgets têm eventos como OnChange, OnSelect e OnClick. Cada um informa o método chamado quando o jogador age.

## O programa

```sgs Widgets.sgs
using System.*;
using System.IO.*;
using System.UI.*;
using System.Operations.*;
namespace Corpus;
class WidgetsCorpus {
    static void Main() {
        Window window = new Window("Widgets", 320, 200);
        Column column = new Column();

        TextArea area = new TextArea();
        TextArea notes = new TextArea("notes");
        notes.Text = "more";
        notes.OnChange += Heard;

        NumberBox numbers = new NumberBox();
        NumberBox copies = new NumberBox(1, 9);
        copies.Value = 3;
        copies.Least = 1;
        copies.Most = 9;
        copies.Step = 1;
        copies.OnChange += Heard;

        Slider slider = new Slider();
        Slider speed = new Slider(0, 100);
        speed.Value = 50;
        speed.Least = 0;
        speed.Most = 100;
        speed.Step = 5;
        speed.OnChange += Heard;

        RadioGroup radios = new RadioGroup();
        radios.Add("Fast");
        radios.Add("Slow");
        radios.Selected = 1;
        radios.Across = true;
        radios.OnSelect += Heard;

        ComboBox combo = new ComboBox();
        combo.Add("Legacy");
        combo.Selected = 1;
        combo.OnSelect += Heard;

        GroupBox group = new GroupBox();
        GroupBox backup = new GroupBox("Backup");
        backup.Text = "Backup";
        backup.Content = notes;
        TabView tabs = new TabView();
        tabs.Add("Controls", backup);
        tabs.Selected = 1;
        tabs.OnSelect += Heard;

        ScrollView scroll = new ScrollView();
        scroll.Content = area;

        Table table = new Table();
        table.AddColumn("Name");
        List<string> cells = new List<string>();
        cells.Add("a");
        table.AddRow(cells);
        table.AddRow("a");
        table.AddRow("a", "b");
        table.AddRow("a", "b", "c");
        table.AddRow("a", "b", "c", "d");
        table.Selected = 1;
        table.OnSelect += Heard;
        string cell = table.Cell(1, 1);

        TreeView tree = new TreeView();
        int top = tree.Add("This PC");
        int under = tree.Add("C:", top);
        string node = tree.NodeText(under);
        tree.Selected = under;
        tree.OnSelect += Heard;

        MenuBar bar = new MenuBar();
        bar.Add("File", "Open");
        bar.OnPick += Heard;
        ContextMenu menu = new ContextMenu(table);
        menu.Add("Rename");
        menu.OnPick += Heard;

        StatusBar status = new StatusBar();
        StatusBar ready = new StatusBar("Ready");
        ready.Text = "Ready";
        ready.Add("12:30");

        Image image = new Image();
        Image photo = new Image("/photo.pix");
        photo.Path = "/photo.pix";

        Chart chart = new Chart();
        Chart cpu = new Chart(0, 100);
        cpu.Text = "CPU";
        cpu.Least = 0;
        cpu.Most = 100;
        cpu.Add(61.0);

        LogView log = new LogView();
        log.Add("10:14 INFO started");

        OpenFileDialog open = new OpenFileDialog();
        OpenFileDialog opener = new OpenFileDialog("Open");
        opener.Title = "Open";
        opener.Path = "/";
        opener.Filter = "*.txt";
        opener.OnChoose += Heard;
        opener.OnCancel += Heard;
        SaveFileDialog save = new SaveFileDialog();
        SaveFileDialog saver = new SaveFileDialog("Save");
        saver.Title = "Save";
        saver.Path = "/";
        saver.Filter = "*.txt";
        saver.Name = "a.txt";
        saver.OnChoose += Heard;
        saver.OnCancel += Heard;

        ItemSlot slot = new ItemSlot();
        ItemSlot diamond = new ItemSlot("minecraft:diamond");
        ItemSlot stack = new ItemSlot("minecraft:diamond", 64);
        stack.Item = "minecraft:diamond";
        stack.Amount = 64;
        stack.OnClick += Heard;

        ItemPicker picker = new ItemPicker();
        picker.Add("minecraft:diamond");
        picker.Selected = 1;
        picker.OnSelect += Heard;

        OperationView view = new OperationView();
        view.Text = "SELECT 64 diamond";
        view.State = "PROCESSING";
        view.Done = 3;
        view.Total = 5;
        view.Computer = "Mainframe";
        view.Show(Operations.Get("op"));

        GenericComponent dial = new GenericComponent("corpus:dial");
        dial.Data = 5;
        dial.OnAction(Acted);

        column.Add(bar);
        column.Add(tabs, 1);
        column.Add(scroll);
        column.Add(copies);
        column.Add(speed);
        column.Add(radios);
        column.Add(combo);
        column.Add(table);
        column.Add(tree);
        column.Add(photo);
        column.Add(cpu);
        column.Add(log);
        column.Add(stack);
        column.Add(picker);
        column.Add(view);
        column.Add(dial);
        column.Add(ready);
        window.Content = column;
        window.Show();
        opener.Show(window);
        saver.Show(window);
        Console.PrintLine(cell + node + radios.Count + combo.Count + tabs.Count + table.Count + tree.Count
            + cpu.Count + log.Count + picker.Count + picker.Picked + bar.Picked + bar.PickedMenu + menu.Picked
            + dial.Kind + dial.Data + view.Done + numbers.Value + slider.Value + group.Text + status.Text
            + image.Path + chart.Count + open.Path + save.Name + slot.Amount);
        radios.Clear();
        combo.Clear();
        tabs.Clear();
        table.Clear();
        tree.Clear();
        bar.Clear();
        menu.Clear();
        ready.Clear();
        cpu.Clear();
        log.Clear();
        picker.Clear();
    }

    static void Heard() {
    }

    static void Acted(ComponentAction action) {
        Console.PrintLine(action.Name + action.Value);
    }
}
```

## Os widgets

Table: Widgets e o que fazem

| Widget | O que faz |
| --- | --- |
| `TextArea` | Uma caixa para várias linhas de texto. OnChange roda quando o seu texto muda. |
| `NumberBox, Slider` | Um número inteiro entre um mínimo e um máximo, movido um passo por vez. |
| `RadioGroup` | Um conjunto de escolhas, uma das quais é selecionada. Selected é a escolha selecionada, contada a partir de 1, e 0 para nenhuma. |
| `ComboBox` | Uma caixa com uma lista de opções. Selected é a opção escolhida. |
| `GroupBox` | Uma caixa com uma legenda, e um widget dentro dela. |
| `TabView` | Abas, cada uma com um título e um widget. A primeira adicionada aparece primeiro. |
| `ScrollView` | Mostra um widget que é mais alto que o seu espaço, e o jogador rola. |
| `Table` | Linhas e colunas de texto. Cell dá o texto de uma célula. Uma tabela tem no máximo 8 colunas. |
| `TreeView` | Uma árvore de texto. Add devolve o número do novo nó, e NodeText dá o seu texto. |
| `MenuBar` | Uma barra de menus com entradas. Picked e PickedMenu informam o que o jogador escolheu. |
| `ContextMenu` | Um menu que abre sobre o seu widget quando o jogador clica nele com o outro botão. |
| `StatusBar` | Uma linha de texto, com algumas seções à sua direita. |
| `Image` | Uma imagem lida do disco da máquina, como o programa de pintura a salva. |
| `Chart` | Uma linha desenhada pelos números acrescentados a ela, entre um mínimo e um máximo. |
| `LogView` | Linhas uma sob a outra, a mais nova embaixo. Uma linha com WARN ou ERROR é destacada. |
| `OpenFileDialog, SaveFileDialog` | Uma caixa de diálogo na qual o jogador escolhe um arquivo. Show a abre em uma janela. |
| `ItemSlot` | Um slot que guarda um item, mostrado pelo seu id de item e pela quantidade. |
| `ItemPicker` | Uma grade de itens com uma linha de busca acima. O jogador escolhe um. |
| `OperationView` | Mostra uma Operação: o seu texto, o seu estado, quanto já foi feito e o seu computador. |
| `GenericComponent` | Um widget que um mod acrescenta, identificado por um id como corpus:dial. OnAction roda quando ele age. |

> [!DEFINE] Evento
> Algo que acontece, como um clique ou uma mudança, a que um método pode responder.

> [!DEFINE] Componente genérico
> Um widget que um mod acrescenta, identificado pelo seu próprio id, como mod:name.

> [!DEFINE] ID do item
> O nome de um item do jogo, como minecraft:diamond.

## Se algo der errado

> [!PROBLEM] Uma imagem, um gráfico, um slot de item ou um componente é criado em uma máquina só com o seu terminal, e o programa para.
> Rode o programa em uma máquina que desenha as suas janelas, não só o seu terminal.

> [!PROBLEM] Uma janela tem mais de 256 widgets, e o programa para com "uma janela contém no máximo 256 widgets, contando os que estão dentro de linhas e colunas".
> Coloque menos widgets na janela, e conte os que estão dentro de linhas e colunas.

> [!PROBLEM] Uma tabela recebe mais de 8 colunas, e o programa para com "uma tabela tem no máximo 8 colunas".
> Use no máximo 8 colunas em uma tabela.

> [!PROBLEM] Um menu recebe mais de 64 entradas, e o programa para com "um menu contém no máximo 64 entradas".
> Mantenha cada menu com 64 entradas ou menos.

> [!PROBLEM] Uma lista recebe mais de 1024 linhas, e o programa para com "uma list contém no máximo 1024 linhas".
> Mantenha cada lista com 1024 linhas ou menos.

> [!SEE] jsc:sigma_example_windows, jsc:operations, jsc:soundfoundry, jsc:sigma_library
