package org.cyoda.example.simple.serializations

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.BigInteger
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*


object UuidKSerializer : KSerializer<UUID> {
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
}

object BigDecimalKSerializer : KSerializer<BigDecimal> {
    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)
}

object BigIntegerKSerializer : KSerializer<BigInteger> {
    override fun serialize(encoder: Encoder, value: BigInteger) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): BigInteger = BigInteger(decoder.decodeString())
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)
}

object LocalDateKSerializer : KSerializer<LocalDate> {
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(DateTimeFormatter.ISO_DATE.format(value))
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE)
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
}

object LocalDateTimeKSerializer : KSerializer<LocalDateTime> {
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(DateTimeFormatter.ISO_DATE_TIME.format(value))
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString(),
        DateTimeFormatter.ISO_DATE_TIME)
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
}

object ZonedDateTimeKSerializer : KSerializer<ZonedDateTime> {
    override fun serialize(encoder: Encoder, value: ZonedDateTime) = encoder.encodeString(DateTimeFormatter.ISO_DATE_TIME.format(value))
    override fun deserialize(decoder: Decoder): ZonedDateTime = ZonedDateTime.parse(decoder.decodeString(),
        DateTimeFormatter.ISO_DATE_TIME)
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)
}

object LocalTimeKSerializer : KSerializer<LocalTime> {
    override fun serialize(encoder: Encoder, value: LocalTime) = encoder.encodeString(DateTimeFormatter.ISO_TIME.format(value))
    override fun deserialize(decoder: Decoder): LocalTime = LocalTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_TIME)
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)
}

object YearMonthKSerializer : KSerializer<YearMonth> {
    override fun serialize(encoder: Encoder, value: YearMonth) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): YearMonth = YearMonth.parse(decoder.decodeString())
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("YearMonth", PrimitiveKind.STRING)
}

object YearKSerializer : KSerializer<Year> {
    override fun serialize(encoder: Encoder, value: Year) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Year = Year.parse(decoder.decodeString())
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Year", PrimitiveKind.STRING)
}

object DateKSerializer : KSerializer<Date> {

    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeString(
        DateTimeFormatter.ISO_DATE_TIME.format(
            LocalDateTime.ofInstant(
                value.toInstant(), ZoneId.systemDefault()
            )
        )
    )
    override fun deserialize(decoder: Decoder): Date {
        val parse = LocalDateTime.parse(
            decoder.decodeString(),
            DateTimeFormatter.ISO_DATE_TIME
        )
        return Date.from(parse
            .atZone(ZoneId.systemDefault())
            .toInstant()
        )
    }
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)
}

