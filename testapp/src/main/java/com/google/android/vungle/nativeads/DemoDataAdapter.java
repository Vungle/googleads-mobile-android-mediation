package com.google.android.vungle.nativeads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.vungle.R;
import java.util.List;

public class DemoDataAdapter extends
    RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private final List<MenuItem> mDatas;

  public DemoDataAdapter(List<MenuItem> datas) {
    mDatas = datas;
  }

  @Override
  @NonNull
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
    View menuItemLayoutView = LayoutInflater.from(viewGroup.getContext()).inflate(
        R.layout.menu_item_container, viewGroup, false);
    return new MenuItemViewHolder(menuItemLayoutView);
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
    MenuItemViewHolder menuItemHolder = (MenuItemViewHolder) viewHolder;
    MenuItem menuItem = mDatas.get(position);

    menuItemHolder.menuItemName.setText(menuItem.getName());
    menuItemHolder.menuItemPrice.setText(menuItem.getPrice());
    menuItemHolder.menuItemCategory.setText(menuItem.getCategory());
    menuItemHolder.menuItemDescription.setText(menuItem.getDescription());
  }

  @Override
  public int getItemCount() {
    return mDatas.size();
  }

  private static class MenuItemViewHolder extends RecyclerView.ViewHolder {

    private TextView menuItemName;
    private TextView menuItemDescription;
    private TextView menuItemPrice;
    private TextView menuItemCategory;
    private ImageView menuItemImage;

    private View view;

    public Context getContext() {
      return view.getContext();
    }

    MenuItemViewHolder(View view) {
      super(view);
      this.view = view;
      menuItemImage = view.findViewById(R.id.menu_item_image);
      menuItemName = view.findViewById(R.id.menu_item_name);
      menuItemPrice = view.findViewById(R.id.menu_item_price);
      menuItemCategory = view.findViewById(R.id.menu_item_category);
      menuItemDescription = view.findViewById(R.id.menu_item_description);
    }
  }

}
