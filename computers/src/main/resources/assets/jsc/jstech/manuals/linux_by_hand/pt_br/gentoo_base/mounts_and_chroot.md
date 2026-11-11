---
section: gentoo_base
order: 321
---
# Montagens e chroot

O novo sistema fica no seu disco, não na mídia. Para acessá-lo, você monta as partições do disco em pastas da mídia ao vivo ([](jsc:mounting)). A partição raiz vai para /mnt/gentoo.

Depois você entra no novo sistema com chroot ([](jsc:arch_chroot)). O chroot faz /mnt/gentoo parecer a pasta raiz /, então todo comando que você digita roda no novo sistema.

1. Digite mount /dev/sda2 /mnt/gentoo e pressione [[Enter]].
2. Só para UEFI (Unified Extensible Firmware Interface), digite mount /dev/sda1 /mnt/gentoo/efi e pressione [[Enter]]. Essa partição é a partição de sistema EFI (ESP).
3. Digite mount --types proc /proc /mnt/gentoo/proc e pressione [[Enter]].
4. Faça o bind das outras três pastas da mesma forma: digite mount --rbind /sys /mnt/gentoo/sys e depois faça o mesmo com /dev e /run.
5. Digite chroot /mnt/gentoo /bin/bash e pressione [[Enter]].
6. Digite source /etc/profile e pressione [[Enter]]. O comando não imprime nada.

As pastas /proc, /sys, /dev e /run não são arquivos do seu disco. O kernel as preenche com informações ao vivo sobre a máquina, e o novo sistema precisa delas para acessar seus dispositivos. Um bind mostra uma pasta em um segundo lugar, para que o novo sistema também a veja.

Depois que você entra, o prompt começa com (chroot), e os comandos funcionam no novo sistema. Digite exit para sair, e o shell da própria mídia ao vivo volta.

## Se algo der errado

> [!PROBLEM] mount diz "wrong fs type, bad option, bad superblock" para a raiz.
> A partição raiz ainda não está formatada como ext4. Formate-a primeiro: mkfs.ext4 /dev/sda2 ([](jsc:filesystems_and_mkfs)).

> [!PROBLEM] mount diz "mount point does not exist" para /mnt/gentoo/efi.
> Monte a partição raiz primeiro. A partição de sistema EFI fica dentro dela.

> [!PROBLEM] mount diz "wrong fs type" para a partição EFI.
> Formate essa partição primeiro: mkfs.fat -F 32 /dev/sda1.

> [!PROBLEM] mount diz "mount point does not exist" ao fazer o bind de uma pasta.
> Monte a partição raiz primeiro, depois faça o bind das pastas.

> [!PROBLEM] chroot diz "cannot change root directory" e mostra o caminho.
> Digite o caminho /mnt/gentoo exatamente, com o disco montado ali.

> [!PROBLEM] chroot diz "failed to run command '/bin/bash': No such file or directory".
> O estágio 3 ainda não foi descompactado. Descompacte-o em /mnt/gentoo primeiro ([](jsc:stage_three)).

> [!PROBLEM] bash diz "arch-chroot: command not found".
> O Gentoo não tem arch-chroot. Entre no sistema com chroot /mnt/gentoo /bin/bash.

> [!SEE] jsc:arch_chroot, jsc:mounting, jsc:stage_three, jsc:gentoo_reboot
