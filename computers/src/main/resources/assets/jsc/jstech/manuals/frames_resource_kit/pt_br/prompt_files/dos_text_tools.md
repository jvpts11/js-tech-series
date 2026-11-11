---
section: prompt_files
order: 239
---
# Texto: find, sort, more e o pipe

A família DOS tem três comandos para texto. O find mostra as linhas que contêm algum texto, o sort coloca as linhas em ordem, e o more mostra um arquivo uma tela por vez.

O find e o sort recebem o texto de um arquivo que você indica, ou de um pipe. O more lê somente o arquivo que você indica.

> [!DEFINE] Pipe
> Uma linha com dois ou mais comandos unidos por um símbolo de barra vertical, de modo que a saída do primeiro comando vira a entrada do seguinte.

O pipe envia a saída de um comando ao comando seguinte, como entrada. Cada barra precisa de um comando de cada lado. Cada etapa entrega no máximo 4.096 linhas à seguinte.

> [!DEFINE] Redirecionamento
> Enviar a saída de um comando para um arquivo, ou ler a entrada dele de um arquivo, com um símbolo de seta.

Table: Símbolos que unem e redirecionam

| Símbolo | O que faz |
| --- | --- |
| `\|` | Envia a saída do comando da esquerda para o comando da direita. |
| `>` | Grava a saída em um arquivo, substituindo o que o arquivo tinha. |
| `>>` | Acrescenta a saída ao fim de um arquivo. |
| `<` | Lê a entrada de um arquivo. |

As ferramentas de texto do DOS aceitam um nome de arquivo ou um pipe. Os sistemas Unix têm ferramentas com os mesmos nomes, que funcionam separadamente, com regras próprias ([](jsc:text_tools)).

## find

O find mostra as linhas que contêm o texto entre aspas duplas. Ele diferencia maiúsculas de minúsculas, a menos que você acrescente /I, que ignora letras maiúsculas. /V mostra as linhas que não contêm o texto.

```command
id: jsc:command/mc_dos/find
```

## sort

O sort coloca as linhas em ordem, comparando-as como texto. /R inverte a ordem. Ele lê um pipe ou o arquivo que você indica.

```command
id: jsc:command/mc_dos/sort
```

## more

O more mostra um arquivo uma tela por vez. É um paginador: um visualizador que espera você entre as telas. Suas teclas são estas.

> [!DEFINE] Paginador
> Um visualizador que mostra uma tela de um arquivo por vez e espera você continuar.

Table: Teclas no more

| Tecla | O que faz |
| --- | --- |
| `Space` | Avança para a próxima tela. |
| `Page Down` | Avança para a próxima tela. |
| `b` | Volta uma tela. |
| `Page Up` | Volta uma tela. |
| Setas para cima e para baixo | Move uma linha. |
| `/` | Procura o texto que você digitar. |
| `n` | Encontra a próxima ocorrência da busca. |
| `q` | Devolve o terminal. |

```command
id: jsc:command/mc_dos/more
```

## Se algo der errado

> [!PROBLEM] O find não mostra nada.
> O find diferencia maiúsculas de minúsculas, a menos que você acrescente /I. Confira também as letras maiúsculas e mantenha o texto entre aspas duplas.

> [!PROBLEM] O sort não coloca os números em ordem como números.
> O sort compara as linhas como texto, então a linha "10" vem antes da linha "9". O sort do DOS não tem opção numérica.

> [!PROBLEM] A linha diz erro de sintaxe perto de token inesperado, e cita a barra.
> Um pipe precisa de um comando de cada lado da barra.

> [!PROBLEM] Uma seta não tem nome de arquivo depois dela, e a linha para com um erro de sintaxe.
> Digite o nome do arquivo depois da seta, como em > RESULT.TXT.

> [!SEE] jsc:pipes_and_variables, jsc:text_tools
