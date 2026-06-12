# BlueMap Banner Zones

> JouTak fork of [BlueMap Banner Marker](https://github.com/MiraculixxT/bluemap-banner) by Miraculixx.
> The original point-marker mechanic was replaced with **territory zones**: players outline areas on the
> [BlueMap](https://modrinth.com/mod/bluemap) web map by placing named banners.

## How it works

Place a banner **while sneaking** after naming it on an anvil — the banner joins the zone with that name:

- **1 banner** — the zone shows as a point marker (colored by the banner dye)
- **2 banners** — the zone becomes a rectangle with the banners as corners
- **3+ banners** — the zone becomes a polygon; every new banner attaches to the nearest edge,
  so you can walk the perimeter to outline any shape and refine a badly connected edge by
  placing another banner near it

The zone color follows the **last placed** banner, so a zone can be repainted by adding a banner of
another dye. Breaking a banner shrinks the zone back (polygon → rectangle → point → removed).
Placing a banner normally (without sneaking) does nothing.

### Banner protection

Zone banners are physical and stay in the world. They can only be broken by their owner — until the
configured time (default **30 days** since zone creation) passes. After that anyone may clean them up:
banners are meant for the initial marking of plans, finished builds speak for themselves.
Players with the `territory.admin` permission can always break zone banners.

### State territories

Names listed in `territory.state-names` (config) are reserved: placing such a banner requires the
`territory.state.place` permission and adds it to a shared **state zone** — no single owner, distinct
style on the map, protection never expires.

### Dates

Every zone stores its creation date and every banner its placement date. They are shown in the marker
popup on the web map and drive the protection expiry check (evaluated at break time, nothing is scheduled).

## Commands & permissions

| | |
|---|---|
| `/bmb global` | overview GUI with all zones (teleport / delete) |
| `/bmb <player>` | overview GUI with the player's zones |

| Permission | Effect |
|---|---|
| `bmb.overview` | access to the `/bmb` GUI |
| `territory.zone-limit.<rank>` | zone limit by rank (`territory.zone-limit` config section, default 1) |
| `territory.state.place` | place/break state territory banners |
| `territory.admin` | break any zone banner |

## Configuration

See `settings.yml` — protection duration (`territory.protect-days`), zone limits, marker styles for
player/state zones, the state name registry and the marker set. Messages live in `language/<lang>.yml`
(en_US, de_DE, ru_RU, fr_FR provided) in [MiniMessage](https://webui.advntr.dev/) format. Marker point
icons can be changed at `assets/marker_<color>.png`. Changes apply after a server restart.

## Migrating from BlueMap Banner Marker

Legacy point markers (`marker/<world>.json`, `marker/player_markers.json`) are **not loaded** anymore.
Old banners in the world stay as decoration; players re-mark their territory with sneak-placed banners.
Zone data is stored in `zones.json`.

## Building

```
./gradlew :impl-paper:build
```

Requires JDK 25. The jar lands in `impl-paper/build/libs/`. Note: this is a 3rd party extension and not
official by BlueMap in any way!
