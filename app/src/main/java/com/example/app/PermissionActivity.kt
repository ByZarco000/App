package com.example.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.content.edit

@Suppress("DEPRECATION")
class PermissionActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_WHATSAPP = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPermissionInfo()
    }

    private fun showPermissionInfo() {
        AlertDialog.Builder(this)
            .setTitle("Permiso requerido")
            .setMessage(
                "Para recuperar fotos y videos de WhatsApp:\n\n" +
                        "1️⃣ Pulsa CONTINUAR\n" +
                        "2️⃣ Pulsa \"Usar esta carpeta\"\n\n" +
                        "⚠️ NO cambies de carpeta\n" +
                        "Solo acepta el permiso."
            )
            .setCancelable(false)
            .setPositiveButton("Continuar") { _, _ ->
                openAndroidMediaFolder()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .show()
    }

    private fun openAndroidMediaFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            putExtra(
                "android.provider.extra.INITIAL_URI",
                "content://com.android.externalstorage.documents/tree/primary:Android/media".toUri()
            )
        }
        startActivityForResult(intent, REQUEST_CODE_WHATSAPP)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_WHATSAPP && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            getSharedPreferences("permissions", MODE_PRIVATE)
                .edit {
                    putString("whatsapp_uri", uri.toString())
                }

            startActivity(Intent(this, ScanActivity::class.java))
            finish()
        }
    }
}//end
