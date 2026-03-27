package com.harmonica.application;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // UI Components
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ImageButton btnMenu = findViewById(R.id.btnMenu);

        // Menu Button (Open the side drawer)
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Navigation Clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;

            if (item.getGroupId() == 2) {
                ChatFragment chat = new ChatFragment();
                Bundle b = new Bundle();
                b.putLong("sessionId", item.getItemId());
                chat.setArguments(b);
                loadFragment(chat);
            }
            else {
                if (id == R.id.nav_chat) {
                    selectedFragment = new ChatFragment();
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

        // 4. Load the "Safe Space" (Chat) by default on startup
        if (savedInstanceState == null) {
            loadFragment(new ChatFragment());
            navigationView.setCheckedItem(R.id.nav_chat);
        }



        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If the side menu is open, close it
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Otherwise, do the normal back action (close app or go back)
                    setEnabled(false); // Disable this callback to avoid infinite loop
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true); // Re-enable it for next time
                }
            }
        });


        updateMenuWithSessions();
    }

    // Helper method to swap fragments smoothly
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void updateMenuWithSessions() {
        NavigationView navView = findViewById(R.id.nav_view);
        android.view.Menu menu = navView.getMenu();

        // Clear old dynamic items (Group 2 is our history group)
        menu.removeGroup(2);

        MoodDatabase db = new MoodDatabase(this);
        List<MoodDatabase.SessionHeader> sessions = db.getCategorizedSessions();

        android.view.SubMenu todaySub = null;
        android.view.SubMenu yesterdaySub = null;
        android.view.SubMenu previousSub = null;

        for (MoodDatabase.SessionHeader session : sessions) {
            android.view.SubMenu targetSub;

            // Group by category
            if (session.category.equals("Today")) {
                if (todaySub == null) todaySub = menu.addSubMenu(2, android.view.Menu.NONE, 1, "Today");
                targetSub = todaySub;
            } else if (session.category.equals("Yesterday")) {
                if (yesterdaySub == null) yesterdaySub = menu.addSubMenu(2, android.view.Menu.NONE, 2, "Yesterday");
                targetSub = yesterdaySub;
            } else {
                if (previousSub == null) previousSub = menu.addSubMenu(2, android.view.Menu.NONE, 3, "Previous");
                targetSub = previousSub;
            }

            // Add the session item
            android.view.MenuItem item = targetSub.add(2, (int)session.id, android.view.Menu.NONE, session.title);
            item.setIcon(R.drawable.ic_chat_bubble); // Use your chat icon here
        }
    }
}