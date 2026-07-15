package dev.tescari.capacitorcameracrop

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@CapacitorPlugin(
    name = "CapacitorCameraCrop"
)
class CapacitorCameraCropPlugin : Plugin() {

    companion object {
        private const val TAG = "CapacitorCameraCrop"

        // Safety cap for decoding when the caller doesn't request a size, to avoid
        // OutOfMemoryError on very high-resolution sensors while keeping high quality.
        private const val MAX_DIMENSION = 4096
    }

    // Single pending camera URI (for ACTION_IMAGE_CAPTURE output)
    private var cameraImageUri: Uri? = null

    // --------------------------------------------------
    // Entry from JavaScript
    // --------------------------------------------------
    // Cropping behavior (aligned with iOS flags):
    //   enableCropping = false                      -> no crop, return raw image
    //   enableCropping = true & nativeCropping=true -> UCrop with locked aspect ratio
    //   enableCropping = true & nativeCropping=false-> UCrop free-style cropping
    @PluginMethod
    fun captureAndCrop(call: PluginCall) {
        val source = call.getString("source") ?: "camera"
        if (source == "gallery") {
            openGallery(call)
        } else {
            openCamera(call)
        }
    }

    // --------------------------------------------------
    // Camera
    // --------------------------------------------------
    private fun openCamera(call: PluginCall) {
        try {
            val tempFile = File.createTempFile("cap_photo_", ".jpg", context.cacheDir)
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            cameraImageUri = imageUri

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(call, intent, "onCameraResult")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
            cameraImageUri = null
            call.reject("Failed to open camera: ${e.message}", e)
        }
    }

