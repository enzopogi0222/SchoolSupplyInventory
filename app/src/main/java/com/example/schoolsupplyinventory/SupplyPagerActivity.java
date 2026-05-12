package com.example.schoolsupplyinventory;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import java.util.List;
import java.util.UUID;

public class SupplyPagerActivity extends AppCompatActivity {

    private static final String EXTRA_ITEM_ID = "com.schoolapp.item_id";

    private ViewPager2 mViewPager;
    private List<SupplyItem> mItems;
    private ProgressBar mProgressBar;

    public static Intent newIntent(Context packageContext, UUID itemId) {
        Intent intent = new Intent(packageContext, SupplyPagerActivity.class);
        intent.putExtra(EXTRA_ITEM_ID, itemId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supply_pager);

        final UUID itemId = (UUID) getIntent().getSerializableExtra(EXTRA_ITEM_ID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Supply Details");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mViewPager = findViewById(R.id.supply_view_pager);
        mProgressBar = findViewById(R.id.supply_pager_progress_bar);

        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        mViewPager.setVisibility(View.INVISIBLE);

        // Modern Depth Page Transformer
        mViewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            if (position < -1) {
                page.setAlpha(0f);
            } else if (position <= 1) {
                page.setAlpha(Math.max(0.4f, 1 - absPos));
                page.setTranslationX(page.getWidth() * -position);
                float scale = 0.8f + (1 - absPos) * 0.2f;
                page.setScaleX(scale);
                page.setScaleY(scale);
            } else {
                page.setAlpha(0f);
            }
        });

        SupplyLab.get(this).getItemsAsync(items -> {
            mItems = items;
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
            mViewPager.setVisibility(View.VISIBLE);
            
            mViewPager.setAdapter(new FragmentStateAdapter(this) {
                @NonNull
                @Override
                public Fragment createFragment(int position) {
                    SupplyItem item = mItems.get(position);
                    return SupplyDetailFragment.newInstance(item.getId());
                }

                @Override
                public int getItemCount() {
                    return mItems.size();
                }
            });

            for (int i = 0; i < mItems.size(); i++) {
                if (mItems.get(i).getId().equals(itemId)) {
                    mViewPager.setCurrentItem(i, false);
                    break;
                }
            }
        });
    }
}
