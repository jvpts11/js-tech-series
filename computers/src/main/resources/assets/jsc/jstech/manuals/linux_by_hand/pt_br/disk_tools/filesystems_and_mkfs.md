---
section: disk_tools
order: 306
---
# Dando um sistema de arquivos a uma partição

Uma partição é um espaço vazio até conter um sistema de arquivos, que decide como os seus arquivos são armazenados. O root do novo sistema vai em um sistema de arquivos ext4 (o quarto sistema de arquivos estendido), e a Partição de sistema EFI recebe um sistema de arquivos FAT (File Allocation Table).

Cada comando abaixo cria um sistema de arquivos, e apaga o que havia na partição antes. Crie um sistema de arquivos apenas na partição que você dividiu para ele.

## A raiz: mkfs.ext4

ext4 é o sistema de arquivos onde os sistemas Linux mantêm a raiz deles. O comando mkfs.ext4 o cria, com o nome da partição depois dele: mkfs.ext4 /dev/sda2.

A partição que você nomeia se torna a raiz. Só existe uma raiz por vez: um segundo mkfs.ext4 em outra partição torna essa a raiz. Uma raiz que estava montada é desmontada pela nova formatação.

Criar o sistema de arquivos leva tempo, e isso depende do disco. Um disco rígido leva 140 ticks, o que equivale a 7 segundos. Uma unidade de estado sólido (SSD) leva 35 ticks, e um disco NVMe (memória não volátil expressa) leva 24 ticks, o mínimo que ele pode levar. 20 ticks equivalem a um segundo ([](jsc:units_and_ticks)).

A ferramenta imprime estas linhas enquanto trabalha, nesta ordem:

Table: O que o mkfs.ext4 imprime

| Linha | O que informa |
| --- | --- |
| `mke2fs 1.47.1 (20-May-2024)` | A versão do mke2fs, o programa por trás do mkfs.ext4 que cria o ext4. |
| `Creating filesystem with N 4k blocks and M inodes` | Quantos blocos de 4 kibibytes (KiB) e quantos inodes o sistema de arquivos tem, o que depende do seu tamanho. |
| `Filesystem UUID: followed by an identifier` | O identificador deste sistema de arquivos, um identificador único universal (UUID), o mesmo sempre para aquele disco. |
| `Superblock backups stored on blocks:` | Impresso apenas em sistemas de arquivos maiores que 128 megabytes (MB): os números dos blocos que guardam cópias de reserva do cabeçalho. |
| `Allocating group tables:` | Uma barra de contagem, que termina com done. |
| `Writing inode tables:` | Uma barra de contagem, que termina com done. |
| `Creating journal (N blocks):` | Impresso quando o sistema de arquivos tem um journal, e depois done. |
| `Writing superblocks and filesystem accounting information:` | A última etapa, uma barra de contagem, que termina com done. |

Em um disco inteiro sem tabela de partições, o comando também funciona. É o jeito das máquinas mais antigas, que começam pelo disco inteiro e não têm partição de sistema EFI ([](jsc:eras)).

## A partição de sistema EFI: mkfs.fat

Digite mkfs.fat -F 32 /dev/sda1, com o nome da partição de sistema EFI. A ferramenta não confere o -F 32, então digite-o mesmo assim, como mostrado.

A ferramenta espera um pouco e depois imprime uma linha e nada mais: "mkfs.fat 4.2 (2021-01-31)". Leva 17 ticks em um disco rígido e 8 ticks em um SSD ou NVMe. A partição fica formatada e não montada, então você a monta depois, por conta própria.

> [!DEFINE] ext4
> O sistema de arquivos Linux que o sistema de arquivos raiz usa. O programa mke2fs o cria.

> [!DEFINE] FAT
> O sistema de arquivos simples que o firmware lê da partição de sistema EFI. Também é chamado de FAT32 ou vfat.

> [!DEFINE] Bloco
> A unidade em que o ext4 guarda os dados: 4 KiB, que são 4.096 bytes.

> [!DEFINE] Inode
> Um registro para cada arquivo ou pasta, que diz o que ele é e onde estão os seus dados. Seu número é fixado quando o sistema de arquivos é criado.

> [!DEFINE] Superbloco
> O cabeçalho de um sistema de arquivos, com seu tamanho e layout. Os backups são cópias de reserva dele.

> [!DEFINE] Journal
> Uma pequena área onde o ext4 grava cada alteração antes de torná-la definitiva, para que uma falha não danifique o sistema de arquivos.

> [!DEFINE] UUID
> Um identificador universalmente único, um número longo que nomeia um sistema de arquivos. A tabela de arquivos do novo sistema, /etc/fstab, pode nomear um sistema de arquivos pelo seu UUID, então o nome do dispositivo do disco não importa.

## Se algo der errado

> [!PROBLEM] Uso: mkfs.ext4 /dev/<dispositivo>
> Você não nomeou uma partição. Digite mkfs.ext4 /dev/sda2, com o nome que o lsblk deu a ela.

> [!PROBLEM] mke2fs: Arquivo ou diretório inexistente ao tentar determinar o tamanho do sistema de arquivos
> Essa partição ainda não existe. Crie-a com fdisk antes, ou confira o número dela no lsblk.

> [!PROBLEM] mke2fs: sda1 é a partição de sistema EFI.
> Essa partição guarda o gerenciador de inicialização, então não é para ext4. Formate-a com mkfs.fat -F 32 /dev/sda1, e dê ao mkfs.ext4 a outra partição.

> [!PROBLEM] /dev/sda contém uma tabela de partições gpt.
> Você nomeou o disco inteiro, e ele tem uma tabela de partições. Use uma das partições dele, como /dev/sda2.

> [!PROBLEM] Uso: mkfs.fat -F 32 /dev/<partição>
> Você não nomeou uma partição. Digite mkfs.fat -F 32 /dev/sda1, com o nome que o lsblk deu a ela.

> [!PROBLEM] mkfs.fat: não foi possível abrir sda1: Arquivo ou diretório inexistente
> Essa partição não existe. Crie-a com fdisk antes, ou confira o número dela no lsblk.

> [!PROBLEM] mkfs.fat: sda1 não é uma partição de sistema EFI.
> Defina o tipo primeiro: fdisk /dev/sda, depois digite t, depois digite 1 para EFI System, depois digite w. Em seguida, execute mkfs.fat de novo.

> [!SEE] jsc:firmware, jsc:disks, jsc:units_and_ticks
