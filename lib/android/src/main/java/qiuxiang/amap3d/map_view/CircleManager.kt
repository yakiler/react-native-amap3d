package qiuxiang.amap3d.map_view

import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import qiuxiang.amap3d.toLatLng
import qiuxiang.amap3d.toPx

@Suppress("unused")
internal class CircleManager : SimpleViewManager<Circle>() {
  override fun getName(): String {
    return "AMapCircle"
  }

  override fun createViewInstance(reactContext: ThemedReactContext): Circle {
    return Circle(reactContext)
  }

  @ReactProp(name = "center")
  fun setCenter(circle: Circle, center: ReadableMap) {
    circle.center = center.toLatLng()
  }

  @ReactProp(name = "radius")
  fun setRadius(circle: Circle, radius: Double) {
    circle.radius = radius
  }

  @ReactProp(name = "fillColor", customType = "Color")
  fun setFillColor(circle: Circle, fillColor: Int) {
    circle.fillColor = fillColor
  }

  @ReactProp(name = "strokeColor", customType = "Color")
  fun setStrokeColor(circle: Circle, strokeColor: Int) {
    circle.strokeColor = strokeColor
  }

  @ReactProp(name = "strokeWidth")
  fun setStrokeWidth(circle: Circle, strokeWidth: Float) {
    circle.strokeWidth = strokeWidth.toPx().toFloat()
  }

  @ReactProp(name = "zIndex")
  fun setIndex(circle: Circle, zIndex: Float) {
    circle.zIndex = zIndex
  }

  // ====== 新增文字样式属性 ======
  @ReactProp(name = "text")
  fun setText(circle: Circle, text: String?) {
    circle.textContent = text
  }

  @ReactProp(name = "textColor", customType = "Color")
  fun setTextColor(circle: Circle, color: Int) {
    circle.textColor = color
  }

  @ReactProp(name = "textSize")
  fun setTextSize(circle: Circle, size: Int) {
    circle.textSize = size
  }

  @ReactProp(name = "textBgColor", customType = "Color")
  fun setTextBgColor(circle: Circle, color: Int) {
    circle.textBgColor = color
  }

  @ReactProp(name = "textAlign")
  fun setTextAlign(circle: Circle, map: ReadableMap?) {
    val alignX = map?.getInt("x") ?: 1
    val alignY = map?.getInt("y") ?: 1
    circle.textAlignX = alignX
    circle.textAlignY = alignY
  }


  @ReactProp(name = "offsetPositionOfText")
  fun setOffsetPositionOfText(circle: Circle, offset: ReadableMap?) {
    circle.offsetPositionOfText = offset?.toLatLng()
  }
}
