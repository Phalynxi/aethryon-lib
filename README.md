# Phi Lib

A shared library for Phalynxi's mods (NeoForge 1.21.1). Mods depend on this and not on each other.

- **Services**: a mod registers what it knows (wind, clouds, wetness, disasters, the clock) with `Services`; other mods read it through `PhiApi`. Every call has an answer: the best provider, or a plain default that works with vanilla alone.
- Planned: a particle system (our own, not Minecraft's), grid and field simulations, a settings API, worldgen helpers.

MIT licensed.
