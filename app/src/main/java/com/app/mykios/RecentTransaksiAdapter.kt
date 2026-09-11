package com.app.mykios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RecentTransaksiAdapter(private val list: List<Transaksi>) : RecyclerView.Adapter<RecentTransaksiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTotal: TextView = view.findViewById(android.R.id.text1)
        val tvDetail: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = list[position]
        holder.tvTotal.text = CurrencyUtils.formatRupiah(t.total)
        
        val sdf = SimpleDateFormat("HH:mm • dd MMM", Locale.getDefault())
        holder.tvDetail.text = "${t.metode} • ${sdf.format(Date(t.tanggal))}"
    }

    override fun getItemCount() = list.size
}
