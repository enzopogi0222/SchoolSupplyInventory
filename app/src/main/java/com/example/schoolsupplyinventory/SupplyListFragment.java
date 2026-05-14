package com.example.schoolsupplyinventory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SupplyListFragment extends Fragment {

    private RecyclerView mSupplyRecyclerView;
    private InventoryAdapter mAdapter;
    private ExtendedFloatingActionButton mAddSupplyFab;
    private TextInputEditText mSearchEditText;
    private ChipGroup mMainFilterChipGroup, mSubFilterChipGroup;
    private HorizontalScrollView mSubFilterScroll;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ShimmerFrameLayout mShimmerViewContainer;
    private View mEmptyStateView;
    private List<SupplyItem> mAllItems;
    private String mSelectedCategory = null;
    private OnBackPressedCallback mOnBackPressedCallback;
    
    private LruCache<String, Bitmap> mThumbnailCache;
    private final ExecutorService mImageExecutor = Executors.newFixedThreadPool(2);
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_ITEM = 1;

    private static class Category {
        String name;
        int count;
        Category(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        mThumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        mOnBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (mSelectedCategory != null) {
                    mSelectedCategory = null;
                    applyFilters();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, mOnBackPressedCallback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory_list, container, false);

        mSupplyRecyclerView = view.findViewById(R.id.inventory_recycler_view);
        mSupplyRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mSearchEditText = view.findViewById(R.id.search_edit_text);
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        mMainFilterChipGroup = view.findViewById(R.id.main_filter_chip_group);
        mSubFilterChipGroup = view.findViewById(R.id.sub_filter_chip_group);
        mSubFilterScroll = view.findViewById(R.id.sub_filter_scroll);

        mMainFilterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            updateSubFilterVisibility();
            applyFilters();
        });

        mSubFilterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this::updateUI);

        mShimmerViewContainer = view.findViewById(R.id.shimmer_view_container);
        mEmptyStateView = view.findViewById(R.id.empty_state_view);

        mAddSupplyFab = view.findViewById(R.id.add_supply_fab);
        mAddSupplyFab.setOnClickListener(v -> startActivity(SupplyPagerActivity.newIntent(getActivity(), null)));

        setupItemTouchHelper();
        updateUI();

        return view;
    }

    private void updateSubFilterVisibility() {
        int checkedId = mMainFilterChipGroup.getCheckedChipId();
        mSubFilterChipGroup.clearCheck();
        
        if (checkedId == R.id.chip_main_all) {
            mSubFilterScroll.setVisibility(View.GONE);
        } else if (checkedId == R.id.chip_main_consumable) {
            mSubFilterScroll.setVisibility(View.VISIBLE);
            mSubFilterChipGroup.findViewById(R.id.chip_sub_available).setVisibility(View.VISIBLE);
            mSubFilterChipGroup.findViewById(R.id.chip_sub_used).setVisibility(View.VISIBLE);
            mSubFilterChipGroup.findViewById(R.id.chip_sub_low_stock).setVisibility(View.VISIBLE);
            mSubFilterChipGroup.findViewById(R.id.chip_sub_borrowed).setVisibility(View.GONE);
        } else if (checkedId == R.id.chip_main_borrowable) {
            mSubFilterScroll.setVisibility(View.VISIBLE);
            mSubFilterChipGroup.findViewById(R.id.chip_sub_available).setVisibility(View.VISIBLE);
            mSubFilterChipGroup.findViewById(R.id.chip_sub_borrowed).setVisibility(View.VISIBLE);
            mSubFilterChipGroup.findViewById(R.id.chip_sub_used).setVisibility(View.GONE);
            mSubFilterChipGroup.findViewById(R.id.chip_sub_low_stock).setVisibility(View.GONE);
        }
    }

    private void applyFilters() {
        if (mAllItems == null) return;

        String query = mSearchEditText.getText().toString().toLowerCase();
        int mainCheckedId = mMainFilterChipGroup.getCheckedChipId();
        int subCheckedId = mSubFilterChipGroup.getCheckedChipId();

        List<SupplyItem> filteredList = mAllItems.stream()
                .filter(item -> {
                    String name = item.getName() != null ? item.getName().toLowerCase() : "";
                    String category = item.getCategory() != null ? item.getCategory().toLowerCase() : "";
                    
                    boolean matchesQuery = name.contains(query) || category.contains(query);

                    boolean matchesMain = true;
                    if (mainCheckedId == R.id.chip_main_consumable) {
                        matchesMain = SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(item.getItemType());
                    } else if (mainCheckedId == R.id.chip_main_borrowable) {
                        matchesMain = SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(item.getItemType());
                    }

                    boolean matchesSub = true;
                    if (subCheckedId != View.NO_ID) {
                        if (subCheckedId == R.id.chip_sub_available) {
                            matchesSub = item.getAvailableQuantity() > 0;
                        } else if (subCheckedId == R.id.chip_sub_borrowed) {
                            matchesSub = item.getBorrowedQuantity() > 0;
                        } else if (subCheckedId == R.id.chip_sub_used) {
                            matchesSub = item.getUsedQuantity() > 0;
                        } else if (subCheckedId == R.id.chip_sub_low_stock) {
                            matchesSub = item.getAvailableQuantity() <= 5 && item.getAvailableQuantity() > 0;
                        }
                    }

                    return matchesQuery && matchesMain && matchesSub;
                })
                .collect(Collectors.toList());

        if (mAdapter != null) {
            if (mSelectedCategory == null) {
                Map<String, List<SupplyItem>> grouped = filteredList.stream()
                        .collect(Collectors.groupingBy(item -> item.getCategory() != null ? item.getCategory() : "OTHER"));
                List<Object> displayList = grouped.entrySet().stream()
                        .map(entry -> new Category(entry.getKey(), entry.getValue().size()))
                        .sorted((c1, c2) -> c1.name.compareToIgnoreCase(c2.name))
                        .collect(Collectors.toList());
                mAdapter.setDisplayItems(displayList);
                mOnBackPressedCallback.setEnabled(false);
            } else {
                List<Object> displayList = filteredList.stream()
                        .filter(item -> mSelectedCategory.equals(item.getCategory()))
                        .collect(Collectors.toList());
                mAdapter.setDisplayItems(displayList);
                mOnBackPressedCallback.setEnabled(true);
            }
            mAdapter.notifyDataSetChanged();
            mEmptyStateView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (viewHolder instanceof SupplyHolder) {
                    SupplyItem item = ((SupplyHolder) viewHolder).mItem;
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Delete Item")
                            .setMessage("Delete " + item.getName() + "?")
                            .setPositiveButton("Delete", (d, w) -> {
                                SupplyLab.get(getActivity()).deleteSupply(item);
                                updateUI();
                            })
                            .setNegativeButton("Cancel", (d, w) -> mAdapter.notifyItemChanged(viewHolder.getAdapterPosition()))
                            .show();
                } else mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
            }
            @Override public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                if (vh instanceof CategoryHolder) return 0;
                return super.getSwipeDirs(rv, vh);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(mSupplyRecyclerView);
    }

    private void updateUI() {
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (!isAdded()) return;
            mShimmerViewContainer.setVisibility(View.GONE);
            mSupplyRecyclerView.setVisibility(View.VISIBLE);
            mAllItems = items;
            if (mAdapter == null) {
                mAdapter = new InventoryAdapter(new ArrayList<>());
                mSupplyRecyclerView.setAdapter(mAdapter);
            }
            applyFilters();
            mSwipeRefreshLayout.setRefreshing(false);
        });
    }

    private class CategoryHolder extends RecyclerView.ViewHolder {
        private TextView mNameTextView, mCountTextView;
        public CategoryHolder(View v) {
            super(v);
            mNameTextView = v.findViewById(R.id.category_name);
            mCountTextView = v.findViewById(R.id.category_count);
            v.setOnClickListener(view -> {
                mSelectedCategory = ((Category) mAdapter.mDisplayItems.get(getAdapterPosition())).name;
                applyFilters();
            });
        }
        public void bind(Category c) {
            mNameTextView.setText(c.name);
            mCountTextView.setText(c.count + " items");
        }
    }

    private class SupplyHolder extends RecyclerView.ViewHolder {
        private TextView mTitle, mCategory, mStatus, mQuantity;
        private ImageView mThumbnail;
        private SupplyItem mItem;
        public SupplyHolder(View v) {
            super(v);
            mTitle = v.findViewById(R.id.item_title);
            mCategory = v.findViewById(R.id.item_category);
            mStatus = v.findViewById(R.id.item_status);
            mQuantity = v.findViewById(R.id.item_quantity);
            mThumbnail = v.findViewById(R.id.item_photo_thumbnail);
            v.setOnClickListener(view -> startActivity(SupplyPagerActivity.newIntent(getActivity(), mItem.getId())));
        }
        public void bind(SupplyItem item) {
            mItem = item;
            mTitle.setText(item.getName());
            mCategory.setText(item.getCategory() + " • " + item.getItemType().toUpperCase());
            mQuantity.setText("Available: " + item.getAvailableQuantity() + " / Total: " + item.getTotalQuantity() + " " + item.getUnit());
            
            mStatus.setText(item.getStatus().toUpperCase());
            int color = R.color.color_success;
            if (item.getAvailableQuantity() == 0) {
                mStatus.setText("OUT OF STOCK");
                color = R.color.color_error;
            } else if (item.getAvailableQuantity() <= 5) {
                mStatus.setText("LOW STOCK");
                color = R.color.color_warning;
            } else if (SupplyItem.TYPE_BORROWABLE.equals(item.getItemType()) && item.getBorrowedQuantity() > 0) {
                mStatus.setText("ON LOAN");
                color = R.color.color_info;
            }
            mStatus.setTextColor(ContextCompat.getColor(getActivity(), color));
        }
    }

    private class InventoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Object> mDisplayItems;
        public InventoryAdapter(List<Object> items) { mDisplayItems = items; }
        @Override public int getItemViewType(int pos) { return mDisplayItems.get(pos) instanceof Category ? TYPE_CATEGORY : TYPE_ITEM; }
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            return viewType == TYPE_CATEGORY ? new CategoryHolder(inflater.inflate(R.layout.list_item_category, parent, false))
                                             : new SupplyHolder(inflater.inflate(R.layout.list_item_supply, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            if (holder instanceof CategoryHolder) ((CategoryHolder) holder).bind((Category) mDisplayItems.get(pos));
            else ((SupplyHolder) holder).bind((SupplyItem) mDisplayItems.get(pos));
        }
        @Override public int getItemCount() { return mDisplayItems.size(); }
        public void setDisplayItems(List<Object> items) { mDisplayItems = items; }
    }
}
