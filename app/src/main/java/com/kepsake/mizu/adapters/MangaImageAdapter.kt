package com.kepsake.mizu.adapters

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
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

class MangaPanelAdapter(
    private val manga: MangaFile,
    private val mangaPages: List<MangaPage>,
) : RecyclerView.Adapter<MangaPanelAdapter.MangaViewHolder>() {

    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val pageCache = mutableMapOf<String, Bitmap?>()

    class MangaViewHolder(val binding: MangaPanelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val binding = MangaPanelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MangaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        val page = mangaPages[position]
        val imageHeight = (screenWidth / page.aspect_ratio).toInt()
        val pageKey = mangaPages[position].page_name

        holder.binding.mangaImageViewSmall.layoutParams.height = imageHeight
        holder.binding.mangaImageViewSmall.requestLayout()

        if (pageCache[pageKey] != null) {
            Log.e("ROHAN", "onBindViewHolder: Cache Hit!")
        }

        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = pageCache[pageKey] ?: withContext(Dispatchers.IO) {
                extractImageFromZip(manga.path, page.page_name).let {
                    pageCache[pageKey] = it
                    return@withContext it
                }
            }
            bitmap?.let {
                holder.binding.mangaImageViewSmall.load(it) {
                    crossfade(true)
                    placeholder(R.drawable.image_thin)
                    error(R.drawable.image_broken_thin)
                }
            }
        }
    }

    override fun getItemCount() = mangaPages.size
}