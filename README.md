# Tornadic

**Realistic tornadoes, supercells and severe weather for Minecraft 1.21.1 (Fabric).**

Tornadic is a severe-weather simulation, not a weather-mod-with-a-tornado. Tornadoes are the
main event: they form from organized supercells, track terrain, intensify and weaken through a
full life cycle, tear up the landscape along a persistent path, and dissipate. Everything is
driven by a deterministic daily atmosphere so forecasts, warnings and the sky you are standing in
all agree.

No magic. No dimensions. No bosses. Just physics-flavored bad weather.

## Features

### Tornadoes (the main feature)
- Full life cycle: instability → thunderstorm → strong thunderstorm → supercell →
  tornadic supercell → funnel develops → touchdown → intensify/weaken → dissipate.
- **EF0–EF5** rating with realistic rarity (weak tornadoes common, EF4/5 rare).
- Per-tornado properties: wind speed, funnel width, forward speed, duration, damage potential,
  debris radius, rotation speed, path length.
- Realistic wind field: wind scales with proximity — far away you get drifting leaves and rain,
  close in you get pushed around, at the core debris and structural damage scale with EF.
  **Nothing one-shots you on contact** — danger comes from wind, debris and buildings.
- Terrain-following movement with gradual, meandering direction changes; tornadoes can form
  kilometers away, travel toward or away from you, and happen entirely unwatched.
- **Persistent paths**: damaged vegetation, exposed dirt, flung debris and downed trees remain
  after the funnel is gone.
- Spawn safety: tornadoes never form on top of players, and only form inside active storm
  systems.

### Severe-weather system
- Storm types: cumulonimbus → thunderstorm → strong thunderstorm → **supercell** (rotating,
  large hail, frequent lightning) → tornadic supercell.
- Thunderstorms: dark skies, heavy rain, lightning with distance-delayed thunder, strong wind.
- Hail of variable size; large hail damages crops, entities and fragile blocks (extreme hail is
  rare).
- Global wind system that varies with weather, storms and tornadoes — affects rain, particles,
  tall grass, crops and entities.

### Forecasts & ratings
- **Daily forecast every Minecraft day**: temperature, dew point, humidity, pressure, wind,
  instability (CAPE), shear, storm probability, tornado probability.
- Severe-weather risk rating: **NONE / MARGINAL / SLIGHT / ENHANCED / MODERATE / HIGH** —
  HIGH days are extremely rare; most days are uneventful.
- Believable atmospheric logic: instability + shear → supercells and tornado potential;
  humidity + instability → thunderstorms; shear → organization and rotation.
- Forecasts are **deterministic per world seed and day** — the same world on the same day is
  always the same.

### Chasing tools
- `/thermos` — current atmospheric values (these are the actual simulation values, not a
  display gimmick). `/thermos tomorrow` / `/thermos 3` for deterministic future forecasts.
- Weather radio, anemometer, thermometer, barometer, camera-free storm notebook.
- **Storm radar** (item or `R`): a range-limited, quantized scope showing cells, supercells,
  rotation and strong tornado signatures. It is deliberately imperfect.
- **Storm chaser vehicle** (research SUV): rideable, faster than walking, gives partial shelter
  from wind/debris damage. Not a tank.
- Storm tracking: warnings on tornado formation (coordinates, EF estimate, direction, speed,
  "seek shelter") — rate limited so chat never spams.

### Debug
- `V` toggles the debug HUD (default **off**): live atmosphere, probabilities, and
  storm/tornado positions and intensities.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/thermos` | Current atmospheric values + forecast | all |
| `/thermos tomorrow` (or `1`–`30`) | Deterministic future forecast | all |
| `/thermos debug on\|off` | Toggle debug HUD | op |
| `/weatherstorm [type]` | Spawn a storm (cumulonimbus/thunderstorm/strong_thunderstorm/supercell/tornadic) | op |
| `/spawn_tornado ef0`…`ef5` | Spawn a test tornado ~96 blocks away | op |
| `/tornado_info` | Info about the nearest tornado | all |
| `/storm_info` | Info about the nearest storm | all |

## Configuration

`tornadic/tornadic.toml` (auto-generated on first run): tornado and storm frequency, max
intensity, damage and debris settings, wind strength, sim speed, warning toggles, equipment
toggles, and whether natural tornadoes occur at all.

## Performance & multiplayer

- Server-authoritative simulation; clients only render synced state (particles, sounds, screens,
  HUD) — safe on dedicated servers, no desync, no client-only crashes.
- No per-block-per-tick work: budgeted block destruction with per-tick and per-tornado caps,
  chunk-aware checks, rate-limited entity/debris updates, capped particles.
- Protected structures (configurable block list) are never broken by tornadoes or hail.
- All state (storms, tornadoes, daily stats) persists in world data and survives save/reload;
  daily weather is deterministic per world/day.

## Building

```
./gradlew build
```

Produces `build/libs/tornadic-1.5.0.jar` (Fabric, Minecraft 1.21.1, Java 21, Fabric API
0.115.6+1.21.1). GitHub Actions runs the same build on push and uploads the artifact.

## Testing checklist (runtime)

- Mod loads, world loads, no crashes at title/overworld.
- Survive multiple in-game days; weather cycles; `/thermos` values change daily.
- Spawn storms and tornadoes; tornadoes form, touch down, track, damage, and dissipate;
  the path remains.
- Save/reload mid-tornado: state restored, simulation continues.
- Multiplayer: sync over LAN/dedicated server, no crashes.
