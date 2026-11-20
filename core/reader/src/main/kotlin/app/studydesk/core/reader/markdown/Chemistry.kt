package app.studydesk.core.reader.markdown

/**
 * Finds chemical formulas in plain text (H2SO4, C6H5NH2, NO2+, AlCl3, Cl2) so the reader can
 * draw the digits as subscripts and a trailing charge as a superscript.
 *
 * Deliberately cautious, because a wrong subscript is worse than none:
 * - every part must be a real element symbol (so course codes like BP301T are skipped);
 * - a single element with a number counts only for common molecules (H2, O2, O3, Cl2, Br2, I2, N2),
 *   so vitamin B12, vitamin K3 and "C6" are left alone;
 * - mechanism names (SN1, SN2) are excluded.
 */
object Chemistry {
    data class Mark(val start: Int, val end: Int, val superscript: Boolean)

    private val ELEMENTS = setOf(
        "H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar",
        "K", "Ca", "Ti", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "As", "Se", "Br", "Ag", "Sn", "Sb", "I",
        "Ba", "Pt", "Au", "Hg", "Pb", "Bi",
    )
    private val MOLECULES = setOf("H2", "N2", "O2", "O3", "F2", "Cl2", "Br2", "I2")
    private val NOT_FORMULAS = setOf("SN1", "SN2")
    private val TOKEN = Regex("(?<![A-Za-z0-9])((?:[A-Z][a-z]?\\d*)+)([+\\-−]{1,2})?(?![A-Za-z0-9])")
    private val PART = Regex("([A-Z][a-z]?)(\\d*)")

    fun marks(text: String): List<Mark> {
        val marks = mutableListOf<Mark>()
        for (match in TOKEN.findAll(text)) {
            val formula = match.groups[1]!!
            val charge = match.groups[2]
            if (formula.value in NOT_FORMULAS) continue
            val parts = PART.findAll(formula.value).toList()
            if (parts.any { it.groupValues[1] !in ELEMENTS }) continue
            val hasDigit = formula.value.any(Char::isDigit)
            val valid = when {
                parts.size >= 2 -> hasDigit || charge != null
                else -> formula.value in MOLECULES || (charge != null && formula.value.length <= 3)
            }
            if (!valid) continue
            for (part in parts) {
                val digits = part.groups[2]!!
                if (digits.value.isNotEmpty()) {
                    marks += Mark(formula.range.first + digits.range.first, formula.range.first + digits.range.last + 1, false)
                }
            }
            if (charge != null) marks += Mark(charge.range.first, charge.range.last + 1, true)
        }
        return marks
    }
}
