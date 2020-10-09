package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.ZipUtil
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

class Zipper {
    private val LOG = Logger.getInstance("org.jetbrains.jps.build.snapshot.dumper.Zipper")

    fun zip(zipFile: File, dir: File) {
        try {
            ZipOutputStream(FileOutputStream(zipFile)).use {
                ZipUtil.addDirToZipRecursively(it, null, dir, "", null, null)
            }
        } catch (e:Exception) {
            LOG.error("ZIP failed:")
            LOG.error(e.message)
            if(zipFile.exists()) zipFile.delete()
        }
    }
}