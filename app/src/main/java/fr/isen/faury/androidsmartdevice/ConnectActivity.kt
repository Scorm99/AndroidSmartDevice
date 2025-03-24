package fr.isen.faury.androidsmartdevice

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID


class ConnectActivity : AppCompatActivity() {

    private lateinit var connectButton: Button
    private lateinit var connectionStatusText: TextView
    private var deviceAddress: String? = null
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        // Initialiser les vues
        connectButton = findViewById(R.id.connectButton)
        connectionStatusText = findViewById(R.id.connectionStatusText)

        // Récupérer l'adresse du périphérique à partir de l'intent
        deviceAddress = intent.getStringExtra("deviceAddress")

        // Afficher l'adresse de l'appareil
        connectionStatusText.text = "Connexion à : $deviceAddress"

        // Vérifier les permissions au lancement
        checkPermissions()

        // Lancer la connexion au périphérique Bluetooth
        connectButton.setOnClickListener {
            deviceAddress?.let {
                connectToDevice(it)
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Demander la permission pour Bluetooth
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun connectToDevice(deviceAddress: String) {
        // Vérifier à nouveau si la permission est accordée avant de tenter la connexion
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si la permission n'est pas accordée, demander à l'utilisateur
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        // Créer le socket Bluetooth RFCOMM
        val bluetoothSocket: BluetoothSocket? = device.createRfcommSocketToServiceRecord(MY_UUID)

        try {
            // Tentative de connexion
            bluetoothSocket?.connect()
            connectionStatusText.text = "Connecté à $deviceAddress"
        } catch (e: IOException) {
            e.printStackTrace()
            connectionStatusText.text = "Échec de la connexion"
        }
    }

    // Gérer la réponse de la demande de permission dans onRequestPermissionsResult
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, on peut essayer de se connecter au périphérique
                deviceAddress?.let {
                    connectToDevice(it)
                }
            } else {
                // Permission refusée
                Toast.makeText(this, "Permission Bluetooth refusée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID générique SPP
    }
}