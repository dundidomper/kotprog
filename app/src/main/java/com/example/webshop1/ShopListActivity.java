package com.example.webshop1;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ShopListActivity extends AppCompatActivity {
    private static final String LOG_TAG = ShopListActivity.class.getName();
    private FirebaseUser user;
    private TextView countTextView;
    private int cartItems = 0;

    private RecyclerView mRecycleView;
    private ArrayList<ShoppingItem> mItemList;
    private ShoppingItemAdapter mAdapter;
    private FrameLayout redCircle;

    private NotificationHandler mNotificationHandler;
    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    private int gridNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_list);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "Autentikált felhasználó");
        } else {
            Log.d(LOG_TAG, "Nem autentikált felhasználó");
            finish();
        }

        mRecycleView = findViewById(R.id.recycleView);
        mRecycleView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mItemList = new ArrayList<>();

        mAdapter = new ShoppingItemAdapter(this, mItemList);
        mRecycleView.setAdapter(mAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("Items");

        queryData();

        mNotificationHandler = new NotificationHandler(this);

    }

    private void queryData() {
        mItemList.clear();

        //mItems.whereEqualTo()...
        mItems.orderBy("cartedCount", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                ShoppingItem item = document.toObject(ShoppingItem.class);
                item.setId(document.getId());
                mItemList.add(item);
            }

            if (mItemList.size() == 0) {
                initializeData();
            }

            mAdapter.notifyDataSetChanged();
        });
    }

    private void initializeData() {
        String[] itemsList = getResources().getStringArray(R.array.shopping_item_names);
        String[] itemsInfo = getResources().getStringArray(R.array.shopping_item_description);
        String[] itemsPrice = getResources().getStringArray(R.array.shopping_item_price);
        TypedArray itemsImageResources = getResources().obtainTypedArray(R.array.shopping_item_images);
        TypedArray itemRate = getResources().obtainTypedArray(R.array.shopping_item_rates);

        //mItemList.clear();

        for (int i = 0; i < itemsList.length; i++) {
            mItems.add(new ShoppingItem(
                    itemsList[i],
                    itemsInfo[i],
                    itemsPrice[i],
                    itemRate.getFloat(i, 0),
                    itemsImageResources.getResourceId(i, 0),
                    0));
        }

        itemsImageResources.recycle();
    }

    public void deleteItem(ShoppingItem item) {
        DocumentReference reference = mItems.document(item._getId());
        reference.delete()
                .addOnSuccessListener(success -> {
                    Log.d(LOG_TAG, "Elem sikeresen törölve: " + item._getId());
                })
                .addOnFailureListener(fail -> {
                    Toast.makeText(this, "Elem: " + item._getId() + " törlése nem sikerült.", Toast.LENGTH_LONG).show();
                });

        queryData();
        mNotificationHandler.cancel();
    }


        @Override
        public boolean onCreateOptionsMenu (Menu menu){
            super.onCreateOptionsMenu(menu);
            getMenuInflater().inflate(R.menu.shop_menu, menu);
            MenuItem menuItem = menu.findItem(R.id.search_bar);
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    Log.d(LOG_TAG, s);
                    if (s.isEmpty()) {
                        queryData();
                    } else {
                        searchItems(s);
                    }
                    return true;
                }

            });
            return true;
        }


        @Override
        public boolean onPrepareOptionsMenu (Menu menu){
            final MenuItem alertMenuItem = menu.findItem(R.id.cart);
            FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

            redCircle = (FrameLayout) rootView.findViewById(R.id.view_alert_red_circle);
            countTextView = (TextView) rootView.findViewById(R.id.view_alert_count_textview);

            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onOptionsItemSelected(alertMenuItem);
                }
            });
            return super.onPrepareOptionsMenu(menu);
        }
    public void updateAlertIcon (ShoppingItem item){
        cartItems = (cartItems + 1);
        if (0 < cartItems) {
            countTextView.setText(String.valueOf(cartItems));
        } else {
            countTextView.setText("");
        }
        redCircle.setVisibility((cartItems > 0) ? VISIBLE : GONE);

        mItems.document(item._getId()).update("cartedCount", item.getCartedCount() + 1)
                .addOnFailureListener(fail -> {
                    Toast.makeText(this, "Elem: " + item._getId() + " nem módosítható.", Toast.LENGTH_LONG).show();
                });

        mNotificationHandler.send(item.getName());
        queryData();
    }

    private void searchItems(String searchText) {
        mItemList.clear();
        mItems.whereEqualTo("name", searchText)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ShoppingItem item = document.toObject(ShoppingItem.class);
                        item.setId(document.getId());
                        mItemList.add(item);
                    }
                    mAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Hiba a keresés közben: ", e);
                });
    }

}
