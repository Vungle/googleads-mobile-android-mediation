package com.google.android.vungle.nativeads.advanced;

import static com.google.android.vungle.nativeads.advanced.NativeAdFeedAdvancedActivity.ITEMS_PER_AD;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.android.vungle.R;
import com.google.android.vungle.nativeads.MenuItem;
import java.util.List;

public class NativeAdAdvancedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  // A menu item view type.
  private static final int MENU_ITEM_VIEW_TYPE = 0;

  // The native ad view type.
  private static final int NATIVE_AD_VIEW_TYPE = 1;

  // An Activity's Context.
  private final Context context;

  // The list of native ads and menu items.
  private final List<Object> recyclerViewItems;

  public NativeAdAdvancedAdapter(Context context, List<Object> recyclerViewItems) {
    this.context = context;
    this.recyclerViewItems = recyclerViewItems;
  }

  public class MenuItemViewHolder extends RecyclerView.ViewHolder {

    private TextView menuItemName;
    private TextView menuItemDescription;
    private TextView menuItemPrice;
    private TextView menuItemCategory;

    MenuItemViewHolder(View view) {
      super(view);
      menuItemName = view.findViewById(R.id.menu_item_name);
      menuItemPrice = view.findViewById(R.id.menu_item_price);
      menuItemCategory = view.findViewById(R.id.menu_item_category);
      menuItemDescription = view.findViewById(R.id.menu_item_description);
    }
  }

  public class AdViewHolder extends RecyclerView.ViewHolder {

    AdViewHolder(View view) {
      super(view);
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    switch (viewType) {
      case MENU_ITEM_VIEW_TYPE:
        View menuItemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
            R.layout.menu_item_container, parent, false);
        return new MenuItemViewHolder(menuItemLayoutView);
      case NATIVE_AD_VIEW_TYPE:
        // fall through
      default:
        View nativeLayoutView = LayoutInflater.from(
            parent.getContext()).inflate(R.layout.native_ad_container,
            parent, false);
        return new AdViewHolder(nativeLayoutView);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    int viewType = getItemViewType(position);
    switch (viewType) {
      case MENU_ITEM_VIEW_TYPE:
        MenuItemViewHolder menuItemHolder = (MenuItemViewHolder) holder;
        MenuItem menuItem = (MenuItem) recyclerViewItems.get(position);

        // Add the menu item details to the menu item view.
        menuItemHolder.menuItemName.setText(menuItem.getName());
        menuItemHolder.menuItemPrice.setText(menuItem.getPrice());
        menuItemHolder.menuItemCategory.setText(menuItem.getCategory());
        menuItemHolder.menuItemDescription.setText(menuItem.getDescription());
        break;
      case NATIVE_AD_VIEW_TYPE:
        // fall through
      default:
        AdViewHolder nativeAdHolder = (AdViewHolder) holder;
        NativeAdTemplate nativeAdTemplate = (NativeAdTemplate) recyclerViewItems.get(position);
        ViewGroup adCardView = (ViewGroup) nativeAdHolder.itemView;

        if (adCardView.getChildCount() > 0) {
          adCardView.removeAllViews();
        }

        // Add the native ad to the ad view.
        nativeAdTemplate.populateNativeAdView(adCardView);

        // Optional
        TextView tv = new TextView(context);
        tv.setText((String)nativeAdTemplate.getTag());
        adCardView.addView(tv);
    }
  }

  @Override
  public int getItemCount() {
    return recyclerViewItems.size();
  }

  @Override
  public int getItemViewType(int position) {
    return (position % ITEMS_PER_AD == 0) ? NATIVE_AD_VIEW_TYPE
        : MENU_ITEM_VIEW_TYPE;
  }

}
