# PostmanPat

## Dependencies

All dependencies are `compileOnly` — they are provided at runtime by the
corresponding server plugins, not shaded into PostmanPat.

### Required plugins

These must be present for PostmanPat to enable (`required: true` in
`paper-plugin.yml`):

| Plugin     | Compiled against | Purpose                                             |
|------------|------------------|-----------------------------------------------------|
| DiscordSRV | 1.29.0           | Bot instance + slash-command API; Discord↔MC linking |
| EssentialsX| 2.21.0           | Player identity / account resolution and mail        |
| Treasury   | `v2.3.379`¹      | Economy backend — all balances and transfers         |

### Optional plugins

Features that use these are only registered when the plugin is installed
(`required: false`):

| Plugin        | Compiled against | Enables                                          |
|---------------|------------------|--------------------------------------------------|
| Business      | `v2.3.379`¹      | `/firm` commands (firm balance, pay, list)       |
| Realty        | `v1.4.4`²        | Realty commands (plot transfer/info)             |
| WorldGuard    | 7.0.14           | Region handling used by the Realty integration   |
| LibertyBans   | 1.1.4            | Deport/ban/mute checks before running commands   |
| PlaceholderAPI| 2.11.6           | Placeholder expansion                            |

¹ Hibernia Economy — the `business-api` and `treasury-api` surfaces are
resolved from JitPack (`com.github.MCCitiesClone.hibernia-economy:*`). The tag
is set by `hiberniaEconomyVersion` in `build.gradle.kts`.

² Realty — `realty-paper-api`, resolved from JitPack
(`com.github.MCCitiesClone.realty:realty-paper-api`); tag set by `realtyVersion`.

### Build-time libraries

Pulled from Maven and loaded at runtime by `ExternalDependencyLoader`: the
Kotlin stdlib, Configurate (hocon + extra-kotlin, 4.1.2), and kotlinx-coroutines.
Targets Paper 1.21.4 (Java 21).

## Features

### Description

This plugin uses DiscordSRV's Bot instance and api to add SlashCommands to allow
for mail, and economy services.

### Commands

Mail Commands
* `/mail read [page] [include-read]`
* `/mail send <user> <message>`
* `/mail mark-read`
* `/mail ignore [user] [uuid]`

Econ Commands (default names; several are configurable)
* `/balance [user]`
* `/pay <user> <amount> [business]`

Firm Commands (require the Business plugin)
* `/firm balance <business>`
* `/firm pay <business> <amount> <user>`
* `/firm list`

All economy operations go directly through Treasury: `/pay` and `/firm pay`
are performed as single atomic Treasury transfers, and balances are read from
Treasury accounts (personal accounts for players, the firm's default account
for businesses). Considerations have been made for negative values, zero
values, and insufficient balances.

### Config

The important config node is `NotifyChannel` which should be set
to a public textchannel for users who cannot receive a DM, to be pinged
about new mail. An invalid channel ID will log an error in console

Some sub-commands have been made configurable, as a bit of an oversight root commands
aren't currently configurable due to how commands have been implemented.

