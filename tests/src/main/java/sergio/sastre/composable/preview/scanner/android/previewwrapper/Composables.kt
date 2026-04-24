package sergio.sastre.composable.preview.scanner.android.previewwrapper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider

class SampleScaffoldWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        Scaffold { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .background(Color.Red)
            ) { content() }
        }
    }
}

@Preview
@Composable
@PreviewWrapper(wrapper = SampleScaffoldWrapper::class)
fun PreviewWrapperSample() {
    Text("PreviewWrapperSample")
}