package com.example.cleancache

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var currentPhotoPath: String
    private val REQUEST_CAMERA_PERMISSION = 100
    private val REQUEST_CAMERA_CAPTURE = 101
    private val REQUEST_GALLERY_ACCESS = 102

    private var tapCount = 0
    private var lastTapTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleText = findViewById<TextView>(R.id.titleText)
        val cleanButton = findViewById<Button>(R.id.cleanButton)

        // Скрытый жест: тройное нажатие на заголовок
        titleText.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < 1000) {
                tapCount++
                if (tapCount >= 3) {
                    openSecretGallery()
                    tapCount = 0
                }
            } else {
                tapCount = 1
            }
            lastTapTime = currentTime
        }

        cleanButton.setOnClickListener {
            // Фейковая очистка кэша
            Toast.makeText(this, "Кэш успешно очищен!", Toast.LENGTH_SHORT).show()
        }

        // Автопроверка жеста при запуске
        checkForSecretGesture()
    }

    private fun checkForSecretGesture() {
        // Проверка специальных условий для активации
        if (intent?.action == "SECRET_ACTION" ||
            intent?.hasExtra("secret_key") == true) {
            openSecretGallery()
        }
    }

    private fun openSecretGallery() {
        // Проверяем разрешения для камеры и хранилища
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            startSecretActivity()
        }
    }

    private fun startSecretActivity() {
        val intent = Intent(this, SecretGalleryActivity::class.java)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startSecretActivity()
            } else {
                Toast.makeText(this, "Для доступа к скрытым функциям нужны разрешения", Toast.LENGTH_LONG).show()
            }
        }
    }
}