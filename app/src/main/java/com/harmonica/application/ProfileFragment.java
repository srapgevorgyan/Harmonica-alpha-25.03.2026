package com.harmonica.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextView txtEmail, txtAccountType, txtStreakCount;
    private ImageView imgProfile;
    private LinearLayout layoutPasswordChange, layoutGuestCTA, layoutWeeklySquares;
    private TextInputEditText editNewPassword, editConfirmNewPassword, editGeminiKey;
    private Button btnUpdatePassword, btnGoToLogin, btnSaveApiKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        txtEmail = v.findViewById(R.id.txtUserEmail);
        txtAccountType = v.findViewById(R.id.txtAccountType);
        txtStreakCount = v.findViewById(R.id.txtStreakCount);
        imgProfile = v.findViewById(R.id.imgProfile);
        layoutPasswordChange = v.findViewById(R.id.layoutPasswordChange);
        layoutGuestCTA = v.findViewById(R.id.layoutGuestCTA);
        layoutWeeklySquares = v.findViewById(R.id.layoutWeeklySquares);
        editNewPassword = v.findViewById(R.id.editNewPassword);
        editConfirmNewPassword = v.findViewById(R.id.editConfirmNewPassword);
        editGeminiKey = v.findViewById(R.id.editGeminiApiKey);
        btnUpdatePassword = v.findViewById(R.id.btnUpdatePassword);
        btnGoToLogin = v.findViewById(R.id.btnGoToLogin);
        btnSaveApiKey = v.findViewById(R.id.btnSaveApiKey);

        // Load existing API key
        SharedPreferences prefs = requireContext().getSharedPreferences("HarmonicaPrefs", Context.MODE_PRIVATE);
        editGeminiKey.setText(prefs.getString("gemini_api_key", ""));

        btnSaveApiKey.setOnClickListener(view -> {
            String key = editGeminiKey.getText().toString().trim();
            prefs.edit().putString("gemini_api_key", key).apply();
            Toast.makeText(getContext(), "API Key Updated. Restarting session...", Toast.LENGTH_SHORT).show();
        });

        if (user != null) {
            setupUserUI(user);
            calculateStreakAndActivity(user.getUid());
        } else {
            setupGuestUI();
            calculateStreakAndActivity(null);
        }

        return v;
    }

    private void setupUserUI(FirebaseUser user) {
        txtEmail.setText(user.getEmail());
        layoutGuestCTA.setVisibility(View.GONE);
        
        boolean isGoogleUser = false;
        for (UserInfo profile : user.getProviderData()) {
            if ("google.com".equals(profile.getProviderId())) {
                isGoogleUser = true;
                break;
            }
        }

        if (isGoogleUser) {
            txtAccountType.setText("Linked with Google");
            layoutPasswordChange.setVisibility(View.GONE);
        } else {
            txtAccountType.setText("Verified Harmonica Account");
            layoutPasswordChange.setVisibility(View.VISIBLE);
            btnUpdatePassword.setOnClickListener(view -> updatePassword(user));
        }
    }

    private void setupGuestUI() {
        txtEmail.setText("Guest Session");
        txtAccountType.setText("Unregistered Mode");
        layoutPasswordChange.setVisibility(View.GONE);
        layoutGuestCTA.setVisibility(View.VISIBLE);
        btnGoToLogin.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new AuthFragment());
            }
        });
    }

    private void calculateStreakAndActivity(String uid) {
        MoodDatabase db = new MoodDatabase(getContext());
        Set<String> activityDates = db.getActivityDates(uid, 365);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        if (cal.getFirstDayOfWeek() == Calendar.SUNDAY) cal.add(Calendar.DAY_OF_YEAR, 1);

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            View square = layoutWeeklySquares.getChildAt(i);
            if (square != null) {
                if (activityDates.contains(dateStr)) {
                    square.setBackgroundColor(Color.parseColor("#4CAF50"));
                } else {
                    square.setBackgroundColor(Color.parseColor("#E0E0E0"));
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        int streak = 0;
        Calendar streakCal = Calendar.getInstance();
        String todayStr = sdf.format(streakCal.getTime());
        
        if (activityDates.contains(todayStr)) {
            streak = 1;
            streakCal.add(Calendar.DAY_OF_YEAR, -1);
            while (activityDates.contains(sdf.format(streakCal.getTime()))) {
                streak++;
                streakCal.add(Calendar.DAY_OF_YEAR, -1);
            }
        } else {
            streakCal.add(Calendar.DAY_OF_YEAR, -1);
            while (activityDates.contains(sdf.format(streakCal.getTime()))) {
                streak++;
                streakCal.add(Calendar.DAY_OF_YEAR, -1);
            }
        }
        
        txtStreakCount.setText(streak + " Days");
    }

    private void updatePassword(FirebaseUser user) {
        String pass = editNewPassword.getText().toString().trim();
        String confirm = editConfirmNewPassword.getText().toString().trim();

        if (pass.isEmpty() || !pass.equals(confirm)) {
            Toast.makeText(getContext(), "Passwords must match and not be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 8) {
            Toast.makeText(getContext(), "Password must be at least 8 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.updatePassword(pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Password updated successfully.", Toast.LENGTH_SHORT).show();
                editNewPassword.setText("");
                editConfirmNewPassword.setText("");
            } else {
                Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
