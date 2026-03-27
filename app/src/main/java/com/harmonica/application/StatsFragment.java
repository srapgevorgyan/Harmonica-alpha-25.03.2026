package com.harmonica.application;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats, container, false);
        chart = v.findViewById(R.id.moodChart);
        db = new MoodDatabase(getContext());

        setupChart();
        return v;
    }

    private void setupChart() {
        // 1. Get the full entries (score + timestamp)
        List<MoodDatabase.MoodEntry> entriesData = db.getRecentMoodEntries();
        if (entriesData.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        // 2. Prepare data (Reverse because DB returns newest first, but chart goes left-to-right)
        for (int i = 0; i < entriesData.size(); i++) {
            MoodDatabase.MoodEntry entry = entriesData.get(entriesData.size() - 1 - i);
            entries.add(new Entry(i, entry.score));
            labels.add(sdf.format(new Date(entry.timestamp)));
        }

        int primaryColor = getResources().getColor(R.color.harmonica_primary, null);
        int accentColor = getResources().getColor(R.color.harmonica_accent, null);

        // 3. Style the Line
        LineDataSet dataSet = new LineDataSet(entries, "Mood Intensity");
        dataSet.setColor(primaryColor);
        dataSet.setCircleColor(accentColor);
        dataSet.setLineWidth(3f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
        dataSet.setFillColor(primaryColor);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // 4. Configure X-Axis (The Time Labels)
        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(true); // Enable it!
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });

        // 5. General Chart Styling
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false); // Hide right axis for cleaner look
        chart.getAxisLeft().setGridColor(Color.LTGRAY);

        chart.animateY(1000); // Nice entrance animation
        chart.invalidate(); // Refresh
    }
}