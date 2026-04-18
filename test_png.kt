import java.util.Base64
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.io.File

fun main() {
    val size = 64 * 64 * 4
    val dummyBytes = ByteArray(size)
    for (i in 0 until size step 4) {
        dummyBytes[i] = 255.toByte() // A
        dummyBytes[i+1] = 0.toByte() // B
        dummyBytes[i+2] = 255.toByte() // G
        dummyBytes[i+3] = 0.toByte() // R
    }
    
    val base64Data = Base64.getEncoder().encodeToString(dummyBytes)
    val decodedBytes = Base64.getDecoder().decode(base64Data)

    val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
    var byteIndex = 0
    for (y in 0 until 64) {
        for (x in 0 until 64) {
            if (byteIndex + 3 < decodedBytes.size) {
                val a = decodedBytes[byteIndex].toInt() and 0xFF
                val b = decodedBytes[byteIndex + 1].toInt() and 0xFF
                val g = decodedBytes[byteIndex + 2].toInt() and 0xFF
                val r = decodedBytes[byteIndex + 3].toInt() and 0xFF

                val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, argb)
                byteIndex += 4
            }
        }
    }

    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, "PNG", outputStream)
    File("test_output.png").writeBytes(outputStream.toByteArray())
    println("PNG size: ${outputStream.toByteArray().size}")
}
