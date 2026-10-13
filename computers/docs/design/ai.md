# Artificial intelligence

The AI exists to **generate items** in huge amounts: a model trained on an item produces it in bulk, much faster than
the 3D Printer ([Teracoin](teracoin.md)), at the cost of energy, heat and hardware. It also does **forecasting** and
**planning** for the network.

Today none of it exists. The Envya Tessera compute cards (K40, V100, A100, H100) work as ordinary graphics cards, and
the MI300X and the NPUs only exist as art.

## To build

### From the Vintage to the Advanced

It follows real history:

- In the **Vintage** and the **Legacy**, AI is small and runs on the processor of any computer with its era's AI
  framework (a Personal Computer, a server in an ordinary rack, the Mainframe): tokens per tick come from the
  processors' capacity, at a small fraction.
- In the **Transition**, graphics cards start to train and generate (GPGPU), much faster than the processor.
- In the **Standard** come the **AI Server** and the **AI Rack**, and AI starts generating at scale.
- In the **Advanced** come the NPUs and the modern architectures.

The old architectures are tiny and have short context windows, so they only reach cheap items (below): no other gate is
needed. Generated items go into the network's storage, so the computer has to be on a network.

### The AI Server and the AI Rack

The **AI Server** is a server made for AI, and differs from an ordinary server as it does in reality:

- its **GPU baseboard** (like SXM modules) pools the video memory of all its cards, and the total decides how big a
  model it loads without spilling over (below);
- **energy and heat:** it draws many kW and runs very hot ([Power](power.md), [Temperature](temperature.md));
- the **High compute** line ([The network](network.md)), to train across several servers as one.

**Compute cards** (the Envya Tessera K40, V100, A100 and H100, and the MI300X) are the datacenter GPUs: they go on an AI
Server's GPU baseboard, pool their video memory, train and generate. Outside an AI Server they work as ordinary graphics
cards, but with no video outputs, like the real ones: they light no monitors. **NPUs** (Advanced) are accelerators for
**inference only**, like the real ones: they don't train, but they generate with much less energy per item than a GPU. A
rack of GPUs trains; a rack of NPUs generates cheaply.

The **AI Rack** (Standard and Advanced) is the third type of rack, next to the Server Rack and the Supercomputer Rack
([Servers and racks](servers-and-racks.md)). It has optimised cooling: a much larger base thermal budget (an estimate:
5,000 W against 1,000 W). It only takes AI Servers and their units:

| Unit | What it does | The real equivalent |
| --- | --- | --- |
| AI Server | the GPU server: its baseboard pools the video memory | DGX / HGX |
| GPU Interconnect Switch | joins the GPUs of **every** AI Server in the rack into one, with their video memory added up, for bigger models | NVLink Switch (NVL72) |
| Coolant Distribution Unit | water cooling straight onto the chips: adds a lot to the thermal budget | CDU |
| Power Shelf | powers the whole rack through a busbar, with redundant units (power supply failure applies to each one, [Power](power.md)) | busbar power shelf |
| High Compute Switch | joins the rack to the High compute line, to train with several racks as one | InfiniBand leaf switch |

Every bay has a **media slot** at the front (like the USB ports and front disk bays of a DGX): a USB stick, an optical
disc or a disk in a caddy with a dataset or a model, read at the medium's speed.

### Datasets and models are files

Everything is data: datasets and models are files on the network's disks ([Storage](storage.md)), with backups
([Programs](programs.md)) and sharing through the Mirror, which is the network's model hub. They reach the AI Servers
through the **network** (at the speed of the slowest cable; the High compute line is the fast one) or on **media**, in
the bay's slot.

