import { registerPlugin } from '@capacitor/core';

import type { NativeCameraCropPlugin } from './definitions';

const NativeCameraCrop = registerPlugin<NativeCameraCropPlugin>('NativeCameraCrop', {
  web: () => import('./web').then(m => new m.NativeCameraCropWeb()),
});

export * from './definitions';
export { NativeCameraCrop };

