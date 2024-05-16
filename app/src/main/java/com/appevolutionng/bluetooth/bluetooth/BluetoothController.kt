package com.appevolutionng.bluetooth.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pub.devrel.easypermissions.EasyPermissions
import java.io.Closeable


class  BluetoothController(
    private val contextt: Activity,
    private val bluetooth: BluetoothAdapter,
    private val listener: BluetoothDiscoveryDeviceListener
) : Closeable {

    private val TAG = "BluetoothManager"
    private val broadcastReceiverDelegator = BroadcastReceiverDelegator(contextt, listener, this)
    private var bluetoothDiscoveryScheduled = false
    private var boundingDevice: BluetoothDevice? = null

    fun isBluetoothEnabled(): Boolean {
        return bluetooth.isEnabled
    }

    fun startDiscovery() {
        broadcastReceiverDelegator.onDeviceDiscoveryStarted()
        fun startDiscovery() {
            // Check if Bluetooth is supported
            if (bluetooth == null) {
                // Bluetooth is not supported on this device
                Log.e(TAG, "BluetoothAdapter is null. Bluetooth might not be supported on this device.")
                Toast.makeText(
                    contextt,
                    "Bluetooth is not supported on this device.",
                    Toast.LENGTH_SHORT
                ).show()
                broadcastReceiverDelegator.onDeviceDiscoveryEnd()
                return
            }

            // Continue with Bluetooth discovery process...
        }


        if (!bluetooth.isEnabled) {
            Log.d(TAG, "Bluetooth is not enabled. Prompting user to enable it.")
            // Prompt the user to enable Bluetooth
            // You can use startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), requestCode)
            // Make sure to handle the result of the enable request in your activity
            // Alternatively, you can provide instructions to the user to manually enable Bluetooth

            // You might also choose to handle this case differently based on your app's requirements
            Toast.makeText(
                contextt,
                "Bluetooth is not enabled. Please enable Bluetooth and try again.",
                Toast.LENGTH_SHORT
            ).show()
            broadcastReceiverDelegator.onDeviceDiscoveryEnd()
            return
        }

        if (bluetooth.isDiscovering) {
            bluetooth.cancelDiscovery()
        }

        Log.d(TAG, "Bluetooth starting discovery.")

        // Check if Bluetooth adapter is null
        if (bluetooth == null) {
            Log.e(TAG, "BluetoothAdapter is null. Bluetooth might not be supported on this device.")
            Toast.makeText(
                contextt,
                "Bluetooth is not supported on this device.",
                Toast.LENGTH_SHORT
            ).show()
            broadcastReceiverDelegator.onDeviceDiscoveryEnd()
            return
        }

        // Check if Bluetooth adapter is in the STATE_ON state
        if (bluetooth.state != BluetoothAdapter.STATE_ON) {
            Log.d(TAG, "Bluetooth is not in the STATE_ON state. Waiting for Bluetooth to be enabled.")
            // You might want to handle this case differently based on your app's requirements
            // For example, you can wait for Bluetooth to be enabled and then start discovery
            // or prompt the user to enable Bluetooth and wait for the result
            return
        }

        // Start discovery
        if (ActivityCompat.checkSelfPermission(
                contextt,
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
            return
        }
        if (!bluetooth.startDiscovery()) {
            Toast.makeText(
                contextt,
                "Error while starting device discovery!",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "StartDiscovery returned false. Maybe Bluetooth isn't on?")
            broadcastReceiverDelegator.onDeviceDiscoveryEnd()
        }
    }



    fun turnOnBluetooth() {
        Log.d(TAG, "Enabling Bluetooth.")
        broadcastReceiverDelegator.onBluetoothTurningOn()

        if (ContextCompat.checkSelfPermission(
                contextt,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // Permission not granted, request it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    contextt,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    2
                )
            }
            return
        }

        // Permission granted, enable Bluetooth
        bluetooth.enable()
    }

    fun pair(device: BluetoothDevice): Boolean {
        if (ActivityCompat.checkSelfPermission(
                contextt,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle the case where permission is not granted, for example, return false.
            return false
        }
        if (bluetooth.isDiscovering) {
            Log.d(TAG, "Bluetooth cancelling discovery.")
            bluetooth.cancelDiscovery()
        }

        val outcome = device.createBond()
        Log.d(TAG, "Bounding outcome : $outcome")
        if (outcome) {
            boundingDevice = device
        }
        return outcome
    }

    fun isAlreadyPaired(device: BluetoothDevice): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                contextt,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle the case where permission is not granted, for example, return false.
            false
        } else {
            bluetooth.bondedDevices.contains(device)
        }
    }

    override fun close() {
        broadcastReceiverDelegator.close()
    }

    fun isDiscovering(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                contextt,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle the case where permission is not granted, for example, return false.
            false
        } else {
            bluetooth.isDiscovering
        }
    }

    fun cancelDiscovery() {
        if (ActivityCompat.checkSelfPermission(
                contextt,
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
                    Toast.makeText(contextt, "Error while turning Bluetooth on.", Toast.LENGTH_SHORT)
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
                    contextt,
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
}