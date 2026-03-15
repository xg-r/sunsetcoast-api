package kr.sunsetcoast.domain.room.vo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

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

    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)