package me.zodd.postmanpat.playtime

import com.earth2me.essentials.User
import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.Utils.SlashCommandUtils.emptyCommand
import me.zodd.postmanpat.Utils.SlashCommandUtils.playerFromUserOrPlayerArg
import me.zodd.postmanpat.command.PPSlashCommand
import me.zodd.postmanpat.command.PostmanCommandProvider

class PlaytimeSlashCommands : PostmanCommandProvider {
    enum class PlaytimeCommands(override val command: String) : PPSlashCommand<PlaytimeCommands> {
        PLAYTIME_CHECK(plugin.configManager.conf.moduleConfig.playtime.playtimeBaseCommand);

        override fun exec(event: SlashCommandEvent, sender: User) {
            when (this) {
                PLAYTIME_CHECK -> {
                    plugin.papi?.let {
                        this::checkPlaytime
                    } ?: emptyCommand()
                }
            }.invoke(event, sender)
        }

        private val playtimeConfig = plugin.configManager.conf.moduleConfig.playtime

        fun checkPlaytime(event: SlashCommandEvent, sender: User) {
            event.playerFromUserOrPlayerArg().let {
                (it ?: sender.offline).let { player ->
                    plugin.papi?.parsePlaytime(
                        player, """
                    [${player.name}]
                    Join Date: ${playtimeConfig.joinDatePlaceholder}
                    Total Playtime: ${playtimeConfig.totalPlaytimePlaceholder}
                    30 Day Playtime: ${playtimeConfig.monthPlaytimePlaceholder}
                    7 Day Playtime: ${playtimeConfig.weekPlaytimePlaceholder}
                    24 Hour Playtime: ${playtimeConfig.dailyPlaytimePlaceholder}
                """.trimIndent()
                    )
                }
            }?.let {
                event.replyEphemeral(it).queue()
            }


        }

    }

    override fun slashCommands(): List<PluginSlashCommand> {
        return plugin.papi?.let {
            listOf(
                PluginSlashCommand(
                    plugin,
                    CommandData(PlaytimeCommands.PLAYTIME_CHECK.command, "mail command")
                        .addOption(OptionType.USER, "user", "user to send mail to.", false)
                        .addOption(OptionType.STRING, "player", "player to send mail to.", false),
                )
            )
        } ?: listOf()
    }
}