# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Four metals found in the ground: Steel, Cobalt, Mithril and Hellsteel. Steel is
  mined in the places iron is, Cobalt where gold is, Mithril where diamond is and
  Hellsteel in the Nether, a little more often than ancient debris. Each has raw
  metal, an ingot, a nugget, a plate, a dust, a block, a pickaxe, an axe, a shovel,
  a hoe, a sword and a full set of armor. Steel is as fast as iron and a little
  sturdier, Cobalt is as fast as diamond and lasts longer, Mithril is almost as good as
  netherite, and Hellsteel is better than netherite and does not burn in lava. A tool
  or armor is mended with an ingot of its own metal. Raw metal and ore smelt in any
  furnace; the Crusher turns raw metal into two dusts, the Pulverizer an ingot into a
  dust and the Compressor an ingot into a plate. Iron, gold and copper get dusts and
  plates too, and dust smelts into an ingot.
- Ingots of Steel, Cobalt, Mithril and Hellsteel work as armor trim materials on any
  armor, and any trim pattern can be put on armor of these metals. When armor is
  trimmed with its own metal, the trim is a darker shade.
- A bucket used on an Extractor fills with the fluid in its tank.
- Items of the metals, their alloys, and the dusts and plates of iron, gold and copper
  carry the common tags other mods use, such as `c:ingots/steel` and `c:dusts/iron`, so
  recipes of other mods accept them and filters by tag match them.
- Nexus Ore in stone and deepslate, a deep and rare ore that drops a Nexus Crystal.
- Parts for crafting: Core, Upgrade Blank, Machine Casing, Battery, Screen, Antenna,
  Cell Housing, a Cell Part for each size of Vault Cell, the ingots of the alloys
  Voltsteel, Lumen and Aether, and the bio resources Polymer and Biomass.
- Biofuel, a fluid that fills a bucket and can be placed in the world. It spreads like
  lava and is as slow to wade through, but it does not hurt and you do not drown in it.
  The Crusher grinds plants, saplings, leaves, logs, crops and the like into Biomass,
  and the Extractor presses Biomass into biofuel.
- Four more generators beside the Coal Generator: the Lava Generator, the Steam
  Generator (water and lava), the Biofuel Generator and the Nether Star Generator. A
  generator of a fluid has a tank for each fluid it needs: pour a bucket in with a
  right-click, put it in the slot of the panel (the fluid goes to the tank and the empty
  bucket stays to be taken) or let a pipe fill it. Each has its own look, with fire, lava or
  steam at work, and its own particles.
  A generator has a panel of its sides, like a machine's: each side takes fuel in (items and
  fluids), gives FE out, does both or is closed. Pipes and hoppers can put coal in.
- External Vault: put it against a chest, a drawer, a backpack or any other block that holds items or
  fluids, and the network uses that block as storage of its own. Its items show in the terminals, the
  Puller, the Pusher and the Assembler take from it, an order for crafting counts what lies in it, and the
  network puts resources into it as it does into a cell. Set its priority (what ranks higher is filled
  first and emptied last, so the chest can come before the cells or after them), a filter of the
  items and fluids it may use, as a whitelist or a blacklist, and whether the network may also put
  resources in or only take them out. Changes made to the block by hoppers or by hand reach the
  network within a second. Devices and crafting move only so much through it in a tick, which up to four Speed
  Upgrades raise; a player at a terminal is not held back. Four upgrade slots also take a Chunk Loader Upgrade,
  which keeps the chunk loaded so the block is read with nobody near, and up to three Capacity Upgrades, nine
  more filter slots each. It does not attach to a block of a network.
- Efficiency and Buffer Upgrades, one to a device. Efficiency makes a machine pay less
  FE, a generator get more FE from its fuel, and a Puller, Pusher, Placer or Remover move
  twice as much at a time. Buffer doubles the energy buffer of a machine or generator
  and the tank of an Extractor or a fluid generator. The generators and machines have
  four slots for upgrades.
- Void Upgrade, for a Storage Vault or an External Vault, one to a vault. Right-click it in the hand
  to list up to nine items or fluids; while it sits in the vault, the network destroys what is listed
  as it comes in instead of storing it. Only what arrives after it is installed is destroyed: what
  the network already holds stays, an empty list destroys nothing, and a cell set to take a listed
  resource still gets it first.
