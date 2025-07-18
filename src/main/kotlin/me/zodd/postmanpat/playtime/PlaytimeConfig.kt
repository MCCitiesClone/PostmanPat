package me.zodd.postmanpat.playtime

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
class PlaytimeConfig {
    @field:Comment("The Placeholder to use to check a join date")
    val joinDatePlaceholder: String = "%plan_player_registered%"

    @field:Comment("The Placeholder to use to check total playtime")
    val totalPlaytimePlaceholder: String = "%plan_player_time_active%"

    @field:Comment("The Placeholder to use to check 30 day playtime")
    val monthPlaytimePlaceholder: String = "%plan_player_time_active_month%"

    @field:Comment("The Placeholder to use to check 7 day playtime")
    val weekPlaytimePlaceholder: String = "%plan_player_time_active_week%"

    @field:Comment("The Placeholder to use to check 24 hours playtime")
    val dailyPlaytimePlaceholder: String = "%plan_player_time_active_day%"

    @field:Comment("Playtime Base command")
    val playtimeBaseCommand: String = "playtime"
}