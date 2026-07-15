import Foundation
import Capacitor
import UIKit
import PhotosUI
import CropViewController

@objc(CapacitorCameraCropPlugin)
public class CapacitorCameraCropPlugin: CAPPlugin, CAPBridgedPlugin, UIImagePickerControllerDelegate, UINavigationControllerDelegate, PHPickerViewControllerDelegate, CropViewControllerDelegate {
    // CAPBridgedPlugin conformance — required for Capacitor 6+ / SPM registration.
    // Replaces the legacy CapacitorCameraCropPlugin.m CAP_PLUGIN macro.
    public let identifier = "CapacitorCameraCropPlugin"
    public let jsName = "CapacitorCameraCrop"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "captureAndCrop", returnType: CAPPluginReturnPromise)
    ]

    private var call: CAPPluginCall?
    private var pendingImage: UIImage?

    @objc func captureAndCrop(_ call: CAPPluginCall) {
        // Reject any previous in-flight call so its promise doesn't leak.
        self.call?.reject("Superseded by a new captureAndCrop call")
        self.pendingImage = nil
        self.call = call
        call.keepAlive = true

        let source = call.getString("source") ?? "camera"
        let enableCropping = call.getBool("enableCropping") ?? false
        let useSystemEditing = call.getBool("useSystemEditingIfAvailable") ?? true
        let nativeCropping = call.getBool("nativeCropping") ?? false

        // If native cropping is enabled, disable system editing.
        let shouldUseSystemEditing = useSystemEditing && enableCropping && !nativeCropping

        DispatchQueue.main.async {
            if source == "gallery" {
                if shouldUseSystemEditing {
                    self.presentPicker(type: .photoLibrary, allowsEditing: true)
                } else if #available(iOS 14.0, *) {
                    self.presentPHPicker()
                } else {
                    self.presentPicker(type: .photoLibrary, allowsEditing: false)
                }
            } else {
                if !UIImagePickerController.isSourceTypeAvailable(.camera) {
                    self.rejectCall("Camera not available on this device")
                    return
                }
                self.presentPicker(type: .camera, allowsEditing: shouldUseSystemEditing)
            }
        }
    }

    // MARK: - Call settling (clears stored state so promises never leak)

    private func rejectCall(_ message: String) {
        call?.reject(message)
        call = nil
        pendingImage = nil
    }

    private func resolveCall(_ data: [String: Any]) {
        call?.resolve(data)
        call = nil
        pendingImage = nil
    }

    private func presentPicker(type: UIImagePickerController.SourceType, allowsEditing: Bool) {
        guard UIImagePickerController.isSourceTypeAvailable(type) else {
            rejectCall("Source type not available")
            return
        }

        let picker = UIImagePickerController()
        picker.delegate = self
        picker.sourceType = type
        picker.allowsEditing = allowsEditing

        DispatchQueue.main.async {
            self.bridge?.viewController?.present(picker, animated: true, completion: nil)
        }
    }

    @available(iOS 14.0, *)
    private func presentPHPicker() {
        var config = PHPickerConfiguration()
        config.selectionLimit = 1
        config.filter = .images

        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self

        DispatchQueue.main.async {
            self.bridge?.viewController?.present(picker, animated: true, completion: nil)
        }
    }

    // MARK: - UIImagePickerControllerDelegate

    public func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        guard let image = (info[.editedImage] as? UIImage) ?? (info[.originalImage] as? UIImage) else {
            picker.dismiss(animated: true, completion: nil)
            rejectCall("No image returned from picker")
            return
        }

        // Whether the built-in editor already cropped the image (allowsEditing path).
        let systemEdited = info[.editedImage] != nil

        picker.dismiss(animated: true) { [weak self] in
            self?.handleImageSelection(image, systemEditingUsed: systemEdited)
        }
    }

    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true, completion: nil)
        rejectCall("User cancelled the operation")
    }

    // MARK: - PHPickerViewControllerDelegate

    @available(iOS 14.0, *)
    public func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        guard let itemProvider = results.first?.itemProvider else {
            picker.dismiss(animated: true, completion: nil)
            rejectCall("No image selected")
            return
        }

        guard itemProvider.canLoadObject(ofClass: UIImage.self) else {
            picker.dismiss(animated: true, completion: nil)
            rejectCall("Cannot load image")
            return
        }

        picker.dismiss(animated: true, completion: nil)

        itemProvider.loadObject(ofClass: UIImage.self) { [weak self] (object, error) in
            if let error = error {
                DispatchQueue.main.async {
                    self?.rejectCall("Error loading image: \(error.localizedDescription)")
                }
                return
            }

            guard let image = object as? UIImage else {
                DispatchQueue.main.async {
                    self?.rejectCall("Could not load image from selection")
                }
                return
            }

            DispatchQueue.main.async {
                // PHPicker never applies system editing.
                self?.handleImageSelection(image, systemEditingUsed: false)
            }
        }
    }

    // MARK: - Image Handling

    private func handleImageSelection(_ image: UIImage, systemEditingUsed: Bool) {
        guard let call = self.call else {
            return
        }

        let enableCropping = call.getBool("enableCropping") ?? false

        // Show a crop UI whenever cropping was requested but the system editor
        // didn't already do it — mirrors Android's UCrop free/locked behavior.
        if enableCropping && !systemEditingUsed {
            presentCropViewController(image: image)
        } else {
            processImage(image)
        }
    }

    // MARK: - TOCropViewController

    private func presentCropViewController(image: UIImage) {
        guard let call = self.call else { return }

        pendingImage = image

        let nativeCropping = call.getBool("nativeCropping") ?? false
        let cropViewController = CropViewController(image: image)
        cropViewController.delegate = self

        if nativeCropping {
            // Locked aspect ratio (parity with Android nativeCropping=true).
            let aspectRatioPreset = mapAspectRatioToPreset(call.getString("aspectRatio") ?? "free")
            cropViewController.aspectRatioPreset = aspectRatioPreset
            cropViewController.aspectRatioLockEnabled = true
            cropViewController.aspectRatioPickerButtonHidden = true

            if aspectRatioPreset == .presetCustom {
                if let aspectRatioObj = call.getObject("aspectRatio") as? [String: Any],
                   let x = aspectRatioObj["x"] as? Double,
                   let y = aspectRatioObj["y"] as? Double,
                   y > 0 {
                    cropViewController.customAspectRatio = CGSize(width: CGFloat(x), height: CGFloat(y))
                }
            }
        }
        // else: free-style cropping — user crops freely (parity with Android free mode).

        cropViewController.modalPresentationStyle = .fullScreen

        DispatchQueue.main.async { [weak self] in
            guard let viewController = self?.bridge?.viewController else {
                self?.rejectCall("Unable to present crop view controller")
                return
            }
            viewController.present(cropViewController, animated: true, completion: nil)
        }
    }

    private func mapAspectRatioToPreset(_ aspectRatio: String) -> CropViewControllerAspectRatioPreset {
        switch aspectRatio {
        case "1:1":
            return .presetSquare
        case "4:3":
            return .preset4x3
        case "16:9":
            return .preset16x9
        default:
            if let aspectRatioObj = call?.getObject("aspectRatio") as? [String: Any],
               let x = aspectRatioObj["x"] as? Double,
               let y = aspectRatioObj["y"] as? Double,
               y > 0 {
                return .presetCustom
            }
            return .presetOriginal
        }
    }

    // MARK: - CropViewControllerDelegate

    public func cropViewController(_ cropViewController: CropViewController, didCropToImage image: UIImage, withRect cropRect: CGRect, angle: Int) {
        cropViewController.dismiss(animated: true) { [weak self] in
            self?.processImage(image)
        }
    }

    public func cropViewController(_ cropViewController: CropViewController, didFinishCancelled cancelled: Bool) {
        cropViewController.dismiss(animated: true) { [weak self] in
            if cancelled {
                self?.rejectCall("User cancelled the crop operation")
            }
        }
    }

    // MARK: - Image Processing

    private func processImage(_ image: UIImage) {
        guard let call = self.call else { return }

        var finalImage = image

        // Apply resizing if width/height specified.
        let maxWidth = call.getInt("width")
        let maxHeight = call.getInt("height")

        if maxWidth != nil || maxHeight != nil {
            finalImage = resizeImage(image, maxWidth: maxWidth, maxHeight: maxHeight)
        }

        // Clamp quality to a valid 0-100 range.
        let quality = min(max(call.getInt("quality") ?? 90, 0), 100)
        let compressionQuality = CGFloat(quality) / 100.0

        guard let imageData = finalImage.jpegData(compressionQuality: compressionQuality) else {
            rejectCall("Failed to convert image to JPEG")
            return
        }

        // Report true pixel dimensions (points * scale), matching the encoded file
        // and Android's pixel-based result.
        let pixelWidth = Int(finalImage.size.width * finalImage.scale)
        let pixelHeight = Int(finalImage.size.height * finalImage.scale)

        let resultType = call.getString("resultType") ?? "uri"

        if resultType == "base64" {
            resolveCall([
                "value": imageData.base64EncodedString(),
                "mimeType": "image/jpeg",
                "width": pixelWidth,
                "height": pixelHeight
            ])
        } else {
            guard let fileURL = saveTempImage(imageData) else {
                rejectCall("Failed to save image to disk")
                return
            }
            resolveCall([
                "value": fileURL.absoluteString,
                "mimeType": "image/jpeg",
                "width": pixelWidth,
                "height": pixelHeight
            ])
        }
    }

    private func resizeImage(_ image: UIImage, maxWidth: Int?, maxHeight: Int?) -> UIImage {
        let currentWidth = image.size.width
        let currentHeight = image.size.height

        var targetWidth = currentWidth
        var targetHeight = currentHeight

        // Calculate new size maintaining aspect ratio.
        if let maxW = maxWidth, currentWidth > CGFloat(maxW) {
            targetWidth = CGFloat(maxW)
            targetHeight = currentHeight * (targetWidth / currentWidth)
        }

        if let maxH = maxHeight, targetHeight > CGFloat(maxH) {
            targetHeight = CGFloat(maxH)
            targetWidth = currentWidth * (targetHeight / currentHeight)
        }

        if targetWidth == currentWidth && targetHeight == currentHeight {
            return image
        }

        let size = CGSize(width: targetWidth, height: targetHeight)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1.0
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
    }

    private func saveTempImage(_ imageData: Data) -> URL? {
        let filename = UUID().uuidString + ".jpg"
        let fileURL = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(filename)

        do {
            try imageData.write(to: fileURL)
            return fileURL
        } catch {
            return nil
        }
    }
}
