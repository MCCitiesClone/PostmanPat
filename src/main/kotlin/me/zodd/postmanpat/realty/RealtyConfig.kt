package me.zodd.postmanpat.realty

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class RealtyConfig(
    @field:Comment("Whether the realty module is enabled, requires Areashop plugin")
    val enabled: Boolean = false,
    @field:Comment("Ownership Transfer command")
    val ownershipTransferCommand: String = "transfer-ownership",
    @field:Comment("Rental Transfer command")
    val rentalTransferCommand: String = "transfer-rental",
    @field:Comment("Information about a plot")
    val plotInfoCommand: String = "info",
    @field:Comment("Base realty command")
    val baseCommand: String = "realty"
) {

}