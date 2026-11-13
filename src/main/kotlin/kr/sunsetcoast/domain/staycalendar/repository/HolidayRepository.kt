package kr.sunsetcoast.domain.staycalendar.repository

import kr.sunsetcoast.domain.staycalendar.entity.HolidayEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface HolidayRepository : JpaRepository<HolidayEntity, LocalDate> {
    fun findByHolidayDateBetween(from: LocalDate, to: LocalDate): List<HolidayEntity>
}
