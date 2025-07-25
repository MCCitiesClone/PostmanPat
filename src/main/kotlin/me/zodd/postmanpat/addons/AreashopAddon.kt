package me.zodd.postmanpat.addons

import com.earth2me.essentials.User
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.wiefferink.areashop.AreaShop
import me.wiefferink.areashop.regions.GeneralRegion
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.Utils.SlashCommandUtils.get
import me.zodd.postmanpat.Utils.SlashCommandUtils.userOrPlayerArg
import java.util.UUID

class AreashopAddon {
    private val areashop: AreaShop by lazy {
        AreaShop.getInstance()
    }

    private val fileManager get() = areashop.fileManager

    fun areaInfo(event: SlashCommandEvent, sender: User) {
        fileManager?.getRegion(event["region"]?.asString)?.let { rg ->
            event.replyEphemeral(
                """
                Region: ${rg.name}
                Landlord: ${rg.landlordName}
                Owner: ${getEssxUser(rg.owner)?.name ?: "Unowned"}
            """.trimIndent()
            ).queue()
        } ?: event.replyEphemeral("Region may not exist!").queue()
    }

    fun landlordTransfer(event: SlashCommandEvent, sender: User) {
        transfer(event) { rg, user, runner ->
            if (!rg.isLandlord(runner)) {
                event.replyEphemeral("You are not the landlord of the property: ${rg.name}").queue()
                return@transfer false
            }
            rg.setLandlord(user.uuid, user.name)
            return@transfer true
        }
    }


    fun ownerTransfer(event: SlashCommandEvent, sender: User) {
        transfer(event) { rg, user, runner ->
            if (!rg.isOwner(runner)) {
                event.replyEphemeral("You are not the owner of the property: ${rg.name}").queue()
                return@transfer false
            }

            rg.owner = user.uuid
            return@transfer true
        }
    }

    private fun transfer(
        event: SlashCommandEvent,
        transferProperty: (region: GeneralRegion, target: User, runner: UUID) -> Boolean
    ) {
        val commandRunnerId = getEssxUser(event)?.uuid ?: run {
            event.replyEphemeral("You may not be linked!").queue()
            return
        }
        val regionName = event["region"]?.asString
        val targetOwner = event.userOrPlayerArg() ?: run {
            event.replyEphemeral("Cannot find user or player!").queue()
            return
        }

        fileManager?.getRegion(regionName)?.let { rg ->
            takeIf { transferProperty(rg, targetOwner, commandRunnerId) }?.let {
                event.reply("Property $regionName transferred to ${targetOwner.name}").queue()
            } ?: return
        } ?: event.replyEphemeral("Region: $regionName, may not exist!").queue()
    }
}