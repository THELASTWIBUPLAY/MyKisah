package com.example.mykisah.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mykisah.databinding.ItemMykisahBinding
import com.example.mykisah.entity.MyNovel

// 1. Tambahkan parameter lambda di constructor
class NovelAdapter(
    private val onItemClicked: (MyNovel) -> Unit,
    private val onItemLongClicked: (MyNovel) -> Unit
) : ListAdapter<MyNovel, NovelAdapter.NovelViewHolder>(NovelDiffCallback()) {

    class NovelViewHolder(private val binding: ItemMykisahBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(novel: MyNovel) {
            binding.title.text = novel.title
            binding.description.text = novel.synopsis
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelViewHolder {
        val binding =
            ItemMykisahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NovelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
        val novel = getItem(position)
        holder.bind(novel)

        // 2. Set OnClickListener di sini
        // Saat item (seluruh CardView) di-klik
        holder.itemView.setOnClickListener {
            // Panggil lambda function-nya, kirim data novel yang di-klik
            onItemClicked(novel)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(novel)
            return@setOnLongClickListener true // Penting!
        }
    }

    // DiffCallback tetap sama...
    class NovelDiffCallback : DiffUtil.ItemCallback<MyNovel>() {
        override fun areItemsTheSame(oldItem: MyNovel, newItem: MyNovel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MyNovel, newItem: MyNovel): Boolean {
            return oldItem == newItem
        }
    }
}