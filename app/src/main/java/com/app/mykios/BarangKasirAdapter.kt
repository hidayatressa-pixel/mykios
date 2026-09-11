package com.app.mykios

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.TextView

import android.view.inputmethod.EditorInfo

class BarangKasirAdapter(
    private val context: Context,
    private var listBarang: List<Barang>,
    private val onQtyChanged: (Barang, Int) -> Unit
) : BaseAdapter() {

    private val qtyMap = mutableMapOf<Int, Int>() // barangId -> quantity

    fun updateData(newList: List<Barang>) {
        listBarang = newList
        notifyDataSetChanged()
    }

    fun clearQty() {
        qtyMap.clear()
        notifyDataSetChanged()
    }

    fun syncQty(newKeranjang: Map<Int, Int>) {
        qtyMap.clear()
        qtyMap.putAll(newKeranjang)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = listBarang.size
    override fun getItem(position: Int): Any = listBarang[position]
    override fun getItemId(position: Int): Long = listBarang[position].id?.toLong() ?: position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_barang_kasir, parent, false)
        val barang = listBarang[position]

        val tvNama = view.findViewById<TextView>(R.id.tvNamaBarang)
        val tvVariant = view.findViewById<TextView>(R.id.tvVariant)
        val tvHarga = view.findViewById<TextView>(R.id.tvHargaBarang)
        val tvQty = view.findViewById<TextView>(R.id.tvQty)
        val btnPlus = view.findViewById<View>(R.id.btnPlus)
        val btnMinus = view.findViewById<View>(R.id.btnMinus)

        tvNama.text = barang.nama
        tvHarga.text = CurrencyUtils.formatRupiah(barang.harga)

        // Variant Info
        val variantParts = mutableListOf<String>()
        if (!barang.warna.isNullOrEmpty()) variantParts.add("Warna: ${barang.warna}")
        if (!barang.ukuran.isNullOrEmpty()) variantParts.add("Ukuran: ${barang.ukuran}")
        if (!barang.satuan.isNullOrEmpty()) variantParts.add("Satuan: ${barang.satuan}")

        if (variantParts.isNotEmpty()) {
            tvVariant.visibility = View.VISIBLE
            tvVariant.text = variantParts.joinToString(" | ")
        } else {
            tvVariant.visibility = View.GONE
        }

        val currentQty = qtyMap[barang.id] ?: 0
        tvQty.text = currentQty.toString()

        btnPlus.setOnClickListener {
            val newQty = (qtyMap[barang.id] ?: 0) + 1
            qtyMap[barang.id!!] = newQty
            tvQty.text = newQty.toString()
            onQtyChanged(barang, newQty)
        }

        btnMinus.setOnClickListener {
            val current = qtyMap[barang.id] ?: 0
            if (current > 0) {
                val newQty = current - 1
                qtyMap[barang.id!!] = newQty
                tvQty.text = newQty.toString()
                onQtyChanged(barang, newQty)
            }
        }

        return view
    }
}