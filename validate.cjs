#!/usr/bin/env node

/**
 * Validation script for capacitor-camera-crop plugin
 * Run this to verify the plugin is properly built and configured
 */

const fs = require('fs');
const path = require('path');

const checks = [];
let passed = 0;
let failed = 0;

function check(name, condition, errorMsg) {
  if (condition) {
    checks.push({ name, status: '✅', message: 'OK' });
    passed++;
  } else {
    checks.push({ name, status: '❌', message: errorMsg });
    failed++;
  }
}

console.log('🔍 Validating capacitor-camera-crop plugin...\n');

// Check source files
check(
  'TypeScript definitions',
  fs.existsSync('src/definitions.ts'),
  'src/definitions.ts not found'
);

check(
  'TypeScript index',
  fs.existsSync('src/index.ts'),
  'src/index.ts not found'
);

check(
  'TypeScript web stub',
  fs.existsSync('src/web.ts'),
  'src/web.ts not found'
);

// Check iOS files
check(
  'iOS Swift implementation',
  fs.existsSync('ios/Plugin/CapacitorCameraCropPlugin.swift'),
  'ios/Plugin/CapacitorCameraCropPlugin.swift not found'
);

check(
  'iOS Objective-C bridge',
  fs.existsSync('ios/Plugin/CapacitorCameraCropPlugin.m'),
  'ios/Plugin/CapacitorCameraCropPlugin.m not found'
);

check(
  'iOS Podspec',
  fs.existsSync('CapacitorCameraCrop.podspec'),
  'CapacitorCameraCrop.podspec not found'
);

// Check Android files
check(
  'Android Kotlin implementation',
  fs.existsSync('android/src/main/java/com/example/capacitorcameracrop/CapacitorCameraCropPlugin.kt'),
  'Android plugin not found'
);

check(
  'Android build.gradle',
  fs.existsSync('android/build.gradle'),
  'android/build.gradle not found'
);

check(
  'Android manifest',
  fs.existsSync('android/src/main/AndroidManifest.xml'),
  'AndroidManifest.xml not found'
);

// Check build files
check(
  'TypeScript config',
  fs.existsSync('tsconfig.json'),
  'tsconfig.json not found'
);

check(
  'Rollup config',
  fs.existsSync('rollup.config.js'),
  'rollup.config.js not found'
);

check(
  'Package.json',
  fs.existsSync('package.json'),
  'package.json not found'
);

// Check dist folder
check(
  'Dist folder exists',
  fs.existsSync('dist'),
  'dist/ folder not found - run "bun run build"'
);

if (fs.existsSync('dist')) {
  check(
    'ESM build',
    fs.existsSync('dist/esm/index.js'),
    'dist/esm/index.js not found - build may have failed'
  );

  check(
    'Type definitions',
    fs.existsSync('dist/esm/index.d.ts'),
    'dist/esm/index.d.ts not found - build may have failed'
  );

  check(
    'IIFE bundle',
    fs.existsSync('dist/plugin.js'),
    'dist/plugin.js not found - rollup may have failed'
  );

  check(
    'CommonJS bundle',
    fs.existsSync('dist/plugin.cjs.js'),
    'dist/plugin.cjs.js not found - rollup may have failed'
  );
}

// Check package.json contents
if (fs.existsSync('package.json')) {
  const pkg = JSON.parse(fs.readFileSync('package.json', 'utf8'));
  
  check(
    'Package name',
    pkg.name === 'capacitor-camera-crop',
    'Package name is not correct'
  );

  check(
    'Main entry point',
    pkg.main && fs.existsSync(pkg.main),
    `Main entry point ${pkg.main} not found`
  );

  check(
    'Module entry point',
    pkg.module && fs.existsSync(pkg.module),
    `Module entry point ${pkg.module} not found`
  );

  check(
    'Type definitions entry',
    pkg.types && fs.existsSync(pkg.types),
    `Type definitions ${pkg.types} not found`
  );

  check(
    'Capacitor iOS config',
    pkg.capacitor && pkg.capacitor.ios,
    'Capacitor iOS config not found in package.json'
  );

  check(
    'Capacitor Android config',
    pkg.capacitor && pkg.capacitor.android,
    'Capacitor Android config not found in package.json'
  );
}

// Print results
console.log('📋 Validation Results:\n');
checks.forEach(({ name, status, message }) => {
  console.log(`${status} ${name}: ${message}`);
});

console.log(`\n📊 Summary: ${passed} passed, ${failed} failed\n`);

if (failed === 0) {
  console.log('✅ All checks passed! Plugin is ready to use.\n');
  console.log('Next steps:');
  console.log('  1. Install in your Capacitor app: bun install <path-to-plugin>');
  console.log('  2. Sync with native projects: npx cap sync');
  console.log('  3. Configure iOS Info.plist (see QUICK_START.md)');
  console.log('  4. Configure Android FileProvider (see QUICK_START.md)');
  console.log('  5. Start using the plugin!\n');
  process.exit(0);
} else {
  console.log('❌ Some checks failed. Please fix the issues above.\n');
  if (!fs.existsSync('dist')) {
    console.log('💡 Tip: Run "bun run build" to build the plugin\n');
  }
  process.exit(1);
}

