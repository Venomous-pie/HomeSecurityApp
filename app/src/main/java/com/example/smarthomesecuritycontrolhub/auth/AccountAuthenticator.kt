package com.example.smarthomesecuritycontrolhub.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.smarthomesecuritycontrolhub.ui.login.LoginActivity // We'll create this later

class AccountAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {

    companion object {
        const val ACCOUNT_TYPE = "com.example.smarthomesecuritycontrolhub.account"
        const val AUTH_TOKEN_TYPE_FULL_ACCESS = "full_access"
    }

    override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle? {
        // Not implemented
        throw UnsupportedOperationException()
    }

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle? {
        val intent = Intent(context, LoginActivity::class.java) // Or your Compose Auth Activity
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(LoginActivity.EXTRA_ACCOUNT_TYPE, accountType)
        intent.putExtra(LoginActivity.EXTRA_AUTH_TOKEN_TYPE, authTokenType)
        intent.putExtra(LoginActivity.EXTRA_IS_ADDING_NEW_ACCOUNT, true)

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle? {
        // Not implemented, can be used for re-confirming password
        throw UnsupportedOperationException()
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? {
        val accountManager = AccountManager.get(context)
        var authToken = accountManager.peekAuthToken(account, authTokenType)

        // If we have an authToken, return it
        if (!authToken.isNullOrEmpty()) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account?.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account?.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            return result
        }

        // If not, try to refresh it or ask the user to login again
        // For now, we'll just ask to login again by starting LoginActivity
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(LoginActivity.EXTRA_ACCOUNT_NAME, account?.name)
        intent.putExtra(LoginActivity.EXTRA_AUTH_TOKEN_TYPE, authTokenType)
        
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthTokenLabel(authTokenType: String?): String? {
        // You can customize this label based on the authTokenType
        return if (AUTH_TOKEN_TYPE_FULL_ACCESS == authTokenType) {
            "Full access to SmartHome Hub"
        } else {
            null
        }
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? {
        // Not implemented
        throw UnsupportedOperationException()
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle? {
        // Not implemented, check if account supports certain features
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false) // Default to false
        return result
    }
} 