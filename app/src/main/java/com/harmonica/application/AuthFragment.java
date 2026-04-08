package com.harmonica.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.regex.Pattern;

public class AuthFragment extends Fragment {

    private TextInputEditText editEmail, editPassword, editConfirmPassword;
    private TextInputLayout layoutConfirmPassword;
    private TextView txtPasswordReqs, txtSwitch;
    private Button btnAction, btnSkip, btnGoogle;
    private boolean isSignInMode = true;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Log.e("AuthFragment", "Google sign in failed", e);
                        Toast.makeText(getContext(), "Google sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_auth, container, false);

        mAuth = FirebaseAuth.getInstance();
        setupGoogleSignIn();

        editEmail = v.findViewById(R.id.editEmail);
        editPassword = v.findViewById(R.id.editPassword);
        editConfirmPassword = v.findViewById(R.id.editConfirmPassword);
        layoutConfirmPassword = v.findViewById(R.id.layoutConfirmPassword);
        txtPasswordReqs = v.findViewById(R.id.txtPasswordRequirements);
        txtSwitch = v.findViewById(R.id.txtSwitchAuth);
        btnAction = v.findViewById(R.id.btnAuthAction);
        btnSkip = v.findViewById(R.id.btnSkip);
        btnGoogle = v.findViewById(R.id.btnGoogleSignIn);

        btnAction.setOnClickListener(view -> handleAuth());
        btnSkip.setOnClickListener(view -> proceedToApp());
        txtSwitch.setOnClickListener(view -> toggleMode());
        btnGoogle.setOnClickListener(view -> signInWithGoogle());

        return v;
    }

    private void setupGoogleSignIn() {
        // Use the default_web_client_id which is generated from google-services.json
        // If this string is missing, ensure you have enabled Google Sign-In in Firebase Console
        // and re-downloaded the google-services.json file.
        int clientIdRes = getResources().getIdentifier("default_web_client_id", "string", requireContext().getPackageName());
        
        String webClientId;
        if (clientIdRes != 0) {
            webClientId = getString(clientIdRes);
        } else {
            // Fallback to the one you provided if resource is missing, 
            // but note that it might need to be the full ID (usually starts with numbers)
            webClientId = "harmonica-5d34d.apps.googleusercontent.com"; 
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    private void signInWithGoogle() {
        // Sign out first to ensure account picker always appears
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) {
            Toast.makeText(getContext(), "Failed to get ID Token from Google", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        proceedToApp();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e("AuthFragment", "Firebase Auth with Google failed: " + error);
                        Toast.makeText(getContext(), "Auth Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void toggleMode() {
        isSignInMode = !isSignInMode;
        btnAction.setText(isSignInMode ? "Sign In" : "Sign Up");
        txtSwitch.setText(isSignInMode ? "Don't have an account? Sign Up" : "Already have an account? Sign In");
        
        int visibility = isSignInMode ? View.GONE : View.VISIBLE;
        layoutConfirmPassword.setVisibility(visibility);
        txtPasswordReqs.setVisibility(visibility);
    }

    private void handleAuth() {
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isSignInMode) {
            String confirmPassword = editConfirmPassword.getText() != null ? editConfirmPassword.getText().toString().trim() : "";
            if (!password.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidPassword(password)) {
                Toast.makeText(getContext(), "Password does not meet requirements", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (isSignInMode) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                proceedToApp();
                            } else {
                                Toast.makeText(getContext(), "Please verify your email first.", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            if (verifyTask.isSuccessful()) {
                                                Toast.makeText(getContext(), "Verification email sent.", Toast.LENGTH_LONG).show();
                                                mAuth.signOut();
                                                toggleMode();
                                            }
                                        });
                            }
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                            Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private boolean isValidPassword(String password) {
        Pattern upperCase = Pattern.compile("[A-Z]");
        Pattern lowerCase = Pattern.compile("[a-z]");
        Pattern specialChar = Pattern.compile("[@#$%^&+=!]");
        
        return password.length() >= 8 &&
                upperCase.matcher(password).find() &&
                lowerCase.matcher(password).find() &&
                specialChar.matcher(password).find();
    }

    private void proceedToApp() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onAuthFinished();
        }
    }
}
