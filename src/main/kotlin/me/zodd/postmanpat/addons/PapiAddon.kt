package me.zodd.postmanpat.addons

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.OfflinePlayer

class PapiAddon {
    fun parsePlaytime(player: OfflinePlayer, text: String): String {
        return PlaceholderAPI.setPlaceholders(player, text)
    }
}