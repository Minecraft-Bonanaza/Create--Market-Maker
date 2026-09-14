# packwiz metadata

`mods/marketcoordination.pw.toml` is a ready-to-use [packwiz](https://packwiz.infra.link/) mod
file for the current release. It points at the GitHub release asset and pins its `sha512` hash.

## Use it

Copy the file into your pack's `mods/` directory and refresh:

```bash
cp packwiz/mods/marketcoordination.pw.toml <your-pack>/mods/
cd <your-pack>
packwiz refresh
```

## Or add it yourself from the release URL

```bash
packwiz url add marketcoordination \
  "https://github.com/Minecraft-Bonanaza/Create--Market-Maker/releases/download/v0.5.0/marketcoordination-0.5.0.jar"
```

When bumping versions, update `filename`, the `url` tag, and the `hash` (packwiz does this
automatically if you re-run `packwiz url add`).
