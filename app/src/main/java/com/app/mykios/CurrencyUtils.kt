package com.app.mykios

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    fun formatRupiah(amount: Int): String = formatRupiah(amount.toLong())

    fun formatRupiah(amount: Long): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(amount).replace(",00", "").replace("Rp", "Rp.")
    }

    fun cleanCurrency(formatted: String): String {
        return formatted.replace(".", "").replace("Rp", "").replace(" ", "").trim()
    }
}

class CurrencyTextWatcher(
    private val editText: android.widget.EditText,
    private val onTextChanged: (() -> Unit)? = null
) : android.text.TextWatcher {
    private var current = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: android.text.Editable?) {
        if (s == null) return
        val str = s.toString()
        if (str != current) {
            editText.removeTextChangedListener(this)

            val cleanString = str.replace("[^0-9]".toRegex(), "")

            if (cleanString.isNotEmpty()) {
                try {
                    val parsed = cleanString.toDouble()
                    val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID")).format(parsed)

                    current = formatted
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                } catch (e: Exception) {
                    current = ""
                    editText.setText("")
                }
            } else {
                current = ""
                editText.setText("")
            }

            onTextChanged?.invoke()
            editText.addTextChangedListener(this)
        }
    }
}