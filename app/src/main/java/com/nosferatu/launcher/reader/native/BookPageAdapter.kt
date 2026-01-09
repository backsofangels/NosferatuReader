package com.nosferatu.launcher.reader.native

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nosferatu.launcher.R

class BookPageAdapter(private var pages: List<CharSequence>) :
    RecyclerView.Adapter<BookPageAdapter.PageViewHolder>() {

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.pageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        // Applichiamo il testo pre-calcolato alla TextView
        holder.textView.text = pages[position]
    }

    override fun getItemCount(): Int = pages.size

    /**
     * Metodo per aggiornare le pagine (es. quando si cambia capitolo o font size)
     */
    fun updateData(newPages: List<CharSequence>) {
        this.pages = newPages
        notifyDataSetChanged()
    }
}