---
section: prompt_files
order: 236
---
# Trabalhando com arquivos e pastas

A maior parte do trabalho do prompt com arquivos é isto: você lista, muda entre pastas, copia, renomeia e apaga. Cada verbo recebe as suas palavras na ordem que a tabela mostra, e cada um tem uma página própria abaixo.

Estes verbos precisam de uma máquina com sistema de arquivos. O MC-NET não tem nenhum, então eles não estão no seu prompt.

> [!DEFINE] Extensão
> As letras depois do último ponto em um nome de arquivo. Elas dizem ao sistema que tipo de arquivo ele é.

> [!DEFINE] Somente leitura
> Um arquivo que você pode ler, mas não pode alterar nem apagar.

Table: Os verbos de arquivo

| Verbo | Palavras depois dele, e o que faz |
| --- | --- |
| `dir` | Aceita uma pasta opcional. Lista os arquivos e pastas nela. |
| `cd (chdir)` | Aceita uma pasta opcional. Mostra a pasta em que você está, ou muda para a pasta que você indicar. |
| `type` | Um nome de arquivo. Imprime as linhas do arquivo. |
| `write (save)` | Um nome de arquivo, depois o texto. Cria o arquivo, ou o sobrescreve, com o texto. |
| `del (erase)` | Um nome de arquivo. Apaga um arquivo do disco do sistema. |
| `copy` | Uma origem, depois um destino. Copia um arquivo, ou uma árvore de pastas dentro de uma unidade. |
| `move` | Uma origem, depois uma pasta. Move um arquivo ou uma árvore de pastas para dentro dessa pasta. |
| `ren (rename)` | Um nome de arquivo, depois o novo nome. Renomeia o arquivo no lugar; não o move. |
| `mkdir (md)` | Um nome de pasta. Cria a pasta. |
| `rmdir (rd)` | Um nome de pasta. Remove a pasta, se ela estiver vazia. |
| `tree` | Sem palavras. Desenha as pastas abaixo da que você está. |
| `run` | Um nome de arquivo .iql. Executa o arquivo como instruções IQL, a linguagem de consulta da rede. |

dir lista a pasta que você indicar, ou a pasta em que você está. Cada linha mostra quando o item foi escrito pela última vez, o seu tamanho e o seu nome. Uma pasta mostra <DIR> no lugar do tamanho, e um arquivo somente leitura tem [RO] depois do nome. A última linha conta os arquivos e as pastas e mostra o tamanho total em mB, a unidade que a listagem imprime. Uma pasta vazia diz Arquivo Não Encontrado.

```command
id: jsc:command/mc_dos/dir
```

cd sem palavras mostra a pasta em que você está. cd com uma pasta muda para ela. Só uma letra de unidade leva à raiz dessa unidade ([](jsc:paths_and_drives)).

```command
id: jsc:command/mc_dos/cd
```

type imprime cada linha de um arquivo. Um arquivo vazio imprime a linha esmaecida "(arquivo vazio)". Um arquivo .dat é recusado, porque é a visão dos itens de um disco.

```command
id: jsc:command/mc_dos/type
```

write recebe o nome do arquivo, depois tudo que vem depois dele como texto. A extensão decide o tipo do arquivo. Você pode escrever arquivos com as extensões iql, txt, cfg, csv e cmd. Você não pode escrever arquivos log e craft à mão. Em caso de sucesso, write imprime "gravado" e o nome do arquivo.

```command
id: jsc:command/mc_dos/write
```

del apaga um arquivo e imprime "apagado" e o seu nome. Um arquivo .dat não pode ser apagado.

```command
id: jsc:command/mc_dos/del
```

copy copia um arquivo, ou uma árvore de pastas inteira dentro de uma unidade. Entre unidades, copia apenas arquivos, e em caso de sucesso imprime "1 arquivo(s) copiado(s)." Um arquivo de outra máquina pode ser copiado, mas não movido.

```command
id: jsc:command/mc_dos/copy
```

move recebe as mesmas palavras que copy. Ele move um arquivo ou uma árvore de pastas para outra pasta, e entre unidades move apenas arquivos. Em caso de sucesso imprime "1 arquivo(s) movido(s)."

```command
id: jsc:command/mc_dos/move
```

ren muda um nome no lugar. O novo nome é tudo que vem depois do nome antigo. ren não move o arquivo, então use move para isso.

```command
id: jsc:command/mc_dos/ren
```

mkdir cria uma pasta. Algumas unidades são planas e não têm pasta nenhuma, e mkdir as recusa.

```command
id: jsc:command/mc_dos/mkdir
```

rmdir remove uma pasta, mas somente uma vazia. Uma pasta com arquivos dentro é recusada.

```command
id: jsc:command/mc_dos/rmdir
```

tree desenha as pastas abaixo da que você está. Sem nenhuma, ele mostra "Não existem subpastas".

```command
id: jsc:command/mc_dos/tree
```

run executa um arquivo .iql, como se você tivesse digitado as suas instruções com iql ([](jsc:network_verbs)). Somente arquivos .iql são executados. Um resultado aparece em verde em caso de sucesso e em vermelho em caso de falha.

```command
id: jsc:command/mc_dos/run
```

## Se algo der errado

> [!PROBLEM] NAME: arquivos .dat são somente leitura (use o Interator de Rede para acessar itens)
> Um arquivo .dat é a visão dos itens do disco. Para mover itens, use interac ([](jsc:network_verbs)).

> [!PROBLEM] NAME: .log é somente leitura
> Você não pode alterar arquivos de log à mão. Para o log da rede, veja [](jsc:cancelling_and_the_log).

> [!PROBLEM] NAME: arquivos .craft não podem ser editados
> As fabricações são pedidas na rede, não escritas à mão ([](jsc:asking_for_a_craft)).

> [!PROBLEM] NAME: não há espaço livre suficiente no disco
> Apague um arquivo que você não precisa mais, ou use um disco maior.

> [!PROBLEM] Diretórios não são suportados nesta unidade.
> Esta unidade é plana e não tem pastas. Coloque a pasta em outra unidade.

> [!PROBLEM] NAME: já existe um arquivo com esse nome
> Escolha outro nome, ou apague o arquivo antigo antes.

> [!PROBLEM] NAME: o destino é somente leitura
> Copie ou mova o arquivo para uma pasta onde você possa escrever.

> [!SEE] jsc:paths_and_drives, jsc:storage
