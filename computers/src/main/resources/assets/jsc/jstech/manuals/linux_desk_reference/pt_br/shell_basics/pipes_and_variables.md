---
section: shell_basics
order: 250
---
# Pipes, redirecionamento, tarefas em segundo plano, variáveis, curingas e Tab

Alguns sinais pequenos mudam o que um comando lê e para onde vai a sua resposta. Eles funcionam no Linux, no FreeBSD e no UNIX System V, e este item diz onde os três diferem.

## Pipes e redirecionamento

Um pipe, escrito com uma barra vertical, pega o que o comando à sua esquerda imprime e o entrega ao comando à sua direita. Dois comandos unidos dessa forma funcionam como um só.

O shell lê uma barra ou um e comercial (&) apenas como uma palavra própria. Escrito como ls|grep x, o conjunto inteiro é uma palavra só, e nenhum pipe é criado.

Table: Operadores

| Operador | O que faz |
| --- | --- |
| `\|` | Envia a resposta do comando da esquerda para o da direita. |
| `>` | Grava a resposta em um arquivo e substitui o que o arquivo continha. |
| `>>` | Acrescenta a resposta ao final de um arquivo. |
| `<` | Lê um arquivo como entrada do comando. |
| `&` | Executa o comando em segundo plano, como a última palavra da linha. |

O nome do arquivo pode ficar colado à seta, como em >notes.txt, ou vir depois dela como palavra própria, como em > notes.txt. Uma seta sem nome de arquivo é um erro de sintaxe. Um operador entre aspas é texto comum, então uma barra entre aspas é um caractere comum.

Um pipe transporta no máximo 4.096 linhas de um comando para o próximo.

Alguns comandos ocupam o terminal inteiro, então não podem receber pipe nem ser redirecionados: less, vim, emacs, vi e ee, info no Linux, e man quando abre uma entrada de manual.

> [!DEFINE] Pipe
> Uma conexão que entrega o que um comando imprime ao próximo comando como entrada.

> [!DEFINE] Redirecionamento
> Enviar a resposta de um comando para um arquivo com as setas > e >>, ou ler a entrada dele a partir de um arquivo com <.

## Tarefas em segundo plano

Coloque um e comercial (&) como última palavra de uma linha, e o comando roda em segundo plano. O prompt volta na hora, e você pode continuar digitando enquanto o comando trabalha.

> [!DEFINE] Job
> Um comando iniciado com o e comercial (&) e deixado para rodar em segundo plano. Ele tem um número, que você usa para listá-lo ou encerrá-lo.

Cada tarefa em segundo plano ocupa 1 MB da memória da máquina. Quando não sobra espaço, o shell recusa a nova tarefa.

## Variáveis

Uma variável é um nome que representa um valor de texto. Você atribui um valor a ela com NAME=value em uma linha própria, e a usa com cifrão: $NAME, ou ${NAME} quando letras vêm logo após o nome.

> [!DEFINE] Variável
> Um nome que representa um valor de texto, como USER para o nome de login.

Um nome tem apenas letras, dígitos e sublinhados, e não começa com dígito. O comando set atribui um valor em qualquer máquina. O comando export torna um valor visível para os programas que o shell executa, e unset esquece um nome.

Table: Nomes que a máquina responde

| Nome | Valor |
| --- | --- |
| `HOSTNAME` | O nome do computador. |
| `USER` | O nome de login, que é sempre player. |
| `OS` | O sistema: Linux, FreeBSD ou UNIX. |
| `PWD` | A pasta em que você está. |
| `PATH` | Definida por padrão somente no UNIX System V; no Linux e no FreeBSD ela fica vazia até você defini-la. |

Uma variável pertence ao computador, não a uma janela, então um nome que você define continua definido para o computador inteiro.

## Curingas

Um asterisco representa qualquer texto em um nome de arquivo. Uma palavra sem aspas com asterisco se expande para os nomes dos arquivos correspondentes na sua pasta, antes de o comando rodar.

> [!DEFINE] Curinga
> Um símbolo, como o asterisco, que representa outro texto em nomes. O shell o substitui pelos nomes correspondentes.

Coloque a palavra entre aspas, como em "notes\*", e o shell mantém o asterisco como você escreveu. No MC-DOS e nos sistemas Frames, o comando recebe o próprio asterisco.

## Tab

Pressione Tab para completar uma palavra que você começou a digitar. Na primeira palavra, Tab completa o nome de um comando. Depois de um comando como mkfs.ext4 ou mount, ele completa um nome de dispositivo. Tab não completa nomes de arquivos nem de pastas.

Pressione Tab duas vezes seguidas, e o shell imprime todas as possíveis complementações acima do prompt.

## O que não está aqui

Alguns recursos de shell de outros sistemas estão ausentes nestes três. A tabela diz o que cada um faz aqui.

Table: Ausente aqui

| Recurso | O que acontece |
| --- | --- |
| `;` | O ponto e vírgula, && e \|\| são palavras comuns que o comando recebe como texto. |
| `2>` | Não há forma de enviar erros para um arquivo; só existem as setas acima. |
| `$(command)` | Um comando não pode substituir a própria resposta. |
| `alias` | Não existe o comando alias nem a forma de histórico com !, em nenhum desses sistemas. |
| `find` | Não no Unix, onde find é um comando DOS; grep procura as linhas de um arquivo. |
| `more` | Não no Unix, onde o comando para ler página por página é less. |

## Se algo der errado

> [!PROBLEM] O shell diz erro de sintaxe perto de token inesperado, seguido de uma barra ou de uma seta.
> Um pipe precisa de um comando de cada lado, e uma seta precisa de um nome de arquivo. Coloque um espaço em volta da barra vertical.

> [!PROBLEM] O shell diz que o comando ocupa o terminal inteiro, por isso não pode ser usado com pipe nem redirecionado.
> Execute o comando sozinho. Para ler uma resposta longa, passe o comando por um pipe para grep ou head, para manter apenas as linhas de que você precisa, ou grave a resposta em um arquivo e leia com less.

> [!PROBLEM] O shell diz que a máquina não tem mais memória para outra tarefa.
> Encerre uma tarefa concluída ou indesejada com kill %<id> ([](jsc:processes_and_jobs)).

> [!PROBLEM] ls | wc -l imprime 1.
> O comando ls imprime todos os nomes em uma linha, a menos que você use -l. Use ls -l, que imprime uma linha para cada nome.

> [!PROBLEM] O shell diz que um nome é formado por letras, dígitos e sublinhados, e não começa com dígito.
> Escolha um nome de variável com apenas letras, dígitos e sublinhados, e não o comece com um dígito.

> [!SEE] jsc:shell_prompt, jsc:processes_and_jobs
