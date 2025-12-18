package com.example.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GlideImageAdapter(
    private val images: List<Uri>,
    private val onSelectionChanged: (Int, List<Uri>) -> Unit
) : RecyclerView.Adapter<GlideImageAdapter.ImageViewHolder>() {

    private val selectedPositions = linkedMapOf<Int, Uri>()

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivItem)
        val tvNumber: TextView = view.findViewById(R.id.tvNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]

        Glide.with(holder.imageView.context)
            .load(uri)
            .centerCrop()
            .into(holder.imageView)

        bindSelectionState(holder, position)

        holder.itemView.setOnClickListener {
            toggleSelection(position, uri)
        }

        holder.itemView.setOnLongClickListener {
            showPreview(holder.itemView.context, uri)
            true
        }
    }

    override fun getItemCount(): Int = images.size

    // ===================== SELECCIÓN ===================== //
    private fun toggleSelection(position: Int, uri: Uri) {
        if (selectedPositions.containsKey(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions[position] = uri
        }

        notifyItemChanged(position)
        updateSelectionNumbers()

        onSelectionChanged(
            selectedPositions.size,
            selectedPositions.values.toList()
        )
    }

    private fun updateSelectionNumbers() {
        selectedPositions.keys.forEach {
            notifyItemChanged(it)
        }
    }

    private fun bindSelectionState(holder: ImageViewHolder, position: Int) {
        if (selectedPositions.containsKey(position)) {

            val selectionIndex = selectedPositions.keys.indexOf(position) + 1

            holder.tvNumber.visibility = View.VISIBLE
            holder.tvNumber.text = holder.itemView.context.getString(
                R.string.selection_number,
                selectionIndex
            )

            holder.imageView.alpha = 0.6f
        } else {
            holder.tvNumber.visibility = View.GONE
            holder.imageView.alpha = 1f
        }
    }

    // ===================== PREVIEW ===================== //
    private fun showPreview(context: android.content.Context, uri: Uri) {
        val dialog = android.app.Dialog(
            context,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
        dialog.setContentView(R.layout.dialog_image_preview)

        val imageView = dialog.findViewById<ImageView>(R.id.ivPreview)
        Glide.with(context).load(uri).into(imageView)

        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

}//end