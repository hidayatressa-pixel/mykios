package com.app.mykios

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class HutangAdapter(
    private val context: Context,
    private var listHutang: List<Hutang>,
    private val onAction: (Hutang, ActionType) -> Unit
) : BaseAdapter() {

    enum class ActionType { TAGIH, LUNAS, TAMBAH_TEMPO }

    fun updateData(newList: List<Hutang>) {
        listHutang = newList
        notifyDataSetChanged()
    }

    override fun getCount(): Int = listHutang.size
    override fun getItem(position: Int): Any = listHutang[position]
    override fun getItemId(position: Int): Long = listHutang[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_hutang, parent, false)
        val hutang = listHutang[position]

        view.findViewById<TextView>(R.id.tvNamaHutang).text = hutang.namaPelanggan
        view.findViewById<TextView>(R.id.tvJumlahHutang).text = CurrencyUtils.formatRupiah(hutang.jumlah)
        
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        view.findViewById<TextView>(R.id.tvJatuhTempo).text = "Jatuh Tempo: ${sdf.format(Date(hutang.jatuhTempo))}"

        view.findViewById<MaterialButton>(R.id.btnTagihWa).setOnClickListener {
            onAction(hutang, ActionType.TAGIH)
        }

        view.findViewById<MaterialButton>(R.id.btnLunas).setOnClickListener {
            onAction(hutang, ActionType.LUNAS)
        }

        view.findViewById<MaterialButton>(R.id.btnTambahTempo).setOnClickListener {
            onAction(hutang, ActionType.TAMBAH_TEMPO)
        }

        return view
    }
}