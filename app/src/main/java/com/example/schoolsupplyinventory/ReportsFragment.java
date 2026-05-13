package com.example.schoolsupplyinventory;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
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
import java.util.Calendar;
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
                String cat = item.getCategory() != null ? item.getCategory() : "Other";
                categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + item.getQuantity());
            }

            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(new int[]{
                    ContextCompat.getColor(requireContext(), R.color.primary_purple), 
                    ContextCompat.getColor(requireContext(), R.color.color_success), 
                    ContextCompat.getColor(requireContext(), R.color.color_warning), 
                    ContextCompat.getColor(requireContext(), R.color.color_info),
                    ContextCompat.getColor(requireContext(), R.color.secondary_purple)
            });
            dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            dataSet.setValueTextSize(12f);

            PieData data = new PieData(dataSet);
            mStockPieChart.setData(data);
            mStockPieChart.getDescription().setEnabled(false);
            mStockPieChart.setHoleColor(Color.TRANSPARENT);
            mStockPieChart.setCenterText("Inventory");
            mStockPieChart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mStockPieChart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mStockPieChart.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mStockPieChart.animateY(1000);
            mStockPieChart.invalidate();
        });
    }

    private void setupBorrowChart() {
        SupplyLab.get(getActivity()).getAllBorrowRecordsAsync(records -> {
            if (records == null) return;

            final String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            int[] monthlyTotals = new int[12];
            
            Calendar cal = Calendar.getInstance();
            for (BorrowRecord record : records) {
                if (record.getDateBorrowed() != null) {
                    cal.setTime(record.getDateBorrowed());
                    int month = cal.get(Calendar.MONTH);
                    monthlyTotals[month] += record.getQuantity();
                }
            }

            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                if (monthlyTotals[i] > 0) {
                    entries.add(new BarEntry(i, monthlyTotals[i]));
                }
            }

            // If no data, show some empty placeholder or handle accordingly
            if (entries.isEmpty()) {
                mBorrowBarChart.clear();
                return;
            }

            BarDataSet dataSet = new BarDataSet(entries, "Items Borrowed");
            dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_purple));
            dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            dataSet.setValueTextSize(10f);

            BarData data = new BarData(dataSet);
            mBorrowBarChart.setData(data);
            
            mBorrowBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
            mBorrowBarChart.getXAxis().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mBorrowBarChart.getXAxis().setGranularity(1f);
            mBorrowBarChart.getXAxis().setLabelCount(entries.size());
            
            mBorrowBarChart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mBorrowBarChart.getAxisLeft().setAxisMinimum(0f);
            
            mBorrowBarChart.getAxisRight().setEnabled(false);
            mBorrowBarChart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mBorrowBarChart.getDescription().setEnabled(false);
            mBorrowBarChart.animateY(1000);
            mBorrowBarChart.invalidate();
        });
    }
}
