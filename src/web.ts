import { WebPlugin } from '@capacitor/core';

import type {
  NativeCameraCropPlugin,
  CaptureAndCropOptions,
  CaptureAndCropResult,
} from './definitions';

export class NativeCameraCropWeb extends WebPlugin implements NativeCameraCropPlugin {
  async captureAndCrop(_options: CaptureAndCropOptions): Promise<CaptureAndCropResult> {
    throw this.unimplemented('NativeCameraCrop is not supported on the web platform.');
  }
}

