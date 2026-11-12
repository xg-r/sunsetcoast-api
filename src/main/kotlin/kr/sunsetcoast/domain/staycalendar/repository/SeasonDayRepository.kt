package kr.sunsetcoast.domain.staycalendar.repository

import kr.sunsetcoast.domain.staycalendar.entity.SeasonDayEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SeasonDayRepository : JpaRepository<SeasonDayEntity, Long> {
    fun findByPropertyIdAndStayDateBetween(propertyId: Long, from: LocalDate, to: LocalDate): List<SeasonDayEntity>

    fun findByPropertyIdAndStayDate(propertyId: Long, stayDate: LocalDate): SeasonDayEntity?
}
