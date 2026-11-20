package app.studydesk.core.content

import app.studydesk.core.model.Subject
import javax.inject.Inject
import javax.inject.Named

/** Which subjects can be opened: reviewed ones always, unreviewed ones when the build allows it. */
class ContentVisibility @Inject constructor(
    @param:Named("showUnreviewed") private val showUnreviewed: Boolean,
) {
    fun canOpen(subject: Subject): Boolean = subject.hasContent && (subject.reviewed == true || showUnreviewed)

    fun canOpen(reviewed: Boolean): Boolean = reviewed || showUnreviewed
}
