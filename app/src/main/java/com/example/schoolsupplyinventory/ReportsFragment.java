package com.example.schoolsupplyinventory;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsFragment extends Fragment {

    private PieChart mStockPieChart;
    private BarChart mBorrowBarChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reports, container, false);

        mStockPieChart = v.findViewById(R.id.stock_pie_chart);
        mBorrowBarChart = v.findViewById(R.id.borrow_bar_chart);

        setupStockChart();
        setupBorrowChart();

        v.findViewById(R.id.btn_export_pdf).setOnClickListener(view -> 
            Toast.makeText(getActivity(), "Exporting to PDF...", Toast.LENGTH_SHORT).show());
        
        v.findViewById(R.id.btn_export_excel).setOnClickListener(view -> 
            Toast.makeText(getActivity(), "Exporting to CSV...", Toast.LENGTH_SHORT).show());

        return v;
    }

    private void setupStockChart() {
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (items == null || items.isEmpty()) return;
            
            Map<String, Integer> categoryCount = new HashMap<>();
            for (SupplyItem item : items) {
                String cat = item.getCategory() != null ? item.getCategory().name() : "Other";
                categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + item.getQuantity());
            }

            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(new int[]{
                    Color.parseColor("#8B5CF6"), 
                    Color.parseColor("#10B981"), 
                    Color.parseColor("#F59E0B"), 
                    Color.parseColor("#3B82F6"),
                    Color.parseColor("#A78BFA")
            });
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueTextSize(12f);

            PieData data = new PieData(dataSet);
            mStockPieChart.setData(data);
            mStockPieChart.getDescription().setEnabled(false);
            mStockPieChart.setHoleColor(Color.TRANSPARENT);
            mStockPieChart.setCenterText("Inventory");
            mStockPieChart.setCenterTextColor(Color.WHITE);
            mStockPieChart.getLegend().setTextColor(Color.WHITE);
            mStockPieChart.setEntryLabelColor(Color.WHITE);
            mStockPieChart.invalidate();
        });
    }

    private void setupBorrowChart() {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, 10f));
        entries.add(new BarEntry(1f, 25f));
        entries.add(new BarEntry(2f, 15f));
        entries.add(new BarEntry(3f, 30f));
        entries.add(new BarEntry(4f, 20f));

        BarDataSet dataSet = new BarDataSet(entries, "Items Borrowed");
        dataSet.setColor(Color.parseColor("#8B5CF6"));
        dataSet.setValueTextColor(Color.WHITE);

        BarData data = new BarData(dataSet);
        mBorrowBarChart.setData(data);
        
        final String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May"};
        mBorrowBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        mBorrowBarChart.getXAxis().setTextColor(Color.WHITE);
        mBorrowBarChart.getXAxis().setGranularity(1f);
        mBorrowBarChart.getAxisLeft().setTextColor(Color.WHITE);
        mBorrowBarChart.getAxisRight().setEnabled(false);
        mBorrowBarChart.getLegend().setTextColor(Color.WHITE);
        mBorrowBarChart.getDescription().setEnabled(false);
        mBorrowBarChart.invalidate();
    }
}
