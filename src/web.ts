import { WebPlugin } from '@capacitor/core';

import type {
  CapacitorCameraCropPlugin,
  CaptureAndCropOptions,
  CaptureAndCropResult,
} from './definitions';

export class CapacitorCameraCropWeb extends WebPlugin implements CapacitorCameraCropPlugin {
  async captureAndCrop(_options: CaptureAndCropOptions): Promise<CaptureAndCropResult> {
    throw this.unimplemented('CapacitorCameraCrop is not supported on the web platform.');
  }
}
