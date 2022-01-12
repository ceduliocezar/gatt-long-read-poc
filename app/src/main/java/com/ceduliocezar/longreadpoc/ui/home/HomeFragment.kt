package com.ceduliocezar.longreadpoc.ui.home

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ceduliocezar.longreadpoc.MainActivity
import com.ceduliocezar.longreadpoc.databinding.FragmentHomeBinding


private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            this@HomeFragment.onScanResult(callbackType, result)
        }
    }

    private fun onScanResult(callbackType: Int, result: ScanResult?) {
        Log.d(TAG, "onScanResult callbackType:$callbackType, result:${result?.device?.name+result?.device}")
        device = result!!.device
        bluetoothManager.adapter.bluetoothLeScanner.stopScan(this@HomeFragment.scanCallback)
    }

    private lateinit var device: BluetoothDevice
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothGatt: BluetoothGatt
    private val bluetoothCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange status:$status, newState:$newState")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onServicesDiscovered status:$status")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val value = String(characteristic?.value ?: byteArrayOf())
            Log.d(TAG, "onCharacteristicRead value:$value")
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d(
                TAG,
                "onMtuChanged gatt:${gatt?.connectedDevices?.map { it.name }} .mtu:$mtu, status:$status"
            )
        }

    }
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.connectToPeripheral.setOnClickListener {
            connectToPeripheral(BluetoothDevice.TRANSPORT_BREDR, null, false)
        }

        binding.discoverServices.setOnClickListener {
            discoverServices()
        }

        binding.read.setOnClickListener {
            read()
        }
        binding.starScan.setOnClickListener {
            startScan()
        }

        binding.connectToScanned.setOnClickListener {
            connectToPeripheral(BluetoothDevice.TRANSPORT_LE, device, true)
        }

        bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        return root
    }

    private fun startScan() {

        val filter =
            ScanFilter.Builder().setServiceUuid(ParcelUuid(MainActivity.SERVICE_ID)).build()

        val filters = listOf<ScanFilter>(filter)

        bluetoothManager.adapter.bluetoothLeScanner.startScan(
            filters,
            ScanSettings.Builder().build(),
            scanCallback
        )
    }

    private fun discoverServices() {
        bluetoothGatt.discoverServices()
    }

    private fun read() {
        val service = bluetoothGatt.getService(MainActivity.SERVICE_ID)

        val characteristic = service.getCharacteristic(MainActivity.CHARACTERISTIC_ID)

        val readCharacteristic = bluetoothGatt.readCharacteristic(characteristic)

        Log.d(TAG, "Read initiated: $readCharacteristic")
    }

    private fun connectToPeripheral(transport: Int, bluetoothDevice: BluetoothDevice?, autoConnect: Boolean) {


        val peripheral = bluetoothDevice
            ?: bluetoothManager.adapter.bondedDevices.firstOrNull { it.name.contains("Pixel") }
            ?: throw RuntimeException("Device not found")

        bluetoothGatt = peripheral.connectGatt(
            requireContext(),
            autoConnect,
            bluetoothCallback,
            transport
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}