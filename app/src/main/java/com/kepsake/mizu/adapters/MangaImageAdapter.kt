package com.kepsake.mizu.adapters

import android.content.res.Resources
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MangaImageAdapter(
    private val manga: MangaFile,
    private val mangaPages: List<MangaPage>
) : RecyclerView.Adapter<MangaImageAdapter.ImageViewHolder>() {
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels

    private val itemHeights: List<Int> = mangaPages.map { page ->
        val height = screenWidth / page.aspect_ratio
        return@map height.toInt()
    }

    fun getItemHeights(): List<Int> {
        return itemHeights
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val page = mangaPages[position]
        holder.bind(manga.path, page.page_name)
    }

    override fun getItemCount(): Int {
        return mangaPages.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.mangaImageView)
        private var currentJob: Job? = null

        fun bind(zipPath: String, pageName: String) {
            currentJob?.cancel()

            currentJob = CoroutineScope(Dispatchers.IO).launch {
                val bitmap = extractImageFromZip(zipPath, pageName)
                withContext(Dispatchers.Main) {
                    imageView.load(bitmap) {
                        crossfade(true)
                        placeholder(R.drawable.image)
                        error(R.drawable.image_broken)
                    }
                }
            }
        }

        fun clear() {
            currentJob?.cancel()
            imageView.setImageDrawable(null)
        }
    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }
}
