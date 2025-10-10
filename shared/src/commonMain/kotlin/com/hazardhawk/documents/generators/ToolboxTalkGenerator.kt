package com.hazardhawk.documents.generators
import kotlinx.datetime.Clock

import com.hazardhawk.core.models.*
import com.hazardhawk.documents.models.*
import com.hazardhawk.documents.services.DocumentAIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.uuid.uuid4

/**
 * AI-powered Toolbox Talk generator that creates engaging safety presentations
 * from recent hazard analysis and trending safety issues.
 */
class ToolboxTalkGenerator(
    private val aiService: DocumentAIService
) {
    
    /**
     * Generate a complete Toolbox Talk document from recent hazards and safety topics.
     */
    suspend fun generateToolboxTalk(
        request: ToolboxTalkGenerationRequest
    ): Result<ToolboxTalkGenerationResponse> = withContext(Dispatchers.Default) {
        
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        try {
            // Phase 1: Analyze recent hazards for topic relevance
            val relevantHazards = filterRelevantHazards(request.recentHazards, request.topic)
            
            // Phase 2: Generate core talk content using AI
            val talkContent = generateTalkContent(
                topic = request.topic,
                hazards = relevantHazards,
                audience = request.targetAudience
            )
            
            // Phase 3: Create interactive elements
            val interactiveElements = generateInteractiveElements(
                topic = request.topic,
                hazards = relevantHazards,
                includeInteractive = request.includeInteractiveElements
            )
            
            // Phase 4: Generate safety reminders and takeaways
            val safetyReminders = generateSafetyReminders(
                topic = request.topic,
                hazards = relevantHazards
            )
            
            // Phase 5: Create visual aids references
            val visualAids = generateVisualAids(
                topic = request.topic,
                hazards = relevantHazards,
                includePhotos = request.includePhotos
            )
            
            // Phase 6: Generate follow-up actions
            val followUpActions = generateFollowUpActions(
                hazards = relevantHazards,
                topic = request.topic
            )
            
            // Phase 7: Assemble complete document
            val toolboxTalk = ToolboxTalkDocument(
                id = uuid4().toString(),
                title = generateTalkTitle(request.topic, relevantHazards),
                topic = request.topic,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                meetingInfo = request.meetingInfo,
                talkContent = talkContent,
                interactiveElements = interactiveElements,
                safetyReminders = safetyReminders,
                visualAids = visualAids,
                attendance = createEmptyAttendanceSection(),
                followUpActions = followUpActions,
                sourceHazards = relevantHazards.map { it.id }
            )
            
            // Phase 8: Calculate engagement score and recommendations
            val engagementScore = calculateEngagementScore(toolboxTalk)
            val recommendations = generatePresentationRecommendations(
                toolboxTalk = toolboxTalk,
                hazards = relevantHazards
            )
            
            val response = ToolboxTalkGenerationResponse(
                document = toolboxTalk,
                generationMetadata = GenerationMetadata(
                    aiModel = "Gemma 3N E2B + Safety Knowledge Base",
                    processingTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                    confidenceScore = calculateContentConfidence(relevantHazards),
                    hazardsProcessed = relevantHazards.size,
                    templatesUsed = listOf("Interactive Toolbox Talk Template"),
                    reviewRequired = engagementScore < 0.7f
                ),
                engagementScore = engagementScore,
                recommendations = recommendations
            )
            
            Result.success(response)
            
        } catch (e: Exception) {
            Result.failure(Exception("Toolbox Talk generation failed: ${e.message}", e))
        }
    }
    
    /**
     * Filter hazards relevant to the selected topic.
     */
    private fun filterRelevantHazards(hazards: List<Hazard>, topic: SafetyTopic): List<Hazard> {
        return hazards.filter { hazard ->
            when (topic) {
                SafetyTopic.FALL_PROTECTION -> hazard.type == HazardType.FALL_PROTECTION
                SafetyTopic.PPE_COMPLIANCE -> hazard.type == HazardType.PPE_VIOLATION
                SafetyTopic.ELECTRICAL_SAFETY -> hazard.type == HazardType.ELECTRICAL_HAZARD
                SafetyTopic.SCAFFOLDING_SAFETY -> hazard.type == HazardType.SCAFFOLDING_UNSAFE
                SafetyTopic.TOOL_SAFETY -> hazard.type == HazardType.EQUIPMENT_DEFECT
                SafetyTopic.CHEMICAL_SAFETY -> hazard.type == HazardType.CHEMICAL_HAZARD
                SafetyTopic.FIRE_PREVENTION -> hazard.type == HazardType.FIRE_HAZARD
                SafetyTopic.HOUSEKEEPING -> hazard.type == HazardType.HOUSEKEEPING
                SafetyTopic.LOCKOUT_TAGOUT -> hazard.type == HazardType.LOCKOUT_TAGOUT
                SafetyTopic.CONFINED_SPACES -> hazard.type == HazardType.CONFINED_SPACE
                SafetyTopic.GENERAL_SAFETY_AWARENESS -> true // Include all hazards
                else -> hazard.severity in listOf(Severity.HIGH, Severity.CRITICAL)
            }
        }.sortedByDescending { it.severity.ordinal }
    }
    
    /**
     * Generate engaging talk content with real-world examples.
     */
    private suspend fun generateTalkContent(
        topic: SafetyTopic,
        hazards: List<Hazard>,
        audience: String
    ): TalkContent {
        
        val introduction = generateIntroduction(topic, hazards, audience)
        val objectives = generateLearningObjectives(topic, hazards)
        val keyPoints = generateKeyPoints(topic, hazards)
        val examples = generateRealWorldExamples(topic, hazards)
        val statistics = generateRelevantStatistics(topic)
        val conclusion = generateConclusion(topic, hazards)
        
        return TalkContent(
            introduction = introduction,
            objectives = objectives,
            keyPoints = keyPoints,
            realWorldExamples = examples,
            statistics = statistics,
            conclusion = conclusion
        )
    }
    
    private fun generateIntroduction(
        topic: SafetyTopic,
        hazards: List<Hazard>,
        audience: String
    ): String {
        val topicName = formatTopicName(topic)
        val hasRecentIncidents = hazards.isNotEmpty()
        
        return when {
            hasRecentIncidents -> {
                val criticalCount = hazards.count { it.severity == Severity.CRITICAL }
                """
                Good morning, team. Today we're focusing on $topicName because our recent site analysis 
                identified ${hazards.size} related safety concern${if (hazards.size > 1) "s" else ""}.
                ${if (criticalCount > 0) "We found $criticalCount critical issue${if (criticalCount > 1) "s" else ""} that need immediate attention." else ""}
                This 15-minute discussion will help us stay safe and compliant with OSHA standards.
                """.trimIndent()
            }
            else -> {
                """
                Good morning, everyone. Today's safety topic is $topicName. Even though we haven't 
                seen recent incidents in this area, it's important to stay vigilant and review 
                best practices. Prevention is always better than reaction when it comes to safety.
                """.trimIndent()
            }
        }
    }
    
    private fun generateLearningObjectives(
        topic: SafetyTopic,
        hazards: List<Hazard>
    ): List<String> {
        val baseObjectives = when (topic) {
            SafetyTopic.FALL_PROTECTION -> listOf(
                "Identify fall hazards at 6 feet or greater",
                "Select appropriate fall protection systems",
                "Inspect fall protection equipment properly"
            )
            SafetyTopic.PPE_COMPLIANCE -> listOf(
                "Recognize required PPE for different tasks",
                "Properly inspect and maintain PPE",
                "Understand consequences of PPE non-compliance"
            )
            SafetyTopic.ELECTRICAL_SAFETY -> listOf(
                "Identify electrical hazards on construction sites",
                "Apply lockout/tagout procedures correctly",
                "Use electrical equipment safely"
            )
            else -> listOf(
                "Understand key hazards related to ${formatTopicName(topic)}",
                "Apply safety best practices in daily work",
                "Recognize when to stop work for safety"
            )
        }
        
        // Add hazard-specific objectives
        val hazardObjectives = if (hazards.isNotEmpty()) {
            listOf("Address specific site conditions identified in recent analysis")
        } else emptyList()
        
        return baseObjectives + hazardObjectives
    }
    
    private fun generateKeyPoints(
        topic: SafetyTopic,
        hazards: List<Hazard>
    ): List<KeyPoint> {
        val points = mutableListOf<KeyPoint>()
        
        // Add topic-specific key points
        when (topic) {
            SafetyTopic.FALL_PROTECTION -> {
                points.add(KeyPoint(
                    point = "The 6-foot rule applies to most construction work",
                    explanation = "OSHA requires fall protection when working at heights of 6 feet or more above a lower level",
                    oshaReference = "29 CFR 1926.501",
                    actionItems = listOf(
                        "Always assess height before starting work",
                        "Ensure fall protection is in place before accessing elevated areas"
                    )
                ))
                
                points.add(KeyPoint(
                    point = "Three types of fall protection systems",
                    explanation = "Guardrail systems, safety net systems, and personal fall arrest systems each have specific applications",
                    actionItems = listOf(
                        "Choose the right system for the job",
                        "Inspect equipment before each use"
                    )
                ))
            }
            
            SafetyTopic.PPE_COMPLIANCE -> {
                points.add(KeyPoint(
                    point = "PPE is your last line of defense",
                    explanation = "Personal protective equipment protects you when other controls aren't sufficient",
                    oshaReference = "29 CFR 1926.95",
                    actionItems = listOf(
                        "Wear required PPE at all times in designated areas",
                        "Report damaged or missing PPE immediately"
                    )
                ))
            }
            
            else -> {
                points.add(KeyPoint(
                    point = "Safety is everyone's responsibility",
                    explanation = "Each worker has the right and responsibility to work safely and report hazards",
                    actionItems = listOf(
                        "Speak up when you see unsafe conditions",
                        "Follow all safety procedures and protocols"
                    )
                ))
            }
        }
        
        // Add points based on recent hazards
        hazards.take(2).forEach { hazard ->
            points.add(KeyPoint(
                point = "Recent site concern: ${hazard.type.name.replace('_', ' ').lowercase()}",
                explanation = "Our AI analysis identified: ${hazard.description}",
                actionItems = hazard.recommendations
            ))
        }
        
        return points
    }
    
    private fun generateRealWorldExamples(
        topic: SafetyTopic,
        hazards: List<Hazard>
    ): List<String> {
        val examples = mutableListOf<String>()
        
        // Topic-specific examples
        when (topic) {
            SafetyTopic.FALL_PROTECTION -> {
                examples.add("A roofer fell 12 feet when his safety harness wasn't properly secured, resulting in a broken leg and 6 weeks off work")
                examples.add("A scaffold collapse injured 3 workers because proper fall arrest systems weren't used")
            }
            SafetyTopic.PPE_COMPLIANCE -> {
                examples.add("An eye injury from metal shavings could have been prevented with safety glasses")
                examples.add("A worker avoided serious head injury because his hard hat deflected falling debris")
            }
            SafetyTopic.ELECTRICAL_SAFETY -> {
                examples.add("An electrician was electrocuted when working on a circuit assumed to be de-energized")
                examples.add("Proper lockout/tagout saved a maintenance worker's life when equipment unexpectedly started")
            }
            else -> {
                examples.add("Following safety procedures prevented a potential incident on a similar project")
            }
        }
        
        // Add examples based on hazard types found
        hazards.forEach { hazard ->
            when (hazard.type) {
                HazardType.FALL_PROTECTION -> 
                    examples.add("Site example: ${hazard.description} - This could lead to serious injury")
                HazardType.PPE_VIOLATION -> 
                    examples.add("Recent observation: ${hazard.description} - A reminder of why PPE matters")
                else -> 
                    examples.add("Current site condition: ${hazard.description}")
            }
        }
        
        return examples.distinct().take(3)
    }
    
    private fun generateRelevantStatistics(topic: SafetyTopic): List<SafetyStatistic> {
        return when (topic) {
            SafetyTopic.FALL_PROTECTION -> listOf(
                SafetyStatistic(
                    statistic = "Falls are the #1 cause of construction fatalities",
                    source = "OSHA Construction Focus Four",
                    context = "36% of construction deaths in 2019",
                    relevance = "Every fall protection violation is a potential fatality"
                )
            )
            SafetyTopic.PPE_COMPLIANCE -> listOf(
                SafetyStatistic(
                    statistic = "Eye injuries cost $300 million annually in lost productivity",
                    source = "National Institute for Occupational Safety and Health",
                    context = "90% could be prevented with proper eye protection",
                    relevance = "Safety glasses are required in construction zones"
                )
            )
            SafetyTopic.ELECTRICAL_SAFETY -> listOf(
                SafetyStatistic(
                    statistic = "Electrical incidents cause 8.5% of construction fatalities",
                    source = "OSHA Focus Four Hazards",
                    context = "Most are preventable with proper procedures",
                    relevance = "Lockout/tagout is critical for electrical safety"
                )
            )
            else -> listOf(
                SafetyStatistic(
                    statistic = "1 in 5 worker deaths in 2019 was in construction",
                    source = "Bureau of Labor Statistics",
                    context = "Despite being 5% of the workforce",
                    relevance = "Construction is inherently hazardous - safety protocols save lives"
                )
            )
        }
    }
    
    private fun generateConclusion(topic: SafetyTopic, hazards: List<Hazard>): String {
        val topicName = formatTopicName(topic)
        return """
        Remember, $topicName isn't just about compliance - it's about everyone going home safely. 
        ${if (hazards.isNotEmpty()) "We've identified specific concerns on our site, so let's all commit to addressing them." else ""}
        If you see something unsafe, speak up. If you have questions, ask. Your safety and the safety of your coworkers depends on it.
        Any questions before we get back to work?
        """.trimIndent()
    }
    
    // Generate interactive elements for engagement
    private fun generateInteractiveElements(
        topic: SafetyTopic,
        hazards: List<Hazard>,
        includeInteractive: Boolean
    ): InteractiveElements {
        
        if (!includeInteractive) {
            return InteractiveElements(
                discussionQuestions = emptyList(),
                scenarios = emptyList(),
                demonstrations = emptyList()
            )
        }
        
        val questions = generateDiscussionQuestions(topic, hazards)
        val scenarios = generateSafetyScenarios(topic, hazards)
        val demonstrations = generateDemonstrations(topic)
        
        return InteractiveElements(
            discussionQuestions = questions,
            scenarios = scenarios,
            demonstrations = demonstrations
        )
    }
    
    private fun generateDiscussionQuestions(
        topic: SafetyTopic,
        hazards: List<Hazard>
    ): List<DiscussionQuestion> {
        val questions = mutableListOf<DiscussionQuestion>()
        
        // Topic-specific questions
        when (topic) {
            SafetyTopic.FALL_PROTECTION -> {
                questions.add(DiscussionQuestion(
                    question = "What are the three main types of fall protection systems?",
                    purpose = "Assess knowledge of fall protection hierarchy",
                    expectedAnswers = listOf("Guardrails", "Safety nets", "Personal fall arrest"),
                    followUpPoints = listOf("Each has specific applications", "Guardrails are preferred when feasible")
                ))
            }
            SafetyTopic.PPE_COMPLIANCE -> {
                questions.add(DiscussionQuestion(
                    question = "What should you do if your PPE is damaged?",
                    purpose = "Reinforce PPE inspection and replacement procedures",
                    expectedAnswers = listOf("Stop work", "Report to supervisor", "Get replacement"),
                    followUpPoints = listOf("Damaged PPE provides no protection", "Replace immediately")
                ))
            }
            else -> {
                questions.add(DiscussionQuestion(
                    question = "What would you do if you saw an unsafe condition?",
                    purpose = "Reinforce hazard reporting culture",
                    expectedAnswers = listOf("Report immediately", "Stop work if necessary", "Tell supervisor"),
                    followUpPoints = listOf("Everyone has the right to stop work", "Safety is everyone's responsibility")
                ))
            }
        }
        
        // Hazard-specific questions
        if (hazards.isNotEmpty()) {
            val hazardQuestion = "We found ${hazards.size} safety concern(s) in our recent analysis. What should be our response?"
            questions.add(DiscussionQuestion(
                question = hazardQuestion,
                purpose = "Connect analysis results to action",
                expectedAnswers = listOf("Address immediately", "Implement controls", "Monitor conditions"),
                followUpPoints = listOf("AI helps us identify risks", "We must act on the information")
            ))
        }
        
        return questions
    }
    
    private fun generateSafetyScenarios(
        topic: SafetyTopic,
        hazards: List<Hazard>
    ): List<SafetyScenario> {
        val scenarios = mutableListOf<SafetyScenario>()
        
        // Use actual hazards as scenarios
        hazards.take(2).forEach { hazard ->
            scenarios.add(SafetyScenario(
                title = "Site Condition Scenario",
                scenario = "You encounter this situation: ${hazard.description}. What do you do?",
                hazards = listOf(hazard.type.name.replace('_', ' ')),
                correctResponse = hazard.immediateAction ?: hazard.recommendations.firstOrNull() ?: "Report to supervisor",
                lessonsLearned = hazard.recommendations
            ))
        }
        
        return scenarios
    }
    
    private fun generateDemonstrations(topic: SafetyTopic): List<Demonstration> {
        return when (topic) {
            SafetyTopic.FALL_PROTECTION -> listOf(
                Demonstration(
                    title = "Harness Inspection",
                    equipment = listOf("Safety harness", "Inspection checklist"),
                    steps = listOf(
                        "Check all webbing for cuts, fraying, or burn marks",
                        "Inspect all hardware for cracks, distortion, or corrosion", 
                        "Verify all buckles operate smoothly",
                        "Check attachment points for wear"
                    ),
                    safetyNotes = listOf("Replace harness if any defects found", "Never modify or repair harness"),
                    learningObjectives = listOf("Recognize defective harness", "Perform proper inspection")
                )
            )
            SafetyTopic.PPE_COMPLIANCE -> listOf(
                Demonstration(
                    title = "Proper PPE Donning",
                    equipment = listOf("Hard hat", "Safety glasses", "Gloves", "Safety vest"),
                    steps = listOf(
                        "Adjust hard hat for secure, comfortable fit",
                        "Position safety glasses properly on face",
                        "Select appropriate gloves for task",
                        "Ensure safety vest is visible and fastened"
                    ),
                    safetyNotes = listOf("PPE only works when worn correctly", "Comfort encourages compliance"),
                    learningObjectives = listOf("Demonstrate proper PPE use", "Understand fit requirements")
                )
            )
            else -> emptyList()
        }
    }
    
    // Helper methods
    private fun generateTalkTitle(topic: SafetyTopic, hazards: List<Hazard>): String {
        val baseName = formatTopicName(topic)
        return if (hazards.isNotEmpty()) {
            "$baseName: Addressing Current Site Conditions"
        } else {
            "$baseName: Best Practices Review"
        }
    }
    
    private fun formatTopicName(topic: SafetyTopic): String {
        return topic.name.lowercase().split('_').joinToString(" ") { 
            it.replaceFirstChar { char -> char.uppercase() }
        }
    }
    
    private fun generateSafetyReminders(
        topic: SafetyTopic,
        hazards: List<Hazard>
    ): List<SafetyReminder> {
        val reminders = mutableListOf<SafetyReminder>()
        
        // Topic-specific reminders
        when (topic) {
            SafetyTopic.FALL_PROTECTION -> {
                reminders.add(SafetyReminder(
                    reminder = "Check your fall protection equipment before each use",
                    priority = Priority.HIGH,
                    frequency = "Before each elevated task",
                    consequences = "Equipment failure could result in serious injury or death"
                ))
            }
            SafetyTopic.PPE_COMPLIANCE -> {
                reminders.add(SafetyReminder(
                    reminder = "Wear all required PPE in designated areas",
                    priority = Priority.HIGH,
                    frequency = "Always when on site",
                    consequences = "PPE violations can result in injury and disciplinary action"
                ))
            }
            else -> {
                reminders.add(SafetyReminder(
                    reminder = "Always follow safety procedures and report hazards",
                    priority = Priority.MEDIUM,
                    frequency = "Throughout workday"
                ))
            }
        }
        
        // Hazard-specific reminders
        hazards.filter { it.severity in listOf(Severity.HIGH, Severity.CRITICAL) }.forEach { hazard ->
            reminders.add(SafetyReminder(
                reminder = "Address identified ${hazard.type.name.replace('_', ' ').lowercase()}: ${hazard.description}",
                priority = when (hazard.severity) {
                    Severity.CRITICAL -> Priority.CRITICAL
                    Severity.HIGH -> Priority.HIGH
                    else -> Priority.MEDIUM
                },
                frequency = "Before starting work in affected area"
            ))
        }
        
        return reminders
    }
    
    private fun generateVisualAids(
        topic: SafetyTopic,
        hazards: List<Hazard>,
        includePhotos: Boolean
    ): List<VisualAid> {
        val aids = mutableListOf<VisualAid>()
        
        if (includePhotos && hazards.isNotEmpty()) {
            aids.add(VisualAid(
                type = VisualAidType.PHOTO,
                title = "Site Analysis Photos",
                description = "Photos from recent AI hazard analysis showing current conditions",
                source = "hazard_analysis_photos", // Photo collection ID
                purpose = "Show real site conditions to discuss"
            ))
        }
        
        aids.add(VisualAid(
            type = VisualAidType.POSTER,
            title = "${formatTopicName(topic)} Safety Poster",
            description = "Visual reference for key safety practices",
            source = "safety_posters/${topic.name.lowercase()}",
            purpose = "Visual reinforcement of key concepts"
        ))
        
        return aids
    }
    
    private fun generateFollowUpActions(
        hazards: List<Hazard>,
        topic: SafetyTopic
    ): List<FollowUpAction> {
        val actions = mutableListOf<FollowUpAction>()
        
        // Critical hazards need immediate action
        hazards.filter { it.severity == Severity.CRITICAL }.forEach { hazard ->
            actions.add(FollowUpAction(
                action = "Address critical ${hazard.type.name.replace('_', ' ').lowercase()}: ${hazard.description}",
                responsible = "Site Supervisor",
                dueDate = "Today",
                priority = Priority.CRITICAL
            ))
        }
        
        // General follow-up for topic
        actions.add(FollowUpAction(
            action = "Schedule ${formatTopicName(topic)} refresher training",
            responsible = "Safety Manager",
            dueDate = "Within 30 days",
            priority = Priority.MEDIUM
        ))
        
        return actions
    }
    
    private fun createEmptyAttendanceSection(): AttendanceSection {
        return AttendanceSection(
            attendees = emptyList(),
            absentees = emptyList(),
            makeUpRequired = emptyList()
        )
    }
    
    private fun calculateEngagementScore(talk: ToolboxTalkDocument): Float {
        var score = 0.5f // Base score
        
        // Add points for interactive elements
        if (talk.interactiveElements.discussionQuestions.isNotEmpty()) score += 0.1f
        if (talk.interactiveElements.scenarios.isNotEmpty()) score += 0.1f
        if (talk.interactiveElements.demonstrations.isNotEmpty()) score += 0.1f
        
        // Add points for visual aids
        if (talk.visualAids.isNotEmpty()) score += 0.1f
        
        // Add points for real-world examples
        if (talk.talkContent.realWorldExamples.isNotEmpty()) score += 0.1f
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun generatePresentationRecommendations(
        toolboxTalk: ToolboxTalkDocument,
        hazards: List<Hazard>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        recommendations.add("Start with the real site conditions from AI analysis to grab attention")
        recommendations.add("Encourage participation through discussion questions")
        
        if (hazards.any { it.severity == Severity.CRITICAL }) {
            recommendations.add("Emphasize critical hazards identified on site")
        }
        
        if (toolboxTalk.interactiveElements.demonstrations.isNotEmpty()) {
            recommendations.add("Use hands-on demonstrations to reinforce key points")
        }
        
        recommendations.add("End with specific commitments from team members")
        
        return recommendations
    }
    
    private fun calculateContentConfidence(hazards: List<Hazard>): Float {
        return if (hazards.isNotEmpty()) {
            hazards.map { it.confidence }.average().toFloat()
        } else {
            0.8f // Base confidence for general content
        }
    }
}