- A machine or a generator that is broken, or taken down with the Wrench, keeps the FE and the fluid it held
  on its item, which tells what it holds, and has them again when it is put up.
- Jade and The One Probe show what a block of Nexus is doing when you look at it: the network it is in, in its
  color, whether the network has energy, who owns the device and what stops it, and then what is its own: the
  energy, tanks and progress of machines and generators, what a Placer places or a Remover breaks and what its
  filter allows, the cells and priority of a vault, the state of terminals and wireless blocks. Nothing is needed
  to turn it on; each mod can switch the lines off in its own settings.
- JEI and REI keep their overlay off the buttons and windows beside the panels of machines, generators, Energy
  Cells and the Nexus, as they already did for terminals, Pullers and Assemblers, and items from JEI can be
  dragged into the list of a Void Upgrade.
- The Nexus Crystal in the hand turns slowly and glows.
- JEI and REI show the recipes of the Crusher, Pulverizer, Compressor, Alloy Smelter
  and Extractor, with the machines of every tier as the stations, and the Crafting
  Terminal beside the crafting table.
- The creative tab is in order: the Nexus and cables, terminals, storage, power,
  machines, automation, upgrades, parts, metals, other materials, tools and armor.
- Six machines that run on FE, each in four tiers: the Energy Furnace, the Crusher,
  the Pulverizer, the Compressor, the Alloy Smelter and the Extractor. A tier brings
  a bigger energy buffer, more speed and, for most, more lines of input and output
  slots (one, three, five and seven). Raise a machine on the spot with the tier
  upgrades. A machine has a panel of its working sides, a choice of how its input is
  shared out over the lines (one resource to a slot, or one spread over all), a
  redstone mode and four slots for Speed Upgrades. It draws FE from its network,
  takes it from any other source too and gives none away. The Energy Furnace smelts,
  blasts and smokes by the recipes of the game. The Crusher, Pulverizer, Compressor,
  Alloy Smelter (up to three inputs) and Extractor (a tank of fluid) read recipes of
  their own, which a data pack or another mod can add. The mod ships recipes for the
  metals, for plants into Biomass and Biomass into biofuel, and for the three alloys.
- The Alloy Smelter shows what it works on: the items of its input slots ride the
  rails to the mold, and the result grows there and leaves it as the recipe is
  done.
- A Stack Upgrade in an Assembler lets it hand a machine a whole stack of runs at once.
- An Assembler reaches any machine of Nexus whatever its sides say, and joins the
  network through the machine it faces.
- Tier upgrades. Three new items, the Advanced, Superior and Quantum Tier Upgrade,
  raise an Energy Cell by one tier: sneak and right-click the cell with the upgrade
  of the next tier. Tiers cannot be skipped, and everything the cell holds is kept:
  its energy, upgrades, name, priority and sides.
- Every Energy Cell has a panel of its sides, to the right of its window: the six
  sides unfolded into squares, and a click moves a side through closed, input,
  output and input and output. A side that is opened lets blocks of other mods,
  such as the pipes of a mod of pipes, put energy in or take it out as its mode
  says; the player answers for the energy that leaves that way.
- Three more Energy Cells: Advanced, Superior and Quantum. Each holds and moves ten
  times what the one below does. All four tiers share a new look: a trim in the
  colour of the network runs along the top edges, over the corners and down the
  vertical edges, and the squares under the battery tell the tier, from one on a
  Basic to four on a Quantum.
- The Wrench. A right click turns a device of the network round its sides, in a
  fixed order, and a cable that ends up at its front is let go. A left click
  sets the front of a device to the side you struck. A sneaking right click
  takes any block of the network down at once: it goes into your inventory
  together with its upgrades, cells, name and network, and falls beside the
  block if there is no room. Turning needs permission to configure the
  network, and you can always turn and take down your own devices.
- Networks have owners and access rules. The player who places a Nexus owns its
  network; the new Access tab of the Nexus panel lists who else may use it.
  Members are added from the players online and get one of the roles Admin,
  User, Guest or Blocked, and every permission can still be allowed or denied
  for a single player: opening panels, putting into and taking out of the
  network, autocrafting, configuring devices and building. Everyone who is not
  a member gets a role of their own, Blocked for a new network. The owner can
  hand the network over to a member.
- Devices work for the player who placed them: a Pusher, Placer, Puller,
  Remover or Assembler stands still when its owner may no longer move
  resources the way it does, and its panel says why.
