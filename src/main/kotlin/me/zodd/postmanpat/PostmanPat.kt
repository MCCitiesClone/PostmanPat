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
import me.zodd.postmanpat.playtime.PlaytimeSlashCommands.PlaytimeCommands.*
import io.paradaux.business.api.BusinessApi
import io.paradaux.treasury.api.TreasuryApi
import net.essentialsx.api.v2.events.UserMailEvent
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import me.zodd.postmanpat.Utils.EssxUtils
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.addons.PapiAddon
import me.zodd.postmanpat.playtime.PlaytimeSlashCommands

class PostmanPat : JavaPlugin(), SlashCommandProvider {
    val srv: DiscordSRV = DiscordSRV.getPlugin()
    val ess: IEssentials? = server.pluginManager.getPlugin("Essentials") as IEssentials?

    val configManager by lazy { ConfigManager(plugin, "postmanpatConfig", PostmanPatConfig::class) }
    val userStorageManager by lazy { ConfigManager(plugin, "userStorage", MailUserStorage::class) }

    val treasury: TreasuryApi by lazy {
        loadTreasury() ?: run {
            server.pluginManager.disablePlugin(this)
            throw Error("Failed to load economy! This plugin requires Treasury!")
        }
    }

    /** Business API, present only when the optional Business plugin is installed. */
    val business: BusinessApi? by lazy {
        takeIf { server.pluginManager.isPluginEnabled("Business") }
            ?.let { server.servicesManager.getRegistration(BusinessApi::class.java)?.provider }
    }

    val jda: JDA
        get() = DiscordUtil.getJda()

    val litebans: LitebansAddon? by lazy {
        return@lazy takeIf { plugin.server.pluginManager.isPluginEnabled("LiteBans") }?.let { LitebansAddon() }
    }

    val papi: PapiAddon? by lazy {
        return@lazy takeIf { plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI") }?.let { PapiAddon() }
    }

    companion object {
        val plugin by lazy { getPlugin(PostmanPat::class.java) }
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

    private fun loadTreasury(): TreasuryApi? {
        return server.servicesManager.getRegistration(TreasuryApi::class.java)?.provider
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

        // Commands are processed here, the enum stores the command string #command
        // Then should point to the Enum itself
        // Return null if no valid options
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

            // Business (firm) commands
            ECON_FIRM_BASE.command -> when (event.subcommandName) {
                ECON_FIRM_PAY.command -> ECON_FIRM_PAY
                ECON_FIRM_LIST.command -> ECON_FIRM_LIST
                ECON_FIRM_BALANCE.command -> ECON_FIRM_BALANCE
                else -> null
            }

            // Realty commands
            REALTY_BASE.command -> when (event.subcommandName) {
                LANDLORD_TRANSFER.command -> LANDLORD_TRANSFER
                OWNER_TRANSFER.command -> OWNER_TRANSFER
                PLOT_INFO.command -> PLOT_INFO
                else -> null
            }

            PLAYTIME_CHECK.command -> PLAYTIME_CHECK

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
                addAll(PlaytimeSlashCommands().slashCommands())
            }
        )
    }
}