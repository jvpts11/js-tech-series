---
section: dos_basics
order: 204
---
# Instalando o MC-DOS

MC-DOS é o sistema Vintage que funciona no prompt ([](jsc:the_systems)). Você o instala somente a partir de uma mídia: um disquete, colocado em uma Unidade de Disquete. O instalador copia o sistema para um disco que você escolhe, e depois o computador inicia a partir desse disco.

```table
{"caption": "O que você precisa", "rows": [[{"words": "Mídia de instalação"}, {"words": "O disquete do MC-DOS, das abas do modo criativo."}], [{"words": "Unidade"}, {"words": "Uma Unidade de Disquete ligada ao computador ([](jsc:drives))."}], [{"words": "Espaço no disco"}, {"words": "4 megabytes (MB) livres no disco onde você instala."}], [{"words": "Memória de acesso aleatório (RAM)"}, {"words": "1 megabyte (MB) enquanto o MC-DOS executa."}]]}
```

O firmware é o programa na placa-mãe que inicia o computador ([](jsc:firmware)).

1. Coloque o disquete do MC-DOS na Unidade de Disquete.
2. Inicie o computador com o disquete na unidade, como explica a página de instalação ([](jsc:installing)).
3. Leia a página "Bem-vindo ao Setup." e pressione [[Enter]].
4. Mova o cursor até um disco com as teclas de seta. Digite um nome em "Nome do computador:" se quiser, depois pressione [[Enter]].
5. Espere a barra encher. A página diz "O Setup está copiando MC-DOS para o disco."
6. Leia "O Setup do MC-DOS foi concluído." e pressione [[Enter]] para reiniciar.

```table
{"caption": "As quatro páginas do Setup", "heads": ["Página", "Título na tela", "Teclas no rodapé"], "rows": [[{"words": "Bem-vindo"}, {"words": "Bem-vindo ao Setup."}, {"literal": "ENTER=Continue  F3=Exit"}], [{"words": "Configurações"}, {"words": "O Setup usará as seguintes configurações:"}, {"literal": "ENTER=Continue  F3=Exit"}], [{"words": "Cópia"}, {"words": "O Setup está copiando MC-DOS para o disco."}, {"words": "nenhuma"}], [{"words": "Concluído"}, {"words": "O Setup do MC-DOS foi concluído."}, {"literal": "ENTER=Restart"}]]}
```

A página de configurações lista os discos do computador. Continuar fica desativado até você escolher um disco que tenha pelo menos 4 megabytes (MB) livres. O Setup copia o sistema para o disco escolhido. Quando a cópia termina, o computador reinicia a partir desse disco.

Enquanto o computador inicia, a tela imprime uma linha para cada unidade, como Unidade C:, e depois a linha de versão, MC-DOS Versão 1.0. Então você está no prompt ([](jsc:dos_prompt)).

> [!DEFINE] Firmware
> O programa na placa-mãe que inicia o computador. Ele roda antes de qualquer sistema.

## Se algo der errado

> [!PROBLEM] Nada inicia a partir do disquete.
> A Unidade de Disquete não está ligada ao computador, ou o firmware não a encontra. Ligue a unidade, depois verifique o firmware ([](jsc:firmware)).

> [!PROBLEM] A tela diz "Sem espaço aqui. Apague este disco, ou escolha outro."
> O disco que você escolheu tem menos de 4 megabytes (MB) livres. Escolha outro disco que tenha espaço ([](jsc:disks)).

> [!PROBLEM] Continuar não faz nada na página de configurações.
> Nenhum disco foi escolhido ainda, ou o disco escolhido não tem espaço. Primeiro escolha um disco com espaço.

> [!PROBLEM] Pressionar [[F3]] não faz nada.
> A cópia já começou, e [[F3]] funciona somente antes dela. Espere a última página.

> [!SEE] jsc:dos_system_files, jsc:frames_which_machine
