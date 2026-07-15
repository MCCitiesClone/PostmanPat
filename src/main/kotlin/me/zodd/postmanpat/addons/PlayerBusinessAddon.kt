package me.zodd.postmanpat.addons

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import io.paradaux.business.api.BusinessApi
import io.paradaux.business.model.Firm
import io.paradaux.business.model.RolePermission
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.embedMessage
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeralEmbed
import me.zodd.postmanpat.Utils.SlashCommandUtils.get
import me.zodd.postmanpat.Utils.SlashCommandUtils.userOrPlayerArg
import me.zodd.postmanpat.econ.PostmanEconManager
import me.zodd.postmanpat.econ.entity.BusinessEntity
import me.zodd.postmanpat.econ.entity.UserEntity
import java.awt.Color
import java.util.UUID

class PlayerBusinessAddon {

    // Only constructed once PostmanPat.business is known to be present (see EconSlashCommands).
    private val business: BusinessApi = plugin.business
        ?: error("PlayerBusinessAddon constructed while the Business plugin is unavailable")

    internal fun listOwnedBusinesses(event: SlashCommandEvent, senderUuid: UUID) {
        val embedBuilder = EmbedBuilder()
            .setTitle("Your Businesses")
            .setColor(Color.blue)
            .setFooter(PostmanPat.plugin.configManager.conf.serverBranding)
        business.firms().getPlayerFirms(senderUuid).forEach {
            embedBuilder.addField(it.displayName, "", true)
        }

        event.replyEphemeralEmbed(
            embedBuilder.build()
        ).queue()
    }

    internal fun firmBal(event: SlashCommandEvent, senderUuid: UUID) {
        val businessName = event["business"]?.asString
        val firm: Firm = firmByName(businessName) ?: run {
            event.replyEphemeral("Business by name [$businessName] was not found!").queue()
            return
        }
        event.replyEphemeralEmbed(
            embedMessage(
                "Balance for ${firm.displayName}",
                business.firms().getFormattedTotalBalance(firm.firmId)
            )
        ).queue()
    }

    internal fun firmPay(event: SlashCommandEvent, senderUuid: UUID) {
        val businessName = event["business"]?.asString

        val target = event.userOrPlayerArg() ?: run {
            event.replyEphemeral("Unable to find user! Ensure name is spelled correctly or try @tagging them").queue()
            return
        }

        val firm: Firm = firmByName(businessName) ?: run {
            event.replyEphemeral("Business by name [$businessName] was not found!").queue()
            return
        }

        if (!firm.hasFinancialAccess(senderUuid)) {
            event.replyEphemeral("You do not have permission to view or transfer funds from this business").queue()
            return
        }

        if (firm.defaultAccountId == null) {
            event.replyEphemeral("${firm.displayName} has no account to pay from!").queue()
            return
        }

        val businessSender = BusinessEntity(firm)
        val receiver = UserEntity(target.uuid, target.name)
        PostmanEconManager(businessSender, event).transferFunds(receiver, senderUuid)
    }

    /**
     * Whether [playerId] may move money out of this firm: its proprietor, or an
     * employee with the FINANCIAL or ADMIN permission. Defensive against the
     * BusinessApi throwing for non-employees.
     */
    private fun Firm.hasFinancialAccess(playerId: UUID): Boolean = try {
        business.firms().isProprietor(firmId, playerId)
            || business.staff().hasPermission(firmId, playerId, RolePermission.FINANCIAL)
            || business.staff().hasPermission(firmId, playerId, RolePermission.ADMIN)
    } catch (ex: RuntimeException) {
        false
    }

    internal fun firmByName(name: String?): Firm? {
        return name?.let { business.firms().getFirmByName(it) }
    }
}
