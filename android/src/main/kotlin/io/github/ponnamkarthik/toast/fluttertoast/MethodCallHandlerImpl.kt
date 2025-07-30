package io.github.ponnamkarthik.toast.fluttertoast

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.core.content.ContextCompat
import io.flutter.FlutterInjector
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

internal class MethodCallHandlerImpl(private var context: Context) : MethodCallHandler {

    private var mToast: Toast? = null

    override fun onMethodCall(call: MethodCall, rawResult: MethodChannel.Result) {
    if (this.activity == null) {
        rawResult.error(
            "no_activity",
            "file picker plugin requires a foreground activity",
            null
        )
        return
    }

    val result: MethodChannel.Result = MethodResultWrapper(rawResult)
    val arguments = call.arguments as? HashMap<*, *>
    val method = call.method

    when (method) {
        "clear" -> {
            result.success(activity?.applicationContext?.let { clearCache(it) })
        }

        "save" -> {
            val type = resolveType(arguments?.get("fileType") as String)
            val initialDirectory = arguments["initialDirectory"] as String?
            val bytes = arguments["bytes"] as ByteArray?
            val fileNameWithoutExtension = "${arguments["fileName"]}"
            val fileName =
                if (fileNameWithoutExtension.isNotEmpty() && !fileNameWithoutExtension.contains(".")) {
                    "$fileNameWithoutExtension.${getFileExtension(bytes)}"
                } else fileNameWithoutExtension
            delegate?.saveFile(fileName, type, initialDirectory, bytes, result)
        }

        "custom" -> {
            @Suppress("UNCHECKED_CAST")
            val allowedExtensions = arguments?.get("allowedExtensions") as? ArrayList<String>

            if (allowedExtensions.isNullOrEmpty()) {
                result.error(
                    TAG,
                    "Unsupported filter. Ensure using extension without dot (e.g., jpg, not .jpg).",
                    null
                )
            } else {
                val mimeTypes = getMimeTypes(allowedExtensions)
                delegate?.startFileExplorer(
                    resolveType(method),
                    arguments["allowMultipleSelection"] as? Boolean,
                    arguments["withData"] as? Boolean,
                    mimeTypes,
                    arguments["compressionQuality"] as? Int,
                    result
                )
            }
        }

        else -> {
            val fileType = resolveType(method)
            if (fileType == null) {
                result.notImplemented()
                return
            }

            @Suppress("UNCHECKED_CAST")
            val allowedExtensions = arguments?.get("allowedExtensions") as? ArrayList<String>
            val mimeTypes = getMimeTypes(allowedExtensions)

            delegate?.startFileExplorer(
                fileType,
                arguments?.get("allowMultipleSelection") as? Boolean,
                arguments?.get("withData") as? Boolean,
                mimeTypes,
                arguments?.get("compressionQuality") as? Int,
                result
            )
        }
    }
}

}
