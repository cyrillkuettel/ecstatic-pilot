package li.garteroboter.pren.qrcode.qrcode

interface QRCodeFoundListener {
        fun onQRCodeFound(qrCode: String?)
        fun qrCodeNotFound()
}
