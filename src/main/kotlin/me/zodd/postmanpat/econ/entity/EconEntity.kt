package me.zodd.postmanpat.econ.entity

/**
 * Represents an entity that owns a Treasury account money can move to or from
 * (a player's personal account or a firm's account).
 */
interface EconEntity {
    /**
     * The name of the entity
     */
    val name: String

    /**
     * The Treasury account id funds move to/from for this entity.
     */
    val accountId: Int

    /**
     * The entity's current balance, read from [accountId].
     */
    val balance: Double
}