- Only players allowed to build may place or break the blocks of a network or
  connect new blocks to it, and only those who manage it may pick up or move
  its Nexus. A device can always be taken down by the player who placed it.
- Server operators, and the player whose world it is, may always do
  everything. Networks from older worlds stay open to everyone until an
  operator claims them in the Access tab.
- A panel shows a lock in its header when you may not do everything there.
- New graphite icons for Vault Cells, upgrades, the Blueprint, the Network Card and the Nexus
  Terminal. Cells show what they hold, with a band in the color of their size, and upgrades show
  their purpose: a pickaxe for Silk Touch, a target for Range, a crafting screen for Autocrafting.
- Nexus now needs GeckoLib. The Nexus, Network Transmitter, Network Receiver
  and Nexus Link have new animated models in the colors of their network, with
  a port that opens on every side where a cable is attached.
- The Nexus shows a faceted crystal floating between two emitters, with
  shards circling it. Rings and crystal go dark without energy and glow with
  it; a Nexus in conflict flashes red.
- Network Transmitter and Network Receiver show an arrow on every side,
  pointing up for the transmitter and down for the receiver, that sways
  slowly. The transmitter sends rings across its top, the receiver draws
  packets into a dish. The Nexus Link shows three bars of signal and a beam
  sweeping its dial.
- The Terminal, Crafting Terminal and Blueprint Terminal have new screens with
  a raised picture each: a search bar over rows of cells, a crafting grid with
  its result, a ruled sheet beside a card. They glow in the color of the
  network while it has energy; without it they go dark but can still be told
  apart.
- The Crafting Monitor has a new screen with a row for each of the first three
  crafting tasks: an icon and a bar that fills as the task progresses. Without
  tasks it shows a ready mark in each row; with tasks but no energy the icons
  turn orange and the bars stand still.
- Cables are thinner, and look better in inventory slots, the creative tabs and
  recipe viewers: a proper piece of cable with a colored band along each side, in
  the color of the cable.
- Cables in all 16 dye colors. They connect on every side, including up and
  down, to cables of the same color and to the Nexus. Cables of different
  colors run side by side without joining.
- Dye a cable in the crafting grid to change its color.
- The Nexus opens a port on each side where a cable is attached.
- Basic Energy Cell: stores FE, accepts and gives energy on every side and adds
  its capacity to the network's energy pool. Any number of cells can join one
  network.
- The Nexus interface shows the number of devices connected to it, the stored
  energy and the energy flowing in and out. Cables are not counted.
- Coal Generator: burns coal, charcoal and coal blocks into FE. It joins the
  network like any device and feeds the network's Energy Cells, and still
  pushes energy into neighbouring blocks. It looks like a furnace, glows while
  working and takes a cable on every side except its firebox.
- Energy Cell panel: the cell's own charge and the energy flowing in and out,
  in the color of the network it belongs to.
- The color chosen in the Nexus now shows on the Nexus and every device in its
  network: the crystals, the core, the charge bars, the generator's lamp and
  the cable sockets. The Coal Generator panel takes the network color too.
  Devices cut off from any Nexus return to the standard blue.

- The Nexus shows the state of its network: its core pulses in the network
  color while there is energy and goes dark when the energy runs out.
- Cables of a network with energy light up along their colored band.
- The Energy Cell's charge bars run while energy flows into it.
- Only one Nexus leads a network. A second Nexus joined to it flashes red,
  sheds red sparks and stays off; it takes over when the first one is gone.
- Rename the Nexus, the Energy Cell and the Coal Generator by clicking the
  title of their panel. A device keeps its name when broken and placed again.
- Hovering the flame in the Coal Generator panel shows how long the fuel burns.
- Storage Vault: a drive cabinet that holds up to 16 Vault Cells and puts
  their contents into the network. Its front shows all 16 cartridges; a lamp
  on each glows green while
  the cell has room, orange when it has run out of types or of space, red when
  it has run out of both, and stays dark for an empty bay or when the network
  has no energy. Give each vault a priority in its panel: higher priority
  vaults fill first and are emptied last.
- Vault Cells for items and for fluids in six sizes, 1k to 512k. A cell holds
  up to 64 kinds of items or 8 kinds of fluids, and every kind takes some of
  its space. Cells keep their contents when taken out of a vault.
