package com.kepsake.mizu.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kepsake.mizu.R
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.utils.extractImageFromZip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MangaImageAdapter(private val manga: MangaFile, private val mangaPages: List<MangaPage>) :
    RecyclerView.Adapter<MangaImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val page = mangaPages.get(position)
            val bitmap = extractImageFromZip(manga.path, page.page_name)
            holder.imageView.load(bitmap) {
                crossfade(true) // Enable smooth transitions
                placeholder(R.drawable.image) // Placeholder image while loading
                error(R.drawable.image_broken) // Error image if loading fails
            }
        }
    }

    override fun getItemCount(): Int {
        return mangaPages.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.mangaImageView)
    }
}