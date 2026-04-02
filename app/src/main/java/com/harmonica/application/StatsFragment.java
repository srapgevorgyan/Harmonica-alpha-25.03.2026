package com.harmonica.application;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {
    private LineChart chart;
    private MoodDatabase db;
    private TextView txtSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats, container, false);
        chart = v.findViewById(R.id.moodChart);
        // Corrected ID: used R.id.txtSummary which matches fragment_stats.xml
        txtSummary = v.findViewById(R.id.txtSummary); 
        db = new MoodDatabase(getContext());

        setupChart();
        return v;
    }

    private void setupChart() {
        if (getContext() == null) return;

        // 1. Get entries from database (up to 30 for the month)
        List<MoodDatabase.MoodEntry> entriesData = db.getMonthMoodEntries();
        if (entriesData.isEmpty()) {
            if (txtSummary != null) txtSummary.setText("Start chatting to see your mood patterns!");
            return;
        }

        List<Entry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        float totalScore = 0;
        // 2. Prepare data (Reverse because DB returns newest first, but chart goes left-to-right)
        for (int i = 0; i < entriesData.size(); i++) {
            MoodDatabase.MoodEntry entry = entriesData.get(entriesData.size() - 1 - i);
            entries.add(new Entry(i, (float) entry.score));
            labels.add(sdf.format(new Date(entry.timestamp)));
            totalScore += entry.score;
        }

        // 3. Set summary text with mood insight
        if (txtSummary != null) {
            float avg = totalScore / entriesData.size();
            String insight = getMoodInsight(avg);
            txtSummary.setText(String.format(Locale.getDefault(), "Average Mood: %.1f/10\n\n%s", avg, insight));
        }

        // 4. Chart Styling
        int primaryColor = Color.parseColor("#9C27B0"); // harmonica_primary
        int accentColor = Color.parseColor("#FF4081");  // harmonica_accent

        LineDataSet dataSet = new LineDataSet(entries, "Mood Intensity");
        dataSet.setColor(primaryColor);
        dataSet.setCircleColor(accentColor);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(40);
        dataSet.setFillColor(primaryColor);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // 5. Axis Configuration
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(entriesData.size(), 5));
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < labels.size()) ? labels.get(index) : "";
            }
        });

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMaximum(10.5f);
        chart.getAxisLeft().setAxisMinimum(0f);
        xAxis.setYOffset(10f);

        chart.animateY(1200);
        chart.invalidate();
    }

    private String getMoodInsight(float avg) {
        if (avg >= 8) return "You've been feeling great lately! Your hormones seem balanced and your mindset is positive.";
        if (avg >= 6) return "You're doing well. Maintain your current healthy habits to keep your mood stable.";
        if (avg >= 4) return "You're in a bit of a neutral zone. Consider if stress or lack of sleep might be affecting your baseline.";
        return "It looks like you've had a tough month. Remember to be kind to yourself and speak with Dr. Harmonica for support.";
    }
}
