package kr.sunsetcoast.domain.reservation.entity

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
import kr.sunsetcoast.domain.room.entity.RoomEntity

/** 예약의 객실. 다중 객실 예약은 객실마다 1행, 단일 객실 예약도 1행을 가진다. */
@Entity
@Table(
    name = "reservation_rooms",
    uniqueConstraints = [UniqueConstraint(name = "uq_reservation_rooms", columnNames = ["reservation_id", "room_id"])],
)
class ReservationRoomEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    val reservation: ReservationEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    val room: RoomEntity,

    /** 파일에 적힌 순서 (0부터) */
    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
