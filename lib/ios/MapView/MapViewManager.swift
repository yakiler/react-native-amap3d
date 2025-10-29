import Foundation
import MAMapKit

@objc(AMapViewManager)
class AMapViewManager: RCTViewManager {
  override class func requiresMainQueueSetup() -> Bool { false }

  override func view() -> UIView {
    let view = MapView()
    view.delegate = view
    return view
  }

  @objc func moveCamera(_ reactTag: NSNumber, position: NSDictionary, duration: Int) {
    getView(reactTag: reactTag) { view in
      view.moveCamera(position: position, duration: duration)
    }
  }

  @objc func call(_ reactTag: NSNumber, callerId: Double, name: String, args: NSDictionary) {
    getView(reactTag: reactTag) { view in
      view.call(id: callerId, name: name, args: args)
    }
  }

  func getView(reactTag: NSNumber, callback: @escaping (MapView) -> Void) {
    bridge.uiManager.addUIBlock { _, viewRegistry in
      callback(viewRegistry![reactTag] as! MapView)
    }
  }
}

class MapView: MAMapView, MAMapViewDelegate {
  var initialized = false
  var overlayMap: [MABaseOverlay: Overlay] = [:]
  var markerMap: [MAPointAnnotation: Marker] = [:]

  @objc var onLoad: RCTBubblingEventBlock = { _ in }
  @objc var onCameraMove: RCTBubblingEventBlock = { _ in }
  @objc var onCameraIdle: RCTBubblingEventBlock = { _ in }
  @objc var onPress: RCTBubblingEventBlock = { _ in }
  @objc var onPressPoi: RCTBubblingEventBlock = { _ in }
  @objc var onLongPress: RCTBubblingEventBlock = { _ in }
  @objc var onLocation: RCTBubblingEventBlock = { _ in }
  @objc var onCallback: RCTBubblingEventBlock = { _ in }

  // MARK: 初始化
  override init(frame: CGRect) {
    super.init(frame: frame)
    setupUserLocationStyle()
  }

  required init?(coder: NSCoder) {
    super.init(coder: coder)
    setupUserLocationStyle()
  }

  // MARK: 蓝点箭头配置
  private func setupUserLocationStyle() {
    showsUserLocation = true
    // userTrackingMode = .followWithHeading
    userTrackingMode = .none
    delegate = self

    let rep = MAUserLocationRepresentation()
    rep.showsAccuracyRing = true
    rep.showsHeadingIndicator = true
    rep.enablePulseAnnimation = false
    rep.fillColor = UIColor(red: 0, green: 0, blue: 1, alpha: 0.1)
    rep.strokeColor = UIColor(red: 0, green: 0, blue: 1, alpha: 0.3)
    rep.lineWidth = 2.0
    let base64String = """
    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
    """
    if let data = Data(base64Encoded: base64String),
      let image = UIImage(data: data) {
        
        let scaleImage = resizeImage(image, targetSize: CGSize(width: 30, height: 30))
        rep.image = scaleImage
        update(rep)
    }
  }

  func mapInitComplete(_ mapView: MAMapView!) {
    setupUserLocationStyle()
    onLoad(nil)
  }

  // MARK: React接口
  @objc func setInitialCameraPosition(_ json: NSDictionary) {
    if !initialized {
      initialized = true
      moveCamera(position: json)
    }
  }

  func moveCamera(position: NSDictionary, duration: Int = 0) {
    let status = MAMapStatus()
    status.zoomLevel = (position["zoom"] as? Double)?.toCGFloat ?? zoomLevel
    status.cameraDegree = (position["tilt"] as? Double)?.toCGFloat ?? cameraDegree
    status.rotationDegree = (position["bearing"] as? Double)?.toCGFloat ?? rotationDegree
    status.centerCoordinate = (position["target"] as? NSDictionary)?.toCoordinate ?? centerCoordinate
    setMapStatus(status, animated: true, duration: Double(duration) / 1000)
  }

  func call(id: Double, name: String, args: NSDictionary) {
    switch name {
    case "getLatLng":
      callback(id: id, data: convert(args.point, toCoordinateFrom: self).json)
    default:
      break
    }
  }

  func callback(id: Double, data: [String: Any]) {
    onCallback(["id": id, "data": data])
  }

  // MARK: - 子视图管理
  override func didAddSubview(_ subview: UIView) {
    if let overlay = (subview as? Overlay)?.getOverlay() {
      overlayMap[overlay] = subview as? Overlay
      add(overlay)
    }
    if let annotation = (subview as? Marker)?.annotation {
      markerMap[annotation] = subview as? Marker
      addAnnotation(annotation)
    }
  }

