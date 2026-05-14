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
    private View mLoadingOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reports, container, false);

        mTypePieChart = v.findViewById(R.id.type_pie_chart);
        mStockPieChart = v.findViewById(R.id.stock_pie_chart);
        mRoomUsagePieChart = v.findViewById(R.id.room_usage_chart);
        mBorrowBarChart = v.findViewById(R.id.borrow_bar_chart);
        mTopItemsChart = v.findViewById(R.id.top_items_chart);
        mLoadingOverlay = v.findViewById(R.id.loading_overlay);
        
        mFilterChipGroup = v.findViewById(R.id.report_filter_chip_group);
        mFilterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> updateAllCharts());

        updateAllCharts();

        v.findViewById(R.id.btn_export_pdf).setOnClickListener(view -> exportToPDF());
        v.findViewById(R.id.btn_export_csv).setOnClickListener(view -> exportToCSV());

        return v;
    }

    private void updateAllCharts() {
        if (mLoadingOverlay != null) mLoadingOverlay.setVisibility(View.VISIBLE);
        
        SupplyLab lab = SupplyLab.get(getActivity());
        lab.getItemsAsync(items -> {
            lab.getAllBorrowRecordsAsync(borrows -> {
                lab.getTopBorrowedItemsAsync(10, topItems -> {
                    if (!isAdded()) return;
                    if (mLoadingOverlay != null) mLoadingOverlay.setVisibility(View.GONE);
                    
                    List<SupplyItem> filteredItems = filterItemsByType(items);
                    
                    renderClassificationChart(items);
                    renderStockChart(filteredItems);
                    renderRoomUsageChart(filteredItems);
                    renderBorrowChart(filteredItems, borrows);
                    renderTopItemsChart(filteredItems, topItems);
                });
            });
        });
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

    private void renderClassificationChart(List<SupplyItem> items) {
        long consumable = items.stream().filter(i -> SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(i.getItemType())).count();
        long borrowable = items.stream().filter(i -> SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(i.getItemType())).count();

        List<PieEntry> entries = new ArrayList<>();
        if (consumable > 0) entries.add(new PieEntry(consumable, "Consumable"));
        if (borrowable > 0) entries.add(new PieEntry(borrowable, "Borrowable"));

        updatePieChart(mTypePieChart, entries, "Item Types");
    }

    private void renderStockChart(List<SupplyItem> items) {
        Map<String, Integer> categoryCount = new HashMap<>();
        for (SupplyItem item : items) {
            String cat = (item.getCategory() != null && !item.getCategory().isEmpty()) ? item.getCategory() : "Other";
            categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + item.getAvailableQuantity());
        }

        List<PieEntry> entries = new ArrayList<>();
        categoryCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(8)
                .forEach(e -> entries.add(new PieEntry(e.getValue(), e.getKey())));

        updatePieChart(mStockPieChart, entries, "Category Stock");
    }

    private void renderRoomUsageChart(List<SupplyItem> items) {
        Map<String, Integer> roomUsage = new HashMap<>();
        for (SupplyItem item : items) {
            String room = (item.getRoom() != null && !item.getRoom().isEmpty()) ? item.getRoom() : "General";
            int issued = item.getTotalQuantity() - item.getAvailableQuantity();
            if (issued > 0) {
                roomUsage.put(room, roomUsage.getOrDefault(room, 0) + issued);
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        roomUsage.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(8)
                .forEach(e -> entries.add(new PieEntry(e.getValue(), e.getKey())));

        updatePieChart(mRoomUsagePieChart, entries, "Issued by Room");
    }

    private void renderBorrowChart(List<SupplyItem> items, List<BorrowRecord> borrows) {
        List<String> itemIds = items.stream().map(i -> i.getId().toString()).collect(Collectors.toList());
        List<BorrowRecord> filteredRecords = borrows.stream()
                .filter(r -> itemIds.contains(r.getItemId().toString()))
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
    }

    private void renderTopItemsChart(List<SupplyItem> items, List<Map.Entry<String, Integer>> topItems) {
        List<String> itemNames = items.stream().map(SupplyItem::getName).collect(Collectors.toList());
        List<Map.Entry<String, Integer>> filteredTop = topItems.stream()
                .filter(e -> itemNames.contains(e.getKey()))
                .limit(5)
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
    }

    private void updatePieChart(PieChart chart, List<PieEntry> entries, String label) {
        if (entries.isEmpty()) {
            chart.clear();
            chart.setNoDataText("No data available");
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
        if (mLoadingOverlay != null) mLoadingOverlay.setVisibility(View.VISIBLE);
        SupplyLab lab = SupplyLab.get(getActivity());
        lab.getItemsAsync(items -> {
            lab.getAllBorrowRecordsAsync(borrows -> {
                if (!isAdded()) return;
                StringBuilder csvInventory = new StringBuilder("Item ID,Item Name,Category,Item Type,Total Stock,Available,Borrowed,Used,Unit,Status,Date Added\n");
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
                       .append(item.getStatus()).append(",")
                       .append(sdf.format(item.getDateAdded())).append("\n");
                }
                saveFile("Inventory_Export.csv", csvInventory.toString().getBytes());

                StringBuilder csvBorrows = new StringBuilder("Item,Borrower,Quantity,Date Borrowed,Return Date,Status\n");
                for (BorrowRecord record : borrows) {
                    SupplyItem item = items.stream().filter(i -> i.getId().equals(record.getItemId())).findFirst().orElse(null);
                    csvBorrows.append(escapeCsv(item != null ? item.getName() : "Unknown")).append(",")
                            .append(escapeCsv(record.getBorrowerName())).append(",")
                            .append(record.getQuantity()).append(",")
                            .append(sdf.format(record.getDateBorrowed())).append(",")
                            .append(record.getActualReturnDate() != null ? sdf.format(record.getActualReturnDate()) : "N/A").append(",")
                            .append(record.getStatus()).append("\n");
                }
                saveFile("Borrowing_Logs.csv", csvBorrows.toString().getBytes());
                
                if (mLoadingOverlay != null) mLoadingOverlay.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Reports exported to Documents", Toast.LENGTH_LONG).show();
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
        if (mLoadingOverlay != null) mLoadingOverlay.setVisibility(View.VISIBLE);
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (!isAdded()) return;
            PdfDocument document = new PdfDocument();
            int pageNumber = 1;
            int yPos = 100;
            int itemsPerPage = 35;
            
            Paint paint = new Paint();
            paint.setTextSize(12f);
            
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();
            
            // Header
            Paint headerPaint = new Paint();
            headerPaint.setTextSize(18f);
            headerPaint.setFakeBoldText(true);
            canvas.drawText("School Supply Inventory Report", 50, 50, headerPaint);
            canvas.drawText("Generated: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()), 50, 75, paint);
            
            // Table Header
            paint.setFakeBoldText(true);
            canvas.drawText("Item Name", 50, yPos, paint);
            canvas.drawText("Category", 200, yPos, paint);
            canvas.drawText("Stock", 350, yPos, paint);
            canvas.drawText("Status", 450, yPos, paint);
            yPos += 20;
            canvas.drawLine(50, yPos - 10, 545, yPos - 10, paint);
            paint.setFakeBoldText(false);

            for (int i = 0; i < items.size(); i++) {
                if (i > 0 && i % itemsPerPage == 0) {
                    document.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPos = 50;
                }
                
                SupplyItem item = items.get(i);
                canvas.drawText(truncate(item.getName(), 25), 50, yPos, paint);
                canvas.drawText(truncate(item.getCategory(), 20), 200, yPos, paint);
                canvas.drawText(item.getAvailableQuantity() + "/" + item.getTotalQuantity(), 350, yPos, paint);
                canvas.drawText(item.getStatus(), 450, yPos, paint);
                yPos += 20;
            }
            
            document.finishPage(page);
            
            File file = new File(requireContext().getExternalFilesDir(null), "Inventory_Report.pdf");
            try {
                document.writeTo(new FileOutputStream(file));
                Toast.makeText(getActivity(), "PDF Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "Export failed", Toast.LENGTH_SHORT).show();
            }
            document.close();
            if (mLoadingOverlay != null) mLoadingOverlay.setVisibility(View.GONE);
        });
    }

    private String truncate(String text, int length) {
        if (text == null) return "";
        return text.length() <= length ? text : text.substring(0, length - 3) + "...";
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
