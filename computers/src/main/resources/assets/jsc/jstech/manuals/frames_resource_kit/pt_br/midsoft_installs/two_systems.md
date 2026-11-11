---
section: midsoft_installs
order: 200
icon: jsc:cd_rom
---
# Dois sistemas em um computador

Por que escolher? Instale um segundo sistema em outro disco do mesmo computador, e você terá os dois.

A menos que o menu de inicialização do servidor esteja desligado, cada início de um computador com dois sistemas para num gerenciador de inicialização, para que você escolha qual sistema iniciar: o Gerenciador de Inicialização Midsoft no Frames, o GRUB no Linux, o carregador próprio do FreeBSD. Com um sistema só, uma máquina Frames segue direto ([](jsc:midsoft_boot_manager)). A ordem de inicialização no firmware define qual disco o firmware tenta primeiro ([](jsc:firmware)).

O instalador propõe um disco padrão para o novo sistema: o primeiro disco sem sistema nenhum. Quando todos os discos têm um sistema, ele propõe o primeiro disco. Um disco guarda até 16 sistemas, desde que tenha espaço para cada um.

O novo sistema passa a ser o que o computador inicia por padrão. Instalar um sistema que o disco já tem não adiciona uma segunda cópia: a instalação reescreve o gerenciador de inicialização do sistema, que é como um sistema danificado é reparado.

Uma configuração do servidor pode desligar o menu de inicialização, então o computador inicia o sistema padrão de imediato, sem lista. A configuração é boot.show_boot_menu.

> [!DEFINE] Gerenciador de inicialização
> O programa que lista os sistemas de um computador, para você escolher um para iniciar.

> [!DEFINE] Gerenciador de Inicialização Midsoft
> A lista de sistemas que uma máquina Frames mostra ao iniciar, para você escolher um.

> [!DEFINE] Ordem de inicialização
> A ordem em que o firmware tenta os discos quando o computador inicia. O primeiro disco com um sistema nele é o que inicia.

## Se algo der errado

> [!PROBLEM] O computador inicia no sistema errado.
> Escolha o sistema que quer no Gerenciador de Inicialização Midsoft, ou mude a ordem de inicialização no firmware ([](jsc:firmware)).

> [!PROBLEM] A tela diz "O disco de destino não tem espaço para", seguido do nome do segundo sistema e dos megabytes que ele precisa.
> Instale em outro disco com espaço, ou em um disco maior ([](jsc:disks)).

> [!PROBLEM] O computador inicia de imediato, sem lista, embora tenha dois sistemas.
> A configuração do menu de inicialização do servidor está desligada. Ligue-a, com a configuração boot.show_boot_menu.

> [!SEE] jsc:midsoft_boot_manager, jsc:installing, jsc:firmware
