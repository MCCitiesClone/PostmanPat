package me.zodd.postmanpat.realty

import com.earth2me.essentials.User
import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.SlashCommandUtils.emptyCommand
import me.zodd.postmanpat.addons.AreashopAddon
import me.zodd.postmanpat.command.PPSlashCommand
import me.zodd.postmanpat.command.PostmanCommandProvider
import me.zodd.postmanpat.realty.RealtySlashCommands.RealtyCommands.Companion.areashop
import me.zodd.postmanpat.realty.RealtySlashCommands.RealtyCommands.LANDLORD_TRANSFER

class RealtySlashCommands : PostmanCommandProvider {

    enum class RealtyCommands(override val command: String) : PPSlashCommand<RealtyCommands> {
        REALTY_BASE(plugin.configManager.conf.moduleConfig.realty.baseCommand),
        PLOT_INFO(plugin.configManager.conf.moduleConfig.realty.plotInfoCommand),
        LANDLORD_TRANSFER(plugin.configManager.conf.moduleConfig.realty.landlordTransferCommand),
        OWNER_TRANSFER(plugin.configManager.conf.moduleConfig.realty.ownershipTransferCommand)
        ;

        companion object {
            internal val areashop: AreashopAddon? by lazy {
                return@lazy takeIf {
                    plugin.server.pluginManager.isPluginEnabled("areashop") && plugin.configManager.conf.moduleConfig.realty.enabled
                }?.let { AreashopAddon() }
            }
        }

        override fun exec(event: SlashCommandEvent, sender: User) {
            when (this) {
                LANDLORD_TRANSFER -> {
                    areashop?.let {
                        it::landlordTransfer
                    } ?: emptyCommand()
                }

                OWNER_TRANSFER -> {
                    areashop?.let {
                        it::ownerTransfer
                    } ?: emptyCommand()
                }

                PLOT_INFO -> {
                    areashop?.let {
                        it::areaInfo
                    } ?: emptyCommand()
                }

                REALTY_BASE -> emptyCommand()
            }.invoke(event, sender)
        }

    }

    override fun slashCommands(): List<PluginSlashCommand> {

        return areashop?.let {
            listOf(
                PluginSlashCommand(
                    plugin,
                    CommandData(RealtyCommands.REALTY_BASE.command, "Base command for realty commands")
                        .addSubcommands(
                            SubcommandData(LANDLORD_TRANSFER.command, "Command to transfer plot landlordship").apply {
                                addOption(OptionType.STRING, "region", "property to transfer", true)
                                addOption(OptionType.USER, "user", "user to transfer property to", false)
                                addOption(OptionType.STRING, "player", "player to transfer property to", false)
                            },
                            SubcommandData(
                                RealtyCommands.OWNER_TRANSFER.command,
                                "Command to transfer a plots ownership"
                            ).apply {
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


