import { registerPlugin } from '@capacitor/core';

import type { CapacitorCameraCropPlugin } from './definitions';

const CapacitorCameraCrop = registerPlugin<CapacitorCameraCropPlugin>('CapacitorCameraCrop', {
  web: () => import('./web').then(m => new m.CapacitorCameraCropWeb()),
});

export * from './definitions';
export { CapacitorCameraCrop };

