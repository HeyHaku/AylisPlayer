package com.aylis;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.aylis.Common.Boast;
import com.aylis.Common.Events.WeakDelegate2;
import com.aylis.Common.Events.WeakDelegate3;
import com.aylis.Common.Events.WeakEvent;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.Utils;
import com.aylis.Common.UtilsUI;
import com.aylis.EventsGlobal.EventsGlobalApp;
import com.aylis.EventsGlobal.EventsGlobalTextNotifier;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.Common.ISearchEntry;
import com.aylis.comp.LibraryQueueUI.FragmentLibrary;
import com.aylis.comp.MediaControlsUI.MediaControlsUI;
import com.aylis.comp.SleepTimer.SleepTimerConfig;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.comp.visual.core.CustomFontManager;
import com.aylis.comp.visual.design.VisualizerThemes;
import com.aylis.comp.visual.ui.FragmentVisualizer;
import com.aylis.utils.HapticManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.color.DynamicColors;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import nl.joery.animatedbottombar.AnimatedBottomBar;

public class MainActivity extends AppCompatActivity {

    // ==========================================
    // 1. CONSTANTS
    // ==========================================
    public static final int LIBRARY_PAGE_INDEX = 0;
    public static final int ONLINE_PAGE_INDEX = 1;
    public static final int VISUAL_PAGE_INDEX = 2;
    public static final int SETTINGS_PAGE_INDEX = 3;

    public static final int ACTION_Visualizer = 2;
    public static final int ACTION_Exit = 15;

    private static final int MSG_HIDE = 2;
    private static final int MSG_TICK10 = 3;

    // ==========================================
    // 2. STATIC EVENTS / ARCHITECTURE BRIDGE
    // ==========================================
    public static WeakEvent1<Activity> onCreate = new WeakEvent1<>();
    public static WeakEvent1<Context> onCreateEarly = new WeakEvent1<>();
    public static WeakEvent1<Context> onStart = new WeakEvent1<>();
    public static WeakEvent onStop = new WeakEvent();
    public static WeakEvent1<ContextData> onDestroy = new WeakEvent1<>();
    public static WeakEvent onExit = new WeakEvent();

    public static WeakEvent2<Integer, Activity> onViewPagerPageSelected = new WeakEvent2<>();
    public static WeakEvent1<Context> onViewPagerSwipeOutAtStart = new WeakEvent1<>();
    public static WeakEvent2<Float, Context> onViewPagerSwipeProgressUpdate = new WeakEvent2<>();

    public static WeakEvent2<Integer, ContextData> onMainUIAction = new WeakEvent2<>();
    public static WeakEvent4<AlbumArtRequest, ImageLoadedListener, Integer, Integer> onRequestAlbumArtLarge = new WeakEvent4<>();
    public static WeakEventR<SleepTimerConfig> onMainUIRequestSleepTimerConfig = new WeakEventR<>();
    public static WeakEventR<Boolean> onRequestLockOrientState = new WeakEventR<>();
    public static WeakEventR<PlaylistSong.Data> onRequestTrackInfo = new WeakEventR<>();
    public static WeakEventR<ISearchEntry> onRequestCurrentSearchEntry = new WeakEventR<>();

    public static WeakEvent2<Integer, String> onUISearchQueryTextChange = new WeakEvent2<>();
    public static WeakEvent1<Boolean> onUISearchQueryStateChange = new WeakEvent1<>();
    public static WeakEvent1<Integer> onSetCurrentSearchIndex = new WeakEvent1<>();

    public static WeakDelegate2<View, View> onCreateView = new WeakDelegate2<>();
    public static WeakEvent2<List<PlaylistSong>, Integer> onPreviewOpen = new WeakEvent2<>();
    public static WeakEvent1<Boolean> onFullscreenChanged = new WeakEvent1<>();
    public static WeakEvent1<Boolean> onHideBottomNav = new WeakEvent1<>();
    public static WeakEvent1<Integer> onRequestPermissionsResult = new WeakEvent1<>();

    private static volatile WeakReference<MainActivity> instanceWeak = new WeakReference<>(null);

    // ==========================================
    // 3. FIELDS & VIEWS
    // ==========================================
    public int currentFragmentPage = -1;

    private PlayerCore playerCore;
    private CustomViewPager viewPager;
    private SectionsPagerAdapter sectionsPagerAdapter;

