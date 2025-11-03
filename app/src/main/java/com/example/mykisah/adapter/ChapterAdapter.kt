package com.example.mykisah.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mykisah.databinding.ItemChapterBinding // Pastikan import binding ini benar
import com.example.mykisah.entity.MyChapter

class ChapterAdapter(
    private val onChapterClicked: (MyChapter) -> Unit,
    private val onChapterLongClicked: (MyChapter) -> Unit
) : ListAdapter<MyChapter, ChapterAdapter.ChapterViewHolder>(ChapterDiffCallback()) {

    class ChapterViewHolder(private val binding: ItemChapterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chapter: MyChapter) {
            binding.tvChapterTitle.text = chapter.title
            // Anda bisa tambahkan info lain di sini jika ada di layout
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding =
            ItemChapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = getItem(position)
        holder.bind(chapter)

        holder.itemView.setOnClickListener {
            onChapterClicked(chapter)
        }

        holder.itemView.setOnLongClickListener {
            onChapterLongClicked(chapter)
            return@setOnLongClickListener true
        }
    }

    class ChapterDiffCallback : DiffUtil.ItemCallback<MyChapter>() {
        override fun areItemsTheSame(oldItem: MyChapter, newItem: MyChapter): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MyChapter, newItem: MyChapter): Boolean {
            return oldItem == newItem
        }
    }
}