# Backup and Restore

Life HUD stores user facts as JSON files and uploaded media in one data directory. The default is `./data` relative to the process working directory. Override it with `lifehud.data-dir` or `LIFEHUD_DATA_DIR`; the legacy `OTAKU_ENERGY_DATA_DIR` remains supported.

## Back up

1. Stop Life HUD so no write can occur during the copy.
2. Copy the entire data directory, including `content/` and `uploads/`, to a dated folder or archive.
3. Keep the backup outside the repository. Do not commit it.
4. Record the Life HUD version used when the backup was made.

Copying only selected JSON files is not recommended: business records, `LifeEvent`, Growth receipts, snapshots, and media paths form one fact system.

## Restore

1. Stop Life HUD.
2. Move the current data directory aside instead of deleting it.
3. Restore the complete backed-up directory to the configured location.
4. Start Life HUD and verify Today, Journal, Growth, and `/api/agent/context/today`.

For an upgrade, back up first, then start the newer version against a copy when possible. v1.0 reads older missing fields with defaults and does not require clearing data. A malformed JSON file is reported with its filename and is not silently replaced with an empty file.

Uploaded images, covers, wallpapers, and audio under `uploads/` are part of the backup. If they are omitted, records remain but their media cannot be displayed.
