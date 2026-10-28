package com.example.mykisah.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mykisah.databinding.ItemWorldEntryBinding
import com.example.mykisah.entity.WorldEntry

class WorldEntryAdapter(
    private val onItemClicked: (WorldEntry) -> Unit,
    private val onItemLongClicked: (WorldEntry) -> Unit
) : ListAdapter<WorldEntry, WorldEntryAdapter.WorldEntryViewHolder>(DiffCallback()) {

    class WorldEntryViewHolder(private val binding: ItemWorldEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: WorldEntry) {
            binding.tvEntryName.text = entry.name
            binding.tvEntryCategory.text = entry.category
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorldEntryViewHolder {
        val binding =
            ItemWorldEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorldEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorldEntryViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)

        holder.itemView.setOnClickListener {
            onItemClicked(entry)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(entry)
            true
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WorldEntry>() {
        override fun areItemsTheSame(oldItem: WorldEntry, newItem: WorldEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorldEntry, newItem: WorldEntry): Boolean {
            return oldItem == newItem
        }
    }
}