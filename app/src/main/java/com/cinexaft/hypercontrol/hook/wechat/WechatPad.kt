package com.cinexaft.hypercontrol.hook.wechat

import android.util.Log; import io.github.libxposed.api.XposedInterface; import org.luckypray.dexkit.DexKitBridge; import java.lang.reflect.Method; import java.lang.reflect.Modifier; import kotlin.coroutines.Continuation

internal class WechatPad(
    private val xposed: XposedInterface
) {
    fun install(bridge: DexKitBridge, classLoader: ClassLoader) {
        val keywordMatches = bridge.findMethod {
            matcher {
                usingStrings(TABLET_ENV_KEYWORD)
            }
        }
        val environmentMatches = keywordMatches.filter { method ->
            Modifier.isPublic(method.modifiers) &&
                Modifier.isStatic(method.modifiers) &&
                method.returnTypeName == Boolean::class.javaPrimitiveType!!.name
        }
        xposed.log(
            Log.INFO,
            TAG,
            "DexKit 微信平板判定候选：关键词=${keywordMatches.size}，" +
                "签名=${environmentMatches.size}"
        )
        val tabletEnvironmentMethod = environmentMatches.single().getMethodInstance(classLoader)
        val eligibilityKeywordMatches = bridge.findMethod {
            matcher {
                usingEqStrings(LOGIN_AS_PAD_CGI_PATH)
            }
        }
        val eligibilityMatches = eligibilityKeywordMatches.filter { method ->
            Modifier.isPublic(method.modifiers) &&
                !Modifier.isStatic(method.modifiers) &&
                method.returnTypeName == Any::class.java.name &&
                method.paramTypeNames == listOf(
                    String::class.java.name,
                    String::class.java.name,
                    Continuation::class.java.name
                )
        }
        xposed.log(
            Log.INFO,
            TAG,
            "DexKit 微信副设备登录资格候选：" +
                "关键词=${eligibilityKeywordMatches.size}，签名=${eligibilityMatches.size}"
        )
        val loginAsPadEligibilityMethod = eligibilityMatches.single().getMethodInstance(classLoader)
        hookBooleanMethod(
            tabletEnvironmentMethod,
            TABLET_ENVIRONMENT_HOOK_ID
        )
        hookBooleanMethod(
            loginAsPadEligibilityMethod,
            LOGIN_AS_PAD_ELIGIBILITY_HOOK_ID
        )
        xposed.log(Log.INFO, TAG, "微信平板登录模式已启用")
    }
    private fun hookBooleanMethod(method: Method, hookId: String) {
        method.isAccessible = true
        xposed.hook(method)
            .setId(hookId)
            .intercept { true }
    }
    companion object {
        private const val TAG = "TabletHook"
        private const val TABLET_ENV_KEYWORD = "inTabletEnv"
        private const val LOGIN_AS_PAD_CGI_PATH = "/cgi-bin/micromsg-bin/checkloginaspad"
        private const val TABLET_ENVIRONMENT_HOOK_ID = "wechat_tablet_environment"
        private const val LOGIN_AS_PAD_ELIGIBILITY_HOOK_ID =
            "wechat_login_as_pad_eligibility"
    }
}
