package com.example.schoolsupplyinventory;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsFragment extends Fragment {

    private PieChart mStockPieChart;
    private PieChart mRoomUsagePieChart;
    private BarChart mBorrowBarChart;
    private HorizontalBarChart mTopItemsChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reports, container, false);

        mStockPieChart = v.findViewById(R.id.stock_pie_chart);
        mRoomUsagePieChart = v.findViewById(R.id.room_usage_chart);
        mBorrowBarChart = v.findViewById(R.id.borrow_bar_chart);
        mTopItemsChart = v.findViewById(R.id.top_items_chart);

        setupStockChart();
        setupRoomUsageChart();
        setupBorrowChart();
        setupTopItemsChart();

        v.findViewById(R.id.btn_export_pdf).setOnClickListener(view -> exportToPDF());
        v.findViewById(R.id.btn_export_csv).setOnClickListener(view -> exportToCSV());
        v.findViewById(R.id.btn_export_excel).setOnClickListener(view -> exportToCSV()); // CSV is Excel compatible

        return v;
    }

    private void setupStockChart() {
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (items == null || items.isEmpty() || !isAdded()) return;
            
            Map<String, Integer> categoryCount = new HashMap<>();
            for (SupplyItem item : items) {
                String cat = item.getCategory() != null ? item.getCategory() : "Other";
                categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + item.getQuantity());
            }

            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }

            updatePieChart(mStockPieChart, entries, "Inventory Stock");
        });
    }

    private void setupRoomUsageChart() {
        SupplyLab.get(getActivity()).getUsageByRoomAsync(roomUsage -> {
            if (roomUsage == null || roomUsage.isEmpty() || !isAdded()) return;

            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : roomUsage.entrySet()) {
                String room = entry.getKey() != null ? entry.getKey() : "General";
                entries.add(new PieEntry(entry.getValue(), room));
            }

            updatePieChart(mRoomUsagePieChart, entries, "Usage by Dept");
        });
    }

    private void updatePieChart(PieChart chart, List<PieEntry> entries, String label) {
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getChartColors());
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setCenterText(label);
        chart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        chart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        chart.setEntryLabelColor(Color.WHITE);
        chart.animateY(1000);
        chart.invalidate();
    }

    private void setupBorrowChart() {
        SupplyLab.get(getActivity()).getAllBorrowRecordsAsync(records -> {
            if (records == null || !isAdded()) return;

            final String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            int[] monthlyTotals = new int[12];
            
            Calendar cal = Calendar.getInstance();
            for (BorrowRecord record : records) {
                if (record.getDateBorrowed() != null) {
                    cal.setTime(record.getDateBorrowed());
                    int month = cal.get(Calendar.MONTH);
                    monthlyTotals[month] += record.getInitialQuantity();
                }
            }

            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                entries.add(new BarEntry(i, monthlyTotals[i]));
            }

            BarDataSet dataSet = new BarDataSet(entries, "Monthly Borrowings");
            dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_purple));
            dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

            BarData data = new BarData(dataSet);
            mBorrowBarChart.setData(data);
            mBorrowBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
            mBorrowBarChart.getXAxis().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mBorrowBarChart.getXAxis().setGranularity(1f);
            mBorrowBarChart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mBorrowBarChart.getAxisRight().setEnabled(false);
            mBorrowBarChart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mBorrowBarChart.getDescription().setEnabled(false);
            mBorrowBarChart.animateY(1000);
            mBorrowBarChart.invalidate();
        });
    }

    private void setupTopItemsChart() {
        SupplyLab.get(getActivity()).getTopBorrowedItemsAsync(5, topItems -> {
            if (topItems == null || topItems.isEmpty() || !isAdded()) return;

            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            // MPAndroidChart HorizontalBarChart order is bottom-up, so we might want to reverse if needed
            for (int i = 0; i < topItems.size(); i++) {
                entries.add(new BarEntry(i, topItems.get(i).getValue()));
                labels.add(topItems.get(i).getKey());
            }

            BarDataSet dataSet = new BarDataSet(entries, "Borrow Count");
            dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.secondary_purple));
            dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

            BarData data = new BarData(dataSet);
            mTopItemsChart.setData(data);
            mTopItemsChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            mTopItemsChart.getXAxis().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mTopItemsChart.getXAxis().setGranularity(1f);
            mTopItemsChart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            mTopItemsChart.getAxisRight().setEnabled(false);
            mTopItemsChart.getLegend().setEnabled(false);
            mTopItemsChart.getDescription().setEnabled(false);
            mTopItemsChart.animateX(1000);
            mTopItemsChart.invalidate();
        });
    }

    private int[] getChartColors() {
        return new int[]{
                ContextCompat.getColor(requireContext(), R.color.primary_purple),
                ContextCompat.getColor(requireContext(), R.color.color_success),
                ContextCompat.getColor(requireContext(), R.color.color_warning),
                ContextCompat.getColor(requireContext(), R.color.color_info),
                ContextCompat.getColor(requireContext(), R.color.secondary_purple),
                ContextCompat.getColor(requireContext(), R.color.color_error)
        };
    }

    private void exportToCSV() {
        SupplyLab lab = SupplyLab.get(getActivity());
        lab.getItemsAsync(items -> {
            lab.getAllBorrowRecordsAsync(borrows -> {
                // Export 1: Inventory List
                StringBuilder csvInventory = new StringBuilder("ID,Name,Category,Quantity,Unit,Room,Supplier,Barcode\n");
                for (SupplyItem item : items) {
                    csvInventory.append(item.getId()).append(",")
                       .append(item.getName()).append(",")
                       .append(item.getCategory()).append(",")
                       .append(item.getQuantity()).append(",")
                       .append(item.getUnit()).append(",")
                       .append(item.getRoom()).append(",")
                       .append(item.getSupplier()).append(",")
                       .append(item.getBarcode()).append("\n");
                }
                saveFile("Inventory_List.csv", csvInventory.toString().getBytes());

                // Export 2: Borrowing Records
                StringBuilder csvBorrows = new StringBuilder("Item ID,Borrower,Quantity,Date Borrowed,Return Date,Status\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                for (BorrowRecord record : borrows) {
                    csvBorrows.append(record.getItemId()).append(",")
                            .append(record.getBorrowerName()).append(",")
                            .append(record.getQuantity()).append(",")
                            .append(sdf.format(record.getDateBorrowed())).append(",")
                            .append(record.getActualReturnDate() != null ? sdf.format(record.getActualReturnDate()) : "N/A").append(",")
                            .append(record.getStatus()).append("\n");
                }
                saveFile("Borrowing_History.csv", csvBorrows.toString().getBytes());
                
                Toast.makeText(getActivity(), "CSV reports exported to app folder", Toast.LENGTH_LONG).show();
            });
        });
    }

    private void exportToPDF() {
        PdfDocument document = new PdfDocument();
        // Page 1: Inventory Summary
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        
        android.graphics.Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        
        canvas.drawText("SupplyFlow Inventory & Usage Report", 50, 50, paint);
        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()), 50, 80, paint);
        
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            int y = 120;
            paint.setFakeBoldText(true);
            canvas.drawText("CURRENT INVENTORY SUMMARY", 50, y, paint);
            y += 25;
            paint.setFakeBoldText(false);
            canvas.drawText("Item Name | Category | Stock | Location", 50, y, paint);
            y += 10;
            canvas.drawLine(50, y, 545, y, paint);
            y += 20;
            
            for (int i = 0; i < Math.min(items.size(), 25); i++) {
                SupplyItem item = items.get(i);
                String line = String.format(Locale.getDefault(), "%s | %s | %d %s | %s", 
                        item.getName(), item.getCategory(), item.getQuantity(), item.getUnit(), item.getRoom());
                canvas.drawText(line, 50, y, paint);
                y += 20;
            }
            
            document.finishPage(page);
            
            // Add another page if needed, but for simplicity we'll save now
            File file = new File(requireContext().getExternalFilesDir(null), "Comprehensive_Report.pdf");
            try {
                document.writeTo(new FileOutputStream(file));
                Toast.makeText(getActivity(), "PDF Report saved: " + file.getName(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "PDF Export failed", Toast.LENGTH_SHORT).show();
            }
            document.close();
        });
    }

    private void saveFile(String filename, byte[] data) {
        File file = new File(requireContext().getExternalFilesDir(null), filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            Toast.makeText(getActivity(), "Failed to save " + filename, Toast.LENGTH_SHORT).show();
        }
    }
}
