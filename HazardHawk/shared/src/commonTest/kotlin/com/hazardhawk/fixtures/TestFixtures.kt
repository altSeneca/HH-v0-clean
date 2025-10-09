package com.hazardhawk.fixtures

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

/**
 * Test fixtures for Phase 2 testing
 * Provides sample data for certifications, crew, projects, and dashboard
 */
object TestFixtures {
    
    /**
     * Sample certification fixtures
     */
    object Certifications {
        
        fun osha10(
            id: String = "cert-osha10-001",
            workerId: String = "worker-001",
            workerName: String = "John Doe"
        ) = mapOf(
            "id" to id,
            "workerId" to workerId,
            "workerName" to workerName,
            "certificationType" to "OSHA 10",
            "certificationNumber" to "OSHA10-2025-${System.currentTimeMillis() % 1000000}",
            "issueDate" to "2025-01-15",
            "expirationDate" to null,
            "issuingAuthority" to "OSHA Authorized Training Provider",
            "documentUrl" to "https://cdn.hazardhawk.com/certifications/$id.pdf",
            "status" to "active",
            "verificationStatus" to "approved",
            "uploadedAt" to Clock.System.now().toString(),
            "verifiedAt" to Clock.System.now().toString(),
            "verifiedBy" to "admin-001"
        )
        
        fun osha30(
            id: String = "cert-osha30-001",
            workerId: String = "worker-002",
            workerName: String = "Jane Smith"
        ) = mapOf(
            "id" to id,
            "workerId" to workerId,
            "workerName" to workerName,
            "certificationType" to "OSHA 30",
            "certificationNumber" to "OSHA30-2025-${System.currentTimeMillis() % 1000000}",
            "issueDate" to "2025-02-01",
            "expirationDate" to null,
            "issuingAuthority" to "Construction Safety Institute",
            "documentUrl" to "https://cdn.hazardhawk.com/certifications/$id.pdf",
            "status" to "active",
            "verificationStatus" to "approved"
        )
        
        fun cpr(
            id: String = "cert-cpr-001",
            workerId: String = "worker-003",
            workerName: String = "Bob Johnson",
            expirationDate: String = "2027-03-10"
        ) = mapOf(
            "id" to id,
            "workerId" to workerId,
            "workerName" to workerName,
            "certificationType" to "CPR",
            "certificationNumber" to "CPR-2025-${System.currentTimeMillis() % 1000000}",
            "issueDate" to "2025-03-10",
            "expirationDate" to expirationDate,
            "issuingAuthority" to "American Red Cross",
            "documentUrl" to "https://cdn.hazardhawk.com/certifications/$id.pdf",
            "status" to "active",
            "verificationStatus" to "pending"
        )
        
        fun firstAid(
            id: String = "cert-firstaid-001",
            workerId: String = "worker-004",
            expirationDate: String = "2027-04-15"
        ) = mapOf(
            "id" to id,
            "workerId" to workerId,
            "certificationType" to "First Aid",
            "certificationNumber" to "FIRSTAID-2025-${System.currentTimeMillis() % 1000000}",
            "issueDate" to "2025-04-15",
            "expirationDate" to expirationDate,
            "issuingAuthority" to "American Red Cross"
        )
        
        fun expired(
            id: String = "cert-expired-001",
            workerId: String = "worker-005",
            certificationType: String = "CPR"
        ) = mapOf(
            "id" to id,
            "workerId" to workerId,
            "certificationType" to certificationType,
            "certificationNumber" to "$certificationType-2023-999999",
            "issueDate" to "2023-01-01",
            "expirationDate" to "2025-01-01",
            "status" to "expired",
            "verificationStatus" to "approved"
        )
        
        fun pending(
            id: String = "cert-pending-001",
            workerId: String = "worker-006"
        ) = mapOf(
            "id" to id,
            "workerId" to workerId,
            "certificationType" to "OSHA 10",
            "certificationNumber" to "OSHA10-2025-PENDING",
            "issueDate" to "2025-10-01",
            "status" to "active",
            "verificationStatus" to "pending",
            "uploadedAt" to Clock.System.now().toString(),
            "verifiedAt" to null,
            "verifiedBy" to null
        )
    }
    
    /**
     * Sample crew/worker fixtures
     */
    object Crew {
        
        fun worker(
            id: String = "worker-001",
            firstName: String = "John",
            lastName: String = "Doe",
            email: String = "john.doe@example.com",
            role: String = "general_laborer"
        ) = mapOf(
            "id" to id,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phone" to "+1555${(1000000..9999999).random()}",
            "role" to role,
            "status" to "active",
            "certifications" to listOf("cert-osha10-001", "cert-firstaid-001"),
            "assignedProjects" to listOf("project-001", "project-002"),
            "hireDate" to "2024-06-15",
            "dateOfBirth" to "1990-05-20",
            "emergencyContact" to mapOf(
                "name" to "Jane Doe",
                "phone" to "+15559876543",
                "relationship" to "Spouse"
            ),
            "qrCode" to "https://cdn.hazardhawk.com/qr/$id.png"
        )
        
        fun supervisor(
            id: String = "worker-sup-001",
            firstName: String = "Sarah",
            lastName: String = "Manager"
        ) = mapOf(
            "id" to id,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to "${firstName.lowercase()}.${lastName.lowercase()}@example.com",
            "phone" to "+15552345678",
            "role" to "site_supervisor",
            "status" to "active",
            "certifications" to listOf("cert-osha30-001", "cert-cpr-001", "cert-firstaid-001"),
            "assignedProjects" to listOf("project-001"),
            "hireDate" to "2022-01-10",
            "dateOfBirth" to "1985-08-15"
        )
        