    // --------------------------------------------------
    // Gallery
    // --------------------------------------------------
    private fun openGallery(call: PluginCall) {
        try {
            val intent = Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }
            startActivityForResult(call, intent, "onGalleryResult")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open gallery", e)
            call.reject("Failed to open gallery: ${e.message}", e)
        }
    }

    // --------------------------------------------------
    // Activity Callbacks (modern Capacitor pattern)
    // --------------------------------------------------

    @ActivityCallback
    private fun onCameraResult(call: PluginCall?, result: ActivityResult) {
        if (call == null) {
            cameraImageUri = null
            return
        }

        if (result.resultCode != Activity.RESULT_OK) {
            cameraImageUri = null
            call.reject("User cancelled")
            return
        }

        val uri = cameraImageUri
        cameraImageUri = null
        if (uri == null) {
            call.reject("Missing temp photo URI")
            return
        }

        val enableCropping = call.getBoolean("enableCropping") ?: false
        if (enableCropping) {
            startCrop(call, uri)
        } else {
            processAndFinish(call, uri)
        }
    }

    @ActivityCallback
    private fun onGalleryResult(call: PluginCall?, result: ActivityResult) {
        if (call == null) {
            return
        }

        if (result.resultCode != Activity.RESULT_OK) {
            call.reject("User cancelled")
            return
        }

        val uri = result.data?.data
        if (uri == null) {
            call.reject("No image selected")
            return
        }

        val enableCropping = call.getBoolean("enableCropping") ?: false
        if (enableCropping) {
            startCrop(call, uri)
        } else {
            processAndFinish(call, uri)
        }
    }

    @ActivityCallback
    private fun onCropResult(call: PluginCall?, result: ActivityResult) {
        if (call == null) {
            return
        }

        if (result.resultCode != Activity.RESULT_OK) {
            call.reject("Crop cancelled or failed")
            return
        }

        val data = result.data
        if (data == null) {
            call.reject("Crop failed: no data")
            return
        }

        val uri = UCrop.getOutput(data)
        if (uri == null) {
            call.reject("Crop failed")
            return
        }

        processAndFinish(call, uri)
    }

    // --------------------------------------------------
    // Cropping (UCrop) – aligned with iOS flags
    // --------------------------------------------------
    private fun startCrop(call: PluginCall, sourceUri: Uri) {
        try {
            val destFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
            val destUri = Uri.fromFile(destFile)

            val quality = (call.getInt("quality") ?: 90).coerceIn(0, 100)
            val nativeCropping = call.getBoolean("nativeCropping") ?: false

            val options = UCrop.Options().apply {
                setCompressionQuality(quality)

                // UCrop UI colors and appearance (black toolbar to match input/crop transition).
                setStatusBarColor(android.graphics.Color.parseColor("#000000"))
                setToolbarColor(android.graphics.Color.parseColor("#000000"))
                setToolbarWidgetColor(android.graphics.Color.parseColor("#FFFFFF"))
                setActiveControlsWidgetColor(android.graphics.Color.parseColor("#4CAF50"))
                setRootViewBackgroundColor(android.graphics.Color.parseColor("#000000"))
                setToolbarTitle("Crop Image")
                setShowCropFrame(true)
                setShowCropGrid(true)
                setCropGridStrokeWidth(2)
                setCropGridColor(android.graphics.Color.parseColor("#FFFFFF"))
                setCropFrameStrokeWidth(2)
                setCropFrameColor(android.graphics.Color.parseColor("#FFFFFF"))
                setDimmedLayerColor(android.graphics.Color.parseColor("#AA000000"))

                if (nativeCropping) {
                    // Locked aspect ratio (similar to iOS TOCropViewController).
                    setFreeStyleCropEnabled(false)
                    setHideBottomControls(true)
                } else {
                    // Free-style cropping (behaves like iOS "system editing").
                    setFreeStyleCropEnabled(true)
                    setHideBottomControls(false)
                }
            }

            val uCrop = UCrop.of(sourceUri, destUri).withOptions(options)

            configureAspectRatio(call, uCrop, nativeCropping, sourceUri)

            // Max result size — honor either dimension alone (parity with iOS resize).
            val w = call.getInt("width")
            val h = call.getInt("height")
            if (w != null || h != null) {
                uCrop.withMaxResultSize(w ?: MAX_DIMENSION, h ?: MAX_DIMENSION)
            }

            startActivityForResult(call, uCrop.getIntent(context), "onCropResult")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start crop", e)
            call.reject("Failed to start crop: ${e.message}", e)
        }
    }

    /**
     * Configure aspect ratio:
     * - aspectRatio: "free" | "1:1" | "4:3" | "16:9"
     * - aspectRatio: { x: number, y: number } (custom)
     * Only applied when nativeCropping = true (locked). In free-style mode the user
     * crops freely and no ratio is forced.
     */
    private fun configureAspectRatio(
        call: PluginCall,
        uCrop: UCrop,
        nativeCropping: Boolean,
        sourceUri: Uri
    ) {
        if (!nativeCropping) {
            return
        }

        val aspectString = call.getString("aspectRatio")
        val aspectObject = call.getObject("aspectRatio")

        var customX: Float? = null
        var customY: Float? = null

        if (aspectObject != null) {
            try {
                val xVal = aspectObject.getDouble("x")
                val yVal = aspectObject.getDouble("y")
                if (!xVal.isNaN() && !yVal.isNaN() && yVal > 0) {
                    customX = xVal.toFloat()
                    customY = yVal.toFloat()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse aspectRatio object", e)
            }
        }

        when {
            customX != null && customY != null && customY > 0f -> uCrop.withAspectRatio(customX, customY)
            aspectString == "1:1" -> uCrop.withAspectRatio(1f, 1f)
            aspectString == "4:3" -> uCrop.withAspectRatio(4f, 3f)
            aspectString == "16:9" -> uCrop.withAspectRatio(16f, 9f)
            else -> {
                // "free"/null/unknown -> use the source image's own aspect ratio.
                val (w, h) = getImageDimensions(sourceUri)
                if (w > 0 && h > 0) {
                    uCrop.withAspectRatio(w.toFloat(), h.toFloat())
                }
            }
        }
    }

    // --------------------------------------------------
    // Final Image Processing
    // --------------------------------------------------
    private fun processAndFinish(call: PluginCall, uri: Uri) {
        val maxW = call.getInt("width")
        val maxH = call.getInt("height")

        val bitmap = loadBitmap(uri, maxW, maxH)
        if (bitmap == null) {
            call.reject("Failed to decode image")
            return
        }

        val finalBitmap = if (maxW != null || maxH != null) resizeBitmap(bitmap, maxW, maxH) else bitmap

        val result = JSObject()
        val resultType = call.getString("resultType") ?: "uri"
        val quality = (call.getInt("quality") ?: 90).coerceIn(0, 100)

        try {
            if (resultType == "base64") {
                result.put("value", bitmapToBase64(finalBitmap, quality))
            } else {
                val file = File(context.cacheDir, "final_${System.currentTimeMillis()}.jpg")
                saveBitmap(finalBitmap, file, quality)
                result.put("value", Uri.fromFile(file).toString())
            }

            result.put("width", finalBitmap.width)
            result.put("height", finalBitmap.height)
            result.put("mimeType", "image/jpeg")
            call.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image", e)
            call.reject("Failed to process image: ${e.message}", e)
        } finally {
            // Recycle the original once a distinct scaled bitmap replaced it.
            if (finalBitmap != bitmap) {
                bitmap.recycle()
            }
        }
    }

    // --------------------------------------------------
    // Helper Functions
    // --------------------------------------------------

    /**
     * Decode a bitmap with downsampling (to avoid OOM) and correct EXIF orientation.
     */
    private fun loadBitmap(uri: Uri, reqWidth: Int?, reqHeight: Int?): Bitmap? {
        return try {
            // 1. Bounds-only pass to read source dimensions. decodeStream returns
            //    null here by design (inJustDecodeBounds), so guard the STREAM, not
            //    the decode result, and ignore the null return.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            (context.contentResolver.openInputStream(uri) ?: return null).use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            // 2. Downsampled decode (this one really returns the bitmap).
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
            }
            val decoded = (context.contentResolver.openInputStream(uri) ?: return null).use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            // 3. Apply EXIF orientation so photos aren't returned sideways.
            applyExifOrientation(uri, decoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from $uri", e)
            null
        }
    }

    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int?, reqHeight: Int?): Int {
        val targetW = reqWidth ?: MAX_DIMENSION
        val targetH = reqHeight ?: MAX_DIMENSION
        var inSampleSize = 1
        if (srcWidth <= 0 || srcHeight <= 0) return inSampleSize
        if (srcHeight > targetH || srcWidth > targetW) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while ((halfHeight / inSampleSize) >= targetH && (halfWidth / inSampleSize) >= targetW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            Log.e(TAG, "applyExifOrientation failed for $uri", e)
            bitmap
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int?, maxHeight: Int?): Bitmap {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        var tw = w
        var th = h

        if (maxWidth != null && w > maxWidth) {
            tw = maxWidth.toFloat()
            th = h * (tw / w)
        }
        if (maxHeight != null && th > maxHeight) {
            th = maxHeight.toFloat()
            tw = w * (th / h)
        }

        if (tw == w && th == h) return bitmap

        return Bitmap.createScaledBitmap(bitmap, tw.toInt(), th.toInt(), true)
    }

    private fun saveBitmap(bitmap: Bitmap, file: File, quality: Int) {
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Read image dimensions without loading the full bitmap into memory.
     */
    private fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            Log.w(TAG, "getImageDimensions failed for $uri", e)
            Pair(0, 0)
        }
    }
}
