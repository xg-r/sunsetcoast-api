package kr.sunsetcoast.domain.room.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.sunsetcoast.domain.businessowner.entity.BusinessOwnerEntity
import kr.sunsetcoast.domain.property.entity.PropertyEntity

@Entity
@Table(
    name = "rooms",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_rooms_name", columnNames = ["name"]),
    ]
)
class RoomEntity(
    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, length = 30)
    val type: String,

    @Column(nullable = false, length = 30)
    val status: String,

    /** 운영 단위(업소) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    val property: PropertyEntity,

    /** 명의 단위(사업자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_owner_id", nullable = false)
    val businessOwnerEntity: BusinessOwnerEntity,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
