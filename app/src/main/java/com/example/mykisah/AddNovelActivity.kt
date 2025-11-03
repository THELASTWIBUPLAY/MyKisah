package com.example.mykisah

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mykisah.databinding.ActivityAddNovelBinding
import com.example.mykisah.viewmodel.MyKisahViewModel

class AddNovelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNovelBinding

    // Gunakan ViewModel yang sama
    private val viewModel: MyKisahViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNovelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSaveNovel.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val synopsis = binding.etSynopsis.text.toString().trim()

            if (title.isNotEmpty() && synopsis.isNotEmpty()) {
                viewModel.addNovel(title, synopsis)
                Toast.makeText(this, "Novel disimpan!", Toast.LENGTH_SHORT).show()
                finish() // Tutup activity dan kembali ke list
            } else {
                Toast.makeText(this, "Judul dan Sinopsis tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }
}