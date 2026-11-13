package kr.sunsetcoast.domain.staycalendar.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.sunsetcoast.domain.staycalendar.vo.ListPrice
import java.math.BigDecimal

/** 객실 정가 (docs/요금표.xlsx) */
@Entity
@Table(name = "room_list_prices")
class RoomListPriceEntity(
    @Id
    @Column(name = "room_id")
    val roomId: Long,

    @Column(name = "offpeak_weekday", nullable = false, precision = 15, scale = 2)
    val offpeakWeekday: BigDecimal,

    @Column(name = "offpeak_weekend", nullable = false, precision = 15, scale = 2)
    val offpeakWeekend: BigDecimal,

    @Column(name = "peak_weekday", nullable = false, precision = 15, scale = 2)
    val peakWeekday: BigDecimal,

    @Column(name = "peak_weekend", nullable = false, precision = 15, scale = 2)
    val peakWeekend: BigDecimal,
) {
    fun toListPrice() = ListPrice(offpeakWeekday, offpeakWeekend, peakWeekday, peakWeekend)
}
