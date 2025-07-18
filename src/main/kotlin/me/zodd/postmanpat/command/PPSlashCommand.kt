package me.zodd.postmanpat.command

import com.earth2me.essentials.User
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent

interface PPSlashCommand<T : PPSlashCommand<T>> {
    val command: String

    fun exec(event: SlashCommandEvent, sender: User)
}