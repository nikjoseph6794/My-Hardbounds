package com.bookshlef.bookshelf.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bookshlef.bookshelf.R
import com.bookshlef.bookshelf.databinding.ItemBookBinding
import com.bookshlef.bookshelf.db.Book

// NEW: pass an onClick callback from the Activity
class BookAdapter(
    private val onClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val b: ItemBookBinding,
        private val onClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {

        private var current: Book? = null

        init {
            b.root.setOnClickListener {
                current?.let(onClick)
            }
        }

        fun bind(item: Book) {
            current = item
            b.title.text = item.title.ifBlank { "—" }
            b.authors.text = if (item.authors.isBlank()) "—" else item.authors
            b.isbn.text = "ISBN: ${item.isbn}"

            // --- Rounded Read/Unread Tag ---
            val ctx = b.root.context
            val bg = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.bg_tag_round)?.mutate()

            if (item.isRead) {
                b.readStatusTag.text = "Read"
                bg?.setTint(ctx.getColor(android.R.color.holo_green_dark))
            } else {
                b.readStatusTag.text = "Unread"
                bg?.setTint(ctx.getColor(android.R.color.holo_blue_dark))
            }

            b.readStatusTag.background = bg
        }

    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book) = oldItem.isbn == newItem.isbn
            override fun areContentsTheSame(oldItem: Book, newItem: Book) = oldItem == newItem
        }
    }
}
