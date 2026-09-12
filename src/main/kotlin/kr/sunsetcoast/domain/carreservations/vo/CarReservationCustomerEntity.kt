package kr.sunsetcoast.domain.carreservations.vo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "car_reservation_customers")
class CarReservationCustomerEntity(
    @Column(name = "name", nullable = false, length = 50)
    val name: String,

    @Column(name = "phone", nullable = false, length = 20)
    val phone: String,

    @Column(name = "identity_hash", nullable = false, length = 64)
    val identityHash: String,

    @Column(
        name = "identity_encrypted",
        nullable = false,
        columnDefinition = "VARBINARY(512)"
    )
    val identityEncrypted: ByteArray,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
