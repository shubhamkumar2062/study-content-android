package app.studydesk.core.model

import kotlinx.serialization.Serializable

/** `v1/catalog.json`: stream > programme > scheme > term > subject. */
@Serializable
data class Catalog(
    val contentFormat: Int,
    val examBodies: List<ExamBody> = emptyList(),
    val streams: List<Stream> = emptyList(),
) {
    fun examBody(id: String): ExamBody? = examBodies.firstOrNull { it.id == id }

    fun programme(streamId: String, programmeId: String): Programme? =
        streams.firstOrNull { it.id == streamId }?.programmes?.firstOrNull { it.id == programmeId }

    fun term(course: CourseSelection): Term? =
        programme(course.streamId, course.programmeId)
            ?.schemes?.firstOrNull { it.id == course.schemeId }
            ?.terms?.firstOrNull { it.id == course.termId }

    /** Every subject with the path that leads to it. */
    val allSubjects: List<SubjectRef>
        get() = streams.flatMap { stream ->
            stream.programmes.flatMap { programme ->
                programme.schemes.flatMap { scheme ->
                    scheme.terms.flatMap { term ->
                        term.subjects.map { SubjectRef(stream, programme, scheme, term, it) }
                    }
                }
            }
        }

    fun subject(subjectId: String): SubjectRef? = allSubjects.firstOrNull { it.subject.id == subjectId }
}

@Serializable
data class ExamBody(
    val id: String,
    val name: String,
    val shortName: String,
    val website: String,
    val examPage: String? = null,
)

@Serializable
data class Stream(
    val id: String,
    val name: String,
    val programmes: List<Programme> = emptyList(),
)

@Serializable
data class Programme(
    val id: String,
    val name: String,
    val level: String,
    val examBodyId: String,
    val note: String? = null,
    val schemes: List<Scheme> = emptyList(),
)

@Serializable
data class Scheme(
    val id: String,
    val name: String,
    val termKind: String,
    val status: String,
    val syllabusSource: Source? = null,
    val note: String? = null,
    val terms: List<Term> = emptyList(),
) {
    /** "confirmed", "to-confirm" or "not-sourced". */
    val isListed: Boolean get() = status != "not-sourced"
}

@Serializable
data class Source(val title: String, val url: String? = null)

@Serializable
data class Term(
    val id: String,
    val number: Int,
    val name: String,
    val subjects: List<Subject> = emptyList(),
)

@Serializable
data class Subject(
    val id: String,
    val code: String,
    val title: String,
    val kind: String,
    val linkedTheory: String? = null,
    /** Id of the pack that holds this subject's content; null while there is none. */
    val pack: String? = null,
    val reviewed: Boolean? = null,
) {
    val isPractical: Boolean get() = kind == "practical"
    val hasContent: Boolean get() = pack != null
}

/** A subject together with where it sits in the catalog. */
data class SubjectRef(
    val stream: Stream,
    val programme: Programme,
    val scheme: Scheme,
    val term: Term,
    val subject: Subject,
)

/** The student's own course, chosen on first launch. */
@Serializable
data class CourseSelection(
    val streamId: String,
    val programmeId: String,
    val schemeId: String,
    val termId: String,
)
