package com.example.schoolsupplyinventory;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SupplyListFragment extends Fragment {

    private RecyclerView mSupplyRecyclerView;
    private SupplyAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory_list, container, false);

        mSupplyRecyclerView = (RecyclerView) view.findViewById(R.id.inventory_recycler_view);
        mSupplyRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        setupItemTouchHelper();
        updateUI();

        return view;
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                final SupplyItem itemToDelete = mAdapter.mItems.get(position);

                new AlertDialog.Builder(getActivity())
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to delete this item?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SupplyLab.get(getActivity()).deleteSupply(itemToDelete);
                                updateUI();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAdapter.notifyItemChanged(position);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mAdapter.notifyItemChanged(position);
                            }
                        })
                        .show();
            }
        };

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mSupplyRecyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_supply_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.new_supply) {
            SupplyItem item_new = new SupplyItem();
            SupplyLab.get(getActivity()).addSupply(item_new);

            Intent intent = SupplyPagerActivity.newIntent(getActivity(), item_new.getId());
            startActivity(intent);
            return true;
        } else if (itemId == R.id.show_subtitle) {
            updateSubtitle();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void updateSubtitle() {
        SupplyLab supplyLab = SupplyLab.get(getActivity());
        int count = supplyLab.getItems().size();
        String subtitle = getString(R.string.subtitle_format, count);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    private void updateUI() {
        SupplyLab supplyLab = SupplyLab.get(getActivity());
        List<SupplyItem> items = supplyLab.getItems();

        if (mAdapter == null) {
            mAdapter = new SupplyAdapter(items);
            mSupplyRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setItems(items);
            mAdapter.notifyDataSetChanged();
        }
    }

    private class SupplyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mTitleTextView;
        private TextView mBrandTextView;
        private TextView mCategoryTextView;
        private TextView mStatusTextView;
        private SupplyItem mItem;

        public SupplyHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_supply, parent, false));
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.item_title);
            mBrandTextView = (TextView) itemView.findViewById(R.id.item_brand);
            mCategoryTextView = (TextView) itemView.findViewById(R.id.item_category);
            mStatusTextView = (TextView) itemView.findViewById(R.id.item_status);
        }

        public void bind(SupplyItem item) {
            mItem = item;
            mTitleTextView.setText(mItem.getName() != null ? mItem.getName() : "Unnamed Item");
            mBrandTextView.setText("Brand: " + (mItem.getBrand() != null ? mItem.getBrand() : "N/A"));
            mCategoryTextView.setText("Category: " + mItem.getCategory().name());
            mStatusTextView.setText(mItem.isBorrowed() ? "Borrowed" : "Available");
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

        public void setItems(List<SupplyItem> items) {
            mItems = items;
        }
    }
}
