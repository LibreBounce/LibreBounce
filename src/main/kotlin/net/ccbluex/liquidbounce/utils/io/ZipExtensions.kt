/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*
import java.util.zip.ZipInputStream

/**
 * Extracts a ZIP archive from an input stream to a specified folder
 */
suspend fun extractZip(zipStream: InputStream, folder: File) = withContext(Dispatchers.IO) {
    if (!folder.exists()) {
        folder.mkdir()
    }

    ZipInputStream(zipStream.buffered()).use { zipInputStream ->
        generateSequence { zipInputStream.nextEntry }
            .filterNot { it.isDirectory }
            .forEach { entry ->
                val newFile = File(folder, entry.name).apply {
                    parentFile?.mkdirs()
                }

                // Ensure the entry is within the target directory to prevent zip slip
                if (!newFile.canonicalPath.startsWith(folder.canonicalPath)) {
                    throw SecurityException("Entry is outside of the target directory: ${entry.name}")
                }

                FileOutputStream(newFile).buffered().use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }
            }
    }
}

/**
 * Extracts a ZIP file to a specified
 */
suspend fun extractZip(zipFile: File, folder: File) =
    withContext(Dispatchers.IO) {
        FileInputStream(zipFile).use { stream ->
            extractZip(stream, folder)
        }
    }

/**
 * Extracts a tar.gz file to a specified folder
 */
suspend fun extractTarGz(tarGzFile: File, folder: File) = withContext(Dispatchers.IO) {
    if (!folder.exists()) {
        folder.mkdir()
    }

    FileInputStream(tarGzFile).buffered().use { fileInputStream ->
        GzipCompressorInputStream(fileInputStream).use { gzipInputStream ->
            TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
                generateSequence { tarInputStream.nextTarEntry }
                    .filterNot { it.isDirectory }
                    .forEach { entry ->
                        val newFile = File(folder, entry.name).apply {
                            parentFile?.mkdirs()
                        }

                        // Prevent tar slip vulnerability
                        if (!newFile.canonicalPath.startsWith(folder.canonicalPath)) {
                            throw SecurityException("Entry is outside of the target directory: ${entry.name}")
                        }

                        FileOutputStream(newFile).buffered().use { outputStream ->
                            tarInputStream.copyTo(outputStream)
                        }
                    }
            }
        }
    }
}

/**
 * Create a buffered output stream with a default buffer size for fewer I/O operations
 */
private fun OutputStream.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream =
    if (this is BufferedOutputStream) this else BufferedOutputStream(this, bufferSize)

/**
 * Create a buffered input stream with a default buffer size for fewer I/O operations
 */
private fun InputStream.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream =
    if (this is BufferedInputStream) this else BufferedInputStream(this, bufferSize)
