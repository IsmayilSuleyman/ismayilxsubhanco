package com.subhanismayil.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subhanismayil.budget.ui.BalancesViewModel
import com.subhanismayil.budget.ui.HistoryScreen
import com.subhanismayil.budget.ui.HomeScreen
import com.subhanismayil.budget.ui.MonthlyScreen
import com.subhanismayil.budget.ui.theme.AccentPrimary
import com.subhanismayil.budget.ui.theme.AccentSecondary
import com.subhanismayil.budget.ui.theme.BgGradientBottom
import com.subhanismayil.budget.ui.theme.BgGradientTop
import com.subhanismayil.budget.ui.theme.CardBorder
import com.subhanismayil.budget.ui.theme.ShadowTint
import com.subhanismayil.budget.ui.theme.SharedBudgetTheme
import com.subhanismayil.budget.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SharedBudgetTheme {
                AppRoot()
            }
        }
    }
}

private enum class AppTab(val label: String) {
    Home("Home"),
    Months("Months"),
    History("History");

    val icon: ImageVector
        get() = when (this) {
            Home -> Icons.Rounded.Home
            Months -> Icons.Rounded.BarChart
            History -> Icons.AutoMirrored.Rounded.ReceiptLong
        }
}

@Composable
private fun AppRoot() {
    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    // Share the BalancesViewModel between all tabs so they reflect the same
    // data and a transaction added on Home shows up in Months/History.
    val balancesVm: BalancesViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgGradientTop, BgGradientBottom)))
    ) {
        // Soft accent glows behind the content
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-110).dp, y = (-70).dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentPrimary.copy(alpha = 0.10f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(340.dp)
                .offset(x = 130.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentSecondary.copy(alpha = 0.09f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { FloatingNavBar(current = tab, onSelect = { tab = it }) }
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(260)) { dir * it / 10 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(180)) { -dir * it / 14 } + fadeOut(tween(180)))
                },
                label = "tabContent"
            ) { current ->
                when (current) {
                    AppTab.Home -> HomeScreen(
                        onShowHistory = { tab = AppTab.History },
                        onShowMonthly = { tab = AppTab.Months },
                        balancesViewModel = balancesVm
                    )
                    AppTab.Months -> MonthlyScreen(viewModel = balancesVm)
                    AppTab.History -> HistoryScreen(viewModel = balancesVm)
                }
            }
        }
    }
}

@Composable
private fun FloatingNavBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp, top = 4.dp)
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(shape)
            .background(Color(0xFAFFFFFF))
            .border(1.dp, CardBorder, shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppTab.entries.forEach { item ->
            NavItem(
                tab = item,
                selected = item == current,
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavItem(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        animationSpec = tween(200),
        label = "navTint"
    )
    val background = if (selected) {
        Brush.horizontalGradient(listOf(AccentPrimary, AccentSecondary))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(200)) + fadeIn(tween(200)),
            exit = shrinkHorizontally(tween(160)) + fadeOut(tween(120))
        ) {
            Row {
                Spacer(Modifier.width(7.dp))
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
