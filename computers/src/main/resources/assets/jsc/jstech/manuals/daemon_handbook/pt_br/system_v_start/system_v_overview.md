---
section: system_v_start
order: 349
---
# O que é o UNIX System V

O UNIX System V é o mais antigo dos sistemas UNIX que você encontra em um prompt, e o que um computador da primeira era roda. A sua versão é 3.2. Você trabalha nele como em qualquer sistema UNIX: digita um comando, o sistema responde, e os seus arquivos ficam em pastas.

O sistema foi feito para uma máquina pequena. Ele ocupa 10 megabytes (MB) de disco, e guarda 2 megabytes (MB) de memória para si enquanto roda, mas roda vários programas ao mesmo tempo. Ele pega os seus programas de mídias e de nenhum outro lugar: não há Mirror de onde instalar, só mídias ([](jsc:programs)).

```table
{"caption": "UNIX System V em resumo", "rows": [[{"words": "Versão"}, {"words": "3.2"}], [{"words": "Era de hardware"}, {"words": "Vintage, a primeira era"}], [{"words": "Espaço no disco"}, {"amount": 10, "unit": "MB"}], [{"words": "Memória reservada para o sistema enquanto roda"}, {"amount": 2, "unit": "MB"}], [{"words": "Mídia de instalação"}, {"words": "Disquete, lido por uma Unidade de Disquete"}], [{"words": "Shell"}, {"words": "sh"}], [{"words": "Gerenciador de pacotes"}, {"words": "Nenhum: os programas vêm de mídias"}], [{"words": "Casa de software"}, {"words": "Bellwether Labs"}], [{"words": "Área de trabalho"}, {"words": "CDE (Common Desktop Environment), instalado como programa"}]]}
```

> [!DEFINE] Mídia de instalação
> Um item que carrega os arquivos de um sistema ou de um programa. Ele vai para uma unidade ligada ao computador, e o sistema lê dali.

Ao iniciar, o sistema imprime algumas linhas em ordem, e a última diz que o sistema está pronto.

```table
{"caption": "As linhas da inicialização", "heads": ["Linha", "O que ela informa"], "rows": [[{"literal": "UNIX System V Release 3.2"}, {"words": "O nome do sistema e a sua versão."}], [{"literal": "Copyright (c) 1984, 1986, 1987 Bellwether Labs"}, {"words": "O aviso da casa de software."}], [{"literal": "All Rights Reserved"}, {"words": "O fim do aviso."}], [{"literal": "Total real memory     = <n>"}, {"words": "A memória que o computador tem."}], [{"literal": "Available memory      = <n>"}, {"words": "A memória que o sistema pode usar, depois que o kernel reserva a sua parte."}], [{"literal": "The system is coming up.  Please wait."}, {"words": "O sistema está iniciando."}], [{"literal": "/dev/root: clean"}, {"words": "O disco do qual o sistema inicia passou na verificação."}], [{"literal": "The system is ready."}, {"words": "O prompt está pronto para o seu primeiro comando."}]]}
```

Ao desligar o sistema, ele imprime antes um aviso: "O SISTEMA ESTÁ SENDO DESLIGADO AGORA ! ! !", depois "Encerre a sessão agora ou arrisque que seus arquivos sejam danificados."

A última linha diz "O sistema está desligado.", ou "O sistema está sendo reiniciado." quando o sistema volta logo em seguida.

## Pastas

A sua pasta pessoal é /usr/player, e as pessoas do sistema ficam em /usr. A instalação cria bin, etc, mnt e tmp na raiz, e bin, lib e player dentro de usr. Dentro da sua pasta pessoal ela cria Desktop e Documents.

Os programas vão para /usr/bin. Quando uma unidade tem uma mídia, os arquivos dela aparecem em /mnt com a letra da unidade, como /mnt/d.

## Comandos

A maioria dos comandos do shell funciona aqui, e [o shell](jsc:shell_prompt) os explica. Dois comandos existem só neste sistema: installpkg, que instala um programa de uma mídia, e pace, o sistema de menus. Os dois têm as suas próprias entradas ([](jsc:installpkg), [](jsc:pace)).

> [!DEFINE] PACE
> O sistema de menus do UNIX System V, com os menus em molduras sobrepostas.

Table: Comandos que este sistema não tem

| Comando | Pertence a |
| --- | --- |
| `ee` | FreeBSD |
| `info` | Linux |
| `lspci, lsusb` | Linux |
| `pciconf, usbconfig` | FreeBSD |

## Se algo der errado

> [!PROBLEM] Você digita um comando que conhece de outro sistema, e o prompt não o reconhece.
> Confira a tabela acima: alguns comandos pertencem a outros sistemas. Use o comando que este sistema tem.

> [!PROBLEM] O sistema inicia em um prompt, e você espera uma área de trabalho.
> Uma área de trabalho é um programa que você instala. Em um computador Legacy ou mais recente, instale o CDE ([](jsc:cde_on_system_v)), depois reinicie o computador. A área de trabalho aparece na próxima inicialização.

> [!SEE] jsc:installing, jsc:the_systems, jsc:shell_prompt
