package com.zxingscanner

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.google.zxing.BarcodeFormat

class ScannerManager : SimpleViewManager<ScannerView>() {

    override fun getName() = "Scanner"

    override fun createViewInstance(context: ThemedReactContext) = ScannerView(context)

    @ReactProp(name = "scanAreaFraction", defaultFloat = 1.0f)
    fun setScanAreaFraction(view: ScannerView, fraction: Float) {
        view.scanAreaFraction = fraction
    }

    @ReactProp(name = "codeTypes")
    fun setCodeTypes(view: ScannerView, types: ReadableArray?) {
        if (types == null) return
        view.codeTypes = (0 until types.size()).mapNotNull { i ->
            when (types.getString(i)) {
                "qr"       -> BarcodeFormat.QR_CODE
                "code-128" -> BarcodeFormat.CODE_128
                "code-39"  -> BarcodeFormat.CODE_39
                "ean-13"   -> BarcodeFormat.EAN_13
                "ean-8"    -> BarcodeFormat.EAN_8
                "code-93"  -> BarcodeFormat.CODE_93
                else       -> null
            }
        }
    }

    override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Any> = mapOf(
        "onScan" to mapOf(
            "phasedRegistrationNames" to mapOf(
                "bubbled" to "onScan",
                "captured" to "onScanCapture",
            )
        )
    )
}
