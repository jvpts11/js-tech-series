---
section: arch_system
order: 311
icon: jsc:cd_rom
---
# Entrando no novo sistema (arch-chroot)

Até agora você digitou na mídia ao vivo, e seus comandos chegaram ao disco de fora. Para terminar o novo sistema, você entra nele. A partir daí, a pasta /mnt é o topo do sistema, e cada caminho que você digitar significa um caminho dentro do novo sistema.

A ferramenta do Arch para isso é o arch-chroot. Executar um programa com outra pasta como topo do disco se chama chroot, e o arch-chroot cria um para o novo sistema.

> [!DEFINE] chroot
> Executar um programa com outra pasta como topo do disco, para que o programa veja essa pasta como o sistema inteiro dele.

Fora do novo sistema, o prompt mostra root@archiso ~ #. Dentro, ele mostra [root@archiso /]#, com a pasta em que você está entre os colchetes.

1. Digite arch-chroot /mnt e pressione [[Enter]].
2. Confira o prompt. Ele deve mostrar [root@archiso /]#.
3. Faça as etapas deste capítulo lá dentro. Digite exit quando terminar.

Se você informar uma pasta diferente de /mnt, a ferramenta se recusa, e você fica do lado de fora:

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "arch-chroot /mnt/x", "typed": true}, {"text": "chroot: cannot change root directory to '/mnt/x': No such file or directory"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Para sair, use exit. Nada se perde ao sair: o trabalho que você fez permanece, e arch-chroot /mnt leva você de volta para dentro.

Table: Comandos que funcionam só dentro do novo sistema

| Comando | Na mídia ao vivo |
| --- | --- |
| `pacman` | Recusado: você ainda não é root no novo sistema. |
| `mkinitcpio` | Não encontrado: entre no sistema primeiro. |
| `grub-install` | Não encontrado: entre no sistema primeiro. |
| `grub-mkconfig` | Não encontrado: entre no sistema primeiro. |
| `hostname` | Recusado: você não pode alterar o nome da mídia ao vivo. |

## Se algo der errado

> [!PROBLEM] A tela mostra "chroot: não foi possível mudar o diretório raiz para '/mnt/x': Arquivo ou diretório inexistente".
> A pasta não é /mnt. Digite arch-chroot /mnt.

> [!PROBLEM] A tela mostra "chroot: falha ao executar o comando '/bin/bash': Arquivo ou diretório inexistente".
> O sistema base ainda não está no disco, então não há shell para iniciar. Instale-o primeiro ([](jsc:arch_base_system)).

> [!PROBLEM] A tela mostra "erro: você não pode realizar esta operação a menos que seja root no novo" e depois "sistema (arch-chroot /mnt primeiro)".
> Você digitou pacman na mídia ao vivo. Entre primeiro com arch-chroot /mnt.

> [!PROBLEM] A tela mostra "mkinitcpio: comando não encontrado (é do sistema novo: arch-chroot /mnt primeiro)".
> Você está na mídia ao vivo. Entre primeiro com arch-chroot /mnt.

> [!SEE] jsc:arch_clock_and_name
