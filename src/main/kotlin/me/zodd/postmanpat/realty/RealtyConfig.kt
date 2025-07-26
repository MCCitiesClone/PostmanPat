package me.zodd.postmanpat.realty

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class RealtyConfig(
    @field:Comment("Whether the realty module is enabled, requires Areashop plugin")
    val enabled: Boolean = false,
    @field:Comment("Landlord Transfer command")
    val landlordTransferCommand: String = "transfer-landlord",
    @field:Comment("Ownership Transfer command")
    val ownershipTransferCommand: String = "transfer-ownership",
    @field:Comment("Information about a plot")
    val plotInfoCommand: String = "info",
    @field:Comment("Base realty command")
    val baseCommand: String = "realty",
    @field:Comment("The world where shop regions are found")
    val regionWorld: String = "world"
)