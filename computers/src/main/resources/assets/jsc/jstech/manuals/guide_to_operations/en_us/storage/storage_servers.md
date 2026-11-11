---
section: storage
order: 118
icon: jsc:storage_server
---
# Storage servers

The network's room for items is on disks, and the disks sit in servers. A server lives in a Server Rack, a cabinet with slots called rack units (U), and each rack unit is one slot of height. A storage server is the server built for disks: it is 2 rack units tall and has more disk bays than any other server of its era. Every era has one, from the Vintage's five bays to the Standard's and the Advanced's ten.

> [!DEFINE] Storage server
> A server built for disks, with the most disk bays in 2 rack units (U). Its disks give the network room for items.

> [!DEFINE] Rack unit (U)
> One slot of height in a Server Rack. A server takes one or more of them.

Each disk goes into a drive bay on the front of the server. The bays are hot-swap: you can pull a disk out and push another in while the server runs.

Every server's bays, processors and slots are in [](jsc:server_types).

RAID (redundant array of independent disks) joins the disks of one server into one disk for the network. You seat a RAID controller card, the Vaultis Array of the era, in an expansion slot of the server, and you choose the mode on the STORAGE page of the server's Setup ([](jsc:setup_utility)). The modes and their numbers are in [](jsc:raid_arrays).

```scene
{"balloons": [{"detail": "Right-click its front to open the bays.", "number": 1, "part": {"x": 16, "y": 0, "z": 16}, "text": "The Server Rack"}, {"detail": "The Standard storage server has 10 of them.", "number": 2, "part": {"box": [0, 4, 14, 10, 14, 16], "x": 16, "y": 2, "z": 16}, "text": "The drive bays"}, {"detail": "Pull one out and push another in while the server runs.", "number": 3, "part": {"box": [6, 4, 14, 16, 14, 16], "x": 15, "y": 2, "z": 16}, "text": "The disks"}], "blocks": [{"data": "{Servers:{Size:8,Items:[{Slot:0,id:\"jsc:storage_server\",count:1,components:{\"jsc:server_hardware\":[{slot:13,item:{id:\"jsc:vaultis_array_pcie3\",count:1,components:{\"jsc:raid_mode\":\"raid5\",\"jsc:raid_members\":10}}}]}}]},FrontSlots:{Size:40,Items:[{Slot:0,id:\"jsc:disk_hdd_4t\",count:1},{Slot:1,id:\"jsc:disk_hdd_4t\",count:1},{Slot:2,id:\"jsc:disk_hdd_4t\",count:1},{Slot:3,id:\"jsc:disk_hdd_4t\",count:1},{Slot:4,id:\"jsc:disk_hdd_4t\",count:1},{Slot:5,id:\"jsc:disk_hdd_4t\",count:1},{Slot:6,id:\"jsc:disk_hdd_4t\",count:1},{Slot:7,id:\"jsc:disk_hdd_4t\",count:1},{Slot:8,id:\"jsc:disk_hdd_4t\",count:1},{Slot:9,id:\"jsc:disk_hdd_4t\",count:1}]},BayPowerOff:0,ServicePanelOff:0b,ActiveChannel:0,DisplayEra:3,Units:[B;4b,0b,0b,0b,0b,0b,0b,0b]}", "state": "jsc:server_rack[facing=south]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false]", "x": 15, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false]", "x": 16, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false]", "x": 16, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false]", "x": 15, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false]", "x": 15, "y": 2, "z": 15}], "caption": "A Server Rack with a storage server in its top rack units: ten disks in the drive bays, joined as a RAID 5 array by the Vaultis Array card inside the server. Drag it to see the rack from the side.", "height": 130}
```

The network counts the room of every disk in every server of a rack. A server with no disks adds nothing to the network's room.

## If something goes wrong

> [!PROBLEM] A slot says "Blocked - the chassis does not cable this slot".
> That slot cannot hold a disk. Use another slot.

> [!PROBLEM] A slot says "Blocked - no unit in this row cables these slots".
> Use a slot that a unit in this row cables.

> [!SEE] jsc:server_racks, jsc:raid_arrays, jsc:local_storage
