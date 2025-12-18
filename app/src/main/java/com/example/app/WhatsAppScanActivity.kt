package com.example.app

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.OutputStream

class WhatsAppScanActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvResult: TextView
    private lateinit var rvImages: RecyclerView
    private lateinit var buttonsLayout: View
    private lateinit var btnDownload: ImageButton
    private lateinit var btnShare: ImageButton

    private val selectedUris = mutableListOf<Uri>()
    private lateinit var adapter: GlideImageAdapter

    inner class ScanTask : AsyncTask<Void, Int, List<Uri>>() {

        private val images = mutableListOf<Uri>()
        private var totalFiles = 0
        private var processedFiles = 0

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE
            tvResult.visibility = View.VISIBLE
            tvResult.text = "Escaneando WhatsApp..."
            rvImages.visibility = View.GONE
        }

        override fun doInBackground(vararg params: Void?): List<Uri> {
            val uriString = getSharedPreferences("permissions", MODE_PRIVATE)
                .getString("whatsapp_uri", null) ?: return emptyList()

            val root = DocumentFile.fromTreeUri(this@WhatsAppScanActivity, Uri.parse(uriString))
                ?: return emptyList()

            totalFiles = countFilesRecursive(root)
            processedFiles = 0

            scanFolderRecursive(root)
            return images
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            val progress = values[0] ?: 0
            progressBar.progress = progress
            tvResult.text = "Escaneando WhatsApp... $progress%"
        }

        override fun onPostExecute(result: List<Uri>) {
            super.onPostExecute(result)
            progressBar.visibility = View.GONE

            if (result.isEmpty()) {
                tvResult.text = "No se encontraron imágenes"
                return
            }

            tvResult.text = "Se encontraron ${result.size} imágenes"
            rvImages.visibility = View.VISIBLE
            rvImages.layoutManager = GridLayoutManager(this@WhatsAppScanActivity, 3)
            adapter = GlideImageAdapter(result) { selectedCount, uris ->
                selectedUris.clear()
                selectedUris.addAll(uris)
                updateButtonsVisibility(selectedCount)
            }
            rvImages.adapter = adapter
        }

        private fun countFilesRecursive(folder: DocumentFile): Int {
            var count = 0
            folder.listFiles().forEach { file ->
                count += if (file.isDirectory) countFilesRecursive(file) else 1
            }
            return count
        }

        private fun scanFolderRecursive(folder: DocumentFile) {
            folder.listFiles().forEach { file ->
                if (file.isDirectory) scanFolderRecursive(file)
                else if (file.type?.startsWith("image/") == true) images.add(file.uri)

                processedFiles++
                val progress = (processedFiles.toFloat() / totalFiles * 100).toInt()
                publishProgress(progress)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp_scan)

        progressBar = findViewById(R.id.progressBar)
        tvResult = findViewById(R.id.tvResult)
        rvImages = findViewById(R.id.rvImages)
        buttonsLayout = findViewById(R.id.buttonsLayout)
        btnDownload = findViewById(R.id.btnDownload)
        btnShare = findViewById(R.id.btnShare)

        val spacing = 8
        rvImages.addItemDecoration(GridSpacingItemDecoration(3, spacing, true))

        progressBar.isIndeterminate = false
        progressBar.max = 100
        progressBar.progress = 0

        btnDownload.setOnClickListener { downloadSelected() }
        btnShare.setOnClickListener { shareSelected() }

        ScanTask().execute()
    }

    private fun updateButtonsVisibility(selectedCount: Int) {
        buttonsLayout.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
    }

    private fun downloadSelected() {
        if (selectedUris.isEmpty()) return

        for (uri in selectedUris) {
            val resolver = contentResolver
            val name = "WhatsApp_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }
            val outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            outUri?.let { destUri ->
                resolver.openOutputStream(destUri)?.use { outputStream ->
                    resolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }

    private fun shareSelected() {
        if (selectedUris.isEmpty()) return
        val intent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedUris))
        }
        startActivity(Intent.createChooser(intent, "Compartir imágenes"))
    }

    class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount
            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount
                if (position < spanCount) outRect.top = spacing
                outRect.bottom = spacing
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                if (position >= spanCount) outRect.top = spacing
            }
        }
    }
}
