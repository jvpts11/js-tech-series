---
section: system_v_desktop
order: 353
---
# CDE no System V

A CDE, o Common Desktop Environment, é a única área de trabalho que o UNIX System V tem. É uma área de trabalho com janelas e um painel, e precisa de um computador Legacy ou mais novo. Um computador Vintage roda o sistema apenas em um prompt.

A CDE é um programa, então você a instala a partir da sua própria mídia, um CD. Depois de instalada, o computador inicia nela após uma reinicialização. O resto da CDE, o seu painel, os seus menus e as suas ferramentas, é explicado em [](jsc:cde).

```table
{"caption": "A CDE no System V em resumo", "rows": [[{"words": "Era de hardware necessária"}, {"words": "Legacy ou mais novo"}], [{"words": "Espaço no disco"}, {"amount": 32, "unit": "MB"}], [{"words": "Memória enquanto roda"}, {"amount": 16, "unit": "MB"}], [{"words": "Mídia de instalação"}, {"words": "CD, lido por uma Unidade de CD"}], [{"words": "Instalada com"}, {"words": "installpkg cde"}], [{"words": "Sistemas que a executam"}, {"words": "UNIX System V, FreeBSD e Linux"}]]}
```

1. Coloque o CD da CDE em uma Unidade de CD conectada ao computador.
2. Digite installpkg cde e pressione Enter.
3. Espere a instalação terminar. A sua linha diz o nome do programa e do CD.
4. Reinicie o computador. A área de trabalho inicia na próxima inicialização.

A área de trabalho tem um painel elevado no centro inferior da tela, com quatro espaços de trabalho chamados One, Two, Three e Four. Os seus programas incluem um gerenciador de arquivos, que o painel chama de Arquivos, um editor de texto, um terminal, uma calculadora, um medidor de desempenho, o Gerenciador de Estilos e o Visualizador de Ajuda.

## Se algo der errado

> [!PROBLEM] A instalação diz "A CDE precisa de hardware Legacy ou mais novo. Este computador é Vintage."
> Use um computador Legacy ou um mais novo. Um computador Vintage roda o System V apenas em um prompt.

> [!PROBLEM] A instalação diz "A CDE precisa de 32 MB livres no disco. Há <n> MB."
> Libere espaço em disco, ou instale em um disco maior.

> [!PROBLEM] A linha do installpkg diz "nenhuma mídia de pacote em uma unidade".
> Coloque o CD da CDE em uma Unidade de CD conectada ao computador e tente de novo.

> [!PROBLEM] O computador inicia em um prompt depois que a CDE é instalada.
> A área de trabalho só aparece depois de uma reinicialização. Reinicie o computador.

> [!SEE] jsc:cde, jsc:desktops, jsc:installpkg
