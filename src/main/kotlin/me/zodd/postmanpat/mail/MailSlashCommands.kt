package me.zodd.postmanpat.mail

import com.earth2me.essentials.User
import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData
import me.zodd.postmanpat.PostmanPat.Companion.litebans
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.Utils.SlashCommandUtils.emptyCommand
import me.zodd.postmanpat.Utils.SlashCommandUtils.userOrPlayerArg
import me.zodd.postmanpat.command.PPSlashCommand
import me.zodd.postmanpat.command.PostmanCommandProvider
import net.essentialsx.api.v2.services.mail.MailMessage
import java.util.UUID
import me.zodd.postmanpat.Utils.SlashCommandUtils.get

class MailSlashCommands : PostmanCommandProvider {

    enum class MailCommands(override val command: String) : PPSlashCommand<MailCommands> {
        MAIL_BASE(plugin.configManager.conf.moduleConfig.mail.baseCommand),
        MAIL_READ(plugin.configManager.conf.moduleConfig.mail.readSubCommand),
        MAIL_SEND(plugin.configManager.conf.moduleConfig.mail.sendSubCommand),
        MAIL_MARK_READ(plugin.configManager.conf.moduleConfig.mail.markReadSubCommand),
        MAIL_IGNORE(plugin.configManager.conf.moduleConfig.mail.ignoreSubCommand);

        override fun exec(event: SlashCommandEvent, sender: User) {


            when (this) {
                MAIL_READ -> this::mailReadCommand
                MAIL_SEND -> this::mailSendCommand
                MAIL_MARK_READ -> this::markAsReadCommand
                MAIL_IGNORE -> this::ignoreUserCommand
                MAIL_BASE -> emptyCommand()
            }.invoke(event,sender)
        }

        private fun ignoreUserCommand(event: SlashCommandEvent, sender: User) {
            val userOpt = event["user"]
            val uuidOpt = event["uuid"]

            if (userOpt == null && uuidOpt == null) {
                event.replyEphemeral("You may target a user by their @, or by a players UUID!").queue()
                return
            }

            val targetUUID = userOpt?.let {
                getEssxUser(it.asUser.id)?.uuid
            } ?: runCatching {
                return@runCatching UUID.fromString(uuidOpt?.asString)
            }.getOrElse {
                event.replyEphemeral("UUID malformed").queue()
                return
            }

            val userList: MutableList<UUID> =
                plugin.userStorageManager.conf.mailIgnoreList.getOrDefault(sender.uuid, mutableListOf())

            val targetUser = getEssxUser(targetUUID)

            val targetName = targetUser?.name ?: targetUUID.toString()

            if (userList.remove(targetUUID)) {
                event.replyEphemeral("You have removed $targetName to your ignore list.")
                    .queue()
            } else {
                userList.add(targetUUID)
                event.replyEphemeral("You have added $targetName to your ignore list.")
                    .queue()
            }
            plugin.userStorageManager.conf.mailIgnoreList[sender.uuid] = userList
            plugin.userStorageManager.save()
        }

        private fun markAsReadCommand(event: SlashCommandEvent, sender: User) {
            if (litebans?.isLBMuted(sender) == true) {
                event.replyEphemeral("You have been muted from the server and cannot send mail at this time.").queue()
                return
            }
            markMailAsRead(sender, sender.mailMessages)
            event.reply("Mail has been marked as read!").setEphemeral(true).queue()
        }

        private fun mailReadCommand(event: SlashCommandEvent, sender: User) {
            val mailMessage = sender.mailMessages

            val includeRead = event.getOption(plugin.configManager.conf.moduleConfig.mail.includeReadArg)
            val includeBool = includeRead != null && includeRead.asBoolean

            if (!includeBool && sender.unreadMailAmount <= 0) {
                event.replyEphemeral("No new mail!").queue()
                return
            }

            val mailManager = DiscordMailManager(mailMessage, includeBool)

            val content = event["page"]?.asLong?.let { opt ->
                runCatching {
                    mailManager.getPage(opt - 1)
                }.getOrElse {
                    "No mail on page $opt"
                }
            } ?: mailManager.getPage(0L)

            event.replyEphemeral(content).queue()
        }

        private fun mailSendCommand(event: SlashCommandEvent, sender: User) {
            val user = event.userOrPlayerArg() ?: run {
                event.replyEphemeral("Unable to find user! Ensure name is spelled correctly or try @tagging them").queue()
                return
            }

            val message = event["message"]?.asString

            user.sendMail(sender, message)

            event.replyEphemeral("Sent mail to " + user.displayName).queue()
        }

        /**
         * Marks all mail as read in the Essentials mailbox
         *
         * @param user         an Essentials User
         * @param mailMessages The messages to send and mark as read
         */
        private fun markMailAsRead(user: User?, mailMessages: List<MailMessage>) {
            // Essx doesn't have an easy way to mark a single mail as read
            // So we have to re-create all the messages with the read bool
            // This mimics the way Essx does it in their `mail read` command
            val readMail = mailMessages
                .map { m: MailMessage ->
                    MailMessage(
                        true,
                        m.isLegacy,
                        m.senderUsername,
                        m.senderUUID,
                        m.timeSent,
                        m.timeExpire,
                        m.message
                    )
                }.toCollection(arrayListOf())
            user?.setMailList(readMail)
        }
    }

    override fun slashCommands(): List<PluginSlashCommand> {
        return listOf(
            PluginSlashCommand(
                plugin, CommandData(MailCommands.MAIL_BASE.command, "mail command")
                    .addSubcommands(
                        SubcommandData(MailCommands.MAIL_READ.command, "read mail")
                            .addOption(OptionType.INTEGER, "page", "page number to view", false)
                            .addOption(
                                OptionType.BOOLEAN,
                                plugin.configManager.conf.moduleConfig.mail.includeReadArg,
                                "include all mail including already read"
                            ),

                        SubcommandData(MailCommands.MAIL_SEND.command, "send mail")
                            .addOption(OptionType.STRING, "message", "message to send to the user", true)
                            .addOption(OptionType.USER, "user", "user to send mail to.", false)
                            .addOption(OptionType.STRING, "player", "player to send mail to.", false),


                        SubcommandData(MailCommands.MAIL_MARK_READ.command, "Marks all mail as having been read."),

                        SubcommandData(MailCommands.MAIL_IGNORE.command, "Toggle receiving messages from a user")
                            .addOption(OptionType.USER, "user", "User to toggle ignoring", false)
                            .addOption(OptionType.STRING, "uuid", "Use a players UUID directly", false)
                    )
            )
        )
    }
}

