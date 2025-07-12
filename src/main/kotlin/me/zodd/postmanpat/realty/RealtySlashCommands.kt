package me.zodd.postmanpat.realty

import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.SlashCommandUtils.commandNotLoaded
import me.zodd.postmanpat.command.PPSlashCommand
import me.zodd.postmanpat.command.PostmanCommandProvider
import me.zodd.postmanpat.realty.RealtySlashCommands.RealtyCommands.Companion.areashop
import me.zodd.postmanpat.realty.RealtySlashCommands.RealtyCommands.OWNER_TRANSFER

class RealtySlashCommands : PostmanCommandProvider {

    enum class RealtyCommands(override val command: String) : PPSlashCommand<RealtyCommands> {
        REALTY_BASE(plugin.configManager.conf.moduleConfig.realty.baseCommand),
        PLOT_INFO(plugin.configManager.conf.moduleConfig.realty.plotInfoCommand),
        OWNER_TRANSFER(plugin.configManager.conf.moduleConfig.realty.ownershipTransferCommand),
        RENTAL_TRANSFER(plugin.configManager.conf.moduleConfig.realty.rentalTransferCommand)
        ;

        companion object {
            internal val areashop: AreashopAddon? by lazy {
                return@lazy takeIf { plugin.server.pluginManager.isPluginEnabled("areashop") }?.let { AreashopAddon() }
            }
        }

        override fun exec(): (SlashCommandEvent) -> Unit {
            return when (this) {
                OWNER_TRANSFER -> { s ->
                    areashop?.ownershipTransfer(s) ?: s.commandNotLoaded()
                }
                RENTAL_TRANSFER -> {s ->
                    areashop?.rentalTransfer(s) ?: s.commandNotLoaded()
                }
                PLOT_INFO -> { s ->
                    areashop?.areaInfo(s) ?: s.commandNotLoaded()
                }
                REALTY_BASE -> { _ -> /*This command is never run*/ }
            }
        }

    }

    override fun slashCommands(): List<PluginSlashCommand> {

        return areashop?.let {
            listOf(
                PluginSlashCommand(
                    plugin,
                    CommandData(RealtyCommands.REALTY_BASE.command, "Base command for realty commands")
                        .addSubcommands(
                            SubcommandData(OWNER_TRANSFER.command, "Command to transfer plot ownership").apply {
                                addOption(OptionType.STRING, "region", "property to transfer", true)
                                addOption(OptionType.USER, "user", "user to transfer property to", false)
                                addOption(OptionType.STRING, "player", "player to transfer property to", false)
                            },
                            SubcommandData(RealtyCommands.RENTAL_TRANSFER.command, "Command to transfer a rental").apply {
                                addOption(OptionType.STRING, "region", "rental to transfer", true)
                                addOption(OptionType.USER, "user", "user to transfer property to", false)
                                addOption(OptionType.STRING, "player", "player to transfer property to", false)
                            },
                            SubcommandData(RealtyCommands.PLOT_INFO.command, "Check the info of a plot").apply {
                                addOption(OptionType.STRING, "region", "region to check", true)
                            }
                        )
                )
            )
        } ?: listOf()
    }
}


