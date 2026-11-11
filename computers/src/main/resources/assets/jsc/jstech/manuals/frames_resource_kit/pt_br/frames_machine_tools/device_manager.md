---
section: frames_machine_tools
order: 222
---
# O Gerenciador de Dispositivos

O Gerenciador de Dispositivos lista o hardware da máquina e a porta em que cada peça está conectada. Ele permite desativar um dispositivo, o que impede o computador de usá-lo, e ativá-lo de novo. Ele existe só no Frames.

A aparência segue a edição. No Frames 95 ele fica em Propriedades do Sistema, sob o nome Sistema. No Frames XP é uma janela de console, com as visões no menu Visão. No Frames 11 as visões são uma opção na sua barra de menus.

Table: Visões

| Visão | Pelo que a lista agrupa |
| --- | --- |
| Dispositivos por tipo | O que cada dispositivo é. |
| Dispositivos por conexão | Como cada dispositivo está conectado. |
| Dispositivos por porta | A porta em que cada dispositivo está conectado. |

Table: Menus da janela

| Menu | Entradas |
| --- | --- |
| Arquivo | Imprimir... (não faz nada), Sair |
| Ação | Ativar ou Desativar, Atualizar driver... (não faz nada), Desinstalar (não faz nada), Propriedades |
| Visão | As três visões, e Mostrar dispositivos ocultos (não faz nada) |
| Ajuda | Sobre o Gerenciador de Dispositivos |

## Desativando um dispositivo

1. Selecione o dispositivo na lista.
2. Abra o menu Ação e escolha Desativar. Um dispositivo que já está desativado oferece Ativar.
3. Olhe a lista: um dispositivo desativado aparece riscado.

Um dispositivo que você desativa continua conectado, e você pode ativá-lo de novo a qualquer momento. Só um dispositivo em uma das portas com link deste próprio computador pode ser desativado.

## O que um dispositivo desativado deixa de fazer

Table: Quando um dispositivo está desativado

| Dispositivo | O que para |
| --- | --- |
| Monitor | Ele não é acionado, e a sua memória de vídeo não é contada. |
| Alto-falante | Ele fica em silêncio. |
| Qualquer Redstone Interface | Ela não faz nada enquanto o seu link estiver desativado. |
| Gateway de Rede | Ele não fica ativo enquanto o seu proprietário estiver desativado. |
| Estação de Dock | Ela não inicia uma área de trabalho. |

> [!DEFINE] Porta
> Um soquete da máquina, como um slot de placa ou uma porta de link, no qual um dispositivo é conectado.

> [!DEFINE] Hub
> Um dispositivo que adiciona portas próprias. Um dispositivo conectado a um hub aparece listado sob ele.

Um hub também é um dispositivo, e desativar um hub desativa todos os dispositivos ligados a ele. Se você desvincular um dispositivo, a configuração de desativado dele é removida, então um dispositivo que você vincular de novo volta ativado.

## Se algo der errado

> [!PROBLEM] O dispositivo que você quer desativar não aparece na lista.
> Só um dispositivo em uma das portas com link deste computador pode ser desativado. Faça o link antes ([](jsc:peripheral_cables)).

> [!PROBLEM] Todos os dispositivos sob um hub ficam desativados.
> Ative o hub de novo. Um dispositivo desativado por si mesmo continua desativado, então ative-o também.

> [!PROBLEM] Atualizar driver, Desinstalar, Mostrar dispositivos ocultos e Imprimir não fazem nada quando você os escolhe.
> Use Ativar e Desativar, que são as ações que funcionam.

> [!SEE] jsc:disable_device, jsc:peripheral_cables
