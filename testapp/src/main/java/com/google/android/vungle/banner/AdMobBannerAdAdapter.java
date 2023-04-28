package com.google.android.vungle.banner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdView;
import com.google.android.vungle.R;
import com.google.android.vungle.nativeads.MenuItem;
import java.util.List;

public class AdMobBannerAdAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  // A menu item view type.
  private static final int MENU_ITEM_VIEW_TYPE = 0;

  // The banner ad view type.
  private static final int BANNER_AD_VIEW_TYPE = 1;

  // An Activity's Context.
  private final Context context;

  // The list of banner ads and menu items.
  private final List<Object> recyclerViewItems;

  /**
   * For this example app, the recyclerViewItems list contains only {@link MenuItem} and
   * {@link AdView} types.
   */
  public AdMobBannerAdAdapter(Context context, List<Object> recyclerViewItems) {
    this.context = context;
    this.recyclerViewItems = recyclerViewItems;
  }

  /**
   * The {@link MenuItemViewHolder} class. Provides a reference to each view in the menu item view.
   */
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

  /**
   * The {@link AdViewHolder} class.
   */
  public class AdViewHolder extends RecyclerView.ViewHolder {

    AdViewHolder(View view) {
      super(view);
    }
  }

  @Override
  public int getItemCount() {
    return recyclerViewItems.size();
  }

  /**
   * Determines the view type for the given position.
   */
  @Override
  public int getItemViewType(int position) {
    return (position % BannerAdFeedActivity.ITEMS_PER_AD == 0) ? BANNER_AD_VIEW_TYPE
        : MENU_ITEM_VIEW_TYPE;
  }

  /**
   * Creates a new view for a menu item view or a banner ad view based on the viewType. This method
   * is invoked by the layout manager.
   */
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
    switch (viewType) {
      case MENU_ITEM_VIEW_TYPE:
        View menuItemLayoutView = LayoutInflater.from(viewGroup.getContext()).inflate(
            R.layout.menu_item_container, viewGroup, false);
        return new MenuItemViewHolder(menuItemLayoutView);
      case BANNER_AD_VIEW_TYPE:
        // fall through
      default:
        View bannerLayoutView = LayoutInflater.from(
            viewGroup.getContext()).inflate(R.layout.banner_ad_container,
            viewGroup, false);
        return new AdViewHolder(bannerLayoutView);
    }
  }

  /**
   * Replaces the content in the views that make up the menu item view and the banner ad view. This
   * method is invoked by the layout manager.
   */
  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
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
      case BANNER_AD_VIEW_TYPE:
        // fall through
      default:
        AdViewHolder bannerHolder = (AdViewHolder) holder;
        AdView adView = (AdView) recyclerViewItems.get(position);
        ViewGroup adCardView = (ViewGroup) bannerHolder.itemView;
        // The AdViewHolder recycled by the RecyclerView may be a different
        // instance than the one used previously for this position. Clear the
        // AdViewHolder of any subviews in case it has a different
        // AdView associated with it, and make sure the AdView for this position doesn't
        // already have a parent of a different recycled AdViewHolder.
        if (adCardView.getChildCount() > 0) {
          adCardView.removeAllViews();
        }
        if (adView.getParent() != null) {
          ((ViewGroup) adView.getParent()).removeView(adView);
        }

        // Add the banner ad to the ad view.
        adCardView.addView(adView);

        // Optional
        TextView tv = new TextView(context);
        tv.setText((String)adView.getTag());
        adCardView.addView(tv);
    }
  }

}
