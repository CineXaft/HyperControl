package com.cinexaft.hypercontrol.hook.systemui

import android.view.View
import com.cinexaft.hypercontrol.base.Entry.Hooks
import com.cinexaft.hypercontrol.base.Entry.State
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import kotlin.math.max

object LockscreenClockHook {
    private const val CLASSIC_ANIMATION =
        "com.android.keyguard.clock.animation.classic.ClassicClockAnimation"
    private const val CLOCK_CONTAINER = "com.android.keyguard.clock.KeyguardClockContainer"
    private const val SIZE_SELECTOR =
        "com.android.systemui.keyguard.domain.interactor.KeyguardClockInteractor\$clockSize\$1"
    private const val CLOCK_SIZE = "com.android.systemui.keyguard.shared.model.ClockSize"
    private const val FLOW_KT = "kotlinx.coroutines.flow.FlowKt"
    private const val SHOW_FULL_TIME = "mShowFullTime"
    private const val NOTIFICATION_MARGIN = "notification_margin_top"

    private class NotificationTransition {
        val savedHideStates = mutableListOf<Pair<Any, Any?>>()
    }

    private val notificationTransition = ThreadLocal<NotificationTransition>()
    @Volatile
    private var notificationActive = false

    @JvmStatic
    fun installClassic(classLoader: ClassLoader) {
        installNotificationSpacing(classLoader)

        val animation = classLoader.loadClass(CLASSIC_ANIMATION)
        val hideState = animation.getDeclaredField("mClassicHideState").apply { isAccessible = true }
        val showState = animation.getDeclaredField("mClassicShowState").apply { isAccessible = true }
        val updateState = animation.getDeclaredMethod("updateAnimationState")

        check(Hooks.install(updateState) { chain ->
            val result = chain.proceed()
            val instance = chain.thisObject
            val transition = notificationTransition.get()
            if (transition != null && instance.javaClass.name == CLASSIC_ANIMATION) {
                val currentHideState = hideState.get(instance)
                val expandedState = showState.get(instance)
                if (currentHideState != null && expandedState != null && currentHideState !== expandedState) {
                    transition.savedHideStates += instance to currentHideState
                    hideState.set(instance, expandedState)
                }
            }
            result
        }) { "Classic clock animation state hook could not be installed" }

        val notificationAnimation = animation.getDeclaredMethod(
            "doAnimationToNotification", Boolean::class.javaPrimitiveType
        )
        check(Hooks.install(notificationAnimation) { chain ->
            notificationActive = chain.getArg(0) == true
            if (State.enabled(22) && notificationActive) withExpandedClock(chain, hideState)
            else chain.proceed()
        }) { "Classic clock notification animation hook could not be installed" }

        val aodAnimation = animation.getDeclaredMethod(
            "doAnimationToAod", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
        )
        check(Hooks.install(aodAnimation) { chain ->
            val fromAod = chain.getArg(0) == false
            val hasNotification = chain.getArg(1) == true
            if (fromAod) notificationActive = hasNotification
            if (State.enabled(22) && fromAod && hasNotification) {
                withExpandedClock(chain, hideState)
            } else {
                chain.proceed()
            }
        }) { "Classic clock AOD return animation hook could not be installed" }
    }

    /**
     * ClassicClockAnimation keeps the expanded clock visible, but the legacy
     * notification layout still asks KeyguardClockContainer for the original
     * fixed clock bottom. Feed it the measured expanded clock height instead.
     */
    private fun installNotificationSpacing(classLoader: ClassLoader) {
        val container = classLoader.loadClass(CLOCK_CONTAINER)
        val clockBottom = runCatching { container.getDeclaredMethod("getClockBottom") }.getOrNull()
        if (clockBottom != null) {
            Hooks.install(clockBottom) { chain ->
                val result = chain.proceed()
                if (result !is Int || !State.enabled(22) || !notificationActive) result
                else expandedClockBottom(chain.thisObject, result)
            }
        }

        val setter = runCatching {
            container.getDeclaredMethod("setMHasNotification", Boolean::class.javaPrimitiveType)
        }.getOrNull()
        if (setter != null) {
            Hooks.install(setter) { chain ->
                notificationActive = chain.getArg(0) == true
                chain.proceed()
            }
        }
    }

    private fun expandedClockBottom(container: Any, original: Int): Int {
        val controller = fieldValue(container, "mMiuiClockController") ?: return original
        val clockView = fieldValue(controller, "mClockView") as? View ?: return original
        if (clockView.height <= 0 || !isExpandedClock(clockView)) return original
        val marginId = clockView.resources.getIdentifier(NOTIFICATION_MARGIN, "dimen", "com.android.systemui")
        val margin = if (marginId == 0) 0 else clockView.resources.getDimensionPixelSize(marginId)
        return max(original, clockView.height + margin)
    }

    private fun isExpandedClock(view: View): Boolean {
        val field = findField(view.javaClass, SHOW_FULL_TIME) ?: return notificationActive
        return runCatching { field.getBoolean(view) }.getOrDefault(notificationActive)
    }

    private fun fieldValue(instance: Any, name: String): Any? = runCatching {
        findField(instance.javaClass, name)?.get(instance)
    }.getOrNull()

    private fun findField(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null) {
            try {
                return current.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }

    private fun withExpandedClock(chain: XposedInterface.Chain, hideState: Field): Any? {
        val previous = notificationTransition.get()
        val transition = previous ?: NotificationTransition().also(notificationTransition::set)
        try {
            return chain.proceed()
        } finally {
            if (previous == null) {
                try {
                    transition.savedHideStates.forEach { (instance, state) -> hideState.set(instance, state) }
                } finally {
                    notificationTransition.remove()
                }
            }
        }
    }

    @JvmStatic
    fun installDynamicSize(classLoader: ClassLoader) {
        val selector = classLoader.loadClass(SIZE_SELECTOR)
        val method = selector.getDeclaredMethod("invoke", Any::class.java, Any::class.java)
        val large = classLoader.loadClass(CLOCK_SIZE).getDeclaredField("LARGE").apply {
            isAccessible = true
        }.get(null)
        val flowOf = classLoader.loadClass(FLOW_KT).getDeclaredMethod("flowOf", Any::class.java).apply {
            isAccessible = true
        }
        val largeClockFlow = flowOf.invoke(null, large)
        check(Hooks.install(method) { chain ->
            val selectedSize = chain.getArg(0) as? Enum<*>
            if (State.enabled(22) && selectedSize?.name == "DYNAMIC") largeClockFlow
            else chain.proceed()
        }) { "Dynamic clock size selector hook could not be installed" }
    }

}
