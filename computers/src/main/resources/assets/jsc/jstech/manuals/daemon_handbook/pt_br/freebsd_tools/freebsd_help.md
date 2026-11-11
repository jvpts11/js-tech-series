---
section: freebsd_tools
order: 341
---
# Como se orientar sem ajuda

O FreeBSD não tem comando help. Digitar help mostra uma linha que aponta para outras duas formas. Você usa essas formas, e a tecla Tab, para encontrar qualquer comando.

> [!DEFINE] Candidato
> Um nome de comando que o Tab poderia completar.

No prompt, digite help e pressione [[Enter]]. O FreeBSD responde com uma linha:

```session
{"lines": [{"text": "help: not found. Try: apropos <word>, or man intro"}], "look": "jsc:guide/terminal/sh", "title": "FreeBSD"}
```

## apropos, man e whatis

Table: Os auxiliares do FreeBSD

| Comando | O que faz |
| --- | --- |
| `apropos <word>` | Procura a palavra no nome e no resumo de cada comando daqui, e em cada tópico. |
| `man <command>` | Abre a página de manual de um comando. Uma entrada de manual, como graphics-cards, abre do mesmo jeito. |
| `man intro` | Abre o único tópico que o FreeBSD tem: a introdução aos comandos. |
| `whatis <command>` | Mostra a linha única com que uma página de manual começa. |

1. Digite apropos e uma palavra para o que você quer fazer, como apropos network.
2. Leia as linhas que ele imprime. Cada linha traz o nome de um comando e o seu resumo em uma linha.
3. Digite man e o nome do comando para ler a página de manual dele.

A introdução é curta, então leia-a primeiro. Ela diz que um comando é um programa pequeno que faz uma só coisa, e que um shell executa aquele que você digita no prompt.

## Tab

Pressione [[Tab]] uma vez para completar um nome de comando, quando só um nome couber. Pressione duas vezes seguidas, sem nada mais para completar, para listar todos os candidatos. Os candidatos aparecem na linha acima do prompt, com dois espaços entre eles.

Table: Mensagens dos auxiliares

| Mensagem | O que significa |
| --- | --- |
| `help: not found. Try: apropos <word>, or man intro` | O FreeBSD não tem comando help. Use uma das duas formas que ele indica. |
| `<text>: nothing appropriate.` | Nenhum comando ou tópico corresponde à palavra que você pesquisou. |
| `No manual entry for <word>` | Não há página de manual com esse nome. Confira a grafia. |

## Se algo der errado

> [!PROBLEM] O computador diz help: não encontrado.
> Digite apropos e uma palavra para o que você quer fazer, ou digite man intro.

> [!PROBLEM] O apropos diz: nada apropriado.
> Tente uma palavra mais curta, ou uma diferente.

> [!PROBLEM] O man diz: Nenhuma entrada de manual para graphics.
> Digite o nome inteiro, como man graphics-cards.

> [!SEE] jsc:help_on_a_computer, jsc:finding_help_overview, jsc:man_pages, jsc:whatis_and_apropos, jsc:pipes_and_variables
