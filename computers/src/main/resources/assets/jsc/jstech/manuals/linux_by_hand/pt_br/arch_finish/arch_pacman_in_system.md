---
section: arch_finish
order: 317
icon: jsc:cd_rom
---
# Instalando pacotes com pacman

Dentro do novo sistema, pacman -S instala programas do Mirror, para que estejam no novo sistema quando ele iniciar. Um programa instalado dessa forma é um pacote, com seus arquivos e os nomes do que ele precisa.

Digite pacman -S, um espaço, e depois os nomes dos programas que você quer. Cada nome é o nome do comando de um programa que o Mirror tem para Linux.

1. Confirme que você está dentro do novo sistema.
2. Digite pacman -S e os nomes, por exemplo pacman -S grub efibootmgr.
3. Responda à pergunta com Y ou pressione [[Enter]] para instalar, ou digite n para parar.

O pacman pergunta "Continuar com a instalação? [Y/n]" antes de alterar qualquer coisa. Uma resposta que começa com n o interrompe, e nada é instalado. Qualquer outra resposta, mesmo vazia, segue adiante.

```session
{"lines": [{"prompt": "[root@archiso /]# ", "text": "pacman -S efibootmgr", "typed": true}, {"text": ":: Proceed with installation? [Y/n]"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Cada programa que você nomear fica registrado, e é instalado no novo sistema quando a instalação terminar. O pacote grub também é o que o gerenciador de inicialização precisa ([](jsc:arch_bootloader)).

## pacman no sistema que você instalou

Quando o Arch Linux inicia do seu próprio disco, o pacman continua como gerenciador de pacotes. Assim, pacman -S instala um pacote de uma vez, e o pacman também tem comandos para pesquisar, listar, atualizar e remover os seus pacotes. Pesquisar, instalar e atualizar acessam o Mirror pela rede, enquanto listar e remover funcionam com os pacotes que já estão neste computador.

Table: O que o pacman faz no sistema instalado

| Palavras | O que fazem |
| --- | --- |
| `pacman -S <package>` | Instala o pacote do Mirror. |
| `pacman -Ss [term]` | Procura no Mirror pacotes cujos nomes contenham o termo. |
| `pacman -Q` | Lista os pacotes instalados neste computador. |
| `pacman -Sy` | Lê de novo as listas do Mirror. |
| `pacman -Syu` | Atualiza cada pacote instalado para a versão que o Mirror oferece. |
| `pacman -R <package>` | Remove o pacote. |
| `pacman -Qu` | Lista os pacotes instalados de que o Mirror tem uma versão mais nova, cada um com a versão que você tem e a nova. |

```command
id: jsc:command/linux/pacman
```

## Se algo der errado

> [!PROBLEM] A tela mostra "erro: alvo não encontrado:" e depois o nome que você digitou.
> O Mirror não tem nenhum programa com esse nome para Linux, ou esta máquina não consegue executá-lo. Confira a grafia e veja os programas do Mirror ([](jsc:programs)).

> [!PROBLEM] A tela mostra "erro: nenhum alvo especificado (use -h para ajuda)".
> Você digitou apenas opções, sem nome. Acrescente o nome de um programa depois de -S.

> [!PROBLEM] A tela mostra "erro: nenhuma operação especificada (use -h para ajuda)".
> O comando deve começar com -S e nomear um programa. Digite pacman -S, um espaço, e depois o nome.

> [!PROBLEM] A tela mostra "erro: você não pode realizar esta operação a menos que seja root no novo" e depois "sistema (arch-chroot /mnt primeiro)".
> Você está na mídia ao vivo. Entre primeiro com arch-chroot /mnt ([](jsc:arch_chroot)).

> [!PROBLEM] A tela mostra "erro: falha ao obter o arquivo 'core.db' de mirror://mainframe : Could not resolve host: mainframe".
> Este computador não consegue alcançar um Mirror. Coloque-o em uma rede cujo Mainframe execute o Mirror ([](jsc:mirror)).

> [!SEE] jsc:programs
