/**
 * Example usage of the capacitor-camera-crop plugin
 * 
 * This file demonstrates various use cases for the plugin.
 * You can copy these examples into your Capacitor app.
 */

import { NativeCameraCrop, type CaptureAndCropResult } from 'capacitor-camera-crop';

/**
 * Example 1: Take a photo with the camera and crop it to 1:1 aspect ratio
 */
export async function takeCroppedPhoto(): Promise<CaptureAndCropResult> {
  try {
    const result = await NativeCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      aspectRatio: '1:1',
      resultType: 'uri',
      quality: 90,
    });

    console.log('Photo captured and cropped:', result);
    console.log('Image URI:', result.value);
    console.log('Dimensions:', `${result.width}x${result.height}`);
    
    return result;
  } catch (error) {
    console.error('Error taking photo:', error);
    throw error;
  }
}

/**
 * Example 2: Select an image from the gallery without cropping
 */
export async function selectFromGallery(): Promise<CaptureAndCropResult> {
  try {
    const result = await NativeCameraCrop.captureAndCrop({
      source: 'gallery',
      enableCropping: false,
      resultType: 'uri',
    });

    console.log('Image selected:', result);
    return result;
  } catch (error) {
    console.error('Error selecting image:', error);
    throw error;
  }
}

/**
 * Example 3: Take a photo and get base64 encoded result
 */
export async function takePhotoAsBase64(): Promise<string> {
  try {
    const result = await NativeCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      aspectRatio: '4:3',
      resultType: 'base64',
      quality: 85,
    });

    // The result.value is a base64 string
    // You can use it directly in an <img> tag:
    // <img src={`data:${result.mimeType};base64,${result.value}`} />
    
    console.log('Base64 length:', result.value.length);
    return result.value;
  } catch (error) {
    console.error('Error:', error);
    throw error;
  }
}

/**
 * Example 4: Select and crop with custom aspect ratio
 */
export async function selectAndCropCustom(): Promise<CaptureAndCropResult> {
  try {
    const result = await NativeCameraCrop.captureAndCrop({
      source: 'gallery',
      enableCropping: true,
      aspectRatio: { x: 16, y: 9 }, // Custom 16:9 ratio
      resultType: 'uri',
      quality: 90,
    });

    return result;
  } catch (error) {
    console.error('Error:', error);
    throw error;
  }
}

/**
 * Example 5: Take a photo with size constraints
 */
export async function takePhotoWithSizeLimit(): Promise<CaptureAndCropResult> {
  try {
    const result = await NativeCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      aspectRatio: 'free',
      width: 1920,  // Max width
      height: 1080, // Max height
      quality: 80,
      resultType: 'uri',
    });

    console.log('Photo size:', `${result.width}x${result.height}`);
    return result;
  } catch (error) {
    console.error('Error:', error);
    throw error;
  }
}

/**
 * Example 6: Complete workflow with error handling and loading state
 */
export async function completePhotoWorkflow(): Promise<void> {
  // You would typically have these in your component state
  let isLoading = false;
  let imageUri: string | null = null;
  let error: string | null = null;

  try {
    isLoading = true;
    error = null;

    const result = await NativeCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      aspectRatio: '1:1',
      resultType: 'uri',
      quality: 90,
    });

    imageUri = result.value;
    console.log('Success! Image URI:', imageUri);

    // You can now upload the image or display it
    // await uploadImage(imageUri);

  } catch (err: any) {
    if (err.message?.includes('cancelled')) {
      console.log('User cancelled the operation');
      error = 'Operation cancelled';
    } else if (err.message?.includes('permission')) {
      console.error('Permission denied');
      error = 'Camera/Gallery permission is required';
    } else {
      console.error('Unexpected error:', err);
      error = 'An error occurred';
    }
  } finally {
    isLoading = false;
  }
}

/**
 * Example 7: React/Vue component integration
 */
export const PhotoCaptureButton = {
  // For React
  react: `
import React, { useState } from 'react';
import { NativeCameraCrop } from 'capacitor-camera-crop';

export const PhotoCapture: React.FC = () => {
  const [imageUri, setImageUri] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleCapture = async () => {
    try {
      setLoading(true);
      const result = await NativeCameraCrop.captureAndCrop({
        source: 'camera',
        enableCropping: true,
        aspectRatio: '1:1',
      });
      setImageUri(result.value);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <button onClick={handleCapture} disabled={loading}>
        {loading ? 'Processing...' : 'Take Photo'}
      </button>
      {imageUri && <img src={imageUri} alt="Captured" />}
    </div>
  );
};
  `,

  // For Vue
  vue: `
<template>
  <div>
    <button @click="handleCapture" :disabled="loading">
      {{ loading ? 'Processing...' : 'Take Photo' }}
    </button>
    <img v-if="imageUri" :src="imageUri" alt="Captured" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { NativeCameraCrop } from 'capacitor-camera-crop';

const imageUri = ref<string | null>(null);
const loading = ref(false);

const handleCapture = async () => {
  try {
    loading.value = true;
    const result = await NativeCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      aspectRatio: '1:1',
    });
    imageUri.value = result.value;
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};
</script>
  `,
};

/**
 * Example 8: Upload captured image to a server
 */
export async function captureAndUpload(uploadUrl: string): Promise<void> {
  try {
    // Capture the image
    const result = await NativeCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      aspectRatio: '1:1',
      resultType: 'base64',
      quality: 85,
    });

    // Convert base64 to blob for upload
    const base64Response = await fetch(`data:${result.mimeType};base64,${result.value}`);
    const blob = await base64Response.blob();

    // Create FormData
    const formData = new FormData();
    formData.append('image', blob, 'photo.jpg');
    formData.append('width', result.width?.toString() || '');
    formData.append('height', result.height?.toString() || '');

    // Upload
    const response = await fetch(uploadUrl, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error('Upload failed');
    }

    console.log('Upload successful!');
  } catch (error) {
    console.error('Error capturing and uploading:', error);
    throw error;
  }
}

