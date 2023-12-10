package com.esg.plogging.activity

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*

class StampAdapter(private val context: Context, private val stampArray: Array<Int>) : BaseAdapter() {
    override fun getCount(): Int = stampArray.size

    override fun getItem(position: Int): Any = stampArray[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView
        if (convertView == null) {
            imageView = ImageView(context)

            val imageParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                210 // 또는 원하는 높이
            )

            imageParams.setMargins(15,15,15,15)

            imageView.layoutParams = imageParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setPadding(8, 8, 8, 8)
        } else {
            imageView = convertView as ImageView
        }

        imageView.setImageResource(stampArray[position])
        return imageView
    }
}

