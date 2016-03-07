package io.mironov.smuggler.compiler.generators

import io.mironov.smuggler.compiler.common.GeneratorAdapter
import io.mironov.smuggler.compiler.common.Methods
import io.mironov.smuggler.compiler.common.Types
import io.mironov.smuggler.compiler.signature.GenericType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

internal interface ValueAdapter {
  fun fromParcel(adapter: GeneratorAdapter, context: ValueContext)
  fun toParcel(adapter: GeneratorAdapter, context: ValueContext)
}

internal abstract class OptionalValueAdapter() : ValueAdapter {
  final override fun fromParcel(adapter: GeneratorAdapter, context: ValueContext) {
    val start = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readInt", Types.INT))
    adapter.ifZCmp(Opcodes.IFEQ, start)

    adapter.fromParcelNotNullValue(context)
    adapter.goTo(end)

    adapter.mark(start)
    adapter.pushNull()

    adapter.mark(end)
  }

  final override fun toParcel(adapter: GeneratorAdapter, context: ValueContext) {
    val start = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.value())
    adapter.ifNull(start)

    adapter.loadLocal(context.parcel())
    adapter.push(1)
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))
    adapter.toParcelNotNullValue(context)
    adapter.goTo(end)

    adapter.mark(start)
    adapter.loadLocal(context.parcel())
    adapter.push(0)
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))

    adapter.mark(end)
  }

  private fun GeneratorAdapter.fromParcelNotNullValue(context: ValueContext) = fromParcelNotNull(this, context)
  private fun GeneratorAdapter.toParcelNotNullValue(context: ValueContext) = toParcelNotNull(this, context)

  abstract fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext)
  abstract fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext)
}

internal open class SimpleValueAdapter(
    private val type: Type,
    private val reader: String,
    private val writer: String
) : ValueAdapter {
  override fun fromParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get(reader, type))
  }

  override fun toParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get(writer, Types.VOID, type))
  }
}

internal open class SimpleBoxedValueAdapter(
    private val delegate: ValueAdapter,
    private val unboxed: Type,
    private val boxed: Type,
    private val unboxer: String,
    private val boxer: String
) : OptionalValueAdapter() {
  override fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    delegate.fromParcel(adapter, context)
    adapter.invokeStatic(boxed, Methods.get(boxer, boxed, unboxed))
  }

  override fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.value())
    adapter.invokeVirtual(boxed, Methods.get(unboxer, unboxed))

    delegate.toParcel(adapter, context.typed(GenericType.RawType(unboxed)).apply {
      value(adapter.newLocal(unboxed).apply {
        adapter.storeLocal(this)
      })
    })
  }
}

internal object ByteValueAdapter : SimpleValueAdapter(Types.BYTE, "readByte", "writeByte")
internal object DoubleValueAdapter : SimpleValueAdapter(Types.DOUBLE, "readDouble", "writeDouble")
internal object FloatValueAdapter : SimpleValueAdapter(Types.FLOAT, "readFloat", "writeFloat")
internal object IntValueAdapter : SimpleValueAdapter(Types.INT, "readInt", "writeInt")
internal object LongValueAdapter : SimpleValueAdapter(Types.LONG, "readLong", "writeLong")
internal object StringValueAdapter : SimpleValueAdapter(Types.STRING, "readString", "writeString")
internal object BundleValueAdapter : SimpleValueAdapter(Types.ANDROID_BUNDLE, "readBundle", "writeBundle")
internal object SparseBooleanArrayValueAdapter : SimpleValueAdapter(Types.ANDROID_SPARSE_BOOLEAN_ARRAY, "readSparseBooleanArray", "writeSparseBooleanArray")

