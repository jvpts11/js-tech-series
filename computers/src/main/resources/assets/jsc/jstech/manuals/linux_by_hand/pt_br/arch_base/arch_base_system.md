---
section: arch_base
order: 309
icon: jsc:cd_rom
---
# Instalando o sistema base (pacstrap)

O sistema base é o núcleo do Arch Linux: o kernel ([](jsc:kernel_gentoo)), as ferramentas centrais e o gerenciador de pacotes, o pacman. O comando que o instala é o pacstrap. Ele coloca os pacotes no disco, em /mnt, a pasta onde o novo sistema é construído.

O pacstrap é a primeira etapa que grava o novo sistema, então o disco precisa estar pronto antes de ele rodar. A partição root precisa estar montada em /mnt ([](jsc:mounting)).

Em uma instalação real do Arch, o pacstrap baixa os pacotes da internet. Aqui, os pacotes vêm do Mirror, o servidor de pacotes da sua rede. Um Mainframe precisa executá-lo, e este computador precisa estar nessa rede. Sem um Mirror, o pacstrap não consegue iniciar.

> [!DEFINE] pacman
> O gerenciador de pacotes do Arch Linux: ele encontra, baixa e instala pacotes.

> [!DEFINE] Mirror
> O servidor de pacotes de uma rede. Um Mainframe que o execute serve pacotes a todos os computadores da rede.

1. Confirme que a partição root está montada em /mnt.
2. Digite pacstrap -K /mnt base linux linux-firmware e pressione [[Enter]].
3. Aguarde até o prompt voltar. A etapa leva o tempo que sua conexão e seu disco precisarem.

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "pacstrap -K /mnt base linux linux-firmware", "typed": true}, {"text": "==> Creating install root at /mnt"}, {"text": "==> Installing packages to /mnt"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Depois vêm as barras de download, as verificações dos pacotes, uma linha para cada pacote que ele instala, e as últimas etapas da instalação. O pacstrap não pergunta nada: ele não para para pedir confirmação, e instala de imediato.

```table
{"caption": "O que o sistema base traz", "rows": [[{"words": "Pacotes instalados"}, {"words": "48"}], [{"words": "Versão do kernel"}, {"words": "6.11.5-arch1-1"}]]}
```

Os 48 pacotes incluem base, linux, linux-firmware, pacman, systemd, bash, coreutils e mkinitcpio. As últimas etapas da instalação também constroem as duas imagens de inicialização, então você não precisa construí-las de novo.

Quando o pacstrap termina, o novo sistema tem suas pastas /etc, /boot, /root, /usr, /var, /proc, /sys, /dev e /run. O kernel dele e suas duas imagens de inicialização ([](jsc:arch_mkinitcpio)) ficam em /boot. A etapa só é registrada como concluída nesse momento.

Executar o pacstrap de novo recomeça do zero. As etapas que você fez antes, como a tabela do sistema de arquivos e as etapas de inicialização, são esquecidas, e você as faz de novo. A configuração do relógio é mantida.

Você pode interromper o pacstrap com [[Ctrl]]+[[C]].

## Se algo der errado

> [!PROBLEM] A tela mostra "==> ERRO: /mnt não é um ponto de montagem!".
> Nada está montado em /mnt. Monte a partição root ali, depois que o sistema de arquivos dela for criado ([](jsc:mounting)): mount /dev/sda2 /mnt.

> [!PROBLEM] A tela mostra "erro: falha ao obter o arquivo 'core.db' de mirror://mainframe : Could not resolve host: mainframe", e depois "erro: falha ao sincronizar todas as bases de dados (erro inesperado)".
> Este computador não consegue alcançar um Mirror. Coloque-o em uma rede cujo Mainframe execute o Mirror ([](jsc:mirror)).

> [!PROBLEM] Você interrompeu o pacstrap com [[Ctrl]]+[[C]] antes de ele terminar.
> A etapa não foi registrada como concluída. Execute o pacstrap de novo desde o início.

> [!SEE] jsc:arch_fstab