  override func removeReactSubview(_ subview: UIView!) {
    super.removeReactSubview(subview)
    if let overlay = (subview as? Overlay)?.getOverlay() {
      overlayMap.removeValue(forKey: overlay)
      remove(overlay)
    }
    if let annotation = (subview as? Marker)?.annotation {
      markerMap.removeValue(forKey: annotation)
      removeAnnotation(annotation)
    }
  }

  // MARK: - Delegate
  func mapView(_: MAMapView, rendererFor overlay: MAOverlay) -> MAOverlayRenderer? {
    if let key = overlay as? MABaseOverlay {
      return overlayMap[key]?.getRenderer()
    }
    return nil
  }

  func mapView(_: MAMapView!, viewFor annotation: MAAnnotation) -> MAAnnotationView? {
    if let key = annotation as? MAPointAnnotation {
      return markerMap[key]?.getView()
    }
    return nil
  }

  func mapView(_: MAMapView!, annotationView view: MAAnnotationView!, didChange newState: MAAnnotationViewDragState, fromOldState _: MAAnnotationViewDragState) {
    if let key = view.annotation as? MAPointAnnotation {
      let marker = markerMap[key]!
      if newState == .starting { marker.onDragStart(nil) }
      if newState == .dragging { marker.onDrag(nil) }
      if newState == .ending { marker.onDragEnd(view.annotation.coordinate.json) }
    }
  }

  func mapView(_: MAMapView!, didAnnotationViewTapped view: MAAnnotationView!) {
    if let key = view.annotation as? MAPointAnnotation {
      markerMap[key]?.onPress(nil)
    }
  }

  func mapView(_: MAMapView!, didSingleTappedAt coordinate: CLLocationCoordinate2D) {
    onPress(coordinate.json)
  }

  func mapView(_: MAMapView!, didTouchPois pois: [Any]!) {
    let poi = pois[0] as! MATouchPoi
    onPressPoi(["name": poi.name!, "id": poi.uid!, "position": poi.coordinate.json])
  }

  func mapView(_: MAMapView!, didLongPressedAt coordinate: CLLocationCoordinate2D) {
    onLongPress(coordinate.json)
  }

  func mapViewRegionChanged(_: MAMapView!) {
    onCameraMove(cameraEvent)
  }

  func mapView(_: MAMapView!, regionDidChangeAnimated _: Bool) {
    onCameraIdle(cameraEvent)
  }

  func mapView(_ mapView: MAMapView!, didUpdate userLocation: MAUserLocation!, updatingLocation: Bool) {
    onLocation(userLocation.json)

    // 仅在方向变化时更新
    guard !updatingLocation, let heading = userLocation.heading?.trueHeading else { return }

    // 获取当前样式
    let rep = MAUserLocationRepresentation()
    rep.showsAccuracyRing = true
    rep.showsHeadingIndicator = true
    rep.enablePulseAnnimation = false
    rep.fillColor = UIColor(red: 0, green: 0, blue: 1, alpha: 0.1)
    rep.strokeColor = UIColor(red: 0, green: 0, blue: 1, alpha: 0.3)
    rep.lineWidth = 2.0

    let base64String = """
    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
    """
    if let data = Data(base64Encoded: base64String),
       let image = UIImage(data: data) {
        let resized = resizeImage(image, targetSize: CGSize(width: 30, height: 30))
        let rotated = imageRotatedByDegrees(oldImage: resized, degrees: CGFloat(heading))
        rep.image = rotated
        mapView.update(rep)
    }
  }
}

private func resizeImage(_ image: UIImage, targetSize: CGSize) -> UIImage {
    let renderer = UIGraphicsImageRenderer(size: targetSize)
    return renderer.image { _ in
        image.draw(in: CGRect(origin: .zero, size: targetSize))
    }
}

private func imageRotatedByDegrees(oldImage: UIImage, degrees: CGFloat) -> UIImage {
  let size = oldImage.size
  UIGraphicsBeginImageContext(size)
  guard let context = UIGraphicsGetCurrentContext() else { return oldImage }

  // 将坐标系移动到中心点
  context.translateBy(x: size.width / 2, y: size.height / 2)
  // 旋转角度
  context.rotate(by: degrees * .pi / 180)
  // 绘制图像
  oldImage.draw(in: CGRect(x: -size.width / 2, y: -size.height / 2, width: size.width, height: size.height))

  let newImage = UIGraphicsGetImageFromCurrentImageContext()
  UIGraphicsEndImageContext()
  return newImage ?? oldImage
}

// MARK: - 扩展（避免命名冲突）
private extension Double {
  var toCGFloat: CGFloat { CGFloat(self) }
}

private extension NSDictionary {
  var toCoordinate: CLLocationCoordinate2D {
    CLLocationCoordinate2D(
      latitude: (self["latitude"] as? Double) ?? 0,
      longitude: (self["longitude"] as? Double) ?? 0
    )
  }
}
