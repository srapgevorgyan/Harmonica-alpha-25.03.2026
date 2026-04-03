package com.harmonica.application;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private android.view.View customToolbar;
    private TextView toolbarTitle;
    private ImageButton btnMenu;
    private boolean isIncognitoGlobal = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // UI Components
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btnMenu);
        customToolbar = findViewById(R.id.custom_toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);

        // Menu Button (Open the side drawer)
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Navigation Clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;

            // Reset incognito mode if we switch away from it
            if (id != R.id.nav_incognito && item.getGroupId() != 2) {
                setIncognitoMode(false);
            }

            if (id == R.id.nav_manage) {
                showMultiDeleteDialog();
            }
            else if (item.getGroupId() == 2) {
                showChatOptionsDialog(item.getItemId(), item.getTitle().toString());
            }
            else {
                if (id == R.id.nav_chat) {
                    selectedFragment = new ChatFragment();
                } else if (id == R.id.nav_incognito) {
                    selectedFragment = new ChatFragment();
                    Bundle b = new Bundle();
                    b.putLong("sessionId", -2); // Incognito session
                    selectedFragment.setArguments(b);
                } else if (id == R.id.nav_stats) {
                    selectedFragment = new StatsFragment();
                } else if (id == R.id.nav_edu) {
                    selectedFragment = new EducationFragment();
                }
                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        if (savedInstanceState == null) {
            loadFragment(new ChatFragment());
            navigationView.setCheckedItem(R.id.nav_chat);
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        updateMenuWithSessions();
    }

    public void setIncognitoMode(boolean isIncognito) {
        this.isIncognitoGlobal = isIncognito;
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());

        if (isIncognito) {
            customToolbar.setBackgroundColor(Color.parseColor("#121212"));
            toolbarTitle.setTextColor(Color.parseColor("#90CAF9"));
            toolbarTitle.setText("Incognito Space");
            btnMenu.setColorFilter(Color.parseColor("#90CAF9"));
            
            window.setStatusBarColor(Color.parseColor("#121212"));
            insetsController.setAppearanceLightStatusBars(false); // Light icons on dark background
            
            drawerLayout.setBackgroundColor(Color.parseColor("#121212"));
        } else {
            int bgColor = ContextCompat.getColor(this, R.color.harmonica_bg);
            customToolbar.setBackgroundColor(bgColor);
            toolbarTitle.setTextColor(ContextCompat.getColor(this, R.color.harmonica_primary));
            toolbarTitle.setText("Harmonica");
            btnMenu.setColorFilter(ContextCompat.getColor(this, R.color.harmonica_primary));
            
            window.setStatusBarColor(bgColor);
            insetsController.setAppearanceLightStatusBars(true); // Dark icons on light background
            
            drawerLayout.setBackgroundColor(bgColor);
        }
    }

    private void showMultiDeleteDialog() {
        MoodDatabase db = new MoodDatabase(this);
        List<MoodDatabase.SessionHeader> sessions = db.getCategorizedSessions();
        if (sessions.isEmpty()) {
            Toast.makeText(this, "No saved chats to clean up.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] titles = new String[sessions.size()];
        boolean[] checked = new boolean[sessions.size()];
        for (int i = 0; i < sessions.size(); i++) {
            titles[i] = sessions.get(i).title;
        }

        List<Integer> selectedItems = new ArrayList<>();
        new AlertDialog.Builder(this)
                .setTitle("Clean Up Conversations")
                .setMultiChoiceItems(titles, checked, (dialog, which, isChecked) -> {
                    if (isChecked) selectedItems.add(which);
                    else selectedItems.remove(Integer.valueOf(which));
                })
                .setPositiveButton("Delete Selected", (dialog, which) -> {
                    for (int index : selectedItems) {
                        db.deleteSession(sessions.get(index).id);
                    }
                    updateMenuWithSessions();
                    Toast.makeText(this, "Selected chats deleted.", Toast.LENGTH_SHORT).show();
                    loadFragment(new ChatFragment());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChatOptionsDialog(int sessionId, String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(new CharSequence[]{"Open Chat", "Delete Chat"}, (dialog, which) -> {
                    if (which == 0) {
                        ChatFragment chat = new ChatFragment();
                        Bundle b = new Bundle();
                        b.putLong("sessionId", sessionId);
                        chat.setArguments(b);
                        loadFragment(chat);
                    } else {
                        new MoodDatabase(this).deleteSession(sessionId);
                        updateMenuWithSessions();
                        loadFragment(new ChatFragment());
                    }
                })
                .show();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }

    public void updateMenuWithSessions() {
        NavigationView navView = findViewById(R.id.nav_view);
        android.view.Menu menu = navView.getMenu();
        menu.removeGroup(2);

        MoodDatabase db = new MoodDatabase(this);
        List<MoodDatabase.SessionHeader> sessions = db.getCategorizedSessions();

        android.view.SubMenu todaySub = null;
        android.view.SubMenu yesterdaySub = null;
        android.view.SubMenu previousSub = null;

        for (MoodDatabase.SessionHeader session : sessions) {
            android.view.SubMenu targetSub;
            if (session.category.equals("Today")) {
                if (todaySub == null) todaySub = menu.addSubMenu(2, android.view.Menu.NONE, 1, "Today's History");
                targetSub = todaySub;
            } else if (session.category.equals("Yesterday")) {
                if (yesterdaySub == null) yesterdaySub = menu.addSubMenu(2, android.view.Menu.NONE, 2, "Yesterday");
                targetSub = yesterdaySub;
            } else {
                if (previousSub == null) previousSub = menu.addSubMenu(2, android.view.Menu.NONE, 3, "Older Logs");
                targetSub = previousSub;
            }

            android.view.MenuItem item = targetSub.add(2, (int)session.id, android.view.Menu.NONE, session.title);
            item.setIcon(R.drawable.ic_chat_bubble);
        }
    }
}