internal object BoxedByteValueAdapter : SimpleBoxedValueAdapter(ByteValueAdapter, Types.BYTE, Types.BOXED_BYTE, "byteValue", "valueOf")
internal object BoxedCharValueAdapter : SimpleBoxedValueAdapter(CharValueAdapter, Types.CHAR, Types.BOXED_CHAR, "charValue", "valueOf")
internal object BoxedDoubleValueAdapter : SimpleBoxedValueAdapter(DoubleValueAdapter, Types.DOUBLE, Types.BOXED_DOUBLE, "doubleValue", "valueOf")
internal object BoxedFloatValueAdapter : SimpleBoxedValueAdapter(FloatValueAdapter, Types.FLOAT, Types.BOXED_FLOAT, "floatValue", "valueOf")
internal object BoxedIntValueAdapter : SimpleBoxedValueAdapter(IntValueAdapter, Types.INT, Types.BOXED_INT, "intValue", "valueOf")
internal object BoxedLongValueAdapter : SimpleBoxedValueAdapter(LongValueAdapter, Types.LONG, Types.BOXED_LONG, "longValue", "valueOf")
internal object BoxedShortValueAdapter : SimpleBoxedValueAdapter(ShortValueAdapter, Types.SHORT, Types.BOXED_SHORT, "shortValue", "valueOf")
internal object BoxedBooleanValueAdapter : SimpleBoxedValueAdapter(BooleanValueAdapter, Types.BOOLEAN, Types.BOXED_BOOLEAN, "booleanValue", "valueOf")

internal object CharValueAdapter : ValueAdapter {
  override fun fromParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readInt", Types.INT))
    adapter.cast(Types.INT, Types.CHAR)
  }

  override fun toParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.cast(Types.CHAR, Types.INT)
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))
  }
}

internal object ShortValueAdapter : ValueAdapter {
  override fun fromParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readInt", Types.INT))
    adapter.cast(Types.INT, Types.SHORT)
  }

  override fun toParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.cast(Types.SHORT, Types.INT)
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))
  }
}

internal object BooleanValueAdapter : ValueAdapter {
  override fun fromParcel(adapter: GeneratorAdapter, context: ValueContext) {
    val start = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readInt", Types.INT))
    adapter.ifZCmp(Opcodes.IFEQ, start)

    adapter.push(true)
    adapter.goTo(end)
    adapter.mark(start)
    adapter.push(false)

    adapter.mark(end)
  }

  override fun toParcel(adapter: GeneratorAdapter, context: ValueContext) {
    val start = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.ifZCmp(Opcodes.IFEQ, start)
    adapter.push(1)
    adapter.goTo(end)
    adapter.mark(start)
    adapter.push(0)
    adapter.mark(end)

    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))
  }
}

internal object EnumValueAdapter : OptionalValueAdapter() {
  override fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readInt", Types.INT))
    adapter.invokeStatic(context.type.asAsmType(), Methods.get("values", Types.getArrayType(context.type.asAsmType())))
    adapter.swap(Types.INT, Types.getArrayType(context.type.asAsmType()))
    adapter.arrayLoad(context.type.asAsmType())
  }

  override fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.invokeVirtual(context.type.asAsmType(), Methods.get("ordinal", Types.INT))
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))
  }
}

internal object DateValueAdapter : OptionalValueAdapter() {
  override fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.newInstance(Types.DATE, Methods.getConstructor(Types.LONG)) {
      adapter.loadLocal(context.parcel())
      adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readLong", Types.LONG))
    }
  }

  override fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.invokeVirtual(Types.DATE, Methods.get("getTime", Types.LONG))
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeLong", Types.VOID, Types.LONG))
  }
}

internal object SerializableValueAdapter : ValueAdapter {
  override fun fromParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readSerializable", Types.SERIALIZABLE))
    adapter.checkCast(context.type.asAsmType())
  }

  override fun toParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.checkCast(Types.SERIALIZABLE)
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeSerializable", Types.VOID, Types.SERIALIZABLE))
  }
}

internal object ParcelableValueAdapter : ValueAdapter {
  override fun fromParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.push(context.type.asAsmType())
    adapter.invokeVirtual(Types.CLASS, Methods.get("getClassLoader", Types.CLASS_LOADER))
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readParcelable", Types.ANDROID_PARCELABLE, Types.CLASS_LOADER))
    adapter.checkCast(context.type.asAsmType())
  }

  override fun toParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.checkCast(Types.ANDROID_PARCELABLE)
    adapter.loadLocal(context.flags())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeParcelable", Types.VOID, Types.ANDROID_PARCELABLE, Types.INT))
  }
}

