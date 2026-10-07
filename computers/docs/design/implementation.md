# Implementation

Performance, the settings and their keys, compatibility with other mods, and how state is saved.

## Performance

**Nothing in the mod may weigh on the server's TPS.** Work per tick is close to O(1); what is heavy (rebuilding the
index, planning a craft, compiling a program) runs off the tick, on a virtual thread (Java 21), and the result lands on
the main thread. The world is only touched on the main thread.

- **The network's index** ([Operations](operations.md)) is an in-memory map from every storage key to the places that
  hold it. The key has three kinds (item, fluid, chemical, [Storage](storage.md)) and tells apart items with different
  components. Every store counts its own changes, and a rebuild only reads again the ones that changed. A rebuild runs
  on a virtual thread and carries the number it was asked with: an older one that comes back late gives up instead of
  writing over a newer one. A change counter on the catalogue lets whoever watches the network (Σ#'s watches, programs)
  know with one comparison that nothing changed, and do nothing on a quiet network.
- **Disk waits:** every disk parks its own virtual thread for the length of its wait; several disks wait in parallel
  without touching the world off the main thread, and the transfer resumes on the main thread.
- **Dispatch:** J's Core's Operation dispatcher runs each Operation's work on a virtual thread per task; planning a
  craft and NextgreIQL's planner also run off the tick, on a snapshot of the stock.
- **Programs:** the credits per tick and the clock of each machine and of the server ([Σ and Σ#](sigma.md)) are what
  stop a program from weighing on the server.
- **Crafting:** the crafts per coprocessor slot come from 8 × 2^(N-1), with no table and no arithmetic per tick.

## Settings

Settings are declared once with J's Core, each with its range: a value outside its range is pulled back into it, with a
warning in the log, and a file with something wrong is written again in full. Every TOML file has a settings screen in
the mods menu ([Overview](overview.md)).

**`jstech-balance.toml`** (J's Core, server, in each world's `serverconfig`):

```toml
[balance]
hdd_latency_ticks = 10
ssd_latency_ticks = 3
nvme_latency_ticks = 1
operation_waiting_timeout_ticks = 1200   # how long an Operation waits
operation_priority_aging_ticks = 600     # waiting per priority level
subframe_efficiency_factor = 0.6         # the share of capacity a Subframe lends
orphaned_operations_expiry_hours = 24    # 0 never
program_machine_micros = 1000            # a machine's clock per tick
program_server_micros = 8000             # the server's clock per tick

[media]
download_kilobytes_per_second = 1024
upload_kilobytes_per_second = 512
max_file_megabytes = 32
player_quota_megabytes = 512

[calendar]
days_per_season = 28

[world]
chunks_per_owner = 25
```

**`jscomputers-server.toml`** (J's Computers, server):

```toml
[boot]
show_boot_menu = true                    # stop at the boot manager

[install_by_hand]
gentoo_every_step = false
arch_every_step = false

[prompt]
list_commands = false                    # the listcmd command on every machine

[soundfoundry]
catalog = true                           # the server's music catalogue
ethernet_kilobytes_per_second = 512      # a song's speed over the network
hbw_kilobytes_per_second = 2048
hpc_kilobytes_per_second = 8192

[programs]
outside_components = false               # window components that reach outside the game (a web address)
```

**`jscomputers-client.toml`** (J's Computers, each player):

```toml
[client]
reduce_motion = false
desktop_cursors = true
outside_components = false
```

**`jstech-audio`** (J's Core, client, JSON): each player's volumes and audio.

**What isn't a setting.** The formulas' constants (the processor's 40, the 256 items per GB, the server bonus's 0.05,
the efficiencies) live in the code, not in the settings: changing them on one server would unbalance the whole
catalogue. Whoever wants a faster or slower network has the keys that exist (the disks' waits, the programs' clock, the
watt to FE factor). The keys of each system still to build come with it (security, for example). The lists of items (the
3D Printer's and the AI's) are in the server's settings, not in data packs; models in loot come in through a loot
function ([Artificial intelligence](ai.md)); the hardware's recipes are decided with J's Industrial ([Eras](eras.md)).

## Compatibility

| Mod | Dependency | What it does |
| --- | --- | --- |
| J's Core | required | the series' library |
| GeckoLib | required | the animated models (Mainframes, racks); to be replaced by a native format of J's Core |
| JEI | optional | item and part information |
| Mekanism | optional | chemicals as the third kind of data ([Storage](storage.md)) |
| CC: Tweaked | optional | the Network Gateway ([Peripherals](peripherals.md)) |
| FTB Teams | optional | owners and teams, through J's Core |

J's Core also has the bridges for EMI, Jade, Curios and Accessories. Every optional mod stays behind a guard isolated in
an adapter: without it, nothing in the mod names it.

J's Computers works with any mod without an integration of its own: the buses and the External Storage Bus use any
inventory, the Crafting Interface feeds any machine with slots and the usual capabilities, and chemicals come in through
the bridge. J's Core's Space Persistor makes Chicken Chunks unnecessary ([The network](network.md)). New specific
integrations are decided when there is a concrete reason, always behind a guard.

## Saving

- **World state** (J's Core's saved states, on 1.21.1's saved data, each with the version of its format): the network
  registry, the disks' volumes ([Storage devices](storage-devices.md)) and the chunks loaded per owner.
- **Block entities:** the installed hardware, the programs and the system state of every computer (the files, the open
  windows, the Σ# processes with their memory and threads), in NBT with the version of what was written. The Mainframe
  also keeps the Operations log and the Operations under way ([Operations](operations.md)), and the Mirror's shelf of
  packages.
- **Items:** data components, never raw NBT. A disk carries its volume's id and a summary of its use; small media carry
  their contents.
- A save from an older version is read or refused with a message; breaking worlds between alpha versions is acceptable.
- The code is the source of what each block and item keeps: this design doesn't list the fields.

## To build

The keys of the systems still to build, which come with them:

```toml
# jscomputers-server.toml
[update]
cost = "fe"                              # fe, xp, both, none (Operations)

[hardware]
psu_failures = true                      # power supply failure (Power)
psu_life_multiplier = 1.0

[item_loss]                              # items lost when the network fails (Power)
power_loss = true
network_split = true
network_conflict = true
corruption = true

[teracoin]                               # the block's target time, the reward, the halving and its interval,
                                         # the difficulty floor, the blocks' rare items (Teracoin)
[teracoin.printer]                       # the 3D Printer's prices: the list of what can be printed

[ai]                                     # the AI's list: the items that can be generated and the tokens of each

# jscomputers-client.toml
[client]
notifications_on_hud = false             # the network's alerts on the HUD too (Wireless)
```
