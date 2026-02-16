/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2025- Scandit AG. All rights reserved.
 */

package com.scandit.datacapture.cordova.label

import com.scandit.datacapture.cordova.core.ScanditCaptureCore
import com.scandit.datacapture.cordova.core.utils.CordovaEventEmitter
import com.scandit.datacapture.cordova.core.utils.CordovaMethodCall
import com.scandit.datacapture.cordova.core.utils.CordovaResult
import com.scandit.datacapture.cordova.core.utils.PluginMethod
import com.scandit.datacapture.frameworks.core.CoreModule
import com.scandit.datacapture.frameworks.core.locator.DefaultServiceLocator
import com.scandit.datacapture.frameworks.label.LabelCaptureModule
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import org.json.JSONObject

class ScanditLabelCapture :
    CordovaPlugin() {

    private val emitter = CordovaEventEmitter()

    private val labelCaptureModule = LabelCaptureModule(emitter)

    private var lastLabelCaptureEnabledState: Boolean = false

    private val serviceLocator = DefaultServiceLocator.getInstance()

    override fun pluginInitialize() {
        super.pluginInitialize()
        ScanditCaptureCore.addPlugin(serviceName)
        labelCaptureModule.onCreate(cordova.context)
    }

    override fun onStop() {
        lastLabelCaptureEnabledState = labelCaptureModule.isTopmostModeEnabled()
        labelCaptureModule.setTopmostModeEnabled(false)
    }

    override fun onStart() {
        labelCaptureModule.setTopmostModeEnabled(lastLabelCaptureEnabledState)
    }

    override fun onReset() {
        labelCaptureModule.onDestroy()
        pluginInitialize()
    }

    override fun onDestroy() {
        labelCaptureModule.onDestroy()
        super.onDestroy()
    }

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        return when (action) {
            "executeLabel" -> executeLabel(args, callbackContext)
            "getDefaults" -> getDefaults(callbackContext)
            else -> false
        }.let { true }
    }

    fun executeLabel(args: JSONArray, callbackContext: CallbackContext) {
        val argsJson = args.getJSONObject(0)
        val coreModule = serviceLocator.resolve(
            CoreModule::class.java.simpleName
        ) as? CoreModule ?: return run {
            callbackContext.error("Unable to retrieve the CoreModule from the locator.")
        }

        val result = coreModule.execute(
            CordovaMethodCall(args),
            CordovaResult(callbackContext, emitter),
            labelCaptureModule
        )

        if (!result) {
            val methodName = argsJson.getString("methodName") ?: "unknown"
            callbackContext.error("Unknown method: $methodName")
        }
    }

    @PluginMethod
    fun getDefaults(callbackContext: CallbackContext) {
        callbackContext.success(
            JSONObject(
                mapOf(
                    "LabelCapture" to labelCaptureModule.getDefaults()
                )
            )
        )
    }
}
