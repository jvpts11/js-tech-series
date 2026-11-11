---
section: library_windows
order: 474
---
# Caixas de diálogo: escolhendo um arquivo para abrir ou salvar

Uma caixa de diálogo é uma pequena janela que pergunta algo ao jogador. Ela abre sobre outra janela, a sua janela de origem, e fecha quando o jogador responde.

Seu programa cria a caixa de diálogo, a mostra sobre uma janela que está aberta, e lê o caminho que o jogador escolheu.

> [!DEFINE] Diálogo
> Uma pequena janela que pergunta algo ao jogador e abre sobre outra janela.

> [!DEFINE] Janela de origem
> A janela sobre a qual uma caixa de diálogo fica.

Table: Membros da caixa de diálogo

| Membro | O que faz |
| --- | --- |
| `OpenFileDialog.new(), OpenFileDialog.new(title)` | Cria uma caixa de diálogo que pede ao jogador para abrir um arquivo. Versão 2. |
| `SaveFileDialog.new(), SaveFileDialog.new(title)` | Cria uma caixa de diálogo que pergunta ao jogador onde salvar um arquivo. Versão 2. |
| `OpenFileDialog.Title, SaveFileDialog.Title` | O título da caixa de diálogo. |
| `OpenFileDialog.Path, SaveFileDialog.Path` | O caminho que o jogador escolheu. |
| `OpenFileDialog.Filter, SaveFileDialog.Filter` | O texto de filtro da caixa de diálogo. |
| `OpenFileDialog.Show(owner), SaveFileDialog.Show(owner)` | Mostra a caixa de diálogo sobre a janela de origem. |
| `SaveFileDialog.Name` | O nome de arquivo que a caixa de diálogo sugere. |
| `OpenFileDialog.OnChoose, SaveFileDialog.OnChoose` | O evento que roda quando o jogador escolhe um arquivo. |
| `OpenFileDialog.OnCancel, SaveFileDialog.OnCancel` | O evento que roda quando o jogador cancela. |
| `MessageBox.Show(title, text)` | Uma janela com um título e uma linha de texto. Somente Sigma#. |

Uma janela mantém um diálogo por vez.

## Se algo der errado

> [!PROBLEM] O programa para e diz "um diálogo abre sobre uma janela que está aberta", porque a Janela de origem não está aberta.
> Mostre primeiro a janela de origem, depois abra a caixa de diálogo sobre ela.

> [!PROBLEM] O programa para e diz que esta janela já tem uma caixa de diálogo aberta sobre ela.
> Feche primeiro a caixa de diálogo aberta, depois abra a próxima.

> [!SEE] jsc:monitors, jsc:windows_and_layout
