package com.example.cleancache

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class ImageAdapter(private val context: Context, private val imagePaths: List<String>) : BaseAdapter() {

    override fun getCount(): Int = imagePaths.size

    override fun getItem(position: Int): Any = imagePaths[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView = if (convertView == null) {
            ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(200, 200)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        } else {
            convertView as ImageView
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = 4 // Уменьшаем размер для экономии памяти
        }

        val bitmap = BitmapFactory.decodeFile(imagePaths[position], options)
        imageView.setImageBitmap(bitmap)

        return imageView
    }
}
