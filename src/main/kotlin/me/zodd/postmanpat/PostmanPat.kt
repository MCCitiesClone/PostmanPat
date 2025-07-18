package me.zodd.postmanpat

import com.earth2me.essentials.IEssentials
import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.api.commands.SlashCommand
import github.scarsz.discordsrv.api.commands.SlashCommandProvider
import github.scarsz.discordsrv.dependencies.jda.api.JDA
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.util.DiscordUtil
import me.zodd.postmanpat.addons.LitebansAddon
import me.zodd.postmanpat.config.ConfigManager
import me.zodd.postmanpat.config.PostmanPatConfig
import me.zodd.postmanpat.econ.EconSlashCommands
import me.zodd.postmanpat.econ.EconSlashCommands.EconCommands.*
import me.zodd.postmanpat.mail.MailListeners
import me.zodd.postmanpat.mail.MailSlashCommands
import me.zodd.postmanpat.mail.MailSlashCommands.MailCommands.*
import me.zodd.postmanpat.mail.MailUserStorage
import me.zodd.postmanpat.realty.RealtySlashCommands
import me.zodd.postmanpat.realty.RealtySlashCommands.RealtyCommands.*
import net.essentialsx.api.v2.events.UserMailEvent
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import me.zodd.postmanpat.Utils.EssxUtils
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral


class PostmanPat : JavaPlugin(), SlashCommandProvider {
    val srv: DiscordSRV = DiscordSRV.getPlugin()
    val ess: IEssentials? = server.pluginManager.getPlugin("Essentials") as IEssentials?

    val configManager by lazy { ConfigManager(plugin, "postmanpatConfig", PostmanPatConfig::class) }
    val userStorageManager by lazy { ConfigManager(plugin, "userStorage", MailUserStorage::class) }

    val econ: Economy by lazy {
        loadEcon() ?: run {
            server.pluginManager.disablePlugin(this)
            throw Error("Failed to load economy! This plugin requires Vault!")
        }
    }

    val jda: JDA
        get() = DiscordUtil.getJda()

    companion object {
        val plugin by lazy { getPlugin(PostmanPat::class.java) }

        val litebans: LitebansAddon? by lazy {
            return@lazy takeIf { plugin.server.pluginManager.isPluginEnabled("LiteBans") }?.let { LitebansAddon() }
        }
    }

    override fun onEnable() {
        // Initializes ConfigManagers
        userStorageManager
        configManager

        Bukkit.getPluginManager().registerEvent(
            UserMailEvent::class.java, object : Listener {
            }, EventPriority.NORMAL,
            { exec: Listener?, e: Event ->
                MailListeners(this).userMailListener(exec, e)
            }, this
        )
    }

    private fun loadEcon(): Economy? {
        server.pluginManager.getPlugin("Vault") ?: return null
        return server.servicesManager.getRegistration(Economy::class.java)?.provider ?: return null
    }

    @SlashCommand(path = "*")
    fun processSlashCommands(event: SlashCommandEvent) {

        // Pre-check for banned users and ensure runner is a synced user.
        val user = EssxUtils.getEssxUser(event)?.let { user ->
            if (litebans?.isLBBanned(user) == true || litebans?.isDeported(user) == true) {
                event.replyEphemeral("You have been deported from the server and cannot use commands.").queue()
                return
            }
            user
        } ?: run {
            event.replyEphemeral("You must be synced to use postman pat's commands!").queue()
            return
        }

        when (event.commandPath.substringBefore("/")) {
            ECON_PAY.command -> ECON_PAY
            ECON_BALANCE.command -> ECON_BALANCE
            MAIL_BASE.command -> when (event.subcommandName) {
                MAIL_READ.command -> MAIL_READ
                MAIL_SEND.command -> MAIL_SEND
                MAIL_IGNORE.command -> MAIL_IGNORE
                MAIL_MARK_READ.command -> MAIL_MARK_READ
                else -> null
            }

            ECON_FIRM_BASE.command -> when (event.subcommandName) {
                ECON_FIRM_PAY.command -> ECON_FIRM_PAY
                ECON_FIRM_LIST.command -> ECON_FIRM_LIST
                ECON_FIRM_BALANCE.command -> ECON_FIRM_BALANCE
                else -> null
            }

            REALTY_BASE.command -> when (event.subcommandName) {
                OWNER_TRANSFER.command -> OWNER_TRANSFER
                RENTAL_TRANSFER.command -> RENTAL_TRANSFER
                PLOT_INFO.command -> PLOT_INFO
                else -> null
            }

            else -> null
        }?.exec(event, user)
    }

    override fun onDisable() {
        userStorageManager.save()
    }

    override fun getSlashCommands(): MutableSet<PluginSlashCommand> {
        return HashSet(
            mutableListOf<PluginSlashCommand>().apply {
                addAll(EconSlashCommands().slashCommands())
                addAll(MailSlashCommands().slashCommands())
                addAll(RealtySlashCommands().slashCommands())
            }
        )
    }
}