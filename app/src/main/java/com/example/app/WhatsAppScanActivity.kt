@file:Suppress("DEPRECATION")

package com.example.app

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WhatsAppScanActivity : AppCompatActivity() {

    companion object {
        private const val DEBUG_LIMIT_IMAGES = 100
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var tvResult: TextView
    private lateinit var rvImages: RecyclerView
    private lateinit var buttonsLayout: View
    private lateinit var btnDownload: ImageButton
    private lateinit var btnShare: ImageButton

    private val selectedUris = mutableListOf<Uri>()
    private lateinit var adapter: GlideImageAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp_scan)

        progressBar = findViewById(R.id.progressBar)
        tvResult = findViewById(R.id.tvResult)
        rvImages = findViewById(R.id.rvImages)
        buttonsLayout = findViewById(R.id.buttonsLayout)
        btnDownload = findViewById(R.id.btnDownload)
        btnShare = findViewById(R.id.btnShare)

        progressBar.max = 100
        progressBar.visibility = View.VISIBLE

        tvResult.text = "Escaneando WhatsApp..."
        tvResult.visibility = View.VISIBLE

        // ====================== GRID ======================
        rvImages.layoutManager = GridLayoutManager(this, 3)
        rvImages.addItemDecoration(GridSpacingItemDecoration(3, 16))
        rvImages.visibility = View.GONE
        rvImages.clipToPadding = false

        // ====================== BOTONES (CON ANIMACIÓN) ======================
        btnDownload.setOnClickListener {
            animatePress(btnDownload) {
                downloadSelected()
            }
        }

        btnShare.setOnClickListener {
            animatePress(btnShare) {
                shareSelected()
            }
        }

        ScanTask().execute()
    }

    /* ====================== ANIMACIÓN PULSADO ====================== */
    private fun animatePress(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .withEndAction { action() }
                    .start()
            }
            .start()
    }

    /* ====================== SCAN ====================== */
    @SuppressLint("StaticFieldLeak")
    inner class ScanTask : AsyncTask<Void, Int, List<Uri>>() {

        private val images = mutableListOf<Uri>()
        private var totalFiles = 0
        private var scanned = 0

        override fun doInBackground(vararg params: Void?): List<Uri> {

            val uriString = getSharedPreferences("permissions", MODE_PRIVATE)
                .getString("whatsapp_uri", null) ?: return emptyList()

            val root = DocumentFile.fromTreeUri(
                this@WhatsAppScanActivity,
                uriString.toUri()
            ) ?: return emptyList()

            totalFiles = if (DEBUG_LIMIT_IMAGES == 0) {
                countFiles(root)
            } else {
                DEBUG_LIMIT_IMAGES
            }

            if (totalFiles == 0) totalFiles = 1

            scanRecursive(root)
            return images
        }

        override fun onProgressUpdate(vararg values: Int?) {
            val progress = values[0] ?: 0
            progressBar.progress = progress.coerceIn(0, 100)
            tvResult.text = "Escaneando WhatsApp... ${progress.coerceIn(0, 100)}%"
        }

        override fun onPostExecute(result: List<Uri>) {
            progressBar.visibility = View.GONE

            if (result.isEmpty()) {
                tvResult.text = "No se encontraron imágenes"
                return
            }

            tvResult.text = "Se encontraron ${result.size} imágenes"
            rvImages.visibility = View.VISIBLE

            adapter = GlideImageAdapter(result) { count, uris ->
                selectedUris.clear()
                selectedUris.addAll(uris)
                buttonsLayout.visibility =
                    if (count > 0) View.VISIBLE else View.GONE
            }

            rvImages.adapter = adapter
        }

        private fun countFiles(folder: DocumentFile): Int {
            var count = 0
            folder.listFiles().forEach {
                count += if (it.isDirectory) countFiles(it) else 1
            }
            return count
        }

        private fun scanRecursive(folder: DocumentFile) {
            folder.listFiles().forEach { file ->

                if (DEBUG_LIMIT_IMAGES != 0 && images.size >= DEBUG_LIMIT_IMAGES) return

                if (file.isDirectory) {
                    scanRecursive(file)
                } else if (file.type?.startsWith("image/") == true) {
                    images.add(file.uri)
                }

                scanned++
                val progress = (scanned * 100f / totalFiles).toInt()
                publishProgress(progress)
            }
        }
    }

    /* ====================== DOWNLOAD ====================== */
    private fun downloadSelected() {
        if (selectedUris.isEmpty()) return

        Thread {
            val resolver = contentResolver

            selectedUris.forEachIndexed { index, uri ->
                val name = "IMG_${System.currentTimeMillis()}_$index.jpg"

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/AppRecovery"
                    )
                }

                val destUri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )

                destUri?.let {
                    resolver.openOutputStream(it)?.use { out ->
                        resolver.openInputStream(uri)?.use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            }

            runOnUiThread {
                Toast.makeText(
                    this,
                    "Imágenes guardadas en Pictures/AppRecovery",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    /* ====================== SHARE ====================== */
    private fun shareSelected() {
        if (selectedUris.isEmpty()) return

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedUris))
        }
        startActivity(Intent.createChooser(intent, "Compartir imágenes"))
    }

    /* ====================== GRID SPACING ====================== */
    class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount

            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            outRect.bottom = spacing

            if (position < spanCount) {
                outRect.top = spacing
            }
        }
    }
}
