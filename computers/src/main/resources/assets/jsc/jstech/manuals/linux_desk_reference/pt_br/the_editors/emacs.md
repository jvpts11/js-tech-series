---
section: the_editors
order: 276
---
# Emacs

O Emacs não tem modos. Cada tecla que você pressiona digita uma letra, a menos que você a use junto com Ctrl ou Alt. Um comando é uma dessas combinações, às vezes seguida de uma segunda tecla. As teclas com Ctrl são escritas C-, e as teclas com Alt são escritas M-.

Enquanto um comando espera a próxima tecla, a linha de baixo mostra as teclas que você já pressionou, como C-x-. Caso contrário, mostra o nome do arquivo. Para um arquivo sem alterações, aparece -UUU:----F1 e depois o nome. Para um arquivo com alterações não salvas, os dois traços depois de -UUU: são dois asteriscos.

> [!DEFINE] Combinação de teclas
> Uma tecla usada com Ctrl ou Alt, às vezes seguida de uma segunda tecla, como C-x C-s.

> [!DEFINE] Buffer
> O texto que o editor guarda na memória, no buffer, até você salvar.

Letras, dígitos e símbolos são digitados no arquivo. [[Enter]] começa uma nova linha com o mesmo recuo da linha de cima, e [[Tab]] digita quatro espaços. As setas, Home e End movem o cursor, e PageUp e PageDown o movem dez linhas.

## Salvar e sair

1. Pressione [[Ctrl]]+[[X]], depois [[Ctrl]]+[[S]], para salvar o texto.
2. Pressione [[Ctrl]]+[[X]], depois [[Ctrl]]+[[C]], para sair do editor.
3. Se a linha de baixo fizer uma pergunta, responda com y ou n.

Table: Combinações e comandos

| Teclas | O que faz | Mensagem na linha de baixo |
| --- | --- | --- |
| `C-x C-s` | Salva o arquivo. | "hello.txt" gravado |
| `C-x C-c` | Sai do editor. Com alterações não salvas, pergunta antes. | Existem buffers modificados; sair mesmo assim? (y ou n) |
| `C-x u` | Desfaz a última alteração. | Desfazer, ou Não há mais informações para desfazer no início do texto |
| `C-g` | Cancela o que você está fazendo. | Cancelado |
| `C-b` | Move um caractere para a esquerda. | nenhuma |
| `C-f` | Move um caractere para a direita. | nenhuma |
| `C-p` | Move uma linha para cima. | nenhuma |
| `C-n` | Move uma linha para baixo. | nenhuma |
| `C-a` | Move para o início da linha. | nenhuma |
| `C-e` | Move para o fim da linha. | nenhuma |
| `C-d` | Apaga o caractere depois do cursor. | nenhuma |
| `C-k` | Apaga o texto do cursor até o fim da linha; no fim de uma linha, apaga a quebra de linha. | nenhuma |
| `C-y` | Cola de volta o texto que C-k recortou por último. | nenhuma |
| `M-<` | Move para o início do arquivo. | nenhuma |
| `M->` | Move para o fim do arquivo. | nenhuma |
| `M-x compile` | Compila o arquivo, como descrito abaixo. | Compilação concluída |

O Emacs não conhece outras combinações que você possa conhecer do Emacs, como C-s para pesquisar. Ele avisa: C-s não está definido, e as teclas digitadas são descartadas.

## Saindo com alterações não salvas

Quando o texto tem alterações não salvas, C-x C-c pergunta: Existem buffers modificados; sair mesmo assim? (y ou n). Pressione y para sair sem salvar. Pressione n para continuar no arquivo, depois salve com C-x C-s.

Enquanto a pergunta está aberta, Ctrl+G ou Esc a cancela, e a linha de baixo diz Cancelado. Qualquer outra tecla pergunta de novo: Responda y ou n.

> [!DEFINE] Recortar
> Recortar texto e guardá-lo, para que colar possa devolvê-lo.

> [!DEFINE] Colar
> Colar de volta o texto que foi recortado por último.

## Compilando com M-x compile

M-x compile compila o texto como ele está no buffer, do jeito que o compilador da sua linguagem lê um arquivo. Ele não salva o arquivo antes. O resultado abre em uma segunda área abaixo do texto, para que os erros possam ser lidos enquanto o texto continua na tela.

A extensão do nome do arquivo decide a linguagem dele. Quais linguagens podem compilar depende dos compiladores instalados na máquina.

1. Pressione [[Alt]]+[[X]].
2. Digite compile, depois pressione [[Enter]].
3. Leia a segunda área abaixo do texto.

Table: A segunda área

| Linha | O que significa |
| --- | --- |
| A linguagem, depois o nome do arquivo | Qual linguagem compilou qual arquivo. |
| <file> -> <N> linhas de assembly | A compilação deu certo, e N é o número de linhas de assembly geradas. |
| Compilação concluída | A compilação terminou. |
| Cada erro, uma linha para cada um | Um erro que o compilador encontrou. |
| A compilação terminou de forma anormal com <N> erro(s) | A compilação falhou, e N é o número de erros. |
| nenhum compilador reconhece <file> | Nenhuma linguagem nesta máquina reconhece a extensão do arquivo. |

```command
id: jsc:command/linux/emacs
```

## Se algo der errado

> [!PROBLEM] A segunda área diz nenhum compilador reconhece, seguido do nome do arquivo.
> A extensão do arquivo não é a de uma linguagem usada nesta máquina. Salve o arquivo com a extensão da sua linguagem, depois compile de novo.

> [!PROBLEM] Você pressiona C-x C-c, e depois uma tecla que não é y nem n.
> A pergunta repete Responda y ou n. Pressione y ou n.

> [!PROBLEM] A linha de baixo faz a pergunta, e você quer continuar trabalhando.
> Pressione n para voltar ao texto, depois salve com C-x C-s.

> [!PROBLEM] A linha de baixo diz que C-s não está definido.
> Este editor não tem comando para essa combinação. Use as combinações da tabela acima.

> [!SEE] jsc:vim, jsc:vi, jsc:ee
