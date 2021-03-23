package com.wizt.activities.chatbot

import android.annotation.SuppressLint
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.view.LayoutInflater
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.dialogflow.v2.*
import java.util.*
import android.widget.*
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.util.Log
import android.view.ViewGroup
import com.wizt.R
import com.wizt.activities.auth.LoginActivity
import com.wizt.activities.auth.RegisterActivity
import com.wizt.common.constants.Constants
import com.wizt.utils.MyToastUtil
import kotlinx.android.synthetic.main.activity_chatbot.*
import kotlinx.android.synthetic.main.item_recyclerview.view.*


const val USER=0
const val BOT=1
const val SPEECH_INPUT=2
const val INIT_CALL = "init_call"
const val INIT_STEP1 = "init_step1"
const val INIT_STEP2 = "init_step2"
const val INIT_STEP3 = "init_step3"
const val SPLIT_SENTENCE = "[n]"
const val SPLIT_WORDS = "[w]"

const val NEWUSER = "New User"
const val EXISTINGUSER = "Existing User"
const val LetsGo = "Let's Go"

const val STEPONEANSWER = "Hi, I'm FYBE, your personal AI, Welcome to WIZT"
const val STEPTWOANSWER = "We help you record and remember where your items are kept"

class ChatBotActivity : AppCompatActivity() {

    companion object {
        const val TAG = "WIZT:ChatBotActivity"
    }

    private val uuid = UUID.randomUUID().toString()

    private var client: SessionsClient? = null
    private var session: SessionName? = null

    private var asistan_voice:TextToSpeech?=null
    private var userType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        val scrollview=findViewById<ScrollView>(R.id.scroll_chat)
        scrollview.post{
            scrollview.fullScroll(ScrollView.FOCUS_DOWN)
        }

