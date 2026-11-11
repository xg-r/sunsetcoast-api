package kr.sunsetcoast.domain.businessowner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.sunsetcoast.domain.businessowner.vo.OwnershipType
import java.time.LocalDateTime

@Entity
@Table(
    name = "business_owners",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_business_number", columnNames = ["business_number"]),
    ]
)
class BusinessOwnerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_type", nullable = false, length = 20)
    val ownershipType: OwnershipType = OwnershipType.FAMILY,

    @Column(name = "business_number", length = 20)
    val businessNumber: String? = null,

    @Column(name = "representative_name", length = 100)
    val representativeName: String? = null,

    @Column(length = 30)
    val phone: String? = null,

    @Column(length = 255)
    val email: String? = null,

    @Column(columnDefinition = "text")
    val address: String? = null,

    @Column(nullable = false, length = 30)
    val status: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
