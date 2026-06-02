import UIKit
import AVFoundation
import React

@objc(ScannerView)
class ScannerView: UIView {

    private var session: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var metadataOutput: AVCaptureMetadataOutput?
    private var lastScannedAt: TimeInterval = 0
    private var debugLayer: CALayer?

    @objc var onScan: RCTBubblingEventBlock?

    @objc var showScanArea: Bool = false {
        didSet { updateDebugLayer() }
    }

    @objc var scanAreaFraction: CGFloat = 1.0 {
        didSet { updateRectOfInterest() }
    }

    @objc var codeTypes: NSArray = ["qr", "code-128", "code-39"] {
        didSet { refreshMetadataTypes() }
    }

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
        session.sessionPreset = .high

        guard
            let device = AVCaptureDevice.default(for: .video),
            let input = try? AVCaptureDeviceInput(device: device),
            session.canAddInput(input)
        else { return }

        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else { return }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = resolvedMetadataTypes()

        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        self.layer.insertSublayer(layer, at: 0)

        self.session = session
        self.previewLayer = layer
        self.metadataOutput = output

        DispatchQueue.global(qos: .userInitiated).async {
            session.startRunning()
            DispatchQueue.main.async {
                // metadataOutputRectConverted requires the session to be running.
                // Set the frame explicitly before converting, since layoutSubviews()
                // may have already fired while the session was still starting.
                if self.bounds != .zero {
                    self.previewLayer?.frame = self.bounds
                }
                self.updateRectOfInterest()
                self.setNeedsLayout()
            }
        }
    }

    private func refreshMetadataTypes() {
        metadataOutput?.metadataObjectTypes = resolvedMetadataTypes()
        updateRectOfInterest()
    }

    private func resolvedMetadataTypes() -> [AVMetadataObject.ObjectType] {
        guard let types = codeTypes as? [String] else { return [] }
        return types.compactMap { type in
            switch type {
            case "qr":       return .qr
            case "code-128": return .code128
            case "code-39":  return .code39
            case "ean-13":   return .ean13
            case "ean-8":    return .ean8
            case "code-93":  return .code93
            default:         return nil
            }
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
        updateRectOfInterest()
    }

    private func updateRectOfInterest() {
        guard let layer = previewLayer,
              let output = metadataOutput,
              layer.bounds != .zero else { return }
        let viewBounds = layer.bounds
        let size = viewBounds.width * scanAreaFraction
        let x = (viewBounds.width - size) / 2
        let y = (viewBounds.height - size) / 2
        let layerRect = CGRect(x: x, y: y, width: size, height: size)
        output.rectOfInterest = layer.metadataOutputRectConverted(fromLayerRect: layerRect)
        updateDebugLayer(rect: layerRect)
    }

    private func updateDebugLayer(rect: CGRect? = nil) {
        if showScanArea {
            if debugLayer == nil {
                let dl = CALayer()
                dl.borderColor = UIColor.red.cgColor
                dl.borderWidth = 2
                dl.backgroundColor = UIColor.red.withAlphaComponent(0.15).cgColor
                self.layer.addSublayer(dl)
                debugLayer = dl
            }
            if let r = rect {
                CATransaction.begin()
                CATransaction.setDisableActions(true)
                debugLayer?.frame = r
                CATransaction.commit()
            }
        } else {
            debugLayer?.removeFromSuperlayer()
            debugLayer = nil
        }
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

extension ScannerView: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput objects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        let now = Date().timeIntervalSince1970
        guard now - lastScannedAt > 0.3,
              let readable = objects.first as? AVMetadataMachineReadableCodeObject,
              let value = readable.stringValue
        else { return }

        lastScannedAt = now
        onScan?(["value": value])
    }
}
