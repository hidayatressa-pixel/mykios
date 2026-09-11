package com.app.mykios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class PengeluaranAdapter(
    private var list: List<Pengeluaran>
) : RecyclerView.Adapter<PengeluaranAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKet: TextView = view.findViewById(android.R.id.text1)
        val tvDetail: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvKet.text = item.keterangan
        
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(item.tanggal))
        
        holder.tvDetail.text = "${CurrencyUtils.formatRupiah(item.jumlah)} • $dateStr"
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Pengeluaran>) {
        list = newList
        notifyDataSetChanged()
    }
}
