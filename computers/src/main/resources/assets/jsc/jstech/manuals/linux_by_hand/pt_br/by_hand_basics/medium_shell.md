---
section: by_hand_basics
order: 303
---
# Os comandos da mídia e Ctrl+C

O shell da mídia de instalação responde a uma lista fixa de comandos. A maioria são as ferramentas reais da instalação, com os nomes reais. Alguns, como help, exit e reboot, são do próprio meio.

Uma palavra que o shell não conhece recebe a recusa do sistema ao qual o shell pertence: bash no Gentoo, zsh no Arch Linux. Cada sistema tem os seus próprios comandos, então um comando do outro sistema é recusado.

Table: Comandos dos dois sistemas

| Grupo | Comandos |
| --- | --- |
| Arquivos e pastas | `ls, cat, less, more, cd, echo, nano, mkdir` |
| Discos | `lsblk, blkid, fdisk, mkfs, mkfs.ext4, mkfs.fat, mkfs.vfat, mount, umount` |
| Entrar no novo sistema | `chroot` |
| Configurações | `source, export, env-update, hwclock, hostname` |
| Iniciar programa | `grub-install, grub-mkconfig` |
| Os próprios da mídia | `help, exit, reboot` |

Table: Comandos de um sistema

| Sistema | Comandos próprios |
| --- | --- |
| Arch Linux | `pacstrap, genfstab, pacman, mkinitcpio, arch-chroot` |
| Gentoo | `wget, tar, emerge-webrsync, emerge, eselect, genkernel, make` |

O prompt mostra onde você está. Fora do novo sistema, ele mostra o nome da mídia, e dentro do novo sistema ele muda conforme a tabela mostra. O ~ é a pasta própria da raiz, e qualquer outra pasta mostra o seu próprio caminho.

Table: O prompt

| Sistema | Fora do novo sistema | Dentro do novo sistema |
| --- | --- | --- |
| Arch Linux | `root@archiso ~ #` | `[root@archiso ~]#` |
| Gentoo | `livecd ~ #` | `(chroot) livecd ~ #` |

## Entrar e sair do novo sistema

Você entra no novo sistema com chroot em qualquer um dos dois sistemas, ou com arch-chroot somente no Arch Linux. Uma vez dentro, os comandos e os arquivos que você usa são os do novo sistema. Digite exit para sair. Fora do novo sistema, exit imprime logout.

## O editor de partições

fdisk abre um editor de partições que toma conta do terminal. Cada comando é uma letra, com um valor opcional depois dela. Você digita o comando sozinho em uma linha e pressiona [[Enter]]. O capítulo [Discos](jsc:preparing_disks) leva você por uma sessão completa neste editor.

Table: Comandos do editor de partições

| Comando | O que faz |
| --- | --- |
| `m` | Mostra a ajuda do editor, o mesmo que digitar help. |
| `g` | Cria uma nova tabela de partições vazia (GUID Partition Table, GPT). |
| `p` | Mostra a tabela como ela está agora. |
| `n` | Cria uma nova partição, e n 512M dá a ela 512 MB na mesma linha. |
| `t` | Define o tipo de uma partição: t 1 a marca para UEFI. |
| `d` | Apaga uma partição. |
| `w` | Grava a tabela no disco, o único comando que altera o disco. |
| `q` | Sai sem gravar, e descarta tudo o que foi digitado desde que o editor foi aberto. |

## Ctrl+C

Pressione [[Ctrl]]+[[C]] para parar a ferramenta que está rodando. O prompt volta, e a parte do trabalho que a ferramenta não terminou fica por fazer.

Sem nenhuma ferramenta rodando, [[Ctrl]]+[[C]] não faz nada. As ferramentas que demoram são pacstrap, pacman, emerge, emerge-webrsync, genkernel, make, wget, tar, grub-install, grub-mkconfig, fdisk e as ferramentas mkfs. A saída delas aparece enquanto rodam.

> [!WARNING] No fdisk, [[Ctrl]]+[[C]] descarta tudo o que foi digitado desde que o editor foi aberto, do mesmo jeito que q. Só w grava o disco.

## Se algo der errado

> [!PROBLEM] bash: pacstrap: comando não encontrado (Gentoo), ou zsh: comando não encontrado: wget (Arch Linux).
> A palavra não é um comando do seu sistema. Confira a lista de comandos acima: pacstrap é do Arch Linux, e wget é do Gentoo.

> [!PROBLEM] emerge: comando não encontrado, e a mensagem diz que o gerenciador de pacotes é do novo sistema.
> Entre primeiro no novo sistema, com chroot /mnt/gentoo /bin/bash, e então digite emerge lá ([o capítulo Gentoo](jsc:gentoo)).

> [!PROBLEM] mkinitcpio: comando não encontrado, e a mensagem diz que ele é do novo sistema.
> Entre primeiro no novo sistema, com arch-chroot /mnt, e então digite mkinitcpio ([o capítulo Arch](jsc:arch)).

> [!PROBLEM] grub-install: comando não encontrado, e a mensagem diz que ele é do novo sistema.
> Entre primeiro no novo sistema e depois digite o comando de novo.

> [!PROBLEM] chroot: não foi possível mudar o diretório raiz para '/mnt': Arquivo ou diretório inexistente
> A pasta não é a raiz do novo sistema. Monte primeiro a partição raiz em /mnt no Arch Linux, ou em /mnt/gentoo no Gentoo ([o capítulo Discos](jsc:preparing_disks)).

> [!PROBLEM] chroot: falha ao executar o comando '/bin/bash': Arquivo ou diretório inexistente
> O sistema base ainda não está instalado. Instale-o primeiro, com pacstrap no Arch Linux ou com wget e tar no Gentoo.

> [!PROBLEM] fdisk: não foi possível abrir /dev/sdc: Arquivo ou diretório inexistente
> Não existe esse disco. Digite lsblk para listar os discos que o shell consegue ver.

> [!PROBLEM] nano: /mnt/gentoo é um diretório
> nano abre arquivos, não pastas. Coloque o nome de um arquivo depois de nano.

> [!SEE] jsc:installing, jsc:firmware_for_install
