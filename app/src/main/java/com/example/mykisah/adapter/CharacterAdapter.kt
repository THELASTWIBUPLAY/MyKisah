package com.example.mykisah.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mykisah.databinding.ItemCharacterBinding // Import binding
import com.example.mykisah.entity.MyCharacter

class CharacterAdapter(
    private val onCharacterClicked: (MyCharacter) -> Unit,
    private val onCharacterLongClicked: (MyCharacter) -> Unit
) : ListAdapter<MyCharacter, CharacterAdapter.CharacterViewHolder>(CharacterDiffCallback()) {

    class CharacterViewHolder(private val binding: ItemCharacterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(character: MyCharacter) {
            binding.tvCharacterName.text = character.name
            binding.tvCharacterDesc.text = character.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val binding =
            ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CharacterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val character = getItem(position)
        holder.bind(character)

        holder.itemView.setOnClickListener {
            onCharacterClicked(character)
        }

        holder.itemView.setOnLongClickListener {
            onCharacterLongClicked(character)
            return@setOnLongClickListener true
        }
    }

    class CharacterDiffCallback : DiffUtil.ItemCallback<MyCharacter>() {
        override fun areItemsTheSame(oldItem: MyCharacter, newItem: MyCharacter): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MyCharacter, newItem: MyCharacter): Boolean {
            return oldItem == newItem
        }
    }
}