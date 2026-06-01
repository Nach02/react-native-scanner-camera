import Foundation

@objc(CameraManager)
class CameraManager: RCTViewManager {

    override func view() -> UIView! {
        return CameraView()
    }

    override func receiveCommand(_ view: UIView!, commandKey: String!, args: NSArray!) {
        guard let cameraView = view as? CameraView else { return }
        if commandKey == "takePhoto" {
            cameraView.takePhoto()
        }
    }

    @objc override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}
