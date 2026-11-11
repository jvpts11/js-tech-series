---
section: gentoo_system
order: 325
---
# GRUB no Gentoo

O bootloader inicia o kernel que você compilou, e mostra o menu dos sistemas no disco. No Gentoo, o bootloader é o GRUB ([](jsc:arch_bootloader)). O GRUB é um pacote como qualquer outro, então você também o compila a partir do código-fonte.

O firmware decide onde o GRUB vai. A UEFI (Interface Unificada Extensível de Firmware) lê o GRUB a partir da partição de sistema EFI. A BIOS (sistema básico de entrada e saída) o encontra no próprio disco.

1. Digite emerge --ask sys-boot/grub e pressione [[Enter]]. Responda à pergunta com Enter.
2. Digite o comando de instalação do seu firmware, da tabela abaixo, e pressione [[Enter]].
3. Digite grub-mkconfig -o /boot/grub/grub.cfg e pressione [[Enter]].

Table: O comando de instalação de cada firmware

| Firmware | Comando |
| --- | --- |
| UEFI | `grub-install --efi-directory=/efi` |
| BIOS (firmware mais antigo) | `grub-install /dev/sda` |

A parte --efi-directory=/efi deve informar o lugar onde a partição de sistema EFI está montada no novo sistema. O passo de montagem a colocou em /mnt/gentoo/efi, que é /efi dentro do novo sistema.

O menu que o grub-mkconfig escreve tem uma entrada, chamada Gentoo Linux. Essa entrada inicia o kernel que você compilou, e informa ao kernel a sua partição root pelo seu UUID.

## Se algo der errado

> [!PROBLEM] O bash diz "grub-install: comando não encontrado".
> O GRUB ainda não foi compilado. Digite primeiro emerge --ask sys-boot/grub.

> [!PROBLEM] O grub-install diz "comando não encontrado". O programa pertence ao novo sistema e ainda não foi compilado ali.
> Entre primeiro no novo sistema: chroot /mnt/gentoo /bin/bash, depois execute o comando ali.

> [!PROBLEM] O grub-install diz "aviso: nenhuma imagem de kernel encontrada em /boot."
> Compile o kernel primeiro: genkernel all, ou make em /usr/src/linux ([](jsc:kernel_gentoo)).

> [!PROBLEM] O grub-install diz "erro: /boot/efi não parece uma partição EFI."
> A partição de sistema EFI não está montada onde o grub-install procura. Monte-a e, em seguida, informe o seu lugar com --efi-directory=/efi, como faz o passo a passo.

> [!PROBLEM] O grub-install diz "erro: dispositivo de instalação não especificado."
> O firmware BIOS precisa do disco informado. Digite grub-install /dev/sda.

> [!PROBLEM] O grub-install diz "erro: não foi possível encontrar um dispositivo para /dev/<dev>."
> O GRUB vai no disco, não em uma partição. Digite grub-install /dev/sda.

> [!PROBLEM] O grub-mkconfig diz "/boot/grub: Arquivo ou diretório inexistente".
> O GRUB ainda não está instalado. Execute primeiro o grub-install.

> [!SEE] jsc:arch_bootloader, jsc:arch_grub_config, jsc:gentoo_reboot
