package app.studydesk.feature.practice

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** A light tick for a right answer and a distinct buzz for a wrong one (system haptics settings apply). */
internal fun View.answerHaptic(correct: Boolean) {
    val constant = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> if (correct) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        else -> if (correct) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}
