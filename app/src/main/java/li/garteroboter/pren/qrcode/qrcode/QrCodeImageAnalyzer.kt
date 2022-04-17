package li.garteroboter.pren.qrcode.qrcode

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

class QRCodeImageAnalyzer(private val listener: QRCodeFoundListener? = null) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {

        if (image.format == ImageFormat.YUV_420_888 || image.format == ImageFormat.YUV_422_888 || image.format == ImageFormat.YUV_444_888) {
            val byteBuffer = image.planes[0].buffer
            val imageData = ByteArray(byteBuffer.capacity())
            byteBuffer[imageData]
            val source = PlanarYUVLuminanceSource(
                imageData,
                image.width, image.height,
                0, 0,
                image.width, image.height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val result = QRCodeReader().decode(binaryBitmap)
                listener?.onQRCodeFound(result.text)
            } catch (e: Exception ) {
                listener?.qrCodeNotFound()
            }
        }
        image.close()
    }

}