package com.example.schoolsupplyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SupplyListFragment extends Fragment {

    private RecyclerView mSupplyRecyclerView;
    private SupplyAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory_list, container, false);

        mSupplyRecyclerView = (RecyclerView) view.findViewById(R.id.inventory_recycler_view);
        mSupplyRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        SupplyLab supplyLab = SupplyLab.get(getActivity());
        List<SupplyItem> items = supplyLab.getItems();

        if (mAdapter == null) {
            mAdapter = new SupplyAdapter(items);
            mSupplyRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    private class SupplyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mTitleTextView;
        private TextView mStatusTextView;
        private SupplyItem mItem;

        public SupplyHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_supply, parent, false));
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.item_title);
            mStatusTextView = (TextView) itemView.findViewById(R.id.item_status);
        }

        public void bind(SupplyItem item) {
            mItem = item;
            mTitleTextView.setText(mItem.getName());
            mStatusTextView.setText(mItem.isAvailable() ? "Available" : "Borrowed");
        }

        @Override
        public void onClick(View view) {
            Intent intent = SupplyPagerActivity.newIntent(getActivity(), mItem.getId());
            startActivity(intent);
        }
    }

    private class SupplyAdapter extends RecyclerView.Adapter<SupplyHolder> {
        private List<SupplyItem> mItems;

        public SupplyAdapter(List<SupplyItem> items) {
            mItems = items;
        }

        @NonNull
        @Override
        public SupplyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new SupplyHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull SupplyHolder holder, int position) {
            SupplyItem item = mItems.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }
}
