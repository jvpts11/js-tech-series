---
section: sharing_packages
order: 497
---
# Instalando um pacote que outros jogadores publicaram

Os jogadores podem publicar pacotes para qualquer pessoa na rede. Você instala um pacote como qualquer outro programa: com o comando de instalação do gerenciador de pacotes do seu sistema e o nome do pacote, como apt install mypkg em um sistema Debian ou Ubuntu ([](jsc:software_commands)).

Instalar o pacote de um jogador exige o runtime do Sigma no seu computador ([](jsc:sigma_installing)). Sem ele, a instalação para e pede para instalar o sigma primeiro.

O jogo descompacta cada pacote em uma pasta própria: PROGRAMS, e depois o nome do pacote. A pasta guarda apenas os arquivos desse pacote, então dois pacotes não conseguem sobrescrever um ao outro. Depois, o pacote aparece na área de trabalho com o seu ícone, e abri-lo executa o programa.

1. Digite o comando de instalação do seu gerenciador de pacotes, com o nome do pacote.
2. Abra o programa pela área de trabalho.

Para remover um pacote, digite o comando de remoção do seu gerenciador de pacotes, com o nome dele, como apt remove mypkg. O jogo apaga exatamente os arquivos que a instalação gravou, depois a pasta, e responde removido mypkg.

## Se algo der errado

> [!PROBLEM] A instalação diz que este sistema instala programas a partir de mídia de instalação, não de um gerenciador de pacotes.
> Instale o programa a partir da mídia de instalação do sistema ([](jsc:installing)).

> [!PROBLEM] A instalação diz que a cópia deste pacote no Mirror não é legível.
> Peça ao jogador que o publicou para publicá-lo de novo a partir de um arquivo compilado.

> [!PROBLEM] A instalação diz que mypkg é um programa Σ#; instale o sigma primeiro.
> Instale o runtime do Sigma neste computador ([](jsc:sigma_installing)), e depois instale de novo.

> [!PROBLEM] A instalação diz que não há disco de sistema onde instalar.
> Coloque um disco de sistema no computador e instale de novo.

> [!PROBLEM] A instalação diz que este sistema não tem pastas onde instalar.
> Verifique o disco de sistema e instale de novo.

> [!SEE] jsc:software_commands, jsc:sigma_installing, jsc:sigma_sharing_errors
