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

### Changed

- Block names are translated in every supported language.
- Larger Energy Cell and Coal Generator panels; slots are lighter so items
  stand out.
- The Nexus panel has no Rename button any more: click the title instead.

### Fixed
