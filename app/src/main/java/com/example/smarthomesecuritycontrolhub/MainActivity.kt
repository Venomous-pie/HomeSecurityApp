package com.example.smarthomesecuritycontrolhub

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.smarthomesecuritycontrolhub.auth.AccountAuthenticator
import com.example.smarthomesecuritycontrolhub.ui.dashboard.DashboardActivity
import com.example.smarthomesecuritycontrolhub.ui.home.HomePage
import com.example.smarthomesecuritycontrolhub.ui.landing.LandingActivity
import com.example.smarthomesecuritycontrolhub.ui.login.LoginActivity
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme

class MainActivity : ComponentActivity() {

    private lateinit var accountManager: AccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        accountManager = AccountManager.get(this)

        // Check if the user explicitly logged out previously
        try {
            val prefs = getSharedPreferences("app_preferences", android.content.Context.MODE_PRIVATE)
            val userLoggedOut = prefs.getBoolean("user_logged_out", false)
            
            if (userLoggedOut) {
                // If user explicitly logged out, go to landing screen
                Log.d("MainActivity", "User previously logged out, going to landing screen")
                startActivity(Intent(this, LandingActivity::class.java))
                finish()
                return
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking logout status: ${e.message}", e)
            // Continue with normal app startup
        }

        setContent {
            SmartHomeSecurityControlHubTheme {
                var appState by remember { mutableStateOf<AppState>(AppState.Loading) }

                LaunchedEffect(Unit) {
                    appState = checkUserStatus()
                }

                when (val currentAppState = appState) {
                    is AppState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is AppState.LoggedIn -> {
                        // Navigate to Dashboard instead of HomePage
                        val dashboardIntent = Intent(this@MainActivity, DashboardActivity::class.java)
                        startActivity(dashboardIntent)
                        finish()
                    }
                    is AppState.LoggedOut -> {
                        startActivity(Intent(this@MainActivity, LandingActivity::class.java))
                        finish() // Finish MainActivity so it doesn't stay in backstack below LandingActivity
                    }
                    is AppState.Guest -> {
                        // Guest mode uses the same HomePage but with limited functionality
                        HomePage(
                            username = "Guest",
                            authToken = null,
                            onLogout = {
                                // For Guest users, "logout" means go to login screen
                                appState = AppState.LoggedOut
                            }
                        )
                    }
                }
            }
        }
    }

    @RequiresPermission("android.permission.USE_CREDENTIALS")
    private fun checkUserStatus(): AppState {
        try {
            val accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)
            Log.d("MainActivity", "Checking user status, found ${accounts.size} accounts")
            
            if (accounts.isEmpty()) {
                Log.d("MainActivity", "No accounts found, user needs to login or register")
                return AppState.LoggedOut
            }

            val account = accounts[0]
            Log.d("MainActivity", "Found account: ${account.name}")

            try {
                val authToken = accountManager.blockingGetAuthToken(
                    account,
                    AccountAuthenticator.AUTH_TOKEN_TYPE_FULL_ACCESS,
                    true 
                )
                return if (authToken != null) {
                    Log.d("MainActivity", "Auth token retrieved: $authToken for ${account.name}")
                    AppState.LoggedIn(account.name, authToken)
                } else {
                    Log.d("MainActivity", "Auth token is null, user needs to login.")
                    // If we have an account but no token, we should still log out
                    logout() 
                    AppState.LoggedOut
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting auth token", e)
                // If we can't get the token, we should force logout
                logout()
                return AppState.LoggedOut
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Fatal error checking user status", e)
            return AppState.LoggedOut
        }
    }

    private fun logout() {
        try {
            val accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)
            Log.d("MainActivity", "Logging out, found ${accounts.size} accounts")
            
            for (account in accounts) {
                Log.d("MainActivity", "Removing account: ${account.name}")
                // Clear auth token
                accountManager.invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE, null)
                
                // Completely remove the account to ensure full logout
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                        val removed = accountManager.removeAccountExplicitly(account)
                        Log.d("MainActivity", "Account removed explicitly: $removed")
                    } else {
                        // For older Android versions
                        accountManager.removeAccount(account, null, null)
                        Log.d("MainActivity", "Account removal requested (legacy method)")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error removing account", e)
                }
            }
            
            // Double-check if accounts were removed
            val remainingAccounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)
            Log.d("MainActivity", "After logout, remaining accounts: ${remainingAccounts.size}")
            
            // Clear any shared preferences related to the user session
            try {
                val prefs = getSharedPreferences("app_preferences", android.content.Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                
                // Set the logged out flag in SharedPreferences
                prefs.edit().putBoolean("user_logged_out", true).apply()
                
                Log.d("MainActivity", "Preferences updated for logout")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error updating preferences", e)
            }
            
            // Clear app data cache if needed
            try {
                cacheDir.deleteRecursively()
                Log.d("MainActivity", "Cache cleared")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error clearing cache", e)
            }
            
            // After successful logout, navigate to LandingActivity instead of LoginActivity
            val intent = Intent(this, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            
            Log.d("MainActivity", "User logged out successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during logout", e)
        }
    }
}

sealed class AppState {
    object Loading : AppState()
    data class LoggedIn(val username: String, val authToken: String?) : AppState()
    object LoggedOut : AppState()
    object Guest : AppState() 
}