internal class ArrayPropertyAdapter(
    private val delegate: ValueAdapter
) : OptionalValueAdapter() {
  final override fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    val index = adapter.newLocal(Types.INT)
    val length = adapter.newLocal(Types.INT)

    val elementType = Types.getElementType(context.type.asAsmType())
    val elements = adapter.newLocal(context.type.asAsmType())

    val begin = adapter.newLabel()
    val body = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readInt", Types.INT))
    adapter.storeLocal(length)

    adapter.loadLocal(length)
    adapter.newArray(elementType)
    adapter.storeLocal(elements)

    adapter.push(0)
    adapter.storeLocal(index)

    adapter.mark(begin)
    adapter.loadLocal(index)
    adapter.loadLocal(length)

    adapter.ifICmp(Opcodes.IFLT, body)
    adapter.goTo(end)

    adapter.mark(body)
    adapter.loadLocal(elements)
    adapter.loadLocal(index)
    adapter.readElement(context.asElementContext())
    adapter.arrayStore(elementType)

    adapter.iinc(index, 1)
    adapter.goTo(begin)
    adapter.mark(end)

    adapter.loadLocal(elements)
  }

  final override fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    val index = adapter.newLocal(Types.INT)
    val length = adapter.newLocal(Types.INT)

    val elementType = Types.getElementType(context.type.asAsmType())
    val element = adapter.newLocal(elementType)

    val begin = adapter.newLabel()
    val body = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.push(0)
    adapter.storeLocal(index)

    adapter.loadLocal(context.value())
    adapter.arrayLength()
    adapter.storeLocal(length)

    adapter.loadLocal(context.parcel())
    adapter.loadLocal(length)
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))

    adapter.mark(begin)
    adapter.loadLocal(index)
    adapter.loadLocal(length)

    adapter.ifICmp(Opcodes.IFLT, body)
    adapter.goTo(end)

    adapter.mark(body)
    adapter.loadLocal(context.value())
    adapter.loadLocal(index)
    adapter.arrayLoad(elementType)
    adapter.storeLocal(element)
    adapter.writeElement(context.asElementContext().apply {
      value(element)
    })

    adapter.iinc(index, 1)
    adapter.goTo(begin)
    adapter.mark(end)
  }

  private fun ValueContext.asElementContext(): ValueContext {
    return if (type !is GenericType.ArrayType) {
      typed(GenericType.RawType(Types.getElementType(type.asAsmType())))
    } else {
      typed(type.elementType)
    }
  }

  private fun GeneratorAdapter.readElement(context: ValueContext) = delegate.fromParcel(this, context)
  private fun GeneratorAdapter.writeElement(context: ValueContext) = delegate.toParcel(this, context)
}

internal class SparseArrayValueAdapter(
    private val element: Type
) : ValueAdapter {
  override fun fromParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.push(element)
    adapter.invokeVirtual(Types.CLASS, Methods.get("getClassLoader", Types.CLASS_LOADER))
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readSparseArray", Types.ANDROID_SPARSE_ARRAY, Types.CLASS_LOADER))
    adapter.checkCast(context.type.asAsmType())
  }

  override fun toParcel(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.checkCast(Types.ANDROID_SPARSE_ARRAY)
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeSparseArray", Types.VOID, Types.ANDROID_SPARSE_ARRAY))
  }
}

