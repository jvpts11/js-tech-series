---
section: examples_interface
order: 513
---
# Janelas

Uma janela é uma caixa na área de trabalho, com um título. Este exemplo cria uma janela, coloca nela uma linha e uma coluna de widgets, e a mostra. Um widget é uma peça de uma janela, como um rótulo, um botão ou uma caixa para digitar. Você cria uma janela com um título, uma largura e uma altura.

O próprio sistema da máquina desenha os widgets, então o mesmo programa tem a aparência do sistema em que roda. O exemplo fecha a sua janela perto do fim do Main e, em seguida, mostra uma caixa de mensagem.

## O programa

```sgs Ui.sgs
using System.*;
using System.IO.*;
using System.UI.*;
namespace Corpus;
class UiCorpus {
    static void Main() {
        Window window = new Window("Corpus", 240, 150);
        window.Title = "Corpus";
        Row row = new Row();
        Column column = new Column();
        row.Spacing = 2;
        column.Spacing = 2;

        Label label = new Label();
        Label named = new Label("name");
        named.Text = "name";
        named.Visible = true;
        named.Enabled = true;
        named.Width = 40;
        named.Height = 10;

        Button button = new Button();
        Button pressed = new Button("press");
        pressed.Text = "press";
        pressed.OnClick += Clicked;

        TextBox box = new TextBox();
        TextBox typed = new TextBox("x");
        typed.Text = "y";
        typed.OnChange += Clicked;
        typed.OnSubmit += Clicked;

        CheckBox check = new CheckBox();
        CheckBox labelled = new CheckBox("on");
        CheckBox ticked = new CheckBox("on", true);
        ticked.Text = "on";
        ticked.Checked = false;
        ticked.OnToggle += Clicked;

        ProgressBar bar = new ProgressBar();
        ProgressBar ranged = new ProgressBar(0, 10);
        ranged.Value = 5;
        ranged.Least = 0;
        ranged.Most = 10;

        ListBox listBox = new ListBox();
        listBox.Add("one");
        listBox.Add("two", "second");
        listBox.Selected = 0;
        listBox.OnSelect += Clicked;

        Canvas canvas = new Canvas();
        Canvas sized = new Canvas(100, 50);
        sized.Clear(0);
        sized.FillRect(0, 0, 10, 10, 1);
        sized.DrawLine(0, 0, 10, 10, 2);
        sized.DrawText("hi", 1, 1, 3);
        sized.SetPixel(2, 2, 4);
        sized.OnClick += Clicked;

        row.Add(label);
        row.Add(named, 1);
        column.Add(row);
        column.Add(sized, 1);
        column.Add(box);
        column.Add(check);
        column.Add(labelled);
        column.Add(bar);
        column.Add(canvas);
        window.Content = column;
        window.Add(button, 0, 0, 20, 10);
        window.OnClose += Clicked;
        window.Show();
        Console.PrintLine("" + window.Open + listBox.Count + sized.ClickX + sized.ClickY);
        row.Clear();
        column.Clear();
        listBox.Clear();
        window.Close();
        MessageBox.Show("title", "text");
    }

    static void Clicked() {
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `new Window("Corpus", 240, 150)` | Cria uma janela com o título Corpus, largura 240 e altura 150. |
| `new Row(), new Column(), Spacing` | Uma linha alinha os seus widgets na horizontal, e uma coluna os alinha na vertical. Spacing define o espaço entre eles. |
| `new Label("name"), Text, Width, Height` | Um rótulo mostra um texto. O seu tamanho é definido por Width e Height. |
| `new Button("press"), OnClick` | Um botão com um texto. OnClick indica o método chamado quando o jogador clica nele. |
| `new TextBox("x"), OnChange, OnSubmit` | Uma caixa para digitar. OnChange é executado quando o seu texto muda, e OnSubmit quando o jogador o envia. |
| `new CheckBox("on", true), Checked` | Uma caixa de seleção com um texto. Checked indica se ela está marcada. |
| `new ProgressBar(0, 10), Value, Least, Most` | Uma barra que mostra um valor entre um mínimo e um máximo. |
| `ListBox: Add, Selected, OnSelect` | Uma lista de linhas. Selected é a linha escolhida, e OnSelect é executado quando uma delas é selecionada. |
| `new Canvas(100, 50), Clear, FillRect, DrawLine` | Uma área em branco para desenhar, com 100 de largura e 50 de altura. Cada chamada de desenho pinta uma forma ou um texto em uma cor de 0 a 15. |
| `DrawText, SetPixel, ClickX, ClickY` | Desenha um texto ou um ponto. ClickX e ClickY indicam onde o jogador clicou por último. |
| `row.Add(named, 1)` | Adiciona o rótulo com peso 1, que recebe uma parte do espaço que sobra. |
| `window.Content = column` | Torna a coluna o conteúdo principal da janela. |
| `window.Add(button, 0, 0, 20, 10)` | Coloca o botão na posição 0, 0, com largura 20 e altura 10. |
| `window.OnClose` | Indica o nome do método chamado quando a janela é fechada. |
| `window.Show()` | Abre a janela na área de trabalho e na sua barra de tarefas. A janela continua aberta depois do tick que a abriu. |
| `window.Open, window.Close()` | Open indica se a janela está aberta. Close a fecha. |
| `MessageBox.Show("title", "text")` | Mostra uma pequena caixa com um título e uma linha de texto. |

> [!DEFINE] Janela
> Uma caixa na área de trabalho, com um título e widgets dentro dela.

> [!DEFINE] Linha
> Uma caixa que alinha os seus widgets na horizontal, um após o outro.

> [!DEFINE] Coluna
> Uma caixa que alinha os seus widgets na vertical, um abaixo do outro.

> [!DEFINE] Peso
> Um número que dá a um widget uma parte do espaço que sobra. Só os widgets com peso dividem esse espaço.

> [!DEFINE] Área de desenho
> Uma área em branco onde o programa desenha, com formas, linhas e texto.

> [!DEFINE] Manipulador
> Um método que o programa nomeia e que é chamado quando algo acontece.

## Se algo der errado

> [!PROBLEM] Uma janela contém mais de 256 widgets, contando os que estão dentro de linhas e colunas, e o programa para com a mensagem "uma janela contém no máximo 256 widgets, contando os que estão dentro de linhas e colunas".
> Coloque menos widgets na janela. Widgets dentro de linhas e colunas também contam, então remova alguns deles também.

> [!PROBLEM] Uma linha ou uma coluna contém mais de 256 widgets, e o programa para com a mensagem "uma linha ou uma coluna contém no máximo 256 widgets".
> Divida os widgets entre duas linhas ou duas colunas.

> [!PROBLEM] Um widget é colocado dentro de si mesmo, e o programa para com a mensagem "um widget não pode conter a si mesmo ou um widget dentro do qual está".
> Coloque cada widget em um único lugar, e nunca dentro de si mesmo.

> [!PROBLEM] Uma área de desenho recebe mais de 4.096 traços, e o programa para com a mensagem "um canvas contém no máximo 4096 traços".
> Desenhe menos formas, ou use uma segunda área de desenho para o resto.

> [!SEE] jsc:sigma_example_widgets, jsc:sigma_library
