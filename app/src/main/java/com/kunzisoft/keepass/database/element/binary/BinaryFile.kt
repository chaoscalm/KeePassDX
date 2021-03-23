/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element.binary

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import android.util.Base64InputStream
import android.util.Base64OutputStream
import com.kunzisoft.keepass.stream.readAllBytes
import org.apache.commons.io.output.CountingOutputStream
import java.io.*
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec

class BinaryFile : BinaryData {

    private var mLength: Long = 0
    private var mBinaryHash = 0
    private var mDataFile: File? = null

    // Cipher to encrypt temp file
    @Transient
    private var cipherEncryption: Cipher = Cipher.getInstance(LoadedKey.BINARY_CIPHER)
    @Transient
    private var cipherDecryption: Cipher = Cipher.getInstance(LoadedKey.BINARY_CIPHER)

    constructor(dataFile: File,
                compressed: Boolean = false,
                protected: Boolean = false) : super(compressed, protected) {
        this.mLength = 0
        this.mBinaryHash = 0
        this.mDataFile = dataFile
    }

    constructor(parcel: Parcel) : super(parcel) {
        mLength = parcel.readLong()
        mBinaryHash = parcel.readInt()
        parcel.readString()?.let {
            mDataFile = File(it)
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLong(mLength)
        dest.writeInt(mBinaryHash)
        dest.writeString(mDataFile?.absolutePath)
    }

    @Throws(IOException::class)
    override fun getInputDataStream(binaryCache: BinaryCache): InputStream {
        return buildInputStream(mDataFile, binaryCache)
    }

    @Throws(IOException::class)
    override fun getOutputDataStream(binaryCache: BinaryCache): OutputStream {
        return buildOutputStream(mDataFile, binaryCache)
    }

    @Throws(IOException::class)
    private fun buildInputStream(file: File?, binaryCache: BinaryCache): InputStream {
        val cipherKey = binaryCache.loadedCipherKey
        return when {
            file != null && file.length() > 0 -> {
                cipherDecryption.init(Cipher.DECRYPT_MODE, cipherKey.key, IvParameterSpec(cipherKey.iv))
                Base64InputStream(CipherInputStream(FileInputStream(file), cipherDecryption), Base64.NO_WRAP)
            }
            else -> ByteArrayInputStream(ByteArray(0))
        }
    }

    @Throws(IOException::class)
    private fun buildOutputStream(file: File?, binaryCache: BinaryCache): OutputStream {
        val cipherKey = binaryCache.loadedCipherKey
        return when {
            file != null -> {
                cipherEncryption.init(Cipher.ENCRYPT_MODE, cipherKey.key, IvParameterSpec(cipherKey.iv))
                BinaryCountingOutputStream(Base64OutputStream(CipherOutputStream(FileOutputStream(file), cipherEncryption), Base64.NO_WRAP))
            }
            else -> throw IOException("Unable to write in an unknown file")
        }
    }

    @Throws(IOException::class)
    override fun compress(binaryCache: BinaryCache) {
        mDataFile?.let { concreteDataFile ->
            // To compress, create a new binary with file
            if (!isCompressed) {
                // Encrypt the new gzipped temp file
                val fileBinaryCompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                getInputDataStream(binaryCache).use { inputStream ->
                    GZIPOutputStream(buildOutputStream(fileBinaryCompress, binaryCache)).use { outputStream ->
                        inputStream.readAllBytes { buffer ->
                            outputStream.write(buffer)
                        }
                    }
                }
                // Remove ungzip file
                if (concreteDataFile.delete()) {
                    if (fileBinaryCompress.renameTo(concreteDataFile)) {
                        // Harmonize with database compression
                        isCompressed = true
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun decompress(binaryCache: BinaryCache) {
        mDataFile?.let { concreteDataFile ->
            if (isCompressed) {
                // Encrypt the new ungzipped temp file
                val fileBinaryDecompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                getUnGzipInputDataStream(binaryCache).use { inputStream ->
                    buildOutputStream(fileBinaryDecompress, binaryCache).use { outputStream ->
                        inputStream.readAllBytes { buffer ->
                            outputStream.write(buffer)
                        }
                    }
                }
                // Remove gzip file
                if (concreteDataFile.delete()) {
                    if (fileBinaryDecompress.renameTo(concreteDataFile)) {
                        // Harmonize with database compression
                        isCompressed = false
                    }
                }
            }
        }
    }

    override fun dataExists(binaryCache: BinaryCache): Boolean {
        return mDataFile != null && mLength > 0
    }

    override fun getSize(binaryCache: BinaryCache): Long {
        return mLength
    }

    override fun binaryHash(binaryCache: BinaryCache): Int {
        return mBinaryHash
    }

    override fun clear(binaryCache: BinaryCache) {
        if (mDataFile != null && !mDataFile!!.delete())
            throw IOException("Unable to delete temp file " + mDataFile!!.absolutePath)
    }

    override fun toString(): String {
        return mDataFile.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryFile) return false
        if (!super.equals(other)) return false

        return mDataFile != null && mDataFile == other.mDataFile
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (mDataFile?.hashCode() ?: 0)
        result = 31 * result + mLength.hashCode()
        result = 31 * result + mBinaryHash
        return result
    }

    /**
     * Custom OutputStream to calculate the size and hash of binary file
     */
    private inner class BinaryCountingOutputStream(out: OutputStream): CountingOutputStream(out) {

        private val mMessageDigest: MessageDigest
        init {
            mLength = 0
            mMessageDigest = MessageDigest.getInstance("MD5")
            mBinaryHash = 0
        }

        override fun beforeWrite(n: Int) {
            super.beforeWrite(n)
            mLength = byteCount
        }

        override fun write(idx: Int) {
            super.write(idx)
            mMessageDigest.update(idx.toByte())
        }

        override fun write(bts: ByteArray) {
            super.write(bts)
            mMessageDigest.update(bts)
        }

        override fun write(bts: ByteArray, st: Int, end: Int) {
            super.write(bts, st, end)
            mMessageDigest.update(bts, st, end)
        }

        override fun close() {
            super.close()
            mLength = byteCount
            val bytes = mMessageDigest.digest()
            mBinaryHash = ByteBuffer.wrap(bytes).int
        }
    }

    companion object {
        private val TAG = BinaryFile::class.java.name

        @JvmField
        val CREATOR: Parcelable.Creator<BinaryFile> = object : Parcelable.Creator<BinaryFile> {
            override fun createFromParcel(parcel: Parcel): BinaryFile {
                return BinaryFile(parcel)
            }

            override fun newArray(size: Int): Array<BinaryFile?> {
                return arrayOfNulls(size)
            }
        }
    }

}