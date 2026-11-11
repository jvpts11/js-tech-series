---
section: library_systems
order: 478
---
# IDesktop e Desktop: um desktop próprio

Um desktop próprio é uma classe que implementa IDesktop. Ele declara a aparência dele e traz os próprios programas em Sigma#. O mod move as janelas dele e cuida do foco, então ele é tão fluido quanto os desktops do mod.

Table: O que um desktop declara

| Propriedade | O que diz |
| --- | --- |
| `Name, Version, Author` | O nome de pacote do desktop, a versão e quem o escreveu. |
| `Menu, PanelAt` | A palavra no painel que abre o menu, e onde o painel fica: top ou bottom. |
| `Frames` | O estilo das molduras das janelas: flat, bevel, luna, aero, metro, kde2, gnome1, oxygen, clearlooks, motif ou platinum. |
| `DesktopColour, PanelColour, TextColour, AccentColour, WindowColour, TitleColour` | As cores dele, cada uma escrita #RRGGBB. |
| `Icons, Wallpaper` | A pasta das imagens dos programas dele, e a imagem que ele pendura. |
| `Programs` | Os programas dele, nomeados num só texto com vírgulas. |

Table: Membros de Desktop

| Membro | O que faz |
| --- | --- |
| `Desktop.Start(package)` | Inicia um desktop: um dos do mod, ou um que um jogador compilou. |
| `Desktop.Stop()` | Volta à tela de texto ou ao prompt do sistema. |
| `Desktop.Current` | O desktop que está no ar, ou nada. |

sigma build --desktop faz o pacote do desktop. Um sistema que o inicia o traz junto, e os gerenciadores de pacotes dos sistemas do mod o instalam a partir do Mirror.

## Se algo der errado

> [!PROBLEM] Desktop.Start para o sistema e diz que não existe desktop com esse nome.
> Compile o desktop com sigma build --desktop, e inicie-o pelo nome do pacote.

> [!PROBLEM] Desktop.Start diz que o sistema não controla nenhuma tela gráfica.
> Acrescente graphics aos drivers do kernel.

> [!SEE] jsc:system_os_library
