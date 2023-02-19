package com.example.pdfviewer

import android.R
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.shockwave.pdfium.PdfDocument.Bookmark


const val REQUEST_CODE = 42
const val PERMISSION_CODE = 42042

const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"

var pdfView: PDFView? = null

var uri: Uri? = null

var pageNumber = 0

var pdfFileName: String? = null


class ViewActivity : AppCompatActivity() {
    private val TAG: String = ViewActivity::class.java.simpleName

    @OptionsItem(R.id.pickFile)
    fun pickFile() {
        val permissionCheck = ContextCompat.checkSelfPermission(
            this,
            READ_EXTERNAL_STORAGE
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(READ_EXTERNAL_STORAGE),
                PERMISSION_CODE
            )
            return
        }
        launchPicker()
    }

    private fun launchPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        try {
            startActivityForResult(intent, REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            //alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun afterViews() {
        pdfView!!.setBackgroundColor(Color.LTGRAY)
        displayFromUri(uri)
        title = pdfFileName
    }

    private fun displayFromUri(uri: Uri?) {
        pdfFileName = getFileName(uri)
        pdfView!!.fromUri(uri)
            .defaultPage(pageNumber)
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .onLoad(this)
            .scrollHandle(DefaultScrollHandle(this))
            .spacing(10) // in dp
            .onPageError(this)
            .load()
    }

    fun onResult(resultCode: Int, intent: Intent) {
        if (resultCode == RESULT_OK) {
            uri = intent.data
            displayFromUri(uri)
        }
    }

    fun onPageChanged(page: Int, pageCount: Int) {
        pageNumber = page
        title = String.format("%s %s / %s", pdfFileName, page + 1, pageCount)
    }

    private fun getFileName(uri: Uri?): String? {
        var result: String? = null
        if (uri!!.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
    }

    private fun printBookmarksTree(tree: List<Bookmark>, sep: String) {
        for (b in tree) {
            Log.e(TAG, String.format("%s %s, p %d", sep, b.title, b.pageIdx))
            if (b.hasChildren()) {
                printBookmarksTree(b.children, "$sep-")
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                launchPicker()
            }
        }
    }

    fun onPageError(page: Int, t: Throwable?) {
        Log.e(TAG, "Cannot load page $page")
    }
}





