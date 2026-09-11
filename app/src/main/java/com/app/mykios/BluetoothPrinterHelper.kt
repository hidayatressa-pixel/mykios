package com.app.mykios

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.OutputStream
import java.util.*

class BluetoothPrinterHelper(private val context: Context) {

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val printerUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Boolean {
        return try {
            socket = device.createRfcommSocketToServiceRecord(printerUuid)
            socket?.connect()
            outputStream = socket?.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun printText(text: String) {
        try {
            outputStream?.write(text.toByteArray())
            outputStream?.write("\n".toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun printReceipt(toko: String, items: List<String>, total: String) {
        val esc = byteArrayOf(0x1B, 0x40) // Initialize
        val center = byteArrayOf(0x1B, 0x61, 0x01)
        val left = byteArrayOf(0x1B, 0x61, 0x00)
        val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
        val boldOff = byteArrayOf(0x1B, 0x45, 0x00)

        try {
            outputStream?.write(esc)
            outputStream?.write(center)
            outputStream?.write(boldOn)
            outputStream?.write("$toko\n".toByteArray())
            outputStream?.write(boldOff)
            outputStream?.write("--------------------------------\n".toByteArray())
            
            outputStream?.write(left)
            items.forEach {
                outputStream?.write("$it\n".toByteArray())
            }
            
            outputStream?.write("--------------------------------\n".toByteArray())
            outputStream?.write(boldOn)
            outputStream?.write("TOTAL: $total\n".toByteArray())
            outputStream?.write(boldOff)
            outputStream?.write(center)
            outputStream?.write("\nTerima Kasih\n\n\n\n".toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
