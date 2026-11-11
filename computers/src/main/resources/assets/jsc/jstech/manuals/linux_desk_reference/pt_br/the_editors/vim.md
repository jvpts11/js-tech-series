---
section: the_editors
order: 275
---
# Vim

O Vim funciona em modos. Um modo decide o que uma tecla faz: em um modo, uma letra é digitada no arquivo, e em outro a mesma letra é um comando que move o cursor ou apaga texto.

O Vim aqui é uma cópia moderna do editor. Não é o Vim inteiro. Ele tem o que você precisa para escrever e alterar um programa: mover, digitar, apagar, copiar uma linha, desfazer e alguns comandos de dois-pontos. Não tem busca nem janelas divididas.

A linha de baixo mostra o nome do arquivo entre aspas, com [+] depois dele quando o texto tem alterações não salvas. No modo de inserção ela também mostra -- INSERT --.

> [!DEFINE] Modo
> O estado que decide o que uma tecla faz: normal, inserção ou comando.

## Os três modos

Você começa no modo normal. Pressione [[Esc]] para voltar ao modo normal a partir do modo de inserção ou do modo de comando.

Table: Os modos

| Modo | O que as teclas fazem |
| --- | --- |
| Normal | Uma letra é um comando: move o cursor, apaga, copia ou inicia o modo de inserção. |
| Inserção | Letras, dígitos e símbolos são digitados no arquivo. |
| Comando | O que você digita depois dos dois-pontos roda quando você pressiona [[Enter]]. |

## Teclas no modo normal

Table: Teclas no modo normal

| Tecla | O que faz |
| --- | --- |
| `i` | Inicia o modo de inserção antes do cursor. |
| `a` | Inicia o modo de inserção depois do cursor. |
| `A` | Inicia o modo de inserção no fim da linha. |
| `I` | Inicia o modo de inserção no início da linha. |
| `o` | Abre uma nova linha abaixo e inicia o modo de inserção ali. |
| `O` | Abre uma nova linha acima e inicia o modo de inserção ali. |
| `h j k l` | Move para a esquerda, para baixo, para cima e para a direita. |
| `w` | Vai para o início da próxima palavra, na linha seguinte quando esta linha não tem mais palavras. |
| `b` | Vai para o início da palavra anterior. |
| `e` | Vai para o fim da palavra onde está o cursor, ou da próxima. |
| `0` | Vai para o início da linha. |
| `^` | Vai para o primeiro caractere que não é espaço. |
| `$` | Vai para o fim da linha. |
| `G` | Vai para a última linha do arquivo. |
| `gg` | Vai para a primeira linha do arquivo. |
| `x` | Apaga o caractere sob o cursor, a menos que o cursor esteja no fim da linha. |
| `D` | Apaga do cursor até o fim da linha. |
| `dd` | Apaga a linha inteira e a guarda no registrador. |
| `dw` | Apaga até a próxima palavra. Quando a próxima palavra está em outra linha, apaga até o fim desta linha. |
| `yy` | Copia a linha inteira para o registrador. |
| `p` | Coloca o registrador de volta na linha de baixo. |
| `P` | Coloca o registrador de volta na linha de cima. |
| `u` | Desfaz a última alteração. |
| `Ctrl+R` | Refaz a alteração que o desfazer tirou, no modo normal. |
| `:` | Inicia um comando de dois-pontos. A linha de baixo mostra os dois-pontos e o que você digita. |
| `Arrows, Home, End` | Move o cursor. |
| `PageUp, PageDown` | Move o cursor dez linhas para cima ou para baixo. |
| `Enter` | Desce uma linha. |
| `Backspace` | Vai uma coluna para a esquerda. |
| `Esc` | Continua no modo normal, e cancela um dd, dw, yy ou gg pela metade. |

As teclas d, y e g esperam uma segunda tecla. Só dd, dw, yy e gg fazem algo. Qualquer outra segunda tecla é ignorada, e a espera termina.

