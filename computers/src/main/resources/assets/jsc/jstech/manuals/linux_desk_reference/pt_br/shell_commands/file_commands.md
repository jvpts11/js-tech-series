---
section: shell_commands
order: 251
---
# Trabalhando com arquivos e pastas

A maior parte do seu trabalho em um sistema Unix é com arquivos e pastas: você lista, muda entre eles, copia, renomeia e apaga. Estes comandos precisam de um disco, o disco do sistema ou um em uma unidade.

Aqui está uma sessão curta. Ela mostra como o shell responde quando um comando não recebe nome de arquivo.

```session
{"lines": [{"prompt": "player@ubuntu:~$ ", "text": "cat", "typed": true}, {"text": "cat: usage: cat <file>"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

Nomes de pasta terminam com uma barra na listagem do ls, assim você distingue arquivos e pastas de relance. Os comandos abaixo são os mais comuns primeiro.

1. Digite write notes.txt seguido do texto que você quer no arquivo.
2. Digite cat notes.txt para ler o arquivo de volta.
3. Digite ls para ver o arquivo na pasta.

```command
id: jsc:command/linux/ls
```

```command
id: jsc:command/linux/cd
```

```command
id: jsc:command/linux/pwd
```

```command
id: jsc:command/linux/cat
```

```command
id: jsc:command/linux/mkdir
```

```command
id: jsc:command/linux/touch
```

```command
id: jsc:command/linux/write
```

```command
id: jsc:command/linux/cp
```

```command
id: jsc:command/linux/mv
```

```command
id: jsc:command/linux/rm
```

```command
id: jsc:command/linux/rmdir
```

```command
id: jsc:command/linux/run
```

```command
id: jsc:command/linux/du
```

```command
id: jsc:command/linux/df
```

O comando df lista todos os discos do computador, com o nome do dispositivo de cada um. O comando mkfs.ext4 cria um sistema de arquivos novo e vazio em um dispositivo.

```command
id: jsc:command/linux/mkfs.ext4
```

> [!WARNING] O mkfs.ext4 apaga o dispositivo inteiro. Tudo nele se perde, e o apagamento não pode ser desfeito.

## Se algo der errado

> [!PROBLEM] O shell diz usage: cat <file>, porque você não informou nenhum nome de arquivo.
> Informe o nome de um arquivo depois de cat.

> [!PROBLEM] O comando mkfs.ext4 diz que não consegue abrir um dispositivo, e que não existe esse dispositivo.
> Rode df e escolha um dos dispositivos que ele listar.

> [!PROBLEM] O comando df diz que não há sistemas de arquivos montados.
> Nenhum disco está no sistema ou em uma unidade. Coloque um e rode df de novo.

> [!SEE] jsc:file_tree, jsc:text_tools
