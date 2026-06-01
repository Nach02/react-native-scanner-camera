package com.zxingscanner

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext

class CameraManager : SimpleViewManager<CameraView>() {

    override fun getName() = "Camera"

    override fun createViewInstance(context: ThemedReactContext) = CameraView(context)

    override fun getCommandsMap(): Map<String, Int> = mapOf("takePhoto" to COMMAND_TAKE_PHOTO)

    override fun receiveCommand(view: CameraView, commandId: Int, args: ReadableArray?) {
        if (commandId == COMMAND_TAKE_PHOTO) view.takePhoto()
    }

    override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Any> = mapOf(
        "onReady" to mapOf(
            "phasedRegistrationNames" to mapOf("bubbled" to "onReady", "captured" to "onReadyCapture")
        ),
        "onPhotoTaken" to mapOf(
            "phasedRegistrationNames" to mapOf("bubbled" to "onPhotoTaken", "captured" to "onPhotoTakenCapture")
        ),
        "onPhotoError" to mapOf(
            "phasedRegistrationNames" to mapOf("bubbled" to "onPhotoError", "captured" to "onPhotoErrorCapture")
        ),
    )

    companion object {
        const val COMMAND_TAKE_PHOTO = 1
    }
}
