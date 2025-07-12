package me.zodd.postmanpat.mail

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import github.scarsz.discordsrv.dependencies.jda.api.entities.PrivateChannel
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.EssxUtils.manager
import net.essentialsx.api.v2.events.UserMailEvent
import org.bukkit.event.Event
import org.bukkit.event.Listener
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class MailListeners(private var plugin: PostmanPat) {

    companion object {
        /*
         * Holds a cache of UUIDs for the purpose of tracking users
         * who have already received a notification.
         * Cache is meant to expire after cooldown period after which
         * the user will be notified again.
         */
        val notificationCache: Cache<UUID, String> = CacheBuilder.newBuilder()
            .expireAfterWrite(plugin.configManager.conf.moduleConfig.mail.notificationCooldown, TimeUnit.MINUTES)
            .build()
    }

    fun userMailListener(exec: Listener?, e: Event) {
        val event = e as UserMailEvent

        val msg = event.message
        val senderUUID = msg.senderUUID
        val senderUser = getEssxUser(senderUUID)

        val mailManager = DiscordMailManager(listOf(msg))
        val recipient = event.recipient
        val discordID = manager().getDiscordId(recipient.uuid)

        if (discordID == null) {
            plugin.logger.warning("Failed to retrieve discord ID, account may not be linked.")
            return
        }

        val user = plugin.jda.getUserById(discordID) ?: run {
            plugin.logger.warning("Failed to get user by ID, are they in the guild?")
            return
        }

        val ignores = plugin.userStorageManager.conf.mailIgnoreList.getOrDefault(recipient.uuid, ArrayList())
        if (senderUser?.let { sender -> getEssxUser(user.id)?.isIgnoredPlayer(sender) } == true
            || (ignores.isNotEmpty() && ignores.contains(senderUUID))
        ) {
            // Don't send a message if ignoring player
            // They will still receive the mail, just not through discord
            return
        }

        val content = """
                Sent: ${Instant.ofEpochMilli(msg.timeSent)}
                Sender: ${msg.senderUsername}
                Message: ${msg.message.replace("§\\w".toRegex(), "")}
                
                """.trimIndent()

        user.openPrivateChannel().queue { c: PrivateChannel ->
            mailManager.splitContent(content).forEach { m: String ->
                c.sendMessage(m).queue(
                    { /* On success */ }
                ) OnFail@{
                    // If uuid is in cache, don't send another notification
                    notificationCache.getIfPresent(recipient.uuid)?.let {
                        // Name was present, return
                        return@OnFail
                    }

                    // If we're unable to send a DM to the user
                    val channelID = plugin.configManager.conf.moduleConfig.mail.notificationChannel

                    plugin.jda.getTextChannelById(channelID)?.let {
                        // Cache the uuid
                        notificationCache.put(recipient.uuid, "")
                        it.sendMessage(user.asMention + " You have received mail! Check it with `/mail read`!")
                            .queue()
                    } ?: run {
                        plugin.logger.warning("Unable to find configured discord channel! $channelID")
                        return@OnFail
                    }
                }
            }
        }
    }
}