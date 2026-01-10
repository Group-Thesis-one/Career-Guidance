package com.example.careerguidance.data.cv

import com.example.careerguidance.data.recommendation.SkillNormalizer
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.util.Locale

data class CvExtractedProfile(
    val educationLevel: String? = null,
    val major: String? = null,
    val experienceYears: Int? = null,
    val skillsRaw: List<String> = emptyList(),
    val skillsNormalized: List<String> = emptyList(),
    val phone: String? = null
)

object CvParser {

    fun extractTextFromPdfBytes(pdfBytes: ByteArray): String {
        val doc = PDDocument.load(pdfBytes)
        return try {
            PDFTextStripper().getText(doc)
        } finally {
            doc.close()
        }
    }

    fun extractProfileFromText(
        rawText: String,
        skillOptions: List<String>,
        normalizer: SkillNormalizer
    ): CvExtractedProfile {
        val text = rawText
            .replace("\r", "\n")
            .replace(Regex("[ \t]+"), " ")
            .lowercase(Locale.getDefault())

        val phone = extractPhone(text)

        val education = detectEducation(text)
        val major = detectMajor(text)

        val expYears = extractExperienceYears(text)

        val detectedSkills = detectSkills(text, skillOptions)
        val normalized = normalizer.normalizeAll(detectedSkills).toList().sorted()

        return CvExtractedProfile(
            educationLevel = education,
            major = major,
            experienceYears = expYears,
            skillsRaw = detectedSkills.sorted(),
            skillsNormalized = normalized,
            phone = phone
        )
    }

    private fun extractPhone(text: String): String? {
        val r = Regex("(\\+?\\d[\\d \\-()]{7,}\\d)")
        return r.find(text)?.value?.trim()
    }

    private fun extractExperienceYears(text: String): Int? {
        // examples: "2 years", "3+ years", "over 5 years"
        val r = Regex("(\\d{1,2})\\s*\\+?\\s*(years|year)\\s*(of\\s*)?(experience|exp)?")
        val m = r.find(text) ?: return null
        return m.groupValues[1].toIntOrNull()
    }

    private fun detectEducation(text: String): String? {
        return when {
            "phd" in text || "doctorate" in text -> "PhD"
            "master" in text || "msc" in text || "m.sc" in text -> "Master"
            "bachelor" in text || "bsc" in text || "b.sc" in text -> "Bachelor"
            "diploma" in text -> "Diploma"
            "high school" in text || "secondary school" in text -> "High School"
            else -> null
        }
    }

    private fun detectMajor(text: String): String? {
        return when {
            "computer science" in text || "computing" in text -> "Computer Science"
            "software engineering" in text -> "Software Engineering"
            "information technology" in text || "it " in text -> "Information Technology"
            "data science" in text || "data analytics" in text -> "Data Science"
            "cybersecurity" in text || "information security" in text -> "Cybersecurity"
            "business" in text || "management" in text -> "Business"
            else -> null
        }
    }

    private fun detectSkills(text: String, skillOptions: List<String>): List<String> {
        val found = mutableSetOf<String>()
        for (s in skillOptions) {
            val needle = s.lowercase(Locale.getDefault())
            if (needle.isNotBlank() && text.contains(needle)) {
                found.add(needle)
            }
        }
        return found.toList()
    }
}
