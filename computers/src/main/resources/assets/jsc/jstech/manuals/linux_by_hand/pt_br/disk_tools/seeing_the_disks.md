---
section: disk_tools
order: 304
---
# Vendo os discos (lsblk)

Antes de mudar qualquer coisa num disco, olhe para ele. O comando lsblk lista todos os discos que o computador tem, e todas as partições desses discos, numa única tabela. Ele responde na hora e não muda nada.

Você o usa em três momentos: primeiro para achar o seu disco, depois de dividi-lo, e depois de criar os sistemas de arquivos, para conferir se cada passo fez o que você queria. lsblk não precisa de nada depois do nome.

Table: As colunas do lsblk

| Coluna | O que ela informa |
| --- | --- |
| `NAME` | O nome do dispositivo, como sda para um disco ou sda2 para a sua segunda partição. |
| `MAJ:MIN` | Dois números que o kernel usa para distinguir os dispositivos, que você pode ignorar. |
| `RM` | Se o dispositivo pode ser removido; todas as linhas aqui mostram 0. |
| `SIZE` | O tamanho do dispositivo: 512M são 512 megabytes, 1G é 1 gigabyte. |
| `RO` | 1 quando o dispositivo é somente leitura: a mídia ao vivo é, e os seus discos não são. |
| `TYPE` | disk para um disco inteiro, part para uma partição, e loop para a mídia ao vivo. |
| `MOUNTPOINTS` | Onde o dispositivo está montado, ou none quando não está. |

Os tamanhos são escritos com M para megabytes, G para gigabytes e T para terabytes. Uma casa decimal aparece só quando é preciso: 512M, 1G, 1,5G, 4T.

## Lendo os nomes

Um disco recebe o nome do slot onde está, não de quantos discos você tem. O primeiro slot é sda, o segundo sdb, e assim por diante. Um slot vazio também ocupa a sua letra, então um disco sozinho no terceiro slot é sdc. Os slots do próprio computador vêm primeiro, depois a baia na frente da caixa ([](jsc:case_front)).

Leia o nome no lsblk e use exatamente esse nome em todos os comandos seguintes. Seu disco pode não ser sda.

## Um disco com partições

Uma partição é uma parte de um disco que tem seu próprio sistema de arquivos. O lsblk lista cada partição abaixo do seu disco. Uma barra vertical e um traço marcam todas as partições, exceto a última, e a última tem um acento grave e um traço. Um disco que ninguém dividiu não mostra nenhuma linha de partição.

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "lsblk", "typed": true}, {"text": "NAME      MAJ:MIN RM   SIZE RO TYPE MOUNTPOINTS"}, {"text": "loop0     7:0      0  1126M  1 loop /run/archiso/airootfs"}, {"text": "sda       8:0      0     1G  0 disk"}, {"text": "|-sda1    8:1      0   512M  0 part /mnt/boot"}, {"text": "`-sda2    8:2      0   512M  0 part /mnt"}], "look": "jsc:guide/terminal/bash", "title": "Arch"}
```

Aqui o seu disco é sda, com 1G de tamanho, e ele está dividido em duas partições. Você as criou com fdisk na próxima entrada. A coluna MOUNTPOINTS mostra onde o novo sistema está anexado: sda2 é o sistema de arquivos raiz, montado em /mnt, e sda1 está montado em /mnt/boot. A linha loop0 é a mídia ao vivo com que você está trabalhando, somente leitura, e não é um disco onde se instala.

> [!DEFINE] Dispositivo
> Um disco ou uma partição que o sistema consegue ler e gravar, com o nome que lsblk mostra.

> [!DEFINE] Tabela de partições
> A lista guardada no início de um disco que indica onde cada partição começa e termina.

> [!DEFINE] Sistema de arquivos
> A forma como uma partição guarda seus arquivos: uma tabela de nomes, pastas e dados. Cada partição tem uma, e uma ferramenta chamada mkfs cria uma vazia.

> [!DEFINE] Montar
> Anexar um sistema de arquivos a uma pasta, para que os seus arquivos apareçam dentro dessa pasta.

> [!DEFINE] Ponto de montagem
> A pasta à qual um sistema de arquivos está anexado.

> [!DEFINE] Sistema de arquivos raiz
> O sistema de arquivos onde todo o novo sistema vive. O sistema o vê como a sua pasta de topo, /.

## Se algo der errado

> [!PROBLEM] Seu disco não está na tabela do lsblk.
> Somente um disco num dos slots de disco do computador, ou na sua baia de troca a quente, é um dispositivo. Coloque o disco ali e execute lsblk de novo.

> [!PROBLEM] O disco tem um nome diferente do que você esperava.
> A letra segue o slot onde o disco está. Use o nome que lsblk mostra para ele em todos os comandos seguintes.

> [!SEE] jsc:disks, jsc:case_front, jsc:server_racks
