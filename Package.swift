// swift-tools-version: 5.9
import PackageDescription

// Swift Package Manager manifest for capacitor-camera-crop.
// Required for Capacitor 8+ iOS apps, which default to SPM instead of CocoaPods.
//
// NOTE: `cap sync` rewrites the capacitor-swift-pm `from:` version below to match
// the consuming app's Capacitor major (7.x for a Cap 7 app, 8.x for a Cap 8 app),
// so this plugin works with both. The committed default targets Capacitor 8.
let package = Package(
    name: "CapacitorCameraCrop",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorCameraCrop",
            targets: ["CapacitorCameraCropPlugin"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0"),
        .package(url: "https://github.com/TimOliver/TOCropViewController.git", from: "2.6.0")
    ],
    targets: [
        .target(
            name: "CapacitorCameraCropPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "CropViewController", package: "TOCropViewController")
            ],
            path: "ios/Plugin"
        )
    ]
)
