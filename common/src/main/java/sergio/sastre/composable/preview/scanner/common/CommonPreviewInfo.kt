package sergio.sastre.composable.preview.scanner.common

@Deprecated(
    message = "The :common module is deprecated and will be removed in 0.10.0. Use Android Previews instead.",
)
data class CommonPreviewInfo(
    val name: String = "",
    val group: String = "",
    val widthDp: Int = -1,
    val heightDp: Int = -1,
    val locale: String = "",
    val showBackground: Boolean = false,
    val backgroundColor: Long = 0,
)