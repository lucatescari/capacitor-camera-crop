export type PresetAspectRatio = 'free' | '1:1' | '4:3' | '16:9';
export type CustomAspectRatio = { x: number; y: number };
export type AspectRatio = PresetAspectRatio | CustomAspectRatio;

export interface CaptureAndCropOptions {
  /**
   * Source to pick the image from.
   * @default 'camera'
   */
  source?: 'camera' | 'gallery';

  /**
   * Enable cropping after capturing/selecting the image.
   * - iOS: System editing or TOCropViewController depending on flags
   * - Android: Uses UCrop
   * @default false
   */
  enableCropping?: boolean;

  /**
   * Aspect ratio for cropping.
   *
   * - 'free': User can crop freely (Android/iOS)
   * - Presets: '1:1', '4:3', '16:9'
   * - Custom ratio: { x: number; y: number }
   *
   * Defaults:
   * - iOS nativeCropping=true: uses locked aspect ratio
   * - iOS system editing: OS UI decides
   * - Android: interpreted according to nativeCropping flag
   *
   * @default 'free'
   */
  aspectRatio?: AspectRatio;

  /**
   * Result type: file URI or base64 encoded string.
   * @default 'uri'
   */
  resultType?: 'uri' | 'base64';

  /**
   * Maximum width (in pixels) for the output image. Aspect ratio is preserved.
   * May be set independently of `height`; honored on both platforms.
   */
  width?: number;

  /**
   * Maximum height (in pixels) for the output image. Aspect ratio is preserved.
   * May be set independently of `width`; honored on both platforms.
   */
  height?: number;

  /**
   * Use the iOS system editing UI when cropping. **iOS only.**
   *
   * - iOS:
   *    - If true and enableCropping=true: UIImagePicker's built-in editor is used.
   *    - Ignored when nativeCropping=true.
   * - Android: no effect. Free-vs-locked cropping is controlled solely by
   *   `nativeCropping` (uCrop free mode when false, locked when true).
   *
   * @default true
   */
  useSystemEditingIfAvailable?: boolean;

  /**
   * Use native TOCropViewController on iOS.
   *
   * - iOS:
   *    - If true and enableCropping=true: TOCropViewController is used
   *    - Overrides useSystemEditingIfAvailable
   *
   * - Android:
   *    - If true: UCrop is presented in locked aspect mode
   *    - If false: UCrop free mode
   *
   * @default false
   */
  nativeCropping?: boolean;

  /**
   * JPEG quality (0–100).
   * @default 90
   */
  quality?: number;
}

export interface CaptureAndCropResult {
  /** The file URI or base64 encoded string. */
  value: string;

  /** MIME type of the image (always image/jpeg in this plugin). */
  mimeType: string;

  /** Actual width of the returned image. */
  width: number;

  /** Actual height of the returned image. */
  height: number;
}

export interface CapacitorCameraCropPlugin {
  /**
   * Capture/select image → optional crop → process → return.
   */
  captureAndCrop(options?: CaptureAndCropOptions): Promise<CaptureAndCropResult>;
}
