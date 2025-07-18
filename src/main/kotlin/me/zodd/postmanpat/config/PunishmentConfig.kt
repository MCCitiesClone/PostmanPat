package me.zodd.postmanpat.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
class PunishmentConfig {
    @field:Comment("Warning phrase to check for to determine if a player is deported. Case sensitive")
    val warnPhrase: String = "Deportation:"
}