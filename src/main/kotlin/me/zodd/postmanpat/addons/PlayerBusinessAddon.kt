package me.zodd.postmanpat.addons

import com.earth2me.essentials.User
import com.olziedev.playerbusinesses.api.PlayerBusinessesAPI
import com.olziedev.playerbusinesses.api.business.BStaff
import com.olziedev.playerbusinesses.api.business.Business
import com.olziedev.playerbusinesses.api.business.BusinessPermission
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.Utils.MessageUtils.embedMessage
import me.zodd.postmanpat.Utils.SlashCommandUtils.get
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeralEmbed
import me.zodd.postmanpat.Utils.SlashCommandUtils.userOrPlayerArg
import me.zodd.postmanpat.econ.PostmanEconManager
import me.zodd.postmanpat.econ.entity.BusinessEntity
import me.zodd.postmanpat.econ.entity.UserEntity
import java.awt.Color

class PlayerBusinessAddon {

    companion object {
        private val pba: PlayerBusinessesAPI by lazy {
            PlayerBusinessesAPI.getInstance()
        }
    }

    private val econConf = PostmanPat.plugin.configManager.conf.moduleConfig.econ
    private val decimalFormat = econConf.decimalFormat()

    internal fun listOwnedBusinesses(event: SlashCommandEvent, sender: User) {
        val embedBuilder = EmbedBuilder()
            .setTitle("Owned Businesses")
            .setColor(Color.blue)
            .setFooter(PostmanPat.plugin.configManager.conf.serverBranding)
        pba.getBusinessesByPlayer(sender.uuid).map { it.name }.map {
            embedBuilder.addField(it, "", true)
        }

        event.replyEphemeralEmbed(
            embedBuilder.build()
        ).queue()
    }

    internal fun firmBal(event: SlashCommandEvent, sender: User) {
        val businessName = event["business"]?.asString
        val business: Business = pba.getBusinessByName(businessName?.lowercase()) ?: run {
            event.replyEphemeral("Business by name [$businessName] was not found!").queue()
            return
        }
        event.replyEphemeralEmbed(
            embedMessage(
                "Balance for ${business.name}",
                "${econConf.currencySymbol}${decimalFormat.format(business.balance)}"
            )
        ).queue()
    }


    internal fun firmPay(event: SlashCommandEvent, sender: User) {

        val businessName = event["business"]?.asString

        val targetUser = event.userOrPlayerArg() ?: run {
            event.replyEphemeral("Unable to find user! Ensure name is spelled correctly or try @tagging them").queue()
            return
        }

        val business: Business = pba.getBusinessByName(businessName?.lowercase()) ?: run {
            event.replyEphemeral("Business by name [$businessName] was not found!").queue()
            return
        }

        sender.hasFirmPermission(event, business) ?: return

        val businessSender = BusinessEntity(business)
        val receiver = UserEntity(targetUser)
        PostmanEconManager(businessSender, event).transferFunds(receiver)
    }

    private fun User.hasFirmPermission(event: SlashCommandEvent, business: Business): BStaff? {
        return business.staff?.firstOrNull {
            val permCheck = it.role.permission
            it.uuid == uuid && (permCheck.contains(BusinessPermission.FINANCIAL)
                    || permCheck.contains(BusinessPermission.PROPRIETOR)
                    || permCheck.contains(BusinessPermission.ADMINISTRATOR))
        } ?: run {
            event.replyEphemeral("You do not have permission to view or transfer funds from this business")
                .queue()
            null
        }
    }

    internal fun businessByName(name: String): Business? {
        return pba.getBusinessByName(name.lowercase())
    }
}