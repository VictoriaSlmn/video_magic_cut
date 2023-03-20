package com.example.video_magic_cut

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Range
import com.example.video_magic_cut.media.Utils
import com.example.video_magic_cut.media.Utils.createOutputUri
import com.example.video_magic_cut.media.Utils.markVideoAsCompletedInGallery
import com.example.video_magic_cut.media.VideoCutter
import com.example.video_magic_cut.media.VideoFrameExtractor
import com.example.video_magic_cut.media.VideoFrameExtractor.Frame
import org.tensorflow.lite.task.vision.detector.Detection


class FindInterestingVideoParts(context: Context) {
    private val contentResolver = context.contentResolver
    private val objectDetectorHelper = ObjectDetectorHelper(context = context).also {
        it.currentModel = ObjectDetectorHelper.MODEL_EFFICIENTDETV2 //preferred result
    }
    private val videoCutter = VideoCutter(contentResolver)

    fun generateOptimisedVideo(videoUri: Uri): Uri {
        val timeAnalyseUs = System.currentTimeMillis()
        val interestingVideoParts = InterestingVideoParts()
        val videoFrameExtractor = VideoFrameExtractor(videoUri, contentResolver)
      //  var frameExtractTimeUs = System.currentTimeMillis()
        videoFrameExtractor.doExtract { frame ->
            //Log.i("Time", "Extract ${System.currentTimeMillis() - frameExtractTimeUs}")

           // val timeDetectUs = System.currentTimeMillis()
            val results = objectDetectorHelper.detect(frame.bitmap, videoFrameExtractor.rotation)
           // Log.i("Time", "Detect ${System.currentTimeMillis() - timeDetectUs}")

            interestingVideoParts.process(
                results,
                frame
            )

          //  frameExtractTimeUs = System.currentTimeMillis()
        }
        videoFrameExtractor.close()
        Log.i("Time", "Analyse ${System.currentTimeMillis() - timeAnalyseUs}")

        val timeCutUs = System.currentTimeMillis()
        val outputUri = createOutputUri(contentResolver)
        videoCutter.slice(
            srcUri = videoUri,
            dstUri = outputUri,
            timeUs = interestingVideoParts.get()
        )
        markVideoAsCompletedInGallery(outputUri, contentResolver)
        Log.i("Time", "Cutting ${System.currentTimeMillis() - timeCutUs}")
        return outputUri
    }

    class InterestingVideoParts {
        private var category: String? = null
        private var categoryTimeStartUs = 0L
        private var categoryLastTimeUs = 0L
        private val interestingVideoPartsTimeUs = mutableListOf<Range<Long>>()
        //  var boundingBoxs: MutableList<RectF> = mutableListOf()

        fun process(
            results: List<Detection>,
            frame: Frame
        ) {
            val nextCategory =
                results.find {
                    it.categories.any { category ->
                        validCategories.contains(category.label)
                    }
                }?.categories?.firstOrNull { validCategories.contains(it.label) }?.label

            if (category != nextCategory) {
                //    val inCenter = boundingBoxs.any {  it.contains(frame.bitmap.height/2f, frame.bitmap.width/2f) }
                val isLongEnough = categoryLastTimeUs - categoryTimeStartUs > 1_000_000
                if (category != null && isLongEnough) {
                    interestingVideoPartsTimeUs.add(Range(categoryTimeStartUs, categoryLastTimeUs))
                    Log.i(
                        TAG,
                        "$category ${(categoryLastTimeUs - categoryTimeStartUs) / 1_000_000.0}sec from $categoryTimeStartUs to $categoryLastTimeUs"
                    )
                }
                category = nextCategory
                categoryTimeStartUs = frame.timestampUs
                categoryLastTimeUs = frame.timestampUs
                //  boundingBoxs = result?.boundingBox?.let {  mutableListOf(it) } ?: mutableListOf()
            } else {
                categoryLastTimeUs = frame.timestampUs
                //   result?.boundingBox?.let {  boundingBoxs.add(it) }
            }
        }

        fun get() = interestingVideoPartsTimeUs.toList()
    }

    companion object {
        private const val TAG = "FindInterestingVideoParts"
        // https://github.com/JerryKurata/TFlite-object-detection/blob/main/labelmap.txt
        private val validCategories = listOf("person", "cat", "dog")
    }
}
