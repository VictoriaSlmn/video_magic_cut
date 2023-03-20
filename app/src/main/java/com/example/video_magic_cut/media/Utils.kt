package com.example.video_magic_cut.media

import android.content.ContentResolver
import android.content.ContentValues
import android.media.MediaExtractor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.util.*
import java.util.concurrent.TimeUnit

object Utils {
    fun createMediaExtractor(uri: Uri, contentResolver: ContentResolver): MediaExtractor {
        val mediaExtractor = MediaExtractor()
        checkNotNull(contentResolver.openFileDescriptor(uri, "r")) {
            "unable to acquire file descriptor for $uri"
        }.use {
            mediaExtractor.setDataSource(it.fileDescriptor)
        }
        return mediaExtractor
    }

    fun createOutputUri(contentResolver: ContentResolver): Uri {
        val date = Date()
        val fileName = "${date.time}.mp4"
        val values = ContentValues()
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        values.put(MediaStore.Video.Media.TITLE, fileName)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATE_ADDED, TimeUnit.MILLISECONDS.toSeconds(date.time))
        values.put(MediaStore.Video.Media.DATE_TAKEN, date.time)
        values.put(MediaStore.Video.Media.IS_PENDING, 1)
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return contentResolver.insert(collection, values)!!
    }

    fun markVideoAsCompletedInGallery(outputMp4Uri: Uri, contentResolver: ContentResolver) {
        val values = ContentValues()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        contentResolver.update(outputMp4Uri, values, null, null)
    }
}
