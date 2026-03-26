package com.harmonica.application;
// pulls data from the database and draws the graph.

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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.List;

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
        List<Integer> scores = db.getRecentScores();
        List<Entry> entries = new ArrayList<>();

        // Convert scores to chart points
        for (int i = 0; i < scores.size(); i++) {
            entries.add(new Entry(i, scores.get(scores.size() - 1 - i)));
        }

        int primaryColor = getResources().getColor(R.color.harmonica_primary, null);
        int accentColor = getResources().getColor(R.color.harmonica_accent, null);

        LineDataSet dataSet = new LineDataSet(entries, "Mood Intensity");
        dataSet.setColor(primaryColor); // Olive
        dataSet.setCircleColor(accentColor); // Sunset
        dataSet.setLineWidth(3f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setEnabled(false); // Minimalist look
        chart.invalidate(); // Refresh
    }
}
