# BlueMap Booking

> JouTak fork of [BlueMap Banner Marker](https://github.com/MiraculixxT/bluemap-banner) by Miraculixx.
> The original point-marker mechanic was replaced with **territory booking**: players outline (book) areas
> on the [BlueMap](https://modrinth.com/mod/bluemap) web map by placing named banners.

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
banners are meant for booking plans, finished builds speak for themselves.
Players with the `booking.admin` permission can always break zone banners.

### State territories

Zone names starting with one of the prefixes from `booking.state-prefixes` (e.g. `JT -`, `ИТМО -`)
are state territories: placing such a banner requires the `booking.state.place` permission and adds it
to a shared **state zone** — no single owner, distinct style on the map, protection never expires.
Each full name is its own zone, so `JT - Spawn` and `JT - Roads` are two separate state zones.

### Dates

Every zone stores its creation date and every banner its placement date. They are shown in the marker
popup on the web map and drive the protection expiry check (evaluated at break time, nothing is scheduled).

### Banner info on right-click

Right-clicking any zone banner in-world prints the zone info (name, type, owner, founding date,
protection status, copy-clickable coordinates) into the chat of the clicking player. Useful for
single-banner zones placed inside builds (e.g. dorm rooms) when you don't want to open the web map.

## Commands & permissions

| | |
|---|---|
| `/bmb global` | overview GUI with all zones (teleport / delete) |
| `/bmb <player>` | overview GUI with the player's zones |

| Permission | Effect |
|---|---|
| `bmb.overview` | access to the `/bmb` GUI |
| `booking.zone-limit.<rank>` | polygon zone limit by rank (`booking.zone-limit` config section, default 1) |
| `booking.point-limit.<rank>` | point zone limit by rank (`booking.point-limit` config section, default unlimited) |
| `booking.state.place` | place/break state territory banners |
| `booking.admin` | break any zone banner |

## Configuration

See `settings.yml` — protection duration (`booking.protect-days`), zone limits, marker styles for
player/state zones, the state name prefixes and the marker set. Messages live in `language/<lang>.yml`
(en_US, de_DE, ru_RU, fr_FR provided) in [MiniMessage](https://webui.advntr.dev/) format. Marker point
icons can be changed at `assets/marker_<color>.png`. Changes apply after a server restart.

## Migrating

**From BlueMap-BannerMarker v1.x (point markers):** legacy data (`marker/<world>.json`,
`marker/player_markers.json`) is not loaded anymore. Old banners in the world stay as decoration;
players re-mark their territory with sneak-placed banners.

## Building

```
./gradlew build
```

Requires JDK 21. The jar lands in `impl-paper/build/libs/`. Note: this is a 3rd party extension and not
official by BlueMap in any way!
