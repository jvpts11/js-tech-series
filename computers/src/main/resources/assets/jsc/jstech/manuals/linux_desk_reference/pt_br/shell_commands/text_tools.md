---
section: shell_commands
order: 252
---
# Ler e ordenar texto

As ferramentas de texto leem linhas de um arquivo, ou as linhas que um pipe traz. Elas contam as linhas, escolhem algumas, colocam em ordem ou mostram uma tela por vez.

Uma linha é o texto entre duas quebras de linha. A maioria dessas ferramentas conta ou escolhe por linha.

> [!DEFINE] Paginador
> Um programa que mostra uma tela de texto por vez, para você ler um arquivo longo.

```command
id: jsc:command/linux/grep
```

```command
id: jsc:command/linux/wc
```

```command
id: jsc:command/linux/head
```

```command
id: jsc:command/linux/tail
```

```command
id: jsc:command/linux/sort
```

## Teclas no less

O less mostra um arquivo uma página por vez. Estas teclas movem você por ele, e cada uma funciona enquanto o less ocupa o terminal inteiro.

Table: Teclas no less

| Tecla | O que faz |
| --- | --- |
| `Space` | Mostra a próxima página. |
| `b` | Mostra a página anterior. |
| `Arrow keys` | Move uma linha por vez. |
| `/` | Procura no texto o que você digitar em seguida. |
| `n` | Vai para a próxima ocorrência da busca. |
| `q` | Sai do less e devolve o prompt. |

```command
id: jsc:command/linux/less
```

## Se algo der errado

> [!PROBLEM] O grep não mostra nenhuma linha.
> O grep procura exatamente o texto que você digitar, então confira a grafia. Adicione -i para ignorar maiúsculas e minúsculas.

> [!PROBLEM] O less diz que não pode receber um pipe.
> Digite less sozinho, com um nome de arquivo. Para ler só algumas linhas, primeiro escolha-as com grep e depois leia o resultado num arquivo com less.

> [!SEE] jsc:file_commands, jsc:pipes_and_variables
