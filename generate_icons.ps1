Add-Type -AssemblyName System.Drawing

function Generate-Icon {
    param(
        [int]$size,
        [bool]$isRound,
        [string]$outputPath
    )

    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)

    # 1. Background Gradient
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.Color]::FromArgb(255, 0x8B, 0x5C, 0xF6),
        [System.Drawing.Color]::FromArgb(255, 0x5B, 0x21, 0xB6),
        45
    )

    if ($isRound) {
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse(0, 0, $size, $size)
        $g.SetClip($path)
    } else {
        $radius = [int]($size * 0.22)
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $d = $radius * 2
        $path.AddArc(0, 0, $d, $d, 180, 90)
        $path.AddArc($size - $d, 0, $d, $d, 270, 90)
        $path.AddArc($size - $d, $size - $d, $d, $d, 0, 90)
        $path.AddArc(0, $size - $d, $d, $d, 90, 90)
        $path.CloseFigure()
        $g.SetClip($path)
    }

    $g.FillRectangle($brush, $rect)

    # 2. Bottom Right Dark Wave Curve
    $wavePath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $wavePath.AddBezier(
        0, [int]($size * 0.85),
        [int]($size * 0.35), [int]($size * 0.70),
        [int]($size * 0.75), [int]($size * 0.50),
        $size, [int]($size * 0.28)
    )
    $wavePath.AddLine($size, $size, 0, $size)
    $wavePath.CloseFigure()

    $waveBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.Color]::FromArgb(255, 0x68, 0x1F, 0xBE),
        [System.Drawing.Color]::FromArgb(255, 0x3B, 0x07, 0x64),
        60
    )
    $g.FillPath($waveBrush, $wavePath)

    # 3. Clipboard White Body
    $scale = $size / 108.0
    $clipW = [int](40 * $scale)
    $clipH = [int](51 * $scale)
    $clipX = [int](29 * $scale)
    $clipY = [int](29 * $scale)
    $clipR = [int](8 * $scale)

    $clipPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $cd = $clipR * 2
    $clipPath.AddArc($clipX, $clipY, $cd, $cd, 180, 90)
    $clipPath.AddArc($clipX + $clipW - $cd, $clipY, $cd, $cd, 270, 90)
    $clipPath.AddArc($clipX + $clipW - $cd, $clipY + $clipH - $cd, $cd, $cd, 0, 90)
    $clipPath.AddArc($clipX, $clipY + $clipH - $cd, $cd, $cd, 90, 90)
    $clipPath.CloseFigure()

    # Shadow
    $g.TranslateTransform([int](2 * $scale), [int](2 * $scale))
    $shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(90, 0x30, 0x05, 0x52))
    $g.FillPath($shadowBrush, $clipPath)
    $g.ResetTransform()

    if ($isRound) {
        $g.SetClip($path)
    } else {
        $g.SetClip($path)
    }

    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.FillPath($whiteBrush, $clipPath)

    # 4. Top Clip Handle
    $handleW = [int](18 * $scale)
    $handleH = [int](10 * $scale)
    $handleX = [int](40 * $scale)
    $handleY = [int](24 * $scale)
    $handleR = [int](4.5 * $scale)

    $handlePath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $hd = $handleR * 2
    $handlePath.AddArc($handleX, $handleY, $hd, $hd, 180, 90)
    $handlePath.AddArc($handleX + $handleW - $hd, $handleY, $hd, $hd, 270, 90)
    $handlePath.AddArc($handleX + $handleW - $hd, $handleY + $handleH - $hd, $hd, $hd, 0, 90)
    $handlePath.AddArc($handleX, $handleY + $handleH - $hd, $hd, $hd, 90, 90)
    $handlePath.CloseFigure()

    $purpleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 0x7C, 0x3A, 0xED))
    $g.FillPath($purpleBrush, $handlePath)
    $g.FillEllipse($whiteBrush, [float](46.8 * $scale), [float](26.5 * $scale), [float](4.4 * $scale), [float](4.4 * $scale))

    # 5. Checkmarks & Lavender Lines
    $darkPurplePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 0x6D, 0x28, 0xD9), [float](3.5 * $scale))
    $darkPurplePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $darkPurplePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

    $g.DrawLine($darkPurplePen, [float](31 * $scale), [float](41.5 * $scale), [float](33.8 * $scale), [float](44.3 * $scale))
    $g.DrawLine($darkPurplePen, [float](33.8 * $scale), [float](44.3 * $scale), [float](41 * $scale), [float](37.1 * $scale))

    $g.DrawLine($darkPurplePen, [float](31 * $scale), [float](52.5 * $scale), [float](33.8 * $scale), [float](55.3 * $scale))
    $g.DrawLine($darkPurplePen, [float](33.8 * $scale), [float](55.3 * $scale), [float](41 * $scale), [float](48.1 * $scale))

    $g.DrawLine($darkPurplePen, [float](31 * $scale), [float](63.5 * $scale), [float](33.8 * $scale), [float](66.3 * $scale))
    $g.DrawLine($darkPurplePen, [float](33.8 * $scale), [float](66.3 * $scale), [float](41 * $scale), [float](59.1 * $scale))

    $lavenderBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 0xC4, 0xB5, 0xFD))
    $g.FillRectangle($lavenderBrush, [float](45 * $scale), [float](39 * $scale), [float](19 * $scale), [float](4 * $scale))
    $g.FillRectangle($lavenderBrush, [float](45 * $scale), [float](50 * $scale), [float](19 * $scale), [float](4 * $scale))
    $g.FillRectangle($lavenderBrush, [float](45 * $scale), [float](61 * $scale), [float](11 * $scale), [float](4 * $scale))

    # 6. Clock Emblem
    $clockDarkBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 0x4C, 0x1D, 0x95))
    $g.FillEllipse($clockDarkBrush, [float](50.5 * $scale), [float](35.5 * $scale), [float](33 * $scale), [float](33 * $scale))
    $g.FillEllipse($whiteBrush, [float](53.5 * $scale), [float](38.5 * $scale), [float](27 * $scale), [float](27 * $scale))

    $g.FillRectangle($clockDarkBrush, [float](66 * $scale), [float](44 * $scale), [float](2.2 * $scale), [float](8 * $scale))
    $clockHandPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 0x4C, 0x1D, 0x95), [float](2.2 * $scale))
    $clockHandPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $clockHandPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawLine($clockHandPen, [float](67 * $scale), [float](52 * $scale), [float](73.5 * $scale), [float](58.5 * $scale))
    $g.FillEllipse($clockDarkBrush, [float](65 * $scale), [float](50 * $scale), [float](4 * $scale), [float](4 * $scale))

    # 7. Sparkle Rays
    $sparklePen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, [float](3 * $scale))
    $sparklePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $sparklePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawLine($sparklePen, [float](73 * $scale), [float](23.5 * $scale), [float](77.5 * $scale), [float](17 * $scale))
    $g.DrawLine($sparklePen, [float](78.5 * $scale), [float](29.5 * $scale), [float](85 * $scale), [float](26.3 * $scale))

    $bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

$densities = @(
    @{ folder = "mipmap-mdpi"; size = 48 },
    @{ folder = "mipmap-hdpi"; size = 72 },
    @{ folder = "mipmap-xhdpi"; size = 96 },
    @{ folder = "mipmap-xxhdpi"; size = 144 },
    @{ folder = "mipmap-xxxhdpi"; size = 192 }
)

$resDir = "D:\ToDoGit\ToDoList\app\src\main\res"

foreach ($d in $densities) {
    $dirPath = Join-Path $resDir $d.folder
    if (-not (Test-Path $dirPath)) {
        New-Item -ItemType Directory -Path $dirPath | Out-Null
    }
    $iconPath = Join-Path $dirPath "ic_launcher.png"
    $roundIconPath = Join-Path $dirPath "ic_launcher_round.png"

    Generate-Icon -size $d.size -isRound $false -outputPath $iconPath
    Generate-Icon -size $d.size -isRound $true -outputPath $roundIconPath
}