- Use a Vault Cell in hand to open its panel: see how full it is, rename it
  and set a whitelist or blacklist of up to 9 items or fluids. Click a filter
  slot with an item, or with a bucket for a fluid cell; right-click to clear it.
  Items go first to cells whose whitelist lists them.
- Terminal: attaches to a cable like a small screen and shows everything the
  network stores. Take a stack with a left click, half a stack with a right
  click, shift-click to move a stack into your inventory. Click with items to
  store them, or shift-click them in your inventory. Click with a bucket or
  tank to pour its fluid into the network, or click a fluid to fill it.
- The terminal has a search box (start a word with @ to search by mod) and
  buttons beside it to sort by amount, name, mod or id, flip the order, show
  only items, only fluids or only energy, and pick a small, medium, large or full screen
  window. Each size shows as much as fits the game window. The terminal
  remembers these choices.
- The two terminals are easy to tell apart: the Terminal's screen shows a
  search bar over rows of items, the Crafting Terminal's a row of items over a
  crafting grid and its result, even without energy.
- A cross at the end of the terminal's search box clears it. Erasing the text
  or clicking elsewhere lets the panel's keys work again.
- Crafting Terminal: a terminal with a crafting grid. Crafting refills the
  grid from the network, so shift-clicking the result crafts as long as the
  ingredients last. The Clear button returns the grid to the network.
- Terminals need energy in the network: without it they show that the
  network has no power and their screen goes dark.
- JEI and REI support: the "+" button of a crafting recipe lays it out on the
  Crafting Terminal's grid with items from your inventory and the network,
  and marks the ingredients nobody has. Shift-click it to lay out as many
  crafts as your items last for.
- Drag an item or a fluid from JEI or REI onto a Vault Cell's filter slot to
  list it there, without having it in your inventory. The recipe and usage keys work on the
  items and fluids a terminal lists.
- Puller and Pusher: small devices that sit against a chest, a furnace or a
  machine and move items and fluids between it and the network, one item or
  one bucket every half second. Place one against a block and it faces that
  block; sneak to place it against a block that opens a screen. They stand on
  their own and join the network once a cable or any network block touches
  them, reaching out to it with a cable arm on any side but their face. Each
  arm takes the color of the cable it meets and lights up while the network
  has energy. Lamps on their sides glow green while the network has energy
  and go dark without it.
- They work with the side of the block they touch, the way the block offers
  it: a Puller under a furnace takes out only what it has smelted, a Pusher on
  its side fills only the fuel, and blocks that let you set their sides follow
  those settings.
- The Puller takes whatever its filter allows, as a whitelist or a blacklist.
  The Pusher delivers what its whitelist lists, in order, in turn or at
  random. With a blacklist it delivers everything in the network but what is
  listed.
- Both can work always, only with a redstone signal, only without one, or once
  per pulse, and have four upgrade slots.
- A Puller or Pusher filter slot clicked with a filled bucket or tank lists
  the fluid inside; hold Shift to list the container itself. Items and fluids
  can be dragged onto it from JEI or REI too.
- The Nexus shows how many Pullers, Pushers and Storage Vaults its network has.
- A button at the top of the Puller and Pusher panels picks what the device
  moves: items, fluids or energy. New devices move items. A device moves only
  that kind, so an empty Puller filter no longer takes fluids along with
  items; on a fluid device a bucket on the filter lists its fluid. Each kind
  keeps a filter of its own: switching to fluids and back finds the item
  filter as it was.
- Pullers and Pushers move energy too, up to 10,000 FE every half second. A
  Puller draws FE from any block that gives it, such as a generator, a solar
  panel or a battery of another mod, into the network; a Pusher powers a
  machine from the network's energy. With energy there is nothing to filter:
  the filter shows a lightning bolt and locked slots, and a Pusher can still
  keep an amount of FE in the machine.
- The Nexus takes FE from the cables and generators of other mods and gives it
  back, so a Pusher of one network can feed another through its Nexus.
- Energy Vault Cells in six sizes, from 8.4 million FE at 1k to 4.3 billion FE
  at 512k. They hold energy only, so they have no filter, and their lamp stays
  green until the cell is full. Their energy joins the network's energy pool
  at the priority of their Storage Vault.
- The terminal lists the network's energy, Energy Cells included, as a
  lightning bolt next to its items and fluids.
