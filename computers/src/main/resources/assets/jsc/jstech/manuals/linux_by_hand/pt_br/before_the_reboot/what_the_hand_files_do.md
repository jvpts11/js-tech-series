---
section: before_the_reboot
order: 330
---
# Quais arquivos digitados chegam ao sistema instalado

Boa parte de uma instalação manual acontece no shell ao vivo, em arquivos que você digita ou edita: a tabela, o nome, o kernel, a lista do carregador de inicialização. O sistema instalado não recebe todos eles. A lista de verificação da reinicialização registra os passos que você executou, e no Gentoo ela também lê a tabela que você escreveu.

Enquanto você trabalha, os arquivos que você digita vão para a pasta raiz do novo sistema na sessão ativa: /mnt no Arch, e /mnt/gentoo no Gentoo.

## O que chega ao disco

Table: O que a instalação copia para o disco

| Arquivo | Copiado para o disco? |
| --- | --- |
| O nome da máquina | Sim: o nome que você escolheu. |
| Os pacotes que você pediu | Sim. |
| `/etc/fstab` | Sim: a tabela do que montar. |
| `/etc/portage/make.conf` | Sim: as opções de compilação (Gentoo). |
| O arquivo de kernel que você instalou (/boot/vmlinuz-6.11.5-gentoo-x86_64 no Gentoo, /boot/vmlinuz-linux no Arch) | Não: ele não é copiado. |
| `/boot/grub/grub.cfg` | Não: a lista do carregador de inicialização não é copiada. |

Depois, a instalação grava os próprios arquivos de inicialização no disco: uma pasta chamada boot, e o arquivo de kernel boot/vmlinuz dentro dela. Ela os grava independentemente do que você digitou, para que a máquina possa iniciar mesmo que o arquivo de kernel que você instalou não seja copiado.
