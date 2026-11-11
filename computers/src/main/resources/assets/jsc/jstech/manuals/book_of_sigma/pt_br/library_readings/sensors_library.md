---
section: library_readings
order: 460
---
# Sensors: o que os sensores em volta da máquina leem

Um sensor do J's Core lê algo do mundo: calor, luz, pressão, radiação, velocidade. Um programa lê um sensor ligado à máquina dele por um Redstone Interface: ligue a interface à máquina e vire a lente dela para o sensor. Você lê o sensor pelo nome da interface, pelo Sensors, um tipo embutido do System.Machine. O Sensors veio com o Sigma# 3.

Table: Membros do Sensors

| Membro | O que dá |
| --- | --- |
| `Sensors.Read(name)` | O que o sensor na frente da interface com esse nome lê, como uma lista de registros SensorReading. A lista vem vazia quando nada ali lê coisa alguma, e a chamada dá null quando nenhuma interface tem esse nome. |
| `Sensors.Names()` | Os nomes das interfaces que têm um sensor na frente. |

Table: Registros do Sensors

| Registro | Campos | O que cada campo guarda |
| --- | --- | --- |
| `SensorReading` | `SensorReading.Name, SensorReading.Value, SensorReading.Least, SensorReading.Most, SensorReading.Unit` | Name: o que ele lê. Value: a leitura agora. Least e Most: a faixa em que a leitura corre. Unit: a unidade do valor, ou texto vazio quando não tem. |

Um sensor pode ler várias coisas de uma vez, e cada uma é uma leitura própria. O redstone que um sensor dá diz uma delas como uma força de 0 a 15; um programa lê todas, como números.

> [!DEFINE] Sensor
> Um bloco que lê algo do mundo e o dá como um sinal.

## Se algo der errado

> [!PROBLEM] Sensors.Read dá null.
> Nenhum Redstone Interface ligado a esta máquina tem esse nome. Confira o nome na janela dele.

> [!PROBLEM] Sensors.Read dá uma lista vazia.
> A lente da interface não está virada para um sensor. Vire-a para o sensor.

> [!SEE] jsc:redstone_interfaces, jsc:redstone_library
