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
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SupplyListFragment extends Fragment {

    private RecyclerView mSupplyRecyclerView;
    private SupplyAdapter mAdapter;
    private ExtendedFloatingActionButton mAddSupplyFab;
    private TextInputEditText mSearchEditText;
    private ChipGroup mFilterChipGroup;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ShimmerFrameLayout mShimmerViewContainer;
    private View mEmptyStateView;
    private List<SupplyItem> mAllItems;
    
    private LruCache<String, Bitmap> mThumbnailCache;
    private final ExecutorService mImageExecutor = Executors.newFixedThreadPool(2);
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory_list, container, false);

        mSupplyRecyclerView = view.findViewById(R.id.inventory_recycler_view);
        mSupplyRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mSearchEditText = view.findViewById(R.id.search_edit_text);
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mFilterChipGroup = view.findViewById(R.id.filter_chip_group);
        mFilterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_purple);
        mSwipeRefreshLayout.setOnRefreshListener(this::updateUI);

        mShimmerViewContainer = view.findViewById(R.id.shimmer_view_container);
        mEmptyStateView = view.findViewById(R.id.empty_state_view);

        mAddSupplyFab = view.findViewById(R.id.add_supply_fab);
        mAddSupplyFab.setOnClickListener(v -> createNewSupply());

        mSupplyRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) mAddSupplyFab.shrink();
                else if (dy < 0) mAddSupplyFab.extend();
            }
        });

        setupItemTouchHelper();
        updateUI();

        return view;
    }

    private void applyFilters() {
        if (mAllItems == null) return;

        String query = mSearchEditText.getText().toString().toLowerCase();
        int checkedId = mFilterChipGroup.getCheckedChipId();

        List<SupplyItem> filteredList = mAllItems.stream()
                .filter(item -> {
                    String name = item.getName() != null ? item.getName().toLowerCase() : "";
                    String brand = item.getBrand() != null ? item.getBrand().toLowerCase() : "";
                    String category = item.getCategory() != null ? item.getCategory().toLowerCase() : "";
                    String room = item.getRoom() != null ? item.getRoom().toLowerCase() : "";
                    String tag = item.getPropertyTag() != null ? item.getPropertyTag().toLowerCase() : "";
                    
                    boolean matchesQuery = name.contains(query) || 
                                          brand.contains(query) || 
                                          category.contains(query) || 
                                          room.contains(query) || 
                                          tag.contains(query);

                    boolean matchesChip = true;
                    if (checkedId == R.id.chip_available) {
                        matchesChip = item.getQuantity() > 0;
                    } else if (checkedId == R.id.chip_borrowed) {
                        matchesChip = item.isBorrowed();
                    } else if (checkedId == R.id.chip_low_stock) {
                        matchesChip = item.getQuantity() > 0 && item.getQuantity() <= 5;
                    }

                    return matchesQuery && matchesChip;
                })
                .collect(Collectors.toList());

        if (mAdapter != null) {
            mAdapter.setItems(filteredList);
            mAdapter.notifyDataSetChanged();
        }

        mEmptyStateView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void createNewSupply() {
        Intent intent = SupplyPagerActivity.newIntent(getActivity(), null);
        startActivity(intent);
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
                if (mAdapter == null || position >= mAdapter.mItems.size()) return;
                
                final SupplyItem itemToDelete = mAdapter.mItems.get(position);

                new AlertDialog.Builder(getActivity(), R.style.Base_Theme_SchoolSupplyInventory)
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to delete " + itemToDelete.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            SupplyLab.get(getActivity()).deleteSupply(itemToDelete);
                            Snackbar.make(mSupplyRecyclerView, itemToDelete.getName() + " deleted", Snackbar.LENGTH_LONG)
                                    .setAction("UNDO", v -> {
                                        SupplyLab.get(getActivity()).addSupply(itemToDelete);
                                        updateUI();
                                    }).show();
                            updateUI();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> mAdapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> mAdapter.notifyItemChanged(position))
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

    private void updateUI() {
        if (mAllItems == null || mAllItems.isEmpty()) {
            mShimmerViewContainer.setVisibility(View.VISIBLE);
            mShimmerViewContainer.startShimmer();
            mSupplyRecyclerView.setVisibility(View.GONE);
        }

        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (!isAdded()) return;
            
            mShimmerViewContainer.stopShimmer();
            mShimmerViewContainer.setVisibility(View.GONE);
            mSupplyRecyclerView.setVisibility(View.VISIBLE);

            mAllItems = items;
            if (mAdapter == null) {
                mAdapter = new SupplyAdapter(new ArrayList<>(mAllItems));
                mSupplyRecyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.setItems(new ArrayList<>(mAllItems));
                mAdapter.notifyDataSetChanged();
            }
            applyFilters();
            if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(false);
        });
    }

    private class SupplyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mTitleTextView, mBrandTextView, mCategoryTextView, mRoomTextView, mStatusTextView, mQuantityTextView;
        private ImageView mPhotoThumbnail;
        private SupplyItem mItem;
        private String mCurrentPhotoPath;

        public SupplyHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_supply, parent, false));
            itemView.setOnClickListener(this);
            mTitleTextView = itemView.findViewById(R.id.item_title);
            mBrandTextView = itemView.findViewById(R.id.item_brand);
            mCategoryTextView = itemView.findViewById(R.id.item_category);
            mRoomTextView = itemView.findViewById(R.id.item_room);
            mStatusTextView = itemView.findViewById(R.id.item_status);
            mQuantityTextView = itemView.findViewById(R.id.item_quantity);
            mPhotoThumbnail = itemView.findViewById(R.id.item_photo_thumbnail);
        }

        public void bind(SupplyItem item) {
            mItem = item;
            mTitleTextView.setText(mItem.getName() != null && !mItem.getName().isEmpty() ? mItem.getName() : "Unnamed Item");
            mBrandTextView.setText(mItem.getBrand() != null && !mItem.getBrand().isEmpty() ? mItem.getBrand() : "No Brand");
            mCategoryTextView.setText(mItem.getCategory() != null ? mItem.getCategory() : "");
            mRoomTextView.setText("• " + (mItem.getRoom() != null ? mItem.getRoom() : "No Room"));
            mQuantityTextView.setText("In Stock: " + mItem.getQuantity());
            
            if (mItem.getQuantity() > 0) {
                mStatusTextView.setText("AVAILABLE");
                mStatusTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.color_success));
            } else {
                mStatusTextView.setText("OUT OF STOCK");
                mStatusTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.color_error));
            }

            loadThumbnail();
        }

        private void loadThumbnail() {
            File photoFile = SupplyLab.get(getActivity()).getPhotoFile(mItem);
            if (photoFile == null || !photoFile.exists()) {
                mPhotoThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                mCurrentPhotoPath = null;
                return;
            }

            String path = photoFile.getPath();
            mCurrentPhotoPath = path;
            Bitmap cached = mThumbnailCache.get(path);
            
            if (cached != null) {
                mPhotoThumbnail.setImageBitmap(cached);
            } else {
                mPhotoThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                mImageExecutor.execute(() -> {
                    final Bitmap bitmap = PictureUtils.getScaledBitmap(path, 160, 160);
                    if (bitmap != null) {
                        mThumbnailCache.put(path, bitmap);
                        mMainHandler.post(() -> {
                            if (path.equals(mCurrentPhotoPath)) {
                                mPhotoThumbnail.setImageBitmap(bitmap);
                            }
                        });
                    }
                });
            }
        }

        @Override
        public void onClick(View view) {
            Intent intent = SupplyPagerActivity.newIntent(getActivity(), mItem.getId());
            startActivity(intent);
        }
    }

    private class SupplyAdapter extends RecyclerView.Adapter<SupplyHolder> {
        private List<SupplyItem> mItems;
        public SupplyAdapter(List<SupplyItem> items) { mItems = items; }
        @NonNull @Override
        public SupplyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SupplyHolder(LayoutInflater.from(getActivity()), parent);
        }
        @Override
        public void onBindViewHolder(@NonNull SupplyHolder holder, int position) {
            holder.bind(mItems.get(position));
        }
        @Override
        public int getItemCount() { return mItems.size(); }
        public void setItems(List<SupplyItem> items) { mItems = items; }
    }
}
