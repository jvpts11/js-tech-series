---
section: getting_help
order: 93
icon: jsc:guide_to_operations
---
# Ajuda em um computador

Toda área de trabalho tem um programa de Ajuda, com um nome próprio. Ele vem com a área de trabalho, então você não precisa instalá-lo. Ele lê os manuais da série e, ao lado deles, as páginas de manual dos próprios comandos da máquina. Este manual não está só nas suas mãos: todo computador também pode lê-lo.

Table: Ajuda de cada área de trabalho

| Área de trabalho | O nome da Ajuda é |
| --- | --- |
| Frames 95 | Tópicos |
| Frames XP e 7 | Ajuda e Suporte |
| Frames 10 e 11 | Obter Ajuda |
| KDE | Central de Ajuda |
| GNOME e Cinnamon | Ajuda |
| CDE | Visualizador de Ajuda |

No prompt, vários comandos oferecem ajuda. Cada um funciona em um conjunto diferente de sistemas.

O comando help lista os comandos, ou mostra como um deles é usado. Digite help sozinho para listá-los, ou help e o nome de um comando para ver como ele é usado. Funciona em todos os sistemas, exceto no FreeBSD. No MC-DOS e no MC-NET ele ocupa a tela inteira. Nos outros sistemas ele imprime seu texto ([](jsc:help_command)).

O comando man abre a página de manual de um comando, no Linux, no UNIX System V e no FreeBSD. Um tópico como graphics-cards abre como uma página da seção 7 ([](jsc:man_pages)). Os comandos whatis e apropos buscam no manual ([](jsc:whatis_and_apropos)).

No Linux, o comando info lê os manuais como uma árvore de nós. Suas teclas são estas:

Table: Teclas do info

| Tecla | O que faz |
| --- | --- |
| `n` | Vai para o próximo nó. |
| `p` | Vai para o nó anterior. |
| `u` | Sobe para o nó acima. |
| `l` | Volta para o nó em que você estava. |
| `Tab` | Escolhe um link. |
| `Enter` | Segue o link escolhido. |
| `m` | Vai para um item de menu, pelo nome. |
| `s` | Busca nos manuais. |
| `h` | Mostra as teclas. |
| `q` | Sai do info. |

Sem nome, o info abre o diretório de todos os manuais.

## Se algo der errado

> [!PROBLEM] O man diz: Nenhuma entrada de manual para o nome.
> Confira o nome. Para a entrada sobre placas de vídeo, digite man graphics-cards, e não man graphics.

> [!PROBLEM] help não funciona no FreeBSD.
> Use man no FreeBSD ([](jsc:man_pages)).

> [!PROBLEM] info não funciona no seu sistema.
> info serve só para o Linux. Use man ([](jsc:man_pages)).

> [!SEE] jsc:library, jsc:finding_help_overview, jsc:help_command, jsc:info_keys
