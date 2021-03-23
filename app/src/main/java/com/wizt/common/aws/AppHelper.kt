package com.wizt.common.aws

import android.content.Context
import android.graphics.Color
import android.util.Base64
import android.util.Log
import com.amazonaws.mobileconnectors.cognitoidentityprovider.*
import com.amazonaws.regions.Regions
import com.amazonaws.services.cognitoidentityprovider.model.AttributeType
import com.wizt.common.constants.Constants
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

object AppHelper {

    private val TAG = "AppHelper"
    // App settings

    private var attributeDisplaySeq: MutableList<String>? = null
    private var signUpFieldsC2O: MutableMap<String, String>? = null
    private var signUpFieldsO2C: MutableMap<String, String>? = null

    private var appHelper: AppHelper? = null
    var pool: CognitoUserPool? = null
        private set
    var currUser: String? = null
        private set
    //public static

    var newDevice: CognitoDevice? = null
        private set

    private val attributesChanged: CognitoUserAttributes? = null
    private val attributesToDelete: List<AttributeType>? = null

    private var currDisplayedItems: MutableList<ItemToDisplay>? = null
    var itemCount: Int = 0
        private set

    private var trustedDevices: MutableList<ItemToDisplay>? = null
    var devicesCount: Int = 0
        private set
    private var deviceDetails: List<CognitoDevice>? = null
    var thisDevice: CognitoDevice? = null
    var thisDeviceTrustState: Boolean = false
        private set

    private var firstTimeLogInDetails: MutableList<ItemToDisplay>? = null
    private var firstTimeLogInUserAttributes: MutableMap<String, String>? = null
    private var firstTimeLogInRequiredAttributes: List<String>? = null
    var firstTimeLogInItemsCount: Int = 0
        private set
    private var firstTimeLogInUpDatedAttributes: MutableMap<String, String>? = null
    var passwordForFirstTimeLogin: String? = null

    private var mfaOptions: MutableList<ItemToDisplay>? = null
    var allMfaOptions: List<String>? = null
        private set

    // Change the next three lines of code to run this demo on your user pool

    /**
     * Add your pool id here
     */
    //private static final String userPoolId = "Your userpool id here";
    private val userPoolId = "ap-southeast-1_iKDq5Cqg7"

    /**
     * Add you app id
     */
    //private static final String clientId = "Your client id here";
    private val clientId = "25mdd56j35t796frobq3fjk5cr"

    /**
     * App secret associated with your app id - if the App id does not have an associated App secret,
     * set the App secret to null.
     * e.g. clientSecret = null;
     */
    //private static final String clientSecret = null;
    //private val clientSecret = ""
    private val clientSecret = ""
    /**
     * Set Your User Pools region.
     * e.g. if your user pools are in US East (N Virginia) then set cognitoRegion = Regions.US_EAST_1.
     */
    //private static final Regions cognitoRegion = Regions.DEFAULT_REGION;
    private val cognitoRegion = Regions.AP_SOUTHEAST_1


    var currSession: CognitoUserSession? = null
    var userDetails: CognitoUserDetails? = null
        set(details) {
            field = details
            refreshWithSync()
        }

    // User details to display - they are the current values, including any local modification
    var isPhoneVerified: Boolean = false
    var isEmailVerified: Boolean = false

    var isPhoneAvailable: Boolean = false
    var isEmailAvailable: Boolean = false

    private var currUserAttributes: MutableSet<String>? = null

    val newAvailableOptions: List<String>
        get() {
            val newOption = ArrayList<String>()
            for (attribute in attributeDisplaySeq!!) {
                if (!currUserAttributes!!.contains(attribute)) {
                    newOption.add(attribute)
                }
            }
            return newOption
        }

    val userAttributesForFirstTimeLogin: Map<String, String>?
        get() = firstTimeLogInUpDatedAttributes

    val mfaOptionsCount: Int
        get() = mfaOptions!!.size

