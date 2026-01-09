package com.nosferatu.launcher.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nosferatu.launcher.R
import com.nosferatu.launcher.data.EbookEntity

class BookAdapter(
    private var books: List<EbookEntity>,
    private val onBookClick: (EbookEntity) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverImageView: ImageView = itemView.findViewById(R.id.imageViewCover)
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        private val authorTextView: TextView = itemView.findViewById(R.id.textViewAuthor)

        fun bind(book: EbookEntity) {
            titleTextView.text = book.title
            authorTextView.text = book.author ?: "Sconosciuto"

            // --- LOGICA COPERTINA DINAMICA ---
            if (book.coverData != null && book.coverData.isNotEmpty()) {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(book.coverData, 0, book.coverData.size)
                    coverImageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // Se la decodifica fallisce, usa il placeholder
                    coverImageView.setImageResource(R.drawable.ic_book_placeholder)
                }
            } else {
                // Se non ci sono dati, usa il placeholder
                coverImageView.setImageResource(R.drawable.ic_book_placeholder)
            }

            // --- GESTIONE CLICK ---
            itemView.setOnClickListener {
                onBookClick(book)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_cover, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount() = books.size

    fun updateBooks(newBooks: List<EbookEntity>) {
        this.books = newBooks
        notifyDataSetChanged()
    }
}