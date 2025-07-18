package me.zodd.postmanpat.econ

import com.earth2me.essentials.User
import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.embedMessage
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeralEmbed
import me.zodd.postmanpat.Utils.SlashCommandUtils.emptyCommand
import me.zodd.postmanpat.Utils.SlashCommandUtils.get
import me.zodd.postmanpat.Utils.SlashCommandUtils.userOrPlayerArg
import me.zodd.postmanpat.addons.PlayerBusinessAddon
import me.zodd.postmanpat.command.PPSlashCommand
import me.zodd.postmanpat.command.PostmanCommandProvider
import me.zodd.postmanpat.econ.EconSlashCommands.EconCommands.Companion.pba
import me.zodd.postmanpat.econ.entity.BusinessEntity
import me.zodd.postmanpat.econ.entity.EconEntity
import me.zodd.postmanpat.econ.entity.UserEntity

class EconSlashCommands : PostmanCommandProvider {

    enum class EconCommands(override val command: String) : PPSlashCommand<EconCommands> {
        ECON_PAY(plugin.configManager.conf.moduleConfig.econ.payCommand),
        ECON_BALANCE(plugin.configManager.conf.moduleConfig.econ.balCommand),
        ECON_FIRM_BASE(plugin.configManager.conf.moduleConfig.econ.firmBaseCommand),
        ECON_FIRM_PAY(plugin.configManager.conf.moduleConfig.econ.firmPayCommand),
        ECON_FIRM_LIST(plugin.configManager.conf.moduleConfig.econ.firmListBusinesses),
        ECON_FIRM_BALANCE(plugin.configManager.conf.moduleConfig.econ.firmBalCommand),
        ;

        companion object {
            internal val pba: PlayerBusinessAddon? by lazy {
                return@lazy takeIf { plugin.server.pluginManager.isPluginEnabled("democracybusiness") }?.let { PlayerBusinessAddon() }
            }
        }

        override fun exec(event: SlashCommandEvent, sender: User) {
            when (this) {
                ECON_PAY -> this::payUserCommand
                ECON_BALANCE -> this::balanceUserCommand
                ECON_FIRM_PAY -> {
                    pba?.let {
                        it::firmPay
                    } ?: emptyCommand()
                }

                ECON_FIRM_LIST -> {
                    pba?.let { it::listOwnedBusinesses } ?: emptyCommand()
                }

                ECON_FIRM_BALANCE -> {
                    pba?.let {
                        it::firmBal
                    } ?: emptyCommand()
                }

                ECON_FIRM_BASE -> emptyCommand()

            }.invoke(event, sender)
        }

        private val config = plugin.configManager.conf
        private val econConfig = config.moduleConfig.econ
        private val decimalFormat = econConfig.decimalFormat()

        private fun payUserCommand(event: SlashCommandEvent, sender: User) {
            val senderEntity = sender.let(::UserEntity)

            val targetEntity: EconEntity = pba?.let { api ->
                event["business"]?.let { option ->
                    api.businessByName(option.asString)?.let(::BusinessEntity)
                }
            } ?: event.userOrPlayerArg()?.let(::UserEntity) ?: run {
                event.replyEphemeral("Unable to find user! Ensure name is spelled correctly or try @tagging them").queue()
                return
            }

            PostmanEconManager(senderEntity, event).transferFunds(targetEntity)
        }

        private fun balanceUserCommand(event: SlashCommandEvent, sender: User) {
            val targetUser = event.userOrPlayerArg() ?: sender

            val target = UserEntity(targetUser)

            event.replyEphemeralEmbed(
                embedMessage(
                    "Balance for ${target.name}",
                    "They currently have ${econConfig.currencySymbol}${decimalFormat.format(target.balance)} available in their balance"
                )
            ).queue()
        }
    }

    override fun slashCommands(): List<PluginSlashCommand> {

        // This is offered an optional argument from PlayerBusinesses
        val payCommand = CommandData(EconCommands.ECON_PAY.command, "Pay's the target user a specified amount").apply {
            addOption(OptionType.NUMBER, "amount", "amount to pay user", true)
            addOption(OptionType.USER, "user", "user to pay by @tag", false)
            addOption(OptionType.STRING, "player", "player to pay by username", false)
        }

        val commands = mutableListOf(
            PluginSlashCommand(
                plugin,
                CommandData(EconCommands.ECON_BALANCE.command, "Checks the balance of the target or sender").apply {
                    addOption(OptionType.USER, "user", "user to check balance of", false)
                    addOption(OptionType.STRING, "player", "player to check balance of", false)
                }
            )
        )
        // Add command if PlayerBusinesses is enabled
        pba?.let {
            commands.add(
                PluginSlashCommand(
                    plugin, CommandData(EconCommands.ECON_FIRM_BASE.command, "Base command for business transactions")
                        .addSubcommands(
                            SubcommandData(EconCommands.ECON_FIRM_PAY.command, "Command to pay from your business")
                                .addOption(OptionType.STRING, "business", "business to pay from", true)
                                .addOption(OptionType.NUMBER, "amount", "amount to pay another user", true)
                                .addOption(OptionType.USER, "user", "user to pay by @tag", false)
                                .addOption(OptionType.STRING, "player", "player to pay by username", false),
                            SubcommandData(
                                EconCommands.ECON_FIRM_BALANCE.command,
                                "Command to check your businesses balance"
                            ).addOption(OptionType.STRING, "business", "business to check balance of", true),
                            SubcommandData(
                                EconCommands.ECON_FIRM_LIST.command,
                                "Lists businesses you have financial access to"
                            )
                        )
                )
            )
            // Add business option, or if null just the regular command
            commands.add(
                PluginSlashCommand(
                    plugin,
                    payCommand.addOption(OptionType.STRING, "business", "business to pay", false)
                )
            )
        } ?: commands.add(PluginSlashCommand(plugin, payCommand))

        return commands
    }
}