**An item's dataset** is made with the **SAMPLE** Operation, which **reads** stored items destructively: a stored item
is already information, and reading turns it into a sample (`SAMPLE 1024 iron_ingot INTO '\\data\datasets\iron.ds'`, in
IQL, in the AI program or in Σ#). The file grows with the samples. Quality follows how many there are (estimates):

| Samples | Highest accuracy the item reaches |
| ---: | ---: |
| 64 | 50% |
| 256 | 75% |
| 1,024 | 90% |
| 4,096 | 99% |

**Making a model from scratch**, in the AI program ("New Model"): choose the **architecture**, the **size** (the number
of parameters) and the **context window** (below). The file weighs the number of parameters times the bytes of each,
fixed from the moment the model is made, trained or not: an MLP weighs a few KB; an empty Transformer of a few billion
parameters takes GB of disk before it learns anything. Models are versioned like files (`diamond-v1`, `diamond-v2`).

**A model learns several items:** every TRAIN teaches it the item of a dataset. Each item's accuracy depends on the
samples and on the model's **capacity**: a small model with many items is less accurate on each. **Fine-tuning** is
training more items on top of an existing model, a found one included (below). At the end of a training, the program
shows each item's accuracy (the validation).

**The architectures**, by their real dates (the numbers are estimates):

| Architecture | Era | Role | How it plays |
| --- | --- | --- | --- |
| MLP | Vintage | generates | tiny, 1 or 2 items, low highest accuracy (about 80%) |
| RNN | Vintage | forecasts | short series |
| CNN | Vintage | generates | small and cheap; few items; up to about 90% |
| LSTM | Vintage | forecasts | long series |
| DBN | Transition | generates | the first "deep" one: more items than the CNN |
| VAE | Standard | generates | stable and cheap to train, lower highest accuracy per item |
| GAN | Standard | generates | very fast but unstable: accuracy swings from batch to batch |
| Transformer | Advanced | generates, forecasts | the most accurate (up to 99%); a lot of video memory per item, and its window grows fast |
| MoE | Advanced | generates | many items per video memory, and fast: it only wakes the expert of the item asked for |
| Diffusion | Advanced | generates | very high quality but slow: good for expensive items, bad for volume |
| SSM | Advanced | generates, forecasts | efficient: less video memory per item than the Transformer, a cheap window |

The Exa and the Singularity bring their own when they have hardware.

### Training (TRAIN)

TRAIN is an Operation the Mainframe dispatches to an AI Server or an AI Rack (from the Standard on; before that, to the
computer running the framework). It reads the dataset, trains, and writes the model to its destination.

- The **work** is counted in tokens: the samples × the tokens per item × a training factor (about 3, because training
  goes forward and back). The **time** is that work divided by the tokens per tick of the GPUs and compute cards that
  train (NPUs don't train).
- It only moves forward with the AI Server loaded: with its chunk unloaded it pauses and resumes (for long trainings,
  the Network Anchor Card or the Space Persistor, [The network](network.md)).
- **Checkpoints:** training writes a checkpoint, a file next to the model, at every interval chosen in the TRAIN;
  writing stops training for the time it takes (the model's size divided by the disk's speed) and takes disk. On a
  sudden shutdown ([Power](power.md)), training resumes from the last checkpoint and loses what it did since; a thermal
  trip can corrupt the checkpoint itself ([Temperature](temperature.md)). On a clean shutdown, it writes a checkpoint
  before stopping and loses nothing.

### Loading and serving

An AI Server's **inference service** **loads** a model: the weights and the context window go into video memory, in a
time that is the file's size divided by the link's speed. The model stays resident until it is unloaded.

- A server, or a rack joined by the GPU Interconnect Switch, holds **several models** loaded at once, while they fit.
- **Offload:** what doesn't fit in video memory spills into the server's memory (the memory ledger,
  [Hardware](hardware.md)), and the part in memory works at a fraction of the speed (an estimate: a tenth): a model with
  half of it in memory crawls. What doesn't fit even in video memory plus memory doesn't load. The AI program shows how
  much of each model is in video memory and how much in memory, in red when there is offload.

### Generating (INFER)

INFER is the Operation that generates items: from the AI program, from IQL (`INFER 10000 diamond`, or
`... USING MODEL 'diamond-v2'`) or from Σ#, with priority and logging like the others.

- **Tokens:** generating an item costs its **tokens per item**, from the AI's list in the settings (its "difficulty":
  dirt costs a few, a Nether Star an enormous amount). Accelerators make **tokens per tick** from their power
  ([Hardware](hardware.md)), NPUs with less energy.
  `items per tick = tokens per tick × the architecture's factor / tokens per item`. The program shows the tokens per
  second of each server and of the rack.
- **The context window** is how much the model works on at once: **an item whose tokens don't fit in the window can't be
  generated** by that model; one pass generates as many items as fit in it. The window spends video memory (the real KV
  cache): in the Transformer it grows very fast with size, in the SSM slowly.
- **Where it goes:** the Mainframe only looks at servers with a loaded model that knows the item, picks the one that
  gives the most **good** items per tick (accuracy counts) and splits a big INFER across several, in proportion to each
  one's speed. A server's tokens per tick are shared between the INFERs it serves, by priority. With no loaded model
  that knows the item, the INFER waits (WAITING) and the log says why. The work runs on the accelerators and takes none
  of the Mainframe's queues.
- **Accuracy:** each item comes out right at the model's accuracy for it; the ones that fail give nothing, but the time
  and energy are spent. It is worked out per batch (the expected number of hits, with the remainder carried from tick to
  tick), never a roll per item.
- **The generated items go into the network's storage** as the INFER's result; with the network full, generation stops
  and waits.
- **Energy and heat** are the cost: the accelerators draw their watts while they generate, and an AI Rack that generates
  needs its CDU.
- A stopped or cancelled INFER ends COMPLETED_PARTIAL with what it already generated.

### Forecasting and planning

- A dataset can also be made from the **network's data**, without using up items (`SAMPLE STOCK OF iron_ingot`: the
  series of stock and demand, taken from the Operations log and the event log).