internal class CollectionValueAdapter(
    private val collection: Type,
    private val implementation: Type,
    private val delegate: ValueAdapter
) : OptionalValueAdapter() {
  override fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    val parameterizedType = context.type.asParameterizedType()
    val elementType = parameterizedType.typeArguments[0].asAsmType()

    val index = adapter.newLocal(Types.INT)
    val length = adapter.newLocal(Types.INT)
    val elements = adapter.newLocal(collection)

    val begin = adapter.newLabel()
    val body = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readInt", Types.INT))
    adapter.storeLocal(length)

    adapter.newInstance(implementation, Methods.getConstructor())
    adapter.storeLocal(elements)

    adapter.push(0)
    adapter.storeLocal(index)

    adapter.mark(begin)
    adapter.loadLocal(index)
    adapter.loadLocal(length)

    adapter.ifICmp(Opcodes.IFLT, body)
    adapter.goTo(end)

    adapter.mark(body)
    adapter.loadLocal(elements)
    adapter.readElement(context.typed(parameterizedType.typeArguments[0]))
    adapter.checkCast(elementType)
    adapter.invokeInterface(Types.COLLECTION, Methods.get("add", Types.BOOLEAN, Types.OBJECT))
    adapter.pop()

    adapter.iinc(index, 1)
    adapter.goTo(begin)
    adapter.mark(end)

    adapter.loadLocal(elements)
  }

  override fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    val parameterizedType = context.type.asParameterizedType()
    val elementType = parameterizedType.typeArguments[0].asAsmType()

    val element = adapter.newLocal(elementType)
    val iterator = adapter.newLocal(Types.ITERATOR)

    val begin = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.invokeInterface(Types.COLLECTION, Methods.get("size", Types.INT))
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))

    adapter.loadLocal(context.value())
    adapter.invokeInterface(Types.COLLECTION, Methods.get("iterator", Types.ITERATOR))
    adapter.storeLocal(iterator)

    adapter.mark(begin)
    adapter.loadLocal(iterator)
    adapter.invokeInterface(Types.ITERATOR, Methods.get("hasNext", Types.BOOLEAN))
    adapter.ifZCmp(Opcodes.IFEQ, end)

    adapter.loadLocal(iterator)
    adapter.invokeInterface(Types.ITERATOR, Methods.get("next", Types.OBJECT))
    adapter.checkCast(elementType)
    adapter.storeLocal(element)
    adapter.writeElement(context.typed(parameterizedType.typeArguments[0]).apply {
      value(element)
    })

    adapter.goTo(begin)
    adapter.mark(end)
  }

  private fun GeneratorAdapter.readElement(context: ValueContext) = delegate.fromParcel(this, context)
  private fun GeneratorAdapter.writeElement(context: ValueContext) = delegate.toParcel(this, context)
}

internal class MapValueAdapter(
    private val key: ValueAdapter,
    private val value: ValueAdapter
) : OptionalValueAdapter() {
  override fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    val parameterizedType = context.type.asParameterizedType()

    val keyType = parameterizedType.typeArguments[0].asAsmType()
    val valueType = parameterizedType.typeArguments[1].asAsmType()

    val index = adapter.newLocal(Types.INT)
    val length = adapter.newLocal(Types.INT)
    val elements = adapter.newLocal(Types.MAP)

    val begin = adapter.newLabel()
    val body = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.parcel())
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("readInt", Types.INT))
    adapter.storeLocal(length)

    adapter.newInstance(Types.LINKED_MAP, Methods.getConstructor())
    adapter.storeLocal(elements)

    adapter.push(0)
    adapter.storeLocal(index)

    adapter.mark(begin)
    adapter.loadLocal(index)
    adapter.loadLocal(length)

    adapter.ifICmp(Opcodes.IFLT, body)
    adapter.goTo(end)

    adapter.mark(body)
    adapter.loadLocal(elements)
    adapter.readKey(context.typed(parameterizedType.typeArguments[0]))
    adapter.checkCast(keyType)
    adapter.readValue(context.typed(parameterizedType.typeArguments[1]))
    adapter.checkCast(valueType)
    adapter.invokeInterface(Types.MAP, Methods.get("put", Types.OBJECT, Types.OBJECT, Types.OBJECT))
    adapter.pop()

    adapter.iinc(index, 1)
    adapter.goTo(begin)
    adapter.mark(end)

    adapter.loadLocal(elements)
  }

  override fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    val parameterizedType = context.type.asParameterizedType()

    val keyType = parameterizedType.typeArguments[0].asAsmType()
    val valueType = parameterizedType.typeArguments[1].asAsmType()

    val keyElement = adapter.newLocal(keyType)
    val valueElement = adapter.newLocal(valueType)
    val iterator = adapter.newLocal(Types.ITERATOR)

    val begin = adapter.newLabel()
    val end = adapter.newLabel()

    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.value())
    adapter.invokeInterface(Types.MAP, Methods.get("size", Types.INT))
    adapter.invokeVirtual(Types.ANDROID_PARCEL, Methods.get("writeInt", Types.VOID, Types.INT))

    adapter.loadLocal(context.value())
    adapter.invokeInterface(Types.MAP, Methods.get("entrySet", Types.SET))
    adapter.invokeInterface(Types.COLLECTION, Methods.get("iterator", Types.ITERATOR))
    adapter.storeLocal(iterator)

    adapter.mark(begin)
    adapter.loadLocal(iterator)
    adapter.invokeInterface(Types.ITERATOR, Methods.get("hasNext", Types.BOOLEAN))
    adapter.ifZCmp(Opcodes.IFEQ, end)

    adapter.loadLocal(iterator)
    adapter.invokeInterface(Types.ITERATOR, Methods.get("next", Types.OBJECT))
    adapter.checkCast(Types.ENTRY)
    adapter.dup()

    adapter.invokeInterface(Types.ENTRY, Methods.get("getKey", Types.OBJECT))
    adapter.checkCast(keyType)
    adapter.storeLocal(keyElement)
    adapter.writeKey(context.typed(parameterizedType.typeArguments[0]).apply {
      value(keyElement)
    })

    adapter.invokeInterface(Types.ENTRY, Methods.get("getValue", Types.OBJECT))
    adapter.checkCast(valueType)
    adapter.storeLocal(valueElement)
    adapter.writeValue(context.typed(parameterizedType.typeArguments[1]).apply {
      value(valueElement)
    })

    adapter.goTo(begin)
    adapter.mark(end)
  }

  private fun GeneratorAdapter.readKey(context: ValueContext) = key.fromParcel(this, context)
  private fun GeneratorAdapter.writeKey(context: ValueContext) = key.toParcel(this, context)

  private fun GeneratorAdapter.readValue(context: ValueContext) = value.fromParcel(this, context)
  private fun GeneratorAdapter.writeValue(context: ValueContext) = value.toParcel(this, context)
}

