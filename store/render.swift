// Renders the Play Store icon and feature graphic. Run: swift render.swift
import AppKit

func cap() -> NSBezierPath { // graduation cap from res/drawable/ic_tau_logo.xml (108x108 viewport)
    let p = NSBezierPath()
    p.move(to: .init(x: 54, y: 24)); p.line(to: .init(x: 20, y: 40)); p.line(to: .init(x: 54, y: 56))
    p.line(to: .init(x: 84, y: 41.88)); p.line(to: .init(x: 84, y: 62)); p.line(to: .init(x: 88, y: 62))
    p.line(to: .init(x: 88, y: 40)); p.close()
    p.move(to: .init(x: 30, y: 52.88)); p.line(to: .init(x: 30, y: 66))
    p.curve(to: .init(x: 54, y: 84), controlPoint1: .init(x: 30, y: 75.94), controlPoint2: .init(x: 40.75, y: 84))
    p.curve(to: .init(x: 78, y: 66), controlPoint1: .init(x: 67.25, y: 84), controlPoint2: .init(x: 78, y: 75.94))
    p.line(to: .init(x: 78, y: 52.88)); p.line(to: .init(x: 54, y: 64)); p.line(to: .init(x: 30, y: 52.88)); p.close()
    return p
}

func render(_ file: String, _ w: Int, _ h: Int, _ draw: (CGFloat, CGFloat) -> Void) {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: w, pixelsHigh: h, bitsPerSample: 8, samplesPerPixel: 4,
                               hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    let ctx = NSGraphicsContext(bitmapImageRep: rep)!
    NSGraphicsContext.current = ctx
    // y-down coordinates, like SVG / Android
    ctx.cgContext.translateBy(x: 0, y: CGFloat(h)); ctx.cgContext.scaleBy(x: 1, y: -1)
    NSGradient(starting: NSColor(srgbRed: 0.545, green: 0.361, blue: 0.965, alpha: 1),
               ending: NSColor(srgbRed: 0.357, green: 0.129, blue: 0.714, alpha: 1))!
        .draw(in: NSRect(x: 0, y: 0, width: w, height: h), angle: 45)
    draw(CGFloat(w), CGFloat(h))
    NSGraphicsContext.restoreGraphicsState()
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: file))
}

func drawCap(x: CGFloat, y: CGFloat, size: CGFloat) {
    let p = cap()
    var t = AffineTransform(translationByX: x, byY: y); t.scale(size / 108)
    p.transform(using: t)
    NSColor.white.setFill(); p.fill()
}

func text(_ s: String, rightX: CGFloat, baselineY: CGFloat, size: CGFloat, bold: Bool, alpha: CGFloat = 1) {
    let font = bold ? NSFont.boldSystemFont(ofSize: size) : NSFont.systemFont(ofSize: size)
    let attr = NSAttributedString(string: s, attributes: [.font: font, .foregroundColor: NSColor.white.withAlphaComponent(alpha)])
    let sz = attr.size()
    let ctx = NSGraphicsContext.current!.cgContext
    ctx.saveGState() // text must be drawn un-flipped
    ctx.translateBy(x: rightX - sz.width, y: baselineY); ctx.scaleBy(x: 1, y: -1)
    attr.draw(at: .zero)
    ctx.restoreGState()
}

render("icon-512.png", 512, 512) { w, _ in drawCap(x: w * 0.09, y: w * 0.07, size: w * 0.82) }

render("feature-1024x500.png", 1024, 500) { _, _ in
    NSColor.white.withAlphaComponent(0.08).setFill()
    NSBezierPath(ovalIn: NSRect(x: 790, y: -80, width: 280, height: 280)).fill()
    NSBezierPath(ovalIn: NSRect(x: -40, y: 340, width: 240, height: 240)).fill()
    drawCap(x: 70, y: 90, size: 320)
    text("StudyMate", rightX: 950, baselineY: 140, size: 88, bold: true)
    text("כל הלימודים שלך במקום אחד", rightX: 950, baselineY: 250, size: 46, bold: true)
    text("משימות · Moodle · תזכורות · טיימר פוקוס", rightX: 950, baselineY: 320, size: 32, bold: false, alpha: 0.85)
}
