---
section: prompt_system
order: 240
---
# Os comandos do próprio prompt

Alguns comandos pertencem ao próprio prompt. help lista os comandos e explica cada um, cls limpa a janela, echo imprime as palavras que você der a ele, e set dá a um nome um valor. A maioria existe em todos os sistemas, e a tabela mostra onde cada um roda.

Table: Onde rodam os comandos do próprio prompt

| Comando | Sistemas |
| --- | --- |
| `help` | MC-DOS, MC-NET, as edições Frames e os sistemas Linux e UNIX. Não o FreeBSD. |
| `cls` | MC-DOS e as edições Frames. |
| `version` | MC-DOS e as edições Frames. |
| `echo` | Todos os sistemas. |
| `whoami` | Todos os sistemas. |
| `exit` | Todos os sistemas. |
| `set` | Todos os sistemas. |

## help

help sem palavras lista os comandos que esta máquina pode executar, uma linha para cada um, com um resumo de cada. No final está escrito "'help <comando>' para detalhes". A lista mostra apenas os comandos que esta máquina tem, e não todos os comandos da família.

help <comando> imprime a página de um comando. No MC-DOS e no MC-NET, help ocupa a tela inteira, com os manuais da série ao lado dos comandos. Nas edições Frames, help imprime. help dir abre a página de dir. A página termina com uma linha "also" que lista os outros nomes do comando.

Table: Teclas na ajuda em tela cheia

| Tecla | O que faz |
| --- | --- |
| `Tab` | Escolhe um botão ou um link. |
| `Enter` | Segue o botão ou link escolhido. |
| `Alt+C` | Mostra o conteúdo. |
| `Alt+N` | Vai para o próximo tópico. |
| `Alt+B` | Vai para o tópico anterior. |
| `Alt+I` | Mostra o índice. |
| `Alt+S` | Procura uma palavra. |
| `F3` | Procura a mesma palavra de novo. |
| `Esc` | Sai da ajuda. |

```command
id: jsc:command/mc_dos/help
```

## cls, version e echo

cls limpa a janela, e o histórico de rolagem vai junto.

```command
id: jsc:command/mc_dos/cls
```

version imprime a linha "J's Computers Shell v1.0" na cor de destaque.

```command
id: jsc:command/mc_dos/version
```

echo imprime as palavras que você der a ele. No MC-DOS e nas edições Frames, o texto é impresso como você digitou.

```command
id: jsc:command/mc_dos/echo
```

## whoami e exit

whoami imprime o nome deste computador, o tipo dele e o nó dele, uma linha para cada. Um computador sem nome mostra (unnamed).

```command
id: jsc:command/mc_dos/whoami
```

exit fecha uma sessão remota que você abriu com ssh ([](jsc:network_verbs)). No prompt deste computador, exit só diz que não está conectado. Para sair da janela, pressione [[Esc]].

```command
id: jsc:command/mc_dos/exit
```

## set e variáveis

> [!DEFINE] Variável
> Um nome que a máquina guarda com um valor. Você dá a ele um valor com set e o lê de volta entre sinais de porcentagem.

set sem palavras lista todos os nomes que esta máquina conhece. set NAME=value dá a um nome um valor. set NAME= sem nada depois do sinal de igual esquece o nome. Os nomes usam letras, dígitos e sublinhados, e não começam com dígito.

Um valor definido aqui pertence à máquina. Ele continua definido no monitor, em uma sessão aberta a partir de outra máquina, e depois de uma reinicialização.

1. Digite set BASE=C:\\\\work para dar a BASE um valor.
2. Digite set para listar os nomes, e BASE estará na lista.
3. Digite dir %BASE% para ler o valor de volta, que o prompt substitui antes de dir ser executado.
4. Digite set BASE= para esquecer o nome de novo.

```command
id: jsc:command/mc_dos/set
```

## Se algo der errado

> [!PROBLEM] help não ocupa a tela inteira.
> A ajuda em tela cheia existe somente no MC-DOS e no MC-NET. Nas edições Frames, help imprime a lista ([](jsc:help_on_a_computer)).

> [!PROBLEM] help <command> diz que não existe esse comando.
> O comando não está nesta máquina. Digite help para ver a lista, ou listcmd se o servidor permitir ([](jsc:finding_a_command)).

> [!PROBLEM] set diz que um nome não é permitido.
> Comece o nome com uma letra, e use somente letras, dígitos e sublinhados.

> [!PROBLEM] set diz uso: set NAME=value
> Coloque um sinal de igual entre o nome e o valor: set NAME=value.

> [!PROBLEM] exit diz que não está conectado.
> exit só fecha uma sessão ssh. Para sair desta janela, pressione [[Esc]].

> [!SEE] jsc:help_on_a_computer, jsc:network_verbs
