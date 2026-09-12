package kr.sunsetcoast.domain.carreservations.vo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kr.sunsetcoast.domain.car.vo.CarEntity
import kr.sunsetcoast.domain.room.vo.RoomEntity
import java.time.LocalDateTime

@Entity
@Table(name = "car_reservations")
class CarReservationEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    val car: CarEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_reservation_customer_id", nullable = false)
    val customer: CarReservationCustomerEntity,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    val room: RoomEntity,

    @Column(name = "start_at", nullable = false)
    val startAt: LocalDateTime,

    @Column(name = "end_at", nullable = false)
    val endAt: LocalDateTime,

    @Column(name = "memo", length = 500)
    val memo: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
