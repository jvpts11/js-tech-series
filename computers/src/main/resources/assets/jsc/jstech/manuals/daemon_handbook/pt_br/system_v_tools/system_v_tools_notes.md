---
section: system_v_tools
order: 354
---
# vi, lp e man no System V

Três comandos deste grupo são explicados por completo em outros capítulos, porque os sistemas UNIX os compartilham. Esta página diz apenas o que o UNIX System V faz por conta própria.

## vi

O vi vem com o sistema, então está sempre lá, e você nunca o instala. Ele usa o mesmo motor que o [vim](jsc:vim), com as mensagens do System V. A tela não tem cor nem régua no canto. As suas teclas e os seus comandos com dois-pontos estão em [](jsc:vi).

Table: Mensagens quando um arquivo é aberto ou gravado

| Mensagem | O que significa |
| --- | --- |
| `"<name>" [New file]` | O arquivo ainda não existe. Gravá-lo cria o arquivo. |
| `"<name>" <lines> lines, <characters> characters` | O arquivo existe. Os números são o tamanho dele. |

## lp

lp envia um arquivo para uma impressora ligada ao computador ([](jsc:peripheral_cables)). Suas opções e mensagens estão na página de impressão, [](jsc:printing). A menos que você nomeie uma impressora com -d, lp usa a impressora padrão.

## man

O man mostra a página de manual de um comando. Neste sistema, ele também lê os manuais mais amplos: man graphics-cards abre a entrada do manual sobre placas de vídeo, como uma página da seção 7. O comando está explicado em [](jsc:man_pages), e whatis e apropos estão em [](jsc:whatis_and_apropos).

## Se algo der errado

> [!PROBLEM] vi diz "E37: nenhuma gravação desde a última alteração (adicione ! para forçar)".
> Grave o arquivo antes com :w, ou saia com :q! para descartar as alterações.

> [!PROBLEM] lp diz "lp: nenhum destino padrão".
> Ligue uma impressora ao computador. A primeira impressora ligada se torna a padrão.

> [!PROBLEM] man diz "Não há entrada no manual para intro".
> O tópico de introdução pertence ao FreeBSD. No UNIX System V, digite man e o nome de um comando.

> [!PROBLEM] man diz "Qual página do manual você quer?"
> Informe o nome de um comando depois de man, por exemplo man graphics-cards.

> [!SEE] jsc:vi, jsc:printing, jsc:man_pages
