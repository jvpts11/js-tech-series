---
section: the_editors
order: 274
---
# Editores no prompt

Um arquivo guarda texto: uma nota, uma configuração ou um programa. Para alterar o texto você precisa de um editor. No prompt, o editor ocupa a tela inteira enquanto você trabalha, e devolve a tela quando você sai. O prompt é o lugar onde você digita comandos ([](jsc:shell_prompt)).

Quatro editores ocupam a tela: [vim](jsc:vim), [emacs](jsc:emacs), [vi](jsc:vi) e [ee](jsc:ee). Cada um tem as suas próprias teclas, e a entrada dele as lista.

As teclas de seta movem o cursor.

> [!DEFINE] Cursor
> O cursor no texto: o lugar onde a próxima letra vai.

Você inicia um editor digitando o nome dele, um espaço e o nome do arquivo. O arquivo não precisa existir. O editor abre um arquivo vazio com esse nome, e o arquivo é criado quando você salva pela primeira vez.

1. Digite o nome do editor, um espaço e o nome do arquivo, depois pressione [[Enter]].
2. Escreva ou altere o texto, com as teclas desse editor.
3. Salve o arquivo e depois saia do editor com as teclas dele.

Table: Editores e os sistemas em que rodam

| Editor | Sistemas em que roda | Vem com o sistema |
| --- | --- | --- |
| `vim` | MC-DOS, Frames, Linux, FreeBSD e UNIX System V | Não, você o instala |
| `emacs` | MC-DOS, Frames, Linux, FreeBSD e UNIX System V | Não, você o instala |
| `vi` | FreeBSD e UNIX System V | Sim |
| `ee` | FreeBSD | Sim |

Para instalar o vim ou o emacs, veja [](jsc:programs). Até que um editor esteja instalado, o prompt não conhece o nome dele, então instale-o antes de digitar o nome.

```table
{"caption": "Espaço livre em disco necessário para instalar um editor", "heads": ["Editor", "Espaço livre em disco necessário"], "rows": [[{"literal": "vim"}, {"amount": 8, "unit": "MB"}], [{"literal": "emacs"}, {"amount": 24, "unit": "MB"}]]}
```

## Se algo der errado

> [!PROBLEM] O prompt diz "comando não encontrado: vim" e "digite 'help' para listar os comandos".
> O editor não está instalado neste computador. Instale-o ([](jsc:programs)) e depois digite o nome dele de novo.

> [!PROBLEM] No FreeBSD, o mesmo problema aparece como "vim: não encontrado. Tente: apropos <palavra>, ou man intro".
> A causa e a solução são as mesmas.

> [!PROBLEM] Você digita ee no Linux ou no MC-DOS, e o prompt diz que o comando não foi encontrado.
> O ee vem apenas com o FreeBSD ([](jsc:the_systems)). Em outros sistemas, use outro editor.

> [!PROBLEM] Você digita apenas o nome de um editor, e o prompt diz "uso: vim <arquivo>".
> Digite o nome de um arquivo depois do nome do editor, com um espaço entre eles.

> [!SEE] jsc:programs, jsc:the_systems
