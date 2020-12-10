package com.thegauravparmar.tictactoe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

// won't work until this app is on playstore
public class PaymentActivity extends AppCompatActivity {
    ProgressBar progressBar;
    RecyclerView recyclerView;

    BillingClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        initViews();
        connectToGoogle();
    }

    private void loadPurchases(){
        Log.d("debugg", "Supported inapp " + client.isFeatureSupported(BillingClient.SkuType.INAPP).getResponseCode());
        List<String> skuList = new ArrayList<>();
        // yaha jitne chahe add karlo product ids, abhi sirf 2 hi hai
        skuList.add("credits_500");
        skuList.add("credits_100");
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        client.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                            showProgress(false);
                            if(skuDetailsList.size() == 0){
                                Toast.makeText(getApplicationContext(), "No Items Found in store", Toast.LENGTH_LONG).show();
                            }
                            setUpRecyclerView(skuDetailsList);
                        }else{
                            showProgress(false);
                            Log.d("paymentActivity", "Error Loading purchases : ".concat(billingResult.getDebugMessage()));
                            Toast.makeText(getApplicationContext(), "Error Loading Store", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }

    private void setUpRecyclerView(List<SkuDetails> skuDetailsList){
        Adapter adapter = new Adapter(PaymentActivity.this, skuDetailsList, new OnItemClicked() {
            @Override
            public void OnClicked(SkuDetails item) {
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(item)
                        .build();
                int response = client.launchBillingFlow(PaymentActivity.this, billingFlowParams).getResponseCode();
                if(response == BillingClient.BillingResponseCode.USER_CANCELED){
                    Toast.makeText(getApplicationContext(), "Payment Cancelled by User", Toast.LENGTH_LONG).show();
                }else if(response == BillingClient.BillingResponseCode.OK){
                    Toast.makeText(getApplicationContext(), "Payment Success", Toast.LENGTH_LONG).show();
                    // yaha se seedhe connectToGoogle me jo setListner kiya hai waha aayega response onPurchasesUpdated pr
                }else{
                    Toast.makeText(getApplicationContext(), "Error Detecting Payment", Toast.LENGTH_LONG).show();
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(PaymentActivity.this));
        recyclerView.setAdapter(adapter);
    }

    private void connectToGoogle(){
        showProgress(true);
        client = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
                        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null){
                            // yaha apan ko purchases mil jayenge list me to apne server pr save karlena
                            /*TODO
                               SAVE PURCHASE ON SERVER
                               */
                        }
                    }
                }).build();

        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    loadPurchases();
                }else{
                    showProgress(false);
                    Log.d("paymentActivity", "Error connecting to google service : ".concat(billingResult.getDebugMessage()));
                    Toast.makeText(getApplicationContext(), "Error Loading Store", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                showProgress(false);
                Toast.makeText(getApplicationContext(), "Error Loading Store", Toast.LENGTH_LONG).show();
                Log.d("paymentActivity", "Google Service Disconnected");
                finish();
            }
        });
    }

    private void initViews(){
        progressBar = findViewById(R.id.progress);
        recyclerView = findViewById(R.id.recycler);
    }

    private void showProgress(boolean show){
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(!show ? View.VISIBLE : View.GONE);
    }

    class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder>{
        List<SkuDetails> skuDetailsList;
        Context mContext;
        OnItemClicked callback;

        public Adapter(Context mContext, List<SkuDetails> skuDetailsList, OnItemClicked callback){
            this.skuDetailsList = skuDetailsList;
            this.mContext = mContext;
            this.callback = callback;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.entry_purchase, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            holder.title.setText(skuDetailsList.get(position).getTitle());
            holder.desc.setText(skuDetailsList.get(position).getDescription());
            holder.price.setText(skuDetailsList.get(position).getPrice());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.OnClicked(skuDetailsList.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            return skuDetailsList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            TextView title, desc, price;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.purchase_title);
                desc = itemView.findViewById(R.id.purchase_desc);
                price = itemView.findViewById(R.id.purchase_price);
            }
        }
    }

    interface OnItemClicked{
        void OnClicked(SkuDetails item);
    }
}