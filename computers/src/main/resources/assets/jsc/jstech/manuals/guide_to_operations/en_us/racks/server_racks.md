---
section: racks
order: 54
icon: jsc:server_rack
covers_family: jsc:server_racks
---
# Server racks and servers

Your items have to live somewhere. In J's Computers they live on the disks of servers, and the servers live in racks.

> [!DEFINE] Server
> A computer built to sit in a Server Rack.

A Server Rack gives eight rack units of room. A machine's height is counted in those units: most servers take one, the bigger ones take two.

> [!DEFINE] Server Rack
> A cabinet, two blocks wide, three tall and two deep, that holds machines in rows.

> [!DEFINE] Rack unit (U)
> The height a machine takes in a rack, counted in units. Each cabinet has eight.

```scene
{"balloons": [{"detail": "Click the front of the rack with a server in your hand to mount it.", "number": 1, "part": {"box": [6, 9, 14, 16, 14, 16], "x": 15, "y": 2, "z": 16}, "text": "Server"}, {"detail": "It lets one monitor reach every machine in the rack.", "number": 2, "part": {"box": [6, 4, 14, 16, 8, 16], "x": 15, "y": 2, "z": 16}, "text": "KVM Switch"}, {"detail": "It takes two rack units, and its ten drive bays hold the disks.", "number": 3, "part": {"box": [6, 4, 14, 16, 13, 16], "x": 15, "y": 1, "z": 16}, "text": "Storage Server"}, {"detail": "This Standard one adds 1,500 W to the heat the rack can carry.", "number": 4, "part": {"box": [6, 8, 14, 16, 13, 16], "x": 15, "y": 0, "z": 16}, "text": "Cooling Unit"}, {"number": 5, "part": {"box": [6, 3, 14, 16, 7, 16], "x": 15, "y": 0, "z": 16}, "text": "Rack UPS"}], "blocks": [{"data": "{Units:[B;1b,10b,0b,4b,0b,1b,12b,11b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:0b}", "state": "jsc:server_rack[facing=south]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 15, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 16, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 16, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 15, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 15, "y": 2, "z": 15}], "caption": "A Server Rack with its rack units filled: a server, a KVM Switch, a Storage Server, another server, a Cooling Unit and a Rack UPS. Drag it to turn it.", "height": 130}
```

The Supercomputer Rack holds supercomputer nodes only, and comes in the Standard and the Advanced ([](jsc:supercomputers)).

> [!DEFINE] Supercomputer
> Many computers that work as one large machine, joined by high compute cable.

A rack seats servers of its own era or older. Each server has drive bays, which show at the front of the rack. The [server types](jsc:server_types) list how many a server has.

To mount a server by hand, hold it and click the front of the cabinet. It goes into the row under the crosshair, counted from the top. Aim elsewhere, and it takes the first row that fits, from the top down. A rack unit mounts the same way.

Click the cabinet with an empty hand to open the rack screen. On a Supercomputer Rack, sneak-click to take the livery panel off.

Each rack unit has a power switch for its bay, and a new rack switches every bay on. The switch of a bay is the power button of the machine in it. The machine still runs its self-test and stops at its boot menu.

The disks in the bays are the storage. A server item carries no disks of its own: its drives are the disks you put in its bays. Join the rack's back port to the backbone cable, which the rack takes up to its era ([](jsc:data_cables)). From the Standard on, the backbone is fibre, and the rack takes it only through a Fibre Optic Interface mounted in one of its rows ([](jsc:rack_equipment)).

## If something goes wrong

> [!PROBLEM] The fibre does not join the rack.
> Mount a Fibre Optic Interface in the rack: the Standard one for the Fibre Optic Cable, the Advanced one for the OM5 Fibre Cable too ([](jsc:rack_equipment)).

> [!PROBLEM] A server does not go in.
> It is from a newer era than the rack, or the rows it needs are taken or too small for it. Use a rack of its era or a newer one, or free those rows.

> [!PROBLEM] The message names another kind of rack, such as "This chassis belongs in a Server Rack".
> Put it in the rack the message names.

> [!PROBLEM] The message says "This chassis is of a later era than this rack".
> Put it in a rack of its own era or a later one.

> [!SEE] jsc:rack_drive_bays, jsc:data_cables
