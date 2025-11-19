import Foundation
import Capacitor
import UIKit
import PhotosUI
import CropViewController

@objc(CapacitorCameraCropPlugin)
public class CapacitorCameraCropPlugin: CAPPlugin, UIImagePickerControllerDelegate, UINavigationControllerDelegate, PHPickerViewControllerDelegate, CropViewControllerDelegate {
    private var call: CAPPluginCall?
    private var pendingImage: UIImage?
    
    @objc func captureAndCrop(_ call: CAPPluginCall) {
        self.call = call
        
        let source = call.getString("source") ?? "camera"
        let enableCropping = call.getBool("enableCropping") ?? false
        let useSystemEditing = call.getBool("useSystemEditingIfAvailable") ?? true
        let nativeCropping = call.getBool("nativeCropping") ?? false
        let resultType = call.getString("resultType") ?? "uri"
        
        // Store options for later use
        call.keepAlive = true
        
        // If native cropping is enabled, disable system editing
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
                    call.reject("Camera not available on this device")
                    return
                }
                self.presentPicker(type: .camera, allowsEditing: shouldUseSystemEditing)
            }
        }
    }
    
    private func presentPicker(type: UIImagePickerController.SourceType, allowsEditing: Bool) {
        guard UIImagePickerController.isSourceTypeAvailable(type) else {
            call?.reject("Source type not available")
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
            call?.reject("No image returned from picker")
            return
        }

        picker.dismiss(animated: true) { [weak self] in
            self?.handleImageSelection(image)
        }
    }
    
    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true, completion: nil)
        call?.reject("User cancelled the operation")
    }
    
    // MARK: - PHPickerViewControllerDelegate
    
    @available(iOS 14.0, *)
    public func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        guard let itemProvider = results.first?.itemProvider else {
            picker.dismiss(animated: true, completion: nil)
            call?.reject("No image selected")
            return
        }

        guard itemProvider.canLoadObject(ofClass: UIImage.self) else {
            picker.dismiss(animated: true, completion: nil)
            call?.reject("Cannot load image")
            return
        }

        picker.dismiss(animated: true, completion: nil)

        itemProvider.loadObject(ofClass: UIImage.self) { [weak self] (object, error) in
            if let error = error {
                self?.call?.reject("Error loading image: \(error.localizedDescription)")
                return
            }

            guard let image = object as? UIImage else {
                self?.call?.reject("Could not load image from selection")
                return
            }

            DispatchQueue.main.async {
                self?.handleImageSelection(image)
            }
        }
    }
    
    // MARK: - Image Handling
    
    private func handleImageSelection(_ image: UIImage) {
        guard let call = self.call else {
            return
        }

        let nativeCropping = call.getBool("nativeCropping") ?? false
        let enableCropping = call.getBool("enableCropping") ?? false

        // If native cropping is enabled, present TOCropViewController
        if nativeCropping && enableCropping {
            presentCropViewController(image: image)
        } else {
            // Otherwise, process the image directly
            processImage(image)
        }
    }
    
    // MARK: - TOCropViewController
    
    private func presentCropViewController(image: UIImage) {
        guard let call = self.call else { return }
        
        // Store the image temporarily
        pendingImage = image
        
        // Get aspect ratio
        let aspectRatioString = call.getString("aspectRatio") ?? "free"
        let aspectRatioPreset = mapAspectRatioToPreset(aspectRatioString)
        
        // Create crop view controller
        let cropViewController = CropViewController(image: image)
        cropViewController.delegate = self
        cropViewController.aspectRatioPreset = aspectRatioPreset
        
        // Always lock aspect ratio when native cropping is enabled
        cropViewController.aspectRatioLockEnabled = true
        
        // Hide the aspect ratio picker buttons - user cannot change aspect ratio
        cropViewController.aspectRatioPickerButtonHidden = true
        
        // Handle custom aspect ratio
        if aspectRatioPreset == .presetCustom {
            if let aspectRatioObj = call.getObject("aspectRatio") as? [String: Any],
               let x = aspectRatioObj["x"] as? Double,
               let y = aspectRatioObj["y"] as? Double,
               y > 0 {
                let customAspectRatio = CGSize(width: CGFloat(x), height: CGFloat(y))
                cropViewController.customAspectRatio = customAspectRatio
            }
        }
        
        // Configure appearance
        cropViewController.modalPresentationStyle = .fullScreen
        
        // Present the crop view controller
        DispatchQueue.main.async { [weak self] in
            guard let viewController = self?.bridge?.viewController else {
                call.reject("Unable to present crop view controller")
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
            // Try to parse custom aspect ratio {x: number, y: number}
            if let aspectRatioObj = call?.getObject("aspectRatio") as? [String: Any],
               let x = aspectRatioObj["x"] as? Double,
               let y = aspectRatioObj["y"] as? Double,
               y > 0 {
                // For custom ratios, we'll use presetCustom and set the aspect ratio manually
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
                self?.call?.reject("User cancelled the crop operation")
            }
        }
    }
    
    // MARK: - Image Processing
    
    private func processImage(_ image: UIImage) {
        guard let call = self.call else { return }
        
        var finalImage = image
        
        // Apply resizing if width/height specified
        let maxWidth = call.getInt("width")
        let maxHeight = call.getInt("height")
        
        if maxWidth != nil || maxHeight != nil {
            finalImage = resizeImage(image, maxWidth: maxWidth, maxHeight: maxHeight)
        }
        
        // Get quality
        let quality = call.getInt("quality") ?? 90
        let compressionQuality = CGFloat(quality) / 100.0
        
        // Get result type
        let resultType = call.getString("resultType") ?? "uri"
        
        if resultType == "base64" {
            guard let imageData = finalImage.jpegData(compressionQuality: compressionQuality) else {
                call.reject("Failed to convert image to JPEG")
                return
            }
            
            let base64String = imageData.base64EncodedString()
            
            call.resolve([
                "value": base64String,
                "mimeType": "image/jpeg",
                "width": Int(finalImage.size.width),
                "height": Int(finalImage.size.height)
            ])
        } else {
            // Save to temp file and return URI
            let fileURL = saveTempImage(finalImage, quality: compressionQuality)
            
            call.resolve([
                "value": fileURL.absoluteString,
                "mimeType": "image/jpeg",
                "width": Int(finalImage.size.width),
                "height": Int(finalImage.size.height)
            ])
        }
    }
    
    private func resizeImage(_ image: UIImage, maxWidth: Int?, maxHeight: Int?) -> UIImage {
        let currentWidth = image.size.width
        let currentHeight = image.size.height
        
        var targetWidth = currentWidth
        var targetHeight = currentHeight
        
        // Calculate new size maintaining aspect ratio
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
        
        UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
        image.draw(in: CGRect(origin: .zero, size: size))
        let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return resizedImage ?? image
    }
    
    private func saveTempImage(_ image: UIImage, quality: CGFloat) -> URL {
        guard let imageData = image.jpegData(compressionQuality: quality) else {
            fatalError("Failed to convert image to JPEG")
        }
        
        let filename = UUID().uuidString + ".jpg"
        let fileURL = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(filename)
        
        do {
            try imageData.write(to: fileURL)
        } catch {
            print("Error saving image: \(error)")
        }
        
        return fileURL
    }
}

