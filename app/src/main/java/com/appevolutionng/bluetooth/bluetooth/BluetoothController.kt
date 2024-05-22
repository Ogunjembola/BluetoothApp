package com.appevolutionng.bluetooth.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.Closeable


class  BluetoothController(
    private val context: Activity,
    private val bluetooth: BluetoothAdapter,
    private val listener: BluetoothDiscoveryDeviceListener
) : Closeable {

    private val TAG = "BluetoothManager"
    private val broadcastReceiverDelegator = BroadcastReceiverDelegator(context, listener, this)
    private var bluetoothDiscoveryScheduled = false
    private var boundingDevice: BluetoothDevice? = null


    fun isBluetoothEnabled(): Boolean {
        return bluetooth.isEnabled
    }

    fun startDiscovery() {
        broadcastReceiverDelegator.onDeviceDiscoveryStarted()

        // This line of code is very important. In Android >= 6.0 you have to ask for the runtime
        // permission as well in order for the discovery to get the devices ids. If you don't do
        // this, the discovery won't find any device.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }

        // If another discovery is in progress, cancels it before starting the new one.
        if (bluetooth.isDiscovering) {
            bluetooth.cancelDiscovery()
        }

        // Tries to start the discovery. If the discovery returns false, this means that the
        // bluetooth has not started yet.
        Log.d(TAG, "Bluetooth starting discovery.")
        if (!bluetooth.startDiscovery()) {
            Toast.makeText(context, "Error while starting device discovery!", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "StartDiscovery returned false. Maybe Bluetooth isn't on?")

            // Ends the discovery.
            broadcastReceiverDelegator.onDeviceDiscoveryEnd()
        }
    }


    fun turnOnBluetooth() {
        Log.d(TAG, "Enabling Bluetooth.")
        broadcastReceiverDelegator.onBluetoothTurningOn()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetooth.enable()
    }

    fun pair(device: BluetoothDevice): Boolean {
        // Stops the discovery and then creates the pairing.
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false
        }

        if (bluetooth.isDiscovering) {
            Log.d(TAG, "Bluetooth cancelling discovery.")
            bluetooth.cancelDiscovery()
        }
        Log.d(TAG, "Bluetooth bonding with device: " + deviceToString(device))
        val outcome = device.createBond()
        Log.d(TAG, "Bounding outcome : $outcome")

        // If the outcome is true, we are bounding with this device.
        if (outcome == true) {
            boundingDevice = device
        }
        return outcome
    }

    @SuppressLint("MissingPermission")
    fun isAlreadyPaired(device: BluetoothDevice?): Boolean {
        return bluetooth.bondedDevices.contains(device)
    }


    override fun close() {
        broadcastReceiverDelegator.close()
    }

    fun isDiscovering(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false
        } else {
            bluetooth.isDiscovering
        }
    }

    fun cancelDiscovery() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle the case where permission is not granted.
            // For example, you can request permissions here.
            // ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.BLUETOOTH_SCAN), requestCode)
            return
        }
        bluetooth.cancelDiscovery()
        broadcastReceiverDelegator.onDeviceDiscoveryEnd()
    }

    fun turnOnBluetoothAndScheduleDiscovery() {
        bluetoothDiscoveryScheduled = true
        turnOnBluetooth()
    }

    fun onBluetoothStatusChanged() {
        if (bluetoothDiscoveryScheduled) {
            when (bluetooth.state) {
                BluetoothAdapter.STATE_ON -> {
                    Log.d(TAG, "Bluetooth successfully enabled, starting discovery")
                    startDiscovery()
                    bluetoothDiscoveryScheduled = false
                }

                BluetoothAdapter.STATE_OFF -> {
                    Log.d(TAG, "Error while turning Bluetooth on.")
                    Toast.makeText(context, "Error while turning Bluetooth on.", Toast.LENGTH_SHORT)
                        .show()
                    bluetoothDiscoveryScheduled = false
                }

                else -> {
                }
            }
        }
    }

    fun getPairingDeviceStatus(): Int {
        return boundingDevice?.let { device ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Handle the case where permission is not granted, for example, return a default value.
                -1 // Default value indicating permission not granted
            } else {
                val bondState = device.bondState
                if (bondState != BluetoothDevice.BOND_BONDING) {
                    boundingDevice = null
                }
                bondState
            }
        } ?: throw IllegalStateException("No device currently bounding")
    }

    fun getPairingDeviceName(context: Context): String {
        return boundingDevice?.let { getDeviceName(context, it) } ?: ""
    }

    fun isPairingInProgress(): Boolean {
        return boundingDevice != null
    }

    fun getBoundingDevice(): BluetoothDevice? {
        return boundingDevice
    }

    fun getDeviceName(context: Context, device: BluetoothDevice): String {
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle the case where permission is not granted, for example, return a default value.
            "Permission not granted"
        } else {
            device.name ?: device.address
        }
    }

    @SuppressLint("MissingPermission")
    fun deviceToString(device: BluetoothDevice): String? {
        return "[Address: " + device.address + ", Name: " + device.name + "]"
    }

}