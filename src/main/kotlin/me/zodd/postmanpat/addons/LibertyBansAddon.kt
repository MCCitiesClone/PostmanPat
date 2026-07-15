package me.zodd.postmanpat.addons

import com.earth2me.essentials.User
import me.zodd.postmanpat.PostmanPat
import space.arim.libertybans.api.LibertyBans
import space.arim.libertybans.api.PlayerVictim
import space.arim.libertybans.api.PunishmentType
import space.arim.omnibus.OmnibusProvider
import java.util.UUID

class LibertyBansAddon {

    // LibertyBans registers itself in the Omnibus registry; this is the documented
    // way to obtain the API instance.
    private val libertyBans: LibertyBans =
        OmnibusProvider.getOmnibus().registry.getProvider(LibertyBans::class.java).orElseThrow()

    private val deportationPhrase = PostmanPat.plugin.configManager.conf.moduleConfig.punishment.warnPhrase

    fun isMuted(user: User) = hasActivePunishment(user.uuid, PunishmentType.MUTE)

    fun isBanned(user: User) = hasActivePunishment(user.uuid, PunishmentType.BAN)

    // This is primarily a "DC" specific feature in line with the deportations feature:
    // a player is deported when they hold an active warn whose reason begins with the
    // configured deportation phrase.
    fun isDeported(user: User): Boolean {
        return libertyBans.selector.selectionBuilder()
            .type(PunishmentType.WARN)
            .victim(PlayerVictim.of(user.uuid))
            .build()
            .allSpecificPunishments
            .toCompletableFuture()
            .join()
            .any { it.reason.startsWith(deportationPhrase) }
    }

    // selectionBuilder() defaults to active-only, so expired/undone punishments are
    // excluded from the check.
    private fun hasActivePunishment(uuid: UUID, type: PunishmentType): Boolean {
        return libertyBans.selector.selectionBuilder()
            .type(type)
            .victim(PlayerVictim.of(uuid))
            .build()
            .firstSpecificPunishment
            .toCompletableFuture()
            .join()
            .isPresent
    }
}
