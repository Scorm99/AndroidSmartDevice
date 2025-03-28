package fr.isen.faury.androidsmartdevice

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ScanActivity : AppCompatActivity() {

    private lateinit var scanButton: Button
    private lateinit var scanStatusText: TextView
    private lateinit var devicesListView: ListView

    private val permissionRequestCode = 1001

    private val scannedDevices = mutableListOf<String>()
    private val adapter by lazy { ArrayAdapter(this, android.R.layout.simple_list_item_1, scannedDevices) }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device

            // Vérification de la permission BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(
                    this@ScanActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Si la permission n'est pas accordée, on demande la permission
                ActivityCompat.requestPermissions(
                    this@ScanActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    permissionRequestCode
                )
                return
            }

            // Si la permission est accordée, on peut accéder aux informations du périphérique
            val deviceName = device.name ?: "Inconnu"
            val deviceAddress = device.address

            val deviceInfo = "$deviceName ($deviceAddress)"
            if (!scannedDevices.contains(deviceInfo)) {
                scannedDevices.add(deviceInfo)
                adapter.notifyDataSetChanged()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(applicationContext, "Échec du scan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Initialisation des vues
        scanStatusText = findViewById(R.id.scanStatusText)
        devicesListView = findViewById(R.id.devicesListView)
        scanButton = findViewById(R.id.scanButton)

        devicesListView.adapter = adapter

        // Vérifier les permissions au lancement
        checkPermissions()

        devicesListView.setOnItemClickListener { _, _, position, _ ->
            // Récupérer l'adresse du périphérique sélectionné
            val selectedDeviceInfo = scannedDevices[position]
            val deviceAddress = selectedDeviceInfo.substringAfter("(").substringBefore(")")

            // Passer l'adresse du périphérique à la nouvelle activité
            val intent = Intent(this, ConnectActivity::class.java)
            intent.putExtra("deviceAddress", deviceAddress)
            startActivity(intent)
        }

        // Écouter l'événement du bouton de scan
        scanButton.setOnClickListener {
            if (checkBluetoothEnabled()) {
                if (scanStatusText.text == "Scan arrêté") {
                    startScan()
                } else {
                    stopScan()
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        }
    }

    private fun checkBluetoothEnabled(): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Le téléphone ne prend pas en charge le Bluetooth
            Toast.makeText(this, "Bluetooth non disponible sur cet appareil.", Toast.LENGTH_SHORT).show()
            return false
        } else if (!bluetoothAdapter.isEnabled) {
            // Bluetooth est désactivé, demander à l'utilisateur de l'activer
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            // Vérifier les permissions nécessaires pour Bluetooth
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Demander la permission pour le Bluetooth si elle n'est pas accordée
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    permissionRequestCode
                )
                return false // Bluetooth ne peut pas être activé tant que la permission n'est pas accordée
            }

            // Si la permission est accordée, lancer l'intention pour activer le Bluetooth
            startActivityForResult(enableBtIntent, 1)
            return false
        }
        return true // Bluetooth est activé et prêt à l'emploi
    }

    // Gérer la réponse de la demande de permission dans onRequestPermissionsResult
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, continuer à activer le Bluetooth ou scanner
                checkBluetoothEnabled()
            } else {
                // Permission refusée, afficher un message à l'utilisateur
                Toast.makeText(this, "Permission Bluetooth refusée. L'application ne peut pas scanner.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Pour le résultat de l'activation Bluetooth si l'utilisateur refuse
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode != RESULT_OK) {
            Toast.makeText(this, "Bluetooth non activé. Impossible de scanner.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startScan() {
        scanStatusText.text = "Scanning..."
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.let {
            val scanner = it.bluetoothLeScanner
            val scanFilter = ScanFilter.Builder().build()
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            val filters = listOf(scanFilter)
            if (ActivityCompat.checkSelfPermission(
                    this,
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
            scanner.startScan(filters, scanSettings, scanCallback)
            scanButton.text = "Arrêter le scan"
        }
    }

    private fun stopScan() {
        scanStatusText.text = "Scan arrêté"
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.let {
            val scanner = it.bluetoothLeScanner
            if (ActivityCompat.checkSelfPermission(
                    this,
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
            scanner.stopScan(scanCallback)
            scanButton.text = "Démarrer le scan"
        }
    }
}




