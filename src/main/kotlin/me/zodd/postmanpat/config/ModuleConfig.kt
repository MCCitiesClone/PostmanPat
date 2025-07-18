package me.zodd.postmanpat.config

import me.zodd.postmanpat.econ.EconConfig
import me.zodd.postmanpat.mail.MailConfig
import me.zodd.postmanpat.playtime.PlaytimeConfig
import me.zodd.postmanpat.realty.RealtyConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ModuleConfig(
    val mail: MailConfig = MailConfig(),
    val econ: EconConfig = EconConfig(),
    val realty : RealtyConfig = RealtyConfig(),
    val punishment: PunishmentConfig = PunishmentConfig(),
    val playtime : PlaytimeConfig = PlaytimeConfig(),
)