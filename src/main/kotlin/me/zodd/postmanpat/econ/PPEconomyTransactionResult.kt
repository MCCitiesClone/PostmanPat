package me.zodd.postmanpat.econ

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral

enum class PPEconomyTransactionResult(private val msg: String) {
    INSUFFICIENT_FUNDS("You are too poor for this transaction!"),
    UNDER_MINIMUM("Amount must be more than ${plugin.configManager.conf.moduleConfig.econ.minimumSendable}!"),
    SENT_TO_SELF("You cannot send money to yourself!"),
    TRANSFER_FAILED("The transaction could not be completed. Please try again later."),
    SUCCESS("Success"); // Message won't be sent

    fun isSuccess() = this == SUCCESS

    fun emitError(event: SlashCommandEvent): PPEconomyTransactionResult {
        if (!isSuccess())
            event.replyEphemeral(msg).queue()
        return this
    }
}
