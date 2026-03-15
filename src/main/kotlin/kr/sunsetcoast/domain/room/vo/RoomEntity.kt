package kr.sunsetcoast.domain.room.vo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.sunsetcoast.domain.businessowner.vo.BusinessOwnerEntity

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_owner_id")
    val businessOwnerEntity: BusinessOwnerEntity,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
