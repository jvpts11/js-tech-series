---
section: the_editors
order: 278
---
# ee

ee é um editor pequeno, e vem apenas com o FreeBSD. Ele não tem modos. Cada tecla que você pressiona digita uma letra, exceto as combinações com Ctrl e a tecla Esc.

Enquanto a janela de informações está ligada, cinco linhas de atalhos ficam na tela. Cada linha mostra uma tecla e o nome do que ela faz. O ^ antes de uma letra significa Ctrl: ^k é Ctrl com K.

> [!DEFINE] Janela de informações
> As cinco linhas de atalhos na tela, que você pode esconder nas configurações.

Table: As linhas de atalhos

| Tecla | Nome na tela | O que faz |
| --- | --- | --- |
| `^[` | (escape) menu | Abre o menu principal. |
| `^y` | prompt de pesquisa | Pede o texto a pesquisar. |
| `^k` | apagar linha | Apaga a linha onde está o cursor, e guarda para restaurar linha. |
| `^p` | lin ant | Move uma linha para cima. |
| `^g` | pág ant | Move uma tela para cima. |
| `^o` | código ASCII | Pede um número de 0 a 255, e digita esse caractere. |
| `^x` | pesquisar | Pesquisa de novo o último texto que você pesquisou. |
| `^l` | restaurar linha | Coloca de volta a última linha apagada abaixo da linha do cursor. |
| `^n` | lin próx | Move uma linha para baixo. |
| `^v` | pág próx | Move uma tela para baixo. |
| `^u` | fim do arquivo | Move para a última linha do arquivo. |
| `^a` | início da linha | Move para o início da linha. |
| `^w` | apagar palavra | Apaga a palavra depois do cursor, e guarda para restaurar palavra. |
| `^b` | voltar 1 caractere | Move um caractere para a esquerda. |
| `^z` | próxima palavra | Move para o início da próxima palavra. |
| `^t` | início do texto | Move para o início do texto. |
| `^e` | fim da linha | Move para o fim da linha. |
| `^r` | restaurar palavra | Coloca de volta a palavra que apagar palavra removeu por último. |
| `^f` | avançar 1 caractere | Move um caractere para a direita. |
| `^c` | comando | Pede um comando: write, quit ou exit. |
| `^d` | apagar caractere | Apaga o caractere sob o cursor, e guarda para restaurar caractere. |
| `^j` | restaurar caractere | Coloca de volta o caractere que apagar caractere removeu por último. |

A quinta linha termina com a dica ESC-Enter: sair. Nomes como lin ant e lin próx são curtos, porque a tela é estreita.

## O menu principal

Pressione [[Esc]] para abrir o menu principal, intitulado menu principal do ee. Mova-se com as teclas Cima e Baixo, escolha com [[Enter]], ou feche o menu com [[Esc]].

Table: O menu principal

| Item | O que faz |
| --- | --- |
| sair do editor | Pergunta o que fazer com as alterações: salvar alterações, que já vem selecionado, ou não salvar. |
| ajuda | Mostra a página de referência de comandos do ee; pressione qualquer tecla para fechá-la. |
| operações com arquivos | read a file pede um arquivo e coloca o texto dele na posição do cursor; save file grava o arquivo aberto. |
| redesenhar a tela | Desenha a tela de novo. |
| configurações | janela de informações: ligada e janela de informações: desligada mostram ou escondem as cinco linhas. |
| pesquisar | Pede o texto a pesquisar. |

## Sair rapidamente

1. Pressione [[Esc]] para abrir o menu principal.
2. Pressione [[Enter]] para escolher sair do editor.
3. Pressione [[Enter]] de novo para escolher salvar alterações, que já vem selecionado.

Escolha não salvar para sair sem gravar o arquivo. O ee não pergunta de novo antes de sair.

## Abrir, salvar e a linha de posição

Ao abrir um arquivo, a linha de baixo diz "hello.txt" [Arquivo novo] para um arquivo novo. Para um arquivo existente, ela mostra o nome, as linhas e os caracteres, como em "hello.txt" 3 linhas, 42 caracteres. Salvar mostra a mesma coisa.

Abaixo das linhas de atalhos, uma linha mostra onde está o cursor: a linha dele, a coluna dele e quantas linhas ele está do topo da tela.

## A linha de comando

Pressione ^c. A linha de baixo mostra o rótulo "Comando" e espera por uma palavra. Digite a palavra e pressione [[Enter]].

Table: Palavras de comando

| Palavra | O que faz |
| --- | --- |
| `write` | Salva o arquivo. |
| `quit` | Sai do editor na hora, sem salvar. |
| `exit` | Salva o arquivo, depois sai do editor. |

## Pesquisa e prompts

Pressione ^y, ou escolha pesquisar no menu principal, e digite o texto. Pressione [[Enter]]. O cursor vai até o texto. ^x pesquisa de novo o último texto que você digitou.

Qualquer prompt na linha de baixo espera por você. Pressione [[Enter]] para responder, [[Esc]] para cancelar, e Backspace para corrigir o que você digitou.

```command
id: jsc:command/freebsd/ee
```

## Se algo der errado

> [!PROBLEM] A linha de baixo diz Comando desconhecido, seguido do que você digitou.
> A linha de comando conhece apenas write, quit e exit. Digite uma dessas palavras.

> [!PROBLEM] A linha de baixo diz Não encontrado, seguido do seu texto.
> O texto não está no arquivo, ou está escrito de outro jeito. Confira e pesquise de novo.

> [!PROBLEM] Você pressiona ^x, e nada acontece.
> Ainda não foi digitado nenhum texto para pesquisar. Pesquise com ^y primeiro, depois ^x pesquisa de novo.

> [!PROBLEM] Você escolhe ler um arquivo, e a linha de baixo diz "nome": Não existe esse arquivo ou diretório.
> O arquivo não existe, ou o nome está errado. Digite o nome de novo, exatamente como o arquivo o tem.

> [!PROBLEM] Você sai sem salvar ou com quit, e suas alterações desaparecem.
> O ee não pergunta antes quando você sai sem salvar ou com quit. Escolha salvar alterações no menu principal, ou digite write no prompt de comando (^c), antes de sair.

> [!PROBLEM] As linhas de atalhos desapareceram.
> A janela de informações está desligada. Abra o menu principal, escolha configurações, depois janela de informações: ligada.

> [!SEE] jsc:vi, jsc:vim, jsc:the_systems
