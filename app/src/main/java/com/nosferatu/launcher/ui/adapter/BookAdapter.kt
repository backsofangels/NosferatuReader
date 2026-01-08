package com.nosferatu.launcher.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nosferatu.launcher.R
import com.nosferatu.launcher.ReaderActivity
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

            itemView.setOnClickListener { onBookClick(book) }

            coverImageView.setImageResource(R.drawable.ic_book_placeholder)
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, ReaderActivity::class.java).apply {
                    putExtra("FILE_PATH", book.filePath)
                }
                context.startActivity(intent)
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