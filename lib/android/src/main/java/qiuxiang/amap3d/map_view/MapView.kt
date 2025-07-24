package qiuxiang.amap3d.map_view

import android.annotation.SuppressLint
import android.view.View
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MyLocationStyle
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.ThemedReactContext
import qiuxiang.amap3d.getFloat
import qiuxiang.amap3d.toJson
import qiuxiang.amap3d.toLatLng
import qiuxiang.amap3d.toPoint
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.LocationSource
import com.amap.api.maps.model.BitmapDescriptorFactory
import qiuxiang.amap3d.R
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.Bitmap

@SuppressLint("ViewConstructor")
class MapView(context: ThemedReactContext) : TextureMapView(context), LocationSource, AMapLocationListener {
  @Suppress("Deprecation")
  private val eventEmitter =
    context.getJSModule(com.facebook.react.uimanager.events.RCTEventEmitter::class.java)
  private val markerMap = HashMap<String, qiuxiang.amap3d.map_view.Marker>()
  private val polylineMap = HashMap<String, Polyline>()
  private var initialCameraPosition: ReadableMap? = null
  private var locationStyle: MyLocationStyle
  private var locationChangedListener: LocationSource.OnLocationChangedListener? = null
  private var locationClient: AMapLocationClient? = null

  init {
    super.onCreate(null)

    locationStyle = MyLocationStyle()

    // 1. 从资源文件中解码出原始的 Bitmap
    val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.blue_dot)

    // 2. 定义你想要的图标宽度和高度 (单位: 像素 px)
    val widthInDp = 40f
    val heightInDp = 40f

    // 获取屏幕像素密度
    val density = context.resources.displayMetrics.density

    // 将 dp 值转换为 px 值
    val newWidth = (widthInDp * density).toInt()
    val newHeight = (heightInDp * density).toInt()

    // 3. 创建一个按指定尺寸缩放后的新 Bitmap
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

    // 4. 使用缩放后的 Bitmap 创建 BitmapDescriptor
    val resizedIcon = BitmapDescriptorFactory.fromBitmap(scaledBitmap)

    // 5. 将调整好尺寸的图标设置给 locationStyle
    locationStyle.myLocationIcon(resizedIcon)

    // locationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot))
    locationStyle.strokeColor(Color.TRANSPARENT)
    locationStyle.radiusFillColor(Color.TRANSPARENT)
    locationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
    map.myLocationStyle = locationStyle

    map.setLocationSource(this)
    map.isMyLocationEnabled = true

    map.setOnMapLoadedListener { emit(id, "onLoad") }
    map.setOnMapClickListener { latLng -> emit(id, "onPress", latLng.toJson()) }
    map.setOnPOIClickListener { poi -> emit(id, "onPressPoi", poi.toJson()) }
    map.setOnMapLongClickListener { latLng -> emit(id, "onLongPress", latLng.toJson()) }
    map.setOnPolylineClickListener { polyline -> emit(polylineMap[polyline.id]?.id, "onPress") }

    map.setOnMarkerClickListener { marker ->
      markerMap[marker.id]?.let { emit(it.id, "onPress") }
      true
    }

    map.setOnMarkerDragListener(object : AMap.OnMarkerDragListener {
      override fun onMarkerDragStart(marker: Marker) {
        emit(markerMap[marker.id]?.id, "onDragStart")
      }

      override fun onMarkerDrag(marker: Marker) {
        emit(markerMap[marker.id]?.id, "onDrag")
      }

      override fun onMarkerDragEnd(marker: Marker) {
        emit(markerMap[marker.id]?.id, "onDragEnd", marker.position.toJson())
      }
    })

    map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
      override fun onCameraChangeFinish(position: CameraPosition) {
        emit(id, "onCameraIdle", Arguments.createMap().apply {
          putMap("cameraPosition", position.toJson())
          putMap("latLngBounds", map.projection.visibleRegion.latLngBounds.toJson())
        })
      }

