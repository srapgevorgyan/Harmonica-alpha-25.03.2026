package com.harmonica.application;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class EducationFragment extends Fragment {

    private TextView txtName, txtDesc, txtHormoneType;
    private ProgressBar progressBar;
    private String currentUrl = "https://www.mayoclinic.org";
    private GeminiService gemini;
    
    // List to keep track of recently shown hormones to avoid repetition
    private final List<String> recentHormones = new ArrayList<>();
    private static final int MAX_RECENT_SIZE = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_education, container, false);

        txtName = v.findViewById(R.id.txtHormoneName);
        txtDesc = v.findViewById(R.id.txtHormoneDesc);
        txtHormoneType = v.findViewById(R.id.txtHormoneType);
        progressBar = v.findViewById(R.id.progressBarEdu);
        Button btnLearn = v.findViewById(R.id.btnLearnMore);
        Button btnNext = v.findViewById(R.id.btnNextHormone);

        gemini = new GeminiService();

        loadNewHormone();

        btnNext.setOnClickListener(view -> loadNewHormone());

        btnLearn.setOnClickListener(view -> {
            if (currentUrl != null && !currentUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
                startActivity(browserIntent);
            }
        });

        return v;
    }

    private void loadNewHormone() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        gemini.getRandomHormone(recentHormones, new GeminiService.HormoneCallback() {
            @Override
            public void onResult(GeminiService.HormoneEducation hormone) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    
                    // Update the recent list
                    recentHormones.add(hormone.name);
                    if (recentHormones.size() > MAX_RECENT_SIZE) {
                        recentHormones.remove(0);
                    }
                    
                    txtName.setText(hormone.name);
                    txtDesc.setText(hormone.description);
                    if (txtHormoneType != null) txtHormoneType.setText(hormone.type);
                    currentUrl = hormone.url;
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "AI is busy: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
