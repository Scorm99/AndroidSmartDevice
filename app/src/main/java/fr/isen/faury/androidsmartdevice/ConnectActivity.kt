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
import android.bluetooth.BluetoothGattDescriptor
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
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.util.UUID


class ConnectActivity : AppCompatActivity() {

    private lateinit var connectButton: Button
    private lateinit var led1Button: Button
    private lateinit var led2Button: Button
    private lateinit var led3Button: Button
    private lateinit var connectionStatusText: TextView
    private lateinit var button1ClicksText: TextView
    private lateinit var button3ClicksText: TextView

    private var deviceAddress: String? = null
    private var bluetoothGatt: BluetoothGatt? = null

    // Compteurs de clics
    private var button1ClickCount = 0
    private var button3ClickCount = 0

    // Gestion de l'état des notifications pour chaque bouton
    private val isSubscribedButton1 = MutableLiveData<Boolean>()
    private val isSubscribedButton3 = MutableLiveData<Boolean>()

    // Gestion des notifications à ignorer
    private var skipNextNotification1 = false
    private var skipNextNotification3 = false

    // État des LEDs
    private val ledStates = mutableMapOf(
        0x01.toByte() to false, // LED 1
        0x02.toByte() to false, // LED 2
        0x03.toByte() to false  // LED 3
    )

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT = 1
        private val SERVICE_UUID = UUID.fromString("0000feed-cc7a-482a-984a-7f2ed5b3e58f")
        private val LED_UUID = UUID.fromString("0000abcd-8e22-4541-9d4c-21edae82ed19")
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
        button1ClicksText = findViewById(R.id.button1ClicksText)
        button3ClicksText = findViewById(R.id.button3ClicksText)

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
                            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return@runOnUiThread
                            }
                            gatt.discoverServices()
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            connectionStatusText.text = "Déconnecté"
                            connectButton.isEnabled = true
                            closeConnection()
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    try {
                        val service3 = gatt.services[2]
                        val button1Characteristic = service3.characteristics[1]
                        val service4 = gatt.services[3]
                        val button3Characteristic = service4.characteristics[0]

                        val handler = Handler(Looper.getMainLooper())

                        button1Characteristic?.let {
                            handler.postDelayed({
                                enableButtonNotification(it, 1, null)
                            }, 500) // Attendre 500ms avant d'activer la première notification
                        }

                        button3Characteristic?.let {
                            handler.postDelayed({
                                enableButtonNotification(it, 3, null)
                            }, 1000) // Attendre 1000ms pour éviter tout conflit
                        }

                    } catch (e: Exception) {
                        Log.e("BLE", "Erreur lors de l'activation des notifications : ${e.message}")
                    }
                }
            }


            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                runOnUiThread {
                    when (characteristic.uuid.toString()) {
                        "00001234-8e22-4541-9d4c-21edae82ed19" -> {
                            button1ClickCount++
                            button1ClicksText.text = "Clics sur bouton 1 : $button1ClickCount"
                            Log.d("BLE", "Compteur bouton 1 = $button1ClickCount")
                        }

                        "0000cdef-8e22-4541-9d4c-21edae82ed19" -> {
                            button3ClickCount++
                            button3ClicksText.text = "Clics sur bouton 3 : $button3ClickCount"
                            Log.d("BLE", "Compteur bouton 3 = $button3ClickCount")
                        }

                        else -> Log.d("BLE", "Notification reçue mais UUID inconnu")
                    }
                }
            }
        })
    }

    private fun closeConnection() {
        if (ActivityCompat.checkSelfPermission(
                this,
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
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun enableButtonNotification(
        characteristic: BluetoothGattCharacteristic,
        buttonNumber: Int,
        onComplete: ((Boolean) -> Unit)?
    ) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            onComplete?.invoke(false)
            return
        }

        val success = bluetoothGatt?.setCharacteristicNotification(characteristic, true) ?: false
        Log.d("BLE", "Activation notification bouton $buttonNumber: $success")

        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(descriptor)
            onComplete?.invoke(true) // Indiquer que l'activation est terminée
        } else {
            Log.e("BLE", "Descripteur introuvable pour le bouton $buttonNumber")
            onComplete?.invoke(false)
        }
    }

    // Fonction pour gérer le basculement de l'état des LEDs
    private fun toggleLed(ledValue: Byte) {
        Log.d("BLE", "Tentative de basculement de la LED $ledValue")

        if (bluetoothGatt == null) {
            Log.e("BLE", "Bluetooth GATT non connecté")
            Toast.makeText(this, "Non connecté au périphérique", Toast.LENGTH_SHORT).show()
            return
        }

        val ledService = bluetoothGatt?.getService(SERVICE_UUID)
        if (ledService == null) {
            Log.e("BLE", "Service LED introuvable")
            Toast.makeText(this, "Service LED introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        val ledCharacteristic = ledService.getCharacteristic(LED_UUID)
        if (ledCharacteristic == null) {
            Log.e("BLE", "Caractéristique LED introuvable")
            Toast.makeText(this, "Caractéristique LED introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        // Basculement de l'état de la LED
        val newState = !(ledStates[ledValue] ?: false)
        val command = if (newState) ledValue else 0x00.toByte()

        ledCharacteristic.value = byteArrayOf(command)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "Permission Bluetooth manquante")
            return
        }

        val success = bluetoothGatt?.writeCharacteristic(ledCharacteristic) ?: false
        if (success) {
            ledStates[ledValue] = newState // Mise à jour de l'état
            val stateText = if (newState) "allumée" else "éteinte"
            Log.d("BLE", "Commande envoyée : LED $ledValue $stateText")
            Toast.makeText(this, "LED $ledValue $stateText", Toast.LENGTH_SHORT).show()
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













