package com.harmonica.application;

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

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextView txtEmail, txtAccountType;
    private ImageView imgProfile;
    private LinearLayout layoutPasswordChange, layoutGuestCTA;
    private TextInputEditText editNewPassword, editConfirmNewPassword;
    private Button btnUpdatePassword, btnGoToLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        txtEmail = v.findViewById(R.id.txtUserEmail);
        txtAccountType = v.findViewById(R.id.txtAccountType);
        imgProfile = v.findViewById(R.id.imgProfile);
        layoutPasswordChange = v.findViewById(R.id.layoutPasswordChange);
        layoutGuestCTA = v.findViewById(R.id.layoutGuestCTA);
        editNewPassword = v.findViewById(R.id.editNewPassword);
        editConfirmNewPassword = v.findViewById(R.id.editConfirmNewPassword);
        btnUpdatePassword = v.findViewById(R.id.btnUpdatePassword);
        btnGoToLogin = v.findViewById(R.id.btnGoToLogin);

        if (user != null) {
            setupUserUI(user);
        } else {
            setupGuestUI();
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
