package com.example.capacitorcameracrop

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.core.content.FileProvider
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
import java.io.InputStream

@CapacitorPlugin(
    name = "CapacitorCameraCrop"
)
class CapacitorCameraCropPlugin : Plugin() {

    companion object {
        private const val TAG = "CapacitorCameraCrop"
    }

    // Single pending camera URI (for ACTION_IMAGE_CAPTURE output)
    private var cameraImageUri: Uri? = null

    // --------------------------------------------------
    // Entry from JavaScript
    // --------------------------------------------------
    @PluginMethod
    fun captureAndCrop(call: PluginCall) {
        val source = call.getString("source") ?: "camera"
        val enableCropping = call.getBoolean("enableCropping") ?: false
        val nativeCropping = call.getBoolean("nativeCropping") ?: false
        val useSystemEditing = call.getBoolean("useSystemEditingIfAvailable") ?: true
        val resultType = call.getString("resultType") ?: "uri"
        val quality = call.getInt("quality") ?: 90

        Log.d(TAG, "captureAndCrop: source=$source, enableCropping=$enableCropping, nativeCropping=$nativeCropping, useSystemEditingIfAvailable=$useSystemEditing")
        Log.d(TAG, "captureAndCrop: resultType=$resultType, quality=$quality")
        Log.d(TAG, "PLUGIN VERSION ANDROID ALIGNED 1.0.0")

        // NOTE:
        // - Android has no true "system editing" crop like iOS UIImagePicker.
        // - We interpret:
        //   * enableCropping = false      -> no crop, return raw image
        //   * enableCropping = true &
        //     nativeCropping = true       -> UCrop with locked aspect ratio (similar to TOCropViewController)
        //   * enableCropping = true &
        //     nativeCropping = false      -> UCrop with free-style cropping (behaves like "system editing" on iOS)

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
            Log.d(TAG, "openCamera: START - callId=${call.callbackId}, thread=${Thread.currentThread().name}")

            val tempFile = File.createTempFile("cap_photo_", ".jpg", context.cacheDir)
            Log.d(TAG, "openCamera: tempFile created=${tempFile.absolutePath}")

            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            Log.d(TAG, "openCamera: imageUri=$imageUri")

            cameraImageUri = imageUri

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Log.d(TAG, "openCamera: intent created, about to call startActivityForResult")

            try {
                Log.d(TAG, "openCamera: calling startActivityForResult - callId=${call.callbackId}")
                // Modern pattern: use ActivityCallback name
                startActivityForResult(call, intent, "onCameraResult")
                Log.d(TAG, "openCamera: startActivityForResult returned successfully")
            } catch (e: Exception) {
                Log.e(TAG, "openCamera: Exception in startActivityForResult", e)
                e.printStackTrace()
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "openCamera: Failed to open camera", e)
            cameraImageUri = null
            call.reject("Failed to open camera: ${e.message}", e)
        }
    }

    // --------------------------------------------------
    // Gallery
    // --------------------------------------------------
    private fun openGallery(call: PluginCall) {
        try {
            Log.d(TAG, "openGallery: START - callId=${call.callbackId}, thread=${Thread.currentThread().name}")

            val intent = Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }
            Log.d(TAG, "openGallery: intent created, about to call startActivityForResult")

            try {
                Log.d(TAG, "openGallery: calling startActivityForResult - callId=${call.callbackId}")
                startActivityForResult(call, intent, "onGalleryResult")
                Log.d(TAG, "openGallery: startActivityForResult returned successfully")
            } catch (e: Exception) {
                Log.e(TAG, "openGallery: Exception in startActivityForResult", e)
                e.printStackTrace()
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "openGallery: Failed to open gallery", e)
            call.reject("Failed to open gallery: ${e.message}", e)
        }
    }

    // --------------------------------------------------
    // Activity Callbacks (modern Capacitor pattern)
    // --------------------------------------------------

    @ActivityCallback
    private fun onCameraResult(call: PluginCall?, result: ActivityResult) {
        if (call == null) {
            Log.e(TAG, "onCameraResult: call is null")
            cameraImageUri = null
            return
        }

        if (result.resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "onCameraResult: user cancelled or result not OK (${result.resultCode})")
            cameraImageUri = null
            call.reject("User cancelled")
            return
        }

        val uri = cameraImageUri
        if (uri == null) {
            Log.e(TAG, "onCameraResult: Missing cameraImageUri")
            call.reject("Missing temp photo URI")
            return
        }

        Log.d(TAG, "onCameraResult: uri=$uri")

        val enableCropping = call.getBoolean("enableCropping") ?: false
        if (enableCropping) {
            startCrop(call, uri)
        } else {
            cameraImageUri = null
            processAndFinish(call, uri)
        }
    }

    @ActivityCallback
    private fun onGalleryResult(call: PluginCall?, result: ActivityResult) {
        if (call == null) {
            Log.e(TAG, "onGalleryResult: call is null")
            return
        }

        if (result.resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "onGalleryResult: user cancelled or result not OK (${result.resultCode})")
            call.reject("User cancelled")
            return
        }

        val dataIntent = result.data
        val uri = dataIntent?.data
        if (uri == null) {
            Log.e(TAG, "onGalleryResult: No image selected from gallery")
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
            Log.e(TAG, "onCropResult: call is null")
            return
        }

        if (result.resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "onCropResult: result not OK (${result.resultCode})")
            // UCrop sometimes returns errors via getError; you could surface that if you want
            call.reject("Crop cancelled or failed")
            return
        }

        val data = result.data
        if (data == null) {
            Log.e(TAG, "onCropResult: Crop result intent is null")
            call.reject("Crop failed: no data")
            return
        }

        val uri = UCrop.getOutput(data)
        if (uri == null) {
            Log.e(TAG, "onCropResult: Crop failed, output URI is null")
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

            val quality = call.getInt("quality") ?: 90
            val enableCropping = call.getBoolean("enableCropping") ?: false
            val nativeCropping = call.getBoolean("nativeCropping") ?: false
            val useSystemEditing = call.getBoolean("useSystemEditingIfAvailable") ?: true

            Log.d(TAG, "startCrop: enableCropping=$enableCropping, nativeCropping=$nativeCropping, useSystemEditingIfAvailable=$useSystemEditing")

            val options = UCrop.Options().apply {
                setCompressionQuality(quality)
                if (nativeCropping) {
                    // lock aspect
                    setFreeStyleCropEnabled(false)
                    setHideBottomControls(true)
                } else {
                    // free-style cropping
                    setFreeStyleCropEnabled(true)
                    setHideBottomControls(false)
                }
            }

            val uCrop = UCrop.of(sourceUri, destUri).withOptions(options)

            // Aspect ratio handling
            configureAspectRatio(call, uCrop, nativeCropping, sourceUri)

            // Max result size
            val w = call.getInt("width")
            val h = call.getInt("height")
            if (w != null && h != null) {
                uCrop.withMaxResultSize(w, h)
            }

            Log.d(TAG, "startCrop: about to call startActivityForResult - callId=${call.callbackId}")
            try {
                startActivityForResult(call, uCrop.getIntent(context), "onCropResult")
                Log.d(TAG, "startCrop: startActivityForResult returned successfully")
            } catch (e: Exception) {
                Log.e(TAG, "startCrop: Exception in startActivityForResult", e)
                e.printStackTrace()
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start crop", e)
            call.reject("Failed to start crop: ${e.message}", e)
        }
    }

    /**
     * Configure aspect ratio:
     * - aspectRatio: "free" | "1:1" | "4:3" | "16:9"
     * - aspectRatio: { x: number, y: number } (custom)
     * Behavior:
     * - If nativeCropping = true   -> apply fixed aspect ratio (locked)
     * - If nativeCropping = false  -> we allow free-style; we don't force ratio
     */
    private fun configureAspectRatio(
        call: PluginCall,
        uCrop: UCrop,
        nativeCropping: Boolean,
        sourceUri: Uri
    ) {
        if (!nativeCropping) {
            // Free-style mode: like iOS "system editing" – user can crop freely.
            Log.d(TAG, "configureAspectRatio: nativeCropping=false (free style crop)")
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
                Log.w(TAG, "configureAspectRatio: Failed to parse aspectRatio object", e)
            }
        }

        when {
            customX != null && customY != null && customY > 0f -> {
                Log.d(TAG, "configureAspectRatio: using custom aspect ${customX}:${customY}")
                uCrop.withAspectRatio(customX, customY)
            }

            aspectString == "1:1" -> {
                Log.d(TAG, "configureAspectRatio: preset 1:1")
                uCrop.withAspectRatio(1f, 1f)
            }

            aspectString == "4:3" -> {
                Log.d(TAG, "configureAspectRatio: preset 4:3")
                uCrop.withAspectRatio(4f, 3f)
            }

            aspectString == "16:9" -> {
                Log.d(TAG, "configureAspectRatio: preset 16:9")
                uCrop.withAspectRatio(16f, 9f)
            }

            aspectString == "free" || aspectString == null -> {
                val (w, h) = getImageDimensions(sourceUri)
                if (w > 0 && h > 0) {
                    Log.d(TAG, "configureAspectRatio: original aspect ${w}:${h}")
                    uCrop.withAspectRatio(w.toFloat(), h.toFloat())
                } else {
                    Log.d(TAG, "configureAspectRatio: failed to read original size; using UCrop default")
                }
            }

            else -> {
                val (w, h) = getImageDimensions(sourceUri)
                if (w > 0 && h > 0) {
                    Log.d(TAG, "configureAspectRatio: unknown aspect=$aspectString, using original ${w}:${h}")
                    uCrop.withAspectRatio(w.toFloat(), h.toFloat())
                }
            }
        }
    }

    // --------------------------------------------------
    // Final Image Processing
    // --------------------------------------------------
    private fun processAndFinish(call: PluginCall, uri: Uri) {
        val bitmap = loadBitmap(uri)
        if (bitmap == null) {
            Log.e(TAG, "Failed to decode image from $uri")
            call.reject("Failed to decode image")
            return
        }

        var finalBitmap = bitmap

        val maxW = call.getInt("width")
        val maxH = call.getInt("height")

        if (maxW != null || maxH != null) {
            finalBitmap = resizeBitmap(bitmap, maxW, maxH)
        }

        val result = JSObject()
        val resultType = call.getString("resultType") ?: "uri"
        val quality = call.getInt("quality") ?: 90

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

        Log.d(TAG, "processAndFinish: width=${finalBitmap.width}, height=${finalBitmap.height}")
        call.resolve(result)
    }

    // --------------------------------------------------
    // Helper Functions
    // --------------------------------------------------
    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            BitmapFactory.decodeStream(stream).also { stream.close() }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from $uri", e)
            null
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
     * Read image dimensions without loading full bitmap into memory.
     */
    private fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        var inputStream: InputStream? = null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            Log.w(TAG, "getImageDimensions: failed for $uri", e)
            Pair(0, 0)
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {
            }
        }
    }
}

