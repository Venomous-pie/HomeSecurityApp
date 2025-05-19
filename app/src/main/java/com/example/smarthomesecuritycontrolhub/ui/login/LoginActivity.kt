package com.example.smarthomesecuritycontrolhub.ui.login

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.smarthomesecuritycontrolhub.auth.AccountAuthenticator
import com.example.smarthomesecuritycontrolhub.ui.dashboard.DashboardActivity
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme
import com.example.smarthomesecuritycontrolhub.ui.theme.TextBlack
import com.example.smarthomesecuritycontrolhub.ui.theme.TextWhite

class LoginActivity : ComponentActivity() {

    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var resultBundle: Bundle? = null

    // Flag to indicate if finish() should report success or error to AccountManager
    private var reportSuccessToAccountManager = false

    companion object {
        const val EXTRA_ACCOUNT_TYPE = "com.example.smarthomesecuritycontrolhub.ACCOUNT_TYPE"
        const val EXTRA_AUTH_TOKEN_TYPE = "com.example.smarthomesecuritycontrolhub.AUTH_TOKEN_TYPE"
        const val EXTRA_ACCOUNT_NAME = "com.example.smarthomesecuritycontrolhub.ACCOUNT_NAME"
        const val EXTRA_IS_ADDING_NEW_ACCOUNT = "com.example.smarthomesecuritycontrolhub.IS_ADDING_NEW_ACCOUNT"
        const val EXTRA_REGISTRATION_SUCCESSFUL = "com.example.smarthomesecuritycontrolhub.REGISTRATION_SUCCESSFUL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME)
        // This is crucial: true if AccountManager itself is asking to add an account of this type.
        val isSystemAddingNewAccount = intent.getBooleanExtra(EXTRA_IS_ADDING_NEW_ACCOUNT, false)
        // Check if this is coming from a successful registration
        val isFromSuccessfulRegistration = intent.getBooleanExtra(EXTRA_REGISTRATION_SUCCESSFUL, false)

        setContent {
            SmartHomeSecurityControlHubTheme {
                LoginScreen(
                    accountName = accountName,
                    isSystemAddingNewAccount = isSystemAddingNewAccount,
                    isFromSuccessfulRegistration = isFromSuccessfulRegistration,
                    onLoginAttempt = { username, password, isRegistering ->
                        // TODO: Replace with actual auth logic (network call, etc.)
                        val simulatedAuthToken = "fake_auth_token_for_${username}"
                        val accountManager = AccountManager.get(this)

                        if (isRegistering) { // User wants to register a new account
                            val account = android.accounts.Account(username, AccountAuthenticator.ACCOUNT_TYPE)
                            try {
                                if (accountManager.addAccountExplicitly(account, password, null /* userdata */)) {
                                    accountManager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_TYPE_FULL_ACCESS, simulatedAuthToken)
                                    
                                    // Successfully added and set token
                                    val bundle = Bundle()
                                    bundle.putString(AccountManager.KEY_ACCOUNT_NAME, username)
                                    bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, intent.getStringExtra(EXTRA_ACCOUNT_TYPE))
                                    bundle.putString(AccountManager.KEY_AUTHTOKEN, simulatedAuthToken)
                                    resultBundle = bundle
                                    reportSuccessToAccountManager = true
                                    
                                    // Clear logout flag in preferences - fix to prevent crash
                                    try {
                                        getSharedPreferences("app_preferences", android.content.Context.MODE_PRIVATE)
                                            .edit().putBoolean("user_logged_out", false).apply()
                                    } catch (e: Exception) {
                                        Log.e("LoginActivity", "Error updating preferences: ${e.message}", e)
                                        // Continue with login even if preferences update fails
                                    }
                                    
                                    Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                                    
                                    // Redirect to login screen with the username pre-filled
                                    val loginIntent = intent.clone() as android.content.Intent
                                    loginIntent.putExtra(EXTRA_IS_ADDING_NEW_ACCOUNT, false)
                                    loginIntent.putExtra(EXTRA_ACCOUNT_NAME, username)
                                    loginIntent.putExtra(EXTRA_REGISTRATION_SUCCESSFUL, true)
                                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    
                                    finish() // Finish current activity
                                    startActivity(loginIntent) // Start login activity
                                    
                                } else {
                                    // Failed to add account explicitly
                                    if (isSystemAddingNewAccount) {
                                        // If system was adding, this is a failure for the AccountManager operation.
                                        Toast.makeText(this, "Account already exists or cannot be added by system.", Toast.LENGTH_LONG).show()
                                        finish() // This will call onError in finish()
                                    } else {
                                        // User was trying to register, but account might exist.
                                        Toast.makeText(this, "Account already exists. Try logging in.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("LoginActivity", "Registration error: ${e.message}", e)
                                Toast.makeText(this, "Error during registration: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else { // User wants to login
                            // TODO: Actual login: validate credentials, get token
                            try {
                                val accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)
                                val matchingAccount = accounts.find { it.name == username }
                                
                                if (matchingAccount != null) {
                                    try {
                                        val storedPassword = accountManager.getPassword(matchingAccount)
                                        if (storedPassword == password) {
                                            val bundle = Bundle()
                                            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, username)
                                            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, intent.getStringExtra(EXTRA_ACCOUNT_TYPE))
                                            bundle.putString(AccountManager.KEY_AUTHTOKEN, simulatedAuthToken) // Token from your backend
                                            resultBundle = bundle
                                            reportSuccessToAccountManager = true
                                            
                                            // Clear logout flag in preferences
                                            getSharedPreferences("app_preferences", MODE_PRIVATE)
                                                .edit().putBoolean("user_logged_out", false).apply()
                                            
                                            // Navigate to Dashboard instead of finishing
                                            val dashboardIntent = Intent(this, DashboardActivity::class.java)
                                            dashboardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            startActivity(dashboardIntent)
                                            
                                            // Still need to finish for account manager
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LoginActivity", "Login error: ${e.message}", e)
                                        Toast.makeText(this, "Error during login: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Account not found. Please register first.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("LoginActivity", "Account lookup error: ${e.message}", e)
                                Toast.makeText(this, "Error finding account: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onGuestLogin = {
                        setResult(Activity.RESULT_CANCELED)
                        reportSuccessToAccountManager = false // Not a success for AccountManager
                        finish()
                    }
                )
            }
        }
    }

    override fun finish() {
        accountAuthenticatorResponse?.let {
            if (reportSuccessToAccountManager && resultBundle != null) {
                it.onResult(resultBundle)
            } else {
                // Send error if not reporting success or if resultBundle is somehow null despite success flag
                it.onError(AccountManager.ERROR_CODE_CANCELED, "Authentication cancelled or failed")
            }
            accountAuthenticatorResponse = null // Clear response after handling
        }
        super.finish()
    }
}

@Composable
fun LoginScreen(
    accountName: String?, // From existing account if AccountManager is re-prompting
    isSystemAddingNewAccount: Boolean, // True if system initiated this for adding an account type
    isFromSuccessfulRegistration: Boolean = false, // True if coming from successful registration
    onLoginAttempt: (username_email: String, pass: String, isRegistering: Boolean) -> Unit,
    onGuestLogin: () -> Unit
) {
    var username by remember { mutableStateOf(accountName ?: "") }
    var password by remember { mutableStateOf("") }
    var isRegisteringScreen by remember { mutableStateOf(isSystemAddingNewAccount && !isFromSuccessfulRegistration) } 
    var passwordVisible by remember { mutableStateOf(false) } // Add password visibility state
    val context = LocalContext.current
    
    // Show a welcome message if coming from registration
    LaunchedEffect(isFromSuccessfulRegistration) {
        if (isFromSuccessfulRegistration) {
            // Display a success message if we just registered
            // (already showing toast from activity)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRegisteringScreen) "Register Account" else "Login",
            style = MaterialTheme.typography.headlineMedium,
            color = TextBlack
        )
        
        if (isFromSuccessfulRegistration) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Registration successful! Please login.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username or Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextBlack,
                unfocusedTextColor = TextBlack.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                focusedContainerColor = TextWhite,
                unfocusedContainerColor = TextWhite,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = TextBlack.copy(alpha = 0.5f),
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextBlack,
                unfocusedTextColor = TextBlack.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                focusedContainerColor = TextWhite,
                unfocusedContainerColor = TextWhite,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = TextBlack.copy(alpha = 0.5f),
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.Visibility
                        else 
                            Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onLoginAttempt(username, password, isRegisteringScreen) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(if (isRegisteringScreen) "Register" else "Login")
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (!isSystemAddingNewAccount) { // Don't show toggle if system is forcing add account
            TextButton(
                onClick = { isRegisteringScreen = !isRegisteringScreen },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRegisteringScreen) "Already have an account? Login" else "Don't have an account? Register")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onGuestLogin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Login as Guest")
        }
    }
} 