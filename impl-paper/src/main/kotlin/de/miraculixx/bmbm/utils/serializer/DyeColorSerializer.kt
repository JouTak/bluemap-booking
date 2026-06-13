package de.miraculixx.bmbm.utils.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.DyeColor

object DyeColorSerializer : KSerializer<DyeColor> {
    override val descriptor = PrimitiveSerialDescriptor("DyeColor", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DyeColor = DyeColor.valueOf(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: DyeColor) {
        encoder.encodeString(value.name)
    }
}
