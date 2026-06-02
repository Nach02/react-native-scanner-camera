import UIKit
import AVFoundation
import React

@objc(CameraView)
class CameraView: UIView {

    private var session: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var photoOutput: AVCapturePhotoOutput?

    @objc var onReady: RCTBubblingEventBlock?
    @objc var onPhotoTaken: RCTBubblingEventBlock?
    @objc var onPhotoError: RCTBubblingEventBlock?

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupSession()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupSession()
    }

    private func setupSession() {
        let session = AVCaptureSession()
        session.sessionPreset = .photo

        guard
            let device = AVCaptureDevice.default(for: .video),
            let input = try? AVCaptureDeviceInput(device: device),
            session.canAddInput(input)
        else { return }

        session.addInput(input)

        let output = AVCapturePhotoOutput()
        guard session.canAddOutput(output) else { return }
        session.addOutput(output)

        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        self.layer.insertSublayer(layer, at: 0)

        self.session = session
        self.previewLayer = layer
        self.photoOutput = output

        DispatchQueue.global(qos: .userInitiated).async {
            session.startRunning()
            DispatchQueue.main.async {
                self.setNeedsLayout()
                self.onReady?([:])
            }
        }
    }

    func takePhoto() {
        let settings = AVCapturePhotoSettings()
        photoOutput?.capturePhoto(with: settings, delegate: self)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }

    override func willMove(toSuperview newSuperview: UIView?) {
        super.willMove(toSuperview: newSuperview)
        if newSuperview == nil {
            session?.stopRunning()
        } else if session?.isRunning == false {
            DispatchQueue.global(qos: .userInitiated).async { self.session?.startRunning() }
        }
    }
}

extension CameraView: AVCapturePhotoCaptureDelegate {
    func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        if let error = error {
            onPhotoError?(["message": error.localizedDescription])
            return
        }

        guard let data = photo.fileDataRepresentation() else {
            onPhotoError?(["message": "Failed to get photo data"])
            return
        }

        let base64 = data.base64EncodedString()
        let fileName = "scanner_photo_\(Int(Date().timeIntervalSince1970 * 1000)).jpg"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)

        do {
            try data.write(to: url)
            onPhotoTaken?(["uri": url.absoluteString, "base64": base64])
        } catch {
            onPhotoError?(["message": error.localizedDescription])
        }
    }
}
