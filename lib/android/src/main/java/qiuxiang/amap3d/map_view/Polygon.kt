package qiuxiang.amap3d.map_view

import android.content.Context
import android.graphics.Color
import com.amap.api.maps.AMap
import com.amap.api.maps.model.*
import com.facebook.react.views.view.ReactViewGroup

class Polygon(context: Context) : ReactViewGroup(context), Overlay {
  private var polygon: com.amap.api.maps.model.Polygon? = null
  private var text: Text? = null

  var points: List<LatLng> = emptyList()
    set(value) {
      field = value
      polygon?.points = value
      updateTextPosition()
    }

  var strokeWidth: Float = 1f
    set(value) {
      field = value
      polygon?.strokeWidth = value
    }

  var strokeColor: Int = Color.BLACK
    set(value) {
      field = value
      polygon?.strokeColor = value
    }

  var fillColor: Int = Color.BLACK
    set(value) {
      field = value
      polygon?.fillColor = value
    }

  var zIndex: Float = 0f
    set(value) {
      field = value
      polygon?.zIndex = value
      text?.zIndex = value + 1
    }

  // 文字属性
  var textContent: String? = null
    set(value) {
      field = value
      updateText()
    }

  var textColor: Int = Color.BLACK
    set(value) {
      field = value
      text?.fontColor = value
    }

  var textSize: Int = 36
    set(value) {
      field = value
      text?.fontSize = value
    }

  var textBgColor: Int = Color.TRANSPARENT
    set(value) {
      field = value
      text?.backgroundColor = value
    }

  /** 水平对齐方式：AMap.ALIGN_CENTER, AMap.ALIGN_LEFT, AMap.ALIGN_RIGHT */
  var textAlignX: Int = Text.ALIGN_CENTER_HORIZONTAL
    set(value) {
      field = value
      text?.setAlign(value, textAlignY)
    }

  /** 垂直对齐方式：AMap.ALIGN_CENTER_VERTICAL, AMap.ALIGN_TOP, AMap.ALIGN_BOTTOM */
  var textAlignY: Int = Text.ALIGN_CENTER_VERTICAL
    set(value) {
      field = value
      text?.setAlign(textAlignX, value)
    }

  var offsetPositionOfText: LatLng? = null
    set(value) {
      field = value
      updateTextPosition()
    }

  private fun updateText() {
    text?.text = textContent
  }

  private fun getCenterOfPolygon(): LatLng? {
    if (points.isEmpty()) return null
    var latSum = 0.0
    var lngSum = 0.0
    for (p in points) {
      latSum += p.latitude
      lngSum += p.longitude
    }
    val count = points.size
    return LatLng(latSum / count, lngSum / count)
  }

  private fun updateTextPosition() {
    val center = getCenterOfPolygon() ?: return
    val position = if (offsetPositionOfText != null) {
      LatLng(
        center.latitude + offsetPositionOfText!!.latitude,
        center.longitude + offsetPositionOfText!!.longitude
      )
    } else center
    text?.position = position
  }

  override fun add(map: AMap) {
    polygon = map.addPolygon(
      PolygonOptions()
        .addAll(points)
        .strokeColor(strokeColor)
        .strokeWidth(strokeWidth)
        .fillColor(fillColor)
        .zIndex(zIndex)
    )

    val center = getCenterOfPolygon() ?: return
    if (!textContent.isNullOrEmpty()) {
      val position = if (offsetPositionOfText != null) {
        LatLng(
          center.latitude + offsetPositionOfText!!.latitude,
          center.longitude + offsetPositionOfText!!.longitude
        )
      } else center

      val textOptions = TextOptions()
        .position(position)
        .text(textContent)
        .fontColor(textColor)
        .fontSize(textSize)
        .backgroundColor(textBgColor)
        .align(textAlignX, textAlignY)
        .zIndex(zIndex + 1)

      text = map.addText(textOptions)
    }
  }

  override fun remove() {
    polygon?.remove()
    text?.remove()
  }
}