> [!DEFINE] Registrador
> O registrador onde dd e yy guardam a linha que colaram, para que p e P a coloquem de volta.

## Teclas no modo de inserção

Table: Teclas no modo de inserção

| Tecla | O que faz |
| --- | --- |
| Letras, dígitos e símbolos | São digitados no arquivo. |
| `Enter` | Inicia uma nova linha com o mesmo recuo da linha de cima. Depois de uma chave {, recua mais quatro espaços. |
| `Tab` | Digita quatro espaços. |
| `Backspace` | Apaga o caractere antes do cursor. |
| `Delete` | Apaga o caractere sob o cursor. |
| `Esc` | Volta ao modo normal. Um desfazer desfaz a inserção inteira. |

## Comandos de dois-pontos

Digite um comando de dois-pontos depois dos dois-pontos, e depois pressione [[Enter]]. Nada acontece até você pressionar.

Table: Comandos de dois-pontos

| Comando | O que faz |
| --- | --- |
| `:w` | Grava o arquivo no disco. |
| `:q` | Sai. É recusado enquanto o texto tiver alterações não salvas. |
| `:q!` | Sai sem salvar, mesmo com alterações. |
| `:wq` | Grava o arquivo, e depois sai. |
| `:x` | Grava o arquivo, e depois sai. |
| `:wq!` | Grava o arquivo, e depois sai. |
| `:x!` | Grava o arquivo, e depois sai. |
| `:<number>` | Vai para essa linha. 0 vai para a linha 1, e um número além do fim vai para a última linha. |
| `: Enter` | Sem nada depois dos dois-pontos, não faz nada. |

## Mensagens na linha de baixo

Table: Mensagens

| Mensagem | Quando você a vê |
| --- | --- |
| "hello.txt" [Novo] | Você abre um arquivo que ainda não existe. |
| "hello.txt" gravado | Você grava o arquivo com :w, :wq ou :x. |
| 1 linha colada | Você copia uma linha com yy. |
| 1 alteração; antes | Você desfaz uma alteração com u. |
| 1 alteração; depois | Você refaz uma alteração com Ctrl+R. |
| Já está na alteração mais antiga | Você pressiona u, e não há mais nada para desfazer. |
| Já está na alteração mais recente | Você pressiona Ctrl+R, e não há mais nada para refazer. |
| E492: não é um comando do editor: seguido do que você digitou | O comando de dois-pontos não é um dos comandos acima. |
| E37: sem escrita desde a última alteração (adicione ! para ignorar) | Você tenta sair com alterações não salvas. |
| Texto longo demais para salvar: seguido de dois números | O texto é longo demais para salvar, e o arquivo mantém o conteúdo antigo. |

```command
id: jsc:command/linux/vim
```

## Se algo der errado

> [!PROBLEM] A linha de baixo diz E37: sem escrita desde a última alteração (adicione ! para ignorar).
> O texto tem alterações que não foram salvas. Digite :w para salvar, e depois :q. Ou digite :q! para sair sem salvar.

> [!PROBLEM] A linha de baixo diz E492: não é um comando do editor, seguido do que você digitou.
> O comando de dois-pontos não está na tabela acima. Verifique a grafia.

> [!PROBLEM] A linha de baixo diz Texto longo demais para salvar.
> O texto tem mais de 32.767 caracteres, então o arquivo mantém o conteúdo antigo. Deixe o texto mais curto, ou divida-o em mais de um arquivo.

> [!PROBLEM] Você digita 3 e depois dd, e só uma linha sai.
> Contagens não são suportadas aqui. Digite dd uma vez para cada linha.

> [!PROBLEM] Depois de d, y ou g, nada acontece.
> Só dd, dw, yy e gg são comandos. Pressione [[Esc]] para cancelar a espera, e depois digite um comando.

> [!PROBLEM] A linha de baixo diz Já está na alteração mais antiga.
> Não há mais nada para desfazer. Faça a alteração de novo, ou deixe o arquivo como está.

> [!SEE] jsc:emacs, jsc:vi, jsc:ee