        val queryEditText = findViewById<EditText>(R.id.edittext)
        queryEditText.setOnKeyListener { view, keyCode, event ->
            if (event.action === KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        sendMessage(send)
                        true
                    }
                    else -> {
                    }
                }
            }
            false
        }

        send.setOnClickListener(this::sendMessage)

        microphone.setOnClickListener(this::sendMicrophoneMessage)

        initAsisstant()

        initAsisstantVoice()

        initBot()

    }

    private fun initBot() {

        callBot(INIT_STEP1)
    }

    private fun callBot(msg: String) {
        val queryInput =
            QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en")).build()
        RequestTask(this@ChatBotActivity, session!!, client!!, queryInput).execute()
    }

    private fun initAsisstantVoice() {

        asistan_voice= TextToSpeech(applicationContext,object : TextToSpeech.OnInitListener {
            override fun onInit(status: Int) {
                if (status!=TextToSpeech.ERROR){
                    asistan_voice?.language=Locale("en")
                }
            }

        })

    }

    private fun initAsisstant() {
        try {
            val stream = resources.openRawResource(R.raw.asistan)
            val credentials = GoogleCredentials.fromStream(stream)
            val projectId = (credentials as ServiceAccountCredentials).projectId

            val settingsBuilder = SessionsSettings.newBuilder()
            val sessionsSettings =
                settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build()
            client = SessionsClient.create(sessionsSettings)
            session = SessionName.of(projectId, uuid)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private fun sendMessage(view: View) {
        val msg = edittext.text.toString()
        if (msg.trim { it <= ' ' }.isEmpty()) {
            Log.d(TAG,"onlyTest")
        } else {
            appendText(msg, USER)
            edittext.setText("")

            // Java V2
            val queryInput =
                QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en")).build()
            RequestTask(this@ChatBotActivity, session!!, client!!, queryInput).execute()
        }
    }

    private fun sendMicrophoneMessage(view:View){
        val intent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            getString(R.string.speech_prompt)
        )
        try {
            startActivityForResult(intent, SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                getString(R.string.speech_not_supported),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun appendText(message: String, type: Int) {
        val layout: FrameLayout
        when (type) {
            USER -> layout = appendUserText()
            BOT -> layout = appendBotText()
            else -> layout = appendBotText()
        }
        layout.isFocusableInTouchMode = true
        linear_chat.addView(layout)
        val tv = layout.findViewById<TextView>(R.id.chatMsg)

        var newMsg = message
        if (newMsg.contains(SPLIT_WORDS)) {
            val newMsgList = newMsg.split(SPLIT_WORDS)
            newMsg = newMsgList[0]
            var msgList = newMsgList.subList(1, newMsgList.size)
            var recyclerView: RecyclerView = layout.findViewById(R.id.buttonsReclerView)
            recyclerView.visibility = View.VISIBLE
            var gridLayoutManager: GridLayoutManager = GridLayoutManager(applicationContext, msgList.size)
            recyclerView.layoutManager = gridLayoutManager
            var adapter: RecyclerAdapter = RecyclerAdapter(applicationContext, itemClickListener, msgList)
            recyclerView.adapter = adapter

        }
        tv.setText(newMsg)
        Util.hideKeyboard(this)
        layout.requestFocus()
        edittext.requestFocus() // change focus back to edit text to continue typing
        //if(type!= USER) asistan_voice?.speak(newMsg,TextToSpeech.QUEUE_FLUSH,null)


    }

    private fun sendMessageTxt(msg: String) {
        if (msg.trim { it <= ' ' }.isEmpty()) {
            MyToastUtil.showWarning(this,getString(R.string.entermsg))
        } else {
            appendText(msg, USER)

            if (LetsGo.equals(msg)) {
                when (userType) {
                    0 -> MyToastUtil.showWarning(applicationContext, "Error")
                    1 -> {
                        //Toast.makeText(applicationContext, "Go to SignUp", Toast.LENGTH_SHORT).show()
                        gotoRegisterActivity()
                    }
                    2 -> {
                        //Toast.makeText(applicationContext, "SignIn", Toast.LENGTH_SHORT).show()
                        gotoLoginActivity()
                    }
                }
            } else {
                // Java V2
                val queryInput =
                    QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en")).build()
                RequestTask(this@ChatBotActivity, session!!, client!!, queryInput).execute()
            }
        }
    }

    fun gotoLoginActivity() {
        val intent = Intent(applicationContext, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun gotoRegisterActivity() {
        val intent = Intent(applicationContext, RegisterActivity::class.java)
        intent.putExtra(Constants.PREF_FROM_CHATBOT, true)
        startActivity(intent)
        finish()
    }

    val itemClickListener:(String) -> Unit = { message ->
        if(NEWUSER.equals(message)) {
            userType = 1
        }
        if (EXISTINGUSER.equals(message)) {
            userType = 2
        }
        sendMessageTxt(message)
    }

    class RecyclerAdapter(private val context: Context, val itemClickListener: (msg: String) -> Unit, arr: List<String>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var objAdapter: List<String> = arr
        private var isClickOnce = false

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_recyclerview, parent, false)

            return LabelViewHolder(view)
        }

        override fun getItemCount(): Int {
            return this.objAdapter.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.buttonObj.setOnClickListener {
                if (isClickOnce) return@setOnClickListener
                itemClickListener(objAdapter[position])
                isClickOnce = true
            }

            holder.itemView.buttonObj.text = objAdapter[position]

            (holder as LabelViewHolder).bind()
        }

        class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            @SuppressLint("SetTextI18n")
            fun bind() {
            }
        }
    }


    fun appendUserText(): FrameLayout {
        val inflater = LayoutInflater.from(this@ChatBotActivity)
        return inflater.inflate(R.layout.user_message, null) as FrameLayout
    }

    fun appendBotText(): FrameLayout {
        val inflater = LayoutInflater.from(this@ChatBotActivity)
        return inflater.inflate(R.layout.bot_message, null) as FrameLayout
    }

    fun onResult(response: DetectIntentResponse?) {
        try {
            if (response != null) {
                var botReply:String=""
                if(response.queryResult.fulfillmentText==" ")
                    botReply= response.queryResult.fulfillmentMessagesList[0].text.textList[0].toString()
                else
                    botReply= response.queryResult.fulfillmentText

//                if (botReply.contains(SPLIT_SENTENCE)) {
//
//                    val botMsg = botReply.split(SPLIT_SENTENCE)
//                    for (i in 0 .. botMsg.size - 1) {
//                        appendText(botMsg[i], BOT)
//                    }
//
//                } else {
//                    appendText(botReply, BOT)
//                }

                appendText(botReply, BOT)
                if(STEPONEANSWER.equals(botReply)) callBot(INIT_STEP2)
                if(STEPTWOANSWER.equals(botReply)) callBot(INIT_STEP3)

            } else {
                appendText(getString(R.string.understoodnot), BOT)
            }
        }catch (e:Exception){
            appendText(getString(R.string.understoodnot), BOT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            SPEECH_INPUT->{
                if(resultCode== Activity.RESULT_OK
                    && data !=null){
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    edittext.text=Editable.Factory.getInstance().newEditable(result[0])
                    sendMessage(send)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(asistan_voice !=null){
            asistan_voice?.stop()
            asistan_voice?.shutdown()
        }
    }

}
