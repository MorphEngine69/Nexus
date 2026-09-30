# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

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
  after the Energy Cells.
- The terminal lists the network's energy, Energy Cells included, as a
  lightning bolt next to its items and fluids.
- Upgrades for Pullers and Pushers. Speed Upgrade: works more often, up to
  four stacked in one slot. Stack Upgrade: moves a whole stack at once, 64
  items or 64 buckets. Regulator Upgrade: a Pusher keeps a set amount in the
  block and stops there, a Puller leaves a set amount behind instead of
  emptying the block; scroll over a filter slot to set it. Capacity Upgrade:
  adds a full row of 9 filter slots, up to 3 stacked in one slot, for 36 in all.
- Range, Fortune and Silk Touch Upgrades are in the creative tab already;
  their tooltip says they are still in development, and no device takes them
  yet.
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

### Changed

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
