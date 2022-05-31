package li.garteroboter.pren.qrcode.qrcode

import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

class QRCodeImageAnalyzer(private val listener: QRCodeFoundListener? = null) : ImageAnalysis.Analyzer {

    private val yuvFormats = listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)


    override fun analyze(image: ImageProxy) {

        if (image.format in yuvFormats) {

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
        } else {
            Log.e(TAG, "FATAL: image.format not in yuvFormats")
        }
        image.close()
    }

    companion object {
        const val TAG = "QRCodeImageAnalyzer"
    }

}