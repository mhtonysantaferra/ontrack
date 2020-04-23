package net.nemerosa.ontrack.repository

import net.nemerosa.ontrack.model.security.Account
import net.nemerosa.ontrack.model.structure.Token
import java.time.LocalDateTime

/**
 * Access to the `TOKENS` table.
 */
interface TokensRepository {

    /**
     * Invalidates any token for the account designated by its [id].
     */
    fun invalidate(id: Int)

    /**
     * Saves a token.
     *
     * @param id ID of the account
     * @param token Token to save
     * @param time Creation time
     */
    fun save(id: Int, token: String, time: LocalDateTime)

    /**
     * Gets the token for an account.
     *
     * Note that the [Token.validUntil] property is always set to `null`.
     */
    fun getForAccount(account: Account): Token?

}