package com.newmoon.dark.pro

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.android.billingclient.api.*
import com.newmoon.common.util.*
import com.newmoon.dark.R
import com.newmoon.dark.extensions.updateNotification
import com.newmoon.dark.logEvent
import com.newmoon.dark.view.AdvancedPageIndicator
import com.newmoon.dark.view.IndicatorMark
import kotlinx.android.synthetic.main.activity_billing.*
import java.util.*

class BillingActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var product: SkuDetails? = null
    private var from: String? = null

    private var mTextPager: ViewPager? = null
    private var mPagerAdapter: WelcomePagerAdapter? = null
    private var mViewPageIndicator: AdvancedPageIndicator? = null
    private var mIsViewPagerAutoSlide = true
    private var mViewPagerCurrentPosition = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immersive(window)
        setStatusBarTheme(window, false)

        setContentView(R.layout.activity_billing)

        findViewById<View>(R.id.ic_back).setOnClickListener { finish() }

        if (intent != null) {
            from = intent.getStringExtra(EXTRA_FROM)
        }

        val priceTv = findViewById<TextView>(R.id.price_text_view)
        val purchaseButton = findViewById<TextView>(R.id.welcome_start_button)

        billingClient = BillingClient.newBuilder(this).setListener(this)
            .enablePendingPurchases().build()
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    val skuList = ArrayList<String>()
                    skuList.add(BillingManager.NO_ADS_PRODUCT)
                    val params = SkuDetailsParams.newBuilder()
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
                    billingClient!!.querySkuDetailsAsync(
                        params.build()
                    ) { billingResult, skuDetailsList ->
                        // Process the result.
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList.size > 0) {
                            product = skuDetailsList[0]

                            val price = product!!.price
                            var amountStartIndex = 0

                            var i = 0
                            val length = price.length
                            while (i < length) {
                                if (Character.isDigit(price[i])) {
                                    amountStartIndex = i
                                    break
                                }
                                i++
                            }
//                            currencyCodeTv.text = price.substring(0, amountStartIndex)
//                            priceTv.text = price.substring(amountStartIndex)
                            priceTv.text = price + " / "
                            price_text_view_double.text =
                                price.substring(0, amountStartIndex) +
                                        (product!!.priceAmountMicros * 2f / 1000000).toString()
                            price_text_view_double.paint.flags =
                                Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
                            off.visibility = View.VISIBLE
                        }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })

        purchaseButton.background = BackgroundDrawables.createBackgroundDrawable(
            resources.getColor(R.color.primary_color),
            Dimensions.pxFromDp(8f).toFloat(),
            true
        )

        price_container.background = BackgroundDrawables.createBackgroundDrawable(
            0xff636B7B.toInt(),
            Dimensions.pxFromDp(8f).toFloat(),
            false
        )
        purchaseButton.setOnClickListener(View.OnClickListener {
            if (product == null) {
                return@OnClickListener
            }

            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(product)
                .build()
            val result = billingClient!!.launchBillingFlow(this@BillingActivity, flowParams)
            logEvent(
                this, "Pro_Buy_Click", mapOf(
                    "from" to if (from == null) {
                        ""
                    } else {
                        from!!
                    }
                )
            )
        })

        logEvent(
            this, "Pro_Show", mapOf(
                "from" to if (from == null) {
                    ""
                } else {
                    from!!
                }
            )
        )


        Preferences.default.putBoolean("pref_key_has_billing_shown", true)

        initTextPager()
    }

    private fun initTextPager() {
        val pagerAdapter = WelcomePagerAdapter(this)
        mPagerAdapter = pagerAdapter

        mTextPager = findViewById(R.id.welcome_guide_viewpager)
        mTextPager!!.adapter = pagerAdapter

        mViewPageIndicator = findViewById(R.id.welcome_guide_viewpager_indicator)

        val markerTypes = ArrayList<IndicatorMark.MarkerType>()
        for (i in 0 until pagerAdapter.count) {
            markerTypes.add(IndicatorMark.MarkerType.CIRCLE)
        }

        if (pagerAdapter.count > 1) {
            mIsViewPagerAutoSlide = true
            mViewPageIndicator!!.addMarkers(markerTypes)
            mTextPager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    positionAbsolute: Int,
                    positionAbsoluteOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    val position: Int
                    val positionOffset: Float
                    if (Dimensions.isRtl) {
                        position = mPagerAdapter!!.count - 2 - positionAbsolute
                        positionOffset = 1 - positionAbsoluteOffset
                    } else {
                        position = positionAbsolute
                        positionOffset = positionAbsoluteOffset
                    }

                    mViewPageIndicator!!.onScrolling(position, positionOffset)
                }

                override fun onPageSelected(positionAbsolute: Int) {
                    if (mIsViewPagerAutoSlide) {
                        return
                    }
                    if (positionAbsolute > mViewPagerCurrentPosition) {
                        //                        playForwardDropAnimation(positionAbsolute);
                    } else {
                        //                        playBackwardAnimation(positionAbsolute);
                    }
                    mViewPagerCurrentPosition = positionAbsolute
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })
        } else {
            mIsViewPagerAutoSlide = false
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    BillingManager.isPremium = true

                    updateNotification()

                    finish()
                    Preferences.default.putBoolean(
                        BillingManager.PREF_KEY_USER_HAS_VERIFIED_SUCCESS,
                        true
                    )

                    logEvent(
                        this, "Pro_Buy_Success", mapOf(
                            "from" to if (from == null) {
                                ""
                            } else {
                                from!!
                            }
                        )
                    )

                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient!!.acknowledgePurchase(acknowledgePurchaseParams) { }
                    }
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    companion object {

        val EXTRA_FROM = "extra_from"
    }
}
