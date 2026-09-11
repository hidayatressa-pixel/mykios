package com.app.mykios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class BarangAdapter(
    private var list: List<Barang>,
    private val onSelectionChanged: (Int) -> Unit,
    private val onItemClick: (Barang) -> Unit
) : RecyclerView.Adapter<BarangAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<Int>() // IDs of selected items
    private var isSelectionMode = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardBarang)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val tvNama: TextView = view.findViewById(R.id.tvNama)
        val tvVariant: TextView = view.findViewById(R.id.tvVariant)
        val tvKode: TextView = view.findViewById(R.id.tvKode)
        val tvHarga: TextView = view.findViewById(R.id.tvHarga)
        val tvStok: TextView = view.findViewById(R.id.tvStok)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barang_stok, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val barang = list[position]
        
        holder.tvNama.text = barang.nama
        holder.tvHarga.text = CurrencyUtils.formatRupiah(barang.harga)
        holder.tvStok.text = "Stok: ${barang.stok} ${barang.satuan ?: "Pcs"}"
        holder.tvKode.text = barang.kodeBarang ?: "-"

        // Variant Info
        if (!barang.warna.isNullOrEmpty() || !barang.ukuran.isNullOrEmpty()) {
            holder.tvVariant.visibility = View.VISIBLE
            val warna = barang.warna ?: "-"
            val ukuran = barang.ukuran ?: "-"
            holder.tvVariant.text = "Warna: $warna | Ukuran: $ukuran"
        } else {
            holder.tvVariant.visibility = View.GONE
        }

        // Selection UI
        holder.checkbox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        val isSelected = selectedItems.contains(barang.id)
        holder.checkbox.isChecked = isSelected
        holder.card.isChecked = isSelected
        
        if (isSelected) {
            holder.card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.primaryContainer))
            holder.card.strokeColor = androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.primary)
        } else {
            holder.card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.surface))
            holder.card.strokeColor = androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.divider)
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(barang.id)
            } else {
                onItemClick(barang)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                toggleSelection(barang.id)
                notifyDataSetChanged()
            }
            true
        }
    }

    private fun toggleSelection(id: Int) {
        if (selectedItems.contains(id)) {
            selectedItems.remove(id)
        } else {
            selectedItems.add(id)
        }
        
        if (selectedItems.isEmpty()) {
            isSelectionMode = false
            notifyDataSetChanged()
        } else {
            notifyDataSetChanged()
        }
        onSelectionChanged(selectedItems.size)
    }

    fun getSelectedCount() = selectedItems.size
    fun getSelectedIds() = selectedItems.toList()
    
    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Barang>) {
        list = newList
        notifyDataSetChanged()
    }
}
