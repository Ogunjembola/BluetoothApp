package com.appevolutionng.bluetooth.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.io.Closeable

class BroadcastReceiverDelegator (
    private val context: Context,
    private val listener: BluetoothDiscoveryDeviceListener,
    bluetooth: BluetoothController
    ) : BroadcastReceiver(), Closeable {

        private val TAG = "BroadcastReceiver"

        init {
            listener.setBluetoothController(bluetooth)

            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            }
            context.registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = it.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                     /*   Log.d(TAG, "Device discovered! ${device?.let { it1 ->
                            BluetoothController.deviceToString(
                                it1
                            )
                        }}")*/
                        device?.let { d -> listener.onDeviceDiscovered(d) }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Log.d(TAG, "Discovery ended.")
                        listener.onDeviceDiscoveryEnd()
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        Log.d(TAG, "Bluetooth state changed.")
                        listener.onBluetoothStatusChanged()
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        Log.d(TAG, "Bluetooth bonding state changed.")
                        listener.onDevicePairingEnded()
                    }
                    else -> {
                        // Does nothing.
                    }
                }
            }
        }

        fun onDeviceDiscoveryStarted() {
            listener.onDeviceDiscoveryStarted()
        }

        fun onDeviceDiscoveryEnd() {
            listener.onDeviceDiscoveryEnd()
        }

        fun onBluetoothTurningOn() {
            listener.onBluetoothTurningOn()
        }

        override fun close() {
            context.unregisterReceiver(this)
        }
    }
