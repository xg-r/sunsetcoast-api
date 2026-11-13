package kr.sunsetcoast.domain.staycalendar.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

/** 관공서 공휴일. 매년 마이그레이션 시드로 추가한다. */
@Entity
@Table(name = "holidays")
class HolidayEntity(
    @Id
    @Column(name = "holiday_date")
    val holidayDate: LocalDate,

    @Column(nullable = false, length = 50)
    val name: String,
)