- A **forecasting model** (RNN, LSTM, Transformer, SSM) predicts demand. The **Predictive Cache** loads it and gets more
  of the items that will be asked for right; and **predictive autocrafting** is an Automation Engine job that crafts
  before something runs out, when the model predicts the stock will drop under a threshold.
- **NextgreIQL** can use a model as a learned cost estimator, through its planner's API ([Operations](operations.md),
  [The API](api.md)). The Midsoft IQL Server and Prophet can't.
- There is no assistant in J's Computers.

### The software

The real layers, as parodies with their houses (the names are set when this is built):

- each era's **AI framework** (from the Vintage's neural network simulators to today's), which brings SAMPLE, TRAIN and
  INFER to the machine;
- a **GPU toolkit** (from the Transition on), which serves the cards of any maker;
- the **inference service** (from the Standard on), which loads and serves models;
- the **AI management program:** datasets, trainings, the models loaded on each server, video memory and offload; it
  loads, unloads and trains, from the network or from the media slot.

### Settings

**The AI's list** in `jscomputers-server.toml`, separate from the 3D Printer's: only what is on it can be generated, and
every item has its tokens per item. There is no on and off switch ([Overview](overview.md)). The numbers on this page
are estimates.

### Other mods

- **Pre-trained models:** media with a model already trained, found in other mods' structures (J's Overworld's abandoned
  labs, J's Space's wrecks). J's Computers gives a **loot function** any mod or data pack uses to put "a USB stick with
  a model" in a table (the architecture, the items, within the AI's list, the accuracy and the size, chosen or at
  random).
- **J's Robotics**, installed alongside, trains robots' behaviours on this infrastructure: it records actions into a
  dataset, TRAIN runs on the AI Servers, and the model is installed in the robot. J's Computers lets add-ons **register
  dataset and model types** through the API ([The API](api.md)) and never knows what a robot is.
