package com.kepsake.mizu.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kepsake.mizu.R
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.databinding.MangaPanelBinding
import com.kepsake.mizu.utils.extractImageFromZip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class MangaPage(val imagePath: String, val aspectRatio: Float)

class MangaPanelAdapter(
    private val manga: MangaFile,
    private val mangaPages: List<MangaPage>,
) : RecyclerView.Adapter<MangaPanelAdapter.MangaViewHolder>() {

    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels

    class MangaViewHolder(val binding: MangaPanelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val binding = MangaPanelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MangaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        val page = mangaPages[position]

        // Calculate height using aspect ratio
        val imageHeight = (screenWidth / page.aspect_ratio).toInt()

        // Set the precomputed height
        holder.binding.mangaImageViewSmall.layoutParams.height = imageHeight
        holder.binding.mangaImageViewSmall.requestLayout()

        CoroutineScope(Dispatchers.Main).launch {
            val bitmap =
                withContext(Dispatchers.IO) { extractImageFromZip(manga.path, page.page_name) }
            bitmap?.let {
                holder.binding.mangaImageViewSmall.load(it) {
                    crossfade(true)
                    placeholder(R.drawable.image)
                    error(R.drawable.image_broken)
                }
            }
        }
    }

    override fun getItemCount() = mangaPages.size
}


//class MangaImageAdapter(
//    private val manga: MangaFile,
//    private val mangaPages: List<MangaPage>
//) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//    class MangaPanelHolder(val binding: MangaPanelBinding) :
//        RecyclerView.ViewHolder(binding.root)
//
//    companion object {
//        private const val VIEW_TYPE_SMALL = 1
//        private const val VIEW_TYPE_LARGE = 2
//    }
//
//
//    override fun getItemViewType(position: Int): Int {
//        return if (mangaPages[position].aspect_ratio > 1) VIEW_TYPE_SMALL else VIEW_TYPE_LARGE
//    }
//
//    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
//
//    private val itemHeights: List<Int> = mangaPages.map { page ->
//        val height = screenWidth / page.aspect_ratio
//        return@map height.toInt()
//    }
//
//    fun getItemHeights(): List<Int> {
//        return itemHeights
//    }
//
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaPanelHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.manga_panel, parent, false)
//        return MangaPanelHolder(view)
//    }
//
//
//    override fun onBindViewHolder(holder: MangaPanelHolder, position: Int) {
//        val item = mangaPages[position]
//        val imageHeight = (screenWidth / item.aspect_ratio).toInt()
//
//        when (holder) {
//            is LargeViewHolder -> holder.bind(manga.path, item.page_name)
//            is SmallViewHolder -> holder.bind(manga.path, item.page_name)
//        }
//    }
//
//
//    override fun getItemCount(): Int {
//        return mangaPages.size
//    }
//
//    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val imageView: ImageView = itemView.findViewById(R.id.mangaImageViewSmall)
//
//        private var currentJob: Job? = null
//
//        fun bind(zipPath: String, pageName: String) {
//            currentJob?.cancel()
//
//            currentJob = CoroutineScope(Dispatchers.IO).launch {
//                val bitmap = extractImageFromZip(zipPath, pageName)
//                withContext(Dispatchers.Main) {
//                    imageView.load(bitmap) {
//                        crossfade(true)
//                        placeholder(R.drawable.image)
//                        error(R.drawable.image_broken)
//                    }
//                }
//            }
//        }
//    }
//
//    class LargeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val imageView: ImageView = itemView.findViewById(R.id.mangaImageViewLarge)
//
//        private var currentJob: Job? = null
//
//        fun bind(zipPath: String, pageName: String) {
//            currentJob?.cancel()
//
//            currentJob = CoroutineScope(Dispatchers.IO).launch {
//                val bitmap = extractImageFromZip(zipPath, pageName)
//                withContext(Dispatchers.Main) {
//                    imageView.load(bitmap) {
//                        crossfade(true)
//                        placeholder(R.drawable.image)
//                        error(R.drawable.image_broken)
//                    }
//                }
//            }
//        }
//    }
//
//}
