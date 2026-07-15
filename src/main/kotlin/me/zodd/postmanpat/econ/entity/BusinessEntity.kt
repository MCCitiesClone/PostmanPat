package me.zodd.postmanpat.econ.entity

import io.paradaux.business.model.Firm
import me.zodd.postmanpat.PostmanPat.Companion.plugin

/**
 * A firm, backed by its default Treasury account.
 *
 * The firm must have a [Firm.getDefaultAccountId]; callers are expected to guard
 * against a null default account before constructing this.
 */
class BusinessEntity(firm: Firm) : EconEntity {

    override val name: String = firm.displayName

    override val accountId: Int = firm.defaultAccountId

    override val balance: Double
        get() = plugin.treasury.getBalanceByAccountId(accountId).toDouble()
}
