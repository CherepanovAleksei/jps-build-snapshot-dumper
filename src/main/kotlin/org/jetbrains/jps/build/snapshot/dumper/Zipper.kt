package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.util.io.ZipUtil
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

class Zipper {
    companion object {
        fun zip(zipFile: File, dir: File) {
            try {
                val zos = ZipOutputStream(FileOutputStream(zipFile))
                ZipUtil.addDirToZipRecursively(zos, null, dir, "", null, null)
                zos.close()
            } catch (e:Exception){
                println(e.message)
            }
        }
    }
}