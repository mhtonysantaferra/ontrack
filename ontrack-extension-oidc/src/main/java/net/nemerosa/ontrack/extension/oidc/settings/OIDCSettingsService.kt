package net.nemerosa.ontrack.extension.oidc.settings

/**
 * Management of OIDC providers.
 */
interface OIDCSettingsService {

    /**
     * Gets the list of providers
     */
    val providers: List<OntrackOIDCProvider>

}