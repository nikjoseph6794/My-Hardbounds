package com.bookshlef.bookshelf.ui


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bookshlef.bookshelf.databinding.ItemScanHistoryBinding
import com.bookshlef.bookshelf.db.ScanHistoryEntry

class ScanHistoryAdapter(
    private val onAddLibrary: (ScanHistoryEntry) -> Unit,
    private val onAddWishlist: (ScanHistoryEntry) -> Unit,
    private val onDelete: (ScanHistoryEntry) -> Unit
) : ListAdapter<ScanHistoryEntry, ScanHistoryAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ScanHistoryEntry>() {
            override fun areItemsTheSame(a: ScanHistoryEntry, b: ScanHistoryEntry) = a.isbn == b.isbn
            override fun areContentsTheSame(a: ScanHistoryEntry, b: ScanHistoryEntry) = a == b
        }
    }

    inner class VH(val b: ItemScanHistoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.b.titleText.text = item.title
        holder.b.authorText.text = item.authors

        holder.b.addLibraryBtn.setOnClickListener { onAddLibrary(item) }
        holder.b.addWishlistBtn.setOnClickListener { onAddWishlist(item) }
        holder.b.deleteBtn.setOnClickListener { onDelete(item) }
    }
}
