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
import com.google.android.material.chip.ChipGroup;

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
import java.util.stream.Collectors;

public class ReportsFragment extends Fragment {

    private PieChart mStockPieChart, mTypePieChart, mRoomUsagePieChart;
    private BarChart mBorrowBarChart;
    private HorizontalBarChart mTopItemsChart;
    private ChipGroup mFilterChipGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reports, container, false);

        mTypePieChart = v.findViewById(R.id.type_pie_chart);
        mStockPieChart = v.findViewById(R.id.stock_pie_chart);
        mRoomUsagePieChart = v.findViewById(R.id.room_usage_chart);
        mBorrowBarChart = v.findViewById(R.id.borrow_bar_chart);
        mTopItemsChart = v.findViewById(R.id.top_items_chart);
        
        mFilterChipGroup = v.findViewById(R.id.report_filter_chip_group);
        mFilterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> updateAllCharts());

        updateAllCharts();

        v.findViewById(R.id.btn_export_pdf).setOnClickListener(view -> exportToPDF());
        v.findViewById(R.id.btn_export_csv).setOnClickListener(view -> exportToCSV());
        v.findViewById(R.id.btn_export_excel).setOnClickListener(view -> exportToCSV());

        return v;
    }

    private void updateAllCharts() {
        setupClassificationChart();
        setupStockChart();
        setupRoomUsageChart();
        setupBorrowChart();
        setupTopItemsChart();
    }

    private List<SupplyItem> filterItemsByType(List<SupplyItem> items) {
        int checkedId = mFilterChipGroup.getCheckedChipId();
        if (checkedId == R.id.chip_consumable_reports) {
            return items.stream().filter(i -> SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(i.getItemType())).collect(Collectors.toList());
        } else if (checkedId == R.id.chip_borrowable_reports) {
            return items.stream().filter(i -> SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(i.getItemType())).collect(Collectors.toList());
        }
        return items;
    }

    private void setupClassificationChart() {
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (items == null || items.isEmpty() || !isAdded()) return;

            long consumable = items.stream().filter(i -> SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(i.getItemType())).count();
            long borrowable = items.stream().filter(i -> SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(i.getItemType())).count();

            List<PieEntry> entries = new ArrayList<>();
            if (consumable > 0) entries.add(new PieEntry(consumable, "Consumable"));
            if (borrowable > 0) entries.add(new PieEntry(borrowable, "Borrowable"));

            updatePieChart(mTypePieChart, entries, "Item Types");
        });
    }

    private void setupStockChart() {
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (items == null || items.isEmpty() || !isAdded()) return;
            
            List<SupplyItem> filteredItems = filterItemsByType(items);
            Map<String, Integer> categoryCount = new HashMap<>();
            for (SupplyItem item : filteredItems) {
                String cat = item.getCategory() != null ? item.getCategory() : "Other";
                categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + item.getAvailableQuantity());
            }

            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }

            updatePieChart(mStockPieChart, entries, "Category Stock");
        });
    }

    private void setupRoomUsageChart() {
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (items == null || items.isEmpty() || !isAdded()) return;
            
            List<SupplyItem> filteredItems = filterItemsByType(items);
            Map<String, Integer> roomUsage = new HashMap<>();
            for (SupplyItem item : filteredItems) {
                String room = item.getRoom() != null ? item.getRoom() : "General";
                int used = item.getTotalQuantity() - item.getAvailableQuantity();
                roomUsage.put(room, roomUsage.getOrDefault(room, 0) + used);
            }

            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : roomUsage.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }

            updatePieChart(mRoomUsagePieChart, entries, "Room Usage");
        });
    }

    private void updatePieChart(PieChart chart, List<PieEntry> entries, String label) {
        if (entries.isEmpty()) {
            chart.clear();
            chart.setNoDataText("No data available for this filter");
            chart.invalidate();
            return;
        }

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
        SupplyLab lab = SupplyLab.get(getActivity());
        lab.getItemsAsync(items -> {
            if (items == null || !isAdded()) return;
            
            List<SupplyItem> filteredItems = filterItemsByType(items);
            List<String> filteredIds = filteredItems.stream().map(i -> i.getId().toString()).collect(Collectors.toList());

            lab.getAllBorrowRecordsAsync(records -> {
                if (records == null || !isAdded()) return;

                List<BorrowRecord> filteredRecords = records.stream()
                        .filter(r -> filteredIds.contains(r.getItemId().toString()))
                        .collect(Collectors.toList());

                final String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int[] monthlyTotals = new int[12];
                
                Calendar cal = Calendar.getInstance();
                for (BorrowRecord record : filteredRecords) {
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

                BarDataSet dataSet = new BarDataSet(entries, "Monthly Issuance");
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
        });
    }

    private void setupTopItemsChart() {
        SupplyLab lab = SupplyLab.get(getActivity());
        lab.getItemsAsync(items -> {
            if (items == null || items.isEmpty() || !isAdded()) return;
            
            List<SupplyItem> filteredItems = filterItemsByType(items);
            List<String> filteredNames = filteredItems.stream().map(SupplyItem::getName).collect(Collectors.toList());

            lab.getTopBorrowedItemsAsync(5, topItems -> {
                if (topItems == null || !isAdded()) return;

                List<Map.Entry<String, Integer>> filteredTop = topItems.stream()
                        .filter(e -> filteredNames.contains(e.getKey()))
                        .collect(Collectors.toList());

                List<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                for (int i = 0; i < filteredTop.size(); i++) {
                    entries.add(new BarEntry(i, filteredTop.get(i).getValue()));
                    labels.add(filteredTop.get(i).getKey());
                }

                BarDataSet dataSet = new BarDataSet(entries, "Most Active Items");
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
                StringBuilder csvInventory = new StringBuilder("Item ID,Item Name,Category,Item Type,Total Stock,Available,Borrowed,Used,Unit,Description,Condition,Status,Date Added\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                for (SupplyItem item : items) {
                    csvInventory.append(item.getBarcode() != null ? item.getBarcode() : item.getId()).append(",")
                       .append(escapeCsv(item.getName())).append(",")
                       .append(escapeCsv(item.getCategory())).append(",")
                       .append(item.getItemType()).append(",")
                       .append(item.getTotalQuantity()).append(",")
                       .append(item.getAvailableQuantity()).append(",")
                       .append(item.getBorrowedQuantity()).append(",")
                       .append(item.getUsedQuantity()).append(",")
                       .append(escapeCsv(item.getUnit())).append(",")
                       .append(escapeCsv(item.getDescription())).append(",")
                       .append(item.getCondition()).append(",")
                       .append(item.getStatus()).append(",")
                       .append(sdf.format(item.getDateAdded())).append("\n");
                }
                saveFile("School_Inventory_Data.csv", csvInventory.toString().getBytes());

                StringBuilder csvBorrows = new StringBuilder("Item ID,Borrower,Quantity,Date Borrowed,Actual Return,Status\n");
                SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                for (BorrowRecord record : borrows) {
                    csvBorrows.append(record.getItemId()).append(",")
                            .append(escapeCsv(record.getBorrowerName())).append(",")
                            .append(record.getQuantity()).append(",")
                            .append(fullSdf.format(record.getDateBorrowed())).append(",")
                            .append(record.getActualReturnDate() != null ? fullSdf.format(record.getActualReturnDate()) : "N/A").append(",")
                            .append(record.getStatus()).append("\n");
                }
                saveFile("Borrowing_Logs_Detailed.csv", csvBorrows.toString().getBytes());
                
                Toast.makeText(getActivity(), "Reports exported to application storage", Toast.LENGTH_LONG).show();
            });
        });
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void exportToPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        
        android.graphics.Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        
        canvas.drawText("School Supply Inventory System Report", 50, 50, paint);
        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()), 50, 80, paint);
        
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            int y = 120;
            paint.setFakeBoldText(true);
            canvas.drawText("MASTER INVENTORY SUMMARY", 50, y, paint);
            y += 25;
            paint.setFakeBoldText(false);
            canvas.drawText("Name | Category | Type | Total | Available | Status", 50, y, paint);
            y += 10;
            canvas.drawLine(50, y, 545, y, paint);
            y += 20;
            
            for (int i = 0; i < Math.min(items.size(), 30); i++) {
                SupplyItem item = items.get(i);
                String line = String.format(Locale.getDefault(), "%s | %s | %s | %d | %d | %s", 
                        item.getName(), item.getCategory(), item.getItemType(), item.getTotalQuantity(), item.getAvailableQuantity(), item.getStatus());
                canvas.drawText(line, 50, y, paint);
                y += 20;
                if (y > 800) break;
            }
            
            document.finishPage(page);
            
            File file = new File(requireContext().getExternalFilesDir(null), "Comprehensive_Inventory_Report.pdf");
            try {
                document.writeTo(new FileOutputStream(file));
                Toast.makeText(getActivity(), "PDF Saved: " + file.getName(), Toast.LENGTH_LONG).show();
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
