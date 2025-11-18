package qiuxiang.amap3d.map_view

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import qiuxiang.amap3d.toLatLng
import qiuxiang.amap3d.toLatLngList
import qiuxiang.amap3d.toPx

@Suppress("unused")
internal class PolygonManager : SimpleViewManager<Polygon>() {
  override fun getName(): String {
    return "AMapPolygon"
  }

  override fun createViewInstance(reactContext: ThemedReactContext): Polygon {
    return Polygon(reactContext)
  }

  @ReactProp(name = "points")
  fun setPoints(polygon: Polygon, points: ReadableArray) {
    polygon.points = points.toLatLngList()
  }

  @ReactProp(name = "fillColor", customType = "Color")
  fun setFillColor(polygon: Polygon, fillColor: Int) {
    polygon.fillColor = fillColor
  }

  @ReactProp(name = "strokeColor", customType = "Color")
  fun setStrokeColor(polygon: Polygon, strokeColor: Int) {
    polygon.strokeColor = strokeColor
  }

  @ReactProp(name = "strokeWidth")
  fun setStrokeWidth(polygon: Polygon, strokeWidth: Float) {
    polygon.strokeWidth = strokeWidth.toPx().toFloat()
  }

  @ReactProp(name = "zIndex")
  fun setIndex(polygon: Polygon, zIndex: Float) {
    polygon.zIndex = zIndex
  }

  // ====== 新增文字属性 ======
  @ReactProp(name = "text")
  fun setText(polygon: Polygon, text: String?) {
    polygon.textContent = text
  }

  @ReactProp(name = "textColor", customType = "Color")
  fun setTextColor(polygon: Polygon, color: Int) {
    polygon.textColor = color
  }

  @ReactProp(name = "textSize")
  fun setTextSize(polygon: Polygon, size: Int) {
    polygon.textSize = size
  }

  @ReactProp(name = "textBgColor", customType = "Color")
  fun setTextBgColor(polygon: Polygon, color: Int) {
    polygon.textBgColor = color
  }

  @ReactProp(name = "textAlign")
  fun setTextAlign(circle: Circle, map: ReadableMap?) {
    val alignX = map?.getInt("x") ?: 1
    val alignY = map?.getInt("y") ?: 1
    circle.textAlignX = alignX
    circle.textAlignY = alignY
  }


  @ReactProp(name = "offsetPositionOfText")
  fun setOffsetPositionOfText(polygon: Polygon, offset: ReadableMap?) {
    polygon.offsetPositionOfText = offset?.toLatLng()
  }
}
