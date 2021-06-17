package com.byagowi.persiancalendar.ui

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.byagowi.persiancalendar.*
import com.byagowi.persiancalendar.ReleaseDebugDifference.debugAssertNotNull
import com.byagowi.persiancalendar.databinding.ActivityMainBinding
import com.byagowi.persiancalendar.databinding.NavigationHeaderBinding
import com.byagowi.persiancalendar.service.ApplicationService
import com.byagowi.persiancalendar.ui.calendar.CalendarFragmentDirections
import com.byagowi.persiancalendar.ui.preferences.INTERFACE_CALENDAR_TAB
import com.byagowi.persiancalendar.utils.*
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

/**
 * Program activity for android
 */
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener,
    NavigationView.OnNavigationItemSelectedListener, NavController.OnDestinationChangedListener,
    DrawerHost {

    private var creationDateJdn: Jdn? = null
    private var settingHasChanged = false
    private lateinit var binding: ActivityMainBinding

    private val onBackPressedCloseDrawerCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = binding.drawer.closeDrawer(GravityCompat.START)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeFromName(getThemeFromPreference(this, appPrefs)))

        applyAppLanguage(this)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCloseDrawerCallback)
        ReleaseDebugDifference.startLynxListenerIfIsDebug(this)
        initUtils(this)

        // Don't apply font override to English and Japanese locales
        when (language) {
            LANG_EN_US, LANG_JA, LANG_FR -> Unit
            else -> overrideFont("SANS_SERIF", getAppFont(applicationContext))
        }

        startEitherServiceOrWorker(this)

        setDeviceCalendarEvents(applicationContext)
        update(applicationContext, false)

        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) window?.also { window ->
            // https://learnpainless.com/android/material/make-fully-android-transparent-status-bar
            window.attributes = window.attributes.also {
                it.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS.inv()
            }
            window.statusBarColor = Color.TRANSPARENT
        }

        binding.drawer.addDrawerListener(createDrawerListener())

        navHostFragment?.navController?.addOnDestinationChangedListener(this)
        when (intent?.action) {
            "COMPASS" -> R.id.compass
            "LEVEL" -> R.id.level
            "CONVERTER" -> R.id.converter
            "SETTINGS" -> R.id.settings
            "DEVICE" -> R.id.deviceInformation
            else -> null // unsupported action. ignore
        }?.also {
            navigateTo(it)
            // So it won't happen again if the activity is restarted
            intent?.action = ""
        }

        appPrefs.registerOnSharedPreferenceChangeListener(this)

        if (isShowDeviceCalendarEvents && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) askForCalendarPermission(this)

        binding.navigation.setNavigationItemSelectedListener(this)

        NavigationHeaderBinding.bind(binding.navigation.getHeaderView(0))
            .seasonImage.setImageResource(run {
                var season = (Jdn.today.toPersianCalendar().month - 1) / 3

                // Southern hemisphere
                if ((getCoordinate(this)?.latitude ?: 1.0) < .0) season = (season + 2) % 4

                when (season) {
                    0 -> R.drawable.spring
                    1 -> R.drawable.summer
                    2 -> R.drawable.fall
                    else -> R.drawable.winter
                }
            })

        if (!appPrefs.getBoolean(CHANGE_LANGUAGE_IS_PROMOTED_ONCE, false)) {
            showChangeLanguageSnackbar()
            appPrefs.edit { putBoolean(CHANGE_LANGUAGE_IS_PROMOTED_ONCE, true) }
        }

        creationDateJdn = Jdn.today

        if (mainCalendar == CalendarType.SHAMSI && isIranHolidaysEnabled &&
            Jdn.today.toPersianCalendar().year > supportedYearOfIranCalendar
        ) showAppIsOutDatedSnackbar()

        applyAppLanguage(this)

        previousAppThemeValue = appPrefs.getString(PREF_THEME, null)
    }

    private var previousAppThemeValue: String? = null

    private val navHostFragment by lazy {
        (supportFragmentManager.findFragmentById(R.id.navHostFragment) as? NavHostFragment)
            .debugAssertNotNull
    }

    override fun onDestinationChanged(
        controller: NavController, destination: NavDestination, arguments: Bundle?
    ) {
        binding.navigation.menu.findItem(
            when (destination.id) {
                // We don't have a menu entry for compass, so
                R.id.level -> R.id.compass
                else -> destination.id
            }
        )?.also {
            it.isCheckable = true
            it.isChecked = true
        }

        if (settingHasChanged) { // update when checked menu item is changed
            initUtils(this)
            update(applicationContext, true)
            settingHasChanged = false // reset for the next time
        }
    }

    private fun navigateTo(@IdRes id: Int) {
        navHostFragment?.navController?.navigate(
            id, null, navOptions {
                anim {
                    enter = R.anim.nav_enter_anim
                    exit = R.anim.nav_exit_anim
                    popEnter = R.anim.nav_enter_anim
                    popExit = R.anim.nav_exit_anim
                }
            }
        )
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        settingHasChanged = true

        when (key) {
            PREF_SHOW_DEVICE_CALENDAR_EVENTS -> {
                if (sharedPreferences?.getBoolean(PREF_SHOW_DEVICE_CALENDAR_EVENTS, true) == true
                    && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.READ_CALENDAR
                    ) != PackageManager.PERMISSION_GRANTED
                ) askForCalendarPermission(this)
            }
            PREF_APP_LANGUAGE -> restartToSettings()
            PREF_THEME -> {
                // Restart activity if theme is changed and don't if app theme
                // has just got a default value by preferences as going
                // from null => SystemDefault which makes no difference
                if (previousAppThemeValue != null ||
                    sharedPreferences?.getString(PREF_THEME, null) != SYSTEM_DEFAULT_THEME
                ) restartToSettings()
            }
            PREF_NOTIFY_DATE -> {
                if (sharedPreferences?.getBoolean(PREF_NOTIFY_DATE, true) == false) {
                    stopService(Intent(this, ApplicationService::class.java))
                    startEitherServiceOrWorker(applicationContext)
                }
            }
        }

        updateStoredPreference(this)
        update(applicationContext, true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CALENDAR_READ_PERMISSION_REQUEST_CODE -> when (PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.READ_CALENDAR
                ) -> {
                    toggleShowDeviceCalendarOnPreference(this, true)
                    val navController = navHostFragment?.navController
                    if (navController?.currentDestination?.id == R.id.calendar)
                        navController.navigateSafe(CalendarFragmentDirections.navigateToSelf())
                }
                else -> toggleShowDeviceCalendarOnPreference(this, false)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        initUtils(this)
        binding.drawer.layoutDirection = when {
            isRTL(this) -> View.LAYOUT_DIRECTION_RTL
            else -> View.LAYOUT_DIRECTION_LTR
        }
    }

    override fun onResume() {
        super.onResume()
        applyAppLanguage(this)
        update(applicationContext, false)
        if (creationDateJdn != Jdn.today) {
            creationDateJdn = Jdn.today
            val navController = navHostFragment?.navController
            if (navController?.currentDestination?.id == R.id.calendar) {
                navController.navigateSafe(CalendarFragmentDirections.navigateToSelf())
            }
        }
    }

    // Checking for the ancient "menu" key
    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = when (keyCode) {
        KeyEvent.KEYCODE_MENU -> {
            if (binding.drawer.isDrawerOpen(GravityCompat.START))
                binding.drawer.closeDrawer(GravityCompat.START)
            else
                binding.drawer.openDrawer(GravityCompat.START)
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun restartToSettings() {
        val intent = intent
        intent?.action = "SETTINGS"
        finish()
        startActivity(intent)
    }

    private var clickedItem = 0

    override fun onNavigationItemSelected(selectedMenuItem: MenuItem): Boolean {
        when (val itemId = selectedMenuItem.itemId) {
            R.id.exit -> finish()
            else -> {
                binding.drawer.closeDrawer(GravityCompat.START)
                if (navHostFragment?.navController?.currentDestination?.id != itemId) {
                    clickedItem = itemId
                }
            }
        }
        return true
    }

    private fun showChangeLanguageSnackbar() = Snackbar.make(
        binding.root, "✖  Change app language?", 7000
    ).also {
        it.view.layoutDirection = View.LAYOUT_DIRECTION_LTR
        it.view.setOnClickListener { _ -> it.dismiss() }
        it.setAction("Settings") {
            navHostFragment?.navController?.navigateSafe(
                CalendarFragmentDirections.navigateToSettings(
                    INTERFACE_CALENDAR_TAB, PREF_APP_LANGUAGE
                )
            )
        }
        it.setActionTextColor(ContextCompat.getColor(it.context, R.color.dark_accent))
    }.show()

    private fun showAppIsOutDatedSnackbar() = Snackbar.make(
        binding.root, getString(R.string.outdated_app), 10000
    ).also {
        it.setAction(getString(R.string.update)) { bringMarketPage(this) }
        it.setActionTextColor(ContextCompat.getColor(it.context, R.color.dark_accent))
    }.show()

    override fun setupToolbarWithDrawer(viewLifecycleOwner: LifecycleOwner, toolbar: Toolbar) {
        val listener = ActionBarDrawerToggle(
            this, binding.drawer, toolbar,
            androidx.navigation.ui.R.string.nav_app_bar_open_drawer_description, R.string.close
        ).also { it.syncState() }

        binding.drawer.addDrawerListener(listener)
        toolbar.setNavigationOnClickListener { binding.drawer.openDrawer(GravityCompat.START) }
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                binding.drawer.removeDrawerListener(listener)
                toolbar.setNavigationOnClickListener(null)
            }
        })
    }

    private fun createDrawerListener() = object : DrawerLayout.SimpleDrawerListener() {
        val slidingDirection = if (isRTL(this@MainActivity)) -1 else +1

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            super.onDrawerSlide(drawerView, slideOffset)
            slidingAnimation(drawerView, slideOffset / 1.5f)
        }

        private fun slidingAnimation(drawerView: View, slideOffset: Float) {
            binding.navHostFragment.translationX =
                slideOffset * drawerView.width.toFloat() * slidingDirection.toFloat()
            binding.drawer.bringChildToFront(drawerView)
            binding.drawer.requestLayout()
        }

        override fun onDrawerOpened(drawerView: View) {
            super.onDrawerOpened(drawerView)
            onBackPressedCloseDrawerCallback.isEnabled = true
        }

        override fun onDrawerClosed(drawerView: View) {
            super.onDrawerClosed(drawerView)
            onBackPressedCloseDrawerCallback.isEnabled = false
            if (clickedItem != 0) {
                navigateTo(clickedItem)
                clickedItem = 0
            }
        }
    }
}
