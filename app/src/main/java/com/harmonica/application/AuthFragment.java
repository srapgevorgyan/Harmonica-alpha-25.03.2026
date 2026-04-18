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
                Log.d("AuthFragment", "Google Sign-In activity result received. Code: " + result.getResultCode());
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            Log.d("AuthFragment", "Google Account found: " + account.getEmail());
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Log.e("AuthFragment", "Google sign in failed code: " + e.getStatusCode(), e);
                        String errorMsg = getGoogleErrorMessage(e.getStatusCode());
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("AuthFragment", "Google Sign-In canceled or failed. Result code: " + result.getResultCode());
                    if (result.getResultCode() != android.app.Activity.RESULT_CANCELED) {
                        Toast.makeText(getContext(), "Sign-in failed. Result: " + result.getResultCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private String getGoogleErrorMessage(int statusCode) {
        switch (statusCode) {
            case 7: return "Google Error 7: Network error. Please check your connection.";
            case 10: return "Google Error 10: Configuration error. Ensure SHA-1 is in Firebase and Web Client ID is correct.";
            case 12500: return "Google Error 12500: Sign-in failed. Check Play Services or account status.";
            case 12501: return "Sign-in canceled by user.";
            default: return "Google sign in failed (Code " + statusCode + "). Please check Firebase Console.";
        }
    }

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
        int clientIdRes = getResources().getIdentifier("default_web_client_id", "string", requireContext().getPackageName());
        
        String webClientId = null;
        if (clientIdRes != 0) {
            webClientId = getString(clientIdRes);
            Log.d("AuthFragment", "Using Web Client ID from resources: " + webClientId);
        } else {
            // Fallback
            webClientId = "369981916912-8ut1eqemthnu9l0dhsac2opjm4rgelec.apps.googleusercontent.com";
            Log.w("AuthFragment", "Using fallback Web Client ID. This may fail if not correct for your project.");
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) {
            Log.e("AuthFragment", "ID Token is null");
            Toast.makeText(getContext(), "Failed to get token from Google", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthFragment", "Firebase Auth with Google successful");
                        proceedToApp();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown Firebase error";
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
        if (editEmail == null || editPassword == null) return;
        
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
                Toast.makeText(getContext(), "Password must be 8+ chars, with upper, lower, and special char (@#$%^&+=!)", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (isSignInMode) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && (user.isEmailVerified() || !user.getProviderData().get(1).getProviderId().equals("password"))) {
                                proceedToApp();
                            } else if (user != null) {
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
