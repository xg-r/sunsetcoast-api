package kr.sunsetcoast.domain.room.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.sunsetcoast.domain.reservation.vo.OtaPlatform

/**
 * 플랫폼 파일에 적힌 객실명 -> 내부 객실 매핑
 */
@Entity
@Table(
    name = "room_aliases",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_room_aliases_source_name", columnNames = ["source", "external_name"]),
    ]
)
class RoomAliasEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: OtaPlatform,

    @Column(name = "external_name", nullable = false)
    val externalName: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    val room: RoomEntity,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