      override fun onCameraChange(position: CameraPosition) {
        emit(id, "onCameraMove", Arguments.createMap().apply {
          putMap("cameraPosition", position.toJson())
          putMap("latLngBounds", map.projection.visibleRegion.latLngBounds.toJson())
        })
      }
    })

    map.setOnMultiPointClickListener { item ->
      item.customerId.split("_").let {
        emit(
          it[0].toInt(),
          "onPress",
          Arguments.createMap().apply { putInt("index", it[1].toInt()) },
        )
      }
      false
    }

    map.setOnMyLocationChangeListener {
      if (it.time > 0) {
        emit(id, "onLocation", it.toJson())
      }
    }
  }

  fun emit(id: Int?, event: String, data: WritableMap = Arguments.createMap()) {
    @Suppress("Deprecation")
    id?.let { eventEmitter.receiveEvent(it, event, data) }
  }

  fun add(child: View) {
    if (child is Overlay) {
      child.add(map)
      if (child is qiuxiang.amap3d.map_view.Marker) {
        markerMap[child.marker?.id!!] = child
      }
      if (child is Polyline) {
        polylineMap[child.polyline?.id!!] = child
      }
    }
  }

  fun remove(child: View) {
    if (child is Overlay) {
      child.remove()
      if (child is qiuxiang.amap3d.map_view.Marker) {
        markerMap.remove(child.marker?.id)
      }
      if (child is Polyline) {
        polylineMap.remove(child.polyline?.id)
      }
    }
  }

  private val animateCallback = object : AMap.CancelableCallback {
    override fun onCancel() {}
    override fun onFinish() {}
  }

  fun moveCamera(args: ReadableArray?) {
    val current = map.cameraPosition
    val position = args?.getMap(0)!!
    val target = position.getMap("target")?.toLatLng() ?: current.target
    val zoom = position.getFloat("zoom") ?: current.zoom
    val tilt = position.getFloat("tilt") ?: current.tilt
    val bearing = position.getFloat("bearing") ?: current.bearing
    val cameraUpdate = CameraUpdateFactory.newCameraPosition(
      CameraPosition(target, zoom, tilt, bearing)
    )
    map.animateCamera(cameraUpdate, args.getInt(1).toLong(), animateCallback)
  }

  fun setInitialCameraPosition(position: ReadableMap) {
    if (initialCameraPosition == null) {
      initialCameraPosition = position
      moveCamera(Arguments.createArray().apply {
        pushMap(Arguments.createMap().apply { merge(position) })
        pushInt(0)
      })
    }
  }

  fun call(args: ReadableArray?) {
    val id = args?.getDouble(0)!!
    when (args.getString(1)) {
      "getLatLng" -> callback(
        id,
        // @todo 暂时兼容 0.63
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        map.projection.fromScreenLocation(args.getMap(2)!!.toPoint()).toJson()
      )
    }
  }

  private fun callback(id: Double, data: Any) {
    emit(this.id, "onCallback", Arguments.createMap().apply {
      putDouble("id", id)
      when (data) {
        is WritableMap -> putMap("data", data)
      }
    })
  }

  override fun activate(listener: LocationSource.OnLocationChangedListener) {
    locationChangedListener = listener
    locationClient = AMapLocationClient(context).apply {
      setLocationListener(this@MapView)
      setLocationOption(AMapLocationClientOption().apply {
        locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        isOnceLocation = false
        isNeedAddress = false
        interval = 2000  // 可选，持续定位间隔
      })
      startLocation()
    }
  }

  override fun deactivate() {
    locationClient?.stopLocation()
    locationClient?.onDestroy()
    locationClient = null
  }

  override fun onLocationChanged(location: AMapLocation?) {
    if (location != null && location.errorCode == 0) {
      locationChangedListener?.onLocationChanged(location) // 蓝点更新
    }
  }

}