    private View mainContent;
    private View mediaControlsRoot;
    private View bottomNavCard;
    private View layoutMediaControls;
    private View scrimDimOverlay;
    private FrameLayout playerBottomSheet;
    private AnimatedBottomBar bottomNav;

    private MenuItem searchMenuItem;
    private MenuItem sleepTimerIndicatorMenuItem;
    private MenuItem lockOrientIndicatorMenuItem;

    private boolean slowClosingInProgress = false;
    private Toast slowClosingToast;
    private Timer slowClosingTimer;

    private final Handler handler;
    private final List<Object> listenerReferenceHolder = new LinkedList<>();

    // ==========================================
    // 4. CONSTRUCTOR
    // ==========================================
    public MainActivity() {
        EventsGlobalTextNotifier.onTextMsg.subscribeWeak(new WeakEvent1.Handler<String>() {
            @Override
            public void invoke(String textMsg) {
                Boast.makeText(MainActivity.this, textMsg, Toast.LENGTH_SHORT).show();
            }
        }, listenerReferenceHolder);

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_HIDE:
                        if (currentFragmentPage == VISUAL_PAGE_INDEX) {
                            if (AppPreferences.createOrGetInstance()
                                    .getBool(AppPreferences.PREF_Bool_pref_visControlsTimeout)) {
                                showControls(false, currentFragmentPage);
                            }
                        }
                        break;

                    case MSG_TICK10:
                        scheduleTick();
                        EventsGlobalApp.onUITick10.invoke();
                        break;
                }
                return false;
            }
        });
    }

    // ==========================================
    // 5. LIFECYCLE
    // ==========================================
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (com.aylis.utils.HapticManager.INSTANCE.getGlobalVibration()) {
                com.aylis.utils.HapticManager.INSTANCE.performGlobalTick(getWindow().getDecorView());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instanceWeak = new WeakReference<>(this);

        initThemeAndSystemBars();
        initSystemServices();

        onCreateEarly.invoke(getApplicationContext());
        onCreate.invoke(this);

        setContentView(R.layout.main_activity);

        initViews();
        setupWindowInsets();
        setupBottomNav();
        setupMediaControls();
        setupViewPager();
        handleIntent(getIntent());

        scheduleTick();
        VisualizerThemes.s().loadCustomThemes();

        checkAndShowProjectAboutDialog();
        com.aylis.core.CrashReporter.INSTANCE.checkAndShowCrashDialog(this);

        // Проверка обновлений
        new com.aylis.core.updater.UpdateManager(this).checkForUpdatesAsync(true);
    }

    private void checkAndShowProjectAboutDialog() {
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean shown = prefs.getBoolean("ProjectAboutShown", false);
        if (!shown) {
            prefs.edit().putBoolean("ProjectAboutShown", true).apply();
            com.aylis.ui.dialogs.ProjectAboutBottomSheetDialog dialog =
                    com.aylis.ui.dialogs.ProjectAboutBottomSheetDialog.Companion.newInstance();
            dialog.show(getSupportFragmentManager(), com.aylis.ui.dialogs.ProjectAboutBottomSheetDialog.TAG);
        }
    }

    @Override
    protected void onStart() {
        onStart.invoke(getApplicationContext());
        super.onStart();
        scheduleTick();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        onStop.invoke();
        AppPreferences.createOrGetInstance().save(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        onDestroy.invoke(new ContextData(this));
        AppPreferences.createOrGetInstance().save(this);
        setScreenLock(false);
        super.onDestroy();
    }

    // ==========================================
    // 6. INITIALIZATION & SETUP METHODS
    // ==========================================
    private void initThemeAndSystemBars() {
        HapticManager.INSTANCE.init(this);

        String themeStr = AppPreferences.createOrGetInstance().preferencesGetStringSafe(this, "pref_appTheme", "0");
        if (Utils.strToIntSafe(themeStr) == 2) {
            DynamicColors.applyToActivityIfAvailable(this);
        }

        applyMainTheme();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
    }

    private void initSystemServices() {
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        super.onFragmentStarted(fm, f);
                        if (f instanceof DialogFragment) {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HapticManager.INSTANCE.performTick(getWindow().getDecorView());
                                }
                            }, 250);
                        }
                    }
                }, true);

        AppPermissions.requestAllPermissions(this);
        try {
            CustomFontManager.createFolders();
        } catch (Exception ignored) {
        }

        playerCore = PlayerCore.s();
    }

    private void initViews() {
        mainContent = findViewById(R.id.main_content);
        mediaControlsRoot = findViewById(R.id.media_controls_root);
        bottomNavCard = findViewById(R.id.bottomNavCard);
        layoutMediaControls = findViewById(R.id.layoutMediaControls);
        scrimDimOverlay = findViewById(R.id.scrimDimOverlay);
        playerBottomSheet = findViewById(R.id.player_bottom_sheet);
        bottomNav = findViewById(R.id.bottomNavigationView);

        viewPager = findViewById(R.id.viewPager);

        if (mediaControlsRoot != null) {
            boolean isHidden = AppPreferences.createOrGetInstance()
                    .getBool(AppPreferences.PREF_Bool_mediaControlsHidden);
            mediaControlsRoot.setVisibility(isHidden ? View.GONE : View.VISIBLE);
        }

        onCreateView.invoke(findViewById(R.id.layoutMediaControls), findViewById(R.id.layoutMediaControls));
    }

    private void setupWindowInsets() {
        if (mainContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                // Do NOT pad mainContent, so the player can span edge-to-edge
                v.setPadding(systemBars.left, 0, systemBars.right, 0);

                if (viewPager != null) {
                    viewPager.setPadding(0, systemBars.top, 0, 0);
                }

                if (mediaControlsRoot != null) {
                    mediaControlsRoot.setPadding(0, 0, 0, 0);
                }

                if (bottomNavCard != null) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNavCard.getLayoutParams();
                    params.bottomMargin = systemBars.bottom;
                    bottomNavCard.setLayoutParams(params);
                }

                if (playerBottomSheet != null) {
                    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(playerBottomSheet);
                    int peekDp = (int) (146 * getResources().getDisplayMetrics().density);
                    behavior.setPeekHeight(peekDp + systemBars.bottom);
                }

                View layoutExpandedInner = findViewById(R.id.layoutExpandedInner);
                if (layoutExpandedInner != null) {
                    int padding20 = (int) (20 * getResources().getDisplayMetrics().density);
                    layoutExpandedInner.setPadding(padding20, padding20 + systemBars.top, padding20,
                            padding20 + systemBars.bottom);
                }

                return insets;
            });
        }
    }

    private void setupBottomNav() {
        if (bottomNav == null)
            return;

        bottomNav.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {
            @Override
            public void onTabSelected(int lastIndex, @Nullable AnimatedBottomBar.Tab lastTab, int newIndex,
                    @NonNull AnimatedBottomBar.Tab newTab) {
                HapticManager.INSTANCE.performTick(bottomNav);
                if (viewPager != null) {
                    if (lastIndex == -1 || Math.abs(newIndex - lastIndex) <= 1) {
                        viewPager.setCurrentItem(newIndex, true);
                    } else {
                        viewPager.animate()
                                .alpha(0f)
                                .setDuration(120)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewPager.setCurrentItem(newIndex, false);
                                        viewPager.animate().alpha(1f).setDuration(120).start();
                                    }
                                }).start();
                    }
                }
            }

            @Override
            public void onTabReselected(int index, @NonNull AnimatedBottomBar.Tab tab) {
            }
        });

        onHideBottomNav.subscribeWeak(new WeakEvent1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean hide) {
                if (bottomNavCard != null && layoutMediaControls != null) {
                    float targetY = hide ? bottomNavCard.getHeight() + 100f : 0f;
                    float density = layoutMediaControls.getContext().getResources().getDisplayMetrics().density;
                    float targetYMedia = hide ? (200 * density) : 0f;

                    android.view.animation.Interpolator interpolator = hide
                            ? new android.view.animation.AnticipateInterpolator(2.0f)
                            : new android.view.animation.OvershootInterpolator(2.5f);

                    bottomNavCard.animate()
                            .translationY(targetY)
                            .setDuration(450)
                            .setInterpolator(interpolator)
                            .start();

                    layoutMediaControls.animate()
                            .translationY(targetYMedia)
                            .setDuration(450)
                            .setInterpolator(interpolator)
                            .start();
                }
            }
        }, listenerReferenceHolder);
    }

    private void setupMediaControls() {
        if (scrimDimOverlay != null) {
            scrimDimOverlay.setOnClickListener(v -> {
                MediaControlsUI mediaControlsUI = MediaControlsUI.getInstance();
                if (mediaControlsUI != null && mediaControlsUI.isExpanded()) {
                    mediaControlsUI.collapse();
                }
            });
        }

        if (playerBottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(playerBottomSheet);
            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if (scrimDimOverlay != null) {
                        if (slideOffset > 0.01f) {
                            scrimDimOverlay.setVisibility(View.VISIBLE);
                            scrimDimOverlay.setAlpha(slideOffset);
                        } else {
                            scrimDimOverlay.setAlpha(0f);
                            scrimDimOverlay.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (scrimDimOverlay == null)
                        return;
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        scrimDimOverlay.setAlpha(0f);
                        scrimDimOverlay.setVisibility(View.GONE);
                    } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        scrimDimOverlay.setVisibility(View.VISIBLE);
                        scrimDimOverlay.setAlpha(1.0f);
                    }
                }
            });
        }

        MediaControlsUI.onSetAudioViewExpandedState.subscribeWeak(new WeakEvent1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean expanded) {
                if (scrimDimOverlay != null) {
                    scrimDimOverlay.clearAnimation();
                    if (expanded) {
                        scrimDimOverlay.setVisibility(View.VISIBLE);
                        scrimDimOverlay.animate().alpha(1.0f).setDuration(300).setListener(null).start();
                    } else {
                        scrimDimOverlay.animate().alpha(0.0f).setDuration(300)
                                .setListener(new android.animation.AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(android.animation.Animator animation) {
                                        scrimDimOverlay.setVisibility(View.GONE);
                                    }
                                }).start();
                    }
                }

                if (layoutMediaControls != null && bottomNavCard != null) {
                    if (expanded) {
                        layoutMediaControls.animate().translationY(0f).setDuration(300).start();
                    } else {
                        layoutMediaControls.animate().translationY(0f).setDuration(300).start();
                        bottomNavCard.animate().translationY(0f).setDuration(300).start();
                    }
                }
            }
        }, listenerReferenceHolder);

        FragmentVisualizer.onToggleMediaControls.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                if (mediaControlsRoot != null) {
                    mediaControlsRoot.clearAnimation();
                    mediaControlsRoot.animate().cancel();
                    boolean isHidden = AppPreferences.createOrGetInstance()
                            .getBool(AppPreferences.PREF_Bool_mediaControlsHidden);
                    if (isHidden) {
                        ((ViewGroup) mediaControlsRoot).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                        mediaControlsRoot.animate().alpha(0.0f).setDuration(250)
                                .setListener(new android.animation.AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(android.animation.Animator animation) {
                                        if (AppPreferences.createOrGetInstance()
                                                .getBool(AppPreferences.PREF_Bool_mediaControlsHidden)) {
                                            mediaControlsRoot.setVisibility(View.GONE);
                                        }
                                    }
                                }).start();
                    } else {
                        if (mediaControlsRoot.getVisibility() != View.VISIBLE) {
                            mediaControlsRoot.setAlpha(0.0f);
                            mediaControlsRoot.setVisibility(View.VISIBLE);
                        }
                        ((ViewGroup) mediaControlsRoot).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                        mediaControlsRoot.animate().alpha(1.0f).setDuration(250).setListener(null).start();
                    }
                }
            }
        }, listenerReferenceHolder);
    }

    private void setupViewPager() {
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setPageTransformer(false, (page, position) -> {
            float absPos = Math.abs(position);

            if (position <= -1.0f || position >= 1.0f) {
                page.setAlpha(0.0f);
                page.setScaleX(0.92f);
                page.setScaleY(0.92f);
                page.setTranslationX(0f);
                page.setTranslationY(0f);
            } else if (position == 0.0f) {
                page.setAlpha(1.0f);
                page.setScaleX(1.0f);
                page.setScaleY(1.0f);
                page.setTranslationX(0f);
                page.setTranslationY(0f);
            } else {
                page.setTranslationX(page.getWidth() * -position);
                page.setTranslationY(0f);

                float scale = 0.92f + (0.08f * (1.0f - absPos));
                page.setScaleX(scale);
                page.setScaleY(scale);

                float alpha = 1.0f - (absPos * 1.5f);
                page.setAlpha(Math.max(0.0f, alpha));
            }
        });

        viewPager.setOnSwipeOutListener(new CustomViewPager.OnSwipeOutListener() {
            @Override
            public void onSwipeOutAtStart() {
                onViewPagerSwipeOutAtStart.invoke(getApplicationContext());
            }

            @Override
            public void onSwipeOutAtEnd() {
            }

            @Override
            public void onSwipeProgressUpdate(float val) {
                onViewPagerSwipeProgressUpdate.invoke(val, getApplicationContext());
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                MainActivity.this.onViewPagerPageSelected(position);
                if (bottomNav != null) {
                    bottomNav.selectTabAt(position, true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void handleIntent(Intent intent) {
        PlaylistSong songOpened = null;
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                songOpened = new PlaylistSong(-1, uri);
            }
        }

        if (songOpened != null) {
            viewPager.setCurrentItem(VISUAL_PAGE_INDEX);
            onViewPagerPageSelected(VISUAL_PAGE_INDEX);

            List<PlaylistSong> songList = new ArrayList<>();
            songList.add(songOpened);
            onPreviewOpen.invoke(songList, 0);
        } else {
            viewPager.setCurrentItem(LIBRARY_PAGE_INDEX);
            onViewPagerPageSelected(LIBRARY_PAGE_INDEX);
        }
    }

    // ==========================================
    // 7. USER INTERACTION & INPUT
    // ==========================================
    @Override
    public void onBackPressed() {
        MediaControlsUI mediaControlsUI = MediaControlsUI.getInstance();
        if (mediaControlsUI != null && mediaControlsUI.isExpanded()) {
            mediaControlsUI.collapse();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            MediaControlsUI mediaControlsUI = MediaControlsUI.getInstance();
            if (mediaControlsUI != null && mediaControlsUI.isExpanded()) {
                mediaControlsUI.collapse();
                return true;
            }

            if (closeSearchView())
                return true;

            if (currentFragmentPage == LIBRARY_PAGE_INDEX) {
                FragmentLibrary fragmentLibrary = getFragmentLibraryInstance();
                if (fragmentLibrary != null && fragmentLibrary.navigateForBackwardLibraryAddress()) {
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && slowClosingInProgress) {
            slowClosingInProgress = false;
            if (slowClosingTimer != null) {
                slowClosingTimer.cancel();
                slowClosingTimer = null;
            }
            if (slowClosingToast != null) {
                slowClosingToast.setText(getString(R.string.hold_exit_canceled));
                slowClosingToast.setDuration(Toast.LENGTH_SHORT);
                slowClosingToast.show();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        String timeStr = AppPreferences.preferencesGetStringSafe(
                AppPreferences.createOrGetInstance().getPreferences(getApplicationContext()), "pref_holdexit", "0");
        int slowClosingTime = Utils.strToIntSafe(timeStr);

        if (keyCode == KeyEvent.KEYCODE_BACK && slowClosingTime > 0) {
            slowClosingInProgress = true;
            slowClosingToast = Toast.makeText(getApplicationContext(), getString(R.string.hold_exit),
                    Toast.LENGTH_SHORT);
            slowClosingToast.show();

            slowClosingTimer = new Timer();
            slowClosingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    slowClosingInProgress = false;
                    if (slowClosingToast != null)
                        slowClosingToast.cancel();
                    if (slowClosingTimer != null)
                        slowClosingTimer.cancel();
                    doExit();
                }
            }, slowClosingTime);

            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onUserInteraction() {
        resetVideoMaximizeTimeout(true);
        super.onUserInteraction();
    }

    // ==========================================
    // 8. MENU & SEARCH
    // ==========================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        sleepTimerIndicatorMenuItem = menu.findItem(R.id.action_bar_sleep_timer_indicator);
        SleepTimerConfig timerConfig = onMainUIRequestSleepTimerConfig.invoke(null);
        updateSleepTimerIndicator(timerConfig != null && timerConfig.enabled, true);

        lockOrientIndicatorMenuItem = menu.findItem(R.id.action_bar_lock_orient_indicator);
        boolean lockOrientState = onRequestLockOrientState.invoke(false);
        updateLockOrientIndicator(lockOrientState, true);

        searchMenuItem = menu.findItem(R.id.action_bar_search);
        if (searchMenuItem != null) {
            SearchView searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setIconifiedByDefault(true);
            searchView.setSubmitButtonEnabled(false);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    if (searchView.getTag() != null) {
                        onUISearchQueryTextChange.invoke((int) searchView.getTag(), query);
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (searchView.getTag() != null) {
                        onUISearchQueryTextChange.invoke((int) searchView.getTag(), newText);
                    }
                    return true;
                }
            });

            searchView.setOnCloseListener(() -> {
                onUISearchQueryStateChange.invoke(false);
                return false;
            });

            setSearchViewStyle(searchView);
            updateSearchView(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_bar_sleep_timer_indicator) {
            onMainUIAction.invoke(1, new ContextData(this));
            return true;
        } else if (id == R.id.action_bar_lock_orient_indicator) {
            onMainUIAction.invoke(2, new ContextData(this));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateSearchView(boolean eventFromOnCreateOptionsMenu) {
        updateSearchView(onRequestCurrentSearchEntry.invoke(null), eventFromOnCreateOptionsMenu);
    }

    public void updateSearchView(ISearchEntry currentSearchEntry, boolean eventFromOnCreateOptionsMenu) {
        if (searchMenuItem == null)
            return;

        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        if (currentSearchEntry != null && currentSearchEntry.isEnabled()) {
            String currentQuery = currentSearchEntry.getQuery();
            searchMenuItem.setVisible(true);

            if (currentQuery != null && !currentQuery.isEmpty()) {
                searchView.setTag(currentSearchEntry.getIndex());
                searchView.setQuery(currentQuery, false);
                if (searchView.isIconified())
                    searchView.setIconified(false);
            } else {
                searchView.setTag(currentSearchEntry.getIndex());
                searchView.setQuery("", false);
            }
            searchView.setQueryHint(currentSearchEntry.getHint());
        } else {
            searchMenuItem.setVisible(false);
            searchView.setQueryHint("");
        }
    }

    private void setSearchViewStyle(SearchView searchView) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        int textColorSecondary = typedValue.data != 0 ? typedValue.data : android.graphics.Color.GRAY;
        UtilsUI.setViewStyle(searchView, ContextCompat.getColor(this, R.color.white_alpha_1), textColorSecondary);
    }

    private boolean closeSearchView() {
        if (searchMenuItem == null)
            return false;
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        if (!searchView.isIconified()) {
            searchView.setTag(currentFragmentPage);
            searchView.setQuery("", false);
            searchView.setIconified(true);
            return true;
        }
        return false;
    }

    public void updateSleepTimerIndicator(boolean state, boolean evenFromOnCreateOptionsMenu) {
        if (!evenFromOnCreateOptionsMenu) {
            invalidateOptionsMenu();
            return;
        }
        if (sleepTimerIndicatorMenuItem != null) {
            sleepTimerIndicatorMenuItem.setVisible(state);
        }
    }

    public void updateLockOrientIndicator(boolean state, boolean evenFromOnCreateOptionsMenu) {
        if (!evenFromOnCreateOptionsMenu) {
            invalidateOptionsMenu();
            return;
        }
        if (lockOrientIndicatorMenuItem != null) {
            lockOrientIndicatorMenuItem.setVisible(state);
        }
    }

    // ==========================================
    // 9. UI CONTROL & HELPERS
    // ==========================================
    public void onViewPagerPageSelected(int position) {
        currentFragmentPage = position;
        showControls(true, currentFragmentPage);
        onViewPagerPageSelected.invoke(currentFragmentPage, this);
        onSetCurrentSearchIndex.invoke(currentFragmentPage);
    }

    public void toggleShowControls(int pagePosition) {
        ActionBar actionBar = getSupportActionBar();
        showControls(actionBar != null && !actionBar.isShowing(), pagePosition);
    }

    public void showControls(boolean show, int pagePosition) {
        showControls(show, pagePosition, false);
    }

    private void showControls(boolean show, int pagePosition, boolean eventFromSystemUiHider) {
        show = true;
        resetVideoMaximizeTimeout(show);

        if (mediaControlsRoot != null) {
            boolean isHidden = AppPreferences.createOrGetInstance()
                    .getBool(AppPreferences.PREF_Bool_mediaControlsHidden);
            if (!isHidden) {
                mediaControlsRoot.clearAnimation();
                mediaControlsRoot.animate().cancel();
                ((ViewGroup) mediaControlsRoot).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                mediaControlsRoot.setVisibility(View.VISIBLE);
                mediaControlsRoot.setAlpha(1.0f);
            }
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        if (controller != null) {
            controller.show(WindowInsetsCompat.Type.systemBars());
        }

        onFullscreenChanged.invoke(!show);
    }

    public void resetVideoMaximizeTimeout(boolean resetTimer) {
        if (AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_pref_visControlsTimeout)) {
            handler.removeMessages(MSG_HIDE);
            if (resetTimer) {
                int sec = getResources().getInteger(R.integer.video_maximize_timeout);
                handler.sendMessageDelayed(handler.obtainMessage(MSG_HIDE), sec);
            }
        }
    }

    private void scheduleTick() {
        handler.removeMessages(MSG_TICK10);
        handler.sendMessageDelayed(handler.obtainMessage(MSG_TICK10), 10000);
    }

    private void applyMainTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            View decor = window.getDecorView();
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int flags = decor.getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                decor.setSystemUiVisibility(flags);
            }
        }
    }

    public void setScreenLock(boolean b) {
        if (b) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void switchToVisualizer() {
        if (viewPager != null)
            viewPager.setCurrentItem(VISUAL_PAGE_INDEX);
    }

    public void openVisualizer() {
        switchToVisualizer();
    }

    public void openDrawer() {
    }

    public void doExit() {
        onExit.invoke();
        finish();
    }

    public void doExitFromService() {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppPermissions.REQUEST_STORAGE) {
            boolean granted = false;
            for (int res : grantResults) {
                if (res == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                FragmentLibrary frag = getFragmentLibraryInstance();
                if (frag != null)
                    frag.updateLibraryItems();
            }
        }
        onRequestPermissionsResult.invoke(requestCode);
    }

    // ==========================================
    // 10. STATIC GETTERS
    // ==========================================
    public static MainActivity getInstance() {
        return instanceWeak.get();
    }

    public static FragmentLibrary getFragmentLibraryInstance() {
        MainActivity mainActivity = instanceWeak.get();
        return mainActivity != null ? (FragmentLibrary) mainActivity.findFragmentByPosition(LIBRARY_PAGE_INDEX) : null;
    }

    public static FragmentVisualizer getFragmentVisualizerInstance() {
        MainActivity mainActivity = instanceWeak.get();
        return mainActivity != null ? (FragmentVisualizer) mainActivity.findFragmentByPosition(VISUAL_PAGE_INDEX)
                : null;
    }

    private Fragment findFragmentByPosition(int position) {
        if (viewPager == null || sectionsPagerAdapter == null)
            return null;
        return getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + viewPager.getId() + ":" + sectionsPagerAdapter.getItemId(position));
    }

    // ==========================================
    // 11. ADAPTER
    // ==========================================
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case LIBRARY_PAGE_INDEX:
                    return FragmentLibrary.newInstance();
                case ONLINE_PAGE_INDEX:
                    return com.aylis.comp.online.ui.FragmentOnline.Companion.newInstance();
                case VISUAL_PAGE_INDEX:
                    return FragmentVisualizer.newInstance();
                case SETTINGS_PAGE_INDEX:
                    return com.aylis.ui.settings.FragmentSettings.Companion.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Drawable myDrawable;
            switch (position) {
                case LIBRARY_PAGE_INDEX:
                    myDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_library_2_s);
                    break;
                case ONLINE_PAGE_INDEX:
                    myDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_search_2_s);
                    break;
                case VISUAL_PAGE_INDEX:
                    myDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_visual2);
                    break;
                case SETTINGS_PAGE_INDEX:
                    myDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_settings);
                    break;
                default:
                    return " ";
            }

            if (myDrawable == null)
                return "";

            SpannableStringBuilder sb = new SpannableStringBuilder("   ");
            myDrawable.setBounds(0, 0, myDrawable.getIntrinsicWidth(), myDrawable.getIntrinsicHeight());
            sb.setSpan(new ImageSpan(myDrawable, ImageSpan.ALIGN_BASELINE), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sb;
        }
    }
}