package qiuxiang.amap3d.map_view

import android.content.Context
import android.graphics.Color
import com.amap.api.maps.AMap
import com.amap.api.maps.model.*
import com.facebook.react.views.view.ReactViewGroup

class Circle(context: Context) : ReactViewGroup(context), Overlay {
  private var circle: com.amap.api.maps.model.Circle? = null
  private var text: Text? = null

  var center: LatLng? = null
    set(value) {
      field = value
      circle?.center = value
      updateTextPosition()
    }

  var radius: Double = 0.0
    set(value) {
      field = value
      circle?.radius = value
    }

  var strokeWidth: Float = 1f
    set(value) {
      field = value
      circle?.strokeWidth = value
    }

  var strokeColor: Int = Color.BLACK
    set(value) {
      field = value
      circle?.strokeColor = value
    }

  var fillColor: Int = Color.TRANSPARENT
    set(value) {
      field = value
      circle?.fillColor = value
    }

  var zIndex: Float = 0f
    set(value) {
      field = value
      circle?.zIndex = value
      text?.zIndex = value + 1
    }

  // ===== 文字属性 =====
  var textContent: String? = null
    set(value) {
      field = value
      updateText()
    }

  var textColor: Int = Color.BLACK
    set(value) {
      field = value
      text?.fontColor = value
      updateText()
    }

  var textSize: Int = 36
    set(value) {
      field = value
      text?.fontSize = value
      updateText()
    }

  var textBgColor: Int = Color.TRANSPARENT
    set(value) {
      field = value
      text?.backgroundColor = value
      updateText()
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
      updateText()
    }

  var offsetPositionOfText: LatLng? = null
    set(value) {
      field = value
      updateTextPosition()
    }

  private fun updateText() {
    text?.text = textContent
  }

  private fun updateTextPosition() {
    val c = center ?: return
    val pos = if (offsetPositionOfText != null) {
      LatLng(
        c.latitude + offsetPositionOfText!!.latitude,
        c.longitude + offsetPositionOfText!!.longitude
      )
    } else c
    text?.position = pos
    updateText()
  }

  override fun add(map: AMap) {
    val c = center ?: return
    circle = map.addCircle(
      CircleOptions()
        .center(c)
        .radius(radius)
        .strokeColor(strokeColor)
        .strokeWidth(strokeWidth)
        .fillColor(fillColor)
        .zIndex(zIndex)
    )

    if (!textContent.isNullOrEmpty()) {
      val pos = if (offsetPositionOfText != null) {
        LatLng(
          c.latitude + offsetPositionOfText!!.latitude,
          c.longitude + offsetPositionOfText!!.longitude
        )
      } else c

      val textOptions = TextOptions()
        .position(pos)
        .text(textContent)
        .fontColor(textColor)
        .fontSize(textSize)
        .backgroundColor(textBgColor)
        .align(textAlignX, textAlignY) // ✅ 修复双参数写法
        .zIndex(zIndex + 1)

      text = map.addText(textOptions)
    }
  }

  override fun remove() {
    circle?.remove()
    text?.remove()
  }
}
