---
section: gentoo_system
order: 323
---
# O kernel

O kernel é a parte do sistema que controla o hardware. Todo programa chega ao processador, à memória e aos discos por meio dele. O Gentoo compila o seu kernel a partir dos códigos-fonte, então a instalação baixa primeiro os códigos-fonte.

Esta instalação usa o kernel na versão 6.11.5. Os arquivos dele levam o nome 6.11.5-gentoo, e o kernel comprimido vai para /boot.

1. Digite emerge sys-kernel/gentoo-sources e pressione [[Enter]].
2. Digite eselect kernel list e pressione [[Enter]] para ver os kernels.
3. Digite eselect kernel set 1 e pressione [[Enter]]. O comando não imprime nada.
4. Compile e instale o kernel por uma das duas rotas da tabela abaixo.

genkernel é um script que compila o kernel inteiro com um único comando. A rota manual roda em /usr/src/linux, a pasta que a escolha do kernel define.

> [!DEFINE] genkernel
> Um script que compila o kernel e os seus módulos com um único comando.

Table: As duas rotas

| Rota | Comando | O que faz |
| --- | --- | --- |
| Automática | `genkernel all` | Compila o kernel e os seus módulos, e copia o kernel para /boot. |
| Manual | `make -j4 && make modules_install && make install` | Compila em /usr/src/linux, instala os módulos e copia o kernel para /boot. |

Os módulos ficam em /lib/modules, em uma pasta com o nome da versão do kernel.

> [!DEFINE] módulo
> Uma parte do kernel que é carregada quando algo precisa dela, guardada como um arquivo próprio.

## Se algo der errado

> [!PROBLEM] genkernel diz "comando não encontrado", porque o comando está no novo sistema e você ainda está na mídia ao vivo.
> Entre primeiro no sistema novo: chroot /mnt/gentoo /bin/bash. O make precisa do mesmo passo.

> [!PROBLEM] O genkernel diz "\* ERRO: nenhum código-fonte do kernel encontrado em /usr/src".
> Baixe primeiro os códigos-fonte: emerge sys-kernel/gentoo-sources.

> [!PROBLEM] O eselect diz "!!! Erro: Nenhum código-fonte do kernel encontrado em /usr/src".
> Baixe primeiro os códigos-fonte: emerge sys-kernel/gentoo-sources.

> [!PROBLEM] O make diz que não tem alvos nem makefile.
> O kernel é compilado dentro dos seus códigos-fonte. Digite primeiro cd /usr/src/linux.

> [!PROBLEM] O make diz que não há regra para o alvo vmlinux.
> O kernel ainda não foi compilado. Digite make primeiro, depois a instalação.

> [!SEE] jsc:fstab_by_hand, jsc:gentoo_bootloader, jsc:package_tree_and_world