- Upgrades for Pullers and Pushers. Speed Upgrade: works more often, up to
  four stacked in one slot. Stack Upgrade: moves a whole stack at once, 64
  items or 64 buckets. Regulator Upgrade: a Pusher keeps a set amount in the
  block and stops there, a Puller leaves a set amount behind instead of
  emptying the block; scroll over a filter slot to set it. Capacity Upgrade:
  adds a full row of 9 filter slots, up to 3 stacked in one slot, for 36 in all.
- A Match button on Pullers and Pushers picks how closely the filter compares
  an item: exactly, ignoring wear, or ignoring every component. It applies
  wherever the filter decides yes or no, so a blacklisted item still gets
  blocked however worn or enchanted it is.
- Autocrafting. Blueprints hold recipes: crafting recipes and processing in
  any machine, vanilla or modded. Put encoded Blueprints into an Assembler and
  the network can craft what they give, working out every step from the
  ingredients it stores, including those it has to craft first.
- Blueprint Terminal: a terminal with a Blueprint encoder under its list. Set
  a crafting grid, or up to nine inputs and three outputs of processing with
  their amounts, then encode it onto a blank Blueprint. An encoded Blueprint
  put back into the encoder can be changed and encoded over. The "+" button of
  JEI and REI lays any recipe out in the encoder, and their items and fluids
  can be dragged onto its slots. Sneak and use an encoded Blueprint to wipe it.
- Substitutes: a Blueprint can take more than the exact items it lists. A
  crafting recipe then takes whatever the recipe accepts in each slot, such as
  planks of any wood; a processing input takes anything in its tag, picked
  with a middle-click or Ctrl-click on the input. Crafting recipes brought in
  from JEI or REI take substitutes from the start.
- Assembler: crafts crafting recipes itself and hands processing inputs to the
  block its face touches, all of them at once, then takes the results back out
  of that block when its side allows. A Puller on the machine brings results
  back just as well. It can wait until the machine is empty before handing it
  the next batch, has a priority for its Blueprints and takes Speed Upgrades.
  Arrows on its sides point at its face, and looking at it outlines the
  machine it works with.
- Terminals list what the network can craft even when none is stored. Click it,
  or Ctrl-click or middle-click anything craftable, to ask for an amount: the
  plan shows what comes from storage, what gets crafted and what is missing,
  and Start begins the task.
- Crafting Monitor: lists every crafting task of the network with its progress,
  what each resource is waiting for and what holds it up, and cancels a task,
  giving back what it held. Tasks pause without energy and lose nothing.
- The Autocrafting Upgrade works now: a Pusher with it orders a craft of what
  its whitelist lists and the network has run out of.
- Craft Less: when the network lacks something for the amount asked, a button
  in the crafting request window brings the amount down to the most the
  network can craft, counting everything it would have to craft on the way.
- Assemblers can be chained: an Assembler facing another Assembler works with
  the machine at the end of the chain, so several Assemblers with their own
  Blueprints share one furnace. Waiting for the machine to empty counts the
  inputs of every Assembler in the chain.
- Energy Cells have a priority, 10 by default, and rank together with the
  Storage Vaults and their Energy Vault Cells: energy fills the highest
  priority first and is drawn from the lowest first.
- The Coal Generator takes up to four Speed Upgrades: each makes it burn its
  fuel one time faster, so the same coal gives its energy sooner.
- Placer: sits against the world like a Pusher and places blocks from the
  network in front of it, as its whitelist lists them or anything its
  blacklist does not, or drops them there as items. Set to fluids, it pours a
  source block.
- Remover: breaks the block in front of it and puts what it drops into the
  network, but only when the network has room for all of it; its filter lists
  blocks. Set to pick up items, it collects the items lying in front of it.
  Set to fluids, it takes fluid source blocks. Fortune Upgrades break with
  Fortune, up to level three, and a Silk Touch Upgrade breaks with Silk Touch.
- Network Transmitter and Network Receiver: link a Network Card to a receiver
  by right-clicking it, then put the card into a transmitter. Everything
  connected to the receiver joins the transmitter's network, however far
  away, even in another dimension, while its chunk is loaded.
- Nexus Link: lets Nexus Terminals reach its network within 32 blocks, and 32
  more for every Range Upgrade, up to four.
- Nexus Terminal: a terminal to carry. Right-click a Nexus to bind it, then use
  it anywhere a Nexus Link of that network reaches. Sneak and use it to switch
  between terminal, crafting terminal and blueprint terminal. Its crafting grid
  and encoder slots give their items back when you close it.
