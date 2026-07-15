package me.zodd.postmanpat.econ

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import io.paradaux.treasury.model.economy.TransferRequest
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.embedMessage
import me.zodd.postmanpat.econ.entity.EconEntity
import java.math.BigDecimal
import java.util.UUID

class PostmanEconManager(private val sender: EconEntity, private val event: SlashCommandEvent) {

    private val econConf = plugin.configManager.conf.moduleConfig.econ
    private val decimalFormat = econConf.decimalFormat()

    /**
     * Moves the command's "amount" from [sender] to [recipient] as a single atomic
     * Treasury transfer, initiated by [actor] (the acting player).
     */
    fun transferFunds(recipient: EconEntity, actor: UUID) {
        takeUnless { sender.accountId == recipient.accountId } ?: run {
            PPEconomyTransactionResult.SENT_TO_SELF.emitError(event)
            return
        }

        val amount = checkAmountOption() ?: run {
            PPEconomyTransactionResult.UNDER_MINIMUM.emitError(event)
            return
        }

        takeIf { sender.balance >= amount } ?: run {
            PPEconomyTransactionResult.INSUFFICIENT_FUNDS.emitError(event)
            return
        }

        val request = TransferRequest(
            sender.accountId,
            recipient.accountId,
            BigDecimal.valueOf(amount),
            "${sender.name} -> ${recipient.name}",
            actor,
            null,
            "PostmanPat",
            null,
        )

        try {
            plugin.treasury.transfer(request)
        } catch (ex: RuntimeException) {
            plugin.logger.warning("Transfer from ${sender.name} to ${recipient.name} failed: ${ex.message}")
            PPEconomyTransactionResult.TRANSFER_FAILED.emitError(event)
            return
        }

        event.replyEmbeds(
            embedMessage(
                "${sender.name} -> ${recipient.name}",
                "You have sent ${econConf.currencySymbol}${decimalFormat.format(amount)} to ${recipient.name}"
            )
        ).queue()
    }

    private fun isAtleastMinimum(amount: Double): Boolean {
        return amount >= plugin.configManager.conf.moduleConfig.econ.minimumSendable
    }

    private fun checkAmountOption(): Double? {
        return event.getOption("amount")?.asDouble?.takeIf { isAtleastMinimum(it) }
    }
}
