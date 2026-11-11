---
section: installing_linux
order: 286
---
# Instalando o Ubuntu

O Ubuntu tem seu próprio instalador. Você o inicia pela mídia de instalação, o disco ou pen drive a partir do qual o sistema é instalado, depois de inicializar o computador a partir dessa unidade ([](jsc:firmware)). O instalador abre em uma janela intitulada "Ubuntu installer", com uma faixa no topo que traz o título da página e as palavras "[ Ajuda ]".

> [!DEFINE] Mídia de instalação
> O disco ou pen drive a partir do qual um sistema é instalado. O instalador é o programa que está nele.

O instalador passa por suas páginas em ordem, e ENTER avança de cada página. A página de área de trabalho só aparece quando um Mirror responde.

Table: As páginas do instalador do Ubuntu

| Página | Título na página | O que você faz |
| --- | --- | --- |
| Disco | Configuração guiada de armazenamento | Escolha o disco com UP e DOWN, e depois pressione ENTER. |
| Nome | Configuração do perfil | Digite o nome do computador e depois pressione ENTER. |
| Área de trabalho | Ambiente de área de trabalho | Escolha uma área de trabalho ou nenhuma, e depois pressione ENTER. |
| Cópia | Instalando o sistema | Espere enquanto o sistema é copiado. A página mostra cada etapa. |
| Concluído | Instalação concluída | Retire a mídia de instalação e depois pressione ENTER para reiniciar. |

```text
{"text": "O rodapé da tela mostra os botões \"[ Concluir ]  [ Voltar ]\" nas páginas antes da cópia, e \"[ Reiniciar Agora ]\" nas páginas de cópia e de conclusão. Os instaladores não têm nenhuma tecla que volte uma página, então confira cada página antes de pressionar ENTER."}
```

1. Inicialize o computador a partir da mídia de instalação.
2. Escolha o disco na página Disco, e depois pressione ENTER.
3. Digite o nome do computador na página Nome, e depois pressione ENTER.
4. Na página Área de trabalho, escolha uma área de trabalho ou nenhuma, e depois pressione ENTER.
5. Espere a cópia terminar.
6. Retire a mídia de instalação e depois pressione ENTER para reiniciar.

## Se algo der errado

> [!PROBLEM] A tela diz "Nenhum Mirror responde, então ele inicia no seu terminal."
> A página de área de trabalho é pulada, e o sistema inicia sem área de trabalho. Conecte o computador a uma rede cujo Mainframe execute o Mirror, e depois adicione uma área de trabalho após a primeira inicialização ([](jsc:apt)).

> [!SEE] jsc:installing_linux_keys, jsc:first_boot
