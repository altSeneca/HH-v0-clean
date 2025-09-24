package com.hazardhawk.documents.models

import com.hazardhawk.ai.models.Hazard
import com.hazardhawk.ai.models.WorkType
import kotlinx.serialization.Serializable

/**
 * Toolbox Talk document model for weekly safety meetings.
 * Generated from recent hazard analysis to address current site conditions.
 */
@Serializable
data class ToolboxTalkDocument(
    val id: String,
    val title: String,
    val topic: SafetyTopic,
    val createdAt: Long,
    val meetingInfo: MeetingInfo,
    val talkContent: TalkContent,
    val interactiveElements: InteractiveElements,
    val safetyReminders: List<SafetyReminder>,
    val visualAids: List<VisualAid>,
    val attendance: AttendanceSection,
    val followUpActions: List<FollowUpAction>,
    val sourceHazards: List<String> = emptyList() // Hazard IDs that inspired this talk
)

/**
 * Meeting information and logistics.
 */
@Serializable
data class MeetingInfo(
    val meetingDate: String,
    val meetingTime: String,
    val duration: String, // e.g., "15 minutes"
    val location: String,
    val conductor: String,
    val targetAudience: String,
    val weatherConditions: String? = null
)

/**
 * Main content of the toolbox talk.
 */
@Serializable
data class TalkContent(
    val introduction: String,
    val objectives: List<String>,
    val keyPoints: List<KeyPoint>,
    val realWorldExamples: List<String>,
    val statistics: List<SafetyStatistic>,
    val conclusion: String
)

/**
 * Key point with supporting details.
 */
@Serializable
data class KeyPoint(
    val point: String,
    val explanation: String,
    val oshaReference: String? = null,
    val personalStory: String? = null,
    val actionItems: List<String> = emptyList()
)

/**
 * Safety statistic to support the talk.
 */
@Serializable
data class SafetyStatistic(
    val statistic: String,
    val source: String,
    val context: String,
    val relevance: String
)

/**
 * Interactive elements to engage attendees.
 */
@Serializable
data class InteractiveElements(
    val discussionQuestions: List<DiscussionQuestion>,
    val scenarios: List<SafetyScenario>,
    val demonstrations: List<Demonstration>,
    val quizQuestions: List<QuizQuestion> = emptyList()
)

/**
 * Discussion question for audience engagement.
 */
@Serializable
data class DiscussionQuestion(
    val question: String,
    val purpose: String,
    val expectedAnswers: List<String>,
    val followUpPoints: List<String>
)

/**
 * Safety scenario for discussion.
 */
@Serializable
data class SafetyScenario(
    val title: String,
    val scenario: String,
    val hazards: List<String>,
    val correctResponse: String,
    val lessonsLearned: List<String>
)

/**
 * Live demonstration instructions.
 */
@Serializable
data class Demonstration(
    val title: String,
    val equipment: List<String>,
    val steps: List<String>,
    val safetyNotes: List<String>,
    val learningObjectives: List<String>
)

/**
 * Quiz question for knowledge check.
 */
@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)

/**
 * Safety reminder for key takeaways.
 */
@Serializable
data class SafetyReminder(
    val reminder: String,
    val priority: Priority,
    val frequency: String, // "Daily", "Before each task", etc.
    val consequences: String? = null
)

/**
 * Visual aid reference for the presentation.
 */
@Serializable
data class VisualAid(
    val type: VisualAidType,
    val title: String,
    val description: String,
    val source: String, // File path, URL, or photo ID
    val purpose: String
)

/**
 * Attendance tracking section.
 */
@Serializable
data class AttendanceSection(
    val attendees: List<Attendee>,
    val absentees: List<String> = emptyList(),
    val makeUpRequired: List<String> = emptyList()
)

/**
 * Individual attendee record.
 */
@Serializable
data class Attendee(
    val name: String,
    val signature: String? = null, // Base64 encoded
    val employeeId: String? = null,
    val company: String? = null,
    val participated: Boolean = true,
    val questionsAsked: List<String> = emptyList()
)

/**
 * Follow-up action from the meeting.
 */
@Serializable
data class FollowUpAction(
    val action: String,
    val responsible: String,
    val dueDate: String,
    val priority: Priority,
    val status: ActionStatus = ActionStatus.PENDING
)

/**
 * Safety topics for toolbox talks.
 */
@Serializable
enum class SafetyTopic {
    FALL_PROTECTION,
    PPE_COMPLIANCE,
    ELECTRICAL_SAFETY,
    SCAFFOLDING_SAFETY,
    EXCAVATION_SAFETY,
    LADDER_SAFETY,
    TOOL_SAFETY,
    CHEMICAL_SAFETY,
    FIRE_PREVENTION,
    HEAT_STRESS,
    COLD_WEATHER,
    HOUSEKEEPING,
    LOCKOUT_TAGOUT,
    CRANE_SAFETY,
    CONFINED_SPACES,
    INCIDENT_REPORTING,
    EMERGENCY_PROCEDURES,
    HAZARD_COMMUNICATION,
    RESPIRATORY_PROTECTION,
    HEARING_CONSERVATION,
    BACK_INJURY_PREVENTION,
    VEHICLE_SAFETY,
    GENERAL_SAFETY_AWARENESS
}

/**
 * Visual aid types.
 */
@Serializable
enum class VisualAidType {
    PHOTO,
    DIAGRAM,
    VIDEO,
    POSTER,
    DEMONSTRATION,
    HANDOUT,
    CHECKLIST,
    INFOGRAPHIC
}

/**
 * Action status tracking.
 */
@Serializable
enum class ActionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    OVERDUE,
    CANCELLED
}

/**
 * Toolbox talk generation request.
 */
@Serializable
data class ToolboxTalkGenerationRequest(
    val topic: SafetyTopic,
    val recentHazards: List<Hazard>,
    val meetingInfo: MeetingInfo,
    val targetAudience: String = "Construction Workers",
    val includePhotos: Boolean = true,
    val includeInteractiveElements: Boolean = true,
    val customRequirements: List<String> = emptyList()
)

/**
 * Toolbox talk generation response.
 */
@Serializable
data class ToolboxTalkGenerationResponse(
    val document: ToolboxTalkDocument,
    val generationMetadata: GenerationMetadata,
    val engagementScore: Float,
    val recommendations: List<String>
)