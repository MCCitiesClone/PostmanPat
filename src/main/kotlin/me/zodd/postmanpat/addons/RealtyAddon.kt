package me.zodd.postmanpat.addons

import com.earth2me.essentials.User
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook
import io.github.md5sha256.realty.api.RealtyBackend
import io.github.md5sha256.realty.api.RealtyPaperApi
import io.github.md5sha256.realty.api.WorldGuardRegion
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.SlashCommandUtils.get
import me.zodd.postmanpat.Utils.SlashCommandUtils.userOrPlayerArg
import org.bukkit.World
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Realty integration, replacing the previous AreaShop addon.
 *
 * In Realty a property's *owner* is the freehold title holder, and a rental's
 * *landlord* is the leasehold landlord. Every [RealtyPaperApi] call is asynchronous,
 * so each command defers its Discord reply up front and responds through the
 * interaction hook once the future completes.
 */
class RealtyAddon {

    private val api: RealtyPaperApi by lazy {
        plugin.server.servicesManager.getRegistration(RealtyPaperApi::class.java)?.provider
            ?: error("Realty API service is not registered")
    }

    private val regionWorld: World?
        get() = plugin.server.worlds.firstOrNull {
            it.name.equals(plugin.configManager.conf.moduleConfig.realty.regionWorld, true)
        }

    private fun resolveRegion(regionName: String?, world: World): ProtectedRegion? {
        if (regionName.isNullOrBlank()) return null
        val manager = WorldGuard.getInstance().platform.regionContainer
            .get(BukkitAdapter.adapt(world)) ?: return null
        return manager.getRegion(regionName)
    }

    fun areaInfo(event: SlashCommandEvent, sender: User) {
        event.deferReply(true).queue()
        val world = regionWorld
            ?: return event.hook.respond("Configured region world is not loaded!")
        val region = resolveRegion(event["region"]?.asString, world)
            ?: return event.hook.respond("Region may not exist!")

        api.getRegionInfo(region.id, world.uid).whenComplete { info, err ->
            if (err != null || info == null) {
                event.hook.respond("Failed to look up region info!")
                return@whenComplete
            }
            val owner = info.freehold()?.titleHolderId()?.let { getEssxUser(it)?.name } ?: "Unowned"
            val landlord = info.leasehold()?.landlordId()?.let { getEssxUser(it)?.name } ?: "None"
            event.hook.respond(
                """
                Region: ${region.id}
                Landlord: $landlord
                Owner: $owner
                """.trimIndent()
            )
        }
    }

    fun landlordTransfer(event: SlashCommandEvent, sender: User) {
        transfer(event) { region, world, target, runner, info ->
            val landlordId = info.leasehold()?.landlordId()
            when {
                landlordId == null -> {
                    event.hook.respond("Property ${region.id} is not a rentable (leasehold) property!")
                    null
                }
                landlordId != runner -> {
                    event.hook.respond("You are not the landlord of the property: ${region.id}")
                    null
                }
                else -> api.setLandlord(WorldGuardRegion(region, world), target.uuid)
                    .thenApply { it is RealtyPaperApi.SetLandlordResult.Success }
            }
        }
    }

    fun ownerTransfer(event: SlashCommandEvent, sender: User) {
        transfer(event) { region, world, target, runner, info ->
            val ownerId = info.freehold()?.titleHolderId()
            when {
                info.freehold() == null -> {
                    event.hook.respond("Property ${region.id} is not an ownable (freehold) property!")
                    null
                }
                ownerId != runner -> {
                    event.hook.respond("You are not the owner of the property: ${region.id}")
                    null
                }
                else -> api.transferTitleHolder(WorldGuardRegion(region, world), target.uuid)
                    .thenApply { it is RealtyPaperApi.SetTitleHolderResult.Success }
            }
        }
    }

    /**
     * Shared flow for the transfer commands: validates the caller, resolves the
     * region and target, loads the current [RealtyBackend.RegionInfo], then defers
     * to [action] to authorise and perform the change. [action] returns the future
     * of the operation (true on success), or null if it already replied with a failure.
     */
    private fun transfer(
        event: SlashCommandEvent,
        action: (region: ProtectedRegion, world: World, target: User, runner: UUID, info: RealtyBackend.RegionInfo) -> CompletableFuture<Boolean>?
    ) {
        event.deferReply(true).queue()
        val runner = getEssxUser(event)?.uuid
            ?: return event.hook.respond("You may not be linked!")
        val world = regionWorld
            ?: return event.hook.respond("Configured region world is not loaded!")
        val regionName = event["region"]?.asString
        val region = resolveRegion(regionName, world)
            ?: return event.hook.respond("Region: $regionName, may not exist!")
        val target = event.userOrPlayerArg()
            ?: return event.hook.respond("Cannot find user or player!")

        api.getRegionInfo(region.id, world.uid).whenComplete { info, err ->
            if (err != null || info == null) {
                event.hook.respond("Failed to look up region info!")
                return@whenComplete
            }
            val outcome = action(region, world, target, runner, info) ?: return@whenComplete
            outcome.whenComplete { success, actionErr ->
                if (actionErr == null && success == true) {
                    event.hook.respond("Property ${region.id} transferred to ${target.name}", ephemeral = false)
                } else {
                    event.hook.respond("Transfer failed for property: ${region.id}")
                }
            }
        }
    }

    /** Sends a follow-up to a deferred interaction, ephemeral by default. */
    private fun InteractionHook.respond(message: String, ephemeral: Boolean = true) {
        setEphemeral(ephemeral).sendMessage(message).queue()
    }
}
