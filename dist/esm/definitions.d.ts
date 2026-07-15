export type PresetAspectRatio = 'free' | '1:1' | '4:3' | '16:9';
export type CustomAspectRatio = {
    x: number;
    y: number;
};
export type AspectRatio = PresetAspectRatio | CustomAspectRatio;
export interface CaptureAndCropOptions {
    source?: 'camera' | 'gallery';
    enableCropping?: boolean;
    aspectRatio?: AspectRatio;
    resultType?: 'uri' | 'base64';
    width?: number;
    height?: number;
    useSystemEditingIfAvailable?: boolean;
    nativeCropping?: boolean;
    quality?: number;
}
export interface CaptureAndCropResult {
    value: string;
    mimeType: string;
    width: number;
    height: number;
}
export interface CapacitorCameraCropPlugin {
    captureAndCrop(options?: CaptureAndCropOptions): Promise<CaptureAndCropResult>;
}
//# sourceMappingURL=definitions.d.ts.map