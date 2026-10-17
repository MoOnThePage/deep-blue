package gui

import org.openrndr.panel.elements.*
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

fun Element.doubleControl(
    name: String, min: Double, max: Double,
    prop: KMutableProperty0<Double>, decimals: Int = 2
) = slider {
    label = name
    range = Range(min, max)
    value = prop.get()
    precision = decimals
    events.valueChanged.listen { prop.set(it.newValue) }
}

fun Element.intControl(
    name: String, min: Int, max: Int, prop: KMutableProperty0<Int>
) = slider {
    label = name
    range = Range(min.toDouble(), max.toDouble())
    value = prop.get().toDouble()
    precision = 0
    events.valueChanged.listen { prop.set(it.newValue.roundToInt()) }
}

fun Element.boolControl(
    name: String, prop: KMutableProperty0<Boolean>
) = toggle {
    label = name
    value = prop.get()
    events.valueChanged.listen { prop.set(it.newValue) }
}