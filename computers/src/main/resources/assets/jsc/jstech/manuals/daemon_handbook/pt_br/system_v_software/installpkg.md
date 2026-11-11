---
section: system_v_software
order: 351
---
# Instalando um pacote da mídia

Os programas do UNIX System V vêm em mídia própria, e você os instala com installpkg. Não há gerenciador de pacotes nem Mirror. O programa está na mídia, e a mídia precisa estar em uma unidade vinculada ao computador ([](jsc:drives)).

Sem indicar nada, o installpkg pega o programa da mídia na unidade, como o sistema real funcionava: a mídia é a pergunta e a resposta. Com mais de uma mídia inserida, ele lista os programas e pede que você diga qual, pelo nome.

1. Coloque a mídia do programa em uma unidade vinculada ao computador.
2. Digite installpkg e pressione Enter.
3. Se o sistema listar vários pacotes, digite installpkg e o nome do que você quer.
4. Espere enquanto o programa é configurado, e depois digite o nome do comando dele para executá-lo.

Table: O que a tela diz

| O que você vê | O que significa |
| --- | --- |
| `installpkg: no package medium in a drive` | Nenhuma mídia está em uma unidade vinculada. Coloque a mídia do programa em uma unidade. |
| `installpkg: <name>: no such package on the media in the drives` | O nome não está em nenhuma das mídias das unidades. |
| `The following packages are on the media in the drives:` | Há mais de uma mídia inserida. Digite installpkg e o nome que você quer. |
| `Installing the <name> package.` | A instalação começou. A linha de setup indica o programa e a mídia. |
| `This computer is still setting up <name>.` | Um setup anterior ainda está em execução. Espere ele terminar. |
| `<name> is already installed.` | O programa já está neste computador. |
| `<name> needs <era> hardware or newer. This computer is <era>.` | O computador é mais antigo que o programa. Use um computador mais novo. |
| `<name> runs on <list>, not on <system>.` | O programa foi escrito para outro sistema. |
| `<name> needs <n> MB free on the disk. There are <m> MB.` | O disco tem pouco espaço livre para o programa. |
| `<name> cannot install on this computer's system or hardware.` | O programa não se encaixa neste sistema ou neste hardware. |

A linha de Setup indica a mídia: o disquete, o disco compacto (CD), o disco digital versátil (DVD), o pen drive USB (Universal Serial Bus) ou o disco Blu-ray.

Um programa escrito para uma era mais nova que a do seu computador é recusado, com a linha sobre o hardware dele. Um programa escrito para a era Vintage, em disquete, não tem regra de era.

Os programas instalados vão para /usr/bin. Enquanto a mídia de um programa estiver em uma unidade, seus arquivos aparecem em /mnt com a letra da unidade.

```command
id: jsc:command/unix/installpkg
```

## Se algo der errado

> [!PROBLEM] A tela diz "installpkg: nenhuma mídia de pacote em uma unidade".
> Insira a mídia em que o pacote veio, e tente de novo.

> [!PROBLEM] A tela diz "installpkg: <name>: nenhum pacote com esse nome na mídia das unidades".
> Digite apenas installpkg para ver a lista de pacotes, e depois digite o nome exatamente como a lista mostra.

> [!PROBLEM] Este computador ainda está configurando <name>.
> Espere o primeiro setup terminar e digite o comando de novo.

> [!PROBLEM] A tela diz "<name> precisa de hardware <era> ou mais recente."
> Use um computador dessa era ou de uma mais nova. A linha indica a era que o programa precisa.

> [!SEE] jsc:programs, jsc:drives, jsc:installing
