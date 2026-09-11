package com.app.mykios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LowStockAdapter(private val list: List<Barang>) : RecyclerView.Adapter<LowStockAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvLowStockNama)
        val tvStok: TextView = view.findViewById(R.id.tvLowStockQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_low_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val barang = list[position]
        holder.tvNama.text = barang.nama
        holder.tvStok.text = "Sisa: ${barang.stok}"
    }

    override fun getItemCount() = list.size
}
