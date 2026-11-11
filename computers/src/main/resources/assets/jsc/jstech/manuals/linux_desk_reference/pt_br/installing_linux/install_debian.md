---
section: installing_linux
order: 287
---
# Instalando o Debian

O Debian tem seu próprio instalador, intitulado "Debian installer". Você o inicia pela mídia de instalação, como faz com o Ubuntu. É uma janela cinza com uma aba na borda superior que traz o título da página.

A linha de baixo mostra as teclas a usar. Leia essa linha com atenção: algumas das teclas que ela cita não fazem nada aqui.

O instalador passa por suas páginas em ordem, e ENTER avança de cada página. A página de software só aparece quando um Mirror responde.

Table: As páginas do instalador do Debian

| Página | Título na página | O que você faz |
| --- | --- | --- |
| Disco | Particionar discos | Escolha o disco e depois pressione ENTER. |
| Nome | Configurar a rede | Digite o nome do computador e depois pressione ENTER. |
| Área de trabalho | Seleção de software | Escolha uma área de trabalho ou nenhuma, e depois pressione ENTER. |
| Cópia | Instalando o sistema base | Espere pelas etapas: descompactação, a entrada de inicialização e o nome de host. |
| Concluído | Concluir a instalação | Retire a mídia de instalação e depois pressione ENTER para reiniciar. |

A página de nome tem o título "Configurar a rede", que é o texto do próprio instalador. O campo dessa página é o nome do computador, e o nome vira o nome de host.

A linha de baixo diz "<Tab> move; <Espaço> seleciona; <Enter> ativa os botões". No Debian, Tab e Espaço não fazem nada. Use UP e DOWN para mover, e ENTER para escolher.

1. Inicialize o computador a partir da mídia de instalação.
2. Escolha o disco e depois pressione ENTER.
3. Digite o nome do computador e depois pressione ENTER.
4. Escolha uma área de trabalho ou nenhuma, e depois pressione ENTER.
5. Espere a cópia terminar.
6. Retire a mídia de instalação e depois pressione ENTER para reiniciar.

## Se algo der errado

> [!PROBLEM] A página de software não aparece.
> Nenhum Mirror responde. Conecte o computador a uma rede cujo Mainframe execute o Mirror, e depois adicione uma área de trabalho após a primeira inicialização com apt install gnome ([](jsc:apt)).

> [!PROBLEM] A linha de baixo diz "<Tab> move", mas Tab não faz nada.
> Use UP e DOWN para mover entre as opções, e ENTER para escolher.

> [!SEE] jsc:installing_linux_keys, jsc:first_boot
