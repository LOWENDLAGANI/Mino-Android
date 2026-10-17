package com.minetallest.mino.notify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.minetallest.mino.R
import org.json.JSONObject

/**
 * Xiaomi HyperIsland ("超级岛") support, implemented from the public
 * `miui.focus.*` notification contract — no vendor SDK is required.
 *
 * The island is only a presentation layer: when the device does not expose it,
 * [ReplyNotifier] still posts a normal notification.
 */
object HyperIsland {

    const val KEY_PARAM = "miui.focus.param"
    private const val KEY_PICS = "miui.focus.pics"
    private const val KEY_TICKER = "miui.focus.ticker"
    private const val PIC_KEY = "miui.focus.pic_mino"
    private const val FOCUS_PROVIDER = "content://miui.statusbar.notification.public"

    data class Payload(val json: String, val extras: Bundle)

    /**
     * Mirrors the vendor contract: HyperOS device, focus permission granted for
     * this package, and the island feature flag enabled.
     */
    fun isSupported(context: Context): Boolean =
        isXiaomiDevice() && hasFocusPermission(context) && isIslandFeatureEnabled()

    /**
     * Builds the `param_v2` JSON and the picture bundle expected by HyperOS.
     * Returns null on devices where the island is unavailable.
     */
    fun payload(context: Context, chatTitle: String): Payload? {
        if (!isSupported(context)) return null
        val extras = Bundle().apply {
            putBundle(
                KEY_PICS,
                Bundle().apply {
                    putParcelable(PIC_KEY, Icon.createWithResource(context, R.mipmap.ic_launcher))
                },
            )
            putString(KEY_TICKER, context.getString(R.string.notification_ready_title))
        }
        return Payload(json = buildJson(context, chatTitle), extras = extras)
    }

    private fun buildJson(context: Context, chatTitle: String): String {
        val root = JSONObject()

        val params = JSONObject()
            .put("protocol", 3)
            .put("business", "mino")
            .put("updatable", true)
            .put("ticker", context.getString(R.string.notification_ready_title))
            .put("enableFloat", true)
            .put("isShowNotification", true)
            .put("smallWindowInfo", JSONObject().put("targetPage", "${context.packageName}.MainActivity"))
            .put(
                "chatInfo",
                JSONObject()
                    .put("type", 1)
                    .put("title", "Mino")
                    .put("content", chatTitle)
                    .put("picProfile", PIC_KEY)
                    .put("picProfileDark", PIC_KEY)
                    .put("appIconPkg", context.packageName),
            )

        val island = JSONObject()
            .put("islandProperty", 1)
            .put("islandPriority", 2)
            .put("dismissIsland", false)
            .put("maxSize", true)
            .put("needCloseAnimation", true)
            .put(
                "smallIslandArea",
                JSONObject().put("picInfo", iconJson()),
            )
            .put(
                "bigIslandArea",
                JSONObject()
                    .put(
                        "imageTextInfoLeft",
                        JSONObject()
                            .put("type", 1)
                            .put("picInfo", iconJson())
                            .put(
                                "textInfo",
                                JSONObject()
                                    .put("title", "Mino")
                                    .put("content", chatTitle),
                            ),
                    )
                    .put(
                        "imageTextInfoRight",
                        JSONObject()
                            .put("type", 2)
                            .put("textInfo", JSONObject().put("title", context.getString(R.string.notification_ready_open))),
                    ),
            )

        params.put("param_island", island)
        root.put("param_v2", params)
        root.put("isShowNotification", true)
        return root.toString()
    }

    private fun iconJson(): JSONObject = JSONObject()
        .put("type", 1)
        .put("pic", PIC_KEY)

    private fun isXiaomiDevice(): Boolean =
        Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    private fun hasFocusPermission(context: Context): Boolean = try {
        val result = context.contentResolver.call(
            Uri.parse(FOCUS_PROVIDER),
            "canShowFocus",
            null,
            Bundle().apply { putString("package", context.packageName) },
        )
        result?.getBoolean("canShowFocus", false) ?: false
    } catch (_: Throwable) {
        false
    }

    @SuppressLint("PrivateApi")
    private fun isIslandFeatureEnabled(): Boolean = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val getBoolean = clazz.getDeclaredMethod(
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType,
        )
        getBoolean.invoke(null, "persist.sys.feature.island", false) as? Boolean ?: false
    } catch (_: Throwable) {
        false
    }
}
