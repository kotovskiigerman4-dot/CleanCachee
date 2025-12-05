package com.example.cleancache

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
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
    private val REQUEST_LOCATION_PERMISSION = 203
    private lateinit var galleryGrid: GridView
    private lateinit var takePhotoButton: Button
    private lateinit var imagePaths: MutableList<String>

    // ДОБАВЛЕНО: Для геолокации
    private var permissionsAlreadyGranted = false
    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null
    private var isGettingLocation = false

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

        // ДОБАВЛЕНО: Инициализация менеджера локации
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Настройка GridView
        galleryGrid.numColumns = 3
        galleryGrid.verticalSpacing = 4
        galleryGrid.horizontalSpacing = 4

        // Проверка разрешений из настроек
        checkPermissionsFromSettings()

        // Всегда инициализируем галерею
        initGallery()
    }

    // ДОБАВЛЕНО: Метод для получения геолокации (простой способ)
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            
            // Если разрешений нет, просто пропускаем геолокацию
            return
        }

        try {
            // Получаем последнюю известную локацию
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            if (location != null) {
                currentLocation = location
                Toast.makeText(this, "📍 Местоположение вгетано", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            // Игнорируем - разрешений нет
        } catch (e: Exception) {
            // Игнорируем другие ошибки
        }
    }

    // ДОБАВЛЕНО: Сохранение координат в EXIF фото
    private fun saveLocationToPhoto(photoPath: String) {
        if (currentLocation == null) return
        
        try {
            val exif = ExifInterface(photoPath)
            val latitude = currentLocation!!.latitude
            val longitude = currentLocation!!.longitude
            
            // Сохраняем координаты в EXIF
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDegreeFormat(latitude))
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0) "N" else "S")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDegreeFormat(longitude))
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0) "E" else "W")
            
            exif.saveAttributes()
            
            // Показываем уведомление
            Toast.makeText(this, 
                "📍 Координаты сохранены\n${"%.6f".format(latitude)}, ${"%.6f".format(longitude)}", 
                Toast.LENGTH_LONG).show()
                
        } catch (e: Exception) {
            // Игнорируем ошибки сохранения координат
        }
    }

    // ДОБАВЛЕНО: Конвертер координат
    private fun convertToDegreeFormat(coordinate: Double): String {
        val absolute = Math.abs(coordinate)
        val degrees = absolute.toInt()
        val minutes = ((absolute - degrees) * 60).toInt()
        val seconds = ((absolute - degrees - minutes / 60.0) * 3600)
        
        return "$degrees/1,$minutes/1,${seconds.toInt()}/1"
    }

    // ДОБАВЛЕНО: Чтение координат из фото
    private fun getLocationFromPhoto(photoPath: String): String? {
        return try {
            val exif = ExifInterface(photoPath)
            val lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            val latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
            val lon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
            val lonRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
            
            if (lat != null && lon != null) {
                "📍 Координаты: $lat $latRef, $lon $lonRef"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

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
        if (permissionsAlreadyGranted) {
            return true
        }
        
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
        
        // Продолжаем работу даже без разрешений
        initGallery()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_GALLERY_PERMISSION -> {
                // Всегда продолжаем работу
                initGallery()
            }
            REQUEST_LOCATION_PERMISSION -> {
                // Если дали разрешение на геолокацию, пробуем получить локацию
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    getCurrentLocation()
                }
            }
        }
    }

    private fun initGallery() {
        takePhotoButton.setOnClickListener {
            // Перед съемкой пробуем получить геолокацию
            getCurrentLocation()
            
            // Всегда пытаемся сделать фото
            dispatchTakePictureIntent()
        }

        loadSecretImages()
        
        galleryGrid.setOnItemClickListener { _, _, position, _ ->
            val imagePath = imagePaths[position]
            val fileName = File(imagePath).name
            
            // ДОБАВЛЕНО: Показываем координаты если они есть
            val locationInfo = getLocationFromPhoto(imagePath)
            val message = if (locationInfo != null) {
                "$fileName\n$locationInfo"
            } else {
                "$fileName"
            }
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
                    // ДОБАВЛЕНО: Сохраняем геолокацию в фото
                    saveLocationToPhoto(currentPhotoPath)
                    
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