internal class AdaptedWithClassValueAdapter(
    private val adapterType: Type,
    private val elementType: Type
) : OptionalValueAdapter() {
  private companion object {
    private val METHOD_TO_PARCEL = Methods.get("toParcel", Types.VOID, Types.OBJECT, Types.ANDROID_PARCEL, Types.INT)
    private val METHOD_FROM_PARCEL = Methods.get("fromParcel", Types.OBJECT, Types.ANDROID_PARCEL)
  }

  override fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.newInstance(adapterType, Methods.getConstructor())
    adapter.loadLocal(context.parcel())
    adapter.invokeInterface(Types.SMUGGLER_ADAPTER, METHOD_FROM_PARCEL)
    adapter.checkCast(elementType)
  }

  override fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.newInstance(adapterType, Methods.getConstructor())
    adapter.loadLocal(context.value())
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.flags())
    adapter.invokeInterface(Types.SMUGGLER_ADAPTER, METHOD_TO_PARCEL)
  }
}

internal class AdaptedWithObjectValueAdapter(
    private val adapterType: Type,
    private val elementType: Type
) : OptionalValueAdapter() {
  private companion object {
    private val METHOD_TO_PARCEL = Methods.get("toParcel", Types.VOID, Types.OBJECT, Types.ANDROID_PARCEL, Types.INT)
    private val METHOD_FROM_PARCEL = Methods.get("fromParcel", Types.OBJECT, Types.ANDROID_PARCEL)
  }

  override fun fromParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.getStatic(adapterType, "INSTANCE", adapterType)
    adapter.loadLocal(context.parcel())
    adapter.invokeInterface(Types.SMUGGLER_ADAPTER, METHOD_FROM_PARCEL)
    adapter.checkCast(elementType)
  }

  override fun toParcelNotNull(adapter: GeneratorAdapter, context: ValueContext) {
    adapter.getStatic(adapterType, "INSTANCE", adapterType)
    adapter.loadLocal(context.value())
    adapter.loadLocal(context.parcel())
    adapter.loadLocal(context.flags())
    adapter.invokeInterface(Types.SMUGGLER_ADAPTER, METHOD_TO_PARCEL)
  }
}
