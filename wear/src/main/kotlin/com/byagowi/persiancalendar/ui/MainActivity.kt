package com.byagowi.persiancalendar.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.bundleOf
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.byagowi.persiancalendar.requestComplicationUpdate
import com.byagowi.persiancalendar.requestTileUpdate
import io.github.persiancalendar.calendar.CivilDate
import java.util.GregorianCalendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        run {
            // Request update of both on activity just in case
            requestComplicationUpdate()
            requestTileUpdate()
        }
        setContent { WearApp() }
    }
}

@Composable
private fun WearApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = dynamicColorScheme(LocalContext.current) ?: MaterialTheme.colorScheme
        ) {
            AppScaffold(
//                timeText = {
//                    val persianDigitsFormatter = run {
//                        val symbols = DecimalFormatSymbols.getInstance(ULocale("fa_IR"))
//                        DecimalFormat("#", symbols)
//                    }
//                    TimeText { time ->
//                        timeTextCurvedText(
//                            time.map {
//                                it.digitToIntOrNull()?.let(persianDigitsFormatter::format) ?: it
//                            }.joinToString(""),
//                        )
//                    }
//                }
            ) {
                val navController = rememberSwipeDismissableNavController()
                val mainRoute = "app"
                val settingsRoute = "settings"
                val utilitiesRoute = "utilities"
                val converterRoute = "converter"
                val calendarRoute = "calendar"
                val dayRoute = "day"
                val dayJdnKey = "dayJdnKey"
                // This shouldn't be needed by just in case
                val todayJdn = run {
                    val calendar = GregorianCalendar.getInstance()
                    CivilDate(
                        calendar[GregorianCalendar.YEAR],
                        calendar[GregorianCalendar.MONTH] + 1,
                        calendar[GregorianCalendar.DAY_OF_MONTH],
                    ).toJdn()
                }
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = mainRoute,
                ) {
                    composable(mainRoute) {
                        MainScreen(
                            navigateToUtilities = { navController.navigate(utilitiesRoute) },
                            navigateToDay = { jdn ->
                                navController.graph.findNode(dayRoute)?.let { destination ->
                                    navController.navigate(
                                        destination.id, bundleOf(dayJdnKey to jdn)
                                    )
                                }
                            }
                        )
                    }
                    composable(utilitiesRoute) {
                        UtilitiesScreen(
                            navigateToConverter = { navController.navigate(converterRoute) },
                            navigateToCalendar = { navController.navigate(calendarRoute) },
                            navigateToSettings = { navController.navigate(settingsRoute) },
                        )
                    }
                    composable(converterRoute) { ConverterScreen() }
                    composable(calendarRoute) {
                        CalendarScreen { jdn ->
                            navController.graph.findNode(dayRoute)?.let { destination ->
                                navController.navigate(
                                    destination.id, bundleOf(dayJdnKey to jdn)
                                )
                            }
                        }
                    }
                    composable(dayRoute) { backStackEntry ->
                        DayScreen(
                            backStackEntry.arguments?.getLong(dayJdnKey, todayJdn) ?: todayJdn
                        )
                    }
                    composable(settingsRoute) { SettingsScreen() }
                }
            }
        }
    }
}
