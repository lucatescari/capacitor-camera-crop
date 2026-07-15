import { Capacitor, registerPlugin } from '@capacitor/core';

import type {
  CapacitorCameraCropPlugin,
  CaptureAndCropOptions,
  CaptureAndCropResult,
} from './definitions';
import { OverlayManager } from './overlay-manager';

const CapacitorCameraCropNative = registerPlugin<CapacitorCameraCropPlugin>('CapacitorCameraCrop', {
  web: () => import('./web').then((m) => new m.CapacitorCameraCropWeb()),
});

/**
 * Wrapped plugin with overlay management for native platforms.
 */
const CapacitorCameraCrop: CapacitorCameraCropPlugin = {
  async captureAndCrop(options?: CaptureAndCropOptions): Promise<CaptureAndCropResult> {
    // The overlay only exists to prevent flicker between native views. Skip it on
    // the web, where the call rejects as unimplemented and an overlay would just flash.
    const useOverlay = Capacitor.isNativePlatform();
    if (useOverlay) {
      OverlayManager.show();
    }

    try {
      // Execute native operation.
      return await CapacitorCameraCropNative.captureAndCrop(options);
    } finally {
      // Always hide overlay, even on error or cancellation.
      if (useOverlay) {
        OverlayManager.hide();
      }
    }
  },
};

export * from './definitions';
export { CapacitorCameraCrop };
