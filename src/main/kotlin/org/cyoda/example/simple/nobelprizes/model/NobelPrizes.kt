package org.cyoda.example.simple.nobelprizes.model

import kotlinx.serialization.Serializable
import org.cyoda.example.simple.serializations.LocalDateKSerializer
import org.cyoda.example.simple.serializations.UuidKSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class NobelPrizes(
    @Serializable(with = LocalDateKSerializer::class)
    val extractionDate: LocalDate,
    val data: PrizeData
)

@Serializable
data class PrizeData(
    val source:String,
    val data: List<NobelPrize>
)


@Serializable
data class NobelPrize(
    @Serializable(with = UuidKSerializer::class) val dataSetId: UUID? = null,
    val category: String,
    val year: String,
    val overallMotivation: String? = "",
    val laureates: List<Laureate> = emptyList()
) {
    fun withDataSetId(dataSetId: UUID) = NobelPrize(dataSetId, category, year, overallMotivation,laureates)
}

@Serializable
data class Laureate(
    val firstname: String,
    val share: String,
    val id: String,
    val surname: String = "",
    val motivation: String = ""
)
