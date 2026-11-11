---
section: help_at_the_prompt
order: 280
icon: jsc:guide_to_operations
---
# O comando help

help é o comando que diz o que os outros comandos fazem. Digite help sozinho, e ele lista os comandos que este computador tem. Digite help e o nome de um comando, e ele imprime a página desse comando: sua sintaxe, o que ele faz e suas opções.

No MC-DOS e no MC-NET, o help ocupa a tela inteira, e você se move pela página com as teclas abaixo. No Frames, no Linux e no UNIX System V ele imprime a página como texto. As janelas de Ajuda da área de trabalho são outra coisa ([](jsc:help_on_a_computer)).

## Usando a ajuda

1. Digite help e pressione [[Enter]] para listar os comandos que este computador tem.
2. Digite help, um espaço e um nome, como help ls no Linux, para ler a página desse comando.
3. No MC-DOS ou no MC-NET, pressione [[Esc]] ao terminar, para voltar ao prompt.

No MC-DOS e no MC-NET, a linha de cima da tela cheia mostra, por exemplo, "MC-DOS Ajuda: dir". As teclas são as abaixo.

Table: Teclas da ajuda de tela cheia

| Tecla | O que faz |
| --- | --- |
| `Tab` | Escolhe o próximo botão ou link. |
| `Shift+Tab` | Escolhe o botão ou link anterior. |
| `Enter` | Segue o link escolhido, ou escolhe um item de menu. |
| `Up, Down` | Rola uma linha. |
| `Page Up, Page Down` | Avança uma página. |
| `Home, End` | Vai para o início ou o fim do tópico. |
| `F10 or Alt+F` | Abre o menu Arquivo. |
| `Alt+S` | Abre o menu Busca. |
| `F3` | Repete a última busca. |
| `Alt+C, Alt+N, Alt+B, Alt+I` | Mostra o sumário, o próximo tópico, o tópico anterior e o índice. |
| `Escape` | Fecha um menu ou busca aberta, ou sai da ajuda quando nenhum estiver aberto. |
| `Alt+F4` | Sai da ajuda. |

Para buscar, pressione [[Alt]]+[[S]], depois pressione [[Enter]] no primeiro item do menu Busca, o que se chama Buscar com três pontos depois dele. A tela pergunta "Buscar o quê:". Digite a palavra e pressione [[Enter]].

O segundo item, "Repetir Última Busca", busca de novo. Quando a palavra não está no texto, a tela diz "Nada encontrado."

```command
id: jsc:command/linux/help
```

> [!DEFINE] tópico
> Um tópico em que a ajuda de tela cheia se abre, como o nome de um comando ou um título no seu sumário.

> [!DEFINE] link
> Uma palavra no texto de ajuda que você escolhe com Tab e segue com Enter.

## Se algo der errado

> [!PROBLEM] A resposta é "comando inexistente:" e o nome que você digitou.
> O nome não é um comando deste computador. Digite help sozinho para listar os comandos que ele tem.

> [!PROBLEM] A resposta é "help: não encontrado. Tente: apropos <palavra>, ou man intro".
> O FreeBSD não tem o comando help. Digite apropos e uma palavra, ou man intro para a introdução.

> [!PROBLEM] A tela cheia não fecha.
> Pressione Esc uma vez para cada menu ou busca aberta, e mais uma vez para sair da ajuda.

> [!PROBLEM] A resposta é "Nada encontrado."
> Confira a grafia da palavra, ou tente uma palavra mais curta.

> [!SEE] jsc:man_pages, jsc:help_on_a_computer
