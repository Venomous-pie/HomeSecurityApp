package com.example.smarthomesecuritycontrolhub.ui.landing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smarthomesecuritycontrolhub.ui.login.LoginActivity
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme
import com.example.smarthomesecuritycontrolhub.ui.theme.TextBlack

class LandingActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SmartHomeSecurityControlHubTheme {
                LandingScreen(
                    onLoginClicked = {
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra(LoginActivity.EXTRA_IS_ADDING_NEW_ACCOUNT, false)
                        startActivity(intent)
                    },
                    onRegisterClicked = {
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra(LoginActivity.EXTRA_IS_ADDING_NEW_ACCOUNT, true)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun LandingScreen(
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Smart Home Security Control Hub",
            style = MaterialTheme.typography.headlineMedium,
            color = TextBlack
        )
        
        Spacer(modifier = Modifier.height(50.dp))
        
        Button(
            onClick = onLoginClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Login")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onRegisterClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Register")
        }
    }
} 