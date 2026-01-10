package com.nosferatu.launcher.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nosferatu.launcher.R
import com.nosferatu.launcher.data.EbookEntity

class BookAdapter(
    private var books: List<EbookEntity>,
    private val onBookClick: (EbookEntity) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImageView: ImageView = itemView.findViewById(R.id.imageViewCover)
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val authorTextView: TextView = itemView.findViewById(R.id.textViewAuthor)

        fun bind(book: EbookEntity) {
            titleTextView.text = book.title
            authorTextView.text = book.author ?: "Sconosciuto"

            if (book.coverData != null && book.coverData.isNotEmpty()) {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(book.coverData, 0, book.coverData.size)
                    coverImageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    coverImageView.setImageResource(R.drawable.ic_book_placeholder)
                }
            } else {
                coverImageView.setImageResource(R.drawable.ic_book_placeholder)
            }

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

    override fun onViewRecycled(holder: BookViewHolder) {
        super.onViewRecycled(holder)
        holder.coverImageView.setImageDrawable(null)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount() = books.size

    fun updateBooks(newBooks: List<EbookEntity>) {
        val diffResult = DiffUtil.calculateDiff(BookDiffCallback(this.books, newBooks))
        this.books = newBooks
        diffResult.dispatchUpdatesTo(this)
    }

    class BookDiffCallback(
        private val oldList: List<EbookEntity>,
        private val newList: List<EbookEntity>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos].id == newList[newPos].id
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos] == newList[newPos]
        }
    }
}