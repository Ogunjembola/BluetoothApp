package com.appevolutionng.bluetooth.bluetooth

import android.bluetooth.BluetoothDevice

interface BluetoothDiscoveryDeviceListener {


        fun onDeviceDiscovered(device: BluetoothDevice)

        fun onDeviceDiscoveryStarted()

        fun setBluetoothController(bluetooth: BluetoothController)

        fun onDeviceDiscoveryEnd()

        fun onBluetoothStatusChanged()

        fun onBluetoothTurningOn()

        fun onDevicePairingEnded()
    }

