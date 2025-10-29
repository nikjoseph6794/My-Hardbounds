package com.example.bookshelf.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshelf.databinding.ItemBookBinding
import com.example.bookshelf.db.WishlistEntry

class WishlistAdapter(
    private val onLongPress: (WishlistEntry) -> Unit
) : ListAdapter<WishlistEntry, WishlistAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WishlistEntry>() {
            override fun areItemsTheSame(o: WishlistEntry, n: WishlistEntry) = o.isbn == n.isbn
            override fun areContentsTheSame(o: WishlistEntry, n: WishlistEntry) = o == n
        }
    }

    inner class VH(val b: ItemBookBinding) : RecyclerView.ViewHolder(b.root) {
        private var current: WishlistEntry? = null
        init {
            // No normal click; only long-press to delete
            b.root.setOnLongClickListener {
                current?.let(onLongPress)
                true
            }
        }
        fun bind(item: WishlistEntry) {
            current = item
            b.title.text = item.title.ifBlank { "—" }
            b.authors.text = item.authors.ifBlank { "—" }
            b.isbn.text = "ISBN: ${item.isbn}"


        }
    }

    override fun onCreateViewHolder(p: ViewGroup, vType: Int): VH =
        VH(ItemBookBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}