    fun init(context: Context) {
        setData()

        if (appHelper != null && pool != null) {
            return
        }

        if (appHelper == null) {
            appHelper = AppHelper
        }

        if (pool == null) {

            val data = Base64.decode(clientSecret, Base64.DEFAULT)
            val text = String(data, StandardCharsets.UTF_8)
            // Create a user pool with default ClientConfiguration
            pool = CognitoUserPool(context,
                userPoolId,
                clientId,
                text,
                cognitoRegion
            )

            // This will also work
            /*
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            AmazonCognitoIdentityProvider cipClient = new AmazonCognitoIdentityProviderClient(new AnonymousAWSCredentials(), clientConfiguration);
            cipClient.setRegion(Region.getRegion(cognitoRegion));
            userPool = new CognitoUserPool(context, userPoolId, clientId, clientSecret, cipClient);
            */


        }

        isPhoneVerified = false
        isPhoneAvailable = false
        isEmailVerified = false
        isEmailAvailable = false

        currUserAttributes = HashSet()
        currDisplayedItems = ArrayList<ItemToDisplay>()
        trustedDevices = ArrayList<ItemToDisplay>()
        firstTimeLogInDetails = ArrayList<ItemToDisplay>()
        firstTimeLogInUpDatedAttributes = HashMap()

        newDevice = null
        thisDevice = null
        thisDeviceTrustState = false

        mfaOptions = ArrayList<ItemToDisplay>()
    }

    fun getSignUpFieldsC2O(): Map<String, String>? {
        return signUpFieldsC2O
    }

    fun getSignUpFieldsO2C(): Map<String, String>? {
        return signUpFieldsO2C
    }

    fun getAttributeDisplaySeq(): List<String>? {
        return attributeDisplaySeq
    }

    fun setUser(newUser: String) {
        currUser = newUser
    }

    fun clearCurrUserAttributes() {
        currUserAttributes!!.clear()
    }

    fun addCurrUserattribute(attribute: String) {
        currUserAttributes!!.add(attribute)
    }

