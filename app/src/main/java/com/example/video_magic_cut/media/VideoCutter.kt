package com.example.video_magic_cut.media

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.media.*
import android.net.Uri
import android.util.Log
import android.util.Range
import java.nio.ByteBuffer


class VideoCutter(private val contentResolver: ContentResolver) {
    @SuppressLint("WrongConstant")
    fun slice(
        srcUri: Uri,
        dstUri: Uri,
        timeUs: List<Range<Long>>
    ) {
        var currentPresentationUs = 0L
        // Set up MediaExtractor to read from the source.
        val extractor = Utils.createMediaExtractor(srcUri, contentResolver)
        val trackCount = extractor.trackCount
        // Set up MediaMuxer for the destination.
        val dstFileDescriptor = contentResolver.openFileDescriptor(dstUri, "w")!!.fileDescriptor
        val muxer = MediaMuxer(dstFileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Set up the tracks and retrieve the max buffer size for selected
        // tracks.
        val indexMap: HashMap<Int, Int> = HashMap(trackCount)
        var bufferSize = -1
        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            var selectCurrentTrack = false
            if (mime!!.startsWith("audio/")) {
                selectCurrentTrack = true
            } else if (mime.startsWith("video/")) {
                selectCurrentTrack = true
            }
            if (selectCurrentTrack) {
                extractor.selectTrack(i)
                val dstIndex = muxer.addTrack(format)
                indexMap[i] = dstIndex
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    bufferSize = if (newSize > bufferSize) newSize else bufferSize
                }
            }
        }
        if (bufferSize < 0) {
            bufferSize = DEFAULT_BUFFER_SIZE
        }
        // Set up the orientation and starting time for extractor.
        val retrieverSrc = MediaMetadataRetriever()
        val srcFileDescriptor = contentResolver.openFileDescriptor(srcUri, "r")!!.fileDescriptor
        retrieverSrc.setDataSource(srcFileDescriptor)
        val degreesString = retrieverSrc.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        )
        if (degreesString != null) {
            val degrees = degreesString.toInt()
            if (degrees >= 0) {
                muxer.setOrientationHint(degrees)
            }
        }
        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
        // for copying each sample and stop when we get to the end of the source
        // file or exceed the end time of the trimming.
        val offset = 0
        var trackIndex: Int
        val dstBuf: ByteBuffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        try {
            muxer.start()
            var index = 0
            var endUs = timeUs[index].upper
            // trim only by sync frame, trimming more precisely need decoder and will made process much longer
            extractor.seekTo(timeUs[index].lower, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            while (true) {
                bufferInfo.offset = offset
                bufferInfo.size = extractor.readSampleData(dstBuf, offset)
                if (bufferInfo.size < 0) {
                    Log.d(TAG, "Saw input EOS.")
                    bufferInfo.size = 0
                    break
                } else {
                    if (endUs > 0 && extractor.sampleTime > endUs) {
                        Log.d(TAG, "The current sample is over the trim end time.")
                        index++
                        if (index >= timeUs.size) {
                            break
                        }
                        endUs = timeUs[index].upper
                        // trim only by sync frame, trimming more precisely need decoder and will made process much longer
                        extractor.seekTo(timeUs[index].lower, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    } else {
                        bufferInfo.presentationTimeUs = currentPresentationUs
                        bufferInfo.flags = extractor.sampleFlags
                        trackIndex = extractor.sampleTrackIndex
                        muxer.writeSampleData(
                            indexMap[trackIndex]!!, dstBuf,
                            bufferInfo
                        )
                        extractor.advance()
                        currentPresentationUs += FRAME_PRESENTATION_TIME_US
                    }
                }
            }
            muxer.stop()
        } catch (e: IllegalStateException) {
            // Swallow the exception due to malformed source.
            Log.w(TAG, "The source video file is malformed")
        } finally {
            muxer.release()
        }
        return
    }

    companion object {
        private const val TAG = "VideoCutter"
        private const val FRAME_PRESENTATION_TIME_US = 1_000_000 / 30 // assume that video is 30fps
    }
}
