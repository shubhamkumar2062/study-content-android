package app.studydesk.core.model

import kotlinx.serialization.Serializable

/** `v1/packs/<subjectId>.json`: everything for one subject. */
@Serializable
data class SubjectPack(
    val contentFormat: Int,
    val subjectId: String,
    val code: String,
    val title: String,
    val stream: String,
    val programme: String,
    val scheme: String,
    val term: String,
    val reviewed: Boolean,
    val syllabusSource: Source? = null,
    val practicalCodes: List<String> = emptyList(),
    val units: List<StudyUnit> = emptyList(),
    val extras: List<Extra> = emptyList(),
    val questions: List<Question> = emptyList(),
    val mcqs: List<Mcq> = emptyList(),
    val mockTests: List<MockTest> = emptyList(),
    val practicals: List<Practical> = emptyList(),
    val media: List<String> = emptyList(),
) {
    @Serializable
    data class StudyUnit(val number: Int, val title: String, val markdown: String)

    @Serializable
    data class Extra(val id: String, val title: String, val markdown: String)

    @Serializable
    data class Practical(val id: String, val title: String, val markdown: String)
}

@Serializable
data class Question(
    val id: String,
    val unit: Int,
    val type: String,
    val marks: Int,
    val probability: String,
    val question: String,
    val answer: String,
    val tags: List<String> = emptyList(),
) {
    val rating: Probability get() = Probability.of(probability)
}

@Serializable
data class Mcq(
    val id: String,
    val unit: Int,
    val question: String,
    val options: List<String>,
    val correct: Int,
    val explanation: String,
    val probability: String,
    val tags: List<String> = emptyList(),
) {
    val rating: Probability get() = Probability.of(probability)
}

@Serializable
data class MockTest(
    val id: String,
    val title: String,
    val patternSource: String? = null,
    val durationMinutes: Int,
    val totalMarks: Int,
    val sections: List<Section>,
) {
    @Serializable
    data class Section(
        val id: String,
        val title: String,
        val source: String,
        val marksEach: Int,
        val count: Int,
        val attempt: Int,
    )
}

enum class Probability(val rank: Int) {
    High(0), Medium(1), Low(2);

    companion object {
        fun of(value: String): Probability = when (value.lowercase()) {
            "high" -> High
            "medium" -> Medium
            else -> Low
        }
    }
}
