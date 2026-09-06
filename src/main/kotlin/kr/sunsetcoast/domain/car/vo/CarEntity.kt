package kr.sunsetcoast.domain.car.vo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "cars")
class CarEntity(
    @Column(name = "car_number", nullable = false, unique = true)
    val carNumber: String,

    @Column(name = "car_model", nullable = false)
    val carModel: String,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
