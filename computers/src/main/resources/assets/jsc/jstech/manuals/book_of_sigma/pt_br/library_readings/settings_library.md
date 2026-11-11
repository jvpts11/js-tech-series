---
section: library_readings
order: 459
---
# Settings: as configurações do próprio programa

Um programa pode guardar as próprias configurações, cada uma um nome e uma linha de texto, e lê-las de volta na próxima vez que rodar. O sistema dele as guarda num arquivo do jeito que o sistema de verdade guardava, então você pode ler esse arquivo com as ferramentas do próprio sistema. Você chega a elas pelo Settings, um tipo embutido do System.Machine. O Settings veio com o Sigma# 3.

Table: Membros do Settings

| Membro | O que faz |
| --- | --- |
| `Settings.Get(name, fallback)` | A configuração com esse nome, ou o valor reserva quando o programa não tem nenhuma com esse nome. |
| `Settings.Set(name, value)` | Guarda a configuração, no lugar do que ela tinha. Dá não quando o disco recusa o arquivo. |
| `Settings.Remove(name)` | Tira a configuração. Dá não quando o programa não tinha nenhuma com esse nome. |
| `Settings.Names()` | Os nomes de todas as configurações que o programa guarda. |
| `Settings.Path` | Onde as configurações ficam, escrito como o sistema escreve caminhos. |

Cada programa guarda as configurações sob o nome da classe pela qual foi iniciado, então dois programas nunca dividem uma.

Table: Onde cada sistema as guarda

| Sistema | Onde |
| --- | --- |
| Frames | No registro dele, o arquivo C:\\Frames\\System32\\config\\SOFTWARE, sob HKEY_LOCAL_MACHINE\\SOFTWARE e o nome do programa. |
| MC-DOS | Num arquivo INI próprio no topo do C:, com o nome cortado em oito letras. |
| Linux, os BSDs e o UNIX | No ~/.config, numa pasta própria do programa com um arquivo chamado settings. Um programa que fica rodando como serviço as guarda no /etc, num arquivo com o nome dele terminado em .conf. |
| Quince OS X e quinceOS | No ~/Library/Preferences, numa lista de propriedades com o nome do programa. |
| Os sistemas Quince clássicos | Nas Preferences da System Folder. |

O nome de uma configuração é feito de letras, dígitos, pontos, hifens e sublinhados, até 64 deles. O valor é uma linha, de até 1024 caracteres. O arquivo pesa no disco como qualquer outro.

```sgs Settings.sgs
int runs = Convert.ToInt(Settings.Get("runs", "0")) + 1;
Settings.Set("runs", "" + runs);
Console.PrintLine("Run " + runs + ", kept in " + Settings.Path);
```

> [!DEFINE] Registro
> O único arquivo em que o Frames guarda as configurações de todos os programas, cada um sob uma chave própria.

## Se algo der errado

> [!PROBLEM] O programa para e diz que o nome não é um nome de configuração.
> Use só letras, dígitos, pontos, hifens e sublinhados, até 64 deles.

> [!PROBLEM] O programa para e diz que uma configuração guarda uma linha.
> Tire as quebras de linha do valor e mantenha-o em até 1024 caracteres.

> [!PROBLEM] Settings.Set dá não.
> O disco do sistema está cheio ou não pode ser gravado. Libere espaço nele.

> [!SEE] jsc:file_tree, jsc:computer_info
