---
section: library_windows
order: 471
---
# Rótulos, botões, caixas e barras

Um widget é uma parte de uma janela. Os mais simples mostram texto, e os outros recebem o que o jogador digita ou clica. Criar um widget não custa nada. Mudar um valor que o jogador vê custa créditos, veja [](jsc:call_costs).

Table: Widgets básicos

| Widget | Cria | Valores | Eventos |
| --- | --- | --- | --- |
| `Label` | `Label.new(), Label.new(text)` | `Label.Text` | nenhum |
| `Button` | `Button.new(), Button.new(text)` | `Button.Text` | `Button.OnClick` |
| `TextBox` | `TextBox.new(), TextBox.new(text)` | `TextBox.Text` | `TextBox.OnChange, TextBox.OnSubmit` |
| `CheckBox` | `CheckBox.new(), CheckBox.new(text, checked)` | `CheckBox.Text, CheckBox.Checked` | `CheckBox.OnToggle` |
| `ProgressBar` | `ProgressBar.new(), ProgressBar.new(least, most)` | `ProgressBar.Value, ProgressBar.Least, ProgressBar.Most` | nenhum |

Um evento avisa seu programa de que algo aconteceu, e ele vem sem argumento. OnChange roda a cada mudança em uma caixa de texto. OnSubmit roda quando o jogador pressiona Enter na caixa.

Uma caixa de texto guarda no máximo 256 caracteres de texto.

## Se algo der errado

> [!PROBLEM] A caixa de texto para de aceitar caracteres.
> Encurte o texto, ou divida-o entre duas caixas.

> [!PROBLEM] O programa não reage quando o jogador pressiona Enter.
> Escute OnSubmit, que roda quando o jogador pressiona Enter na caixa.

> [!SEE] jsc:windows_and_layout, jsc:widgets_lists
