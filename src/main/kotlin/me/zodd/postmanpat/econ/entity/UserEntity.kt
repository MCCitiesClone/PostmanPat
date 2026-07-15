package me.zodd.postmanpat.econ.entity

import me.zodd.postmanpat.PostmanPat.Companion.plugin
import java.util.UUID

/**
 * A player, backed by their personal Treasury account.
 */
class UserEntity(val uuid: UUID, override val name: String) : EconEntity {

    override val accountId: Int by lazy {
        plugin.treasury.resolveOrCreatePersonal(uuid).accountId
    }

    override val balance: Double
        get() = plugin.treasury.getBalanceByAccountId(accountId).toDouble()
}
