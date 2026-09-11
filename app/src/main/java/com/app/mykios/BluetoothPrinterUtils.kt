package com.app.mykios

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.widget.Toast
import java.io.IOException
import java.io.OutputStream
import java.util.*

object BluetoothPrinterUtils {

    private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices.toList()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(context: Context, device: BluetoothDevice, onConnected: () -> Unit) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            onConnected()
            Toast.makeText(context, "Terhubung ke ${device.name}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printText(text: String) {
        try {
            outputStream?.write(text.toByteArray())
            outputStream?.write("\n".toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun printBarcodeLabel(barang: Barang) {
        // ESC/POS Command for Barcode (Simplified example)
        try {
            outputStream?.write(byteArrayOf(0x1B, 0x40)) // Initialize
            outputStream?.write("${barang.nama}\n".toByteArray())
            outputStream?.write("Harga: ${barang.harga}\n".toByteArray())
            
            // GS k m d1...dk 0 (Barcode print)
            // This is a generic command, specific printers might need specific GS commands
            outputStream?.write(byteArrayOf(0x1D, 0x6B, 0x04)) // CODE39 or CODE128
            outputStream?.write(barang.kodeBarang?.toByteArray() ?: "000".toByteArray())
            outputStream?.write(0)
            
            outputStream?.write("\n\n\n".toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
