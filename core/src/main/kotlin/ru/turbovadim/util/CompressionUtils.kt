package ru.turbovadim.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object CompressionUtils {
    @Throws(IOException::class)
    fun compressFiles(files: MutableCollection<File>, outputZip: File) {
        FileOutputStream(outputZip).use { fos ->
            ZipOutputStream(fos).use { zos ->
                for (file in files) {
                    addToZipFile(file, zos)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun addToZipFile(file: File, zos: ZipOutputStream) {
        FileInputStream(file).use { fis ->
            val zipEntry = ZipEntry(file.getName())
            zos.putNextEntry(zipEntry)

            val buffer = ByteArray(1024)
            var length: Int
            while ((fis.read(buffer).also { length = it }) >= 0) {
                zos.write(buffer, 0, length)
            }
            zos.closeEntry()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun decompressFiles(zipFile: File, outputDir: File) {
        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zis ->
                var zipEntry: ZipEntry?
                while ((zis.getNextEntry().also { zipEntry = it }) != null) {
                    val newFile = newFile(outputDir, zipEntry!!)
                    if (zipEntry.isDirectory) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw IOException("Failed to create directory $newFile")
                        }
                    } else {
                        val parent = newFile.getParentFile()
                        if (parent != null && !parent.isDirectory()) {
                            if (!parent.mkdirs()) {
                                throw IOException("Failed to create directory $parent")
                            }
                        }

                        FileOutputStream(newFile).use { fos ->
                            val buffer = ByteArray(1024)
                            var length: Int
                            while ((zis.read(buffer).also { length = it }) > 0) {
                                fos.write(buffer, 0, length)
                            }
                        }
                    }
                    zis.closeEntry()
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir, zipEntry.getName())

        val destDirPath = destinationDir.getCanonicalPath()
        val destFilePath = destFile.getCanonicalPath()

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw IOException("Entry is outside of the target dir: " + zipEntry.getName())
        }

        return destFile
    }
}
