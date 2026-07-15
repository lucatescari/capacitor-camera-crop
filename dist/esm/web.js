import { WebPlugin } from '@capacitor/core';
export class CapacitorCameraCropWeb extends WebPlugin {
    async captureAndCrop(_options) {
        throw this.unimplemented('CapacitorCameraCrop is not supported on the web platform.');
    }
}
//# sourceMappingURL=web.js.map