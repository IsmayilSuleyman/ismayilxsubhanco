package com.subhanismayil.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subhanismayil.budget.ui.BalancesViewModel
import com.subhanismayil.budget.ui.HistoryScreen
import com.subhanismayil.budget.ui.HomeScreen
import com.subhanismayil.budget.ui.theme.BgLight
import com.subhanismayil.budget.ui.theme.SharedBudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SharedBudgetTheme {
                Surface(modifier = Modifier.fillMaxSize().background(BgLight), color = BgLight) {
                    AppNav()
                }
            }
        }
    }
}

private enum class Screen { Home, History }

@Composable
private fun AppNav() {
    var screen by remember { mutableStateOf(Screen.Home) }
    // Share the BalancesViewModel between Home and History so both reflect
    // the same data and a transaction added on Home shows up in History.
    val balancesVm: BalancesViewModel = viewModel()
    when (screen) {
        Screen.Home -> HomeScreen(
            onShowHistory = { screen = Screen.History },
            balancesViewModel = balancesVm
        )
        Screen.History -> HistoryScreen(
            onBack = { screen = Screen.Home },
            viewModel = balancesVm
        )
    }
}
