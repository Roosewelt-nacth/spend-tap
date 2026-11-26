package com.austin.spendtap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.austin.spendtap.ui.SpendTapRoot
import com.austin.spendtap.ui.SpendTapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpendTapTheme { SpendTapRoot() } }
    }
}
