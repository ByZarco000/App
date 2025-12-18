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
    private val onSelectionChanged: (selectedCount: Int, selectedUris: List<Uri>) -> Unit
) : RecyclerView.Adapter<GlideImageAdapter.ImageViewHolder>() {

    private val selectedUris = mutableListOf<Uri>()

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        // Mostrar número si está seleccionado
        val index = selectedUris.indexOf(uri)
        holder.tvNumber.visibility = if (index >= 0) View.VISIBLE else View.GONE
        holder.tvNumber.text = (index + 1).toString()

        holder.imageView.setOnClickListener {
            if (selectedUris.contains(uri)) {
                selectedUris.remove(uri)
            } else {
                selectedUris.add(uri)
            }
            notifyDataSetChanged()
            onSelectionChanged(selectedUris.size, selectedUris)
        }
    }

    override fun getItemCount(): Int = images.size
}
