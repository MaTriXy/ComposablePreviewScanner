package sergio.sastre.composable.preview.scanner.tests.paparazzi.utils

import app.cash.paparazzi.HtmlReportWriter
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotVerifier
import app.cash.paparazzi.TestName

internal fun paparazziTestNameSnapshotHandler(tolerance: Double = 0.0) =
    when(System.getProperty("paparazzi.test.verify")?.toBoolean() == true) {
        true -> PreviewSnapshotVerifier(tolerance)
        false -> PreviewHtmlReportWriter()
    }

// Define the prefix = <packageName>_<className>_<methodName>
private val paparazziTestName =
    TestName(packageName = "Paparazzi", className = "Preview", methodName = "Test")

private class PreviewSnapshotVerifier(
    maxPercentDifference: Double
): SnapshotHandler {
    private val snapshotHandler = SnapshotVerifier(
        maxPercentDifference = maxPercentDifference
    )
    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int
    ): SnapshotHandler.FrameHandler {
        val newSnapshot = Snapshot(
            name = snapshot.name,
            testName = paparazziTestName,
            timestamp = snapshot.timestamp,
            tags = snapshot.tags,
            file = snapshot.file,
        )
        return snapshotHandler.newFrameHandler(
            snapshot = newSnapshot,
            frameCount = frameCount,
            fps = fps
        )
    }

    override fun close() {
        snapshotHandler.close()
    }
}

private class PreviewHtmlReportWriter: SnapshotHandler {
    private val snapshotHandler = HtmlReportWriter()
    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int
    ): SnapshotHandler.FrameHandler {
        val newSnapshot = Snapshot(
            name = snapshot.name,
            testName = paparazziTestName,
            timestamp = snapshot.timestamp,
            tags = snapshot.tags,
            file = snapshot.file,
        )
        return snapshotHandler.newFrameHandler(
            snapshot = newSnapshot,
            frameCount = frameCount,
            fps = fps
        )
    }

    override fun close() {
        snapshotHandler.close()
    }
}