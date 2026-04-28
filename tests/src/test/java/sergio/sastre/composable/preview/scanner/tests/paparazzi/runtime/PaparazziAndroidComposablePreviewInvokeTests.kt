package sergio.sastre.composable.preview.scanner.tests.paparazzi.runtime

import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import sergio.sastre.composable.preview.scanner.core.annotations.RequiresShowStandardStreams
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import sergio.sastre.composable.preview.scanner.tests.paparazzi.utils.DeviceConfigBuilder
import sergio.sastre.composable.preview.scanner.tests.paparazzi.utils.applyUiMode
import sergio.sastre.composable.preview.scanner.tests.paparazzi.utils.paparazziTestNameSnapshotHandler

/**
 * These tests ensure that the invoke() function of a ComposablePreview works as expected
 * for all the @Composable's in the main source at build time.
 *
 * ./gradlew :tests:recordPaparazziDebug --tests 'PaparazziAndroidComposablePreviewInvokeTests' -Plibrary=paparazzi
 */
@RunWith(Parameterized::class)
class PaparazziAndroidComposablePreviewInvokeTests(
    private val preview: ComposablePreview<AndroidPreviewInfo>,
) {

    companion object {
        @OptIn(RequiresShowStandardStreams::class)
        private val cachedPreviews: List<ComposablePreview<AndroidPreviewInfo>> by lazy {
            AndroidComposablePreviewScanner()
                .enableScanningLogs()
                .scanPackageTrees("sergio.sastre.composable.preview.scanner")
                .includePrivatePreviews()
                .getPreviews()
        }

        @JvmStatic
        @Parameterized.Parameters
        fun values(): List<ComposablePreview<AndroidPreviewInfo>> = cachedPreviews
    }

    @get:Rule
    val paparazzi = Paparazzi(
        environment = detectEnvironment().copy(compileSdkVersion = 34),
        deviceConfig = DeviceConfigBuilder
            .build(preview.previewInfo.device)
            .applyUiMode(preview.previewInfo.uiMode),
        renderingMode = SessionParams.RenderingMode.SHRINK,
        snapshotHandler = paparazziTestNameSnapshotHandler()
    )

    @Test
    fun snapshot() {
        val screenshotId = AndroidPreviewScreenshotIdBuilder(preview)
            .doNotIgnoreMethodParametersType()
            .encodeUnsafeCharacters()
            .build()
            .replace("sergio.sastre.composable.preview.scanner.", "")

        paparazzi.snapshot(name = screenshotId) {
            preview()
        }
    }
}