        fun foreman(
            id: String = "worker-foreman-001"
        ) = mapOf(
            "id" to id,
            "firstName" to "Mike",
            "lastName" to "Foreman",
            "email" to "mike.foreman@example.com",
            "phone" to "+15553456789",
            "role" to "foreman",
            "status" to "active",
            "certifications" to listOf("cert-osha30-001"),
            "assignedProjects" to listOf("project-001", "project-002", "project-003")
        )
        
        fun inactive(
            id: String = "worker-inactive-001"
        ) = mapOf(
            "id" to id,
            "firstName" to "Old",
            "lastName" to "Worker",
            "email" to "old.worker@example.com",
            "role" to "general_laborer",
            "status" to "inactive",
            "certifications" to emptyList<String>(),
            "assignedProjects" to emptyList<String>(),
            "hireDate" to "2020-01-01",
            "terminationDate" to "2024-12-31"
        )
    }
    
    /**
     * Sample project fixtures
     */
    object Projects {
        
        fun constructionProject(
            id: String = "project-001",
            name: String = "Downtown Office Building"
        ) = mapOf(
            "id" to id,
            "name" to name,
            "description" to "15-story office building construction",
            "status" to "active",
            "startDate" to "2025-01-01",
            "expectedEndDate" to "2026-12-31",
            "location" to mapOf(
                "address" to "123 Main St",
                "city" to "Springfield",
                "state" to "IL",
                "zip" to "62701"
            ),
            "assignedCrew" to listOf("worker-001", "worker-002", "worker-sup-001"),
            "requiredCertifications" to listOf("OSHA 10", "Fall Protection", "First Aid")
        )
        
        fun roadworkProject(
            id: String = "project-002",
            name: String = "Highway 95 Expansion"
        ) = mapOf(
            "id" to id,
            "name" to name,
            "description" to "Highway expansion and repaving",
            "status" to "active",
            "startDate" to "2025-03-15",
            "expectedEndDate" to "2025-11-30",
            "assignedCrew" to listOf("worker-001", "worker-003"),
            "requiredCertifications" to listOf("OSHA 10", "Traffic Control")
        )
        
        fun completedProject(
            id: String = "project-completed-001"
        ) = mapOf(
            "id" to id,
            "name" to "Completed Project",
            "status" to "completed",
            "startDate" to "2024-01-01",
            "expectedEndDate" to "2024-12-31",
            "actualEndDate" to "2024-12-15",
            "assignedCrew" to emptyList<String>()
        )
    }
    
    /**
     * Sample dashboard/metrics fixtures
     */
    object Dashboard {
        
        fun safetyMetrics(period: String = "last_30_days") = mapOf(
            "period" to period,
            "incidentCount" to 2,
            "incidentRate" to 0.5,
            "nearMissCount" to 8,
            "safetyObservations" to 45,
            "complianceScore" to 92.5,
            "activeCertifications" to 156,
            "expiringCertifications" to 12,
            "expiredCertifications" to 3,
            "totalWorkers" to 120,
            "activeProjects" to 8
        )
        
        fun complianceSummary() = mapOf(
            "totalWorkers" to 120,
            "workersWithAllCerts" to 108,
            "workersWithExpiringSoon" to 12,
            "workersWithExpired" to 3,
            "compliancePercentage" to 90.0,
            "requiredCertifications" to listOf("OSHA 10", "Fall Protection", "First Aid/CPR"),
            "mostCommonGap" to "Fall Protection"
        )
        
        fun activityFeed() = listOf(
            mapOf(
                "id" to "activity-001",
                "type" to "certification_uploaded",
                "timestamp" to Clock.System.now().toString(),
                "actor" to "worker-001",
                "actorName" to "John Doe",
                "description" to "Uploaded OSHA 10 certification"
            ),
            mapOf(
                "id" to "activity-002",
                "type" to "certification_approved",
                "timestamp" to Clock.System.now().toString(),
                "actor" to "admin-001",
                "actorName" to "Sarah Admin",
                "description" to "Approved OSHA 10 certification for John Doe"
            ),
            mapOf(
                "id" to "activity-003",
                "type" to "safety_observation",
                "timestamp" to Clock.System.now().toString(),
                "actor" to "worker-sup-001",
                "actorName" to "Sarah Manager",
                "description" to "Reported safety hazard on Project Alpha"
            )
        )
    }
    
    /**
     * Sample test images (base64 encoded 1x1 pixel PNG for testing)
     */
    object TestImages {
        // 1x1 red pixel PNG
        const val RED_PIXEL_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
        
        // 1x1 blue pixel PNG
        const val BLUE_PIXEL_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        
        // Sample certificate placeholder (100x100 white PNG)
        const val CERTIFICATE_PLACEHOLDER = "iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAALEgAACxIB0t1+/AAAABZ0RVh0Q3JlYXRpb24gVGltZQAwNy8xMy8xM8tzCccAAAAcdEVYdFNvZnR3YXJlAEFkb2JlIEZpcmV3b3JrcyBDUzVxteM2AAAAEElEQVR4nGNgYGBgAAAABQABpfZFQAAAAABJRU5ErkJggg=="
    }
}
