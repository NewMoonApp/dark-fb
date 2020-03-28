package com.newmoon.dark.pro;

import android.app.Application;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.newmoon.common.util.Preferences;
import com.newmoon.dark.BuildConfig;

import java.util.List;

public class BillingManager {

    public static final String BILLING_VERIFY_SUCCESS = "billing.verify.success";
    public static final String PREF_KEY_USER_HAS_VERIFIED_SUCCESS = "iap.user.has.verified.success";

    public static final String NO_ADS_PRODUCT = "dark_pro_1";

    public static boolean isPremiumUser() {
        return BuildConfig.DEBUG ? false : isPremium;
    }

    public static boolean isPremium = BillingManager.hasUserEverVerifiedSuccessfully();

    public static BillingClient billingClient;
    public static PurchasesUpdatedListener listener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {

        }
    };

    public static void init(Application application) {
        billingClient = BillingClient.newBuilder(application).setListener(listener)
                .enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                List<Purchase> purchases = purchasesResult.getPurchasesList();
                if (purchases != null && purchases.size() > 0) {
                    for (Purchase purchase : purchases) {
                        if (NO_ADS_PRODUCT.equals(purchase.getSku()) && purchase.isAcknowledged()) {
//                            Threads.postOnMainThread(() -> HSGlobalNotificationCenter.sendNotification(BILLING_VERIFY_SUCCESS));
                            Preferences.Companion.getDefault().putBoolean(BillingManager.PREF_KEY_USER_HAS_VERIFIED_SUCCESS, true);
                            isPremium = true;
                        } else if (NO_ADS_PRODUCT.equals(purchase.getSku())) {
                            AcknowledgePurchaseParams acknowledgePurchaseParams =
                                    AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase.getPurchaseToken())
                                            .build();
                            billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                                @Override
                                public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                    isPremium = true;
                                    Preferences.Companion.getDefault().putBoolean(BillingManager.PREF_KEY_USER_HAS_VERIFIED_SUCCESS, true);
                                }
                            });
                        }
                    }
                } else {
                    isPremium = false;
                }
            }

            @Override
            public void onBillingServiceDisconnected() {

            }
        });
    }


    public static boolean hasUserEverVerifiedSuccessfully() {
        return Preferences.Companion.getDefault().getBoolean(PREF_KEY_USER_HAS_VERIFIED_SUCCESS, false);
    }
}
