package com.example.app

import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardMain = findViewById<androidx.cardview.widget.CardView>(R.id.cardMain)

        cardMain.setOnClickListener {
            showScanOptionsDialog()
        }
    }

    private fun showScanOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_scan_apps, null)

        val cbWhatsapp = dialogView.findViewById<CheckBox>(R.id.cbWhatsapp)
        val cbTelegram = dialogView.findViewById<CheckBox>(R.id.cbTelegram)
        val cbInstagram = dialogView.findViewById<CheckBox>(R.id.cbInstagram)

        AlertDialog.Builder(this)
            .setTitle("Selecciona apps a escanear")
            .setView(dialogView)
            .setPositiveButton("Continuar") { _, _ ->

                if (cbWhatsapp.isChecked) {
                    val intent = Intent(this, WhatsAppPermissionActivity::class.java)
                    startActivity(intent)
                }
                else {
                    Toast.makeText(
                        this,
                        "Selecciona al menos WhatsApp",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
