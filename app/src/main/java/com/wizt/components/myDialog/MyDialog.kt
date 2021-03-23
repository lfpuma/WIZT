package com.wizt.components.myDialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Environment
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.wizt.R
import com.wizt.activities.*
import com.wizt.activities.CreateLabelActivity.Companion.roundedCon
import com.wizt.common.constants.Constants
import com.wizt.extensions.ImageHelper
import com.wizt.fragments.CoverImageDetailFragment
import com.wizt.fragments.HomeFragment
import com.wizt.models.SuggestionFybe
import com.wizt.utils.DateTimeUtils
import com.wizt.utils.MyToastUtil
import kotlinx.android.synthetic.main.activity_creat_label.*
import kotlinx.android.synthetic.main.chooseoption_dialog.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_share.*
import kotlinx.android.synthetic.main.fragment_share.recyclerView
import kotlinx.android.synthetic.main.imagedetail_dialog.*
import kotlinx.android.synthetic.main.imagedetail_dialog.closeImgBtn
import kotlinx.android.synthetic.main.imagedetail_dialog.imageView
import kotlinx.android.synthetic.main.imagedetail_dialog.tvTitle
import kotlinx.android.synthetic.main.item_label_image.view.*
import kotlinx.android.synthetic.main.item_suggestion_fybe_recycler.view.*
import kotlinx.android.synthetic.main.locationinfo_dialog.*
import kotlinx.android.synthetic.main.reminder_dialog.*
import kotlinx.android.synthetic.main.suggestion_fybe_dialog.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MyDialog {

    companion object {
        lateinit var recyclerView: RecyclerView
        var adapter: RecyclerAdapter? = null

        fun showCustomSuggestionFybeDialog(context: Context, url: String, tags : MutableSet<String>) {

            val mDialogView = LayoutInflater.from(context).inflate(R.layout.suggestion_fybe_dialog, null)
            val mBuilder = AlertDialog.Builder(context)
                .setView(mDialogView)
                .setTitle("")
            val mAlertDialog = mBuilder.show()

            // UI
            var gridLayoutManager: GridLayoutManager
            val tagsArr = ArrayList<SuggestionFybe>()
            for (tag in tags) {
                val aTag = SuggestionFybe("",false)
                aTag.tagName = tag
                tagsArr.add(aTag)
            }

            recyclerView = mAlertDialog.recyclerView
            gridLayoutManager = GridLayoutManager(context, 2)
            recyclerView.layoutManager = gridLayoutManager
            adapter = RecyclerAdapter(context, itemClickListener, tagsArr)
            recyclerView.adapter = adapter

//            Glide.with(context!!)
//                .asBitmap()
//                .load(url)
//                .into(object : CustomTarget<Bitmap>(){
//                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                        val sqRes = ImageHelper.getCroppedImage_Special(resource)
//                        //val round = RoundedBitmapDrawableFactory.create(context!!.resources,sqRes)
//                        //round.cornerRadius = 25.0f
//                        mAlertDialog.imageView.setImageBitmap(sqRes)
//                        mAlertDialog.tvTitle.visibility = View.VISIBLE
//
//                    }
//                    override fun onLoadCleared(placeholder: Drawable?) {
//                    }
//                })

            val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val file = File(filePath, url)
            var bitmap = BitmapFactory.decodeFile(file.path)
            if( bitmap != null) {
                val sqRes = ImageHelper.getCroppedImage_Special(bitmap)
                //val round = RoundedBitmapDrawableFactory.create(context.resources,sqRes)
                //round.cornerRadius = roundedCon
                mAlertDialog.imageView.setImageBitmap(sqRes)
                mAlertDialog.tvTitle.visibility = View.VISIBLE
            }


            mAlertDialog.cancelBtn.setOnClickListener {
                mAlertDialog.dismiss()
            }

            mAlertDialog.addBtn.setOnClickListener {

                val parentActivity = context as CreateLabelActivity

                var count = 0
                for (tag in tagsArr) {
                    if (tag.is_sel) {
                        count++
                        parentActivity.tagContainer.addTag(tag.tagName)
                    }
                }

                if (count == 0) {
                    MyToastUtil.showWarning(context,"No selected Item")
                    return@setOnClickListener
                }
                mAlertDialog.dismiss()
            }


        }

        private val itemClickListener:(Int, SuggestionFybe) -> Unit = { position, suggestionItem ->
            suggestionItem.is_sel = !suggestionItem.is_sel
            adapter!!.notifyDataSetChanged()
        }


        class RecyclerAdapter(private val context: Context, val itemClickListener: (int: Int, suggestionItem: SuggestionFybe) -> Unit, arr: ArrayList<SuggestionFybe>) :
            RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            private var labelThumbUrlAdapter: ArrayList<SuggestionFybe> = arr

            override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.item_suggestion_fybe_recycler, parent, false)

                return SuggestionFybeViewHolder(view)
            }

            override fun getItemCount(): Int {
                return this.labelThumbUrlAdapter.size
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                holder.itemView.setOnClickListener {
                    itemClickListener(position, labelThumbUrlAdapter[position])
                }

                if(labelThumbUrlAdapter[position].is_sel) {
                    holder.itemView.tagName.setBackgroundResource(R.drawable.suggestion_item_background)
                }
                else {
                    holder.itemView.tagName.setBackgroundResource(R.drawable.suggestion_item_background_unsel)
                }
                holder.itemView.tagName.setText(labelThumbUrlAdapter[position].tagName)

                (holder as SuggestionFybeViewHolder).bind()
            }

            class SuggestionFybeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                @SuppressLint("SetTextI18n")
                fun bind() {
                }
            }
        }

        fun showCustomDialog(context: Context, imageUrl: String, title: String, delIndex: Int) {

            val mDialogView = LayoutInflater.from(context).inflate(R.layout.imagedetail_dialog, null)
            val mBuilder = AlertDialog.Builder(context)
                .setView(mDialogView)
                .setTitle("")
            val mAlertDialog = mBuilder.show()

            mAlertDialog.closeImgBtn.setOnClickListener {
                mAlertDialog.dismiss()
            }

            mAlertDialog.tvTitle.visibility = View.GONE
            mAlertDialog.fileImage.visibility = View.GONE
            mAlertDialog.removeImageButton.visibility = View.GONE

            when (delIndex) {
                -2 -> {
                    if (!title.isEmpty()) {
                        mAlertDialog.tvTitle.visibility = View.VISIBLE
                        mAlertDialog.fileImage.visibility = View.VISIBLE
                        mAlertDialog.tvTitle.setText(title)
                    }
                }
                else -> mAlertDialog.removeImageButton.visibility = View.VISIBLE
            }

            mAlertDialog.removeImageButton.setOnClickListener {

                val cusContext = context as CreateLabelActivity

                if (delIndex == -1) {

                    cusContext.arMarkerImageUrl = ""
                    cusContext.imageARMarker.setImageDrawable(null)

                }
                else {
                    cusContext.labelImageURL.removeAt(delIndex)
                    cusContext.labelThumbURL.removeAt(delIndex)
                    cusContext.adapter?.notifyDataSetChanged()
                    cusContext.updateUI()
                }

                mAlertDialog.dismiss()

            }


            if(imageUrl.contains("http")) {
                Glide.with(context!!).load(imageUrl).into(mAlertDialog.imageView)
            } else {
                val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val file = File(filePath, imageUrl)
                var bitmap = BitmapFactory.decodeFile(file.path)
                if( bitmap != null) {
                    mAlertDialog.imageView.setImageBitmap(bitmap)
                }
            }
        }

        fun showReminderDialog(context: Context) {

            val parentactivity = context as CreateLabelActivity

            val mDialogView = LayoutInflater.from(context).inflate(R.layout.reminder_dialog, null)
            val mBuilder = AlertDialog.Builder(context)
                .setView(mDialogView)
                .setTitle("")
            val mAlertDialog = mBuilder.show()


            mAlertDialog.notesTV.text = parentactivity.labelNameTV.text
            mAlertDialog.timeTV.text = parentactivity.reminderTime
            mAlertDialog.dateTV.text = parentactivity.reminderDate

            val reminderT = parentactivity.reminderDate + " " + parentactivity.reminderTime

            mAlertDialog.closeImgBtn.setOnClickListener {
                parentactivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                mAlertDialog.dismiss()
            }

            mAlertDialog.saveBtn1.setOnClickListener {
                parentactivity.labelNameTV.setText(mAlertDialog.notesTV.text)
                parentactivity.reminderTime = mAlertDialog.timeTV.text.toString()
                parentactivity.reminderDate = mAlertDialog.dateTV.text.toString()
                parentactivity.updateReminder()
                parentactivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                mAlertDialog.dismiss()
            }

            mAlertDialog.deleteBtn.setOnClickListener {
                parentactivity.reminderTime = ""
                parentactivity.reminderDate = ""
                parentactivity.labelNameTV.setText("")
                parentactivity.updateReminder()
                parentactivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                mAlertDialog.dismiss()
            }

            mAlertDialog.notesTV.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if(!mAlertDialog.notesTV.text.toString().isEmpty()) {
                        val imm = parentactivity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                        //parentactivity.labelNameTV.setText(mAlertDialog.notesTV.text)
                        return@OnEditorActionListener true
                    }
                }
                false
            })

            val cal = Calendar.getInstance()
            if(!parentactivity.reminderTime.isEmpty()) {
                cal.setTime(DateTimeUtils().getDateFromLString(reminderT))
            }

            val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)

                mAlertDialog.timeTV.text =  SimpleDateFormat("hh:mm a").format(cal.time)
                //parentactivity.reminderTime = mAlertDialog.timeTV.text.toString()
                //parentactivity.labelNameTV.setText(mAlertDialog.notesTV.text)
                //parentactivity.updateReminder()
            }

            mAlertDialog.timeTV.setOnClickListener {

                TimePickerDialog(context, R.style.timepicker, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
            }


            val c = Calendar.getInstance()
            if(!parentactivity.reminderDate.isEmpty()) {
                c.setTime(DateTimeUtils().getDateFromLString(reminderT))
            }
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(context, R.style.datepicker, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                // Display Selected date in textbox
                mAlertDialog.dateTV.setText("" + (monthOfYear + 1) + "/" + dayOfMonth + "/" + year)
                //parentactivity.reminderDate = "" + (monthOfYear + 1) + "/" + dayOfMonth + "/" + year
                //parentactivity.labelNameTV.setText(mAlertDialog.notesTV.text)
                //parentactivity.updateReminder()

            }, year, month, day)

            mAlertDialog.dateTV.setOnClickListener {
                dpd.show()
            }

        }

        fun showLocationInfoDialog(context: Context) {

            val parentactivity = context as SetARMarkerActivity

            val mDialogView = LayoutInflater.from(context).inflate(R.layout.locationinfo_dialog, null)
            val mBuilder = AlertDialog.Builder(context)
                .setView(mDialogView)
                .setTitle("")
            val mAlertDialog = mBuilder.show()
            mAlertDialog.setCancelable(false)

            mAlertDialog.closeImgBtn.setOnClickListener {
                mAlertDialog.dismiss()
            }

            mAlertDialog.roomNameTV.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if(mAlertDialog.roomNameTV.text.toString().isEmpty()) {
                        val imm = parentactivity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                        return@OnEditorActionListener true
                    } else {
                        parentactivity.isFinalSave = true
                        parentactivity.roomName = mAlertDialog.roomNameTV.text.toString()
                        mAlertDialog.dismiss()
                    }
                }
                false
            })
        }

        fun showChooseOptionDialog(context: Context) {

            val parentactivity = context as MainActivity

            val mDialogView = LayoutInflater.from(context).inflate(R.layout.chooseoption_dialog, null)
            val mBuilder = AlertDialog.Builder(context)
                .setView(mDialogView)
                .setTitle("")
            val mAlertDialog = mBuilder.show()

            mAlertDialog.closeImgBtn.setOnClickListener {
                mAlertDialog.dismiss()
            }

            mAlertDialog.newLabelBtn.setOnClickListener {
                mAlertDialog.dismiss()
                val intent = Intent(context, CreateLabelActivity::class.java)
                intent.putExtra(Constants.EXTRA_CREATE_LABEL_ACTIVITY_TYPE, true)
                intent.putExtra(Constants.EXTRA_CREATE_FYBE_ACTIVITY_TYPE, false)
                parentactivity.startActivity(intent)
            }

            mAlertDialog.fybeBtn.setOnClickListener {
                mAlertDialog.dismiss()
                val intent = Intent(context, CreateLabelActivity::class.java)
                intent.putExtra(Constants.EXTRA_CREATE_LABEL_ACTIVITY_TYPE, true)
                intent.putExtra(Constants.EXTRA_CREATE_FYBE_ACTIVITY_TYPE, true)
                parentactivity.startActivity(intent)
            }


        }

        fun showTrainObjectImageDialog(context: Context, imageUrl: String, title: String, delIndex: Int) {

            val mDialogView = LayoutInflater.from(context).inflate(R.layout.imagedetail_dialog, null)
            val mBuilder = AlertDialog.Builder(context)
                .setView(mDialogView)
                .setTitle("")
            val mAlertDialog = mBuilder.show()

            mAlertDialog.closeImgBtn.setOnClickListener {
                mAlertDialog.dismiss()
            }

            mAlertDialog.tvTitle.visibility = View.GONE
            mAlertDialog.fileImage.visibility = View.GONE
            mAlertDialog.removeImageButton.visibility = View.GONE

            when (delIndex) {
                -2 -> {
                    if (!title.isEmpty()) {
                        mAlertDialog.tvTitle.visibility = View.VISIBLE
                        mAlertDialog.fileImage.visibility = View.VISIBLE
                        mAlertDialog.tvTitle.setText(title)
                    }
                }
                else -> mAlertDialog.removeImageButton.visibility = View.VISIBLE
            }

            mAlertDialog.removeImageButton.setOnClickListener {

                val cusContext = context as CreateTrainObjectActivity

                cusContext.labelImageURL.removeAt(delIndex)
                cusContext.adapter?.notifyDataSetChanged()

                mAlertDialog.dismiss()

            }

            if(imageUrl.contains("http")) {
                Glide.with(context!!).load(imageUrl).into(mAlertDialog.imageView)
            } else {
                val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val file = File(filePath, imageUrl)
                var bitmap = BitmapFactory.decodeFile(file.path)
                if( bitmap != null) {
                    mAlertDialog.imageView.setImageBitmap(bitmap)
                }
            }
        }

    }




}