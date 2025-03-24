package fr.isen.faury.androidsmartdevice

import android.os.Bundle
import android.content.Intent
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity




class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ici, on ne définit pas de layout XML, tout est créé directement dans le code
        val titleText = TextView(this).apply {
            text = "Application de Scan BLE"
            textSize = 24f

            // Ajouter une marge pour descendre le texte
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 100, 0, 0)
            this.layoutParams = layoutParams
        }

        val descriptionText = TextView(this).apply {
            text = "Cette application vous permet de scanner les appareils BLE à proximité. Appuyez sur le bouton ci-dessous pour commencer le scan."

            // Ajouter un peu de marge en bas pour espacer de l'icône
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, 0, 200) // Marge en bas
            this.layoutParams = layoutParams
        }

        val bluetoothIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_bluetooth)

            // Réduire la taille de l'icône
            val layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                gravity = Gravity.CENTER
            }
            this.layoutParams = layoutParams
        }

        val scanButton = Button(this).apply {
            text = "Lancer le scan"
            setOnClickListener {
                val intent = Intent(this@MainActivity, ScanActivity::class.java)
                startActivity(intent)
            }

            // Ajouter une marge en haut pour espacer le bouton de l'icône
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply{
                gravity = Gravity.CENTER_HORIZONTAL // Centrer horizontalement
                setMargins(0, 50, 0, 0)
            }
            layoutParams.setMargins(0, 100, 0, 0) // Marge en haut
            this.layoutParams = layoutParams
        }

        // Organiser les éléments dans un layout linéaire
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(titleText)
            addView(descriptionText)
            addView(bluetoothIcon)
            addView(scanButton)
        }

        // Définir le layout comme contenu de l'activité
        setContentView(layout)
    }
}