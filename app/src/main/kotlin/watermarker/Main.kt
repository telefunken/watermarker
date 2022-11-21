package watermarker

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

class Main {
companion object {
    @JvmStatic
    fun main(args: Array<String>){
        println(Main().main())
    }
}

fun error(msg: String) {
    println(msg)
    exitProcess(1)
}

fun main() {
    print("Input the image filename:")
    val imageFileName = readln()
    val imageFile = File(imageFileName)
    if (!imageFile.exists()) {
        error("The file $imageFileName doesn't exist.")
    }
    val image = ImageIO.read(imageFile)
    if (image.colorModel.numColorComponents != 3) {
        error("The number of image color components isn't 3.")
    }
    if (image.colorModel.pixelSize != 24 && image.colorModel.pixelSize != 32) {
        error("The image isn't 24 or 32-bit.")
    }

    print("Input the watermark image filename:")
    val watermarkFileName = readln()
    val watermarkFile = File(watermarkFileName)
    if (!watermarkFile.exists()) {
        error("The file $watermarkFileName doesn't exist.")
    }
    val watermarkImage = ImageIO.read(watermarkFile)
    if (watermarkImage.colorModel.numColorComponents != 3) {
        error("The number of watermark color components isn't 3.")
    }

    if (watermarkImage.colorModel.pixelSize != 24 && watermarkImage.colorModel.pixelSize != 32) {
        error("The watermark isn't 24 or 32-bit.")
    }

    if (watermarkImage.height > image.height || watermarkImage.width > image.width) {
        error("The watermark's dimensions are larger.")
    }

    var useAlpha = false
    var transparencyColor = Color.cyan

    if (watermarkImage.colorModel.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        val useAlphaInput = readln()
        if (useAlphaInput.contentEquals("yes", ignoreCase = true)) {
            useAlpha = true
        }
    } else {
        println("Do you want to set a transparency color?")
        val setTransparencyColor = readln()
        if (setTransparencyColor.contentEquals("yes", ignoreCase = true)) {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            val colorInput = readln()
            if (!colorInput.matches(Regex("\\d+ \\d+ \\d+"))) error("The transparency color input is invalid.")

            val (r, g, b) = colorInput.split(' ').map { it.toInt() }
            if (r !in 0..255 || g !in 0..255 || b !in 0..255) error("The transparency color input is invalid.")
            transparencyColor = Color(r, g, b)
        }
    }

    println("Input the watermark transparency percentage (Integer 0-100):")
    val transparencyIn = readln()
    if (transparencyIn.all { char -> !char.isDigit() }) {
        error("The transparency percentage isn't an integer number.")
    }
    val transparency = try {
        transparencyIn.toInt()
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(1)
    }
    if (transparency !in 0..100) {
        error("The transparency percentage is out of range.")
    }

    println("Choose the position method (single, grid):")
    val positionMethod = readln()
    if (!positionMethod.matches(Regex("(single|grid)"))) error("The position method input is invalid.")
    var watermarkX = 0
    var watermarkY = 0
    if (positionMethod == "single") {
        val positionMin = 0
        val positionXMax = image.width - watermarkImage.width
        val positionYMax = image.height - watermarkImage.height
        println("Input the watermark position ([x $positionMin-$positionXMax] [y $positionMin-$positionYMax]):")
        val watermarkPositionIn = readln()
        try {
            val (x, y) = watermarkPositionIn.split(" ").map { it.toInt() }
            if (x > positionXMax || x < positionMin || y < positionMin || y > positionYMax) {
                error("The position input is out of range.")
            }
            watermarkX = x
            watermarkY = y
        } catch (e: NumberFormatException) {
            println("The position input is invalid.")
            exitProcess(1)
        }
    }

    println("Input the output image filename (jpg or png extension):")
    val outputFileName = readln()
    if (!outputFileName.endsWith(".jpg") && !outputFileName.endsWith(".png")) {
        error("The output file extension isn't \"jpg\" or \"png\".")
    }

    val newImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)

    mergeWatermark(
        image,
        positionMethod,
        watermarkImage,
        useAlpha,
        watermarkX,
        watermarkY,
        transparencyColor,
        transparency,
        newImage
    )

    val outFile = File(outputFileName)
    ImageIO.write(newImage, "png", outFile)
    println("The watermarked image $outputFileName has been created.")
}

private fun mergeWatermark(
    image: BufferedImage,
    positionMethod: String,
    watermarkImage: BufferedImage,
    useAlpha: Boolean,
    watermarkX: Int,
    watermarkY: Int,
    transparencyColor: Color?,
    transparency: Int,
    newImage: BufferedImage
) {
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val i = Color(image.getRGB(x, y))
            val w = when {
                positionMethod == "grid" -> Color(
                    watermarkImage.getRGB(
                        x % watermarkImage.width, y % watermarkImage.height
                    ), useAlpha
                )

                x in watermarkX until watermarkX + watermarkImage.width && y in watermarkY until watermarkY + watermarkImage.height -> Color(
                    watermarkImage.getRGB(x - watermarkX, y - watermarkY),
                    useAlpha
                )

                else -> i
            }
            val color = if (w == i || w.alpha == 0 || w == transparencyColor) i

            // combine the two colours
            else Color(
                (transparency * w.red + (100 - transparency) * i.red) / 100,
                (transparency * w.green + (100 - transparency) * i.green) / 100,
                (transparency * w.blue + (100 - transparency) * i.blue) / 100
            )
            newImage.setRGB(x, y, color.rgb)
        }
    }
}
}
