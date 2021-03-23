package com.wizt.fragments


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.wizt.common.base.BaseFragment
import com.wizt.common.http.APIManager
import com.wizt.R
import com.wizt.common.base.BaseActivity
import com.wizt.common.constants.Constants
import com.wizt.models.Global
import com.wizt.models.Profile
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import com.stripe.android.view.CardInputWidget
import com.stripe.android.Stripe
import com.stripe.android.TokenCallback
import com.stripe.android.model.Card
import com.stripe.android.model.Token
import com.wizt.utils.ToastUtil
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_stripe.view.*
import java.lang.Exception


//TODO change it to real
private const val ARG_PLAN_ID = "param1"
//private const val publishableKey ="pk_test_IY0jwIvF9rJgayzNuRVP4g9M"
private const val publishableKey ="pk_live_6yBaS4vrxSp9iFWHjQUbNyYR"

class StripeFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance(planID: Int) =
            StripeFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PLAN_ID, planID)
                }
            }
    }

    private var planID: Int? = null

    private lateinit var mCardInputWidget: CardInputWidget
    private lateinit var myView: View


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            planID = it.getInt(ARG_PLAN_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stripe, container, false)
        mCardInputWidget = view.findViewById(R.id.card_input_widget)
        myView = view
        view.title.text = "Payment"
        view.notificationBtn.visibility = View.GONE
        view.menuBtn.visibility = View.GONE

        view.doneBtn.setOnClickListener {
            val cardToSave = mCardInputWidget.card
            if (cardToSave == null) {
                Toast.makeText(context, "Invalid Card Data", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            createToken(mCardInputWidget.card!!)
        }

        return view
    }

    private fun createToken(card: Card) {
        val stripe = Stripe(context!!, publishableKey)
        val tokenCallback = object : TokenCallback {
            override fun onSuccess(result: Token) {
                val token = result.id
                sendPayRequest(token)
            }

            override fun onError(e: Exception) {
                Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
                (activity as BaseActivity).closeWaitDialog()
            }
        }

        hideKeyboard()
        (activity as BaseActivity).showWaitDialog()
        stripe.createToken(card, tokenCallback)
    }

    fun hideKeyboard() {

        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(myView.windowToken, 0)
    }

    private fun sendPayRequest(token: String) {

        val successCallback: () -> Unit = {
            activity?.runOnUiThread {
                MyToastUtil.showMessage(context!!, "Thanks for your payment")
                Global.isSubscription = true
                (activity as BaseActivity).closeWaitDialog()
                this.activity?.finish()
                //this.getMyProfile()
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            (activity as BaseActivity).closeWaitDialog()
            activity?.runOnUiThread {
                MyToastUtil.showWarning(context!!, message)
            }
        }

        APIManager.share.subscribe(token, planID!!, successCallback, errorCallback)
    }
}