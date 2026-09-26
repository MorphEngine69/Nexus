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
- Basic Energy Cell: stores RF, accepts and gives energy on every side and adds
  its capacity to the network's energy pool. Any number of cells can join one
  network.
- The Nexus interface shows the number of devices connected to it, the stored
  energy and the energy flowing in and out. Cables are not counted.
- Coal Generator: burns coal, charcoal and coal blocks into RF. It joins the
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
  only items or only fluids, and pick a small, medium, large or full screen
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

### Changed

- Block names are translated in every supported language.
- Larger Energy Cell and Coal Generator panels; slots are lighter so items
  stand out.
- The Nexus panel has no Rename button any more: click the title instead.
- Slimmer panel headers with a smaller title.

### Fixed
