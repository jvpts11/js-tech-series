---
section: library_systems
order: 476
---
# Boot, Panic, Screen e Input

Com estes você desenha as páginas do seu sistema: a que ele mostra enquanto inicia, a que mostra quando cai, e a própria tela de texto. Só o próprio sistema, o processo 1, desenha as páginas de inicialização e de pane dele.

Table: Membros de Boot

| Membro | O que faz |
| --- | --- |
| `Boot.Logo(path)` | Mostra uma imagem do disco no meio da página de inicialização. |
| `Boot.Line(text)` | Escreve a linha embaixo da barra de progresso. |
| `Boot.Progress(percent)` | Enche a barra de progresso, de 0 a 100 por cento (%). |
| `Boot.Done()` | Diz que o sistema está no ar, o que encerra a página de inicialização. |

Table: Membros de Panic

| Membro | O que faz |
| --- | --- |
| `Panic.Halt(message)` | Derruba a máquina na página de pane, com essa mensagem no topo. |
| `Panic.Page(kind)` | A aparência da página de pane: blue, red, console ou own. |
| `Panic.Colours(background, text)` | As cores da sua página de pane própria, cada uma escrita #RRGGBB. |
| `Panic.RebootAfter(seconds)` | Reinicia a máquina esse número de segundos depois que ela cai. Sem isso, a página fica até você reiniciar a máquina. |

Table: Membros de Screen

| Membro | O que faz |
| --- | --- |
| `Screen.Write(text), Screen.WriteLine(text)` | Escreve na tela de texto do sistema, o segundo terminando a linha. |
| `Screen.Clear()` | Limpa a tela. |
| `Screen.Cursor(column, row)` | Move o cursor. |
| `Screen.Colours(text, background)` | As cores do que for escrito em seguida: uma das 16 cores com nome, ou #RRGGBB. |
| `Screen.Columns, Screen.Rows` | O tamanho da tela: 80 colunas por 25 linhas. |

Table: Membros de Input

| Membro | O que faz |
| --- | --- |
| `Input.Focus(process)` | Entrega o que for digitado a esse programa, uma linha por vez. Devolva com Processes.Current. |
| `InputEvent.Kind` | key, char, move ou click. |
| `InputEvent.Key, InputEvent.Char` | A tecla pressionada, e a letra digitada. |
| `InputEvent.X, InputEvent.Y, InputEvent.Button` | Onde o mouse está, em colunas e linhas, e o botão clicado. |

A tela de texto aparece quando o shell do sistema é o próprio e ele não inicia nenhum desktop. Um sistema sem driver de texto não tem tela, então o que ele escreve não vai para lugar nenhum.

## Se algo der errado

> [!PROBLEM] O programa para e diz que Boot.Line só responde ao próprio sistema.
> Chame Boot, Panic e Desktop.Start pela própria classe do sistema, não por um serviço.

> [!SEE] jsc:system_os_library, jsc:system_processes_library
