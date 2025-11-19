import { registerPlugin } from '@capacitor/core';

import type { CapacitorCameraCropPlugin, CaptureAndCropOptions, CaptureAndCropResult } from './definitions';
import { OverlayManager } from './overlay-manager';

const CapacitorCameraCropNative = registerPlugin<CapacitorCameraCropPlugin>('CapacitorCameraCrop', {
  web: () => import('./web').then(m => new m.CapacitorCameraCropWeb()),
});

/**
 * Wrapped plugin with overlay management for native platforms
 */
const CapacitorCameraCrop: CapacitorCameraCropPlugin = {
  async captureAndCrop(options?: CaptureAndCropOptions): Promise<CaptureAndCropResult> {
    // Show black screen overlay immediately (respects safe areas)
    OverlayManager.show();

    try {
      // Execute native operation
      const result = await CapacitorCameraCropNative.captureAndCrop(options);
      return result;
    } finally {
      // Always hide overlay, even on error or cancellation
      OverlayManager.hide();
    }
  },
};

export * from './definitions';
export { CapacitorCameraCrop };

