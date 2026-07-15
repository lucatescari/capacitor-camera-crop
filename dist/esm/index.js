import { registerPlugin } from '@capacitor/core';
import { OverlayManager } from './overlay-manager';
const CapacitorCameraCropNative = registerPlugin('CapacitorCameraCrop', {
    web: () => import('./web').then(m => new m.CapacitorCameraCropWeb()),
});
const CapacitorCameraCrop = {
    async captureAndCrop(options) {
        OverlayManager.show();
        try {
            const result = await CapacitorCameraCropNative.captureAndCrop(options);
            return result;
        }
        finally {
            OverlayManager.hide();
        }
    },
};
export * from './definitions';
export { CapacitorCameraCrop };
//# sourceMappingURL=index.js.map