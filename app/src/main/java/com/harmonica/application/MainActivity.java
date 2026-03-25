package com.harmonica.application;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize UI Components
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ImageButton btnMenu = findViewById(R.id.btnMenu);

        // 2. Handle the Menu Button (Open the side drawer)
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 3. Handle Navigation Clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;

            if (id == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (id == R.id.nav_stats) {
                // We will create this next!
                // selectedFragment = new StatsFragment();
            } else if (id == R.id.nav_edu) {
                // We will create this next!
                // selectedFragment = new EducationFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
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
    }

    // Helper method to swap fragments smoothly
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}