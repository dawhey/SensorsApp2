package com.example.dawhey.sensorsapp.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.dawhey.sensorsapp.Models.ChartEntry;
import com.example.dawhey.sensorsapp.Models.Entries;
import com.example.dawhey.sensorsapp.Models.Entry;
import com.example.dawhey.sensorsapp.R;
import com.example.dawhey.sensorsapp.Utilities.EntriesEvent;
import com.example.dawhey.sensorsapp.Utilities.EntryMarkerView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dawhey on 15.09.16.
 */
public class DataChartFragment extends Fragment implements OnChartValueSelectedListener {

    private LineChart chart;
    private Entries entries;
    private List<Entry> entriesList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        entriesList = new ArrayList<>();
        Bundle arguments = this.getArguments();
        if (arguments != null) {
            entries = (Entries) arguments.getSerializable("entries");
            if (entries != null) {
                entriesList = Entries.getLatestEntries(entries);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.data_chart, null);
        chart = (LineChart) v.findViewById(R.id.chart);

        if (this.entries != null) {
            initializeChart(this.entries);
        }
        return v;
    }

    private void initializeChart(Entries entries) {
        LineDataSet tempSet = initializeDataSet("Temperature", getResources().getColor(R.color.temperature), getResources().getColor(R.color.humidity), entries);
        LineDataSet humSet = initializeDataSet("Humidity", getResources().getColor(R.color.humidity), getResources().getColor(R.color.temperature), entries);

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(tempSet);
        dataSets.add(humSet);

        LineData data = new LineData(dataSets);

        chart.setDescription("");
        chart.setData(data);
        chart.setMarkerView(new EntryMarkerView(getContext(), R.layout.marker_popup));
        chart.getXAxis().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.invalidate(); // refresh
    }

    public LineDataSet initializeDataSet(String tag, int lineColor, int highlightColor, Entries entries) {
        List<com.github.mikephil.charting.data.Entry> chartEntries = new ArrayList<>();

        float reftime = 0;
        float startTime = 0;
        for (int i = 0; i < entries.getEntries().size(); i+=5) {
            Entry currentEntry = entries.getEntries().get(i);
            if (i == 0) {
                startTime = currentEntry.getUnixTimestamp();
            }
            ChartEntry chartEntry = new ChartEntry();
            if (tag.equalsIgnoreCase("temperature")) {
                chartEntry.setY(currentEntry.getTemperature().floatValue());

            } else if (tag.equalsIgnoreCase("humidity")) {
                chartEntry.setY(currentEntry.getHumidity().floatValue());
            }

            float unixTimestamp = currentEntry.getUnixTimestamp();
            float timeX = (unixTimestamp - startTime);
            if (timeX > reftime) {
                chartEntry.setX(timeX);
                chartEntry.setTag(tag);
                chartEntry.setTimestamp(currentEntry.getFormattedTimestamp());
                chartEntries.add(chartEntry);
            }
            reftime = timeX;
        }

        LineDataSet dataSet = new LineDataSet(chartEntries, tag);
        dataSet.setColor(lineColor);
        dataSet.setHighLightColor(highlightColor);
        dataSet.setHighlightLineWidth(1.3f);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setDrawCircles(false);

        return dataSet;
    }

    @Subscribe
    public void onEntriesEvent(EntriesEvent event) {
        this.entries = event.getEntries();
        this.entriesList = Entries.getLatestEntries(event.getEntries());
        initializeChart(this.entries);
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }
}
