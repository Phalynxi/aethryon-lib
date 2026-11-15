# Aethryon

A shared library for Phalynxi's mods (NeoForge 1.21.1). Mods depend on this and not on each other: a mod registers what it knows, other mods read it, and every question has an answer that works with vanilla alone.

## What is in it

- **Services** (`Services`, `AethryonApi`): providers registered by mods and read by others, with plain defaults when nobody provides one.
  - `WindProvider`, `ClockProvider`, `CloudModel` (and the default cloud maths), `WetnessProvider`
  - `StormProvider`: heavy-storm level, storm fog, drying, the player's wetness from the server, and the regional **storm cells** (`StormCell`) with `AethryonApi.rainAt`
  - `SeaProvider` (how open the sea is, surges), `WaterEvents` (rings, foam, ship hulls to draw), `DisasterView` (funnels: tornadoes, dust devils, whirlpools)
  - `AethryonApi.ground`: the first air above the ground, leaves not counted, also on the client
- **Disturbances** (`Disturbance`, `DisturbanceListener`, `AethryonApi.disturb`): one channel for steps, wheels, rotor wash, hulls, blasts and splashes. Whatever causes one sends it; whatever reacts (snow, sand, dust, water, particles) listens. A wheel from any mod makes tracks in the snow of any other.
- **Look** (`Look`): the seam between the mods that run the world and the mod that draws it (a shader look). The drawing mod sets what draws the water and clouds, the puddle map and the precise water amounts; until it does, the others draw plain versions.
- **Particles** (`dev.phi.aethryon.particle`): our own particle engine, nothing goes through Minecraft's. `ParticleKind` (billboard or flat shape, sprites, a per-tick behaviour), `Particle` (plain fields), `Particles` (pool of up to 24,000, ticked every client tick and drawn interpolated in one batch from the particle atlas). Minecraft only stitches the sprites (register a particle description file and hand its `SpriteSet` to the kind).

## Using it

Add it as a `compileOnly` dependency and declare `aethryon` as a required dependency in `neoforge.mods.toml`. Register providers in your mod constructor with `Services.register(Type.class, provider, priority, modId)`; read with `AethryonApi`.

Planned: grid and field simulation toolkit, a settings API, worldgen helpers.

MIT licensed.
