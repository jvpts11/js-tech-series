---
section: help_at_the_prompt
order: 279
icon: jsc:guide_to_operations
---
# Onde procurar

Esqueceu o nome de um comando, ou a opção de que precisa? Você pode perguntar ao computador no prompt, sem sair do terminal. Cinco ferramentas respondem à pergunta do que um comando faz, e uma ferramenta lista todos os comandos que o computador tem.

Cada ferramenta funciona em alguns sistemas e não em outros. As mesmas páginas aparecem em todos os livros que contêm estes capítulos, então cada página diz a quais sistemas se aplica.

Table: Onde cada ferramenta funciona

| Ferramenta | Onde funciona | O que oferece |
| --- | --- | --- |
| `help` | MC-DOS, MC-NET, Frames, Linux, UNIX System V | Uma lista de comandos, ou a página de um comando; em tela cheia no MC-DOS e no MC-NET. |
| `man` | Linux, UNIX System V, FreeBSD | A página de manual de um comando, e as entradas dos manuais. |
| `whatis` | Linux, UNIX System V, FreeBSD | Uma linha que diz para que serve um comando. |
| `apropos` | Linux, UNIX System V, FreeBSD | Os comandos cujo nome ou resumo contém uma palavra que você informa. |
| `info` | Linux | Os manuais como telas interligadas, lidas com teclas. |
| `listcmd` | Todos os sistemas, quando o servidor permitir | Todos os comandos e programas deste computador. |

Table: Qual ferramenta começar

| Você quer | Comece com |
| --- | --- |
| Como um comando é usado, quando você conhece o nome dele | help <comando> onde help existe; man <comando> nos sistemas UNIX |
| Para que serve um comando, em uma linha | `whatis <command>` |
| Um comando, encontrado pelo que ele faz | `apropos <word>` |
| Todos os manuais e comandos, lidos como telas interligadas | `info` |
| Todos os comandos e programas que este computador tem | `listcmd` |

A maioria dos sistemas responde a uma palavra desconhecida com "comando não encontrado:" seguido da palavra, e depois uma dica para digitar help. O UNIX System V responde apenas com a palavra e "não encontrado", e o FreeBSD, que não tem comando help, aponta para apropos e man intro.

> [!DEFINE] comando
> Uma palavra digitada no prompt que faz o sistema executar uma tarefa, como ls, help ou man.

> [!DEFINE] página de manual
> A página escrita e curta que explica um comando: seu nome, sua forma, o que ele faz e suas opções.

## Se algo der errado

> [!PROBLEM] A resposta é "comando não encontrado: info", ou, no FreeBSD, "info: não encontrado. Tente: apropos <palavra>, ou man intro".
> info existe apenas no Linux. No UNIX System V e no FreeBSD, use man <comando>; no MC-DOS, MC-NET e Frames, use help <comando>.

> [!PROBLEM] A resposta é "comando não encontrado: listcmd".
> O servidor está com a opção desligada. Leia a página de listcmd ([](jsc:listcmd)) para ver como ela é ligada.

> [!SEE] jsc:help_on_a_computer, jsc:shell
