package com.harmonica.application;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Random;

public class EducationFragment extends Fragment {

    private TextView txtName, txtDesc;
    private String currentUrl = "https://www.mayoclinic.org";

    // Data structure for hormones
    private static class Hormone {
        String name;
        String description;
        String url;

        Hormone(String n, String d, String u) {
            this.name = n;
            this.description = d;
            this.url = u;
        }
    }

    private final Hormone[] hormones = {
            new Hormone("Serotonin", "The 'happiness' chemical. It stabilizes mood, feelings of well-being, and happiness.", "https://www.healthline.com/health/mental-health/serotonin"),
            new Hormone("Dopamine", "The 'reward' chemical. It plays a role in how we feel pleasure and motivation.", "https://www.webmd.com/mental-health/what-is-dopamine"),
            new Hormone("Oxytocin", "The 'love' hormone. It is released during physical touch and social bonding.", "https://www.medicalnewstoday.com/articles/275795"),
            new Hormone("Melatonin", "The 'sleep' hormone. It signals to your brain that it's time to rest.", "https://www.sleepfoundation.org/melatonin"),
            new Hormone("Cortisol", "The 'stress' hormone. It triggers your fight-or-flight response.", "https://www.mayoclinic.org/healthy-lifestyle/stress-management/in-depth/stress/art-20046037")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_education, container, false);

        txtName = v.findViewById(R.id.txtHormoneName);
        txtDesc = v.findViewById(R.id.txtHormoneDesc);
        Button btnLearn = v.findViewById(R.id.btnLearnMore);
        Button btnNext = v.findViewById(R.id.btnNextHormone);

        showRandomHormone();

        btnNext.setOnClickListener(view -> showRandomHormone());

        btnLearn.setOnClickListener(view -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
            startActivity(browserIntent);
        });

        return v;
    }

    private void showRandomHormone() {
        int index = new Random().nextInt(hormones.length);
        Hormone h = hormones[index];
        txtName.setText(h.name);
        txtDesc.setText(h.description);
        currentUrl = h.url;
    }
}
