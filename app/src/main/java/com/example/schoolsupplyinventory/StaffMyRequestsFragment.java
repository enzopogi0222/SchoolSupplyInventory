package com.example.schoolsupplyinventory;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class StaffMyRequestsFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private StaffRequestRowAdapter mAdapter;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_request_list, container, false);
        mRecyclerView = v.findViewById(R.id.request_recycler);
        mEmptyView = v.findViewById(R.id.request_empty);
        mEmptyView.setText("No requests yet.\nTap New request to fill out the form.");
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        v.findViewById(R.id.btn_staff_new_request).setOnClickListener(b -> showNewRequestBottomSheet());
        
        View clearBtn = v.findViewById(R.id.btn_clear_requests);
        if (clearBtn != null) {
            clearBtn.setOnClickListener(v1 -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Clear Request History")
                        .setMessage("Are you sure you want to clear all your request history?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            SupplyLab lab = SupplyLab.get(requireContext());
                            lab.clearRequestsForUserAsync(lab.getCurrentUser(), result -> reload());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        setupSwipeToDelete();

        return v;
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (mAdapter != null) {
                    SupplyRequest request = mAdapter.mList.get(position);
                    SupplyLab.get(getActivity()).deleteRequestAsync(request.getId(), result -> {
                        mAdapter.mList.remove(position);
                        mAdapter.notifyItemRemoved(position);
                        if (mAdapter.mList.isEmpty()) mEmptyView.setVisibility(View.VISIBLE);
                    });
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(mRecyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        SupplyLab lab = SupplyLab.get(requireContext());
        lab.getSupplyRequestsForRequesterAsync(lab.getCurrentUser(), this::bindList);
    }

    private void bindList(List<SupplyRequest> list) {
        if (!isAdded()) return;
        if (list == null) {
            list = new ArrayList<>();
        }
        mEmptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        mAdapter = new StaffRequestRowAdapter(list);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void showNewRequestBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_staff_new_request, null);
        dialog.setContentView(sheet);

        MaterialAutoCompleteTextView itemSearch = sheet.findViewById(R.id.staff_req_item_autocomplete);
        TextView itemHint = sheet.findViewById(R.id.staff_req_item_hint);
        TextInputEditText qtyEdit = sheet.findViewById(R.id.staff_req_quantity);
        TextInputEditText purposeEdit = sheet.findViewById(R.id.staff_req_purpose);
        TextInputLayout unitLayout = sheet.findViewById(R.id.staff_req_unit_layout);
        TextInputEditText unitEdit = sheet.findViewById(R.id.staff_req_unit_id);
        MaterialButton dueBtn = sheet.findViewById(R.id.staff_req_due_date_btn);
        MaterialButton submitBtn = sheet.findViewById(R.id.staff_req_submit_btn);

        final SupplyItem[] selected = new SupplyItem[1];
        final Calendar[] dueHolder = new Calendar[]{Calendar.getInstance()};
        dueHolder[0].add(Calendar.DAY_OF_YEAR, 7);
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        dueBtn.setText("Return by: " + fmt.format(dueHolder[0].getTime()));
        qtyEdit.setText("1");

        SupplyLab.get(requireContext()).getItemsAsync(items -> {
            if (!isAdded()) return;
            List<SupplyItem> sorted = items.stream()
                    .sorted(Comparator.comparing(SupplyItem::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            List<String> labels = sorted.stream()
                    .map(i -> i.getName() + " (" + i.getAvailableQuantity() + " avail · " + i.getItemType() + ")")
                    .collect(Collectors.toList());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, labels);
            itemSearch.setAdapter(adapter);
            itemSearch.setThreshold(0);
            itemSearch.setOnClickListener(v -> itemSearch.showDropDown());
            itemSearch.setOnItemClickListener((parent, v, position, id) -> {
                selected[0] = sorted.get(position);
                SupplyItem it = selected[0];
                boolean borrow = SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(it.getItemType());
                itemHint.setText(it.getName() + " — " + it.getItemType() + " — Available: " + it.getAvailableQuantity());
                if (borrow) {
                    dueBtn.setVisibility(View.VISIBLE);
                    unitLayout.setVisibility(it.getUnitIdentifiersList().isEmpty() ? View.GONE : View.VISIBLE);
                } else {
                    dueBtn.setVisibility(View.GONE);
                    unitLayout.setVisibility(View.GONE);
                }
            });
        });

        dueBtn.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Expected return date")
                    .setSelection(dueHolder[0].getTimeInMillis())
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                dueHolder[0].setTimeInMillis(sel);
                dueBtn.setText("Return by: " + fmt.format(dueHolder[0].getTime()));
            });
            picker.show(getParentFragmentManager(), "STAFF_SHEET_RETURN");
        });

        submitBtn.setOnClickListener(v -> {
            if (selected[0] == null) {
                Toast.makeText(requireContext(), "Select an item first", Toast.LENGTH_SHORT).show();
                return;
            }
            String qtyStr = qtyEdit.getText() != null ? qtyEdit.getText().toString().trim() : "";
            String purpose = purposeEdit.getText() != null ? purposeEdit.getText().toString().trim() : "";
            if (qtyStr.isEmpty() || purpose.isEmpty()) {
                Toast.makeText(requireContext(), "Enter quantity and purpose", Toast.LENGTH_SHORT).show();
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            SupplyItem it = selected[0];
            if (qty <= 0 || qty > it.getAvailableQuantity()) {
                Toast.makeText(requireContext(), "Invalid quantity for this item", Toast.LENGTH_SHORT).show();
                return;
            }
            SupplyRequest req = new SupplyRequest();
            req.setItemId(it.getId());
            req.setItemTitle(it.getName());
            req.setRequesterName(SupplyLab.get(requireContext()).getCurrentUser());
            req.setQuantity(qty);
            req.setPurpose(purpose);
            if (SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(it.getItemType())) {
                req.setRequestType(SupplyRequest.TYPE_BORROW);
                req.setExpectedReturnDate(dueHolder[0].getTime());
                String uid = unitEdit.getText() != null ? unitEdit.getText().toString().trim() : "";
                if (!uid.isEmpty()) {
                    req.setUnitId(uid);
                }
            } else {
                req.setRequestType(SupplyRequest.TYPE_CONSUME);
            }
            SupplyLab.get(requireContext()).submitSupplyRequestAsync(req, ok -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(requireContext(), "Request submitted (pending approval)", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        reload();
                    } else {
                        Toast.makeText(requireContext(), "Could not submit (check stock and item type)", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private static class StaffRequestRowAdapter extends RecyclerView.Adapter<StaffRequestRowAdapter.Holder> {

        private final List<SupplyRequest> mList;

        StaffRequestRowAdapter(List<SupplyRequest> list) {
            mList = list;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_supply_request, parent, false);
            return new Holder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            SupplyRequest r = mList.get(position);
            String title = r.getItemTitle() != null && !r.getItemTitle().isEmpty() ? r.getItemTitle() : "Item";
            h.title.setText(title);
            
            String typeLabel;
            if (SupplyRequest.TYPE_RETURN.equals(r.getRequestType())) {
                typeLabel = "Return";
            } else if (SupplyRequest.TYPE_BORROW.equals(r.getRequestType())) {
                typeLabel = "Borrow";
            } else {
                typeLabel = "Consume";
            }
            
            String meta = typeLabel + " · Qty " + r.getQuantity() + " · " + DATE_FMT.format(r.getDateRequested());
            if (SupplyRequest.TYPE_BORROW.equals(r.getRequestType()) && r.getExpectedReturnDate() != null) {
                meta += " · Due " + DATE_FMT.format(r.getExpectedReturnDate());
            }
            h.meta.setText(meta);
            String purpose = r.getPurpose() != null ? r.getPurpose() : "";
            h.purpose.setText(purpose.isEmpty() ? (SupplyRequest.TYPE_RETURN.equals(r.getRequestType()) ? "Item Return Verification" : "—") : purpose);

            String status = r.getStatus() != null ? r.getStatus() : "";
            
            if (SupplyRequest.TYPE_RETURN.equals(r.getRequestType()) && SupplyRequest.STATUS_APPROVED.equals(status)) {
                h.status.setText("RETURNED APPROVED");
            } else {
                h.status.setText(status);
            }

            int colorRes = R.color.color_info;
            if (SupplyRequest.STATUS_PENDING.equals(status)) {
                colorRes = R.color.color_warning;
            } else if (SupplyRequest.STATUS_APPROVED.equals(status)) {
                colorRes = R.color.color_success;
            } else if (SupplyRequest.STATUS_REJECTED.equals(status)) {
                colorRes = R.color.color_error;
            }
            h.status.setTextColor(ContextCompat.getColor(h.itemView.getContext(), colorRes));
            h.actions.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView meta;
            final TextView purpose;
            final TextView status;
            final View actions;

            Holder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.request_item_title);
                meta = itemView.findViewById(R.id.request_meta);
                purpose = itemView.findViewById(R.id.request_purpose);
                status = itemView.findViewById(R.id.request_status_badge);
                actions = itemView.findViewById(R.id.request_admin_actions);
            }
        }
    }
}
