package app.studydesk.core.model

import kotlinx.serialization.Serializable

/** `v1/exams/<bodyId>.json`: one exam body's date sheets. */
@Serializable
data class ExamSchedule(
    val bodyId: String,
    val sessions: List<ExamSession> = emptyList(),
)

@Serializable
data class ExamSession(
    val id: String,
    val title: String,
    val status: String,
    val publishedOn: String? = null,
    val lastChecked: String,
    val officialUrl: String,
    val note: String? = null,
    val papers: List<ExamPaper> = emptyList(),
) {
    val isTentative: Boolean get() = status == "tentative"
}

@Serializable
data class ExamPaper(
    val date: String,
    val start: String,
    val end: String,
    val programmeId: String,
    val schemeId: String,
    val termId: String,
    val subjectCode: String,
    val title: String,
    val paperType: String? = null,
)
