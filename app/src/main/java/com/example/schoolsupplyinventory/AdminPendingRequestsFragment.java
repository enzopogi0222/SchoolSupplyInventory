package com.example.schoolsupplyinventory;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AdminPendingRequestsFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_request_list, container, false);
        v.findViewById(R.id.staff_only_header).setVisibility(View.GONE);
        mRecyclerView = v.findViewById(R.id.request_recycler);
        mEmptyView = v.findViewById(R.id.request_empty);
        mEmptyView.setText("No pending requests.");
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        SupplyLab.get(requireContext()).getPendingRequestsAsync(list -> {
            if (!isAdded()) return;
            if (list == null) {
                list = new ArrayList<>();
            }
            mEmptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            mRecyclerView.setAdapter(new AdminRequestAdapter(list, this::confirmApprove, this::confirmReject));
        });
    }

    private void confirmApprove(SupplyRequest r) {
        String actionVerb = SupplyRequest.TYPE_RETURN.equals(r.getRequestType()) ? "verify return" : "approve request";
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Action")
                .setMessage("Do you want to " + actionVerb + " for " + r.getQuantity() + " unit(s) of " + r.getItemTitle() + "?")
                .setPositiveButton("Approve", (d, w) ->
                        SupplyLab.get(requireContext()).approveSupplyRequestAsync(r.getId(), ok -> {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), ok ? "Action approved" : "Could not process request", Toast.LENGTH_SHORT).show();
                                if (ok) reload();
                            });
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReject(SupplyRequest r) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reject request")
                .setMessage("Reject this request? Inventory will not change.")
                .setPositiveButton("Reject", (d, w) ->
                        SupplyLab.get(requireContext()).rejectSupplyRequestAsync(r.getId(), ok -> {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), ok ? "Request rejected" : "Could not update request", Toast.LENGTH_SHORT).show();
                                if (ok) reload();
                            });
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class AdminRequestAdapter extends RecyclerView.Adapter<AdminRequestAdapter.Holder> {

        private final List<SupplyRequest> mList;
        private final Consumer<SupplyRequest> mOnApprove;
        private final Consumer<SupplyRequest> mOnReject;

        AdminRequestAdapter(List<SupplyRequest> list, Consumer<SupplyRequest> onApprove, Consumer<SupplyRequest> onReject) {
            mList = list;
            mOnApprove = onApprove;
            mOnReject = onReject;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_supply_request, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            SupplyRequest r = mList.get(position);
            String title = r.getItemTitle() != null && !r.getItemTitle().isEmpty() ? r.getItemTitle() : "Item";
            h.title.setText(title + " — " + r.getRequesterName());
            
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
            if (r.getUnitId() != null && !r.getUnitId().isEmpty()) {
                meta += " · ID: " + r.getUnitId();
            }
            
            h.meta.setText(meta);
            String purpose = r.getPurpose() != null ? r.getPurpose() : "";
            h.purpose.setText(purpose.isEmpty() ? (SupplyRequest.TYPE_RETURN.equals(r.getRequestType()) ? "Item Return Verification" : "—") : purpose);

            h.status.setText("PENDING VERIFICATION");
            h.status.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.color_warning));

            h.actions.setVisibility(View.VISIBLE);
            h.approve.setOnClickListener(v -> mOnApprove.accept(r));
            h.reject.setOnClickListener(v -> mOnReject.accept(r));
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
            final View approve;
            final View reject;

            Holder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.request_item_title);
                meta = itemView.findViewById(R.id.request_meta);
                purpose = itemView.findViewById(R.id.request_purpose);
                status = itemView.findViewById(R.id.request_status_badge);
                actions = itemView.findViewById(R.id.request_admin_actions);
                approve = itemView.findViewById(R.id.request_btn_approve);
                reject = itemView.findViewById(R.id.request_btn_reject);
            }
        }
    }
}