    fun formatException(exception: Exception): String {
        var formattedString = "Internal Error"
        Log.e(TAG, " -- Error: $exception")
        Log.getStackTraceString(exception)

        val temp = exception.message

        if (temp != null && temp.length > 0) {
            formattedString = temp.split("\\(".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            if (temp != null && temp.length > 0) {
                return formattedString
            }
        }

        return formattedString
    }

    fun getItemForDisplay(position: Int): ItemToDisplay {
        return currDisplayedItems!![position]
    }

    fun getDeviceForDisplay(position: Int): ItemToDisplay {
        return if (position >= trustedDevices!!.size) {
            ItemToDisplay(
                " ",
                " ",
                " ",
                Color.BLACK,
                Color.DKGRAY,
                Color.parseColor("#37A51C"),
                0,
                null
            )
        } else trustedDevices!![position]
    }

    fun getUserAttributeForFirstLogInCheck(position: Int): ItemToDisplay {
        return firstTimeLogInDetails!![position]
    }

    fun setUserAttributeForDisplayFirstLogIn(
        currAttributes: MutableMap<String, String>,
        requiredAttributes: List<String>
    ) {
        firstTimeLogInUserAttributes = currAttributes
        firstTimeLogInRequiredAttributes = requiredAttributes
        firstTimeLogInUpDatedAttributes = HashMap()
        refreshDisplayItemsForFirstTimeLogin()
    }

    fun setUserAttributeForFirstTimeLogin(attributeName: String, attributeValue: String) {
        if (firstTimeLogInUserAttributes == null) {
            firstTimeLogInUserAttributes = HashMap()
        }
        firstTimeLogInUserAttributes!![attributeName] = attributeValue
        firstTimeLogInUpDatedAttributes!![attributeName] = attributeValue
        refreshDisplayItemsForFirstTimeLogin()
    }

    private fun refreshDisplayItemsForFirstTimeLogin() {
        firstTimeLogInItemsCount = 0
        firstTimeLogInDetails = ArrayList<ItemToDisplay>()

        for ((key, value) in firstTimeLogInUserAttributes!!) {
            if ("phone_number_verified" == key || "email_verified" == key) {
                continue
            }
            var message = ""
            if (firstTimeLogInRequiredAttributes != null && firstTimeLogInRequiredAttributes!!.contains(key)) {
                message = "Required"
            }
            val item =
                ItemToDisplay(
                    key,
                    value,
                    message,
                    Color.BLACK,
                    Color.DKGRAY,
                    Color.parseColor("#329AD6"),
                    0,
                    null
                )
            firstTimeLogInDetails!!.add(item)
            firstTimeLogInRequiredAttributes!!.size
            firstTimeLogInItemsCount++
        }

        for (attr in firstTimeLogInRequiredAttributes!!) {
            if (!firstTimeLogInUserAttributes!!.containsKey(attr)) {
                val item =
                    ItemToDisplay(
                        attr,
                        "",
                        "Required",
                        Color.BLACK,
                        Color.DKGRAY,
                        Color.parseColor("#329AD6"),
                        0,
                        null
                    )
                firstTimeLogInDetails!!.add(item)
                firstTimeLogInItemsCount++
            }
        }
    }

    fun newDevice(device: CognitoDevice) {
        newDevice = device
    }

    fun setDevicesForDisplay(devicesList: List<CognitoDevice>) {
        devicesCount = 0
        thisDeviceTrustState = false
        deviceDetails = devicesList
        trustedDevices = ArrayList<ItemToDisplay>()
        for (device in devicesList) {
            if (thisDevice != null && thisDevice!!.deviceKey == device.deviceKey) {
                thisDeviceTrustState = true
            } else {
                val item = ItemToDisplay(
                    "",
                    device.deviceName,
                    device.createDate.toString(),
                    Color.BLACK,
                    Color.DKGRAY,
                    Color.parseColor("#329AD6"),
                    0,
                    null
                )
                //item.setDataDrawable("checked")
                trustedDevices!!.add(item)
                devicesCount++
            }
        }
    }

    fun getDeviceDetail(position: Int): CognitoDevice? {
        return if (position <= devicesCount) {
            deviceDetails!![position]
        } else {
            null
        }
    }

    fun setMfaOptionsForDisplay(options: List<String>, parameters: Map<String, String>) {
        allMfaOptions = options
        mfaOptions = ArrayList<ItemToDisplay>()
        var textToDisplay = ""
        for (option in options) {
            if ("SMS_MFA" == option) {
                textToDisplay = "Send SMS"
                if (parameters.containsKey("CODE_DELIVERY_DESTINATION")) {
                    textToDisplay = textToDisplay + " to " + parameters["CODE_DELIVERY_DESTINATION"]
                }
            } else if ("SOFTWARE_TOKEN_MFA" == option) {
                textToDisplay = "Use TOTP"
                if (parameters.containsKey("FRIENDLY_DEVICE_NAME")) {
                    textToDisplay = textToDisplay + ": " + parameters["FRIENDLY_DEVICE_NAME"]
                }
            }
            val item =
                ItemToDisplay(
                    "",
                    textToDisplay,
                    "",
                    Color.BLACK,
                    Color.DKGRAY,
                    Color.parseColor("#329AD6"),
                    0,
                    null
                )
            mfaOptions!!.add(item)
            textToDisplay = "Unsupported MFA"
        }
    }

    fun getMfaOptionCode(position: Int): String {
        return allMfaOptions!![position]
    }

    fun getMfaOptionForDisplay(position: Int): ItemToDisplay {
        return if (position >= mfaOptions!!.size) {
            ItemToDisplay(
                " ",
                " ",
                " ",
                Color.BLACK,
                Color.DKGRAY,
                Color.parseColor("#37A51C"),
                0,
                null
            )
        } else mfaOptions!![position]
    }

    private fun setData() {
        // Set attribute display sequence
        attributeDisplaySeq = ArrayList()
        attributeDisplaySeq!!.add("given_name")
        attributeDisplaySeq!!.add("middle_name")
        attributeDisplaySeq!!.add("family_name")
        attributeDisplaySeq!!.add("nickname")
        attributeDisplaySeq!!.add("phone_number")
        attributeDisplaySeq!!.add("email")

        signUpFieldsC2O = HashMap()
        signUpFieldsC2O!!["Given name"] = "given_name"
        signUpFieldsC2O!!["Family name"] = "family_name"
        signUpFieldsC2O!!["Nick name"] = "nickname"
        signUpFieldsC2O!!["Phone number"] = "phone_number"
        signUpFieldsC2O!!["Phone number verified"] = "phone_number_verified"
        signUpFieldsC2O!!["Email verified"] = "email_verified"
        signUpFieldsC2O!!["Email"] = "email"
        signUpFieldsC2O!!["Middle name"] = "middle_name"

        signUpFieldsO2C = HashMap()
        signUpFieldsO2C!!["given_name"] = "Given name"
        signUpFieldsO2C!!["family_name"] = "Family name"
        signUpFieldsO2C!!["nickname"] = "Nick name"
        signUpFieldsO2C!!["phone_number"] = "Phone number"
        signUpFieldsO2C!!["phone_number_verified"] = "Phone number verified"
        signUpFieldsO2C!!["email_verified"] = "Email verified"
        signUpFieldsO2C!!["email"] = "Email"
        signUpFieldsO2C!!["middle_name"] = "Middle name"

    }

    private fun refreshWithSync() {
        // This will refresh the current items to display list with the attributes fetched from service
        val tempKeys = ArrayList<String>()
        val tempValues = ArrayList<String>()

        isEmailVerified = false
        isPhoneVerified = false

        isEmailAvailable = false
        isPhoneAvailable = false

        currDisplayedItems = ArrayList<ItemToDisplay>()
        currUserAttributes!!.clear()
        itemCount = 0

        for ((key, value) in userDetails!!.attributes.attributes) {

            tempKeys.add(key)
            tempValues.add(value)

            if (key.contains("email_verified")) {
                isEmailVerified = value.contains("true")
            } else if (key.contains("phone_number_verified")) {
                isPhoneVerified = value.contains("true")
            }

            if (key == "email") {
                isEmailAvailable = true
            } else if (key == "phone_number") {
                isPhoneAvailable = true
            }
        }

        // Arrange the input attributes per the display sequence
        val keySet = HashSet(tempKeys)
        for (det in attributeDisplaySeq!!) {
            if (keySet.contains(det)) {
                // Adding items to display list in the required sequence

                val item = ItemToDisplay(
                    signUpFieldsO2C!![det], tempValues[tempKeys.indexOf(det)], "",
                    Color.BLACK, Color.DKGRAY, Color.parseColor("#37A51C"),
                    0, null
                )

                if (det.contains("email")) {
                    if (isEmailVerified) {
                        //item.setDataDrawable("checked")
                        //item.setMessageText("Email verified")
                    } else {
                        //item.setDataDrawable("not_checked")
                        //item.setMessageText("Email not verified")
                        //item.setMessageColor(Color.parseColor("#E94700"))
                    }
                }

                if (det.contains("phone_number")) {
                    if (isPhoneVerified) {
                        //item.setDataDrawable("checked")
                        //item.setMessageText("Phone number verified")
                    } else {
                        //item.setDataDrawable("not_checked")
                        //item.setMessageText("Phone number not verified")
                        //item.setMessageColor(Color.parseColor("#E94700"))
                    }
                }

                currDisplayedItems!!.add(item)
                currUserAttributes!!.add(det)
                itemCount++
            }
        }
    }

    private fun modifyAttribute(attributeName: String, newValue: String) {
        //

    }

    private fun deleteAttribute(attributeName: String) {

    }
}