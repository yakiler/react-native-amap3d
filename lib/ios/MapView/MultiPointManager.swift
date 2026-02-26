@objc(AMapMultiPointManager)
class AMapMultiPointManager: RCTViewManager {
  override class func requiresMainQueueSetup() -> Bool { false }

  override func view() -> UIView {
    let view = MultiPoint()
    view.imageLoader = bridge.module(forName: "ImageLoader") as? RCTImageLoader
    return view
  }
}

class MultiPoint: UIView, Overlay, MAMultiPointOverlayRendererDelegate {
  var imageLoader: RCTImageLoader?
  var overlay: MAMultiPointOverlay?
  var renderer: MAMultiPointOverlayRenderer?
  var icon: UIImage?

  @objc var onPress: RCTDirectEventBlock = { _ in }

  @objc func setIcon(_ icon: NSDictionary) {
    imageLoader?.loadImage(icon) { image in
      self.renderer?.icon = image
    }
  }

  @objc func setItems(_ items: NSArray) {
    overlay = MAMultiPointOverlay(multiPointItems: items.map { it -> MAMultiPointItem in
      let item = MAMultiPointItem()
      item.coordinate = (it as! NSDictionary).coordinate
      return item
    })
  }

  func getOverlay() -> MABaseOverlay { overlay! }
  func getRenderer() -> MAOverlayRenderer {
    if renderer == nil {
      renderer = MAMultiPointOverlayRenderer(multiPointOverlay: overlay)
      renderer?.icon = icon
      renderer?.delegate = self
    }
    return renderer!
  }

  func multiPointOverlayRenderer(_: MAMultiPointOverlayRenderer!, didItemTapped item: MAMultiPointItem!) {
    guard let overlay = overlay else { return }
    
    // 通过坐标比较来查找索引，而不是使用对象比较
    let tappedCoordinate = item.coordinate
    for (index, overlayItem) in overlay.items.enumerated() {
      if overlayItem.coordinate.latitude == tappedCoordinate.latitude &&
         overlayItem.coordinate.longitude == tappedCoordinate.longitude {
        onPress(["index": index])
        return
      }
    }
  }
}
