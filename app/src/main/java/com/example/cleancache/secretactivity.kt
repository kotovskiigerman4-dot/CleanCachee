package com.example.cleancache

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SecretGalleryActivity : AppCompatActivity() {

    private lateinit var currentPhotoPath: String
    private val REQUEST_CAMERA_CAPTURE = 201
    private val REQUEST_GALLERY_PERMISSION = 202
    private lateinit var galleryGrid: GridView
    private lateinit var takePhotoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secret_gallery)

        galleryGrid = findViewById(R.id.galleryGrid)
        takePhotoButton = findViewById(R.id.takePhotoButton)

        // Проверяем разрешения при запуске
        checkPermissionsAndInit()
    }

    private fun checkPermissionsAndInit() {
        if (hasRequiredPermissions()) {
            initGallery()
        } else {
            requestGalleryPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val hasCamera = ContextCompat.checkSelfPermission(
            this, 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android до 13
            ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        return hasCamera && hasStorage
    }

    private fun requestGalleryPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            REQUEST_GALLERY_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_GALLERY_PERMISSION -> {
                val allGranted = grantResults.isNotEmpty() && 
                                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                
                if (allGranted) {
                    initGallery()
                } else {
                    Toast.makeText(
                        this, 
                        "Не жми так часто.", 
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun initGallery() {
        takePhotoButton.setOnClickListener {
            if (hasRequiredPermissions()) {
                dispatchTakePictureIntent()
            } else {
                requestGalleryPermissions()
            }
        }

        loadSecretImages()
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            try {
                val photoFile = createImageFile()
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    photoFile
                )
                
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                
                startActivityForResult(takePictureIntent, REQUEST_CAMERA_CAPTURE)
                
            } catch (ex: Exception) {
                Toast.makeText(
                    this, 
                    "Ошибка при создании файла: ${ex.message}", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(this, "Нет приложения камеры", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CAMERA_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Фото сохранено в секретной галерее", Toast.LENGTH_SHORT).show()
                // Обновляем галерею после съёмки
                loadSecretImages()
                
                // Сканируем файл для добавления в медиа-библиотеку
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(File(currentPhotoPath))
                mediaScanIntent.data = contentUri
                sendBroadcast(mediaScanIntent)
                
            } else if (resultCode == RESULT_CANCELED) {
                // Удаляем пустой файл если фото не сделано
                try {
                    File(currentPhotoPath).delete()
                } catch (e: Exception) {
                    // Игнорируем ошибку удаления
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        
        // Создаём скрытую папку
        val secretDir = File(storageDir, ".secret_photos")
        if (!secretDir.exists()) {
            secretDir.mkdirs()
            // Скрываем папку (только для некоторых файловых менеджеров)
            val nomedia = File(secretDir, ".nomedia")
            nomedia.createNewFile()
        }
        
        val imageFile = File.createTempFile(
            "SECRET_${timeStamp}_",
            ".jpg",
            secretDir
        )
        
        currentPhotoPath = imageFile.absolutePath
        return imageFile
    }

    private fun loadSecretImages() {
        val images = mutableListOf<String>()
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val secretDir = File(storageDir, ".secret_photos")
        
        if (secretDir.exists() && secretDir.isDirectory) {
            val imageFiles = secretDir.listFiles { file ->
                file.isFile && (file.name.lowercase(Locale.getDefault()).endsWith(".jpg") || 
                               file.name.lowercase(Locale.getDefault()).endsWith(".jpeg") ||
                               file.name.lowercase(Locale.getDefault()).endsWith(".png"))
            }
            
            imageFiles?.sortedByDescending { it.lastModified() }?.forEach { file ->
                images.add(file.absolutePath)
            }
        }
        
        // Простой адаптер для демонстрации (замени на настоящий)
        if (images.isNotEmpty()) {
            Toast.makeText(this, "Найдено ${images.size} фото", Toast.LENGTH_SHORT).show()
            
            // Временный код - создай свой ImageAdapter
            // galleryGrid.adapter = ImageAdapter(this, images)
            
        } else {
            Toast.makeText(this, "Секретная галерея пуста", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Возвращаемся к главному экрану
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}
