---
section: library_readings
order: 460
---
# Sensors: what the sensors around the machine read

A sensor of J's Core reads something of the world: heat, light, pressure, radiation, speed. A program reads a sensor joined to its machine through a Redstone Interface: link the interface to the machine and turn its lens toward the sensor. You read the sensor by the interface's name through Sensors, a built-in type of System.Machine. Sensors came with Sigma# 3.

Table: Sensors members

| Member | What it gives |
| --- | --- |
| `Sensors.Read(name)` | What the sensor in front of the interface with that name reads, as a list of SensorReading records. The list is empty when nothing there reads anything, and the call gives null when no interface has that name. |
| `Sensors.Names()` | The names of the interfaces that have a sensor in front of them. |

Table: Records of Sensors

| Record | Fields | What each field holds |
| --- | --- | --- |
| `SensorReading` | `SensorReading.Name, SensorReading.Value, SensorReading.Least, SensorReading.Most, SensorReading.Unit` | Name: what it reads. Value: the reading now. Least and Most: the span the reading runs over. Unit: the unit of the value, or empty text when it has none. |

A sensor can read several things at once, and each is a reading of its own. The redstone a sensor gives says one of them as a strength from 0 to 15; a program reads them all, as numbers.

> [!DEFINE] Sensor
> A block that reads something of the world and gives it as a signal.

## If something goes wrong

> [!PROBLEM] Sensors.Read gives null.
> No Redstone Interface linked to this machine has that name. Check the name in its window.

> [!PROBLEM] Sensors.Read gives an empty list.
> The interface's lens does not face a sensor. Turn it toward the sensor.

> [!SEE] jsc:redstone_interfaces, jsc:redstone_library
