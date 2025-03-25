package fr.isen.faury.androidsmartdevice

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import fr.isen.faury.androidsmartdevice.R
import android.widget.ListView
import android.widget.TextView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID


class ConnectActivity : AppCompatActivity() {

    private lateinit var connectButton: Button
    private lateinit var led1Button: Button
    private lateinit var led2Button: Button
    private lateinit var led3Button: Button
    private lateinit var connectionStatusText: TextView
    private var deviceAddress: String? = null
    private var bluetoothGatt: BluetoothGatt? = null

    companion object {
        private var bluetoothGatt: BluetoothGatt? = null
        private const val REQUEST_BLUETOOTH_CONNECT = 1

        // ⚡ UUIDs à modifier si nécessaire
        val service = bluetoothGatt?.getService(UUID.fromString("0000feed-cc7a-482a-984a-7f2ed5b3e58f"))
        val characteristic = service?.getCharacteristic(UUID.fromString("0000abcd-8e22-4541-9d4c-21edae82ed19"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        // Initialisation des boutons et du texte
        connectButton = findViewById(R.id.connectButton)
        led1Button = findViewById(R.id.led1Button)
        led2Button = findViewById(R.id.led2Button)
        led3Button = findViewById(R.id.led3Button)
        connectionStatusText = findViewById(R.id.connectionStatusText)

        deviceAddress = intent.getStringExtra("deviceAddress")
        connectionStatusText.text = "Connexion en cours..."

        connectButton.setOnClickListener {
            connectToDevice()
        }

        // Ajout des listeners pour les LEDs
        led1Button.setOnClickListener { toggleLed(0x01) }
        led2Button.setOnClickListener { toggleLed(0x02) }
        led3Button.setOnClickListener { toggleLed(0x03) }
    }

    private fun connectToDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                runOnUiThread {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            connectionStatusText.text = "Connecté à $deviceAddress"
                            connectButton.isEnabled = false

                            if (ActivityCompat.checkSelfPermission(
                                    this@ConnectActivity, // CORRECTION ICI
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    this@ConnectActivity,
                                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                    REQUEST_BLUETOOTH_CONNECT
                                )
                                return@runOnUiThread
                            }

                            gatt.discoverServices()
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            connectionStatusText.text = "Déconnecté"
                            connectButton.isEnabled = true
                            bluetoothGatt = null
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE", "Services BLE découverts :")
                    for (service in gatt.services) {
                        Log.d("BLE", "Service trouvé: ${service.uuid}")
                        for (characteristic in service.characteristics) {
                            Log.d("BLE", "  ↳ Caractéristique: ${characteristic.uuid} | Properties: ${characteristic.properties}")
                        }
                    }
                }
            }
        })
    }

    private fun toggleLed(ledValue: Byte) {
        Log.d("BLE", "Tentative d'allumage de la LED $ledValue")

        if (bluetoothGatt == null) {
            Log.e("BLE", "Bluetooth GATT non connecté")
            Toast.makeText(this, "Non connecté au périphérique", Toast.LENGTH_SHORT).show()
            return
        }

        val ledService = bluetoothGatt?.getService(UUID.fromString("0000feed-cc7a-482a-984a-7f2ed5b3e58f"))
        if (ledService == null) {
            Log.e("BLE", "Service LED introuvable")
            Toast.makeText(this, "Service LED introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        val ledCharacteristic = ledService.getCharacteristic(UUID.fromString("0000abcd-8e22-4541-9d4c-21edae82ed19"))
        if (ledCharacteristic == null) {
            Log.e("BLE", "Caractéristique LED introuvable")
            Toast.makeText(this, "Caractéristique LED introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        ledCharacteristic.value = byteArrayOf(ledValue)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "Permission Bluetooth manquante")
            return
        }

        val success = bluetoothGatt?.writeCharacteristic(ledCharacteristic) ?: false
        if (success) {
            Log.d("BLE", "Commande envoyée avec succès pour la LED $ledValue")
            Toast.makeText(this, "LED $ledValue activée", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("BLE", "Échec de l'envoi de la commande LED")
            Toast.makeText(this, "Impossible de contrôler la LED", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToDevice()
            } else {
                Toast.makeText(this, "Permission Bluetooth requise", Toast.LENGTH_SHORT).show()
            }
        }
    }
}











