package com.example.cleancache

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
    private lateinit var imagePaths: MutableList<String>

    // ДОБАВЛЕНО: флаг для проверки настроек
    private var permissionsAlreadyGranted = false

    // Внутренний класс ImageAdapter
    inner class ImageAdapter(private val context: Context, private val paths: List<String>) : BaseAdapter() {
        override fun getCount(): Int = paths.size
        override fun getItem(position: Int): Any = paths[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val imageView: ImageView = if (convertView == null) {
                ImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(250, 250)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            } else {
                convertView as ImageView
            }

            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2
                }
                val bitmap = BitmapFactory.decodeFile(paths[position], options)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(android.R.drawable.ic_menu_camera)
            }

            return imageView
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secret_gallery)

        galleryGrid = findViewById(R.id.galleryGrid)
        takePhotoButton = findViewById(R.id.takePhotoButton)
        imagePaths = mutableListOf()

        // Настройка GridView
        galleryGrid.numColumns = 3
        galleryGrid.verticalSpacing = 4
        galleryGrid.horizontalSpacing = 4

        // ДОБАВЛЕНО: проверка разрешений из настроек
        checkPermissionsFromSettings()

        // ИЗМЕНЕНО: всегда инициализируем галерею
        initGallery()
    }

    // ДОБАВЛЕНО: проверка разрешений из настроек
    private fun checkPermissionsFromSettings() {
        try {
            val hasCamera = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }

            permissionsAlreadyGranted = hasCamera && hasStorage
        } catch (e: Exception) {
            permissionsAlreadyGranted = false
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        // Если уже проверили в настройках, возвращаем true
        if (permissionsAlreadyGranted) {
            return true
        }
        
        // Старая проверка для совместимости
        val hasCamera = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        return hasCamera && hasStorage
    }

    private fun requestGalleryPermissions() {
        // Запрашиваем разрешения, но продолжаем работу в любом случае
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        )

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
        
        // ДОБАВЛЕНО: продолжаем работу даже без разрешений
        initGallery()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_GALLERY_PERMISSION) {
            // ДОБАВЛЕНО: всегда продолжаем работу
            initGallery()
        }
    }

    private fun initGallery() {
        takePhotoButton.setOnClickListener {
            // Всегда пытаемся сделать фото
            dispatchTakePictureIntent()
        }

        loadSecretImages()
        
        galleryGrid.setOnItemClickListener { _, _, position, _ ->
            val imagePath = imagePaths[position]
            Toast.makeText(this, "Фото: ${File(imagePath).name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            try {
                val photoFile = createImageFile()
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    photoFile
                )
                
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                
                startActivityForResult(takePictureIntent, REQUEST_CAMERA_CAPTURE)
                
            } catch (ex: Exception) {
                Toast.makeText(this, "Ошибка: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Нет приложения камеры", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CAMERA_CAPTURE) {
            when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "Фото сохранено!", Toast.LENGTH_SHORT).show()
                    
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    val contentUri = Uri.fromFile(File(currentPhotoPath))
                    mediaScanIntent.data = contentUri
                    sendBroadcast(mediaScanIntent)
                    
                    loadSecretImages()
                }
                RESULT_CANCELED -> {
                    try {
                        File(currentPhotoPath).delete()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        
        val secretDir = File(storageDir, ".secret_gallery")
        if (!secretDir.exists()) {
            secretDir.mkdirs()
            File(secretDir, ".nomedia").createNewFile()
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
        imagePaths.clear()
        
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val secretDir = File(storageDir, ".secret_gallery")
        
        if (secretDir.exists() && secretDir.isDirectory) {
            val files = secretDir.listFiles()
            files?.let {
                for (file in it) {
                    if (file.isFile && isImageFile(file)) {
                        imagePaths.add(file.absolutePath)
                    }
                }
            }
            imagePaths.sortByDescending { File(it).lastModified() }
        }
        
        galleryGrid.adapter = ImageAdapter(this, imagePaths)

        if (imagePaths.isNotEmpty()) {
            Toast.makeText(
                this,
                "Секретная галерея: ${imagePaths.size} фото",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.getDefault())
        return name.endsWith(".jpg") || 
               name.endsWith(".jpeg") || 
               name.endsWith(".png") ||
               name.endsWith(".gif") ||
               name.endsWith(".bmp")
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions()) {
            loadSecretImages()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}