- Puller, Pusher, Placer and Remover filters can match by tag: Ctrl-click or
  middle-click an item in a filter slot to match every item of one of its
  tags instead, such as all logs or all iron ores; click again for the next
  tag. A small mark in the corner shows a slot that matches by tag. Tags are
  not used while a Regulator Upgrade keeps stock.

- Chunk Loader Upgrade: keeps the chunk its device stands in loaded, with
  nobody near, so a network goes on working and a Network Transmitter reaches
  a Network Receiver in another dimension. One per device. Pullers, Pushers,
  Placers, Removers, Assemblers, the Coal Generator, the Nexus Link, the
  Network Transmitter and Receiver, the Nexus, the Storage Vault and the
  Energy Cell take it. Several devices in one chunk share
  it, and the chunk is let go when the last of them loses its upgrade or is
  broken.
- The Network Receiver has a panel now, with its upgrade slot and a name you
  can change. Right-click it with a Network Card in hand to link the card, as
  before.

### Changed

- The energy bar of the machines, the generators and the Energy Cell is the same thin
  bar, with the numbers written under it.
- The panels of the generators put the slot, the fuel gauges, the energy bar and the
  upgrades in one row.
- The Nexus counts the machines of Nexus among the machines connected.

- A terminal keeps the resources where they are while you hold Shift, so that you can
  take stack after stack of one resource without it moving to its new place in the
  sort; let go of Shift and the list falls into order.
- The Nexus no longer hands the energy of the network to blocks of other mods,
  and no longer takes it from them directly: it gives and takes energy only with
  blocks of the network. Energy goes in and out of the network through a Puller
  or a Pusher.
- Energy Cells no longer hand their energy to blocks of other mods, and no longer
  take it from them directly. A pipe or a battery of another mod touching a cell
  gets nothing: energy goes in and out of the network through a Puller or a
  Pusher, which keep to the rules of the network. Blocks of the network next to
  a cell work as before.
- Placers and Removers place and break blocks as the player who owns them, so
  land protection treats their work as that player's.
- Cables can no longer be moved by pistons.
- The Nexus Link and the Coal Generator have a second upgrade slot.
- The Nexus, the Storage Vault and the Energy Cell have a column of four
  upgrade slots, and the Nexus panel shows your inventory.
- The Nexus and Energy Cell panels write large energy figures short, such as
  18.43M or 25.00 млрд in the units of your language; hover a line to see the
  exact figure.
- A Nexus with a name or a color of its own now drops when broken in creative
  mode too, with its network, so it can be moved.
- A Nexus Terminal stays bound to its network when the network is renamed
  and when the Nexus is broken and placed somewhere else. A Nexus replaced on
  the same spot keeps working with terminals bound there.
- A broken Nexus keeps the color of its network as well as its name.
- The priority row of the Energy Cell panel sits at the top, as in every
  other panel.

- Network Transmitter, Network Receiver and Nexus Link no longer take a cable
  on their top side: that is where their antenna stands.
- Placer and Remover have new heads, easy to tell from the Puller and the
  Pusher: a thin square frame holding a flat stamp, and the same frame holding
  a tilted pickaxe.
- Keeping an amount stocked now takes a Regulator Upgrade in the Pusher; a
  Pusher set to keep stock without one delivers as long as the block takes
  more.
- Shift-click an item in your inventory while a filter is open to list it in
  the first free filter slot; the item stays where it is. A filter slot takes
  an item with either mouse button and is cleared by clicking it empty-handed.
- A device panel outside any network shows the standard blue instead of grey.
- The network line sits right under the panel title, and the rest of the
  panel moves up with it.

- Block names are translated in every supported language.
- Larger Energy Cell and Coal Generator panels.
- Slots in every panel are darker, close to black.
- Energy is shown in FE instead of RF.
- The Nexus panel has no Rename button any more: click the title instead.
- Slimmer panel headers with a smaller title.

### Fixed

- The texts beside the upgrade slots of the Nexus Link, the Network Transmitter and the Network Receiver stay
  inside the frame and stand level with their slot, and the line beside the Chunk Loader Upgrade slot names the
  upgrade and says what it does, or that it is optional.
- Assemblers set against each other now always share the network, whichever
  way they face. Before, an Assembler whose face touched another Assembler
  stayed out of the network unless it had a cable of its own.
