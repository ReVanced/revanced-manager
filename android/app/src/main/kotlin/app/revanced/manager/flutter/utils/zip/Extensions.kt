package app.revanced.manager.flutter.utils.zip

import java.io.DataInput
import java.io.DataOutput
import java.nio.ByteBuffer

fun UInt.toLittleEndian() =
    (((this.toInt() and 0xff000000.toInt()) shr 24) or ((this.toInt() and 0x00ff0000) shr 8) or ((this.toInt() and 0x0000ff00) shl 8) or (this.toInt() shl 24)).toUInt()

fun UShort.toLittleEndian() = (this.toUInt() shl 16).toLittleEndian().toUShort()

fun UInt.toBigEndian() = (((this.toInt() and 0xff) shl 24) or ((this.toInt() and 0xff00) shl 8)
        or ((this.toInt() and 0x00ff0000) ushr 8) or (this.toInt() ushr 24)).toUInt()

fun UShort.toBigEndian() = (this.toUInt() shl 16).toBigEndian().toUShort()

fun ByteBuffer.getUShort() = this.short.toUShort()
fun ByteBuffer.getUInt() = this.int.toUInt()

fun ByteBuffer.putUShort(ushort: UShort) = this.putShort(ushort.toShort())
fun ByteBuffer.putUInt(uint: UInt) = this.putInt(uint.toInt())

fun DataInput.readUShort() = this.readShort().toUShort()
fun DataInput.readUInt() = this.readInt().toUInt()

fun DataOutput.writeUShort(ushort: UShort) = this.writeShort(ushort.toInt())
fun DataOutput.writeUInt(uint: UInt) = this.writeInt(uint.toInt())

fun DataInput.readUShortLE() = this.readUShort().toBigEndian()
fun DataInput.readUIntLE() = this.readUInt().toBigEndian()

fun DataOutput.writeUShortLE(ushort: UShort) = this.writeUShort(ushort.toLittleEndian())
fun DataOutput.writeUIntLE(uint: UInt) = this.writeUInt(uint.toLittleEndian())
