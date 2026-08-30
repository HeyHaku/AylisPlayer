package com.aylis.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aylis.MainActivity
import com.aylis.R

class OnboardingActivity : AppCompatActivity() {

    private val PREFS_NAME = "AppPrefs"
    private val PREF_ONBOARDING_COMPLETE = "OnboardingComplete"

    private lateinit var layoutSplash: RelativeLayout
    private lateinit var layoutPermissions: RelativeLayout
    private lateinit var btnWelcomeStart: Button
    private lateinit var btnGrantPermissions: Button

    private lateinit var itemStorage: LinearLayout
    private lateinit var itemMic: LinearLayout
    private lateinit var itemNotif: LinearLayout

    private lateinit var checkStorage: ImageView
    private lateinit var checkMic: ImageView
    private lateinit var checkNotif: ImageView

    // Launchers
    private val storageLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.entries.any { it.value }
        if (granted) {
            TransitionManager.beginDelayedTransition(findViewById(android.R.id.content) as ViewGroup)
            checkStorage.visibility = View.VISIBLE
            itemStorage.isClickable = false
        }
        checkContinueButtonState()
    }

    private val micLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            TransitionManager.beginDelayedTransition(findViewById(android.R.id.content) as ViewGroup)
            checkMic.visibility = View.VISIBLE
            itemMic.isClickable = false
        }
    }

    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            TransitionManager.beginDelayedTransition(findViewById(android.R.id.content) as ViewGroup)
            checkNotif.visibility = View.VISIBLE
            itemNotif.isClickable = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make Status Bar and Nav Bar transparent (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_ONBOARDING_COMPLETE, false)) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_onboarding)

        layoutSplash = findViewById(R.id.layout_splash)
        layoutPermissions = findViewById(R.id.layout_permissions)
        
        // Handle insets so content doesn't hide behind transparent bars
        ViewCompat.setOnApplyWindowInsetsListener(layoutSplash) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(layoutPermissions) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Base padding was 24dp (which is roughly 60px depending on density, but we'll add roughly 48px to safe sides)
            // To be precise we convert 24dp to px:
            val p = (24 * resources.displayMetrics.density).toInt()
            view.setPadding(p + sysBars.left, p + sysBars.top, p + sysBars.right, p + sysBars.bottom)
            insets
        }

        btnWelcomeStart = findViewById(R.id.btn_welcome_start)
        btnGrantPermissions = findViewById(R.id.btn_grant_permissions)

        itemStorage = findViewById(R.id.item_storage)
        itemMic = findViewById(R.id.item_mic)
        itemNotif = findViewById(R.id.item_notif)

        checkStorage = findViewById(R.id.check_storage)
        checkMic = findViewById(R.id.check_mic)
        checkNotif = findViewById(R.id.check_notif)

        // Hide notification item on older Androids
        if (Build.VERSION.SDK_INT < 33) {
            itemNotif.visibility = View.GONE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            TransitionManager.beginDelayedTransition(findViewById(android.R.id.content) as ViewGroup)
            btnWelcomeStart.text = getString(R.string.onboarding_start)
            btnWelcomeStart.isEnabled = true
            btnWelcomeStart.setOnClickListener {
                TransitionManager.beginDelayedTransition(findViewById(android.R.id.content) as ViewGroup)
                layoutSplash.visibility = View.GONE
                layoutPermissions.visibility = View.VISIBLE
                updateInitialCheckmarks()
            }
        }, 2000)

        itemStorage.setOnClickListener {
            val perms = if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            storageLauncher.launch(perms)
        }

        itemMic.setOnClickListener {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        itemNotif.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        btnGrantPermissions.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun updateInitialCheckmarks() {
        val hasStorage = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        
        if (hasStorage) {
            checkStorage.visibility = View.VISIBLE
            itemStorage.isClickable = false
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            checkMic.visibility = View.VISIBLE
            itemMic.isClickable = false
        }

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            checkNotif.visibility = View.VISIBLE
            itemNotif.isClickable = false
        }

        checkContinueButtonState()
    }

    private fun checkContinueButtonState() {
        val hasStorage = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (hasStorage) {
            btnGrantPermissions.isEnabled = true
            btnGrantPermissions.setTextColor(Color.parseColor("#005A9C")) // Active blue color
        } else {
            btnGrantPermissions.isEnabled = false
            btnGrantPermissions.setTextColor(Color.parseColor("#999999")) // Disabled gray color
        }
    }

    private fun completeOnboarding() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETE, true).apply()
        navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
