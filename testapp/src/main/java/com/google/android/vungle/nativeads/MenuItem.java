package com.google.android.vungle.nativeads;

import android.content.Context;
import android.util.Log;
import com.google.android.vungle.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Sample data in RecyclerView for Banner and Native ad demo
 */
public class MenuItem {

  private final String name;
  private final String description;
  private final String price;
  private final String category;

  private MenuItem(String name, String description, String price, String category) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getPrice() {
    return price;
  }

  public String getCategory() {
    return category;
  }

  public static ArrayList<MenuItem> createDemoDataList(Context context) {
    ArrayList<MenuItem> datas = new ArrayList<>();
    try {
      String jsonDataString = readJsonDataFromFile(context);
      JSONArray menuItemsJsonArray = new JSONArray(jsonDataString);

      for (int i = 0; i < menuItemsJsonArray.length(); ++i) {

        JSONObject menuItemObject = menuItemsJsonArray.getJSONObject(i);

        String menuItemName = menuItemObject.getString("name");
        String menuItemDescription = menuItemObject.getString("description");
        String menuItemPrice = menuItemObject.getString("price");
        String menuItemCategory = menuItemObject.getString("category");

        MenuItem menuItem = new MenuItem(menuItemName, menuItemDescription, menuItemPrice,
            menuItemCategory);
        datas.add(menuItem);
      }
    } catch (IOException | JSONException exception) {
      Log.e("MenuItem", "Unable to parse JSON file.", exception);
    }

    return datas;
  }

  private static String readJsonDataFromFile(Context context) throws IOException {

    InputStream inputStream = null;
    StringBuilder builder = new StringBuilder();

    try {
      String jsonDataString = null;
      inputStream = context.getResources().openRawResource(R.raw.menu_items_json);
      BufferedReader bufferedReader = new BufferedReader(
          new InputStreamReader(inputStream, "UTF-8"));
      while ((jsonDataString = bufferedReader.readLine()) != null) {
        builder.append(jsonDataString);
      }
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }

    return new String(builder);
  }
}
