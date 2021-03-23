package com.wizt.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.bumptech.glide.Glide
import com.wizt.activities.SubscribeActivity
import com.wizt.common.base.BaseFragment
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.extensions.setupClearButtonWithAction
import com.wizt.models.Address
import com.wizt.models.User
import com.wizt.R
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import kotlinx.android.synthetic.main.app_bar.view.*
import kotlinx.android.synthetic.main.fragment_address.view.*
import matteocrippa.it.karamba.isNumeric
import matteocrippa.it.karamba.isValidEmail

private const val ARG_PLAN_ID = "param1"

class AddressFragment : BaseFragment() {

    companion object {

        @JvmStatic
        fun newInstance(planID: Int) =
            AddressFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PLAN_ID, planID)
                }
            }
    }
    private lateinit var address: Address

    var planID : Int? = 0
    var billingMethodStr: String? = ""
    var savedMoneyStr : String? = ""
    var getLabelStr: String? = ""
    var iconUrl: String? = ""
    var levelStr: String = ""

    // UI Elements
    private lateinit var nameET: EditText
    private lateinit var mobileET: EditText
    private lateinit var addressET: EditText
    private lateinit var countryET: EditText
    private lateinit var stateET: EditText
    private lateinit var zipCodeET: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            planID = it.getInt(ARG_PLAN_ID)
//        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_address, container, false)

        //Initialize UI
        if (iconUrl != null) {
            Glide.with(context!!).load(iconUrl).into(view.imageView)
        }
        view.level.text = levelStr
        view.billMethodTV.text = billingMethodStr
        view.saveMoneyTV.text = savedMoneyStr
        view.countLabelTV.text = getLabelStr

        view.backBtn.setOnClickListener {
            (activity as SubscribeActivity).popFragment()
        }

        view?.payBtn?.setOnClickListener {
            if (!checkValidation()) {
                return@setOnClickListener
            }

//            if (PreferenceUtils.getBoolean(Constants.PREF_IS_FIRST_ADDRESS)) {
//                (activity as SubscribeActivity).popFragment()
//                (activity as SubscribeActivity).pushFragment(StripeFragment.newInstance(planID!!))
//            } else {
//
//            }
            saveAddressInLocal()
            postAddressRequest()
        }

        nameET = view.findViewById(R.id.nameET)
        mobileET = view.findViewById(R.id.mobileET)
        addressET = view.findViewById(R.id.addressET)
        countryET = view.findViewById(R.id.countryET)
        stateET = view.findViewById(R.id.stateET)
        zipCodeET = view.findViewById(R.id.zipCodeET)

        nameET.setupClearButtonWithAction()
        mobileET.setupClearButtonWithAction()
        addressET.setupClearButtonWithAction()
        countryET.setupClearButtonWithAction()
        stateET.setupClearButtonWithAction()
        zipCodeET.setupClearButtonWithAction()

//        if (PreferenceUtils.getBoolean(Constants.PREF_IS_FIRST_ADDRESS)) {
//            // Initialize UI with address
//            nameET.setText(PreferenceUtils.getString(Constants.ADDRESS_NAME))
//            mobileET.setText(PreferenceUtils.getString(Constants.ADDRESS_MOBILE))
//            addressET.setText(PreferenceUtils.getString(Constants.ADDRESS_SHIPPING))
//            countryET.setText(PreferenceUtils.getString(Constants.ADDRESS_COUNTRY))
//            stateET.setText(PreferenceUtils.getString(Constants.ADDRESS_STATE))
//            zipCodeET.setText(PreferenceUtils.getString(Constants.ADDRESS_ZIP_CODE))
//        }

        //loadAddressFromLocal()

        return view
    }

    private fun saveAddressInLocal() {
        address = Address("", 0, "", "", "", "", "", "")

        PreferenceUtils.saveString(Constants.ADDRESS_NAME, nameET.text.toString())
        PreferenceUtils.saveString(Constants.ADDRESS_MOBILE, mobileET.text.toString())
        PreferenceUtils.saveString(Constants.ADDRESS_SHIPPING, addressET.text.toString())
        PreferenceUtils.saveString(Constants.ADDRESS_COUNTRY, countryET.text.toString())
        PreferenceUtils.saveString(Constants.ADDRESS_STATE, stateET.text.toString())
        PreferenceUtils.saveString(Constants.ADDRESS_ZIP_CODE, zipCodeET.text.toString())

        address.name = nameET.text.toString()
        address.country = countryET.text.toString()
        address.mobile = mobileET.text.toString()
        address.shipping_address = addressET.text.toString()
        address.state = stateET.text.toString()
        address.zip_code = zipCodeET.text.toString()
    }

    private fun loadAddressFromLocal() {
        nameET.setText(PreferenceUtils.getString(Constants.ADDRESS_NAME))
        countryET.setText(PreferenceUtils.getString(Constants.ADDRESS_MOBILE))
        mobileET.setText(PreferenceUtils.getString(Constants.ADDRESS_SHIPPING))
        addressET.setText(PreferenceUtils.getString(Constants.ADDRESS_COUNTRY))
        stateET.setText(PreferenceUtils.getString(Constants.ADDRESS_STATE))
        zipCodeET.setText(PreferenceUtils.getString(Constants.ADDRESS_ZIP_CODE))
    }

    private fun postAddressRequest() {
        val successCallback : (Address) -> Unit = { _ ->
            PreferenceUtils.saveBoolean(Constants.PREF_IS_FIRST_ADDRESS, true)
            (activity as SubscribeActivity).popFragment()
            (activity as SubscribeActivity).pushFragment(StripeFragment.newInstance(planID!!))
        }

        APIManager.share.postMyAddress(address, successCallback, errorCallback)
    }

    private fun checkValidation(): Boolean {

        if (nameET.text.isEmpty()) {
            MyToastUtil.showWarning(context!!, "Name is required")
            return false
        }
        if (!mobileET.text.toString().isValidEmail()) {
            MyToastUtil.showWarning(context!!, "Email is required")
            return false
        }
        if (addressET.text.isEmpty()) {
            MyToastUtil.showWarning(context!!, "Address is required")
            return false
        }
        if (countryET.text.isEmpty()) {
            MyToastUtil.showWarning(context!!, "Country is required")
            return false
        }
        if (stateET.text.isEmpty()) {
            MyToastUtil.showWarning(context!!, "State is required")
            return false
        }
        if (zipCodeET.text.isEmpty()) {
            MyToastUtil.showWarning(context!!, "Zip code is required")
            return false
        }
        if (!zipCodeET.text.toString().isNumeric()) {
            MyToastUtil.showWarning(context!!, "Zip code should be numeric")
            return false
        }

        return true
    }
}
